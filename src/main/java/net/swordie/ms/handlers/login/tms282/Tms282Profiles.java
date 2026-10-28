package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.tms282.Tms282ExpGain;
import net.swordie.ms.connection.protocol.codec.AttackRequestDecoder;
import net.swordie.ms.connection.protocol.codec.DropPacketEncoder;
import net.swordie.ms.connection.protocol.codec.ExpGainStrategy;
import net.swordie.ms.connection.protocol.codec.Tms282AttackDecoder;
import net.swordie.ms.connection.protocol.codec.Tms282AttackLayout;
import net.swordie.ms.connection.protocol.codec.Tms282DropPackets;
import net.swordie.ms.connection.protocol.OpcodeTable;
import net.swordie.ms.connection.protocol.ProfileCodecs;
import net.swordie.ms.connection.protocol.Tms2823Profile;
import net.swordie.ms.connection.protocol.Tms282HandshakeStrategy;
import net.swordie.ms.connection.protocol.Tms282OpcodeTables;
import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import net.swordie.ms.connection.protocol.codec.MobStatCodec;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MobPackets;
import net.swordie.ms.connection.protocol.codec.Tms282MobStatCodec;
import net.swordie.ms.connection.protocol.codec.Tms282MovePathCodec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory of the phase-1 TMS 282.3 profile (what {@code protocol.version=tms282.3} starts): the skeleton table
 * plus the opcodes {@link Tms282LoginOverlay} declares, the overlay registry, and a handshake whose session start
 * attaches {@link Tms282LoginState} (keep-alive included), and the 282 codec families ({@link #codecs()}).
 */
public final class Tms282Profiles {
    /**
     * Codec families of the 282 profile (phase 3: move paths, MobStat, mob packets). The GMS 265 profile registers
     * none; 265 call sites fall back to the Gms265 codecs ({@code find(X).orElse(Gms265...INSTANCE)}, e.g.
     * {@link MobPacketEncoder#active()}).
     */
    private static final ProfileCodecs CODECS = ProfileCodecs.builder()
            .add(MovePathCodec.class, Tms282MovePathCodec.INSTANCE)
            .add(MobStatCodec.class, Tms282MobStatCodec.INSTANCE)
            .add(MobPacketEncoder.class, Tms282MobPackets.INSTANCE)
            .add(AttackRequestDecoder.class, new Tms282AttackDecoder(Tms282AttackLayout.STANDARD))
            .add(DropPacketEncoder.class, Tms282DropPackets.INSTANCE)
            .add(ExpGainStrategy.class, Tms282ExpGain.standard())
            .build();

    private Tms282Profiles() {
    }

    /** Settings from config/server.properties and the server's migration registry. */
    public static Tms2823Profile standard(Path dataDir) {
        return create(dataDir, Tms282LoginContext.standard(Tms282LoginSettings.load()));
    }

    /** The 282 codec families (immutable, shared by every 282 profile instance). */
    public static ProfileCodecs codecs() {
        return CODECS;
    }

    public static Tms2823Profile create(Path dataDir, Tms282LoginContext context) {
        Objects.requireNonNull(dataDir, "dataDir");
        Tms282LoginOverlay overlay = new Tms282LoginOverlay(context);
        OpcodeTable table;
        try {
            OpcodeTable.Builder builder = Tms282OpcodeTables.builder(dataDir, Tms2823Profile.PHASE0_INBOUND,
                    Tms2823Profile.PHASE0_OUTBOUND, overlay::declaredHeader);
            overlay.declareOpcodes(builder);
            table = builder.build();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load TMS 282.3 opcode tables from " + dataDir, e);
        }
        boolean channelPing = context.settings().channelPing();
        Tms282HandshakeStrategy handshake = new Tms282HandshakeStrategy(
                (client, kind) -> Tms282LoginState.attach(client, kind, channelPing));
        return new Tms2823Profile(table, overlay.registry(table), codecs(), handshake);
    }
}
