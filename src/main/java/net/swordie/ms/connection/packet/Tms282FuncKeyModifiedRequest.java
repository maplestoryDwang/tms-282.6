package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CP020F FUNC_KEY_MAPPED_MODIFIED (265 InHeader 516), sent only by the KeyConfig code (senders 3AE9B1B, 3AE9E72,
 * 3AEBCF1, 3AEBD81, 3AECF03, 3AED608, 3AEEDF1). Evidence: research/tms2823/packet-audit/ingame-layouts-282.md 4.3.
 * <pre>
 * type  sender   fields after the u8 type
 * 0     3AE9D70  u8 preset (0..3), i32 x (normally -1), u8 count, count x (u8 idx, u8 type, i32 val);
 *                idx 0..88 are keys, 102..111 the combination slots (slot + 100)            [verified]
 * 1     3AEBCC0  i32 pet HP potion item ID                                                     [verified]
 * 2     3AEBD50  i32 pet MP potion item ID                                                     [verified]
 * 3     3AECEC0  u8 active preset (282 may send 3; 265 only accepts 0..2)                      [verified]
 * 4     3AED5E0  u8 unionkey                                                                   [verified]
 * 5     3AE98F0  u8 count, u8 flag, count x (u8 idx 0..88, u8 type, i32 val),
 *                flag != 0: 32 x i32 (source array ending at 9EE8A20)                          [PRESUMED]
 * 6     3AEEDC0  i32 pet cure potion item ID                                                   [verified]
 * </pre>
 * Type 5 has no 265 counterpart (265 drops it in the switch default); its read order is from the sender, its
 * meaning is not known. The parser is strict: any other type, an out-of-range index or preset, or trailing bytes
 * are rejected, so a layout mistake shows up instead of being silently absorbed.
 */
public record Tms282FuncKeyModifiedRequest(int updateType, Payload payload) {
    public static final int OPCODE = 0x020F;
    public static final int KEY_COUNT = 89;
    public static final int FIRST_COMBINATION_INDEX = 102;
    public static final int LAST_COMBINATION_INDEX = 111;
    public static final int PRESET_COUNT = 4;
    public static final int QUICK_SLOT_COUNT = 32;

    public static final int TYPE_KEY_CHANGES = 0;
    public static final int TYPE_PET_HP_ITEM = 1;
    public static final int TYPE_PET_MP_ITEM = 2;
    public static final int TYPE_ACTIVE_PRESET = 3;
    public static final int TYPE_UNION_KEY = 4;
    /** [presumed] */
    public static final int TYPE_ALTERNATE_KEY_CHANGES = 5;
    public static final int TYPE_PET_CURE_ITEM = 6;

    public Tms282FuncKeyModifiedRequest {
        Objects.requireNonNull(payload, "payload");
        boolean matches = switch (updateType) {
            case TYPE_KEY_CHANGES -> payload instanceof KeyChanges;
            case TYPE_PET_HP_ITEM, TYPE_PET_MP_ITEM, TYPE_PET_CURE_ITEM -> payload instanceof ItemId;
            case TYPE_ACTIVE_PRESET -> payload instanceof ActivePreset;
            case TYPE_UNION_KEY -> payload instanceof UnionKey;
            case TYPE_ALTERNATE_KEY_CHANGES -> payload instanceof AlternateKeyChanges;
            default -> false;
        };
        if (!matches) {
            throw new IllegalArgumentException("CP020F type " + updateType + " does not carry " + payload.getClass().getSimpleName());
        }
    }

    /** Body of one update type. */
    public sealed interface Payload permits KeyChanges, ItemId, ActivePreset, UnionKey, AlternateKeyChanges {
    }

    /** One (u8 idx, u8 type, i32 val) entry; type is the FuncKeyMapped type (1 skill, 2 item, 4 menu, ...). */
    public record KeyChange(int index, int type, int value) {
        public KeyChange {
            if (index < 0 || index > 255 || type < 0 || type > 255) {
                throw new IllegalArgumentException("Key index and type must fit one byte");
            }
        }

        public boolean isCombination() {
            return index >= FIRST_COMBINATION_INDEX && index <= LAST_COMBINATION_INDEX;
        }
    }

