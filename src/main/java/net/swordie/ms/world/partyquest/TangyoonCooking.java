package net.swordie.ms.world.partyquest;


import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;

public class TangyoonCooking implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;

    public TangyoonCooking(Char chr) {
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
        int selection = sm.sendNext("#e<組隊任務：和湯寶寶一起做料理>#n\r\n想為鯨魚號的船員們做些美味的料理嗎？我可以教你。\r\n\r\n#b#L0#開始和湯寶寶一起做料理。#l\r\n#L1#領取湯寶寶的廚師服。#l\r\n#L2#聽和湯寶寶一起做料理的說明。#l\r\n#L3#查看今天剩餘的挑戰次數。#l#k");
        switch (selection) {
            case 0:
                if (chr.getField().getId() != GameConstants.TANGYOON_COOKING_ENTRANCE_MAP) {
                    sm.sendNext("料理可不簡單喔。如果你還是想試試看，就跟我來吧");
                    sm.warp(GameConstants.TANGYOON_COOKING_ENTRANCE_MAP);
                } else {
                    if (party == null) {
                        sm.sendSayOkay("必須加入#b隊伍#k才能進入湯寶寶料理組隊任務。快去找朋友吧！");
                    } else if (!party.isLeader(chr)) {
                        sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                    } else if (sm.checkPartyForPQ()) {
                        if (isPartyEligible((short) 60, (short) 90, party)) {
                            sm.warpInstanceIn(chr, GameConstants.TANGYOON_COOKING_ENTRANCE_MAP, true);
                            sm.sendSayOkay("俗話說廚師太多會煮壞一鍋湯。想進去的話，隊伍人數必須在 3 人以下，由隊長帶大家進去就好，一個人來也可以。");
                            sm.setInstanceTime(20 * 60);
                            party.getInstance().addProperty("Tangyoon Cooking", true);
                            chr.getParty().setPartyQuest(this);
                            return;
                        } else {
                            sm.sendSayOkay("有隊員的等級低於 60 或高於 90。");
                        }
                    }
                }
                break;
            case 1:
                int selection1 = sm.sendNext("廚師服？只有最有天分的廚師才能拿到喔。\r\n\r\n#L0#給我廚師服(需要 2 張以上#t4033668#)。#l\r\n#L1#給我廚師帽(需要 3 張以上#t4033668#)。#l");
                if (selection1 == 0) {
                    if (sm.hasItem(4033668, 2) && sm.canHold(1052578)) {
                        sm.consumeItem(4033668, 2);
                        sm.giveItem(1052578);
                    } else {
                        sm.sendNext("你確定有 2 張#t4033668#嗎？如果有，請確認裝備欄沒有滿。還有別想騙我！我聞得出謊言的味道。");
                    }
                } else {
                    if (sm.hasItem(4033668, 3) && sm.canHold(1003762)) {
                        sm.consumeItem(4033668, 3);
                        sm.giveItem(1003762);
                    } else {
                        sm.sendNext("你確定有 3 張#t4033668#嗎？如果有，請確認裝備欄沒有滿。還有別想騙我！我聞得出謊言的味道。");
                    }
                }
                break;
            case 2:
                sm.sendNext("歡迎來到楓之谷世界最棒的海盜船─鯨魚號！我是負責廚房的主廚湯寶寶！大家都知道這裡是我說了算，我做的菜可是好吃到不行。");
                sm.sendPrev("但就連我這麼厲害的廚師，也快應付不了這些海盜了！我從沒見過這麼會吃的傢伙。我開始覺得需要找助手了。");
                sm.sendPrev("這就是你登場的時候了。只要你夠強，我就教你做料理。你看，要先把食材打到服服貼貼才能下鍋。還要恰到好處地撒鹽、燒烤，不過這些之後再說。");
                sm.sendPrev("料理好吃的話，你就會被當成英雄。要是不好吃……嗯，你也知道，海盜都是用拳頭表達意見的。不過我想你應付得來。要試試看嗎？一天最多可以挑戰 5 次。\r\n#e- 等級#n：60 以上 #r(建議等級：60 - 90)#k\r\n#e- 時間限制#n：20 分鐘\r\n#e- 人數#n：1-3 人\r\n#e- 獎勵#n：\r\n#v1003762# #t1003762#\r\n#v1052578# #t1052578#");
                break;
            case 3:
                //TODO: Attempt
                sm.sendSayOkay("今天還可以挑戰 5 次。");
                break;
        }
    }

    @Override
    public void exit() {
        sm.setSpeakerID(1097001);
        if (sm.sendAskYesNo("已經要放棄了嗎？")) {
            sm.warpInstanceOut(chr, GameConstants.TANGYOON_COOKING_ENTRANCE_MAP);
        }
    }

    @Override
    public void end(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.warpInstanceOut(chr, GameConstants.TANGYOON_COOKING_ENTRANCE_MAP);
    }

    @Override
    public void clear(Char chr) {

    }

    @Override
    public void event() {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }
}
