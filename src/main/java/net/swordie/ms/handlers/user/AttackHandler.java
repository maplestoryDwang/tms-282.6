package net.swordie.ms.handlers.user;

import net.swordie.ms.client.jobs.special.SpecialJobProgression;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.client.character.tms282.Tms282VMatrixSpecialEngine;

import net.swordie.ms.BannedMachines;
import net.swordie.ms.DiscordAPI;
import net.swordie.ms.client.User;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Jianghu.Lynn;
import net.swordie.ms.client.jobs.Jianghu.MoXuan;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.adventurer.Kinesis;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.pirate.Buccaneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Corsair;
import net.swordie.ms.client.jobs.adventurer.warrior.Hero;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.anima.HoYoung;
import net.swordie.ms.client.jobs.anima.Ren;
import net.swordie.ms.client.jobs.cygnus.BlazeWizard;
import net.swordie.ms.client.jobs.cygnus.DawnWarrior;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.cygnus.WindArcher;
import net.swordie.ms.client.jobs.flora.Adele;
import net.swordie.ms.client.jobs.legend.Mercedes;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.nova.AngelicBuster;
import net.swordie.ms.client.jobs.nova.Cadena;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.client.jobs.resistance.BattleMage;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.client.jobs.resistance.WildHunter;
import net.swordie.ms.client.jobs.resistance.Xenon;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.connection.protocol.codec.Gms265AttackDecoder;
import net.swordie.ms.constants.*;
import net.swordie.ms.console.ConsoleDebugMode;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.enums.StylishKillType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.*;
import net.swordie.ms.world.field.Field;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class AttackHandler {

    public static void handleAttack(Char chr, AttackInfo attackInfo) {
        handleAttack(chr, attackInfo, null);
    }

    public static void handleAttack(Char chr, AttackInfo attackInfo, net.swordie.ms.client.character.tms282.Tms282BuffBook book) {
        final boolean oniAttack = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.client.jobs.sengoku.Tms282KannaOniController.isGhostAttack(attackInfo.skillId);
        final var oniGrant = oniAttack
                ? net.swordie.ms.client.jobs.sengoku.Tms282KannaOniController.attackGrant(chr, attackInfo) : null;
        if (oniAttack && oniGrant == null) {
            if (chr != null) chr.dispose();
            return;
        }
        if (net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.rejectUnimplementedAttack(attackInfo.skillId)) {
            if (chr != null) chr.dispose();
            return;
        }
        final boolean weaponBurst = net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.requiresBurstGrant(attackInfo.skillId);
        final var burstGrant = weaponBurst
                ? net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.claimBurst(chr, attackInfo) : null;
        if (weaponBurst && burstGrant == null) {
            if (chr != null) chr.dispose();
            return;
        }
        if (weaponBurst) {
            attackInfo.slv = 1;
            attackInfo.skillInfo = SkillData.getSkillInfoById(attackInfo.skillId);
        }
        boolean specialSkill = SpecialJobProgression.isSpecialSkill(attackInfo.skillId);
        if (specialSkill) {
            int level = SpecialJobProgression.requestLevel(chr, attackInfo.skillId);
            if (level <= 0) {
                if (chr != null) chr.dispose();
                return;
            }
            if (attackInfo.skillId == 121002003
                    && !SpecialJobProgression.claimBeastBreathingAttack(chr, attackInfo, System.currentTimeMillis())) {
                chr.dispose();
                return;
            }
            attackInfo.slv = level;
            attackInfo.skillInfo = SkillData.getSkillInfoById(attackInfo.skillId);
        }
        var isBattleRecordOn = chr.isBattleRecordOn();
        var skillID = attackInfo.skillId;
        final boolean paidTempest = net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.handles(skillID);
        final AttackGatePolicy gates = AttackGatePolicy.active(); // 265: every gate enforced (unchanged)
        if (skillID == 0) {
            attackInfo.slv = 1;
        } else if (paidTempest) {
            attackInfo.slv = net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents.useLevel(chr, skillID);
            if (attackInfo.slv <= 0) { chr.dispose(); return; }
        } else if (!specialSkill && !weaponBurst && !oniAttack) {
            if (attackInfo.slv == 0) {
                attackInfo.slv = chr.getSkillLevel(skillID);
            } else {
                int curSLV = chr.getSkillLevel(skillID);
                if (chr.hasSkill(skillID) && attackInfo.slv != curSLV) {
                    attackInfo.slv = curSLV;
                }
            }
        }

        if (isBattleRecordOn) chr.write(BattleRecordMan.killDamageInfo(skillID, attackInfo.totalDamageDealt));

        boolean paidStorm = net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.handles(skillID);
        if (paidStorm) {
            attackInfo.slv = chr.getSkillLevel(net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.parent(skillID));
            attackInfo.skillInfo = SkillData.getSkillInfoById(skillID);
        }
        net.swordie.ms.life.AffectedArea arrowStormArea = null;
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()
                && skillID == net.swordie.ms.client.jobs.adventurer.archer.BowMaster.STORM_OF_ARROWS_AA) {
            // Native16B0644 requires job310..312;1095946->1095A57 derives this child from400031002.
            attackInfo.slv = chr.getSkillLevel(net.swordie.ms.client.jobs.adventurer.archer.BowMaster.STORM_OF_ARROWS);
            var life = chr.getField() == null ? null : chr.getField().getLifeByObjectID(attackInfo.affectedAreaObjId);
            if (!JobConstants.isBowMaster(chr.getJob()) || attackInfo.slv <= 0 || !chr.isOnline() || chr.getHP() <= 0
                    || chr.getClient() == null || chr.getClient().getChr() != chr || chr.getClient().getCurrentState() != Client.IN_FIELD
                    || attackInfo.inHeader != InHeader.USER_AREA_DOT_ATTACK
                    || !(life instanceof net.swordie.ms.life.AffectedArea aa) || aa.getOwner() != chr
                    || aa.getSkillID() != skillID || aa.getSlv() != attackInfo.slv || !aa.hasHitMob()) { chr.dispose(); return; }
            arrowStormArea = aa;
        }
        final var checkedArrowStormArea = arrowStormArea;
        int slv = attackInfo.slv;

        Field field = chr.getField();

        if ((field.getFieldLimit() & FieldOption.SkillLimit.getVal()) > 0
                || (field.getFieldLimit() & FieldOption.MoveSkillOnly.getVal()) > 0) {
            chr.dispose();
            return;
        }

        boolean paidTanjiroDash = skillID == 121001008
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        if (paidTanjiroDash && (Byte.toUnsignedInt(attackInfo.hits) > 5 || attackInfo.mobCount > 5
                || attackInfo.mobAttackInfo.size() > 5
                || !net.swordie.ms.client.character.tms282.Tms282BuffDirectory.INSTANCE
                    .consumeTanjiroDashAttack(chr.getId(), System.currentTimeMillis()))) {
            chr.dispose();
            return;
        }
        boolean paidCosmos = skillID == DawnWarrior.COSMOS
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        Option cosmosCast = paidCosmos ? chr.getTemporaryStatManager().getOption(Cosmos) : null;
        if (paidCosmos && (!(chr.getJobHandler() instanceof DawnWarrior dawn)
                || !dawn.canAttackWithCosmos(attackInfo, cosmosCast, System.currentTimeMillis()))) {
            chr.dispose();
            return;
        }
        boolean paidPhoenix = BlazeWizard.isPhoenixDrive(skillID)
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        Option phoenixCast = paidPhoenix ? chr.getTemporaryStatManager().getOptByCTSAndSkill(DevilishPower, skillID) : null;
        if (paidPhoenix && (!(chr.getJobHandler() instanceof BlazeWizard blaze)
                || !blaze.canAttackWithPhoenix(attackInfo, phoenixCast, System.currentTimeMillis()))) {
            chr.dispose();
            return;
        }
        var stormGrant = paidStorm ? net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.claim(chr, attackInfo, System.currentTimeMillis()) : null;
        if (paidStorm && stormGrant == null) { chr.dispose(); return; }
        boolean paidOmega = net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.handles(skillID);
        var omegaGrant = paidOmega ? net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.claim(chr,attackInfo,System.currentTimeMillis()) : null;
        if(paidOmega && omegaGrant==null){chr.dispose();return;}
        boolean paidAdeleShards = net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()
                && net.swordie.ms.connection.packet.Tms282AdeleShardPackets.handles(skillID);
        var shardGrant = paidAdeleShards && chr.getJobHandler() instanceof net.swordie.ms.client.jobs.flora.Adele adele
                ? adele.shardbreaker().claim(attackInfo,System.currentTimeMillis()) : null;
        if(paidAdeleShards && shardGrant==null){chr.dispose();return;}
        var tempestGrant = paidTempest ? net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.claim(chr,book,attackInfo,System.currentTimeMillis()) : null;
        if(paidTempest && tempestGrant==null){chr.dispose();return;}
        boolean noCoolTimeAttackHeader = attackInfo.attackHeader == OutHeader.SUMMONED_ATTACK ||
                                         attackInfo.inHeader == InHeader.USER_AREA_DOT_ATTACK ||
                                         attackInfo.inHeader == InHeader.USER_BODY_ATTACK ||
                                         attackInfo.inHeader == InHeader.USER_AFFECTED_AREA_FOR_SCREEN_ATTACK;
        boolean paidInferno = skillID == BlazeWizard.INFERNO_SPHERE
                && net.swordie.ms.connection.packet.Tms282RemoteBuffs.active();
        Option infernoCast = paidInferno && chr.getJobHandler() instanceof BlazeWizard blaze
                ? blaze.claimInferno(attackInfo, System.currentTimeMillis()) : null;
        if (paidInferno && infernoCast == null) { chr.dispose(); return; }
        if (!weaponBurst && !paidTempest && !paidTanjiroDash && !paidCosmos && !paidPhoenix && !paidInferno && !paidOmega && !paidStorm && !attackInfo.byUnreliableMemory && !noCoolTimeAttackHeader && !chr.applyMpCon(attackInfo.skillInfo, slv, false)
                && gates.enforce(chr, skillID, AttackGatePolicy.Gate.MP_CON)) {
            chr.dispose();
            return;
        }

        if (!weaponBurst && attackInfo.inHeader == InHeader.USER_SHOOT_ATTACK && !chr.applyBulletCon(attackInfo.skillInfo, slv)
                && gates.enforce(chr, skillID, AttackGatePolicy.Gate.BULLET_CON)) {
            chr.dispose();
            return;
        }

        final boolean isCheckAndSetCoolTime = weaponBurst || paidTempest || paidTanjiroDash || paidCosmos || paidPhoenix || paidOmega || paidInferno || paidStorm || paidAdeleShards || chr.checkAndSetSkillCooltime(skillID, true)
                || !gates.enforce(chr, skillID, AttackGatePolicy.Gate.COOLTIME);

        if (isCheckAndSetCoolTime || noCoolTimeAttackHeader || attackInfo.byUnreliableMemory) {

            final var mobList = attackInfo.mobAttackInfo;
            final int mobSize = mobList == null ? 0 : mobList.size();

            final int[] mobIds;
            final long[] totals;
            final long[] minHits;

            if (mobSize > 0) {
                mobIds = new int[mobSize];
                totals = new long[mobSize];
                minHits = new long[mobSize];

                for (int i = 0; i < mobSize; i++) {
                    MobAttackInfo mai = mobList.get(i);
                    mobIds[i] = mai.mobId;

                    long totalDamage = 0L;
                    long minDamage = 0L;
                    final long[] dmgArr = mai.damages;
                    for (long damage : dmgArr) {
                        totalDamage += damage;
                        if (chr.isGM() && ConsoleDebugMode.isActive()) {
                            System.out.println("[ATTACK SKILL] " + skillID + " : " + Util.getNumberFormat(damage));
                        }
                        if (minDamage == 0L || damage < minDamage) minDamage = damage;
                    }
                    totals[i] = totalDamage;
                    minHits[i] = minDamage;
                }
            } else {
                mobIds = null;
                totals = null;
                minHits = null;
            }

            field.addTask(() -> {
                if (oniAttack && (chr.getField() != field
                        || !net.swordie.ms.client.jobs.sengoku.Tms282KannaOniController.validAttack(chr, attackInfo, oniGrant)
                        || attackInfo.slv != slv)) return;
                if (checkedArrowStormArea != null && (chr.getField() != field || !chr.isOnline() || chr.getHP() <= 0
                        || chr.getClient() == null || chr.getClient().getChr() != chr || chr.getClient().getCurrentState() != Client.IN_FIELD
                        || !JobConstants.isBowMaster(chr.getJob())
                        || chr.getSkillLevel(net.swordie.ms.client.jobs.adventurer.archer.BowMaster.STORM_OF_ARROWS) != slv
                        || field.getLifeByObjectID(checkedArrowStormArea.getObjectId()) != checkedArrowStormArea)) return;
                if(paidStorm&&!net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.validGrant(chr,stormGrant,System.currentTimeMillis()))return;
                if(paidAdeleShards && (!(chr.getJobHandler() instanceof net.swordie.ms.client.jobs.flora.Adele adele)
                        || !adele.shardbreaker().valid(shardGrant,System.currentTimeMillis())))return;
                if(paidTempest&&!net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.validGrant(chr,tempestGrant,System.currentTimeMillis()))return;
                if(paidOmega&&!net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.validGrant(chr,omegaGrant,System.currentTimeMillis()))return;
                if (paidInferno && (chr.getField() != field
                        || !(chr.getJobHandler() instanceof BlazeWizard blaze)
                        || !blaze.validInferno(infernoCast, System.currentTimeMillis()))) return;
                if (paidCosmos && (chr.getField() != field
                        || !(chr.getJobHandler() instanceof DawnWarrior dawn)
                        || !dawn.canAttackWithCosmos(attackInfo, cosmosCast, System.currentTimeMillis()))) return;
                if (paidPhoenix && (chr.getField() != field
                        || !(chr.getJobHandler() instanceof BlazeWizard blaze)
                        || !blaze.canAttackWithPhoenix(attackInfo, phoenixCast, System.currentTimeMillis()))) return;
                if (weaponBurst && !net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects
                        .validAttack(chr, burstGrant, attackInfo, System.currentTimeMillis())) return;
                long start = 0;
                if (chr.isGM()) {
                    start = System.currentTimeMillis();
                }

                int multiKillMessage = 0;
                long mobexp = 0;

                long now = System.currentTimeMillis();
                if (Tms282VMatrixRuntime.managed(chr)) {
                    // Once per accepted attack, outside the effect budget/party fan-out below.
                    Tms282VMatrixRuntime.attack(chr, attackInfo);
                    if (skillID > 0 && slv > 0 && attackInfo.skillInfo != null
                            && (specialSkill || chr.getSkillLevel(skillID) > 0)) {
                        Tms282VMatrixRuntime.event(chr, Tms282VMatrixSpecialEngine.Type.SKILL_USE, 1, 0);
                    }
                }
                if (chr.tryConsumeAttackEffectBudget(skillID, now, 50)) {
                    try {
                        handleAttack(chr, attackInfo.skillInfo, attackInfo, skillID, slv, now);
                    } catch (Exception e) {
                        DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                    }
                }

                if (attackInfo.attackHeader != null) {
                    if (attackInfo.attackHeader == OutHeader.SUMMONED_ATTACK && attackInfo.summon != null) {
                        field.broadcast(Summoned.attack(chr, attackInfo, false), chr);
                    } else {
                        if (!SkillConstants.notSureRemoteAttackSkill(skillID)) {
                            field.broadcast(UserRemote.attack(chr, attackInfo), chr);
                        }
                    }
                }

                if (mobSize <= 0) {
                    return;
                }

                final Map<Integer, Life> lifes = field.getLifes();
                boolean corsairHitNotified = false;

                for (int i = 0; i < mobSize; i++) {
                    Life life = lifes.get(mobIds[i]);
                    if (!(life instanceof Mob mob)) {
                        continue;
                    }
                    if (mob.getHp() <= 0) {
                        continue;
                    }

                    long totalDamage = totals[i];
                    if (totalDamage <= 0) {
                        continue;
                    }

                    // Clamp to current HP to avoid negative & unnecessary work
                    long hp = mob.getHp();
                    long hpLeft = hp - totalDamage;
                    if (hpLeft < 0) {
                        totalDamage += hpLeft; // subtract overflow
                        if (totalDamage <= 0) {
                            continue;
                        }
                    }

                    if (attackInfo.attackHeader != OutHeader.SUMMONED_ATTACK &&
                            attackInfo.attackHeader != OutHeader.FAMILIAR_ATTACK &&
                            attackInfo.attackHeader != OutHeader.REMOTE_BODY_ATTACK) {
                        totalDamage = mob.handleDamageReflect(chr, attackInfo.attackHeader, skillID, totalDamage);
                        if (totalDamage <= 0) {
                            continue;
                        }
                        totalDamage = mob.handleDamageImmune(chr, attackInfo.attackHeader, totalDamage, mobList.get(i).damages);
                        if (totalDamage <= 0) {
                            continue;
                        }
                    }

                    try {

                        if (chr.isGM() && gates.gmOneHitKill(chr, mob)) {
                            totalDamage = Long.MAX_VALUE;
                        }

                        long hpBeforeAppliedHit = mob.getHp();
                        boolean isKilled = mob.damage(chr, totalDamage, skillID);
                        if ((isKilled || mob.getHp() < hpBeforeAppliedHit)
                                && skillID == net.swordie.ms.client.jobs.resistance.demon.Tms282DemonicBlast.SKILL
                                && chr.getJobHandler() instanceof DemonAvenger avenger) {
                            try {
                                avenger.onVerifiedDemonicBlastHit(attackInfo, mob, isKilled);
                            } catch (RuntimeException effectFailure) {
                                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, effectFailure);
                            }
                        }
                        if (!corsairHitNotified && (isKilled || mob.getHp() < hpBeforeAppliedHit)
                                && chr.getJobHandler() instanceof Corsair corsair) {
                            corsairHitNotified = true;
                            try {
                                corsair.onVerifiedHeadshotHit(skillID);
                            } catch (RuntimeException effectFailure) {
                                // Damage already happened; a derived skill effect must not suppress kill/EXP accounting.
                                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, effectFailure);
                            }
                        }
                        if (isKilled) {
                            multiKillMessage++;
                            long exp = mob.getForcedMobStat().getExp();
                            if (exp > mobexp) mobexp = exp;
                            if (isBattleRecordOn) chr.write(BattleRecordMan.enemiesDefeatedUpdate());
                        } else {
                            try {
                                long minHit = minHits[i];
                                if (minHit > 0) {
                                    try {
                                        mob.handleDebuffOnMob(chr, attackInfo.skillInfo, skillID, slv, minHit);
                                    } catch (Exception e) {
                                        DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                                    }
                                }
                            } catch (Exception e) {
                                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                            }
                        }
                    } catch (Exception e) {
                        DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                    }
                }
                if (multiKillMessage > 2 && mobexp > 0) {
                    int bonusExpMultiplier = (multiKillMessage - 2) * 5;
                    long totalBonusExp = (long) (mobexp * (bonusExpMultiplier * GameConstants.MULTI_KILL_BONUS_EXP_MULTIPLIER));
                    chr.write(UserLocal.comboCounter(StylishKillType.MULTI_KILL, (int) totalBonusExp, Math.min(multiKillMessage, 10)));
                    chr.addExpNoMsg(totalBonusExp);
                    AchievementHandler.handleMultiKO(chr, multiKillMessage);
                }
                if (chr.isGM() && ConsoleDebugMode.isActive()) {
                    long total = System.currentTimeMillis() - start;
                    System.out.printf("技能處理耗時 %s (%d): %d ms。%n", StringData.getSkillStringById(skillID).getName(), skillID, total);
                }
            });
        }
    }

    private static void handleAttack(Char chr, SkillInfo si, AttackInfo attackInfo, int skillID, int slv, long now) {
        var src = chr.getJobHandler();
        if (si != null) {
            src.handleAttack(chr.getClient(), attackInfo, si, now);
            if (si.isMassSpell() && chr.getParty() != null && chr.getParty().getPartyMembersInSameField(chr).size() > 1) {
                var r = si.getFirstRect();
                if (r != null) {
                    var rectAround = chr.getRectAround(r);
                    for (var pmChr : chr.getParty().getPartyMembersInSameField(chr)) {
                        if (pmChr != null && rectAround.hasPositionInside(pmChr.getPosition())) {
                            src.handleAttack(chr.getClient(), attackInfo, si, now);
                            var effect = Effect.skillAffected(skillID, slv, 0);
                            pmChr.write(UserPacket.effect(effect));
                            chr.getField().broadcast(UserRemote.effect(pmChr.getId(), effect), pmChr);
                        }
                    }
                }
            }
        }
    }

    @Handler(ops = InHeader.USER_BODY_ATTACK)
    public static void handleBodyAttack(Char chr, InPacket inPacket) {
        AttackInfo ai = new AttackInfo();
        ai.attackHeader = OutHeader.REMOTE_BODY_ATTACK;
        inPacket.decodeInt();
        inPacket.decodeInt();
        ai.fieldKey = inPacket.decodeByte();
        byte mask = inPacket.decodeByte();
        ai.hits = (byte) (mask & 0xF);
        ai.mobCount = (mask >>> 4) & 0xF;
        ai.skillId = inPacket.decodeInt();
        ai.slv = inPacket.decodeInt();
        ai.crc1 = inPacket.decodeInt();
        ai.crc2 = inPacket.decodeInt();
        ai.crc3 = inPacket.decodeInt();

        int skillID = ai.skillId;

        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null && skillID != 0) {
            if (chr.isGM()) {
                chr.chatScriptMessage("技能 " + skillID + " 尚未實作。");
            }
            chr.dispose();
            return;
        } else {
            ai.skillInfo = si;
        }

        attackBonusReader(inPacket, ai);

        SkillUseInfo skillUseInfo = new SkillUseInfo(si, ai.slv);
        ProcessType processType = new ProcessType(skillUseInfo);
        processType.decode(inPacket);
        ai.processType = processType;

        if (SkillConstants.isZeroSkill(ai.skillId)) {
            ai.zeroTag = inPacket.decodeByte();
        }
        ai.areaPAD = inPacket.decodeByte() >>> 3;
        byte nul = inPacket.decodeByte(); // encoded as 0
        short actionMask = inPacket.decodeShort();
        ai.left = ((actionMask >>> 15) & 1) != 0;
        ai.attackAction = (short) (actionMask & 0x7FFF);
        ai.attackCount = inPacket.decodeInt();
        ai.attackSpeed = inPacket.decodeByte();

        inPacket.skipByte();

        ai.wt = inPacket.decodeInt();
        ai.ar01Mad = inPacket.decodeInt();

        if (ai.skillId > 0) {
            for (int i = 0; i < ai.mobCount; i++) {
                MobAttackInfo mai = new MobAttackInfo();
                mai.mobId = inPacket.decodeInt();

                mai.hitAction = inPacket.decodeByte();
                mai.left = inPacket.decodeByte();
                mai.idk3 = inPacket.decodeByte();
                mai.forceActionAndLeft = inPacket.decodeByte();
                mai.frameIdx = inPacket.decodeByte();

                mai.templateID = inPacket.decodeInt();

                mai.calcDamageStatIndexAndDoomed = inPacket.decodeByte(); // 1st bit for bDoomed, rest for calcDamageStatIndex

                mai.hitX = inPacket.decodeShort();
                mai.hitY = inPacket.decodeShort();
                mai.oldPosX = inPacket.decodeShort(); // ?
                mai.oldPosY = inPacket.decodeShort(); // ?

                mai.idk6 = inPacket.decodeShort();
                mai.hitAction = inPacket.decodeByte();
                mai.idk7 = inPacket.decodeInt();
                mai.idk8 = inPacket.decodeInt();
                long totalDamage = 0;
                mai.damages = new long[ai.hits];
                for (int j = 0; j < ai.hits; j++) {
                    long damage = inPacket.decodeLong();
                    totalDamage += damage;
                    mai.damages[j] = damage;
                }
                ai.totalDamageDealt = totalDamage;
                mai.mobUpDownYRange = inPacket.decodeInt();
                mai.crc1 = inPacket.decodeInt(); // crc
                mai.crc2 = inPacket.decodeInt(); // crc
                parseAttackInfoPacket(inPacket, mai);
                ai.mobAttackInfo.add(mai);
            }
        }
        ai.pos = inPacket.decodePosition();
        handleAttack(chr, ai);
    }

    @Handler(op = InHeader.SUMMONED_ATTACK)
    public static void handleSummonedAttack(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        AttackInfo ai = new AttackInfo();
        int summonedID = inPacket.decodeInt();
        ai.attackHeader = OutHeader.SUMMONED_ATTACK;
        Life life = field.getLifeByObjectID(summonedID);
        if (life == null) {
            return;
        }
        if (!(life instanceof Summon summon)) {
            return;
        }
        ai.summon = summon;
        ai.updateTime = inPacket.decodeInt();
        ai.skillId = inPacket.decodeInt();
        ai.summonSpecialSkillId = inPacket.decodeInt();

        int skillID = ai.skillId;

        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null && skillID != 0) {
            if (chr.isGM()) {
                chr.chatScriptMessage("技能 " + skillID + " 尚未實作。");
            }
            chr.dispose();
            return;
        } else {
            ai.skillInfo = si;
        }

        summonAttackBonusReader(inPacket, ai);

        byte leftAndAction = inPacket.decodeByte();
        ai.attackActionType = (byte) (leftAndAction & 0x7F);
        ai.left = (byte) (leftAndAction >>> 7) != 0;
        byte mask = inPacket.decodeByte();
        ai.hits = (byte) (mask & 0xF);
        ai.mobCount = (mask >>> 4) & 0xF;
        inPacket.decodeByte(); // hardcoded 0
        ai.pos = inPacket.decodePosition();
        inPacket.decodePosition(); // new pos i guess
        inPacket.decodeByte(); // 0
        inPacket.decodeInt(); // hardcoded 0xFFFFFFFF
        inPacket.decodeShort();
        inPacket.decodeInt();
        inPacket.decodeInt();
        if (skillID == AngelicBuster.MIGHTY_MASCOT
                || skillID == Corsair.BROADSIDE_SUMMON
                || skillID == Corsair.HEXA_BROADSIDE_SUMMON) {
            inPacket.decodeInt();
        }
        if (skillID == 5221028 || skillID == 5241016) {
            ai.grenadePos = inPacket.decodePositionInt();
        }
        for (int i = 0; i < ai.mobCount; i++) {
            MobAttackInfo mai = new MobAttackInfo();
            mai.mobId = inPacket.decodeInt();
            mai.templateID = inPacket.decodeInt();
            mai.hitAction = inPacket.decodeByte();
            mai.left = inPacket.decodeByte();
            mai.idk3 = inPacket.decodeByte();
            mai.forceActionAndLeft = inPacket.decodeByte();
            mai.frameIdx = inPacket.decodeByte();
            mai.templateID = inPacket.decodeInt();
            mai.calcDamageStatIndexAndDoomed = inPacket.decodeByte(); // 1st bit for bDoomed, rest for calcDamageStatIndex
            mai.hitX = inPacket.decodeShort();
            mai.hitY = inPacket.decodeShort();
            mai.oldPosX = inPacket.decodeShort(); // ?
            mai.oldPosY = inPacket.decodeShort(); // ?
            int idk8 = inPacket.decodeInt(); //
            mai.idk6 = inPacket.decodeShort();
            mai.idk7 = inPacket.decodeInt();
            mai.idk8 = inPacket.decodeInt();
            mai.idk9 = inPacket.decodeByte();
            long totalDamage = 0;
            mai.damages = new long[ai.hits];
            for (int j = 0; j < ai.hits; j++) {
                long damage = inPacket.decodeLong();
                totalDamage += damage;
                mai.damages[j] = damage;
            }
            ai.totalDamageDealt = totalDamage;
            mai.mobUpDownYRange = inPacket.decodeInt();
            parseAttackInfoPacket(inPacket, mai);
            ai.mobAttackInfo.add(mai);
        }
        handleAttack(chr, ai);
    }

    public static void handleAttack(Char chr, InPacket inPacket, InHeader header) {
        Gms265AttackDecoder.INSTANCE.decode(chr, inPacket, header).ifPresent(request -> {
            try {
                boolean verify = !isExploitSkill(chr, request.info());
                if (verify) {
                    handleAttack(chr, request.info());
                }
            } catch (Exception e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_ATTACK, String.format("[%s] Skill ID: %d unable to handle Attacks.", chr.getName(), request.requestedSkillId()), false);
                chr.dispose();
            }
        });
    }

    public static boolean[] calculateCriticalsByAverage(long[] damages, int totalCr) {
        return Gms265AttackDecoder.calculateCriticalsByAverage(damages, totalCr);
    }

    @Handler(op = InHeader.FAMILIAR_ATTACK)
    public static void handleFamiliarAttack(Char chr, InPacket inPacket) {
        inPacket.decodeByte(); // ?
        int familiarID = inPacket.decodeInt();
        if (chr.getActiveFamiliar() == null || chr.getActiveFamiliar().getFamiliarID() != familiarID) {
            return;
        }
        AttackInfo ai = new AttackInfo();
        ai.attackHeader = OutHeader.FAMILIAR_ATTACK;
        ai.fieldKey = inPacket.decodeByte();
        ai.skillId = inPacket.decodeInt();
        ai.idk = inPacket.decodeByte();
        ai.slv = 1;

        int skillID = ai.skillId;

        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null && skillID != 0) {
            if (chr.isGM()) {
                chr.chatScriptMessage("技能 " + skillID + " 尚未實作。");
            }
            chr.dispose();
            return;
        } else {
            ai.skillInfo = si;
        }

        ai.mobCount = inPacket.decodeByte();
        for (int i = 0; i < ai.mobCount; i++) {
            MobAttackInfo mai = new MobAttackInfo();
            mai.mobId = inPacket.decodeInt();
            inPacket.decodeInt();
            mai.byteIdk1 = inPacket.decodeByte();
            inPacket.decodeInt();
            mai.byteIdk2 = inPacket.decodeByte();
            inPacket.decodeInt();
            ai.hits = inPacket.decodeByte();
            long totalDamage = 0;
            mai.damages = new long[ai.hits];
            for (int j = 0; j < ai.hits; j++) {
                long damage = inPacket.decodeLong();
                totalDamage += damage;
                mai.damages[j] = damage;
            }
            ai.totalDamageDealt = totalDamage;
            ai.mobAttackInfo.add(mai);
        }
        inPacket.decodeInt();
        handleAttack(chr, ai);
    }

    @Handler(op = InHeader.USER_MOVING_SHOOT_ATTACK_PREPARE)
    public static void handleMovingShootAttackPrepare(Char chr, InPacket inPacket) {
        if (chr == null) return;
        int skillID = inPacket.decodeInt();
        short mask = inPacket.decodeShort();
        boolean isLeft = (mask >> 15) != 0;
        int action = mask & 0x7FFF;
        byte actionSpeed = inPacket.decodeByte();
    }

    @Handler(op = InHeader.ADD_ATTACK_RESET)
    public static void handleAddAttackReset(Char chr, InPacket inPacket) {
        int parentSkillID = inPacket.decodeInt();
        if (JobConstants.isZero(chr.getJob())) {
            return;
        }
        chr.resetSkillCoolTime(parentSkillID);
    }

    @Handler(op = InHeader.CHARGE_STACKING_MATRIX_SKILL_REQUEST)
    public static void handleStackingMatrixSkillRequest(Char chr, InPacket inPacket) {
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) return;
        if (JobConstants.isWarriorEquipJob(chr.getJob())) {
            if (chr.getTemporaryStatManager().hasStat(Warrior_AuraWeaponStack) && chr.hasSkill(Job.WEAPON_AURA)) {
                chr.write(UserLocal.userBonusAttackRequest(Job.WEAPON_AURA_ATTACK));
            }
        }
    }

    @Handler(op = InHeader.DIVINE_ECHO_EXPIRE_REQUEST)
    public static void handleDivineEchoExpireRequest(Char chr, InPacket inPacket) {
        Paladin.removeDivineEchoLinkedBuffs(chr);
    }

    @Handler(op = InHeader.STACK_OVER_TIME_SKILL_INCREASE_REQUEST)
    public static void handleStackOverTimeSkillIncreaseRequest(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        Option o1 = new Option();
        switch (skillID) {
            case Job.WEAPON_AURA:
                if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()) break;
                if (chr.hasSkill(skillID)) {
                    TemporaryStatManager tsm = chr.getTemporaryStatManager();
                    byte val = tsm.hasStat(Warrior_AuraWeaponStack) ? (byte) (tsm.getTotalNOptionOfStat(Warrior_AuraWeaponStack) + 1) : 2;
                    o1.nOption = Math.min(2, val);
                    o1.rOption = skillID;
                    tsm.sendStat(Warrior_AuraWeaponStack, o1);
                    chr.write(WvsContext.updateSkillStackRequestResult(skillID, (byte) o1.nOption));
                }
                break;
            case BowMaster.SILHOUETTE_MIRAGE:
                if (chr.hasSkill(skillID)) {
                    ((BowMaster) chr.getJobHandler()).increaseSilhouetteMirageClone();
                }
                break;
            case BlazeWizard.INFERNO_SPHERE:
                if (chr.hasSkill(skillID)) {
                    ((BlazeWizard) chr.getJobHandler()).increasePhoenixFeather();
                }
                break;
            case WindArcher.HOWLING_GALE:
                if (chr.hasSkill(skillID)) {
                    ((WindArcher) chr.getJobHandler()).increaseWindEnergy();
                }
                break;
            case Xenon.SUPPLY_SURPLUS:
            case Xenon.CORE_OVERLOAD_BUFF:
                if (chr.hasSkill(skillID)) {
                    ((Xenon) chr.getJobHandler()).incrementSupply(1);
                }
                break;
            case Adele.AETHER_WEAVING:
                if (chr.hasSkill(skillID)) {
                    ((Adele) chr.getJobHandler()).autoModifyAether();
                }
                break;
            case HoYoung.MASTER_ELIXIR:
                if (chr.hasSkill(skillID)) {
                    ((HoYoung) chr.getJobHandler()).restoreMasterExilir();
                }
                break;
            case DemonAvenger.REVENANT:
                if (chr.hasSkill(skillID)) {
                    ((DemonAvenger) chr.getJobHandler()).reduceFuryRevenant();
                }
                break;
            case Hero.HEXA_BEAM_BLADE:
                if (chr.hasSkill(skillID)) {
                    ((Hero) chr.getJobHandler()).increaseBeamBlade();
                }
                break;
            case Cannoneer.BARREL_ROULETTE:
                if (chr.hasSkill(skillID)) {
                    ((Cannoneer) chr.getJobHandler()).giveBarrelRouletteBuff(skillID);
                }
                break;
            case Cannoneer.HEXA_ROLLING_RAINBOW:
                if (chr.hasSkill(skillID)) {
                    ((Cannoneer) chr.getJobHandler()).increaseRollingRainbow();
                }
                break;
            // Không cố định skill ID:
            case -2:
                if (chr.hasSkill(Cannoneer.BIG_HUGE_GIGANTIC_ROCKET)) {
                    ((Cannoneer) chr.getJobHandler()).incVSkillStackBuff();
                }
                if (chr.hasSkill(Buccaneer.SERPENT_VORTEX)) {
                    ((Buccaneer) chr.getJobHandler()).incVSkillStackBuff();
                }
                if (chr.hasSkill(MoXuan.SECRET_ART_ARROW_FLIGHT)) {
                    ((MoXuan) chr.getJobHandler()).increaseArrowFlight();
                }
                if (chr.hasSkill(Paladin.MIGHTY_MJOLNIR)) {
                    ((Paladin) chr.getJobHandler()).increaseMightyMjolnir();
                }
                if (chr.hasSkill(Bishop.DIVINE_PUNIHSMENT)) {
                    ((Bishop) chr.getJobHandler()).increaseDivinePunishment();
                }
                if (chr.hasSkill(Lynn.SWEEP)) {
                    ((Lynn) chr.getJobHandler()).incVSkillStackBuff();
                }
                break;
            case -1:
                if (chr.hasSkill(Cannoneer.MONKEY_MORTAR)) {
                    ((Cannoneer) chr.getJobHandler()).increaseMonkeyMortar();
                }
                break;
            case -7:
                // 六轉 Origin 技能的 Sol Erda 量表：客戶端進場後定時請求充能，
                // 不回覆的話量表永遠為空，Origin 技能會一直是灰色不可用
                chr.write(WvsContext.updateSkillStackRequestResult(-7, (byte) 1));
                break;
        }
    }

    @Handler(op = InHeader.CREEPING_TOXIN_AREA_REQUEST)
    public static void handleCreepingToxinAreaRequest(Char chr, InPacket inPacket) {
        if (JobConstants.isFirePoison(chr.getJob()) && chr.getJobHandler() instanceof FirePoison firePoison) {
            firePoison.spawnCreepingToxinAreas(inPacket);
        }
    }

    @Handler(op = InHeader.SHOOT_OBJECT_CREATE_REQUEST)
    public static void handleShootObjectCreateRequest(Char chr, InPacket inPacket) {
        ShootObjectSkillInfo shootObjectSkillInfo = new ShootObjectSkillInfo(chr.getId());

        int skillID = inPacket.decodeInt();
        int slv = inPacket.decodeInt();

        shootObjectSkillInfo.setSkillId(skillID);
        shootObjectSkillInfo.setSlv(slv);

        SkillUseInfo skillUseInfo = new SkillUseInfo(skillID, slv);
        new ProcessType(skillUseInfo).decode(inPacket);

        shootObjectSkillInfo.setAction(inPacket.decodeInt()); // action
        shootObjectSkillInfo.setActionSpeed(inPacket.decodeInt()); // action Speed
        shootObjectSkillInfo.setUnknownBool(inPacket.decodeByte());
        shootObjectSkillInfo.setProjectileItemId(inPacket.decodeInt());
        shootObjectSkillInfo.setProjectileItemPosition(inPacket.decodeInt());

        inPacket.decodeByte();
        inPacket.decodeInt();
        inPacket.decodeByte();
        inPacket.decodeByte(); // Unknown

        shootObjectSkillInfo.setPosition(inPacket.decodePosition());

        shootObjectSkillInfo.setEncodeExtra(inPacket.decodeByte() != 0);
        if (shootObjectSkillInfo.isEncodeExtra()) {
            shootObjectSkillInfo.extraEncodeInt1 = inPacket.decodeInt();
            shootObjectSkillInfo.extraEncodeInt2 = inPacket.decodeInt();
            shootObjectSkillInfo.extraEncodeInt3 = inPacket.decodeInt();
            shootObjectSkillInfo.extraEncodeInt4 = inPacket.decodeInt();
            shootObjectSkillInfo.extraEncodeInt5 = inPacket.decodeInt();
            shootObjectSkillInfo.extraEncodeInt6 = inPacket.decodeInt();
            shootObjectSkillInfo.extraEncodeByte1 = inPacket.decodeByte();
            shootObjectSkillInfo.extraEncodeInt7 = inPacket.decodeInt();
            shootObjectSkillInfo.extraEncodeByte2 = inPacket.decodeByte();
            shootObjectSkillInfo.extraEncodeLong1 = inPacket.decodeLong();
            shootObjectSkillInfo.extraEncodeInt8 = inPacket.decodeInt();
            shootObjectSkillInfo.extraEncodeInt9 = inPacket.decodeInt();
        }
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        int loopSize = inPacket.decodeInt();
        for (int i = 0; i < loopSize; i++) {
            ShootObject shootObject = new ShootObject(chr, inPacket);
            shootObjectSkillInfo.getShootObjects().add(shootObject);
        }

        Job job = chr.getJobHandler();
        job.handleShootObject(chr, shootObjectSkillInfo);
        if (skillID == Kinesis.MIND_OVER_MATTER) {
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int ppCons = si.getValue(ppCon, slv);
            ((Kinesis) job).substractPP(ppCons);
        }

        if (skillID == Ren.RIOTOUS_HEART || skillID == Ren.HEXA_RIOTOUS_HEART) {
            if (!chr.getTemporaryStatManager().hasStat(RenPlumSwordForm3Shoot)) {
                return;
            }
        }

        chr.write(UserLocal.shootObjectCreated(shootObjectSkillInfo));
        chr.getField().broadcast(UserRemote.shootObject(shootObjectSkillInfo), chr);
        chr.dispose();
    }

    @Handler(op = InHeader.SHOOT_OBJECT_EXPLODE_REQUEST)
    public static void handleShootObjectExplodeRequest(Char chr, InPacket inPacket) {
        int size = inPacket.decodeInt();
        List<Integer> shootObjectIdList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            shootObjectIdList.add(inPacket.decodeInt());
        }

        if (JobConstants.isFirePoison(chr.getJob()) && chr.hasSkill(FirePoison.POISON_NOVA)
                && chr.getJobHandler() instanceof FirePoison firePoison) {
            firePoison.setExplodeShootObjList(shootObjectIdList);
            chr.getTimer().addEvent(() -> firePoison.getExplodeShootObjList().clear(), 6, TimeUnit.SECONDS);
        }
    }

    private static void attackBonusReader(InPacket inPacket, AttackInfo attackInfo) {
        Gms265AttackDecoder.readAttackBonus(inPacket, attackInfo);
    }

    private static void summonAttackBonusReader(InPacket inPacket, AttackInfo attackInfo) {
        inPacket.decodeByte();
        inPacket.decodeByte();
        inPacket.decodeByte();
        int count = inPacket.decodeInt(); // bonus attack Count
        for (int i = 0; i < count; i++) {
            inPacket.decodeInt();
            inPacket.decodeInt();
        }
        inPacket.decodeInt();
        inPacket.decodeString();
        inPacket.decodeInt();
        inPacket.decodeByte();
    }

    private static void parseAttackInfoPacket(InPacket inPacket, MobAttackInfo mai) {
        Gms265AttackDecoder.readMobAttackInfo(inPacket, mai);
    }

    public static boolean isExploitSkill(Char chr, AttackInfo attackInfo) {
        switch (attackInfo.skillId) {
            case 80001332, 80001593, 80001706 -> {
                User user = chr.getUser();
                String name = user.getName();
                try {
                    LocalDateTime banDate = LocalDateTime.now().plusDays(365);
                    user.setBanExpireDate(FileTime.fromDate(banDate));
                    user.setBanReason("Memory Edits");
                    user.saveToSQL(true);
                    BannedMachines.addBannedMachine(user.getMachineID());
                    DiscordAPI.send("banned", String.format("Account ||%s|| have been banned with reason: %s for %d day(s).", name, "Memory Edits", 365), DiscordAPI.mainGuildServer);
                } catch (Exception e) {
                    DiscordAPI.send("lệnh", "Không thể khoá tài khoản " + chr.getName(), DiscordAPI.staffGuildServer);
                } finally {
                    chr.setUser(user);
                    chr.getClient().setUser(user);
                    chr.getClient().close();
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING,
                            String.format("[%s] 使用技能 bug，角色: %s，技能ID: %d，技能名稱: %s。",
                                    chr.getClient().getIP(),
                                    chr.getName(),
                                    attackInfo.skillId,
                                    StringData.getSkillStringById(attackInfo.skillId)));
                }
                return true;
            }
        }
        return false;
    }
}
