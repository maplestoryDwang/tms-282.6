package net.swordie.ms.world.tms282.newbie;

/**
 * One step of the newbie flow (command pattern): an immutable object built by {@link NewbieSteps} from its JSON
 * entry in {@code newbie-flow.json}. {@link NewbieFlowRunner} runs the configured steps in order inside one NPC
 * dialog; every step must be idempotent because a closed dialog, a disconnect or a relogin resumes at the step that
 * did not finish (its id is stored in {@code newbie_flow.step_id}).
 */
public interface NewbieStep {

    /** What a step tells the runner. */
    enum Outcome {
        /** Done (or nothing to do): the stored progress moves to the next step. */
        NEXT,
        /** Stop here and keep this step: it runs again at the next login or lobby entry. */
        STAY
    }

    /** The configured id (unique within the flow; stored as the progress marker). */
    String id();

    /** The registered type name ({@code Spawn}, {@code StoryDialog}, ...). */
    String type();

    /** Runs the step for {@code run.character()}. A closed dialog surfaces as the dialog's own stop exception. */
    Outcome run(NewbieRun run);
}
