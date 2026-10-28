package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.world.field.Field;

public final class Papulatus {
    private Papulatus() {}

    public static boolean isTms282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static void enter(Char chr) {
        if (!isTms282() || chr.getInstance() == null) return;
        Field field = chr.getField();
        int mobId = switch (field.getId()) {
            case 220080100 -> 8500001;
            case 220080200 -> 8500011;
            case 220080300 -> 8500021;
            default -> 0;
        };
        if (mobId == 0) return;
        synchronized (field) {
            if (field.hasProperty("PapulatusNativeSpawned")) return;
            // Native WZ revive links lead to *2; only *2 completes the boss in BossHelper.
            // spawnBossMob and Field.spawnMob retain the shared HP resolver for both phases.
            if (chr.getScriptManager().spawnBossMob(mobId, 15, 218) == null) return;
            field.setProperty("PapulatusNativeSpawned", true);
            int lives = field.getId() == 220080100 ? 30 : 5;
            if (chr.getParty() == null) {
                chr.getScriptManager().setDeathCount(lives);
            } else {
                for (Char member : chr.getParty().getOnlineChars()) {
                    if (member != null && member.getInstance() == chr.getInstance()) {
                        member.getScriptManager().setDeathCount(lives);
                    }
                }
            }
        }
    }
}
