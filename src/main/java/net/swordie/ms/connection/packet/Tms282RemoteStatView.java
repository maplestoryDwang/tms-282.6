package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The remote CTS block LP03D0 #24 carries for a character (what the users already in the field, or the one entering
 * it, see of that character's buffs): the character's active stats ({@link Tms282RemoteStatSource}) through
 * {@link Tms282RemoteStatEncoder}. The encoder's table is loaded on first use; when it cannot be loaded, or nothing
 * is sendable, the block is the empty one (184 bytes), so LP03D0 itself never fails because of buffs.
 */
public final class Tms282RemoteStatView {
    /** Always the empty block (checks, and profiles without the 282 skill service). */
    public static final Tms282RemoteStatView EMPTY = new Tms282RemoteStatView(Tms282RemoteStatSource.NONE, () -> null,
            System::currentTimeMillis);

    private final Tms282RemoteStatSource source;
    private final Supplier<Tms282RemoteStatEncoder> encoder;
    private final LongSupplier clock;
    private volatile Tms282RemoteStatEncoder built;
    private volatile boolean failed;

    public Tms282RemoteStatView(Tms282RemoteStatSource source, Supplier<Tms282RemoteStatEncoder> encoder,
                                LongSupplier clock) {
        this.source = Objects.requireNonNull(source, "source");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** {@code source} with the table in {@code ctsDir} (normally data/protocol/tms282.3), wall-clock time. */
    public static Tms282RemoteStatView of(Tms282RemoteStatSource source, Path ctsDir) {
        Objects.requireNonNull(ctsDir, "ctsDir");
        return new Tms282RemoteStatView(source, () -> {
            try {
                return new Tms282RemoteStatEncoder(Tms282RemoteCtsTable.load(ctsDir));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, System::currentTimeMillis);
    }

    /** Writes the block for {@code characterId}. */
    public void encode(OutPacket out, int characterId) {
        Map<CharacterTemporaryStat, List<Option>> stats = source.activeStats(characterId);
        Tms282RemoteStatEncoder enc = stats.isEmpty() ? null : encoder();
        if (enc == null) {
            Tms282RemoteStatEncoder.encodeEmpty(out);
            return;
        }
        enc.encodeForRemote(out, enc.select(stats), clock.getAsLong());
    }

    private Tms282RemoteStatEncoder encoder() {
        Tms282RemoteStatEncoder e = built;
        if (e != null || failed) {
            return e;
        }
        synchronized (this) {
            if (built == null && !failed) {
                try {
                    built = encoder.get();
                } catch (RuntimeException ex) {
                    failed = true;
                    System.out.println("[TMS282] remote CTS table not loaded, LP03D0 carries no buffs: " + ex);
                }
            }
            return built;
        }
    }
}
