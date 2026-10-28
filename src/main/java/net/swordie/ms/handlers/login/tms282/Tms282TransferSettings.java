package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.protocol.ProtocolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.OptionalInt;
import java.util.Properties;

/**
 * Settings of the TMS 282.3 field-transfer handlers, from {@code config/server.properties}; a JVM {@code -D}
 * property with the same key wins.
 * <ul>
 *     <li>{@code tms282.transferFieldOp}: the client opcode of USER_TRANSFER_FIELD_REQUEST (walking through a
 *     portal), e.g. {@code 0xFA}. Empty by default: the opcode is built inside the client's VM and not known yet,
 *     so no handler is registered until a capture shows it (see {@link Tms282TransferHandlers}).</li>
 * </ul>
 *
 * @param transferFieldOp the configured opcode, empty when not set
 */
public record Tms282TransferSettings(OptionalInt transferFieldOp) {
    public static final String KEY_TRANSFER_FIELD_OP = "tms282.transferFieldOp";
    public static final Tms282TransferSettings NONE = new Tms282TransferSettings(OptionalInt.empty());

    public Tms282TransferSettings {
        if (transferFieldOp.isPresent() && (transferFieldOp.getAsInt() <= 0 || transferFieldOp.getAsInt() > 0xFFFF)) {
            throw new IllegalArgumentException(KEY_TRANSFER_FIELD_OP + " out of range: " + transferFieldOp.getAsInt());
        }
    }

    /** Reads {@link ProtocolConfig#CONFIG_FILE} (when present) plus system properties. */
    public static Tms282TransferSettings load() {
        Properties p = new Properties();
        if (Files.isRegularFile(ProtocolConfig.CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + ProtocolConfig.CONFIG_FILE, e);
            }
        }
        return fromProperties(p);
    }

    public static Tms282TransferSettings fromProperties(Properties file) {
        String v = System.getProperty(KEY_TRANSFER_FIELD_OP);
        if (v == null) {
            v = file.getProperty(KEY_TRANSFER_FIELD_OP);
        }
        return new Tms282TransferSettings(parseOpcode(v));
    }

    /** Hex with a 0x prefix ({@code 0xFA}) or decimal ({@code 250}); blank means not set. */
    static OptionalInt parseOpcode(String v) {
        if (v == null || v.isBlank()) {
            return OptionalInt.empty();
        }
        String s = v.trim();
        try {
            return OptionalInt.of(s.startsWith("0x") || s.startsWith("0X") ? Integer.parseInt(s.substring(2), 16)
                    : Integer.parseInt(s));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(KEY_TRANSFER_FIELD_OP + " is not an opcode: " + v, e);
        }
    }

    public static Tms282TransferSettings withOpcode(int op) {
        return new Tms282TransferSettings(OptionalInt.of(op));
    }
}
