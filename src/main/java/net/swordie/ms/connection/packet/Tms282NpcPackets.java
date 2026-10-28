package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.NpcPacketEncoder;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.life.npc.Npc;
import net.swordie.ms.util.Position;

import java.nio.charset.Charset;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 CNpcPool packets (CNpcPool::OnPacket 40FFD00; per-NPC packets 0x731.. go through the jump table at
 * 40FFEB1 after {@code i32 objectId}). Read back by the emulated client (research/tms2823/field-trace).
 * <ul>
 * <li>LP072B NPC_ENTER_FIELD [verified] (inline 40FFD75): {@code i32 objectId, i32 templateId} + CNpc::Init data;</li>
 * <li>LP072C NPC_LEAVE_FIELD [presumed] (41000F6): {@code i32 objectId};</li>
 * <li>LP072D NPC_CHANGE_CONTROLLER [verified] (4100790): {@code u8 controller, i32 objectId} and, unless the NPC
 * leaves, {@code i32 templateId} + CNpc::Init data;</li>
 * <li>LP0731 NPC_MOVE [verified] (40CC7B0): {@code i32 objectId, u8 oneTimeAction, u8 chatIdx, i32 duration}, then
 * - only when the client's NPC template moves ([npc+0x280]+0x28) - the path through 3FDD1D0 with keyPad 0
 * ({@link MovePathCodec#encodeBroadcast}: header and 282 elements, no key pad).</li>
 * </ul>
 * CNpc::Init data: see {@link #encodeInit} (265 {@code Npc.encode} with one byte moved: 265 packets misread).
 */
public final class Tms282NpcPackets implements NpcPacketEncoder {
    public static final int NPC_ENTER_FIELD = 0x072B;
    public static final int NPC_LEAVE_FIELD = 0x072C;
    public static final int NPC_CHANGE_CONTROLLER = 0x072D;
    public static final int NPC_MOVE = 0x0731;
    /** 265 Npc.encode constant (v263). */
    public static final long INIT_CONSTANT = 368573625000000L;

    private final Charset charset;

    public Tms282NpcPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public OutPacket enterField(Npc npc) {
        return enterField(npc, charset);
    }

    @Override
    public OutPacket leaveField(Npc npc) {
        return leaveField(npc.getObjectId());
    }

    @Override
    public OutPacket changeController(Npc npc, boolean controller, boolean remove) {
        return changeController(npc, controller, remove, charset);
    }

    /** LP072B. */
    public static OutPacket enterField(Npc npc, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(NPC_ENTER_FIELD);
        out.encodeInt((long) npc.getObjectId());
        out.encodeInt((long) npc.getTemplateId());
        encodeInit(out, npc, charset);
        return out;
    }

    /** LP072C. */
    public static OutPacket leaveField(int objectId) {
        OutPacket out = Tms282LoginPacketSupport.packet(NPC_LEAVE_FIELD);
        out.encodeInt((long) objectId);
        return out;
    }

    /** LP072D. */
    public static OutPacket changeController(Npc npc, boolean controller, boolean remove, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(NPC_CHANGE_CONTROLLER);
        out.encodeByte(controller);
        out.encodeInt((long) npc.getObjectId());
        if (!remove) {
            out.encodeInt((long) npc.getTemplateId());
            encodeInit(out, npc, charset);
        }
        return out;
    }

    /** LP0731; {@code path} null when the NPC does not move (265 NpcPool.npcMove with move = false). */
    public static OutPacket move(int objectId, int oneTimeAction, int chatIdx, int duration, MovementInfo path) {
        return move(objectId, oneTimeAction, chatIdx, duration, path, Tms282MovePathCodec.INSTANCE);
    }

    public static OutPacket move(int objectId, int oneTimeAction, int chatIdx, int duration, MovementInfo path,
                                 MovePathCodec paths) {
        OutPacket out = Tms282LoginPacketSupport.packet(NPC_MOVE);
        out.encodeInt((long) objectId);
        out.encodeByte(oneTimeAction);
        out.encodeByte(chatIdx);
        out.encodeInt((long) duration);
        if (path != null) {
            paths.encodeBroadcast(out, path);
        }
        return out;
    }

    /**
     * CNpc::Init (40C18E0) data [verified: every Decode call of the function and the traced read order]:
     * <pre>
     * i16 x, i16 y              40C196C / 40C197F
     * i32 -1, i32 -1            40C1992 / 40C19A2   (265 v263 ints)
     * u8  move, u8 !flip        40C19E6 / 40C19FE
     * i16 fh, i16 rx0, i16 rx1  40C1A18 / 40C1A36 / 40C1A43
     * i16 y, i16 y              40C1A5E / 40C1A6B
     * u8  enabled               40C1BEF
     * i32 0                     40C3E8F  (-> 40DC750)
     * i32 presentItemId, u8 presentItemState, i32 presentItemTime   40C3EA1 / 40C3EB1 / 40C3EC1
     * buf8                      40C3EDF  265 writes u8 0 + i64 here: 282 has no u8 before the 8 bytes
     * i32 noticeBoardType       40C3F04
     * i32 alpha                 40C3FAC
     * i32 0                     40C403D
     * str localRepeatEffect     40C4065
     * u8  0  [AvatarLook 901AF0 when non-zero]            40C4205
     * u8  0  [str + AvatarLook 901AF0 when non-zero]      40C4A10  282 new
     * </pre>
     * 265's packet has the same length (one byte moved) and reads without an exception, but every field after the
     * present item would be shifted by one byte.
     */
    static void encodeInit(OutPacket out, Npc npc, Charset charset) {
        Position pos = npc.getPosition();
        out.encodePosition(pos);
        out.encodeInt(-1L);
        out.encodeInt(-1L);
        out.encodeByte(npc.isMove());
        out.encodeByte(!npc.isFlip());
        out.encodeShort(npc.getFh());
        out.encodeShort(npc.getRx0());
        out.encodeShort(npc.getRx1());
        out.encodeShort(pos == null ? 0 : pos.getY());
        out.encodeShort(pos == null ? 0 : pos.getY());
        out.encodeByte(npc.isEnabled());
        out.encodeInt(0L);
        out.encodeInt((long) npc.getPresentItemID());
        out.encodeByte(npc.getPresentItemState());
        out.encodeInt((long) npc.getPresentItemTime());
        out.encodeLong(INIT_CONSTANT);
        out.encodeInt((long) npc.getNoticeBoardType());
        out.encodeInt((long) npc.getAlpha());
        out.encodeInt(0L);
        str(out, npc.getLocalRepeatEffect() == null ? "" : npc.getLocalRepeatEffect(), charset);
        out.encodeByte(0);                                 // no AvatarLook
        out.encodeByte(0);                                 // no second name + AvatarLook (282 new)
    }
}
