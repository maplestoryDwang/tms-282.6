package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.InvType;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * TMS 282.3 LP008F INVENTORY_OPERATION (CWvsContext handler 5EE6F80). Layout: ingame-layouts-282.md 3.G [verified],
 * read back by the emulated client (research/tms2823/combat-trace, Tms282DropCheck):
 * <pre>
 * u8 exclRequest  u8 0  i32 n  u8 notRemoveAddInfo
 * n x (u8 type, u8 invType, u16 pos, body)
 *     0 Add            GW_ItemSlotBase ({@link Tms282ItemEncoder})
 *     1 UpdateQuantity i32 quantity          (265: u16)
 *     2 Move           u16 newPos
 *     3 Remove         -
 * [u8 0]           once, when any Move of invType 1/6 has pos &lt; 0 or newPos &lt; 0, or any Remove of invType 1/6
 *                  has pos &lt; 0 (5EE90DE)
 * </pre>
 * The 282 bag operations are the 265 ones +2 (9..14) and are not offered here. Each operation is one
 * {@link Operation} (a small sealed hierarchy: the client's switch, one record per case).
 */
public final class Tms282InventoryPackets {
    public static final int INVENTORY_OPERATION = 0x008F;

    private Tms282InventoryPackets() {
    }

    /** One entry of the operation list. */
    public sealed interface Operation permits Add, UpdateQuantity, Move, Remove {
        int type();

        InvType invType();

        short pos();

        void encodeBody(OutPacket out, Charset charset);

        /** True when this entry makes the client read the trailing movement byte. */
        default boolean needsMovementByte() {
            return false;
        }
    }

    /** Type 0: a new item in {@code pos}. */
    public record Add(InvType invType, short pos, Item item) implements Operation {
        public Add {
            Objects.requireNonNull(invType, "invType");
            Objects.requireNonNull(item, "item");
        }

        @Override
        public int type() {
            return 0;
        }

        @Override
        public void encodeBody(OutPacket out, Charset charset) {
            Tms282ItemEncoder.encode(out, item, charset);
        }
    }

    /** Type 1: the stack in {@code pos} now holds {@code quantity} (i32). */
    public record UpdateQuantity(InvType invType, short pos, int quantity) implements Operation {
        public UpdateQuantity {
            Objects.requireNonNull(invType, "invType");
        }

        @Override
        public int type() {
            return 1;
        }

        @Override
        public void encodeBody(OutPacket out, Charset charset) {
            out.encodeInt(quantity);
        }
    }

    /** Type 2: the item in {@code pos} moves to {@code newPos}. */
    public record Move(InvType invType, short pos, short newPos) implements Operation {
        public Move {
            Objects.requireNonNull(invType, "invType");
        }

        @Override
        public int type() {
            return 2;
        }

        @Override
        public void encodeBody(OutPacket out, Charset charset) {
            out.encodeShort(clientPos(invType, newPos));
        }

        @Override
        public boolean needsMovementByte() {
            return equipLike(invType) && (pos < 0 || newPos < 0);
        }
    }

    /** Type 3: the item in {@code pos} is gone. */
    public record Remove(InvType invType, short pos) implements Operation {
        public Remove {
            Objects.requireNonNull(invType, "invType");
        }

        @Override
        public int type() {
            return 3;
        }

        @Override
        public void encodeBody(OutPacket out, Charset charset) {
        }

        @Override
        public boolean needsMovementByte() {
            return equipLike(invType) && pos < 0;
        }
    }

    /** LP008F with the given operations. */
    public static OutPacket inventoryOperation(boolean exclRequest, List<? extends Operation> operations, Charset charset) {
        return inventoryOperation(exclRequest, operations, charset, false);
    }

    /**
     * {@code notRemoveAddInfo}: as 265's sort / gather (InventoryHandler), the client's Remove branch (5EE7FF1) then
     * skips its "item removed" feedback for the Remove + Add pairs of a rearrangement (placeholder-audit-0929 #6).
     */
    public static OutPacket inventoryOperation(boolean exclRequest, List<? extends Operation> operations, Charset charset,
                                               boolean notRemoveAddInfo) {
        Objects.requireNonNull(operations, "operations");
        Objects.requireNonNull(charset, "charset");
        OutPacket out = Tms282LoginPacketSupport.packet(INVENTORY_OPERATION);
        out.encodeByte(exclRequest ? 1 : 0);
        out.encodeByte(0);
        out.encodeInt(operations.size());
        out.encodeByte(notRemoveAddInfo ? 1 : 0);
        // addMovementInfo (5EE90DE -> 579CC50 -> the user's vtbl+0x130): as 265, 1 after a move into / out of a worn
        // slot, 2 after removing a worn item (the last such operation wins). 0 skipped the avatar update: totems (and
        // other looks) appeared only after a field change (live 2026-09-29).
        boolean movementByte = false;
        int movementInfo = 0;
        for (Operation op : operations) {
            InvType invType = op.invType() == InvType.EQUIPPED ? InvType.EQUIP : op.invType();
            out.encodeByte(op.type());
            out.encodeByte(invType.getVal());
            out.encodeShort(clientPos(op.invType(), op.pos()));
            op.encodeBody(out, charset);
            if (op.needsMovementByte()) {
                movementByte = true;
                movementInfo = op.type() == 3 ? 2 : 1;
            }
        }
        if (movementByte) {
            out.encodeByte(movementInfo);
        }
        return out;
    }

    /** Unlocks the client without changing anything (n = 0). */
    public static OutPacket unlock(Charset charset) {
        return inventoryOperation(true, List.of(), charset);
    }

    /** A worn position of the equip inventories as 282 numbers it ({@link Tms282BodySlots}: android, heart). */
    static short clientPos(InvType invType, short pos) {
        return equipLike(invType) ? Tms282BodySlots.toClient(pos) : pos;
    }

    private static boolean equipLike(InvType invType) {
        return invType == InvType.EQUIP || invType == InvType.EQUIPPED || invType == InvType.DECORATION;
    }
}
