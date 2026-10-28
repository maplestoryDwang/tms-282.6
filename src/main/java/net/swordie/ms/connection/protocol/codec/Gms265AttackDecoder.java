package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.ProcessType;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.jobs.Jianghu.Lynn;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.pirate.Buccaneer;
import net.swordie.ms.client.jobs.anima.Ren;
import net.swordie.ms.client.jobs.cygnus.DawnWarrior;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.legend.Mercedes;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.nova.AngelicBuster;
import net.swordie.ms.client.jobs.nova.Cadena;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.client.jobs.resistance.BattleMage;
import net.swordie.ms.client.jobs.resistance.Blaster;
import net.swordie.ms.client.jobs.resistance.WildHunter;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.DataPrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

/**
 * The GMS 265 attack request layout, moved unchanged out of {@code AttackHandler.handleAttack(Char, InPacket,
 * InHeader)} (same reads, same side effects: the GM "not implemented" chat line and {@code dispose} for an unknown
 * skill, the {@code EXCEPTION_CAUGHT_ATTACK} report and {@code dispose} when the body cannot be read). Checks and
 * damage stay in {@code AttackHandler}.
 * <p>
 * {@link #readAttackBonus}, {@link #readMobAttackInfo} and {@link #calculateCriticalsByAverage} are the 265 helpers
 * the other 265 attack handlers (body, summon) share.
 */
public final class Gms265AttackDecoder implements AttackRequestDecoder {
    public static final Gms265AttackDecoder INSTANCE = new Gms265AttackDecoder();

    private Gms265AttackDecoder() {
    }

    @Override
    public Optional<AttackRequest> decode(Char chr, InPacket inPacket, InHeader header) {
        AttackInfo ai = new AttackInfo();
        ai.inHeader = header;
        switch (header) {
            case USER_MELEE_ATTACK:
                ai.attackHeader = OutHeader.REMOTE_MELEE_ATTACK;
                inPacket.skipInt();
                inPacket.skipInt();
                break;
            case USER_AREA_DOT_ATTACK:
                inPacket.skipInt();
                inPacket.skipInt();
                break;
            case USER_SHOOT_ATTACK:
                inPacket.skipInt();
                inPacket.skipInt();
                ai.boxAttack = inPacket.decodeByte() != 0; // hardcoded 0
                ai.attackHeader = OutHeader.REMOTE_SHOOT_ATTACK;
                break;
            case USER_NON_TARGET_FORCE_ATOM_ATTACK:
                inPacket.skipInt();
                inPacket.skipInt();

                // id/crc/something else
                inPacket.skipInt(); // skillID
                inPacket.skipInt(); // crc?
                inPacket.skipInt(); // mask?

                inPacket.skipInt(); // crc
                inPacket.skipInt(); // crc
                break;
            case USER_MAGIC_ATTACK:
                ai.attackHeader = OutHeader.REMOTE_MAGIC_ATTACK;
                inPacket.skipInt();
                inPacket.skipInt();
                break;
            case USER_AFFECTED_AREA_FOR_SCREEN_ATTACK:
                ai.attackHeader = OutHeader.REMOTE_MELEE_ATTACK;
                break;
        }
        ai.charPosition = chr.getPosition();

        ai.fieldKey = inPacket.decodeByte();

        byte mask = inPacket.decodeByte();
        ai.hits = (byte) (mask & 0xF);
        ai.mobCount = (mask >>> 4) & 0xF;

        int skillID = ai.skillId = inPacket.decodeInt();
        ai.slv = inPacket.decodeInt();

        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null && skillID != 0) {
            if (chr.isGM()) {
                chr.chatScriptMessage("技能 " + skillID + " 尚未實作。");
            }
            chr.dispose();
            return Optional.empty();
        } else {
            ai.skillInfo = si;
        }

