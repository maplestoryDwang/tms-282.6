package net.swordie.ms.connection.api.launcher;

/**
 * Where a {@link LauncherAction} may run.
 * <ul>
 *     <li>{@link #ANY}: over HTTP, or through {@code LocalAccountCli} when 8600 cannot be reached (server down).</li>
 *     <li>{@link #SERVER_WHEN_RUNNING}: while the game server runs, only through its process (127.0.0.1:8600): its
 *     in-memory Account / User / DailyGift caches would overwrite a direct DB write (reset: a User held from
 *     character select on saves its password back on disconnect). The CLI refuses with {@code serverRunning} when
 *     127.0.0.1:8484 is listening.</li>
 *     <li>{@link #SERVER_ONLY}: needs server memory (sessions, online users); the CLI always refuses with
 *     {@code serverOnly}.</li>
 * </ul>
 */
public enum LauncherTransport {
    ANY,
    SERVER_WHEN_RUNNING,
    SERVER_ONLY
}
