package net.swordie.ms.handlers.dispatch;

import net.swordie.ms.connection.netty.PacketHandler;
import net.swordie.ms.connection.protocol.InboundOpcode;
import net.swordie.ms.connection.protocol.ProtocolProfile;
import net.swordie.ms.connection.protocol.WireFormat;
import net.swordie.ms.handlers.header.InHeader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable InHeader -> {@link PacketHandler} dispatch table (command pattern: one handler object per packet).
 * <p>
 * A registry is built once ({@link #builder}); registering a header twice in one builder is a programming
 * error and throws. A profile's overlay is layered over the base with {@link #withOverlay}: overlay entries
 * win and are tagged {@link HandlerLayer#OVERLAY}, which is what {@link #validate} relies on.
 */
public final class HandlerRegistry {
    private static final int SIZE = InHeader.values().length;

    private final String name;
    private final PacketHandler[] handlers;
    private final HandlerLayer[] layers;

    private HandlerRegistry(String name, PacketHandler[] handlers, HandlerLayer[] layers) {
        this.name = name;
        this.handlers = handlers;
        this.layers = layers;
    }

    /** Builder whose entries count as {@link HandlerLayer#BASE} (the shared 265 handlers). */
    public static Builder builder(String name) {
        return new Builder(name, HandlerLayer.BASE);
    }

    /** Builder whose entries count as {@link HandlerLayer#OVERLAY} (profile overlays). */
    public static Builder overlayBuilder(String name) {
        return new Builder(name, HandlerLayer.OVERLAY);
    }

    public static HandlerRegistry empty(String name) {
        return overlayBuilder(name).build();
    }

    public String name() {
        return name;
    }

    /** Handler for a header, or null. Hot path. */
    public PacketHandler get(InHeader header) {
        return handlers[header.ordinal()];
    }

    public HandlerLayer layerOf(InHeader header) {
        return layers[header.ordinal()];
    }

    public boolean isEmpty() {
        for (PacketHandler h : handlers) {
            if (h != null) {
                return false;
            }
        }
        return true;
    }

    /** Headers with a handler, in enum order. */
    public Set<InHeader> registeredHeaders() {
        Set<InHeader> set = EnumSet.noneOf(InHeader.class);
        for (InHeader h : InHeader.values()) {
            if (handlers[h.ordinal()] != null) {
                set.add(h);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    /** This registry with {@code overlay}'s entries on top (tagged OVERLAY); an empty overlay returns this. */
    public HandlerRegistry withOverlay(HandlerRegistry overlay) {
        Objects.requireNonNull(overlay, "overlay");
        if (overlay.isEmpty()) {
            return this;
        }
        PacketHandler[] merged = handlers.clone();
        HandlerLayer[] mergedLayers = layers.clone();
        for (int i = 0; i < SIZE; i++) {
            if (overlay.handlers[i] != null) {
                merged[i] = overlay.handlers[i];
                mergedLayers[i] = HandlerLayer.OVERLAY;
            }
        }
        return new HandlerRegistry(name + "+" + overlay.name, merged, mergedLayers);
    }

    /**
     * Problems that make {@code profile} unsafe to serve with this registry: every inbound
     * {@link WireFormat#PROFILE_CODEC} opcode that still routes to a {@link HandlerLayer#BASE} (265) handler,
     * which would parse a body it does not understand.
     */
    public List<String> violations(ProtocolProfile profile) {
        List<String> out = new ArrayList<>();
        for (InboundOpcode entry : profile.opcodes().inboundOpcodes()) {
            InHeader header = entry.header();
            if (entry.format() == WireFormat.PROFILE_CODEC && header != null && layerOf(header) == HandlerLayer.BASE) {
                out.add(entry + " routes to the 265 base handler of " + header);
            }
        }
        return out;
    }

    /**
     * Inbound opcodes whose body format is {@link WireFormat#UNKNOWN} but that route to a 265 base handler.
     * Not fatal (the layout may well match), but each one is an unproven parse; startup prints the count.
     */
    public List<String> unprovenRoutes(ProtocolProfile profile) {
        List<String> out = new ArrayList<>();
        for (InboundOpcode entry : profile.opcodes().inboundOpcodes()) {
            InHeader header = entry.header();
            if (entry.format() == WireFormat.UNKNOWN && header != null && layerOf(header) == HandlerLayer.BASE) {
                out.add(entry + " -> 265 " + header);
            }
        }
        return out;
    }

    /** @throws IllegalStateException listing every {@link #violations} entry; the server must not start */
    public void validate(ProtocolProfile profile) {
        List<String> problems = violations(profile);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Handler registry " + name + " is unsafe for " + profile.version().id()
                    + " (" + problems.size() + " PROFILE_CODEC opcodes without an overlay handler):\n  "
                    + String.join("\n  ", problems));
        }
    }

    @Override
    public String toString() {
        return "HandlerRegistry[" + name + ", " + registeredHeaders().size() + " handlers]";
    }

    /** Collects handlers; each header at most once. */
    public static final class Builder {
        private final String name;
        private final HandlerLayer layer;
        private final PacketHandler[] handlers = new PacketHandler[SIZE];
        private boolean built;

        private Builder(String name, HandlerLayer layer) {
            this.name = Objects.requireNonNull(name, "name");
            this.layer = layer;
        }

        /** @throws IllegalStateException when {@code header} already has a handler in this builder */
        public Builder register(InHeader header, PacketHandler handler) {
            Objects.requireNonNull(header, "header");
            Objects.requireNonNull(handler, "handler");
            if (built) {
                throw new IllegalStateException("Registry " + name + " is already built");
            }
            if (handlers[header.ordinal()] != null) {
                throw new IllegalStateException("Duplicate handler for " + header + " in registry " + name);
            }
            handlers[header.ordinal()] = handler;
            return this;
        }

        /** Registers one handler for several headers. */
        public Builder registerAll(PacketHandler handler, InHeader... headers) {
            for (InHeader header : headers) {
                register(header, handler);
            }
            return this;
        }

        public HandlerRegistry build() {
            built = true;
            HandlerLayer[] layers = new HandlerLayer[SIZE];
            Arrays.fill(layers, HandlerLayer.NONE);
            for (int i = 0; i < SIZE; i++) {
                if (handlers[i] != null) {
                    layers[i] = layer;
                }
            }
            return new HandlerRegistry(name, handlers.clone(), layers);
        }
    }
}