        try {
            if (header == InHeader.USER_MELEE_ATTACK
                    || header == InHeader.USER_SHOOT_ATTACK
                    || header == InHeader.USER_NON_TARGET_FORCE_ATOM_ATTACK) {
                ai.addAttackProc = inPacket.decodeByte();
            }

            // Fully Charged Key Down Skills
            if (ai.skillId == 12121055) {
                ai.skillId = 12121054;
            }

            inPacket.skipInt();
            inPacket.skipInt();
            inPacket.skipInt();

            readAttackBonus(inPacket, ai);

            SkillUseInfo skillUseInfo = new SkillUseInfo(si, ai.slv);
            ProcessType processType = new ProcessType(skillUseInfo);
            processType.decode(inPacket);
            ai.processType = processType;

            if (SkillConstants.isKeyDownSkill(skillID) || SkillConstants.isSuperNovaSkill(skillID)) {
                ai.keyDown = inPacket.decodeInt();
            }
            if (SkillConstants.isRushBombSkill(skillID) || skillID == 5300007 || skillID == 11101030 || skillID == 64101008) {
                ai.grenadeId = inPacket.decodeInt();
            }
            if (header == InHeader.USER_NON_TARGET_FORCE_ATOM_ATTACK && (skillID == 12121057 || skillID == 12121059)) {
                ai.pos = inPacket.decodePositionInt();
            }
            if (SkillConstants.isZeroSkill(skillID)) {
                ai.zeroTag = inPacket.decodeByte();
            }
            if (SkillConstants.isUsercloneSummonedAbleSkill(skillID)) {
                ai.bySummonedID = inPacket.decodeInt();
            }
            if (skillID == AngelicBuster.MIGHTY_MASCOT) { // Mighty Mascot
                inPacket.decodeInt();
            }
            if (skillID == Mercedes.ROLLING_MOONSAULT) { // Mercedes Rolling Moonsault skill
                inPacket.decodeInt();
            }
            if (skillID == NightWalker.SHADOW_SPEAR_AA_LARGE) {
                ai.shadowSpear1 = inPacket.decodeInt();
                ai.shadowSpear2 = inPacket.decodeInt();
            }
            if (skillID == 400011133
                    || skillID == DemonAvenger.BAT_SWARM
                    || skillID == Ren.THOUSAND_BLOSSOM_FLURRY_EX
                    || skillID == WildHunter.ANOTHER_BITE
                    || SkillConstants.isDelugeSkill(skillID)) {
                inPacket.skipInt();
            }
            ai.buckShot = inPacket.decodeByte();
            ai.someMask = inPacket.decodeByte(); // decides where the Remote Attack goes for mimicked skills (Divine Echo)
            if (header == InHeader.USER_SHOOT_ATTACK) {
                int bAddtionalBolt = inPacket.decodeInt();
                ai.isJablin = inPacket.decodeByte() != 0;
                if (ai.boxAttack) {
                    int boxAttack_1 = inPacket.decodeInt();
                    short boxAttack_2 = inPacket.decodeShort();
                    short boxAttack_3 = inPacket.decodeShort();
                }
            }
            if (skillID == 400031033 || skillID == 400031070) {
                // Primal Grenade: Flash
                ai.isJablin = inPacket.decodeByte() != 0;
                if (ai.boxAttack) {
                    int boxAttack_1 = inPacket.decodeInt();
                    short boxAttack_2 = inPacket.decodeShort();
                    short boxAttack_3 = inPacket.decodeShort();
                }
            }
            if (header == InHeader.USER_NON_TARGET_FORCE_ATOM_ATTACK) {
                if (SkillConstants.isOrbitalFlameOrPhoenixDrive(skillID)) {
                    inPacket.skipInt();
                }
                if (SkillConstants.isHexaOrbitalFlame(skillID)) {
                    inPacket.skipInt();
                }
            }
            ai.mask = inPacket.decodeShort(); // COutPacket::Encode2(v105, v118 & 0x7FFF | (v30 << 15));
            ai.left = ((ai.mask >>> 15) & 1) != 0;
            ai.attackAction = (short) (ai.mask & 0x7FFF);
            ai.requestTime = inPacket.decodeInt();
            ai.attackActionType = inPacket.decodeByte();

            if (skillID == 23111001 || skillID == 80001915 || skillID == 36111010) {
                inPacket.skipInt(); // bIsEncryptedByShanda
                inPacket.skipInt(); // x
                inPacket.skipInt(); // y
            }

            if (SkillConstants.isEvanForceSkill(skillID)) {
                inPacket.skipByte();
            }

            ai.attackSpeed = inPacket.decodeByte();
            ai.tick = inPacket.decodeInt();

            if (header == InHeader.USER_AREA_DOT_ATTACK) {
                ai.affectedAreaObjId = inPacket.decodeInt();
            }

            if (header != InHeader.USER_MAGIC_ATTACK
                    && header != InHeader.USER_NON_TARGET_FORCE_ATOM_ATTACK
                    && header != InHeader.USER_AREA_DOT_ATTACK
                    && header != InHeader.USER_AFFECTED_AREA_FOR_SCREEN_ATTACK) {
                ai.bulletSlot = inPacket.decodeInt(); // 2
            }

            if (header == InHeader.USER_MELEE_ATTACK || header == InHeader.USER_SHOOT_ATTACK) {
                ai.finalAttackLastSkillID = inPacket.decodeInt();
                if (ai.finalAttackLastSkillID > 0) {
                    ai.finalAttackByte = inPacket.decodeByte();
                }
            } else if (header == InHeader.USER_MAGIC_ATTACK
                    || header == InHeader.USER_AREA_DOT_ATTACK
                    || header == InHeader.USER_NON_TARGET_FORCE_ATOM_ATTACK) {
                inPacket.skipInt();
            } else {
                ai.ptStart_X = inPacket.decodeShort();
                ai.hitRange = inPacket.decodeShort();
            }
            if (header == InHeader.USER_NON_TARGET_FORCE_ATOM_ATTACK
                    || skillID == RuneStone.LIBERATE_THE_RUNE_OF_THUNDER_ATTACK) {
                ai.maximumMobCount = inPacket.decodeInt();
            }
            if (header == InHeader.USER_SHOOT_ATTACK) {
                inPacket.skipShort();
                inPacket.skipByte();
                ai.bulletID = chr.getBulletIDForAttack();
                ai.rect = inPacket.decodeShortRect();
            }
            if (skillID == Buccaneer.SPIRAL_ASSAULT) {
                ai.ignorePCounter = inPacket.decodeByte() != 0;
            }
            if (skillID == Shade.SPIRIT_FRENZY) {
                ai.spiritCoreEnhance = inPacket.decodeInt();
            }
            if (skillID == Lynn.STRIKE_1 || skillID == Lynn.STRIKE_2
                    || skillID == Lynn.STRIKE_3 || skillID == Lynn.STRIKE_4
                    || skillID == Lynn.PREDATOR_BLOW) {
                inPacket.decodeInt();
                if (skillID == Lynn.PREDATOR_BLOW) {
                    inPacket.decodeByte();
                }
            }

            long totalDamage = 0;
            int totalCr = chr.getTotalStat(BaseStat.cr);
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
                if (header == InHeader.USER_MAGIC_ATTACK) {
                    mai.hpPerc = inPacket.decodeByte();
                    mai.magicInfo = inPacket.decodeShort();
                } else {
                    inPacket.skipShort();
                }
                inPacket.skipInt();
                inPacket.skipInt();
                inPacket.skipByte();
                mai.damages = new long[ai.hits];
                for (int j = 0; j < ai.hits; j++) {
                    long damage = inPacket.decodeLong();
                    totalDamage += damage;
                    mai.damages[j] = damage;
                }
                mai.crits = calculateCriticalsByAverage(mai.damages, totalCr);
                mai.mobUpDownYRange = inPacket.decodeInt();
                inPacket.skipInt(); // crc
                inPacket.skipInt(); // crc
                if (skillID == Blaster.ROCKET_RUSH) {
                    mai.isResWarriorLiftPress = inPacket.decodeByte() != 0;
                } else if (skillID == 400021029) {
                    inPacket.decodeByte();
                    inPacket.decodeInt();
                } else if (SkillConstants.isKinesisPsychicLockSkill(skillID)) {
                    inPacket.decodeInt();
                    inPacket.decodeInt();
                }
                readMobAttackInfo(inPacket, mai);
                ai.mobAttackInfo.add(mai);
            }
            ai.totalDamageDealt = totalDamage;

