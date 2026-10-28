package net.swordie.ms.world.tms282.newbie.steps;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.world.tms282.newbie.NewbieGame;
import net.swordie.ms.world.tms282.newbie.NewbieJson;
import net.swordie.ms.world.tms282.newbie.NewbieRun;
import net.swordie.ms.world.tms282.newbie.NewbieStep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code FirstJob}: the first job advancement inside the dialog. {@code jobs} maps the character's current (beginner)
 * job ({@code from}) to its branches; one branch is done directly, several are offered as a menu with a yes / no
 * confirmation (explorers: warrior / magician / bowman / thief / pirate). A character whose job has no entry
 * (already in its first job at creation, the special event jobs) has nothing to do here. Zero uses its own
 * prologue completion, retaining its paired appearance, weapons, AP and SP.
 * <p>
 * The advancement is the server's own ({@link NewbieGame#advanceJob}: 282 level service up to {@code levelTo},
 * job quests, ScriptManager.jobAdvance + resetAP) and happens once per character: {@code newbie_flow.job_done_at}
 * is set by a compare-and-set under the locks after the advancement is saved, and the character must still have the
 * {@code from} job. Each branch has a {@code key} - the starter gift's equipment set ({@code "*"} = the common set).
 */
public final class FirstJobStep implements NewbieStep {
    public static final String TYPE = "FirstJob";

    /** One choice: equipment-set key, the label shown, the target job and the job quests to complete. */
    public record Branch(String key, String label, int job, List<Integer> quests) {
        public Branch {
            quests = List.copyOf(quests);
        }
    }

    /** The branches of one beginner job. */
    public record Rule(int from, List<Branch> branches) {
        public Rule {
            branches = List.copyOf(branches);
        }
    }

    private final String id;
    private final int levelTo;
    private final boolean autoAssignAp;
    private final String chooseText;
    private final String confirmText;
    private final String singleText;
    private final String doneText;
    private final String failText;
    private final Map<Integer, Rule> rules;
    private final Map<String, Rule> byLoginJob;

    FirstJobStep(String id, int levelTo, boolean autoAssignAp, String chooseText, String confirmText, String singleText,
                 String doneText, String failText, Map<Integer, Rule> rules, Map<String, Rule> byLoginJob) {
        this.id = id;
        this.levelTo = levelTo;
        this.autoAssignAp = autoAssignAp;
        this.chooseText = chooseText;
        this.confirmText = confirmText;
        this.singleText = singleText;
        this.doneText = doneText;
        this.failText = failText;
        this.rules = Map.copyOf(rules);
        this.byLoginJob = Map.copyOf(byLoginJob);
    }

    public static FirstJobStep read(String id, NewbieJson s) {
        int levelTo = s.integer("levelTo", 0, 30, 10);
        boolean ap = s.bool("autoAssignAp", true);
        String choose = s.text("chooseText");
        String confirm = s.text("confirmText");
        String single = s.text("singleText");
        String done = s.text("doneText");
        String fail = s.text("failText", "目前無法完成轉職，請稍後重新登入再試。");
        Map<Integer, Rule> rules = new LinkedHashMap<>();
        for (NewbieJson j : s.array("jobs", 1, 64)) {
            int from = j.integer("from", 0, 32767);
            if (JobConstants.JobEnum.getJobById((short) from) == null) throw NewbieJson.invalid(j.path + ".from", "unknown job " + from);
            List<Branch> branches = new ArrayList<>();
            Set<Integer> targets = new HashSet<>();
            for (NewbieJson b : j.array("branches", 1, 8)) {
                String key = b.word("key", 32);
                String label = b.text("label");
                if (label.contains("#")) throw NewbieJson.invalid(b.path + ".label", "must not contain #");
                int job = b.integer("job", 1, 32767);
                if (JobConstants.JobEnum.getJobById((short) job) == null) throw NewbieJson.invalid(b.path + ".job", "unknown job " + job);
                if (job == from) throw NewbieJson.invalid(b.path + ".job", "same as from");
                if (!targets.add(job)) throw NewbieJson.invalid(b.path + ".job", "duplicate target " + job);
                branches.add(new Branch(key, label, job, b.integers("quests", 1, 99_999_999, 8)));
            }
            if (rules.putIfAbsent(from, new Rule(from, branches)) != null) throw NewbieJson.invalid(j.path + ".from", "duplicate from " + from);
        }
        Map<String, Rule> byLogin = new LinkedHashMap<>();
        for (var e : s.entries("byLoginJob").entrySet()) {
            try { JobConstants.LoginJob.valueOf(e.getKey()); }
            catch (IllegalArgumentException ex) { throw NewbieJson.invalid(e.getValue().path, "unknown LoginJob"); }
            NewbieJson b = e.getValue();
            int from = b.integer("from", 0, 32767);
            int target = b.integer("job", 1, 32767);
            if (JobConstants.JobEnum.getJobById((short) from) == null || JobConstants.JobEnum.getJobById((short) target) == null || from == target)
                throw NewbieJson.invalid(b.path, "invalid job pair");
            byLogin.put(e.getKey(), new Rule(from, List.of(new Branch(b.word("key", 32), b.text("label"), target, b.integers("quests", 1, 99_999_999, 8)))));
        }
        return new FirstJobStep(id, levelTo, ap, choose, confirm, single, done, fail, rules, byLogin);
    }

