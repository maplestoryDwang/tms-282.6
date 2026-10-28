package net.swordie.ms.world.partyquest;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.client.social.Party.PartyMember;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.UIType;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Instance;

import java.util.HashMap;

public class AlienVisitor implements PartyQuest {

    private final ScriptManagerImpl sm;
    private final Char chr;
    private final Party party;
    private final int entryQuest = 17206;
    private final long RANK_S_EXP = 5720000;
    private final long RANK_A_EXP = 4630000;
    private final long RANK_B_EXP = 3120000;
    private final long RANK_C_EXP = 1610000;

    public AlienVisitor(Char chr) {
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
        if (sm.getFieldID() == GameConstants.ALIEN_VISITOR_ENTRANCE_MAP) {
            int selection = sm.sendNext("太空船內部正發出奇怪的訊號！我需要一位強者幫忙進行一些實驗。\r\n\r\n" +
                    "#b" +
                    "#L0#詢問任務內容。#l\r\n" +
                    "#L1##e進入太空船。#n#l\r\n" +
                    "#L2#尋找隊伍。#l\r\n" +
                    "#k");
            switch (selection) {
                case 0:
                    sm.sendNext("外星訪客的目標是幫我打倒墜落在楓之谷世界附近的外星人，而且越快完成，獲得的獎勵就越多。\r\n" +
                            "#e- 等級：#n 200 以上 #r(建議等級：200-249)#k\r\n" +
                            "#e- 時間：#n 每關 5 分鐘\r\n" +
                            "#e- 人數上限：#n 4\r\n" +
                            "#e- 獎勵：#n" +
                            "\r\n#i1113038# #z1113038#\r\n" +
                            "#i1122256# #z1122256#\r\n" +
                            "#i1032191# #z1032191#\r\n" +
                            "#i1132230# #z1132230#\r\n" +
                            "#i1003893# #z1003893#");
                    break;
                case 1:
                    if (party == null) {
                        sm.sendSayOkay("請先建立隊伍再繼續。");
                    } else if (!party.isLeader(chr)) {
                        sm.sendSayOkay("請讓你的隊長與我對話。");
                    } else if (sm.checkPartyForPQ()) {
                        if (isPartyEligible((short) 200, (short) 275, party)) {
                            if (sm.checkAttempt(entryQuest, party)) {
                                if (sm.sendNext("準備好了的話，就踏進光裡吧！\r\n\r\n#b#L0#進入太空船。#l#k") == 0) {
                                    sm.sendNext("1. 必須在時間結束前打倒 180 隻怪物或首領。\r\n2. 必須#e#b完成#k#n 5 個階段才能獲得經驗值。\r\n\r\n別忘了那些關鍵點喔！");
                                    for (Char player : chr.getParty().getOnlineChars()) {
                                        player.setVisitorStageResult(new HashMap<>());
                                    }
                                    sm.warpInstanceIn(chr, GameConstants.ALIEN_VISITOR_START_MAP, true);
                                    sm.setInstanceTime(GameConstants.ALIEN_VISITOR_TIME, GameConstants.ALIEN_VISITOR_EXT_MAP);
                                    chr.getParty().setPartyQuest(this);
                                    sm.addAttempt(entryQuest, party);
                                    return;
                                }
                            } else {
                                sm.sendSayOkay("隊伍中有成員已經用完該組隊任務的入場次數。");
                            }
                        } else {
                            sm.sendSayOkay("隊伍中有人尚未達到 200 級。你必須達到 200 級或更高！");
                        }
                    }
                    break;
                case 2:
                    sm.openUI(UIType.UI_PARTY_INVITATION);
                    break;
            }
        } else {
            exit();
        }
    }

