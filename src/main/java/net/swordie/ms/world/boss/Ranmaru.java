package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.enums.BossPartyType;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

public class Ranmaru {

    public static boolean isTms282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static void spawn(Char chr, RanmaruMode mode) {
        if (isTms282()) {
            spawnNative(chr, mode);
            return;
        }
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        ScriptManagerImpl sm = chr.getScriptManager();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        switch (mode) {
            case NORMAL:
                if (!field.isBossSpawned()) {
                    field.setBossSpawned(true);
                    field.getTimer().addEvent(() -> field.spawnMob(BossHelper.RANMARU_NORMAL, -373, 123, false, 5000000000L), 2000);
                }
                break;
            case HARD:
                if (!field.isBossSpawned()) {
                    field.setBossSpawned(true);
                    field.getTimer().addEvent(() -> field.spawnMob(BossHelper.RANMARU_HARD, -373, 123, false, 50000000000L), 2000);
                }
                break;
        }
    }

    private static void spawnNative(Char chr, RanmaruMode mode) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }
        Field field = chr.getField();
        boolean hard = mode == RanmaruMode.HARD;
        BossPartyType boss = hard ? BossPartyType.RANMARU_HARD : BossPartyType.RANMARU_NORMAL;
        if (field.getId() != (hard ? 807300210 : 807300110)
                || BossHpConstants.getCurrentBossParty(chr) != boss) {
            return;
        }
        int mobId = hard ? BossHelper.RANMARU_HARD : BossHelper.RANMARU_NORMAL;
        long hp = BossHpConstants.resolveMaxHp(mobId, chr);
        synchronized (field) {
            if (field.isBossSpawned()) {
                return;
            }
            field.setBossSpawned(true);
            // TMS273 boss_ranmaru events: ten lives; never replenish on map re-entry.
            if (chr.getParty() != null) {
                for (Char member : chr.getParty().getOnlineChars()) {
                    if (member != null && member.getInstance() == chr.getInstance()) {
                        member.getScriptManager().setDeathCount(10);
                    }
                }
            } else {
                chr.getScriptManager().setDeathCount(10);
            }
            // Preserve native map/music and mob appearance; no evidenced cinematic sequence.
            field.getTimer().addEvent(() -> field.spawnMob(mobId, -373, 123, false, hp), 2000);
        }
    }

    public enum RanmaruMode {
        NORMAL(0),
        HARD(1),
        ;

        private final int val;

        RanmaruMode(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }
}
