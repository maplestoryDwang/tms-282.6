package net.swordie.ms.client.character.keys;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.Tms282KeyPackets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/** One captured creation mistake, gated by identity, unchanged old keys and a persistent single-attempt marker. */
public final class Tms282CreationKeyMigration {
    public static final Tms282CreationKeyRepair.Target TARGET = new Tms282CreationKeyRepair.Target(9, 13, 55, 36, 1);
    private final Path marker;
    private final BiConsumer<Char, List<Tms282KeyMapService.Applied>> save;
    private final Predicate<Char> verify;

    public Tms282CreationKeyMigration(Path directory) {
        this(directory, Tms282CreationKeyMigration::saveSql, Tms282CreationKeyMigration::verifySql);
    }

    /** Persistence callbacks also permit an offline lifecycle rehearsal without opening SQL connections. */
    public Tms282CreationKeyMigration(Path directory, BiConsumer<Char, List<Tms282KeyMapService.Applied>> save,
                                      Predicate<Char> verify) {
        marker = directory.resolve("user9-account13-world55-char36-basic-v1.txt");
        this.save = save;
        this.verify = verify;
    }

    public synchronized boolean repairIfUnchanged(Char chr) throws IOException {
        if (Files.exists(marker)) return false;
        var plan = Tms282CreationKeyRepair.plan(chr, TARGET).orElse(null);
        if (plan == null) return false;
        Files.createDirectories(marker.getParent());
        // Claim before writes. A crash/partial failure leaves pending and requires review, never a blind retry.
        Files.writeString(marker, "pending\n", StandardOpenOption.CREATE_NEW);
        List<Tms282KeyMapService.Applied> applied = new ArrayList<>();
        for (var changes : plan.presets()) applied.add(Tms282KeyMapService.apply(chr, changes));
        chr.setQuickslotKeys(new ArrayList<>(plan.quickSlots()));
        save.accept(chr, applied);
        if (!verify.test(chr)) throw new IOException("Creation keyboard repair readback mismatch; marker left pending");
        Files.writeString(marker, "applied\n", StandardOpenOption.TRUNCATE_EXISTING);
        return true;
    }

    private static void saveSql(Char chr, List<Tms282KeyMapService.Applied> applied) {
        for (var preset : applied) Tms282KeyMapService.save(preset);
        chr.updateCharFieldToSQL("quickslotKeys", chr.getQuickSlotKeysForUpdate());
    }

    private static boolean verifySql(Char chr) {
        // The legacy save APIs can swallow SQL errors, so only mark applied after an independent readback.
        var saved = FuncKeyMap.getFuncKeyMapsFromSQLByCharID(chr.getId());
        if (!Arrays.equals(Tms282KeyPackets.funcKeyMappedInit(saved, chr.getQuickslotKeys()).getData(),
                Tms282KeyPackets.funcKeyMappedInit(chr.getFuncKeyMaps(), chr.getQuickslotKeys()).getData())) return false;
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement("SELECT quickslotKeys FROM characters WHERE id = ? AND accid = ?")) {
            statement.setInt(1, chr.getId());
            statement.setInt(2, chr.getAccId());
            try (var result = statement.executeQuery()) {
                return result.next() && chr.getQuickSlotKeysForUpdate().equals(result.getString(1));
            }
        } catch (java.sql.SQLException e) { throw new IllegalStateException("Creation keyboard repair readback failed", e); }
    }
}
