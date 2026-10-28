package net.swordie.ms.world.tms282.newbie;

import java.sql.SQLException;
import java.util.List;

/**
 * Runs the configured steps for one character inside one dialog, from the stored step on. After each
 * {@link NewbieStep.Outcome#NEXT} the stored progress moves on by a compare-and-set ({@link NewbieStore#advance}) - when
 * it was moved by someone else (a second dialog, another channel) this run stops. {@link NewbieStep.Outcome#STAY}
 * stops and keeps the step. A closed dialog / logout ends the run with the dialog's stop exception: the step that was
 * talking simply runs again next time (every step is idempotent).
 */
public final class NewbieFlowRunner {

    public enum End {COMPLETED, WARP_PENDING, STAYED, MOVED_ELSEWHERE, NOTHING_TO_DO}

    private NewbieFlowRunner() {
    }

    public static End run(NewbieRun run) throws SQLException {
        NewbieProgress p = run.progress();
        if (p.completed()) return End.NOTHING_TO_DO;
        List<NewbieStep> steps = run.config().steps();
        int i = run.config().indexOf(p.stepId());
        for (int before = 0; before < i; before++) {
            NewbieStep step = steps.get(before);
            if (step instanceof net.swordie.ms.world.tms282.newbie.steps.FirstJobStep && (!p.jobDone() || run.character().getJob() == 10000)
                    || step instanceof net.swordie.ms.world.tms282.newbie.steps.StarterGiftStep && !p.giftClaimed()) {
                i = before; // hot-reordering a saved story must not skip unfinished job/gift work
                break;
            }
        }
        String current = p.stepId();
        while (i < steps.size()) {
            NewbieStep step = steps.get(i);
            if (current.isEmpty() || !current.equals(step.id())) {
                // first run ("" = the first step) or a step id the reloaded config no longer has: re-anchor
                if (!run.store().advance(run.character().getId(), current, step.id())) return End.MOVED_ELSEWHERE;
                current = step.id();
            }
            NewbieStep.Outcome o = step.run(run);
            if (o == NewbieStep.Outcome.STAY) return End.STAYED;
            boolean last = i == steps.size() - 1;
            if (last) {
                if (run.character().getJob() == 10000) return End.STAYED;
                // ScriptManager defers the actual warp until the dialogue closes. Complete only on arrival.
                if (step instanceof net.swordie.ms.world.tms282.newbie.steps.WarpMenuStep) return End.WARP_PENDING;
                if (!run.store().complete(run.character().getId(), step.id())) return End.MOVED_ELSEWHERE;
                System.out.println("[Newbie] flow completed for character " + run.character().getId());
                return End.COMPLETED;
            }
            String next = steps.get(i + 1).id();
            if (!run.store().advance(run.character().getId(), step.id(), next)) return End.MOVED_ELSEWHERE;
            current = next;
            run.reload();
            i++;
        }
        return End.NOTHING_TO_DO;
    }
}
