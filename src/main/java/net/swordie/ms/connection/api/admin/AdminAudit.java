package net.swordie.ms.connection.api.admin;

/**
 * One line per admin write in the server log ({@code launcher\formal-server.log}): action, source, parameter summary
 * (never a password or token) and the result code.
 */
public final class AdminAudit {
    private AdminAudit() {
    }

    public static void write(String action, String sourceIp, String summary, String code) {
        System.out.printf("[AdminApi] audit %s from %s%s -> %s%n", action, sourceIp,
                summary == null || summary.isEmpty() ? "" : ": " + clean(summary), code);
    }

    /** One line, at most 300 characters, control characters replaced. */
    static String clean(String s) {
        String t = s.replaceAll("\\p{Cntrl}", " ");
        return t.length() > 300 ? t.substring(0, 300) + "..." : t;
    }
}
