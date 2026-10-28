package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.WzConstants;
import net.swordie.ms.enums.UIType;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Instance;

public class Juliet implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1205;

    public Juliet(Char chr) {
        this.sm = chr.getScriptManager();
        this.chr = chr;
        this.party = chr.getParty();
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
        if (sm.getFieldID() == 910002000) {
            if (sm.sendNext("勇敢的楓之谷冒險家們，請幫助我們守護瑪加提亞脆弱的和平！\r\n#L0##b前往#m261000021#聽茱麗葉的故事。") == 0) {
                sm.warp(261000021);
            }
        } else if (sm.getFieldID() == 261000021) {
            int selection = sm.sendNext("#e<組隊任務：羅密歐與茱麗葉>#n\r\n瑪加提亞正面臨嚴重的威脅，我們需要勇敢的冒險家幫忙。\r\n\r\n" +
                    "#b" +
                    "#L0#聽茱麗葉的故事。#l\r\n" +
                    "#L1#開始任務。#l\r\n" +
                    "#L2#尋找隊伍。#l\r\n" +
                    "#L3#用#z4001160#製作項鍊。#l\r\n" +
                    "#L4#將兩條項鍊合而為一。#l\r\n" +
                    "#L5#我還剩幾次挑戰次數？#l" +
                    "#k");
            switch (selection) {
                case 0:
                    sm.sendNext("我是茱麗葉，我深愛著羅密歐，我也知道他同樣愛著我。問題是，我屬於卡帕萊特協會，而羅密歐屬於蒙特鳩協會，我們注定無法在一起……");
                    sm.sendSay("但你要知道，過去並不是這樣的。所以我們最大的心願，就是成為蒙特鳩與卡帕萊特之間的橋樑，為兩個協會的和平盡一份心力。");
                    sm.sendSay("我們已經盡力了，但很遺憾，瑪加提亞現在#b正處於全面開戰的邊緣#k。因為不久前，#b蒙特鳩和卡帕萊特的能量來源都不見了#k。兩個協會互相指責對方，情況一天比一天惡化。");
                    sm.sendSay("最近我收到匿名消息，說這其實是跟兩個協會毫無關係的#b第三者#k幹的。為了阻止瑪加提亞的內戰、讓我對羅密歐的愛開花結果，我們必須找出那個#b第三者#k，阻止他摧毀這座偉大的塔。");
                    sm.sendSay("拿出你的勇氣，幫忙守護瑪加提亞的和平吧！\r\n" +
                            "#e- 等級：#n 70 以上 #r(建議等級：70-119)#k\r\n" +
                            "#e- 時間限制：#n 20 分鐘\r\n" +
                            "#e- 參加人數：#n 4 人\r\n" +
                            "#e- 獎勵：#n" +
                            "\r\n#i1122117# #z1122117#\r\n(收集 #r20#k 個#b#z4001160##k後可向#b茱麗葉#k領取。)" +
                            "\r\n#i1122118# #z1122118#\r\n(可用 1 個#b#z1122116##k和 1 個#b#z1122117##k交換)");
                    break;
                case 1:
                    if (party == null) {
                        sm.sendSayOkay("必須加入#b隊伍#k才能進入茱麗葉組隊任務。快去找朋友吧！");
                    } else if (!party.isLeader(chr)) {
                        sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                    } else if (sm.checkPartyForPQ()) {
                        if (isPartyEligible((short) 70, (short) 275, party)) {
                            if (sm.checkAttempt(entryQuest, party, 10)) {
                                sm.warpInstanceIn(chr, GameConstants.JULIET_ENTRANCE_FIRST_STAGE, true);
                                sm.setInstanceTime(GameConstants.ROMEO_JULIET_TIME, GameConstants.JULIET_EXIT_MAP);
                                sm.setAchieveRatio(0);
                                chr.getParty().setPartyQuest(this);
                                sm.addAttempt(entryQuest, party);
                                return;
                            } else {
                                sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                            }
                        } else {
                            sm.sendSayOkay("隊伍中有人還沒到 70 等。必須 70 等以上才能幫助茱麗葉！");
                        }
                    }
                    break;
                case 2:
                    sm.openUI(UIType.UI_PARTY_INVITATION);
                    break;
                case 3:
                    if (sm.hasItem(4001160, 20)) {
                        if (sm.canHold(1122117)) {
                            sm.giveItem(1122117);
                            sm.consumeItem(4001160, 20);
                        } else {
                            sm.sendSayOkay("裝備欄的空格不足。");
                        }
                    } else {
                        sm.sendSayOkay("#i4001160# #z4001160#不足 20 個。");
                    }
                    break;
                case 4:
                    if (sm.hasItem(1122116) && sm.hasItem(1122117)) {
                        if (sm.canHold(1122118)) {
                            sm.giveItem(1122118);
                            sm.consumeItem(1122116, 1);
                            sm.consumeItem(1122117, 1);
                        } else {
                            sm.sendSayOkay("裝備欄的空格不足。");
                        }
                    } else {
                        sm.sendSayOkay("需要 #i1122116# #z1122116# 和 #i1122117# #z1122117# 各 1 個。");
                    }
                    break;
                case 5:
                    int count = 10;
                    if (chr.hasQuest(entryQuest)) {
                        count = 10 - Integer.parseInt(chr.getQRValueByKey(entryQuest, "count"));
                    }
                    sm.sendSayOkay("你還可以挑戰 " + count + " 次。");
                    break;
            }
        } else {
            exit();
        }
    }

    @Override
    public void exit() {
        if (sm.sendAskYesNo("要離開嗎？")) {
            sm.setAchieveRatio(0, false);
            sm.warpInstanceOut(chr, GameConstants.JULIET_EXIT_MAP);
        }
    }

    @Override
    public void event() {
        Instance instance = chr.getParty().getInstance();
        if (chr.getField().getId() == GameConstants.JULIET_ENTRANCE_FIRST_STAGE) {
            if (Util.succeedProp(5)) {
                if (!instance.hasProperty("juliet1clear") && !sm.hasItem(4001131) && sm.canHold(4001131)) {
                    sm.giveItem(4001131);
                    sm.sendSayOkay("獲得了 #i4001131# #z4001131#！");
                } else {
                    sm.sendSayOkay("這裡什麼都沒找到。");
                }
            } else if (Util.succeedProp(20)) {
                int selection = sm.sendNext("這是一個看起來很可疑的開關。#b\r\n\r\n#L0#按下開關。\r\n#L1#不要動它。");
                if (selection == 0 && !instance.hasProperty("juliet1clear")) {
                    clearStage(1);
                }
            } else if (Util.succeedProp(40)) {
                if (!instance.hasProperty("juliet1clear")) {
                    sm.giveExpNoAffectedByExpRate(500);
                    sm.sendSayOkay("獲得了 500 經驗值！");
                } else {
                    sm.sendSayOkay("這裡什麼都沒找到。");
                }
            } else if (Util.succeedProp(60)) {
                if (!instance.hasProperty("juliet1clear")) {
                    sm.giveMesos(500);
                    sm.sendSayOkay("找到了 500 楓幣");
                } else {
                    sm.sendSayOkay("這裡什麼都沒找到。");
                }
            } else {
                sm.sendSayOkay("這裡什麼都沒找到。");
            }
        } else if (chr.getField().getId() == 926110100) {
            boolean canPass = true;
            for (Reactor r : chr.getField().getReactors()) {
                if (r.getTemplateId() == 2618000 && r.getState() < 7) {
                    canPass = false;
                    break;
                }
            }
            if (canPass) {
                clearStage(3);
                sm.warp(926110200);
            } else {
                sm.chat("這個傳送點被封住了");
            }
        } else if (chr.getField().getId() == 926110200) {
            if (!instance.hasProperty("juliet4clear")) {
                if (chr.getParty().isLeader(chr)) {
                    if (sm.hasItem(4001134, 2)) {
                        sm.consumeItem(4001134, 2);
                        clearStage(4);
                        sm.changeReactorState(2618007, (byte) 1);
                    } else {
                        sm.chat("這個傳送點被封住了");
                    }
                } else {
                    sm.sendSayOkay("請把 2 張 #i4001134# #z4001134# 交給隊長！");
                }
            } else {
                sm.warp(926110203);
            }
        } else if (chr.getField().getId() == 926110203) {
            if (!instance.hasProperty("juliet5start")) {
                int avgLevel = chr.getParty().getAvgPartyLevel();
                for (int i = 0; i < 15; i++) {
                    sm.spawnMob(9300143, chr.getPosition().getX(), chr.getPosition().getY(), false, 4000L * avgLevel);
                }
                for (int i = 0; i < 10; i++) {
                    sm.spawnMob(9300144, chr.getPosition().getX(), chr.getPosition().getY(), false, 4000L * avgLevel);
                }
                instance.addProperty("juliet5start", true);
            } else if (instance.hasProperty("juliet5start") && chr.getField().getMobs().size() == 0) {
                if (!instance.hasProperty("juliet5clear")) {
                    if (Util.succeedProp(20)) {
                        int selection = sm.sendNext("這是一個看起來很可疑的開關。#b\r\n\r\n#L0#按下開關。\r\n#L1#不要動它。");
                        if (selection == 0) {
                            clearStage(5);
                        } else {
                            sm.sendSayOkay("這裡什麼都沒找到。");
                        }
                    } else {
                        sm.sendSayOkay("這裡什麼都沒找到。");
                    }
                }
            }
        } else if (chr.getField().getId() == 926110401) {
            if (chr.getParty().isLeader(chr)) {
                sm.removeNpc(2112010);
                int avgLevel = chr.getParty().getAvgPartyLevel();
                chr.getField().spawnMob(9300150, -535, -126, false, 9999999999L);
                chr.getField().spawnMob(9300150, -276, -126, false, 9999999999L);
                for (int i = 0; i < 2; i++) {
                    chr.getField().spawnMob(9300143, 58, 150, false, 20000L * avgLevel);
                }
                for (int i = 0; i < 2; i++) {
                    chr.getField().spawnMob(9300143, 250, 150, false, 20000L * avgLevel);
                }
                for (int i = 0; i < 2; i++) {
                    chr.getField().spawnMob(9300143, 350, 150, false, 20000L * avgLevel);
                }
                for (int i = 0; i < 2; i++) {
                    chr.getField().spawnMob(9300144, -158, 150, false, 20000L * avgLevel);
                }
                for (int i = 0; i < 2; i++) {
                    chr.getField().spawnMob(9300144, 100, 150, false, 20000L * avgLevel);
                }
                for (int i = 0; i < 2; i++) {
                    chr.getField().spawnMob(9300144, 200, 150, false, 20000L * avgLevel);
                }
                chr.getField().spawnMob(9300152, 100, 150, false, 100000L * avgLevel); // Angry Frankenroid
                sm.showWeatherNoticeToField("請打倒弗蘭肯洛伊德，保護羅密歐！", WeatherEffNoticeType.JulietNPC);
            } else {
                sm.sendSayOkay("只有#b隊長#k可以跟我說話！");
            }
        }
    }

    @Override
    public void end(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setAchieveRatio(0);
        sm.warpInstanceOut(chr, GameConstants.JULIET_EXIT_MAP);
    }

    @Override
    public void clear(Char chr) {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }

    private void clearStage(int stage) {
        Instance instance = chr.getParty().getInstance();
        instance.addProperty("juliet" + stage + "clear", sm.getField());
        sm.showEffectToField(WzConstants.EFFECT_PQ_CLEAR);
        sm.playSound(WzConstants.EFFECT_PQ_SOUND_CLEAR, true);
        sm.showClearStageExpWindowToParty(sm.getPQExp());
        sm.setAchieveRatio(stage * 15);
    }
}