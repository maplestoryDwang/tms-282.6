package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.enums.BossPartyType;
import net.swordie.ms.loaders.MobData;
import net.swordie.ms.world.field.Field;
import java.util.ArrayList;

/** Five-floor route and evidenced waves from TMS273 boss_princessno_normal.js.
 * Missing initial map-reset waves and the native EightTrigrams engine are not reconstructed here. */
public final class PrincessNo {
    private static final String WAVE = "PrincessNoWave";
    private static final String CLEAR = "PrincessNoClear";
    private PrincessNo() { }
    public static boolean isTms282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }
    public static void enter(Char chr) {
        Field f = chr.getField();
        if (!isTms282() || chr.getInstance() == null
                || BossHpConstants.getCurrentBossParty(chr) != BossPartyType.PRINCESS_NO) return;
        int id = f.getId();
        if (id < 811000100 || id > 811000500 || id % 100 != 0) return;
        synchronized (f) {
            if (f.hasProperty(WAVE)) return;
            f.setProperty(WAVE, 0);
            if (id == 811000100) {
                for (Char member : new ArrayList<>(chr.getInstance().getChars().values())) {
                    member.getScriptManager().setDeathCount(5);
                }
                // Never silently substitute a single mob for the missing reset() population.
                chr.chatMessage("濃姬前四層的初始怪群資料尚未完整移植，目前僅接入可核對的後續波次。八卦互動亦尚未完整支援。");
                spawn(f, chr, 9450004, 354, -562, 1);
            } else if (id == 811000200) {
                spawn(f, chr, 9450006, -348, -4, 8);
            } else if (id == 811000300) {
                if (MobData.getMobById(9450012) == null || MobData.getMobById(9450013) == null) {
                    chr.chatMessage("????????????????9450012?9450013???????????????????????");
                    return;
                }
                spawn(f, chr, 9450013, 45, -262, 1);
                for (int[] p : new int[][]{{-430,-262},{490,-262},{-430,-27},{45,-27},{490,-27}}) {
                    spawn(f, chr, 9450012, p[0], p[1], 1);
                    spawn(f, chr, 9450011, p[0], p[1], 5);
                }
                f.getTimer().addEvent(() -> {
                    synchronized (f) {
                        if (f.getChars().isEmpty() || f.hasProperty(CLEAR)) return;
                        f.setProperty(WAVE, 1);
                        // TMS273 ends the defence after 60 seconds, then summons the general.
                        f.removeMobs();
                        spawn(f, f.getChars().iterator().next(), 9450014, 41, 146, 1);
                    }
                }, 60000);
            } else if (id == 811000400) {
                spawn(f, chr, 9450020, 380, -134, 1);
            } else {
                // No native movie/cutscene sequence was found; preserve map music and mob appearance.
                spawn(f, chr, 9450022, -8, -205, 1);
            }
        }
    }
    private static void spawn(Field f, Char chr, int mob, int x, int y, int count) {
        long hp = BossHpConstants.resolveMaxHp(mob, chr);
        for (int i = 0; i < count; i++) {
            if (hp > 0) f.spawnMob(mob, x, y, false, hp);
            else f.spawnMob(mob, x, y, false);
        }
        f.setBossSpawned(true);
    }
    /** Invoke after the dead mob is removed. Never infer a clear from administrative removal. */
    public static void onMobDeath(Field f, int mobId) {
        if (!isTms282() || !f.hasProperty(WAVE) || f.hasProperty(CLEAR)) return;
        synchronized (f) {
            int id = f.getId();
            if (id == 811000200 && mobId >= 9450006 && mobId <= 9450009) {
                if (!f.getMobs().isEmpty() || f.getChars().isEmpty()) return;
                int next = mobId + 1;
                int[][] pos = {{393,-147},{-298,-298},{367,-497},{354,-562}};
                int[] p = pos[mobId - 9450006];
                f.setProperty(WAVE, next - 9450006);
                spawn(f, f.getChars().iterator().next(), next, p[0], p[1], next == 9450010 ? 1 : 8);
                return;
            }
            boolean cleared = id == 811000100 && mobId == 9450004
                    || id == 811000200 && mobId == 9450010
                    || id == 811000300 && mobId == 9450014
                    || id == 811000400 && mobId == 9450020
                    || id == 811000500 && mobId == 9450022;
            if (!cleared) return;
            f.setProperty(CLEAR, true);
            // Native fallback maps 1 and 4 have no next portal. Route only on their boss's actual death.
            if (id == 811000100 || id == 811000400) {
                f.getTimer().addEvent(() -> {
                    for (Char member : new ArrayList<>(f.getChars())) nextStage(member);
                }, 1500);
            }
        }
    }
    public static void nextStage(Char chr) {
        Field f = chr.getField();
        if (!isTms282() || chr.getInstance() == null || !f.hasProperty(CLEAR)) {
            chr.chatMessage("請先擊敗本層的首領。");
            return;
        }
        int id = f.getId();
        if (id >= 811000100 && id < 811000500 && id % 100 == 0) chr.warp(id + 100);
    }
}
