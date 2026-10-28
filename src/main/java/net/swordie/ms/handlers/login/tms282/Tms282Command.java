package net.swordie.ms.handlers.login.tms282;

/**
 * One TMS 282.3 client packet's action (command pattern; the research probe's ClientPacketHandler). It receives
 * the whole plaintext packet, opcode included, as the Tms282*Request parsers expect. Throwing ends the session,
 * exactly like an exception in a probe handler.
 */
@FunctionalInterface
public interface Tms282Command {
    void execute(Tms282LoginState session, int op, byte[] packet) throws Exception;
}
