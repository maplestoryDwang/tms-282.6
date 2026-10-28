package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.protocol.codec.ItemAvailability;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * TMS 282.3 item availability: the 14,988 items/equips of the 265 server data that the 282 client no longer has
 * ({@code data/protocol/tms282.3/items-removed.tsv}, generated from packet-audit/data282-diff-item.tsv REMOVED rows).
 * Found live 2026-09-24: shop 9330606 listed 1202236, 2002026, 4034403, 4034405, 4034406 and 2070016; the client's
 * CShopDlg threw assertion 0x21000003 (CP00AE type 3) and disconnected.
 */
public final class Tms282ItemAvailability implements ItemAvailability {
    public static final Path REMOVED = Path.of("data", "protocol", "tms282.3", "items-removed.tsv");

    private final Set<Integer> removed;

    public Tms282ItemAvailability(Set<Integer> removed) {
        this.removed = Set.copyOf(removed);
    }

    public static Tms282ItemAvailability load() {
        if (!Files.exists(REMOVED)) {
            System.out.println("[TMS282] " + REMOVED + " missing; every item is treated as available");
            return new Tms282ItemAvailability(Set.of());
        }
        try {
            Set<Integer> ids = new HashSet<>();
            for (String line : Files.readAllLines(REMOVED, StandardCharsets.UTF_8)) {
                String s = line.trim();
                if (!s.isEmpty() && !s.startsWith("#")) {
                    ids.add(Integer.parseInt(s));
                }
            }
            return new Tms282ItemAvailability(ids);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean isAvailable(int itemId) {
        return !removed.contains(itemId);
    }

    public int removedCount() {
        return removed.size();
    }
}
