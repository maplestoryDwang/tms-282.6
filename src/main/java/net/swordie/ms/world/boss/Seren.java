package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;

/** Original Seren direction assets, two-stage routing and (bossmech3-0930) the SerenFight gauge / clock / bodies. */
public final class Seren {
    private Seren() { }

    public static boolean isTms282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static int stage(int fieldId) {
        if (fieldId < 410002000 || fieldId > 410002280) return -1;
        int stage = (fieldId - 410002000) % 100;
        return stage % 20 == 0 ? stage : -1;
    }

    public static int mobId(int fieldId) {
        if (stage(fieldId) != 20 && stage(fieldId) != 60) return 0;
        int base = switch ((fieldId - 410002000) / 100) {
            case 0 -> 8880600; // hard
            case 1 -> 8880630; // normal
            case 2 -> 8880650; // extreme
            default -> 0;
        };
        return base == 0 ? 0 : base + (stage(fieldId) == 60 ? 2 : 0);
    }

    public static int introDuration(int fieldId) {
        // TMS282 Direction20/bossSeren JSON 2.1.27, animation's final timeline keys.
        return switch (stage(fieldId)) {
            case 0 -> 12667; // 12.6666 seconds
            case 40 -> 9334; // 9.3333 seconds
            default -> 0;
        };
    }

    public static void playIntro(Char chr) {
        Field source = chr.getField();
        var instance = chr.getInstance();
        int duration = introDuration(source.getId());
        if (!isTms282() || instance == null || duration == 0) return;
        var sm = chr.getScriptManager();
        String phase = stage(source.getId()) == 0 ? "1" : "2";
        sm.beginBossIntro(duration);
        sm.spineScreen(false, false, true, 0,
                "Effect/Direction20.img/bossSeren/" + phase + "pahse_spine/skeleton", "animation", "serenIntro");
        chr.getTimer().addEvent(() -> {
            if (chr.getField() == source && chr.getInstance() == instance) {
                sm.endBossIntro();
                chr.warp(source.getId() + 20);
            }
        }, duration);
    }

    public static void spawn(Char chr) {
        Field field = chr.getField();
        int id = mobId(field.getId());
        if (!isTms282() || chr.getInstance() == null || id == 0) return;
        Mob core;
        synchronized (field) {
            // Both XML first-enter and the legacy custom user-enter hook may call this.
            if (field.isBossSpawned()) return;
            // Raw 282 footholds: palace floor y=275; phase-two floor y=305.
            core = chr.getScriptManager().spawnBossMob(id, 0, stage(field.getId()) == 20 ? 275 : 305);
        }
        // bossmech3-0930: sun gauge (both phases), clock / time of day / visible bodies (phase 2). SerenFight.
        if (core != null) {
            if (stage(field.getId()) == 20) {
                SerenFight.startPhaseOne(field, core);
            } else {
                SerenFight.startPhaseTwo(field, core);
            }
        }
    }

    /** P1 only. P2 retains the shared final-boss clear and loot window, without inventing reward mobs. */
    public static boolean onMobDeath(Mob mob) {
        Field field = mob.getField();
        if (!isTms282() || field == null) return false;
        // bossmech3-0930: the phase core died -> stop the sun gauge / clock and remove the phase-2 body.
        if ((stage(field.getId()) == 20 || stage(field.getId()) == 60)
                && mob.getTemplateId() == mobId(field.getId()) && mob.getHp() <= 0) {
            SerenFight fight = SerenFight.of(field);
            if (fight != null) fight.stop(true);
        }
        if (stage(field.getId()) != 20
                || mob.getTemplateId() != mobId(field.getId()) || mob.getHp() > 0) return false;
        synchronized (field) {
            if (field.hasProperty("SerenPhaseCleared")) return true;
            field.setProperty("SerenPhaseCleared", true);
        }
        // 273 boss_seren_* events use six seconds after the first boss's death.
        for (Char member : new ArrayList<>(field.getChars())) {
            var instance = member.getInstance();
            if (instance == null) continue;
            member.getTimer().addEvent(() -> {
                if (member.getField() == field && member.getInstance() == instance) {
                    member.warp(field.getId() + 20);
                }
            }, 6000);
        }
        return true;
    }
}
