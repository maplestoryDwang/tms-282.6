package net.swordie.ms.handlers.ui;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.AccountQuest;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.union.Union;
import net.swordie.ms.client.character.union.UnionArtifact;
import net.swordie.ms.client.character.union.UnionBoard;
import net.swordie.ms.client.character.union.UnionMember;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UnionPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.Etc.Artifact.ArtifactData;
import net.swordie.ms.loaders.QuestData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.FileTime;

import java.time.LocalDateTime;
import java.util.*;

public class UnionHandler {

    @Handler(op = InHeader.UNION_REQUEST)
    public static void handleUnionRequest(Char chr, InPacket inPacket) {
        Account acc = chr.getAccount();
        Union union = chr.getUnion();
        // only take lv 60+ 3rd job characters
        UnionBoard activeBoard = union.getBoardByPreset(chr.getActiveUnionPreset());
        if (activeBoard != null) {
            Set<UnionMember> membersToKeep = new HashSet<>();
            Set<UnionMember> membersToDelete = new HashSet<>();
            Set<Char> eligibleChars = acc.getEligibleUnionChars();
            for (UnionMember member : activeBoard.getActiveMembers()) {
                int charID = member.getCharId();
                Char x = acc.getCharById(charID);
                if (x != null) {
                    membersToKeep.add(member);
                } else {
                    membersToDelete.add(member);
                }
            }
            for (UnionMember member : membersToDelete) {
                member.deleteFromSQL();
            }
            activeBoard.setActiveMembers(membersToKeep);
            var availableCoins = chr.getQRValueByKey(QuestConstants.UNION_COIN, "coin");
            chr.write(UnionPacket.unionResult(availableCoins != null ? Integer.parseInt(availableCoins) : 0,
                    union.getUnionRank(),
                    eligibleChars,
                    membersToKeep,
                    null,
                    null,
                    null,
                    null));
            chr.write(UnionPacket.unionCoin(union.getUnionCoin()));
            chr.write(UnionPacket.unionAritfactChampionResult(eligibleChars));
        }
        if (chr.hasQuest(QuestConstants.UNION_ARTIFACT)) {
            chr.initUnionArtifact();
        }
        chr.dispose();
    }

