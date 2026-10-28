package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** Original client intro assets and reference stage routing; native Kalos mechanics remain pending. */
public final class Kalos {
    private Kalos() { }

    public static boolean enabled() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    public static int baseForField(int id) {
        int base = id - Math.floorMod(id, 100);
        return (base == 410006000 || base == 410006200 || base == 410006300 || base == 410006400)
                && id - base <= 80 && (id - base) % 20 == 0 ? base : 0;
    }

    public static int phaseOneMob(int base) {
        return switch (base) {
            case 410006000 -> 8880800;
            case 410006200 -> 8881010;
            case 410006300 -> 8881030;
            case 410006400 -> 8881050;
            default -> 0;
        };
    }

    public static int nextFieldForDeath(int fieldId, int mobId) {
        int base = baseForField(fieldId);
        return base != 0 && fieldId == base + 20 && mobId == phaseOneMob(base) ? base + 40 : 0;
    }

    public static void enter(Char chr) {
        if (!enabled() || chr.getInstance() == null) return;
        Field field = chr.getField();
        int base = baseForField(field.getId());
        if (base == 0) return;
        int stage = field.getId() - base;
        var sm = chr.getScriptManager();
        if (stage == 0 || stage == 40) {
            var instance = chr.getInstance();
            // Actual 282 skeleton.json animation end times: 6.6666s / 5.5666s.
            int duration = stage == 0 ? 6667 : 5567;
            sm.beginBossIntro(duration);
            sm.spineScreen(false, false, true, 0,
                    "Effect/Direction20.img/bossKalos/" + (stage == 0 ? "1" : "2")
                            + "phase_spine/skeleton", "animation", "skeleton");
            chr.getTimer().addEvent(() -> {
                if (chr.getField() == field && chr.getInstance() == instance) {
                    sm.endBossIntro();
                    chr.warp(field.getId() + 20);
                }
            }, duration);
        } else if (stage == 20 || stage == 60) {
            synchronized (field) {
                if (field.hasProperty("KalosStageSpawned")) return;
                // Use a map-supported floor position; original server spawn choreography remains pending.
                var portal = field.getPortalByName("sp");
                if (portal == null) return;
                int mobId = phaseOneMob(base) + (stage == 60 ? 2 : 0);
                var boss = sm.spawnBossMob(mobId, portal.getX(), portal.getY());
                if (boss != null) {
                    field.setProperty("KalosStageSpawned", true);
                    // bossflow-0930: phase 2's mob is the HP holder (mobType 14, no hit box); add the HpLinkMob body.
                    if (stage == 60) Tms282LinkedBodies.spawnFor(boss);
                    chr.chatMessage("卡洛斯原版進場與階段路線已接入；專屬戰鬥機制仍待移植。");
                }
            }
        }
        // Reward map is not populated with invented rewards. Final arena retains the existing loot window.
    }

    public static void onMobDeath(Field field, int mobId) {
        if (!enabled()) return;
        int target = nextFieldForDeath(field.getId(), mobId);
        if (target == 0) return;
        var instance = field.getChars().stream().map(Char::getInstance).filter(i -> i != null).findFirst().orElse(null);
        if (instance == null) return;
        synchronized (field) {
            if (field.hasProperty("KalosStageCleared")) return;
            field.setProperty("KalosStageCleared", true);
        }
        // Read-only 273 boss_kalos_* event scripts use 5000ms after phase one's actual death.
        field.getTimer().addEvent(() -> {
            for (Char member : new ArrayList<>(field.getChars())) {
                if (member.getField() == field && member.getInstance() == instance) member.warp(target);
            }
        }, 5000, TimeUnit.MILLISECONDS);
    }
}
