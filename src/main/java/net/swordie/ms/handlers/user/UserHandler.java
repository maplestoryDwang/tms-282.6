package net.swordie.ms.handlers.user;

import net.swordie.ms.client.jobs.special.SpecialJobProgression;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.client.character.tms282.Tms282VMatrixSpecialEngine;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.swordie.ms.BannedMachines;
import net.swordie.ms.DiscordAPI;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.*;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.achievement.AchievementRank;
import net.swordie.ms.client.character.damage.DamageSkinSaveData;
import net.swordie.ms.client.character.damage.DamageSkinType;
import net.swordie.ms.client.character.hexa.HexaStat;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.keys.FuncKeyMap;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.client.character.potential.CharacterPotentialMan;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.reward.RewardInfo;
import net.swordie.ms.client.character.reward.RewardResult;
import net.swordie.ms.client.character.reward.RewardSystem;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;
import net.swordie.ms.client.character.skills.info.SkillAlarmInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.character.union.Union;
import net.swordie.ms.client.character.union.UnionBoard;
import net.swordie.ms.client.character.union.UnionMember;
import net.swordie.ms.client.daily.DailyCoin;
import net.swordie.ms.client.daily.DailyGift;
import net.swordie.ms.client.daily.DailyGiftItemInfo;
import net.swordie.ms.client.jobs.Jianghu.Lynn;
import net.swordie.ms.client.jobs.Jianghu.MoXuan;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.adventurer.thief.Shadower;
import net.swordie.ms.client.jobs.adventurer.warrior.DarkKnight;
import net.swordie.ms.client.jobs.cygnus.ThunderBreaker;
import net.swordie.ms.client.jobs.flora.Illium;
import net.swordie.ms.client.jobs.legend.Evan;
import net.swordie.ms.client.jobs.legend.Luminous;
import net.swordie.ms.client.jobs.legend.Phantom;
import net.swordie.ms.client.jobs.legend.Shade;
import net.swordie.ms.client.jobs.nova.AngelicBuster;
import net.swordie.ms.client.jobs.nova.Kaiser;
import net.swordie.ms.client.jobs.resistance.BattleMage;
import net.swordie.ms.client.jobs.resistance.WildHunter;
import net.swordie.ms.client.jobs.resistance.Xenon;
import net.swordie.ms.client.social.Guild.GuildMember;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.enums.reward.RewardItemType;
import net.swordie.ms.enums.reward.RewardSystemType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.life.drop.DropInfo;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.loaders.*;
import net.swordie.ms.loaders.Etc.HexaCore.HexaCore;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.QuestInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.event.BountyHunting;
import net.swordie.ms.world.event.DefenseTowerWave;
import net.swordie.ms.world.event.FrittoDancing;
import net.swordie.ms.world.event.FrittoEagle;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;
import net.swordie.ms.world.field.Portal;
import net.swordie.ms.world.partyquest.DefenseEvent;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.constants.GameConstants.*;

public class UserHandler {

    @Handler(op = InHeader.USER_MOVE)
    public static void handleUserMove(Char chr, InPacket inPacket) {
        // CVecCtrlUser::EndUpdateActive
        byte fieldKey = inPacket.decodeByte();
        inPacket.decodeInt(); // ? something with field
        inPacket.decodeInt(); // tick
        inPacket.decodeByte(); // ? doesn't get set at all
        // CMovePathCommon::Encode
        MovementInfo movementInfo = new MovementInfo(inPacket);
        applyUserMove(chr, movementInfo, UserMoveReplies.GMS265);
    }

    /**
     * The server side of a user move, shared by every protocol profile: applies the decoded path to {@code chr},
     * broadcasts it to the rest of the field and runs the move triggers (affected areas, fall-out failsafe, skills
     * removed when the user stops, Tide of Battle). {@code replies} builds the profile's packets.
     */
    public static void applyUserMove(Char chr, MovementInfo movementInfo, UserMoveReplies replies) {
        Field field = chr.getField();
        movementInfo.applyTo(chr);
        field.checkCharInAffectedAreas(chr);
        field.broadcast(replies.remoteMove(chr, movementInfo), chr);
        Position newPos = chr.getPosition();
        if (newPos != null) {
            if (chr.getField().getId() == HUNGRY_MUTO_HARD_STAGE || chr.getField().getId() == HUNGRY_MUTO_NORMAL_STAGE) {
                int x = newPos.getX();
                int y = newPos.getY();
                if (x >= 1250 && x <= 1600 && y >= -113 && y <= 150) {
                    chr.checkIngredients();
                }
            }
            if (newPos.getY() > 5000) {
                // failsafe when the char falls outside of the map
                Portal portal = field.getDefaultPortal();
                Position position = new Position(portal.getX(), portal.getY());
                chr.setPosition(position);
                chr.write(replies.teleport(chr, position));
            }
        }
        if (chr.getMoveAction() == 4 || chr.getMoveAction() == 5) {
            // client has stopped moving. this might not be the best way
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            for (int skill : Job.REMOVE_ON_STOP) {
                if (tsm.hasStatBySkillId(skill)) {
                    tsm.removeStatsBySkill(skill);
                }
            }
        }
        if (chr.hasSkill(150000017) || chr.hasSkill(80000268)) {
            chr.getJobHandler().handleTideOfBattle();
        }
    }

