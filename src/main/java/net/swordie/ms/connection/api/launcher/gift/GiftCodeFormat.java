package net.swordie.ms.connection.api.launcher.gift;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Gift code text (P7): 16 characters of a 32-letter alphabet without 0 / O / 1 / I (80 random bits), shown as
 * XXXX-XXXX-XXXX-XXXX. Input is normalized: dashes and spaces dropped, upper case. Anything else is not a code.
 */
public final class GiftCodeFormat {
    public static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    public static final int LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GiftCodeFormat() {
    }

    public static String newCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** The normalized code, or null when {@code input} cannot be one. */
    public static String normalize(String input) {
        if (input == null || input.length() > 64) {
            return null;
        }
        String s = input.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (s.length() != LENGTH) {
            return null;
        }
        for (int i = 0; i < s.length(); i++) {
            if (ALPHABET.indexOf(s.charAt(i)) < 0) {
                return null;
            }
        }
        return s;
    }

    public static String display(String normalized) {
        return normalized.substring(0, 4) + "-" + normalized.substring(4, 8) + "-" + normalized.substring(8, 12) + "-" + normalized.substring(12);
    }
}