    @Handler(op = InHeader.UNION_ASSIGN_REQUEST)
    public static void handleUnionAssignRequest(Char chr, InPacket inPacket) {
        Account acc = chr.getAccount();
        Union union = chr.getUnion();
        inPacket.decodeInt();
        inPacket.decodeInt();
        int preset = inPacket.decodeInt() - 1;
        if (!union.hasPresetUnlocked(preset)) {
            preset += 1;
            if (!union.hasPresetUnlocked(preset)) {
                chr.dispose();
                return;
            }
        }
        chr.setActiveUnionPreset(preset);
        int labCount = inPacket.decodeByte();
        StringBuilder synergyGrid = new StringBuilder();
        for (int j = 1; j < 8; j++) {
            synergyGrid.append(String.format("%d=%d%s", j, inPacket.decodeInt(), j == 7 ? "" : ";"));
        }
        AccountQuest q = chr.getAccount().getQuestById(QuestConstants.UNION_SYNERGY_BOARD);
        if (q == null) {
            q = QuestData.createAccQuestFromId(QuestConstants.UNION_SYNERGY_BOARD, chr.getAccount().getId());
            chr.getAccount().addQuest(q);
        }
        if (!q.getQRValue().contentEquals(synergyGrid)) {
            q.setQrValue(synergyGrid.toString());
            chr.write(WvsContext.questRecordExMessage(q));
        }
        int count = inPacket.decodeInt();
        UnionBoard activeBoard = union.getBoardByPreset(preset);
        if (activeBoard != null) {
            for (UnionMember unionMember : activeBoard.getActiveMembers()) {
                unionMember.deleteFromSQL();
            }
            activeBoard.getActiveMembers().clear();
            Account account = chr.getAccount();
            for (int i = 0; i < count; i++) {
                UnionMember.setCharGridPos(chr, inPacket, preset, account);
            }
            for (int i = 0; i < labCount; i++) {
                UnionMember.setCharGridPos(chr, inPacket, preset, account);
                // lab job = 10010900, lab enhanced job = 10010910
            }
            Set<Char> eligibleChars = acc.getEligibleUnionChars();
            activeBoard = union.getBoardByPreset(preset);
            activeBoard.updateUnionBoardToSQL();
            chr.write(UnionPacket.unionAssignResult(union.getUnionRank(),
                    eligibleChars,
                    activeBoard,
                    null,
                    null,
                    null,
                    null));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.UNION_PRESET_CHANGE)
    public static void handleUnionPresetChange(Char chr, InPacket inPacket) {
        Account acc = chr.getAccount();
        Union union = chr.getUnion();
        int preset = inPacket.decodeInt() - 1;
        if (!union.hasPresetUnlocked(preset)) {
            preset += 1;
            if (!union.hasPresetUnlocked(preset)) {
                chr.dispose();
                return;
            }
        }
        boolean unlocked = inPacket.decodeByte() != 0; // unsure
        List<Integer> synGrid = new ArrayList<>();
        for (int i = 0; i < Union.MAX_STATS; i++) {
            int syn = inPacket.decodeInt();
            if (syn < 0 || syn >= Union.MAX_STATS) {
                chr.dispose();
                return;
            }
            synGrid.add(syn);
        }
        UnionBoard ub = union.getBoardByPreset(preset);
        if (ub != null) {
            ub.setSynergyGrid(synGrid);
            chr.setActiveUnionPreset(preset);
            int count = inPacket.decodeInt();
            Account account = chr.getAccount();
            UnionBoard activeBoard = union.getBoardByPreset(preset);
            for (UnionMember unionMember : activeBoard.getActiveMembers()) {
                unionMember.deleteFromSQL();
            }
            activeBoard.getActiveMembers().clear();
            for (int i = 0; i < count; i++) {
                UnionMember.setCharGridPos(chr, inPacket, preset, account);
            }
            Set<Char> eligibleChars = acc.getEligibleUnionChars();
            activeBoard = union.getBoardByPreset(preset);
            chr.write(UnionPacket.unionAssignResult(union.getUnionRank(), eligibleChars, activeBoard,
                    null, null, null, null));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.UNION_PRESET_INFO_REQUEST)
    public static void handleUnionPresetInfoRequest(Char chr, InPacket inPacket) {
        int preset = inPacket.decodeInt();
        Union union = chr.getUnion();
        UnionBoard ub = union.getBoardByPreset(preset);
        if (ub != null) {
            boolean unlocked = union.hasPresetUnlocked(preset);
            chr.write(WvsContext.unionPresetInfoResult(preset, unlocked, ub));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.UNION_RAID_RETREAT)
    public static void handleUnionRaidRetreat(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tRequestTime if i had to guess
        if (chr.getField().getId() == 921172000) {
            chr.warp(921172200);
        }
    }

    @Handler(op = InHeader.UNION_ARTIFACT_UPDATE_REQUEST)
    public static void handleUnionArtifactUpdateRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();
        var info = ArtifactData.getArtifactInfo();
        switch (type) {
            case 0: { // Extend each
                int index = inPacket.decodeInt();
                int reqPoint = inPacket.decodeInt();
                UnionArtifact ua = chr.getAccount().getUnionArtifacts().get(index);
                if (ua == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.write(WvsContext.unionArtifactUpdate(type, index));
                    break;
                }
                int point = chr.getArtifactVal("point");
                if (point < reqPoint) {
                    chr.chatPopup("至少需要 "+reqPoint+" 點數才能使用此功能。");
                    chr.write(WvsContext.unionArtifactUpdate(type, index));
                    break;
                }
                ua.setExpirationTime(FileTime.fromDate(LocalDateTime.now().plusDays(30)));
                point -= reqPoint;
                chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
                chr.write(WvsContext.unionArtifactUpdate(chr));
                chr.write(WvsContext.unionArtifactUpdate(type, index));
                chr.write(WvsContext.unionArtifactQuestMsg(2, index, ua.getExpirationTime()));
                break;
            }
            case 1: { // Extend All
                int reqPoints = inPacket.decodeInt();
                for (UnionArtifact ua : chr.getAccount().getUnionArtifacts().values()) {
                    int point = chr.getArtifactVal("point");
                    if (point < reqPoints) {
                        chr.chatPopup("至少需要 " + reqPoints + " 點數才能使用此功能。");
                        chr.write(WvsContext.unionArtifactUpdate(type, 0));
                        break;
                    }
                    ua.setExpirationTime(FileTime.fromDate(LocalDateTime.now().plusDays(30)));
                    point -= reqPoints;
                    chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
                    chr.write(WvsContext.unionArtifactUpdate(chr));
                    chr.write(WvsContext.unionArtifactUpdate(type, 0));
                    chr.write(WvsContext.unionArtifactQuestMsg(3, 0, ua.getExpirationTime()));
                }
                break;
            }
            case 2: { // Convert Stats
                int index = inPacket.decodeInt();
                UnionArtifact ua = chr.getAccount().getUnionArtifacts().get(index);
                if (ua == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.write(WvsContext.unionArtifactUpdate(type, index));
                    break;
                }
                int point = chr.getArtifactVal("point");
                if (point < info.getChangePoint()) {
                    chr.chatPopup("至少需要 "+info.getChangePoint()+" 點數才能使用此功能。");
                    chr.write(WvsContext.unionArtifactUpdate(type, index));
                    break;
                }
                int req1 = inPacket.decodeInt();
                int req2 = inPacket.decodeInt();
                int req3 = inPacket.decodeInt();

                int cur1 = ua.getSkillID1();
                int cur2 = ua.getSkillID2();
                int cur3 = ua.getSkillID3();
                int level = ua.getLevel();

                // validate requested skills
                if (!info.getStats().containsValue(req1) ||
                        !info.getStats().containsValue(req2) ||
                        !info.getStats().containsValue(req3)) {
                    chr.chatPopup("技能與服務器數據不匹配。");
                    chr.write(WvsContext.unionArtifactUpdate(type, index));
                    break;
                }

                List<Skill> skills = new ArrayList<>(6);
                boolean changed = false;

                // ---- SLOT 1 ----
                if (req1 != cur1) {
                    Skill oldSkill = chr.getSkill(cur1);
                    if (oldSkill != null) {
                        int next = oldSkill.getCurrentLevel() - level;
                        if (next <= 0) chr.removeSkill(cur1);
                        else {
                            oldSkill.setCurrentLevel(next);
                            chr.addSkill(oldSkill);
                            skills.add(oldSkill);
                        }
                    }
                    Skill newSkill = SkillData.getSkillDeepCopyById(req1);
                    if (newSkill != null) {
                        newSkill.setCurrentLevel(Math.min(newSkill.getMasterLevel(), level));
                        chr.addSkill(newSkill);
                        skills.add(newSkill);
                    }
                    ua.setSkillID1(req1);
                    changed = true;
                }

                // ---- SLOT 2 ----
                if (req2 != cur2) {
                    Skill oldSkill = chr.getSkill(cur2);
                    if (oldSkill != null) {
                        int next = oldSkill.getCurrentLevel() - level;
                        if (next <= 0) chr.removeSkill(cur2);
                        else {
                            oldSkill.setCurrentLevel(next);
                            chr.addSkill(oldSkill);
                            skills.add(oldSkill);
                        }
                    }
                    Skill newSkill = SkillData.getSkillDeepCopyById(req2);
                    if (newSkill != null) {
                        newSkill.setCurrentLevel(Math.min(newSkill.getMasterLevel(), level));
                        chr.addSkill(newSkill);
                        skills.add(newSkill);
                    }
                    ua.setSkillID2(req2);
                    changed = true;
                }

                // ---- SLOT 3 ----
                if (req3 != cur3) {
                    Skill oldSkill = chr.getSkill(cur3);
                    if (oldSkill != null) {
                        int next = oldSkill.getCurrentLevel() - level;
                        if (next <= 0) chr.removeSkill(cur3);
                        else {
                            oldSkill.setCurrentLevel(next);
                            chr.addSkill(oldSkill);
                            skills.add(oldSkill);
                        }
                    }
                    Skill newSkill = SkillData.getSkillDeepCopyById(req3);
                    if (newSkill != null) {
                        newSkill.setCurrentLevel(Math.min(newSkill.getMasterLevel(), level));
                        chr.addSkill(newSkill);
                        skills.add(newSkill);
                    }
                    ua.setSkillID3(req3);
                    changed = true;
                }

                if (changed) {
                    point -= info.getResetPoint();
                    chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
                    if (!skills.isEmpty()) chr.addListSkill(skills);
                    chr.write(WvsContext.unionArtifactUpdate(chr));
                    chr.write(WvsContext.unionArtifactUpdate(type, index));
                }
                break;
            }
            case 3: { // Reset All Skills -> level = 1
                List<Skill> skills = new ArrayList<>();
                int point = chr.getArtifactVal("point");
                if (point < info.getResetPoint()) {
                    chr.chatPopup("至少需要 "+info.getResetPoint()+" 點數才能使用此功能。");
                    chr.write(WvsContext.unionArtifactUpdate(type, 0));
                    break;
                }
                for (UnionArtifact ua : chr.getAccount().getUnionArtifacts().values()) {
                    if (ua == null) break;
                    int s1 = ua.getSkillID1();
                    Skill sk1 = chr.getSkill(s1);
                    if (sk1 != null) {
                        sk1.setCurrentLevel(1);
                        chr.addSkill(sk1);
                        skills.add(sk1);
                    }
                    int s2 = ua.getSkillID2();
                    Skill sk2 = chr.getSkill(s2);
                    if (sk2 != null) {
                        sk2.setCurrentLevel(1);
                        chr.addSkill(sk2);
                        skills.add(sk2);
                    }
                    int s3 = ua.getSkillID3();
                    Skill sk3 = chr.getSkill(s3);
                    if (sk3 != null) {
                        sk3.setCurrentLevel(1);
                        chr.addSkill(sk3);
                        skills.add(sk3);
                    }
                    ua.setLevel(1);
                }
                if (!skills.isEmpty()) chr.addListSkill(skills);
                point -= info.getResetPoint();
                chr.setQRValueByKey(QuestConstants.UNION_ARTIFACT, "point", point);
                chr.write(WvsContext.unionArtifactUpdate(chr));
                chr.write(WvsContext.unionArtifactUpdate(type, 0));
                break;
            }
            case 4: { // Level Up
                int index = inPacket.decodeInt();
                UnionArtifact ua = chr.getAccount().getUnionArtifacts().get(index);
                if (ua == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.write(WvsContext.unionArtifactUpdate(type, index));
                    break;
                }

                if (ua.getLevel() >= info.getMaxSlotLevel()) {
                    chr.chatPopup("該聯盟神器已達到最高等級。");
                    chr.write(WvsContext.unionArtifactUpdate(type, index));
                    break;
                }

                ua.setLevel(ua.getLevel() + 1);

                List<Skill> skills = new ArrayList<>(3);

                int s1 = ua.getSkillID1();
                Skill sk1 = chr.getSkill(s1);
                if (sk1 != null) {
                    int next = Math.min(sk1.getMasterLevel(), sk1.getCurrentLevel() + 1);
                    if (next != sk1.getCurrentLevel()) {
                        sk1.setCurrentLevel(next);
                        chr.addSkill(sk1);
                        skills.add(sk1);
                    }
                }

                int s2 = ua.getSkillID2();
                Skill sk2 = chr.getSkill(s2);
                if (sk2 != null) {
                    int next = Math.min(sk2.getMasterLevel(), sk2.getCurrentLevel() + 1);
                    if (next != sk2.getCurrentLevel()) {
                        sk2.setCurrentLevel(next);
                        chr.addSkill(sk2);
                        skills.add(sk2);
                    }
                }

                int s3 = ua.getSkillID3();
                Skill sk3 = chr.getSkill(s3);
                if (sk3 != null) {
                    int next = Math.min(sk3.getMasterLevel(), sk3.getCurrentLevel() + 1);
                    if (next != sk3.getCurrentLevel()) {
                        sk3.setCurrentLevel(next);
                        chr.addSkill(sk3);
                        skills.add(sk3);
                    }
                }

                if (!skills.isEmpty()) chr.addListSkill(skills);

                chr.write(WvsContext.unionArtifactUpdate(chr));
                chr.write(WvsContext.unionArtifactUpdate(type, index));
                break;
            }
            case 7:
                chr.write(WvsContext.unionArtifactUpdate(type, 0));
                break;
            case 8:
                chr.createQuestWithQRValue(QuestConstants.UNION_ARTIFACT_SPECIAL_SYNC, "lastMissionSyncDate="+FileTime.currentTime().weeklyFormat());
                chr.write(WvsContext.unionArtifactUpdate(type, 0));
                break;
        }
    }
}
