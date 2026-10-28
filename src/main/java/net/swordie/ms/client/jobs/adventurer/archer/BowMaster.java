package net.swordie.ms.client.jobs.adventurer.archer;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class BowMaster extends Archer {

    public static final int SOUL_ARROW = 3100011;
    public static final int ARROW_BOMB = 3101005;
    public static final int QUIVER_CARTRIDGE = 3101009;
    public static final int QUIVER_CARTRIDGE_ATOM = 3100010;
    public static final int QUIVER_CARTRIDGE_BLOOD = 3101012;
    public static final int QUIVER_CARTRIDGE_MAGIC = 3101013;

    public static final int PHOENIX = 3111005;

    public static final int MORTAL_BLOW = 3110001;
    public static final int FLAME_SURGE = 3111003;
    public static final int EVASION_BOOST = 3110007;
    public static final int FOCUSED_FURY = 3110012;
    public static final int ARROW_PLATTER = 3111013;
    public static final int MARKSMANSHIP = 3110014;
    public static final int SPEED_MIRAGE = 3111015;
    public static final int SPEED_MIRAGE_ATOM = 3111016;
    public static final int BLINK_SHOT = 3111017;
    public static final int BLINK_SHOT_TP = 3111018;

    public static final int SPEED_MIRAGE_II = 3120021;
    public static final int SHARP_EYES = 3121002;
    public static final int SHARP_EYES_IED_H = 3120044;
    public static final int SHARP_EYES_CR_H = 3120045;
    public static final int ILLUSION_STEP = 3121007;
    public static final int ENCHANTED_QUIVER = 3120022;
    public static final int BINDING_SHOT = 3121014;
    public static final int HURRICANE = 3121020;
    public static final int ARMOR_BREAK = 3120018;
    public static final int HEROS_WILL = 3121009;

    // Hyper skills
    public static final int EPIC_ADVENTURE = 3121053;
    public static final int CONCENTRATION = 3121054;
    public static final int GRITTY_GUST = 3121052;

    //Final Attack
    public static final int FINAL_ATTACK = 3100001;
    public static final int ADVANCED_FINAL_ATTACK = 3120008;

    // V Skills
    public static final int STORM_OF_ARROWS = 400031002;
    public static final int STORM_OF_ARROWS_AA = 400030002;
    public static final int INHUMAN_SPEED = 400031020;
    public static final int INHUMAN_SPEED_BUFF = 400031021;
    public static final int QUIVER_BARRAGE = 400031028;
    public static final int QUIVER_BARRAGE_ATOM = 400031029;
    public static final int SILHOUETTE_MIRAGE = 400031053;
    public static final int SILHOUETTE_MIRAGE_ATTACK = 400031054;

    // HEXA Skills
    public static final int HEXA_HURRICANE = 3141000;
    public static final int HEXA_ARROW_STREAM = 3141002;
    public static final int HEXA_ENCHANCED_ARROW_STREAM = 3141003;
    public static final int HEXA_ARROW_BLASTER = 3141004;
    public static final int HEXA_QUIVER_CARTRIDGE = 3141005;
    public static final int HEXA_QUIVER_CARTRIDGE_ATOM = 3140006;
    public static final int HEXA_QUIVER_CARTRIDGE_MAGIC = 3140007;
    public static final int HEXA_QUIVER_CARTRIDGE_BLOOD = 3140008;
    public static final int HEXA_PHOENIX = 3141012;
    public static final int EXTRA_QUIVER_CARTRIDGE = 3141009;
    public static final int EXTRA_QUIVER_CARTRIDGE_MAGIC = 3140010;
    public static final int EXTRA_QUIVER_CARTRIDGE_BLOOD = 3140011;
    public static final int HEXA_SPEED_MIRAGE = 3141013;
    public static final int HEXA_SPEED_MIRAGE_ATOM = 3141014;
    public static final int HEXA_GRITTY_GUST = 3141015;

    // HEXA Boosts
    public static final int HEXA_SILHOUETTE_MIRAGE = 500061016;
    public static final int HEXA_SILHOUETTE_MIRAGE_ATTACK = 500061017;

    private ScheduledFuture<?> stormArrowTimer;
    private long stormArrowNextProcAt;

    private long lastArmorBreak = Long.MIN_VALUE;

    private int speedMirageHitCounter = 0;        // counts raw hits toward "7 hits = 1 strike"
    private int speedMirageStrikeCounter = 0;     // successful arrow-based strikes
    private long speedMirageNextProcAt = 0;       // 1s internal cooldown

    private long inhumanSpeedExpireAt = 0;      // ms
    private long inhumanSpeedNextFireAt = 0;    // ms
    private long inhumanSpeedNextExtendAt = 0;  // ms (1s gate)
    private int inhumanSpeedPsdAttackCount = 0;

    public BowMaster(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isBowMaster(id);
    }

    private int getQuiverCartridgeSkill() {
        if (chr.hasSkill(EXTRA_QUIVER_CARTRIDGE))
            return EXTRA_QUIVER_CARTRIDGE;
        if (chr.hasSkill(HEXA_QUIVER_CARTRIDGE))
            return HEXA_QUIVER_CARTRIDGE;
        if (chr.hasSkill(ENCHANTED_QUIVER))
            return ENCHANTED_QUIVER;
        return QUIVER_CARTRIDGE;
    }

    private int getQuiverCartridgeAtomSkill() {
        if (chr.hasSkill(HEXA_QUIVER_CARTRIDGE))
            return HEXA_QUIVER_CARTRIDGE_ATOM;
        return QUIVER_CARTRIDGE_ATOM;
    }

    private void quiverCartridge(TemporaryStatManager tsm, Mob mob, int slv) {
        Skill skill = chr.getSkill(getQuiverCartridgeSkill());
        if (skill == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        if (tsm.hasStat(QuiverFullBurst)) {
            bloodArrowEffect(si, slv);
            magicArrowEffect(mob, si, slv);
        } else {
            if (tsm.hasStat(QuiverCatridge)) {
                switch (tsm.getOption(QuiverCatridge).nOption) {
                    case 1: // Magic
                        magicArrowEffect(mob, si, slv);
                        break;
                    case 2: // Blood
                        bloodArrowEffect(si, slv);
                        break;
                }
            }
        }
    }

    private void bloodArrowEffect(SkillInfo si, int slv) {
        if (Util.succeedProp(si.getValue(w, slv))) {
            chr.heal((int) (chr.getMaxHP() * 1.8D / 100.0D));
        }
    }

    private void magicArrowEffect(Mob mob, SkillInfo si, int slv) {
        if (Util.succeedProp(si.getValue(u, slv)) && mob != null) {
            ForceAtomEnum fae = ForceAtomEnum.BM_ARROW;
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 13, 12,
                    (int) Util.getAngleOfTwoPositions(chr.getPosition(), mob.getPosition()), 150, Util.getCurrentTime(), 1, 0,
                    new Position());
            chr.createForceAtom(new ForceAtom(chr.getId(), fae, mob.getObjectId(),
                    getQuiverCartridgeAtomSkill(),
                    forceAtomInfo));
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        if (chr.hasSkill(MORTAL_BLOW)) {
            incrementMortalBlow(mob);
        }
        if (chr.hasSkill(ARMOR_BREAK)) {
            procArmorBreak(mob, skillID);
        }
        switch (skillID) {
            case ARROW_BOMB: {
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss() && Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            }
            case BINDING_SHOT: {
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(s, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    map.put(MobStat.Speed, o1);
                    o2.nOption = si.getValue(x, slv);
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(time, slv);
                    map.put(MobStat.DebuffHealing, o2);
                    mts.addStatOptions(mob, map);
                }
                break;
            }
            case GRITTY_GUST:
            case HEXA_GRITTY_GUST:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nValue = si.getValue(s, slv);
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.IndieSlow, o1);
                }
                break;
            case PHOENIX:
            case HEXA_PHOENIX: {
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(SkillStat.prop, slv))) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = 3;
                        mts.addStatOptions(mob, MobStat.Freeze, o1);
                    }
                }
                break;
            }
        }
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleAttack(c, attackInfo, si, now);
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        if (hasHitMobs) {
            if (Tms282RemoteBuffs.active()) triggerStormOfArrows(attackInfo, now);
            handleSpeedMirageOnAttack(attackInfo, now);
            if (chr.hasSkill(FOCUSED_FURY)) {
                incrementFocusedFury();
            }
            if (chr.hasSkill(MARKSMANSHIP)) {
                handleMarksmanship();
            }
            if (skillID != QUIVER_BARRAGE_ATOM && tsm.hasStat(QuiverFullBurst)) {
                createQuiverBarrageBurstForceAtom(attackInfo, now);
            }
            final boolean inHumanSpeedPsdAttacks = SkillData.getSkillInfoById(INHUMAN_SPEED).getSkillList1().contains(skillID);
            if (chr.hasSkill(INHUMAN_SPEED)) {
                if (skillID == INHUMAN_SPEED) {
                    List<Integer> mobOIDs = new ArrayList<>();
                    for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                        Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                        if (mob == null || mob.getHp() <= 0) {
                            continue;
                        }
                        mobOIDs.add(mob.getObjectId());
                    }
                    if (!mobOIDs.isEmpty()) {
                        chr.write(UserLocal.inHumanSpeedAfterAttack(4, mobOIDs));
                    }
                } else {
                    if (inHumanSpeedPsdAttacks) {
                        handleInhumanSpeedNextRequest(now);
                    }
                }
            }
            if (inHumanSpeedPsdAttacks) {
                final var mob = Util.getRandomFromCollection(chr.getField().getMobs(attackInfo.mobAttackInfo));
                this.quiverCartridge(tsm, mob, chr.getSkillLevel(skillID));
            }
            if (chr.hasSkill(SILHOUETTE_MIRAGE) && tsm.hasStat(ShadowShield)) {
                silhouetteMirageAttack(attackInfo);
            }
        }
    }

    private void silhouetteMirageAttack(AttackInfo attackInfo) {
        int skillID = chr.hasSkill(HEXA_SILHOUETTE_MIRAGE) ? HEXA_SILHOUETTE_MIRAGE : SILHOUETTE_MIRAGE;
        int attSkillID = chr.hasSkill(HEXA_SILHOUETTE_MIRAGE) ? HEXA_SILHOUETTE_MIRAGE_ATTACK : SILHOUETTE_MIRAGE_ATTACK;
        var skill = chr.getSkill(skillID);
        if (skill == null || chr.hasSkillOnCooldown(attSkillID)) {
            return;
        }

        var tsm = chr.getTemporaryStatManager();
        if (attackInfo.mobCount <= 0
                || attackInfo.skillId == attSkillID
                || SkillConstants.isForceAtomSkill(attackInfo.skillId)
                || tsm.getOption(ShadowShield).xOption <= 0) {
            return;
        }

        var si = SkillData.getSkillInfoById(skill.getSkillId());
        var siAttack = SkillData.getSkillInfoById(attSkillID);
        var slv = skill.getCurrentLevel();

        var bulletCount = si.getValue(SkillStat.bulletCount, slv);

        // Effect
        var effect = Effect.showSillhouteMirageAttack(skillID, 0, chr.getPosition());
        chr.write(UserPacket.effect(effect));

        // Force Atom
        var fae = ForceAtomEnum.SILHOUTTE_MIRAGE;
        List<ForceAtomInfo> faiList = new ArrayList<>();
        List<Integer> targetList = new ArrayList<>();
        var delay = siAttack.getValue(q2, slv); // init 90  += 210 every atom
        var ballDelay = siAttack.getValue(SkillStat.ballDelay, slv);
        for (int i = 0; i < bulletCount; i++) {
            var fImpact = Util.getRandom(21, 27);
            var sImpact = Util.getRandom(7, 12);
            var fai = new ForceAtomInfo(1, fae.getInc(), fImpact, sImpact,
                    0, delay += ballDelay, Util.getCurrentTime(), 0, 0,
                    new Position(42, -150));
            var target = Util.getRandomFromCollection(attackInfo.mobAttackInfo).mobId;

            faiList.add(fai);
            targetList.add(target);
        }
        var fa = new ForceAtom(false, chr.getId(), chr.getId(), fae,
                true, targetList, attSkillID, faiList, new Rect(), 0, 0,
                new Position(), 0, new Position(), 0);
        chr.createForceAtom(fa);

        chr.addSkillCooldown(attSkillID, si.getValue(w, slv));
    }

    private void handleMarksmanship() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        int skillID = MARKSMANSHIP;
        Skill skill = chr.getSkill(skillID);
        if (skill != null) {
            if (Util.succeedProp(5) && !tsm.hasStatBySkillId(skillID)) {
                SkillInfo si = SkillData.getSkillInfoById(skillID);
                o.nOption = si.getValue(ignoreMobpdpR, chr.getSkillLevel(skillID));
                o.rOption = skillID;
                o.tOption = 5;
                tsm.sendStat(IgnoreMobpdpR, o);
            }
        }
    }

    private void procArmorBreak(Mob mob, int skillId) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(ARMOR_BREAK) || !SkillConstants.isArmorPiercingSkill(skillId)) {
            if (tsm.hasStatBySkillId(ARMOR_BREAK)) {
                tsm.removeStatsBySkill(ARMOR_BREAK);
                return;
            }
        }

        Skill skill = chr.getSkill(ARMOR_BREAK);
        if (skill != null) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();

            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();

            int pdr = 0;
            pdr = mob.getForcedMobStat().getPdr();
            int finalDmgRate = si.getValue(x, slv);
            final long now = System.currentTimeMillis();
            if (this.lastArmorBreak + (si.getValue(y, slv) * 1000L) < now) {
                if (pdr > 0) {
                    if (tsm.getOptByCTSAndSkill(IndieIgnoreMobpdpR, ARMOR_BREAK) == null) {
                        o1.nValue = si.getValue(z, slv);
                        o1.nReason = ARMOR_BREAK;
                        o1.tTerm = 2;
                        newStats.put(IndieIgnoreMobpdpR, o1);
                        o2.nValue = (int) (pdr * ((double) finalDmgRate / 100));
                        o2.nReason = ARMOR_BREAK;
                        o2.tTerm = 2;
                        newStats.put(IndieDamR, o2);
                        tsm.sendStat(newStats);
                    } else {
                        if (tsm.hasStatBySkillId(ARMOR_BREAK)) {
                            tsm.removeStatsBySkill(ARMOR_BREAK);
                        }
                        this.lastArmorBreak = now;
                        chr.write(UserPacket.effect(Effect.skillUse(skill.getSkillId(), chr.getLevel(), slv)));
                        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(skill.getSkillId(), chr.getLevel(), slv)), chr);
                    }
                }
            }
        }
    }

    private void incrementMortalBlow(Mob mob) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        int amount = 1;
        if (chr.hasSkill(MORTAL_BLOW)) {
            if (tsm.hasStat(BowMasterMortalBlow)) {
                amount = tsm.getOption(BowMasterMortalBlow).nOption;
                if (amount < 9) {
                    amount++;
                } else {
                    if (Util.succeedProp(10)) {
                        chr.getField().broadcast(MobPool.specialEffectBySkill(mob, MORTAL_BLOW, chr.getId(), amount));
                        amount = 1;
                    }
                }
            }
            o.nOption = amount;
            o.rOption = MORTAL_BLOW;
            tsm.sendStat(BowMasterMortalBlow, o);
        }
    }

    private void incrementFocusedFury() {
        int skillID = FOCUSED_FURY;
        Skill skill = chr.getSkill(skillID);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        int amount = 0;

        if (tsm.hasStat(BowMasterConcentration)) {
            amount = tsm.getOption(BowMasterConcentration).nOption;
            if (amount < (100 / si.getValue(x, slv))) {
                amount++;
            }
        }
        o1.nOption = amount;
        o1.rOption = skillID;
        o1.tOption = si.getValue(time, slv);
        tsm.sendStat(BowMasterConcentration, o1);
        if (amount % 10 == 0 && amount < (100 / si.getValue(x, slv))) {
            var effect = Effect.skillUse(skill.getSkillId(), chr.getLevel(), slv);
            chr.write(UserPacket.effect(effect));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
        }
    }

    private void triggerStormOfArrows(AttackInfo attack, long now) {
        if (!JobConstants.isBowMaster(chr.getJob()) || !chr.isOnline() || chr.getHP() <= 0 || chr.getField() == null
                || chr.getClient() == null || chr.getClient().getChr() != chr || chr.getClient().getCurrentState() != Client.IN_FIELD
                || attack.skillId == 0 || attack.skillId == STORM_OF_ARROWS_AA || now < stormArrowNextProcAt
                || !chr.hasSkill(STORM_OF_ARROWS) || !chr.getTemporaryStatManager().hasStatBySkillId(STORM_OF_ARROWS)) return;
        // Bounded to ordinary accepted user attacks. Summon/body/area and separate atom callbacks do not proc it.
        if (attack.inHeader != net.swordie.ms.handlers.header.InHeader.USER_MELEE_ATTACK
                && attack.inHeader != net.swordie.ms.handlers.header.InHeader.USER_SHOOT_ATTACK
                && attack.inHeader != net.swordie.ms.handlers.header.InHeader.USER_MAGIC_ATTACK) return;
        if (net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents.useLevel(chr, attack.skillId) <= 0
                || attack.mobAttackInfo.stream().noneMatch(m -> chr.getField().getLifeByObjectID(m.mobId) instanceof Mob mob
                && mob.getHp() > 0)) return;
        int slv = chr.getSkillLevel(STORM_OF_ARROWS);
        stormArrowNextProcAt = now + SkillData.getSkillInfoById(STORM_OF_ARROWS).getValue(x, slv) * 1000L;
        stormOfArrowAA();
    }

    private void stormOfArrowAA() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(STORM_OF_ARROWS) && tsm.hasStatBySkillId(STORM_OF_ARROWS)) {
            Field field = chr.getField();
            final var mobs = chr.getField().getMobs();
            if (mobs.isEmpty()) {
                return;
            }
            if (Tms282RemoteBuffs.active()) {
                int slv = chr.getSkillLevel(STORM_OF_ARROWS);
                int maximum = SkillData.getSkillInfoById(STORM_OF_ARROWS).getValue(y, slv);
                var occupied = new ArrayList<Rect>();
                field.getLifes().values().stream().filter(l -> l instanceof AffectedArea aa
                                && aa.getOwner() == chr && aa.getSkillID() == STORM_OF_ARROWS_AA)
                        .map(l -> ((AffectedArea) l).getRect()).filter(Objects::nonNull).forEach(occupied::add);
                var candidates = new ArrayList<>(mobs);
                Collections.shuffle(candidates);
                for (Mob mob : candidates) {
                    if (occupied.size() >= maximum) break;
                    if (mob.getHp() <= 0 || mob.getPosition() == null) continue;
                    // Server placement policy: keep the selected live target inside current WZ rect2.
                    // The old+250/jitter belonged to the asymmetric primary rectangle; it displaces this one.
                    Position position = mob.getPosition().deepCopy();
                    Rect rect = position.getRectAround(new Rect(-150, -330, 150, 10));
                    if (occupied.stream().anyMatch(r -> r.overlaps(rect))) continue;
                    AffectedArea aa = AffectedArea.getPassiveAA(chr, STORM_OF_ARROWS_AA, (byte) slv);
                    aa.setPosition(position); aa.setRect(rect); aa.setDuration(2500); aa.setHitMob(true);
                    field.spawnAffectedArea(aa);
                    occupied.add(rect);
                }
            } else {
                for (int i = 0; i < 5; i++) {
                    Mob mob = Util.getRandomFromCollection(mobs);
                    AffectedArea aa = AffectedArea.getPassiveAA(chr, STORM_OF_ARROWS_AA, (byte) chr.getSkill(STORM_OF_ARROWS).getCurrentLevel());
                    aa.setPosition(new Position(mob.getX() + 250, mob.getY()));
                    aa.setDuration(2500);
                    int randomX = new Random().nextInt(200) - 100;
                    int randomY = new Random().nextInt(150) - 75;
                    aa.setPosition(new Position(aa.getX() + randomX, aa.getY() + randomY));
                    aa.setRect(aa.getPosition().getRectAround(SkillData.getSkillInfoById(STORM_OF_ARROWS_AA).getLastRect()));
                    field.spawnAffectedArea(aa);
                }
            }
            if (!Tms282RemoteBuffs.active()) stormArrowTimer = chr.getTimer().addEvent(this::stormOfArrowAA, 5, TimeUnit.SECONDS);
        } else if (!Tms282RemoteBuffs.active()) {
            stormArrowTimer.cancel(false);
            stormArrowTimer = null;
        }
    }

    public void startInhumanSpeed() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        int durationMs = 3000;
        long now = System.currentTimeMillis();

        Option o = new Option();
        o.nOption = 1;
        o.rOption = INHUMAN_SPEED;
        o.tOption = 30; // TSM thường dùng giây

        tsm.sendStat(TempSecondaryStat, o);

        inhumanSpeedPsdAttackCount = 0;
        inhumanSpeedExpireAt = now + durationMs;
        inhumanSpeedNextFireAt = now;         // cho phép bắn ngay tick đầu
        inhumanSpeedNextExtendAt = now;       // cho phép extend

        chr.getField().broadcast(UserPacket.inHumanSpeedResult(chr, durationMs)); // GỬI 1 LẦN
    }

    public void handleInhumanSpeedRequest(int mobId) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(TempSecondaryStat) && tsm.getOption(TempSecondaryStat).rOption == INHUMAN_SPEED) {
            long now = System.currentTimeMillis();
            if (tsm.getOption(TempSecondaryStat).startTime + 30000 <= now) {
                tsm.removeStat(TempSecondaryStat);
                chr.getField().broadcast(UserPacket.inHumanSpeedResult(chr, 0));
                return;
            }
            if (now >= inhumanSpeedExpireAt) {
                return;
            }
            final int intervalMs = 250;
            if (now < inhumanSpeedNextFireAt) {
                return;
            }
            inhumanSpeedNextFireAt = now + intervalMs;
        }
        Mob mob = (Mob) chr.getField().getLifeByObjectID(mobId);
        if (mob == null || mob.getHp() <= 0) {
            mob = chr.getField().getClosestMobInFront(chr);
            if (mob == null) return;
        }
        spawnInhumanSpeedForceAtom(mob);
    }

    private void handleInhumanSpeedNextRequest(long now) {
        // extend duration: "+3 sec every 1 sec" => chỉ extend tối đa 1 lần mỗi 1000ms
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int slv = chr.getSkillLevel(INHUMAN_SPEED);
        if (tsm.hasStat(TempSecondaryStat) && tsm.getOption(TempSecondaryStat).rOption == INHUMAN_SPEED) {
             if (now >= inhumanSpeedNextExtendAt){
                Option o = tsm.getOption(TempSecondaryStat);
                inhumanSpeedNextExtendAt = now + 1000;
                inhumanSpeedExpireAt += 3000;
                if (inhumanSpeedExpireAt <= o.startTime + 30_000L) {
                    o.xOption = (int) inhumanSpeedExpireAt;
                    tsm.updateStat(TempSecondaryStat, o);
                    var effect = Effect.skillUse(INHUMAN_SPEED_BUFF, chr.getLevel(), slv);
                    chr.write(UserPacket.effect(effect));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
                }
                chr.getField().broadcast(UserPacket.inHumanSpeedResult(chr, 3000));
            }
        } else {
            inhumanSpeedPsdAttackCount += 1;
            if (inhumanSpeedPsdAttackCount >= 10) {
                inhumanSpeedPsdAttackCount = 0;
                Option o = new Option();
                o.nOption = 1;
                o.rOption = INHUMAN_SPEED_BUFF;
                o.tOption = 1;
                tsm.sendStat(TempSecondaryStat, o);
                var effect = Effect.skillUse(INHUMAN_SPEED_BUFF, chr.getLevel(), slv);
                chr.write(UserPacket.effect(effect));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
                chr.getField().broadcast(UserPacket.inHumanSpeedResult(chr, 3000));
            }
        }
    }

    private void spawnInhumanSpeedForceAtom(Mob mob) {
        int randomfImpact = Util.getRandom(10, 20);
        int randomsImpact = Util.getRandom(15, 25);

        ForceAtomEnum fae = ForceAtomEnum.INHUMAN_SPEED;

        ForceAtomInfo fai = new ForceAtomInfo(
                chr.getNewForceAtomKey(),
                fae.getInc(),
                randomfImpact,
                randomsImpact,
                chr.isLeft() ? 270 : 90,
                0,
                Util.getCurrentTime(),
                0, 0,
                new Position());

        chr.createForceAtom(new ForceAtom(chr.getId(), fae, mob.getObjectId(), INHUMAN_SPEED, fai));
    }

    private void createQuiverBarrageBurstForceAtom(AttackInfo attackInfo, long now) {
        if (!chr.hasSkillOnCooldown(QUIVER_BARRAGE_ATOM)) {
            int firstImpact = new Random().nextInt(15) + 35;
            int secondImpact = new Random().nextInt(2) + 5;
            ForceAtomEnum fae = ForceAtomEnum.QUIVER_FULL_BURST;
            final var mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
            for (int i = 0; i < 6; i++) {
                try {
                    Mob mob = Util.getRandomFromCollection(mobs);
                    if (mob != null) {
                        ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), firstImpact, secondImpact,
                                chr.isLeft() ? 320 : 40, i * 50, Util.getCurrentTime(), 1, 0,
                                new Position(0, 0));
                        ForceAtom fa = new ForceAtom(chr.getId(), fae, mob.getObjectId(), QUIVER_BARRAGE_ATOM, fai);
                        fa.setForcedTargetPosition(mob.getPosition());
                        chr.createForceAtom(fa);
                    }
                } catch (Exception ignored) {}
            }
            chr.addSkillCooldown(QUIVER_BARRAGE_ATOM, 2000);
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == FINAL_ATTACK) {
            if (chr.hasSkill(ADVANCED_FINAL_ATTACK)) return ADVANCED_FINAL_ATTACK;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleSkill(c, inPacket, skillUseInfo);
        }
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        switch (skillID) {
            case QUIVER_CARTRIDGE:
                if (tsm.hasStat(QuiverCatridge)) {
                    o1 = tsm.getOption(QuiverCatridge);
                    o1.nOption = o1.nOption == 1 ? 2 : 1;
                } else {
                    o1.nOption = 1;
                }
                o1.rOption = skillID;
                tsm.sendStat(QuiverCatridge, o1);
                break;
            case PHOENIX:
            case HEXA_PHOENIX:
                Summon summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                Field field = chr.getField();
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setAssistType(AssistType.Attack);
                field.spawnSummon(summon);
                break;
            case BLINK_SHOT:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                field.spawnSummon(summon);
                chr.getField().broadcast(Summoned.assistAttackRequest(summon, 0));
                break;
            case BLINK_SHOT_TP:
                chr.addSkillCooldown(BLINK_SHOT_TP, 2000);
                break;
            case SPEED_MIRAGE:
            case HEXA_SPEED_MIRAGE:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(FlashMirage, o1);
                }
                this.speedMirageHitCounter = 0;
                this.speedMirageStrikeCounter = 0;
                this.speedMirageNextProcAt = 0;
                break;
            case SHARP_EYES:
                int cr = si.getValue(x, slv);
                int crDmg = si.getValue(y, slv);
                cr += SkillData.getSkillInfoById(SHARP_EYES_CR_H).getValue(x, chr.getSkillLevel(SHARP_EYES_CR_H));
                o1.nOption = (cr << 8) + crDmg;
                if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    o1.xOption = cr;
                    o1.yOption = crDmg;
                }
                o1.nValue = si.getValue(y, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                //mOption is for the hyper passive
                if (chr.hasSkill(SHARP_EYES_IED_H)) {
                    o1.mOption = si.getValue(ignoreMobpdpR, slv);
                }
                tsm.sendStat(SharpEyes, o1);
                break;
            case ENCHANTED_QUIVER:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ExtraQuiverCatridge, o1);
                break;
            case EPIC_ADVENTURE:
                o1.nValue = si.getValue(indieDamR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case CONCENTRATION:
                o1.nValue = si.getValue(indiePad, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o1);
                o2.nOption = 1;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Preparation, o2);
                tsm.sendStat(newStats);
                break;
            case STORM_OF_ARROWS:
                o1.nValue = si.getValue(indieDamR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                if (stormArrowTimer != null) {
                    stormArrowTimer.cancel(false);
                }
                if (Tms282RemoteBuffs.active()) {
                    stormArrowTimer = null;
                    stormArrowNextProcAt = 0;
                } else stormOfArrowAA();
                break;
            case INHUMAN_SPEED:
                startInhumanSpeed();
                break;
            case QUIVER_BARRAGE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(QuiverFullBurst, o1);
                break;
            case SILHOUETTE_MIRAGE:
            case HEXA_SILHOUETTE_MIRAGE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.xOption = 0; // clones
                tsm.sendStat(ShadowShield, o1);
                chr.write(WvsContext.updateSkillStackRequestResult(SILHOUETTE_MIRAGE, (byte) o1.xOption));
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
        }
    }

    public void increaseSilhouetteMirageClone() {
        var tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(ShadowShield)) {
            return;
        }
        var opt = tsm.getOption(ShadowShield);
        opt.xOption++;
        opt.xOption = Math.max(0, Math.min(opt.xOption, 2));
        tsm.sendStat(ShadowShield, opt);
        chr.write(WvsContext.updateSkillStackRequestResult(QUIVER_BARRAGE, (byte) opt.xOption));
    }

    private void decreaseSilhouetteMirageClone() {
        var tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(ShadowShield)) {
            return;
        }
        var opt = tsm.getOption(ShadowShield);
        opt.xOption -= 1;
        tsm.sendStat(ShadowShield, opt);
        chr.write(WvsContext.updateSkillStackRequestResult(QUIVER_BARRAGE, (byte) opt.xOption));
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        var tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(ShadowShield)) {
            var o = tsm.getOption(ShadowShield); // has Atleast 1 clone
            if (o.xOption > 0) {
                var skillID = o.rOption;
                var si = SkillData.getSkillInfoById(skillID);
                var slv = chr.getSkillLevel(skillID);
                var threshold = chr.getHPPerc(si.getValue(y, slv));
                decreaseSilhouetteMirageClone();
                if (hitInfo.hpDamage > chr.getHP() && chr.getHP() > 1) {
                    hitInfo.hpDamage = chr.getHP() - 1;
                } else if (hitInfo.hpDamage >= threshold) {
                    var protection = si.getValue(q, slv);
                    hitInfo.hpDamage -= (int) ((hitInfo.hpDamage * protection) / 100D);
                }
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleMobDebuffSkill(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(FOCUSED_FURY) && tsm.hasStat(BowMasterConcentration)) {
            tsm.removeStatsBySkill(FOCUSED_FURY);
            var effect = Effect.skillSpecial(FOCUSED_FURY);
            chr.write(UserPacket.effect(effect));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
            tsm.removeAllDebuffs();
        }
        super.handleMobDebuffSkill(chr);
    }

    public void handleSpeedMirageOnAttack(AttackInfo attackInfo, long now) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        // toggle must be ON
        if (!tsm.hasStat(FlashMirage)) {
            return;
        }

        SkillInfo si = SkillData.getSkillInfoById(SPEED_MIRAGE);
        if (!si.getSkillList1().contains(attackInfo.skillId) && !si.getSkillList2().contains(attackInfo.skillId)) {
            return;
        }

        if (attackInfo.mobCount <= 0) {
            return;
        }

        // add hits
        speedMirageHitCounter += 1;

        // convert hits -> strikes (7 hits = 1 strike)
        if (speedMirageHitCounter >= 7) {
            int addStrikes = speedMirageHitCounter / 7;
            speedMirageHitCounter %= 7;
            speedMirageStrikeCounter += addStrikes;
        }

        // check proc gate
        int needStrikes = chr.hasSkill(SPEED_MIRAGE_II) ? 7 : 12;

        Option o = tsm.getOption(FlashMirage);
        o.nOption = Math.min(needStrikes, speedMirageStrikeCounter + 1);
        o.rOption = chr.hasSkill(HEXA_SPEED_MIRAGE) ? HEXA_SPEED_MIRAGE : SPEED_MIRAGE;
        tsm.sendStat(FlashMirage, o);

        if (speedMirageStrikeCounter < needStrikes) {
            return;
        }

        if (now < speedMirageNextProcAt) {
            return;
        }

        // consume strikes (use -= needStrikes so overflow carries)
        speedMirageStrikeCounter -= needStrikes;
        speedMirageNextProcAt = now + 1000;

        int afterimages = chr.hasSkill(SPEED_MIRAGE_II) ? 4 : 3;

        // spawn / fire afterimages
        procSpeedMirageAfterimages(afterimages, now);
    }

    private void procSpeedMirageAfterimages(int count, long now) {
        List<SecondAtom> secondAtoms = new LinkedList<>();
        List<Triple<Position, Boolean, Integer>> speedMirages = new LinkedList<>();
        int skillID = chr.hasSkill(HEXA_SPEED_MIRAGE) ? HEXA_SPEED_MIRAGE: SPEED_MIRAGE;
        int atomSkillID = skillID == HEXA_SPEED_MIRAGE ? HEXA_SPEED_MIRAGE_ATOM : SPEED_MIRAGE_ATOM;
        SkillInfo si = SkillData.getSkillInfoById(atomSkillID);
        int w = Util.getRandom(150, 300);
        int edgeX = (int) (w * 0.85);
        int edgeY = (int) (w * 0.70);
        final var mobs = chr.getField().getMobsFiltered();
        if (mobs.isEmpty()) return;
        for (int i = 0; i < count; i++) {
            var sai = si.getSecondAtomInfos().getOrDefault(i, null);
            var mob = Util.getRandomFromCollection(mobs);
            if (sai == null || mob == null) continue;
            Position tp = mob.getPosition();
            int x, y, angle;
            boolean isLeft;
            switch (i) {
                case 0 -> { // LT
                    x = tp.getX() - edgeX;
                    y = tp.getY() - edgeY;
                    angle = 325;
                    isLeft = true;
                }
                case 1 -> { // RT
                    x = tp.getX() + edgeX;
                    y = tp.getY() - edgeY;
                    angle = 325;
                    isLeft = false;
                }
                case 2 -> { // LB
                    x = tp.getX() - edgeX;
                    y = tp.getY() + edgeY;
                    angle = 45;
                    isLeft = true;
                }
                default -> { // RB
                    x = tp.getX() + edgeX;
                    y = tp.getY() + edgeY;
                    angle = 45;
                    isLeft = false;
                }
            }
            x += Util.getRandom(-20, 20);
            y += Util.getRandom(-15, 15);
            Position pos = new Position(x, y);
            SecondAtom sa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, i,
                    si.getSkillId(), pos, now);
            speedMirages.add(new Triple<>(pos, isLeft, angle));
            secondAtoms.add(sa);
        }

        chr.createSecondAtom(secondAtoms);
        var eff = Effect.speedMirageEff(skillID, chr.getId(), speedMirages);
        chr.write(UserPacket.effect(eff));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), eff), chr);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_ENCHANCED_ARROW_STREAM -> {
                chr.addSkillCooldown(skillId, 6000);
                return 1;
            }
            case HEXA_GRITTY_GUST -> {
                int skillID = GRITTY_GUST;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
