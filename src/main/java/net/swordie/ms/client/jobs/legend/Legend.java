package net.swordie.ms.client.jobs.legend;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.constants.JobConstants;

public class Legend extends Job {

    public Legend(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return id == JobConstants.JobEnum.LEGEND.getJobId();
    }
}
