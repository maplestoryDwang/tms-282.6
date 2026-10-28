package net.swordie.ms.connection.protocol.codec;

import java.util.Locale;

/**
 * Where the next per-mob block of a TMS 282.3 attack request starts (strategy). Each block begins with
 * {@code u32 mobObjectId, 5 x u8, u32 templateId} [verified], so a block can be recognised by an object id of the
 * attacker's field followed 9 bytes later by that mob's template id: an anchor that does not depend on the
 * [presumed] parts before it (bonus block, skill extras, the previous mob's sub_5956260).
 */
public enum Tms282MobLocator {
    /** Trust the static layout: the block starts where the previous section ended. */
    SEQUENTIAL {
        @Override
        public Located locate(byte[] body, int expected, boolean expectedReliable, int searchFrom, MobDirectory mobs) {
            return expectedReliable && expected >= 0 && expected + BLOCK_ANCHOR <= body.length
                    ? new Located(expected, "sequential") : Located.NONE;
        }
    },
    /**
     * The static position when its object id and template match a field mob; otherwise the first such anchor after
     * {@code searchFrom}. Missing field mobs are skipped, never guessed from a template alone: the presumed auxiliary
     * layout can stop four bytes before a real block, turning its preceding checksum into an object id and damage.
     */
    ANCHORED {
        @Override
        public Located locate(byte[] body, int expected, boolean expectedReliable, int searchFrom, MobDirectory mobs) {
            if (expectedReliable && isAnchor(body, expected, mobs)) {
                return new Located(expected, "sequential, anchored");
            }
            for (int at = Math.max(0, searchFrom); at + BLOCK_ANCHOR <= body.length; at++) {
                if (isAnchor(body, at, mobs)) {
                    return new Located(at, expectedReliable ? "anchor scan (sequential guess " + expected + ")" : "anchor scan");
                }
            }
            return Located.NONE;
        }
    };

    /** Bytes needed to test an anchor: u32 id, 5 bytes, u32 template. */
    public static final int BLOCK_ANCHOR = 13;

    /**
     * @param body             the request body (no opcode)
     * @param expected         where the static layout says the block starts (-1 when it could not get there)
     * @param expectedReliable the static layout read everything before {@code expected} without failing
     * @param searchFrom       first offset an anchor scan may test
     * @return the block's offset, or {@link Located#NONE}
     */
    public abstract Located locate(byte[] body, int expected, boolean expectedReliable, int searchFrom, MobDirectory mobs);

    static boolean isAnchor(byte[] body, int at, MobDirectory mobs) {
        if (at < 0 || at + BLOCK_ANCHOR > body.length) {
            return false;
        }
        int objectId = intAt(body, at);
        int template = mobs.templateOf(objectId);
        return template > 0 && template == intAt(body, at + 9);
    }

    static int intAt(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    /** {@code anchored} (default) or {@code sequential}. */
    public static Tms282MobLocator parse(String value) {
        return value == null || value.isBlank() ? ANCHORED : valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /** The mobs of the attacker's field. */
    @FunctionalInterface
    public interface MobDirectory {
        /** Template id of the mob with {@code objectId}, or -1 when the field has no such mob. */
        int templateOf(int objectId);

        /** Whether {@code templateId} is a mob template (tells a block of a mob that already left from noise). */
        default boolean isKnownTemplate(int templateId) {
            return false;
        }

        MobDirectory NONE = objectId -> -1;
    }

    /** A located block. {@code offset} &lt; 0 = not found. */
    public record Located(int offset, String how) {
        public static final Located NONE = new Located(-1, "not found");

        public boolean found() {
            return offset >= 0;
        }

        Located withNote(String note) {
            return found() ? new Located(offset, note) : this;
        }
    }
}
