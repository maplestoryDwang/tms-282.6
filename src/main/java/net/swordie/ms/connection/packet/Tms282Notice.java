package net.swordie.ms.connection.packet;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.handlers.login.tms282.Tms282LoginSettings;
import net.swordie.ms.util.HotProperties;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 全服公告 (2026-10-05 notice-1005): one place that turns an announcement text into the packet(s) the 282 client
 * shows, used by the maintenance countdown, {@code @notice} and the admin action {@code notice.send}.
 * <p>
 * Kinds (research/tms2823/notice-1005/README.md has the evidence):
 * <ul>
 *   <li>{@link Kind#CHAT} (default) LP04F0 CHAT_MSG {@code u16 11 (SystemNotice), str}: <b>LIVE</b> - the same packet
 *   as {@code Char.chatMessage(String)}/{@code chatPopup}; shown in the chat log (PROJECT-HISTORY 09-23 「系統訊息有顯示」,
 *   boss-entry-feedback-0925) and accepted in field in the formal log (formal-channel-1791201000054-154: type 11
 *   「你獲得了 1,000 榮譽經驗值。」 then 2802 more client packets).</li>
 *   <li>{@link Kind#TIP} LP04F0 type 7 (Tip): LIVE in field - the online-reward 「你今天已獲得 100 點免費 MVP 點數。」
 *   (formal-channel-1791192047817-4120 and three more sessions, client kept playing).</li>
 *   <li>{@link Kind#NOTICE} LP00E1 BROADCAST_MSG type 0 {@code u8 0, str} and {@link Kind#SLIDE} type 4 (scrolling
 *   header) {@code u8 4, u8 1, str}: static only - CWvsContext::OnPacket 5DF5B20 jump table (op-0x8F @5DFB140) sends
 *   0xE1 to 5EFB900, which reads u8 type, a u8 flag for types 4/26, a string unless type is 13/14/27/29 (bitmask
 *   0x28006000) = the 265 BroadcastMsg layout; both fixtures traced CLEAN. Never sent to a live client yet.</li>
 *   <li>{@link Kind#WEATHER} LP010C WEATHER_EFFECT_NOTICE {@code str, i32 type, i32 ms, u8 1}: sent live twice (Lucid
 *   entry, type 138) without a client problem; whether the art fits a general notice is not checked.</li>
 * </ul>
 * Only CHAT is proven to display; the others are GM probes ({@code @notice --kind=slide ...}). Text rules
 * ({@link #validate}): stripped, 1..{@value #DEFAULT_MAX_BYTES} bytes in MS950 ({@value #KEY_MAX_BYTES}), no control
 * characters, every character encodable in MS950 (the TW client's string charset), so nothing turns into '?'.
 */
public final class Tms282Notice {
    /** Comma list of the kinds used when a caller names none (hot reload); default {@code chat}. */
    public static final String KEY_KINDS = "tms282.notice.kinds";
    public static final String KEY_MAX_BYTES = "tms282.notice.maxBytes";
    public static final String KEY_WEATHER_TYPE = "tms282.notice.weatherType";
    public static final String KEY_WEATHER_MS = "tms282.notice.weatherMs";
    public static final int DEFAULT_MAX_BYTES = 200;
    public static final int MAX_BYTES_LIMIT = 400;
    public static final Charset MS950 = Charset.forName("MS950");

    public static final int LP_BROADCAST_MSG = 0x00E1;

    /** One way of showing a notice; {@link #proven()} = seen working on a live 282 client. */
    public enum Kind {
        CHAT("chat", true, "LP04F0 type 11"),
        TIP("tip", true, "LP04F0 type 7"),
        NOTICE("notice", false, "LP00E1 type 0"),
        SLIDE("slide", false, "LP00E1 type 4"),
        WEATHER("weather", false, "LP010C");

        private final String id;
        private final boolean proven;
        private final String wire;

        Kind(String id, boolean proven, String wire) {
            this.id = id;
            this.proven = proven;
            this.wire = wire;
        }

        public String id() {
            return id;
        }

        public boolean proven() {
            return proven;
        }

        public String wire() {
            return wire;
        }

        public static Optional<Kind> of(String id) {
            String s = id == null ? "" : id.strip().toLowerCase(Locale.ROOT);
            for (Kind k : values()) {
                if (k.id.equals(s)) {
                    return Optional.of(k);
                }
            }
            return Optional.empty();
        }
    }

    /** {@link #validate} result: the stripped text, or why it was refused ({@code empty}, {@code tooLong}, {@code control}, {@code charset}). */
    public record Checked(String text, String problem, int bytes) {
        public boolean ok() {
            return problem == null;
        }
    }

    private Tms282Notice() {
    }

    public static int maxBytes() {
        return HotProperties.server().integer(KEY_MAX_BYTES, DEFAULT_MAX_BYTES, 1, MAX_BYTES_LIMIT);
    }

    public static Checked validate(String raw) {
        return validate(raw, maxBytes());
    }

    public static Checked validate(String raw, int maxBytes) {
        String text = raw == null ? "" : raw.strip();
        if (text.isEmpty()) {
            return new Checked(text, "empty", 0);
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c) || Character.getType(c) == Character.FORMAT || Character.isSurrogate(c)) {
                return new Checked(text, "control", 0);
            }
        }
        CharsetEncoder enc = MS950.newEncoder();
        for (int i = 0; i < text.length(); i++) {
            if (!enc.canEncode(text.charAt(i))) {
                return new Checked(text, "charset", 0);
            }
        }
        int bytes = text.getBytes(MS950).length;
        if (bytes > maxBytes) {
            return new Checked(text, "tooLong", bytes);
        }
        return new Checked(text, null, bytes);
    }

    /**
     * Kinds from a comma list; unknown names are reported in {@code unknown} (when not null) and skipped; an empty
     * result falls back to {@link Kind#CHAT}.
     */
    public static List<Kind> parseKinds(String list, List<String> unknown) {
        Set<Kind> out = new LinkedHashSet<>();
        if (list != null) {
            for (String part : list.split(",")) {
                if (part.isBlank()) {
                    continue;
                }
                Optional<Kind> k = Kind.of(part);
                if (k.isPresent()) {
                    out.add(k.get());
                } else if (unknown != null) {
                    unknown.add(part.strip());
                }
            }
        }
        if (out.isEmpty()) {
            out.add(Kind.CHAT);
        }
        return List.copyOf(out);
    }

    /** The configured default kinds ({@value #KEY_KINDS}, default chat). */
    public static List<Kind> defaultKinds() {
        return configuredKinds(KEY_KINDS);
    }

    /** Kinds from {@code key}, falling back to {@value #KEY_KINDS}, then chat. */
    public static List<Kind> configuredKinds(String key) {
        HotProperties p = HotProperties.server();
        String v = p.string(key, "").strip();
        if (v.isEmpty() && !KEY_KINDS.equals(key)) {
            v = p.string(KEY_KINDS, "").strip();
        }
        List<String> unknown = new ArrayList<>();
        List<Kind> kinds = parseKinds(v, unknown);
        if (!unknown.isEmpty()) {
            System.out.println("[Notice] " + key + ": unknown kinds " + unknown + " ignored");
        }
        return kinds;
    }

    /** The packet of {@code kind} for the active protocol (265 builders on GMS 265, profile packets on 282). */
    public static OutPacket packet(Kind kind, String text) {
        boolean tms = Protocol.active().version() == ProtocolVersion.TMS_282_3;
        return packet(kind, text, tms, tms ? Tms282LoginSettings.load().nameCharset() : null);
    }

    /**
     * {@code tms282}: the 282 wire for {@code charset} (MS950 on the TW client); otherwise the 265 builder. LP04F0 and
     * LP010C stay 265 builders on 282 too - the LegacyPacketTranslator turns them into the 282 bodies on the way out,
     * exactly as for every message players already see.
     */
    public static OutPacket packet(Kind kind, String text, boolean tms282, Charset charset) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(text, "text");
        switch (kind) {
            case CHAT:
                return UserLocal.chatMsg(ChatType.SystemNotice, text);
            case TIP:
                return UserLocal.chatMsg(ChatType.Tip, text);
            case WEATHER: {
                OutPacket out = new OutPacket(net.swordie.ms.handlers.header.OutHeader.WEATHER_EFFECT_NOTICE);
                out.encodeString(text);
                HotProperties p = HotProperties.server();
                out.encodeInt(p.integer(KEY_WEATHER_TYPE, 138, 0, 10_000));
                out.encodeInt(p.integer(KEY_WEATHER_MS, 8000, 1000, 60_000));
                out.encodeByte(1);
                return out;
            }
            case NOTICE:
            case SLIDE: {
                if (!tms282) {
                    return WvsContext.broadcastMsg(kind == Kind.NOTICE ? BroadcastMsg.notice(text) : BroadcastMsg.slideNotice(text, true));
                }
                OutPacket out = Tms282LoginPacketSupport.packet(LP_BROADCAST_MSG);
                out.encodeByte(kind == Kind.NOTICE ? 0 : 4);
                if (kind == Kind.SLIDE) {
                    out.encodeByte(1);
                }
                Tms282LoginPacketSupport.str(out, text, Objects.requireNonNull(charset, "charset"));
                return out;
            }
            default:
                throw new IllegalArgumentException(kind.toString());
        }
    }

    /** Sends {@code text} (already {@link #validate validated}) to every character in a field, once per kind; returns the characters online. */
    public static int broadcast(List<Kind> kinds, String text) {
        for (Kind k : kinds) {
            try {
                Server.get().broadcastForWorld(packet(k, text));
            } catch (RuntimeException e) {
                System.out.println("[Notice] " + k.id() + " failed: " + e);
            }
        }
        int online = Server.get().getChars().size();
        System.out.println("[Notice] " + kinds.stream().map(Kind::id).toList() + " to " + online + " online: " + text);
        return online;
    }
}
