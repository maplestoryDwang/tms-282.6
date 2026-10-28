package net.swordie.ms.client.character.runestones;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.enums.RuneType;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class RuneStone {


    public static final int LIBERATE_THE_SWIFT_RUNE = 80001427;
    public static final int LIBERATE_THE_RECOVERY_RUNE = 80001428;
    public static final int LIBERATE_THE_DESTRUCTIVE_RUNE = 80001431;
    public static final int LIBERATE_THE_DESTRUCTIVE_RUNE_BUFF = 80001432;
    public static final int LIBERATE_THE_RUNE_OF_THUNDER = 80001752;
    public static final int LIBERATE_THE_RUNE_OF_MIGHT = 80001753;
    public static final int LIBERATE_THE_RUNE_OF_DARKNESS = 80001754;
    public static final int LIBERATE_THE_RUNE_OF_BLESSING = 80003911;
    public static final int LIBERATE_THE_RUNE_OF_BLESSING_SUMMON = 80003912;
    public static final int LIBERATE_THE_RUNE_OF_SKILL = 80001878;
    public static final int LIBERATE_THE_RUNE_OF_MIGHT_2 = 80001757;
    public static final int LIBERATE_THE_RUNE_OF_THUNDER_ATTACK = 80001762;
    public static final int LIBERATE_THE_RUNE_OF_GREED = 80002281;
    public static final int LIBERATE_THE_RUNE_OF_PURIFICATION = 80002888;
    public static final int LIBERATE_THE_RUNE_OF_CONTACT = 80002889;
    public static final int LIBERATE_THE_RUNE_OF_IGNITION = 80002890;
    public static final int SEALED_RUNE_POWER = 80002282;
    private int objectId;
    private RuneType runeType;
    private Position position;
    private boolean flip;
    private ScheduledFuture<?> thunderTimer;
    private EventType eventType;

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    public RuneType getRuneType() {
        return runeType;
    }

    public void setRuneType(RuneType runeType) {
        this.runeType = runeType;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public boolean isFlip() {
        return flip;
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
    }

    public void spawnRune(Char chr) {
        chr.write(FieldPacket.runeStoneAppear(this));
    }

    public void despawnRune(Char chr) {
        chr.write(FieldPacket.runeStoneClearAndAllRegister());
    }

    public enum RuneStoneAction {
        LEFT_RIGHT_DOWN_DOWN(new int[]{2, 3, 0, 0}, "93 B8 73 FC 28 00 00 00 BE 55 23 88 EE E5 BF DE E0 08 BF DE 18 64 A8 16 5A 01 76 CD B8 97 D0 2D 17 BD 1C 21 FE 97 CA 0F 4C F1 98 D7 A8 A5 14 BC"),
        LEFT_RIGHT_UP_UP(new int[]{2, 3, 1, 1}, "A6 B3 44 26 28 00 00 00 91 F8 B9 53 64 64 88 DA DB D1 E3 4B E2 8D 84 E0 41 D3 71 F2 7C 6E 92 24 60 36 F9 C4 A6 27 3F A6 45 46 69 44 BD B2 4B 34"),
        LEFT_RIGHT_LEFT_RIGHT(new int[]{2, 3, 2, 3}, "37 3C EC 27 28 00 00 00 11 C0 15 52 D0 61 C7 D9 7A 47 11 B9 03 63 70 DD AD 9F EC 23 27 6D 9F 16 FF BE 7E F8 02 D3 36 81 9A D2 F5 BF 28 DC 68 6B"),
        LEFT_DOWN_DOWN_LEFT(new int[]{2, 0, 0, 2}, "C5 1D 7F AE 28 00 00 00 A6 CB 93 DA A6 A7 25 75 A7 3B C1 14 47 F7 77 39 D4 64 B0 79 39 DD 29 EE 56 4A 66 55 37 EE 24 7D 92 4B D3 5E 78 9B 1C F6"),
        LEFT_DOWN_RIGHT_RIGHT(new int[]{2, 0, 3, 3}, "54 B1 12 74 28 00 00 00 68 1E 73 01 2B F5 4B 37 57 09 75 67 94 84 AC 20 D1 30 37 DB 4D 73 F8 AD 28 A7 86 41 DE 49 F6 A5 0F 4C 93 CA 17 E3 5A 25"),
        UP_LEFT_RIGHT_RIGHT(new int[]{1, 2, 3, 3}, "44 53 BC 48 28 00 00 00 BC 08 04 3D 4D 39 97 19 64 E5 23 D6 C2 B1 F0 0D F6 4A DF 04 C6 DE C5 1F 5B 91 95 A0 C4 03 77 BD 99 FE A7 1F 4B 4A A0 21"),
        UP_DOWN_RIGHT_DOWN(new int[]{1, 0, 3, 0}, "AF 17 C5 D6 28 00 00 00 C0 5D 59 A2 7E 51 B7 6F 45 D4 92 F9 B4 F4 26 16 05 9A C0 5B B3 21 EE E1 7C E0 79 EA 07 E4 23 DF A2 E2 BA A2 F8 7B 3E 50"),
        DOWN_UP_DOWN_UP(new int[]{0, 1, 0, 1}, "72 50 81 93 28 00 00 00 8C 81 A3 E6 2A B3 68 3F 37 AC 50 67 A4 98 D9 18 C9 46 3F 3E F0 B4 8C FA EF 58 87 0B 5E DC 96 B3 37 9B 3D 9B 06 E3 D3 3F"),
        DOWN_LEFT_RIGHT_RIGHT(new int[]{0, 2, 3, 3}, "49 E4 E3 39 28 00 00 00 9F E8 35 4C 8D AA F1 2F BD C2 9C 3F F3 A8 F1 1A 7A 28 81 CB A3 16 F3 97 8C 7F FF 9D 24 0C 5F 55 63 8A 63 98 CB 68 99 7A"),
        DOWN_DOWN_LEFT_UP(new int[]{0, 0, 2, 1}, "54 0B AE BC 28 00 00 00 DC 73 7E C8 85 2E 54 A3 AA F1 57 E4 92 FB 3B 6B CE 50 2E 42 FF 75 07 23 FF AE 08 39 5A C0 FD 85 7E 97 90 35 AD 7C 4A 3E"),
        DOWN_DOWN_RIGHT_RIGHT(new int[]{0, 0, 3, 3}, "65 84 5C 58 28 00 00 00 F3 9F C5 2D 7F 59 D4 CC A5 6F EE 5A E5 E9 C7 85 CE 45 54 07 6F 16 FE 61 89 A2 6C 06 CA F3 9A F3 3B B6 26 5E FB 6B 2C BD"),
        DOWN_RIGHT_DOWN_UP(new int[]{0, 3, 0, 1}, "D3 2D 88 2E 28 00 00 00 90 E9 64 5B 50 CA 05 F7 A9 50 6F C5 A7 BB 9D DF 9F 7C F4 03 14 14 71 AD F7 6B AC E7 72 EF F6 78 BE 65 F0 A5 72 1C BB E3"),
        DOWN_UP_RIGHT_LEFT(new int[]{0, 1, 3, 2}, "AA 5A F5 73 28 00 00 00 43 B3 94 06 F9 88 0D 5E 02 48 BB F3 C7 59 EF C6 26 64 0B 7B FE 9D 86 0E D6 29 0F 99 62 66 3E 4A B6 80 86 41 A7 BC BC 0B"),
        DOWN_DOWN_LEFT_LEFT(new int[]{0, 0, 2, 2}, "25 98 DE 04 28 00 00 00 4B 8F 9E 72 C6 91 F6 87 3A C3 A5 BE 49 75 A4 B3 48 BF A1 54 EF 77 2E A8 BB 5C 96 02 48 1F A9 D3 6C 6B 50 D8 90 DA 57 88"),
        RIGHT_LEFT_LEFT_RIGHT(new int[]{3, 2, 2, 3}, "25 35 9F 92 28 00 00 00 11 A3 BB E7 E5 29 8D 3B CC 03 11 3E 82 F7 91 83 58 B2 93 70 B5 CC 2B 51 64 94 FC 6B 72 AC 4A A5 D3 AA 24 D1 AA 44 1F CA"),
        RIGHT_UP_UP_LEFT(new int[]{3, 1, 1, 2}, "8A AC 50 86 28 00 00 00 CF 9F 6D F3 BE F8 6C FA 6C B2 39 43 1D E3 AC EE FB D1 2E 5A 7D F0 A9 1B C4 23 F1 04 B5 9B 04 A0 4A B4 02 0C CD D5 24 28"),
        RIGHT_UP_DOWN_DOWN(new int[]{3, 1, 0, 0}, "1D 7E DD 79 28 00 00 00 BF 67 8B 0C DC 0D 91 6F A8 5D EC 38 95 9C 02 17 EC 23 10 AD 66 A5 AF D4 59 D7 21 AF 9F E8 B7 64 92 70 E5 67 60 7B F8 1D"),
        RIGHT_UP_RIGHT_DOWN(new int[]{3, 1, 3, 0}, "D3 B6 64 AF 28 00 00 00 E2 BD 8F DB AF 34 5E 70 D8 2A 55 67 BD 83 2A 31 BA 4A F3 0B 6A 5D AD 85 88 17 C4 84 C7 AF 28 FA F1 65 01 6D BA 47 2E D3"),
        RIGHT_DOWN_DOWN_LEFT(new int[]{3, 0, 0, 2}, "81 29 EA DB 28 00 00 00 CA 29 78 AF DC 1C 41 57 56 05 59 85 AE 3B FC 02 B6 16 13 D7 2B 0F 4A 83 3D D0 CB AE 05 F8 79 C1 E6 09 B6 40 82 2F 63 55"),
        RIGHT_UP_RIGHT_RIGHT(new int[]{3, 1, 3, 3}, "D6 90 EE 4F 28 00 00 00 AB 67 47 3A 64 96 3C 12 6E F9 D6 24 2E 15 15 CD 9D 16 13 AF 34 E5 3F 62 CB 6A BD 6A 4C 90 31 E9 3E 8C 2A E5 50 EE 72 78");
        ;
        int[] action;
        byte[] packet;

        RuneStoneAction(int[] action, String sPacket) {
            this.action = action;
            this.packet = Util.getByteArrayByString(sPacket);
        }

        public int[] getAction() {
            return Arrays.copyOf(action, action.length);
        }

        public byte[] getPacket() {
            return packet;
        }
    }

    public RuneStone getRandomRuneStone(Field field) {
        RuneStone runeStone = null;
        if (field.getMobGens().size() > 0 && field.getBossMobID() == 0 && !field.isTown()) {
            runeStone = new RuneStone();
            runeStone.setRuneType(RuneType.getByVal((byte) new Random().nextInt(RuneType.values().length)));
            runeStone.setEventType(EventType.NewYear);

            List<Foothold> listOfFootHolds = new ArrayList<>(field.getNonWallFootholds());
            Foothold foothold = Util.getRandomFromCollection(listOfFootHolds);
            Position position = foothold.getRandomPosition();

            runeStone.setPosition(position);
            runeStone.setFlip(false);
        }
        return runeStone;
    }

    public RuneStone getRuneStoneByType(int type, Field field) {
        RuneStone runeStone = null;
        if (field.getMobGens().size() > 0 && field.getBossMobID() == 0 && !field.isTown()) {
            runeStone = new RuneStone();
            runeStone.setRuneType(RuneType.getByVal((byte) type));
            runeStone.setEventType(EventType.NewYear);

            List<Foothold> listOfFootHolds = new ArrayList<>(field.getNonWallFootholds());
            Foothold foothold = Util.getRandomFromCollection(listOfFootHolds);
            Position position = foothold.getRandomPosition();

            runeStone.setPosition(position);
            runeStone.setFlip(false);
        }
        return runeStone;
    }

    public void activateRuneStoneEffect(Char chr) {
        int runeBuffID = 0;
        switch (runeType) {
            case Destruction:
                runeBuffID = LIBERATE_THE_DESTRUCTIVE_RUNE_BUFF;
                break;
            case Thunder:
                applyRuneThunder(chr);
                runeBuffID = LIBERATE_THE_RUNE_OF_THUNDER;
                break;
            case Giants:
                applyRuneMight(chr);
                runeBuffID = LIBERATE_THE_RUNE_OF_MIGHT;
                break;
            case Darkness:
                applyRuneDarkness(chr);
                runeBuffID = LIBERATE_THE_RUNE_OF_DARKNESS;
                break;
            case Blessing:
                applyRuneBlessing(chr);
                runeBuffID = LIBERATE_THE_RUNE_OF_BLESSING;
                break;
            case Skill:
                applyRuneSkill(chr);
                runeBuffID = LIBERATE_THE_RUNE_OF_SKILL;
                break;
            case Purification:
                applyRunePurification(chr);
                runeBuffID = LIBERATE_THE_RUNE_OF_PURIFICATION;
                break;
            case Contact:
                if (Protocol.active().version() == ProtocolVersion.TMS_282_3) applyRuneContact(chr);
                runeBuffID = LIBERATE_THE_RUNE_OF_CONTACT;
                break;
            case Ignition:
                applyRuneIgnition(chr);
                runeBuffID = LIBERATE_THE_RUNE_OF_IGNITION;
                break;
        }

        // Common EXP buff
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        // 282 separates the shared EXP reward from each rune's short-lived special effect.
        // The special skills (e.g. 80002889) contain no indieExp and otherwise grant a zero-value buff.
        int expSkillId = Protocol.active().version() == ProtocolVersion.TMS_282_3
                ? (runeType == RuneType.Blessing ? 80003910 : 80002280) : runeBuffID;
        Skill skill = SkillData.getSkillDeepCopyById(expSkillId);
        int skillID = skill.getSkillId();
        skill.setCurrentLevel(1);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        Option o1 = new Option();
        o1.nReason = skillID;
        o1.nValue = si.getValue(indieExp, slv); //200% EXP
        o1.tTerm = si.getValue(time, slv);
        tsm.sendStat(IndieEXP, o1);
    }

    private void applyRuneIgnition(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = SkillData.getSkillDeepCopyById(Protocol.active().version() == ProtocolVersion.TMS_282_3
                ? LIBERATE_THE_RUNE_OF_IGNITION : LIBERATE_THE_RUNE_OF_SKILL);
        int skillID = skill.getSkillId();
        skill.setCurrentLevel(1);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        Option o1 = new Option();
        o1.nOption = 1;
        o1.rOption = skillID;
        o1.tOption = si.getValue(time, slv);
        tsm.sendStat(RuneContagion, o1);
    }

    private void applyRunePurification(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        boolean tms282 = Protocol.active().version() == ProtocolVersion.TMS_282_3;
        Skill skill = SkillData.getSkillDeepCopyById(tms282 ? LIBERATE_THE_RUNE_OF_PURIFICATION : LIBERATE_THE_RUNE_OF_SKILL);
        int skillID = skill.getSkillId();
        skill.setCurrentLevel(1);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        Option o1 = new Option();
        o1.nOption = 100;
        o1.rOption = skillID;
        o1.tOption = si.getValue(time, slv);
        if (tms282) {
            // WZ summon/attack1 and native CSummoned 45116B5/452274B; CTS852 alone does not create the prism.
            Summon prism = Summon.getSummonBy(chr, skillID, slv);
            prism.setCharLevel(chr.getStat(net.swordie.ms.enums.Stat.level));
            prism.setMoveAbility(MoveAbility.Stop);
            o1.summon = prism;
        }
        tsm.sendStat(RunePurification, o1);
        if (o1.summon != null) chr.getField().spawnSummon(o1.summon);
    }

    private void applyRuneContact(Char chr) {
        // Native 443FE94..443FECD requires a nonzero flag and precisely reason80002889.
        Option option = new Option();
        option.nOption = 1;
        option.rOption = LIBERATE_THE_RUNE_OF_CONTACT;
        option.tOption = SkillData.getSkillInfoById(option.rOption).getValue(time, 1);
        chr.getTemporaryStatManager().sendStat(ShineRuneAttack, option);
    }

    private void applyRuneSkill(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = SkillData.getSkillDeepCopyById(LIBERATE_THE_RUNE_OF_SKILL);
        int skillID = 0;
        if (skill != null) {
            skillID = skill.getSkillId();
            skill.setCurrentLevel(1);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            Option o1 = new Option();
            o1.nOption = 5;
            o1.rOption = skillID;
            o1.tOption = si.getValue(time, slv);
            tsm.sendStat(FixCoolTime, o1);
        }
    }

    private void applyRuneSwiftness(Char chr) {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = SkillData.getSkillDeepCopyById(LIBERATE_THE_SWIFT_RUNE);
        int skillID = 0;
        if (skill != null) {
            skillID = skill.getSkillId();
            skill.setCurrentLevel(1);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            Option o4 = new Option();
            o1.nReason = skillID;
            o1.nValue = si.getValue(indieBooster, slv);
            o1.tTerm = si.getValue(time, slv);
            newStats.put(IndieBooster, o1);

            o3.nReason = skillID;
            o3.nValue = si.getValue(indieJump, slv);
            o3.tTerm = si.getValue(time, slv);
            newStats.put(IndieJump, o3);

            o4.nReason = skillID;
            o4.nValue = si.getValue(indieSpeed, slv);
            o4.tTerm = si.getValue(time, slv);
            newStats.put(IndieSpeed, o4);

            tsm.sendStat(newStats);
        }
    }

    private void applyRuneRecovery(Char chr) {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = SkillData.getSkillDeepCopyById(LIBERATE_THE_RECOVERY_RUNE);
        int skillID = skill.getSkillId();
        skill.setCurrentLevel(1);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();

        o1.nReason = skillID;
        o1.nValue = si.getValue(indieAsrR, slv);
        o1.tTerm = si.getValue(time, slv);
        newStats.put(IndieAsrR, o1);
        newStats.put(IndieTerR, o1.deepCopy());
        o2.nReason = skillID;
        o2.nValue = si.getValue(indieExp, slv);
        o2.tTerm = si.getValue(time, slv);
        newStats.put(IndieEXP, o2);
        o3.nOption = si.getValue(ignoreMobDamR, slv);
        o3.rOption = skillID;
        o3.tOption = si.getValue(time, slv);
        newStats.put(IgnoreMobDamR, o3);

        tsm.sendStat(newStats);

        if (chr.getRuneRecoveryTimer() != null) {
            chr.getRuneRecoveryTimer().cancel(true);
        }
        ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(() -> {
            if (chr.getHP() < chr.getMaxHP()) {
                chr.heal((int) (0.1D * chr.getMaxHP()));
            }
        }, 0, 2, TimeUnit.SECONDS, false);
        chr.setRuneRecoveryTimer(sf);
        GlobalTimerManager.addCharTimer(chr.getId(), sf);
        chr.getTimer().addEvent(() -> {
            if (chr.getRuneRecoveryTimer() != null) {
                chr.getRuneRecoveryTimer().cancel(true);
                chr.setRuneRecoveryTimer(null);
            }
        }, si.getValue(time, slv) + 1, TimeUnit.SECONDS);
    }

    private void applyRuneThunder(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = SkillData.getSkillDeepCopyById(LIBERATE_THE_RUNE_OF_THUNDER_ATTACK);
        int skillID = skill.getSkillId();
        skill.setCurrentLevel(1);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        Option o1 = new Option();

        // RandAreaAttack Buff
        o1.nOption = 1;
        o1.rOption = skillID;
        o1.tOption = SkillData.getSkillInfoById(80001756).getValue(time, slv);
        tsm.sendStat(RandAreaAttack, o1);

        int fieldID = chr.getFieldID();
        randAreaAttack(fieldID, tsm, chr);
    }

    private void randAreaAttack(int fieldID, TemporaryStatManager tsm, Char chr) {
        if ((tsm.getOptByCTSAndSkill(RandAreaAttack, LIBERATE_THE_RUNE_OF_THUNDER_ATTACK) == null) || fieldID != chr.getFieldID()) {
            return;
        }

        Map<Position, Integer> mobAttackedList = new HashMap<>(5);
        for (Mob mob : chr.getField().getMobs()) {
            mobAttackedList.put(mob.getPosition(), mob.getObjectId());
        }
        chr.write(UserLocal.userRandAreaAttackRequest(mobAttackedList, LIBERATE_THE_RUNE_OF_THUNDER_ATTACK));

        if (thunderTimer != null && !thunderTimer.isDone()) {
            thunderTimer.cancel(true);
        }
        thunderTimer = chr.getTimer().addEvent(() -> randAreaAttack(fieldID, tsm, chr), FieldConstants.THUNDER_RUNE_ATTACK_DELAY, TimeUnit.SECONDS);
    }

    private void applyRuneMight(Char chr) {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = SkillData.getSkillDeepCopyById(LIBERATE_THE_RUNE_OF_MIGHT_2);
        int skillID = 0;
        if (skill != null) {
            skillID = skill.getSkillId();
            skill.setCurrentLevel(1);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            Option o1 = new Option();
            Option o2 = new Option();
            o1.nOption = si.getValue(x, slv);
            o1.rOption = skillID;
            o1.tOption = si.getValue(time, slv);
            newStats.put(Inflation, o1);
            o2.nReason = skillID;
            o2.nValue = si.getValue(indieSpeed, slv);
            o2.tTerm = si.getValue(time, slv);
            newStats.put(IndieSpeed, o2);
            newStats.put(IndieJump, o2.deepCopy());
            tsm.sendStat(newStats);
        }
    }

    private void applyRuneDarkness(Char chr) {
        Field field = chr.getField();
        int numberOfEliteMobsSpawned = GameConstants.DARKNESS_RUNE_NUMBER_OF_ELITE_MOBS_SPAWNED;
        for (int i = 0; i < numberOfEliteMobsSpawned; i++) {
            Mob mob = Util.getRandomFromCollection(field.getMobs());
            if (mob != null) {
                mob.spawnEliteMobRuneOfDarkness();
            }
        }
    }

    private void applyRuneBlessing(Char chr) {
        Summon summon = Summon.getSummonByAndSetStat(chr, LIBERATE_THE_RUNE_OF_BLESSING_SUMMON, 1);
        summon.setAssistType(AssistType.Attack);
        summon.setMoveAbility(MoveAbility.Stop);
        chr.getField().spawnSummon(summon);
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public enum EventType {
        None,
        Halloween,
        Anniverssary,
        NewYear,
        Honey
    }
}
