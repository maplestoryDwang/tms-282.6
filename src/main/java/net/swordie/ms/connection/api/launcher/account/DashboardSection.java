package net.swordie.ms.connection.api.launcher.account;

import net.swordie.ms.connection.api.launcher.LauncherResult;

import java.util.List;

/**
 * One part of the {@code dashboard} answer (composite: {@link DashboardAction} runs its sections in order on one
 * connection). A required section that fails fails the whole dashboard; any other section that fails only sets its
 * {@link #fields()} to null and is listed in {@code partial}, so e.g. an older database without a later table still
 * shows the characters. Later phases add sections (daily status, news version) without touching the others.
 */
public interface DashboardSection {

    /** The name listed in {@code partial} when the section fails. */
    String name();

    /** Failure (exception or refusal) fails the whole dashboard instead of being listed in {@code partial}. */
    boolean required();

    /** The result fields it writes; set to null when it fails. */
    List<String> fields();

    /**
     * Writes its fields into {@code out}. Answers null, or a refusal: from a required section the dashboard answers
     * the refusal itself (e.g. {@code sessionExpired} when the account row is gone); from another it counts as a
     * failure.
     */
    LauncherResult contribute(DashboardContext dc, LauncherResult out) throws Exception;
}
