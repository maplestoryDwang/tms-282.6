package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.regex.Pattern;

/** username + password with the rules the CLI has always applied; {@link #toString()} omits the password. */
public record AccountCredentials(String username, String password) {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9]{3,24}");
    private static final Pattern PASSWORD = Pattern.compile("[A-Za-z0-9]{8,32}");

    /** The request's credentials, or null when either field is missing or malformed ({@code invalidInput}). */
    public static AccountCredentials parse(JsonNode request) {
        String username = LauncherJson.text(request, "username");
        String password = LauncherJson.text(request, "password");
        if (!validUsername(username) || !PASSWORD.matcher(password).matches()) {
            return null;
        }
        return new AccountCredentials(username, password);
    }

    /**
     * ASCII letters and digits, 3-24 (CP0074 also refuses anything but [A-Za-z0-9]). users.name compares
     * accent-, width- and trailing-space-insensitively (utf8mb4_unicode_ci), so only this spelling may key per-account
     * state such as {@link LoginBackoff}.
     */
    public static boolean validUsername(String username) {
        return username != null && USERNAME.matcher(username).matches();
    }

    @Override
    public String toString() {
        return "AccountCredentials[username=" + username + "]";
    }
}