    public Map<Integer, Rule> rules() {
        return rules;
    }

    /** Reuse validated first-job choices in the GM progression dialog. */
    public Rule ruleFor(int job, String loginJob) {
        Rule special = byLoginJob.get(loginJob);
        return special != null && special.from() == job ? special : rules.get(job);
    }

    public int levelTo() {
        return levelTo;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String type() {
        return TYPE;
    }

    private static String fill(String text, Branch b) {
        return text.replace("{job}", b.label());
    }

    @Override
    public Outcome run(NewbieRun run) {
        Char chr = run.character();
        if (net.swordie.ms.client.character.tms282.Tms282SkillLearning.enabled()
                && (chr.getJob() == 10000 || chr.getJob() == 10112
                && "ZERO".equals(run.progress().loginJob()))) {
            NewbieGame.Job result = run.game().completeZeroPrologue(chr,
                    () -> run.store().markZeroJobDone(chr.getId(), chr.getName()));
            if ((result == NewbieGame.Job.ADVANCED || result == NewbieGame.Job.ALREADY) && chr.getJob() == 10112)
                return Outcome.NEXT;
            run.dialog().ok(failText);
            return Outcome.STAY;
        }
        if (run.progress().jobDone()) return Outcome.NEXT;
        Rule rule = rules.get((int) chr.getJob());
        Rule special = byLoginJob.get(run.progress().loginJob());
        if (special != null && special.from() == chr.getJob()) rule = special;
        if (rule == null) {
            // A creation job already advanced, or a retry after advancement but before progress was saved.
            List<Rule> all = new ArrayList<>(rules.values());
            all.addAll(byLoginJob.values());
            for (Rule r : all) for (Branch b : r.branches()) {
                if (b.job() == chr.getJob()) {
                    NewbieGame.Job result = run.game().advanceJob(chr, r.from(), b.job(), b.quests(), levelTo, autoAssignAp,
                            () -> run.store().markJobDone(chr.getId(), b.key()));
                    if (result != NewbieGame.Job.ADVANCED && result != NewbieGame.Job.ALREADY) {
                        run.dialog().ok(failText);
                        return Outcome.STAY;
                    }
                    return Outcome.NEXT;
                }
            }
            return Outcome.NEXT;
        }
        Branch chosen = null;
        if (rule.branches().size() == 1) {
            chosen = rule.branches().get(0);
            run.dialog().next(fill(singleText, chosen));
        } else {
            List<String> labels = rule.branches().stream().map(Branch::label).toList();
            for (int attempt = 0; attempt < 5 && chosen == null; attempt++) {
                int i = run.dialog().menu(chooseText, labels);
                if (i < 0 || i >= rule.branches().size()) continue;
                Branch b = rule.branches().get(i);
                if (run.dialog().yesNo(fill(confirmText, b))) chosen = b;
            }
            if (chosen == null) return Outcome.STAY;
        }
        Branch b = chosen;
        NewbieGame.Job r = run.game().advanceJob(chr, rule.from(), b.job(), b.quests(), levelTo, autoAssignAp,
                () -> run.store().markJobDone(chr.getId(), b.key()));
        switch (r) {
            case ADVANCED -> {
                run.game().saveSoon(chr);
                run.dialog().ok(fill(doneText, b));
                return Outcome.NEXT;
            }
            case ALREADY -> {
                return Outcome.NEXT;
            }
            case FAILED -> {
                run.dialog().ok(failText);
                return Outcome.STAY;
            }
            default -> {
                return Outcome.STAY;
            }
        }
    }
}
