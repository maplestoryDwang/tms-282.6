package net.swordie.ms.handlers.dispatch;

/** Where a dispatch entry comes from. */
public enum HandlerLayer {
    /** No handler: the packet is ignored. */
    NONE,
    /** The shared GMS 265 handler (PacketProcessor). */
    BASE,
    /** A protocol profile's replacement or addition. */
    OVERLAY
}
