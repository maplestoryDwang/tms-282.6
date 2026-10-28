package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} for the 265 PARTY_RESULT packets ({@code WvsContext.partyResult}), sent as
 * LP00CF: the CWvsContext::OnPacket case 5DF5E4B calls CWvsContext::OnPartyResult 2FD5110 (Decode1 type, jump table
 * 2FD84A4 for types 4..88). 265 PARTY_RESULT is 0x00CE; 282 0x00CE is a different handler (inline case 5DF5E2C).
 * <p>
 * 282 numbers the result types differently from the 265 PartyType values; only the types below are translated, the
 * rest are declined (dropped by the guard as before). Each 282 reader was traced with the fixtures of
 * {@code tools/Tms282SocialCheck} (research/tms2823/social-trace):
 * <pre>
 * 265 type                      282 type  282 reader (2FD5110 case)            difference to 265
 * Request_Invite 4                  4     2FD52DA  I I B Str I I I            + u8 after the leader id
 * Request_Apply 8                   4     (same)                              265 sends its invitations as type 8
 * Response_Load_Success 29         30     2FD5DC0  B [PartyData] / B          PartyData (below)
 * Response_Create_Success 30       31     2FD5EEE  F99040 create block        see {@link #create}
 * Response_Leave_Success 33        35     2FD605E  I B [B Str PartyData]      PartyData
 * Response_Join_Success 35         37     2FD6394  Str B I PartyData          PartyData
 * Response_Join_Success_ForClient 36  38  2FD669B  B I Buf4                   none
 * Response_Invite_Success 46       47     2FD599A  Buf4 Str                   none ("邀請'%s'加入組隊。")
 * Response_ChangeLeader_Success 58 61     2FD698C  I B                        none ("%s成為隊伍的隊長！")
 * Response_UpdateDataMember 66     69     2FD6764  B I Buf4 value(F99460)     value 8 (look) is 480 bytes
 * Response_Setting_Success 69      72     2FD6A94  F98070 name block          + u8 after "appliable"
 * </pre>
 * PartyData (F98EA0): i32 id, u8, 6 x member (F98720), i32 leader, i32 n + n x (member, 8 bytes) (265: i32 0),
 * 24 bytes field ids, 120 bytes town portals, u8, name block F98070 (Str, u8, u8 [new], i32), recruitment F98380
 * (i32, -1 = none). A member (F98720) is the 265 member with one more u8 after the two 265 u8s and a 480-byte packed
 * look instead of 120 bytes; the 282 look format is not known, so it is sent zeroed [推定: blank avatar in the party
 * window]. The meaning of the new u8s is unknown; they are sent as 0.
 * <p>
 * Invitations: 265 PartyHandler sends the invitation to the invited character as {@code request_Apply} (type 8, the
 * same body as type 4). In 282, type 4 opens the invitation (answered with CP01F3 type 47: result 5 accept / 4 decline,
 * sites 3702424 / 3703230), type 5 an intrusion invitation (48), type 8 an application to the leader (49, 2FD5751);
 * so 265 type 8 is sent as 282 type 4 [推定 from the answer sites; the 265 application flow, which also uses type 8,
 * then shows an invitation too].
 * <p>
 * REMOTE_RECEIVE_HP (the party HP bar, 265 0x045F) goes out unchanged as LP0491 (CUserPool::OnUserRemotePacket,
 * 5ADBF20 reads i32 hp, i32 max hp after the dispatcher's character id; ingame-lp-map 證實, traced).
 * <p>
 * Strings are re-encoded from the 265 UTF-8 to the 282 charset; the 265 party name is a fixed 30-byte field
 * (u16 30 + bytes padded with NULs), the 282 name is a normal Str.
 */
public final class Tms282PartyTranslators implements Tms282LegacyPackets.Contributor {
    public static final int PARTY_RESULT = 0x00CF;
    /** REMOTE_RECEIVE_HP (265 0x045F): CUserPool::OnUserRemotePacket handler 5ADBF20, (id) + i32 hp, i32 max hp. */
    public static final int REMOTE_RECEIVE_HP = 0x0491;
    public static final int LOOK_282 = 0x1E0;
    public static final int LOOK_265 = 120;

    /** 265 PartyType value -> 282 result type, for the translated types. */
    public static final Map<Integer, Integer> TYPES = Map.ofEntries(Map.entry(4, 4), Map.entry(8, 4), Map.entry(29, 30),
            Map.entry(30, 31), Map.entry(33, 35), Map.entry(35, 37), Map.entry(36, 38), Map.entry(46, 47),
            Map.entry(58, 61), Map.entry(66, 69), Map.entry(69, 72));

    public static final Tms282PartyTranslators INSTANCE = new Tms282PartyTranslators();

    private Tms282PartyTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset charset = builder.charset();
        builder.on(OutHeader.PARTY_RESULT, p -> partyResult(p, charset))
                .on(OutHeader.REMOTE_RECEIVE_HP, Tms282PartyTranslators::receiveHp);
    }

    static Optional<OutPacket> partyResult(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int type = b.u8();
        Integer type282 = TYPES.get(type);
        if (type282 == null) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(PARTY_RESULT);
        out.encodeByte(type282);
        switch (type) {
            case 4, 8 -> invite(b, out, cs);
            case 29 -> {
                boolean has = b.u8() != 0;
                out.encodeByte(has ? 1 : 0);
                if (has) {
                    partyData(b, out, cs);
                } else {
                    out.encodeByte(b.u8());
                }
            }
            case 30 -> create(b, out, cs);
            case 33 -> {
                out.encodeInt(b.i32());
                boolean exists = b.u8() != 0;
                out.encodeByte(exists ? 1 : 0);
                if (exists) {
                    out.encodeByte(b.u8());
                    str(out, b.str(), cs);
                    partyData(b, out, cs);
                }
            }
            case 35 -> {
                str(out, b.str(), cs);
                out.encodeByte(b.u8());
                out.encodeInt(b.i32());
                partyData(b, out, cs);
            }
            case 36 -> {
                out.encodeByte(b.u8());
                out.encodeInt(b.i32());
                out.encodeInt(b.i32());
            }
            case 46 -> {
                out.encodeInt(b.i32());
                str(out, b.str(), cs);
            }
            case 58 -> {
                out.encodeInt(b.i32());
                out.encodeByte(b.u8());
            }
            case 66 -> updateMember(b, out, cs);
            case 69 -> nameBlock(b, out, cs);
            default -> throw new IllegalStateException("type " + type);
        }
        b.end();
        return Optional.of(out);
    }

    /** Party member HP bar: the 265 body (i32 id, i32 hp, i32 max hp) as is [opcode 證實 Δ=+50, layout traced]. */
    static Optional<OutPacket> receiveHp(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_RECEIVE_HP);
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        b.end();
        return Optional.of(out);
    }

    /** 265 i32 party, i32 leader, Str name, i32 level, i32 job, i32 sub job; 282 adds a u8 after the leader. */
    private static void invite(Tms282LegacyPackets.Body b, OutPacket out, Charset cs) {
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeByte(0);
        str(out, b.str(), cs);
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
    }

    /**
     * 265 create: i32 id, u8 0, town portal (i32 town, i32 field, i32 skill, i16 x, i16 y), member, name (30 bytes),
     * u8 appliable, i32 acquisition, i32 -1, i32 leader. 282 F99040: Buf4 id, Buf4 (sent 0), Buf20 town portal
     * (i32 x, i32 y), member, name block, recruitment i32, then the case reads Buf4 (the leader).
     */
    private static void create(Tms282LegacyPackets.Body b, OutPacket out, Charset cs) {
        out.encodeInt(b.i32());
        b.u8();
        out.encodeInt(0);
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeInt((int) (short) b.u16());
        out.encodeInt((int) (short) b.u16());
        member(b, out, cs);
        nameBlock(b, out, cs);
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
    }

    /** 265 Party.encode -> 282 PartyData F98EA0. */
    static void partyData(Tms282LegacyPackets.Body b, OutPacket out, Charset cs) {
        out.encodeInt(b.i32());
        out.encodeByte(b.u8());
        for (int i = 0; i < 6; i++) {
            member(b, out, cs);
        }
        out.encodeInt(b.i32()); // leader
        int extra = b.i32();
        if (extra != 0) {
            throw new IllegalArgumentException("265 party list count " + extra);
        }
        out.encodeInt(0);
        out.encodeArr(b.bytes(24)); // 6 field ids
        out.encodeArr(b.bytes(120)); // 6 town portals, 20 bytes each
        out.encodeByte(b.u8());
        nameBlock(b, out, cs);
        out.encodeInt(b.i32()); // boss party recruitment, -1
    }

    /** 265 u16 30 + 30-byte name, u8 appliable, i32 acquisition -> 282 Str, u8, u8 0, i32 (F98070). */
    private static void nameBlock(Tms282LegacyPackets.Body b, OutPacket out, Charset cs) {
        str(out, fixedName(b), cs);
        out.encodeByte(b.u8());
        out.encodeByte(0);
        out.encodeInt(b.i32());
    }

    /** 265 Party.encodeMembers entry -> 282 F98720. */
    static void member(Tms282LegacyPackets.Body b, OutPacket out, Charset cs) {
        int id = b.i32();
        out.encodeInt(id);
        if (id == 0) {
            return;
        }
        out.encodeInt(b.i32());
        str(out, b.str(), cs);
        for (int i = 0; i < 6; i++) { // job, sub job, level, world, online, channel
            out.encodeInt(b.i32());
        }
        out.encodeByte(b.u8());
        out.encodeByte(b.u8());
        out.encodeByte(0);
        out.encodeInt(b.i32()); // arcane
        out.encodeInt(b.i32()); // sacred
        out.encodeInt(b.i32()); // dojang rank
        out.encodeLong(b.i64());
        b.bytes(LOOK_265);
        out.encodeArr(new byte[LOOK_282]);
    }

    /** 265 u8 1, i32 character, i32 type, value (PartyMember.encodeVal) -> 282 u8, i32, Buf4 type, value (F99460). */
    private static void updateMember(Tms282LegacyPackets.Body b, OutPacket out, Charset cs) {
        out.encodeByte(b.u8());
        out.encodeInt(b.i32());
        int type = b.i32();
        out.encodeInt(type);
        switch (type) {
            case 0 -> str(out, b.str(), cs);
            case 1, 2, 3, 7 -> out.encodeInt(b.i32());
            case 4, 5 -> out.encodeByte(b.u8());
            case 6 -> {
                out.encodeInt(b.i32());
                out.encodeInt(b.i32());
                out.encodeInt(b.i32());
                out.encodeLong(b.i64());
            }
            case 8 -> {
                b.bytes(LOOK_265);
                out.encodeArr(new byte[LOOK_282]);
            }
            default -> throw new IllegalArgumentException("265 party member value type " + type);
        }
    }

    private static String fixedName(Tms282LegacyPackets.Body b) {
        String s = b.str();
        int nul = s.indexOf('\0');
        return nul < 0 ? s : s.substring(0, nul);
    }

    private static void str(OutPacket out, String s, Charset cs) {
        Tms282LoginPacketSupport.str(out, s, cs);
    }
}
