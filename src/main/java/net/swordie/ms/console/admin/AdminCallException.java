package net.swordie.ms.console.admin;

/** A failed admin call, with a Traditional Chinese message for the console. */
public final class AdminCallException extends Exception {
    private static final long serialVersionUID = 1L;

    public enum Kind {
        /** Nothing listens on 127.0.0.1:port (server not started, or stopped). */
        SERVER_DOWN,
        /** admin.token missing / unreadable, or the server refused it (restarted: a new token). */
        TOKEN,
        /** 403: source / Host / Origin refused. */
        FORBIDDEN,
        /** Anything else: timeout, malformed answer, HTTP error. */
        OTHER
    }

    private final Kind kind;

    public AdminCallException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public AdminCallException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
