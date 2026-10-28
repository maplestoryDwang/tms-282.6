package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** Routes from the read-only TMS273 boss_limbo event scripts; not the native Limbo mechanic engine. */
public final class Limbo {
    public static final int LOBBY = 410007601;
    public static final int NORMAL = 410009000;
    public static final int HARD = 410011000;

    private Limbo() { }

    public static void enter(Char chr) {
        Field field = chr.getField();
        int id = field.getId();
        int base = id >= HARD && id <= HARD + 800 ? HARD : NORMAL;
        int stage = id - base;
        if (chr.getInstance() == null || stage < 0 || stage > 800 || stage % 100 != 0) {
            return;
        }
        if (stage == 0 || stage == 200 || stage == 600) {
            // Supplemental short entry effect, not a reconstruction of the missing native intro.
            BossHelper.playBossEntry(chr, () -> {
                if (chr.getField() == field && chr.getInstance() != null) {
                    chr.warp(base + stage + 100);
                }
            });
            return;
        }
        if (stage == 300 || stage == 400) {
            // 273's initLimboEvent drives these stages. Never invent a timer or bypass their clear condition.
            chr.chatMessage("林波第二階段的專屬機制尚未移植；本場不會自動跳過，請使用首領退出功能離場。");
            return;
        }
        int offset = base == HARD ? 50 : 0;
        int mobId = switch (stage) {
            case 100 -> 8881306 + offset;
            case 500 -> 8881335 + offset;
            case 700 -> 8881332 + offset;
            default -> 0;
        };
        // Reward spawning depends on the missing native limbo_reward script. Do not fabricate rewards.
        if (mobId == 0) {
            return;
        }
        synchronized (field) {
            if (field.hasProperty("LimboStageSpawned")) {
                return;
            }
            var holder = chr.getScriptManager().spawnBossMob(mobId, 0, -16);
            if (holder != null) {
                field.setProperty("LimboStageSpawned", true);
                // bossflow-0930: these three are invincible HP holders; spawn their HpLinkMob bodies.
                Tms282LinkedBodies.spawnFor(holder);
            }
        }
    }

    /** Called only for an actual mob death; never infer phase completion from an empty field. */
    public static void onMobDeath(Field field, int mobId) {
        int id = field.getId();
        int base = id >= HARD && id <= HARD + 800 ? HARD : NORMAL;
        int offset = base == HARD ? 50 : 0;
        int next = 0;
        if (id == base + 100 && mobId == 8881306 + offset) next = base + 200;
        if (id == base + 500 && mobId == 8881335 + offset) next = base + 600;
        if (id == base + 700 && mobId == 8881332 + offset) next = base + 800;
        if (next == 0) return;
        synchronized (field) {
            if (field.hasProperty("LimboStageCleared")) return;
            field.setProperty("LimboStageCleared", true);
        }
        int target = next;
        // The 273 event scripts use 3000 ms after these three death events.
        field.getTimer().addEvent(() -> {
            for (Char member : new ArrayList<>(field.getChars())) {
                if (member.getField() == field && member.getInstance() != null) member.warp(target);
            }
        }, 3000, TimeUnit.MILLISECONDS);
    }
}
