package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Second atoms (Adele's swords / shards, and the SecondAtom users of the other jobs) on TMS 282.3
 * (research/tms2823/second-atom/README.md). Without this contributor the 265 packets had no 282 opcode and the
 * OutboundGuard dropped every one of them (nothing was ever visible).
 * <p>
 * CUserPool::OnUserCommonPacket 5AA7B10 reads the i32 owner id, then range-dispatches 0x462..0x467 to 56954E0
 * (user + 0x88B8 = the owner's second atom manager) -> 303F580, jump table 303F8B0 (op - 0x462). Group size and
 * order equal 265 CUser::OnSecondAtomPacket 1061..1066: 282 op = 265 op + 61 (as pet / dragon / android / fox man /
 * skill pet). The dispatcher resolves any user of the field (local or remote), so every packet is a field broadcast.
 * <ul>
 *     <li>265 CREATE_INFINITY_BLADE_ATOM (1063, the list create every job uses: SecondAtomPacket.createSecondAtoms)
 *     -> <b>LP0464</b> (303FD20): i32 count, count x atom (105C5C0), i32 infinity flag. The 282 atom is the 265 atom
 *     plus <b>one trailing i32</b> (atom info +0x98; both client create paths pre-set it to -1, so -1 is sent):
 *     19 x i32 (objectId, 0, dataIndex, key, owner, target, createDelay, enableDelay, rotate, skillId,
 *     firstAngleRange, firstAngleStart, expire, angle, attackableCount, collisionCheck, 0, x, y), u8 localOnly,
 *     u8 hasExtra (265 always 0; non-zero would add i32 type and a type-1 object), u8 special, u8, u8,
 *     i32 n + n x i32 customs, i32 unk, i32, 8-byte buffer, u8, u8, <b>i32 (282 only)</b>.</li>
 *     <li>265 SECOND_ATOM_COMMAND_RESULT (1062) -> <b>LP0463</b> (3040600 -> 105E600): i32 count (<= 20), per entry
 *     i32 atomId, i32 n (<= 50), n x (i32 type, type body). This is the CP069C request after its two leading bytes;
 *     the 265 handler echoes the request after <b>one</b> byte, so the first byte of its array is dropped.</li>
 *     <li>265 SECOND_ATOM_ATTACK (1064) -> <b>LP0465</b> (inline 303F7D4): i32 objectId, i32 tier (= 265).</li>
 *     <li>265 SECOND_ATOM_COLLISION_RESULT (1065) -> <b>LP0466</b> (30408B0): i32 count, per entry i32 atomId,
 *     i32 mobId, i32 n, n x (i32, i32) (= 265).</li>
 *     <li>265 REMOVE_SECOND_ATOM (1066) -> <b>LP0467</b> (303FA20): i32 count, count x i32 objectId, i32, 4-byte
 *     buffer (= 265: 0, 6).</li>
 * </ul>
 * Not translated: 265 CREATE_SECOND_ATOM (1061 = LP0462, one atom without a count, 303F8D0) is never built by the
 * server. Emulated (daily_trace.py) against fixtures built from real 265 SecondAtomPacket bodies: all CLEAN, no AUX
 * read (tools/Tms282SecondAtomCheck). {@code tms282.secondatom.visible=false} (server.properties / -D) drops them
 * again.
 */
public final class Tms282SecondAtomTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282SecondAtomTranslators INSTANCE = new Tms282SecondAtomTranslators();

    public static final int CREATE_ONE = 0x0462;
    public static final int COMMAND_RESULT = 0x0463;
    public static final int CREATE = 0x0464;
    public static final int ATTACK = 0x0465;
    public static final int COLLISION_RESULT = 0x0466;
    public static final int REMOVE = 0x0467;

    /** The 282-only trailing i32 of an atom: the value both client create paths (303F950 / 303FECC) pre-set. */
    public static final int ATOM_TAIL = -1;
    /** 105E600: count limit of LP0463 entries and of the commands per entry. */
    public static final int MAX_COMMAND_ENTRIES = 20;
    public static final int MAX_COMMANDS = 50;

    public static final boolean VISIBLE = !"false".equalsIgnoreCase(property("tms282.secondatom.visible"));

    private Tms282SecondAtomTranslators() {
    }

    private static String property(String key) {
        String v = System.getProperty(key);
        if (v != null) {
            return v.trim();
        }
        try {
            String c = net.swordie.ms.console.config.ServerRuntimeConfig.stringValue(key);
            return c == null ? "" : c.trim();
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.CREATE_INFINITY_BLADE_ATOM, p -> VISIBLE ? create(p) : Optional.empty())
                .on(OutHeader.SECOND_ATOM_COMMAND_RESULT, p -> VISIBLE ? commandResult(p) : Optional.empty())
                .on(OutHeader.SECOND_ATOM_ATTACK, p -> VISIBLE ? attack(p) : Optional.empty())
                .on(OutHeader.SECOND_ATOM_COLLISION_RESULT, p -> VISIBLE ? collisionResult(p) : Optional.empty())
                .on(OutHeader.REMOVE_SECOND_ATOM, p -> VISIBLE ? remove(p) : Optional.empty());
    }

    /** 265 list create -> LP0464 (each atom + the 282 trailing i32). */
    static Optional<OutPacket> create(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(CREATE);
        out.encodeInt(b.i32()); // owner (read by 5AA7B10)
        int count = b.i32();
        if (count < 0) {
            throw new IllegalArgumentException("atom count " + count);
        }
        out.encodeInt(count);
        for (int i = 0; i < count; i++) {
            for (int k = 0; k < 19; k++) {
                out.encodeInt(b.i32());
            }
            out.encodeByte(b.u8()); // localOnly
            int extra = b.u8();
            if (extra != 0) {
                throw new IllegalArgumentException("265 atom with the extra object flag (not built by the server)");
            }
            out.encodeByte(0);
            out.encodeByte(b.u8()); // special
            out.encodeByte(b.u8());
            out.encodeByte(b.u8());
            int customs = b.i32();
            if (customs < 0) {
                throw new IllegalArgumentException("customs count " + customs);
            }
            out.encodeInt(customs);
            for (int k = 0; k < customs; k++) {
                out.encodeInt(b.i32());
            }
            out.encodeInt(b.i32()); // unk (-1)
            out.encodeInt(b.i32());
            out.encodeArr(b.bytes(8));
            out.encodeByte(b.u8());
            out.encodeByte(b.u8());
            out.encodeInt(ATOM_TAIL); // 282 only (105C9B9)
        }
        out.encodeInt(b.i32()); // infinity flag (105CBA0)
        b.end();
        return Optional.of(out);
    }

    /**
     * 265 command result: owner + the request array the 265 handler kept after skipping one byte. 282 LP0463 starts at
     * the entry count, which follows the <b>two</b> leading bytes of CP069C, so one more byte is dropped.
     */
    static Optional<OutPacket> commandResult(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int owner = b.i32();
        if (b.remaining() < 1 + 4) {
            throw new IllegalArgumentException("command result without entries");
        }
        b.u8();
        return Optional.of(commandResult(owner, b.rest()));
    }

    /** LP0463 from the CP069C entries (the request after its two leading bytes and without the tail u32). */
    public static OutPacket commandResult(int owner, byte[] entries) {
        if (entries.length < 4) {
            throw new IllegalArgumentException("command entries truncated");
        }
        int count = (entries[0] & 0xFF) | (entries[1] & 0xFF) << 8 | (entries[2] & 0xFF) << 16 | (entries[3] & 0xFF) << 24;
        if (count < 0 || count > MAX_COMMAND_ENTRIES) {
            throw new IllegalArgumentException("command entry count " + count);
        }
        OutPacket out = Tms282LoginPacketSupport.packet(COMMAND_RESULT);
        out.encodeInt(owner);
        out.encodeArr(entries);
        return out;
    }

    /** 265 attack -> LP0465: owner, i32 objectId (manager lookup key), i32 tier. */
    static Optional<OutPacket> attack(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(ATTACK);
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        b.end();
        return Optional.of(out);
    }

    /** 265 collision result -> LP0466 (checked, same layout). */
    static Optional<OutPacket> collisionResult(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(COLLISION_RESULT);
        out.encodeInt(b.i32()); // owner
        int count = b.i32();
        if (count < 0) {
            throw new IllegalArgumentException("collision count " + count);
        }
        out.encodeInt(count);
        for (int i = 0; i < count; i++) {
            out.encodeInt(b.i32()); // atom
            out.encodeInt(b.i32()); // mob
            int n = b.i32();
            if (n < 0) {
                throw new IllegalArgumentException("collision map size " + n);
            }
            out.encodeInt(n);
            for (int k = 0; k < n; k++) {
                out.encodeInt(b.i32());
                out.encodeInt(b.i32());
            }
        }
        b.end();
        return Optional.of(out);
    }

    /** 265 remove -> LP0467 (checked, same layout). */
    static Optional<OutPacket> remove(OutPacket legacy) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(REMOVE);
        out.encodeInt(b.i32()); // owner
        int count = b.i32();
        if (count < 0) {
            throw new IllegalArgumentException("remove count " + count);
        }
        out.encodeInt(count);
        for (int i = 0; i < count; i++) {
            out.encodeInt(b.i32());
        }
        out.encodeInt(b.i32());
        out.encodeArr(b.bytes(4));
        b.end();
        return Optional.of(out);
    }
}
