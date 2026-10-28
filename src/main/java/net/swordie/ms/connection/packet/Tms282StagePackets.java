package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.social.tms282.RingRecordService;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 CStage packets (LP02EF..LP02F3, dispatched by CStage::OnPacket 44B9420). Only LP02EF SetField
 * (OnSetField 44B9700) is built here. Evidence: research/tms2823/packet-audit/channel-enter-2823.md section 2.
 * RVAs in the comments are the client's Decode call sites. Tags: [verified] = read order and width seen in
 * readable code; [presumed] = width verified, meaning or harmless value presumed; values written as 0 are the
 * "empty/absent" form of each reader, not recovered semantics. Live acceptance has not been tested yet.
 * <p>
 * Differences from 265 Stage.setField that shift the stream: a u8 after the characterData byte (44B992C);
 * the logout-event block's 282 form plus four i32 read always (5E85DC0); CharacterData (92AD20) differs in its
 * header, Character, Money, InventorySize and item-slot blocks; 265's trailing custom-nickname block is not
 * read by 282 and is not sent.
 * <p>
 * First entry carries the character's items: worn equips, the equip/consume/install/etc/cash/decoration
 * inventories ({@link ItemSlotBlocks}) in the 282 item form ({@link Tms282ItemEncoder}). The Char overload reads
 * them from the character, so the caller must have loaded the inventories ({@code chr.loadCharacterData()}).
 */
public final class Tms282StagePackets {
    public static final int SET_FIELD = 0x02EF;
    /** 92ADC3 DecodeBuffer(100): one byte per 265 DBChar bit index; a non-zero byte enables that block. */
    public static final int CHARACTER_DATA_FLAG_BYTES = 100;
    /**
     * Blocks enabled on first entry: [0] Character, [1] Money, [2] ItemSlotEquip (type 1 and the worn equips),
     * [3] consume, [4] install, [5] etc, [6] cash (926F50 masks of types 2..5), [7] InventorySize and [44]
     * (926F50 type 6: worn cash equips and the decoration inventory).
     */
    public static final int[] FIRST_ENTRY_FLAGS = {0, 1, 2, 3, 4, 5, 6, 7, 44};
    /** Worn items of the first [2]/[44] list: 92B9B1 / 92BD9B accept pos 1..0x27 (282 has 40 body slots). */
    public static final int BODY_SLOT_LIMIT = 39;
    /** 265 stores worn cash equips at body part + 100 (BodyPart.CBPBase 101); the [44] list sends the body part. */
    public static final int CASH_BODY_OFFSET = 100;
    /**
     * 937880(type) reads one u16-terminated list for every i in 0..30 whose 79FFC0(i) equals (type is 5 or 6):
     * type 1 (under [2]) i = 0,1,4,6,7,8,9,13,14,15,28,29,30 and type 6 (under [44]) the other 18 (byte table
     * 79FFF8, jump table 79FFF0: 79FFE6 returns 1, 79FFEC returns 0). List i accepts pos in [base, end) from the
     * dword tables 82058E0 / 8205960 (compared at 937A20 / 937A2E); a pos outside is decoded and dropped.
     */
    static final List<ExtraList> EXTRA_LISTS = ExtraList.table(
            new int[]{1000, 1100, 1200, 1300, 1400, 1500, 5100, 1600, 1700, 1750, 1800, 1900, 2000, 3000, 3100, 3200,
                    4000, 4200, 4400, 4600, 4700, 4800, 7000, 7100, 7200, 8000, 8100, 8200, 5200, 5250, 5000},
            new int[]{1004, 1105, 1214, 1310, 1425, 1512, 5106, 1606, 1706, 1753, 1851, 1930, 2036, 3040, 3140, 3240,
                    4140, 4340, 4540, 4601, 4701, 4801, 7012, 7112, 7212, 8010, 8110, 8210, 5201, 5251, 5003},
            Set.of(2, 3, 5, 10, 11, 12, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27));
    /** Lists read by 937880(1) under [2]. */
    public static final int EQUIP_EXTRA_LIST_COUNT = 13;
    /** Lists read by 937880(6) under [44]. */
    public static final int CASH_EXTRA_LIST_COUNT = 18;
    /** 8B90B0 (under [2]) accepts pos in [20000, 20024) (dwords read at 8B912E / 8B913C; 265 BodyPart.VEIBase). */
    public static final int VEI_BASE = 20000;
    public static final int VEI_END = 20024;
    /** Map used when a character's saved field is missing from the server data (exists in 282 and 265). */
    public static final int FALLBACK_FIELD_ID = 100000000;

    private Tms282StagePackets() {
    }

    /** Field VR size, sent as width/height (44B9908/44B9914); 265 sends |vrRight-vrLeft|, |vrTop-vrBottom|. */
    public record FieldSize(int width, int height) {
        public FieldSize {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("Field size must not be negative");
            }
        }

