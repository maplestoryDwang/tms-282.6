package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.util.Util;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Password / second-password hashing and recovery codes, unchanged from LocalAccountCli (09-22): BCrypt with
 * {@link ServerConstants#BCRYPT_ITERATIONS}; recovery code = 20 random bytes as 40 upper-case hex characters, stored
 * as SHA-256, shown as 5 groups of 8 joined by '-'.
 */
public final class AccountSecrets {
    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountSecrets() {
    }

    /** Made once per process, at the first unknown account: a hash that no password matches in practice. */
    private static final class Decoy {
        static final String HASH = BCrypt.hashpw(Long.toHexString(RANDOM.nextLong()) + Long.toHexString(RANDOM.nextLong()),
                BCrypt.gensalt(ServerConstants.BCRYPT_ITERATIONS));
    }

    static boolean matches(String value, String hash) {
        try {
            return hash != null && BCrypt.checkpw(value, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * A login password against the account's stored hash, where {@code hash} is null for an unknown account. Unknown
     * accounts and non-BCrypt hashes still cost one BCrypt check (against a decoy), so the reply time does not tell
     * whether the name exists. Never true without a BCrypt match.
     */
    public static boolean verifyPassword(String password, String hash) {
        if (hash == null || !Util.isStringBCrypt(hash)) {
            matches(password == null ? "" : password, Decoy.HASH);
            return false;
        }
        return matches(password, hash);
    }

    /** The same one BCrypt check as {@link #verifyPassword} for a path that compares otherwise (legacy plain text). */
    public static void spendVerifyTime(String password) {
        matches(password == null ? "" : password, Decoy.HASH);
    }

    static String hash(String value) {
        return BCrypt.hashpw(value, BCrypt.gensalt(ServerConstants.BCRYPT_ITERATIONS));
    }

    static String newRecoveryCode() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    /** User input -> 40 hex characters (dashes / spaces dropped, upper case); may still be malformed. */
    static String normalizeRecovery(String input) {
        return input.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    static boolean wellFormedRecovery(String normalized) {
        return normalized.matches("[0-9A-F]{40}");
    }

    static byte[] digest(String code) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String displayRecovery(String code) {
        return String.join("-", code.split("(?<=\\G.{8})"));
    }
}
