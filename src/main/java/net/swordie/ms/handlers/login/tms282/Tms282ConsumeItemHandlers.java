package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.Tms282InventoryWatch;
import net.swordie.ms.client.character.tms282.Tms282StatDiff;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.ItemHandler;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.world.tms282.boxes.Tms282BoxUse;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TMS 282.3 consumables whose request layout statically equals the GMS 265 one (item-usability-20260928 A1:
 * ingame-cp-map-2823.tsv "序列一致（exact）", 346A070 appends no tail u32 for any of them). Each 282 request is
 * parsed strictly, pre-checked (slot / item id / target) so the 265 handler cannot throw or eat an item it was not
 * asked to use, handed to the unchanged 265 handler as its 265 body, and answered with the 282 diff replies
 * ({@link Tms282InventoryWatch} + {@link Tms282StatDiff}: LP008F then LP009D, both exclRequest; LP009D unlock when
 * nothing changed). 265 CHANGE_SKILL_RECORD_RESULT reaches the client as LP00A2 (Tms282SkillLegacyTranslators).
 * <ul>
 *     <li>CP014B summon sack (210xxxx, sender 5E0FF10: tick, u16 slot, i32 id): the 265 handler only consumes the
 *     sack, so this uses {@link Tms282SummonSacks} (282 0210.img mob lists and limits) instead: spawn at the user,
 *     consume one; no data / forbidden field -> refused without consuming;</li>
 *     <li>CP014F recipe (251xxxx, 5E11700: tick, u16 slot, i32 id) -> 265 {@code handleUserRecipeOpenItemUseRequest};</li>
 *     <li>CP0154 capture / bridle (227/240, 5E40860: tick, u16 slot, i32 id, i32 mob object id) -> 265
 *     {@code handleUserBridleItemUseRequest};</li>
 *     <li>CP0155 skill / mastery book (228/229, 5E1DEC0: tick, u16 slot, i32 id): items with WZ {@code reward/}
 *     (the 60 神祕技能書) go to the shared data-driven box registry ({@link Tms282BoxUse}) when a handler owns them,
 *     otherwise they are refused without consuming; the rest -> 265 {@code handleUserLearnItemUseRequest};</li>
 *     <li>CP0173 soul enchanter (2590xxx, 36982F0: tick, u16 use slot, i16 equip pos) -> 265
 *     {@code handleUserItemSkillSocketUpdateItemUseRequest};</li>
 *     <li>CP0174 soul (2591xxx, 36978B0: tick, u16 use slot, i16 equip pos) -> 265
 *     {@code handleUserItemSkillOptionUpdateItemUseRequest}, whose option now comes from
 *     {@link net.swordie.ms.client.character.tms282.Tms282SoulOptions} (by soul id, resolved before the equip
 *     changes).</li>
 * </ul>
 * IMPLEMENTED_UNTESTED: no live capture of any of these requests or of the official replies. The 265 result
 * packets that have no 282 translation (MOB_EFFECT_BY_ITEM, BRIDLE_MOB_CATCH_FAIL, the soul socket/option result
 * windows) are dropped by the fail-closed guard. Nothing here ends the session: a bad packet or a 265 failure is
 * logged and answered with LP009D unlock.
 */
final class Tms282ConsumeItemHandlers {
    static final int MOB_SUMMON = 0x014B;
    static final int RECIPE_OPEN = 0x014F;
    static final int BRIDLE = 0x0154;
    static final int SKILL_LEARN = 0x0155;
    static final int SOUL_SOCKET = 0x0173;
    static final int SOUL_OPTION = 0x0174;
    static final int BEAUTY_COUPON = 0x01C2;

    /**
     * CP01C2 consume beauty coupon (sender 5F83E80, packet block 5F855D2; Unicorn: research/tms2823/
     * item-usability-20260928/beauty-cp01c2/cp01c2_sender_check.py PASS): {@code u16 slot, i32 itemId, u8 target
     * (4E16E0: 0 self, 1, 2, 0x64 android, 0x65), u8 dialog flag, [u64 when target = 0x64]}. No tick. 265
     * USER_CONSUME_HAIR_ITEM_USE_REQUEST (0x1B8) read only {@code i16 slot, i32 itemId}.
     */
    record BeautyCouponRequest(short slot, int itemId, int target, int flag, Long extra) {
        static BeautyCouponRequest parse(byte[] packet) {
            int target = packet != null && packet.length >= 9 ? packet[8] & 0xFF : -1;
            ByteBuffer in = open(BEAUTY_COUPON, packet, target == 0x64 ? 18 : 10);
            short slot = in.getShort();
            int itemId = in.getInt();
            in.get();
            int flag = in.get() & 0xFF;
            return new BeautyCouponRequest(slot, itemId, target, flag, target == 0x64 ? in.getLong() : null);
        }

