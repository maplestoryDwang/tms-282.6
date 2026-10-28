package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;

/**
 * The three LP0013 fields after the AvatarEntry list (burning-display-chars-2823.md 3.1/3.3, reader 3CA6A12..3CA6B2E):
 * u8 limitFlag (CLogin+768), i32 displayCount, i32 n, n x i32 displayed character ID (CLogin+750).
 * UNVERIFIED: whether the client uses displayCount or n as the shown count (+23C) is not proven;
 * {@link #only} passes the character-slot count, which may be wrong once limitFlag is 1.
 * limitFlag 0 shows every character; non-zero makes CharSelect show only the listed IDs. The golden capture
 * send-029-0013.bin is {@link #showAll(int)} with the character-slot count.
 */
public record Tms282DisplayedCharacters(int limitFlag, int displayCount, List<Integer> characterIds) {
    public Tms282DisplayedCharacters {
        checkByte(limitFlag, "Displayed-character limit flag");
        if (displayCount < 0) {
            throw new IllegalArgumentException("Display count must not be negative");
        }
        characterIds = List.copyOf(Objects.requireNonNull(characterIds, "characterIds"));
        if (new HashSet<>(characterIds).size() != characterIds.size()) {
            throw new IllegalArgumentException("Duplicate displayed character ID");
        }
    }

    /** The builder's default: limitFlag 0, no list (the value every LP0013 has sent so far). */
    public static Tms282DisplayedCharacters showAll(int displayCount) {
        return new Tms282DisplayedCharacters(0, displayCount, List.of());
    }

    /** limitFlag 1: CharSelect shows only these characters (the list a CP00EC stored). */
    public static Tms282DisplayedCharacters only(int displayCount, List<Integer> characterIds) {
        return new Tms282DisplayedCharacters(1, displayCount, characterIds);
    }

    void encode(OutPacket out) {
        out.encodeByte(limitFlag);
        out.encodeInt(displayCount);
        out.encodeInt(characterIds.size());
        for (int id : characterIds) {
            out.encodeInt(id);
        }
    }
}
