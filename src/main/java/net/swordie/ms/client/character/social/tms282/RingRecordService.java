package net.swordie.ms.client.character.social.tms282;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CoupleRecord;
import net.swordie.ms.client.character.MarriageRecord;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.social.tms282.RingRecordStore.Marriage;
import net.swordie.ms.client.character.social.tms282.RingRecordStore.PairRing;
import net.swordie.ms.client.social.Friend.FriendshipRingRecord;
import net.swordie.ms.connection.OutPacket;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Couple / friendship / marriage ring records of online characters (facade over {@link RingRecordStore}).
 * <ul>
 *     <li>{@link #onLogin} loads the character's records and {@link #apply applies} them: the 265 Char fields
 *     ({@code couple}/{@code inCouple}, {@code friendshipRingRecord}, {@code marriageRecord}) are set from the records
 *     whose ring is currently worn (marriage: whenever married), so the existing 265 encoders
 *     (UserPool.userEnterField, UserRemote.avatarModified) pick them up;</li>
 *     <li>{@link #onEquipChanged} re-applies after the worn rings changed;</li>
 *     <li>{@link #createPair} / {@link #marry} persist new records (cash-shop purchase, wedding script, GM command)
 *     and refresh whichever side is online;</li>
 *     <li>{@link #encodeCharacterData265} writes the 265 CharacterData ring block (DBChar.CoupleRecord).</li>
 * </ul>
 * Database failures are logged and leave the character without ring records; they never break login or equip.
 */
public final class RingRecordService {
    private static final RingRecordService INSTANCE = new RingRecordService();

    /** Loaded records of one online character. */
    record State(List<PairRing> pairs, Optional<Marriage> marriage) {
    }

    private final Map<Integer, State> states = new ConcurrentHashMap<>();

    private RingRecordService() {
    }

    public static RingRecordService get() {
        return INSTANCE;
    }

    // ---- lifecycle -------------------------------------------------------------------------------------------

    /** Loads the records of {@code chr} from the database and applies them. Call once the character is loaded. */
    public void onLogin(Char chr) {
        if (chr == null) {
            return;
        }
        states.put(chr.getId(), load(chr.getId()));
        apply(chr);
    }

    /** Forgets the cached records (the database keeps them). */
    public void onLogout(Char chr) {
        if (chr != null) {
            states.remove(chr.getId());
        }
    }

    /** Re-applies the cached records after an equip / unequip (the worn rings decide which effect is shown). */
    public void onEquipChanged(Char chr) {
        if (chr != null) {
            apply(chr);
        }
    }

    private State load(int characterId) {
        try {
            return new State(RingRecordStore.loadPairs(characterId), RingRecordStore.loadMarriage(characterId));
        } catch (SQLException e) {
            SocialTables.log("load ring records of " + characterId, e);
            return new State(List.of(), Optional.empty());
        }
    }

    private State state(Char chr) {
        return states.computeIfAbsent(chr.getId(), this::load);
    }

    public List<PairRing> pairs(Char chr) {
        return state(chr).pairs();
    }

    public Optional<Marriage> marriage(Char chr) {
        return state(chr).marriage();
    }

    /** Sets the 265 Char ring fields from the records whose ring is worn. */
    public void apply(Char chr) {
        State s = state(chr);
        Set<Long> worn = wornRingSerials(chr);
        PairRing couple = null;
        PairRing friend = null;
        for (PairRing r : s.pairs()) {
            if (!worn.contains(r.ringSn())) {
                continue;
            }
            if (r.kind() == RingKind.COUPLE && couple == null) {
                couple = r;
            } else if (r.kind() == RingKind.FRIENDSHIP && friend == null) {
                friend = r;
            }
        }
        if (couple != null) {
            CoupleRecord cr = new CoupleRecord();
            cr.setPairCharacterId(couple.pairCharacterId());
            cr.setPairCharacterName(couple.pairName());
            cr.setSn(couple.ringSn());
            cr.setPairSn(couple.pairRingSn());
            cr.setItemID(couple.ringId());
            chr.setCouple(cr);
            chr.setInCouple(true);
        } else {
            chr.setCouple(null);
            chr.setInCouple(false);
        }
        if (friend != null) {
            FriendshipRingRecord fr = new FriendshipRingRecord();
            fr.setFriendshipItemSN(friend.ringSn());
            fr.setFriendshipPairItemSN(friend.pairRingSn());
            fr.setItemID(friend.ringId());
            chr.setFriendshipRingRecord(fr);
        } else {
            chr.setFriendshipRingRecord(null);
        }
        chr.setMarriageRecord(s.marriage().filter(m -> m.stage() == Marriage.MARRIED)
                .map(RingRecordService::toMarriageRecord).orElse(null));
    }

    static MarriageRecord toMarriageRecord(Marriage m) {
        MarriageRecord mr = new MarriageRecord();
        mr.setMarriageNo(m.marriageId());
        mr.setGroomId(m.groomId());
        mr.setBrideId(m.brideId());
        mr.setStatus((short) m.stage());
        mr.setGroomItemId(m.groomRingId());
        mr.setBrideItemId(m.brideRingId());
        mr.setGroomName(m.groomName());
        mr.setBridgeName(m.brideName());
        return mr;
    }

    static Set<Long> wornRingSerials(Char chr) {
        Set<Long> out = new HashSet<>();
        Inventory equipped = chr.getEquippedInventory();
        if (equipped == null) {
            return out;
        }
        for (Item item : equipped.getItems()) {
            if (item != null && RingKind.isPairRing(item.getItemId())) {
                out.add(RingKind.serialOf(item));
            }
        }
        return out;
    }

    // ---- creation ----------------------------------------------------------------------------------------------

    /**
     * Records a couple / friendship pair: {@code owner} holds a ring with serial {@code ownerSn}, the partner
     * ({@code partnerId}, {@code partnerName}) the ring with {@code partnerSn}; both are the same item {@code ringId}.
     * Online sides are refreshed.
     *
     * @return false when the item is not a couple / friendship ring or the database failed
     */
    public boolean createPair(int ownerId, String ownerName, int partnerId, String partnerName, int ringId,
                              long ownerSn, long partnerSn) {
        RingKind kind = RingKind.of(ringId);
        if (kind == null || kind == RingKind.MARRIAGE || ownerId == partnerId || ownerSn == 0 || partnerSn == 0) {
            return false;
        }
        try {
            RingRecordStore.insertPair(new PairRing(kind, ownerId, partnerId, partnerName, ringId, ownerSn, partnerSn),
                    ownerName);
        } catch (SQLException e) {
            SocialTables.log("create " + kind + " ring " + ownerId + "<->" + partnerId, e);
            return false;
        }
        reload(ownerId);
        reload(partnerId);
        return true;
    }

    /** {@link #createPair} for two ring items already held by the two characters. */
    public boolean createPair(Char owner, Item ownerRing, Char partner, Item partnerRing) {
        if (ownerRing.getItemId() != partnerRing.getItemId()) {
            return false;
        }
        return createPair(owner.getId(), owner.getName(), partner.getId(), partner.getName(), ownerRing.getItemId(),
                RingKind.serialOf(ownerRing), RingKind.serialOf(partnerRing));
    }

    /** Removes the pair that {@code ringSn} of {@code characterId} belongs to (ring destroyed / deleted). */
    public boolean destroyPair(RingKind kind, int characterId, long ringSn) {
        try {
            List<PairRing> before = states.containsKey(characterId) ? states.get(characterId).pairs() : List.of();
            int n = RingRecordStore.deletePair(kind, characterId, ringSn);
            reload(characterId);
            before.stream().filter(r -> r.ringSn() == ringSn).forEach(r -> reload(r.pairCharacterId()));
            return n > 0;
        } catch (SQLException e) {
            SocialTables.log("destroy " + kind + " ring " + ringSn + " of " + characterId, e);
            return false;
        }
    }

    /**
     * Records a marriage (stage MARRIED) between {@code groom} and {@code bride} with their wedding rings; for the
     * wedding script. Fails when either side is already engaged / married.
     */
    public Optional<Marriage> marry(Char groom, Item groomRing, Char bride, Item brideRing) {
        if (groom.getId() == bride.getId() || marriage(groom).isPresent() || marriage(bride).isPresent()) {
            return Optional.empty();
        }
        try {
            Marriage m = RingRecordStore.insertMarriage(new Marriage(0, groom.getId(), bride.getId(), groom.getName(),
                    bride.getName(), Marriage.MARRIED, groomRing == null ? 0 : groomRing.getItemId(),
                    brideRing == null ? 0 : brideRing.getItemId(), groomRing == null ? 0 : RingKind.serialOf(groomRing),
                    brideRing == null ? 0 : RingKind.serialOf(brideRing)));
            reload(groom.getId());
            reload(bride.getId());
            return Optional.of(m);
        } catch (SQLException e) {
            SocialTables.log("marry " + groom.getId() + "<->" + bride.getId(), e);
            return Optional.empty();
        }
    }

    /** Marks the current marriage of {@code chr} as divorced. */
    public boolean divorce(Char chr) {
        Optional<Marriage> m = marriage(chr);
        if (m.isEmpty()) {
            return false;
        }
        try {
            RingRecordStore.updateMarriageStage(m.get().marriageId(), Marriage.DIVORCED);
        } catch (SQLException e) {
            SocialTables.log("divorce " + chr.getId(), e);
            return false;
        }
        reload(m.get().groomId());
        reload(m.get().brideId());
        return true;
    }

    private void reload(int characterId) {
        Char online = Server.get().getChars().get(characterId);
        if (online != null) {
            states.put(characterId, load(characterId));
            apply(online);
        } else {
            states.remove(characterId);
        }
    }

    // ---- encoding ----------------------------------------------------------------------------------------------

    /**
     * The 265 CharacterData ring block (DBChar.CoupleRecord, replaces the three {@code encodeShort(0)} in
     * Char.encode): u16 n + n x CoupleRecord (i32 pair id, char[13] pair name, i64 sn, i64 pair sn); u16 n + n x
     * FriendRecord (the same + i32 item id); u16 n + n x MarriageRecord.encode. All records are sent, worn or not.
     * 265 layout (GMS v95 CharacterData); the 282 CharacterData ring block is not traced (see the report).
     */
    public void encodeCharacterData265(Char chr, OutPacket out) {
        State s = state(chr);
        List<PairRing> couples = s.pairs().stream().filter(r -> r.kind() == RingKind.COUPLE).toList();
        List<PairRing> friends = s.pairs().stream().filter(r -> r.kind() == RingKind.FRIENDSHIP).toList();
        out.encodeShort(couples.size());
        for (PairRing r : couples) {
            out.encodeInt(r.pairCharacterId());
            out.encodeString(r.pairName(), 13);
            out.encodeLong(r.ringSn());
            out.encodeLong(r.pairRingSn());
        }
        out.encodeShort(friends.size());
        for (PairRing r : friends) {
            out.encodeInt(r.pairCharacterId());
            out.encodeString(r.pairName(), 13);
            out.encodeLong(r.ringSn());
            out.encodeLong(r.pairRingSn());
            out.encodeInt(r.ringId());
        }
        Optional<Marriage> m = s.marriage().filter(x -> x.stage() == Marriage.MARRIED);
        out.encodeShort(m.isPresent() ? 1 : 0);
        m.ifPresent(x -> toMarriageRecord(x).encode(out));
    }

    // ---- TMS 282.3 encoding (research/tms2823/ring-records/README.md) ------------------------------------------

    /** 282 fixed name buffers (couple / friend pair name, groom / bride name) are 15 bytes (265: 13). */
    public static final int NAME_WIDTH_282 = 15;
    /** 282 couple record message buffer (record +0x23, shown by the item tooltip as "\r\n%s" when not empty). */
    public static final int COUPLE_MESSAGE_WIDTH_282 = 0xC9;
    /** CharacterData ring record sizes read by 8E49E0 / 8E4A20 / 8E4A60 (DecodeBuffer). */
    public static final int COUPLE_RECORD_BYTES_282 = 0xEC;
    public static final int FRIEND_RECORD_BYTES_282 = 0x27;
    public static final int MARRIAGE_RECORD_BYTES_282 = 0x34;
    /** CharacterData flag byte (100-byte flag array, one byte per 265 DBChar bit) of the ring block: 265 CoupleRecord. */
    public static final int CHARACTER_DATA_FLAG_282 = 11;
    /** Marriage status the 282 client treats as married (5E04932 / 8BC6BA: status >= 3; 1 = engaged). */
    public static final int MARRIED_STATUS_282 = 3;

    /** One worn couple / friendship ring as other users see it: buf16 = (own ring sn, partner's ring sn). */
    public record RemoteRing(long sn, long pairSn, int itemId, String message) {
        public RemoteRing {
            message = message == null ? "" : message;
        }
    }

    /** The wearer's marriage as other users see it (LP03D0 #67 / LP048D): groom id, bride id, ring item id. */
    public record RemoteMarriage(int groomId, int brideId, int itemId) {
    }

    /**
     * The ring section of LP03D0 (#65..#67, 5AC4271..5AC458D) and LP048D (5ADB098..5ADBD66), identical in both:
     * <pre>
     * u8 couple   [i32 n=1, n x (Buf16 (sn, pairSn), str message, i32 itemId)]
     * u8 friend   [i32 n=1, n x (Buf16 (sn, pairSn), i32 itemId)]
     * u8 marriage [i32 groomId, i32 brideId, i32 itemId]
     * </pre>
     * A null part is sent as u8 0 (the bytes every 282 encoder wrote before).
     */
    public record RemoteRings(RemoteRing couple, RemoteRing friendship, RemoteMarriage marriage) {
        public static final RemoteRings NONE = new RemoteRings(null, null, null);

        public boolean isEmpty() {
            return couple == null && friendship == null && marriage == null;
        }

        public void encode282(OutPacket out, Charset charset) {
            out.encodeByte(couple != null);
            if (couple != null) {
                out.encodeInt(1);
                out.encodeLong(couple.sn());
                out.encodeLong(couple.pairSn());
                str282(out, couple.message(), charset);
                out.encodeInt(couple.itemId());
            }
            out.encodeByte(friendship != null);
            if (friendship != null) {
                out.encodeInt(1);
                out.encodeLong(friendship.sn());
                out.encodeLong(friendship.pairSn());
                out.encodeInt(friendship.itemId());
            }
            out.encodeByte(marriage != null);
            if (marriage != null) {
                out.encodeInt(marriage.groomId());
                out.encodeInt(marriage.brideId());
                out.encodeInt(marriage.itemId());
            }
        }
    }

    /** Couple (message non-null) or friendship record of the CharacterData ring block. */
    public record DataPair(int pairCharacterId, String pairName, long sn, long pairSn, int itemId, String message) {
        public DataPair {
            pairName = pairName == null ? "" : pairName;
            message = message == null ? "" : message;
        }
    }

    /** Marriage record of the CharacterData ring block (52 bytes). */
    public record DataMarriage(int marriageNo, int groomId, int brideId, int status, int groomItemId,
                               int brideItemId, String groomName, String brideName) {
        public DataMarriage {
            groomName = groomName == null ? "" : groomName;
            brideName = brideName == null ? "" : brideName;
        }
    }

    /**
     * The 282 CharacterData ring block (flag [11], CharacterData::Decode 92DA97..92DE70):
     * <pre>
     * u16 n, n x Buf236 couple   (i32 pairId, char[15] pairName, i64 sn, i64 pairSn, char[201] message)
     * u16 n, n x Buf39  friend   (i32 pairId, char[15] pairName, i64 sn, i64 pairSn, i32 itemId)
     * u16 n, n x Buf52  marriage (i32 no, i32 groomId, i32 brideId, u16 status, i32 groomItem, i32 brideItem,
     *                             char[15] groomName, char[15] brideName)
     * </pre>
     * Sent only when not empty (the flag stays 0 otherwise, so ring-less characters get the old bytes).
     */
    public record CharacterDataRings(List<DataPair> couples, List<DataPair> friends, DataMarriage marriage) {
        public static final CharacterDataRings NONE = new CharacterDataRings(List.of(), List.of(), null);

        public CharacterDataRings {
            couples = List.copyOf(couples);
            friends = List.copyOf(friends);
        }

        public boolean isEmpty() {
            return couples.isEmpty() && friends.isEmpty() && marriage == null;
        }

        public void encode282(OutPacket out, Charset charset) {
            out.encodeShort(couples.size());
            for (DataPair r : couples) {
                out.encodeInt(r.pairCharacterId());
                out.encodeArr(fixed282(r.pairName(), NAME_WIDTH_282, charset));
                out.encodeLong(r.sn());
                out.encodeLong(r.pairSn());
                out.encodeArr(fixed282(r.message(), COUPLE_MESSAGE_WIDTH_282, charset));
            }
            out.encodeShort(friends.size());
            for (DataPair r : friends) {
                out.encodeInt(r.pairCharacterId());
                out.encodeArr(fixed282(r.pairName(), NAME_WIDTH_282, charset));
                out.encodeLong(r.sn());
                out.encodeLong(r.pairSn());
                out.encodeInt(r.itemId());
            }
            out.encodeShort(marriage == null ? 0 : 1);
            if (marriage != null) {
                out.encodeInt(marriage.marriageNo());
                out.encodeInt(marriage.groomId());
                out.encodeInt(marriage.brideId());
                out.encodeShort(marriage.status());
                out.encodeInt(marriage.groomItemId());
                out.encodeInt(marriage.brideItemId());
                out.encodeArr(fixed282(marriage.groomName(), NAME_WIDTH_282, charset));
                out.encodeArr(fixed282(marriage.brideName(), NAME_WIDTH_282, charset));
            }
        }
    }

    /**
     * The worn rings of {@code chr} as other users see them (from the 265 Char fields {@link #apply} sets).
     * Never touches the database; a character without applied records gets {@link RemoteRings#NONE}.
     */
    public static RemoteRings remoteRings(Char chr) {
        if (chr == null) {
            return RemoteRings.NONE;
        }
        CoupleRecord c = chr.isInCouple() ? chr.getCouple() : null;
        FriendshipRingRecord f = chr.getFriendshipRingRecord();
        MarriageRecord m = chr.getMarriageRecord();
        return new RemoteRings(
                c == null ? null : new RemoteRing(c.getSn(), c.getPairSn(), c.getItemID(), ""),
                f == null ? null : new RemoteRing(f.getFriendshipItemSN(), f.getFriendshipPairItemSN(), f.getItemID(), ""),
                m == null ? null : new RemoteMarriage(m.getGroomId(), m.getBrideId(),
                        chr.getId() == m.getBrideId() ? m.getBrideItemId() : m.getGroomItemId()));
    }

    /**
     * All records of {@code chr} (worn or not) for the 282 CharacterData ring block, from the records already loaded
     * ({@link #onLogin} / {@link #createPair} / {@link #marry}); never loads from the database, so a character whose
     * records were not loaded gets {@link CharacterDataRings#NONE} (the pre-ring bytes).
     */
    public CharacterDataRings characterData282(Char chr) {
        State s = chr == null ? null : states.get(chr.getId());
        if (s == null) {
            return CharacterDataRings.NONE;
        }
        List<DataPair> couples = s.pairs().stream().filter(r -> r.kind() == RingKind.COUPLE)
                .map(r -> new DataPair(r.pairCharacterId(), r.pairName(), r.ringSn(), r.pairRingSn(), r.ringId(), ""))
                .toList();
        List<DataPair> friends = s.pairs().stream().filter(r -> r.kind() == RingKind.FRIENDSHIP)
                .map(r -> new DataPair(r.pairCharacterId(), r.pairName(), r.ringSn(), r.pairRingSn(), r.ringId(), ""))
                .toList();
        DataMarriage marriage = s.marriage().filter(x -> x.stage() == Marriage.MARRIED)
                .map(x -> new DataMarriage(x.marriageId(), x.groomId(), x.brideId(), MARRIED_STATUS_282,
                        x.groomRingId(), x.brideRingId(), x.groomName(), x.brideName()))
                .orElse(null);
        return new CharacterDataRings(couples, friends, marriage);
    }

    /** Client DecodeStr: u16 byte length + bytes. */
    static void str282(OutPacket out, String s, Charset charset) {
        byte[] b = (s == null ? "" : s).getBytes(charset);
        out.encodeShort(b.length);
        out.encodeArr(b);
    }

    /** A NUL-terminated fixed buffer: whole characters only, at most width - 1 bytes, zero-filled. */
    public static byte[] fixed282(String s, int width, Charset charset) {
        byte[] out = new byte[width];
        int n = 0;
        String text = s == null ? "" : s;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            byte[] b = new String(Character.toChars(cp)).getBytes(charset);
            if (n + b.length > width - 1) {
                break;
            }
            System.arraycopy(b, 0, out, n, b.length);
            n += b.length;
            i += Character.charCount(cp);
        }
        return out;
    }
}