        @Override
        public String toString() {
            return String.format("CP01C2 slot=%d item=%d target=%d flag=%d%s", slot, itemId, target, flag,
                    extra == null ? "" : String.format(" sn=0x%X", extra));
        }
    }

    /** CP014B / CP014F / CP0155 (and CP0154's first three fields): {@code i32 tick, u16 slot, i32 itemId}. */
    record SlotItemRequest(int op, int tick, short slot, int itemId, Integer mobObjectId) {
        static SlotItemRequest parse(int op, byte[] packet, boolean withMob) {
            ByteBuffer in = open(op, packet, withMob ? 16 : 12);
            return new SlotItemRequest(op, in.getInt(), in.getShort(), in.getInt(), withMob ? in.getInt() : null);
        }

        /** The 265 body: the same fields, same order. */
        byte[] toGms265Body() {
            ByteBuffer b = ByteBuffer.allocate(mobObjectId == null ? 10 : 14).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(tick).putShort(slot).putInt(itemId);
            if (mobObjectId != null) {
                b.putInt(mobObjectId);
            }
            return b.array();
        }

        @Override
        public String toString() {
            return String.format("CP%04X slot=%d item=%d%s", op, slot, itemId, mobObjectId == null ? "" : " mob=" + mobObjectId);
        }
    }

    /** CP0173 / CP0174: {@code i32 tick, u16 usePos, i16 equipPos} (negative = worn). */
    record SoulRequest(int op, int tick, short usePos, short equipPos) {
        static SoulRequest parse(int op, byte[] packet) {
            ByteBuffer in = open(op, packet, 10);
            return new SoulRequest(op, in.getInt(), in.getShort(), in.getShort());
        }

        byte[] toGms265Body() {
            return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(tick).putShort(usePos).putShort(equipPos).array();
        }

        @Override
        public String toString() {
            return String.format("CP%04X usePos=%d equipPos=%d", op, usePos, equipPos);
        }
    }

    /** Checks opcode and exact length; returns a reader positioned after the opcode. */
    static ByteBuffer open(int op, byte[] packet, int length) {
        if (packet == null || packet.length != length) {
            throw new IllegalArgumentException(String.format("CP%04X length %d, expected %d", op,
                    packet == null ? -1 : packet.length, length));
        }
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        int actual = Short.toUnsignedInt(in.getShort());
        if (actual != op) {
            throw new IllegalArgumentException(String.format("CP%04X expected, got CP%04X", op, actual));
        }
        return in;
    }

    private final Charset charset;

    Tms282ConsumeItemHandlers(Tms282LoginContext context) {
        this.charset = Objects.requireNonNull(context, "context").settings().nameCharset();
    }