    /** Type 0: keys of one preset that differ from the client's last-sent snapshot. */
    public record KeyChanges(int preset, int unknown, List<KeyChange> changes) implements Payload {
        public KeyChanges {
            if (preset < 0 || preset >= PRESET_COUNT) {
                throw new IllegalArgumentException("Preset must be between 0 and 3");
            }
            changes = List.copyOf(changes);
            for (KeyChange change : changes) {
                if (change.index() >= KEY_COUNT && !change.isCombination()) {
                    throw new IllegalArgumentException("Key index " + change.index() + " is neither 0..88 nor 102..111");
                }
            }
        }
    }

    /** Types 1, 2, 6: the pet auto-potion item ID (0 or negative clears it). */
    public record ItemId(int itemId) implements Payload {
    }

    /** Type 3: the active preset. */
    public record ActivePreset(int preset) implements Payload {
        public ActivePreset {
            if (preset < 0 || preset >= PRESET_COUNT) {
                throw new IllegalArgumentException("Preset must be between 0 and 3");
            }
        }
    }

    /** Type 4: the unionkey byte (265 stores it next to the preset in QR KEY_BINDINGS_PRESET). */
    public record UnionKey(int value) implements Payload {
    }

    /**
     * Type 5 [PRESUMED]: key differences (idx 0..88 only) of an array the client keeps apart from the presets, and,
     * when the flag is set, 32 i32 values (presumed a second quick-slot table). {@code quickSlots} is empty when
     * the flag is 0.
     */
    public record AlternateKeyChanges(int flag, List<KeyChange> changes, int[] quickSlots) implements Payload {
        public AlternateKeyChanges {
            changes = List.copyOf(changes);
            for (KeyChange change : changes) {
                if (change.index() >= KEY_COUNT) {
                    throw new IllegalArgumentException("Type 5 key index must be between 0 and 88");
                }
            }
            quickSlots = Objects.requireNonNull(quickSlots, "quickSlots").clone();
            if (quickSlots.length != (flag != 0 ? QUICK_SLOT_COUNT : 0)) {
                throw new IllegalArgumentException("Type 5 quick slots must be 32 values when the flag is set, else none");
            }
        }

        @Override
        public int[] quickSlots() {
            return quickSlots.clone();
        }
    }

    /** Accepts the full plaintext packet, including the opcode. */
    public static Tms282FuncKeyModifiedRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong CP020F opcode");
            }
            int type = u8(in);
            Payload payload = switch (type) {
                case TYPE_KEY_CHANGES -> {
                    int preset = u8(in);
                    int unknown = in.getInt();
                    yield new KeyChanges(preset, unknown, readChanges(in, u8(in)));
                }
                case TYPE_PET_HP_ITEM, TYPE_PET_MP_ITEM, TYPE_PET_CURE_ITEM -> new ItemId(in.getInt());
                case TYPE_ACTIVE_PRESET -> new ActivePreset(u8(in));
                case TYPE_UNION_KEY -> new UnionKey(u8(in));
                case TYPE_ALTERNATE_KEY_CHANGES -> {
                    int count = u8(in);
                    int flag = u8(in);
                    List<KeyChange> changes = readChanges(in, count);
                    int[] quickSlots = new int[flag != 0 ? QUICK_SLOT_COUNT : 0];
                    for (int i = 0; i < quickSlots.length; i++) {
                        quickSlots[i] = in.getInt();
                    }
                    yield new AlternateKeyChanges(flag, changes, quickSlots);
                }
                default -> throw new IllegalArgumentException("Unknown CP020F type " + type);
            };
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("CP020F type " + type + " has " + in.remaining() + " trailing bytes");
            }
            return new Tms282FuncKeyModifiedRequest(type, payload);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP020F", e);
        }
    }

    public boolean isPresumed() {
        return updateType == TYPE_ALTERNATE_KEY_CHANGES;
    }

    private static List<KeyChange> readChanges(ByteBuffer in, int count) {
        List<KeyChange> changes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int index = u8(in);
            int type = u8(in);
            changes.add(new KeyChange(index, type, in.getInt()));
        }
        return changes;
    }

    private static int u8(ByteBuffer in) {
        return Byte.toUnsignedInt(in.get());
    }
}
