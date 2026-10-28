package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.UIType;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Instance;

public class DragonRider implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1210;

    public DragonRider(Char chr) {
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
        sm.setSpeakerID(9020016);
        int selection = sm.sendNext("#e<組隊任務：龍騎士>#n\r\n可以幫幫我這個老人家嗎？打倒龍騎士，讓神木村恢復和平吧！\r\n\r\n#b" +
                "#L0#1. 進入天空之門。(等級 150 以上 / 隊伍至少 3 人)#l\r\n" +
                "#L1#2. 尋找隊伍。#l\r\n" +
                "#L2#3. 我想多了解一些#l\r\n" +
                "#L3#4. 我今天還可以挑戰幾次？#l#k");
        switch (selection) {
            case 0:
                if (party == null) {
                    sm.sendSayOkay("請先創建隊伍再繼續。");
                } else if (!party.isLeader(chr)) {
                    sm.sendSayOkay("請讓你的隊長與我對話。");
                } else if (sm.checkPartyForPQ()) {
                    if (isPartyEligible((short) 150, (short) 275, party)) {
                        if (sm.checkAttempt(entryQuest, party)) {
                            sm.warpInstanceIn(chr, GameConstants.DRAGON_RIDER_FIRST_STAGE, true);
                            sm.setInstanceTime(GameConstants.DRAGON_RIDER_TIME, 910002000);
                            sm.setAchieveRatio(0);
                            chr.getParty().setPartyQuest(this);
                            sm.addAttempt(entryQuest, party);
                            return;
                        } else {
                            sm.sendSayOkay("隊伍中有成員已經用完該組隊任務的入場次數。");
                        }
                    } else {
                        sm.sendSayOkay("隊伍中有人尚未達到 150 級。你必須達到 150 級或更高！");
                    }
                }
                break;
            case 1:
                sm.openUI(UIType.UI_PARTY_INVITATION);
                break;
            case 2:
                sm.sendSayOkay("進入#b天空之門#k，查明#r最後的龍#k的身分。使用#b飛行#k技能飛越天空，追上飛龍尋找線索。\r\n #e- 等級#n：150 以上 #r（建議等級：150 - 169）#k\r\n#e- 時間限制#n：30 分鐘\r\n#e- 人數#n：3-6 \r\n#e- 需求#n：飛行技能");
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
        if (sm.sendAskYesNo("要撤退嗎？")) {
            sm.setAchieveRatio(0, false);
            sm.warpInstanceOut(chr, 910002000);
        }
    }

    @Override
    public void event() {
        if (sm.getFieldID() == 240080400) {
            sm.warpParty(sm.getFieldID() + 100, party);
        } else {
            Instance instance = party.getInstance();
            int stage = (sm.getFieldID() % 1000) / 100;
            if (instance.hasProperty("escape" + stage + "clear")) {
                sm.warpParty(sm.getFieldID() + 100, party);
            } else {
                sm.chat("還不能繼續，請先完成當前階段。");
            }
        }
    }

    @Override
    public void end(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setAchieveRatio(0);
        sm.warpInstanceOut(chr, 910002000);
    }

    @Override
    public void clear(Char chr) {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }
}