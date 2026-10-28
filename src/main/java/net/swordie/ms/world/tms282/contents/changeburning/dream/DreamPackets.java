package net.swordie.ms.world.tms282.contents.changeburning.dream;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 夢幻的裝備 packets (research/tms2823/lucid-re-0930/ability §3, adapted from its unapplied patch).
 * <h3>LP02C8 (CWvsContext 5F5DD90) — event item inventory operations [traced CLEAN: ability/out/lp02c8-ops.bin]</h3>
 * <pre>
 * u8 exclusiveReset (!=0: clears the request lock every CP02D3 sender sets), u8 result, i32 b, i32 c,
 * i32 n; n x { u8 op; u8 invType (1 equip / 2 gem); i16 pos; op 0 add / 3 update: EventItem; op 1 remove; op 2 move: i16 newPos }
 * EventItem kind 1 equip: u8 1, i64 sn, i32 infoId, i32 grade, i16 flag, u8 enhance, i32 bodyPart, 4 x {u8 has; gem}
 * </pre>
 * {@code infoId} = ItemInfo index 1..19 (the client looks it up in Etc/EventItem.img). This slice has no gems: the
 * four socket flags are always 0.
 * <h3>CP02D3 — event item request (senders 683C50..685600, static only)</h3>
 * {@code i32 updateTime, u8 sub, ...}; the server must always answer LP02C8 with exclusiveReset = 1.
 */
public final class DreamPackets {
    public static final int INVENTORY_OPERATION = 0x02C8;
    public static final int REQUEST = 0x02D3;
    public static final int OP_ADD = 0, OP_REMOVE = 1, OP_MOVE = 2, OP_UPDATE = 3;
    public static final int INV_EQUIP = 1, INV_GEM = 2;

    private DreamPackets() {
    }

    public record Op(int op, int invType, int pos, DreamStore.Equip item) {
        public static Op add(DreamStore.Equip e) {
            return new Op(OP_ADD, INV_EQUIP, e.pos(), e);
        }

        public static Op update(DreamStore.Equip e) {
            return new Op(OP_UPDATE, INV_EQUIP, e.pos(), e);
        }

        public static Op remove(int pos) {
            return new Op(OP_REMOVE, INV_EQUIP, pos, null);
        }
    }

    public static OutPacket inventoryOperation(boolean exclusiveReset, int result, int b, int c, List<Op> ops) {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, INVENTORY_OPERATION);
        out.encodeByte(exclusiveReset ? 1 : 0);
        out.encodeByte(result);
        out.encodeInt(b);
        out.encodeInt(c);
        out.encodeInt(ops.size());
        for (Op op : ops) {
            out.encodeByte(op.op());
            out.encodeByte(op.invType());
            out.encodeShort(op.pos());
            if (op.op() == OP_ADD || op.op() == OP_UPDATE) {
                DreamStore.Equip e = op.item();
                out.encodeByte(1);
                out.encodeLong(e.sn());
                out.encodeInt(e.infoIndex());
                out.encodeInt(e.grade());
                out.encodeShort(e.flag());
                out.encodeByte(e.enhance());
                out.encodeInt(e.bodyPart());
                for (int i = 0; i < 4; i++) {
                    out.encodeByte(0);
                }
            }
        }
        return out;
    }

    /** A parsed CP02D3 (the subs this slice handles; the rest keep only {@code sub}). */
    public sealed interface Request permits Move, Enhance, Disassemble, Other {
        int sub();
    }

    /** sub 0 (683C50): u8 invType, i16 from, i16 to, i64 snFrom, i64 snTo (move / equip / unequip, 推定). */
    public record Move(int invType, int from, int to, long snFrom, long snTo) implements Request {
        public int sub() {
            return 0;
        }
    }

    /** sub 2 (6840A0, UI ENCHANT): i16 pos, i64 sn. */
    public record Enhance(int pos, long sn) implements Request {
        public int sub() {
            return 2;
        }
    }

    /** sub 3 (684250, UI DISASSEMBLE): i32 n, n x (i16 pos, i64 sn). */
    public record Disassemble(List<long[]> items) implements Request {
        public int sub() {
            return 3;
        }
    }

    public record Other(int sub) implements Request {
    }

    /** Parses a CP02D3 packet (opcode + body); IllegalArgumentException for a short / wrong packet. */
    public static Request parse(byte[] packet) {
        try {
            ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            int op = in.getShort() & 0xFFFF;
            if (op != REQUEST) {
                throw new IllegalArgumentException(String.format("CP02D3: opcode %04X", op));
            }
            in.getInt(); // updateTime
            int sub = in.get() & 0xFF;
            return switch (sub) {
                case 0 -> new Move(in.get() & 0xFF, in.getShort(), in.getShort(), in.getLong(), in.getLong());
                case 2 -> new Enhance(in.getShort(), in.getLong());
                case 3 -> {
                    int n = in.getInt();
                    if (n < 0 || n > 200) {
                        throw new IllegalArgumentException("CP02D3 disassemble count " + n);
                    }
                    List<long[]> items = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        items.add(new long[]{in.getShort(), in.getLong()});
                    }
                    yield new Disassemble(items);
                }
                default -> new Other(sub);
            };
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("CP02D3: short body");
        }
    }
}
