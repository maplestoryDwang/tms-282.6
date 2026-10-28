package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.enums.ObtacleAtomEnum;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

import java.util.concurrent.ScheduledFuture;

import static net.swordie.ms.life.mob.skill.MobSkillStat.fixDamR;

public class Lotus {

    /** Existing maps/IDs from the local 273 boss_swoo scripts, not the remastered Suu encounter. */
    public static int legacyMobId(LotusPhase phase, LotusMode mode) {
        return BossConstants.LOTUS_CORE + phase.getVal() + (mode == LotusMode.NORMAL ? 100 : 0);
    }

    public static void enter(Char chr, LotusPhase phase, LotusMode mode) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }
        if (chr.getField().isBossSpawned()) {
            if (phase == LotusPhase.FIRST) {
                respawnLaser(chr);
            }
            return;
        }
        // The original summonpre direction script is unavailable locally. This is a
        // supplemental entrance, not a reconstruction of the official cinematic.
        BossHelper.playBossEntry(chr, () -> spawn(chr, phase, mode));
    }

    public static void respawnLaser(Char chr) {
        Field field = chr.getField();
        int mobId = field.getId() == 350060700 ? 8950100 : 8950000;
        Mob mob = (Mob) field.getLifeByTemplateId(mobId);
        if (mob != null) {
            applyLaser(mob);
        }
    }

    private static void applyLaser(Mob mob) {
        MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(MobSkillID.LaserAttack.getVal(), 5);
        if (msi == null) {
            return;
        }
        MobSkill skill = new MobSkill();
        skill.setLevel(5);
        skill.setSkillID(MobSkillID.LaserAttack.getVal());
        skill.setFixDamR(msi.getSkillStatIntValue(fixDamR));
        skill.applyEffect(mob);
    }

    /** The 273 extreme event uses 350068000/200/400 before each combat phase. */
    public static void enterExtremeDirection(Char chr) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }
        Field source = chr.getField();
        int id = source.getId();
        if (id != 350068000 && id != 350068200 && id != 350068400) {
            return;
        }
        if (id != 350068000) {
            // These are black direction maps; the original 2/3phase script is absent.
            BossHelper.playBossEntry(chr, () -> chr.warp(id + 100));
            return;
        }
        // Original WZ map object plays 02_Swoo_Bossmap_Phase1(SwooBattleDirectingScene).
        // Actual 282 Spine 4.1.24 last key is 10.000000953674316s, including
        // deform timelines (original-intros/media/Map/Obj/BossSuu.img/boss/spine/1).
        var instance = chr.getInstance();
        chr.getScriptManager().beginBossIntro(10001);
        chr.getTimer().addEvent(() -> {
            if (chr.getField() == source && chr.getInstance() == instance) {
                chr.getScriptManager().endBossIntro();
                chr.warp(id + 100);
            }
        }, 10001);
    }

    public static int extremeMobId(int fieldId) {
        return switch (fieldId) {
            case 350068100 -> 8881200;
            case 350068300 -> 8881201;
            case 350068500 -> 8881202;
            default -> 0;
        };
    }

    public static void spawnExtreme(Char chr) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }
        Field field = chr.getField();
        int mobId = extremeMobId(field.getId());
        if (mobId == 0) {
            return;
        }
        synchronized (field) {
            if (!field.isBossSpawned()) {
                // Keep native MobData/BossHpConstants resolution; no guessed extreme HP.
                if (field.spawnMob(mobId, 0, -16, false) != null) {
                    field.setBossSpawned(true);
                }
            }
        }
    }

    public static void spawn(Char chr, LotusPhase phase, LotusMode mode) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        Field field = chr.getField();
        int lotusId = legacyMobId(phase, mode);
        long hp = BossConstants.LOTUS_HP_PHASE_DIFFICULTY[phase.getVal()][mode.getVal()];
        synchronized (field) {
            if (field.isBossSpawned()) {
                return;
            }
            field.setBossSpawned(true);

            ScheduledFuture<?> sf = field.getTimer().addFixedRateEvent(() -> {
                if (!field.getChars().isEmpty()) {
                    field.broadcast(FieldPacket.clearObtacle());
                    init(field);
                }
            }, 1000, mode == LotusMode.HARD ? 10000 : 20000, false);
            GlobalTimerManager.addFieldTimer(field.getSN(), sf);

            field.getTimer().addEvent(() -> {
                if (field.getChars().isEmpty()) {
                    field.setBossSpawned(false);
                    return;
                }
                Mob mob = field.spawnMob(lotusId, 0, -16, false, hp);
                if (mob == null) {
                    field.setBossSpawned(false);
                } else if (phase == LotusPhase.FIRST) {
                    applyLaser(mob);
                }
            }, 2000);
            if (field.getId() == 350060700) {
                ScheduledFuture<?> sf2 = field.getTimer().addFixedRateEvent(() -> {
                    if (!field.getChars().isEmpty() && field.hasMobById(lotusId)) {
                        if (!field.hasMobById(BossConstants.LOTUS_SUMMON_RED)) {
                            field.spawnMob(BossConstants.LOTUS_SUMMON_RED, 525, -300, false);
                        }
                        if (!field.hasMobById(BossConstants.LOTUS_SUMMON_BLUE)) {
                            field.spawnMob(BossConstants.LOTUS_SUMMON_BLUE, -360, -450, false);
                        }
                        if (!field.hasMobById(BossConstants.LOTUS_SUMMON_YELLOW)) {
                            field.spawnMob(BossConstants.LOTUS_SUMMON_YELLOW, -525, -300, false);
                        }
                        if (!field.hasMobById(BossConstants.LOTUS_SUMMON_CHAOS)) {
                            field.spawnMob(BossConstants.LOTUS_SUMMON_CHAOS, 360, -450, false);
                        }
                    }
                }, 2000, 10000L, false);
                GlobalTimerManager.addFieldTimer(field.getSN(), sf2);
            } else if (field.getId() == 350060400) {
                ScheduledFuture<?> sf3 = field.getTimer().addFixedRateEvent(() -> {
                    if (!field.getChars().isEmpty() && field.hasMobById(lotusId)) {
                        if (!field.hasMobById(BossConstants.LOTUS_SUMMON_RED_HARD)) {
                            field.spawnMob(BossConstants.LOTUS_SUMMON_RED_HARD, 525, -300, false);
                        }
                        if (!field.hasMobById(BossConstants.LOTUS_SUMMON_BLUE_HARD)) {
                            field.spawnMob(BossConstants.LOTUS_SUMMON_BLUE_HARD, -360, -450, false);
                        }
                        if (!field.hasMobById(BossConstants.LOTUS_SUMMON_YELLOW_HARD)) {
                            field.spawnMob(BossConstants.LOTUS_SUMMON_YELLOW_HARD, -525, -300, false);
                        }
                        if (!field.hasMobById(BossConstants.LOTUS_SUMMON_CHAOS_HARD)) {
                            field.spawnMob(BossConstants.LOTUS_SUMMON_CHAOS_HARD, 360, -450, false);
                        }
                    }
                }, 2000, 5000L, false);
                GlobalTimerManager.addFieldTimer(field.getSN(), sf3);
            }
        }
    }

    private static void init(Field field) {
        Char chr = field.getChars().stream().findFirst().orElse(null);
        if (chr == null) {
            return;
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        switch (field.getId()) {
            case 350060700 -> {
                if (field.hasMobById(8950100)) {
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_BLUE_ATOM_DAMAGE, 15, 0, BossConstants.LOTUS_BLUE_ATOM_AMOUNT, BossConstants.LOTUS_BLUE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusYellowDebris, 1, BossConstants.LOTUS_YELLOW_ATOM_DAMAGE, 20, 0,  BossConstants.LOTUS_YELLOW_ATOM_AMOUNT, BossConstants.LOTUS_YELLOW_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_PURPLE_ATOM_DAMAGE, 25, 0,  BossConstants.LOTUS_PURPLE_ATOM_AMOUNT, BossConstants.LOTUS_PURPLE_ATOM_PROP);
                }
            }
            case 350060800 -> {
                if (field.hasMobById(8950101)) {
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_BLUE_ATOM_DAMAGE, 15, 0,  BossConstants.LOTUS_BLUE_ATOM_AMOUNT, BossConstants.LOTUS_BLUE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusYellowDebris, 1, BossConstants.LOTUS_YELLOW_ATOM_DAMAGE, 20, 0,  BossConstants.LOTUS_YELLOW_ATOM_AMOUNT, BossConstants.LOTUS_YELLOW_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_PURPLE_ATOM_DAMAGE, 25, 0,  BossConstants.LOTUS_PURPLE_ATOM_AMOUNT, BossConstants.LOTUS_PURPLE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusRobotDebris, 1, BossConstants.LOTUS_ROBOT_ATOM_DAMAGE, 10, 0,  BossConstants.LOTUS_ROBOT_ATOM_AMOUNT, BossConstants.LOTUS_ROBOT_ATOM_PROP);
                }
            }
            case 350060900 -> {
                if (field.hasMobById(8950102)) {
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_BLUE_ATOM_DAMAGE, 15, 0,  BossConstants.LOTUS_BLUE_ATOM_AMOUNT, BossConstants.LOTUS_BLUE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusYellowDebris, 1, BossConstants.LOTUS_YELLOW_ATOM_DAMAGE, 20, 0,  BossConstants.LOTUS_YELLOW_ATOM_AMOUNT, BossConstants.LOTUS_YELLOW_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_PURPLE_ATOM_DAMAGE, 25, 0,  BossConstants.LOTUS_PURPLE_ATOM_AMOUNT, BossConstants.LOTUS_PURPLE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusRobotDebris, 1, BossConstants.LOTUS_ROBOT_ATOM_DAMAGE, 10, 0,  BossConstants.LOTUS_ROBOT_ATOM_AMOUNT, BossConstants.LOTUS_ROBOT_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusCrusherDebris, 1, BossConstants.LOTUS_CRUSHER_ATOM_DAMAGE, 5, 0,  BossConstants.LOTUS_CRUSHER_ATOM_AMOUNT, BossConstants.LOTUS_CRUSHER_ATOM_PROP);
                }
            }
            case 350060400 -> {
                if (field.hasMobById(8950000)) {
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_BLUE_ATOM_DAMAGE, 15, 0,  BossConstants.LOTUS_BLUE_ATOM_AMOUNT, BossConstants.LOTUS_BLUE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusYellowDebris, 1, BossConstants.LOTUS_YELLOW_ATOM_DAMAGE, 20, 0,  BossConstants.LOTUS_YELLOW_ATOM_AMOUNT, BossConstants.LOTUS_YELLOW_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_PURPLE_ATOM_DAMAGE, 25, 0,  BossConstants.LOTUS_PURPLE_ATOM_AMOUNT, BossConstants.LOTUS_PURPLE_ATOM_PROP);

                }
            }
            case 350060500 -> {
                if (field.hasMobById(8950001)) {
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_BLUE_ATOM_DAMAGE, 15, 0,  BossConstants.LOTUS_BLUE_ATOM_AMOUNT, BossConstants.LOTUS_BLUE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusYellowDebris, 1, BossConstants.LOTUS_YELLOW_ATOM_DAMAGE, 20, 0,  BossConstants.LOTUS_YELLOW_ATOM_AMOUNT, BossConstants.LOTUS_YELLOW_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_PURPLE_ATOM_DAMAGE, 25, 0,  BossConstants.LOTUS_PURPLE_ATOM_AMOUNT, BossConstants.LOTUS_PURPLE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusRobotDebris, 1, BossConstants.LOTUS_ROBOT_ATOM_DAMAGE, 10, 0,  BossConstants.LOTUS_ROBOT_ATOM_AMOUNT, BossConstants.LOTUS_ROBOT_ATOM_PROP);
                }
            }
            case 350060600 -> {
                if (field.hasMobById(8950002)) {
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_BLUE_ATOM_DAMAGE, 15, 0,  BossConstants.LOTUS_BLUE_ATOM_AMOUNT, BossConstants.LOTUS_BLUE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusYellowDebris, 1, BossConstants.LOTUS_YELLOW_ATOM_DAMAGE, 20, 0,  BossConstants.LOTUS_YELLOW_ATOM_AMOUNT, BossConstants.LOTUS_YELLOW_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusBlueDebris, 1, BossConstants.LOTUS_PURPLE_ATOM_DAMAGE, 25, 0,  BossConstants.LOTUS_PURPLE_ATOM_AMOUNT, BossConstants.LOTUS_PURPLE_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusRobotDebris, 1, BossConstants.LOTUS_ROBOT_ATOM_DAMAGE, 10, 0,  BossConstants.LOTUS_ROBOT_ATOM_AMOUNT, BossConstants.LOTUS_ROBOT_ATOM_PROP);
                    sm.createObstacleAtom(ObtacleAtomEnum.LotusCrusherDebris, 1, BossConstants.LOTUS_CRUSHER_ATOM_DAMAGE, 5, 0,  BossConstants.LOTUS_CRUSHER_ATOM_AMOUNT, BossConstants.LOTUS_CRUSHER_ATOM_PROP);
                }
            }
        }
    }

    public enum LotusMode {
        NORMAL(0),
        HARD(1),
        ;

        private final int val;

        LotusMode(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    public enum LotusPhase {
        FIRST(0),
        SECOND(1),
        LAST(2),
        ;

        private final int val;

        LotusPhase(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }
}