            try {
                if (skillID == 27121052 || skillID == 80001837) {
                    ai.x = inPacket.decodeShort();
                    ai.y = inPacket.decodeShort();
                }
                if (SkillConstants.isShootObjectSkill(skillID)) {
                    ai.forcedX = inPacket.decodeShort();
                    ai.forcedY = inPacket.decodeShort();
                    ai.shootObjId = inPacket.decodeInt();
                    if (SkillConstants.isSomePathfinderSkill(skillID)) {
                        ai.teleportPt = inPacket.decodePosition();
                        ai.pathFinderBool = inPacket.decodeByte();
                    }
                } else if (header == InHeader.USER_MAGIC_ATTACK
                        && skillID != BattleMage.DARK_SHOCK
                        && skillID != 400021004
                        && skillID != 27121052
                        && skillID != 152001002
                        && skillID != 400021028
                        && !SkillConstants.isKinesisPsychicAreaSkill(skillID)) {
                    ai.forcedX = inPacket.decodeShort();
                    ai.forcedY = inPacket.decodeShort();
                    ai.dragon = inPacket.decodeByte() != 0;
                    if (ai.dragon) {
                        ai.rcDstRight = inPacket.decodeShort();
                        ai.rectRight = inPacket.decodeShort();
                        ai.x = inPacket.decodeShort();
                        ai.y = inPacket.decodeShort();
                        ai.dragonAttackStart = inPacket.decodeByte();
                        ai.dragonAttackActionType = inPacket.decodeByte();
                        ai.dragonAttackProgess = inPacket.decodeByte();
                        inPacket.decodeByte();
                        inPacket.decodeByte();
                    }
                } else if (SkillConstants.isThrowableAttackSkill(skillID)) {
                    ai.throwableAttack = inPacket.decodeInt();
                    ai.throwableAttackFootHold = inPacket.decodeByte();
                }

                if (SkillConstants.isSuperNovaSkill(skillID)
                        || SkillConstants.isScreenCenterAttackSkill(skillID)
                        || skillID == 101000202
                        || skillID == 101141032
                        || skillID == 101000102
                        || skillID == 101141028
                        || skillID == 80002212
                        || skillID == 80002463
                        || skillID == 400041019 || skillID == 400041024 || skillID == 400041080 // (v145 = v246 - 400041019, v145 <= 61) && _bittest64(&v141, v145)
                        || skillID == 400031016
                        || skillID == 31240014
                        || skillID == 3221019
                        || SkillConstants.isWingedJavelinOrAbyssalCast(skillID)
                        || skillID == 400021075
                        || skillID == 14111036
                        || skillID == 400001055 || skillID == 400001056) {
                    ai.ptTarget = inPacket.decodePosition();
                }
                if (skillID == 400041062 || skillID == 400041064 || skillID == 400041065 || skillID == 400041066 || skillID == 400041074 || skillID == 400041079
                        || skillID == 400051080
                        || skillID == 400011125 || skillID == 400011126
                        || skillID == 36141008
                        || skillID == 155121007 // sub_141083250(155121007, v144)
                        || skillID == 80003017
                        || skillID == 22201501
                        || skillID == 1241006
                        || skillID == 11141007
                        || skillID == 400041082 || skillID == 400041083
                        || skillID == 5241501
                        || skillID == 64141003) {
                    ai.pos = inPacket.decodePosition();
                }

                if (skillID == FirePoison.POISON_MIST) {
                    ai.force = inPacket.decodeByte();
                    ai.forcedXSh = inPacket.decodeShort();
                    ai.forcedYSh = inPacket.decodeShort();
                }

                if (skillID == 80001835) { // Soul Shear
                    byte sizeB = inPacket.decodeByte();
                    int[] idkArr2 = new int[sizeB];
                    short[] shortArr2 = new short[sizeB];
                    for (int i = 0; i < sizeB; i++) {
                        idkArr2[i] = inPacket.decodeInt();
                        shortArr2[i] = inPacket.decodeShort();
                    }
                    ai.delay = inPacket.decodeShort();
                    ai.mists = idkArr2;
                    ai.shortArr = shortArr2;
                }

                if (header == InHeader.USER_AREA_DOT_ATTACK) {
                    ai.pos.setX(inPacket.decodeShort());
                    ai.pos.setY(inPacket.decodeShort());
                }

                if (SkillConstants.isAranFallingStopSkill(skillID)) {
                    ai.fh = inPacket.decodeByte();
                }

                if (ai.inHeader == InHeader.USER_AFFECTED_AREA_FOR_SCREEN_ATTACK) {
                    inPacket.decodePosition();
                }

                if (header == InHeader.USER_SHOOT_ATTACK && skillID / 1000000 == 33) {
                    ai.bodyRelMove = inPacket.decodePosition();
                }

                if (skillID == Blaster.HYPER_MAGNUM_PUNCH
                        || SkillConstants.isShadowAssault(skillID)
                        || skillID == DawnWarrior.EQUINOX_SLASH
                        || SkillConstants.isOctoPunch(skillID)) {
                    ai.teleportByte = inPacket.decodeByte();
                    ai.teleportPt.setX(inPacket.decodeInt());
                    ai.teleportPt.setY(inPacket.decodeInt());
                }

                if (SkillConstants.isKeydownSkillRectMoveXY(skillID)) {
                    ai.keyDownRectMoveXY = inPacket.decodePosition();
                }

                if (skillID == Kaiser.INFERNO_BREATH || skillID == Kaiser.INFERNO_BREATH_FINAL_FORM) {
                    inPacket.decodeInt(); // unk
                    ai.Vx = inPacket.decodeShort();
                    ai.positions = new ArrayList<>();
                    for (int i = 0; i < ai.Vx; i++) {
                        // Inferno Breath Affected Area Positions
                        ai.positions.add(inPacket.decodePosition());
                    }
                }
                if (skillID == 14111006 && ai.grenadeId != 0) {
                    ai.grenadePos.setX(inPacket.decodeShort());
                    ai.grenadePos.setY(inPacket.decodeShort());
                }
                if (/*skillID == 23121002 ||*/skillID == 80001914) { // first skill is Spikes Royale, not needed?
                    ai.fh = inPacket.decodeByte();
                }

                if (skillID == Cadena.CHAIN_ARTS_PURSUIT_UP || skillID == Cadena.CHAIN_ARTS_PURSUIT_DOWN) {
                    if (inPacket.getUnreadAmount() > 5) {
                        int toReadAmount = inPacket.getUnreadAmount() - 5;
                        inPacket.decodeArr(toReadAmount);
                    }
                    inPacket.decodeByte(); // unk
                    ai.ptTarget = inPacket.decodePosition(); // pursuit hook end position
                }
                if (SkillConstants.isDivineEchoMimicSkills(skillID) && inPacket.getUnreadAmount() == 8) { // Divine Echo Mimic Skills
                    ai.isMimickedBy = inPacket.decodeInt(); // Mimic Chr Id
                    inPacket.decodePosition(); // Mimic Chr Position
                }
            } catch (Exception i) {
                // TODO?
            }

