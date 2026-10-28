package net.swordie.ms.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.items.ItemBuffs;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.adventurer.archer.Marksman;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.pirate.Corsair;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.client.jobs.adventurer.thief.NightLord;
import net.swordie.ms.client.jobs.adventurer.warrior.DarkKnight;
import net.swordie.ms.client.jobs.cygnus.WindArcher;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.resistance.*;
import net.swordie.ms.client.jobs.resistance.demon.Demon;
import net.swordie.ms.client.jobs.sengoku.Hayato;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;


import java.util.*;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Summon extends Life {

    private int ownerId;
    private int skillID;
    private int bulletID;
    private int summonTerm;
    private int specialJaguarTerm;
    private int charLevel;
    private int slv;
    private AssistType assistType;
    private EnterType enterType;
    private LeaveType leaveType;
    private byte teslaCoilState;
    private boolean flyMob;
    private boolean beforeFirstAttack;
    private boolean jaguarActive;
    private boolean specialJaguarActive;
    private boolean attackActive;
    private short curFoothold;
    private AvatarLook avatarLook;
    private List<Position> teslaCoilPositions = new ArrayList<>();
    private List<Position> clonePosition = new ArrayList<>();
    private MoveAbility moveAbility;
    private int maxHP;
    private int hp;
    private int count;
    private int state;
    private int mobID;
    private Tuple<Tuple<Short, Short>, Tuple<Short, Short>> kishinValue;
    private List<Integer> linkedSummonSkillIds = new ArrayList<>();
    private int actionDelay = 400, movementDelay = 30;
    private boolean deleteOnNextAttack;
    private int mobDefeated;
    private int tms282SequencePhase;

    public Summon(int templateId) {
        super(templateId);
    }

    public static Summon getSummonByAndSetStat(Char chr, int skillID, int slv) {
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        Summon summon = new Summon(-1);
        summon.setOwnerId(chr.getId());
        summon.setSkillID(skillID);
        summon.setSlv(slv);
        summon.setSummonTerm(si.getValue(SkillStat.time, slv));
        summon.setCharLevel((byte) chr.getStat(Stat.level));
        summon.setPosition(chr.getPosition().deepCopy());
        summon.setMoveAction((byte) 1);
        short curFoothold;
        if (summon.getPosition() != null) {
            curFoothold = chr.getField().findFootHoldBelow(summon.getPosition()) != null ? (short) chr.getField().findFootHoldBelow(summon.getPosition()).getId() : 0;
        } else {
            curFoothold = chr.getField().findFootHoldBelow(chr.getPosition()) != null ? (short) chr.getField().findFootHoldBelow(chr.getPosition()).getId() : 0;
        }
        summon.setCurFoothold(curFoothold);
        summon.setMoveAbility(MoveAbility.Walk);
        summon.setAssistType(AssistType.Attack);
        summon.setEnterType(EnterType.Animation);
        summon.setLeaveType(LeaveType.ANIMATION);
        summon.setBeforeFirstAttack(true);
        summon.setTemplateId(skillID);
        summon.setAttackActive(true);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        o1.nReason = skillID;
        o1.nValue = 1;
        o1.summon = summon;
        o1.tTerm = summon.getSummonTerm() / 1000;
        tsm.sendStat(IndieBuffIcon, o1);
        return summon;
    }

    public static Summon getSummonBy(Char chr, int skillID, int slv) {
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        Summon summon = new Summon(-1);
        summon.setOwnerId(chr.getId());
        summon.setSkillID(skillID);
        summon.setSlv(slv);
        summon.setSummonTerm(si.getValue(SkillStat.time, slv));
        summon.setCharLevel((byte) chr.getStat(Stat.level));
        summon.setPosition(chr.getPosition().deepCopy());
        summon.setMoveAction((byte) 1);
        short curFoothold;
        if (summon.getPosition() != null) {
            curFoothold = chr.getField().findFootHoldBelow(summon.getPosition()) != null ? (short) chr.getField().findFootHoldBelow(summon.getPosition()).getId() : 0;
        } else {
            curFoothold = chr.getField().findFootHoldBelow(chr.getPosition()) != null ? (short) chr.getField().findFootHoldBelow(chr.getPosition()).getId() : 0;
        }
        summon.setCurFoothold(curFoothold);
        summon.setMoveAbility(MoveAbility.Walk);
        summon.setAssistType(AssistType.Attack);
        summon.setEnterType(EnterType.Animation);
        summon.setLeaveType(LeaveType.ANIMATION);
        summon.setBeforeFirstAttack(true);
        summon.setTemplateId(skillID);
        summon.setAttackActive(true);
        return summon;
    }

    public static Summon getSummonByAndSetStatWithTime(Char chr, int skillID, int slv, long tStart, int tTerm) {
        Summon summon = new Summon(-1);
        summon.setOwnerId(chr.getId());
        summon.setSkillID(skillID);
        summon.setSlv(slv);
        summon.setSummonTerm(tTerm);
        summon.setCharLevel((byte) chr.getStat(Stat.level));
        summon.setPosition(chr.getPosition().deepCopy());
        summon.setMoveAction((byte) 1);
        short curFoothold;
        if (summon.getPosition() != null) {
            curFoothold = chr.getField().findFootHoldBelow(summon.getPosition()) != null ? (short) chr.getField().findFootHoldBelow(summon.getPosition()).getId() : 0;
        } else {
            curFoothold = chr.getField().findFootHoldBelow(chr.getPosition()) != null ? (short) chr.getField().findFootHoldBelow(chr.getPosition()).getId() : 0;
        }
        summon.setCurFoothold(curFoothold);
        summon.setMoveAbility(MoveAbility.Walk);
        summon.setAssistType(AssistType.Attack);
        summon.setEnterType(EnterType.Animation);
        summon.setLeaveType(LeaveType.ANIMATION);
        summon.setBeforeFirstAttack(true);
        summon.setTemplateId(skillID);
        summon.setAttackActive(true);

        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        o1.nReason = skillID;
        o1.nValue = 1;
        o1.summon = summon;
        o1.tStart = (int) tStart;
        o1.startTime = tStart;
        o1.tTerm = tTerm;
        tsm.sendStat(IndieBuffIcon, o1);
        return summon;
    }

    public static void summonJaguar(Char chr, int skillID) {
        byte slv = 1;
        Summon summon = new Summon(-1);
        summon.setOwnerId(chr.getId());
        summon.setSkillID(skillID);
        summon.setSlv(slv);
        summon.setSummonTerm(Integer.MAX_VALUE);
        summon.setCharLevel((byte) chr.getStat(Stat.level));
        summon.setPosition(chr.getPosition().deepCopy());
        summon.setMoveAction((byte) 1);
        short curFoothold;
        if (summon.getPosition() != null) {
            curFoothold = chr.getField().findFootHoldBelow(summon.getPosition()) != null ? (short) chr.getField().findFootHoldBelow(summon.getPosition()).getId() : 0;
        } else {
            curFoothold = chr.getField().findFootHoldBelow(chr.getPosition()) != null ? (short) chr.getField().findFootHoldBelow(chr.getPosition()).getId() : 0;
        }
        summon.setCurFoothold(curFoothold);
        summon.setMoveAbility(MoveAbility.Jaguar);
        summon.setAssistType(AssistType.AttackJaguar);
        summon.setEnterType(EnterType.Animation);
        summon.setBeforeFirstAttack(true);
        summon.setTemplateId(skillID);
        summon.setJaguarActive(true);
        summon.setAttackActive(true);

        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        o1.nReason = skillID;
        o1.nValue = 1;
        o1.summon = summon;
        o1.tTerm = summon.getSummonTerm() / 1000;
        newStats.put(IndieBuffIcon, o1);
        o2.nOption = 1;
        o2.rOption = skillID;
        o2.tOption = 0;
        newStats.put(JaguarSummoned, o2);
        newStats.put(JaguarCount, o2.deepCopy());

        tsm.sendStat(newStats);

        chr.getField().spawnSummon(summon);
    }

    public static void summonJaguarStorm(Char chr, int skillID, Position pos, int duration) {
        byte slv = 1;
        Summon summon = new Summon(-1);

        Foothold foothold = chr.getField().findFootHoldBelow(pos);
        short currentFootHold = (short) (foothold != null ? foothold.getId() : 0);

        summon.setOwnerId(chr.getId());
        summon.setSkillID(skillID);
        summon.setSlv(slv);
        summon.setSummonTerm(duration);
        summon.setSpecialJaguarTerm(duration);
        summon.setCharLevel((byte) chr.getStat(Stat.level));
        summon.setPosition(pos);
        summon.setMoveAction((byte) 1);
        summon.setCurFoothold(currentFootHold);

        summon.setMoveAbility(MoveAbility.WalkRandom);
        summon.setAssistType(AssistType.Attack);
        summon.setEnterType(EnterType.Animation);
        summon.setBeforeFirstAttack(true);
        summon.setTemplateId(skillID);
        summon.setJaguarActive(true);
        summon.setSpecialJaguarActive(false);
        summon.setAttackActive(true);
        summon.setState(1); // sniffed value
        summon.setCount(300); // sniffed value

        chr.getField().spawnAddSummon(summon);
    }

    public static void createKishin(Char chr, int slv) {
        Field field = chr.getField();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        int skillID = Kanna.KISHIN_SHOUKAN;

        // Remove both Old Kishins
        List<Life> oldSummons = field.getLifes().values().stream().filter(s -> s instanceof Summon summon && summon.getOwnerId() == chr.getId() && summon.getSkillID() == skillID).toList();
        for (Life life : oldSummons) {
            field.removeLife(life.getObjectId(), false);
        }
        Position cPos = chr.getPosition();
        int posInc = getPosInc(chr);
        int x1 = cPos.getX() - 55 - posInc;
        int y1 = cPos.getY() - 93;
        int x2 = cPos.getX() + 55;
        int y2 = cPos.getY() + 5;
        Tuple tuple = new Tuple<>(new Tuple<>((short) x1, (short) x2), new Tuple<>((short) y1, (short) y2));
        //Some field is not true btw.
        Position rPos = new Position(cPos.getX() + posInc, cPos.getY());
        Foothold rFH = field.findFootHoldBelow(rPos);
        Summon sRight = getSummonBy(chr, skillID, slv);
        sRight.setFlyMob(false);
        sRight.setPosition(rPos);
        sRight.setCurFoothold(rFH == null ? chr.getFoothold() : (short) rFH.getId());
        sRight.setMoveAbility(MoveAbility.Stop);
        sRight.setMoveAction((byte) 1);
        sRight.setAssistType(AssistType.Attack);
        sRight.setKishinValue(tuple);
        field.spawnAddSummon(sRight);
        o1.nReason = skillID;
        o1.nValue = 1;
        o1.summon = sRight;
        o1.tTerm = sRight.getSummonTerm() / 1000;
        tsm.sendStat(IndieBuffIcon, o1);

        Position lPos = new Position(cPos.getX() - posInc, cPos.getY());
        Foothold lFH = field.findFootHoldBelow(lPos);
        Summon sLeft = getSummonBy(chr, skillID, slv);
        sLeft.setFlyMob(false);
        sLeft.setPosition(lPos);
        sLeft.setCurFoothold(rFH == null ? chr.getFoothold() : (short) lFH.getId());
        sLeft.setMoveAbility(MoveAbility.Stop);
        sLeft.setMoveAction((byte) 0);
        sLeft.setAssistType(AssistType.Attack);
        sLeft.setKishinValue(tuple);
        field.spawnAddSummon(sLeft);
        o2.nReason = skillID;
        o2.nValue = 1;
        o2.summon = sLeft;
        o2.tTerm = sLeft.getSummonTerm() / 1000;
        tsm.sendStat(IndieBuffIcon, o2);
    }

    private static int getPosInc(Char chr) {
        int MAX_VALUE = 200;
        int x = chr.getPosition().getX();
        int y = chr.getPosition().getY();
        Foothold left, right;
        for (int i = MAX_VALUE; i >= 0; i--) {
            left = chr.getField().findFootHoldBelow(new Position(x - i, y));
            right = chr.getField().findFootHoldBelow(new Position(x + i, y));

            if (left != null && right != null && left.getY1() == right.getY1() && left.getY2() == right.getY2()) {
                return i;
            }
        }
        return 0;
    }

    public Char getChr() {
        return getField().getCharByID(getOwnerId());
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public int getCharLevel() {
        return charLevel;
    }

    public void setCharLevel(int charLevel) {
        this.charLevel = charLevel;
    }

    public int getSlv() {
        return slv;
    }

    public void setSlv(int slv) {
        this.slv = slv;
    }

    /** Server-side phase of the 282 mirror-spider sequence; never encoded in 265 summon packets. */
    public int getTms282SequencePhase() { return tms282SequencePhase; }

    public void setTms282SequencePhase(int phase) { tms282SequencePhase = phase; }

    public int getBulletID() {
        return bulletID;
    }

    public void setBulletID(int bulletID) {
        this.bulletID = bulletID;
    }

    public int getSummonTerm() {
        return summonTerm;
    }

    public void setSummonTerm(int summonTerm) {
        this.summonTerm = 1000 * summonTerm;
    }

    public int getSpecialJaguarTerm() {
        return specialJaguarTerm;
    }

    public void setSpecialJaguarTerm(int specialJaguarTerm) {
        this.specialJaguarTerm = 1000 * specialJaguarTerm;
    }

    public AssistType getAssistType() {
        return assistType;
    }

    public void setAssistType(AssistType assistType) {
        this.assistType = assistType;
    }

    public EnterType getEnterType() {
        return enterType;
    }

    public void setEnterType(EnterType enterType) {
        this.enterType = enterType;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public int getMobID() {
        return mobID;
    }

    public void setMobID(int mobID) {
        this.mobID = mobID;
    }

    public byte getTeslaCoilState() {
        return teslaCoilState;
    }

    public void setTeslaCoilState(byte teslaCoilState) {
        this.teslaCoilState = teslaCoilState;
    }

    public boolean isFlyMob() {
        return flyMob;
    }

    public void setFlyMob(boolean flyMob) {
        this.flyMob = flyMob;
    }

    public boolean isBeforeFirstAttack() {
        return beforeFirstAttack;
    }

    public void setBeforeFirstAttack(boolean beforeFirstAttack) {
        this.beforeFirstAttack = beforeFirstAttack;
    }

    public boolean isJaguarActive() {
        return jaguarActive;
    }

    public void setJaguarActive(boolean jaguarActive) {
        this.jaguarActive = jaguarActive;
    }

    public boolean isSpecialJaguarActive() {
        return specialJaguarActive;
    }

    public void setSpecialJaguarActive(boolean specialJaguarActive) {
        this.specialJaguarActive = specialJaguarActive;
    }

    public boolean isAttackActive() {
        return attackActive;
    }

    public void setAttackActive(boolean attackActive) {
        this.attackActive = attackActive;
    }

    public short getCurFoothold() {
        return curFoothold;
    }

    public void setCurFoothold(short curFoothold) {
        this.curFoothold = curFoothold;
    }

    public AvatarLook getAvatarLook() {
        return avatarLook;
    }

    public void setAvatarLook(AvatarLook avatarLook) {
        this.avatarLook = avatarLook;
    }

    public List<Position> getTeslaCoilPositions() {
        return teslaCoilPositions;
    }

    public void setTeslaCoilPositions(List<Position> teslaCoilPositions) {
        this.teslaCoilPositions = teslaCoilPositions;
    }

    public MoveAbility getMoveAbility() {
        return moveAbility;
    }

    public void setMoveAbility(MoveAbility moveAbility) {
        this.moveAbility = moveAbility;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public void setMaxHP(int maxHP) {
        this.maxHP = maxHP;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public Tuple<Tuple<Short, Short>, Tuple<Short, Short>> getKishinValue() {
        return kishinValue;
    }

    public void setKishinValue(Tuple<Tuple<Short, Short>, Tuple<Short, Short>> kishinValue) {
        this.kishinValue = kishinValue;
    }

    public int getActionDelay() {
        return actionDelay;
    }

    public void setActionDelay(int actionDelay) {
        this.actionDelay = actionDelay;
    }

    public int getMovementDelay() {
        return movementDelay;
    }

    public void setMovementDelay(int movementDelay) {
        this.movementDelay = movementDelay;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incCount() {
        incCount(1);
    }

    public void incCount(int add) {
        setCount(getCount() + add);
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void incState() {
        incState(1);
    }

    public void incState(int inc) {
        setState(getState() + inc);
    }

    public boolean isDeleteOnNextAttack() {
        return deleteOnNextAttack;
    }

    public void setDeleteOnNextAttack(boolean deleteOnNextAttack) {
        this.deleteOnNextAttack = deleteOnNextAttack;
    }

    public int getMobDefeated() {
        return mobDefeated;
    }

    public void setMobDefeated(int mobDefeated) {
        this.mobDefeated = mobDefeated;
    }

    public void incMobDefeated(int inc) {
        this.mobDefeated += inc;
    }

    public List<Integer> getLinkedSummonSkillIds() {
        return linkedSummonSkillIds;
    }

    public void setLinkedSummonSkillIds(List<Integer> linkedSummonSkillIds) {
        this.linkedSummonSkillIds = linkedSummonSkillIds;
    }

    public void onSkillUse(Char chr, int skillId, InPacket inPacket) {
        Job job = chr.getJobHandler();
        switch (skillId) {
            case DarkKnight.EVIL_EYE -> ((DarkKnight) job).healByEvilEye();
            case DarkKnight.HEX_OF_THE_EVIL_EYE -> ((DarkKnight) job).giveHexOfTheEvilEyeBuffs();
            case net.swordie.ms.client.jobs.resistance.Mechanic.SUPPORT_UNIT_HEX, net.swordie.ms.client.jobs.resistance.Mechanic.ENHANCED_SUPPORT_UNIT ->
                    ((Mechanic) job).healFromSupportUnit(this);
            case net.swordie.ms.client.jobs.resistance.Mechanic.BOTS_N_TOTS ->
                    ((net.swordie.ms.client.jobs.resistance.Mechanic) job).spawnBotsNTotsSubSummons(this);
            case Shade.SPIRIT_BOND_MAX_2 -> ((Shade) job).doSpiritWard();
            case NightLord.DARK_LORDS_OMEN -> ((NightLord) job).createDarkLordOmenForceAtoms(this);
            case Demon.DEFENDER_OF_THE_DEMON -> ((Demon) job).giveMastemasMark();
            case Bishop.ANGEL_OF_BALANCE_BENEVOLENCE ->
                    ((Bishop) job).giveAngelOfBalanceSummonBuff();
            case Illium.RESONANCE -> ((Illium) job).doResonanceSkill();
            case Shade.SPIRITGATE_SUMMONER -> ((Shade) job).summonSpiritgateSummons(getPosition());
            case Corsair.ALL_ABOARD, Corsair.HEXA_SCURVY_SUMMONS -> ((Corsair) job).handleScurvySecondAtoms();
            default -> {
                int buffItem = SkillConstants.getBuffSkillItem(skillId);
                if (buffItem != 0) {
                    ItemBuffs.giveItemBuffsFromItemID(chr, chr.getTemporaryStatManager(), buffItem);
                } else {
                    System.out.printf("Unhandled Summon Skill: %d, casted by Summon: %d", skillId, getSkillID());
                }
            }
        }
        chr.write(UserPacket.effect(Effect.skillAffected(skillId, (byte) 1, getObjectId())));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffected(skillId, (byte) 1, getObjectId())), chr);
    }

    public void onHit(int damage, int mobTemplateId) {
        Char owner = getChr();
        Job job = owner.getJobHandler();
        Skill skill = owner.getSkill(getSkillID());
        if (skill == null) {
            return;
        }
        int summonHP = getHp();
        int newSummonHP = summonHP - damage;
        switch (getSkillID()) {
            case DualBlade.MIRRORED_TARGET:
                ((DualBlade) job).giveShadowMeld();
                break;
            case WindArcher.EMERALD_DUST:
                ((WindArcher) job).applyEmeraldDustDebuffToMob(this, mobTemplateId);
                // Fallthrough intended
            case WindArcher.EMERALD_FLOWER:
                ((WindArcher) job).applyEmeraldFlowerDebuffToMob(this, mobTemplateId);
                break;
            case WildHunter.SUMMON_JAGUAR_GREY:
            case WildHunter.SUMMON_JAGUAR_YELLOW:
            case WildHunter.SUMMON_JAGUAR_RED:
            case WildHunter.SUMMON_JAGUAR_PURPLE:
            case WildHunter.SUMMON_JAGUAR_BLUE:
            case WildHunter.SUMMON_JAGUAR_JAIRA:
            case WildHunter.SUMMON_JAGUAR_SNOW_WHITE:
            case WildHunter.SUMMON_JAGUAR_ONYX:
            case WildHunter.SUMMON_JAGUAR_CRIMSON:
                // Invincible
                return;
            case Marksman.ARROW_ILLUSION:
                break;
            default:
                System.out.printf("Unhandled HP Summon, id = %d", getSkillID());
                break;
        }
        if (newSummonHP <= 0) {
            TemporaryStatManager tsm = owner.getTemporaryStatManager();
            owner.getField().broadcast(Summoned.removed(this, LeaveType.ANIMATION));
            tsm.removeStatsBySkill(skill.getSkillId());
        } else {
            setHp(newSummonHP);
            owner.write(Summoned.updateHPTag(this));
        }
    }

    public void onAttack(Char owner, int attackSkillId) {
        if (!owner.hasSkill(attackSkillId)) {
            return;
        }
        Skill skill = owner.getSkill(attackSkillId);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        switch (attackSkillId) {
            case FirePoison.ELEMENTAL_FURY:
            case DarkKnight.RADIANT_EVIL:
                owner.setSkillCooldown(attackSkillId, owner.getSkillLevel(attackSkillId));
                break;

            default:
                System.out.printf("Unhandled Summon Attack: %d. Used by Summon: %d%n", attackSkillId, getSkillID());
                break;
        }
    }

    public void onRemoved() {
        Field field = getField();
        if (field == null) {
            return;
        }
        handleKishinRemove(field);

        Char owner = getChr();
        if (owner == null) {
            return;
        }
        Job job = owner.getJobHandler();
        if (job == null) {
            return;
        }
        int skillId = getSkillID();
        switch (skillId) {
            case Hayato.BATTOUJUTSU_ULTIMATE_WILL:
                ((Hayato) job).incSwordEnergyFromGodOfBladesSummon(getCount());
                break;
            case BattleMage.ALTAR_OF_ANNIHILATION:
                ((BattleMage) job).getAnnihilationAltarList().remove(this);
                break;
            case Kanna.SPIRITS_DOMAIN:
                AffectedArea aa = field.getAffectedAreas().stream().filter(sdaa -> sdaa.getCharID() == owner.getId() && sdaa.getSkillID() == Kanna.SPIRITS_DOMAIN).findFirst().orElse(null);
                if (aa != null) {
                    field.removeLife(aa);
                }
                break;
        }
    }

    @Override
    public void broadcastSpawnPacket(Char onlyChar) {
        Field field = getField();
        if (field == null) {
            return;
        }
        Char owner = getChr();
        if (owner == null) {
            return;
        }
        if (getSummonTerm() > 0) {
            if (getSkillID() != Shade.SPIRIT_BOND_MAX_2) {
                getTimer().addEvent(() -> field.removeLife(getObjectId(), true), getSummonTerm());
            }
        }
        field.broadcast(Summoned.created(this, owner));
    }

    @Override
    public void broadcastLeavePacket() {
        Field field = getField();
        if (field == null) {
            return;
        }
        onRemoved();
        field.broadcast(Summoned.removed(this, getLeaveType() == null ? LeaveType.ANIMATION : getLeaveType()));
    }

    public void handleRemove() {
        Field field = getField();
        if (field == null) {
            return;
        }
        field.removeLife(this);
        handleKishinRemove(field);
    }

    public void handleKishinRemove(Field field) {
        if ((getSkillID() == Job.MONOLITH || getSkillID() == Job.FURY_TOTEM)
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            // Removing one user's totem must not disable another user's totem or an unrelated Kishin.
            int vip = field.getSummons().stream().filter(s -> s != this
                    && (s.getSkillID() == Job.MONOLITH || s.getSkillID() == Job.FURY_TOTEM))
                    .mapToInt(s -> s.getChr() == null || s.getChr().getUser() == null ? 0 : s.getChr().getUser().getVipGrade())
                    .max().orElse(-1);
            field.setTotem(new Tuple<>(vip >= 0, Math.max(0, vip)));
            field.setTms282MonolithTier(net.swordie.ms.world.field.Tms282Monolith.activeTier(field, this));
            return;
        }
        switch (getSkillID()) {
            case Job.MONOLITH:
            case Job.FURY_TOTEM:
                field.setTotem(new Tuple<>(false, 0));
            case Kanna.KISHIN_SHOUKAN:
            case Kanna.HEXA_KISHIN_SHOUKAN:
                field.setKishin(false);
                break;
        }
    }
}
