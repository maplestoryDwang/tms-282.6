package net.swordie.ms.client.character.skills.temp;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.connection.OutPacket;

import java.util.EnumMap;
import java.util.List;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class RemoteSecondaryStat {

    RemoteSecondaryStat() {}

    @FunctionalInterface
    interface Encoder {
        void encode(OutPacket out, Option option);
    }

    record Entry(CharacterTemporaryStat cts, Encoder enc) {

    }

    static final Entry[] entries = {
            new Entry(Speed, (outPacket, o) -> outPacket.encodeByte(o.nOption)),
            new Entry(ComboCounter, (outPacket, o) -> outPacket.encodeByte(o.nOption)),
            new Entry(BlessedHammer, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(WeaponCharge, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ElementalCharge, (outPacket, o) -> outPacket.encodeShort(o.nOption)),
            new Entry(Stun, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(GravityConstraint, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Shock, (outPacket, o) -> outPacket.encodeByte(o.nOption)),
            new Entry(Darkness, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Seal, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Weakness, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(WeaknessMdamage, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Curse, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Slow, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(PvPRaceEffect, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(TimeBomb, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Team, (outPacket, o) -> outPacket.encodeByte(o.nOption)),
            new Entry(DisOrder, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Thread, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Poison, (outPacket, o) -> {
                outPacket.encodeShort(o.xOption);
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ShadowPartner, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DarkSight, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(SoulArrow, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(Morph, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Ghost, (outPacket, o) -> outPacket.encodeShort(o.nOption)),
            new Entry(Attract, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Magnet, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MagnetArea, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NoBulletConsume, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(BanMap, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Barrier, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DojangShield, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ReverseInput, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RespectPImmune, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(RespectMImmune, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(DefenseAtt, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(DefenseState, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(DojangBerserk, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DojangInvincible, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(RepeatEffect, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RepeatEffect2, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(StopPortion, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(StopMotion, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Fear, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MagicShield, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(Flying, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(Frozen, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Frozen2, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Web, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DrawBack, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FinalCut, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Sneak, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(BeastForm, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(Mechanic, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BlessingArmorIncPAD, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(Inflation, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Explosion, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DarkTornado, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AmplifyDamage, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HideAttack, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HideAttack, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HolyMagicShell, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(DevilishPower, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SpiritLink, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Event, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Event2, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DeathMark, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(PainMark, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Lapidification, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(VampDeath, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(VampDeathSummon, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(VenomSnake, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(PyramidEffect, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(PinkbeanRollingGrade, (outPacket, o) -> outPacket.encodeByte(o.nOption)),
            new Entry(IgnoreTargetDEF, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(StrikerElectricUsed, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Invisible, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Judgement, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(KeyDownAreaMoving, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(StackBuff, (outPacket, o) -> outPacket.encodeShort(o.nOption)),
            new Entry(Larkness, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ReshuffleSwitch, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SpecialAction, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(StopForceAtomInfo, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SoulGazeCriDamR, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(PowerTransferGauge, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FifthAdvWarriorShield, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AffinitySlug, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SoulExalt, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HiddenPieceOn, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SmashStack, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MobZoneState, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(GiveMeHeal, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(TouchMe, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Contagion, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
                outPacket.encodeInt(o.xOption);
            }),
            new Entry(IgnoreAllCounter, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(IgnorePImmune, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(IgnoreAllImmune, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(IgnoreAllAbout, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FinalJudgement, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FireAura, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(VengeanceOfAngel, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(HeavensDoor, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DamAbsorbShield, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AntiMagicShell, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NotDamaged, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BleedingToxin, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(WindBreakerFinal, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(KarmaBlade, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(IgnoreMobDamR, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Asura, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MegaSmasher, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
                outPacket.encodeInt(o.tOption);
            }),
            new Entry(UnityOfPower, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Stimulate, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ReturnTeleport, (outPacket, o) -> {
                outPacket.encodeByte(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(CapDebuff, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(OverloadCount, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FireBomb, (outPacket, o) -> {
                outPacket.encodeByte(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SurplusSupply, (outPacket, o) -> outPacket.encodeByte(o.nOption)),
            new Entry(NewFlying, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NaviFlying, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AmaranthGenerator, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(CygnusElementSkill, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(StrikerHyperElectric, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EventPointAbsorb, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EventAssemble, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Translucence, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(PoseType, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(CosmicForge, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ElementSoul, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(GlimmeringTime, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Reincarnation, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Beholder, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(QuiverCatridge, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(UserControlMob, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(ImmuneBarrier, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.tOption);
            }),
            new Entry(FullSoulMP, (outPacket, o) -> {
                outPacket.encodeInt(o.rOption);
                outPacket.encodeInt(o.xOption);
            }),
            new Entry(AntiMagicShell, (outPacket, o) -> {
                outPacket.encodeByte(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Dance, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SpiritGuard, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
                outPacket.encodeInt(o.tOption);
            }),
            new Entry(EunwolUnleashFoxOrb, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MastemaGuard, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ComboTempest, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HalfstatByDebuff, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ComplusionSlant, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(JaguarSummoned, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BombTime, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(TransformOverMan, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EnergyBust, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(LightningUnion, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BulletParty, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(LoadedDice, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BishopPray, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DarkLighting, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AttackCountX, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FireBarrier, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(KeyDownMoving, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MichaelSoulLink, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MichaelSoulLink, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(KinesisPsychicEnergeShield, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BladeStance, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
                outPacket.encodeInt(o.xOption);
            }),
            new Entry(Fever, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AdrenalinBoost, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(RWBarrier, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(RWVulkanPunch, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(RWMagnumBlow, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(SerpentScrew, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(BossAggro, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Cosmos, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(GuidedArrow, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(StraightForceAtomTargets, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(LefBuffMastery, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(TempSecondaryStat, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(CoalitionSupportSoldierStorm, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Stigma, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(PairingUser, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ConstelEagle, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(ConstelHornedWhelk, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ShineMageAppear, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Michael_RhoAias, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Kinesis_DustTornado, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(Wizard_OverloadMana, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(CursorSniping, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(OutSide, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FifthSpotLight, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FreudBlessing, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BlessedHammerActive, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ConvertAD, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EtherealForm, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ReadyToDie, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Oblivion, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Cr2CriDamR, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BlackMageCreate, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BlackMageDestroy, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BlackMageMonochrome, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HitStackDamR, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(LefGloryWing, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BuffControlDebuff, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
                outPacket.encodeInt(o.tOption);
            }),
            new Entry(DispersionDamage, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HarmonyLink, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(LefFastCharge, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SpecterMode, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ComingDeath, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(GreatOldAbyss, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SixthPhoenix, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BossWill_Infection, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DispersionDamage, (outPacket, o) -> outPacket.encodeInt(o.tOption)),
            new Entry(MichaelSwordOfLight, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(GrandCross, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SiphonVitalityBarrier, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BattlePvP_Wongki_FlyingCharge, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BattlePvP_Wongki_AwesomeFairy, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BattlePvP_Mugong_PandaZone, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BattleSurvivalDefence, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BattleSurvivalInvincible, (outPacket, o) -> {
                // intentionally empty
            }),
            new Entry(EventPvPDefence, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EventPvPInvincible, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EventSoccerBall, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EventSoccerMomentBuff, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(PinkbeanMatryoshka, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MinigameStat, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AnimaThiefFifthCloneAttack, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BlackMageWeaponDestruction, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BlackMageWeaponCreation, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DestinyWeaponIndomitable, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DestinyWeaponDecisive, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(LibraryMissionGuard, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(XenonHoloGramGraffiti, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(QuiverFullBurst, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(LefWarriorNobility, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RunePurification, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RuneContagion, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DebuffHallucination, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BMageAuraYellow, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BMageAuraDrain, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BMageAuraBlue, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BMageAuraDark, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BMageAuraDebuff, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BMageAuraUnion, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(IceAura, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(KnightsAura, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ZeroAuraStr, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NovaArcherIncanation, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AranComboTempestAura, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(XenonBursterLaser, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DarknessAura, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ShadowShield, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EquinoxActive, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NAThanatosDescent, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NAAnnihilation, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ATScrollPassive, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(YetiFuryMode, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AMAbsorptionRiver, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AMAbsorptionWind, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AMAbsorptionSun, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(UnwearyingRun, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(IceAuraZone, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FlashMirage, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HolyBlood, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Infinity, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(TeleportMasteryOn, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ChillingStep, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BlessingArmor, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(LimitBreakFinalAttack, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(TranscendentLight, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ArtificialEvolution, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Frenzy, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(LefMageCrystalGate, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(KinesisPsychicPoint, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Confinement, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(FixedSpeedAndJump, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(GrabAndThrow, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(DarkCloud, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(GrandFinale, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(UserAroundAttackDebuff, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(UserTrackingAreaWarning, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(KaringDoolAdvantage, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RPEventStat, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NaturesBelief, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AdrenalinMaximum, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AnimaThiefFlameStrike, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(EunwolFoxSpirit, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(DslayerMetamorphosis, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Anemoi, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Exceed, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RobotTransitionRoboLuncherRM7, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RobotTransitionMagneticField, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RobotTransitionRoboFactory, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Sublimation, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RenPlumSwordEx, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(GiantBossDeathCnt, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ShamanMode, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Chachacha, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BladeStanceMode, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
                outPacket.encodeInt(o.xOption);
            }),
            new Entry(BladeStanceBooster, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BladeStancePower, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SelfHyperBodyIncPAD, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SelfHyperBodyMaxHP, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SelfHyperBodyMaxMP, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(CriticalBuffAdd, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(BossDamageRate, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Stance, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SkillDeployment, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NewPirateUnityOfPower, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(AntiEvilShield, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(KenjiCounter, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(Wet, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(GrabbedByMob, (outPacket, o) -> {
                outPacket.encodeInt(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(WaterSmashTeam, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(WaterSmashClass, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(WaterSmashBuffCount, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SpecialTombPL, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(ReduceMP, (outPacket, o) -> outPacket.encodeInt(o.nOption)),
            new Entry(SplitArrow, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(CoronaBuffOverlap, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MitsuhideDebuff, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MitsuhideStigma, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ShamanIgnoreTargetDEF, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(HyperUpgradeDiscountR, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NeoTokyoBossThesis, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NeoTokyoBossAntiThesis, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NeoTokyoBossBomb, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(NeoTokyoBossPowOfLife, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SixthHakuman, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(SixthShinBatto, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(ConstelEagle, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(MukHyun_HO_SIN_GANG_GI, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
            new Entry(RideOrDieIncDropRate, (outPacket, o) -> {
                outPacket.encodeShort(o.nOption);
                outPacket.encodeInt(o.rOption);
            }),
    };

    public static void encode(OutPacket outPacket, EnumMap<CharacterTemporaryStat, List<Option>> newStats) {
        for (var e : entries) {
            List<Option> options = newStats.get(e.cts);
            if (options == null || options.isEmpty()) {
                continue;
            }
            e.enc.encode(outPacket, options.getFirst());
        }
    }
}
