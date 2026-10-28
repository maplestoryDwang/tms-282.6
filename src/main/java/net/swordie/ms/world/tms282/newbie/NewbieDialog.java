package net.swordie.ms.world.tms282.newbie;

/**
 * The NPC dialog the newbie flow talks through. Only the SCRIPT_MESSAGE (LP0960) kinds whose 282 layouts are traced
 * and used live: say (next / ok), yes-no and menu - never an in-game direction, a cut scene or a lock of the UI.
 * The real one is {@link ScriptDialog} (the character's ScriptManagerImpl, answered through CP012C); a closed dialog,
 * a logout or a stopped script ends the call with the script manager's own stop exception, so a step never acts on a
 * dialog the player did not answer.
 */
public interface NewbieDialog {

    /** A page with a "next" button. */
    void next(String text);

    /** A last page with an "ok" button. */
    void ok(String text);

    /** Yes / no; true for yes. */
    boolean yesNo(String text);

    /**
     * A menu: {@code options} become {@code #L<i>#...#l} lines after {@code text}; returns the chosen index, or -1
     * for an answer that is not one of the options.
     */
    int menu(String text, java.util.List<String> options);

    /** Moves the character to {@code map} / {@code portal} once the dialog has ended. */
    void warpAfter(int map, int portal);
}
