package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.ServerConstants;

import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step scripts of a quest that waits for the client between its dialogs (the 燃燒衝刺班 tutorials 102430..102446):
 * {@code quest/q<id>s} shows a description on a UI (LP MAKE_DESC_ON_UI type 1 with the quest id) and ends; when the
 * player has done what it asks, the client sends CP01A6 act 6 (LaterStep) and the server continues with
 * {@code quest/q<id>s_1}, then {@code _2}, ... Without that continuation the client stays inside the description
 * (keyboard and mouse locked; live 2026-10-06, character 63, quest 102430).
 * <p>
 * Which script is next comes from the quest's own record: every step script stores {@code step=<n>} in the QR value
 * before it waits, so {@code step=n} continues with {@code _n}, limited to the scripts that exist ({@code step=4} of
 * quest 102431 is its third and last script). {@code step=done} and quests without step scripts have none.
 */
public final class Tms282QuestStepScripts {
    private static final Pattern STEP = Pattern.compile("(?:^|;)step=([^;]*)");

    private Tms282QuestStepScripts() {
    }

    /** The script to run for a LaterStep of {@code questId} whose QR value is {@code qrValue}, if the quest has one. */
    public static Optional<String> next(int questId, String qrValue) {
        int last = count(questId);
        if (last == 0) {
            return Optional.empty();
        }
        int step = 1;
        Matcher m = STEP.matcher(qrValue == null ? "" : qrValue);
        if (m.find()) {
            String value = m.group(1).trim();
            if (!value.matches("\\d{1,3}")) {
                return Optional.empty(); // "done": nothing left to continue
            }
            step = Integer.parseInt(value);
        }
        return Optional.of(name(questId, Math.max(1, Math.min(step, last))));
    }

    /** How many consecutive step scripts {@code q<id>s_1..n} exist. */
    static int count(int questId) {
        int n = 0;
        while (exists(name(questId, n + 1))) {
            n++;
        }
        return n;
    }

    private static String name(int questId, int step) {
        return "q" + questId + "s_" + step;
    }

    private static boolean exists(String name) {
        return new File(ServerConstants.SCRIPT_DIR + "/quest/" + name + ".py").isFile()
                || new File(ServerConstants.SCRIPT_DIR + "/quest/" + name + ".js").isFile();
    }
}
