package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} for 265 VIDEO_BY_SCRIPT (UserLocal.videoByScript, ScriptManagerImpl
 * playVideoByScript), which the OutboundGuard drops today: the script then waits for a CP012C that never comes
 * (research/tms2823/stuck-scene-audit, 13 scripts, UI locked).
 * <p>
 * LP0506 (CUserLocal case 5788CCD, lp-map +56 shift, read shape 證實 statically): {@code str path, u8 muted} = the
 * 265 body; when the video cannot start the client answers at once (5788D35 -> 58A3D80 {@code 0}), when it ends
 * 58A3D20 -> 58A3D80 {@code 1}: CP012C wire type 21 ({@link Tms282ScriptDialog#PLAY_MOVIE}), parsed since this audit.
 * VIDEO_BY_SCRIPT_2 (265 {@code str} only) is not translated: 282 LP0507 (5788D45) reads {@code str, u8}.
 * <p>
 * NOT registered yet: add {@code .with(Tms282DirectionVideoTranslators.INSTANCE)} to the builder in
 * {@link Tms282FieldEncoders} (owned by another session), then mark LP0506 IMPLEMENTED_UNTESTED.
 */
public final class Tms282DirectionVideoTranslators implements Tms282LegacyPackets.Contributor {
    public static final int VIDEO_BY_SCRIPT = 0x0506;
    public static final Tms282DirectionVideoTranslators INSTANCE = new Tms282DirectionVideoTranslators();

    private Tms282DirectionVideoTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset charset = builder.charset();
        builder.on(OutHeader.VIDEO_BY_SCRIPT, p -> videoByScript(p, charset));
    }

    /** 265 {@code str path, u8 muted} -> LP0506 {@code str path (282 charset), u8 muted}. */
    public static Optional<OutPacket> videoByScript(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        String path = body.str();
        int muted = body.u8();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(VIDEO_BY_SCRIPT);
        Tms282LoginPacketSupport.str(out, path, charset);
        out.encodeByte(muted);
        return Optional.of(out);
    }
}