    @Override
    public void exit() {
        chr.getParty().setPartyQuest(null);
        if (chr.getField().getId() == GameConstants.ALIEN_VISITOR_EXT_MAP) {
            long totalEXP = 0;
            if (!chr.getVisitorStageResult().isEmpty()) {
                String stage1Result = chr.getVisitorStageResult().getOrDefault(1, "Failed");
                String stage1Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                if (!stage1Result.equals("")) {
                    switch (stage1Result) {
                        case "S" -> {
                            totalEXP += RANK_S_EXP;
                            stage1Icon = "#fUI/UIWindowBT.img/Visitor/rank/s";
                        }
                        case "A" -> {
                            totalEXP += RANK_A_EXP;
                            stage1Icon = "#fUI/UIWindowBT.img/Visitor/rank/a";
                        }
                        case "B" -> {
                            totalEXP += RANK_B_EXP;
                            stage1Icon = "#fUI/UIWindowBT.img/Visitor/rank/b";
                        }
                        case "C" -> {
                            totalEXP += RANK_C_EXP;
                            stage1Icon = "#fUI/UIWindowBT.img/Visitor/rank/c";
                        }
                        default -> {
                            stage1Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                        }
                    }
                }
                String stage2Result = chr.getVisitorStageResult().getOrDefault(2, "Failed");
                String stage2Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                if (!stage2Result.equals("")) {
                    switch (stage2Result) {
                        case "S" -> {
                            totalEXP += RANK_S_EXP;
                            stage2Icon = "#fUI/UIWindowBT.img/Visitor/rank/s";
                        }
                        case "A" -> {
                            totalEXP += RANK_A_EXP;
                            stage2Icon = "#fUI/UIWindowBT.img/Visitor/rank/a";
                        }
                        case "B" -> {
                            totalEXP += RANK_B_EXP;
                            stage2Icon = "#fUI/UIWindowBT.img/Visitor/rank/b";
                        }
                        case "C" -> {
                            totalEXP += RANK_C_EXP;
                            stage2Icon = "#fUI/UIWindowBT.img/Visitor/rank/c";
                        }
                        default -> {
                            stage2Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                        }
                    }
                }
                String stage3Result = chr.getVisitorStageResult().getOrDefault(3, "Failed");
                String stage3Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                if (!stage3Result.equals("")) {
                    switch (stage3Result) {
                        case "S" -> {
                            totalEXP += RANK_S_EXP;
                            stage3Icon = "#fUI/UIWindowBT.img/Visitor/rank/s";
                        }
                        case "A" -> {
                            totalEXP += RANK_A_EXP;
                            stage3Icon = "#fUI/UIWindowBT.img/Visitor/rank/a";
                        }
                        case "B" -> {
                            totalEXP += RANK_B_EXP;
                            stage3Icon = "#fUI/UIWindowBT.img/Visitor/rank/b";
                        }
                        case "C" -> {
                            totalEXP += RANK_C_EXP;
                            stage3Icon = "#fUI/UIWindowBT.img/Visitor/rank/c";
                        }
                        default -> {
                            stage3Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                        }
                    }
                }
                String stage4Result = chr.getVisitorStageResult().getOrDefault(4, "Failed");
                String stage4Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                if (!stage4Result.equals("")) {
                    switch (stage4Result) {
                        case "S" -> {
                            totalEXP += RANK_S_EXP;
                            stage4Icon = "#fUI/UIWindowBT.img/Visitor/rank/s";
                        }
                        case "A" -> {
                            totalEXP += RANK_A_EXP;
                            stage4Icon = "#fUI/UIWindowBT.img/Visitor/rank/a";
                        }
                        case "B" -> {
                            totalEXP += RANK_B_EXP;
                            stage4Icon = "#fUI/UIWindowBT.img/Visitor/rank/b";
                        }
                        case "C" -> {
                            totalEXP += RANK_C_EXP;
                            stage4Icon = "#fUI/UIWindowBT.img/Visitor/rank/c";
                        }
                        default -> {
                            stage4Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                        }
                    }
                }
                String stage5Result = chr.getVisitorStageResult().getOrDefault(5, "Failed");
                String stage5Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                if (!stage5Result.equals("")) {
                    switch (stage5Result) {
                        case "S" -> {
                            totalEXP += RANK_S_EXP;
                            stage5Icon = "#fUI/UIWindowBT.img/Visitor/rank/s";
                        }
                        case "A" -> {
                            totalEXP += RANK_A_EXP;
                            stage5Icon = "#fUI/UIWindowBT.img/Visitor/rank/a";
                        }
                        case "B" -> {
                            totalEXP += RANK_B_EXP;
                            stage5Icon = "#fUI/UIWindowBT.img/Visitor/rank/b";
                        }
                        case "C" -> {
                            totalEXP += RANK_C_EXP;
                            stage5Icon = "#fUI/UIWindowBT.img/Visitor/rank/c";
                        }
                        default -> {
                            stage5Icon = "#fUI/UIWindowBT.img/Visitor/rank/f";
                        }
                    }
                }
                sm.sendNext("你的外星訪客評級結果：\r\n" +
                        "#e第 1 關：#n " + stage1Icon + "\r\n\r\n" +
                        "#e第 2 關：#n " + stage2Icon + "\r\n\r\n" +
                        "#e第 3 關：#n " + stage3Icon + "\r\n\r\n" +
                        "#e第 4 關：#n " + stage4Icon + "\r\n\r\n" +
                        "#e第 5 關：#n " + stage5Icon + "\r\n\r\n" +
                        "#e獲得的總經驗值：#n #r" + Util.getNumberFormat(totalEXP) +
                        "#k");
            }
            if (sm.sendAskYesNo("據其他志願者說，又出現了稀有的首領生物。\r\n#b你要離開這裡嗎？#k")) {
                sm.giveExpNoAffectedByExpRate(totalEXP);
                chr.getVisitorStageResult().clear();
                sm.warp(GameConstants.ALIEN_VISITOR_ENTRANCE_MAP);
            }
        } else {
            if (sm.sendAskYesNo("據其他志願者說，又出現了稀有的首領生物。\r\n#b你要離開這裡嗎？#k")) {
                sm.warpInstanceOut(chr, GameConstants.ALIEN_VISITOR_EXT_MAP);
            }
        }
    }

