package net.swordie.ms.connection.api.admin;

/** The {@code code} values of admin results (besides the launcher's {@code ok}, {@code invalidInput}, {@code unavailable}). */
public final class AdminCodes {
    public static final String OK = "ok";
    public static final String INVALID_INPUT = "invalidInput";
    public static final String UNAVAILABLE = "unavailable";
    /** No such account / character. */
    public static final String NOT_FOUND = "notFound";
    /** The file changed since the console read it (baseSha mismatch): reload and try again. */
    public static final String CONFLICT = "conflict";
    /** A value failed validation; {@code problem} says which. */
    public static final String REJECTED = "rejected";
    /** Protocol refusals (non-200 status). */
    public static final String FORBIDDEN = "forbidden";
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String UNSUPPORTED_MEDIA_TYPE = "unsupportedMediaType";
    public static final String TOO_LARGE = "tooLarge";
    public static final String RATE_LIMITED = "rateLimited";
    /** server.shutdown: the online count changed since the console asked; confirm again. */
    public static final String ONLINE_CHANGED = "onlineChanged";
    /** server.shutdown: a stop request is already pending / the shutdown already started. */
    public static final String ALREADY_STOPPING = "alreadyStopping";
    /** server.shutdown: no stop-file watcher runs in this process (nothing would read the file). */
    public static final String NO_STOP_WATCHER = "noStopWatcher";
    /** server.cancelShutdown: no maintenance countdown is running. */
    public static final String NO_COUNTDOWN = "noCountdown";
    /** release.save: minSupported would go up; save again with confirmRaise once every friend has the new launcher. */
    public static final String CONFIRM_REQUIRED = "confirmRequired";

    private AdminCodes() {
    }
}
