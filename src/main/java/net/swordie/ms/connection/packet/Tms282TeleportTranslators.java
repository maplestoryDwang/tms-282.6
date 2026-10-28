package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.Position;

import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} for the 265 TELEPORT ({@code FieldPacket.teleport}), which the 282
 * OutboundGuard dropped (research/tms2823/lucid-audit-0930 §A: 327 DROPPED TELEPORT 09-23..29, 62 scripts via
 * ScriptManagerImpl.teleportToPortal / teleportInField, Char.java:8091, MigrationHandler, ItemHandler, BlazeWizard,
 * BeastTamer, AdminCommands, Tms282CashItemUse). One rule covers every caller.
 * <p>
 * 265 body {@code u8 exclRequest, u8 callingType, i32 characterId, i16 x, i16 y} (10 bytes) -> LP04CC
 * {@link Tms282FieldPackets#teleport(boolean, int, int, Position)} (the same fields + 282 {@code u8 0, i32 0};
 * CUserLocal::OnPacket 5788060, inline 578810E, [verified] in Tms282FieldPackets).
 */
public final class Tms282TeleportTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282TeleportTranslators INSTANCE = new Tms282TeleportTranslators();

    private Tms282TeleportTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.TELEPORT, Tms282TeleportTranslators::teleport);
    }

    /** 265 TELEPORT -> LP04CC; a body of any other size declines (the guard then drops it as before). */
    public static Optional<OutPacket> teleport(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        boolean exclRequest = body.u8() != 0;
        int callingType = body.u8();
        int characterId = body.i32();
        short x = (short) body.u16();
        short y = (short) body.u16();
        body.end();
        return Optional.of(Tms282FieldPackets.teleport(exclRequest, callingType, characterId, new Position(x, y)));
    }
}
