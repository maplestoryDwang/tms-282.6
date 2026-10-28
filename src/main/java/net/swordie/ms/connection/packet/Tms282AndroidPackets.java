package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * TMS 282.3 android (機器人) packets. CUserPool::OnUserCommonPacket 5AA7B10 reads the owner's i32 characterId, finds
 * the user and sends 0x441..0x46B (past its jump table) to seven sub-dispatchers by range (5AA823D..5AA82E0); the
 * five-opcode range 0x44F..0x453 goes to 5484BA0, which is the android one: 0x44F allocates the CAndroid (0x1058 bytes,
 * kept at CUser+0x1D58) and decodes it with 1D08B90, whose appearance block is 8D9C20 (the same reader as the android
 * item's, 265 {@code Android.encodeAndroidInfo}); 0x453 drops it; 0x450..0x452 need an android. The 265 pet / dragon /
 * android / fox-man / skill-pet / second-atom groups have the sizes of these ranges in the same order (android = 5).
 * Layouts (after the characterId), traced with research/tms2823/android-trace (CLEAN):
 * <pre>
 * LP044F ANDROID_CREATED  i32 template, i16 x, i16 y, u8 moveAction, i16 fh, i32, appearance (8D9C20: i16 skin,
 *                         i32 hair, i32 face, str name, i32, 8 bytes, i32), 14 x item, i32
 *                         item (1D08C83..1D08CBF) = i32 itemId, i32, i32, u8, i32   (265: i32 itemId, i32, u8)
 * LP0450 ANDROID_MOVE     the move path (1D0B190 -> 13E2A50 -> 3FDD1D0, the reader of LP046D, identical reads)
 * LP0451 ANDROID_ACTION   u8 action, u8 randomKey (1D0B1E0, as 265)
 * LP0452 ANDROID_MODIFIED u16 mask; for each of the 14 slots whose bit is set an item (1D0B2B7..1D0B2F6); i32
 *                         (1D0B316 -> +0xF70); only when mask == 0: 8 bytes (1D0B4FE) + appearance (8D9C20);
 *                         then, for the owner only (5484E72 CUser vtbl+0x58), u8 (5484E82; non-zero refreshes a UI)
 * LP0453 ANDROID_REMOVED  nothing
 * LP0481 REMOTE_ANDROID_EMOTION i32 emotion, i32 duration (CUserPool::OnUserRemotePacket 5AA728B -> 545EBA0, as 265)
 * </pre>
 * The 14 slots are the 265 android body parts 1200..1213 (BodyPart.APBase..APEnd), in that order.
 */
public final class Tms282AndroidPackets {
    public static final int ANDROID_CREATED = 0x044F;
    public static final int ANDROID_MOVE = 0x0450;
    public static final int ANDROID_ACTION_SET = 0x0451;
    public static final int ANDROID_MODIFIED = 0x0452;
    public static final int ANDROID_REMOVED = 0x0453;
    public static final int REMOTE_ANDROID_EMOTION = 0x0481;
    /** Android equipment slots (265 BodyPart 1200..1213). */
    public static final int SLOTS = 14;
    /** LP0452 mask with every slot bit set (an equipment refresh; the appearance is only read with mask 0). */
    public static final int ALL_SLOTS_MASK = (1 << SLOTS) - 1;

    private Tms282AndroidPackets() {
    }

    /** The appearance block 8D9C20 (265 {@code Android.encodeAndroidInfo}): the 8 bytes are sent as zero. */
    public record Appearance(int skin, int hair, int face, String name) {
        public Appearance {
            Objects.requireNonNull(name, "name");
        }

        void encode(OutPacket out, Charset charset) {
            out.encodeShort(skin);
            out.encodeInt(hair);
            out.encodeInt(face);
            Tms282LoginPacketSupport.str(out, name, charset);
            out.encodeInt(0);
            out.encodeArr(new byte[8]);
            out.encodeInt(0);
        }
    }

    /** One android slot: the item id and the 265 "fusion anvil" look id (0 = none); the other 282 fields are 0. */
    public record SlotItem(int itemId, int lookItemId) {
        static final SlotItem EMPTY = new SlotItem(0, 0);

        void encode(OutPacket out) {
            out.encodeInt(itemId);
            out.encodeInt(lookItemId);
            out.encodeInt(0);
            out.encodeByte(0);
            out.encodeInt(0);
        }
    }

    /** LP044F: the owner's android appears (to the owner too). {@code items} holds up to {@value #SLOTS} slots. */
    public static OutPacket created(int ownerId, int template, int x, int y, int moveAction, int foothold,
                                    Appearance look, List<SlotItem> items, Charset charset) {
        Objects.requireNonNull(look, "look");
        OutPacket out = Tms282LoginPacketSupport.packet(ANDROID_CREATED);
        out.encodeInt(ownerId);
        out.encodeInt(template);
        out.encodeShort(x);
        out.encodeShort(y);
        out.encodeByte(moveAction);
        out.encodeShort(foothold);
        out.encodeInt(0);
        look.encode(out, charset);
        for (int i = 0; i < SLOTS; i++) {
            slot(items, i).encode(out);
        }
        out.encodeInt(0);
        // 1D08CE1 / 1D08D5C read two fields through the packet's archive stream (939890 -> 93C000), i.e. the AUX
        // region of an extended frame MAIN || AUX || u32 len(AUX), not MAIN (packet-audit/avatar-wire-layout-2823.md
        // "MAIN and AUX are separate wire regions"): u8 flag (+0x1038; non-zero adds 4 zigzag varints) and a byte
        // string (+0x1050: varint length 90F080 + bytes). Without AUX the archive read fails (ZException 0x26, caught
        // by the frame update and re-raised as fatal) - the android equip crash (android-trace/EQUIP-CRASH.md, 09-29).
        out.setAuxiliaryData(new byte[]{0, 0});
        return out;
    }

    /** LP0450: the android's move path (the LP046D path encoding). */
    public static OutPacket move(int ownerId, MovementInfo path) {
        return move(ownerId, path, Tms282MovePathCodec.INSTANCE);
    }

    public static OutPacket move(int ownerId, MovementInfo path, MovePathCodec paths) {
        Objects.requireNonNull(path, "path");
        OutPacket out = Tms282LoginPacketSupport.packet(ANDROID_MOVE);
        out.encodeInt(ownerId);
        paths.encodeBroadcast(out, path);
        return out;
    }

    /** LP0451. */
    public static OutPacket actionSet(int ownerId, int action, int randomKey) {
        OutPacket out = Tms282LoginPacketSupport.packet(ANDROID_ACTION_SET);
        out.encodeInt(ownerId);
        out.encodeByte(action);
        out.encodeByte(randomKey);
        return out;
    }

    /**
     * LP0452 equipment refresh: every slot (mask {@link #ALL_SLOTS_MASK}), then the i32. {@code forOwner} adds the
     * owner-only u8 (0); the other users of the field read no further than the i32.
     */
    public static OutPacket modifiedItems(int ownerId, List<SlotItem> items, boolean forOwner) {
        OutPacket out = Tms282LoginPacketSupport.packet(ANDROID_MODIFIED);
        out.encodeInt(ownerId);
        out.encodeShort(ALL_SLOTS_MASK);
        for (int i = 0; i < SLOTS; i++) {
            slot(items, i).encode(out);
        }
        out.encodeInt(0);
        if (forOwner) {
            out.encodeByte(0);
        }
        return out;
    }

    /** LP0452 appearance change (mask 0): the i32, 8 bytes, the appearance; {@code forOwner} as above. */
    public static OutPacket modifiedLook(int ownerId, Appearance look, boolean forOwner, Charset charset) {
        Objects.requireNonNull(look, "look");
        OutPacket out = Tms282LoginPacketSupport.packet(ANDROID_MODIFIED);
        out.encodeInt(ownerId);
        out.encodeShort(0);
        out.encodeInt(0);
        out.encodeArr(new byte[8]);
        look.encode(out, charset);
        if (forOwner) {
            out.encodeByte(0);
        }
        return out;
    }

    /** LP0453. */
    public static OutPacket removed(int ownerId) {
        OutPacket out = Tms282LoginPacketSupport.packet(ANDROID_REMOVED);
        out.encodeInt(ownerId);
        return out;
    }

    /** LP0481: the android of {@code ownerId} shows an emotion (to the other users of the field). */
    public static OutPacket remoteEmotion(int ownerId, int emotion, int duration) {
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_ANDROID_EMOTION);
        out.encodeInt(ownerId);
        out.encodeInt(emotion);
        out.encodeInt(duration);
        return out;
    }

    private static SlotItem slot(List<SlotItem> items, int i) {
        return items != null && i < items.size() && items.get(i) != null ? items.get(i) : SlotItem.EMPTY;
    }
}
