package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.client.character.Char;

import java.sql.SQLException;
import java.util.Objects;

/**
 * What one step sees while {@link NewbieFlowRunner} runs the flow for a character: the character, the dialog, the
 * configuration the run started with, the game and the store. {@link #progress()} is re-read after every step.
 */
public final class NewbieRun {
    private final Char character;
    private final NewbieDialog dialog;
    private final NewbieFlowConfig config;
    private final NewbieStore store;
    private final NewbieGame game;
    private NewbieProgress progress;

    public NewbieRun(Char character, NewbieDialog dialog, NewbieFlowConfig config, NewbieStore store, NewbieGame game,
                     NewbieProgress progress) {
        this.character = Objects.requireNonNull(character, "character");
        this.dialog = Objects.requireNonNull(dialog, "dialog");
        this.config = Objects.requireNonNull(config, "config");
        this.store = Objects.requireNonNull(store, "store");
        this.game = Objects.requireNonNull(game, "game");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    public Char character() {
        return character;
    }

    public NewbieDialog dialog() {
        return dialog;
    }

    public NewbieFlowConfig config() {
        return config;
    }

    public NewbieStore store() {
        return store;
    }

    public NewbieGame game() {
        return game;
    }

    public NewbieProgress progress() {
        return progress;
    }

    /** Re-reads the row (after a step changed it). */
    void reload() throws SQLException {
        progress = store.load(character.getId()).orElse(progress);
    }
}
