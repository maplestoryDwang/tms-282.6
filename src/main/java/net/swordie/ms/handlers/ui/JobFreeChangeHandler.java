package net.swordie.ms.handlers.ui;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.scripts.ScriptManagerImpl;

public class JobFreeChangeHandler {
    @Handler(op = InHeader.JOB_FREE_CHANGE_REQUEST)
    public static void handleJobFreeChangeRequest(Char chr, InPacket inPacket) {
        int jobID = inPacket.decodeInt();
        //Todo:
        //ScriptManagerImpl scriptManager = new ScriptManagerImpl(chr);
        //scriptManager.jobAdvance((short) jobID);
        //chr.write(WvsContext.jobFreeChangeResult());
        //chr.consumeItem(4310086, 1);
    }
}