    void register(Tms282Routes routes) {
        routes.channel(MOB_SUMMON, InHeader.USER_MOB_SUMMON_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("summon sack (untested) -> 282 0210.img mob list, spawn + LP008F", this::onMobSummon);
        routes.channel(RECIPE_OPEN, InHeader.USER_RECIPE_OPEN_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("recipe (untested) -> 265 recipe open, LP00A2 + LP008F", this::onRecipeOpen);
        routes.channel(BRIDLE, InHeader.USER_BRIDLE_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("capture (untested) -> 265 bridle, LP008F", this::onBridle);
        routes.channel(SKILL_LEARN, InHeader.USER_SKILL_LEARN_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("skill/mastery book (untested) -> WZ reward box registry or 265 learn item, LP00A2 + LP008F", this::onSkillLearn);
        routes.channel(SOUL_SOCKET, InHeader.USER_ITEM_SKILL_SOCKET_UPGRADE_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("soul enchanter (untested) -> 265 socket upgrade, LP008F", this::onSoulSocket);
        routes.channel(SOUL_OPTION, InHeader.USER_ITEM_SKILL_OPTION_UPGRADE_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("soul (untested) -> 265 option upgrade, LP008F (+ LP00A2)", this::onSoulOption);
        routes.channel(BEAUTY_COUPON, InHeader.USER_CONSUME_HAIR_ITEM_USE_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("hair/face coupon 254/289 (untested) -> spec/cosmetic via Tms282Beauty, LP008F + LP009D", this::onBeautyCoupon);
    }

    // ---- CP014B -------------------------------------------------------------------------------------------------

    private void onMobSummon(Tms282LoginState session, int op, byte[] packet) {
        SlotItemRequest request = parseSlotItem(session, op, packet, false);
        if (request == null) {
            return;
        }
        Char chr = session.fieldCharacter();
        if (!ownsInConsume(chr, request.slot(), request.itemId())) {
            refuse(session, chr, request.toString(), "item not in that CONSUME slot");
            return;
        }
        Item sack = chr.getConsumeInventory().getItemBySlot(request.slot());
        Tms282SummonSacks.Result[] result = new Tms282SummonSacks.Result[1];
        // own logic (the 265 handler only consumed the sack): 282 0210.img mob lists, validated before consuming
        delegate(session, chr, request + " (summon sack)",
                () -> result[0] = Tms282SummonSacks.get().use(chr, sack, java.util.concurrent.ThreadLocalRandom.current()));
        if (result[0] != null && !result[0].ok()) {
            chr.chatMessage(ChatType.SystemNotice, "這裡無法使用召喚袋，道具未被消耗。");
            System.out.println("[TMS282] REFUSED " + request + " character=" + chr.getId() + ": " + result[0].refused());
        } else if (result[0] != null) {
            System.out.println("[TMS282] " + request + " summoned " + result[0].spawned());
        }
    }

    // ---- CP01C2 -------------------------------------------------------------------------------------------------

    private void onBeautyCoupon(Tms282LoginState session, int op, byte[] packet) {
        BeautyCouponRequest request;
        try {
            request = BeautyCouponRequest.parse(packet);
        } catch (IllegalArgumentException e) {
            reject(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        if (!ownsInConsume(chr, request.slot(), request.itemId())) {
            refuse(session, chr, request.toString(), "item not in that CONSUME slot");
            return;
        }
        Item coupon = chr.getConsumeInventory().getItemBySlot(request.slot());
        Tms282ConsumeBeautyCoupons.Result[] result = new Tms282ConsumeBeautyCoupons.Result[1];
        delegate(session, chr, request.toString(),
                () -> result[0] = Tms282ConsumeBeautyCoupons.use(chr, coupon, request.target()));
        if (result[0] != null && !result[0].ok()) {
            chr.chatMessage(ChatType.SystemNotice, "此美容券目前無法使用，道具未被消耗。");
            System.out.println("[TMS282] REFUSED " + request + " character=" + chr.getId() + ": " + result[0].refused());
        } else if (result[0] != null) {
            System.out.println("[TMS282] " + request + " -> " + (result[0].kind() == 0 ? "hair " : "face ") + result[0].styleId());
        }
    }

    // ---- CP014F -------------------------------------------------------------------------------------------------

    private void onRecipeOpen(Tms282LoginState session, int op, byte[] packet) {
        SlotItemRequest request = parseSlotItem(session, op, packet, false);
        if (request == null) {
            return;
        }
        Char chr = session.fieldCharacter();
        if (!ownsInConsume(chr, request.slot(), request.itemId()) || !ItemConstants.isRecipeOpenItem(request.itemId())) {
            refuse(session, chr, request.toString(), "not a recipe in that CONSUME slot");
            return;
        }
        delegate(session, chr, request.toString(),
                () -> ItemHandler.handleUserRecipeOpenItemUseRequest(chr, new InPacket(request.toGms265Body())));
    }

    // ---- CP0154 -------------------------------------------------------------------------------------------------

    private void onBridle(Tms282LoginState session, int op, byte[] packet) {
        SlotItemRequest request = parseSlotItem(session, op, packet, true);
        if (request == null) {
            return;
        }
        Char chr = session.fieldCharacter();
        if (!ownsInConsume(chr, request.slot(), request.itemId()) || !ItemConstants.isBridleItem(request.itemId())) {
            refuse(session, chr, request.toString(), "not a capture item in that CONSUME slot");
            return;
        }
        if (chr.getField() == null || !(chr.getField().getLifeByObjectID(request.mobObjectId()) instanceof Mob)) {
            refuse(session, chr, request.toString(), "no such mob in the field");
            return;
        }
        delegate(session, chr, request.toString(),
                () -> ItemHandler.handleUserBridleItemUseRequest(chr, new InPacket(request.toGms265Body())));
    }

    // ---- CP0155 -------------------------------------------------------------------------------------------------

    private void onSkillLearn(Tms282LoginState session, int op, byte[] packet) {
        SlotItemRequest request = parseSlotItem(session, op, packet, false);
        if (request == null) {
            return;
        }
        Char chr = session.fieldCharacter();
        if (!ownsInConsume(chr, request.slot(), request.itemId())) {
            refuse(session, chr, request.toString(), "item not in that CONSUME slot");
            return;
        }
        ItemInfo info = ItemData.getItemInfoByID(request.itemId());
        if (info != null && !info.getItemRewardInfos().isEmpty()) {
            // 神祕技能書 (229xxxx with WZ reward/): a box, not a skill book (boxes README W3).
            Item book = chr.getConsumeInventory().getItemBySlot(request.slot());
            if (!Tms282BoxUse.handles(request.itemId())) {
                chr.chatMessage(ChatType.SystemNotice, "此技能書的獎勵目前尚未支援，道具未被消耗。");
                refuse(session, chr, request.toString(), "WZ reward/ item and no box handler owns it");
                return;
            }
            delegate(session, chr, request + " (WZ reward box)", () -> Tms282BoxUse.tryUse(chr, book));
            return;
        }
        delegate(session, chr, request.toString(),
                () -> ItemHandler.handleUserLearnItemUseRequest(chr, new InPacket(request.toGms265Body())));
    }

    // ---- CP0173 / CP0174 ----------------------------------------------------------------------------------------

    private void onSoulSocket(Tms282LoginState session, int op, byte[] packet) {
        onSoul(session, op, packet, true);
    }

    private void onSoulOption(Tms282LoginState session, int op, byte[] packet) {
        onSoul(session, op, packet, false);
    }

    private void onSoul(Tms282LoginState session, int op, byte[] packet, boolean socket) {
        SoulRequest request;
        try {
            request = SoulRequest.parse(op, packet);
        } catch (IllegalArgumentException e) {
            reject(session, op, packet, e);
            return;
        }
        Char chr = session.fieldCharacter();
        Item use = chr.getConsumeInventory().getItemBySlot(request.usePos());
        if (use == null || use.getQuantity() < 1 || !(socket ? ItemConstants.isSoulEnchanter(use.getItemId())
                : ItemConstants.isSoul(use.getItemId()))) {
            refuse(session, chr, request.toString(), (socket ? "no soul enchanter" : "no soul") + " in that CONSUME slot");
            return;
        }
        InvType equipInv = request.equipPos() < 0 ? InvType.EQUIPPED : InvType.EQUIP;
        if (!(chr.getInventoryByType(equipInv).getItemBySlot(request.equipPos()) instanceof Equip)) {
            refuse(session, chr, request.toString(), "no equip at that position");
            return;
        }
        String summary = request + " item=" + use.getItemId();
        if (socket) {
            delegate(session, chr, summary,
                    () -> ItemHandler.handleUserItemSkillSocketUpdateItemUseRequest(chr, new InPacket(request.toGms265Body())));
        } else {
            delegate(session, chr, summary,
                    () -> ItemHandler.handleUserItemSkillOptionUpdateItemUseRequest(chr, new InPacket(request.toGms265Body())));
        }
    }

    // ---- shared -------------------------------------------------------------------------------------------------

    private static SlotItemRequest parseSlotItem(Tms282LoginState session, int op, byte[] packet, boolean withMob) {
        try {
            return SlotItemRequest.parse(op, packet, withMob);
        } catch (IllegalArgumentException e) {
            reject(session, op, packet, e);
            return null;
        }
    }

    private static boolean ownsInConsume(Char chr, short slot, int itemId) {
        Item item = chr.getConsumeInventory().getItemBySlot(slot);
        return item != null && item.getItemId() == itemId && item.getQuantity() >= 1;
    }

    /** Runs the 265 handler and sends the 282 replies of what it changed (also after a 265 failure). */
    private void delegate(Tms282LoginState session, Char chr, String summary, Runnable gms265) {
        Tms282InventoryWatch inventory = Tms282InventoryWatch.of(chr);
        Tms282StatDiff stats = Tms282StatDiff.of(chr);
        String failure = "";
        try {
            gms265.run();
        } catch (RuntimeException | LinkageError e) {
            failure = " FAILED " + e + ";";
        }
        List<OutPacket> packets = replies(inventory.changes(), stats.changes());
        for (OutPacket p : packets) {
            session.send(p);
        }
        System.out.println("[TMS282] " + summary + " character=" + chr.getId() + " -> 265;" + failure + " SENT "
                + packets.size() + " reply packet(s)");
    }

    /** LP008F (operations) and LP009D (stats) with exclRequest; LP009D unlock when both are empty. */
    List<OutPacket> replies(List<Tms282InventoryPackets.Operation> operations, Map<Tms282StatPackets.Stat, Long> stats) {
        List<OutPacket> packets = new ArrayList<>(2);
        if (!operations.isEmpty()) {
            packets.add(Tms282InventoryPackets.inventoryOperation(true, operations, charset));
        }
        if (!stats.isEmpty() || packets.isEmpty()) {
            packets.add(Tms282StatPackets.statChanged(true, stats));
        }
        return packets;
    }

    private static void refuse(Tms282LoginState session, Char chr, String summary, String why) {
        session.send(Tms282StatPackets.unlock());
        System.out.println("[TMS282] REFUSED " + summary + " character=" + chr.getId() + ": " + why + "; SENT LP009D unlock");
    }

    private static void reject(Tms282LoginState session, int op, byte[] packet, IllegalArgumentException e) {
        session.send(Tms282StatPackets.unlock());
        System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s; SENT LP009D unlock%n", op,
                packet == null ? -1 : packet.length, e.getMessage());
    }
}
