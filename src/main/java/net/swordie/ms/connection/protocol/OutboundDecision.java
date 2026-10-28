package net.swordie.ms.connection.protocol;

/**
 * Verdict of an {@link OutboundGuard}.
 *
 * @param allowed whether the packet may reach the socket
 * @param reason  why it was dropped; empty when allowed
 */
public record OutboundDecision(boolean allowed, String reason) {
    private static final OutboundDecision ALLOW = new OutboundDecision(true, "");

    public static OutboundDecision allow() {
        return ALLOW;
    }

    public static OutboundDecision drop(String reason) {
        return new OutboundDecision(false, reason);
    }
}
