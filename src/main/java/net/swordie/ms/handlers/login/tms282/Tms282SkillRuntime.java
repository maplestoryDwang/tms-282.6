package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.SkillStatBuffResolver;
import net.swordie.ms.client.character.tms282.Tms282CharacterStore;
import net.swordie.ms.client.character.tms282.Tms282LevelService;
import net.swordie.ms.client.character.tms282.Tms282SkillService;
import net.swordie.ms.connection.packet.Tms282CtsTable;
import net.swordie.ms.connection.packet.Tms282RemoteCtsTable;
import net.swordie.ms.connection.packet.Tms282RemoteStatEncoder;
import net.swordie.ms.connection.packet.Tms282TemporaryStatEncoder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The skill agent's TMS 282.3 skill and level services for the formal overlay (the research probe's {@code Skills}
 * holder): {@link Tms282SkillService} over the 282 CTS table ({@link Tms282TemporaryStatEncoder}), the
 * {@link SkillStatBuffResolver}, a {@link Tms282CharacterStore} and a millisecond clock; and
 * {@link Tms282LevelService} for the admin chat commands. The skill service is built on first use, so a missing CTS
 * table cannot break startup (the first CP0196/CP04B4 then gets its unlock and a log line, as in the probe).
 */
public final class Tms282SkillRuntime {
    /** The probe's CTS table location (relative to server/265). */
    public static final Path DEFAULT_CTS_DIR = Path.of("data", "protocol", "tms282.3");

    private final Supplier<Tms282SkillService> service;
    private final Tms282LevelService levels;
    private final LongSupplier clock;
    private volatile Tms282SkillService built;

    private Tms282SkillRuntime(Supplier<Tms282SkillService> service, Tms282LevelService levels, LongSupplier clock) {
        this.service = Objects.requireNonNull(service, "service");
        this.levels = Objects.requireNonNull(levels, "levels");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The server's: CTS table from {@link #DEFAULT_CTS_DIR}, the 265 database, wall-clock milliseconds. */
    public static Tms282SkillRuntime standard() {
        return lazy(DEFAULT_CTS_DIR, Tms282CharacterStore.SQL, System::currentTimeMillis);
    }

    /** Skill service built from the CTS table in {@code ctsDir} on first use. */
    public static Tms282SkillRuntime lazy(Path ctsDir, Tms282CharacterStore store, LongSupplier clock) {
        Objects.requireNonNull(ctsDir, "ctsDir");
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(clock, "clock");
        return new Tms282SkillRuntime(() -> {
            try {
                Tms282CtsTable table = Tms282CtsTable.load(ctsDir);
                Tms282RemoteCtsTable remote = Tms282RemoteCtsTable.load(ctsDir);
                System.out.println("[TMS282] LOADED 282 CTS table " + table.summary() + ", remote " + remote.summary());
                return new Tms282SkillService(new Tms282TemporaryStatEncoder(table), new Tms282RemoteStatEncoder(remote),
                        new SkillStatBuffResolver(), store, clock);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, new Tms282LevelService(store), clock);
    }

    /** Ready services (checks). {@code clock} must be the one {@code service} uses. */
    public static Tms282SkillRuntime of(Tms282SkillService service, Tms282LevelService levels, LongSupplier clock) {
        Objects.requireNonNull(service, "service");
        return new Tms282SkillRuntime(() -> service, levels, clock);
    }

    /** The skill service; throws when the CTS table cannot be loaded (retried on the next call). */
    public Tms282SkillService service() {
        Tms282SkillService s = built;
        if (s == null) {
            synchronized (this) {
                if (built == null) {
                    built = Objects.requireNonNull(service.get(), "skill service");
                }
                s = built;
            }
        }
        return s;
    }

    public Tms282LevelService levels() {
        return levels;
    }

    /** Milliseconds, the clock the skill service stamps buffs and cooldowns with. */
    public LongSupplier clock() {
        return clock;
    }
}
