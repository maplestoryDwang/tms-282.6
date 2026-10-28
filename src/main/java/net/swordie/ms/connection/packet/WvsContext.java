package net.swordie.ms.connection.packet;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.AccountQuest;
import net.swordie.ms.client.LinkSkill;
import net.swordie.ms.client.character.*;
import net.swordie.ms.client.character.achievement.AchievementData;
import net.swordie.ms.client.character.achievement.AchievementRank;
import net.swordie.ms.client.character.info.ExpIncreaseInfo;
import net.swordie.ms.client.character.info.ZeroInfo;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.client.character.potential.CharacterPotentialValueHolder;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.reward.RewardResult;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.TownPortal;
import net.swordie.ms.client.character.skills.matrix.MatrixCore;
import net.swordie.ms.client.character.skills.matrix.NodeEnhance;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.character.union.UnionBoard;
import net.swordie.ms.client.jobs.resistance.WildHunterInfo;
import net.swordie.ms.client.social.Alliance.AllianceResult;
import net.swordie.ms.client.social.Friend.FriendResult;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.client.social.Guild.GuildBBSPacket;
import net.swordie.ms.client.social.Guild.GuildResult;
import net.swordie.ms.client.social.Party.*;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.RandomPortal;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.Etc.Artifact.ArtifactData;
import net.swordie.ms.util.AntiMacro;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.event.EventListData;

import java.time.LocalDateTime;
import java.util.*;

import static net.swordie.ms.constants.HexaMatrixConstants.*;
import static net.swordie.ms.enums.InvType.EQUIPPED;
import static net.swordie.ms.enums.MessageType.*;

public class WvsContext {

    public static OutPacket exclRequest() {
        return new OutPacket(OutHeader.EXCL_REQUEST);
    }

    public static OutPacket statChanged(Map<Stat, Object> stats, int subJob, byte mixBaseHairColor, byte mixAddHairColor, byte mixHairBaseProb) {
        return statChanged(stats, 0, mixBaseHairColor, mixAddHairColor, mixHairBaseProb, (byte) 0, false, 0, 0, (short) subJob);
    }

    public static OutPacket statChanged(Map<Stat, Object> stats,
                                        int exclRequestSent,
                                        byte mixBaseHairColor,
                                        byte mixAddHairColor,
                                        byte mixHairBaseProb,
                                        byte charmOld,
                                        boolean updateCovery,
                                        int hpRecovery,
                                        int mpRecovery,
                                        short subJob) {
        OutPacket outPacket = new OutPacket(OutHeader.STAT_CHANGED);

        outPacket.encodeByte(exclRequestSent);
        outPacket.encodeByte(false); // ?
        outPacket.encodeByte(1);
        // GW_CharacterStat::DecodeChangeStat
        int mask = 0;
        for (Stat stat : stats.keySet()) {
            mask |= stat.getVal();
        }
        outPacket.encodeLong(mask);
        Comparator statComper = Comparator.comparingInt(o -> ((Stat) o).getVal());
        TreeMap<Stat, Object> sortedStats = new TreeMap<>(statComper);
        sortedStats.putAll(stats);
        for (Map.Entry<Stat, Object> entry : sortedStats.entrySet()) {
            Stat stat = entry.getKey();
            Object value = entry.getValue();
            switch (stat) {
                case skin:
                    outPacket.encodeByte((Byte) value);
                    outPacket.encodeInt(0);
                    break;
                case face:
                case hair:
                case hp:
                case mhp:
                case mp:
                case mmp:
                case pop:
                case charismaEXP:
                case insightEXP:
                case willEXP:
                case craftEXP:
                case senseEXP:
                case charmEXP:
                case eventPoints:
                case level:
                    outPacket.encodeInt((Integer) value);
                    break;
                case str:
                case dex:
                case inte:
                case luk:
                case ap:
                case fatigue:
                    outPacket.encodeShort((Short) value);
                    break;
                case sp:
                    if (value instanceof ExtendSP) {
                        ((ExtendSP) value).encode(outPacket);
                    } else {
                        outPacket.encodeShort((Short) value);
                    }
                    break;
                case exp:
                case money:
                    outPacket.encodeLong((Long) value);
                    break;
                case dayLimit:
                    ((NonCombatStatDayLimit) value).encode(outPacket);
                    break;
                case albaActivity:
                    //TODO
                    break;
                case characterCard:
                    //((CharacterCard) value).encode(outPacket);
                    break;
                case pvp2:
                    outPacket.encodeByte((Byte) value);
                    outPacket.encodeByte((Byte) value);
                    break;
                case job:
                    outPacket.encodeShort((Short) value);
                    outPacket.encodeShort(subJob);
            }
        }
        outPacket.encodeByte(charmOld > 0);
        if (charmOld > 0) {
            outPacket.encodeByte(charmOld);
        }
        outPacket.encodeByte(updateCovery);
        if (updateCovery) {
            outPacket.encodeInt(hpRecovery);
            outPacket.encodeInt(mpRecovery);
        }
        return outPacket;
    }

    public static OutPacket addItemToInventory(Item item) {
        java.util.Optional<OutPacket> profile = net.swordie.ms.connection.protocol.codec.InventoryOperationEncoder.active()
                .flatMap(e -> e.add(item)); // 282: LP008F outside a route; 265: none
        if (profile.isPresent()) {
            return profile.get();
        }
        OutPacket outPacket = new OutPacket(OutHeader.INVENTORY_OPERATION);

        outPacket.encodeByte(true);
        outPacket.encodeByte(0);
        outPacket.encodeInt(1); // size
        outPacket.encodeByte(false);
        outPacket.encodeByte(InventoryOperation.Add.getVal());
        outPacket.encodeByte(item.getInvType().getVal());
        outPacket.encodeShort(item.getBagIndex());
        item.encode(outPacket);

        return outPacket;
    }

    public static OutPacket removeAndAddItem(Item item) {
        java.util.Optional<OutPacket> profile = net.swordie.ms.connection.protocol.codec.InventoryOperationEncoder.active()
                .flatMap(e -> e.removeAndAdd(item)); // 282: LP008F outside a route; 265: none
        if (profile.isPresent()) {
            return profile.get();
        }
        OutPacket outPacket = new OutPacket(OutHeader.INVENTORY_OPERATION);

        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(2); // size
        outPacket.encodeByte(false);
        outPacket.encodeByte(InventoryOperation.Remove.getVal());
        outPacket.encodeByte(item.getInvType().getVal());
        outPacket.encodeShort(item.getBagIndex());
        outPacket.encodeByte(InventoryOperation.Add.getVal());
        outPacket.encodeByte(item.getInvType().getVal());
        outPacket.encodeShort(item.getBagIndex());
        item.encode(outPacket);

        return outPacket;
    }

    public static OutPacket moveItems(Map<Item, Tuple<Short, Short>> items) {
        OutPacket outPacket = new OutPacket(OutHeader.INVENTORY_OPERATION);

        outPacket.encodeByte(true);
        outPacket.encodeByte(true);
        outPacket.encodeInt(items.size());
        outPacket.encodeByte(false);
        for (Map.Entry<Item, Tuple<Short, Short>> entry : items.entrySet()) {
            outPacket.encodeByte(InventoryOperation.Move.getVal());
            outPacket.encodeByte(entry.getKey().getInvType().getVal());
            outPacket.encodeShort(entry.getValue().getLeft());
            outPacket.encodeShort(entry.getValue().getRight());
        }

        return outPacket;
    }

    public static OutPacket changeEquippedInventoryPreset(int preset) {
        OutPacket outPacket = new OutPacket(OutHeader.INVENTORY_OPERATION);

        outPacket.encodeByte(true);
        outPacket.encodeByte(true);
        outPacket.encodeInt(1); // size
        outPacket.encodeByte(false);
        outPacket.encodeByte(InventoryOperation.PresetChange.getVal());
        outPacket.encodeByte(1);
        outPacket.encodeByte(preset);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket changeEquippedItemPreset(Item item, short oldPos, short newPos) {
        OutPacket outPacket = new OutPacket(OutHeader.INVENTORY_OPERATION);

        short posOld = oldPos < 0 ? oldPos : (short) -oldPos;
        short posNew = newPos < 0 ? newPos : (short) -newPos;
        outPacket.encodeByte(true);
        outPacket.encodeByte(true);
        outPacket.encodeInt(2); // size
        outPacket.encodeByte(false);
        outPacket.encodeByte(InventoryOperation.Lock.getVal());
        outPacket.encodeByte(item.getInvType().getVal());
        outPacket.encodeShort(posOld);
        item.encode(outPacket);
        outPacket.encodeByte(InventoryOperation.Move.getVal());
        outPacket.encodeByte(item.getInvType().getVal());
        outPacket.encodeShort(posOld);
        outPacket.encodeShort(posNew);
        outPacket.encodeByte(Util.getRandom(0, 255));
        System.out.println("Old Pos : " + posOld + "; New Pos : " + posNew);

        return outPacket;
    }

    public static OutPacket inventoryOperation(boolean exclRequestSent, boolean notRemoveAddInfo, InventoryOperation type, short oldPos, short newPos, int bagPos, Item item) {
        java.util.Optional<OutPacket> profile = net.swordie.ms.connection.protocol.codec.InventoryOperationEncoder.active()
                .flatMap(e -> e.operation(exclRequestSent, notRemoveAddInfo, type, oldPos, newPos, item)); // 282: LP008F outside a route
        if (profile.isPresent()) {
            return profile.get();
        }
        InvType invType = item.getInvType();
        if ((oldPos > 0 && newPos < 0 && invType == EQUIPPED) || (invType == EQUIPPED && oldPos < 0)) {
            invType = item.isCash() ? InvType.DECORATION : InvType.EQUIP;
        }
        OutPacket outPacket = new OutPacket(OutHeader.INVENTORY_OPERATION);

        outPacket.encodeByte(exclRequestSent);
        outPacket.encodeByte(0);
        outPacket.encodeInt(1); // size
        outPacket.encodeByte(notRemoveAddInfo);
        byte addMovementInfo = 0;
        outPacket.encodeByte(type.getVal());
        outPacket.encodeByte(invType.getVal());
        outPacket.encodeShort(oldPos);
        switch (type) {
            case Add:
            case Lock:
                item.encode(outPacket);
                break;
            case UpdateQuantity:
                outPacket.encodeShort(item.getQuantity());
                break;
            case Move:
            case BagToBag:
                outPacket.encodeShort(newPos);
                if ((invType == InvType.EQUIP || invType == InvType.DECORATION) && (oldPos < 0 || newPos < 0)) {
                    addMovementInfo = 1;
                }
                break;
            case Remove:
                if ((invType == InvType.EQUIP || invType == InvType.DECORATION) && (oldPos < 0 || newPos < 0)) {
                    addMovementInfo = 2;
                }
                break;
            case ItemExp:
                outPacket.encodeLong(((Equip) item).getItemEXP());
                break;
            case UpdateBagPos:
                outPacket.encodeInt(bagPos);
                break;
            case UpdateBagQuantity:
                outPacket.encodeShort(newPos);
                break;
            case BagNewItem:
                item.encode(outPacket);
                break;
            case BagRemove:
            case BagRemoveSlot:
                break;
        }
        if (addMovementInfo != 0) {
            outPacket.encodeByte(addMovementInfo);
        }

        return outPacket;
    }

    public static OutPacket expandInventory(byte invType, byte newSlots) {
        OutPacket outPacket = new OutPacket(OutHeader.INVENTORY_GROW);

        outPacket.encodeByte(invType);
        outPacket.encodeByte(newSlots);

        return outPacket;
    }

    public static OutPacket unk372() {
        return new OutPacket(OutHeader.UNK_372);
    }

    public static OutPacket updateEventNameTag(Char chr, int[] tags) {
        OutPacket outPacket = new OutPacket(OutHeader.EVENT_NAME_TAG);
        //5 is number of max Tag of Character.
        String[] str = chr.getEventNameTag().getsNameTags();
        for (int i = 0; i < 5; i++) {
            outPacket.encodeString(str[i]);
            if (i >= tags.length) {
                outPacket.encodeByte(-1);
            } else {
                //This will show your character current Tag.
                outPacket.encodeByte(tags[i]);
            }
        }
        return outPacket;
    }

    public static OutPacket acquireEventNameTag(int nCategory, int nIdx) {
        OutPacket outPacket = new OutPacket(OutHeader.ACQUIRE_EVENT_NAME_TAG);

        outPacket.encodeByte(nCategory); //nCategory = Category
        outPacket.encodeByte(nIdx); //nIdx = Index

        return outPacket;
    }

    public static OutPacket changeSkillRecordResult(Skill skill) {
        List<Skill> skills = new ArrayList<>();
        skills.add(skill);
        return changeSkillRecordResult(skills, true, false, false);
    }

    public static OutPacket changeSkillRecordResult(List<Skill> skills, boolean exclRequestSent, boolean showResult, boolean sn) {
        OutPacket outPacket = new OutPacket(OutHeader.CHANGE_SKILL_RECORD_RESULT);

        outPacket.encodeByte(exclRequestSent);
        outPacket.encodeByte(showResult);
        outPacket.encodeShort(skills.size());
        for (Skill skill : skills) {
            outPacket.encodeInt(skill.getSkillId());
            outPacket.encodeInt(skill.getCurrentLevel());
            outPacket.encodeInt(skill.getMasterLevel());
            outPacket.encodeFT(FileTime.MIN_TIME());
        }
        outPacket.encodeByte(sn);

        return outPacket;
    }

    public static OutPacket temporaryStatSet(TemporaryStatManager tsm, EnumMap<CharacterTemporaryStat, List<Option>> newStats, int curSize, boolean isHideBuff) {
        var encoded = net.swordie.ms.connection.protocol.codec.ProfileEncoders.find(TemporaryStatEncoder.class)
                .flatMap(e -> Tms282SkillPackets.fromManagerSet(e,newStats,curSize,isHideBuff)
                        .or(() -> Tms282ManagedBuffs.set(e,newStats,curSize,isHideBuff)));
        if(encoded.isPresent()) return encoded.get();
        OutPacket outPacket = new OutPacket(OutHeader.TEMPORARY_STAT_SET);

        boolean hasMovingAffectingStat = newStats.keySet().stream().anyMatch(CharacterTemporaryStat::isMovementAffectingStat);
        tsm.encodeForLocal(outPacket, newStats);

        outPacket.encodeShort(0); // delay hardcored 0
        outPacket.encodeByte(0);
        outPacket.encodeByte(isHideBuff); // isHideBuff

        boolean isTransform = false;
        if (newStats.containsKey(CharacterTemporaryStat.EtherealForm)
                || newStats.containsKey(CharacterTemporaryStat.TransformOverMan)
                || newStats.containsKey(CharacterTemporaryStat.IndieKeyDownTime)
                || newStats.containsKey(CharacterTemporaryStat.MichaelSwordOfLight)
                || newStats.containsKey(CharacterTemporaryStat.RideVehicle)
                || newStats.containsKey(CharacterTemporaryStat.NightLord_SpreadThrow)
                || newStats.containsKey(CharacterTemporaryStat.LWRestore)
                || newStats.containsKey(CharacterTemporaryStat.IceAura)
                || newStats.containsKey(CharacterTemporaryStat.Morph)
                || newStats.containsKey(CharacterTemporaryStat.FireBarrier)
                || newStats.containsKey(CharacterTemporaryStat.FoxBless)
                || newStats.containsKey(CharacterTemporaryStat.BlessEnsenble)
                || newStats.containsKey(CharacterTemporaryStat.LefWarriorNobility)) {
            isTransform = true;
        }
        outPacket.encodeByte(isTransform);
        outPacket.encodeByte(true);
        outPacket.encodeByte(true);
        boolean unk = false;
        if (newStats.containsKey(CharacterTemporaryStat.RenPlumSwordEx)
                || newStats.containsKey(CharacterTemporaryStat.RenPlumSwordForm3Shoot)) {
            unk = true;
        }
        outPacket.encodeByte(hasMovingAffectingStat || unk);
        if (hasMovingAffectingStat) {
            outPacket.encodeByte(Util.getRandom(0, 100));
        }
        outPacket.encodeInt(0);
        outPacket.encodeByte(curSize); // buff size
        outPacket.encodeInt(-1);

        return outPacket;
    }

    public static OutPacket temporaryStatReset(TemporaryStatManager tsm, EnumMap<CharacterTemporaryStat, List<Option>> removeStats, int curSize) {
        var encoded = net.swordie.ms.connection.protocol.codec.ProfileEncoders.find(TemporaryStatEncoder.class)
                .flatMap(e -> Tms282SkillPackets.fromManagerReset(e,removeStats,tsm.getCurrentStats(),curSize)
                        .or(() -> Tms282ManagedBuffs.reset(e,removeStats,tsm.getCurrentStats(),curSize)));
        if(encoded.isPresent()) return encoded.get();
        OutPacket outPacket = new OutPacket(OutHeader.TEMPORARY_STAT_RESET);

        outPacket.encodeByte(true);
        outPacket.encodeByte(true); // ?
        outPacket.encodeByte(curSize); // buff size
        outPacket.encodeByte(tsm.hasRemovedMovingEffectingStat(removeStats));
        for (int i : tsm.getMaskByCollection(removeStats)) {
            outPacket.encodeInt(i);
        }
        tsm.encodeIndieTempStat(outPacket, removeStats);
        if (tsm.hasRemovedMovingEffectingStat(removeStats)) {
            outPacket.encodeByte(1);
        }
        if (removeStats.containsKey(CharacterTemporaryStat.PoseType)) { // 361
            outPacket.encodeByte(1);
        }
        if (removeStats.containsKey(CharacterTemporaryStat.RideVehicle)) { // 622
            boolean bool = false;
            outPacket.encodeByte(bool);
            if (bool) {
                outPacket.encodeInt(0);
            }
        }
        if (removeStats.containsKey(CharacterTemporaryStat.RideVehicle)) { // 361
            outPacket.encodeByte(0);
        }
        outPacket.encodeInt(-1);

        return outPacket;
    }

    public static OutPacket forcedStatSet(Map<ForcedStat, Object> forcedStats) {
        OutPacket outPacket = new OutPacket(OutHeader.FORCED_STAT_SET);

        int updateMask = 0;
        for (var entry : forcedStats.entrySet()) {
            updateMask |= entry.getKey().getVal();
        }
        outPacket.encodeInt(updateMask);
        for (var entry : forcedStats.entrySet()) {
            var stat = entry.getKey();
            var value = entry.getValue();
            switch (stat) {
                case speed:
                case jump:
                case speedMax:
                case optOff:
                case jumpMax:
                    outPacket.encodeByte((byte) value);
                    break;
                case Unk3:
                case addMHP:
                case speedDec:
                case Unk4:
                    outPacket.encodeInt((int) value);
                    break;
                default:
                    outPacket.encodeShort((short) value);
                    break;
            }
        }

        return outPacket;
    }

    public static OutPacket forcedStatReset() {
        return new OutPacket(OutHeader.FORCED_STAT_RESET);
    }

    public static OutPacket mobDropMesoPickUp(int mesos) {
        OutPacket outPacket = new OutPacket(OutHeader.MOB_DROP_MESO_PICKUP);

        outPacket.encodeInt(mesos);

        return outPacket;
    }

    public static OutPacket updateAchievements(Set<AchievementData> achievementDataSet) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(ACHIEVEMENT_INIT.getVal());
        outPacket.encodeInt(achievementDataSet.size());
        for (AchievementData achievementData : achievementDataSet) {
            achievementData.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket updateAchievement(AchievementData achievementData) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(ACHIEVEMENT_INIT.getVal());
        outPacket.encodeInt(1); // size
        achievementData.encode(outPacket);

        return outPacket;
    }

    public static OutPacket updateAchievement(Set<AchievementData> datas) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(ACHIEVEMENT_INIT.getVal());
        outPacket.encodeInt(datas.size()); // size
        for (AchievementData data : datas) {
            data.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket achievementMessage(int infoID) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(ACHIEVEMENT_DATA_MESSAGE.getVal());
        outPacket.encodeInt(1); // hardcorded 1
        outPacket.encodeInt(infoID);

        return outPacket;
    }

    public static OutPacket updateAchievementRank(Set<AchievementRank> ranks) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(ACHIEVEMENT_RANK_MESSAGE.getVal());
        outPacket.encodeInt(ranks.size()); // size
        for (AchievementRank data : ranks) {
            outPacket.encodeInt(0);
            data.encode(outPacket);
        }


        return outPacket;
    }

