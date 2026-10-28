package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

public class Hilla {

    public static void check(Char chr) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        ScriptManagerImpl sm = chr.getScriptManager();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        Party party = chr.getParty();
        if (party != null) {
            if (party.isLeader(chr)) {
                if (field.getMobs().isEmpty()) {
                    switch (field.getId()) {
                        case 262030100 -> sm.warpParty(chr, 262030200, party);
                        case 262030200 -> sm.warpParty(chr, 262030300, party);
                        case 262031100 -> sm.warpParty(chr, 262031200, party);
                        case 262031200 -> sm.warpParty(chr, 262031300, party);
                    }
                } else {
                    sm.chat("請先消滅所有怪物再前往下一階段。");
                }
            } else {
                sm.chat("只有隊長可以執行此操作。");
            }
        } else {
            sm.warpInstanceOut(chr, BossConstants.HILLA_ENTRANCE_MAP, 1);
        }
    }
}
