package net.swordie.ms.client.jobs.special;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.jobs.adventurer.PinkBean;

/** Reuses PinkBean's existing combat handler and applies the 282 fixed-level progression. */
public final class SpecialPinkBean extends PinkBean {
    public SpecialPinkBean(Char chr) { super(chr); }

    @Override public void setCharCreationStats(Char chr) { SpecialJobProgression.setLocalCreationStats(chr); }
    @Override public void handleJobAdvance() { SpecialJobProgression.learnAvailableSkills(chr, true); }
    @Override public void handleInitAfterField() {
        super.handleInitAfterField();
        SpecialJobProgression.learnAvailableSkills(chr, true);
    }
}
