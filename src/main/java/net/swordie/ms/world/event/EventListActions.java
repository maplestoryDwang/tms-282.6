package net.swordie.ms.world.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.FileTime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * What the event list's 「參加」 button does (CP USER_EVENT_LIST_OPERATION, 265 0x047D / 282 0x04AF): a handler
 * registry keyed by the entry's {@code scriptName} (event-fixes-1005). The old switch keyed on the display name, which
 * events.tw.json had translated, so no entry ever matched.
 * <ol>
 *     <li>{@link #on registered} actions by {@code scriptName} (the legacy 265 entries and the 282 events);</li>
 *     <li>otherwise by {@code scriptType}: 1 = quest {@code scriptName} with the start script {@code uiName} (or
 *     {@code q<id>s}), 2 = the content script {@code scriptName}, when that script file exists;</li>
 *     <li>otherwise nothing happens ({@link Outcome#NO_ACTION}, logged).</li>
 * </ol>
 */
public final class EventListActions {
    /** One 「參加」 action. */
    @FunctionalInterface
    public interface Action {
        void join(Char chr, EventListData event);
    }

    /** How a press was handled. */
    public enum Outcome {REGISTERED, QUEST_SCRIPT, CONTENT_SCRIPT, NO_ACTION}

    /** Which script files exist (test seam; the server asks ScriptManagerImpl). */
    @FunctionalInterface
    public interface ScriptFiles {
        boolean exists(ScriptType type, String name);
    }

    private static final Pattern QUEST_ID = Pattern.compile("\\d{1,9}");
    private static final Pattern SCRIPT_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static volatile EventListActions standard;

    private final Map<String, Action> actions;
    private final ScriptFiles files;

    private EventListActions(Map<String, Action> actions, ScriptFiles files) {
        this.actions = Collections.unmodifiableMap(new LinkedHashMap<>(actions));
        this.files = Objects.requireNonNull(files, "files");
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The server's registry. */
    public static EventListActions standard() {
        EventListActions a = standard;
        if (a == null) {
            synchronized (EventListActions.class) {
                if (standard == null) {
                    standard = withStandardActions(builder()).build(ScriptManagerImpl::hasScriptFile);
                }
                a = standard;
            }
        }
        return a;
    }

    /** Test seam: replaces the server's registry (null = rebuild the standard one); returns the previous one. */
    public static synchronized EventListActions replaceStandard(EventListActions replacement) {
        EventListActions previous = standard;
        standard = replacement;
        return previous;
    }

    /** The 282 events and the legacy 265 entries (same effects as the old name switch). */
    public static Builder withStandardActions(Builder b) {
        // 282: 變身燃燒：露希妲 -> the designation / transformation NPC (npc/npc_9063907.py)
        b.on("changeBurning_lucid", (chr, e) -> script(chr).startScript(chr, 9063907, "npc_9063907", ScriptType.Npc));
        // legacy 265 entries (kept for the 265 events.json fallback; their 282 quests are expired)
        b.on("beyondBurning_start", (chr, e) -> {
            chr.createQuestWithQRValue(102450, "startlevel=260;before=" + chr.getLevel());
            if (!chr.hasQuest(655)) {
                chr.createQuestWithQRValue(655, "r1=0;r2=0;r3=0;r4=0;r5=0;r6=0;r7=0;r8=0;r9=0;r10=0;r11=0;r12=0");
            }
            script(chr).openUI(1663);
        });
        b.on("q101423s", (chr, e) -> {
            chr.createQuestWithQRValue(657, "001=1;002=1;101=1;003=1;102=1;004=1;103=1;104=1;151=1");
            chr.createQuestWithQRValue(101423, "before=" + chr.getLevel());
            chr.createQuestWithQRValue(101242, "season=202506;bWorld=19;startlevel=30;alert=0");
            script(chr).openUI(1346);
        });
        b.on("q102511s", (chr, e) -> script(chr).openUI(1675));
        b.on("itemRentalOpen", (chr, e) -> script(chr).openUI(3011));
        b.on("102269", (chr, e) -> script(chr).openUI(1556));
        b.on("lenFootstep_start", (chr, e) -> {
            if (!chr.hasQuest(640)) {
                chr.createQuestWithQRValue(640, "start=0;r0=0;q1=0;r1=0;q2=0;r2=0;q3=0;q4=0;r3=0;q5=0;r4=0;r5=0;step=0;r6=0;q8=0;r7=0;r8=0;r9=0;r10=0;r11=0;r12=0");
            }
            script(chr).openUI(1557);
        });
        b.on("505078", (chr, e) -> script(chr).openUI(1945));
        b.on("102422", (chr, e) -> {
            if (!chr.hasQuest(102422)) {
                chr.createQuestWithQRValue(102422, "alert=1");
            }
            if (!chr.hasQuest(102423)) {
                chr.createQuestWithQRValue(102423, "step=0");
            }
            if (!chr.hasQuest(647)) {
                chr.createQuestWithQRValue(647, "m10=0;m11=0;m12=0;m13=0;m14=0;r0=0;r1=0;r2=0;r3=0;r4=0;r5=0;r6=0;r7=0;r8=0;r9=0;r10=0;start=1;r11=0;r12=0;r13=0;r14=0;m0=0;m1=0;m2=0;m3=0;m4=0;m5=0;m6=0;m7=0;m8=0;m9=0");
            }
            if (!chr.hasQuest(654)) {
                chr.createQuestWithQRValue(654, "g2=0;g3=0;g4=0;g5=0;g6=0;g7=0;g8=0;g9=0;Cnt=0;g10=0;g11=0;g12=0;g13=0;g14=0;sTime="
                        + FileTime.currentTime().toYYMMDD_QR() + ";g0=0;g1=0");
            }
            script(chr).openUI(1560);
        });
        b.on("gotoVIPSauna", (chr, e) -> script(chr).startScript(chr, 9063353, e.uiName, ScriptType.Content));
        return b;
    }

    private static ScriptManagerImpl script(Char chr) {
        return chr.getScriptManager();
    }

    /** Runs the action of {@code event} for {@code chr}. */
    public Outcome join(Char chr, EventListData event) {
        Objects.requireNonNull(chr, "chr");
        Objects.requireNonNull(event, "event");
        Action a = event.scriptName == null ? null : actions.get(event.scriptName);
        if (a != null) {
            a.join(chr, event);
            return Outcome.REGISTERED;
        }
        if (event.scriptType == 1 && event.scriptName != null && QUEST_ID.matcher(event.scriptName).matches()) {
            int questId = Integer.parseInt(event.scriptName);
            String name = event.uiName != null && SCRIPT_NAME.matcher(event.uiName).matches()
                    ? event.uiName : "q" + questId + "s";
            if (files.exists(ScriptType.Quest, name)) {
                script(chr).startScript(chr, questId, name, ScriptType.Quest);
                return Outcome.QUEST_SCRIPT;
            }
        }
        if (event.scriptType == 2 && event.scriptName != null && SCRIPT_NAME.matcher(event.scriptName).matches()
                && files.exists(ScriptType.Content, event.scriptName)) {
            script(chr).startScript(chr, 0, event.scriptName, ScriptType.Content);
            return Outcome.CONTENT_SCRIPT;
        }
        return Outcome.NO_ACTION;
    }

    /** Whether {@code scriptName} has a registered action. */
    public boolean has(String scriptName) {
        return actions.containsKey(scriptName);
    }

    public static final class Builder {
        private final Map<String, Action> actions = new LinkedHashMap<>();

        /** Registers the action of {@code scriptName}; a name may be registered once. */
        public Builder on(String scriptName, Action action) {
            Objects.requireNonNull(scriptName, "scriptName");
            Objects.requireNonNull(action, "action");
            if (actions.putIfAbsent(scriptName, action) != null) {
                throw new IllegalStateException("duplicate event list action " + scriptName);
            }
            return this;
        }

        public EventListActions build(ScriptFiles files) {
            return new EventListActions(actions, files);
        }
    }
}
