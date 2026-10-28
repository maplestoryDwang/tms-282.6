package net.swordie.ms.scripts.event;

import net.swordie.ms.client.character.Char;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.nio.file.Path;

/**
 * Bridge to the script runtime (implemented by {@code ScriptManagerImpl.EventBridge}): builds the Nashorn engine of
 * an event instance and adapts characters to the objects the MetaMS scripts expect.
 */
public interface EventScriptHost {

    /** Evaluates {@code file} in a fresh engine whose global {@code event} is {@code instance}. */
    Invocable load(EventInstance instance, Path file) throws ScriptException;

    /** The per-character script object ({@code stage}/{@code members[i]}) of MetaMS scripts. */
    Object player(Char chr);

    /** The {@code party} object of MetaMS scripts for {@code chr} (null when not in a party). */
    Object party(Char chr);

    /** Departure callbacks may be queued until after the party leader has already changed. */
    default Object partyDeparture(Char chr) {
        return party(chr);
    }

    /** Runs content script {@code path} (e.g. {@code expand/xxx}) for {@code player} on its own thread. */
    default void runScriptAsync(Object player, String path) {
    }

    /** Converts a value created in a caller's engine (e.g. an attachment array) for use in another engine. */
    default Object importValue(Object value) {
        return value;
    }
}
