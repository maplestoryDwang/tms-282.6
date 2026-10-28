package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.WzConstants;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.world.field.Instance;

public class FirstTimeTogether implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1323;

    Instance instance;
    byte stage = 0;

    public FirstTimeTogether(Char chr) {
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
        int selection = sm.sendSayOkay("<#e組隊任務：第一次同行#n>\r\n我在等待勇敢的冒險家。請大家同心協力，打倒兇惡的#r微笑大王#k！完成挑戰後微笑大王就會出現。你們必須找到正確的位置，並收集與問題答案相符的通行證。\r\n- #e等級#n：70 以上 #r(建議等級：70 - 255)#k\r\n- #e時間限制#n：20 分鐘\r\n- #e參加人數#n：3 ~ 4 人\r\n- #e獎勵#n：#v1072369# #t1072369#、性向經驗值與組隊點數#k\r\n#L0##b我要進行組隊任務。#k#l\r\n#L1##b查看今天剩餘的挑戰次數。#k#l");
        switch (selection) {
            case 0:
                if (sm.getFieldID() != GameConstants.FIRST_TIME_TOGETHER_ENTRANCE_MAP) {
                    sm.warp(GameConstants.FIRST_TIME_TOGETHER_ENTRANCE_MAP);
                } else {
                    if (party == null) {
                        sm.sendSayOkay("必須加入#b隊伍#k才能進入第一次同行組隊任務。快去找朋友吧！");
                    } else if (!party.isLeader(chr)) {
                        sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                    } else if (sm.checkPartyForPQ()) {
                        if (isPartyEligible((short) 70, (short) 275, party)) {
                            if (sm.checkAttempt(entryQuest, party)) {
                                sm.warpInstanceIn(chr, GameConstants.FIRST_TIME_TOGETHER_STAGE_1, true);
                                sm.setInstanceTime(20 * 60);
                                sm.setAchieveRatio(0);
                                party.getInstance().addProperty("kpq4answer", Randomizer.rand(10, 5000));
                                chr.getParty().setPartyQuest(this);
                                sm.addAttempt(entryQuest, party);
                                return;
                            } else {
                                sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                            }
                        } else {
                            sm.sendSayOkay("有隊員的等級低於 70 或高於 255。");
                        }
                    }
                }
            case 1:
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
        if (sm.sendAskYesNo("要離開嗎？")) {
            sm.setAchieveRatio(0, false);
            sm.warpInstanceOut(chr, GameConstants.FIRST_TIME_TOGETHER_ENTRANCE_MAP);
        }
    }

    @Override
    public void event() {
        String scriptName = sm.getLastActiveScriptName();
        instance = party.getInstance();
        stage = (byte) ((sm.getFieldID() % 10000) / 1000);

        switch (scriptName) {
            case "WUK_StageEnter": { //FirstEnterField
                switch (stage) {
                    case 1:
                        sm.invokeAfterDelay(2000, "showWeatherNoticeToField", "各位！請和克魯特對話，並打倒鱷魚收集克魯特要的通行證！", WeatherEffNoticeType.KerningPQ);
                        break;
                    case 2:
                        sm.invokeAfterDelay(2000, "showWeatherNoticeToField", "找出 3 條能打開下一關大門的繩子，然後抓住它們！", WeatherEffNoticeType.KerningPQ);
                        break;
                    case 3:
                        sm.invokeAfterDelay(2000, "showWeatherNoticeToField", "找出 3 個能打開下一關大門的平台！", WeatherEffNoticeType.KerningPQ);
                        break;
                    case 4:
                        for (Char chr : party.getOnlineChars()) {
                            if (!chr.hasQuestCompleted(5952)) {
                                chr.getScriptManager().startQuest(5952);
                            }
                            if (!chr.hasQuestCompleted(5979)) {
                                chr.getScriptManager().startQuest(5979);
                            }
                        }
                        sm.invokeAfterDelay(2000, "showWeatherNoticeToField", "啟動標記並收集數字，湊出這個數字：" + instance.getProperty("kpq4answer"), WeatherEffNoticeType.KerningPQ);
                        sm.invokeAfterDelay(8000, "showWeatherNoticeToField", "目標數字：" + instance.getProperty("kpq4answer"), WeatherEffNoticeType.KerningPQ, 0);
                        break;
                    case 5:
                        sm.spawnMob(9305205, 0, -435, false);
                        sm.invokeAfterDelay(2000, "showWeatherNoticeToField", "打倒超級綠水靈！", WeatherEffNoticeType.KerningPQ);
                        break;
                }
                break;
            }
            case "WUkerning_next": {
                switch (stage) {
                    case 1:
                        if (instance.hasProperty("kpq" + stage + "clear")) {
                            sm.sendNext("傳送點已開啟，請趕快前往下一關！");
                        } else {
                            byte numPasses = (byte) sm.getPartySize();
                            if (party.isLeader(chr)) {
                                if (instance.hasProperty("kpq" + stage + "leader_preamble")) {
                                    if (sm.hasItem(4001008, numPasses)) {
                                        sm.consumeItem(4001008);
                                        WUK_clearStage(stage);
                                        break;
                                    } else {
                                        sm.sendSayOkay("抱歉，通行證的數量不夠。你必須交給我正確數量的通行證，也就是隊伍人數扣掉隊長，共#b" + numPasses + " 張通行證#k才能通過這一關。請隊員們解開問題、收集通行證後交給你。");
                                    }
                                } else {
                                    sm.sendNext("你好，歡迎來到第一關。如你所見，這裡到處都是鱷魚。每打倒一隻鱷魚就會掉落一張#b卷#k。除了隊長以外，每位隊員都要先跟我對話，再帶來我指定數量的#b卷#k。等所有人都#b完成各自的任務#k，隊伍就能前往下一關。祝你好運！");
                                    instance.addProperty("kpq" + stage + "leader_preamble", sm.getField());
                                }
                            } else {
                                if (instance.hasProperty("kpq" + stage + "member_preamble")) {
                                    sm.sendNext("問題是：\r\n 請收集與三轉所需最低等級相同數量的卷。");
                                    if (sm.hasItem(4001008, 1)) {
                                        sm.sendSayOkay("請把通行證交給隊長。");
                                    } else if (sm.hasItem(4001007, numPasses)) {
                                        sm.sendNext("答對了！你獲得了一張#b通行證#k，請交給隊長。");
                                        sm.consumeItem(4001007, numPasses);
                                        sm.giveItem(4001008, 1);
                                    } else {
                                        sm.sendSayOkay("抱歉，答案不對！請在背包中準備正確數量的卷。");
                                    }
                                } else {
                                    sm.sendNext("你好，歡迎來到第一關。如你所見，這裡到處都是鱷魚。每打倒一隻鱷魚就會掉落一張#b卷#k。除了隊長以外，每位隊員都要先跟我對話，再帶來我指定數量的#b卷#k。等所有人都#b完成各自的任務#k，隊伍就能前往下一關。祝你好運！");
                                    instance.addProperty("kpq" + stage + "member_preamble", sm.getField());
                                }
                            }
                        }
                        break;
                    case 2:
                    case 3:
                        if (instance.hasProperty("kpq" + stage + "clear")) {
                            sm.sendNext("傳送點已開啟，請趕快前往下一關！");
                        } else {
                            String nthText = stage == 2 ? "第二" : "第三";
                            String nthObj = stage == 2 ? "繩子" : "平台";
                            String nthVerb = stage == 2 ? "吊" : "站";
                            String nthPos = stage == 2 ? "吊在繩子太低的位置" : "站得太靠近邊緣";
                            if (party.isLeader(chr)) {
                                if (instance.hasProperty("kpq" + stage + "leader_preamble")) {
                                    if (sm.rectangleStages(stage)) {
                                        WUK_clearStage(stage);
                                    } else {
                                        WUK_wrong();
                                    }
                                    break;
                                } else {
                                    sm.sendNext("歡迎來到" + nthText + "關。我旁邊有好幾個" + nthObj + "。這些" + nthObj + "之中，#b有 3 個連接著通往下一關的傳送點#k。只要讓#b3 名隊員或 3 個道具找到正確的" + nthObj + "並" + nthVerb + "在上面就可以了。#k\r\n但是，如果" + nthPos + "就不算答案；請待在" + nthObj + "的中間才會被判定為正確答案。另外，只能有 3 名隊員待在" + nthObj + "上。等他們" + nthVerb + "好之後，隊長必須#b點兩下我確認答案是否正確#k。現在，去找出正確的" + nthObj + "並" + nthVerb + "上去吧！");
                                    instance.addProperty("kpq" + stage + "leader_preamble", sm.getField());
                                }
                            } else {
                                sm.sendSayOkay("請讓隊長來跟我說話。");
                            }
                        }
                        break;
                    case 4:
                        if (instance.hasProperty("kpq" + stage + "clear")) {
                            sm.sendNext("傳送點已開啟，請趕快前往下一關！");
                        }
                        break;
                    case 5:
                        if (instance.hasProperty("kpq" + stage + "clear")) {
                            sm.sendNext("傳送點已開啟，請趕快前往下一關！");
                        } else {
                            if (!sm.hasMobById(9305205)) {
                                clear(chr);
                                break;
                            } else {
                                sm.sendNext("你好，歡迎來到第五關，也是最後一關。在地圖上四處走走就能找到BOSS怪物。打倒全部的怪物、收集#b通行證#k後交給我。拿到通行證後交給隊長，等#b通行證#k收集齊全再由隊長交給我。這些怪物你或許很熟悉，但可能比你想像的強很多，請務必小心。祝你好運！");
                            }
                        }
                        break;
                }
                break;
            }
            case "WUkerningCal0":
            case "WUkerningCal1":
            case "WUkerningCal2":
            case "WUkerningCal3":
                int parentID = sm.getScriptInfoByType(sm.getLastActiveScriptType()).getParentID();
                String sign = scriptName.equals("WUkerningCal0") ? "+" : (scriptName.equals("WUkerningCal1") ? "-" : (scriptName.equals("WUkerningCal2")) ? "x" : "/");
                if (party.isLeader(chr)) {
                    if (instance.hasProperty("kpq4calculate")) {
                        if (!instance.getProperty("kpq4calculate").equals(sign)) {
                            for (int i = 9250100; i <= 9250103; i++) {
                                sm.changeReactorState(i, (byte) 0);
                            }
                            instance.setProperty("kpq4calculate", sign);
                            sm.increaseReactorState(parentID, 0);
                            break;
                        }
                    } else {
                        sm.increaseReactorState(parentID, 0);
                        instance.addProperty("kpq4calculate", sign);
                        break;
                    }
                } else {
                    chr.chatScriptMessage("只有隊長可以執行該操作。");
                }
                break;
        }
    }

    @Override
    public void end(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setAchieveRatio(0);
        sm.warpInstanceOut(chr, GameConstants.FIRST_TIME_TOGETHER_ENTRANCE_MAP);
    }

    @Override
    public void clear(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.sendNext("恭喜你們通過所有關卡。路上小心……");
        sm.givePQRewards(party);
        end(chr);
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }

    public void WUK_clearStage(byte stage) {
        instance.addProperty("kpq" + stage + "clear", sm.getField());
        sm.sendSayOkay("恭喜通過這一關！我會打開通往下一關的傳送點。前往下一關有時間限制，請快一點。祝大家好運！");
        sm.showEffectToField(WzConstants.EFFECT_PQ_CLEAR);
        sm.playSound(WzConstants.EFFECT_PQ_SOUND_CLEAR, true);
        sm.setObjectState("gate", 0);
        sm.playPortalSoundToField();
        sm.setAchieveRatio(stage * 20);
    }

    public void WUK_wrong() {
        sm.showEffectToField(WzConstants.EFECT_PQ_WRONG);
        sm.playSound(WzConstants.EFECT_PQ_SOUND_WRONG, true);
    }
}