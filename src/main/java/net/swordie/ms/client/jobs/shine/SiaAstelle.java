package net.swordie.ms.client.jobs.shine;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.constants.JobConstants;

public class SiaAstelle extends Job {

    // V Skills
    public static final int SHINE = 400021142;
    public static final int STELLAR_XI_SIRIUS = 400021143;
    public static final int STELLAR_XII_SADALSUUD = 400021147;
    public static final int SAVIOR_CIRCLE = 400021149;
    public static final int TIME_BLINDER = 400021152;

    // Sixth job (HEXA) — H1 origin / H2 ascent
    public static final int CELESTIAL_DESIGN = 182141500;
    public static final int CELESTIAL_DESIGN_ASCENT = 182141503;

    public SiaAstelle(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isSiaAstelle(id);
    }
}
