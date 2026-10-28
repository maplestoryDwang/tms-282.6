package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

public class Arkarium {

    public static void spawn(Char chr) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        ScriptManagerImpl sm = chr.getScriptManager();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        Party party = chr.getParty();
        sm.setPlayerBoxChat();
        if (party != null) {
            if (!party.isLeader(chr)) {
                sm.sendSayOkay("請讓你的隊長與我對話！");
            } else {
                sm.setPlayerBoxChat();
                if (field.getId() == 272020200 && !field.isBossSpawned()) { // easy
                    field.setBossSpawned(true);
                    sm.removeNpc(9130095);
                    field.spawnMob(8860005, 320, -181, false);
                } else if (field.getId() == 272020210 && !field.isBossSpawned()) { // normal
                    field.setBossSpawned(true);
                    sm.removeNpc(9130095);
                    field.spawnMob(8860000, 320, -181, false);
                } else {
                    sm.exitBoss();
                }
            }
        } else {
            sm.warpInstanceOut(chr, BossConstants.ARKARIUM_ENTRACE_MAP);
        }
    }
}
