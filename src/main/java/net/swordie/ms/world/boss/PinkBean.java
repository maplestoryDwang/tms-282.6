package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

public class PinkBean {

    // Pink Bean:
    public static final int PINK_BEAN_MAP = 270050100;
    public static final int CHAOS_PINK_BEAN_MAP = 270051100;
    public static final int INITIAL_MOB = 8820008;
    public static final int CHAOS_INITIAL_MOB = 8820108;
    public static final int[] INITIAL_MOBS = {
            8820002, // Ariel
            8820003, // Solomon The Wise
            8820004, // Rex The Wise
            8820005, // Hugin
            8820006, // Munin
            8820008, // Transparent Mob for Summoning baby Boss
            8820014  // Pink Bean
    };

    public static void exit(Char chr, ScriptManagerImpl sm) {
        sm.setPlayerBoxChat();
        if (sm.sendAskYesNo("要撤退嗎？")) {
            sm.warpInstanceOut(chr, BossConstants.PINK_BEAN_ENTRACE_MAP);
        }
    }

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
                if (field.getId() == PINK_BEAN_MAP && !field.isBossSpawned()) {
                    field.setBossSpawned(true);
                    sm.removeNpc(2141000);
                    field.removeMobs();
                    for (int i : INITIAL_MOBS) {
                        field.spawnMob(i, 5, -42, false);
                    }
                } else if (field.getId() == CHAOS_PINK_BEAN_MAP && !field.isBossSpawned()) {
                    field.setBossSpawned(true);
                    sm.removeNpc(2141000);
                    field.removeMobs();
                    for (int i : INITIAL_MOBS) {
                        field.spawnMob(i + 100, 5, -42, false);
                    }
                    field.spawnMob(8820110, 5, -42, false, 2000000000L);
                    field.spawnMob(8820111, 5, -42, false, 2000000000L);
                    field.spawnMob(8820112, 5, -42, false, 2000000000L);
                    field.spawnMob(8820113, 5, -42, false, 2000000000L);
                } else {
                    exit(chr, sm);
                }
            }
        } else {
            sm.warpInstanceOut(chr, BossConstants.PINK_BEAN_ENTRACE_MAP);
        }
    }
}