            return Optional.of(AttackRequest.of(ai, skillID));
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT_ATTACK, String.format("[%s] Skill ID: %d unable to handle Attacks.", chr.getName(), skillID), false);
            chr.dispose();
            return Optional.empty();
        }
    }

    public static boolean[] calculateCriticalsByAverage(long[] damages, int totalCr) {
        if (damages == null || damages.length == 0) {
            return new boolean[0];
        }
        final double CRITICAL_MULTIPLIER = 1.1;
        final double SAME_SAME_MULTIPLIER = 0.95;
        final double UNIFORMITY_TOLERANCE = 0.10;
        double expectedCr = totalCr / 100.0;
        if (expectedCr >= 1.0) {
            boolean[] crits = new boolean[damages.length];
            Arrays.fill(crits, true);
            return crits;
        }
        if (damages.length == 1) {
            boolean[] crits = new boolean[1];
            Random rand = new Random();
            crits[0] = rand.nextDouble() < expectedCr;
            return crits;
        }
        long totalDamage = 0;
        long maxDamage = Long.MIN_VALUE;
        long minDamage = Long.MAX_VALUE;
        for (long dmg : damages) {
            totalDamage += dmg;
            if (dmg > maxDamage) maxDamage = dmg;
            if (dmg < minDamage) minDamage = dmg;
        }
        double averageDamage = (double) totalDamage / damages.length;
        boolean[] crits = new boolean[damages.length];
        double range = maxDamage - minDamage;
        boolean isHighlyUniform = (range / averageDamage) < UNIFORMITY_TOLERANCE;
        for (int i = 0; i < damages.length; i++) {
            long dmg = damages[i];
            if (isHighlyUniform) {
                crits[i] = true;
            }
            else if (dmg >= averageDamage * CRITICAL_MULTIPLIER || // Hit lớn hơn 110% Average
                    dmg >= averageDamage * SAME_SAME_MULTIPLIER) { // HOẶC Hit "same same" (>= 95% Average)
                crits[i] = true;
            } else {
                crits[i] = false;
            }
        }
        return crits;
    }

    public static void readAttackBonus(InPacket inPacket, AttackInfo attackInfo) {
        inPacket.skipByte();
        inPacket.skipShort(); // bullet slot
        inPacket.skipInt();
        inPacket.skipByte(); // by Unreliable Memory?
        inPacket.skipByte();
        inPacket.skipByte();
        inPacket.skipByte();
        inPacket.skipInt();
        inPacket.skipLong();
        inPacket.skipInt();
        inPacket.skipInt();
        int count = inPacket.decodeInt(); // bonus attack Count
        for (int i = 0; i < count; i++) {
            inPacket.skipInt();
        }
        inPacket.skipByte();
        inPacket.skipByte();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipLong();
        inPacket.skipLong();
        inPacket.skipLong();
        inPacket.skipByte();
        int count2 = inPacket.decodeInt(); // bonus attack Count
        for (int i = 0; i < count2; i++) {
            inPacket.skipInt();
        }
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipInt();
        inPacket.skipString();
        inPacket.skipInt();
        inPacket.skipInt(); // tick
        inPacket.skipByte();
        int count3 = inPacket.decodeInt(); // bonus attack Count
        for (int i = 0; i < count3; i++) {
            inPacket.skipInt();
            inPacket.skipInt();
        }
        inPacket.skipInt();
    }

    public static void readMobAttackInfo(InPacket inPacket, MobAttackInfo mai) {
        // PACKETMAKER::MakeAttackInfoPacket
        mai.type = inPacket.decodeByte();
        mai.currentAnimationName = "";
        if (mai.type == 1) {
            mai.currentAnimationName = inPacket.decodeString();
            inPacket.decodeString();
            mai.animationDeltaL = inPacket.decodeInt();
            mai.animationBool = inPacket.decodeByte() != 0;
            if (mai.animationBool) {
                mai.hitPartRunTimesSize = inPacket.decodeInt();
                mai.hitPartRunTimes = new String[mai.hitPartRunTimesSize];
                for (int j = 0; j < mai.hitPartRunTimesSize; j++) {
                    mai.hitPartRunTimes[j] = inPacket.decodeString();
                }
            } else {
                mai.hitPartRunTimesSize = inPacket.decodeInt();
            }
        } else if (mai.type == 2) {
            mai.currentAnimationName = inPacket.decodeString();
            inPacket.skipString();
            mai.animationDeltaL = inPacket.decodeInt();
            mai.animationBool = inPacket.decodeByte() != 0;
        }
        inPacket.skipByte();
        inPacket.skipPosition();
        inPacket.skipPosition();
        inPacket.skipPosition();
        inPacket.skipByte();
        inPacket.skipByte();
        inPacket.skipByte();
        inPacket.skipInt();
        boolean bool = inPacket.decodeByte() != 0;
        if (bool) {
            inPacket.skipInt();
            inPacket.skipInt();
            int size = inPacket.decodeInt();
            for (int i = 0; i < size; i++) {
                inPacket.skipInt();
                inPacket.skipLong();
                inPacket.skipString();
                inPacket.skipLong();
            }
            inPacket.skipInt();
            inPacket.skipInt();
            inPacket.skipInt();
            inPacket.skipInt();
            inPacket.skipLong();
            inPacket.skipLong();
            inPacket.skipInt();
            inPacket.skipInt();
            inPacket.skipInt();
        }
        inPacket.skipInt(); // mobTemplateID
        inPacket.skipInt();
        int size = inPacket.decodeInt();
        for (int i = 0; i < size; i++) {
            inPacket.skipInt();
            inPacket.skipInt();
        }
    }
}