    public static OutPacket dropPickupMessage(int money, short internetCafeExtra, short smallChangeExtra) {
        return dropPickupMessage(money, (byte) 1, internetCafeExtra, smallChangeExtra, (short) 0);
    }

    public static OutPacket dropPickupMessage(Item item, short quantity) {
        return dropPickupMessage(item.getItemId(), (byte) 0, (short) 0, (short) 0, quantity);
    }

    public static OutPacket dropPickupMessage(int itemID, short quantity) {
        return dropPickupMessage(itemID, (byte) 0, (short) 0, (short) 0, quantity);
    }

    public static OutPacket dropPickupMessage(int i, byte type, short internetCafeExtra, short smallChangeExtra, short quantity) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(DROP_PICKUP_MESSAGE.getVal());
        outPacket.encodeInt(0); // delay
        outPacket.encodeByte(0); // new 200
        outPacket.encodeByte(type);
        switch (type) {
            case 1: // Mesos
                outPacket.encodeByte(false); // boolean: portion was lost after falling to the ground
                outPacket.encodeInt(i); // Mesos
                outPacket.encodeShort(smallChangeExtra); // Spotting small change
                outPacket.encodeInt(0);
                break;
            case 0: // item
                outPacket.encodeInt(i);
                outPacket.encodeInt(quantity); // ?
                outPacket.encodeByte(0);
                break;
        }

