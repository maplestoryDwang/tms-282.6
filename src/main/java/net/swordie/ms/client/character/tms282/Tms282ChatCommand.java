package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;

import java.util.List;
import java.util.Objects;

/**
 * One chat command of the TMS 282.3 in-field chat (Command pattern; registered in {@link Tms282ChatCommands}).
 * A command decides and returns packets; the chat handler sends them and shows {@link Reply#message()} to the
 * player with its own chat packets.
 */
public interface Tms282ChatCommand {

    /** Name after the prefix, lower case (e.g. "lv" for "@lv 5"). */
    String name();

    /** Minimum users.accounttype (265 AccountType value) allowed to run it. */
    int minAccountType();

    /** Usage shown when the arguments are wrong, e.g. "@lv <levels 1..50>". */
    String usage();

    /** Runs the command; throws IllegalArgumentException for bad arguments (the registry shows {@link #usage()}). */
    Reply execute(Char chr, List<String> args);

    /** Packets to send (in order), the text for the player and one log line. */
    record Reply(List<OutPacket> packets, String message, String log) {
        public Reply {
            packets = List.copyOf(packets);
            Objects.requireNonNull(message, "message");
            Objects.requireNonNull(log, "log");
        }

        public static Reply text(String message, String log) {
            return new Reply(List.of(), message, log);
        }
    }
}
