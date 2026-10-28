package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.WzConstants;
import net.swordie.ms.enums.UIType;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Instance;

public class LordPirate implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1204;

    public LordPirate(Char chr) {
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
        int selection = sm.sendNext("#e<組隊任務：金勾海賊王>#n\r\n有什麼事嗎？\r\n\r\n" +
                "#b" +
                "#L0#我要進行組隊任務。#l\r\n" +
                "#L1#我想找隊伍。#l\r\n" +
                "#L2#我想了解一下。#l\r\n" +
                "#L3#我想要海賊王帽子。#l\r\n" +
                "#L4#我今天還剩幾次挑戰次數？#l" +
                "#k");
        switch (selection) {
            case 0:
                if (sm.getFieldID() != GameConstants.LORD_PIRATE_ENTRANCE_MAP) {
                    sm.sendSayOkay("你比外表看起來還勇敢呢。跟我來，我帶你去空中監獄。");
                    sm.warp(GameConstants.LORD_PIRATE_ENTRANCE_MAP);
                } else {
                    if (party == null) {
                        sm.sendSayOkay("必須加入#b隊伍#k才能進入金勾海賊王組隊任務。快去找朋友吧！");
                    } else if (!party.isLeader(chr)) {
                        sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                    } else if (sm.checkPartyForPQ()) {
                        if (isPartyEligible((short) 130, (short) 275, party)) {
                            if (sm.checkAttempt(entryQuest, party)) {
                                sm.sendNext("#m9800156#突襲了我們，深山人蔘之王#b#p2094001##k被綁走了。請趕走#m9800156#和他的手下，救出#b#p2094001##k。\r\n#L0##b沒問題！我來幫你，邱翁。#l#k");
                                sm.warpInstanceIn(chr, GameConstants.LORD_PIRATE_FIRST_STAGE, true);
                                sm.setInstanceTime(GameConstants.LORD_PIRATE_TIME, GameConstants.LORD_PIRATE_ENTRANCE_MAP);
                                sm.setAchieveRatio(0);
                                chr.getParty().setPartyQuest(this);
                                sm.addAttempt(entryQuest, party);
                                return;
                            } else {
                                sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                            }
                        } else {
                            sm.sendSayOkay("有隊員的等級低於 130 或高於 255。");
                        }
                    }
                }
                break;
            case 1:
                sm.openUI(UIType.UI_PARTY_INVITATION);
                break;
            case 2:
                sm.sendSayOkay("深山人蔘的家園#b#m251000000##k遭到#r#m9800156##k襲擊，深山人蔘之王#b#p2094001##k被綁走了。召集夥伴攻進海盜船，把#m9800156#和他的手下趕走吧！\r\n#e- 等級#n：130 以上 #r(建議等級：130 - 149)#k\r\n#e- 人數#n：3 - 6 人\r\n#e- 獎勵#n：#v1003856# #b#t1003856##k");
                break;
            case 3:
                int exchangeSel = sm.sendNext("謝謝你從#b金勾海賊王#k手中救出#b雨揚#k。只要把他的帽子碎塊帶來，我就幫你黏成一頂#b海賊王帽子#k。你想要哪一頂？\r\n" +
                        "#L0##b#v1003856# #t1003856##k #r(需要 #t4001455# x30)#k#l\r\n" +
                        "#L1##b#v1003857# #t1003857##k #r(需要 #t4001455# x60)#k#l\r\n" +
                        "#L2##b#v1003858# #t1003858##k #r(需要 #t4001455# x90)#k#l");
                if (sm.hasItem(4001455, 30 * (exchangeSel + 1)) && sm.canHold(1003856 + exchangeSel)) {
                    sm.giveItem(1003856 + exchangeSel);
                    sm.consumeItem(4001455, 30 * (exchangeSel + 1));
                } else {
                    sm.sendSayOkay("你沒有足夠的 " + 30 * (exchangeSel + 1) + " 個#t4001455#，或裝備欄空間不足。");
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
        if (sm.sendAskYesNo("確定要離開嗎？")) {
            sm.setAchieveRatio(0, false);
            sm.warpInstanceOut(chr, GameConstants.LORD_PIRATE_ENTRANCE_MAP);
        }
    }

    @Override
    public void event() {
        int stage = (sm.getFieldID() % 1000) / 100;
        int requireCount = 30;
        int ROOKIE_PIRATE_MARK = 4001120; // Stage 0
        int RISING_PIRATE_MARK = 4001121; // Stage 1
        int VETERAN_PIRATE_MARK = 4001122; // Stage 2
        Instance instance = party.getInstance();
        if (sm.getFieldID() == 925100100) {
            if (party.isLeader(chr)) {
                if (instance.hasProperty("lordpirate" + stage + "clear")) {
                    sm.sendNext("請從右邊的傳送點前進");
                } else {
                    if (sm.hasItem(ROOKIE_PIRATE_MARK, requireCount) && sm.hasItem(RISING_PIRATE_MARK, requireCount) && sm.hasItem(VETERAN_PIRATE_MARK, requireCount)) {
                        sm.showEffectToField(WzConstants.EFFECT_PQ_CLEAR);
                        sm.playSound(WzConstants.EFFECT_PQ_SOUND_CLEAR, true);
                        sm.setAchieveRatio(stage * 25);
                        sm.showClearStageExpWindowToParty(sm.getPQExp());
                        sm.consumeItem(ROOKIE_PIRATE_MARK, requireCount);
                        sm.consumeItem(RISING_PIRATE_MARK, requireCount);
                        sm.consumeItem(VETERAN_PIRATE_MARK, requireCount);
                        instance.addProperty("lordpirate" + stage + "clear", sm.getField());
                        sm.sendNext("太好了，你們可以前往下一關了！");
                    } else {
                        sm.sendNext("請帶來 " + requireCount + " 個：" +
                                "\r\n#v" + ROOKIE_PIRATE_MARK + "##b#t" + ROOKIE_PIRATE_MARK + "##k" +
                                "\r\n#v" + RISING_PIRATE_MARK + "##b#t" + RISING_PIRATE_MARK + "##k" +
                                "\r\n#v" + VETERAN_PIRATE_MARK + "##b#t" + VETERAN_PIRATE_MARK + "##k");
                    }
                }
            } else {
                sm.sendSayOkay("請讓隊長來跟我說話。");
            }
        } else if (sm.getFieldID() == 925100500) {
            if (!sm.hasMobsInField() && !instance.hasProperty("lordpirate" + stage + "clear")) {
                if (party.isLeader(chr)) {
                    sm.sendNext("你們幫了我們大忙，真不知道該怎麼報答才好？");
                } else {
                    sm.sendSayOkay("請讓隊長來跟我說話。");
                }
                sm.givePQRewards(party);
                sm.setAchieveRatio(100);
                instance.addProperty("lordpirate" + stage + "clear", sm.getField());
                for (Char player : party.getOnlineChars()) {
                    ScriptManagerImpl sm = player.getScriptManager();
                    sm.consumeItem(4001117, sm.getQuantityOfItem(4001117)); // Old Metal Key
                    sm.consumeItem(4001120, sm.getQuantityOfItem(4001120)); // Rookie Pirate Mark
                    sm.consumeItem(4001121, sm.getQuantityOfItem(4001121)); // Rising Pirate Mark
                    sm.consumeItem(4001122, sm.getQuantityOfItem(4001122)); // Veteran Pirate Mark
                }
                sm.invokeAfterDelay(2000, "warpParty", 925100700, party);
            } else {
                sm.sendSayOkay("請打倒船長！");
            }
        } else {
            exit();
        }
    }

    @Override
    public void end(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setAchieveRatio(0);
        sm.warpInstanceOut(chr, GameConstants.LORD_PIRATE_ENTRANCE_MAP);
    }

    @Override
    public void clear(Char chr) {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }
}