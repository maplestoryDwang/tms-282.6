package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;

import java.util.ArrayList;

/** Main-stage compatibility flow; room sharing and BossPattern mechanics are not ported. */
public final class Baldrix {
    private Baldrix() { }

    public static boolean enabled() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static boolean isArena(int id) {
        return id >= 410014000 && id <= 410016100 && id % 100 == 0;
    }

    public static void intro(Char chr) {
        if (!enabled() || chr.getInstance() == null) return;
        int id = chr.getFieldID();
        if (id != 410014000 && id != 410014400 && id != 410014800
                && id != 410015100 && id != 410015500 && id != 410015900) return;
        var instance = chr.getInstance();
        var field = chr.getField();
        // Supplemental transition, NOT a reconstruction of the official intro.
        chr.getScriptManager().fadeInOut(300, 600, 300, 255);
        field.getTimer().addEvent(() -> {
            if (chr.getInstance() == instance && chr.getField() == field) chr.warp(id + 100);
        }, 1200);
    }

    public static void combat(Char chr) {
        if (!enabled() || chr.getInstance() == null) return;
        Field field = chr.getField();
        int id = field.getId();
        boolean hard = id >= 410015100;
        int normalId = hard ? id - 1100 : id;
        int mobId;
        int next;
        switch (normalId) {
            case 410014100 -> { mobId = 8881601; next = 410014400; }
            case 410014500 -> { mobId = 8881615; next = 410014800; }
            case 410014900 -> { mobId = 8881621; next = 0; }
            default -> { return; }
        }
        synchronized (field) {
            if (field.hasProperty("BaldrixStageStarted")) return;
            // Entry portal coordinates come from the corresponding map's WZ.
            var portal = field.getPortalByName("sp");
            if (portal == null) return;
            Mob mob = chr.getScriptManager().spawnBossMob(mobId + (hard ? 50 : 0),
                    portal.getX(), portal.getY());
            if (mob == null) return;
            field.setProperty("BaldrixStageStarted", true);
            if (next != 0) watch(chr.getInstance(), field, mob, next + (hard ? 1100 : 0));
            // Final death uses the shared loot window; don't warp away from freshly dropped loot.
        }
    }

    private static void watch(Instance instance, Field field, Mob mob, int next) {
        field.getTimer().addEvent(() -> {
            // Timers cannot resurrect an abandoned or timed-out instance.
            if (instance.getChars().values().stream().noneMatch(c -> c.getInstance() == instance)) return;
            if (mob.getHp() > 0) {
                if (field.getMobs().contains(mob)) watch(instance, field, mob, next);
                return; // A despawn/reset is not a kill.
            }
            synchronized (field) {
                if (field.hasProperty("BaldrixStageCleared")) return;
                field.setProperty("BaldrixStageCleared", true);
            }
            for (Char member : new ArrayList<>(instance.getChars().values())) {
                if (member.getInstance() == instance && isArena(member.getFieldID())) member.warp(next);
            }
        }, 500);
    }
}
