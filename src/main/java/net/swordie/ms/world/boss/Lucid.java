package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.LucidPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.BossHpConstants;
import net.swordie.ms.enums.ButterFlyType;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.mob.skill.ButterFly;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class Lucid {

    public static void spawn(Char chr, LucidPhase phase) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        ScriptManagerImpl sm = chr.getScriptManager();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        switch (phase) {
            case FIRST -> {
                if (Protocol.active().version() == ProtocolVersion.TMS_282_3) {
                    enterFirstPhase282(chr, field);
                    return;
                }
                if (field.isBossSpawned()) {
                    return;
                }
                field.setBossSpawned(true);
                sm.invokeForParty(500, "spineScreen", false, false, true, 0, "Map/Effect3/BossLucid/Lucid/lusi", "animation", null);
                sm.invokeForParty(500, "playSound", "Sound/SoundEff.img/ArcaneRiver/phase1");
                field.getTimer().addEvent(() -> {
                    if (field.getId() == 450004150) {
                        long hp;
                        if (chr.getLucidMode() == 0) {
                            hp = 60000000000L;
                        } else if (chr.getLucidMode() == 1) {
                            hp = 120000000000L;
                        } else if (chr.getLucidMode() == 2) {
                            hp = 50800000000000L;
                        } else {
                            return;
                        }
                        field.spawnMob(8880140, 1000, 48, false, hp);
                        field.spawnMob(8880166, 1000, 48, false, hp);
                        if (field.getSpawnButterflyTimer() != null) {
                            field.getSpawnButterflyTimer().cancel(false);
                        }
                        ScheduledFuture<?> sf = field.getTimer().addFixedRateEvent(() -> spawnButterFly(chr, LucidPhase.FIRST.getVal()), 1000, 5000, false);
                        field.setSpawnButterflyTimer(sf);
                        GlobalTimerManager.addFieldTimer(field.getSN(), sf);
                        if (field.getDestroyButterflyTimer() != null) {
                            field.getDestroyButterflyTimer().cancel(false);
                        }
                        ScheduledFuture<?> sf2 = field.getTimer().addFixedRateEvent(() -> destroyButterFly(chr), 33500, 32500, false);
                        field.setDestroyButterflyTimer(sf2);
                        GlobalTimerManager.addFieldTimer(field.getSN(), sf2);
                    }
                }, 2000);
            }
            case SECOND -> {
                if (field.isBossSpawned()) {
                    return;
                }
                field.setBossSpawned(true);
                if (field.getId() == 450004250 && !field.hasMobById(8880150)) {
                    long hp;
                    if (chr.getLucidMode() == 0) {
                        hp = 60000000000L;
                    } else if (chr.getLucidMode() == 1) {
                        hp = 120000000000L;
                    } else if (chr.getLucidMode() == 2) {
                        hp = 50800000000000L;
                    } else {
                        return;
                    }
                    field.spawnMob(8880150, 532, -490, false, hp);
                    if (field.getSpawnButterflyTimer() != null) {
                        field.getSpawnButterflyTimer().cancel(false);
                    }
                    ScheduledFuture<?> sf = field.getTimer().addFixedRateEvent(() -> spawnButterFly(chr, 1), 1000, 5000, false);
                    field.setSpawnButterflyTimer(sf);
                    GlobalTimerManager.addFieldTimer(field.getSN(), sf);
                    if (field.getDestroyButterflyTimer() != null) {
                        field.getDestroyButterflyTimer().cancel(false);
                    }
                    ScheduledFuture<?> sf2 = field.getTimer().addFixedRateEvent(() -> destroyButterFly(chr), 33500, 32500, false);
                    field.setDestroyButterflyTimer(sf2);
                    GlobalTimerManager.addFieldTimer(field.getSN(), sf2);
                }
            }
            case REWARD -> {
                if (field.isBossSpawned()) {
                    return;
                }
                field.setBossSpawned(true);
                long hp = 9999999;
                if (field.getId() == 450004300) {
                    if (!field.isLucidRewardSpawned()) {
                        int id;
                        if (chr.getLucidMode() == 0) {
                            id = 8880177;
                            field.getTimer().addEvent(() -> field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossLucid, "恭喜擊敗簡單模式露希妲！", 3000)), 1000);
                        } else if (chr.getLucidMode() == 1) {
                            id = 8880167;
                            field.getTimer().addEvent(() -> field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossLucid, "恭喜擊敗普通模式露希妲！", 3000)), 1000);
                        } else if (chr.getLucidMode() == 2) {
                            id = 8880156;
                            field.getTimer().addEvent(() -> field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossLucid, "恭喜擊敗困難模式露希妲！", 3000)), 1000);
                        } else {
                            return;
                        }
                        field.getTimer().addEvent(() -> chr.getField().spawnMob(id, 50, 36, false, hp), 2000);
                        field.setLucidRewardSpawned(true);
                    }
                }
            }
        }
    }

    private static void enterFirstPhase282(Char chr, Field field) {
        if (field.getId() != 450004150) {
            return;
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        // Every party member sees the intro; only the encounter spawn is shared.
        var instance = chr.getInstance();
        synchronized (field) {
            String key = "LucidIntroPlayed:" + chr.getId();
            if (field.getProperty(key) != null) {
                return;
            }
            field.setProperty(key, true);
        }
        sm.beginBossIntro(9167); // actual 282 lusi.json last key: 9.1666 seconds
        sm.blind(1, 255, 0, 0, 0, 0);
        sm.spineScreen(true, false, true, 0, "Map/Effect3.img/BossLucid/Lucid/lusi", "animation", "");
        sm.playSound("Sound/SoundEff.img/ArcaneRiver/lucid_spine", 200);
        field.getTimer().addEvent(() -> {
            if (chr.getField() == field) {
                sm.blind(0, 0, 0, 0, 0, 0);
                sm.endBossIntro();
            }
        }, 9167);

        synchronized (field) {
            if (field.isBossSpawned()) {
                return;
            }
            field.setBossSpawned(true);
        }
        int mode = chr.getLucidMode();
        int mobId = switch (mode) {
            case 0 -> 8880155;
            case 1 -> 8880151;
            default -> 8880153;
        };
        long hp = BossHpConstants.resolveMaxHp(mobId, chr);
        field.getTimer().addEvent(() -> {
            // Keep the current difficulty template and context HP, not the legacy fixed 8880140.
            if (field.getChars().stream().noneMatch(c -> c.getField() == field && c.getInstance() == instance)) {
                field.setBossSpawned(false);
                return;
            }
            field.spawnMob(mobId, 1000, 48, false, hp);
            ScheduledFuture<?> spawn = field.getTimer().addFixedRateEvent(
                    () -> spawnButterFly(field, LucidPhase.FIRST.getVal()), 1000, 5000, false);
            field.setSpawnButterflyTimer(spawn);
            GlobalTimerManager.addFieldTimer(field.getSN(), spawn);
            ScheduledFuture<?> destroy = field.getTimer().addFixedRateEvent(
                    () -> destroyButterFly(field, mode), 33500, 32500, false);
            field.setDestroyButterflyTimer(destroy);
            GlobalTimerManager.addFieldTimer(field.getSN(), destroy);
        }, 9167);
    }

    public static void spawnButterFly(Char chr, int phase) {
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        spawnButterFly(field, phase);
    }

    private static void spawnButterFly(Field field, int phase) {
        switch (isButterflyField(field.getId()) ? 1 : 0) { // dreamtrial-1005: field ids as data
            case 1:
                List<ButterFly> butterflies = new ArrayList<>();
                Position butterflyPosition = ButterFly.getPosition(phase != 1, Randomizer.rand(0, phase == 1 ? ButterFly.BUTTERFLY_POS2.size() - 1 : ButterFly.BUTTERFLY_POS1.size() - 2));
                butterflies.add(new ButterFly(Randomizer.rand(0, 8), butterflyPosition));
                field.setButterFlyCount(field.getButterFlyCount() + 1);
                field.broadcast(LucidPacket.butterFlyInit(butterflies));
                field.broadcast(LucidPacket.butterflyAction(ButterFlyType.Add, butterflies.get(0).getType(), butterflies.get(0).getPos(), 0, 0));
                break;
            default:
                if (field.getSpawnButterflyTimer() != null) {
                    field.getSpawnButterflyTimer().cancel(false);
                    field.setSpawnButterflyTimer(null);
                }
                break;
        }
    }

    public static void cutScene(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        if (Protocol.active().version() == ProtocolVersion.TMS_282_3) {
            if (field.getId() != 450004200 && field.getId() != 450004500 && field.getId() != 450004800) {
                return;
            }
            synchronized (field) {
                String key = "LucidCutscenePlayed:" + chr.getId();
                if (field.getProperty(key) != null) {
                    return;
                }
                field.setProperty(key, true);
            }
            int destination = field.getId() + 50;
            sm.beginBossIntro(4000);
            sm.blind(1, 255, 0, 0);
            field.getTimer().addEvent(() -> {
                if (chr.getField() == field) {
                    sm.spineScreen(true, false, false, 0, "Map/Effect3.img/BossLucid/butterfly2/buterfly", "animation", "");
                }
            }, 1000);
            field.getTimer().addEvent(() -> {
                if (chr.getField() == field) {
                    sm.blind(0, 0, 0, 0, 0, 0);
                    sm.endBossIntro();
                    chr.warp(destination);
                }
            }, 4000);
            return;
        }
        if (field.getId() == 450004800) {
            sm.lockInGameUI(true);
            sm.blind(1, 255, 0, 0);
            field.getTimer().addEvent(() -> sm.spineScreen(true, false, false, 0, "Map/Effect3.img/BossLucid/butterfly2/buterfly", "animation", ""), 1000);
            field.getTimer().addEvent(() -> sm.blind(0, 0, 0, 0, 0, 1000), 3800);
            field.getTimer().addEvent(() -> sm.lockInGameUI(false), 4000);
            field.getTimer().addEvent(() -> chr.warp(chr.getField().getId() + 50), 3900);
        } else if (field.getId() == 450004500) {
            sm.lockInGameUI(true);
            sm.blind(1, 255, 0, 0);
            field.getTimer().addEvent(() -> sm.spineScreen(true, false, false, 0, "Map/Effect3.img/BossLucid/butterfly2/buterfly", "animation", ""), 1000);
            field.getTimer().addEvent(() -> sm.blind(0, 0, 0, 0, 0, 1000), 3800);
            field.getTimer().addEvent(() -> sm.lockInGameUI(false), 4000);
            field.getTimer().addEvent(() -> chr.warp(chr.getField().getId() + 50), 3900);
        } else if (field.getId() == 450004200) {
            sm.lockInGameUI(true);
            sm.blind(1, 255, 0, 0);
            field.getTimer().addEvent(() -> sm.spineScreen(true, false, false, 0, "Map/Effect3.img/BossLucid/butterfly2/buterfly", "animation", ""), 1000);
            field.getTimer().addEvent(() -> sm.blind(0, 0, 0, 0, 0, 1000), 3800);
            field.getTimer().addEvent(() -> sm.lockInGameUI(false), 4000);
            field.getTimer().addEvent(() -> chr.warp(chr.getField().getId() + 50), 3900);
        }
    }

    public static void destroyButterFly(Char chr) {
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        destroyButterFly(field, chr.getLucidMode());
    }

    private static void destroyButterFly(Field field, int mode) {
        switch (isButterflyField(field.getId()) ? 1 : 0) { // dreamtrial-1005: field ids as data
            case 1:
                final int count = field.getButterFlyCount();
                field.setLucidStatueGauge(0);
                field.setButterFlyCount(0);
                List<Integer> chars = new ArrayList<>();
                for (int i = 0; i <= mode * 10 + 20; i++) {
                    Char x = Util.getRandomFromCollection(field.getChars());
                    if (x != null) {
                        chars.add(x.getId());
                    }
                }
                field.broadcast(LucidPacket.butterflyAttack(count, chars));
                field.broadcast(LucidPacket.statueStateChange(false, Math.min(field.getLucidStatueGauge(), 3), true));
                break;
            default:
                if (field.getDestroyButterflyTimer() != null) {
                    field.getDestroyButterflyTimer().cancel(false);
                    field.setDestroyButterflyTimer(null);
                }
                break;
        }
    }

    // ---- dreamtrial-1005: Lucid fight fields as data (was: field ids hardcoded in the switches above and in
    // MobSkill) so the 夢幻的試煉 copies 993290100 (ft147, phase 1) / 993290300 (ft152, phase 2) can reuse the
    // butterfly / dragon / force-chase mechanics. Extra fields are registered by DreamTrialService from its JSON only
    // while tms282.lucid.dreamTrial is on; the normal ids behave exactly as before.

    private static final int[] NORMAL_FIRST_PHASE_FIELDS = {450004150, 450004450, 450004750, 450003840};
    private static final int[] NORMAL_BUTTERFLY_FIELDS = {450004150, 450004250, 450004450, 450004550, 450004750, 450004850};
    private static volatile java.util.Set<Integer> extraFirstPhase = java.util.Set.of();
    private static volatile java.util.Set<Integer> extraSecondPhase = java.util.Set.of();

    /** Registers (replaces) the extra phase-1 / phase-2 Lucid fields; empty sets remove them. */
    public static void setExtraFields(java.util.Set<Integer> firstPhase, java.util.Set<Integer> secondPhase) {
        extraFirstPhase = java.util.Set.copyOf(firstPhase);
        extraSecondPhase = java.util.Set.copyOf(secondPhase);
    }

    /** Fields where the butterfly spawn / destroy timers run. */
    public static boolean isButterflyField(int fieldId) {
        for (int id : NORMAL_BUTTERFLY_FIELDS) {
            if (id == fieldId) {
                return true;
            }
        }
        return extraFirstPhase.contains(fieldId) || extraSecondPhase.contains(fieldId);
    }

    /** Phase-1 fields (MobSkill Lucid slv 7: dragon type 1). */
    public static boolean isFirstPhaseField(int fieldId) {
        for (int id : NORMAL_FIRST_PHASE_FIELDS) {
            if (id == fieldId) {
                return true;
            }
        }
        return extraFirstPhase.contains(fieldId);
    }

    /** Phase-2 fields where the Lucid skill force-chases (was: 450004250 only). */
    public static boolean isSecondPhaseField(int fieldId) {
        return fieldId == 450004250 || extraSecondPhase.contains(fieldId);
    }

    /** Starts the butterfly timers of a fight field (same periods as the normal fight). */
    public static void startButterflies(Field field, int phase, int mode) {
        stopButterflies(field);
        ScheduledFuture<?> spawn = field.getTimer().addFixedRateEvent(() -> spawnButterFly(field, phase), 1000, 5000, false);
        field.setSpawnButterflyTimer(spawn);
        GlobalTimerManager.addFieldTimer(field.getSN(), spawn);
        ScheduledFuture<?> destroy = field.getTimer().addFixedRateEvent(() -> destroyButterFly(field, mode), 33500, 32500, false);
        field.setDestroyButterflyTimer(destroy);
        GlobalTimerManager.addFieldTimer(field.getSN(), destroy);
    }

    /** Cancels the butterfly timers and erases the butterflies on the client. */
    public static void stopButterflies(Field field) {
        if (field.getSpawnButterflyTimer() != null) {
            field.getSpawnButterflyTimer().cancel(false);
            field.setSpawnButterflyTimer(null);
        }
        if (field.getDestroyButterflyTimer() != null) {
            field.getDestroyButterflyTimer().cancel(false);
            field.setDestroyButterflyTimer(null);
        }
        field.setButterFlyCount(0);
    }

    public enum LucidPhase {
        FIRST(0),
        SECOND(1),
        LAST(2),
        REWARD(3),
        ;

        private final int val;

        LucidPhase(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    public enum LucidMode {
        EASY(0),
        NORMAL(1),
        HARD(2),
        ;

        private final int val;

        LucidMode(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }
}