        return outPacket;
    }

    public static OutPacket questRecordMessage(Quest quest) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(QUEST_RECORD_MESSAGE.getVal());
        outPacket.encodeInt(quest.getQRKey());
        QuestStatus state = quest.getStatus();
        outPacket.encodeByte(state.getVal());
        switch (state) {
            case NotStarted:
                outPacket.encodeByte(0); // If quest is completed, but should never be true?
                break;
            case Started:
                outPacket.encodeString(quest.getQRValue());
                break;
            case Completed:
                outPacket.encodeFT(quest.getCompletedTime());
                break;
        }

        return outPacket;
    }

    public static OutPacket questRecordMessage(AccountQuest quest) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(QUEST_RECORD_MESSAGE.getVal());
        outPacket.encodeInt(quest.getQRKey());
        QuestStatus state = quest.getStatus();
        outPacket.encodeByte(state.getVal());
        switch (state) {
            case NotStarted:
                outPacket.encodeByte(0); // If quest is completed, but should never be true?
                break;
            case Started:
                outPacket.encodeString(quest.getQRValue());
                break;
            case Completed:
                outPacket.encodeFT(quest.getCompletedTime());
                break;
        }

        return outPacket;
    }

    public static OutPacket questRecordExMessage(Quest quest) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(QUEST_RECORD_EX_MESSAGE.getVal());
        outPacket.encodeInt(quest.getQRKey());
        outPacket.encodeString(quest.getQRValue());

        return outPacket;
    }

    public static OutPacket questRecordExMessage(AccountQuest quest) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(QUEST_RECORD_EX_MESSAGE.getVal());
        outPacket.encodeInt(quest.getQRKey());
        outPacket.encodeString(quest.getQRValue());

        return outPacket;
    }

    public static OutPacket questWorldShareMessage(AccountQuest quest) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(WORLD_SHARE_RECORD_MESSAGE.getVal());
        outPacket.encodeInt(quest.getQRKey());
        outPacket.encodeString(quest.getQRValue());

        return outPacket;
    }

    public static OutPacket nxRecordMessage(int nxRecordID, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(NX_RECORD_MESSAGE.getVal());
        outPacket.encodeInt(nxRecordID);
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket incExpMessage(ExpIncreaseInfo eii) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(INC_EXP_MESSAGE.getVal());
        eii.encode(outPacket);

        return outPacket;
    }

    public static OutPacket incSpMessage(short job, byte amount) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(INC_SP_MESSAGE.getVal());
        outPacket.encodeShort(job);
        outPacket.encodeByte(amount);

        return outPacket;
    }

    public static OutPacket incMoneyMessage(int amount) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(INC_MONEY_MESSAGE.getVal());
        outPacket.encodeLong(amount);
        int type = amount > 0 ? 1 : -1;
        outPacket.encodeInt(type);
        if (type == 24) {
            outPacket.encodeString("");
        }

        return outPacket;
    }

    public static OutPacket incGPMessage(int amount) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(INC_GP_MESSAGE.getVal());
        outPacket.encodeInt(amount);

        return outPacket;
    }

    public static OutPacket giveBuffMessage(int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(GIVE_BUFF_MESSAGE.getVal());
        outPacket.encodeInt(itemID);

        return outPacket;
    }

    /**
     * Returns a net.swordie.ms.connection.packet for messages with the following {@link MessageType}:<br>
     * GENERAL_ITEM_EXPIRE_MESSAGE<br>
     * ITEM_PROTECT_EXPIRE_MESSAGE<br>
     * ITEM_ABILITY_TIME_LIMITED_EXPIRE_MESSAGE<br>
     * SKILL_EXPIRE_MESSAGE
     *
     * @param mt    The message type.
     * @param items The list of ints that should be encoded.
     * @return The message OutPacket.
     */
    public static OutPacket message(MessageType mt, List<Integer> items) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(mt.getVal());
        switch (mt) {
            case GENERAL_ITEM_EXPIRE_MESSAGE:
            case ITEM_PROTECT_EXPIRE_MESSAGE:
            case ITEM_ABILITY_TIME_LIMITED_EXPIRE_MESSAGE:
            case SKILL_EXPIRE_MESSAGE:
                outPacket.encodeByte(items.size());
                items.forEach(outPacket::encodeInt);
                break;
        }
        return outPacket;
    }

    public static OutPacket skillExpireMessage(List<Integer> skills) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(GIVE_BUFF_MESSAGE.getVal());
        outPacket.encodeByte(skills.size());
        for (Integer skillID : skills) {
            outPacket.encodeInt(skillID);
        }
        // %s has disappeared as the time limit has passed.

        return outPacket;
    }

    public static OutPacket itemExpireReplaceMessage(List<String> strings) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(ITEM_EXPIRE_REPLACE_MESSAGE.getVal());
        outPacket.encodeByte(strings.size());
        strings.forEach(outPacket::encodeString);

        return outPacket;
    }

    public static OutPacket incNonCombatStatEXPMessage(Stat trait, int amount) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(INC_NON_COMBAT_STAT_EXP_MESSAGE.getVal());
        long mask = 0;
        mask |= trait.getVal();
        outPacket.encodeLong(mask);
        outPacket.encodeInt(amount);

        return outPacket;
    }

    public static OutPacket LimitNonCombatStatEXPMessage(Stat trait) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(LIMIT_NON_COMBAT_STAT_EXP_MESSAGE.getVal());
        long mask = 0;
        mask |= trait.getVal();
        outPacket.encodeLong(mask);

        return outPacket;
    }

    public static OutPacket androidMachineHeartAlsetMessage() {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(ANDROID_MACHINE_HEART_ALSET_MESSAGE.getVal());

        return outPacket;
    }

    public static OutPacket noticeAutoLineChanged(String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(NOTICE_AUTO_LINE_CHANGED.getVal());
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket incWPMessage(int amount) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(INC_WP_MESSAGE.getVal());
        outPacket.encodeInt(amount);

        return outPacket;
    }

    public static OutPacket maxWPMessage() {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(MAX_WP_MESSAGE.getVal());

        return outPacket;
    }

    public static OutPacket whisperMessage(String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(WHISPER_MESSAGE.getVal());
        outPacket.encodeString(msg);

        return outPacket;
    }

    /**
     * Returns a net.swordie.ms.connection.packet for messages with the following {@link MessageType}:<br>
     * int: <br>
     * CASH_ITEM_EXPIRE_MESSAGE<br>
     * INC_POP_MESSAGE<br>
     * GIVE_BUFF_MESSAGE<br><br>
     * int + byte: <br>
     * INC_COMMITMENT_MESSAGE<br><br>
     * String: <br>
     * SYSTEM_MESSAGE<br><br>
     * int + String: <br>
     * QUEST_RECORD_EX_MESSAGE<br>
     * WORLD_SHARE_RECORD_MESSAGE<br>
     *
     * @param mt     The message type.
     * @param i      The integer to encode.
     * @param string The String to encode.
     * @param type   The type (byte) to encode.
     * @return The message OutPacket.
     */
    public static OutPacket message(MessageType mt, int i, String string, byte type) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(mt.getVal());
        switch (mt) {
            case CASH_ITEM_EXPIRE_MESSAGE:
            case INC_POP_MESSAGE:
            case GIVE_BUFF_MESSAGE:
                outPacket.encodeInt(i);
                break;
            case INC_COMMITMENT_MESSAGE:
                outPacket.encodeInt(i);
                outPacket.encodeInt(i < 0 ? 1 : i == 0 ? 2 : 0); // gained = 0, lost = 1, cap = 2
                outPacket.encodeInt(0);
                break;
            case SYSTEM_MESSAGE:
                outPacket.encodeString(string);
                break;
            case QUEST_RECORD_EX_MESSAGE:
            case WORLD_SHARE_RECORD_MESSAGE:
            case COLLECTION_RECORD_MESSAGE:
            case NX_RECORD_MESSAGE:
                outPacket.encodeInt(i);
                outPacket.encodeString(string);
                break;
            case INC_HARDCORE_EXP_MESSAGE:
                outPacket.encodeInt(i); //You have gained x EXP
                outPacket.encodeInt(i); //Field Bonus Exp
                break;
            case BARRIER_EFFECT_IGNORE_MESSAGE:
                outPacket.encodeByte(type); //protection/shield scroll pop-up Message
                break;
        }

        return outPacket;
    }

    public static OutPacket givePopularityResult(PopularityResultType prType, Char targetChr, int newFame, boolean inc) {
        OutPacket outPacket = new OutPacket(OutHeader.GIVE_POPULARITY_RESULT);

        outPacket.encodeByte(prType.getVal());

        switch (prType) {
            case Success:
                outPacket.encodeString(targetChr.getName());
                outPacket.encodeByte(inc); // true = fame  |  false = defame
                outPacket.encodeInt(newFame);
                break;

            case InvalidCharacterId:
            case LevelLow:
            case AlreadyDoneToday:
            case AlreadyDoneTarget:
                break;

            case Notify:
                outPacket.encodeString(targetChr.getName());
                outPacket.encodeByte(inc); // true = fame  |  false = defame
                break;
        }

        return outPacket;
    }

    public static OutPacket mapTransferResult(MapTransferType mapTransferType, byte itemType, int[] hyperrockfields) {
        OutPacket outPacket = new OutPacket(OutHeader.MAP_TRANSFER_RESULT);

        outPacket.encodeByte(mapTransferType.getVal()); // Map Transfer Type
        outPacket.encodeByte(itemType); // Item Type (5 = Cash)
        if (mapTransferType == MapTransferType.DeleteListSend || mapTransferType == MapTransferType.RegisterListSend) {
            for (int fieldid : hyperrockfields) {
                outPacket.encodeInt(fieldid); // Target Field ID
            }
        }

        return outPacket;
    }

    public static OutPacket antiMacroResult(final byte[] image, byte notificationType, byte antiMacroType) {
        return antiMacroResult(image, notificationType, antiMacroType, (byte) 0, (byte) 1);
    }

    public static OutPacket antiMacroResult(final byte[] image, byte notificationType, byte antiMacroType, byte first, byte refreshAntiMacroCount) {
        OutPacket outPacket = new OutPacket(OutHeader.ANTI_MACRO_RESULT);

        outPacket.encodeByte(notificationType);
        outPacket.encodeByte(antiMacroType);
        if (notificationType == AntiMacro.AntiMacroResultType.AntiMacroRes.getVal()) {
            outPacket.encodeByte(first);
            outPacket.encodeByte(refreshAntiMacroCount);
            if (image == null) {
                outPacket.encodeInt(0);
            } else {
                outPacket.encodeInt(image.length);
                outPacket.encodeArr(image);
            }
        } else if (notificationType == AntiMacro.AntiMacroResultType.AntiMacroRes_Fail.getVal() ||
                notificationType == AntiMacro.AntiMacroResultType.AntiMacroRes_Success.getVal()) {
            outPacket.encodeString(""); // unused?
        }

        return outPacket;
    }

    public static OutPacket antiMacroBombResult(byte notificationType, int fieldID, int channelID) {
        OutPacket outPacket = new OutPacket(OutHeader.ANTI_MACRO_BOMB_RESULT);

        outPacket.encodeByte(notificationType);
        outPacket.encodeInt(fieldID);
        outPacket.encodeByte(channelID); // channelID = -1 => Using Bomb Lie Detector on field %s in all channels.

        return outPacket;
    }

    public static OutPacket claimResult(ClaimResultType type) {
        OutPacket outPacket = new OutPacket(OutHeader.CLAIM_RESULT);

        outPacket.encodeByte(type.getVal());

        return outPacket;
    }

    public static OutPacket claimResult(ClaimResultType type, boolean isSuccess, int reportCount) {
        OutPacket outPacket = new OutPacket(OutHeader.CLAIM_RESULT);

        outPacket.encodeByte(type.getVal());
        if (type.equals(ClaimResultType.Success)) {
            outPacket.encodeByte(isSuccess);
            outPacket.encodeInt(reportCount);
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket setClaimSVRAvailableTime(int ClaimSvrOpenTime, int ClaimSvrCloseTime) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_CLAIM_SVR_AVAILABLE_TIME);

        outPacket.encodeByte(ClaimSvrOpenTime);
        outPacket.encodeByte(ClaimSvrCloseTime);

        return outPacket;
    }

    public static OutPacket claimSVRStatusChanged(boolean enabled) {
        OutPacket outPacket = new OutPacket(OutHeader.CLAIM_SVR_STATUS_CHANGED);

        outPacket.encodeByte(enabled);

        return outPacket;
    }

    public static OutPacket modComboResponse(int combo) {
        OutPacket outPacket = new OutPacket(OutHeader.MOD_COMBO_RESPONSE);

        outPacket.encodeInt(combo);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket incComboResponseByComboRecharge(int combo) {
        OutPacket outPacket = new OutPacket(OutHeader.INC_COMBO_RESPONSE_BY_COMBO_RECHARGE);

        outPacket.encodeInt(combo);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket wildHunterInfo(WildHunterInfo whi) {
        OutPacket outPacket = new OutPacket(OutHeader.WILD_HUNTER_INFO);

        whi.encode(outPacket);

        return outPacket;
    }

    public static OutPacket zeroInfo(ZeroInfo currentInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.ZERO_INFO);

        currentInfo.encode(outPacket);

        return outPacket;
    }

    public static OutPacket zeroUpdate(int nWP) {
        OutPacket outPacket = new OutPacket(OutHeader.ZERO_WP);

        outPacket.encodeInt(nWP);

        return outPacket;
    }

    public static OutPacket zeroInfoSubHP(ZeroInfo currentInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.ZERO_INFO_SUB_HP);

        outPacket.encodeByte(currentInfo.isZeroBetaState() ? 1 : 0);
        outPacket.encodeInt(currentInfo.getSubHP());

        return outPacket;
    }

    public static OutPacket gatherItemResult(byte type) {
        OutPacket outPacket = new OutPacket(OutHeader.GATHER_ITEM_RESULT);

        outPacket.encodeByte(1); // doesn't get used
        outPacket.encodeByte(type);

        return outPacket;
    }

    public static OutPacket sortItemResult(byte type) {
        OutPacket outPacket = new OutPacket(OutHeader.SORT_ITEM_RESULT);

        outPacket.encodeByte(1); // doesn't get used
        outPacket.encodeByte(type);

        return outPacket;
    }

    public static OutPacket partyResult(PartyResult pri) {
        OutPacket outPacket = new OutPacket(OutHeader.PARTY_RESULT);

        outPacket.encode(pri);

        return outPacket;
    }

    public static OutPacket partyMemberCandidateResult(Set<Char> chars) {
        OutPacket outPacket = new OutPacket(OutHeader.PARTY_MEMBER_CANDIDATE_RESULT);

        outPacket.encodeByte(chars.size());
        for (Char chr : chars) {
            outPacket.encodeInt(chr.getId());
            outPacket.encodeString(chr.getName());
            outPacket.encodeShort(chr.getJob());
            outPacket.encodeShort(chr.getAvatarData().getCharacterStat().getSubJob());
            outPacket.encodeInt(chr.getLevel());
        }

        return outPacket;
    }

    public static OutPacket partyCandidateResult(Set<Party> parties) {
        OutPacket outPacket = new OutPacket(OutHeader.PARTY_CANDIDATE_RESULT);

        outPacket.encodeByte(parties.size());
        for (Party party : parties) {
            Char leader = party.getPartyLeader().getChr();
            outPacket.encodeInt(party.getId());
            outPacket.encodeString(leader.getName());
            outPacket.encodeByte(party.getAvgPartyLevel());
            outPacket.encodeByte(party.getMembers().size());
            outPacket.encodeString(party.getName());
            outPacket.encodeByte(party.getMembers().size());
            for (PartyMember pm : party.getMembers()) {
                outPacket.encodeInt(pm.getCharID());
                outPacket.encodeString(pm.getCharName());
                outPacket.encodeShort(pm.getJob());
                outPacket.encodeShort(pm.getSubJob());
                outPacket.encodeByte(pm.getLevel());
                outPacket.encodeByte(pm.equals(party.getPartyLeader()));
            }
        }
        outPacket.encodeArr(new byte[40]);

        return outPacket;
    }

    public static OutPacket matrixSPWResult(boolean succeed) {
        OutPacket outPacket = new OutPacket(OutHeader.MATRIX_SPW_RESULT);
        outPacket.encodeInt(3);
        outPacket.encodeByte(succeed);
        return outPacket;
    }

    public static OutPacket guildResult(GuildResult gri) {
        OutPacket outPacket = new OutPacket(OutHeader.GUILD_RESULT);

        gri.encode(outPacket);

        return outPacket;
    }

    public static OutPacket guildSearchResult(Char chr, Collection<Guild> guilds, int mode, String text, int option) {
        OutPacket outPacket = new OutPacket(OutHeader.GUILD_SEARCH_RESULT);

        outPacket.encodeByte(mode);
        outPacket.encodeString(text);
        outPacket.encodeByte(option);
        outPacket.encodeByte(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(guilds.size());
        for (Guild g : guilds) {
            outPacket.encodeInt(g.getId());
            outPacket.encodeByte(g.getLevel());
            outPacket.encodeString(g.getName());
            outPacket.encodeString(g.getGuildLeader().getName());
            outPacket.encodeShort(g.getMembers().size());
            outPacket.encodeShort(g.getAverageMemberLevel());
            outPacket.encodeByte(g.getRequestors().stream().filter(gr -> gr.getCharID() == chr.getId()).findFirst().orElse(null) != null);
            outPacket.encodeLong(0L);
            outPacket.encodeByte(1);
            outPacket.encodeString(g.getNotice() != null ? g.getNotice() : "Chào möng các b¢n «ªn vÜi Bang hØi " + g.getName() + ".");
            outPacket.encodeInt(135);
            outPacket.encodeInt(55);
            outPacket.encodeInt(14);
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket getLotteryResult(String address) {
        OutPacket outPacket = new OutPacket(OutHeader.GET_LOTTERY_RESULT);

        outPacket.encodeString(address);

        return outPacket;
    }

    public static OutPacket checkProcessResult(boolean isFromLogin) {
        OutPacket outPacket = new OutPacket(OutHeader.CHECK_PROCESS_RESULT);

        outPacket.encodeByte(isFromLogin);
        outPacket.encodeByte(0);

        return outPacket;
    }


    public static OutPacket allianceResult(AllianceResult ar) {
        OutPacket outPacket = new OutPacket(OutHeader.ALLIANCE_RESULT);

        outPacket.encode(ar);

        return outPacket;
    }

    public static OutPacket guildBBSResult(GuildBBSPacket gbp) {
        OutPacket outPacket = new OutPacket(OutHeader.GUILD_BBS_RESULT);

        outPacket.encode(gbp);

        return outPacket;
    }

    public static OutPacket flameWizardFlameWalkEffect(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.FLAME_WIZARD_FLAME_WALK_EFFECT);

        outPacket.encodeInt(chr.getId());

        return outPacket;
    }

    public static OutPacket crusaderCodexResult(int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.CRUSADER_CODEX_RESULT);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(0);
        outPacket.encodeInt(1);
        outPacket.encodeInt(1132111);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket flameWizardFlareBlink(Char chr, Position newPosition, boolean used) {
        OutPacket outPacket = new OutPacket(OutHeader.FLAME_WIZARD_FLARE_BLINK);

        Position zero = new Position(0, 0);
        outPacket.encodeInt(chr.getId()); //chr?
        outPacket.encodeByte(used); //used?
        if (!used) {
            //Blink - Set Position
            outPacket.encodeByte(used);
            outPacket.encodeShort(1);
            outPacket.encodePosition(newPosition); //2x encode Short (x/y)
            outPacket.encodePosition(zero); //2x encode Short (x/y)
        }

        return outPacket;
    }

    /** Built by the active profile's FriendPacketEncoder (GMS 265: Gms265FriendPackets, the former body). */
    public static OutPacket friendResult(FriendResult friendResult) {
        return net.swordie.ms.connection.protocol.codec.FriendPacketEncoder.active().friendResult(friendResult);
    }

    public static OutPacket setCustomNickNameResult(Char chr, int type) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_CUSTOM_NICK_NAME_RESULT);

        chr.encodeCustomNickName(outPacket);
        outPacket.encodeInt(type);
        // TODO

        return outPacket;
    }

    public static OutPacket setCustomNickNameUpdate(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_CUSTOM_NICK_NAME_UPDATE);

        chr.encodeCustomNickName(outPacket);

        return outPacket;
    }

    public static OutPacket macroSysDataInit(List<Macro> macros) {
        OutPacket outPacket = new OutPacket(OutHeader.MACRO_SYS_DATA_INIT);

        outPacket.encodeByte(macros.size());
        for (Macro macro : macros) {
            macro.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket monsterBookSetCard(int id) {
//        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_LIFE_INVITE_ITEM_RESULT);
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_BOOK_SET_CARD);

        outPacket.encodeByte(id > 0); // false -> already added msg
        if (id > 0) {
            outPacket.encodeInt(id);
            outPacket.encodeInt(1); // card count, but we're just going to stuck with 1.
        }

        return outPacket;
    }

    public static OutPacket characterPotentialSet(Char chr, boolean exclRequest, boolean changed, int preset) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_POTENTIAL_SET);

        outPacket.encodeByte(exclRequest);
        outPacket.encodeByte(changed);
        if (changed) {
            final Set<CharacterPotential> characterPotentialSet = chr.getPotentialsByPreset(preset);
            outPacket.encodeShort(characterPotentialSet.size());
            for (CharacterPotential cp : characterPotentialSet) {
                cp.encode(outPacket);
            }
        }

        return outPacket;
    }

    public static OutPacket characterPotentialReset(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_POTENTIAL_RESET);

        for (int i = 0; i < 3; i++) {
            final Set<CharacterPotential> characterPotentialSet = chr.getPotentialsByPreset(i);
            outPacket.encodeShort(characterPotentialSet.size());
            for (CharacterPotential cp : characterPotentialSet) {
                cp.encode(outPacket);
            }
        }

        return outPacket;
    }

    public static OutPacket characterPotentialChangePreset(boolean exclRequest, int newPreset) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_POTENTIAL_CHANGE_PRESET);

        outPacket.encodeByte(exclRequest);
        outPacket.encodeByte(newPreset);

        return outPacket;
    }

    public static OutPacket characterHonorExp(int exp) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_HONOR_EXP);

        outPacket.encodeInt(exp);

        return outPacket;
    }

    public static OutPacket characterHonorGift(int emptyUseSlot, int amount) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_HONOR_GIFT);

        outPacket.encodeInt(emptyUseSlot);
        outPacket.encodeInt(amount);

        return outPacket;
    }

    public static OutPacket showEventNotice(int questID, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_EVENT_NOTICE);

        outPacket.encodeInt(questID);
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket cashPetPickUpOnOffResult(int status) {
        OutPacket outPacket = new OutPacket(OutHeader.CASHPET_PICK_UP_ON_OFF_RESULT);

        outPacket.encodeByte(status);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket setSonOfLinkedSkillResult(LinkedSkillResultType lsrt, int sonID, String sonName,
                                                      int originalSkillID, String existingParentName) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_SON_OF_LINKED_SKILL_RESULT);

        outPacket.encodeInt(lsrt.getVal());
        outPacket.encodeInt(originalSkillID);
        switch (lsrt) {
            case SetSonOfLinkedSkillResult_Success:
                outPacket.encodeInt(sonID);
                outPacket.encodeString(sonName);
                break;
            case SetSonOfLinkedSkillResult_Fail_ParentAlreadyExist:
                outPacket.encodeString(existingParentName);
                outPacket.encodeString(sonName);
                break;
            case SetSonOfLinkedSkillResult_Fail_Unknown:
                break;
            case SetSonOfLinkedSkillResult_Fail_MaxCount:
                outPacket.encodeString(existingParentName);
                break;
            case SetSonOfLinkedSkillResult_Fail_DBRequestFail:
                break;
        }

        return outPacket;
    }

    public static OutPacket setLinkedSkillResult(int skillID, LinkedSkillResultType lsrt) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_LINKED_SKILL_RESULT);

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(lsrt.getVal());

        return outPacket;
    }

    public static OutPacket unlinkedSkillInfo(LinkSkill linkSkill) {
        return unlinkedSkillInfo(Collections.singleton(linkSkill));
    }

    public static OutPacket unlinkedSkillInfo(Set<LinkSkill> linkSkills) {
        OutPacket outPacket = new OutPacket(OutHeader.UNLINKED_SKILL_INFO);

        outPacket.encodeInt(linkSkills.size());
        for (LinkSkill ls : linkSkills) {
            outPacket.encodeInt(ls.getLinkSkillID());
            outPacket.encodeInt(ls.getOwnerID());
        }

        return outPacket;
    }

    public static OutPacket linkedSkillInfo(byte preset, List<LinkedSkill> linkSkills) {
        OutPacket outPacket = new OutPacket(OutHeader.LINKED_SKILL_INFO);

        outPacket.encodeInt(0);
        outPacket.encodeByte(preset);
        outPacket.encodeInt(linkSkills.size());
        for (LinkedSkill linkedSkill : linkSkills) {
            outPacket.encodeInt(linkedSkill.getSkillID());
        }

        return outPacket;
    }

    public static OutPacket linkedSkillInfo(int type, byte preset, List<Integer> linkSkills) {
        OutPacket outPacket = new OutPacket(OutHeader.LINKED_SKILL_INFO);

        outPacket.encodeInt(type);
        if (type == 0) { // init Preset
            outPacket.encodeByte(preset);
            outPacket.encodeInt(linkSkills.size());
            for (int i : linkSkills) {
                outPacket.encodeInt(i);
            }
        } else if (type == 1) {
            outPacket.encodeByte(preset); // change Preset
        } else if (type == 2 || type == 3) { // add / remove skill
            outPacket.encodeByte(preset);
            outPacket.encodeInt(linkSkills.getFirst());
        }

        return outPacket;
    }

    public static OutPacket returnEffectConfirm(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.RETURN_EFFECT_CONFIRM.getValue());

        var equip = chr.returnEffectInfo.equip;
        outPacket.encodeByte(false);
        outPacket.encodeLong(equip != null ? equip.getId() : -1);
        chr.returnEffectInfo.encode(outPacket);

        return outPacket;
    }

    public static OutPacket returnEffectModified(Equip equip, int scrollID) {
        OutPacket outPacket = new OutPacket(OutHeader.RETURN_EFFECT_MODIFIED.getValue());

        outPacket.encodeByte(equip != null);
        if (equip != null) {
            equip.encode(outPacket);
            outPacket.encodeInt(scrollID);
        }

        return outPacket;
    }

    public static OutPacket whiteCubeResult(Equip equip, MemorialCubeInfo mci, int cubeCount) {
        OutPacket outPacket = new OutPacket(OutHeader.WHITE_ADDTIONAL_CUBE_RESULT);

        outPacket.encodeLong(equip.getId());
        mci.encode(outPacket);
        outPacket.encodeInt(cubeCount);
        outPacket.encodeInt(equip.getBagIndex());
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket blackCubeResult(Equip equip, MemorialCubeInfo mci, int cubeCount) {
        OutPacket outPacket = new OutPacket(OutHeader.BLACK_CUBE_RESULT);

        outPacket.encodeLong(equip.getId());
        mci.encode(outPacket);
        outPacket.encodeInt(cubeCount);
        outPacket.encodeInt(equip.getBagIndex());
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket memorialCubeResult(Item item, MemorialCubeInfo mci) {
        OutPacket outPacket = new OutPacket(OutHeader.MEMORIAL_CUBE_RESULT);

        outPacket.encodeLong(item.getId());
        mci.encode(outPacket);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket memorialCubeModified(MemorialCubeInfo mci) {
        OutPacket outPacket = new OutPacket(OutHeader.MEMORIAL_CUBE_MODIFIED);

        mci.encode(outPacket);

        return outPacket;
    }

    public static OutPacket blackFlameOfResurrectionModifed() {
        OutPacket outPacket = new OutPacket(OutHeader.BLACK_FLAME_OF_RESURRECTION_MODIFIED);

        outPacket.encodeInt(2);

        return outPacket;
    }

    public static OutPacket blackFlameOfResurrectionModifed(int type, Equip equip, Equip copy, int flameID, short tier) {
        OutPacket outPacket = new OutPacket(OutHeader.BLACK_FLAME_OF_RESURRECTION_MODIFIED);

        outPacket.encodeInt(type);
        if (type == 1) {
            // MemorialFlame::Decode
            outPacket.encodeByte(1);
            outPacket.encodeLong(equip.getId());
            outPacket.encodeShort(equip.getBagIndex());
            outPacket.encodeInt(flameID); // Karma Black Rebirth Flame => 6,7
            outPacket.encodeShort(tier); // tier
            outPacket.encodeByte(1);
            outPacket.encodeLong(Util.getRandom(74242, 34244104074L)); // 64123203
            outPacket.encodeInt(0);
            outPacket.encodeByte(1);
            copy.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket dressUpInfoModified(DressUpInfo dressUpInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.DRESS_UP_INFO_MODIFIED);

        dressUpInfo.encode(outPacket);

        return outPacket;
    }

    public static OutPacket miracleCirculatorResult(List<CharacterPotentialValueHolder> cps, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.MIRACLE_CIRCULATOR_RESULT);

        outPacket.encodeInt(cps.size());
        for (CharacterPotentialValueHolder cp : cps) {
            outPacket.encodeInt(cp.getSkillID());
            outPacket.encodeByte(cp.getSlv());
            outPacket.encodeByte(cp.getKey());
            outPacket.encodeByte(cp.getGrade());
        }
        outPacket.encodeInt(itemID);
        outPacket.encodeFT(FileTime.currentTime());
        outPacket.encodeByte(1); // v263
        outPacket.encodeByte(0); // v263
        outPacket.encodeInt(4);

        return outPacket;
    }

    public static OutPacket broadcastMsg(BroadcastMsg broadcastMsg) {
        OutPacket outPacket = new OutPacket(OutHeader.BROADCAST_MSG);

        broadcastMsg.encode(outPacket);

        return outPacket;
    }

    public static OutPacket incubatorResult(int rewardID, int bonusItemID, int gachaponItemID, int itemID, boolean haveIncubator) {
        OutPacket outPacket = new OutPacket(OutHeader.INCUBATOR_RESULT);

        outPacket.encodeInt(rewardID);
        outPacket.encodeShort(bonusItemID);
        outPacket.encodeInt(gachaponItemID);
        outPacket.encodeInt(0);
        outPacket.encodeInt(itemID);
        outPacket.encodeInt(haveIncubator ? 1 : 0);
        outPacket.encodeByte(0);
        boolean bool = false;
        outPacket.encodeByte(bool);
        if (bool) {
            // item.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket incubatorHotItemResult(int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.INCUBATOR_HOT_ITEM_RESULT);

        outPacket.encodeInt(itemID); // Congratulations, you obtained <itemID>

        return outPacket;
    }

    public static OutPacket setAvatarMegaphone(Char chr, int megaItemId, List<String> lineList, boolean whisperIcon, boolean bool) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_AVATAR_MEGAPHONE);

        outPacket.encodeInt(megaItemId); // Avatar Megaphone Item ID
        outPacket.encodeString(chr.getName());
        String msg = "";
        for (String line : lineList) {
            outPacket.encodeString(line); // x4
            msg += line;
        }
        chr.encodeChatInfo(outPacket, msg);
        outPacket.encodeInt(chr.getClient().getChannel() - 1);
        outPacket.encodeByte(whisperIcon);
        chr.getAvatarData().getAvatarLook().encode(outPacket); // encode AvatarLook
        outPacket.encodeByte(bool);

        return outPacket;
    }

    public static OutPacket receiveToadsHammerRequestResult(int type, Item fromItem, Item toItem) {
        OutPacket outPacket = new OutPacket(OutHeader.RECEIVE_TOADS_HAMMER_REQUEST_RESULT);

        outPacket.encodeShort(type);
        if (type == 1) {
            fromItem.encode(outPacket);
            outPacket.encodeShort(toItem.getBagIndex());
        } else {
            toItem.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket receiveEquipRentalResult(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.RECEIVE_EQUIP_RENTAL_RESULT);

        outPacket.encodeShort(type);

        return outPacket;
    }

    public static OutPacket receiveHyperStatResetResult(int charID, boolean exclRequest, boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.RECEIVE_HYPER_STAT_RESET_RESULT);

        outPacket.encodeByte(exclRequest);
        outPacket.encodeInt(charID);
        outPacket.encodeByte(success);

        return outPacket;
    }

    public static OutPacket inventoryOperationResult(boolean isChecked, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.INVENTORY_OPERATION_RESULT);

        outPacket.encodeInt(isChecked ? 1 : 0);
        if (isChecked) {
            outPacket.encodeInt(itemID);
        }

        return outPacket;
    }

    public static OutPacket getSavedUrusSkill(List<Integer> savedSkills) {
        OutPacket outPacket = new OutPacket(OutHeader.GET_SAVED_URSUS_SKILL);

        for (Integer savedSkillID : savedSkills) { // size = 8
            outPacket.encodeInt(savedSkillID);
        }

        return outPacket;
    }

    public static OutPacket receiveHyperSkillResetResult(int charID, boolean exclRequest, boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.HYPER_SKILL_RESET_RESULT);

        outPacket.encodeByte(exclRequest);
        outPacket.encodeInt(charID);
        outPacket.encodeByte(success);

        return outPacket;
    }

    public static OutPacket MVPInfo() {
        OutPacket outPacket = new OutPacket(OutHeader.MVP_INFO);

        outPacket.encodeString("https://g.nexonstatic.com/maplestory/ingame/mvp");
        outPacket.encodeString("https://g.nexonstatic.com/maplestory/ingame/mvp");
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(3);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(5);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(10);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(10);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(10);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket monsterCollectionResult(MonsterCollectionResultType mcrt, InvType invType, int fullSlots) {
        OutPacket outPacket = new OutPacket(OutHeader.MONSTER_COLLECTION_RESULT);

        outPacket.encodeInt(mcrt.ordinal());
        if (invType != null) {
            outPacket.encodeInt(invType.getVal());
        } else {
            outPacket.encodeInt(0);
        }
        outPacket.encodeInt(fullSlots);

        return outPacket;
    }

    public static OutPacket towerChairSettingResult() {
        return new OutPacket(OutHeader.TOWER_CHAIR_SETTING_RESULT);
    }

    /**
     * Dùng cho khi muốn modify bất kì thứ gì trong Chr
     *
     * @param chr
     * @return
     */
    public static OutPacket characterModified(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_MODIFIED);

        chr.encode(outPacket, DBChar.All);

        return outPacket;
    }

    public static OutPacket staticScreenMessage(boolean isNew, String text, boolean isRemoveText) {
        OutPacket outPacket = new OutPacket(OutHeader.STATIC_SCREEN_MESSAGE);

        outPacket.encodeByte(isNew);
        outPacket.encodeString(text);
        outPacket.encodeByte(isRemoveText);

        return outPacket;
    }

    public static OutPacket offStaticScreenMessage() {
        return new OutPacket(OutHeader.OFF_STATIC_SCREEN_MESSAGE);
    }

    public static OutPacket weatherEffectNotice(WeatherEffNoticeType type, String text) {
        return weatherEffectNotice(type, text, 7000);
    }

    public static OutPacket weatherEffectNotice(WeatherEffNoticeType type, String text, int duration) {
        OutPacket outPacket = new OutPacket(OutHeader.WEATHER_EFFECT_NOTICE);

        outPacket.encodeString(text); // Text
        outPacket.encodeInt(type.getVal()); // Weather Notice Type
        outPacket.encodeInt(duration); // Duration in ms
        outPacket.encodeByte(1); // Forced Notice

        return outPacket;
    }

    public static OutPacket progressMessageFont(int fontNameType, int fontSize, int fontColorType, int fadeOutDelay, String message) {
        OutPacket outPacket = new OutPacket(OutHeader.PROGRESS_MESSAGE_FONT);

        outPacket.encodeInt(fontNameType);
        outPacket.encodeInt(fontSize);
        outPacket.encodeInt(fontColorType);
        outPacket.encodeInt(fadeOutDelay);
        outPacket.encodeByte(0);
        outPacket.encodeString(message);

        return outPacket;
    }

    public static OutPacket clearAnnouncedQuest() {
        return new OutPacket(OutHeader.CLEAR_ANNOUNCED_QUEST);
    }

    public static OutPacket unkBossEntry2(String curTimeUTC) {
        OutPacket outPacket = new OutPacket(OutHeader.UNK_BOSS_ENTRY2);

        outPacket.encodeString(curTimeUTC); // 2026012922544501914534 - YYYYMMDDHHMMSS.....
        outPacket.encodeInt(100001801);
        outPacket.encodeInt(1);
        outPacket.encodeInt(14534);
        outPacket.encodeByte(0);
        outPacket.encodeInt(1);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket resultInstanceTable(String name, int type, int subType, boolean rightResult, int value) {
        OutPacket outPacket = new OutPacket(OutHeader.RESULT_INSTANCE_TABLE);

        outPacket.encodeString(name);
        outPacket.encodeInt(type); // nCount
        outPacket.encodeInt(subType);
        outPacket.encodeByte(rightResult);
        outPacket.encodeInt(value);

        return outPacket;
    }

    public static OutPacket resultInstanceTable(InstanceTableType ritt, boolean rightResult, int value) {
        return resultInstanceTable(ritt.getTableName(), ritt.getType(), ritt.getSubType(), rightResult, value);
    }

    /**
     * Creates a packet to indicate the golden hammer is finished.
     *
     * @param returnResult See below
     * @param result       when returnResult is:
     *                     0 or 1:
     *                     Anything: Golden hammer refinement applied
     *                     2:
     *                     0: Increased available upgrade by 1
     *                     1: Refining using golden hammer failed
     *                     3:
     *                     1: Item is not upgradable
     *                     2: 2 upgrade increases have been used already
     *                     3: You can't vicious hammer non-horntail necklace
     * @param upgradesLeft amount of upgrades left. NOTE: ((v9 >> 8) & 0xFF) - v9 + 2) (where v9 = upgradesLeft)
     * @return the created packet
     */
    public static OutPacket goldHammerItemUpgradeResult(GoldHammerResult returnResult, int result, int upgradesLeft) {
        OutPacket outPacket = new OutPacket(OutHeader.GOLD_HAMMER_ITEM_UPGRADE_RESULT);

        outPacket.encodeByte(returnResult.getVal());
        switch (returnResult) {
            case Success:
                outPacket.encodeInt(result);
                outPacket.encodeInt(upgradesLeft);
                break;
            case Fail:
                //Todo:
                break;
            case Done:
            case Error:
                //Todo: Find Error ?
                outPacket.encodeInt(result);
                break;
        }

        return outPacket;
    }

    public static OutPacket returnToCharacterSelect() {
        return new OutPacket(OutHeader.RETURN_TO_CHARACTER_SELECT);
    }

    public static OutPacket returnToTitle() {
        return new OutPacket(OutHeader.RETURN_TO_TITLE);
    }

    public static OutPacket issueReloginCookie(String token, int charId, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.ISSUE_RELOGIN_COOKIE);

        outPacket.encodeString(token);
        if (!token.isEmpty()) {
            outPacket.encodeInt(charId);
            if (charId != 0) {
                outPacket.encodeString(msg);
            }
        }

        return outPacket;
    }

    public static OutPacket actionBarResult(int reqType, int actionBarID) {
        OutPacket outPacket = new OutPacket(OutHeader.ACTION_BAR_RESULT);

        outPacket.encodeInt(reqType);
        outPacket.encodeInt(actionBarID);
        // Black Mage
        if (reqType == 7) {
            int size =0;
            outPacket.encodeInt(size);
            for (int i = 0; i < size; i++) {
                outPacket.encodeInt(0);
            }
        }

        return outPacket;
    }

    // Hiển thị chữ vàng ở trên màn hình, giống chr.chatScriptMesage(sMsg);
    public static OutPacket screenMsg(String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.SCREEN_MSG);

        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket townPortal(TownPortal townPortal) {
        OutPacket outPacket = new OutPacket(OutHeader.TOWN_PORTAL); // As a response to Enter_TP_Request, creates the Door in the TownField

        outPacket.encodeInt(townPortal.getTownFieldId()); // townFieldId
        outPacket.encodeInt(townPortal.getFieldFieldId()); // field FieldId
        outPacket.encodeInt(townPortal.getSkillid()); // Skill Id
        outPacket.encodePosition(new Position()); // fieldField TownPortal Position

        return outPacket;
    }

    public static OutPacket partyQuestRankingResult(PartyQuestRanking partyQuestRanking) {
        OutPacket outPacket = new OutPacket(OutHeader.PARTY_QUEST_RANKING_RESULT);

        outPacket.encodeInt(partyQuestRanking.getRank());
        outPacket.encodeInt(partyQuestRanking.getQuestValueType());
        outPacket.encodeInt(partyQuestRanking.getRankingType().getVal());
        outPacket.encodeInt(partyQuestRanking.getPartyRankingInfoList().size());
        for (PartyRankingInfo partyRankingInfo : partyQuestRanking.getPartyRankingInfoList()) {
            outPacket.encodeInt(partyRankingInfo.getValue());
            outPacket.encodeInt(partyRankingInfo.getPmName().size());
            for (String pmName : partyRankingInfo.getPmName()) {
                outPacket.encodeString(pmName);
            }
        }
        // Map/Effect.img/WU_PartyQuest/RankedIn

        return outPacket;
    }

    public static OutPacket trunkSlotIncResult(TrunkSlotIncResultType type, int inc) {
        OutPacket outPacket = new OutPacket(OutHeader.TRUNK_SLOT_INC_RESULT);

        outPacket.encodeInt(type.getVal());
        outPacket.encodeByte(0);
        outPacket.encodeInt(inc);

        return outPacket;
    }

    public static OutPacket randomPortalNotice(RandomPortal randomPortal, int fieldID) {
        OutPacket outPacket = new OutPacket(OutHeader.RANDOM_PORTAL_NOTICE);

        outPacket.encodeByte(randomPortal.getAppearType().ordinal());
        outPacket.encodeInt(fieldID);

        return outPacket;
    }

    public static OutPacket setPassenserRequest(int requestorChrId) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_PASSENGER_REQUEST);

        outPacket.encodeInt(requestorChrId);

        return outPacket;
    }

    public static OutPacket platformarEnterResult(boolean wrap) {
        OutPacket outPacket = new OutPacket(OutHeader.PLATFORMAR_ENTER_RESULT);

        outPacket.encodeByte(wrap);

        return outPacket;
    }

    public static OutPacket platformarOxyzen(int oxyzen) {
        OutPacket outPacket = new OutPacket(OutHeader.PLATFORMAR_OXYZEN);

        outPacket.encodeInt(oxyzen); // casted to long in client side

        return outPacket;
    }

    public static OutPacket merchantResult() {
        OutPacket outPacket = new OutPacket(OutHeader.ENTRUSTED_SHOP_CHECK_RESULT);

        outPacket.encodeInt(7);

        return outPacket;
    }

    public static OutPacket bingoResult(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.BINGO_RESULT);

        outPacket.encodeByte(type);

        return outPacket;
    }

    public static OutPacket setMaplePoint(int maplePoint) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAPLE_POINT);

        outPacket.encodeInt(maplePoint);

        return outPacket;
    }

    public static OutPacket setMapleCoin(LocalDateTime ldt) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MAPLE_COIN);

        outPacket.encodeFT(FileTime.fromDate(ldt));
        outPacket.encodeFT(FileTime.fromDate(ldt.plusDays(1)));
        outPacket.encodeByte(true);
        outPacket.encodeByte(false);
        outPacket.encodeInt(95500);
        outPacket.encodeInt(100500);
        outPacket.encodeLong(4659443777199107511L);
        outPacket.encodeByte(4);
        outPacket.encodeInt(4310345);
        outPacket.encodeLong(395700);
        outPacket.encodeInt(3);
        outPacket.encodeInt(4310346);
        outPacket.encodeLong(3956600);
        outPacket.encodeInt(30);
        outPacket.encodeInt(4310347);
        outPacket.encodeLong(39565500);
        outPacket.encodeInt(300);
        outPacket.encodeInt(4310348);
        outPacket.encodeLong(395655000);
        outPacket.encodeInt(3000);

        return outPacket;
    }

    public static OutPacket abilityResetItemResult(int characterID) {
        OutPacket outPacket = new OutPacket(OutHeader.ABILITY_RESET_ITEM_RESULT);

        outPacket.encodeByte(1); // tick
        outPacket.encodeInt(characterID);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket skillResetItemResult(int characterID) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_RESET_ITEM_RESULT);

        outPacket.encodeByte(1); // tick
        outPacket.encodeInt(characterID);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket setBuyEquipExt() {
        return new OutPacket(OutHeader.SET_BUY_EQUIP_EXT);
    }

    public static OutPacket scriptProgressMessageBySoul(String string, int updateDelay) {
        OutPacket outPacket = new OutPacket(OutHeader.SCRIPT_PROGRESS_MESSAGE_BY_SOUL);

        outPacket.encodeString(string);
        outPacket.encodeInt(updateDelay);

        return outPacket;
    }

    public static OutPacket scriptProgressMessage(String string) {
        OutPacket outPacket = new OutPacket(OutHeader.SCRIPT_PROGRESS_MESSAGE);

        outPacket.encodeString(string);

        return outPacket;
    }

    public static OutPacket scriptProgressItemMessage(String string, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.SCRIPT_PROGRESS_ITEM_MESSAGE);

        outPacket.encodeInt(itemID);
        outPacket.encodeString(string);

        return outPacket;
    }

    public static OutPacket nickSkillExpired(int nickSkillItemID) {
        OutPacket outPacket = new OutPacket(OutHeader.NICK_SKILL_EXPIRED);

        outPacket.encodeInt(nickSkillItemID);

        return outPacket;
    }

    public static OutPacket setWeekEventMessage(String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_WEEK_EVENT_MESSAGE);

        outPacket.encodeByte(true);
        outPacket.encodeString(msg); // Màu vàng trong khung chat của nhân vật

        return outPacket;
    }

    public static OutPacket setPotionDiscountRate(byte per) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_POTION_DISCOUNT_RATE);

        outPacket.encodeByte(per);

        return outPacket;
    }

    public static OutPacket bridleMobCatchFail(int itemID, boolean elementRock) {
        OutPacket outPacket = new OutPacket(OutHeader.BRIDLE_MOB_CATCH_FAIL);

        outPacket.encodeByte(elementRock);//rock?
        outPacket.encodeInt(itemID);
        outPacket.encodeInt(0); //ignored

        return outPacket;
    }

    public static OutPacket itemCollectResult(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.ITEM_COLLECT_RESULT);

        outPacket.encodeByte(type);
        // type == 97 : The item has been collected.
        // type == 98 : Try again later.

        return outPacket;
    }

    public static OutPacket getRegDate(FileTime ft) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_REG_DATE_RESULT);

        outPacket.encodeFT(ft);

        return outPacket;
    }

    public static OutPacket updateVMatrix(Char chr, boolean update, int type, int typeArg) {
        OutPacket outPacket = new OutPacket(OutHeader.VMATRIX_UPDATE);

        chr.encodeMatrixSkills(outPacket);
        outPacket.encodeByte(update);
        if (update) {
            outPacket.encodeInt(type);
            if (type == MatrixUpdateType.Activate.getVal()) {
                outPacket.encodeInt(typeArg);
            }
        }

        return outPacket;
    }

    public static OutPacket nodeShardResult(int shard) {
        OutPacket outPacket = new OutPacket(OutHeader.NODESTONE_SHARD_RESULT);

        outPacket.encodeInt(shard);

        return outPacket;
    }

    public static OutPacket nodeEnhanceResult(NodeEnhance enhance) {
        OutPacket outPacket = new OutPacket(OutHeader.NODESTONE_ENHANCE_RESULT);

        enhance.encode(outPacket);

        return outPacket;
    }

    public static OutPacket nodeEnhanceResultInBulk(List<NodeEnhance> enhances) {
        OutPacket outPacket = new OutPacket(OutHeader.NODESTONE_ENHANCE_RESULT_BULK);

        outPacket.encodeInt(enhances.size());
        for (NodeEnhance enhance : enhances) {
            enhance.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket nodeCraftResult(int coreID, int skillLevel, int skillID1, int skillID2, int skillID3) {
        OutPacket outPacket = new OutPacket(OutHeader.NODESTONE_CRAFT_RESULT);

        outPacket.encodeInt(coreID);
        outPacket.encodeInt(skillLevel);
        outPacket.encodeInt(skillID1);
        outPacket.encodeInt(skillID2);
        outPacket.encodeInt(skillID3);
        outPacket.encodeInt(0); // new v214?

        return outPacket;
    }

    public static OutPacket nodeStoneResult(MatrixCore core) {
        OutPacket outPacket = new OutPacket(OutHeader.NODESTONE_RESULT);

        outPacket.encodeInt(core.getCoreID());
        outPacket.encodeInt(core.getSkillLevel());
        outPacket.encodeInt(core.getSkillID1());
        outPacket.encodeInt(core.getSkillID2());
        outPacket.encodeInt(core.getSkillID3());
        outPacket.encodeInt(0); // ?

        return outPacket;
    }

    public static OutPacket nodeStoneResultInBulk(int type, List<MatrixCore> cores) {
        OutPacket outPacket = new OutPacket(OutHeader.NODESTONE_RESULT_BULK);

        outPacket.encodeInt(type);
        outPacket.encodeInt(cores.size());
        for (var core : cores) {
            outPacket.encodeInt(core.getCoreID());
            outPacket.encodeInt(core.getSkillLevel());
            outPacket.encodeInt(core.getSkillID1());
            outPacket.encodeInt(core.getSkillID2());
            outPacket.encodeInt(core.getSkillID3());
        }

        return outPacket;
    }

    public static OutPacket summonedAvatarSync(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMONED_AVATAR_SYNC);

        outPacket.encodeInt(chr.getId());
        chr.getAvatarData().getAvatarLook().encode(outPacket);

        return outPacket;
    }

    public static OutPacket scrollUpgradeFeverTime(int state) {
        //Show Message Spell Trace Fever starting...
        OutPacket outPacket = new OutPacket(OutHeader.SCROLL_UPGRADE_FEVER_TIME);

        outPacket.encodeInt(state);

        return outPacket;
    }

    public static OutPacket renImugiSpiritSwordRequest(int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.REN_IMUGI_SPIRIT_SWORD_REQUEST);

        outPacket.encodeInt(0);
        outPacket.encodeByte(1);
        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket infernoSphereRequest() {
        OutPacket outPacket = new OutPacket(OutHeader.INFERNO_SPHERE_REQUEST);

        outPacket.encodeInt(0);
        outPacket.encodeInt(1);

        return outPacket;
    }

    // "http://maplestory.nexon.net/WZ.ASPX?PART=/Downloads/GamePatches"
    public static OutPacket openURLInBrower() {
        return new OutPacket(OutHeader.OPEN_URL_IN_BROWSER);
    }

    public static OutPacket inheritanceInfo(boolean isOpenUI, boolean isEnableButton, int currentLevel, int nextLevel, int growLevel, int alphaWeaponID, int betaWeaponID, int itemIDForUpgrade, int itemIDCost) {
        OutPacket outPacket = new OutPacket(OutHeader.INHERITANCE_INFO_RESULT);

        outPacket.encodeByte(isOpenUI); //  true -> OpenUI
        outPacket.encodeByte(isEnableButton); //  true -> Enable Button + Popup Effect "Congratulation, choose weapon to upgrade".
        outPacket.encodeInt(currentLevel); //  Current Level (1-9)
        outPacket.encodeInt(growLevel);  //  Able to grow starting from lv. %level
        outPacket.encodeInt(alphaWeaponID); //  Next FirstWeapon ID
        outPacket.encodeInt(betaWeaponID); //  Next SecondWeapon ID
        outPacket.encodeInt(nextLevel); //  Increase from Rank %level to Rank %d
        outPacket.encodeInt(itemIDForUpgrade); //  Item ID Require for Upgrade
        outPacket.encodeInt(itemIDCost); //  Item ID Cost
        outPacket.encodeByte(0); //Unk

        return outPacket;
    }

    public static OutPacket inheritanceComplete() {
        OutPacket outPacket = new OutPacket(OutHeader.INHERITANCE_COMPLETE_RESULT);

        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket changeSoulCollectionResult(int page, int setSoul) {
        OutPacket outPacket = new OutPacket(OutHeader.CHANGE_SOUL_COLLECTION_RESULT);

        outPacket.encodeInt(page);
        outPacket.encodeInt(setSoul);

        return outPacket;
    }

    public static OutPacket selectSoulCollectionResult() {
        return new OutPacket(OutHeader.SELECT_SOUL_COLLECTION_RESULT);
    }

    public static OutPacket limitTradeMesosForNewbie(boolean allow) {
        OutPacket outPacket = new OutPacket(OutHeader.LIMIT_TRADE_MESOS_FOR_NEWBIE);

        outPacket.encodeByte(allow);
        // Players that are Level 15 and below \r\nmay only trade 1 million mesos per day. \r\nYou have reached the limit today%2C\r\nplease try again tomorrow.

        return outPacket;
    }

    // You don't have space for a medal. Clear at least 1 slot in your Equipment Inventory%2C and then open the Crusader.
    public static OutPacket noInventorySlotForMedal() {
        return new OutPacket(OutHeader.NO_INVENTORY_SLOT_FOR_MEDAL);
    }

    public static OutPacket reportUserResult(ReportUserResultType type) {
        OutPacket outPacket = new OutPacket(OutHeader.REPORT_USER_RESULT);

        outPacket.encodeByte(type.getVal());

        return outPacket;
    }

    public static OutPacket popUpMessage(String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.POPUP_MESSAGE);

        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket treasureBoxResult(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.TREASURE_BOX_RESULT);

        outPacket.encodeInt(type);
        // Premium Gold Box: 480000
        // Premium Silver Box: 480001

        return outPacket;
    }

    public static OutPacket questReminderResult(int questID) {
        OutPacket outPacket = new OutPacket(OutHeader.QUEST_REMINDER_RESULT);

        outPacket.encodeInt(questID);
        // Hiện dòng chữ xám "A quest has arrived! Please click on the icon at the bottom of your screen."

        return outPacket;
    }

    public static OutPacket setBossReward(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.BOSS_REWARD);

        chr.encodeBossReward(outPacket);

        return outPacket;
    }

    public static OutPacket userCharacterListResult(Account account) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_CHARACTER_LIST_RESULT);

        outPacket.encodeInt(account.getCharacters().size());
        for (Char chr : account.getCharacters()) {
            var cs = chr.getAvatarData().getCharacterStat();
            outPacket.encodeInt(chr.getId());
            outPacket.encodeShort(cs.getJob());
            outPacket.encodeInt(cs.getLevel());
            outPacket.encodeLong(cs.getExp());
            outPacket.encodeString(cs.getName());
            cs.encodeBurning(outPacket);
            outPacket.encodeByte(false);
        }

        return outPacket;
    }

    public static OutPacket chatBlock(ChatBlockReasonType type, FileTime endDate) {
        OutPacket outPacket = new OutPacket(OutHeader.CHAT_BLOCK);

        outPacket.encodeByte(type.getVal());
        outPacket.encodeFT(endDate.toLocalDateTime());

        return outPacket;
    }

    public static OutPacket openChuChuShop(int npcID) {
        OutPacket outPacket = new OutPacket(OutHeader.OPEN_CHU_CHU_SHOP);

        outPacket.encodeInt(npcID);

        return outPacket;
    }

    public static OutPacket equipDiscountPotionResult(int discount, int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.EQUIP_DISCOUNT_POTION_RESULT);

        outPacket.encodeInt(discount); // < 65%
        outPacket.encodeInt(itemID);
        // For equipping %s%2C potions will be discounted %d%% when you visit store.

        return outPacket;
    }

    public static OutPacket updateUIEventListInfo() {
        OutPacket outPacket = new OutPacket(OutHeader.UPDATE_UI_EVENT_LIST_INFO);

        outPacket.encodeShort(1);
        outPacket.encodeInt(0);
        outPacket.encodeByte(true);

        return outPacket;
    }

    public static OutPacket dojangRankingResult(int typeID, Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.DOJANG_RANKING_RESULT);

        outPacket.encodeInt(1);
        outPacket.encodeByte(typeID);
        outPacket.encodeInt(chr.getJob()); // job
        outPacket.encodeInt(chr.getLevel()); // level
        outPacket.encodeInt(10); // point
        outPacket.encodeInt(1); // ranking
        outPacket.encodeInt(100); // percent
        outPacket.encodeInt(0); // lastpoint
        outPacket.encodeInt(0); // lastranking
        outPacket.encodeInt(0); // lastpercent
        outPacket.encodeByte(typeID);
        outPacket.encodeInt(1);
        outPacket.encodeInt(chr.getJob()); // job
        outPacket.encodeInt(chr.getLevel()); // level
        outPacket.encodeInt(10); // point
        outPacket.encodeInt(1); // ranking
        outPacket.encodeString(chr.getName());
        outPacket.encodeByte(false);

        outPacket.encodeInt(2);
        outPacket.encodeByte(typeID);
        outPacket.encodeInt(chr.getJob()); // job
        outPacket.encodeInt(chr.getLevel()); // level
        outPacket.encodeInt(10); // point
        outPacket.encodeInt(1); // ranking
        outPacket.encodeInt(100); // percent
        outPacket.encodeInt(0); // lastpoint
        outPacket.encodeInt(0); // lastranking
        outPacket.encodeInt(0); // lastpercent
        outPacket.encodeByte(typeID);
        outPacket.encodeInt(1);
        outPacket.encodeInt(chr.getJob()); // job
        outPacket.encodeInt(chr.getLevel()); // level
        outPacket.encodeInt(10); // point
        outPacket.encodeInt(1); // ranking
        outPacket.encodeString(chr.getName());
        outPacket.encodeByte(false);

        outPacket.encodeInt(3);
        outPacket.encodeByte(typeID);
        outPacket.encodeInt(chr.getJob()); // job
        outPacket.encodeInt(chr.getLevel()); // level
        outPacket.encodeInt(10); // point
        outPacket.encodeInt(1); // ranking
        outPacket.encodeInt(100); // percent
        outPacket.encodeInt(0); // lastpoint
        outPacket.encodeInt(0); // lastranking
        outPacket.encodeInt(0); // lastpercent
        outPacket.encodeByte(typeID);
        outPacket.encodeInt(1);
        outPacket.encodeInt(chr.getJob()); // job
        outPacket.encodeInt(chr.getLevel()); // level
        outPacket.encodeInt(10); // point
        outPacket.encodeInt(1); // ranking
        outPacket.encodeString(chr.getName());
        outPacket.encodeByte(false);

        outPacket.encodeByte(true);
        outPacket.encodeInt(4);
        outPacket.encodeByte(typeID);
        outPacket.encodeInt(chr.getJob()); // job
        outPacket.encodeInt(chr.getLevel()); // level
        outPacket.encodeInt(10); // point
        outPacket.encodeInt(1); // ranking
        outPacket.encodeInt(100); // percent
        outPacket.encodeInt(0); // lastpoint
        outPacket.encodeInt(0); // lastranking
        outPacket.encodeInt(0); // lastpercent
        outPacket.encodeByte(typeID);
        outPacket.encodeInt(1);
        outPacket.encodeInt(chr.getJob()); // job
        outPacket.encodeInt(chr.getLevel()); // level
        outPacket.encodeInt(10); // point
        outPacket.encodeInt(1); // ranking
        outPacket.encodeString(chr.getName());
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket requestEventList() {
        OutPacket outPacket = new OutPacket(OutHeader.REQUEST_EVENT_LIST);

        outPacket.encodeInt(0); // defaultLevelLimit
        boolean bool = true;
        outPacket.encodeByte(bool);

        if (bool) {
            outPacket.encodeString("楓之谷活動");
            outPacket.encodeByte(0);
            outPacket.encodeInt(0); // maxCount
            outPacket.encodeInt(0);
            //outPacket.encodeByte(1);
            //outPacket.encodeString("");
            outPacket.encodeInt(27); // pAlarmInfoQuestRoot.m_pInterface
            outPacket.encodeInt(12);
            outPacket.encodeString("點數商品欄轉移活動"); // Event Name
            //outPacket.encodeString("");
            outPacket.encodeInt(25); // timeStart
            outPacket.encodeInt(44); // timeEnd
            outPacket.encodeInt(1);
            outPacket.encodeInt(20251105); // dateStart (Year-Month-Day)
            outPacket.encodeInt(20251112); // dateEnd (Year-Month-Day)
            outPacket.encodeInt(0); // UI
            outPacket.encodeInt(140000);
            outPacket.encodeInt(0); // prior
            outPacket.encodeInt(-1);
            outPacket.encodeInt(0);
            outPacket.encodeByte(false); // hot
            outPacket.encodeByte(true); // expEvent
            outPacket.encodeByte(false); // attend
            outPacket.encodeByte(false); // invisible
            outPacket.encodeByte(false); // continue

            outPacket.encodeInt(5); // reward items size
            outPacket.encodeInt(2000005);
            outPacket.encodeInt(2023207);
            outPacket.encodeInt(5040004);
            outPacket.encodeInt(3015700);
            outPacket.encodeInt(2434981);

            outPacket.encodeInt(0);

            outPacket.encodeByte(false);

            outPacket.encodeString(""); // pAlarmInfoQuestRoot
            outPacket.encodeString(""); // nEntryCount
            outPacket.encodeInt(0);
            outPacket.encodeByte(0); // baseIcon

            outPacket.encodeByte(1);
            outPacket.encodeInt(10); // lvmin
            outPacket.encodeInt(255); // lvmax
            outPacket.encodeInt(0); // world
        }
        outPacket.encodeInt(1);
        // LiveEvent::LIVE_EVENT::Decode
        outPacket.encodeString("");
        outPacket.encodeInt(14); // nCategory
        outPacket.encodeInt(9); // nEventType
        outPacket.encodeInt(8); // nEventValue
        outPacket.encodeInt(20231231); // nDateStart (Year-Month-Day)
        outPacket.encodeInt(20240331); // nDateEnd (Year-Month-Day)
        outPacket.encodeInt(0); // nTimeStart
        outPacket.encodeInt(10000); // nTimeEnd
        outPacket.encodeInt(10); // nMinLevel

        outPacket.encodeInt(0); // Only in GMS
        outPacket.encodeByte(true); // Only in GMS
        outPacket.encodeInt(0); // Only in GMS
        outPacket.encodeInt(0); // Only in GMS
        outPacket.encodeInt(0); // Only in GMS
        outPacket.encodeInt(0); // Only in GMS

        outPacket.encodeString(""); // sDesc
        outPacket.encodeString("");  // Only in GMS
        outPacket.encodeString("");  // v214

        return outPacket;
    }

    public static OutPacket eventListResult(List<EventListData> datas) {
        OutPacket outPacket = new OutPacket(OutHeader.EVENT_LIST);

        outPacket.encodeInt(datas.size());
        for (var event : datas) {
            event.encode(outPacket);
        }

        return outPacket;
    }


    public static OutPacket jobFreeChangeResult(byte type) {
        OutPacket outPacket = new OutPacket(OutHeader.JOB_FREE_CHANGE_RESULT);

        outPacket.encodeByte(type);
        // type = 0 : success
        // type 1 ~ 7, 98, 99 : fail
        // type == 111: SID_JOBFREECHANGE_FAIL_UNIONCHAMPION

        return outPacket;
    }

    public static OutPacket hourChange(short dayOfWeek, short hourOfDay) {
        OutPacket outPacket = new OutPacket(OutHeader.HOUR_CHANGE);

        outPacket.encodeShort(dayOfWeek);
        outPacket.encodeShort(hourOfDay);

        return outPacket;
    }

    public static OutPacket sessionValue(String key, String value) {
        OutPacket outPacket = new OutPacket(OutHeader.SESSION_VALUE);

        outPacket.encodeString(key);
        outPacket.encodeString(value);

        return outPacket;
    }

    public static OutPacket partyValue(String key, String value) {
        OutPacket outPacket = new OutPacket(OutHeader.PARTY_VALUE);

        outPacket.encodeString(key);
        outPacket.encodeString(value);

        return outPacket;
    }

    public static OutPacket fieldSetVariable(String key, String value) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_SET_VARIABLE);

        outPacket.encodeString(key);
        outPacket.encodeString(value);

        return outPacket;
    }

    public static OutPacket fieldValue(String key, String value) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_VALUE);

        outPacket.encodeString(key);
        outPacket.encodeString(value);

        return outPacket;
    }

    public static OutPacket pamsSongResult() {
        return new OutPacket(OutHeader.PAMS_SONG_RESULT);
    }

    public static OutPacket userOpenQuickMove(boolean type, List<QuickMoveInfo> quickMoveInfos) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_OPEN_QUICK_MOVE);

        outPacket.encodeByte(!type);
        if (!type) {
            outPacket.encodeShort(type ? 1 : 0);
            outPacket.encodeByte(quickMoveInfos.size());
            quickMoveInfos.forEach(qmi -> qmi.encode(outPacket));
        }

        return outPacket;
    }

    public static OutPacket skillSquenceSkills(Char chr, List<SequenceSkill> skills) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_SEQUENCE_SKILLS);

        outPacket.encodeByte(skills.size());
        for (SequenceSkill skill : skills) {
            skill.encode(outPacket);
        }
        byte unk0 = Byte.parseByte(chr.getQRValueByKey(QuestConstants.SKILL_SEQUENCE_SKILLS, "0"));
        outPacket.encodeByte(unk0);
        byte unk1 = Byte.parseByte(chr.getQRValueByKey(QuestConstants.SKILL_SEQUENCE_SKILLS, "1"));
        outPacket.encodeByte(unk1);
        byte unk2 = Byte.parseByte(chr.getQRValueByKey(QuestConstants.SKILL_SEQUENCE_SKILLS, "2"));
        outPacket.encodeByte(unk2);

        return outPacket;
    }

    public static OutPacket skillSquenceBuffs(Char chr, List<SequenceBuff> buffs) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_SEQUENCE_BUFFS);

        byte unk0 = Byte.parseByte(chr.getQRValueByKey(QuestConstants.SKILL_SEQUENCE_BUFFS, "0"));
        outPacket.encodeByte(unk0);
        byte unk1 = Byte.parseByte(chr.getQRValueByKey(QuestConstants.SKILL_SEQUENCE_BUFFS, "1"));
        outPacket.encodeByte(unk1);
        outPacket.encodeInt(buffs.size());
        for (SequenceBuff buff : buffs) {
            buff.encode(outPacket, chr);
        }

        return outPacket;
    }

    public static OutPacket skillSquenceBuffsInformation() {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_SEQUENCE_BUFFS_INFORMATION);

        final int[] skillList = SequenceBuff.skillList;
        final int[] itemList = SequenceBuff.itemList;
        outPacket.encodeInt(skillList.length);
        for (int skillID : skillList) {
            outPacket.encodeInt(skillID);
        }
        outPacket.encodeInt(itemList.length);
        for (int itemID : itemList) {
            outPacket.encodeInt(itemID);
        }

        return outPacket;
    }

    public static OutPacket userReceiveStuffs(RewardResult rewardResult) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_RECEIVE_STUFFS);

        rewardResult.encode(outPacket);

        return outPacket;
    }

    public static OutPacket mannequinResult() {
        OutPacket outPacket = new OutPacket(OutHeader.MANNEQUIN_RESULT);

        outPacket.encodeInt(0);
        outPacket.encodeInt(-1); //Slot
        outPacket.encodeByte(1); //is update

        return outPacket;
    }

    public static OutPacket getGachaponWheelResult(int mode, String dataKey, List<Integer> ids, int position) {
        OutPacket outPacket = new OutPacket(OutHeader.GACHAPON_WHEEL_RESULT);

        outPacket.encodeByte(mode);
        if (mode == 3) {
            outPacket.encodeByte(ids.size());
            for (Integer i : ids) {
                outPacket.encodeInt(i);
            }
            outPacket.encodeString(dataKey);
            outPacket.encodeByte(position);
        }
        // 6 = You don't have a Magic Gachapon Wheel in your Inventory.
        // 7 = You don't have any Inventory Space.\r\n You must have 2 or more slots available\r\n in each of your tabs.
        // 8 = Please try this again later.
        // 9 = Failed to delete Magic Gachapon Wheel item.
        // 10 = Failed to receive Magic Gachapon Wheel item.
        // 11 = You cannot move while Magic Wheel window is open.

        return outPacket;
    }

    public static OutPacket unionPresetInfoResult(int preset, boolean unlocked, UnionBoard ub) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_PRESET_INFO_RESULT);

        outPacket.encodeInt(preset);
        outPacket.encodeByte(unlocked);
        if (unlocked) {
            ub.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket updateTime() {
        OutPacket outPacket = new OutPacket(OutHeader.UPDATE_TIME_UNK);

        outPacket.encodeFT(FileTime.currentTime());

        return outPacket;
    }

    public static OutPacket quickPass() {
        OutPacket outPacket = new OutPacket(OutHeader.QUICK_PASS);

        outPacket.encodeInt(1);

        return outPacket;
    }

    public static OutPacket startNavigation(int nTargetMap, int m_eObjectType, String sObjInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.START_NAVIGATION);

        outPacket.encodeInt(nTargetMap);
        outPacket.encodeInt(m_eObjectType);
        outPacket.encodeString(sObjInfo);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket sendSetPhysicalWorldAttach(String local) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_PHYSICAL_WORLD_ATTACH);

        outPacket.encodeString(local);

        return outPacket;
    }

    public static OutPacket sendMigrateSecurityResult(int worldID, int charID) {
        OutPacket outPacket = new OutPacket(OutHeader.SEND_MIGRATE_SECURITY_RESULT);

        String result = worldID + "_" + charID + "_" + FileTime.currentTime().toYYYYMMDDHHMMSSSSS() + "_";
        outPacket.encodeString(result);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket userHonorItemResult(int type, int amount) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_HONOR_ITEM_RESULT);

        outPacket.encodeInt(type);
        if (type == 0) {
            outPacket.encodeInt(amount);
        }

        return outPacket;
    }

    public static OutPacket sendUserIDCheck(int userID) {
        OutPacket outPacket = new OutPacket(OutHeader.SEND_USER_ID_CHECK);

        outPacket.encodeLong(userID);
        outPacket.encodeString("76846D6305F1");

        return outPacket;
    }

    public static OutPacket sendMenu(Account account) {
        OutPacket outPacket = new OutPacket(OutHeader.SEND_MENU);

        outPacket.encodeInt(account.getCharacters().size());
        for (Char chr : account.getCharacters()) {
            outPacket.encodeInt(chr.getId());
            outPacket.encodeShort(chr.getJob());
            outPacket.encodeInt(0);
            outPacket.encodeLong(0);
            outPacket.encodeString(chr.getName());
            chr.getAvatarData().getCharacterStat().encodeBurning(outPacket);
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket hexaMessage(int type, int msgType, int coreID, int coreLevel) {
        OutPacket outPacket = new OutPacket(OutHeader.HEXA_MESSAGE);

        outPacket.encodeInt(type);
        outPacket.encodeInt(msgType);
        switch (type) {
            case 0: // [Skill] ActivateCore
                outPacket.encodeInt(coreID);
                break;
            case 1: // [Skill] EnforceCore
                outPacket.encodeInt(coreID);
                outPacket.encodeInt(coreLevel);
                break;
            case 2: // [Stat] ActivateCore
                outPacket.encodeInt(coreID);
                break;
            case 3: // [Stat] EnforceCore
                outPacket.encodeInt(coreID);
                outPacket.encodeInt(coreLevel);
                break;
            case 4: // [Stat] SaveToSubSlot
            case 5: // [Stat] SetUsingSlot
            case 6: // [Stat] ResetLevel
            case 7: // [Stat] ChangeStatType
            case 8: // [Stat] ChangeStatType
                break;
        }

        return outPacket;
    }

    public static OutPacket hexaSkillsUpdate(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.HEXA_SKILL_UPDATE);

        chr.encodeHexaSkills(outPacket);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket hexaStatsUpdate(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.HEXA_STAT_UPDATE);

        chr.encodeHexaStats(outPacket);

        return outPacket;
    }

    public static OutPacket hexaMatrixInformation() {
        OutPacket outPacket = new OutPacket(OutHeader.HEXA_MATRIX_INFORMATION);

        HexaMatrixSkill[] skillTypes = getHexaMatrixSkillTypes();
        outPacket.encodeInt(skillTypes.length); // size of the cost to activate HEXA Skills
        for (HexaMatrixSkill skill : skillTypes) {
            outPacket.encodeInt(skill.getType()); // Node Type
            outPacket.encodeInt(getSolErdaCostToActivate(skill)); // Sol Erda Required %d to activate  Hexa Skills
            outPacket.encodeInt(getSolErdaFragmentCostToActivate(skill)); // Sol Erda Fragments Required %d to activate Hexa Skills
        }
        outPacket.encodeInt(skillTypes.length); // size for tables of the cost to upgrade HEXA Skills
        for (HexaMatrixSkill skill : skillTypes) {
            outPacket.encodeInt(skill.getType()); // Node Type
            int maxLevel = getHexaSkillMasterLevel(skill);
            outPacket.encodeInt(maxLevel);
            for (int level = 1; level <= maxLevel; level++) {
                outPacket.encodeInt(level); // From Level %d (start with 1)
                outPacket.encodeInt(getSolErdaCostToUpgrade(skill, level)); // Sol Erda Required %d to upgrade
                outPacket.encodeInt(getSolErdaFragmentCostToUpgrade(skill, level)); // Sol Erda Fragments Required %d to upgrade
            }
        }
        outPacket.encodeInt(statCores.size());
        for (HexaStatCore core : statCores) {
            outPacket.encodeInt(core.id);
            outPacket.encodeInt(core.solErdaCostToUnlock);
            outPacket.encodeInt(0); // unk
            outPacket.encodeInt(core.solErdaFragmentCostToUnlock);
            outPacket.encodeInt(hexaStatUpgradeSolErdaCost.length);
            for (int level = 0; level < hexaStatUpgradeSolErdaCost.length; level++) {
                outPacket.encodeInt(level);
                outPacket.encodeInt(hexaStatUpgradeSolErdaCost[level]);
                outPacket.encodeInt(0); // unk
                outPacket.encodeInt(hexaStatUpgradeSolErdaFragmentCost[level]);
            }
            outPacket.encodeInt(hexaStatUpgradeWeight.length);
            for (int level = 0; level < hexaStatUpgradeWeight.length; level++) {
                outPacket.encodeInt(level);
                outPacket.encodeLong(Double.doubleToRawLongBits(hexaStatUpgradeWeight[level]));
            }
            outPacket.encodeInt(core.coreCosts10To30.size());
            for (var c : core.coreCosts10To30) {
                outPacket.encodeInt(c.level);
                outPacket.encodeLong(c.cost);
            }
            outPacket.encodeInt(core.coreCosts0to30.size());
            for (var c : core.coreCosts0to30) {
                outPacket.encodeInt(c.level);
                outPacket.encodeLong(c.cost);
            }
        }
        outPacket.encodeInt(solErdaFragments.length);
        for (int solErdaFragment : solErdaFragments) {
            outPacket.encodeInt(solErdaFragment);
        }

        return outPacket;
    }

    public static OutPacket HexaMatrixActivateResult(boolean isSuccess) {
        OutPacket outPacket = new OutPacket(OutHeader.HEXA_MATRIX_ACTIVATE_RESULT);

        outPacket.encodeByte(isSuccess);

        return outPacket;
    }

    public static OutPacket hexaSkillErdaConversion(int... args) {
        OutPacket outPacket = new OutPacket(OutHeader.HEXA_SKILL_ERDA_CONVERSION);

        outPacket.encodeInt(args[0]);
        outPacket.encodeInt(0);
        if (args[0] == 0) { // init
            outPacket.encodeInt(1);
            outPacket.encodeInt(erdaConversions.length);
            for (int i = 0; i < erdaConversions.length; i++) {
                outPacket.encodeInt(1);
                outPacket.encodeInt(erdaConversions[i][0]);
                outPacket.encodeInt(erdaConversions[i][1]);
            }
        } else if (args[0] == 1) {
            outPacket.encodeShort(args[1]);
            outPacket.encodeInt(args[2]);
            outPacket.encodeInt(args[3]);
        }

        return outPacket;
    }

    public static OutPacket unionArtifactUpdate(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_ARTIFACT_RESULT);

        chr.encodeUnionArtifacts(outPacket);

        return outPacket;
    }

    public static OutPacket unionArtifactInformation() {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_ARTIFACT_INFORMATION);

        var settings = ArtifactData.settings;

        outPacket.encodeInt(settings.length);
        for (int[] setting : settings) {
            outPacket.encodeInt(setting[0]); // Maximum number of Artifact Points held
            outPacket.encodeInt(setting[1]); // Exp Required
            outPacket.encodeInt(setting[2]); // Retained Artifact APs
        }

        return outPacket;
    }

    public static OutPacket unionArtifactUpdate(int type, int index) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_ARTIFACT_UPDATE);

        outPacket.encodeInt(type);
        outPacket.encodeInt(0);
        outPacket.encodeInt(index);

        return outPacket;
    }

    public static OutPacket unionArtifactMsg(Char chr, int exp, int point, boolean levelUp) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_ARTIFACT_MSG);

        outPacket.encodeInt(exp);
        outPacket.encodeInt(point);
        outPacket.encodeInt((levelUp ? chr.getArtifactVal("level") : 0));

        return outPacket;
    }

    public static OutPacket unionArtifactQuestMsg(int type, ArtifactData.MissionType missionType, int missionNum, int exp, int point) {
        return unionArtifactQuestMsg(type, missionType.getVal(), missionNum, exp, point, 0, null);
    }

    public static OutPacket unionArtifactQuestMsg(int type, int index, FileTime extendTime) {
        return unionArtifactQuestMsg(type, 0, 0, 0, 0, index, extendTime);
    }

    public static OutPacket unionArtifactQuestMsg(int type, int missionType, int missionNum, int exp, int point, int index, FileTime extendTime) {
        OutPacket outPacket = new OutPacket(OutHeader.UNION_ARTIFACT_QUEST_MSG);

        outPacket.encodeInt(type);

        switch (type) {
            case 1: {
                outPacket.encodeInt(missionType); // Mission Type (0: Normal / 1: Boss / 2: Special)
                outPacket.encodeInt(missionNum); // Mission number
                outPacket.encodeInt(exp);
                outPacket.encodeInt(point);
                outPacket.encodeByte(0);
                outPacket.encodeByte(0);
                break;
            }
            case 2:
            case 3: {
                if (type == 2) {
                    outPacket.encodeInt(index);
                }
                long filetime = (extendTime.toMillis() + 11644473600000L) * 10000L;
                int high = (int) (filetime >>> 32);
                int low  = (int) filetime;
                outPacket.encodeInt(high);
                outPacket.encodeInt(low);
                break;
            }
        }

        return outPacket;
    }

    public static OutPacket sendExtraSystemResult(int a, int b) {
        OutPacket outPacket = new OutPacket(OutHeader.EXTRA_SYSTEM_RESULT);
        outPacket.encodeInt(-1289454273);
        outPacket.encodeShort(1); // login type
        outPacket.encodeInt(-745184127);
        outPacket.encodeInt(1090473174);
        outPacket.encodeFT(FileTime.currentTime());
        return outPacket;
    }

    public static OutPacket sendExtraSystemInit() {
        OutPacket outPacket = new OutPacket(OutHeader.EXTRA_SYSTEM_RESULT);
        outPacket.encodeInt(-1289454273);
        outPacket.encodeShort(33);
        return outPacket;
    }

    public static OutPacket sendExtraSystemStack(int type, int rand, byte unk) {
        OutPacket outPacket = new OutPacket(OutHeader.EXTRA_SYSTEM_RESULT);
        outPacket.encodeInt(-1289454273);
        outPacket.encodeShort(34);
        outPacket.encodeByte(type);
        outPacket.encodeInt(0);
        outPacket.encodeInt(rand);
        outPacket.encodeInt(0);
        outPacket.encodeByte(true);
        outPacket.encodeShort(InHeader.MO_XUAN_STACK_REQUEST.getValue());
        outPacket.encodeByte(type);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(unk);
        return outPacket;
    }

    public static OutPacket resetMoXuanStack() {
        OutPacket outPacket = new OutPacket(OutHeader.EXTRA_SYSTEM_RESULT);
        outPacket.encodeInt(-1289454273);
        outPacket.encodeShort(34);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(-25758519);
        outPacket.encodeInt(0);
        outPacket.encodeByte(true);
        outPacket.encodeShort(InHeader.MO_XUAN_STACK_REQUEST.getValue());
        outPacket.encodeByte(2);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(Util.getRandom(-127, 128));
        return outPacket;
    }

    public static OutPacket sendMoXuanStack(int powerType, int godPower) {
        OutPacket outPacket = new OutPacket(OutHeader.EXTRA_SYSTEM_RESULT);
        outPacket.encodeInt(-1289454273);
        outPacket.encodeShort(34);
        outPacket.encodeByte(powerType);
        outPacket.encodeInt(godPower);
        outPacket.encodeInt(3000);
        outPacket.encodeInt(1);
        outPacket.encodeByte(true);
        outPacket.encodeShort(InHeader.MO_XUAN_STACK_REQUEST.getValue());
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(Util.getRandom(-127, 128));
        return outPacket;
    }

    public static OutPacket sendMoXuanPower(int powerType, int godPower, int time, int value2, int type2) {
        OutPacket outPacket = new OutPacket(OutHeader.EXTRA_SYSTEM_RESULT);
        outPacket.encodeInt(-1289454273);
        outPacket.encodeShort(34);
        outPacket.encodeByte(powerType);
        outPacket.encodeInt(godPower);
        outPacket.encodeInt(time);
        outPacket.encodeInt(value2);
        outPacket.encodeByte(true);
        outPacket.encodeShort(1866);
        outPacket.encodeByte(type2);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(Util.getRandom(-127, 128));
        return outPacket;
    }

    public static OutPacket consumeCubeRelease(int cubeIndex, long equipSN, int cubeCount, int cubeType) {
        OutPacket outPacket = new OutPacket(OutHeader.CUBE_SYSTEM_RELEASE);

        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(cubeIndex);
        outPacket.encodeInt(0);
        outPacket.encodeByte(15);
        outPacket.encodeInt(0);
        outPacket.encodeInt(cubeType);
        outPacket.encodeLong(equipSN);
        outPacket.encodeInt(cubeCount);

        return outPacket;
    }

    public static OutPacket consumeBrightCubeInit(int cubeIndex, int cubeCount, Item cube, long equipSN, Equip equip, boolean bonus) {
        OutPacket outPacket = new OutPacket(OutHeader.CUBE_SYSTEM_RELEASE);

        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(cubeIndex);
        outPacket.encodeInt(0);
        outPacket.encodeByte(15);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(cubeCount);
        outPacket.encodeInt(1);
        outPacket.encodeInt(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(cube.getItemId());
        outPacket.encodeShort(cube.getBagIndex());
        outPacket.encodeLong(equipSN);
        outPacket.encodeInt(equip.getItemId());
        outPacket.encodeInt(equip.getInvType().getVal() == EQUIPPED.getVal() ? -equip.getBagIndex() : equip.getBagIndex());
        outPacket.encodeByte(1);
        outPacket.encodeByte(equip.getGrade());
        for (int i = 0; i < 7; i++) {
            outPacket.encodeShort(equip.getOptions().get(i)); // 7x, last is fusion anvil
        }
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket violetCubeInit(int cubeIndex, int mode, int qty, Item cube, long equipSN, Equip equip, List<Integer> Potentials) {
        OutPacket outPacket = new OutPacket(OutHeader.CUBE_SYSTEM_RELEASE);

        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(cubeIndex);
        outPacket.encodeInt(0);
        outPacket.encodeByte(17);
        outPacket.encodeInt(0);
        outPacket.encodeByte(mode);
        outPacket.encodeLong(equipSN);
        outPacket.encodeInt(qty);
        if (mode == 0) {
            outPacket.encodeInt(Potentials.size() == 6 ? 3 : 2); //Potential Lines to select acording to the existing potential lines
            outPacket.encodeByte(Potentials.size());
            for (int i = 0; i < Potentials.size(); i++) {
                outPacket.encodeInt(Potentials.get(i)); //PotentialID
            }
        }

        return outPacket;
    }

    public static OutPacket consumeBrightCubeRelease(int cubeIndex) {
        OutPacket outPacket = new OutPacket(OutHeader.CUBE_SYSTEM_RELEASE);

        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(cubeIndex);
        outPacket.encodeByte(0);
        outPacket.encodeByte(true);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket flipTheCoinEnabled(byte b) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_FLIP_THE_COIN_ENABLED);

        outPacket.encodeByte(b);

        return outPacket;
    }

    public static OutPacket updateCharacterPresetResult(int preset) {
        OutPacket outPacket = new OutPacket(OutHeader.UPDATE_CHARACTER_PRESET_RESULT);

        outPacket.encodeInt(preset);
        // 0: SUCCESS
        // 2: SID_CHARACTER_PRESET_ERROR_MONEY
        // 3: SID_CHARACTER_PRESET_ERROR_LIVE_VALUE_ALL
        // 4: SID_CHARACTER_PRESET_ERROR_UNIVERSE
        // 5: SID_CHARACTER_PRESET_ERROR_BAD_FIELD
        // 6: SID_CHARACTER_PRESET_ERROR_INVALID_NAME

        return outPacket;
    }

    public static OutPacket updateExpDropPenalty(boolean first, int totaltime, int nowtime, int exp, int drop) {
        OutPacket outPacket = new OutPacket(OutHeader.EXP_DROP_PENALTY);

        outPacket.encodeShort(first ? 0 : 1);
        outPacket.encodeInt(totaltime);
        outPacket.encodeInt(nowtime);
        outPacket.encodeInt(exp);
        outPacket.encodeInt(drop);

        return outPacket;
    }

    public static OutPacket achievementUpdate(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.ACHIEVEMENT_UPDATE);

        outPacket.encodeByte(type);

        return outPacket;
    }

    public static OutPacket passiveDebuffSkillRequest(int[] skills) {
        OutPacket outPacket = new OutPacket(OutHeader.PASSIVE_DEBUFF_SKILL_REQUEST);

        outPacket.encodeInt(skills.length);
        for (var skillID : skills) {
            outPacket.encodeInt(skillID);
        }

        return outPacket;
    }

    public static OutPacket updateAscentSkillStackRequest(List<Integer> skills) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_ASCENT_SKILL_STACK_REQUEST);

        outPacket.encodeInt(skills.size());
        for (var skillID : skills) {
            outPacket.encodeInt(skillID);
            outPacket.encodeInt(3);
        }

        return outPacket;
    }

    public static OutPacket updateSkillStackRequestResult(int skillID, byte stack) {
        OutPacket outPacket = new OutPacket(OutHeader.STACK_SKILL_REQUEST_RESULT);

        outPacket.encodeInt(skillID);
        outPacket.encodeByte(stack);
        outPacket.encodeInt(-1);

        return outPacket;
    }

    public static OutPacket allStackSkillsResult() {
        OutPacket outPacket = new OutPacket(OutHeader.ALL_STACK_SKILLS_RESULT);

        var stackSkills = SkillConstants.STACK_SKILLS;
        outPacket.encodeInt(stackSkills.length);
        for (int i = 0; i < stackSkills.length; i++) {
            outPacket.encodeInt(stackSkills[i]);
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket updateHyperPresets(Char chr, int index, byte action) {
        OutPacket outPacket = new OutPacket(OutHeader.HYPER_STATS_PRESET);

        outPacket.encodeByte(index);
        outPacket.encodeByte(action);
        if (action != 0) {
            for (int i = 0; i <= 2; i++) {
                outPacket.encodeInt(chr.getHyperStats(i).size());
                for (HyperStat hyperStat : chr.getHyperStats(i)) {
                    hyperStat.encode(outPacket);
                }
            }
        }

        return outPacket;
    }

    public static OutPacket startBossUI(int orderId) {
        OutPacket outPacket = new OutPacket(OutHeader.BOSS_UI);

        final int[] diffs = BossPartyType.getDifficultyIdsByOrderId(orderId);
        outPacket.encodeInt(orderId);
        outPacket.encodeByte(false);
        outPacket.encodeInt(diffs.length);
        for (int diffId : diffs) {
            outPacket.encodeInt(diffId);
            outPacket.encodeInt(7);
            outPacket.encodeInt(0);
            outPacket.encodeByte(1);
            outPacket.encodeInt(7);
            outPacket.encodeByte(0);
        }

        // Buffs
        outPacket.encodeArr("03 00 00 00 00 00 00 00 08 00 00 00 00 00 00 00 10 C2 C4 04 01 00 00 00 11 C2 C4 04 02 00 00 00 12 C2 C4 04 03 00 00 00 13 C2 C4 04 04 00 00 00 14 C2 C4 04 05 00 00 00 15 C2 C4 04 06 00 00 00 16 C2 C4 04 07 00 00 00 17 C2 C4 04 01 00 00 00 0B 00 00 00 00 00 00 00 14 1D E1 FF 01 00 00 00 13 1D E1 FF 02 00 00 00 12 1D E1 FF 03 00 00 00 11 1D E1 FF 04 00 00 00 10 1D E1 FF 05 00 00 00 0F 1D E1 FF 06 00 00 00 0E 1D E1 FF 07 00 00 00 0D 1D E1 FF 08 00 00 00 0C 1D E1 FF 09 00 00 00 0B 1D E1 FF 0A 00 00 00 0A 1D E1 FF 02 00 00 00 0F 00 00 00 00 00 00 00 FF 1C E1 FF 01 00 00 00 05 1D E1 FF 02 00 00 00 06 1D E1 FF 03 00 00 00 FE 1C E1 FF 04 00 00 00 FD 1C E1 FF 05 00 00 00 04 1D E1 FF 06 00 00 00 09 1D E1 FF 07 00 00 00 08 1D E1 FF 08 00 00 00 07 1D E1 FF 09 00 00 00 03 1D E1 FF 0A 00 00 00 02 1D E1 FF 0B 00 00 00 01 1D E1 FF 0C 00 00 00 00 1D E1 FF 0D 00 00 00 D3 1C E1 FF 0E 00 00 00 9D 1C E1 FF 03 00 00 00 00 00 00 00 08 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 01 00 00 00 02 00 00 00 02 00 00 00 03 00 00 00 03 00 00 00 04 00 00 00 04 00 00 00 05 00 00 00 05 00 00 00 06 00 00 00 06 00 00 00 07 00 00 00 07 00 00 00 01 00 00 00 0B 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 01 00 00 00 02 00 00 00 02 00 00 00 03 00 00 00 03 00 00 00 04 00 00 00 04 00 00 00 05 00 00 00 05 00 00 00 06 00 00 00 06 00 00 00 07 00 00 00 07 00 00 00 08 00 00 00 08 00 00 00 09 00 00 00 09 00 00 00 0A 00 00 00 0A 00 00 00 02 00 00 00 0F 00 00 00 00 00 00 00 0D 00 00 00 01 00 00 00 0E 00 00 00 02 00 00 00 00 00 00 00 03 00 00 00 01 00 00 00 04 00 00 00 02 00 00 00 05 00 00 00 03 00 00 00 06 00 00 00 04 00 00 00 07 00 00 00 05 00 00 00 08 00 00 00 06 00 00 00 09 00 00 00 07 00 00 00 0A 00 00 00 08 00 00 00 0B 00 00 00 09 00 00 00 0C 00 00 00 0A 00 00 00 0D 00 00 00 0B 00 00 00 0E 00 00 00 0C 00 00 00 00 00 00 00 06 00 00 00 FD 1C E1 FF 01 00 00 00 FE 1C E1 FF FE 1C E1 FF 01 00 00 00 FD 1C E1 FF 05 1D E1 FF 01 00 00 00 06 1D E1 FF 06 1D E1 FF 01 00 00 00 05 1D E1 FF 16 C2 C4 04 01 00 00 00 17 C2 C4 04 17 C2 C4 04 01 00 00 00 16 C2 C4 04 02 00 00 00 9D 1C E1 FF C8 00 00 00 79 AE 07 00 06 00 71 53 74 61 74 65 01 00 31 D3 1C E1 FF C8 00 00 00 79 AE 07 00 06 00 71 53 74 61 74 65 01 00 31");

        return outPacket;
    }

    public static OutPacket aranDireWolfCurse(Map<Mob, Integer> mobs) {
        OutPacket outPacket = new OutPacket(OutHeader.ARAN_DIRE_WOLF_CURSE);

        outPacket.encodeInt(mobs.size());
        for (var entry : mobs.entrySet()) {
            var mob = entry.getKey();
            var direWolfCurseCount = entry.getValue();

            outPacket.encodeInt(mob.getObjectId());
            outPacket.encodeInt(direWolfCurseCount);
            outPacket.encodeInt(10000); // curse duration
        }

        return outPacket;
    }
}
