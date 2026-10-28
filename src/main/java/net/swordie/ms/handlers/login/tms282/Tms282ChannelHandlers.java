package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.netty.PacketHandler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.migration.MigrateInDecoder;
import net.swordie.ms.world.migration.MigrateInRequest;
import net.swordie.ms.world.migration.MigrationService;

import java.util.Objects;
import java.util.Set;

/**
 * Channel-socket packets of TMS 282.3 (probe channelHandlers(); channel-enter-2823.md): CP0070 (first CP of every
 * socket, a report), CP00AB pong, CP007E migrate-in (once per socket: {@link MigrateInDecoder#TMS_282_3} +
 * {@link MigrationService} with {@link Tms282FieldEntry}), and the in-field CPs that are only recorded.
 */
final class Tms282ChannelHandlers {
    /**
     * CPs the client sends after SetField (channel-enter-2823.md 3.2): 0206/0207 one-byte settings, 02B1
     * integrity values, 04CF/035B/021D conditional. Recorded only; no reply is known to be needed. CP01F4
     * PARTY_INVITABLE_SET is handled by Tms282SocialHandlers since multiplayer-0930 (recorded only, every party
     * invitation was refused).
     */
    static final Set<Integer> FIELD_RECORD_ONLY_OPS = Set.of(0x206, 0x207, 0x2b1, 0x4cf, 0x35b, 0x21d);

    private final Tms282LoginContext context;
    private final Tms282ReportHandlers reports;

    Tms282ChannelHandlers(Tms282LoginContext context, Tms282ReportHandlers reports) {
        this.context = Objects.requireNonNull(context, "context");
        this.reports = Objects.requireNonNull(reports, "reports");
    }

    void register(Tms282Routes routes) {
        routes.channel(0x70, InHeader.CP0070).handle("report (no reply)", reports::report);
        routes.channel(0xab, InHeader.PONG).handle("pong", (s, op, p) -> s.onPong());
        routes.channel(0x7e, InHeader.MIGRATE_IN).require(Tms282Guards.AWAITING_MIGRATE_IN, "Second CP007E on one channel socket")
                .handleWith("migrate in -> LP02EF SetField", migrateIn());
        for (int op : FIELD_RECORD_ONLY_OPS) {
            routes.channel(op, fieldHeader(op)).when(Tms282Guards.IN_FIELD).handle("recorded after SetField (no reply)",
                    (s, o, p) -> System.out.printf("[TMS282] CHANNEL %04X len=%d recorded after SetField (no reply)%n", o, p.length));
        }
    }

    /**
     * CP007E: marks the socket (one migrate-in only), decodes the 282 layout and enters through the shared
     * MigrationService. A malformed CP007E or one without an unexpired matching ticket ends the session.
     */
    private PacketHandler migrateIn() {
        Tms282FieldEntry entry = new Tms282FieldEntry(context.settings().nameCharset());
        return (c, chr, in) -> {
            Tms282LoginState session = Tms282LoginState.of(c);
            session.markMigrateInSeen();
            MigrateInRequest request;
            try {
                request = MigrateInDecoder.TMS_282_3.decode(in);
            } catch (IllegalArgumentException e) {
                consume(in);
                session.terminate("Malformed CP007E (" + e.getMessage() + ")");
                return;
            }
            System.out.println("[TMS282] CHANNEL MIGRATE-IN " + request);
            try {
                new MigrationService(context.migrations()).enter(c, request, entry);
            } catch (RuntimeException e) {
                session.terminate("CP007E entry failed: " + e);
                return;
            }
            if (!net.swordie.ms.scripts.ScriptManagerImpl.GAME_SCRIPTS_ENABLED) return;
            // Login notices (also on channel change); never allowed to fail the entry.
            net.swordie.ms.world.tms282.mail.MailLoginHook.onFirstFieldEntry(c.getChr()); // mail: unread memos / parcels
            try {
                net.swordie.ms.world.shop.cashshop.CashShopGifts.notifyOnLogin(c.getChr()); // cash shop: pending gifts
            } catch (RuntimeException e) {
                System.out.println("[TMS282] cash shop gift notice failed: " + e);
            }
        };
    }

    private static void consume(InPacket in) {
        Tms282Routes.consume(in);
    }

    /** 01F4 has a 265 name in the status table (PARTY_INVITABLE_SET); the others use their CPxxxx constant. */
    static InHeader fieldHeader(int op) {
        return op == 0x1f4 ? InHeader.PARTY_INVITABLE_SET : InHeader.valueOf(String.format("CP%04X", op));
    }
}
