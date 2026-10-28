package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.WzConstants;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Instance;

public class Escape implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 1215;

    public Escape(Char chr) {
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
        sm.setSpeakerID(9020005);
        int selection = sm.sendNext("#e<組隊任務：逃脫>#n\r\n" +
                "其實我本來想逃走的……但我沒辦法丟下他不管。他被關在空中監獄裡，需要有人去把他救出來。\r\n\r\n" +
                "#b" +
                "#L0#我來救被困在城裡的冒險家！#l\r\n" +
                "#L1#請告訴我城堡監獄的事。#l\r\n" +
                "#L2#請告訴我更多關於教官的鑰匙的事。#l" +
                "#k");
        switch (selection) {
            case 0:
                if (sm.getFieldID() != GameConstants.ESCAPE_ENTRANCE_MAP) {
                    sm.sendSayOkay("你比外表看起來還勇敢呢。跟我來，我帶你去空中監獄。");
                    sm.warp(GameConstants.ESCAPE_ENTRANCE_MAP);
                } else {
                    if (party == null) {
                        sm.sendSayOkay("必須加入#b隊伍#k才能進入逃脫組隊任務。快去找朋友吧！");
                    } else if (!party.isLeader(chr)) {
                        sm.sendSayOkay("想挑戰任務的話，請讓#b隊長#k來跟我說話。");
                    } else if (sm.checkPartyForPQ()) {
                        if (isPartyEligible((short) 120, (short) 275, party)) {
                            if (sm.checkAttempt(entryQuest, party)) {
                                sm.warpInstanceIn(chr, GameConstants.ESCAPE_STAGE_FIRST_STAGE, true);
                                sm.setInstanceTime(GameConstants.ESCAPE_TIME, GameConstants.ESCAPE_ENTRANCE_MAP);
                                sm.setAchieveRatio(0);
                                chr.getParty().setPartyQuest(this);
                                sm.addAttempt(entryQuest, party);
                                return;
                            } else {
                                sm.sendSayOkay("有隊員已達今日挑戰次數上限。");
                            }
                        } else {
                            sm.sendSayOkay("有隊員的等級低於 120 或高於 255。");
                        }
                    }
                }
                break;
            case 1:
                sm.sendNext("這座城堡裡有一座隱藏之塔，很多人被關在那座塔的空中監獄裡，必須有人去救他們……\r\n#e- 等級#n：140 以上 #r(建議等級：140 - 159)#k\r\n#e- 時間限制#n：20 分鐘\r\n#e- 人數#n：3-6 人\r\n#e- 獎勵#n：\r\n#v1132094# #t1132094#\r\n#v1132095# #t1132095#\r\n#v1132096# #t1132096#\r\n#v1132097# #t1132097#\r\n#v1132098# #t1132098#");
                sm.sendSay("計畫是這樣的：\r\n1. 避開障礙物，潛入監獄。\r\n2. 消滅地圖上所有的守衛。\r\n3. 穿過迷宮，找到監獄入口。\r\n4. 消滅守護監獄大門的守衛。\r\n5. 避開陷阱，進入空中監獄。\r\n6. 消滅所有守衛，找到監獄鑰匙。\r\n7. 打倒教官亞尼，救出囚犯。");
                break;
            case 2:
                int exchangeSel = sm.sendNext("#r#z4001534##k是隱藏之塔的教官們持有的鑰匙。只要帶 #b5#k 把給我，我就送你一份小禮物。畢竟能拿到這麼多，代表你救了很多人。" +
                        "#b" +
                        "\r\n#L0##v1132094# #z1132094##l" +
                        "\r\n#L1##v1132095# #z1132095##l" +
                        "\r\n#L2##v1132096# #z1132096##l" +
                        "\r\n#L3##v1132097# #z1132097##l" +
                        "\r\n#L4##v1132098# #z1132098##l" +
                        "#k");
                if (sm.hasItem(4001534, 5) && sm.canHold(1132094 + exchangeSel)) {
                    sm.giveItem(1132094 + exchangeSel);
                    sm.consumeItem(4001534, 5);
                } else {
                    sm.sendSayOkay("#t4001534#不足 5 個，或裝備欄空間不足。");
                }
                break;
        }
    }

    @Override
    public void exit() {
        sm.setSpeakerID(9020005);
        if (sm.sendAskYesNo("要離開嗎？")) {
            sm.setAchieveRatio(0, false);
            sm.warpInstanceOut(chr, GameConstants.ESCAPE_ENTRANCE_MAP);
        }
    }

    @Override
    public void event() {
        Instance instance = party.getInstance();
        int stage = (sm.getFieldID() % 1000) / 100;
        if (!instance.hasProperty("escape" + stage + "clear")) {
            if (!sm.hasMobsInField() || sm.getFieldID() == 921160600) {
                instance.addProperty("escape" + stage + "clear", sm.getField());
                sm.showEffectToField(WzConstants.EFFECT_PQ_CLEAR);
                sm.playSound(WzConstants.EFFECT_PQ_SOUND_CLEAR, true);
                sm.showClearStageExpWindowToParty(sm.getPQExp());
                sm.setAchieveRatio((stage == 3 || stage == 6) ? (stage * 10 + 10) : (stage * 10));
                sm.invokeAfterDelay(2000, "warpParty", sm.getFieldID() + 100, party);
            }
        }
    }

    @Override
    public void end(Char chr) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setAchieveRatio(0);
        sm.warpInstanceOut(chr, GameConstants.ESCAPE_ENTRANCE_MAP);
    }

    @Override
    public void clear(Char chr) {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }
}