package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.connection.packet.HungryMutoPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.world.boss.BossHelper.setting;
import static net.swordie.ms.world.boss.BossHelper.xy;

public class HungryMuto implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 34222;
    public final int[][] recipes = {
            {4034959, 0, 1, 130000, 45000},
            {4034960, 1, 1, 130000, 45000},
            {4034961, 2, 1, 130000, 45000},
            {4034962, 3, 1, 130000, 45000},
            {4034963, 4, 1, 130000, 45000},
            {4034964, 5, 1, 130000, 45000},
            {4034965, 6, 2, 130000, 45000},
            {4034966, 7, 1, 130000, 45000},
            {4034967, 8, 1, 130000, 45000},
            {4034968, 9, 1, 130000, 45000},
            {4034969, 10, 1, 130000, 45000},
            {4034970, 11, 1, 130000, 45000},
            {4034971, 12, 1, 130000, 45000},
            {4034972, 13, 1, 130000, 45000},
            {4034973, 14, 1, 130000, 45000},
            {4034974, 15, 1, 130000, 45000}};
    public final Integer[][][] recipeItems = {
            {{2435856, 5}, {2435860, 10}},
            {{2435858, 5}, {2435862, 10}},
            {{2435856, 5}, {2435860, 5}, {2435864, 10}},
            {{2435858, 5}, {2435862, 5}, {2435866, 10}},
            {{2435860, 5}, {2435864, 5}, {2435868, 10}},
            {{2435864, 5}, {2435866, 5}, {2435870, 10}},
            {{2435872, 1}, {2435858, 5}, {2435862, 5}, {2435868, 10}},
            {{2435872, 5}, {2435860, 5}, {2435866, 5}, {2435870, 10}},
            {{2435857, 5}, {2435861, 10}},
            {{2435859, 5}, {2435863, 10}},
            {{2435857, 5}, {2435861, 5}, {2435865, 10}},
            {{2435859, 5}, {2435863, 5}, {2435867, 10}},
            {{2435861, 5}, {2435865, 5}, {2435869, 10}},
            {{2435865, 5}, {2435867, 5}, {2435871, 10}},
            {{2435872, 1}, {2435859, 5}, {2435863, 5}, {2435869, 10}},
            {{2435872, 5}, {2435861, 5}, {2435867, 5}, {2435871, 10}}};

    private boolean enhance = false;
    private int score = 0;
    private int mutoRecipe = 0;
    private boolean bonus = false;
    private boolean complete = false;
    private long finishTime = 0L;
    private long finishBonusTime = 0L;

    public HungryMuto(Char chr) {
        this.sm = chr.getScriptManager();
        this.chr = chr;
        this.party = chr.getParty();
    }

    private boolean isPartyEligible(short lowLevel, short highLevel, Party party) {
        for (PartyMember member : party.getMembers()) {
            if (member.getLevel() < lowLevel || member.getLevel() > highLevel || !member.getChr().hasQuestCompleted(34218)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
        if (sm.getFieldID() == GameConstants.HUNGRY_MUTO_EXIT_MAP) {
            if (chr.getQRValueByKey(entryQuest, "clear") != null && chr.getQRValueByKey(entryQuest, "clear").equals("1")) {
                int sel = sm.sendNext("#e#b<飢餓的武藤>#k#n\r\n\r\n#b#L0#領取<飢餓的武藤>獎勵。#l#k");
                boolean canEnd = true;
                for (Char member : party.getOnlineChars()) {
                    if (member.getScriptManager().getEmptyInventorySlots(1) < 5) {
                        canEnd = false;
                        break;
                    }
                }
                if (sel == 0 && canEnd) {
                    reward(party);
                } else {
                    sm.sendSayOkay("請確認裝備欄至少有 5 個空格。");
                }
            } else if (sm.sendAskYesNo("要離開嗎？")) {
                exit();
            }
        } else {
            int selection = sm.sendNext("#e#b<飢餓的武藤>#k#n\r\n你願意幫#b武藤#k打倒#r古拉#k嗎？"
                    + "\r\n"
                    + "#r*開始 10 分鐘後將無法中途離開。#k"
                    + "\r\n\r\n"
                    + "#b"
                    + "#L0#進入<飢餓的武藤>。#l"
                    + "\r\n"
                    + "#L1#聽西米雅說明。#l"
                    + "\r\n"
                    + "#L2#查看今天剩餘的挑戰次數。#l"
                    + "#k"
                    + "\r\n\r\n"
                    + "#e* 通關 1 次後，就可以選擇立即完成。#n");
            switch (selection) {
                case 0:
                    int response = sm.sendNext("#e#b飢餓的武藤#k#n\r\n"
                            + "\r\n"
                            + "要挑戰哪個#b難度#k呢？"
                            + "\r\n"
                            + "#b"
                            + "#L0#普通#l"
                            + "\r\n"
                            + "#L1#困難#l"
                            + "#k");
                    if (party == null) {
                        sm.sendSayOkay("必須加入#b隊伍#k才能進入飢餓的武藤組隊任務。快去找朋友吧！");
                    } else if (!party.isLeader(chr)) {
                        sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                    } else if (sm.checkPartyForPQ() && (response == 1 || response == 0)) {
                        if (isPartyEligible((short) 210, (short) 275, party)) {
                            if (sm.checkAttempt(entryQuest, party, 3)) {
                                try {
                                    String diff = response == 1 ? "hard" : "normal";
                                    for (Char player : party.getPartyMembersInSameFieldWithChr(chr)) {
                                        final LocalDateTime now = LocalDateTime.now();
                                        if (player.hasQuest(entryQuest) || player.hasQuestCompleted(entryQuest)) {
                                            int count = Integer.parseInt(player.getQRValueByKey(entryQuest, "count"));
                                            player.setQRValueByKey(entryQuest, "diff", diff);
                                            player.setQRValueByKey(entryQuest, "count", count == 3 ? "1" : String.valueOf(count + 1));
                                            player.setQRValueByKey(entryQuest, "inGame", "1");
                                            player.setQRValueByKey(entryQuest, "date", DateTimeFormatter.ofPattern("yy/MM/dd").format(now));
                                            player.setQRValueByKey(entryQuest, "score", "0");
                                            player.setQRValueByKey(entryQuest, "ptime", "0");
                                            player.setQRValueByKey(entryQuest, "clear", "0");
                                        } else {
                                            player.createQuestWithQRValue(entryQuest, String.format("diff=%s;count=%d;ingame=%d;date=%s;score=%d;ptime=%d;clear=%d", diff, 1, 1, DateTimeFormatter.ofPattern("yy/MM/dd").format(now), 0, 0, 0));
                                        }
                                    }
                                } catch (Exception e) {
                                    sm.sendSayOkay("無法開始組隊任務。");
                                } finally {
                                    party.setPartyQuest(this);
                                    sm.warpInstanceIn(chr, response == 1 ? GameConstants.HUNGRY_MUTO_HARD_STAGE : GameConstants.HUNGRY_MUTO_NORMAL_STAGE, true);
                                    sm.setInstanceTime(10 * 60, false);
                                }
                            } else {
                                sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                            }
                        } else {
                            sm.sendSayOkay("有隊員的等級低於 210，或尚未完成任務 [再見了，啾啾艾爾蘭]。");
                        }
                    }
                    break;
                case 1:
                    sm.sendNext("旅行者！#r古拉#k開始進攻了！#b武藤#k餓著肚子沒辦法保護我們不受#r古拉#k攻擊！快幫我做一頓美味的料理給他吃吧！"
                            + "\r\n\r\n"
                            + "#e<飢餓的武藤>#n"
                            + "\r\n\r\n"
                            + "#e1. 隊伍人數：#n 1-4 人"
                            + "\r\n"
                            + "#e2. 時間限制：#n 10 分鐘"
                            + "\r\n"
                            + "#e3. 每日可通關次數：#n 3"
                            + "\r\n"
                            + "#e4. 獎勵：#n #v1712002# #e#b#t1712002##k#n、經驗值"
                            + "\r\n\r\n\r\n"
                            + "#L0#聽詳細說明。#l");
                    int selection2 = sm.sendNext("有什麼需要幫忙的嗎？\r\n\r\n#b"
                            + "#L0#<狩獵與加入食材>#l"
                            + "\r\n"
                            + "#L1#<料理時間與獎勵量表>#l"
                            + "\r\n"
                            + "#L2#難度#l"
                            + "\r\n"
                            + "#L3#獎勵#l"
                            + "\r\n"
                            + "#L4#這樣就夠了。#l"
                            + "#k");
                    switch (selection2) {
                        case 0:
                            sm.sendNext("#e<狩獵與加入食材>#n"
                                    + "\r\n\r\n"
                                    + "守護啾啾艾爾蘭是#b很耗體力的工作#k。唯一能打倒#r古拉#k的方法，就是讓武藤吃飽。也就是說，要#b盡快做出武藤想吃的料理！#k"
                                    + "\r\n\r\n"
                                    + "到處#r狩獵怪物#k收集正確的食材，再帶到#b我和皮皮兄妹等待的鍋子#k旁，#e#r跳躍#k#n就能#b加入食材#k。");
                            break;
                        case 1:
                            sm.sendNext("#e<料理時間與獎勵量表>#n"
                                    + "\r\n\r\n"
                                    + "守護啾啾艾爾蘭是#b很耗體力的工作#k。唯一能打倒#r古拉#k的方法，就是讓武藤吃飽。也就是說，要#b盡快做出武藤想吃的料理！#k"
                                    + "\r\n\r\n"
                                    + "到處#r狩獵怪物#k收集正確的食材，再帶到#b我和皮皮兄妹等待的鍋子#k旁，#e#r跳躍#k#n就能#b加入食材#k。");
                            break;
                        case 2:
                            sm.sendNext("#e<難度>#n"
                                    + "\r\n\r\n"
                                    + "難度分為兩種：#b<普通>#k和#b<困難>#k"
                                    + "\r\n\r\n"
                                    + "困難模式的#r怪物更強#k，#r料理時間也更短。#k"
                                    + "\r\n\r\n"
                                    + "#r請記住，#k料理完成時#b武藤被擊退的速度#k和#b古拉被推回的距離#k，會依#b隊伍人數#k而不同！");
                            break;
                        case 3:
                            sm.sendNext("#e<獎勵>#n"
                                    + "\r\n\r\n"
                                    + "#e#b<普通>#k#n"
                                    + "\r\n"
                                    + "#eS 級#n：5 分鐘內通關"
                                    + " #e祕法符文#n：3"
                                    + "\r\n\r\n"
                                    + "#eA 級#n：8 分鐘內通關"
                                    + " #e祕法符文#n：2"
                                    + "\r\n\r\n"
                                    + "#eB 級#n：10 分鐘內通關"
                                    + " #e祕法符文#n：1"
                                    + "\r\n\r\n"
                                    + "#b+ 依評級獲得經驗值#k\r\n"
                                    + "#e<獎勵>#n"
                                    + "\r\n\r\n"
                                    + "#e#b<困難>#k#n"
                                    + "\r\n"
                                    + "#eS 級#n：5 分鐘內通關"
                                    + " #e祕法符文#n：5"
                                    + "\r\n\r\n"
                                    + "#eA 級#n：8 分鐘內通關"
                                    + " #e祕法符文#n：4"
                                    + "\r\n\r\n"
                                    + "#eB 級#n：10 分鐘內通關"
                                    + " #e祕法符文#n：3"
                                    + "\r\n\r\n"
                                    + "#b+ 依評級獲得經驗值#k");
                            break;
                    }
                    break;
                case 2:
                    int count = 0;
                    if (chr.hasQuest(entryQuest)) {
                        count = Integer.parseInt(chr.getQRValueByKey(entryQuest, "count"));
                    }
                    sm.sendNext("你今天的挑戰次數：" + count + "。");
                    break;
                default:
                    break;
            }
        }
    }

    public void reward(Party party) {
        for (Char chr : party.getOnlineChars()) {
            ScriptManagerImpl sm = chr.getScriptManager();
            String diff = chr.getQRValueByKey(entryQuest, "diff");
            int finishTime = Integer.parseInt(chr.getQRValueByKey(entryQuest, "ptime"));
            if (diff.equals("normal")) {
                if (finishTime >= 300) {
                    sm.giveSymbol(1712002, 3);
                    sm.showClearStageExpWindow(242714400);
                } else if (finishTime >= 120) {
                    sm.giveSymbol(1712002, 2);
                    sm.showClearStageExpWindow(222488200);
                } else {
                    sm.giveSymbol(1712002, 1);
                    sm.showClearStageExpWindow(202262000);
                }
            } else if (diff.equals("hard")) {
                if (finishTime >= 300) {
                    sm.giveSymbol(1712002, 5);
                    sm.showClearStageExpWindow(323619200);
                } else if (finishTime >= 120) {
                    sm.giveSymbol(1712002, 4);
                    sm.showClearStageExpWindow(283166800);
                } else {
                    sm.giveSymbol(1712002, 3);
                    sm.showClearStageExpWindow(242714400);
                }
            }
            chr.setQRValueByKey(entryQuest, "clear", "0");
        }
        exit();
        //sm.addEvent(EventManager.addEvent(this::exit, 2000));
    }

    @Override
    public void exit() {
        chr.setRecipe(new Tuple<>(0, 0));
        sm.warpInstanceOut(chr, GameConstants.HUNGRY_MUTO_ENTRANCE_MAP);
    }

    @Override
    public void event() {
        if (chr == null || chr.getParty() == null) {
            return;
        }
        if (party.isLeader(chr)) {
            setEnhance(false);
            party.getInstance().setMutoScore(500);
            setBonus(true);
            chr.getTimer().addEvent(this::spawnOrigin, 1, TimeUnit.SECONDS);
            sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg1");
        }
        initRecipe();
        //Global Effect
        effect(party);
    }

    @Override
    public void end(Char chr) {
        if (chr.getBonusTimer() != null) {
            chr.getBonusTimer().cancel(false);
        }
        if (chr.getScoreTimer() != null) {
            chr.getScoreTimer().cancel(false);
        }
        for (Char player : party.getOnlineChars()) {
            player.setRecipe(new Tuple<>(0, 0));
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.showEffectToField("Map/Effect3.img/hungryMuto/TimeOver");
        sm.warpInstanceOut(chr, GameConstants.HUNGRY_MUTO_EXIT_MAP);
    }

    @Override
    public void clear(Char chr) {
        if (chr.getBonusTimer() != null) {
            chr.getBonusTimer().cancel(false);
        }
        if (chr.getScoreTimer() != null) {
            chr.getScoreTimer().cancel(false);
        }
        final int finishTime = party.getInstance().getRemainingTime();
        for (Char player : party.getOnlineChars()) {
            ScriptManagerImpl sm = player.getScriptManager();
            player.setRecipe(new Tuple<>(0, 0));
            player.write(HungryMutoPacket.finish());
            chr.setQRValueByKey(entryQuest, "diff", chr.getQRValueByKey(entryQuest, "diff"));
            chr.setQRValueByKey(entryQuest, "count", chr.getQRValueByKey(entryQuest, "count"));
            chr.setQRValueByKey(entryQuest, "inGame", chr.getQRValueByKey(entryQuest, "inGame"));
            chr.setQRValueByKey(entryQuest, "date", chr.getQRValueByKey(entryQuest, "date"));
            chr.setQRValueByKey(entryQuest, "score", "" + party.getInstance().getMutoScore());
            chr.setQRValueByKey(entryQuest, "ptime", "" + finishTime);
            chr.setQRValueByKey(entryQuest, "clear", "1");
        }
        if (finishTime >= 300) {
            chr.getTimer().addEvent(() -> sm.showEffectToField("Map/Effect.img/Visitor/RankS"), 4000);
        } else if (finishTime >= 120) {
            chr.getTimer().addEvent(() -> sm.showEffectToField("Map/Effect.img/Visitor/RankA"), 4000);
        } else {
            chr.getTimer().addEvent(() -> sm.showEffectToField("Map/Effect.img/Visitor/RankB"), 4000);
        }
        chr.getTimer().addEvent(() -> sm.showEffectToField("Map/Effect3.img/hungryMuto/Clear"), 1000);
        sm.warpInstanceOut(chr, GameConstants.HUNGRY_MUTO_EXIT_MAP);
    }

    @Override
    public void leftOrDisband(Char chr) {
        if (chr.getBonusTimer() != null) {
            chr.getBonusTimer().cancel(false);
        }
        if (chr.getScoreTimer() != null) {
            chr.getScoreTimer().cancel(false);
        }
        if (chr.getParty() != null) {
            for (Char player : chr.getParty().getOnlineChars()) {
                player.setRecipe(new Tuple<>(0, 0));
            }
        } else {
            chr.setRecipe(new Tuple<>(0, 0));
        }
        chr.getTimer().addEvent(() -> sm.showEffectToField("Map/Effect3.img/hungryMuto/failed"), 1000);
        sm.warpInstanceOut(chr, GameConstants.HUNGRY_MUTO_EXIT_MAP);
    }

    public void setTimer() {
        Field field = chr.getField();
        //Chỉ chạy ở leader.
        if (!chr.getParty().isLeader(chr)) {
            return;
        }
        setBonus(true);
        setFinishTime((System.currentTimeMillis() + recipes[getMutoRecipe()][3]));
        setFinishBonusTime((System.currentTimeMillis() + recipes[getMutoRecipe()][4]));
        if (chr.getBonusTimer() != null) {
            chr.getBonusTimer().cancel(false);
        }
        ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(() -> {
            try {
                if (field.getChars().isEmpty()) {
                    chr.getBonusTimer().cancel(false);
                } else if (System.currentTimeMillis() > getFinishTime()) {
                    chr.getBonusTimer().cancel(false);
                    chr.getTimer().addEvent(this::recipe, 500);
                } else {
                    if (party.getInstance().getMutoScore() <= 0) {
                        end(chr);
                        return;
                    }
                    if (System.currentTimeMillis() > getFinishBonusTime()) {
                        setBonus(false);
                    }
                    field.broadcast(WvsContext.fieldValue("time", "" + (getFinishTime() - System.currentTimeMillis())));
                }
            } catch (Exception e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                chr.getBonusTimer().cancel(false);
            }
        }, 0, 5000, false);
        chr.setBonusTimer(sf);
        GlobalTimerManager.addCharTimer(chr.getId(), sf);
        int t;
        if (sm.getFieldID() == GameConstants.HUNGRY_MUTO_NORMAL_STAGE) {
            t = 80000;
        } else if (sm.getFieldID() == GameConstants.HUNGRY_MUTO_HARD_STAGE) {
            t = 60000;
        } else {
            end(chr);
            return;
        }
        if (chr.getScoreTimer() != null) {
            chr.getScoreTimer().cancel(false);
        }
        ScheduledFuture<?> sf2 = chr.getTimer().addFixedRateEvent(() -> {
            try {
                if (party.getInstance() == null) {
                    chr.getScoreTimer().cancel(false);
                    return;
                }
                party.getInstance().setMutoScore(party.getInstance().getMutoScore() - 50);
                if (field.getChars().size() == 0) {
                    chr.getScoreTimer().cancel(false);
                } else if (party.getInstance().getMutoScore() <= 0) {
                    chr.getScoreTimer().cancel(false);
                    end(chr);
                } else {
                    field.broadcast(WvsContext.fieldValue("score", "" + party.getInstance().getMutoScore()));
                }
            } catch (Exception e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
                chr.getScoreTimer().cancel(false);
            }
        }, 0, t, false);
        chr.setScoreTimer(sf2);
        GlobalTimerManager.addCharTimer(chr.getId(), sf2);
    }

    public void effect(Party party) {
        chr.write(HungryMutoPacket.setTime(10 * 60 * 1000));
        chr.write(WvsContext.sessionValue("rType", "0"));
        chr.write(WvsContext.sessionValue("rCount", "0"));
        chr.write(WvsContext.fieldValue("phase", "1"));
        if (party.isLeader(chr)) {
            chr.setRecipe(new Tuple<>(0, 0));
        }
    }

    //Dùng cho setup một lần trước khi vào map.
    //Todo: Recode lại toàn bộ event for clean code.
    public void initRecipe() {
        if (party.isLeader(chr)) {
            Field field = chr.getField();
            int mutoRecipe = (int) Math.floor(Math.random() * 7);
            if (isEnhance() && party.getInstance().getMutoScore() <= 350) {
                field.killMobs();
                setEnhance(false);
                spawnOrigin();
                field.broadcast(WvsContext.fieldValue("phase", "1"));
            } else if (!isEnhance() && party.getInstance().getMutoScore() >= 700) {
                field.killMobs();
                setEnhance(true);
                spawnOrigin();
                field.broadcast(WvsContext.fieldValue("phase", "2"));
                sm.showEffectToField("Map/Effect3.img/hungryMuto/TimeOver");
            }
            if (isEnhance()) {
                mutoRecipe += 8;
            }
            if (getMutoRecipe() > 0) {
                sm.showEffectToField("Map/Effect3.img/hungryMuto/failed");
                sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg6");
            }
            if (isComplete()) {
                party.getInstance().setMutoScore(party.getInstance().getMutoScore() + 200);
                sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg5");
                if (isBonus()) {
                    party.getInstance().setMutoScore(party.getInstance().getMutoScore() + 100);
                    sm.showEffectToField("Map/Effect3.img/hungryMuto/perfect");
                } else {
                    sm.showEffectToField("Map/Effect3.img/hungryMuto/good");
                }
                setComplete(false);
            }
            if (party.getInstance().getMutoScore() >= 1000) {
                clear(chr);
                return;
            }
            int type = recipes[mutoRecipe][1];
            setMutoRecipe(mutoRecipe);
            party.getInstance().setMutoRecipe(mutoRecipe);
            field.broadcast(WvsContext.fieldValue("foodType", "" + recipes[mutoRecipe][1]));
            Integer[][] rep = recipeItems[type];
            List<HungryMutoRecipes> hungryMutoRecipesList = new ArrayList<>();
            for (int i = 0; i < rep.length; ++i) {
                HungryMutoRecipes hungryMutoRecipes = new HungryMutoRecipes(i, rep[i][0], rep[i][1], 0);
                if (isEnhance()) {
                    if (Math.floor(Math.random() * 3) == 1) { // 33%
                        hungryMutoRecipes.setRecipeHidden(true);
                    }
                }
                hungryMutoRecipesList.add(hungryMutoRecipes);
            }
            party.getInstance().setMutoRecipes(hungryMutoRecipesList);
            setTimer();
        }
        sendRecipesUI();
    }

    public void recipe() {
        Field field = chr.getField();
        int mutoRecipe = (int) Math.floor(Math.random() * 7);
        if (isEnhance() && party.getInstance().getMutoScore() <= 350) {
            field.killMobs();
            setEnhance(false);
            spawnOrigin();
            field.broadcast(WvsContext.fieldValue("phase", "1"));
        } else if (!isEnhance() && party.getInstance().getMutoScore() >= 700) {
            field.killMobs();
            setEnhance(true);
            spawnOrigin();
            field.broadcast(WvsContext.fieldValue("phase", "2"));
            sm.showEffectToField("Map/Effect3.img/hungryMuto/TimeOver");
        }
        if (isEnhance()) {
            mutoRecipe += 8;
        }
        if (getMutoRecipe() > 0) {
            sm.showEffectToField("Map/Effect3.img/hungryMuto/failed");
            sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg6");
        }
        if (isComplete()) {
            party.getInstance().setMutoScore(party.getInstance().getMutoScore() + 200);
            sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg5");
            if (isBonus()) {
                party.getInstance().setMutoScore(party.getInstance().getMutoScore() + 100);
                sm.showEffectToField("Map/Effect3.img/hungryMuto/perfect");
            } else {
                sm.showEffectToField("Map/Effect3.img/hungryMuto/good");
            }
            setComplete(false);
        }
        if (party.getInstance().getMutoScore() >= 1000) {
            clear(chr);
            return;
        }
        int type = recipes[mutoRecipe][1];
        setMutoRecipe(mutoRecipe);
        party.getInstance().setMutoRecipe(mutoRecipe);
        field.broadcast(WvsContext.fieldValue("foodType", "" + recipes[mutoRecipe][1]));
        Integer[][] rep = recipeItems[type];
        List<HungryMutoRecipes> hungryMutoRecipesList = new ArrayList<>();
        for (int i = 0; i < rep.length; ++i) {
            HungryMutoRecipes hungryMutoRecipes = new HungryMutoRecipes(i, rep[i][0], rep[i][1], 0);
            if (isEnhance()) {
                if (Math.floor(Math.random() * 3) == 1) { // 33%
                    hungryMutoRecipes.setRecipeHidden(true);
                }
            }
            hungryMutoRecipesList.add(hungryMutoRecipes);
        }
        party.getInstance().setMutoRecipes(hungryMutoRecipesList);
        setTimer();
        sendRecipesUI();
        party.getInstance().setCreateNewRecipe(false);
    }

    public void sendRecipesUI() {
        chr.getField().broadcast(HungryMutoPacket.setNewRecipe(recipes[party.getInstance().getMutoRecipe()], party.getInstance().getMutoRecipes()));
    }

    public void spawnOrigin() {
        Field field = chr.getField();
        int[] mobid;
        if (field.getId() == GameConstants.HUNGRY_MUTO_NORMAL_STAGE) {
            mobid = setting[0];
        } else {
            mobid = setting[1];
        }
        if (isEnhance()) {
            for (int i = 0; i < xy.length; i++) {
                field.spawnMob(mobid[(i / 5) + 8], xy[i][0], xy[i][1], true, 0);
            }
            int rand = (int) Math.floor(Math.random() * xy.length);
            field.spawnMob(mobid[mobid.length - 1], xy[rand][0], xy[rand][1], false, 0);
        } else {
            for (int i = 0; i < xy.length; i++) {
                field.spawnMob(mobid[i / 5], xy[i][0], xy[i][1], true, 0);
            }
            int rand = (int) Math.floor(Math.random() * xy.length);
            field.spawnMob(mobid[mobid.length - 1], xy[rand][0], xy[rand][1], false, 0);
        }
    }

    public void check(Char chr) {
        Party party = chr.getParty();
        if (party == null || party.getInstance() == null) {
            chr.chatMessage("好像有點不對勁？請稍後再試。");
            chr.dispose();
            return;
        }
        int[] mutoRecipe = recipes[party.getInstance().getMutoRecipe()];
        boolean right = false; //Sử dụng cho khi một nguyên liệu đủ
        for (HungryMutoRecipes hungryMutoRecipe : party.getInstance().getMutoRecipes()) {
            if (hungryMutoRecipe.getRecipeItem() == chr.getRecipe().getLeft()) {
                right = true;
                hungryMutoRecipe.setRecipeCount(Math.min(hungryMutoRecipe.getRecipeReq(), hungryMutoRecipe.getRecipeCount() + chr.getRecipe().getRight()));
                break;
            }
        }
        boolean isInit = party.getInstance().getMutoRecipes().size() > 0;
        if (isInit) {
            chr.write(HungryMutoPacket.addItem(chr));
            chr.getField().broadcast(HungryMutoPacket.setRecipe(mutoRecipe, party.getInstance().getMutoRecipes(), chr));
            boolean finish = party.getInstance().getMutoRecipes().size() > 0;
            for (HungryMutoRecipes mhr : party.getInstance().getMutoRecipes()) {
                if (mhr.getRecipeCount() < mhr.getRecipeReq()) {
                    finish = false;
                    break;
                }
            }
            if (finish) {
                if (!party.getInstance().isCreateNewRecipe()) {
                    party.getInstance().setCreateNewRecipe(true);
                    setComplete(true);
                    chr.getTimer().addEvent(this::recipe, 500);
                    chr.setRecipe(new Tuple<>(0, 0));
                    chr.getField().broadcast(HungryMutoPacket.addItem(chr));
                    sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg4");
                }
            } else {
                if (right) {
                    chr.setRecipe(new Tuple<>(0, 0));
                    chr.getField().broadcast(HungryMutoPacket.addItem(chr));
                    sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg4");
                }
            }
        }
        chr.dispose();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isEnhance() {
        return enhance;
    }

    public void setEnhance(boolean enhance) {
        this.enhance = enhance;
    }

    public boolean isBonus() {
        return bonus;
    }

    public void setBonus(boolean bonus) {
        this.bonus = bonus;
    }

    public int getMutoRecipe() {
        return mutoRecipe;
    }

    public void setMutoRecipe(int mutoRecipe) {
        this.mutoRecipe = mutoRecipe;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public long getFinishBonusTime() {
        return finishBonusTime;
    }

    public void setFinishBonusTime(long finishBonusTime) {
        this.finishBonusTime = finishBonusTime;
    }
}
