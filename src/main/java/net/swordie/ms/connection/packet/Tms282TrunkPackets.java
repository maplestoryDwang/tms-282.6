package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.trunk.Trunk;
import net.swordie.ms.client.trunk.TrunkDlg;
import net.swordie.ms.client.trunk.TrunkOpen;
import net.swordie.ms.client.trunk.TrunkType;
import net.swordie.ms.client.trunk.TrunkUpdate;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.TrunkPacketEncoder;
import net.swordie.ms.enums.InvType;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * TMS 282.3 LP097B TRUNK_DLG (storage), the {@link TrunkPacketEncoder} of the 282 profile. Handler 462CDB0
 * (f(CInPacket*)): u8 type, then per type (jump table 462E1B0 over types 11..40 = the 265 {@link TrunkType} values).
 * 265 TRUNK_DLG is 2396; the in-game map guessed 0x991 (Delta +53), but 0x991 reads i32 i32 u8 and 0x97D is a small
 * five-case dialog, while 0x97B reads u8 type, i32 npc and the 100-byte slot table of 265 {@code Trunk.encodeItems}.
 * Traced with research/tms2823/daily-trace/daily_trace.py --pkt-first --entry 462CDB0.
 * <pre>
 * 28 open       (462D197): i32 npcTemplateId (462D22E), items block
 * 11 get / 16 put / 18 sort / 23 money (462D2A1 / 462DC33 / 462DC28 / 462DBF4): items block
 * 22 auto store (462D796): i32 stacks, u8 partial, items block
 * 15 find all   (462D2D3): i32 n, u8 partial, i32 n, n x i32 itemId, items block (265 layout, see the trace)
 * other types: no body (messages)
 * items block (4628020): u8 slotCount (4628069), Buf100 (46280C8; the money follows only if a byte is non-zero),
 *                        i64 money (4628234), then per bag 1..6 a u8 count (462853F) and count x GW_ItemSlotBase
 *                        (932790, {@link Tms282ItemEncoder}); 265 writes an i32 count and the 265 item form
 * </pre>
 */
public final class Tms282TrunkPackets implements TrunkPacketEncoder {
    public static final int OPCODE = 0x097B;

    private final Charset charset;

    public Tms282TrunkPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public OutPacket trunkDlg(TrunkDlg dlg) {
        Objects.requireNonNull(dlg, "dlg");
        if (dlg instanceof TrunkOpen open) {
            return open(open.getNpcTemplateID(), open.getTrunk(), charset);
        }
        if (dlg instanceof TrunkUpdate update) {
            return switch (update.getType()) {
                case TrunkRes_FindAll -> findAll(update.getTrunk(), update.getItemIds(), update.isPartial(), charset);
                case TrunkRes_AutoStore -> autoStore(update.getTrunk(), update.getSize(), update.isPartial(), charset);
                default -> update(update.getType(), update.getTrunk(), charset);
            };
        }
        return message(dlg.getType());
    }

    /** Type 28: the storage window of {@code npcTemplateId}. */
    public static OutPacket open(int npcTemplateId, Trunk trunk, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeByte(TrunkType.TrunkRes_OpenTrunkDlg.getVal());
        out.encodeInt(npcTemplateId);
        items(out, trunk, charset);
        return out;
    }

    /** Types 11 / 16 / 18 / 23 (and any other update): the whole storage again. */
    public static OutPacket update(TrunkType type, Trunk trunk, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeByte(type.getVal());
        items(out, trunk, charset);
        return out;
    }

    /** Type 22: {@code stacks} moved into the storage by auto-store. */
    public static OutPacket autoStore(Trunk trunk, int stacks, boolean partial, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeByte(TrunkType.TrunkRes_AutoStore.getVal());
        out.encodeInt(stacks);                                      // 462D7A7
        out.encodeByte(partial);                                    // 462D7B2
        items(out, trunk, charset);
        return out;
    }

    /** Type 15: the items taken out by "find all". */
    public static OutPacket findAll(Trunk trunk, List<Integer> itemIds, boolean partial, Charset charset) {
        List<Integer> ids = itemIds == null ? List.of() : itemIds;
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeByte(TrunkType.TrunkRes_FindAll.getVal());
        out.encodeInt(ids.size());                                  // 462D2E6
        out.encodeByte(partial);                                    // 462D2F1
        out.encodeInt(ids.size());
        for (int id : ids) {
            out.encodeInt(id);
        }
        items(out, trunk, charset);
        return out;
    }

    /** A result without body (the 265 {@code TrunkMsg} types). */
    public static OutPacket message(TrunkType type) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeByte(type.getVal());
        return out;
    }

    /** The items block 4628020. */
    static void items(OutPacket out, Trunk trunk, Charset charset) {
        out.encodeByte(trunk.getSlotCount());
        for (int i = 0; i < 100; i++) {
            out.encodeByte(1);
        }
        out.encodeLong(trunk.getMoney());
        for (int i = 1; i <= 6; i++) {
            InvType type = InvType.getInvTypeByVal(i);
            List<Item> items = trunk.getItems().stream().filter(it -> it.getInvType() == type).toList();
            if (items.size() > 0xFF) {
                throw new IllegalStateException("more than 255 items of " + type + " in trunk " + trunk.getId());
            }
            out.encodeByte(items.size());
            for (Item item : items) {
                Tms282ItemEncoder.encode(out, item, charset);
            }
        }
    }
}
