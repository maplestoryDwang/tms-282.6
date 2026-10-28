package net.swordie.ms.connection.protocol.capture;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Send-side counterpart of {@link CaptureRedaction}. Server packets use the LP opcode space, where
 * {@code SensitiveClientOpcodes} does not apply (same rule as the probe and {@code Tms282PacketTrace}), so no
 * opcode is redacted by number; instead every sent packet is searched for the account's secrets before it is
 * stored or traced. A hit is stored like a sensitive received packet: opcode kept, body zeroed.
 * <p>
 * Searched (main and auxiliary bytes): a bcrypt hash marker ({@code $2a$}, {@code $2b$}, {@code $2y$}) and the
 * account password and SPW/PIC exactly as the user record holds them (hash or plaintext), in ASCII/UTF-8.
 * Values shorter than {@value #MIN_SECRET_LENGTH} characters are not searched (too many false hits).
 */
@FunctionalInterface
public interface SendSecretScan {
    int MIN_SECRET_LENGTH = 4;
    List<String> HASH_MARKERS = List.of("$2a$", "$2b$", "$2y$");

    /** True when {@code main} or {@code auxiliary} contains a secret. */
    boolean leaks(byte[] main, byte[] auxiliary);

    /** Scan for the hash markers only (no account known). */
    SendSecretScan HASH_MARKERS_ONLY = secrets(List::of);

    /**
     * Scans for the hash markers plus the current values of {@code secrets} (evaluated per packet, off the socket
     * thread; the supplier must tolerate a missing account and never block).
     */
    static SendSecretScan secrets(Supplier<? extends Collection<String>> secrets) {
        Objects.requireNonNull(secrets, "secrets");
        return (main, auxiliary) -> {
            List<byte[]> needles = new ArrayList<>(HASH_MARKERS.size() + 2);
            for (String marker : HASH_MARKERS) {
                needles.add(marker.getBytes(StandardCharsets.US_ASCII));
            }
            Collection<String> values;
            try {
                values = secrets.get();
            } catch (RuntimeException e) {
                values = List.of();
            }
            if (values != null) {
                for (String value : values) {
                    if (value != null && value.length() >= MIN_SECRET_LENGTH) {
                        needles.add(value.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            for (byte[] needle : needles) {
                if (contains(main, needle) || auxiliary != null && contains(auxiliary, needle)) {
                    return true;
                }
            }
            return false;
        };
    }

    static boolean contains(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0, last = haystack.length - needle.length; i <= last; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