    @Handler(op = InHeader.USER_HIT)
    public static void handleUserHit(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) return;
        if (c.isExiting()) {
            c.dispose();
            return;
        }
        chr.getJobHandler().handleUserHit(c, inPacket);
    }

    @Handler(op = InHeader.USER_SET_CUSTOMIZE_EFFECT)
    public static void handleUserSetCustomizeEffect(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        int slotPos = inPacket.decodeInt();
        Item item = chr.getInstallInventory().getItemBySlot(slotPos);
        if (item == null || item.getItemId() != itemID) {
            chr.dispose();
            return;
        }
        chr.setCustomizeEffect(itemID);
        chr.getField().broadcast(UserRemote.setCustomizeEffect(chr, itemID, chr.getCustomizeEffectMsg()));
    }

    @Handler(op = InHeader.USER_GROWTH_HELPER_REQUEST)
    public static void handleUserGrowthHelperRequest(Char chr, InPacket inPacket) {
        Field currentField = chr.getField();
        if ((currentField.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) > 0) {
            chr.dispose();
            return;
        }
        short status = inPacket.decodeByte();
        if (status == 0) {
            int fieldID = inPacket.decodeInt();
            int portalID = inPacket.decodeByte();
            int reqLevel = ContentsGuide.getReqLevelByFieldID(fieldID);
            Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldID);
            if (field == null || chr.getLevel() < reqLevel) {
                chr.chatMessage("無法移動到這張地圖。");
                chr.dispose();
                return;
            }
            FileTime ft = FileTime.fromDate(FileTime.nowUTC());
            chr.setQRValueByKey(501922, "ltf", ft.toYYYYMMDD_HHMMSS());
            chr.warp(fieldID, portalID);
        } else if (status == 2) {
            byte unk = inPacket.decodeByte();
        }  else if (status == 3) {
            int questID = 101079;
            String value = "";
            List<Integer> types = new LinkedList<>();
            int size = inPacket.decodeInt();
            for (int i = 0; i < size; i++) {
                types.add(inPacket.decodeInt());
            }
            int i = 0;
            for (int type : types) {
                ContentsGuide.FieldInfo fieldType = ContentsGuide.getFieldInfoByGroupName(String.valueOf(type));
                value += i + "="+ fieldType.type + "/" + fieldType.fieldID;
                if (i < size - 1) {
                    value += ";";
                }
                i += 1;
            }
            chr.createQuestWithQRValue(questID, value);
        } else if (status == 4) {
            int key = inPacket.decodeInt();
            Map<Integer, Integer> rewards = new HashMap<>();
            int qrValue = 0;
            switch (key) {
                case 0:
                    rewards.put(2634282, 1); // Sprout Box
                    qrValue = 30;
                    if (chr.getConsumeInventory().getEmptySlots() < 1) {
                        chr.chatPopup("消耗欄至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    break;
                case 1:
                    rewards.put(1005942, 1); // Budding Sprout Hat
                    qrValue = 60;
                    if (chr.getEquipInventory().getEmptySlots() < 1) {
                        chr.chatPopup("裝備欄至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    break;
                case 2:
                    rewards.put(2634366, 1); // Selective 8-slot Coupon
                    qrValue = 80;
                    if (chr.getConsumeInventory().getEmptySlots() < 1) {
                        chr.chatPopup("消耗欄至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    break;
                case 4:
                    rewards.put(4001832, 3000); // Spell Trace
                    qrValue = 100;
                    if (chr.getEtcInventory().getEmptySlots() < 1) {
                        chr.chatPopup("其他欄至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    break;
                case 5:
                    rewards.put(2634366, 1); // Selective 8-slot Coupon
                    qrValue = 120;
                    if (chr.getConsumeInventory().getEmptySlots() < 1) {
                        chr.chatPopup("消耗欄至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    break;
                case 8:
                    rewards.put(2634280, 1); // Budding Sprout Damage Skin
                    rewards.put(2633349, 1); // Extreme Growth Potion (+10 Level)
                    if (chr.getConsumeInventory().getEmptySlots() < 2) {
                        chr.chatPopup("消耗欄至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    qrValue = 140;
                    break;
                case 10:
                    rewards.put(2633242, 1); // Trait Boost Potion
                    rewards.put(2633349, 1); // Extreme Growth Potion (+10 Level)
                    if (chr.getConsumeInventory().getEmptySlots() < 2) {
                        chr.chatPopup("消耗欄至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    qrValue = 160;
                    break;
                case 11:
                    rewards.put(1703168, 1); // Budding Sprout Weapon
                    rewards.put(2633349, 1); // Extreme Growth Potion (+10 Level)
                    qrValue = 180;
                    if (chr.getConsumeInventory().getEmptySlots() < 1 && chr.getEquipInventory().getEmptySlots() < 1) {
                        chr.chatPopup("裝備欄和消耗欄各至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    break;
                case 14:
                    rewards.put(1143258, 1); // Promising Kid
                    qrValue = 200;
                    if (chr.getEquipInventory().getEmptySlots() < 1) {
                        chr.chatPopup("裝備欄至少需要 1 個空格才能領取。");
                        chr.dispose();
                        return;
                    }
                    break;
            }
            if (chr.getLevel() < qrValue) {
                chr.chatPopup("需要達成領取條件才能領取此獎勵。");
                chr.dispose();
                return;
            }
            if (qrValue != 0) {
                if (chr.getQRValueByKey(501922, qrValue + "") != null) {
                    chr.chatPopup("這個帳號已經領取過此獎勵。");
                    chr.dispose();
                    return;
                }
                // check and set qrkey here
                for (Map.Entry<Integer, Integer> entry : rewards.entrySet()) {
                    int itemID = entry.getKey();
                    int qty = entry.getValue();
                    if (ItemData.getItemInfoByID(itemID) != null && qty >= 1) {
                        chr.addItemToInventory(entry.getKey(), entry.getValue());
                    }
                }
                FileTime ft = FileTime.fromDate(FileTime.nowUTC());
                if (!chr.hasQuest(501922)) {
                    chr.createQuestWithQRValue(501922, qrValue + "=" + chr.getId() + ";ltf=" + ft.toYYYYMMDD_HHMMSS());
                } else {
                    chr.setQRValueByKey(501922, qrValue, chr.getId());
                    chr.setQRValueByKey(501922, "ltf", ft.toYYYYMMDD_HHMMSS());
                }
                chr.chatMessage("<楓之谷指南> 等級 " + qrValue + " 獎勵已發放。");
            }
        }
    }

    @Handler(ops = {InHeader.USER_CONTENTS_MAP_REQUEST, InHeader.QUICK_MOVE_NAVIGATION_REQUEST})
    public static void handleUserContentsMapRequest(Char chr, InPacket inPacket) {
        Field currentField = chr.getField();
        if ((currentField.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) > 0) {
            chr.dispose();
            return;
        }
        int fieldID = inPacket.decodeInt();
        int reqLevel = ContentsGuide.getReqLevelByFieldID(fieldID);
        Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldID);
        if (field == null || chr.getLevel() < reqLevel) {
            chr.chatMessage("你無法移動到該地圖。");
            chr.dispose();
            return;
        }
        FileTime ft = FileTime.fromDate(FileTime.nowUTC().plusMinutes(5));
        chr.createQuestWithQRValue(1953, "can=" + ft.toYYYYMMDD_HHMMssSSS_QR());
        chr.warp(fieldID);
    }

    @Handler(op = InHeader.FUNC_KEY_MAPPED_MODIFIED)
    public static void handleFuncKeyMappedModified(Char chr, InPacket inPacket) {
        int updateType = inPacket.decodeByte();
        switch (updateType) {
            case 0:
                int preset = inPacket.decodeByte();
                int unk = inPacket.decodeInt(); // -1
                FuncKeyMap funcKeyMap = chr.getFuncKeyMapByPreset(preset);
                int size = inPacket.decodeByte();
                for (int i = 0; i < size; i++) {
                    int index = inPacket.decodeByte();
                    byte type = inPacket.decodeByte();
                    int value = inPacket.decodeInt();
                    if (funcKeyMap != null) {
                        funcKeyMap.putKeyBinding(chr.getId(), index, type, value);
                    }
                }
                break;
            case 1: // HP potion
                int data = inPacket.decodeInt();
                if (data <= 0) {
                    chr.removeQuest(69991);
                    chr.write(FieldPacket.petConsumeItemInit(0));
                    break;
                }
                chr.createQuestWithQRValue(69991, "id=" + data);
                chr.write(FieldPacket.petConsumeItemInit(data));
                if (EventConstants.HYPER_BURNING_MAX && chr.hasQuest(102431)
                        && !"3".equals(chr.getQRValueByKey(102431, "step"))) {
                    chr.getScriptManager().startScript(chr, 102431, "q102431s_2", ScriptType.Quest);
                }
                break;
            case 2: // MP potion
                data = inPacket.decodeInt();
                if (data <= 0) {
                    chr.removeQuest(69992);
                    chr.write(FieldPacket.petConsumeMPItem(0));
                    break;
                }
                chr.createQuestWithQRValue(69992, "id=" + data);
                chr.write(FieldPacket.petConsumeMPItem(data));
                if (EventConstants.HYPER_BURNING_MAX && chr.hasQuest(102431)
                        && !"3".equals(chr.getQRValueByKey(102431, "step"))) {
                    chr.getScriptManager().startScript(chr, 102431, "q102431s_2", ScriptType.Quest);
                }
                break;
            case 3: // change preset
                data = inPacket.decodeByte();
                if (data >= 0 && data <= 2) {
                    if (chr.hasQuest(QuestConstants.KEY_BINDINGS_PRESET)) {
                        chr.setQRValueByKey(QuestConstants.KEY_BINDINGS_PRESET, "no", data + "");
                    } else {
                        chr.createQuestWithQRValue(QuestConstants.KEY_BINDINGS_PRESET, "no=" + data + ";unionkey=1");
                    }
                }
                break;
            case 4: // idk
                data = inPacket.decodeByte();
                if (chr.hasQuest(QuestConstants.KEY_BINDINGS_PRESET)) {
                    chr.setQRValueByKey(QuestConstants.KEY_BINDINGS_PRESET, "unionkey", data + "");
                } else {
                    chr.createQuestWithQRValue(QuestConstants.KEY_BINDINGS_PRESET, "no=0;unionkey=" + data);
                }
                break;
            case 6: // Cure potion : 2050004; 2022178; 2001556
                data = inPacket.decodeInt();
                if (data <= 0) {
                    chr.removeQuest(69993);
                    chr.write(FieldPacket.petConsumeCureItem(0));
                    break;
                }
                chr.createQuestWithQRValue(69993, "id=" + data);
                chr.write(FieldPacket.petConsumeCureItem(data));
                break;
        }
    }


    @Handler(op = InHeader.USER_CHARACTER_INFO_SELF_REQUEST)
    public static void handleUserCharacterInfoSelfRequest(Char chr, InPacket inPacket) {
        chr.write(UserPacket.characterSelfInfo(0, true));
    }

    @Handler(op = InHeader.USER_CHARACTER_INFO_REQUEST)
    public static void handleUserCharacterInfoRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        inPacket.decodeInt(); // tick
        String name = inPacket.decodeString();
        Char requestChar = field.getCharByName(name);
        if (name.equals(chr.getName())) {
            requestChar = chr;
        }
        if (requestChar == null) {
            chr.chatMessage("找不到該角色的數據。");
        } else {
            chr.write(FieldPacket.characterInfo(requestChar));
        }
    }

    @Handler(op = InHeader.BATTLE_RECORD_ON_OFF_REQUEST)
    public static void handleBattleRecordOnOffRequest(Client c, InPacket inPacket) {
        // CBattleRecordMan::RequestOnCalc
        Char chr = c.getChr();
        if (chr == null) return;
        boolean on = inPacket.decodeByte() != 0;
        boolean isNew = inPacket.decodeByte() != 0;
        boolean clear = inPacket.decodeByte() != 0;
        chr.setBattleRecordOn(on);
        chr.write(BattleRecordMan.serverOnCalcRequestResult(on));
    }

    @Handler(op = InHeader.BATTLE_RECORD_SKILL_DAMAGE_LOG)
    public static void handleBattleRecordSkillDamageLog(Char chr, InPacket inPacket) {
        // CBattleRecordMan::OnSkillDamageLog
        if (chr.isBattleRecordOn()) {
            chr.write(BattleRecordMan.skillDamageLog(0));
        } else {
            short time = inPacket.decodeShort();
            int startFieldID = inPacket.decodeInt();
            int endFieldID = inPacket.decodeInt();
            short maxSTR = inPacket.decodeShort();
            short maxDEX = inPacket.decodeShort();
            short maxINT = inPacket.decodeShort();
            short maxLUK = inPacket.decodeShort();
            int maxHP = inPacket.decodeInt();
            long totalDamage = inPacket.decodeLong();
            long exp = inPacket.decodeLong();
            long money = inPacket.decodeLong();
            int mask = inPacket.decodeInt();
            inPacket.decodeLong(); // damage?
            inPacket.decodeLong(); // damage?
            chr.write(BattleRecordMan.skillDamageLog(time));
        }
    }

    @Handler(op = InHeader.USER_SIT_REQUEST)
    public static void handleUserSitRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int fieldSeatId = inPacket.decodeShort();
        var chair = chr.getChair();
        int itemID = chair != null ? chair.getItemID() : 0;
        switch (itemID / 100) {
            case 30162 -> {
            }
            case 30161 -> {
            }
        }
        chr.setChair(new PortableChair(chr, 0, ChairType.None));
        chr.write(FieldPacket.sitResult(chr.getId(), fieldSeatId));
        field.broadcast(UserRemote.remoteSetActivePortableChair(chr.getId(), chr.getChair()), chr);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_PORTABLE_CHAIR_SIT_REQUEST)
    public static void handleUserPortableChairSitRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int fieldId = inPacket.decodeInt(); // fieldId
        int itemId = inPacket.decodeInt(); // item id
        int pos = inPacket.decodeInt(); // setup position
        byte chairBag = inPacket.decodeByte(); // is Chair in a bag
        Position charPos = inPacket.decodePositionInt();
        if (ItemConstants.isTowerChair(itemId)) {
            if (!chr.hasQuest(QuestConstants.TOWER_CHAIR_SETTING_QUEST)) {
                chr.createQuestWithQRValue(QuestConstants.TOWER_CHAIR_SETTING_QUEST,
                        String.format(QuestConstants.TOWER_CHAIR_SETTING_FORMAT, itemId, 0, 0, 0, 0, 0));
            }
            PortableChair chair = new PortableChair(chr, itemId, ChairType.TowerChair);
            chr.setChair(chair);
            field.broadcast(UserRemote.remoteSetActivePortableChair(chr.getId(), chr.getChair()), chr);
        } else if (ItemConstants.isTextChair(itemId)) { // text chair
            String msg = inPacket.decodeString();
            PortableChair chair = new PortableChair(chr, itemId, ChairType.TextChair);
            chair.setMsg(msg);
            chr.setChair(chair);
            field.broadcast(UserRemote.remoteSetActivePortableChair(chr.getId(), chr.getChair()), chr);
        } else if (itemId == 3015440 || itemId == 3015650 || itemId == 3015651) {
            long meso = inPacket.decodeLong();
            PortableChair chair = new PortableChair(chr, itemId, ChairType.MesoChair);
            chair.setMeso(meso);
            chr.setChair(chair);
            field.broadcast(UserRemote.remoteSetActivePortableChair(chr.getId(), chr.getChair()), chr);
        } else {
            PortableChair chair = new PortableChair(chr, itemId, ChairType.NormalChair);
            chr.setChair(chair);
            field.broadcast(UserRemote.remoteSetActivePortableChair(chr.getId(), chr.getChair()));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_EMOTION_ITEM_USE_REQUEST)
    public static void handleUserEmotionItemUseRequest(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        int pos = inPacket.decodeInt();
        boolean isBag = inPacket.decodeByte() != 0;
        Item item = chr.getInstallInventory().getItemBySlot(pos);
        if (item == null || item.getItemId() != itemID) {
            chr.dispose();
            return;
        }
        chr.getField().broadcast(FieldPacket.setActiveEmotionItem(chr, itemID));
        chr.dispose();
    }

    @Handler(op = InHeader.USER_EMOTION)
    public static void handleUserEmotion(Char chr, InPacket inPacket) {
        int emotion = inPacket.decodeInt();
        int duration = inPacket.decodeInt();
        boolean byItemOption = inPacket.decodeByte() != 0;
        if (GameConstants.isValidEmotion(emotion)) {
            chr.getField().broadcast(UserRemote.emotion(chr.getId(), emotion, duration, byItemOption), chr);
        }
    }

    @Handler(op = InHeader.USER_SOUL_EFFECT_REQUEST)
    public static void handleUserSoulEffectRequest(Char chr, InPacket inPacket) {
        byte set = inPacket.decodeByte();
        int questID = QuestConstants.SOUL_EFFECT_SHOW;
        if (!chr.hasQuest(questID)) {
            chr.createQuestWithQRValue(questID, "effect=" + set);
        } else {
            chr.setQRValueByKey(questID, "effect", set + "");
        }
        questID = QuestConstants.OVERAL_SETTING;
        if (!chr.hasQuest(questID)) {
            chr.createQuestWithQRValue(questID, "acpHP=0;alFol=1;chAl=1;alAl=1;alPa=1;alExch=1;alHome=1;soErUI=0;alMe=1;bufFav=0;chFr=1;alFr=1;bufAual=1;alMapT=1;chGu=1;frOnNot=1;flHP=10;alGu=1;bufMin=0;soulUI="+set+";alWh=1;flMP=10;debufMin=0");
        } else {
            chr.setQRValueByKey(questID, "soulUI", set + "");
        }
        chr.getField().broadcast(UserPacket.SetSoulEffect(chr.getId(), set != 0));
    }

    @Handler(op = InHeader.MONSTER_BOOK_MOB_INFO)
    public static void handleMonsterBookMobInfo(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        int cardID = inPacket.decodeInt();
        chr.dispose();
        // Unhandled
    }

    @Handler(op = InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_CHANGE_PRESET)
    public static void handleUserRequestCharacterPotentialSkillChangePreset(Char chr, InPacket inPacket) {
        byte preset = inPacket.decodeByte();
        if (preset > 2 && preset < 0) {
            chr.chatPopup("未知錯誤。");
            chr.dispose();
            return;
        }
        chr.createQuestWithQRValue(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential=" + preset);
        chr.write(WvsContext.characterPotentialChangePreset(true, preset));
    }

    @Handler(op = InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_VALUE_SET)
    public static void handleUserRequestCharacterPotentialSkillRandSetUi(Char chr, InPacket inPacket) {
        byte preset = inPacket.decodeByte();
        if (preset > 2 && preset < 0) {
            chr.chatPopup("未知錯誤。");
            chr.dispose();
            return;
        }
        int size = inPacket.decodeInt();
        Set<CharacterPotential> lockedLines = new HashSet<>();
        for (int i = 0; i < size; i++) {
            CharacterPotential newCP = new CharacterPotential(preset, inPacket.decodeByte(), inPacket.decodeInt(), inPacket.decodeByte(), inPacket.decodeByte());
            lockedLines.add(newCP);
        }
        CharacterPotentialMan cpm = chr.getPotentialMan();
        byte grade = cpm.getGrade(preset);
        int cost = GameConstants.CHAR_POT_RESET_COST;
        switch (grade) {
            case 1: // epic
                cost = 200;
                break;
            case 2: // unique
                cost = 1500;
                break;
            case 3: // legendary
                cost = 8000;
                break;
        }
        switch (lockedLines.size()) {
            case 1 -> cost += GameConstants.CHAR_POT_LOCK_1_COST;
            case 2 -> cost += GameConstants.CHAR_POT_LOCK_1_COST + GameConstants.CHAR_POT_LOCK_2_COST;
        }
        if (cost > chr.getHonorExp()) {
            chr.chatMessage("名聲值不足，無法執行此動作。");
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to reset honor without having enough exp (required %d, has %d)", chr.getId(), cost, chr.getHonorExp()));
            chr.dispose();
            return;
        }
        chr.addHonorExp(-cost);
        boolean gradeUp;
        boolean gradeDown;
        if (grade == CharPotGrade.Epic.ordinal()) {
            gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_EPIC_POT_UP_RATE);
            gradeDown = Util.succeedProp(GameConstants.BASE_CHAR_EPIC_POT_DOWN_RATE);
        } else if (grade == CharPotGrade.Unique.ordinal()) {
            gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_UNIQUE_POT_UP_RATE);
            gradeDown = Util.succeedProp(GameConstants.BASE_CHAR_UNIQUE_POT_DOWN_RATE);
        } else if (grade == CharPotGrade.Legendary.ordinal()) {
            gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_LEGENDARY_POT_UP_RATE);
            gradeDown = Util.succeedProp(GameConstants.BASE_CHAR_LEGENDARY_POT_DOWN_RATE);
        } else {
            gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_RARE_POT_UP_RATE);
            gradeDown = Util.succeedProp(GameConstants.BASE_CHAR_RARE_POT_DOWN_RATE);
        }
        // update grades
        if (grade < CharPotGrade.Legendary.ordinal() && gradeUp) {
            grade++;
        } else if (grade > CharPotGrade.Rare.ordinal() && gradeDown) {
            grade--;
        }
        // set new potentials that weren't locked       
        for (CharacterPotential cp : chr.getPotentialsByPreset(preset)) {
            cp.setGrade(grade);
            boolean isLockedLine = false;
            if (!lockedLines.isEmpty()) {
                for (CharacterPotential lockCP : lockedLines) {
                    if (lockCP.equals(cp)) {
                        isLockedLine = true;
                        break;
                    }
                }
            }
            if (isLockedLine) {
                continue;
            }
            CharacterPotential newCP = cpm.generateRandomPotential(preset, cp.getKey());
            cp.setSkillID(newCP.getSkillID());
            cp.setSlv(newCP.getSlv());
            cp.saveToSQL();
        }
        chr.write(WvsContext.characterPotentialSet(chr, true, true, preset));
        chr.write(WvsContext.characterPotentialReset(chr));
        if (gradeUp) {
            chr.write(UserPacket.effect(Effect.avatarOriented("Effect/CharacterEff.img/GradeUp")));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.avatarOriented("Effect/CharacterEff.img/GradeUp")));
        }
    }

    @Handler(op = InHeader.RUNE_STONE_USE_REQ)
    public static void handleRuneStoneUseRequest(Char chr, InPacket inPacket) {
        if (!net.swordie.ms.GameSettings.runeEnabled()) { // feature.rune
            chr.chatMessage(net.swordie.ms.GameSettings.RUNE_DISABLED);
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); // unknown
        RuneType runeType = RuneType.getByVal(inPacket.decodeInt());
        if (runeType.getVal() != chr.getField().getRuneStone().getRuneType().getVal()) {
            chr.dispose();
            return;
        }
        int minLevel = chr.getField().getMobs().stream().mapToInt(m -> m.getForcedMobStat().getLevel()).min().orElse(0);
        boolean isCooldown = (chr.getRuneCooldown() + (FieldConstants.RUNE_COOLDOWN_TIME * 60000)) >= System.currentTimeMillis();
        if (!isCooldown) {
            if (minLevel > chr.getStat(Stat.level)) {
                chr.dispose();
                return;
            } else {
                RuneStone.RuneStoneAction[] actions = RuneStone.RuneStoneAction.values();
                RuneStone.RuneStoneAction action = actions[Randomizer.nextInt(actions.length)];
                chr.write(FieldPacket.runeStoneUseAck(9, 0, action.getPacket()));
            }
        } else {
            chr.write(FieldPacket.runeStoneUseAck(2, (int) ((chr.getRuneCooldown() + (FieldConstants.RUNE_COOLDOWN_TIME * 60000)) - System.currentTimeMillis()), null));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.RUNE_STONE_SKILL_REQ)
    public static void handleRuneStoneSkillRequest(Char chr, InPacket inPacket) {
        if (!net.swordie.ms.GameSettings.runeEnabled()) { // feature.rune
            chr.dispose();
            return;
        }
        boolean success = inPacket.decodeByte() != 0;
        if (success) {
            RuneStone runeStone = chr.getField().getRuneStone();
            chr.getField().useRuneStone(chr.getClient(), runeStone);
            chr.write(FieldPacket.runeStoneSkillAck(runeStone.getRuneType()));
            runeStone.activateRuneStoneEffect(chr);
            chr.setRuneCooldown(System.currentTimeMillis());
            if (Tms282VMatrixRuntime.enabled()) {
                Tms282VMatrixRuntime.event(chr, Tms282VMatrixSpecialEngine.Type.RUNE, 1, 0);
            } else {
                chr.getSpecialNodeSkill().setRune(true);
                chr.getSpecialNodeSkill().activate();
            }
            AchievementHandler.handleUseRuneStone(chr, runeStone.getRuneType().getVal());
        }
        chr.dispose();
    }

    @Handler(op = InHeader.MONSTER_COLLECTION_EXPLORE_REQ)
    public static void handleMonsterCollectionExploreReq(Char chr, InPacket inPacket) {
        int region = inPacket.decodeInt();
        int session = inPacket.decodeInt();
        int group = inPacket.decodeInt();
        int key = region * 10000 + session * 100 + group;
        Account account = chr.getAccount();
        MonsterCollection mc = account.getMonsterCollection();
        MonsterCollectionExploration mce = mc.getExploration(region, session, group);
        boolean complete = mc.isComplete(region, session, group);
        if (complete && mce == null) {
            // starting an exploration
            if (mc.getOpenExplorationSlots() <= 0) {
                chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.NotEnoughExplorationSlots, null, 0));
                return;
            }
            mce = mc.createExploration(region, session, group);
            mc.addExploration(mce);
            chr.write(UserLocal.collectionRecordMessage(mce.getPosition(), mce.getValue(true)));
            chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.ExploreBegin, null, 0));
        } else {
            // trying to start an incomplete/already exploring group
            chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.NoMonstersForExploring, null, 0));
        }
        chr.dispose(); // still required even if you send a collection result
    }

    @Handler(op = InHeader.MONSTER_COLLECTION_COMPLETE_REWARD_REQ)
    public static void handleMonsterCollectionCompleteRewardReq(Char chr, InPacket inPacket) {
        int reqType = inPacket.decodeInt(); // 0 = group
        int region = inPacket.decodeInt();
        int session = inPacket.decodeInt();
        int group = inPacket.decodeInt();
        int exploreIndex = inPacket.decodeInt();
        MonsterCollection mc = chr.getAccount().getMonsterCollection();
        switch (reqType) {
            case 0: // group
                MonsterCollectionGroup mcs = mc.getGroup(region, session, group);
                if (mcs != null && !mcs.isRewardClaimed() && mc.isComplete(region, session, group)) {
                    Tuple<Integer, Integer> rewardInfo = MonsterCollectionData.getReward(region, session, group);
                    Item item = ItemData.getItemDeepCopy(rewardInfo.getLeft());
                    item.setQuantity(rewardInfo.getRight());
                    chr.addItemToInventory(item);
                    mcs.setRewardClaimed(true);
                    chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.CollectionCompletionRewardSuccess, null, 0));
                } else if (mcs != null && mcs.isRewardClaimed()) {
                    chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.AlreadyClaimedReward, null, 0));
                } else {
                    chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.CompleteCollectionBeforeClaim, null, 0));
                }
                break;
            case 4: // exploration
                MonsterCollectionExploration mce = mc.getExploration(region, session, group);
                if (mce != null && mce.getEndDate().isExpired()) {
                    mc.removeExploration(mce);
                    chr.write(UserLocal.collectionRecordMessage(mce.getPosition(), mce.getValue(false)));
                    chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.CollectionCompletionRewardSuccess, null, 0));
                } else {
                    chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.TryAgainInAMoment, null, 0));
                }
                break;
            default:
                System.out.println("Unhandled MonsterCollectionCompleteRewardReq type " + reqType);
                chr.write(WvsContext.monsterCollectionResult(MonsterCollectionResultType.TryAgainInAMoment, null, 0));

        }
        chr.dispose(); // still required even if you send a collection result
    }

    @Handler(op = InHeader.USER_EFFECT_LOCAL)
    public static void handleUserEffectLocal(Char chr, InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        byte slv = inPacket.decodeByte();
        boolean sendLocal = inPacket.decodeByte() != 0;

        if (SpecialJobProgression.isSpecialSkill(skillId)) {
            int level = SpecialJobProgression.requestLevel(chr, skillId);
            if (level <= 0) return;
            slv = (byte) level;
        }

        int chrId = chr.getId();
        Field field = chr.getField();
        Effect effect = null;

        if (!chr.hasSkill(skillId) && skillId != 35000006 && skillId != 65101006 && skillId != 13121009 && skillId != 4211016 && skillId != 400041026) {
            if (StringData.getSkillStringById(skillId) != null) {
                DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("角色 %s 使用了未擁有的技能 %s (%d)。",
                        chr.getName(), StringData.getSkillStringById(skillId).getName(), skillId));
            }
        }

        if (skillId == Evan.DRAGON_FURY) {
            effect = Effect.showDragonFuryEffect(skillId, slv, 0, true);

        } else if (skillId == DarkKnight.FINAL_PACT_INFO) {
            effect = Effect.showFinalPactEffect(skillId, slv, 0, true);

        } else if (skillId == WildHunter.CALL_OF_THE_HUNTER) {
            effect = Effect.showCallOfTheHunterEffect(skillId, slv, 0, chr.isLeft(), chr.getPosition().getX(), chr.getPosition().getY());

        } else if (skillId == Kaiser.VERTICAL_GRAPPLE || skillId == AngelicBuster.GRAPPLING_HEART || skillId == Job.ROPE_LIFT) { // 'Grappling Hook' Skills
            int chrPositionY = inPacket.decodeInt();
            Position ropeConnectDest = inPacket.decodePositionInt();
            effect = Effect.showVerticalGrappleEffect(skillId, slv, 0, chrPositionY, ropeConnectDest.getX(), ropeConnectDest.getY());

        } else if (skillId == Luminous.FLASH_BLINK
                || skillId == ThunderBreaker.FLASH
                || skillId == Shade.FOX_TROT
                || skillId == Shadower.INTO_DARKNESS
                || skillId == Shadower.TRICKBLADE_FINISHER
                || skillId == Illium.CRYSTALLINE_WINGS) { // Flash
            Position origin = inPacket.decodePositionInt();
            Position dest = inPacket.decodePositionInt();
            effect = Effect.showFlashBlinkEffect(skillId, slv, 0, origin.getX(), origin.getY(), dest.getX(), dest.getY());

        } else if (SkillConstants.isSuperNovaSkill(skillId)) { // 'SuperNova' Skills
            Position chrPosition = inPacket.decodePositionInt();
            effect = Effect.showSuperNovaEffect(skillId, slv, 0, chrPosition.getX(), chrPosition.getY());

        } else if (SkillConstants.isUnregisteredSkill(skillId)) { // 'Unregistered' Skills
            effect = Effect.showUnregisteredSkill(skillId, slv, 0, chr.isLeft());

        } else if (SkillConstants.isHomeTeleportSkill(skillId)) {
            effect = Effect.skillUse(skillId, chr.getLevel(), slv);

        } else if (skillId == BattleMage.DARK_SHOCK) {
            Position origin = inPacket.decodePositionInt();
            Position dest = inPacket.decodePositionInt();
            effect = Effect.showDarkShockSkill(skillId, slv, origin, dest);

        } else if (skillId == Xenon.TRIANGULATION) {
            effect = Effect.showDragonFuryEffect(skillId, slv, 0, true);

        } else {
            System.out.printf("Unhandled Remote Effect Skill id %d%n", skillId);
        }

        if (effect != null) {
            if (sendLocal) {
                chr.write(UserPacket.effect(effect));
            }
            field.broadcast(UserRemote.effect(chr.getId(), effect), chr);
        }
    }

    @Handler(op = InHeader.USER_FOLLOW_CHARACTER_REQUEST)
    public static void handleUserFollowCharacterRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int driverChrId = inPacket.decodeInt();
        Char driverChr = field.getCharByID(driverChrId);
        if (driverChr == null) {
            return;
        }
        driverChr.write(WvsContext.setPassenserRequest(chr.getId()));
    }

    @Handler(op = InHeader.USER_FOLLOW_CHARACTER_WITHDRAW)
    public static void handleUserFollowCharacterWithdraw(Char chr, InPacket inPacket) {
        byte isWithdrawable = inPacket.decodeByte();
        if (isWithdrawable == 0) {
            chr.write(UserPacket.followCharacter(chr.getId(), 0, false, null));
        }
    }

    @Handler(op = InHeader.SET_PASSENGER_RESULT)
    public static void handleSetPassengerResult(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        int requestorChrId = inPacket.decodeInt();
        boolean accepted = inPacket.decodeByte() != 0;
        Char requestorChr = field.getCharByID(requestorChrId);

        if (requestorChr != null) {
            if (accepted) {
                requestorChr.getField().broadcast(UserPacket.followCharacter(requestorChr.getId(), chr.getId(), false, new Position()));
            } else {
                int errorType = inPacket.decodeInt();
                requestorChr.dispose();
            }
        }
    }

    @Handler(op = InHeader.USER_CATCH_DEBUFF_COLLISION)
    public static void handleUserCatchDebuffCollision(Char chr, InPacket inPacket) {
        int hpPerc = inPacket.decodeInt();
        chr.damage(chr.getMaxHP() * hpPerc / 100);
    }

    @Handler(op = InHeader.INVITE_CHAIR)
    public static void handleInviteChair(Char chr, InPacket inPacket) {
        int targetId = inPacket.decodeInt();
        Char target = chr.getField().getCharByID(targetId);
        if (target != null) {
            if (target.getChair() != null) {
                chr.write(UserLocal.inviteGroupChair(InviteGroupChairResult.PlayerAlreadySitting));
                return;
            }
            if (target.getChair().getGroupChairOID() == chr.getChair().getGroupChairOID()) {
                chr.write(UserLocal.inviteGroupChair(InviteGroupChairResult.AlreadySitOnChair));
                return;
            }
            chr.write(UserLocal.inviteGroupChair(InviteGroupChairResult.InviteSuccessfully));
            target.write(UserLocal.requireChair(chr.getId()));
        } else {
            chr.write(UserLocal.inviteGroupChair(InviteGroupChairResult.PlayerNotFound));
        }
    }

    @Handler(op = InHeader.GATHER_REQUEST)
    public static void handleGatherRequest(Char chr, InPacket inPacket) {
        int lifeId = inPacket.decodeInt();
        Reactor reactor = (Reactor) chr.getField().getLifeByObjectID(lifeId);
        if (reactor != null)
            reactor.setHitCount(0);
        chr.write(UserLocal.gatherRequestResult(lifeId, true));
    }

    @Handler(op = InHeader.GATHER_END_NOTICE)
    public static void handleGatherEndNotice(Char chr, InPacket inPacket) {
        boolean success = false;
        int lifeId = inPacket.decodeInt();

        Reactor reactor = (Reactor) chr.getField().getLifeByObjectID(lifeId);
        ReactorType type = GameConstants.getReactorType(reactor.getTemplateId());
        if (type == null) {
            return;
        } else if (type == ReactorType.VEIN && chr.hasSkill(SkillConstants.MINING_SKILL) || type == ReactorType.HERB && chr.hasSkill(SkillConstants.HERBALISM_SKILL)) {
            int reactorLevel = ReactorData.getReactorInfoByID(reactor.getTemplateId()).getLevel();
            int chrLevel = type == ReactorType.HERB ? chr.getMakingSkillLevel(SkillConstants.HERBALISM_SKILL) : chr.getMakingSkillLevel(SkillConstants.MINING_SKILL);
            int successChance = chrLevel >= reactorLevel ? 95 : 20;
            success = Util.succeedProp(90);
        }
        boolean isCancel = reactor.getHitCount() < reactor.getMaxHitCount();
        if (success && !isCancel) {
            EventConstants.dropItemFromReactor(chr, reactor);
        } else if (!success && !isCancel && EventConstants.FARM_EVENT) {
            chr.chatMessage("材料變成了怪物！請在 2 秒內消滅它。");
            Mob mob = MobData.getMobDeepCopyById(Util.getRandom(9303020, 9303023));
            mob.setPosition(reactor.getPosition());
            mob.getDrops().clear();
            mob.setDrops(Util.makeSet(
                    new DropInfo(4310095, 10000, 1, 3) //Silver Farm Coin
            ));
            mob.setHp(1);
            mob.setScale(200);
            mob.setRemoveAfter(2);
            chr.getField().spawnLife(mob, null);
        }
        if (!isCancel) {
            chr.getField().removeLife(reactor);
        } else {
            chr.write(UserPacket.gatherResult(chr.getId(), false));
            return;
        }
        chr.write(UserPacket.gatherResult(chr.getId(), success));
    }

    @Handler(op = InHeader.USER_AD_BOARD_CLOSE)
    public static void handleUserADBoardClose(Char chr, InPacket inPacket) {
        chr.setADBoardRemoteMsg(null);
        chr.getField().broadcast(UserPacket.setADBoard(chr, false));
    }

    @Handler(op = InHeader.REQUEST_EVENT_LIST)
    public static void handleRequestEventList(Char chr, InPacket inPacket) {
        chr.write(WvsContext.eventListResult(net.swordie.ms.world.event.EventListRepository.standard().current()));
    }

    @Handler(op = InHeader.TRY_REGISTER_TELEPORT)
    public static void handleTryRegisterTeleport(Char chr, InPacket inPacket) {
        int skillID = inPacket.decodeInt();
        if (chr.hasSkill(skillID)) {
            chr.write(UserLocal.registerTeleport(skillID));
            if (skillID == Phantom.SHROUD_WALK) {
                Option o = chr.getTemporaryStatManager().getOptions(CharacterTemporaryStat.Invisible).get(0);
                if (o != null) {
                    o.nOption--;
                    if (o.nOption == 0) {
                        chr.setSkillCooldown(skillID, chr.getSkillLevel(skillID));
                    }
                }
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_TOWER_CHAIR_SETTING)
    public static void handleUserTowerChairSetting(Char chr, InPacket inPacket) {
        List<Integer> towerChairSetting = new LinkedList<>();
        inPacket.decodeInt(); // hardcore 0
        for (int i = 0; i < 6; i++) {
            int chairID = inPacket.decodeInt();
            towerChairSetting.add(chairID);
        }
        chr.createQuestWithQRValue(QuestConstants.TOWER_CHAIR_SETTING_QUEST, String.format(QuestConstants.TOWER_CHAIR_SETTING_FORMAT, towerChairSetting.get(0), towerChairSetting.get(1), towerChairSetting.get(2), towerChairSetting.get(3), towerChairSetting.get(4), towerChairSetting.get(5)));
        chr.setChair(new PortableChair(chr, 0, ChairType.None));
        chr.getField().broadcast(UserRemote.remoteSetActivePortableChair(chr.getId(), chr.getChair()));
        chr.write(WvsContext.towerChairSettingResult());
        chr.dispose();
    }

    @Handler(op = InHeader.USER_BEAUTY_DATA_REQUEST)
    public static void handleUserBeautyDataRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt();
        if (inPacket.getUnreadAmount() > 0) {
            BeautySalon beautySalon = chr.getBeautySalon();
            if (beautySalon == null) {
                chr.dispose();
                return;
            }
            byte type = inPacket.decodeByte();
            switch (type) {
                case 7: { //Add
                    int index = inPacket.decodeInt();
                    if (index >= 40000) {
                        int skinID = chr.getAvatarData().getAvatarLook().getSkin();
                        if (beautySalon.getSkinString().contains(String.valueOf(skinID))) {
                            chr.chatPopup("這個膚色已經在收藏中，無法新增。");
                            chr.dispose();
                            return;
                        }
                        index -= 40000;
                        beautySalon.setSkinByIndex(index, skinID);
                    } else if (index >= 30000) {
                        int hairID = chr.getAvatarData().getAvatarLook().getHair();
                        if (beautySalon.getHairString().contains(String.valueOf(hairID))) {
                            chr.chatPopup("這個髮型已經在收藏中，無法新增。");
                            chr.dispose();
                            return;
                        }
                        index -= 30000;
                        beautySalon.setHairByIndex(index, hairID);
                    } else if (index >= 20000) {
                        int faceID = chr.getAvatarData().getAvatarLook().getFace();
                        if (beautySalon.getFaceString().contains(String.valueOf(faceID))) {
                            chr.chatPopup("這個臉型已經在收藏中，無法新增。");
                            chr.dispose();
                            return;
                        }
                        index -= 20000;
                        beautySalon.setFaceByIndex(index, faceID);
                    }
                    break;
                }
                case 1: { //Delete
                    int index = inPacket.decodeInt();
                    if (index >= 40000) {
                        index -= 40000;
                        beautySalon.setSkinByIndex(index, -1);
                    } else if (index >= 30000) {
                        index -= 30000;
                        beautySalon.setHairByIndex(index, -1);
                    } else if (index >= 20000) {
                        index -= 20000;
                        beautySalon.setFaceByIndex(index, -1);
                    }
                    break;
                }
                case 3: {
                    int index = inPacket.decodeInt();
                    if (index >= 40000) {
                        index -= 40000;
                        int skinID = beautySalon.getSkinByIndex(index);
                        if (skinID == -1) {
                            break;
                        }
                        chr.getAvatarData().getAvatarLook().setSkin(skinID);
                        chr.setStatAndSendPacket(Stat.skin, skinID);
                    } else if (index >= 30000) {
                        index -= 30000;
                        int hairID = beautySalon.getHairByIndex(index);
                        if (hairID == -1) {
                            break;
                        }
                        chr.getAvatarData().getAvatarLook().setHair(hairID);
                        chr.setStatAndSendPacket(Stat.hair, hairID);
                    } else if (index >= 20000) {
                        index -= 20000;
                        int faceID = beautySalon.getFaceByIndex(index);
                        if (faceID == -1) {
                            break;
                        }
                        chr.getAvatarData().getAvatarLook().setFace(faceID);
                        chr.setStatAndSendPacket(Stat.face, faceID);
                    }
                    chr.write(FieldPacket.closeUI(UIType.UI_BEAUTY_SALON));
                    break;
                }
            }
            beautySalon.saveToSQL();
        }
        chr.write(UserLocal.beautyDataResult(chr.getBeautySalon()));
        chr.dispose();
    }

    @Handler(op = InHeader.USER_SAVE_DAMAGE_SKIN_REQUEST)
    public static void handleUserSaveDamageSkinRequest(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        DamageSkinType error = null;
        final List<DamageSkinSaveData> skins = chr.getDamageSkins().stream().filter(d -> d.getDamageSkinID() != 0).toList();
        if (skins.size() >= GameConstants.DAMAGE_SKIN_MAX_SIZE) {
            error = DamageSkinType.Res_Fail_SlotCount;
        } else if (chr.getDamageSkinByItemID(itemID) != null) {
            error = DamageSkinType.Res_Fail_AlreadyExist;
        }
        if (error != null) {
            chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg, error, null));
        } else {
            DamageSkinSaveData dssd = DamageSkinSaveData.getByItemID(itemID);
            dssd.setCharId(chr.getId());
            dssd.setActivateTime(FileTime.currentTime());
            chr.addDamageSkin(dssd);
            chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg, DamageSkinType.Res_Success, chr));
            chr.write(FieldPacket.characterInfo(chr));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_APPLY_DAMAGE_SKIN_REQUEST)
    public static void handleUserApplyDamageSkinRequest(Char chr, InPacket inPacket) {
        int damageSkinID = inPacket.decodeInt();
        if (chr.getDamageSkinBySkinID(damageSkinID) == null) {
            chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg, DamageSkinType.Res_Fail_Unknown, chr));
            chr.dispose();
            return;
        }
        DamageSkinSaveData dssd = chr.getDamageSkinBySkinID(damageSkinID);
        DamageSkinSaveData curSkin = chr.getActiveDamageSkin();
        if (curSkin != null && dssd.getDamageSkinID() == curSkin.getDamageSkinID()) {
            chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg, DamageSkinType.Res_Fail_AlreadyActive, chr));
            chr.dispose();
            return;
        }
        if (chr.getMoney() < 100000) {
            chr.chatMessage("楓幣不足 100,000，無法使用這個傷害字型。");
            chr.dispose();
            return;
        }
        Quest q = chr.getOrCreateQuestById(QuestConstants.DAMAGE_SKIN);
        q.setStatus(QuestStatus.Started);
        q.setQrValue(String.valueOf(dssd.getDamageSkinID()));
        chr.setActiveDamageSkin(dssd);
        chr.write(UserPacket.setActiveDamageSkin(chr));
        chr.write(WvsContext.questRecordMessage(q));
        chr.write(FieldPacket.characterInfo(chr));
        chr.deductMoney(100000);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_DELETE_DAMAGE_SKIN_REQUEST)
    public static void handleUserDeleteDamageSkinRequest(Char chr, InPacket inPacket) {
        int damageSkinID = inPacket.decodeInt();
        if (chr.getDamageSkinBySkinID(damageSkinID) == null) {
            chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Remove, DamageSkinType.Res_Fail_Unknown, chr));
            chr.dispose();
            return;
        }
        DamageSkinSaveData dssd = chr.getDamageSkinBySkinID(damageSkinID);
        DamageSkinSaveData curSkin = chr.getActiveDamageSkin();
        if (curSkin != null && dssd.getDamageSkinID() == curSkin.getDamageSkinID()) {
            chr.chatMessage("無法刪除目前使用中的傷害字型。");
            chr.dispose();
            return;
        }
        chr.getDamageSkins().removeIf(x -> x.getDamageSkinID() == dssd.getDamageSkinID() && x.getCharId() == dssd.getCharId() && x.getItemID() == dssd.getItemID());
        chr.write(FieldPacket.characterInfo(chr));
        chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Remove, DamageSkinType.Res_Success, chr));
        chr.dispose();
    }

    @Handler(op = InHeader.USER_RENAME_RESULT)
    public static void userRenameResult(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) {
            c.close();
        }

        int charID = inPacket.decodeInt();
        boolean hasCoupon = inPacket.decodeByte() == 1;
        inPacket.decodeInt(); // crc
        short size0 = inPacket.decodeShort();
        String charName = inPacket.decodeString(size0);
        short size = inPacket.decodeShort();
        String newName = inPacket.decodeString(size);
        Account acc = c.getAccount();
        User user = c.getUser();
        if (acc == null || user == null) {
            c.write(UserLocal.userRenameResult(CharRenameType.Error, 0));
            return;
        }
        if (chr == null) {
            c.write(UserLocal.userRenameResult(CharRenameType.InvalidRequest, 0));
            return;
        }
        if (chr.getId() != charID) {
            c.write(UserLocal.userRenameResult(CharRenameType.InvalidRequest, 0));
            return;
        }
        if (chr.getName().equals(newName) || !GameConstants.isValidName(newName)) {
            c.write(UserLocal.userRenameResult(CharRenameType.InvalidName, 0));
            return;
        }
        if (Char.getCharDataByName(newName) != null) {
            c.write(UserLocal.userRenameResult(CharRenameType.UnavailableName, 0));
            return;
        }
        boolean canChange = false;
        if (hasCoupon) {
            chr.consumeItem(4034803, 1);
            canChange = true;
        } else {
            if (user.getMaplePoints() >= 15000) {
                user.deductMaplePoints(15000);
                canChange = true;
            } else {
                c.write(UserLocal.userRenameResult(CharRenameType.NotEnoughMaplePoints, 0));
                return;
            }
        }
        if (canChange) {
            chr.setName(newName);
            Party party = chr.getParty();
            if (party != null) {
                party.updatePartyMemberInfoByChr(chr);
            }
            if (chr.getGuild() != null) {
                GuildMember gm = chr.getGuild().getMemberByCharID(chr.getId());
                if (gm != null) {
                    gm.setName(newName);
                    gm.updateGuildMemberToSQL();
                }
            }
            Account account = chr.getAccount();
            if (account != null) {
                Union union = account.getUnion();
                if (union != null) {
                    for (UnionBoard unionBoard : union.getUnionBoards()) {
                        for (UnionMember unionMember : unionBoard.getActiveMembers()) {
                            if (unionMember.getCharId() == chr.getId()) {
                                unionMember.setCharName(newName);
                            }
                        }
                    }
                }
            }
            c.write(UserLocal.userRenameResult(CharRenameType.Success, 0));
            chr.saveToSQL();
            return;
        }
        c.write(UserLocal.userRenameResult(CharRenameType.PleaseTryAgainLater, 0));
    }

    @Handler(op = InHeader.USER_RENAME_REQUEST)
    public static void userRenameRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) return;
        if (chr.hasItem(4034803)) {
            c.write(UserLocal.userRenameResult(CharRenameType.EnterNewName, 4034803));
        } else {
            c.write(UserLocal.userRenameResult(CharRenameType.EnterNewName, 0));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_REVIVE_IN_BOSS_INSTANCE)
    public static void handleUserReviveInBossInstance(Char chr, InPacket inPacket) {
        chr.revive(chr.getField().getId());
    }

    @Handler(op = InHeader.DIMENSION_MIRROR_REQUEST)
    public static void dimensionMirrorRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // crc
        int id = inPacket.decodeInt();
        int toFieldID = DimensionMirrorType.getByID(id).getMapId();
        int portalID = DimensionMirrorType.getByID(id).getPortal();
        if (toFieldID != 0 && chr.getField().getId() != toFieldID && chr.getInstance() == null) {
            chr.getScriptManager().setReturnField(chr.getFieldID());
            chr.warp(toFieldID, portalID);
        } else {
            chr.chatMessage("無法移動到這張地圖。");
        }
        chr.dispose();
    }

    @Handler(op = InHeader.LEVEL_UP_GUIDE_REQUEST)
    public static void handleLevelUpGuideRequest(Char chr, InPacket inPacket) {
        chr.write(UserLocal.openUrl("https://www.nexon.com/maplestory/game/maple-guides/all"));
        chr.dispose();
    }

    @Handler(op = InHeader.COMMERCI_DEPART)
    public static void handleCommerciDepart(Char chr, InPacket inPacket) {
        inPacket.decodeShort(); // always 1?
        CommerceRecord cr = chr.getCommerceRecord();
        int regionType = inPacket.decodeByte();
        int itemSize = inPacket.decodeInt();
        for (int i = 0; i < itemSize; i++) {
            int itemID = inPacket.decodeInt();
            int usedCount = inPacket.decodeInt();
            cr.getCommerceItemRecords().add(new CommerceItemRecord(itemID, usedCount, FileTime.currentTime()));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.CHECK_PROCESS)
    public static void handleCheckProcess(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) return;

        if (chr.getClient().getCurrentState() >= Client.IN_FIELD) {
            //c.write(WvsContext.checkProcessResult(false));
        } else {
            //c.write(WvsContext.checkProcessResult(true));
        }
    }

    @Handler(op = InHeader.MEMO_IN_GAME_REQUEST)
    public static void handleMemoInGameRequest(Char chr, InPacket inPacket) {
        int fileSize = inPacket.decodeShort();
        String result = "";
        for (int i = 0; i < fileSize; i++) {
            try {
                String path = inPacket.decodeString();
                String idk1 = inPacket.decodeString();
                String idk2 = inPacket.decodeString();
                String idk3 = inPacket.decodeString();
                String idk4 = inPacket.decodeString();
                String check = path + " " + idk1 + " " + idk2 + " " + idk3 + " " + idk4;
                result += " - " + check;
            } catch (Exception ignored) {}
        }
        if (result.contains("Cheat Engine")
                || result.contains("cheatengine")
                || result.contains("cheat engine")
                || result.contains("Nopde Engine")
                || result.contains("Nopde")
                || result.contains("UCEngine")
                || result.contains("plumbwicked")
                || result.contains("Lunar Engine")
                || result.contains("CE+ﾵ\uFFF7ￊￔV5.1(ￃￜￂ￫666)")
                || result.contains("CE_do_anh_bat_duoc_em")
                || result.contains("psyreengine")
                //|| check.contains("Speed Gear")
                //|| check.contains("SpeedGear")
                //|| check.contains("C:\\Users\\quang\\AppData")
                //|| check.contains("C:\\Users\\EI\\AppData")
                || result.contains("Technitium")
                || result.contains("Trainer")) {
            User user = chr.getUser();
            String name = user.getName();
            try {
                LocalDateTime banDate = LocalDateTime.now().plusDays(365);
                user.setBanExpireDate(FileTime.fromDate(banDate));
                user.setBanReason("Memory Edits");
                user.saveToSQL(true);
                BannedMachines.addBannedMachine(user.getMachineID());
                DiscordAPI.send("banned", String.format("Account ||%s|| have been banned with reason: %s for %d day(s).", name, "Memory Edits", 365), DiscordAPI.mainGuildServer);
            } catch (Exception e) {
                DiscordAPI.send("lệnh", "Không thể khoá tài khoản " + chr.getName(), DiscordAPI.staffGuildServer);
            } finally {
                chr.setUser(user);
                chr.getClient().setUser(user);
                chr.getClient().close();
                DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("[%s] Cheat Engine Detect. Char: %s. Url: %s", chr.getClient().getIP(), chr.getName(), result));
            }
        } else if (result.contains("MacroRecorder")
                || result.contains("Macro Recorder")
                || result.contains("Auto Click")
                || result.contains("AutoClick")) {
            User user = chr.getUser();
            String name = user.getName();
            try {
                LocalDateTime banDate = LocalDateTime.now().plusDays(1);
                user.setBanExpireDate(FileTime.fromDate(banDate));
                user.setBanReason("Automating repetitive tasks");
                user.saveToSQL(true);
                DiscordAPI.send("banned", String.format("Account ||%s|| have been banned with reason: %s for %d day(s).", name, "Automating repetitive tasks", 1), DiscordAPI.mainGuildServer);
            } catch (Exception e) {
                DiscordAPI.send("lệnh", "Không thể khoá tài khoản " + chr.getName(), DiscordAPI.staffGuildServer);
            } finally {
                chr.setUser(user);
                chr.getClient().setUser(user);
                chr.getClient().close();
                DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("[%s] Cheat Engine Detect. Char: %s. Url: %s",
                        chr.getClient().getIP(), chr.getName(), result));
            }
        }
        // Tổng hợp tên files:
        Pattern pattern = Pattern.compile("([^\\s\\\\]+\\.exe)");
        Matcher matcher = pattern.matcher(result);
        List<String> exeNames = new ArrayList<>();
        while (matcher.find()) {
            String fullPath = matcher.group(1);
            int lastSlashIndex = fullPath.lastIndexOf('\\');
            exeNames.add(fullPath.substring(lastSlashIndex + 1));
        }
        String finalResult = String.join("; ", exeNames);

        DataPrinter.send("AntiCheat/" + chr.getName() + ".txt", String.format("[%s] Kết quả: %s",
                chr.getClient().getIP(), finalResult), true);
    }

    @Handler(op = InHeader.USER_CLAIM_REQUEST)
    public static void handleUserClaimRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeByte();
        short nameSize = inPacket.decodeShort();
        String name = inPacket.decodeString(nameSize);
        short descSize = inPacket.decodeShort();
        String desc = inPacket.decodeString(descSize);
        Char target = chr.getField().getCharByName(name);
        if (target != null) {
            if (System.currentTimeMillis() - chr.getLastReportTime() >= 60000L) {
                if (chr.getMoney() < 1000) {
                    chr.write(WvsContext.claimResult(ClaimResultType.NotEnoughMesosToReport));
                    chr.dispose();
                    return;
                }
                if (chr.getLastCharReportName().contains(name)) {
                    chr.write(WvsContext.claimResult(ClaimResultType.AlreadyReportThisUser));
                    chr.dispose();
                    return;
                }
                if (chr.getReportCount() == 0) {
                    chr.write(WvsContext.claimResult(ClaimResultType.ExceededNumbersOfReports));
                    chr.dispose();
                    return;
                }
                chr.setReportCount(chr.getReportCount() - 1);
                chr.deductMoney(1000);
                chr.write(WvsContext.claimResult(ClaimResultType.Success, true, chr.getReportCount()));
                DataPrinter.send(DataPrinter.ALL_IN_ONE, chr.getName() + " 舉報玩家 " + target.getName() + "，地圖 " + target.getFieldID() + " | 類型: " + type + " / 描述: " + desc);
            } else {
                chr.write(WvsContext.claimResult(ClaimResultType.PleaseTryAgain));
            }
        } else {
            chr.write(WvsContext.claimResult(ClaimResultType.WrongCharacterName));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.REQUEST_RECEIVE_STUFFS)
    public static void handleRequestReceiveStuffs(Char chr, InPacket inPacket) {
        int nType = 0;
        int unk = inPacket.decodeInt();
        Long startTime = 0L;
        Long endTime = 0L;
        if ((nType & 1) != 0) {
            startTime = inPacket.decodeLong();
            endTime = inPacket.decodeLong();
            inPacket.decodeLong();
            inPacket.decodeLong();
        }
        if ((nType & 2) != 0) {
            inPacket.decodeInt();
            inPacket.decodeInt();
            inPacket.decodeInt();
            inPacket.decodeInt();
            inPacket.decodeInt();
            inPacket.decodeInt();
            inPacket.decodeString();
            inPacket.decodeString();
            inPacket.decodeString();
        }
        int rewardItemType = inPacket.decodeInt();
        int itemID = inPacket.decodeInt();
        int quantity = inPacket.decodeInt();
        inPacket.decodeInt();
        inPacket.decodeLong();
        inPacket.decodeInt();
        int maplePoint = inPacket.decodeInt();
        long meso = inPacket.decodeLong();
        long exp = inPacket.decodeLong();
        inPacket.decodeInt();
        inPacket.decodeInt();
        inPacket.decodeString();
        inPacket.decodeString();
        inPacket.decodeString();
        if ((nType & 4) != 0) {
            inPacket.decodeString();
        }
        String desc = "";
        if ((nType & 8) != 0) {
            desc = inPacket.decodeString();
        }
        inPacket.decodeInt();
        inPacket.decodeInt();
        if (rewardItemType == RewardItemType.Equip.getVal()) {
            byte invType = inPacket.decodeByte();
            // item::decode
        }

        boolean isAccept = inPacket.decodeByte() == 1;

        RewardItemType type = RewardItemType.getByVal((byte) rewardItemType);
        RewardSystem reward = chr.getRewardSystem();
        if (reward == null) {
            chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
            return;
        }
        switch (type) {
            case Item: {
                RewardInfo rewardInfo = reward.getRewardByValue(type, itemID);
                if (rewardInfo == null) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                if (rewardInfo.getEndTime() != null && rewardInfo.getEndTime().isExpired()) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                Item item = ItemData.getItemDeepCopy(itemID);
                if (item == null) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                quantity = rewardInfo.getQuantity();
                if (isAccept) {
                    Inventory inv = chr.getInventoryByType(item.getInvType());
                    if (inv.isFull() || (inv.getEmptySlots() < quantity && ItemConstants.isEquip(itemID))) {
                        chr.write(WvsContext.userReceiveStuffs(item.isCash() ? RewardResult.response_Received_CashItem_Fail((byte) 32) : RewardResult.response_Received_GameItem_Fail((byte) 102)));
                        return;
                    } else {
                        if (ItemConstants.isSymbol(itemID)) {
                            chr.getScriptManager().giveSymbol(itemID, quantity);
                        } else {
                            chr.addItemToInventory(itemID, quantity);
                        }
                    }
                }
                reward.removeReward(rewardInfo);
                chr.sendPacketRewards();
                break;
            }
            case MaplePoint: {
                RewardInfo rewardInfo = reward.getRewardByValue(type, maplePoint);
                if (rewardInfo == null || chr.getUser() == null) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                if (rewardInfo.getEndTime() != null && rewardInfo.getEndTime().isExpired()) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                if (chr.getUser().getMaplePoints() >= Integer.MAX_VALUE && isAccept) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_Received_MaplePoint_Fail)));
                    chr.sendPacketRewards();
                    return;
                }
                if (isAccept) {
                    chr.addMaplePoint(rewardInfo.getMaplePoint());
                }
                reward.removeReward(rewardInfo);
                chr.sendPacketRewards();
                break;
            }
            case Meso: {
                RewardInfo rewardInfo = reward.getRewardByValue(type, meso);
                if (rewardInfo == null) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                if (rewardInfo.getEndTime() != null && rewardInfo.getEndTime().isExpired()) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                if (chr.getMoney() >= GameConstants.MAX_MONEY && isAccept) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_Received_Mesos_Fail)));
                    chr.sendPacketRewards();
                    return;
                }
                if (isAccept) {
                    chr.addMoney(rewardInfo.getMeso());
                }
                reward.removeReward(rewardInfo);
                chr.sendPacketRewards();
                break;
            }
            case Exp: {
                RewardInfo rewardInfo = reward.getRewardByValue(type, exp);
                if (rewardInfo == null) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                if (rewardInfo.getEndTime() != null && rewardInfo.getEndTime().isExpired()) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_UnexpectedError)));
                    chr.sendPacketRewards();
                    return;
                }
                if (chr.getLevel() >= GameConstants.MAX_LEVEL && isAccept) {
                    chr.write(WvsContext.userReceiveStuffs(new RewardResult(RewardSystemType.Response_Received_Exp_Fail)));
                    chr.sendPacketRewards();
                    return;
                }
                if (isAccept) {
                    chr.addExp(rewardInfo.getExp(), false);
                }
                reward.removeReward(rewardInfo);
                chr.sendPacketRewards();
                break;
            }
        }
        if (isAccept) {
            if (reward.getRewards().isEmpty()) {
                chr.write(WvsContext.userReceiveStuffs(RewardResult.response_Received_GameItem_Success(1)));
                chr.write(FieldPacket.closeUI(UIType.UI_REWARD));
            } else {
                chr.sendPacketRewards();
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_SHOOT_ATTACK_IN_FPS)
    public static void handleUserShootAttackInFPS(Char chr, InPacket inPacket) {
        inPacket.decodeInt();
        inPacket.decodeInt();
        FrittoEagle frittoEagle = chr.getFrittoEagle();
        if (frittoEagle != null) {
            int size = inPacket.decodeInt();
            frittoEagle.shootResult(chr);
            for (int i = 0; i < size; i++) {
                int objectID = inPacket.decodeInt();
                inPacket.decodeArr(14);
                Mob mob = (Mob) chr.getField().getLifeByObjectID(objectID);
                frittoEagle.addScore(mob, chr);
                mob.removeWithAnimation();
            }
        }
    }

    @Handler(op = InHeader.POLO_FRITO_COURTSHIP_DANCE_RESULT)
    public static void handlePoloFritoCourtShipDanceResult(Char chr, InPacket inPacket) {

        boolean success = inPacket.decodeByte() == 1;
        FrittoDancing fd = chr.getFrittoDancing();
        ScriptManagerImpl sm = chr.getScriptManager();
        if (fd != null && success) {
            int score = Integer.parseInt(chr.getQRValueByKey(15143, "score"));
            chr.setQRValueByKey(15143, "score", "" + (score + 1));
            if (Integer.parseInt(chr.getQRValueByKey(15143, "score")) >= 10) {
                fd.finish(chr);
            }
        }
    }

    @Handler(op = InHeader.POLO_FRITO_TOWN_DEFENSE_REQUEST)
    public static void handlePoloFritoTownDefenseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt();
        DefenseTowerWave defenseTowerWave = chr.getDefenseTowerWave();
        if (defenseTowerWave != null) {
            defenseTowerWave.checkFinish(chr);
        }
    }

    @Handler(op = InHeader.POLO_FRITO_BOUNTY_HUNTING_REQUEST)
    public static void handlePoloFritoBountyHuntingRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt();
        BountyHunting bountyHunting = chr.getBountyHunting();
        if (bountyHunting != null) {
            bountyHunting.checkFinish(chr);
        }
    }

    @Handler(op = InHeader.DEFENSE_GAME_REQUEST)
    public static void handleDefenseGameRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();
        DefenseEvent defenseEvent = chr.getDefenseEvent();
        if (defenseEvent != null) {
            if (type == 1) {
                int sid = inPacket.decodeInt();
                defenseEvent.useSkill(chr, sid);
            } else if (type == 3) {
                defenseEvent.check();
            }
        }
    }

    @Handler(op = InHeader.ACHIEVEMENT_REQUEST)
    public static void handleAchievementRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();

        Account acc = chr.getAccount();
        if (type == 1) {
            // help
        } else if (type == 3) {
            int rankSet = inPacket.decodeInt();
            AchievementRank currentRank = acc.getAchievementRanks().stream().filter(ar -> ar.getRank() == rankSet).findFirst().orElse(null);
            if (currentRank != null && currentRank.getStatus() == rankSet) {
                chr.dispose();
            } else {
                for (AchievementRank ar : acc.getAchievementRanks()) {
                    if (ar.getRank() == rankSet) {
                        ar.setStatus((byte) 2);
                    } else {
                        ar.setStatus((byte) 1);
                    }
                }
                //chr.write(WvsContext.characterModified(chr));
                chr.dispose();
            }
        } else if (type == 7) {
            // refresh
            chr.write(WvsContext.updateAchievements(chr.getAccount().getAchievementDatas()));
            chr.dispose();
        }
    }

    @Handler(op = InHeader.SPECIAL_GAME_EXIT)
    public static void handleSpecialGameExit(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) return;

        ScriptManagerImpl sm = chr.getScriptManager();
        switch (chr.getField().getId()) {
            case SpiritSavior.SPIRIT_SAVIOR_MAP:
                chr.getSpiritSavior().exit();
                break;
            case DreamBreaker.gameMap:
                chr.getDreamBreaker().exit();
                break;
            case 921172000:
                chr.getScriptManager().warpInstanceOut(chr, 921172200);
                break;
            case 921172100:
                chr.getScriptManager().warpInstanceOut(chr, 921172201);
                break;
        }
        chr.dispose();
    }

    @Handler(op = InHeader.DREAM_BREAKER_SKILL)
    public static void handleDreamBreakerSkill(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) return;

        inPacket.decodeInt();
        int skillID = inPacket.decodeInt();
        ScriptManagerImpl sm = chr.getScriptManager();
        int dream = Integer.parseInt(chr.getQRValueByKey(15901, "dream"));
        Instance instance = chr.getInstance();
        if (instance == null || chr.getDreamBreaker() == null) {
            return;
        }
        switch (skillID) {
            case 0:
                if (dream >= 200) {
                    chr.setQRValueByKey(15901, "dream", String.valueOf(dream - 200));
                    sm.chatScript("量表停止！量表 5 秒內不會移動！");
                    instance.setProperty("gaugeHold", "true");
                    sm.addEvent(chr.getTimer().addEvent(() -> instance.setProperty("gaugeHold", "false"), 5000L));
                    chr.write(DreamBreakerPacket.lockSkill(skillID));
                    break;
                }
                sm.chatScript("夢幻點數不足，無法使用技能。");
                break;
            case 1:
                if (dream >= 300) {
                    chr.setQRValueByKey(15901, "dream", String.valueOf(dream - 300));
                    List<Mob> musicBox = new ArrayList<>();
                    for (Mob m : chr.getField().getMobs()) {
                        if (m.getTemplateId() >= 9833080 && m.getTemplateId() <= 9833084)
                            musicBox.add(m);
                    }
                    if (musicBox.size() > 0) {
                        Util.getRandomFromCollection(musicBox).removeWithAnimation();
                        sm.chatScript("覺醒之鐘的聲音喚醒了音樂盒！");
                    }
                    chr.write(DreamBreakerPacket.lockSkill(skillID));
                    break;
                }
                sm.chatScript("夢幻點數不足，無法使用技能。");
                break;
            case 2:
                if (dream >= 400) {
                    int stage = Integer.parseInt(chr.getQRValueByKey(15901, "stage"));
                    chr.setQRValueByKey(15901, "dream", String.valueOf(dream - 400));
                    sm.chatScript("召喚了軟綿綿先生來挑釁怪物！");
                    sm.spawnMob(9833100, chr.getPosition().getX(), chr.getPosition().getY(), false, GameConstants.getDreamBreakerHP(stage));
                    sm.addEvent(chr.getTimer().addEvent(() -> {
                        Life life = chr.getField().getLifeByTemplateId(9833100);
                        if (life != null) {
                            ((Mob) life).removeWithAnimation();
                            sm.progressMessageFont("軟綿綿先生消失了！");
                        }
                    }, 15000L));
                    chr.write(DreamBreakerPacket.lockSkill(skillID));
                    break;
                }
                sm.chatScript("夢幻點數不足，無法使用技能。");
                break;
            case 3:
                if (dream >= 900) {
                    chr.setQRValueByKey(15901, "dream", String.valueOf(dream - 900));
                    sm.chatScript("攻擊沉睡音樂盒的怪物全部消失了！");
                    for (Mob m : chr.getField().getMobs()) {
                        switch (m.getTemplateId()) {
                            case 9833070, 9833071, 9833072, 9833073, 9833074, 9833080, 9833081, 9833082, 9833083, 9833084, 9833100 ->
                                    m.removeWithAnimation();
                        }
                    }
                    instance.setProperty("stopSpawn", "true");
                    sm.addEvent(chr.getTimer().addEvent(() -> instance.setProperty("stopSpawn", "false"), 10000L));
                    chr.write(DreamBreakerPacket.lockSkill(skillID));
                    break;
                }
                sm.chatScript("夢幻點數不足，無法使用技能。");
                break;
        }
        chr.write(DreamBreakerPacket.result());
        chr.dispose();
    }

    @Handler(op = InHeader.LUCID_ACTIVATE_STATUE_REQUEST)
    public static void handleLucidActivateStatueRequest(Char chr, InPacket inPacket) {
        if (chr == null) return;

        chr.getField().setLucidStatueGauge(0);
        chr.getField().broadcast(LucidPacket.butterflyAction(ButterFlyType.Erase, 0, null, 0, 0));
        chr.getField().broadcast(LucidPacket.statueStateChange(false, Math.min(chr.getField().getLucidStatueGauge(), 3), true));
    }

    @Handler(op = InHeader.DAILY_GIFT_REQUEST)
    public static void handleUserReceiveDailyGiftRequest(Char chr, InPacket inPacket) {
        switch (DailyGift.claim(chr, true)) {
            case ALREADY_CLAIMED -> chr.chatPopup("你已經領取過該禮物。");
            case INVENTORY_FULL -> chr.chatPopup("背包空間不足，無法領取獎勵。");
            case MONTH_COMPLETE -> chr.chatPopup("本月的每日禮物已全部領取。");
            case NO_ACCOUNT, CLAIMED -> { }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_EVENT_GAUGE_GAIN_COIN_REQUEST)
    public static void handleUserEventGaugeGainCoinRequest(Char chr, InPacket inPacket) {
        if (chr == null) {
            return;
        }
        DailyCoin dailyCoin = chr.getDailyCoin();
        if (dailyCoin == null) {
            chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("發生錯誤，請稍後再試。")));
            return;
        }
        if (dailyCoin.getCoin() == 0) {
            chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("你沒有任何硬幣。")));
            return;
        }
        if (chr.getInventoryByType(InvType.getInvTypeByVal(InvType.ETC.getVal())).getEmptySlots() < 1) {
            chr.write(WvsContext.broadcastMsg(BroadcastMsg.popUpMessage("請在其他欄中騰出至少一個空格。")));
            return;
        }
        dailyCoin.gainCoin(chr);
    }

    @Handler(op = InHeader.QUICK_PASS_REQUEST)
    public static void handleQuickPassRequest(Char chr, InPacket inPacket) {
        // FF FF FF FF FF FF FF FF 00
    }

    @Handler(op = InHeader.USER_SKILL_SEQUENCE_SKILLS_REQUEST)
    public static void handleUserSkillSequenceSkillsRequest(Char chr, InPacket inPacket) {
        int n = Byte.toUnsignedInt(inPacket.decodeByte());
        if (n > SequenceSkill.MAX_ROW) {
            chr.dispose(); return;
        }
        List<SequenceSkill> list = chr.getSequenceSkills();
        if (list == null) {
            list = new ArrayList<>();
            chr.setSequenceSkills(list);
        }
        List<SequenceSkill> old = new ArrayList<>(list);
        for (int i = 0; i < n; i++) {
            SequenceSkill ss = new SequenceSkill();
            ss.setCharId(chr.getId());
            ss.setIndex(i);
            ss.setName(inPacket.decodeString());
            for (int j = 0; j < 15; j++) {
                int skillID = inPacket.decodeInt();
                SkillInfo si = SkillData.getSkillInfoById(skillID);
                if (!chr.hasSkill(skillID) || si == null || !si.isSequenceOn()) {
                    ss.getSkillIds().add(0);
                    continue;
                }
                ss.getSkillIds().add(skillID);
            }
            boolean replaced = false;
            for (int k = 0; k < list.size(); k++) {
                if (list.get(k).getIndex() == i) {
                    ss.setId(list.get(k).getId());
                    list.set(k, ss);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) list.add(ss);
        }
        for (SequenceSkill s : old) {
            if (s.getIndex() >= n && s.getId() != 0) s.deleteFromSQL();
        }
        list.sort(Comparator.comparingInt(SequenceSkill::getIndex));
        list.removeIf(ss -> ss.getIndex() >= n);
        for (SequenceSkill ss : list) {
            if (ss.getIndex() < n) {
                ss.saveToSQL();
            }
        }
        chr.createQuestWithQRValue(QuestConstants.SKILL_SEQUENCE_SKILLS, String.format(QuestConstants.SKILL_SEQUENCE_SKILLS_FORMAT, inPacket.decodeByte(), inPacket.decodeByte(), inPacket.decodeByte()));
        //chr.write(WvsContext.userSkillSquence(chr, list));
        chr.chatPopup("已保存。");
    }

    @Handler(op = InHeader.USER_SKILL_SEQUENCE_BUFFS_REQUEST)
    public static void handleUserSkillSequenceBuffsRequest(Char chr, InPacket inPacket) {
        int unk0 = inPacket.decodeByte();
        int unk1 = inPacket.decodeByte();
        int n = inPacket.decodeInt();
        if (unk0 > SequenceBuff.MAX_ROW) {
            chr.dispose(); return;
        }
        List<SequenceBuff> list = chr.getSequenceBuffs();
        if (list == null) {
            list = new ArrayList<>();
            chr.setSequenceBuffs(list);
        }
        final int[] skillList = SequenceBuff.skillList;
        final int[] itemList = SequenceBuff.itemList;
        List<SequenceBuff> old = new ArrayList<>(list);
        for (int i = 0; i < n; i++) {
            SequenceBuff ss = new SequenceBuff();
            ss.setCharId(chr.getId());
            ss.setIndex(i);
            ss.setName(inPacket.decodeString(9));
            for (int j = 0; j < 30; j++) {
                int buffID = inPacket.decodeInt();
                if (buffID == 0) {
                    ss.getBuffs().add(0);
                    continue;
                }
                SkillInfo si = SkillData.getSkillInfoById(buffID);
                ItemInfo ii = ItemData.getItemInfoByID(buffID);
                boolean isSkill = si != null && chr.hasSkill(buffID);
                boolean isItem  = ii != null;
                if (!isSkill && !isItem) {
                    ss.getBuffs().add(0);
                    continue;
                }
                boolean ok = false;
                for (int x : skillList) {
                    if (x == buffID) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    for (int x : itemList) {
                        if (x == buffID) {
                            ok = true;
                            break;
                        }
                    }
                }
                ss.getBuffs().add(ok ? buffID : 0);
            }
            boolean replaced = false;
            for (int k = 0; k < list.size(); k++) {
                if (list.get(k).getIndex() == i) {
                    ss.setId(list.get(k).getId());
                    list.set(k, ss);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) list.add(ss);
        }
        for (SequenceBuff s : old) {
            if (s.getIndex() >= n && s.getId() != 0) s.deleteFromSQL();
        }
        list.sort(Comparator.comparingInt(SequenceBuff::getIndex));
        list.removeIf(ss -> ss.getIndex() >= n);
        for (SequenceBuff ss : list) {
            if (ss.getIndex() < n) {
                ss.saveToSQL();
            }
        }
        chr.createQuestWithQRValue(QuestConstants.SKILL_SEQUENCE_BUFFS, String.format(QuestConstants.SKILL_SEQUENCE_BUFFS_FORMAT, unk0, unk1));
        chr.chatPopup("已保存。");
    }

    @Handler(op = InHeader.USER_SKILL_ALARM_REQUEST)
    public static void handleUserSkillAlarmRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();
        final SkillAlarmType skillAlarmType = SkillAlarmType.getFromType(type);
        SkillAlarmInfo info = null;
        SkillAlarmInfo swapInfo = null;
        switch (skillAlarmType) {
            case ADD:
            case UPDATE_STATE:
                info = new SkillAlarmInfo(inPacket.decodeInt(), inPacket.decodeInt(), inPacket.decodeByte() == 1, inPacket.decodeInt());
                break;
            case REMOVE:
                info = new SkillAlarmInfo(inPacket.decodeInt());
                break;
            case CHANGE_POSITION:
                info = new SkillAlarmInfo(inPacket.decodeInt(), inPacket.decodeInt(), inPacket.decodeByte() == 1, inPacket.decodeInt());
                swapInfo = new SkillAlarmInfo(inPacket.decodeInt(), inPacket.decodeInt(), inPacket.decodeByte() == 1, inPacket.decodeInt());
                break;
        }
        if (info != null) {
            info.apply(chr);
            if (swapInfo != null) {
                swapInfo.apply(chr);
            }
            chr.write(SkillAlarmInfo.encode(skillAlarmType, info, swapInfo));
        }
    }

    @Handler(op = InHeader.HEXA_MATRIX_OPREATION)
    public static void handleHexaMatrixOperationRequest(Char chr, InPacket inPacket) {
        ScriptManagerImpl sm = chr.getScriptManager();
        int type = inPacket.decodeInt();
        switch (type) {
            case 0: {
                int coreID = inPacket.decodeInt();
                int coreType = HexaCore.getSkillCoreType(coreID);
                int job = chr.getJob();
                HexaCore.HexaSkillCoreData coreData = HexaCore.getSkillCoreData(coreID);
                if (coreData == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.dispose();
                    return;
                }
                if (!HexaCore.hasSkillCoreByJob(coreType, coreID, job)) {
                    chr.chatPopup(String.format("你的職業無法解鎖該技能：%s (%d)。", coreData.getName(), coreID));
                    chr.dispose();
                    return;
                }
                int erdaCost;
                if (coreType == 1) {
                    erdaCost = HexaMatrixConstants.getSolErdaCostToActivate(HexaMatrixConstants.HexaMatrixSkill.SKILL_CORE);
                } else if (coreType == 2) {
                    erdaCost = HexaMatrixConstants.getSolErdaCostToActivate(HexaMatrixConstants.HexaMatrixSkill.MASTERY_CORE);
                } else if (coreType == 3) {
                    erdaCost = HexaMatrixConstants.getSolErdaCostToActivate(HexaMatrixConstants.HexaMatrixSkill.BOOST_CORE);
                } else {
                    if (coreType != 4) {
                        chr.chatPopup("未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    erdaCost = HexaMatrixConstants.getSolErdaCostToActivate(HexaMatrixConstants.HexaMatrixSkill.COMMON_CORE);
                }
                int erdaFragmentCost;
                if (coreType == 1) {
                    erdaFragmentCost = HexaMatrixConstants.getSolErdaFragmentCostToActivate(HexaMatrixConstants.HexaMatrixSkill.SKILL_CORE);
                } else if (coreType == 2) {
                    erdaFragmentCost = HexaMatrixConstants.getSolErdaFragmentCostToActivate(HexaMatrixConstants.HexaMatrixSkill.MASTERY_CORE);
                } else if (coreType == 3) {
                    erdaFragmentCost = HexaMatrixConstants.getSolErdaFragmentCostToActivate(HexaMatrixConstants.HexaMatrixSkill.BOOST_CORE);
                } else {
                    if (coreType != 4) {
                        chr.chatPopup("未知錯誤。");
                        chr.dispose();
                        return;
                    }
                    erdaFragmentCost = HexaMatrixConstants.getSolErdaFragmentCostToActivate(HexaMatrixConstants.HexaMatrixSkill.COMMON_CORE);
                }
                int currentSolErdas = chr.getSolErda();
                long currentSolErdaFragments = 0;
                for (int id : HexaMatrixConstants.solErdaFragments) {
                    currentSolErdaFragments += sm.getQuantityOfItem(id);
                }
                if (erdaCost > currentSolErdas) {
                    chr.chatPopup(String.format("靈魂艾爾達不足，無法解鎖該技能。（需要：%d/%d）", currentSolErdas, erdaCost));
                    chr.dispose();
                    return;
                }
                if (erdaFragmentCost > currentSolErdaFragments) {
                    chr.chatPopup(String.format("靈魂艾爾達碎片不足，無法解鎖該技能。（需要：%d/%d）", currentSolErdaFragments, erdaFragmentCost));
                    chr.dispose();
                    return;
                }
                int need = erdaFragmentCost;
                for (int id : HexaMatrixConstants.solErdaFragments) {
                    int take = Math.min(sm.getQuantityOfItem(id), need);
                    if (take > 0) {
                        chr.consumeItem(id, take);
                    }
                    if ((need -= take) == 0) {
                        break;
                    }
                }
                chr.addSolErda(-erdaCost);
                chr.setHexaSkill(coreID, 1);
                chr.write(WvsContext.hexaSkillsUpdate(chr));
                chr.write(WvsContext.hexaMessage(type, 0, coreID, 0));
                if (SixthJobSkillConstants.isOriginCore(coreID)) {
                    SixthJobSkillConstants.sendAscentSkillStackUpdate(chr);
                }
                break;
            }
            case 1: {
                int coreID = inPacket.decodeInt();
                int currentLevel = inPacket.decodeInt();
                int nextLevel = inPacket.decodeInt();
                int solErdaReq = inPacket.decodeInt();
                int solErdaFragmentReq = inPacket.decodeInt();
                int coreLevel = chr.getHexaSkillLevel(coreID);
                if (coreLevel != currentLevel || coreLevel >= nextLevel) {
                    chr.chatPopup("你的角色數據不正確。");
                    chr.dispose();
                    return;
                }
                int erdaCost = 0;
                int erdaFragmentCost = 0;
                int coreType = HexaCore.getSkillCoreType(coreID);
                for (int i = coreLevel; i < nextLevel; i++) {
                    if (coreType == 1) {
                        erdaCost += HexaMatrixConstants.getSolErdaCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.SKILL_CORE, i);
                    } else if (coreType == 2) {
                        erdaCost += HexaMatrixConstants.getSolErdaCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.MASTERY_CORE, i);
                    } else if (coreType == 3) {
                        erdaCost += HexaMatrixConstants.getSolErdaCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.BOOST_CORE, i);
                    } else {
                        if (coreType != 4) {
                            chr.chatPopup("未知錯誤。");
                            chr.dispose();
                            return;
                        }
                        erdaCost += HexaMatrixConstants.getSolErdaCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.COMMON_CORE, i);
                    }
                    if (coreType == 1) {
                        erdaFragmentCost += HexaMatrixConstants.getSolErdaFragmentCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.SKILL_CORE, i);
                    } else if (coreType == 2) {
                        erdaFragmentCost += HexaMatrixConstants.getSolErdaFragmentCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.MASTERY_CORE, i);
                    } else if (coreType == 3) {
                        erdaFragmentCost += HexaMatrixConstants.getSolErdaFragmentCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.BOOST_CORE, i);
                    } else {
                        if (coreType != 4) {
                            chr.chatPopup("未知錯誤。");
                            chr.dispose();
                            return;
                        }
                        erdaFragmentCost += HexaMatrixConstants.getSolErdaFragmentCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.COMMON_CORE, i);
                    }
                }
                if (erdaCost != solErdaReq || erdaFragmentCost != solErdaFragmentReq) {
                    chr.chatPopup("你的角色數據不正確。");
                    chr.dispose();
                    return;
                }
                int currentSolErdas = chr.getSolErda();
                long currentSolErdaFragments = 0;
                for (int id : HexaMatrixConstants.solErdaFragments) {
                    currentSolErdaFragments += sm.getQuantityOfItem(id);
                }
                if (erdaCost > currentSolErdas) {
                    chr.chatPopup(String.format("靈魂艾爾達不足，無法升級該技能。（需要：%d/%d）", currentSolErdas, erdaCost));
                    chr.dispose();
                    return;
                }
                if (erdaFragmentCost > currentSolErdaFragments) {
                    chr.chatPopup(String.format("靈魂艾爾達碎片不足，無法升級該技能。（需要：%d/%d）", currentSolErdaFragments, erdaFragmentCost));
                    chr.dispose();
                    return;
                }
                int need = erdaFragmentCost;
                for (int id : HexaMatrixConstants.solErdaFragments) {
                    int take = Math.min(sm.getQuantityOfItem(id), need);
                    if (take > 0) {
                        chr.consumeItem(id, take);
                    }
                    if ((need -= take) == 0) {
                        break;
                    }
                }
                chr.addSolErda(-erdaCost);
                chr.setHexaSkill(coreID, nextLevel);
                chr.write(WvsContext.hexaSkillsUpdate(chr));
                chr.write(WvsContext.hexaMessage(type, 0, coreID, nextLevel));
                if (SixthJobSkillConstants.isOriginCore(coreID)) {
                    SixthJobSkillConstants.sendAscentSkillStackUpdate(chr);
                }
                break;
            }
            case 2: {
                int coreID = inPacket.decodeInt();
                int stat0 = inPacket.decodeInt();
                int stat1 = inPacket.decodeInt();
                int stat2 = inPacket.decodeInt();
                HexaCore.HexaStatCoreData coreData = HexaCore.getStatCoreData(coreID);
                if (coreData == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.dispose();
                    return;
                }
                if (chr.getLevel() < coreData.getReqLevel() || (coreData.getReqCore() != 0 && chr.getHexaStatByCoreID(coreData.getReqCore()) == null)) {
                    chr.chatPopup("你不符合解鎖該 HEXA 屬性的條件。");
                    chr.dispose();
                    return;
                }
                int erdaCost = HexaMatrixConstants.getSolErdaCostToActivate(HexaMatrixConstants.HexaMatrixSkill.HEXA_STAT);
                int erdaFragmentCost = HexaMatrixConstants.getSolErdaFragmentCostToActivate(HexaMatrixConstants.HexaMatrixSkill.HEXA_STAT);
                int currentSolErdas = chr.getSolErda();
                long currentSolErdaFragments = 0;
                for (int id : HexaMatrixConstants.solErdaFragments) {
                    currentSolErdaFragments += sm.getQuantityOfItem(id);
                }
                if (erdaCost > currentSolErdas) {
                    chr.chatPopup(String.format("靈魂艾爾達不足，無法解鎖該 HEXA 屬性。（需要：%d/%d）", currentSolErdas, erdaCost));
                    chr.dispose();
                    return;
                }
                if (erdaFragmentCost > currentSolErdaFragments) {
                    chr.chatPopup(String.format("靈魂艾爾達碎片不足，無法解鎖該 HEXA 屬性。（需要：%d/%d）", currentSolErdaFragments, erdaFragmentCost));
                    chr.dispose();
                    return;
                }
                int need = erdaFragmentCost;
                for (int id : HexaMatrixConstants.solErdaFragments) {
                    int take = Math.min(sm.getQuantityOfItem(id), need);
                    if (take > 0) {
                        chr.consumeItem(id, take);
                    }
                    if ((need -= take) == 0) {
                        break;
                    }
                }
                chr.addSolErda(-erdaCost);
                int stat = HexaMatrixConstants.getHexaStatByCoreID(coreID);
                HexaStat hexaStat = chr.getHexaStats().get(stat);
                if (hexaStat == null) {
                    hexaStat = new HexaStat(coreID, stat);
                }
                hexaStat.setCharId(chr.getId());
                hexaStat.getStats().put(0, new Tuple<>(HexaCore.HexaStatType.getValByType(stat0), 1));
                hexaStat.getStats().put(1, new Tuple<>(HexaCore.HexaStatType.getValByType(stat1), 1));
                hexaStat.getStats().put(2, new Tuple<>(HexaCore.HexaStatType.getValByType(stat2), 1));
                hexaStat.saveToSQL();
                chr.getHexaStats().put(stat, hexaStat);
                if (!chr.hasSkill(500071000)) {
                    chr.addSkill(500071000, 1, 1);
                }
                chr.write(WvsContext.hexaStatsUpdate(chr));
                chr.write(WvsContext.hexaMessage(type, 0, coreID, 1));
                break;
            }
            case 3: {
                int coreID = inPacket.decodeInt();
                double weight = Double.longBitsToDouble(inPacket.decodeLong());
                HexaCore.HexaStatCoreData coreData = HexaCore.getStatCoreData(coreID);
                HexaStat hexaStat = chr.getHexaStatByCoreID(coreID);
                if (coreData == null || hexaStat == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.dispose();
                    return;
                }
                Tuple<HexaCore.HexaStatType, Integer> info0 = hexaStat.getStats().get(0);
                Tuple<HexaCore.HexaStatType, Integer> info1 = hexaStat.getStats().get(1);
                Tuple<HexaCore.HexaStatType, Integer> info2 = hexaStat.getStats().get(2);
                if (info0 == null || info1 == null || info2 == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.dispose();
                    return;
                }
                int level0 = info0.getRight();
                int level1 = info1.getRight();
                int level2 = info2.getRight();
                if (level0 + level1 + level2 >= coreData.getMaxLevel()) {
                    chr.chatPopup("HEXA 屬性等級已達到最大等級。");
                    chr.dispose();
                    return;
                }
                int erdaCost = HexaMatrixConstants.getSolErdaCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.HEXA_STAT, level0);
                int erdaFragmentCost = HexaMatrixConstants.getSolErdaFragmentCostToUpgrade(HexaMatrixConstants.HexaMatrixSkill.HEXA_STAT, level0);
                int currentSolErdas = chr.getSolErda();
                long currentSolErdaFragments = 0;
                for (int id : HexaMatrixConstants.solErdaFragments) {
                    currentSolErdaFragments += sm.getQuantityOfItem(id);
                }
                if (erdaCost > currentSolErdas) {
                    chr.chatPopup(String.format("靈魂艾爾達不足，無法解鎖該 HEXA 屬性。（需要：%d/%d）", currentSolErdas, erdaCost));
                    chr.dispose();
                    return;
                }
                if (erdaFragmentCost > currentSolErdaFragments) {
                    chr.chatPopup(String.format("靈魂艾爾達碎片不足，無法解鎖該 HEXA 屬性。（需要：%d/%d）", currentSolErdaFragments, erdaFragmentCost));
                    chr.dispose();
                    return;
                }
                double random = new Random().nextDouble();
                int result;
                int oldLevel;
                if (random < HexaMatrixConstants.getHexaStatWeight(level0) && level0 < 10) {
                    result = 0;
                    oldLevel = level0;
                } else if (random < HexaMatrixConstants.getHexaStatWeight(level1) && level1 < 10) {
                    result = 1;
                    oldLevel = level1;
                } else {
                    if (level2 >= 10) {
                        result = 1;
                        oldLevel = level1;
                    } else {
                        result = 2;
                        oldLevel = level2;
                    }
                }
                hexaStat.getStats().get(result).setRight(oldLevel + 1);
                int need = erdaFragmentCost;
                for (int id : HexaMatrixConstants.solErdaFragments) {
                    int take = Math.min(sm.getQuantityOfItem(id), need);
                    if (take > 0) {
                        chr.consumeItem(id, take);
                    }
                    if ((need -= take) == 0) {
                        break;
                    }
                }
                hexaStat.saveToSQL();
                chr.addSolErda(-erdaCost);
                chr.write(WvsContext.hexaStatsUpdate(chr));
                chr.write(WvsContext.hexaMessage(type, 0, coreID, 0));
                break;
            }
            case 4: {
                inPacket.decodeInt();
                break;
            }
            case 5: {
                int coreID = inPacket.decodeInt();
                int preset = inPacket.decodeInt();
                HexaStat hexaStat = chr.getHexaStats().get(preset);
                if (hexaStat == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.dispose();
                    return;
                }
                chr.write(WvsContext.hexaStatsUpdate(chr));
                chr.write(WvsContext.hexaMessage(type, 0, 0, 0));
                break;
            }
            case 6: {
                int coreID = inPacket.decodeInt();
                int stat = HexaMatrixConstants.getHexaStatByCoreID(coreID);
                HexaStat hexaStat = chr.getHexaStats().get(stat);
                if (hexaStat == null) {
                    chr.chatPopup("未知錯誤。");
                    chr.dispose();
                    return;
                }
                if (chr.getMoney() < 10_000_000L) {
                    chr.chatPopup(String.format("楓幣不足，無法執行該操作。（需要：%s/%s）", Util.getNumberFormat(chr.getMoney()), Util.getNumberFormat(10_000_000L)));
                    chr.dispose();
                    return;
                }
                hexaStat.getStats().get(0).setRight(0);
                hexaStat.getStats().get(1).setRight(0);
                hexaStat.getStats().get(2).setRight(0);
                hexaStat.saveToSQL();
                chr.deductMoney(10_000_000L);
                chr.write(WvsContext.hexaStatsUpdate(chr));
                chr.write(WvsContext.hexaMessage(type, 0, 0, 0));
                break;
            }
            case 7: {
                long cost = inPacket.decodeLong();
                if (chr.getMoney() < cost) {
                    chr.chatPopup(String.format("楓幣不足，無法執行該操作。（需要：%s/%s）", Util.getNumberFormat(chr.getMoney()), Util.getNumberFormat(cost)));
                    chr.dispose();
                    return;
                }
                int size = inPacket.decodeInt();
                for (int i = 0; i < size; i++) {
                    int coreID = inPacket.decodeInt();
                    int stat = HexaMatrixConstants.getHexaStatByCoreID(coreID);
                    HexaStat hexaStat = chr.getHexaStats().get(stat);
                    if (hexaStat == null) {
                        continue;
                    }
                    int statType0 = inPacket.decodeInt();
                    HexaCore.HexaStatType type0 = HexaCore.HexaStatType.getValByType(statType0);
                    if (type0 != null) {
                        hexaStat.getStats().get(0).setLeft(type0);
                    }
                    int statType1 = inPacket.decodeInt();
                    HexaCore.HexaStatType type1 = HexaCore.HexaStatType.getValByType(statType1);
                    if (type1 != null) {
                        hexaStat.getStats().get(1).setLeft(type1);
                    }
                    int statType2 = inPacket.decodeInt();
                    HexaCore.HexaStatType type2 = HexaCore.HexaStatType.getValByType(statType2);
                    if (type2 != null) {
                        hexaStat.getStats().get(0).setLeft(type2);
                    }
                    hexaStat.saveToSQL();
                }
                chr.deductMoney(cost);
                chr.write(WvsContext.hexaStatsUpdate(chr));
                chr.write(WvsContext.hexaMessage(type, 0, 0, 0));
                break;
            }
            case 8: {
                String spw = inPacket.decodeString(); // removed
                chr.write(WvsContext.hexaMessage(type, 0, 0, 0));
                break;
            }
        }
    }

    @Handler(op = InHeader.HEXA_ERDA_CONVERSION)
    public static void handleErdaConversion(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();
        if (type == 0) {
            chr.write(WvsContext.hexaSkillErdaConversion(0));
        } else if (type == 1) {
            final int[][] conversions = HexaMatrixConstants.erdaConversions;
            int itemID = inPacket.decodeInt();   // ví dụ 2638879
            int erdaCost = inPacket.decodeInt(); // ví dụ 1
            int rewardPerOne = 0;
            for (int[] c : conversions) {
                if (c[0] == itemID) {
                    rewardPerOne = c[1];
                    break;
                }
            }
            if (rewardPerOne <= 0) {
                return; // item không hợp lệ
            }
            int currentSolErdas = chr.getSolErda();
            if (erdaCost > currentSolErdas) {
                chr.chatPopup(String.format("靈魂艾爾達不足，無法轉換。（需要：%d/%d）", currentSolErdas, erdaCost));
                chr.dispose();
                return;
            }
            chr.addSolErda(-erdaCost);
            int rewardQty = rewardPerOne * erdaCost;
            if (!chr.canHold(itemID, rewardQty)) {
                chr.chatPopup("你的背包已滿。");
                chr.dispose();
                return;
            }
            chr.addItemToInventory(itemID, rewardQty);
            chr.write(WvsContext.hexaSkillErdaConversion(1, erdaCost, itemID, rewardQty));
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_OPEN_QUICK_MOVE_REQUEST)
    public static void handleUserOpenQuickMoveRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeShort();
        chr.write(WvsContext.userOpenQuickMove(type != 0, GameConstants.getQuickMoveInfos().stream().filter(qmi -> !qmi.isNoInstances() || chr.getField().isChannelField()).collect(Collectors.toList())));
    }

    @Handler(op = InHeader.USER_MEDAL_MODIFIED)
    public static void handleUserMedalModified(Char chr, InPacket inPacket) {
        int questID = QuestConstants.TITLE_MEDAL_SHOW;
        byte isOn = inPacket.decodeByte();
        inPacket.decodeInt();
        if (!chr.hasQuest(questID)) {
            chr.createQuestWithQRValue(questID, "1007=" + isOn + ";1009=1");
        } else {
            chr.setQRValueByKey(questID, 1007 + "", isOn + "");
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_TITLE_MODIFIED)
    public static void handleUserTitleModified(Char chr, InPacket inPacket) {
        int questID = QuestConstants.TITLE_MEDAL_SHOW;
        byte isOn = inPacket.decodeByte();
        inPacket.decodeInt();
        if (!chr.hasQuest(questID)) {
            chr.createQuestWithQRValue(questID, "1007=1;1009=" + isOn);
        } else {
            chr.setQRValueByKey(questID, 1009 + "", isOn + "");
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_DECORATION_INVENTORY_MODIFIED)
    public static void handleUserDecorationInventoryModified(Char chr, InPacket inPacket) {
        byte wp_normal = inPacket.decodeByte();
        byte wp_jump = inPacket.decodeByte();
        byte wp_particle = inPacket.decodeByte();
        byte cape = inPacket.decodeByte();
        chr.createQuestWithQRValue(27044, "wp_normal="+wp_normal+";wp_jump="+wp_jump+";wp_particle="+wp_particle+";cape="+cape);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_CASH_EQUIPMENT_PRESET_MODIFIED)
    public static void handleUserCashEquipmentPresetModified(Char chr, InPacket inPacket) {
        int preset = inPacket.decodeInt();
        chr.chatPopup("該功能正在完善中，請稍後再試。");
        //String name = "preset" + preset;
        //chr.createQuestWithQRValue(27043, "preset0=0;preset1=1;applied_new=1;preset_new=1");
        //chr.setQRValueByKey(27043, name, preset);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_EQUIPMENT_PRESET_MODIFIED)
    public static void handleUserEquipmentPresetModified(Char chr, InPacket inPacket) {
        inPacket.decodeInt();
        int preset = inPacket.decodeByte();
        //chr.setQRValueByKey(QuestConstants.EQUIPPED_PRESET, "preset", preset);
        //chr.write(WvsContext.changeEquippedInventoryPreset(preset));
        chr.chatPopup("該功能正在完善中，請稍後再試。");
        chr.dispose();
    }

    @Handler(op = InHeader.USER_CHARACTER_PRESET_MODIFIED)
    public static void handleCharacterPresetModified(Char chr, InPacket inPacket) {
        int questID = QuestConstants.CHARACTER_PRESET_SETTING;
        int type = inPacket.decodeInt();
        if (type == 0 || type == 1) {
            int unk1 = inPacket.decodeInt(); // 63
            int unk2 = inPacket.decodeInt(); // 0
            int unk3 = inPacket.decodeInt(); // 0
            int unk4 = inPacket.decodeInt(); // 0
            int preset = inPacket.decodeInt(); // 0
            String name = inPacket.decodeString(); // PRESET1,2,3,4,...
            int equipmentPreset = inPacket.decodeInt(); // TODO
            int hyperStatPreset = inPacket.decodeInt();
            int abilityPreset = inPacket.decodeInt();
            int legionPreset = inPacket.decodeInt(); // TODO
            int linkSkillPreset = inPacket.decodeInt();
            int hotkeySettingsPreset = inPacket.decodeInt();

            String format = String.format("%d%d%d%d%d%d", equipmentPreset, hyperStatPreset, abilityPreset, legionPreset, linkSkillPreset, hotkeySettingsPreset);
            chr.setQRValueByKey(questID, preset + "", format);
            chr.setQRValueByKey(questID, "name" + preset, name);

            chr.write(WvsContext.updateCharacterPresetResult(0));

            preset = Integer.parseInt(chr.getQRValueByKey(QuestConstants.HYPER_STATS_PRESET, "hyperstats"));
            if (hyperStatPreset >= 0 && hyperStatPreset <= 2 && hyperStatPreset != preset) {
                UserStatHandler.handleChangeHyperStatPreset(chr, hyperStatPreset);
            }
            preset = Integer.parseInt(chr.getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "preset"));
            if (abilityPreset >= 0 && abilityPreset <= 2 && abilityPreset != preset) {
                chr.setQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential", abilityPreset);
                chr.write(WvsContext.characterPotentialChangePreset(true, abilityPreset));
            }
            preset = Integer.parseInt(chr.getQRValueByKey(QuestConstants.LINK_SKILL_PRESET, "preset"));
            if (linkSkillPreset >= 0 && linkSkillPreset <= 2 && linkSkillPreset != preset) {
                chr.write(WvsContext.linkedSkillInfo(type, (byte) linkSkillPreset, null));
                chr.setQRValueByKey(QuestConstants.LINK_SKILL_PRESET, "preset", preset);
                chr.initLinkSkills(false);
            }
            preset = Integer.parseInt(chr.getQRValueByKey(QuestConstants.KEY_BINDINGS_PRESET, "no"));
            if (hotkeySettingsPreset >= 0 && hotkeySettingsPreset <= 2 && hotkeySettingsPreset != preset) {
                if (chr.hasQuest(QuestConstants.KEY_BINDINGS_PRESET)) {
                    chr.setQRValueByKey(QuestConstants.KEY_BINDINGS_PRESET, "no", hotkeySettingsPreset + "");
                } else {
                    chr.createQuestWithQRValue(QuestConstants.KEY_BINDINGS_PRESET, "no=" + hotkeySettingsPreset + ";unionkey=1");
                }
            }
        } else if (type == 2) {
            // unk
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_QUICK_SLOT_MODIFIED)
    public static void handleUserQuickSlotModified(Char chr, InPacket inPacket) {
        if (chr == null) {
            return;
        }
        //int gameSetting = 999;
        int quickSlotID = QuestConstants.QUICK_SLOT_SETTING;
        if (!chr.hasQuest(quickSlotID)) {
            chr.createQuestWithQRValue(quickSlotID, "qsLock=0;qsLeft=1;petHP=50;qsCol=16;petMP=50;qsMin=0;qmMin=0");
        }
        int settingID = QuestConstants.OVERAL_SETTING;
        if (!chr.hasQuest(settingID)) {
            chr.createQuestWithQRValue(settingID, "acpHP=0;alFol=1;chAl=1;alAl=1;alPa=1;alExch=1;alHome=1;soErUI=0;alMe=1;bufFav=0;chFr=1;alFr=1;bufAual=1;alMapT=1;chGu=1;frOnNot=1;flHP=19;alGu=1;bufMin=0;soulUI=1;alWh=1;flMP=19;debufMin=0");
        }
        int type = inPacket.decodeInt();
        if (type == 0) {
            byte size = inPacket.decodeByte();
            for (int i = 0; i < size; i++) {
                int key = inPacket.decodeInt();
                int value = inPacket.decodeInt();
                switch (key) {
                    case 0:
                        //strings.add("SOBGMVOL=" + value);
                        chr.setQRValueByKey(370, "vBG1", value);
                        break;
                    case 1:
                        //strings.add("SOBGMMUTE=" + value);
                        chr.setQRValueByKey(370, "mBG1", value);
                        break;
                    case 2:
                        //strings.add("SOSEVOL=" + value);
                        chr.setQRValueByKey(370, "vE1", value);
                        break;
                    case 3:
                        //strings.add("SOSEMUTE=" + value);
                        chr.setQRValueByKey(370, "mE1", value);
                        break;
                    case 4:
                        //strings.add("SOANDROIDMUTE=" + value);
                        chr.setQRValueByKey(370, "mAnd1", value);
                        break;
                    case 5:
                        //strings.add("SOSSEVOL=" + value);
                        chr.setQRValueByKey(370, "vSE1", value);
                        break;
                    case 6:
                        //strings.add("SOSSEMUTE=" + value);
                        chr.setQRValueByKey(370, "mSE1", value);
                        break;
                    case 7:
                        //strings.add("SOVOICEVOL=" + value);
                        chr.setQRValueByKey(370, "vSV1", value);
                        break;
                    case 8:
                        //strings.add("SOVOICEMUTE=" + value);
                        chr.setQRValueByKey(370, "mSV1", value);
                        break;
                    case 9:
                        //strings.add("SOMASTERVOL=" + value);
                        chr.setQRValueByKey(370, "vM1", value);
                        break;
                    case 10:
                        //strings.add("SOMASTERMUTE=" + value);
                        chr.setQRValueByKey(370, "mM1", value);
                        break;
                    case 11:
                        //strings.add("SOMSEVOL=" + value);
                        chr.setQRValueByKey(370, "vME1", value);
                        break;
                    case 12:
                        //strings.add("SOMSEMUTE=" + value);
                        chr.setQRValueByKey(370, "mME1", value);
                        break;
                    case 15:
                        //strings.add("SOSHOTAUTO=" + value);
                        chr.setQRValueByKey(368, "sAuto1", value);
                        break;
                    case 16:
                        //strings.add("SOSHOTCONTI=" + value);
                        chr.setQRValueByKey(368, "sCon1", value);
                        break;
                    case 18:
                        chr.setQRValueByKey(368, "mobInf1", value);
                        break;
                    case 20:
                        chr.setQRValueByKey(368, "damEff1", value);
                        break;
                    case 21:
                        chr.setQRValueByKey(368, "vSync1", value);
                        break;
                    case 22:
                        chr.setQRValueByKey(368, "fSize2", value);
                        break;
                    case 24:
                        //strings.add("FONTCOLORFRIEND=" + value);
                        chr.setQRValueByKey(368, "fcF2", value);
                        break;
                    case 25:
                        //strings.add("FONTCOLORGUILD=" + value);
                        chr.setQRValueByKey(368, "fcG2", value);
                        break;
                    case 26:
                        //strings.add("FONTCOLORALLIANCE=" + value);
                        chr.setQRValueByKey(368, "fcA2", value);
                        break;
                    case 23:
                        //strings.add("FONTCOLORWHISPER=" + value);
                        chr.setQRValueByKey(368, "fcW2", value);
                        break;
                    case 32:
                        chr.setQRValueByKey(368, "aOther2", value);
                        break;
                    case 33:
                        //strings.add("SOMOBEDGE=" + value);
                        chr.setQRValueByKey(368, "mEdge2", value);
                        break;
                    case 36:
                        chr.setQRValueByKey(369, "avMega2", value);
                        break;
                    case 37:
                        //strings.add("SOPOPUPUICHATWND=" + value);
                        chr.setQRValueByKey(369, "wndHtK1", value);
                        break;
                    case 38:
                        //strings.add("SOSHOWCHATTIMESTAMP=" + value);
                        chr.setQRValueByKey(369, "chTime2", value);
                        break;
                    case 55:
                        chr.setQRValueByKey(369, "damAmt3", value);
                        break;
                    case 56:
                        //strings.add("MAPLELAB_SILHOUETTE_ACTIVE=" + value);
                        chr.setQRValueByKey(369, "silBo3", value);
                        break;
                    case 57:
                        //strings.add("MAPLELAB_SILHOUETTE_TYPE=" + value);
                        chr.setQRValueByKey(369, "silTy3", value);
                        break;
                    case 58:
                        //strings.add("MAPLELAB_SILHOUETTE_THICKNESS=" + value);
                        chr.setQRValueByKey(369, "silTh3", value);
                        break;
                    case 59:
                        //strings.add("MAPLELAB_UPSCALER_ACTIVE=" + value);
                        chr.setQRValueByKey(369, "mlUpAc2", value);
                        break;
                    case 60:
                        //strings.add("MAPLELAB_UPSCALER_TYPE=" + value);
                        chr.setQRValueByKey(370, "mlUpTy2", value);
                        break;
                    case 66:
                        chr.setQRValueByKey(370, "apEm", value);
                        break;
                    case 67:
                        //strings.add("FONTCOLORNORMAL=" + value);
                        chr.setQRValueByKey(370, "fcN", value);
                        break;
                    case 68:
                        //strings.add("FONTCOLORPARTY=" + value);
                        chr.setQRValueByKey(370, "fcP", value);
                        break;
                    case 69:
                        //strings.add("MAPLELAB_EFFICIENCYMODE_AUTOOFF_ACTIVE=" + value);
                        chr.setQRValueByKey(370, "emAOFF", value);
                        break;
                }
                chr.dbgChatMsg("[GameSetting] Key : " + key + "; value : " + value);
            }
            return;
        }
        if (inPacket.getUnreadAmount() > 0 && chr.getField() != null) {
            byte size = inPacket.decodeByte();
            for (int i = 0; i < size; i++) {
                int key = inPacket.decodeInt();
                int value = inPacket.decodeInt();
                chr.dbgChatMsg("[QuickSlot] Key : " + key + "; value : " + value);
                if (key == 0) {
                    chr.setQRValueByKey(settingID, "alWh", value + "");
                } else if (key == 1) {
                    chr.setQRValueByKey(settingID, "chFr", value + "");
                } else if (key == 2) {
                    chr.setQRValueByKey(settingID, "alMe", value + "");
                } else if (key == 3) {
                    chr.setQRValueByKey(settingID, "alExch", value + "");
                } else if (key == 4) {
                    chr.setQRValueByKey(settingID, "soulUI", value + "");
                } else if (key == 5) {
                    chr.setQRValueByKey(settingID, "alPa", value + "");
                } else if (key == 6) {
                    chr.setQRValueByKey(settingID, "alGu", value + "");
                } else if (key == 7) {
                    chr.setQRValueByKey(settingID, "alAl", value + "");
                } else if (key == 8) {
                    chr.setQRValueByKey(settingID, "chGu", value + "");
                } else if (key == 9) {
                    chr.setQRValueByKey(settingID, "chAl", value + "");
                } else if (key == 10) {
                    chr.setQRValueByKey(settingID, "alFr", value + "");
                } else if (key == 11) {
                    chr.setQRValueByKey(settingID, "frOnNot", value + "");
                } else if (key == 12) {
                    chr.setQRValueByKey(settingID, "alMapT", value + "");
                } else if (key == 13) {
                    chr.setQRValueByKey(settingID, "soErUI", value + "");
                } else if (key == 15) {
                    chr.setQRValueByKey(settingID, "flHP", value + "");
                } else if (key == 16) {
                    chr.setQRValueByKey(settingID, "flMP", value + "");
                } else if (key == 17) {
                    chr.setQRValueByKey(settingID, "alFol", value + "");
                } else if (key == 18) {
                    chr.setQRValueByKey(settingID, "bufAual", value + "");
                } else if (key == 19) {
                    chr.setQRValueByKey(settingID, "bufMin", value + "");
                } else if (key == 20) {
                    chr.setQRValueByKey(settingID, "bufFav", value + "");
                } else if (key == 21) {
                    chr.setQRValueByKey(settingID, "debufMin", value + "");
                } else if (key == 29) {
                    chr.setQRValueByKey(settingID, "alHome", value + "");
                }

                else if (key == 22) {
                    chr.setQRValueByKey(quickSlotID, "petHP", value + "");
                } else if (key == 23) {
                    chr.setQRValueByKey(quickSlotID, "petMP", value + "");
                } else if (key == 24) {
                    chr.setQRValueByKey(quickSlotID, "qsLock", value + "");
                } else if (key == 25) {
                    chr.setQRValueByKey(quickSlotID, "qmMin", value + "");
                } else if (key == 26) {
                    chr.setQRValueByKey(quickSlotID, "qsCol", value + "");
                } else if (key == 27) {
                    chr.setQRValueByKey(quickSlotID, "qsLeft", value + "");
                } else if (key == 28) {
                    chr.setQRValueByKey(quickSlotID, "qsMin", value + "");
                }
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_CHANGE_CUSTOMIZED_BACKGROUND_REQUEST)
    public static void handleChangeCustomizedBackgroundRequest(Char chr, InPacket inPacket) {
        int id = inPacket.decodeInt();
        chr.createQuestWithQRValue(7295, "" + id, false);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_QUICK_MOVE_SCRIPT)
    public static void handleUserQuickMoveScript(Char chr, InPacket inPacket) {
        QuickMoveType quickMoveType = QuickMoveType.getValByNum(inPacket.decodeInt());
        if (quickMoveType == null) {
            chr.dispose();
            return;
        }
        ScriptManagerImpl sm = chr.getScriptManager();
        switch (quickMoveType) {
            case MonsterPark:
                sm.startScript(chr, 9071003, "quick_monsterPark", ScriptType.Npc);
                break;
            case Ardentmill:
                sm.startScript(chr, 9031019, "quick_MeisterVille", ScriptType.Npc);
                break;
            case LegionCoinShop:
                sm.openShop(9010111, 9010107);
                break;
            case CrystalShop:
                sm.openShop(9001212, GameConstants.GENERAL_SHOP);
                break;
            case AuctionHouse:
                sm.startScript(chr, 9030300, "quick_auctionMove", ScriptType.Npc);
                break;
            case MesoMarket:
                sm.openUI(1615);
                break;
            case DimensionalPortal:
                sm.sendUnityPortalDialog();
                break;
            case SymbolExpressPass:
                sm.startScript(chr, 3003146, "quickpath_symbol", ScriptType.Npc);
                break;
            case MapleAdministrator:
                sm.startScript(chr, 9010000, "quick_adminNPC", ScriptType.Npc);
                break;
            case StorageRoom:
                sm.openTrunk(9070110);
                break;
        }
    }

    @Handler(op = InHeader.USER_NODE_STONE_REQUEST)
    public static void handleUserNodeStoneInBulkRequest(Char chr, InPacket inPacket) {
        ScriptManagerImpl sm = chr.getScriptManager();
        int type = inPacket.decodeInt();
        int quantity = inPacket.decodeInt();
        if (JobConstants.isPinkBean(chr.getJob()) || JobConstants.isYeti(chr.getJob())) {
            chr.chatMessage("粉豆和雪吉拉無法使用此功能。");
            return;
        }
        if (!sm.hasQuestCompleted(QuestConstants.FIFTH_JOB_QUEST)) {
            chr.chatMessage("請先完成 5 轉任務。");
            return;
        }
        int itemId = switch (type) {
            case 3 -> 2632972;
            case 101 -> 2638846;
            case 102 -> 2831071;
            default -> -1;
        };

        final int[] TYPE0_ITEMS = {2435719, 2435902, 2436078, 2439869, 2436324, 2631128};
        final int[] TYPE1_ITEMS = {2438411, 2438412};
        final int[] TYPE2_ITEMS = {2439279, 2630402, 2631527, 2632290};

        int haveQuantity;
        if (type == 0) {
            int sum = 0;
            for (int id : TYPE0_ITEMS) {
                sum += sm.getQuantityOfItem(id);
            }
            haveQuantity = sum;
        } else if (type == 1) {
            int sum = 0;
            for (int id : TYPE1_ITEMS) {
                sum += sm.getQuantityOfItem(id);
            }
            haveQuantity = sum;
        }  else if (type == 2) {
            int sum = 0;
            for (int id : TYPE2_ITEMS) {
                sum += sm.getQuantityOfItem(id);
            }
            haveQuantity = sum;
        } else if (itemId != -1) {
            haveQuantity = sm.getQuantityOfItem(itemId);
        } else {
            chr.dispose();
            return;
        }
        if (haveQuantity < quantity) {
            chr.dispose();
            return;
        }
        switch (type) {
            case 0: {
                int left = quantity;
                for (int id : TYPE0_ITEMS) {
                    int q = sm.getQuantityOfItem(id);
                    if (q <= 0) {
                        continue;
                    }
                    int take = Math.min(q, left);
                    sm.openNodeStonesInBulk(type, id, take);
                    left -= take;
                    if (left == 0) {
                        break;
                    }
                }
                break;
            }
            case 1: {
                // Mirror World Nodestone
                int left = quantity;
                for (int id : TYPE1_ITEMS) {
                    int q = sm.getQuantityOfItem(id);
                    if (q <= 0) {
                        continue;
                    }
                    int take = Math.min(q, left);
                    sm.openNodeStonesByCoreIDInBulk(type, id, take, 10000024);
                    left -= take;
                    if (left == 0) {
                        break;
                    }
                }
                break;
            }
            case 2: {
                // Experience Nodestone
                int left = quantity;
                for (int id : TYPE2_ITEMS) {
                    int q = sm.getQuantityOfItem(id);
                    if (q <= 0) {
                        continue;
                    }
                    int take = Math.min(q, left);
                    sm.openNodeStonesByCoreIDInBulk(type, id, take, 40000000);
                    left -= take;
                    if (left == 0) {
                        break;
                    }
                }
                break;
            }
            case 3: {
                // Mitra's Nodestone
                sm.openNodeStonesByCoreIDInBulk(type, itemId, quantity, 10000031);
                break;
            }
            case 101: {
                // Mapae Nodestone
                int[] coreIDs = {30000025, 30000026, 30000027, 30000028, 30000029, 30000030};
                sm.openNodeStonesByRandomCoreIDsInBulk(type, itemId, quantity, coreIDs);
                break;
            }
            case 102: {
                // Roro Nodestone
                int[] coreIDs = {30000031, 30000032, 30000033};
                sm.openNodeStonesByRandomCoreIDsInBulk(type, itemId, quantity, coreIDs);
                break;
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_MEDAL_HONOR_REQUEST)
    public static void handleUserMedalOfHonorRequest(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        int quantity = inPacket.decodeInt();
        ScriptManagerImpl sm = chr.getScriptManager();
        int currentQuantity = sm.getQuantityOfItem(itemID);
        if (currentQuantity < quantity || quantity < 0) {
            chr.chatPopup("道具數量不足，無法兌換名譽經驗。");
            chr.dispose();
            return;
        }
        int amount = 0;
        switch (itemID) {
            case 2433840:
            case 2433926:
                amount = 100;
                break;
            case 2432586:
            case 2434637:
            case 2436983:
            case 2439150:
                amount = 500;
                break;
            case 2432602:
            case 2433803:
            case 2432970:
            case 2433457:
            case 2434146:
            case 2434502:
            case 2434638:
            case 2435475:
            case 2435498:
            case 2435776:
            case 2638610:
                amount = 1_000;
                break;
            case 2434639:
            case 2632453:
                amount = 2_000;
                break;
            case 2434590:
            case 2436377:
            case 2633892:
            case 2636292:
            case 2636293:
                amount = 5_000;
                break;
            case 2433808:
            case 2434021:
            case 2434175:
            case 2434287:
            case 2434288:
            case 2434290:
            case 2434381:
            case 2434591:
            case 2434783:
            case 2436378:
            case 2631913:
            case 2436272:
            case 2436562:
            case 2636880:
            case 2638385:
                amount = 10_000;
                break;
            case 2439284:
                amount = 30_000;
                break;
            case 2439297:
                amount = 40_000;
                break;
            case 2439410:
                amount = 50_000;
                break;
        }
        if (amount == 0) {
            sm.chat("無法使用該道具。請聯繫開發者：" + itemID);
            return;
        }
        amount *= quantity;
        sm.chat(String.format("你獲得了 %s 榮譽經驗值。", Util.getNumberFormat(amount)));
        //chr.write(WvsContext.userHonorItemResult(0, amount));
        chr.addHonorExp(amount);
        for (int i = 0; i < quantity; i++) {
            sm.consumeItem(itemID);
        }
        sm.playExclSoundWithDownBGM("FarmSE.img/levelUp", 100);
        if (EventConstants.HYPER_BURNING_MAX && chr.hasQuest(102435)
                && !"2".equals(chr.getQRValueByKey(102435, "step"))) {
            sm.createQuestWithQRValue(102435, "honorItem=1;step=2;give=1");
        }
    }

    @Handler(op = InHeader.USER_MAPLE_PLANNER_MODIFIED)
    public static void handleUserMaplePlannerModified(Char chr, InPacket inPacket) {
        int mode = inPacket.decodeInt();
        int type = inPacket.decodeInt();
        ScriptManagerImpl sm = chr.getScriptManager();
        // MapleScheduler.img.xml
        switch (mode) {
            case 0:
                switch (type) {
                    case 1: {
                        int id = inPacket.decodeInt();
                        int questID = 0;
                        switch (id) {
                            case 2: // Vanishing Journey Daily Quest
                                questID = QuestConstants.VANISHING_JOURNEY_DAILY_QUEST_NEW;
                                break;
                            case 3: // Chu Chu Island Daily Quest
                                questID = QuestConstants.CHU_CHU_DAILY_QUEST_NEW;
                                break;
                            case 4: // Lachelein Daily Quest
                                questID = QuestConstants.LACHELEIN_DAILY_QUEST_NEW;
                                break;
                            case 5: // Arcana Daily Quest
                                questID = QuestConstants.ARCANA_DAILY_QUEST_NEW;
                                break;
                            case 6: // Morass Daily Quest
                                questID = QuestConstants.MORASS_DAILY_QUEST_NEW;
                                break;
                            case 7: // Esfera Daily Quest
                                questID = QuestConstants.ESFERA_DAILY_QUEST_NEW;
                                break;
                            case 8: // Moonbridge Daily Quest
                                questID = QuestConstants.MOONBRIDGE_DAILY_QUEST;
                                break;
                            case 9: // Labyrinth of Suffering Daily Quest
                                questID = QuestConstants.LADYRINTH_DAILY_QUEST;
                                break;
                            case 10: // Limina Daily Quest
                                questID = QuestConstants.LIMINA_DAILY_QUEST;
                                break;
                            case 11: // Cernium Daily Quest
                                questID = 19020;
                                break;
                            case 12: // Hotel Arcus Daily Quest
                                questID = 19024;
                                break;
                            case 13: // Odium Daily Quest
                                questID = 19028;
                                break;
                            case 14: // Shangri-La Daily Quest
                                questID = 19032;
                                break;
                            case 15: // Arteria Daily Quest
                                questID = 19036;
                                break;
                            case 16: // Carcion Daily Quest
                                questID = 19040;
                                break;
                            case 17: // Tallahart Daily Quest
                                questID = 19044;
                                break;
                        }
                        if (questID != 0) {
                            String questName = String.format("q%d%s", questID, ScriptManagerImpl.QUEST_START_SCRIPT_END_TAG);
                            if (!chr.canStartQuest(questID)) {
                                return;
                            }
                            chr.getScriptManager().startScript(chr, questID, questName, ScriptType.Quest);
                        }
                        break;
                    }
                }
                break;
            case 1:
                switch (type) {
                    case 1: {
                        int id = inPacket.decodeInt();
                        int questID = inPacket.decodeInt();
                        QuestInfo qi = QuestData.getQuestInfoById(questID);
                        if (qi == null) {
                            return;
                        }
                        String scriptName = qi.getEndScript();
                        if (QuestConstants.isAccountQuest(questID)) {
                            if (!chr.hasQuestInProgress(questID) || !chr.getAccount().getQuestById(questID).isComplete(chr)) {
                                System.out.println("Could not complete account quest, as the prerequisites haven't been met.");
                                return;
                            }
                        } else {
                            if (!chr.hasQuestInProgress(questID) || !chr.getQuestById(questID).isComplete(chr)) {
                                System.out.println("Could not complete character quest, as the prerequisites haven't been met.");
                                return;
                            }
                        }
                        if (scriptName == null || scriptName.equalsIgnoreCase("")) {
                            scriptName = String.format("q%d%s", questID, ScriptManagerImpl.QUEST_COMPLETE_SCRIPT_END_TAG);
                        }
                        chr.getScriptManager().startScript(chr, questID, scriptName, ScriptType.Quest);
                        break;
                    }
                }
                break;
            case 2:
                switch (type) {
                    case 1: {
                        int id = inPacket.decodeInt();
                        if (id == 0) { // Monster Park
                            sm.startScript(chr, 9071003, "quick_monsterPark", ScriptType.Npc);
                        }
                        break;
                    }
                    case 2: {
                        int id = inPacket.decodeInt();
                        if (id == 7) { // Mu Lung Dojo
                            // tp to mulung
                        }
                        break;
                    }
                }
                break;
            case 3: // start All?
                break;
            case 5:
                int count = inPacket.decodeInt();
                switch (type) {
                    case 1: {
                        String sb = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
                        char[] bits = sb.toCharArray();

                        for (int i = 0; i < count; i++) {
                            int result = inPacket.decodeInt();
                            if (0 <= result && result < bits.length) {
                                bits[result] = '1';
                            }
                        }

                        sb = new String(bits);
                        chr.createQuestWithQRValue(39395, "scheduled="+sb+";typeOrder=contents,quest");
                        break;
                    }
                    case 2: {
                        String sb = "0000000000000000000000";
                        char[] bits = sb.toCharArray();

                        for (int i = 0; i < count; i++) {
                            int result = inPacket.decodeInt();
                            if (0 <= result && result < bits.length) {
                                bits[result] = '1';
                            }
                        }

                        sb = new String(bits);
                        chr.createQuestWithQRValue(39396, "scheduled="+sb+";typeOrder=contents,quest");
                        break;
                    }
                    case 3: {
                        String sb = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
                        char[] bits = sb.toCharArray();

                        for (int i = 0; i < count; i++) {
                            int result = inPacket.decodeInt();
                            if (0 <= result && result < bits.length) {
                                bits[result] = '1';
                            }
                        }

                        sb = new String(bits);
                        chr.createQuestWithQRValue(39397, "scheduled=" + sb);
                        break;
                    }
                }
                break;
        }
        chr.write(UserLocal.maplePLannerUpdate(mode));
    }

    @Handler(op = InHeader.USER_FARVORITE_MAP_REQUEST)
    public static void handleUserFarvoriteMapRequest(Char chr, InPacket inPacket) {
        if (!chr.hasQuest(7697)) {
            chr.createQuestWithQRValue(7697, "0=0;1=0;2=0;3=0;4=0;5=0;6=0;7=0;8=0;9=0;10=0;11=0;12=0;13=0;14=0");
        }
        if (!chr.hasQuest(7698)) {
            chr.createQuestWithQRValue(7698, "15=0;16=0;17=0;18=0;19=0;20=0;21=0;22=0;23=0;24=0;25=0;26=0;27=0;28=0;29=0");
        }
        int size = inPacket.decodeInt();
        for (int i = 0; i < size; i++) {
            if (i >= 15) {
                chr.setQRValueByKey(7698, i, inPacket.decodeInt());
            } else {
                chr.setQRValueByKey(7697, i, inPacket.decodeInt());
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.EXTRA_SYSTEM_REQUEST)
    public static void handleExtraSystemRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt();
        short type1 = inPacket.decodeShort();
        switch (type1) {
            case 36: {
                int skillID = inPacket.decodeInt();
                if (!JobConstants.isMoXuan(chr.getJob()) && !JobConstants.isLynn(chr.getJob())) {
                    return;
                }
                if (skillID == Lynn.PURIFY) {
                    // TODO B6 4C C2 00
                    //  07
                    //  70 00 00 00
                    //  51 F3 3F 24
                    //  01
                    //  22 00
                    //  01 00
                    //  01 00
                    //  A0 34 42 0A
                    //  02 00
                    //  73 57 A4 41
                    //  33 B8 18 41
                    //  03 00
                    //  00 00
                    //  A0 41 00 00
                    //  00 00 00
                    break;
                }
                final boolean isSoul = skillID == MoXuan.SOUL_ART_THE_CONQUERED_SELF;
                final boolean isQi   = skillID == MoXuan.SECRET_ART_QI_PROJECTION;
                if (!isSoul && !isQi) {
                    return;
                }
                if (chr.hasSkillOnCooldown(MoXuan.SECRET_ART_QI_PROJECTION)) {
                    return;
                }
                final int atomId = isSoul ? MoXuan.SOUL_ART_THE_CONQUERED_SELF_ATOM : MoXuan.SECRET_ART_QI_PROJECTION_ATOM;
                final int cap = isSoul ? 10 : 8;
                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                Option o1 = new Option();
                o1.nValue = MoXuan.PERIODICAL_SKILL_ACTIVATION_VALUE;
                o1.nReason = skillID;
                tsm.sendStat(IndiePeriodicalSkillActivation, o1);
                List<Integer> targetList = new ArrayList<>(cap);
                List<ForceAtomInfo> faiList = new ArrayList<>(cap);
                final int now = (int) System.currentTimeMillis();
                final ForceAtomEnum fae = ForceAtomEnum.CLONE_ATOM;
                List<Mob> mobs = chr.getField().getMobs().stream()
                        .filter(m -> m != null && m.getHp() > 0)
                        .toList();
                int mobCount = mobs.size();
                if (mobCount == 0) {
                    return;
                }
                for (int i = 0; i < cap; i++) {
                    Mob mob = mobs.get(i % mobCount);
                    targetList.add(mob.getObjectId());
                    faiList.add(new ForceAtomInfo(
                            chr.getNewForceAtomKey(),
                            fae.getInc(),
                            Util.getRandom(40, 50),
                            Util.getRandom(2, 4),
                            Util.getRandom(100, 400),
                            0,
                            now,
                            0,
                            atomId,
                            new Position()));
                }
                if (!targetList.isEmpty()) {
                    ForceAtom atom = new ForceAtom(chr.getId(), fae, targetList, atomId, faiList);
                    chr.createForceAtom(atom);
                }
                chr.addSkillCooldown(MoXuan.SECRET_ART_QI_PROJECTION, 1500);
                break;
            }
            case 38: {
                int powerType = inPacket.decodeInt();
                if (chr.getJobHandler() instanceof MoXuan moXuan) {
                    if (powerType == 0) {
                        chr.write(WvsContext.sendMoXuanPower(powerType, 0, 3000, 1, 0));
                    } else {
                        int godPower = 0;
                        int time = 30000;
                        if (chr.getSkillLevel(MoXuan.THE_HOLY_MOUNTAIN) > 0) {
                            godPower = (int) moXuan.getGodPower();
                            godPower = Math.max(0, --godPower);
                            moXuan.setGodPower(godPower);
                            if (chr.getSkillLevel(MoXuan.DIVINE_FIST_PERSIST) > 0) {
                                time += 10000;
                            }
                        }
                        chr.write(WvsContext.sendMoXuanPower(powerType, godPower, time, 0, 0));
                    }
                }
                break;
            }
        }
    }

    @Handler(op = InHeader.MO_XUAN_STACK_REQUEST)
    public static void handleMoXuanStackRequest(Char chr, InPacket inPacket) {
        if (chr == null || !JobConstants.isMoXuan(chr.getJob())) {
            int unread = inPacket.getUnreadAmount();
            if (unread > 0) {
                inPacket.decodeArr(unread);
            }
            return;
        }
        if (chr.getJobHandler() instanceof MoXuan moXuan) {
            moXuan.handleStackRequest(chr, inPacket);
        } else {
            int unread = inPacket.getUnreadAmount();
            if (unread > 0) {
                inPacket.decodeArr(unread);
            }
        }
    }

    @Handler(op = InHeader.USER_SAVE_NAVIGATION_REQUEST)
    public static void handleUserSaveNavigationRequest(Char chr, InPacket inPacket) {
        int fieldID = inPacket.decodeInt();
        chr.getScriptManager().startNavigation(fieldID);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_FILTER_MAP_OBJECTS_REQUEST)
    public static void handleUserFilterMapObjectsRequest(Char chr, InPacket inPacket) {
        if (!chr.hasQuest(531)) {
            chr.createQuestWithQRValue(531, "npc=62;char=62;filterOff=0;naviMin=0", false);
        }
        byte filter = inPacket.decodeByte();
        int size = inPacket.decodeInt(); // 2
        int npc = 62 - inPacket.decodeInt();
        int char_ = 62 - inPacket.decodeInt();
        chr.createQuestWithQRValue(531, String.format("npc=%d;char=%d;filterOff=%d;naviMin=%d", npc, char_, filter, 0), false);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_OPEN_FAMILIAR_SHOP)
    public static void handleUserOpenFamiliarShop(Char chr, InPacket inPacket) {
        chr.getScriptManager().openShop(9133404);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_BUFF_FAVORITE_REQUEST)
    public static void handleBuffFavoriteRequest(Char chr, InPacket inPacket) {
        int rowCount  = inPacket.decodeByte() & 0xFF;
        int slotCount = inPacket.decodeByte() & 0xFF; // 10
        int selected  = inPacket.decodeByte() & 0xFF;
        int size = inPacket.decodeInt(); // 100
        Int2ObjectMap<Int2IntMap> favorites = chr.getBuffFavorites();
        for (int i = 0; i < size; i++) {
            int preset  = inPacket.decodeByte() & 0xFF;
            int index   = inPacket.decodeByte() & 0xFF;
            int skillId = inPacket.decodeInt();
            if (preset == 0 && index == 0 && skillId == 0) continue;
            if (preset >= BuffFavorite.MAX_ROW || index >= BuffFavorite.MAX_ROW) continue;
            if (skillId == 0) {
                Int2IntMap m = favorites.get(preset);
                if (m != null) {
                    m.remove(index);
                    if (m.isEmpty()) favorites.remove(preset);
                }
            } else {
                Int2IntMap m = favorites.get(preset);
                if (m == null) {
                    m = new Int2IntOpenHashMap();
                    favorites.put(preset, m);
                }
                m.put(index, skillId);
            }
        }
        BuffFavorite.saveToSQL(chr.getId(), favorites);
    }

    @Handler(op = InHeader.USER_SPECIAL_EXIT)
    public static void handleUserSpecialExit(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();
        int unk = inPacket.decodeInt();
        var sm = chr.getScriptManager();
        switch (type) {
            case 14: // Tera Blink
                if (chr.getFieldID() != EventConstants.TERA_BLINK_FIELD) {
                    break;
                }
                sm.teraBlinkEff(0, "Etc/MinigameClient.img/teraBlink/FadeOut", "", 10, 25);
                sm.teraBlinkWarp(0, "WarpDrive", 1080, 0, 0, 50, 50, 35, 20);
                sm.teraBlinkWarp(0, "WarpDrive", 1080, 0, 0, 50, 50, 35, 20);
                sm.teraBlinkEff(1, "Etc/MinigameClient.img/teraBlink/FadeIn", "", 10, 25);
                sm.teraBlinkWarp(1, "WarpDrive", 480, 0, 0, 50, 50, 35, 20);
                sm.warpInstanceOut(chr, FieldConstants.HENESYS_ID);
                break;
        }
    }

    /** 參加 in the event list: the entry with unkVal == index -> {@link net.swordie.ms.world.event.EventListActions}
     * (handler registry by scriptName; event-fixes-1005 - the old switch keyed on display names that events.tw.json
     * had translated, so nothing matched). The 282 profile routes CP04AF to Tms282EventListHandlers. */
    @Handler(op = InHeader.USER_EVENT_LIST_OPERATION)
    public static void handleUserEventListOperation(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();
        int val = inPacket.decodeInt();
        int index = inPacket.decodeInt();
        var eventData = EventConstants.getEventByIndex(index);
        if (eventData != null) {
            net.swordie.ms.world.event.EventListActions.standard().join(chr, eventData);
        }
    }
}
