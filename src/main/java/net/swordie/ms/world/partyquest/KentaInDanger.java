package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;

public class KentaInDanger implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1214;

    public KentaInDanger(Char chr) {
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
        int selection = sm.sendNext("#e<組隊任務：危險的坎特>#n\r\n唉，可憐的坎特！你一定要救救他。他聽說有些海洋生物行為怪異，就跑去調查了。他到現在都還沒回來，我好擔心。我們一定要找到坎特，你能幫幫我們嗎？\r\n\r\n" +
                "#b" +
                "#L0#我去找坎特#l\r\n" +
                "#L1#我想要坎特的新護目鏡。#l\r\n" +
                "#L2#還有其他需要知道的嗎？#l\r\n" +
                "#L3#我還可以挑戰危險的坎特幾次？#l#k");
        switch (selection) {
            case 0:
                if (sm.getFieldID() != GameConstants.KENTA_IN_DANGER_ENTRANCE_MAP) {
                    sm.sendNext("謝謝你！我們一起前往#m923040000#吧。");
                    sm.warp(GameConstants.KENTA_IN_DANGER_ENTRANCE_MAP);
                } else {
                    if (party == null) {
                        sm.sendSayOkay("必須加入#b隊伍#k才能進入危險的坎特組隊任務。快去找朋友吧！");
                    } else if (!party.isLeader(chr)) {
                        sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                    } else if (sm.checkPartyForPQ()) {
                        if (isPartyEligible((short) 160, (short) 275, party)) {
                            if (!sm.checkAttempt(entryQuest, party)) {
                                sm.warpInstanceIn(chr, GameConstants.KENTA_IN_DANGER_FIRST_STAGE, true);
                                sm.setInstanceTime(GameConstants.KENTA_IN_DANGER_TIME, GameConstants.KENTA_IN_DANGER_ENTRANCE_MAP);
                                sm.setAchieveRatio(0);
                                chr.getParty().setPartyQuest(this);
                                sm.addAttempt(entryQuest, party);
                                return;
                            } else {
                                sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                            }
                        } else {
                            sm.sendSayOkay("隊伍人數不足 2 人，無法進入。需要 2 名 160 等以上的隊員才能進入，請確認後再跟我說話。");
                        }
                    }
                }
                break;
            case 1:
                if (sm.sendAskYesNo("喔，你想要#v1022175# #t1022175#是嗎？我們只把#t1022175#送給真正幫助我們更了解海洋的人。這樣吧，只要你帶#b10 個#t4001535##k給我做研究，我就給你。只不過#t4001535#要打倒海怒斯才拿得到。就看你怎麼選囉！")) {
                    if (sm.hasItem(4001535, 10)) {
                        sm.consumeItem(4001535, 10);
                        sm.giveItem(1022175);
                    } else {
                        sm.sendSayOkay("現在不是做研究的時候！坎特失蹤了！他一定有危險！請先找到他、救救他。");
                    }
                }
                break;
            case 2:
                sm.sendNext("坎特覺得光靠冒險家帶回來的樣本不夠做進一步研究，就說要去觀察海洋生物的怪異行為，跑到了危險之海。從他離開後我就再也沒見過他，也沒有他的消息。我想這一定不是好兆頭。\r\n#e- 等級#n：160 以上 #r(建議等級：160 - 179)#k\r\n#e- 時間限制#n：20 分鐘\r\n#e- 人數#n：2-6 人\r\n#e- 獎勵#n：\r\n#v1022175# #t1022175#");
                break;
            case 3:
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
            sm.warpInstanceOut(chr, GameConstants.KENTA_IN_DANGER_ENTRANCE_MAP);
        }
    }

    @Override
    public void event() {
    }

    @Override
    public void end(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setAchieveRatio(0);
        sm.warpInstanceOut(chr, GameConstants.KENTA_IN_DANGER_ENTRANCE_MAP);
    }

    @Override
    public void clear(Char chr) {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }
}