    @Override
    public void event() {
        Instance instance = chr.getParty().getInstance();
        if (chr.getParty().isLeader(chr)) {
            if (chr.getField().getId() == 861000050) {
                sm.warpParty(861000100, party);
                instance.setVisitorStageCount(0);
                instance.setTimeout(5 * 60, true);
            } else if ((chr.getField().getId() == 861000100 || chr.getField().getId() == 861000200 || chr.getField().getId() == 861000400) && instance.getVisitorStageCount() == 180) {
                sm.warpParty(chr.getField().getId() + 100, party);
                instance.setVisitorStageCount(0);
                instance.setTimeout(5 * 60, true);
            } else if ((chr.getField().getId() == 861000300 || chr.getField().getId() == 861000500) && instance.getVisitorStageCount() == 1) {
                if (chr.getField().getId() == 861000500) {
                    sm.warpInstanceOut(chr, GameConstants.ALIEN_VISITOR_EXT_MAP);
                } else {
                    sm.warpParty(chr.getField().getId() + 100, party);
                    instance.setVisitorStageCount(0);
                    instance.setTimeout(5 * 60, true);
                }
            } else {
                sm.chat("傳送門已關閉！");
            }
        } else {
            sm.sendSayOkay("只有你的 #b隊長#k 可以進入傳送門！");
        }
    }

    @Override
    public void end(Char chr) {
        if (chr.getParty() != null) {
            for (Char player : chr.getParty().getOnlineChars()) {
                player.getVisitorStageResult().clear();
            }
        } else {
            chr.getVisitorStageResult().clear();
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.warpInstanceOut(chr, GameConstants.ALIEN_VISITOR_EXT_MAP);
    }

    @Override
    public void clear(Char chr) {
    }

    @Override
    public void leftOrDisband(Char chr) {
        end(chr);
    }
}