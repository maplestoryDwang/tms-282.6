package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Android and monster card / monster collection rules of {@link Tms282LegacyPackets}: the 265 builders (AndroidPacket,
 * UserRemote.androidEmotion, WvsContext.monsterBookSetCard / monsterCollectionResult) stay as they are and their
 * packets become the 282 forms here, so the 265 android life cycle (Char.initAndroid / Field.spawnLife on equip and
 * field entry, InventoryHandler's ANDROID_MODIFIED on android equipment moves) and the 265 card pick-up
 * (Char.consumeItemOnPickup) reach the 282 client unchanged.
 * <ul>
 *     <li>ANDROID_CREATED -> LP044F ({@link Tms282AndroidPackets#created}): the 265 items are {@code i32, i32, u8}
 *     each (14, BodyPart 1200..1213), 282 items are 17 bytes and a trailing i32 follows.</li>
 *     <li>ANDROID_MODIFIED -> LP0452 equipment refresh with every slot bit. 265 always sends its full update (mask 0xFF,
 *     items, then time + appearance); 282 reads the appearance only with mask 0, so the equipment is sent (the
 *     appearance is the one of LP044F). The owner-only trailing u8 is always sent: the owner reads it, the other users
 *     stop one byte early (the only way one broadcast packet fits both).</li>
 *     <li>ANDROID_REMOVED -> LP0453, ANDROID_ACTION_SET -> LP0451, REMOTE_ANDROID_EMOTION -> LP0481: same bodies.</li>
 *     <li>ANDROID_MOVE is declined: the 282 route of CP02FE builds LP0450 from the 282 path itself
 *     ({@code Tms282AndroidHandlers}); a 265 move body has the 265 path encoding.</li>
 *     <li>MONSTER_BOOK_SET_CARD -> LP00F9 (5E28B30: {@code u8 added; [i32 cardId, i32 count]}, both branches traced
 *     CLEAN): the monster card (238xxxx, consumeOnPickup + monsterBook in 282 Item.wz) pick-up message.</li>
 *     <li>MONSTER_COLLECTION_RESULT -> LP01D7 (58AF770: {@code i32 type, i32, i32, i32 n, n x (str, i32)}): the 265
 *     three i32 and n = 0.</li>
 * </ul>
 */
public final class Tms282AndroidTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282AndroidTranslators INSTANCE = new Tms282AndroidTranslators();
    public static final int MONSTER_BOOK_SET_CARD = 0x00F9;
    public static final int MONSTER_COLLECTION_RESULT = 0x01D7;
    /** Bytes of one 265 android item: {@code i32 itemId, i32 look, u8}. */
    static final int GMS265_ITEM_BYTES = 9;

    /**
     * Whether androids are shown in the field. Off by default: every LP044F ever sent closed the 282 client with a
     * C++ exception (0xE06D7363) 0.5-2.7 s later (research/tms2823/android-trace/EQUIP-CRASH.md, live 09-27 / 09-28),
     * while a worn android without its field object is stable. Off: the android field packets (created / modified /
     * removed / action / emotion) are declined; the worn items, stats and android shop are unchanged.
     * {@code -Dtms282.android.visible=true} sends them again (to capture the crash with an exit dump).
     */
    public static final boolean ANDROID_VISIBLE = Boolean.getBoolean("tms282.android.visible")
            || "true".equalsIgnoreCase(net.swordie.ms.console.config.ServerRuntimeConfig.stringValue("tms282.android.visible").trim());

    private Tms282AndroidTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset charset = builder.charset();
        builder.on(OutHeader.ANDROID_CREATED, p -> ANDROID_VISIBLE ? created(p, charset) : Optional.empty())
                .on(OutHeader.ANDROID_MODIFIED, p -> ANDROID_VISIBLE ? modified(p) : Optional.empty())
                .on(OutHeader.ANDROID_REMOVED, p -> ANDROID_VISIBLE ? copy(p, Tms282AndroidPackets.ANDROID_REMOVED) : Optional.empty())
                .on(OutHeader.ANDROID_ACTION_SET, p -> ANDROID_VISIBLE ? copy(p, Tms282AndroidPackets.ANDROID_ACTION_SET) : Optional.empty())
                .on(OutHeader.REMOTE_ANDROID_EMOTION, p -> ANDROID_VISIBLE ? copy(p, Tms282AndroidPackets.REMOTE_ANDROID_EMOTION) : Optional.empty())
                .on(OutHeader.MONSTER_BOOK_SET_CARD, p -> copy(p, MONSTER_BOOK_SET_CARD))
                .on(OutHeader.MONSTER_COLLECTION_RESULT, Tms282AndroidTranslators::collectionResult);
    }

    static Optional<OutPacket> created(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int owner = body.i32();
        int template = body.i32();
        short x = (short) body.u16();
        short y = (short) body.u16();
        int moveAction = body.u8();
        short fh = (short) body.u16();
        body.i32();
        // 265 never sets the android's foothold / move action (0 / 0), and a warp re-creates the android at its last
        // position of the previous map. Place it at the owner's current position on the foothold below it (the owner's
        // own foothold is stale until its first move after a warp / login) with the owner's move action
        // (placeholder-audit-0929 / spawned-object audit).
        var ownerChar = owner(owner);
        if (ownerChar != null && ownerChar.getPosition() != null && ownerChar.getField() != null) {
            x = (short) ownerChar.getPosition().getX();
            y = (short) ownerChar.getPosition().getY();
            var below = ownerChar.getField().findFootHoldBelow(ownerChar.getPosition());
            if (below != null) {
                fh = (short) below.getId();
            }
            moveAction = ownerChar.getMoveAction() & 0xFF;
        }
        Tms282AndroidPackets.Appearance look = Tms282AndroidLook.full(template, appearance(body)); // 282 needs full hair/face ids
        List<Tms282AndroidPackets.SlotItem> items = items(body, body.remaining() / GMS265_ITEM_BYTES);
        body.end();
        return Optional.of(Tms282AndroidPackets.created(owner, template, x, y, moveAction, fh, look, items, charset));
    }

    /** The online owner, or null (tests / offline). */
    static net.swordie.ms.client.character.Char owner(int characterId) {
        try {
            return net.swordie.ms.Server.get().getChars().get(characterId);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    static Optional<OutPacket> modified(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int owner = body.i32();
        int mask = body.u16();
        if (mask != 0xFF) {
            return Optional.empty(); // only the full update 265 sends is known
        }
        List<Tms282AndroidPackets.SlotItem> items = items(body, Tms282AndroidPackets.SLOTS);
        body.bytes(8);
        appearance(body);
        body.end();
        return Optional.of(Tms282AndroidPackets.modifiedItems(owner, items, true));
    }

    static Optional<OutPacket> collectionResult(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.i32();
        int invType = body.i32();
        int fullSlots = body.i32();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(MONSTER_COLLECTION_RESULT);
        out.encodeInt(type);
        out.encodeInt(invType);
        out.encodeInt(fullSlots);
        out.encodeInt(0);
        return Optional.of(out);
    }

    /** 265 {@code Android.encodeAndroidInfo}: i16 skin, i32 hair, i32 face, str name, i32, FT, i32. */
    static Tms282AndroidPackets.Appearance appearance(Tms282LegacyPackets.Body body) {
        int skin = (short) body.u16();
        int hair = body.i32();
        int face = body.i32();
        String name = body.str();
        body.i32();
        body.bytes(8);
        body.i32();
        return new Tms282AndroidPackets.Appearance(skin, hair, face, name);
    }

    static List<Tms282AndroidPackets.SlotItem> items(Tms282LegacyPackets.Body body, int count) {
        List<Tms282AndroidPackets.SlotItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int itemId = body.i32();
            int look = body.i32();
            body.u8();
            if (i < Tms282AndroidPackets.SLOTS) {
                items.add(new Tms282AndroidPackets.SlotItem(itemId, look));
            }
        }
        return items;
    }

    static Optional<OutPacket> copy(OutPacket legacy, int opcode) {
        OutPacket out = Tms282LoginPacketSupport.packet(opcode);
        out.encodeArr(Tms282LegacyPackets.Body.of(legacy).rest());
        return Optional.of(out);
    }
}
