package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} of the cash shop phase: the 265 packets its flows build directly.
 * <ul>
 *     <li>MIGRATE_COMMAND -> LP001F {@code u8 1, 4 bytes IPv4, u16 port}. 265 {@code Client.migrateOut} (leaving the
 *     cash shop) and {@code auctionHouseOut} send it one second after the request. Handler 34744C0 (no direct caller,
 *     dispatched from the protected ProcessPacket): Decode1 34744EC, Decode4 34746A8 (address, written as-is into the
 *     sockaddr), Decode2 34746B9 (port, 347C4D0 htons at 3474779) and then 5DC7A00 -> 346ACD0, the same connect as
 *     LP0014 SELECT_CHARACTER_RESULT (enter-game-2823.md 2.2). Traced CLEAN with exactly these 7 bytes; the 265 body
 *     has an i32 more, which the 282 reader leaves unread [layout 證實]. Opcode 0x1F: 265 MIGRATE_COMMAND is 0x1F and
 *     the neighbours are unchanged in 282 (CREATE_NEW_CHARACTER_RESULT 0x19, DELETE_CHARACTER_RESULT 0x1A, ALIVE_REQ
 *     0x20 all live-verified) [推定]. A refused migrate (u8 0) is sent as that byte alone.</li>
 * </ul>
 */
public final class Tms282CashShopTranslators implements Tms282LegacyPackets.Contributor {
    public static final int MIGRATE_COMMAND = 0x001F;

    public static final Tms282CashShopTranslators INSTANCE = new Tms282CashShopTranslators();

    private Tms282CashShopTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.MIGRATE_COMMAND, Tms282CashShopTranslators::migrateCommand);
    }

    static Optional<OutPacket> migrateCommand(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int succeed = body.u8();
        if (succeed == 0) {
            body.end();
            return Optional.of(migrateCommand(null, 0));
        }
        byte[] address = body.bytes(4);
        int port = body.u16();
        body.i32();
        body.end();
        return Optional.of(migrateCommand(address, port));
    }

    /** LP001F to {@code address}:{@code port}; a null address is the refusal (u8 0). */
    public static OutPacket migrateCommand(byte[] address, int port) {
        OutPacket out = Tms282LoginPacketSupport.packet(MIGRATE_COMMAND);
        if (address == null) {
            out.encodeByte(0);
            return out;
        }
        if (address.length != 4 || port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("IPv4 address and u16 port expected");
        }
        out.encodeByte(1);                                  // 34744EC
        out.encodeArr(address);                             // 34746A8
        out.encodeShort(port);                              // 34746B9
        return out;
    }
}
