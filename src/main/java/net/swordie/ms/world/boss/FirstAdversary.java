package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;
import java.util.ArrayList;

/** 282 entry/stage routing. Fade is supplemental, not the official cinematic.
 * Clone forms, boss gauges, special attacks and phase-specific packets remain unsupported.
 * Four difficulty groups follow ascending map barrierAut / mob levels; runtime unverified.
 */
public final class FirstAdversary {
    private FirstAdversary() { }

    private static boolean enabled() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static void enter(Char chr) {
        if (!enabled() || chr.getFieldID() != 410007019) {
            return;
        }
        if (chr.getInstance() == null) {
            chr.getScriptManager().startBossUI("First Adversary");
        } else {
            chr.getScriptManager().startBoss();
        }
    }

    public static void exit(Char chr) {
        if (enabled() && chr.getInstance() != null
                && chr.getFieldID() >= 410020000 && chr.getFieldID() <= 410023600) {
            chr.getScriptManager().exitBoss();
        }
    }

    private static boolean stageField(int id, int stage, boolean intro) {
        if (stage < 1 || stage > 3 || id < 410020000 || id > 410023500) {
            return false;
        }
        return (id - 410020000) % 1000 == (stage - 1) * 200 + (intro ? 0 : 100);
    }

    public static void playIntro(Char chr, int stage) {
        Field source = chr.getField();
        Instance instance = chr.getInstance();
        if (!enabled() || instance == null || !stageField(source.getId(), stage, true)) {
            return;
        }
        // No UI lock survives an interrupted or cancelled transition.
        chr.getScriptManager().fadeInOut(300, 600, 300, 255);
        chr.getTimer().addEvent(() -> {
            if (chr.getField() == source && chr.getInstance() == instance) {
                chr.warp(source.getId() + 100);
            }
        }, 1200);
    }

    public static void spawn(Char chr, int stage) {
        Field field = chr.getField();
        Instance instance = chr.getInstance();
        if (!enabled() || instance == null || !stageField(field.getId(), stage, false)) {
            return;
        }
        synchronized (field) {
            if (field.isBossSpawned()) {
                return;
            }
            int group = (field.getId() - 410020000) / 1000;
            int mobId = 8881700 + group * 30 + (stage - 1) * 5;
            Mob mob = chr.getScriptManager().spawnBossMob(mobId, 0, 0);
            if (mob != null && stage < 3) {
                advanceAfterDeath(field, instance, mob);
            }
            // Stage 3 uses BossHelper's existing completion and loot-window flow.
        }
    }

    private static void advanceAfterDeath(Field field, Instance instance, Mob mob) {
        field.getTimer().addEvent(() -> {
            var participants = new ArrayList<>(field.getChars());
            participants.removeIf(member -> member.getField() != field || member.getInstance() != instance);
            if (participants.isEmpty()) {
                return;
            }
            if (mob.getHp() > 0) {
                advanceAfterDeath(field, instance, mob);
                return;
            }
            for (Char member : participants) {
                if (member.getField() == field && member.getInstance() == instance) {
                    member.warp(field.getId() + 100);
                }
            }
        }, 1000);
    }
}
