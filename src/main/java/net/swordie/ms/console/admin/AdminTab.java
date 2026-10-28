package net.swordie.ms.console.admin;

import javax.swing.JComponent;

/**
 * One tab of the admin console (the console keeps them in a list, like the server keeps its admin actions): its
 * title, its component, and how it loads its data through the {@link AdminConsoleContext}.
 */
interface AdminTab {

    String title();

    JComponent component();

    /** Loads (or reloads) the tab's data; called when the tab is shown, by the retry button and by auto refresh. */
    void refresh();

    /**
     * Edits not yet saved to the server. While true the console does not reload the tab by itself (tab switch,
     * reconnect); only the tab's own 「重新載入」 does, after a confirmation.
     */
    default boolean hasUnsavedChanges() {
        return false;
    }

    /** Refreshed every few seconds while visible (monitors yes; editors no, so an edit in progress is never replaced). */
    default boolean autoRefresh() {
        return false;
    }
}
