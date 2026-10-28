package net.swordie.ms.life.mob.boss.demian.stigma;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.packet.DemianFieldPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.BossConstants;
import net.swordie.ms.enums.DemainStigmaType;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.boss.demian.sword.DemianFlyingSword;
import net.swordie.ms.life.mob.boss.demian.sword.DemianFlyingSwordType;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.Stigma;

public class DemianStigma {

    public static boolean checkStigma(Char chr) {
        Mob mob = chr.getField().getMobs().stream()
                .filter(m -> m.getTemplateId() == BossConstants.DEMIAN_NORMAL_PHASE_1_TEMPLATE_ID
                        || m.getTemplateId() == BossConstants.DEMIAN_NORMAL_PHASE_2_TEMPLATE_ID
                        || m.getTemplateId() == BossConstants.DEMIAN_HARD_PHASE_1_TEMPLATE_ID
                        || m.getTemplateId() == BossConstants.DEMIAN_HARD_PHASE_2_TEMPLATE_ID
                ).findAny().orElse(null);
        if (mob != null) {
            Instance instance = chr.getParty().getInstance();
            if (instance.getStigmaChar() != null) {
                if (!instance.getStigmaChar().equals(chr)) {
                    return false;
                }
            }
            HashMap<Char, Integer> stigmas = new HashMap<>();
            for (Char pm : chr.getParty().getPartyMembersInSameFieldWithChr(chr)) {
                stigmas.put(pm, getStigmaCount(pm));
            }
            List<DemainStigmaType> randomStigmaTypes = new ArrayList<>();
            if (mob.getMostDamageChar() != null && mob.getMostDamageChar().equals(chr)) {
                randomStigmaTypes.add(DemainStigmaType.MostThreatingOpponent);
            }
            Char mostStigmaChar = getMostStigmaChar(stigmas);
            if (mostStigmaChar != null && mostStigmaChar.equals(chr)) {
                randomStigmaTypes.add(DemainStigmaType.MostBrandedOpponent);
            }
            Char leastStigmaChar = getLeastStigmaChar(stigmas);
            if (leastStigmaChar != null && leastStigmaChar.equals(chr)) {
                randomStigmaTypes.add(DemainStigmaType.LeastBrandedOpponent);
            }
            Char randomStigmaChar = Util.getRandomFromCollection(stigmas.keySet());
            if (randomStigmaChar != null && randomStigmaChar.equals(chr)) {
                randomStigmaTypes.add(DemainStigmaType.RandomOpponentOpponent);
            }
            DemainStigmaType randType = Util.getRandomFromCollection(randomStigmaTypes);
            if (randType == null) {
                return false;
            }
            switch (randType) {
                case MostThreatingOpponent -> {
                    instance.setStigmaChar(chr);
                    mob.setControllerID(chr.getId());
                    mob.setTargetFromSvr(chr.getId());
                    mob.getField().broadcast(MobPool.nextTargetFromSvr(mob, chr.getId()));
                    mob.notifyControllerChange();
                    for (Life life : chr.getField().getLifes().values()) {
                        if (life instanceof DemianFlyingSword demianFlyingSword) {
                            demianFlyingSword.setTargetChrId(chr.getId());
                            demianFlyingSword.target();
                        }
                    }
                    chr.getField().broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossDamien,
                            "戴米安正以威脅最大的對手作為烙印的目標。",
                            BossConstants.DEMIAN_PASSIVE_STIGMA_TIME));
                    return true;
                }
                case MostBrandedOpponent -> {
                    instance.setStigmaChar(chr);
                    mob.setControllerID(chr.getId());
                    mob.setTargetFromSvr(chr.getId());
                    mob.getField().broadcast(MobPool.nextTargetFromSvr(mob, chr.getId()));
                    mob.notifyControllerChange();
                    for (Life life : chr.getField().getLifes().values()) {
                        if (life instanceof DemianFlyingSword demianFlyingSword) {
                            demianFlyingSword.setTargetChrId(chr.getId());
                            demianFlyingSword.target();
                        }
                    }
                    chr.getField().broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossDamien,
                            "戴米安正以烙印最多的對手作為烙印的目標。",
                            BossConstants.DEMIAN_PASSIVE_STIGMA_TIME));
                    return true;
                }
                case LeastBrandedOpponent -> {
                    instance.setStigmaChar(chr);
                    mob.setControllerID(chr.getId());
                    mob.setTargetFromSvr(chr.getId());
                    mob.getField().broadcast(MobPool.nextTargetFromSvr(mob, chr.getId()));
                    mob.notifyControllerChange();
                    for (Life life : chr.getField().getLifes().values()) {
                        if (life instanceof DemianFlyingSword demianFlyingSword) {
                            demianFlyingSword.setTargetChrId(chr.getId());
                            demianFlyingSword.target();
                        }
                    }
                    chr.getField().broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossDamien,
                            "戴米安正以烙印最少的對手作為烙印的目標。",
                            BossConstants.DEMIAN_PASSIVE_STIGMA_TIME));
                    return true;
                }
                case RandomOpponentOpponent -> {
                    instance.setStigmaChar(chr);
                    mob.setControllerID(chr.getId());
                    mob.setTargetFromSvr(chr.getId());
                    mob.getField().broadcast(MobPool.nextTargetFromSvr(mob, chr.getId()));
                    mob.notifyControllerChange();
                    for (Life life : chr.getField().getLifes().values()) {
                        if (life instanceof DemianFlyingSword demianFlyingSword) {
                            demianFlyingSword.setTargetChrId(chr.getId());
                            demianFlyingSword.target();
                        }
                    }
                    chr.getField().broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossDamien,
                            "戴米安正隨機選擇對手作為烙印的目標。",
                            BossConstants.DEMIAN_PASSIVE_STIGMA_TIME));
                    return true;
                }
            }
        }
        return false;
    }

    public static void resetStigma(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.removeStat(Stigma);
        chr.getField().broadcast(FieldPacket.playSound("SoundEff/BossDemian/decStigma"));
        chr.getField().broadcast(DemianFieldPacket.stigmaEffect(chr.getId(), false));
    }

    public static void incStigma(Char chr) {
        incStigma(chr, 1);
        chr.getField().broadcast(FieldPacket.playSound("SoundEff/BossDemian/incStigma"));
        chr.getField().broadcast(DemianFieldPacket.stigmaEffect(chr.getId(), true));
        chr.getField().broadcast(DemianFieldPacket.corruptionChange(false, chr.getParty().getInstance().getCorruptionCount()));
    }

    public static void incStigma(Char chr, int amount) {
        changeStigma(chr, getStigmaCount(chr) + amount);
    }

    public static void incCorruption(Char chr) {
        Instance instance = chr.getParty().getInstance();
        changeCorruption(chr, instance.getCorruptionCount() + 1);
    }

    public static void changeCorruption(Char chr, int newCorruption) {
        int maxCorruption = BossConstants.DEMIAN_MAX_CORRUPTION;
        Instance instance = chr.getParty().getInstance();
        instance.setCorruptionCount(newCorruption > maxCorruption ? maxCorruption : Math.max(newCorruption, 0));
        for (Char pm : chr.getParty().getPartyMembersInSameFieldWithChr(chr)) {
            TemporaryStatManager tsm = pm.getTemporaryStatManager();
            Option o = new Option();
            o.bOption = BossConstants.DEMIAN_MAX_STIGMA;
            o.nOption = getStigmaCount(pm);
            o.rOption = MobSkillID.Stigma.getVal();
            o.xOption = instance.getCorruptionCount();
            tsm.sendStat(Stigma, o);
            pm.getField().broadcast(DemianFieldPacket.corruptionChange(false, o.xOption));
        }
    }

    public static void changeStigma(Char chr, int newStigma) {
        //int curStigma = getStigmaCount(chr);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Instance instance = chr.getParty().getInstance();
        Option o = new Option();
        o.bOption = BossConstants.DEMIAN_MAX_STIGMA;
        o.nOption = newStigma > o.bOption ? o.bOption : Math.max(newStigma, 0);
        o.rOption = MobSkillID.Stigma.getVal();
        o.xOption = instance.getCorruptionCount();
        tsm.sendStat(Stigma, o);

        // if curStigma reaches maxStigma
        if (o.nOption >= BossConstants.DEMIAN_MAX_STIGMA) {
            incCorruption(chr); // increase Corruption by 1
            chr.damage(chr.getMaxHP()); // kill
            resetStigma(chr);
            if (instance.getCorruptionCount() == BossConstants.DEMIAN_MAX_CORRUPTION) {
                startNextPhase(chr, true);
            }

            // spawn extra sword
            Mob mob = chr.getField().getMobs().stream().findFirst().orElse(null);
            if (mob != null) {
                DemianFlyingSword sword = DemianFlyingSword.createDemianFlyingSword(chr, mob);
                sword.setSwordType(DemianFlyingSwordType.SecondSword);
                chr.getField().spawnLife(sword, null);
                sword.startPath();
                sword.target();
            }
        }
    }

    public static int getStigmaCount(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(Stigma)) {
            return tsm.getOption(Stigma).nOption;
        }
        return 0;
    }

    public static Char getMostStigmaChar(Map<Char, Integer> stigmas) {
        Tuple<Char, Integer> max = new Tuple<>(null, 0);
        for (Map.Entry<Char, Integer> entry : stigmas.entrySet()) {
            Char chr = entry.getKey();
            int stigma = entry.getValue();
            if (max == null || stigma > max.getRight()) {
                max.setLeft(chr);
                max.setRight(stigma);
            }
        }
        return max.getLeft();
    }

    public static Char getLeastStigmaChar(Map<Char, Integer> stigmas) {
        Tuple<Char, Integer> max = new Tuple<>(null, 0);
        for (Map.Entry<Char, Integer> entry : stigmas.entrySet()) {
            Char chr = entry.getKey();
            int stigma = entry.getValue();
            if (max == null || stigma < max.getRight()) {
                max.setLeft(chr);
                max.setRight(stigma);
            }
        }
        return max.getLeft();
    }

    public static void startNextPhase(Char chr, boolean isCrystalReached) {
        Party party = chr.getParty();
        Field field = chr.getField();
        Instance instance = party.getInstance();
        if (!instance.hasProperty("fullCorruption")) {
            instance.addProperty("fullCorruption", true);
            if (isCrystalReached) {
                long leftOverHP = 0;
                for (Mob mob : field.getMobs()) {
                    if (mob.getTemplateId() == BossConstants.DEMIAN_NORMAL_PHASE_1_TEMPLATE_ID || mob.getTemplateId() == BossConstants.DEMIAN_HARD_PHASE_1_TEMPLATE_ID) {
                        leftOverHP = mob.getHp();
                        break;
                    }
                }
                if (leftOverHP > 0) {
                    instance.addProperty("crystalReached", leftOverHP);
                }
                field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossDamien,
                        "烙印完成了，戴米安湧現出黑暗的力量。", BossConstants.DEMIAN_PASSIVE_STIGMA_TIME));
            } else {
                changeCorruption(chr, BossConstants.DEMIAN_MAX_CORRUPTION);
                field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.BossDamien,
                        "戴米安得到了完整的黑暗力量。", BossConstants.DEMIAN_PASSIVE_STIGMA_TIME));
            }
            chr.getTimer().addEvent(() -> {
                chr.getScriptManager().warpParty(chr.getField().getId() + 40, party);
            }, 10, TimeUnit.SECONDS);
        }
    }
}
