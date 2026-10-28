package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282HexaMatrixRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.UserHandler;
import net.swordie.ms.util.DataPrinter;

import java.util.Arrays;
import java.util.function.Function;

/**
 * HEXA matrix (6th job) requests of TMS 282.3 on the channel socket, after LP02EF ({@code when(IN_FIELD)}). Both keep
 * the 265 layout ({@link Tms282HexaMatrixRequest}) and go to the unchanged 265 handlers; their 265 replies
 * (HEXA_MESSAGE / SKILL_UPDATE / STAT_UPDATE / ERDA_CONVERSION, skill records, EXCL_REQUEST) reach the client through
 * the profile's legacy translators ({@link net.swordie.ms.connection.packet.Tms282HexaMatrixTranslators}).
 * <ul>
 *     <li>CP0490 HEXA_MATRIX_OPREATION -> 265 {@link UserHandler#handleHexaMatrixOperationRequest};</li>
 *     <li>CP0491 HEXA_ERDA_CONVERSION -> 265 {@link UserHandler#handleErdaConversion}.</li>
 * </ul>
 * Each request ends with LP009D unlock (the window locks the client when it sends); a request that does not parse
 * or a failure of the 265 logic is logged, neither ends the session.
 */
final class Tms282HexaMatrixHandlers {

    void register(Tms282Routes routes) {
        routes.channel(Tms282HexaMatrixRequest.OPERATION, InHeader.HEXA_MATRIX_OPREATION).when(Tms282Guards.IN_FIELD)
                .handle("HEXA matrix operation -> 265 HEXA handler (LP0271..LP0274) + LP009D unlock",
                        (s, op, p) -> delegate(s, op, p, Tms282HexaMatrixRequest::parseOperation,
                                (chr, in) -> UserHandler.handleHexaMatrixOperationRequest(chr, in)));
        routes.channel(Tms282HexaMatrixRequest.ERDA_CONVERSION, InHeader.HEXA_ERDA_CONVERSION).when(Tms282Guards.IN_FIELD)
                .handle("HEXA erda conversion -> 265 handler (LP0277) + LP009D unlock",
                        (s, op, p) -> delegate(s, op, p, Tms282HexaMatrixRequest::parseErdaConversion,
                                (chr, in) -> UserHandler.handleErdaConversion(chr, in)));
    }

    /** The 265 handler for one request body. */
    @FunctionalInterface
    interface Gms265Handler {
        void handle(Char chr, InPacket in);
    }

    private static void delegate(Tms282LoginState session, int op, byte[] packet,
                                 Function<byte[], Tms282HexaMatrixRequest> parser, Gms265Handler handler) {
        Tms282HexaMatrixRequest request;
        try {
            request = parser.apply(packet);
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REJECTED CP%04X len=%d: %s (%s)%n", op, packet.length, e.getMessage(),
                    packet.length <= 48 ? Arrays.toString(packet) : "");
            session.send(Tms282StatPackets.unlock());
            return;
        }
        Char chr = session.fieldCharacter();
        System.out.println("[TMS282] " + request + " character=" + chr.getId() + " -> 265");
        try {
            handler.handle(chr, new InPacket(request.toGms265Body()));
        } catch (RuntimeException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n", op, chr.getId(), e);
        }
        session.send(Tms282StatPackets.unlock());
    }
}
