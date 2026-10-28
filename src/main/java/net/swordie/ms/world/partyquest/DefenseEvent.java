package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.connection.packet.DefensePacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.UIType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class DefenseEvent implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1211;
    private int wave;
    private int life;
    private Field field;
    private Position position1;
    private Position position2;
    private Position position3;
    private Position position4;
    private boolean next;
    private boolean hard;
    private final int DUARTE = 2103013;
    private final int NETT_EMERALD = 4001623;
    private final long[] hp = new long[]{
            10000L, 20000L, 30000L, 40000L, 50000L, 60000L, 70000L, 80000L, 90000L, 100000L,
            120000L, 130000L, 140000L, 150000L, 160000L, 170000L, 180000L, 190000L, 2000000L, 230000L, 250000L, 300000L, 300000L};

    public DefenseEvent(Char chr) {
        this.sm = chr.getScriptManager();
        this.chr = chr;
        this.party = chr.getParty();
        init();
    }

    private void init() {
        this.wave = -1;
        this.life = 20;
        this.field = null;
        this.position1 = new Position(910, 140);
        this.position2 = new Position(910, -40);
        this.position3 = new Position(910, -220);
        this.position4 = new Position(910, -400);
        this.next = false;
    }

    private boolean isPartyEligible(short lowLevel, short highLevel, Party party) {
        for (PartyMember member : party.getMembers()) {
            if (member.getLevel() < lowLevel || member.getLevel() > highLevel) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
        sm.setSpeakerID(DUARTE);
        int selection = sm.sendNext("你！我叫杜亞特，是奈特金字塔的守衛。\r\n\r\n" +
                "#b" +
                "#L0#聽聽金字塔的事。#l\r\n" +
                "#L1##e進入金字塔。#n#l\r\n" +
                "#L2#尋找隊伍。#l\r\n" +
                "#L3#用<#z4001623#>交換其他道具。#l\r\n" +
                "#L4#查看今天剩餘的挑戰次數。#l" +
                "#k");
        switch (selection) {
            case 0:
                sm.sendNext("這裡是混沌與復仇之神奈特的金字塔。這個地方雖然長年深埋在黃沙之中，卻因奈特的意志而重現於世。若汝不畏懼未知的混沌與隨之而來的毀滅，便可挑戰奈特的試煉。命運永遠掌握在做出選擇的人手中……");
                sm.sendSay("汝一踏進金字塔，奈特的試煉便會開始。汝必須阻止源源不絕的怪物抵達#e#b方尖碑#k#n。用在金字塔中獲得的點數，就能購買#b眼睛道具#k。");
                sm.sendPrev("戰勝奈特試煉的人將獲得榮耀，屈服於試煉的人將會滅亡。我能告訴汝的就只有這些，剩下的就看汝自己的決心了。");
                break;
            case 1:
                if (sm.getFieldID() != GameConstants.NETT_PYRAMID_ENTRANCE_MAP) {
                    sm.sendNext("不知死亡殘酷的人啊，到我這裡來吧。");
                    sm.warp(GameConstants.NETT_PYRAMID_ENTRANCE_MAP);
                } else {
                    int mode = sm.sendNext("尚未見識過死亡殘酷的天真孩子，做出你的選擇吧！\r\n\r\n" +
                            "#L0##b簡單模式#k #r(Lv. 80 - 109)#k#l\r\n" +
                            "#L1##b困難模式#k #r(Lv. 80 - 109)#k#l");
                    if (mode == 0 || mode == 1) {
                        if (party == null) {
                            sm.sendSayOkay("必須加入#b隊伍#k才能進入奈特的金字塔。快去找朋友吧！");
                        } else if (!party.isLeader(chr)) {
                            sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                        } else if (sm.checkPartyForPQ()) {
                            if (isPartyEligible((short) 80, (short) 109, party)) {
                                if (sm.checkAttempt(entryQuest, party)) {
                                    sm.warpInstanceIn(chr, GameConstants.NETT_PYRAMID_MAIN_MAP, true);
                                    sm.setInstanceTime(GameConstants.NETT_PYRAMID_TIME, GameConstants.NETT_PYRAMID_MAIN_MAP, false);
                                    party.setPartyQuest(this);
                                    sm.addAttempt(entryQuest, party);
                                    setHard(mode == 1);
                                    return;
                                } else {
                                    sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                                }
                            } else {
                                sm.sendSayOkay("有隊員的等級低於 80 或高於 109。");
                            }
                        }
                    }
                }
                break;
            case 2:
                sm.openUI(UIType.UI_PARTY_INVITATION);
                break;
            case 3:
                int selection2 = sm.sendNext("若汝戰勝奈特的試煉並收集到#e#b奈特的祖母綠寶石#k#n，就能拿來交換道具。汝想要哪一個？\r\n\r\n" +
                        "#L0##v1132013# #b#z1132013##k #r(需要 #z4001623# x 40)#k#l\r\n" +
                        "#L1##v1072619# #b#z1072619##k #r(需要 #z4001623# x 40)#k#l\r\n" +
                        "#L2##v1112682# #b#z1112682##k #r(需要 #z4001623# x 40，期限 15 天)#k#l");
                if (selection2 == 0) {
                    if (sm.hasItem(NETT_EMERALD, 40) && sm.canHold(1132013)) {
                        sm.consumeItem(NETT_EMERALD, 40);
                        sm.giveItem(1132013);
                    } else {
                        sm.sendSayOkay("請確認有足夠的#r#z4001623# x40#k，且裝備欄有空格！");
                    }
                } else if (selection2 == 1) {
                    if (sm.hasItem(NETT_EMERALD, 40) && sm.canHold(1072619)) {
                        sm.consumeItem(NETT_EMERALD, 40);
                        sm.giveItem(1072619);
                    } else {
                        sm.sendSayOkay("請確認有足夠的#r#z4001623# x40#k，且裝備欄有空格！");
                    }
                } else if (selection2 == 2) {
                    if (sm.hasItem(NETT_EMERALD, 40) && sm.canHold(1112682)) {
                        sm.consumeItem(NETT_EMERALD, 40);
                        chr.addItemToInventory(1112682, 1, "day", 15);
                    } else {
                        sm.sendSayOkay("請確認有足夠的#r#z4001623# x40#k，且裝備欄有空格！");
                    }
                }
                break;
            case 4:
                int count = 5;
                if (chr.hasQuest(entryQuest)) {
                    count = 5 - Integer.parseInt(chr.getQRValueByKey(entryQuest, "count"));
                }
                sm.sendSayOkay("你還可以挑戰 " + count + " 次。");
                break;
        }
    }

    @Override
    public void exit() {
        sm.setSpeakerID(DUARTE);
        if (sm.sendAskYesNo("要離開嗎？")) {
            chr.setDefenseEvent(null);
            chr.setDefenseEventMember(null);
            chr.write(FieldPacket.closeUI(UIType.UI_FIELDITEM));
            chr.getScriptManager().warpInstanceOut(chr, GameConstants.NETT_PYRAMID_ENTRANCE_MAP);
        }
    }

    @Override
    public void end(Char chr) {
        if (chr.getParty() != null) {
            for (Char player : chr.getParty().getOnlineChars()) {
                player.setDefenseEvent(null);
                player.setDefenseEventMember(null);
                player.write(FieldPacket.closeUI(UIType.UI_FIELDITEM));
            }
        } else {
            chr.setDefenseEvent(null);
            chr.setDefenseEventMember(null);
            chr.write(FieldPacket.closeUI(UIType.UI_FIELDITEM));
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.warpInstanceOut(chr, GameConstants.NETT_PYRAMID_ENTRANCE_MAP);
    }

    @Override
    public void clear(Char chr) {

    }

    @Override
    public void event() {
        if (chr == null || chr.getParty() == null || !chr.getParty().isLeader(chr)) {
            return;
        }
        DefenseEvent mnp = DefenseEvent.getInfo(chr, isHard());
        if (mnp != null) {
            mnp.first();
        }
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }

    public static DefenseEvent getInfo(Char chr, boolean hard) {
        DefenseEvent ret = new DefenseEvent(chr);
        ret.setHard(hard);
        ret.setField(chr.getField());
        if (chr.getParty() != null) {
            for (Char player : chr.getParty().getOnlineChars()) {
                player.setDefenseEvent(ret);
                player.setDefenseEventMember(new DefenseEventMember(player));
            }
        } else {
            return null;
        }
        return ret;
    }

    public void first() {
        try {
            setting();
            startDefenseEvent();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public void setting() {
        if (field != null) {
            field.broadcast(DefensePacket.point(0));
            field.broadcast(FieldPacket.openUI(UIType.UI_FIELDITEM));
        }
        nextWave();
        changeLife();
    }

    public void startDefenseEvent() {
        chr.getTimer().addEvent(() -> {
            sm.showEffectToField("Map/Effect.img/defense/count");
        }, 5, TimeUnit.SECONDS);
        chr.getTimer().addEvent(() -> {
            startWave();
            setNext(false);
        }, 8, TimeUnit.SECONDS);
    }

    public void startWave() {
        nextWave();
        for (int i = 0; i < getMonsters().size(); i++) {
            int mobID = getMonsters().get(i);
            chr.getTimer().addEvent(() -> spawnMonsters(mobID), 1 + i, TimeUnit.SECONDS);
        }
    }

    public void nextWave() {
        setWave(getWave() + 1);
        changeWave();
    }

    public void changeWave() {
        if (field != null) {
            field.broadcast(DefensePacket.wave(getWave()));
            if (getWave() >= 1) {
                sm.showEffectToField("Map/Effect.img/defense/wave/" + getWave());
                sm.showEffectToField("Map/Effect.img/killing/first/start" + getWave());
                sm.playSound("event/start", true);
            }
        }
    }

    public void minusLife() {
        setLife(getLife() - 1);
        if (getLife() < 0) {
            setLife(0);
        }
        changeLife();
        if (getLife() == 0) {
            waveFail();
        }
    }

    public void waveFail() {
        field.killMobs();
        setNext(false);
        sm.showEffectToField("Map/Effect.img/killing/fail" + getWave());
        end(chr);
    }

    public void changeLife() {
        if (field != null) {
            field.broadcast(DefensePacket.life(getLife()));
        }
    }

    public void spawnMonsters(int mid) {
        if (field == null) {
            return;
        }
        if (field.getChars().size() > 0) {
            int level = chr.getParty().getAvgPartyLevel();
            long hp = getHp(mid, level);
            int exp = getExp(level);
            int statR = isHard() ? 100 : 50;
            field.spawnMob(mid, getPosition1().getX(), getPosition1().getY(), false, hp, statR, statR, statR, statR, exp);
            field.spawnMob(mid, getPosition3().getX(), getPosition3().getY(), false, hp, statR, statR, statR, statR, exp);
            if (getWave() != 20) {
                field.spawnMob(mid, getPosition2().getX(), getPosition2().getY(), false, hp, statR, statR, statR, statR, exp);
                field.spawnMob(mid, getPosition4().getX(), getPosition4().getY(), false, hp, statR, statR, statR, statR, exp);
            }
        }
    }

    public long getHp(int mid, int level) {
        int id = mid - 9305400;
        int plus = Math.max(1, level - 240);
        if (isHard()) {
            return hp[id] * 2 * plus * ((4 + getWave()) / 4);
        }
        return hp[id] * plus * ((4 + getWave()) / 4);
    }

    public int getExp(int level) {
        double exp = GameConstants.charExp[level] * 1.0E-4D * getWave() / 10000.0D;
        if (isHard()) {
            return (int) exp;
        }
        return (int) (exp * 0.01D);
    }

    public void check() {
        if (field.getMobs().size() == 0 && getWave() > 0 && getWave() < 21 && !isNext()) {
            waveClear();
            setNext(true);
            if (getWave() < 20) {
                startDefenseEvent();
            }
        }
    }

    public List<Integer> getMonsters() {
        List<Integer> monsters = new ArrayList<>();
        switch (getWave()) {
            case 1:
                for (int i = 0; i <= 31; i++) {
                    monsters.add(9305400);
                }
                break;
            case 2:
                for (int i = 0; i <= 39; i++) {
                    monsters.add(9305401);
                }
                break;
            case 3:
                for (int i = 0; i <= 59; i++) {
                    monsters.add(9305400);
                }
                break;
            case 4:
                for (int i = 0; i <= 19; i++) {
                    monsters.add(9305402);
                }
                break;
            case 5:
                for (int i = 0; i <= 19; i++) {
                    monsters.add(9305402);
                }
                for (int i = 20; i <= 39; i++) {
                    monsters.add(9305403);
                }
                break;
            case 6:
                for (int i = 0; i <= 39; i++) {
                    monsters.add(9305404);
                }
                for (int i = 40; i <= 59; i++) {
                    monsters.add(9305403);
                }
                break;
            case 7:
                for (int i = 0; i <= 59; i++) {
                    monsters.add(9305404);
                }
                break;
            case 8:
                for (int i = 0; i <= 10; i++) {
                    monsters.add(9305406);
                }
                for (int i = 11; i <= 59; i++) {
                    monsters.add(9305403);
                }
                break;
            case 9:
                for (int i = 0; i <= 10; i++) {
                    monsters.add(9305407);
                }
                for (int i = 11; i <= 59; i++) {
                    monsters.add(9305403);
                }
                break;
            case 10:
                for (int i = 0; i <= 7; i++) {
                    monsters.add(9305408);
                }
                break;
            case 11:
                for (int i = 0; i <= 47; i++) {
                    monsters.add(9305409);
                }
                break;
            case 12:
                for (int i = 0; i <= 51; i++) {
                    monsters.add(9305410);
                }
                break;
            case 13:
                for (int i = 0; i <= 10; i++) {
                    monsters.add(9305411);
                }
                for (int i = 11; i <= 59; i++) {
                    monsters.add(9305409);
                }
                break;
            case 14:
                for (int i = 0; i <= 10; i++) {
                    monsters.add(9305412);
                }
                for (int i = 11; i <= 59; i++) {
                    monsters.add(9305410);
                }
                break;
            case 15:
                for (int i = 0; i <= 10; i++) {
                    monsters.add(9305412);
                }
                for (int i = 20; i <= 31; i++) {
                    monsters.add(9305413);
                }
                break;
            case 16:
                for (int i = 0; i <= 29; i++) {
                    monsters.add(9305414);
                }
                for (int i = 30; i <= 59; i++) {
                    monsters.add(9305415);
                }
                break;
            case 17:
                for (int i = 0; i <= 39; i++) {
                    monsters.add(9305416);
                }
                for (int i = 40; i <= 79; i++) {
                    monsters.add(9305417);
                }
                break;
            case 18:
                for (int i = 0; i <= 39; i++) {
                    monsters.add(9305416);
                }
                for (int i = 40; i <= 71; i++) {
                    monsters.add(9305418);
                }
                break;
            case 19:
                for (int i = 0; i <= 20; i++) {
                    monsters.add(9305419);
                }
                for (int i = 21; i <= 39; i++) {
                    monsters.add(9305420);
                }
                for (int i = 41; i <= 59; i++) {
                    monsters.add(9305421);
                }
                break;
            case 20:
                for (int i = 0; i <= 9; i++) {
                    monsters.add(9305419);
                }
                for (int i = 0; i <= 1; i++) {
                    monsters.add(9305422);
                }
                break;
        }
        return monsters;
    }

    public void waveClear() {
        for (Char player : chr.getParty().getOnlineChars()) {
            if (getWave() == 20) {
                player.write(FieldPacket.fieldEffect(FieldEffect.getFieldEffectFromWz("Map/Effect.img/killing/clear", 0)));
                chr.getTimer().addEvent(() -> {
                    player.write(DefensePacket.result(true,
                            getWave(), getLife(), player.getDefenseEventMember().getPoint(), player.getDefenseEventMember().getExp()));
                    chr.getTimer().addEvent(() -> {
                        endWave();
                        player.warp(isHard() ? 926010002 : 926010003);
                    }, 7000);
                }, 2000);
            }
            if (player.canHold(NETT_EMERALD)) {
                player.addItemToInventory(NETT_EMERALD, 1);
            } else {
                player.chatMessage("其他欄空格不足，無法領取獎勵奈特的祖母綠寶石 x1。");
            }
        }
    }

    public void plusPoint(Char chr, int point) {
        chr.getDefenseEventMember().plusPoint(point);
    }

    public void minusPoint(Char chr, int point) {
        chr.getDefenseEventMember().minusPoint(point);
    }

    public int getPoint(Char chr) {
        return chr.getDefenseEventMember().getPoint();
    }

    public void plusExp(Char chr, int exp) {
        chr.getDefenseEventMember().plusExp(exp);
    }

    public void endWave() {
        for (Char pm : chr.getParty().getOnlineChars()) {
            pm.setDefenseEvent(null);
            pm.setDefenseEventMember(null);
            pm.write(FieldPacket.closeUI(UIType.UI_FIELDITEM));
        }
    }

    public void useSkill(Char chr, int sid) {
        SkillInfo si;
        Option o = new Option();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        ItemInfo ii = ItemData.getItemInfoByID(sid);
        if (ii == null) {
            chr.dispose();
            return;
        }
        int pointCost = ii.getPointCost();
        switch (sid) {
            case 2800014 -> {
                if (getPoint(chr) >= pointCost) {
                    int ANUBIS_EYES = 80001104;
                    si = SkillData.getSkillInfoById(ANUBIS_EYES);
                    o.nValue = si.getValue(SkillStat.damage, 1);
                    o.nReason = ANUBIS_EYES;
                    o.tTerm = si.getValue(SkillStat.time, 1);
                    EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                    newStats.put(CharacterTemporaryStat.IndiePAD, o);
                    newStats.put(CharacterTemporaryStat.IndieMAD, o.deepCopy());
                    tsm.sendStat(newStats);
                } else {
                    pointCost = -1;
                }
            }
            case 2800015 -> {
                if (getPoint(chr) >= pointCost) {
                    for (Mob mob : field.getMobs()) {
                        if (mob == null) {
                            continue;
                        }
                        int HORUS_EYES = 80001105;
                        si = SkillData.getSkillInfoById(HORUS_EYES);
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        o.nOption = si.getValue(SkillStat.x, 1);
                        o.rOption = HORUS_EYES;
                        o.tOption = si.getValue(SkillStat.time, 1);
                        mts.addStatOptions(mob, MobStat.Speed, o);
                    }
                } else {
                    pointCost = -1;
                }
            }
            case 2800016 -> {
                if (getPoint(chr) >= pointCost) {
                    int IRIS_EYES = 80001106;
                    si = SkillData.getSkillInfoById(IRIS_EYES);
                    int dotTime = si.getValue(SkillStat.dotTime, 1);
                    BurnedInfo bi = BurnedInfo.createBurnInfo(chr, IRIS_EYES, 1, 1);
                    for (Mob mob : field.getMobs()) {
                        if (mob == null) {
                            continue;
                        }
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        mts.createAndAddBurnedInfo(mob, bi, IRIS_EYES);
                    }
                } else {
                    pointCost = -1;
                }
            }
            case 2800017 -> {
                if (getPoint(chr) >= pointCost) {
                    for (Mob mob : field.getMobs()) {
                        if (mob == null) {
                            continue;
                        }
                        int OSIRIS_EYES = 80001107;
                        si = SkillData.getSkillInfoById(OSIRIS_EYES);
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        o.nOption = 1;
                        o.rOption = OSIRIS_EYES;
                        o.tOption = si.getValue(SkillStat.time, 1);
                        mts.addStatOptions(mob, MobStat.Stun, o);
                    }
                } else {
                    pointCost = -1;
                }
            }
            default -> {
                if (getPoint(chr) < pointCost) {
                    pointCost = -1;
                }
            }
        }
        if (pointCost == -1) {
            chr.chatMessage("點數不足。");
        } else if (pointCost == 0) {
            chr.chatMessage("無法使用這個技能。");
        } else {
            minusPoint(chr, pointCost);
        }
    }

    public int getWave() {
        return wave;
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public int getLife() {
        return life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public boolean isNext() {
        return next;
    }

    public void setNext(boolean next) {
        this.next = next;
    }

    public boolean isHard() {
        return hard;
    }

    public void setHard(boolean hard) {
        this.hard = hard;
    }

    public Position getPosition1() {
        return position1;
    }

    public Position getPosition2() {
        return position2;
    }

    public Position getPosition3() {
        return position3;
    }

    public Position getPosition4() {
        return position4;
    }
}