        public static FieldSize of(Field field) {
            Objects.requireNonNull(field, "field");
            return new FieldSize(field.getWidth(), field.getHeight());
        }
    }

    /** Slot counts per inventory type 1..6 (equip, consume, install, etc, cash, decoration), each a u16. */
    public record InventorySlots(int equip, int consume, int install, int etc, int cash, int decoration) {
        public InventorySlots {
            for (int slots : new int[]{equip, consume, install, etc, cash, decoration}) {
                if (slots < 0 || slots > 0xFFFF) {
                    throw new IllegalArgumentException("Inventory slot count must fit u16");
                }
            }
        }

        /** From the character's inventories; 265 getters create the default inventories when none were loaded. */
        public static InventorySlots of(Char chr) {
            Objects.requireNonNull(chr, "chr");
            return new InventorySlots(chr.getEquipInventory().getSlots(), chr.getConsumeInventory().getSlots(),
                    chr.getInstallInventory().getSlots(), chr.getEtcInventory().getSlots(),
                    chr.getCashInventory().getSlots(), chr.getDecorationInventory().getSlots());
        }

        int[] inTypeOrder() {
            return new int[]{equip, consume, install, etc, cash, decoration};
        }
    }

    /** The three damage random seeds (44B9B54/B61/B6D -> 7B0310, 5E34910). The server must keep them. */
    public record DamageSeeds(int s1, int s2, int s3) {
        public static DamageSeeds random(Random random) {
            return new DamageSeeds(random.nextInt(), random.nextInt(), random.nextInt());
        }
    }

    /**
     * The 265 inventories whose items the first-entry CharacterData carries (lists are copied; items are not).
     * {@link #of(Char)} reads them from the character; which 282 list each item goes to is decided when encoding
     * (see {@link ItemSlotBlocks}).
     */
    public record Inventories(List<Item> equipped, List<Item> equip, List<Item> consume, List<Item> install,
                              List<Item> etc, List<Item> cash, List<Item> decoration) {
        public static final Inventories EMPTY = new Inventories(List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());

        public Inventories {
            equipped = List.copyOf(equipped);
            equip = List.copyOf(equip);
            consume = List.copyOf(consume);
            install = List.copyOf(install);
            etc = List.copyOf(etc);
            cash = List.copyOf(cash);
            decoration = List.copyOf(decoration);
        }

        /**
         * The character's seven inventories. They must be loaded before: the channel loads a character with
         * {@code Char.getCharAvatarDataByID(id)} (stat and look only) and then {@code chr.loadCharacterData()},
         * which sets all seven from {@code Inventory.getAllInventoriesFromSQLByCharID}. A character whose
         * inventories were never loaded gets the empty default inventories of the 265 getters (no items sent).
         */
        public static Inventories of(Char chr) {
            Objects.requireNonNull(chr, "chr");
            return new Inventories(chr.getEquippedInventory().getItems(), chr.getEquipInventory().getItems(),
                    chr.getConsumeInventory().getItems(), chr.getInstallInventory().getItems(),
                    chr.getEtcInventory().getItems(), chr.getCashInventory().getItems(),
                    chr.getDecorationInventory().getItems());
        }

        /** Items no 282 list can hold with these slot counts; they are left out of the packet (log them). */
        public List<Item> unsent(InventorySlots slots) {
            return new ItemSlotBlocks(this, slots).unsent();
        }
    }

    /**
     * First entry after migrate-in, CharacterData from the character's stat and inventories. The caller loads
     * the inventories first (see {@link Inventories#of(Char)}: {@code chr.loadCharacterData()}).
     */
    public static OutPacket setFieldFirstEntry(Char chr, int channelId, FieldSize size, DamageSeeds seeds,
                                               Charset charset) {
        Objects.requireNonNull(chr, "chr");
        CharacterStat stat = Objects.requireNonNull(chr.getAvatarData(), "avatar data").getCharacterStat();
        return setFieldFirstEntry(stat, InventorySlots.of(chr), Inventories.of(chr), Tms282SkillRecordBlock.of(chr),
                Tms282CollectionRecordBlock.of(chr), Tms282InnerAbilityBlock.of(chr),
                RingRecordService.get().characterData282(chr), channelId, size, seeds, FileTime.currentTime(), charset);
    }

    /** {@link #setFieldFirstEntry(CharacterStat, InventorySlots, Inventories, int, FieldSize, DamageSeeds, FileTime,
     * Charset)} with every item list empty. */
    public static OutPacket setFieldFirstEntry(CharacterStat stat, InventorySlots slots, int channelId,
                                               FieldSize size, DamageSeeds seeds, FileTime now, Charset charset) {
        return setFieldFirstEntry(stat, slots, Inventories.EMPTY, channelId, size, seeds, now, charset);
    }

    /**
     * LP02EF with characterData=1 (first entry). The spawn map and portal are the stat's posMap/portal, which
     * CharacterStat carries (931E40 at 093227F/0932295). Items are sent in the 282 item form
     * ({@link Tms282ItemEncoder}); see {@link Inventories#unsent} for the ones 282 cannot hold.
     */
    public static OutPacket setFieldFirstEntry(CharacterStat stat, InventorySlots slots, Inventories items,
                                               int channelId, FieldSize size, DamageSeeds seeds, FileTime now,
                                               Charset charset) {
        return setFieldFirstEntry(stat, slots, items, Tms282SkillRecordBlock.EMPTY, channelId, size, seeds, now, charset);
    }

    /**
     * As above with the character's skill records: a non-empty {@code skills} enables CharacterData block [8]
     * ({@link Tms282SkillRecordBlock}), so the client's skill window shows the learned skills after a login.
     */
    public static OutPacket setFieldFirstEntry(CharacterStat stat, InventorySlots slots, Inventories items,
                                               Tms282SkillRecordBlock skills, int channelId, FieldSize size,
                                               DamageSeeds seeds, FileTime now, Charset charset) {
        return setFieldFirstEntry(stat, slots, items, skills, Tms282CollectionRecordBlock.EMPTY, channelId, size, seeds,
                now, charset);
    }

    /**
     * As above with the account's Monster Collection records: a non-empty {@code collection} enables CharacterData
     * block [47] ({@link Tms282CollectionRecordBlock}), so the collection window keeps its registered mobs after a login.
     */
    public static OutPacket setFieldFirstEntry(CharacterStat stat, InventorySlots slots, Inventories items,
                                               Tms282SkillRecordBlock skills, Tms282CollectionRecordBlock collection,
                                               int channelId, FieldSize size, DamageSeeds seeds, FileTime now,
                                               Charset charset) {
        return setFieldFirstEntry(stat, slots, items, skills, collection, Tms282InnerAbilityBlock.NONE, channelId, size,
                seeds, now, charset);
    }

    /**
     * As above with the inner abilities and honor ({@link Tms282InnerAbilityBlock}, CharacterData [28] / [29]), so the
     * 282 內在潛能 window shows the character's lines and 名聲值 after a login.
     */
    public static OutPacket setFieldFirstEntry(CharacterStat stat, InventorySlots slots, Inventories items,
                                               Tms282SkillRecordBlock skills, Tms282CollectionRecordBlock collection,
                                               Tms282InnerAbilityBlock inner, int channelId, FieldSize size,
                                               DamageSeeds seeds, FileTime now, Charset charset) {
        return setFieldFirstEntry(stat, slots, items, skills, collection, inner,
                RingRecordService.CharacterDataRings.NONE, channelId, size, seeds, now, charset);
    }

    /**
     * As above with the couple / friendship / marriage ring records: a non-empty {@code rings} enables CharacterData
     * block [11] ({@link RingRecordService.CharacterDataRings}, research/tms2823/ring-records), so the client knows
     * its own rings (tooltip partner name, couple effect pairing with other users).
     */
    public static OutPacket setFieldFirstEntry(CharacterStat stat, InventorySlots slots, Inventories items,
                                               Tms282SkillRecordBlock skills, Tms282CollectionRecordBlock collection,
                                               Tms282InnerAbilityBlock inner, RingRecordService.CharacterDataRings rings,
                                               int channelId, FieldSize size, DamageSeeds seeds, FileTime now,
                                               Charset charset) {
        Objects.requireNonNull(rings, "rings");
        Objects.requireNonNull(inner, "inner");
        Objects.requireNonNull(skills, "skills");
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(seeds, "seeds");
        Objects.requireNonNull(charset, "charset");
        OutPacket out = header(channelId, size, now, true);
        // characterData != 0 branch, 44B9B54..44B9CD1.
        out.encodeInt(seeds.s1());                         // 44B9B54 [verified]
        out.encodeInt(seeds.s2());                         // 44B9B61 [verified]
        out.encodeInt(seeds.s3());                         // 44B9B6D [verified]
        encodeCharacterData(out, stat, slots, new ItemSlotBlocks(items, slots), skills, collection, inner, rings, charset); // 44B9C09 -> 92AD20(cd, flags, pkt, 0, 0)
        out.encodeByte(0);                                 // 44B9C11 logout-event b1=0 [verified]; b1=1 would add
                                                           // u8 b2 [i32], raw8, u8, raw8 (44B9C21..44B9C73)
        for (int i = 0; i < 4; i++) {
            out.encodeInt(0);                              // 44B9CB7 -> 5E85DD3..5E85DFD, read whatever b1 is [verified]
        }
        encodeCommonTail(out, JobConstants.isExtendSpJob(stat.getJob()));
        return out;
    }

    /**
     * LP02EF with characterData=0: a map change for a character the client already holds (portal, warp).
     * Branch 44B9CD6..44B9E06, identical in order to 265 Stage.setField's else branch.
     *
     * @param portal target portal index in the new map; the client spawns there unless spawn is given
     * @param hp     current HP, stored by the client (44B9D92)
     * @param spawn  optional explicit position (u8 hasPos + i32 x + i32 y); null to use the portal
     */
    public static OutPacket setFieldChangeField(int channelId, FieldSize size, int fieldId, int portal, int hp,
                                                Position spawn, boolean usingBuffProtector, boolean extendSpJob,
                                                FileTime now) {
        return setFieldChangeField(channelId,size,fieldId,portal,hp,spawn,usingBuffProtector,extendSpJob,now,0);
    }

    /** Native template override: CharacterData+23E -> FieldInitInfo+30 -> CField WZ lookup.
     * The main fieldId still selects the field subclass. Caller must authorize a private housing lease.
     */
    public static OutPacket setFieldChangeField(int channelId, FieldSize size, int fieldId, int portal, int hp,
                                                Position spawn, boolean usingBuffProtector, boolean extendSpJob,
                                                FileTime now, int templateMapId) {
        if (templateMapId < 0) throw new IllegalArgumentException("Negative field template ID");
        if (portal < 0 || portal > 0xFF) {
            throw new IllegalArgumentException("Portal index must fit one byte");
        }
        OutPacket out = header(channelId, size, now, false);
        out.encodeByte(usingBuffProtector);                // 44B9CD6 [verified] buff protector
        out.encodeInt(fieldId);                            // 44B9D65 -> 4FE9F0 [verified] map ID
        out.encodeByte(portal);                            // 44B9D7E -> +0x101 [verified] portal
        out.encodeInt(hp);                                 // 44B9D92 [verified] HP
        out.encodeInt(templateMapId);                      // 44B9DC7 -> +0x23E -> 44BA324 -> 371C268 [verified]
        out.encodeByte(spawn != null);                     // 44B9DD5 [verified]
        if (spawn != null) {
            out.encodeInt(spawn.getX());                   // 44B9DEB
            out.encodeInt(spawn.getY());                   // 44B9DFA
        }
        encodeCommonTail(out, extendSpJob);
        return out;
    }

    /** 44B974C..44B9938: the part both branches share before the characterData switch. */
    private static OutPacket header(int channelId, FieldSize size, FileTime now, boolean characterData) {
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(now, "now");
        OutPacket out = Tms282LoginPacketSupport.packet(SET_FIELD);
        out.encodeFT(now);                                 // 44B974C -> 1378BE0 [verified]
        out.encodeByte(0);                                 // 44B97E7 unk -> ctx+4CF4 [verified]; 0 also skips T24
        out.encodeInt(channelId);                          // 44B981F -> 5DF23F0 [verified] zero-based wire channel
        out.encodeByte(0);                                 // 44B9835 -> 5DF2420 [verified] dev
        out.encodeInt(0);                                  // 44B9848 -> 5E23F20 [verified] old driver ID
        out.encodeByte(characterData ? 1 : 2);             // 44B98E8 [verified] 1 calls 6001EB0; 265 sends 1/2
        out.encodeInt(0);                                  // 44B9900 [verified] read and discarded (265 field type)
        out.encodeInt(size.width());                       // 44B9908 [verified]
        out.encodeInt(size.height());                      // 44B9914 [verified]
        out.encodeByte(characterData);                     // 44B9920 [verified] characterData
        out.encodeByte(0);                                 // 44B992C [verified] 282-only; 0 = full 92AD20 decode,
                                                           // non-zero = compact 927B40
        out.encodeShort(0);                                // 44B9938 [verified] notifier count; strings only if n>0
        return out;
    }

    /**
     * CharacterData::Decode 92AD20 with r9d=0 (first entry) and flags FIRST_ENTRY_FLAGS. The reads reachable
     * with exactly these flags were enumerated over the whole function (control flow with every flag test
     * resolved, including the inlined AND-mask tests at 92C520..9313B0 and the 926F50 per-type masks
     * type1=[2] type2=[3] type3=[4] type4=[5] type5=[6] type6=[44]); this is that list, in stream order.
     * The mask at 931602 is copied from static data (9A7F180 = [5]), so [5] also enables 8F9D90. The emulated
     * client (research/tms2823/setfield-trace, expected/builder-items*.tsv) consumes exactly these bytes, with
     * and without items (tools/Tms282SetFieldCheck.java).
     */
    static void encodeCharacterData(OutPacket out, CharacterStat stat, InventorySlots slots, ItemSlotBlocks items,
                                    Charset charset) {
        encodeCharacterData(out, stat, slots, items, Tms282SkillRecordBlock.EMPTY, charset);
    }

    /** With block [8] ({@link Tms282SkillRecordBlock}) when {@code skills} is not empty. */
    static void encodeCharacterData(OutPacket out, CharacterStat stat, InventorySlots slots, ItemSlotBlocks items,
                                    Tms282SkillRecordBlock skills, Charset charset) {
        encodeCharacterData(out, stat, slots, items, skills, Tms282CollectionRecordBlock.EMPTY, charset);
    }

    /** With block [47] ({@link Tms282CollectionRecordBlock}) when {@code collection} is not empty. */
    static void encodeCharacterData(OutPacket out, CharacterStat stat, InventorySlots slots, ItemSlotBlocks items,
                                    Tms282SkillRecordBlock skills, Tms282CollectionRecordBlock collection,
                                    Charset charset) {
        encodeCharacterData(out, stat, slots, items, skills, collection, Tms282InnerAbilityBlock.NONE, charset);
    }

    /** With blocks [28] / [29] ({@link Tms282InnerAbilityBlock}) when {@code inner} is present. */
    static void encodeCharacterData(OutPacket out, CharacterStat stat, InventorySlots slots, ItemSlotBlocks items,
                                    Tms282SkillRecordBlock skills, Tms282CollectionRecordBlock collection,
                                    Tms282InnerAbilityBlock inner, Charset charset) {
        encodeCharacterData(out, stat, slots, items, skills, collection, inner, RingRecordService.CharacterDataRings.NONE,
                charset);
    }

    /** With block [11] ({@link RingRecordService.CharacterDataRings}) when {@code rings} is not empty. */
    static void encodeCharacterData(OutPacket out, CharacterStat stat, InventorySlots slots, ItemSlotBlocks items,
                                    Tms282SkillRecordBlock skills, Tms282CollectionRecordBlock collection,
                                    Tms282InnerAbilityBlock inner, RingRecordService.CharacterDataRings rings,
                                    Charset charset) {
        byte[] flags = new byte[CHARACTER_DATA_FLAG_BYTES];
        if (!rings.isEmpty()) {
            flags[RingRecordService.CHARACTER_DATA_FLAG_282] = 1;
        }
        if (inner.present()) {
            flags[Tms282InnerAbilityBlock.ABILITY_FLAG] = 1;
            flags[Tms282InnerAbilityBlock.HONOR_FLAG] = 1;
        }
        if (!collection.isEmpty()) {
            flags[Tms282CollectionRecordBlock.FLAG] = 1;
        }
        for (int flag : FIRST_ENTRY_FLAGS) {
            flags[flag] = 1;
        }
        if (!skills.isEmpty()) {
            flags[Tms282SkillRecordBlock.FLAG] = 1;
        }
        out.encodeArr(flags);                              // 92ADC3 H1 [verified]
        out.encodeByte(0);                                 // 92ADD4 H2 combatOrders -> +8E8 [verified]
        for (int i = 0; i < 3; i++) {
            out.encodeInt(0);                              // 92ADE6/92AE0B/92AE30 H3 pet skill cooldowns; <=0 means none [verified]
        }
        out.encodeByte(0);                                 // 92AE61 H4 u8 n + n*i32 [verified]
        out.encodeInt(0);                                  // 92AE96 H5 i32 n + n*(i32, raw8) [verified]
        out.encodeByte(0);                                 // 92AEF1 H6 u8; non-zero adds two i32-counted raw8 lists [verified]
        // [0] Character.
        Tms282AvatarEncoder.encodeCharacterStat(out, stat, charset); // 92AFBB -> 931E40(r8d=0) C1 [verified width]
        out.encodeByte(0);                                 // 92AFC3 C2 u8 -> str +C9F only if non-zero [verified]
        out.encodeByte(0);                                 // 92AFFB C2 u8 -> str +CA7 [verified]
        out.encodeByte(0);                                 // 92B033 C2 u8 -> str +CAF [verified]
        out.encodeArr(new byte[8]);                        // 92B078 C3 raw8 -> +119A [verified width, meaning unknown]
        // 8D7830 C4: i32, (u8 slot, i32 item)* until slot 0xFF, i32, (u8, i32)* until 0xFF [verified]
        out.encodeInt(0);                                  // 8D7851 -> +123E
        out.encodeByte(0xFF);                              // 8D7894 list terminator
        out.encodeInt(0);                                  // 8D78F5 -> +1242
        out.encodeByte(0xFF);                              // 8D7951 list terminator
        // [62] [66] [67] off.
        // [1] Money.
        out.encodeByte(0);                                 // 92B137 M1 u8 -> +EF3 (bool) [verified width, meaning unknown]
        out.encodeLong(stat.getMoney());                   // 92B14B M1 i64 money -> +EF4 [verified]
        // [3] (tested with [22] at 92B168/92B18A): 92B1AA i32 n + n*(i32 92B1C1, i32 92B1CC, i32 92B1D7,
        // i64 92B1E2) [verified width]; 265 has two lists here (ExpConsumeItem, ShopBuyLimit). 0 = none [presumed].
        out.encodeInt(0);
        // [44] 92B287 and [2] 92B534 only merge items already held; r9d=0 skips 92B287..92B63F (no read).
        // [7] InventorySize: 92B696 u16 per type 1..6 (loop 92B663..92B7DD) [verified]. Read before the item lists,
        // whose positions the client checks against these counts (937669..937678).
        for (int count : slots.inTypeOrder()) {
            out.encodeShort(count);
        }
        // [64] off.
        items.encodeEquipBlock(out, charset);              // [2] 92B8EA..92BA8A
        items.encodeCashBlock(out, charset);               // [44] 92BCBA..92BF2E
        items.encodeBags(out, charset);                    // [3]..[6] 92BF38..92BF77, then 92C062..92C183
        // [75] [21] [27] off.
        if (!skills.isEmpty()) {
            skills.encode(out);                            // [8] 92C541..92CAEB skill records (skills phase 2, traced)
        }
        // [15] off.
        // [0] again, inlined mask test 92D2A0 (not listed in channel-enter-2823.md 2.3):
        out.encodeArr(new byte[54]);                       // 92D2C1 -> 8FA650: 6*i32, 6*u8, 6*i32 [verified width]
        // 92D2CB -> CharacterData reference +10D4. -1 selects normal skills; 0 activates an empty override
        // table in GetSkillLevelEffective (10FBFE0), making learned skills display/use level 0.
        out.encodeInt(-1);
        // [9] [10] [73] [71] [74] [72] [82] [14] off.
        if (!rings.isEmpty()) {
            rings.encode282(out, charset);                 // [11] 92DA97..92DE70 ring records (ring-records trace)
        }
        // [12] [16] [17] off.
        out.encodeByte(0);                                 // 92E9AA ungated u8 -> +E07; 0 skips the [78] block [verified]
        out.encodeByte(0);                                 // 92EA5D ungated u8 -> +DD3; 0 skips the [37] block [verified]
        // [41] [40] [23] [52] [68] [69] [70] [26] [25] off.
        if (inner.present()) {
            inner.encodeAbilities(out);                    // [28] 92F1C3 inner abilities, 3 presets (innerability-282)
        }
        // [45] off.
        if (inner.present()) {
            inner.encodeHonor(out);                        // [29] 92F376 honor level, honor EXP
        }
        // [84] [85] [86] [30] [31] [32] [87] [61] [36] [53] [39] [46] [48] off.
        if (!collection.isEmpty()) {
            collection.encode(out, charset);               // [47] 930861..9308B6 collection records (monster collection phase)
        }
        // [19] [65] off.
        out.encodeInt(0);                                  // 930A7D -> 8D3B90 ungated: i32 n + n*(i32, str) [verified]
        // [49] [50] [59] [60] [63] [51] off.
        // [5] (mask 9A7F180, static data byte 5, copied by 5661F0 at 9315EA; tested at 931602):
        out.encodeInt(0);                                  // 931622 -> 8F9D90(cd+0x8B8): 8F9DBA i32 n; <=0 reads nothing [verified]
        // [54] [55] [57] [56] [58] [76] [77] [79] [80] [88] [89] [90] off: no further read reaches the packet.
    }

    /** One u16-terminated item list: (u16 pos, 932790 item)* then u16 0. */
    private static void encodeItemList(OutPacket out, List<Placed> items, Charset charset) {
        for (Placed placed : items) {
            out.encodeShort(placed.pos());
            Tms282ItemEncoder.encode(out, placed.item(), charset);
        }
        out.encodeShort(0);
    }

    /** An item with the u16 position its 282 list sends (never 0: 0 ends a list). */
    record Placed(int pos, Item item) {
        Placed {
            if (pos <= 0 || pos > 0xFFFF) {
                throw new IllegalArgumentException("Item list position must be 1..65535");
            }
            Objects.requireNonNull(item, "item");
        }
    }

    /** One 937880 list: index i in the client tables, accepted positions [base, end), read under [2] or [44]. */
    record ExtraList(int index, int base, int end, boolean cashBlock) {
        static List<ExtraList> table(int[] base, int[] end, Set<Integer> cashBlock) {
            return IntStream.range(0, base.length)
                    .mapToObj(i -> new ExtraList(i, base[i], end[i], cashBlock.contains(i))).toList();
        }

        boolean holds(int pos) {
            return pos >= base && pos < end;
        }
    }

    /**
     * The item blocks of CharacterData 92AD20 for one character: which 282 list each 265 item goes to, and their
     * encoding. Positions are the 265 bag indexes (positive, as 265 stores them), except worn cash equips, which
     * 265 keeps at body part + 100.
     * <ul>
     * <li>[2]: u8 0 (92B902; 0 = the equip inventory follows), worn equips 1..39 that are not cash (92B981, 9343B0;
     * 92B9B1 drops pos outside 1..0x27), the equip inventory 1..slots (937590(1)), the 13 type-1 extra lists
     * (937880(1)), then 8B90B0 (20000..20023).</li>
     * <li>[44]: u8 0 (92BCDC), worn cash equips 101..139 sent as 1..39 (92BD64, 92BD9B drops the rest), the
     * decoration inventory (937590(6)), the 18 type-6 extra lists (937880(6)).</li>
     * <li>[3]..[6]: consume, install, etc, cash inventories 1..slots (937590(2..5), each u16 pos until 0), then
     * for types 2..4 the bag contents 92C0AD: i32 n + n*(i32 pos 92C0C3, 8F1450 item); 0 = no bag.</li>
     * </ul>
     * Everything else (a worn non-cash equip at 40..100 such as 265's medal 49 or monster book 55, a bag position
     * of 0 or beyond the slot count) is decoded and dropped by the client or would end a list early, so it is not
     * sent and is reported by {@link #unsent()}.
     */
    static final class ItemSlotBlocks {
        private final List<Placed> worn = new ArrayList<>();
        private final List<Placed> wornCash = new ArrayList<>();
        private final List<List<Placed>> extra = new ArrayList<>();
        private final List<Placed> vei = new ArrayList<>();
        private final List<Placed> equip;
        private final List<Placed> decoration;
        /** Types 2..5 (consume, install, etc, cash), in the order 92BF38..92BF77 reads them. */
        private final List<List<Placed>> bags;
        private final List<Item> unsent = new ArrayList<>();

        ItemSlotBlocks(Inventories items, InventorySlots slots) {
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(slots, "slots");
            for (int i = 0; i < EXTRA_LISTS.size(); i++) {
                extra.add(new ArrayList<>());
            }
            for (Item item : sorted(items.equipped())) {
                placeWorn(item);
            }
            equip = inBag(items.equip(), slots.equip());
            decoration = inBag(items.decoration(), slots.decoration());
            bags = List.of(inBag(items.consume(), slots.consume()), inBag(items.install(), slots.install()),
                    inBag(items.etc(), slots.etc()), inBag(items.cash(), slots.cash()));
        }

        private void placeWorn(Item item) {
            int pos = Tms282BodySlots.wornToClient(item.getBagIndex()); // android 53 -> 27, heart 54 -> 28
            if (pos >= 1 && pos <= BODY_SLOT_LIMIT && !item.isCash()) {
                worn.add(new Placed(pos, item));
                return;
            }
            if (pos > CASH_BODY_OFFSET && pos <= CASH_BODY_OFFSET + BODY_SLOT_LIMIT) {
                wornCash.add(new Placed(pos - CASH_BODY_OFFSET, item));
                return;
            }
            if (pos >= VEI_BASE && pos < VEI_END) {
                vei.add(new Placed(pos, item));
                return;
            }
            for (ExtraList list : EXTRA_LISTS) {
                if (list.holds(pos)) {
                    extra.get(list.index()).add(new Placed(pos, item));
                    return;
                }
            }
            unsent.add(item);
        }

        private List<Placed> inBag(List<Item> items, int slotCount) {
            List<Placed> placed = new ArrayList<>();
            for (Item item : sorted(items)) {
                int pos = item.getBagIndex();
                if (pos >= 1 && pos <= slotCount) {
                    placed.add(new Placed(pos, item));
                } else {
                    unsent.add(item);
                }
            }
            return placed;
        }

        private static List<Item> sorted(List<Item> items) {
            return items.stream().sorted(Comparator.comparingInt(Item::getBagIndex)).toList();
        }

        List<Item> unsent() {
            return List.copyOf(unsent);
        }

        /** [2] ItemSlotEquip. */
        void encodeEquipBlock(OutPacket out, Charset charset) {
            out.encodeByte(0);                             // 92B902 u8; 0 = 937590(1) follows [verified]
            encodeItemList(out, worn, charset);            // 92B981/92BA3C u16 pos (1..0x27), 92B9A6 9343B0 [verified]
            encodeItemList(out, equip, charset);           // 92BA61 -> 937590(1): 93762D/937836 u16, 93765D 9343B0 [verified]
            encodeExtraLists(out, false, charset);         // 92BA72 -> 937880(1): 9379E0 u16, 937A0D 9343B0, 13 lists [verified]
            encodeItemList(out, vei, charset);             // 92BA8A -> 8B90B0: 8B910D u16, 8B9128 932790 [verified]
        }

        /** [44]: worn cash equips and the decoration inventory (926F50 type 6). */
        void encodeCashBlock(OutPacket out, Charset charset) {
            out.encodeByte(0);                             // 92BCDC u8; 0 = 937590(6) follows [verified]
            encodeItemList(out, wornCash, charset);        // 92BD64/92BEF8 u16 pos (1..0x27), 92BD8E 9343B0 [verified]
            encodeItemList(out, decoration, charset);      // 92BF1D -> 937590(6) [verified]
            encodeExtraLists(out, true, charset);          // 92BF2E -> 937880(6), 18 lists [verified]
        }

        /** [3]..[6]: types 2..5, then the bag contents of types 2..4. */
        void encodeBags(OutPacket out, Charset charset) {
            for (List<Placed> bag : bags) {
                encodeItemList(out, bag, charset);         // 92BF44/55/66/77 -> 937590(2..5) [verified]
            }
            for (int type = 2; type <= 4; type++) {
                out.encodeInt(0);                          // 92C0AD i32 n bags of this type; 0 = none [verified width]
            }
        }

        private void encodeExtraLists(OutPacket out, boolean cashBlock, Charset charset) {
            for (ExtraList list : EXTRA_LISTS) {
                if (list.cashBlock() == cashBlock) {
                    encodeItemList(out, extra.get(list.index()), charset);
                }
            }
        }
    }

    /** 44B9EF2..44BAB27: the part both branches share after it; no read follows T25. */
    private static void encodeCommonTail(OutPacket out, boolean extendSpJob) {
        out.encodeByte(0);                                 // 44B9EF5 T1 u8; non-zero adds i32, i32 [verified]
        out.encodeByte(2);                                 // 44B9FEC T2 flags (bit1, bit0 -> 5E04710) [presumed] 265 sends 2
        out.encodeByte(0);                                 // 44BA01E T3 [verified]
        out.encodeInt(100);                                // 44BA1FD T4 mob stat adjust rate [presumed] 265 sends 100
        out.encodeByte(0);                                 // 44BA208 T5 FieldCustom absent (3830A30 not called) [verified]
        out.encodeByte(0);                                 // 44BA30B T6 PvP [verified]
        out.encodeInt(0);                                  // 44BA319 T6 [verified]
        out.encodeByte(0);                                 // 44BA330 T7; non-zero calls 5EFFD60 [verified]
        out.encodeInt(0);                                  // 44BA344 T8 buffer length for 28E3550 (ctx+49C0) [verified]
        out.encodeByte(extendSpJob);                       // 44BA35E T9 [presumed] 265 sends isExtendSpJob
        out.encodeLong(0);                                 // 44BA374 -> 63CC523 T10 [verified]
        out.encodeInt(-1);                                 // 63CC52E T10 [presumed] 265 sends -1
        // 44BA3AB -> 6FDDF00: reflection-deserializes WZ2UserCommonData (class A0640E0) into ctx+0x5768 through a
        // CInPacket archive (vtable 820C280, +0x30 = DecodeBuffer). Its only field "Flags" is a raw uint8, read with
        // DecodeBuffer(1) at 6FDDFB4 [verified from the class/type registration code; the descriptor is filled by
        // static initializers, so the first emulated traces skipped it]. Without this byte every later field shifts
        // and T25 underflows (live 2026-09-23, session channel-1790140955491: CP00AE type 1, code 0x26).
        out.encodeByte(0);                                 // WZ2UserCommonData.Flags [presumed 0 = none]
        out.encodeByte(0);                                 // 44BA5E6 T11 stack event gauge absent [verified]
        // T12 44BA66F: only in a Vonbon field (EAF820); not used by any map this builder serves.
        out.encodeInt(0);                                  // 44BA82E -> 5696B87 T13 i32 n + n*(i32, str) [verified]
        out.encodeByte(0);                                 // 44BA839 -> 56A0F90 -> 1332113 T14 u8 [verified]
        out.encodeInt(0);                                  // 133211D T14 i32, read whatever the u8 is [verified]
        out.encodeInt(0);                                  // 44BA844 -> 56A101C T15 best-friend AID [verified]
        out.encodeByte(0);                                 // 44BA84C T16 u8; non-zero adds i32*5 + str [verified]
        out.encodeInt(0);                                  // 44BA8CD -> 54C5BE8 T17 damage skin ID [verified width]
        str(out, "", Tms282LoginPacketSupport.DEFAULT_CHARSET); // 54C5BFB T17 [verified]
        str(out, "", Tms282LoginPacketSupport.DEFAULT_CHARSET); // 54C5C4E T17 [verified]
        // 44BA8D8 -> 5E61220 T18 SunnySunday: u8 0 when no entry is active today [verified]; the active layout
        // (u8 1, u8, 5 str, 2 i32) is read by 5E61284..5E613FA [verified static, setfield_trace builder-change-sunday].
        Tms282SunnySundayBlock.encode(out, Tms282SunnySundayBlock.current());
        out.encodeInt(0);                                  // 44BA8E0 T19 UI_OPEN [verified]
        out.encodeInt(0);                                  // 44BA991 T20 [verified]
        out.encodeByte(1);                                 // 44BA9C6 T21 -> ctx+4CF0 [presumed] 265 sends 1
        out.encodeInt(0);                                  // 44BA9D8 T22 -> +AD20 [verified]
        out.encodeInt(0);                                  // 44BAA11 -> C1B139 T23 4-byte count [verified]
        // T24 44BAA30: i32 only when the header unk byte is non-zero; it is 0 here.
        out.encodeInt(0);                                  // 44BAB27 -> 6C9C4E7 T25 i32 n + n*i32 [verified]
    }

    /** Random damage seeds from a SecureRandom, as 265 Stage.setField does. */
    public static DamageSeeds newDamageSeeds() {
        return DamageSeeds.random(new SecureRandom());
    }
}
