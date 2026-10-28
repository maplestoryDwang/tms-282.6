package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.tms282.Tms282InventoryWatch;
import net.swordie.ms.connection.packet.Tms282CharacterInfoBackgroundRequest;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282ScriptItemUseRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.Etc.EtcData;
import net.swordie.ms.loaders.overlay.DataOverlays;
import net.swordie.ms.util.XMLApi;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Native list: free IDs 0..6, then numeric properties of QR101367 and QR502593 (2D8C6B0). */
final class Tms282CharacterInfoBackgroundHandlers {
    private static int generation = -1;
    private static Set<Integer> catalogue = Set.of();

    private static synchronized Set<Integer> catalogue() {
        if (generation != DataOverlays.generation()) {
            Set<Integer> loaded = new HashSet<>();
            var doc = XMLApi.getRoot(EtcData.etcWzFile("customBackground.img.xml"));
            if (doc == null) throw new IllegalStateException("Character info background catalogue unavailable");
            for (var node : XMLApi.getAllChildren(doc.getDocumentElement())) {
                try { loaded.add(Integer.parseInt(XMLApi.getNamedAttribute(node, "name"))); }
                catch (NumberFormatException ignored) { }
            }
            catalogue = Set.copyOf(loaded);
            generation = DataOverlays.generation();
        }
        return catalogue;
    }

    private static Map<String, String> properties(Quest quest) {
        Map<String, String> values = new HashMap<>();
        if (quest != null && quest.getQRValue() != null) {
            for (String entry : quest.getQRValue().split(";")) {
                String[] pair = entry.split("=", 2);
                if (pair.length == 2) values.put(pair[0], pair[1]);
            }
        }
        return values;
    }

    private static boolean available(Char chr, int id) {
        if (!catalogue().contains(id)) return false;
        if (id >= 0 && id <= 6) return true;
        for (int key : new int[]{101367, 502593}) {
            var values = properties(chr.getQuestById(key));
            // Original client enumerates keys "0" .. property count - 1; unrelated/sparse keys do not unlock.
            for (int i = 0; i < values.size(); i++) {
                if (Integer.toString(id).equals(values.get(Integer.toString(i)))) return true;
            }
        }
        return false;
    }

    void register(Tms282Routes routes) {
        routes.channel(Tms282CharacterInfoBackgroundRequest.OPCODE, InHeader.USER_CHANGE_CUSTOMIZED_BACKGROUND_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("character info background -> authorized catalogue ID and QR7295", this::select);
    }

    private void select(Tms282LoginState session, int op, byte[] packet) {
        try {
            var request = Tms282CharacterInfoBackgroundRequest.parse(packet);
            Char chr = session.fieldCharacter();
            if (!available(chr, request.id())) throw new IllegalArgumentException("background is not unlocked");
            chr.createQuestWithQRValue(7295, Integer.toString(request.id()), false);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REFUSED CP0124: %s%n", e.getMessage());
        } finally {
            session.send(Tms282StatPackets.unlock());
        }
    }

    static boolean isCoupon(int itemId) { return itemId == 2639943 || itemId == 2639944; }

    /** Existing two scripts register catalogue25; reserve one of their five QR slots before spending one item. */
    static void coupon(Tms282LoginState session, Tms282ScriptItemUseRequest request, Charset charset) {
        Char chr = session.fieldCharacter();
        var item = chr.getConsumeInventory().getItemBySlot(request.slot());
        if (!isCoupon(request.itemId()) || request.quantity() != 1 || item == null
                || item.getItemId() != request.itemId() || item.getCharID() != chr.getId() || item.getQuantity() < 1
                || item.getDateExpire() != null && item.getDateExpire().isExpired() || !catalogue().contains(25)) {
            session.send(Tms282StatPackets.unlock());
            return;
        }
        if (available(chr, 25)) {
            chr.chatMessage(ChatType.SystemNotice, "你已經擁有這個角色資訊背景，道具未被消耗。");
            session.send(Tms282StatPackets.unlock());
            return;
        }
        var slots = properties(chr.getQuestById(502593));
        for (int i = 0; i < 5; i++) {
            String value = slots.get(Integer.toString(i));
            if (value != null && !value.equals("0")) continue;
            var inventory = Tms282InventoryWatch.of(chr);
            chr.setQRValueByKey(502593, Integer.toString(i), 25);
            chr.consumeItem(item, 1);
            session.send(Tms282InventoryPackets.inventoryOperation(true, inventory.changes(), charset));
            return;
        }
        chr.chatMessage(ChatType.SystemNotice, "角色資訊背景登錄欄位已滿，道具未被消耗。");
        session.send(Tms282StatPackets.unlock());
    }
}
