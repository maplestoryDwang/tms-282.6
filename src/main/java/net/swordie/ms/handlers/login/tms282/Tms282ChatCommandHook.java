package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.Tms282ChatCommand;
import net.swordie.ms.client.character.tms282.Tms282ChatCommands;

import java.util.Objects;
import java.util.Optional;

/**
 * Extension point of the CP0117 chat handler for chat commands: the {@link Tms282ChatCommands} registry (Command
 * pattern) that the research probe's runChatCommand dispatches '@' lines to. Empty means ordinary chat, which is
 * then broadcast; a reply goes to the speaker only (its packets, then one LP03D5 "[cmd] message" line).
 */
@FunctionalInterface
public interface Tms282ChatCommandHook {
    /**
     * @param accountType 265 AccountType value of the speaker's user (users.accounttype)
     * @return the command's reply, or empty for ordinary chat
     */
    Optional<Tms282ChatCommand.Reply> dispatch(Char chr, int accountType, String text);

    /** No commands: every line is chat. */
    Tms282ChatCommandHook NONE = (chr, accountType, text) -> Optional.empty();

    /** Lines starting with {@link Tms282ChatCommands#PREFIX} go to {@code commands}. */
    static Tms282ChatCommandHook of(Tms282ChatCommands commands) {
        Objects.requireNonNull(commands, "commands");
        return (chr, accountType, text) -> Tms282ChatCommands.isCommand(text)
                ? commands.dispatch(chr, accountType, text) : Optional.empty();
    }
}
