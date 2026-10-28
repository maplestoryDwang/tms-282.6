package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry of {@link Tms282ChatCommand}s (Command pattern). {@link #dispatch} returns empty for ordinary chat, so the
 * chat handler keeps echoing it; for a command text from a user allowed to run it, it runs the command (or explains
 * why not) and the text is not echoed as chat.
 */
public final class Tms282ChatCommands {
    public static final char PREFIX = '@';
    /** Account type from which an unknown command name is answered instead of passed through as chat. */
    private static final int GM_ACCOUNT_TYPE = 3;

    private final Map<String, Tms282ChatCommand> commands;

    private Tms282ChatCommands(Map<String, Tms282ChatCommand> commands) {
        this.commands = Map.copyOf(commands);
    }

    public static Tms282ChatCommands of(Tms282ChatCommand... commands) {
        Map<String, Tms282ChatCommand> map = new LinkedHashMap<>();
        for (Tms282ChatCommand c : commands) {
            String name = Objects.requireNonNull(c.name(), "name").toLowerCase(Locale.ROOT);
            if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace)) {
                throw new IllegalArgumentException("Bad command name '" + name + "'");
            }
            if (map.putIfAbsent(name, c) != null) {
                throw new IllegalArgumentException("Duplicate chat command " + name);
            }
        }
        return new Tms282ChatCommands(map);
    }

    /** The standard admin commands: @lv and @sp through {@code levels}, @jobv / @jobvi ({@link Tms282JobAdvanceCommands}), @dummy ({@link Tms282DummyCommand}). */
    public static Tms282ChatCommands admin(Tms282LevelService levels) {
        List<Tms282ChatCommand> all = new java.util.ArrayList<>(List.of(new LevelCommand(levels), new SpCommand(levels)));
        all.addAll(Tms282JobAdvanceCommands.all());
        all.addAll(net.swordie.ms.world.tms282.mail.MailChatCommands.all()); // mail: memo / parcel commands
        all.add(new Tms282DummyCommand()); // damage phase: training dummy without the bag
        all.addAll(net.swordie.ms.client.character.social.tms282.SocialChatCommands.all()); // social records: fame / block list / rings
        all.add(new net.swordie.ms.world.tms282.contents.changeburning.dream.DreamCommand()); // @dream: 夢幻的裝備 probe (GM)
        all.add(new net.swordie.ms.world.tms282.contents.changeburning.trial.DreamTrialCommand()); // @dreamtrial: 夢幻的試煉 (GM)
        all.add(new net.swordie.ms.world.tms282.contents.punchking.PunchKingHudCommand()); // @punchhud: LP0833 probe (GM)
        all.addAll(net.swordie.ms.world.tms282.bossrun.BossRunChatCommands.all()); // @bosslog @bossreset (GM), @bossreward @bosspractice
        all.add(new Tms282NoticeCommand()); // @notice: world announcement (notice-1005), --kind= probe
        return of(all.toArray(Tms282ChatCommand[]::new));
    }

    public Map<String, Tms282ChatCommand> commands() {
        return commands;
    }

    /** Text that the registry would handle instead of chat. */
    public static boolean isCommand(String text) {
        return text != null && text.length() > 1 && text.charAt(0) == PREFIX;
    }

    /**
     * Empty for ordinary chat, which includes a command text from a user below the command's account type (commands
     * are not revealed; an unknown name is also chat for any non-GM user). Otherwise the command's reply, or
     * a text-only reply for an unknown command or bad arguments.
     */
    public Optional<Tms282ChatCommand.Reply> dispatch(Char chr, int accountType, String text) {
        if (!isCommand(text)) {
            return Optional.empty();
        }
        String[] parts = text.substring(1).trim().split("\\s+");
        String name = parts[0].toLowerCase(Locale.ROOT);
        Tms282ChatCommand command = commands.get(name);
        if (command == null) {
            // Only GMs learn that a name is unknown; for players a typo like "@xxx" stays ordinary chat.
            return accountType >= GM_ACCOUNT_TYPE ?Optional.of(Tms282ChatCommand.Reply.text("Unknown command.",
                    "chat command '" + name + "' unknown")) : Optional.empty();
        }
        if (accountType < command.minAccountType()) {
            return Optional.empty();
        }
        List<String> args = List.copyOf(Arrays.asList(parts).subList(1, parts.length));
        try {
            return Optional.of(command.execute(chr, args));
        } catch (IllegalArgumentException e) {
            return Optional.of(Tms282ChatCommand.Reply.text("Usage: " + command.usage(),
                    "chat command '" + name + "' bad arguments " + args + ": " + e.getMessage()));
        }
    }

    /** {@code @lv <n>}: n level-up events through {@link Tms282LevelService#gainLevels}. Account type >= 3. */
    public static final class LevelCommand implements Tms282ChatCommand {
        public static final int MAX = 50;
        private final Tms282LevelService levels;

        public LevelCommand(Tms282LevelService levels) {
            this.levels = Objects.requireNonNull(levels, "levels");
        }

        @Override
        public String name() {
            return "lv";
        }

        @Override
        public int minAccountType() {
            return 3;
        }

        @Override
        public String usage() {
            return "@lv <1.." + MAX + ">（提升的等級數；燃燒活動期間每次可能多升數級）";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            int n = oneNumber(args, 1, MAX);
            Tms282LevelService.Result r = levels.gainLevels(chr, n);
            var packets = r.packets();
            if (Tms282JobAdvance.hasDueStage(chr)
                    && !net.swordie.ms.world.tms282.newbie.NewbieFlowService.get().replacesEntryScript(chr)) {
                // Match EXP gain: send numeric level/SP first, then advancement. Returning the old SP snapshot
                // after catchUp would overwrite the freshly granted milestone SP on the client.
                packets.forEach(chr::write);
                Tms282JobAdvance.catchUp(chr);
                packets = List.of();
            }
            return new Reply(packets, "等級 " + r.fromLevel() + " -> " + r.toLevel() + "，SP +" + r.totalSpGained(),
                    "@lv " + n + ": level " + r.fromLevel() + "->" + r.toLevel() + " exp=" + r.exp() + " sp per level "
                            + r.spGained() + " skills=" + r.skillsChanged().size() + "; " + String.join("; ", r.log()));
        }
    }

    /** {@code @sp <n>}: n SP to the current job level through {@link Tms282LevelService#addSp}. Account type >= 3. */
    public static final class SpCommand implements Tms282ChatCommand {
        public static final int MAX = 1000;
        private final Tms282LevelService levels;

        public SpCommand(Tms282LevelService levels) {
            this.levels = Objects.requireNonNull(levels, "levels");
        }

        @Override
        public String name() {
            return "sp";
        }

        @Override
        public int minAccountType() {
            return 3;
        }

        @Override
        public String usage() {
            return "@sp <1.." + MAX + ">";
        }

        @Override
        public Reply execute(Char chr, List<String> args) {
            int n = oneNumber(args, 1, MAX);
            Tms282LevelService.Result r = levels.addSp(chr, n);
            return new Reply(r.packets(), "SP +" + n, "@sp " + n + ": " + String.join("; ", r.log()));
        }
    }

    static int oneNumber(List<String> args, int min, int max) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("expected one number");
        }
        int n;
        try {
            n = Integer.parseInt(args.getFirst());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: " + args.getFirst());
        }
        if (n < min || n > max) {
            throw new IllegalArgumentException(n + " outside " + min + ".." + max);
        }
        return n;
    }
}
