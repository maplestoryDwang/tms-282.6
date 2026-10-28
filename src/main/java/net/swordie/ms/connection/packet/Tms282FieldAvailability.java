package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.protocol.codec.FieldAvailability;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * TMS 282.3 field availability: the 3,405 maps of the 265 server data that the 282 client no longer has
 * ({@code data/protocol/tms282.3/fields-removed.tsv}, generated from packet-audit/data282-diff-map.tsv REMOVED rows).
 * A warp there is refused by {@code Char.warp}; the live crash that found it was 500000000 (Thailand Floating
 * Market) from the control panel's teleport menu, 2026-09-24.
 */
public final class Tms282FieldAvailability implements FieldAvailability {
    public static final Path REMOVED = Path.of("data", "protocol", "tms282.3", "fields-removed.tsv");

    private final Set<Integer> removed;

    public Tms282FieldAvailability(Set<Integer> removed) {
        this.removed = Set.copyOf(removed);
    }

    public static Tms282FieldAvailability load() {
        if (!Files.exists(REMOVED)) {
            System.out.println("[TMS282] " + REMOVED + " missing; every field is treated as available");
            return new Tms282FieldAvailability(Set.of());
        }
        try {
            Set<Integer> ids = new HashSet<>();
            for (String line : Files.readAllLines(REMOVED, StandardCharsets.UTF_8)) {
                String s = line.trim();
                if (!s.isEmpty() && !s.startsWith("#")) {
                    ids.add(Integer.parseInt(s));
                }
            }
            return new Tms282FieldAvailability(ids);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean isAvailable(int fieldId) {
        return !removed.contains(fieldId);
    }

    public int removedCount() {
        return removed.size();
    }
}
