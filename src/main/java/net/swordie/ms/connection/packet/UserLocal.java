package net.swordie.ms.connection.packet;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.swordie.ms.client.character.BeautySalon;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.b2body.B2Body;
import net.swordie.ms.client.character.damage.DamageSkinType;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.daily.DailyCoin;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.anima.Ren;
import net.swordie.ms.client.jobs.cygnus.ThunderBreaker;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.EventConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.PsychicLock;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Familiar;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;

import java.util.*;

public class UserLocal {
    public static OutPacket noticeMsg(String msg, boolean autoSeparated) {
        OutPacket outPacket = new OutPacket(OutHeader.NOTICE_MSG);

        outPacket.encodeString(msg);
        outPacket.encodeByte(autoSeparated);

        return outPacket;
    }

    public static OutPacket chatMsg(ChatType colour, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.CHAT_MSG);

        outPacket.encodeShort(colour.getVal());
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket chatMsg2(ChatType colour, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.CHAT_MSG_2);

        outPacket.encodeShort(colour.getVal());
        outPacket.encodeString(msg);

        return outPacket;
    }

    public static OutPacket setUtilDlg(String msg, int npcID) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_UTIL_DLG);

        outPacket.encodeString(msg);
        outPacket.encodeInt(npcID);

        return outPacket;
    }

    public static OutPacket buffZoneEffect(int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.BUFFZONE_EFFECT);

        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket videoByScript(String videoPath, boolean isMuted) {
        OutPacket outPacket = new OutPacket(OutHeader.VIDEO_BY_SCRIPT);

        outPacket.encodeString(videoPath);
        outPacket.encodeByte(isMuted);

        return outPacket;
    }

    public static OutPacket videoByScript(String videoPath) {
        OutPacket outPacket = new OutPacket(OutHeader.VIDEO_BY_SCRIPT_2);

        outPacket.encodeString(videoPath);

        return outPacket;
    }

    public static OutPacket getRewardMobListResult(List<Integer> items) {
        OutPacket outPacket = new OutPacket(OutHeader.REWARD_MOB_LIST_RESULT);

        outPacket.encodeShort(items.size());
        if (items.size() < 100 && items.size() > 0) {
            for (int i = 0; i < items.size(); i++) {
                outPacket.encodeShort(items.size());
                for (Integer itemID : items) {
                    outPacket.encodeInt(itemID);
                }
            }
        }

        return outPacket;
    }

    public static OutPacket jaguarActive(boolean active) {
        OutPacket outPacket = new OutPacket(OutHeader.JAGUAR_ACTIVE);

        outPacket.encodeByte(active);

        return outPacket;
    }

    public static OutPacket SitTimeCapsule() {
        return new OutPacket(OutHeader.SIT_TIME_CAPSULE);
    }

    public static OutPacket jaguarSkill(int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.JAGUAR_SKILL);

        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket addPopupSay(int npcID, int duration, String message, String effect) {
        return addPopupSay(npcID, duration, message, effect, null);
    }

    public static OutPacket addPopupSay(int npcID, int duration, String message, String effect, AvatarLook avatarLook) {
        OutPacket outPacket = new OutPacket(OutHeader.ADD_POPUP_SAY);

        outPacket.encodeInt(npcID);
        outPacket.encodeInt(duration);
        outPacket.encodeString(message);
        outPacket.encodeString(effect);
        outPacket.encodeByte(false);
        outPacket.encodeInt(300);
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);
        outPacket.encodeByte(avatarLook != null);
        if (avatarLook != null) {
            avatarLook.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket setFieldFloating(int fieldID, int x, int y, int term) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_SET_FIELD_FLOATING);

        outPacket.encodeInt(fieldID);
        outPacket.encodeInt(x);
        outPacket.encodeInt(y);
        outPacket.encodeInt(term);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket incLarknessReponse(LarknessManager lm, boolean isDark) {
        OutPacket outPacket = new OutPacket(OutHeader.INC_LARKNESS_RESPONSE);

        outPacket.encodeInt(Math.min(lm.getGauge(), 10000));
        if (isDark) {
            outPacket.encodeByte(lm.getGauge() < 1 ? 1 : 2);
        } else {
            outPacket.encodeByte(lm.getGauge() >= 10000 ? 2 : 1);
        }

        return outPacket;
    }

    public static OutPacket detonateBomb() {
        return new OutPacket(OutHeader.DETONATE_BOMB);
    }

    public static OutPacket aggroRankInfoName(List<Map.Entry<Char, Long>> sortedDamageDone, long bossMaxHp) {
        OutPacket outPacket = new OutPacket(OutHeader.AGGRO_RANK_INFO_NAME);

        int size = sortedDamageDone.size();
        outPacket.encodeInt(size);
        for (int i = 0; i < size; i++) {
            Map.Entry<Char, Long> e = sortedDamageDone.get(i);
            String name = e.getKey().getName();
            if (name.length() > 10) name = name.substring(0, 10);
            int pct = GameConstants.percentInt(e.getValue(), bossMaxHp);
            outPacket.encodeString(name + "-" + pct + "%");
        }

        return outPacket;
    }

    public static OutPacket royalGuardAttack(boolean attack) {
        OutPacket outPacket = new OutPacket(OutHeader.ROYAL_GUARD_ATTACK);

        outPacket.encodeByte(attack);

        return outPacket;
    }

    public static OutPacket setOffStateForOffSkill(int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_OFF_STATE_FOR_OFF_SKILL);

        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket resetStateForOffSkill() {
        OutPacket outPacket = new OutPacket(OutHeader.RESET_STATE_FOR_OFF_SKILL);

        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket setSwordEnergy(int amount) {
        OutPacket outPacket = new OutPacket(OutHeader.HAYATO_SWORD_ENERGY);

        outPacket.encodeShort(amount);

        return outPacket;
    }

    public static OutPacket incJudgementStack(byte amount) {
        OutPacket outPacket = new OutPacket(OutHeader.INC_JUDGEMENT_STACK_RESPONSE);

        outPacket.encodeByte(false);
        outPacket.encodeByte(amount);

        return outPacket;
    }

    public static OutPacket changeStealMemoryResult(byte type, int stealManagerJobID, int position, int skillid, int stealSkillLv, int stealSkillMaxLv) {
        OutPacket outPacket = new OutPacket(OutHeader.CHANGE_STEAL_MEMORY_RESULT);
        StealMemoryType smType = StealMemoryType.getByVal(type);

        outPacket.encodeByte(1); //Set Excl Request
        outPacket.encodeByte(smType.getVal());    //Type

        switch (smType) {
            case STEAL_SKILL:
                outPacket.encodeInt(stealManagerJobID); //jobId  1~5 | 1 = 1stJob , 2 = 2ndJob ... ..
                outPacket.encodeInt(position); //impecMemSkillID // nPOS  0,1,2,3
                outPacket.encodeInt(skillid); //skill
                outPacket.encodeInt(stealSkillLv);   //StealSkill Lv
                outPacket.encodeInt(stealSkillMaxLv);   //StealSkill Max Lv
                break;
            case NO_TARGETS:
            case FAILED_UNK_REASON:
                break;
            case REMOVE_STEAL_MEMORY:
                outPacket.encodeInt(stealManagerJobID);
                outPacket.encodeInt(position);
                break;
            case REMOVE_MEMORY_ALL_SLOT:
                outPacket.encodeInt(skillid);
                break;
            case REMOVE_ALL_MEMORY:
                break;
        }

        return outPacket;
    }

    public static OutPacket resultSetStealSkill(boolean set, int impecMemSkilLID, int skillId) {
        OutPacket outPacket = new OutPacket(OutHeader.RESULT_SET_STEAL_SKILL);

        outPacket.encodeByte(1); //Set Excl Request
        outPacket.encodeByte(set); //bSet
        outPacket.encodeInt(impecMemSkilLID); //impecMemSkilLID
        if (set) {
            outPacket.encodeInt(skillId); //skill Id
        }
        return outPacket;
    }

    public static OutPacket resultStealSkillList(Collection<Skill> targetSkillsList, int phantomStealResult, int targetChrId, int targetJobId) {
        OutPacket outPacket = new OutPacket(OutHeader.RESULT_STEAL_SKILL_LIST);
        outPacket.encodeByte(1); //Set Excl Request
        outPacket.encodeInt(targetChrId);
        outPacket.encodeInt(phantomStealResult); //   Gets a check  if == 4,   else:   nPhantomStealWrongResult
        if (phantomStealResult == 4) {
            outPacket.encodeInt(targetJobId);
            outPacket.encodeInt(targetSkillsList.size());

            for (Skill skills : targetSkillsList) {
                // if v9 (index??) > 0
                outPacket.encodeInt(skills.getSkillId());
            }
        }

        return outPacket;
    }

    public static OutPacket setFuncKeyByScript(boolean add, int action, int key) {
        OutPacket outPacket = new OutPacket(OutHeader.FUNCKEY_SET_BY_SCRIPT);

        outPacket.encodeByte(add);
        outPacket.encodeInt(action);
        outPacket.encodeInt(key);

        return outPacket;
    }

    public static OutPacket userRenameResult(CharRenameType req, int CharID) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_RENAME_RESULT);

        outPacket.encodeByte(req.getVal());
        if (req.getVal() == 9) {
            outPacket.encodeInt(CharID);
        }

        return outPacket;
    }

    public static OutPacket setCharacterInfoBackground(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_CHARACTER_INFO_BACKGROUND);

        outPacket.encodeInt(type);

        return outPacket;
    }

    public static OutPacket setCharacterSelectionBackground(int type, int type2) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_CHARACTER_SELECTION_BACKGROUND);

        outPacket.encodeInt(type);
        outPacket.encodeInt(type2);

        return outPacket;
    }

    public static OutPacket damageSkinSaveResult(DamageSkinType req, DamageSkinType res, Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.DAMAGE_SKIN_SAVE_RESULT);

        outPacket.encodeByte(req.getVal());
        switch (req) {
            case DamageSkinType.Req_Reg:
            case DamageSkinType.Req_Remove:
            case DamageSkinType.Req_Active:
            case DamageSkinType.Req_SendInfo:
            case DamageSkinType.Res_Fail_Unknown:
            case DamageSkinType.Res_Fail_SlotCount:
            case DamageSkinType.Res_Fail_AlreadyExist:
                outPacket.encodeByte(res.getVal());
                break;
            case DamageSkinType.Res_Success:
                outPacket.encodeInt(GameConstants.DAMAGE_SKIN_MAX_SIZE);
                chr.encodeDamageSkins(outPacket);
                break;
        }

        return outPacket;
    }

    public static OutPacket maplePLannerUpdate(int mode) {
        OutPacket outPacket = new OutPacket(OutHeader.MAPLE_PLANNER_UPDATE);

        outPacket.encodeInt(mode); // MapleScheduler Request
        outPacket.encodeInt(0); // Result
        // "MapleScheduler Request : %d / Result : %d"

        return outPacket;
    }

    public static OutPacket timeBombAttack(int skillID, boolean isTimeBombAttack, int invincible, int userImpactDeg, int damage) {
        OutPacket outPacket = new OutPacket(OutHeader.TIME_BOMB_ATTACK);

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(isTimeBombAttack ? 1 : 0);
        outPacket.encodeInt(invincible);
        outPacket.encodeInt(userImpactDeg);
        outPacket.encodeInt(damage);

        return outPacket;
    }

    public static OutPacket explosionAttack(int skillID, Position position, int mobID, int count) {
        OutPacket outPacket = new OutPacket(OutHeader.EXPLOSION_ATTACK);

        outPacket.encodeInt(skillID);
        outPacket.encodePositionInt(position);
        outPacket.encodeInt(mobID);
        outPacket.encodeInt(count);

        return outPacket;
    }

    public static OutPacket passiveMove(MovementInfo movementInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.PASSIVE_MOVE);

        movementInfo.encode(outPacket);

        return outPacket;
    }

    public static OutPacket setNextShootExJablin() {
        return new OutPacket(OutHeader.SET_NEXT_SHOOT_EX_JABLIN);
    }

    public static OutPacket cadenaVoidStrikeRequest(Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.CADENA_VOID_STRIKE_REQUEST);

        outPacket.encodePositionInt(position);

        return outPacket;
    }

    public static OutPacket fallingLeaves(Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.FALLING_LEAVES);

        outPacket.encodePositionInt(position);

        return outPacket;
    }

    public static OutPacket zodiacBurst(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.ZODIAC_BURST);

        outPacket.encodeInt(type);

        return outPacket;
    }

    public static OutPacket Novilityshiled(int shiled) {
        OutPacket outPacket = new OutPacket(OutHeader.NOVILITY_SHILED);

        outPacket.encodeInt(shiled);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket randomTeleportKey(int randomTeleportKey) {
        OutPacket outPacket = new OutPacket(OutHeader.RANDOM_TELEPORT_KEY);

        outPacket.encodeByte(randomTeleportKey);

        return outPacket;
    }

    public static OutPacket sendClientResolution() {
        return new OutPacket(OutHeader.SEND_CLIENT_RESOLUTION);
    }

    public static OutPacket userBonusAttackRequest(int skillId) {
        return userBonusAttackRequest(skillId, new ArrayList<>());
    }

    public static OutPacket userBonusAttackRequest(int skillId, int mobID) {
        List<Integer> mobList = new ArrayList<>();
        mobList.add(mobID);
        return userBonusAttackRequest(skillId, mobList);
    }

    public static OutPacket userBonusAttackRequest(int skillId, List<Mob> mobs) {
        List<Integer> mobList = new ArrayList<>();
        for (Mob m : mobs) {
            mobList.add(m.getObjectId());
        }
        return userBonusAttackRequest(skillId, mobList);
    }

    public static OutPacket userBonusAttackRequest(int skillId, List<Integer> mobObjIdList, Object... args) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_BONUS_ATTACK_REQUEST);

        outPacket.encodeInt(skillId);
        outPacket.encodeInt(mobObjIdList.size()); // mobs hit
        outPacket.encodeByte(mobObjIdList.size() <= 0); // true ? placeOnChrPosition : placeOnMobPosition
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        for (int mobObjId : mobObjIdList) {
            outPacket.encodeInt(mobObjId);
            outPacket.encodeInt(skillId == ThunderBreaker.LIGHTNING_SPEAR_MULTISTRIKE_THUNDERBOLT_2
                    || skillId == ThunderBreaker.LIGHTNING_SPEAR_MULTISTRIKE_THUNDERBOLT_1 ? 134 : 0);
            if (skillId == Job.VENOM_BURST_ATTACK) { // Thief V - Venom Explosion
                outPacket.encodeInt(2);
            }
        }
        if (skillId == 35121019 || skillId == 35141002 || skillId == 63141502) {
            outPacket.encodeLong((long) args[0]);
        }
        if (skillId == ThunderBreaker.LIGHTNING_SPEAR_MULTISTRIKE_THUNDERBOLT_2
                || skillId == ThunderBreaker.LIGHTNING_SPEAR_MULTISTRIKE_THUNDERBOLT_1) {
            outPacket.encodeInt((int) args[0]);
            outPacket.encodeInt((int) args[1]);
            outPacket.encodeInt((int) args[2]);
            outPacket.encodeInt((int) args[3]);
            outPacket.encodeInt((int) args[4]);
            outPacket.encodeByte((byte) args[5]);
        }
        if (skillId == 400011133 || skillId == 4361501 || skillId == 31141502) {
            outPacket.encodeInt((int) args[0]);
        }
        if (skillId == 14141019) {
            outPacket.encodeInt((int) args[0]);
        }
        if (skillId == 51111015) {
            outPacket.encodeByte((byte) args[0]);
        }
        if (skillId == Ren.THOUSAND_BLOSSOM_FLURRY_EX) {
            outPacket.encodeInt((int) args[0]);
        }

        return outPacket;
    }

    public static OutPacket userRandAreaAttackRequest(Map<Position, Integer> mobAttackedList, int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_RAND_AREA_ATTACK_REQUEST);

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(mobAttackedList.size()); //# of mobs to attack
        mobAttackedList.forEach((key, value) -> {
            outPacket.encodePositionInt(key);
            outPacket.encodeInt(value);
        });
        if (skillID == RuneStone.LIBERATE_THE_RUNE_OF_THUNDER_ATTACK) {
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket userExtraAttackRequest(int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_EXTRA_ATTACK_REQUEST);

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket consumeItemCooltime() {
        return new OutPacket(OutHeader.CONSUME_ITEM_COOLTIME);
    }

    public static OutPacket screenAttack(int mobId, int skillId, int skillLevel, long damage) {
        OutPacket outPacket = new OutPacket(OutHeader.SCREEN_ATTACK);

        outPacket.encodeInt(mobId);
        outPacket.encodeInt(skillId);
        outPacket.encodeInt(skillLevel);
        outPacket.encodeLong(damage);

        return outPacket;
    }

    // Chaos Scroll
    public static OutPacket hyperEnchantScrollResult(int scrollID, Equip equip) {
        OutPacket outPacket = new OutPacket(OutHeader.HYPER_ENCHANT_SCROLL_RESULT);

        outPacket.encodeInt(scrollID);
        outPacket.encodeInt(equip.getItemId());
        // HyperEnchantStat::Decode
        TreeMap<EnchantStat, Integer> vals = equip.getHyperUpgradeStats();
        int mask = 0;
        for (EnchantStat es : vals.keySet()) {
            mask |= es.getVal();
        }
        outPacket.encodeInt(mask);
        vals.forEach((es, val) -> outPacket.encodeInt(val));

        return outPacket;
    }

    // KHI DUNG POTENTIAL/BONUS SCROLL
    public static OutPacket hyperEnchantScrollRegister(boolean success, int scrollID, int equipID) {
        OutPacket outPacket = new OutPacket(OutHeader.HYPER_ENCHANT_SCROLL_REGISTER);

        outPacket.encodeByte(success);
        outPacket.encodeInt(scrollID);
        outPacket.encodeInt(equipID);
        outPacket.encodeInt(success ? 1 : 0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket ItemSkillSocketUpdateItemUse(boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.ITEM_SKILL_SOCKET_UPDATE_RESULT);

        outPacket.encodeByte(success);

        return outPacket;
    }

    public static OutPacket ItemSkillOptionUpdateItemUse(boolean success, int equipID, int soulID) {
        OutPacket outPacket = new OutPacket(OutHeader.ITEM_SKILL_OPTION_UPDATE_RESULT);

        outPacket.encodeByte(success);
        outPacket.encodeByte(0);
        outPacket.encodeInt(equipID);
        outPacket.encodeInt(soulID);

        return outPacket;
    }

    // Stamp
    public static OutPacket itemSlotExtendItemUse(boolean success, int scrollID, int equipID) {
        OutPacket outPacket = new OutPacket(OutHeader.ITEM_SLOT_EXTEND_RESULT);

        outPacket.encodeByte(success);
        outPacket.encodeInt(scrollID);
        outPacket.encodeInt(0);
        outPacket.encodeInt(equipID);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket brightCubeResult(boolean success, int cubeID, int equipID) {
        OutPacket outPacket = new OutPacket(OutHeader.BRIGHT_CUBE_RESULT);

        outPacket.encodeByte(success);
        outPacket.encodeInt(cubeID);
        outPacket.encodeInt(0);
        outPacket.encodeInt(equipID);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket poolMakerRequest(int skillID, int remain, int coolTime) {
        OutPacket outPacket = new OutPacket(OutHeader.POOL_MAKER_REQUEST);

        outPacket.encodeByte(skillID != 0 ? true : false);
        if (skillID != 0) {
            outPacket.encodeInt(skillID);
            outPacket.encodeInt(remain);
            outPacket.encodeInt(coolTime);
        }

        return outPacket;
    }

    public static OutPacket comboCounter(StylishKillType type, int comboCount, int mobID) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(MessageType.STYLISH_KILL_MESSAGE.getVal());
        outPacket.encodeByte(type.getVal()); //1 for Combo   |  0 for MultiKill
        switch (type) {
            case MULTI_KILL: // MultiKill Pop-up
                outPacket.encodeLong(comboCount); // bonus
                outPacket.encodeInt(0); // crc
                outPacket.encodeInt(mobID); // count
                outPacket.encodeInt(EventConstants.COMBO_MULTI_KILL_TYPE); // "MultiKill" | comboAndMultiKillType
                break;
            case COMBO: // Combo Kill Message
                outPacket.encodeInt(comboCount); // count
                outPacket.encodeInt(mobID); // mobID
                outPacket.encodeInt(EventConstants.COMBO_MULTI_KILL_TYPE); // "ComboKill" | comboAndMultiKillType
                outPacket.encodeInt(0); // crc
                break;
        }

        return outPacket;
    }

    public static OutPacket collectionRecordMessage(int collectionIndex, String value) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(MessageType.COLLECTION_RECORD_MESSAGE.getVal());
        outPacket.encodeInt(collectionIndex);
        outPacket.encodeString(value);

        return outPacket;
    }

    public static OutPacket dailyGiftMessage(String value) {
        OutPacket outPacket = new OutPacket(OutHeader.MESSAGE);

        outPacket.encodeByte(MessageType.NX_RECORD_MESSAGE.getVal());
        outPacket.encodeInt(15);
        outPacket.encodeString(value);

        return outPacket;
    }

    public static OutPacket giantPetBuff(int type, int key) {
        OutPacket outPacket = new OutPacket(OutHeader.GIANT_PET_BUFF);

        outPacket.encodeInt(type);
        outPacket.encodeInt(key);

        return outPacket;
    }

    public static OutPacket setDead(boolean tremble, int unk) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_DEAD);

        outPacket.encodeByte(tremble);
        outPacket.encodeInt(unk);

        return outPacket;
    }

    public static OutPacket setHitMiss() {
        return new OutPacket(OutHeader.SET_HIT_MISS);
    }

    public static OutPacket openUIOnDead(Char chr, int flag, int reviveType) {
        OutPacket outPacket = new OutPacket(OutHeader.OPEN_UI_DEAD);

        outPacket.encodeInt(flag);
        outPacket.encodeInt(reviveType);
        outPacket.encodeInt(0);
        outPacket.encodeByte(reviveType == 3 ? 1 : 0);
        outPacket.encodeInt(reviveType == 3 ? 30 : 0);
        outPacket.encodeInt(reviveType == 3 ? 5 : 0);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket skillCooltimeSetM(int skillID, int cdMS) {
        Map<Integer, Integer> cds = new HashMap<>();
        cds.put(skillID, cdMS);
        return skillCooltimeSetM(cds);
    }

    public static OutPacket ItemMakerCooldown(int skillID, int cooldown) {
        OutPacket outPacket = new OutPacket(OutHeader.ITEMMAKER_COOLTIME_SET);

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(cooldown);

        return outPacket;
    }

    public static OutPacket skillCooltimeSetM(Map<Integer, Integer> cooldowns) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_COOLTIME_SET_M);

        outPacket.encodeInt(cooldowns.size());
        cooldowns.forEach((skillID, cooldown) -> {
            outPacket.encodeInt(skillID);
            outPacket.encodeInt(cooldown);
        });

        return outPacket;
    }

    public static OutPacket setBuffProtector(int itemID, boolean active) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_BUFF_PROTECTOR);

        outPacket.encodeInt(itemID);
        outPacket.encodeByte(active);

        return outPacket;
    }

    public static OutPacket deathCountInfo(int deathCount) {
        OutPacket outPacket = new OutPacket(OutHeader.DEATH_COUNT_INFO);

        outPacket.encodeInt(deathCount);

        return outPacket;
    }

    public static OutPacket deathCountInfo(int userID, int deathCount) {
        OutPacket outPacket = new OutPacket(OutHeader.INDIVIDUAL_DEATH_COUNT_INFO);

        outPacket.encodeInt(userID);
        outPacket.encodeInt(deathCount);

        return outPacket;
    }

    public static OutPacket familiarAddResult(Familiar familiar, boolean showInfoChanged, boolean adminMob) {
        OutPacket outPacket = new OutPacket(OutHeader.FAMILIAR_ADD_RESULT);

        outPacket.encodeLong(familiar.getId());
        familiar.encode(outPacket);
        outPacket.encodeByte(showInfoChanged);
        outPacket.encodeByte(adminMob);

        return outPacket;
    }

    public static OutPacket setDressChanged(boolean on, boolean dressInfinity) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_DRESS_CHANGED);

        outPacket.encodeByte(on);
        outPacket.encodeByte(dressInfinity);
        outPacket.encodeByte(1); // unknown a boolean

        return outPacket;
    }

    public static OutPacket serverAckMobZoneStateChange() {
        OutPacket outPacket = new OutPacket(OutHeader.SERVER_ACK_MOB_ZONE_STATE_CHANGE);

        outPacket.encodeByte(true);

        return outPacket;
    }

    public static OutPacket randomEmotion(int itemID) {
        OutPacket outPacket = new OutPacket(OutHeader.RANDOM_EMOTION);

        outPacket.encodeInt(itemID);

        return outPacket;
    }

    public static OutPacket setDirectionMode(boolean show, int unk) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_DIRECTION_MODE);

        outPacket.encodeByte(show);
        outPacket.encodeInt(unk);

        return outPacket;
    }

    public static OutPacket setInGameDirectionMode(boolean lockUI, boolean blackFrame, boolean forceMouseOver, boolean showUI) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_IN_GAME_DIRECTION_MODE);

        outPacket.encodeByte(lockUI); // Locks User's UI        - Is 'showUI' in IDA
        outPacket.encodeByte(blackFrame); // Usually 1 in gms? (@aviv)
        if (lockUI) {
            outPacket.encodeByte(forceMouseOver);
            outPacket.encodeByte(showUI); // showUI
        }

        return outPacket;
    }

    public static OutPacket inGameDirectionEvent(InGameDirectionEvent igdr) {
        OutPacket outPacket = new OutPacket(OutHeader.IN_GAME_DIRECTION_EVENT);

        outPacket.encode(igdr);

        return outPacket;
    }

    public static OutPacket hireTutor(boolean set) {
        OutPacket outPacket = new OutPacket(OutHeader.HIRE_TUTOR);

        outPacket.encodeByte(set);

        return outPacket;
    }

    public static OutPacket tutorMsg(int id, int duration) {
        OutPacket outPacket = new OutPacket(OutHeader.TUTOR_MSG);

        boolean automated = true;
        outPacket.encodeByte(automated);
        outPacket.encodeInt(id);
        outPacket.encodeInt(duration);

        return outPacket;
    }

    public static OutPacket tutorMsg(String message, int width, int duration) {
        OutPacket outPacket = new OutPacket(OutHeader.TUTOR_MSG);

        boolean automated = false;
        outPacket.encodeByte(automated);
        outPacket.encodeString(message);
        outPacket.encodeInt(width);
        outPacket.encodeInt(duration);

        return outPacket;
    }

    public static OutPacket emotion(int emotion, int duration, boolean byItemOption) {
        OutPacket outPacket = new OutPacket(OutHeader.EMOTION);

        outPacket.encodeInt(emotion);
        outPacket.encodeInt(duration);
        outPacket.encodeByte(byItemOption);

        return outPacket;
    }

    public static OutPacket androidEmotion(int emotion, int duration) {
        OutPacket outPacket = new OutPacket(OutHeader.EMOTION);

        outPacket.encodeInt(emotion);
        outPacket.encodeInt(duration);

        return outPacket;
    }

    public static OutPacket questResult(QuestType type, int questID, int npcTemplateID, int secondQuestID, boolean startNavigation) {
        OutPacket outPacket = new OutPacket(OutHeader.QUEST_RESULT);

        outPacket.encodeByte(type.getVal());
        outPacket.encodeInt(questID);
        outPacket.encodeInt(npcTemplateID);

        outPacket.encodeInt(secondQuestID); // starts a second quest
        outPacket.encodeByte(startNavigation);

        return outPacket;
    }

    public static OutPacket medalReissueResult(MedalReissueResultType medalReissueResultType, int itemId) {
        OutPacket outPacket = new OutPacket(OutHeader.MEDAL_REISSUE_RESULT);

        outPacket.encodeByte(medalReissueResultType.getVal());
        outPacket.encodeInt(itemId);

        return outPacket;
    }

    public static OutPacket isUniverse(boolean bool) {
        OutPacket outPacket = new OutPacket(OutHeader.IS_UNIVERSE);

        outPacket.encodeByte(bool);

        return outPacket;
    }

    public static OutPacket moveParticleEff(String type, Position startPoint, Position endPoint, int moveTime, int totalCount, int oneSprayMin, int oneSprayMax) {
        OutPacket outPacket = new OutPacket(OutHeader.MOVE_PARTICLE_EFF);

        outPacket.encodeString(type);
        outPacket.encodePosition(startPoint);
        outPacket.encodePosition(endPoint);
        outPacket.encodeShort(moveTime);
        outPacket.encodeShort(totalCount);
        outPacket.encodeShort(oneSprayMin);
        outPacket.encodeShort(oneSprayMax);

        return outPacket;
    }

    public static OutPacket balloonMsg(String message, int width, int timeOut, Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.BALLOON_MSG);

        outPacket.encodeString(message);
        outPacket.encodeShort(width);// 100
        outPacket.encodeShort(timeOut);// 3
        outPacket.encodeByte(position == null);
        if (position != null) {
            outPacket.encodePosition(position);
        }
        return outPacket;
    }

    public static OutPacket flameWizardElementFlameSummon() {
        return new OutPacket(OutHeader.FLAME_WIZARD_ELEMENT_FLAME_SUMMON);
    }

    public static OutPacket doActivePsychicArea(PsychicArea pa) {
        OutPacket outPacket = new OutPacket(OutHeader.DO_ACTIVE_PSYCHIC_AREA);

        outPacket.encodeInt(pa.localPsychicAreaKey);
        outPacket.encodeInt(pa.psychicAreaKey);

        return outPacket;
    }

    public static OutPacket enterFieldPsychicInfo(int userID, PsychicLock pl, List<PsychicArea> psychicAreas) {
        OutPacket outPacket = new OutPacket(OutHeader.ENTER_FIELD_PSYCHIC_INFO);

        outPacket.encodeByte(true);

        outPacket.encodeInt(userID);
        if (pl == null) {
            outPacket.encodeInt(0);
        } else {
            outPacket.encodeInt(pl.psychicLockBalls.size());
            for (PsychicLockBall plb : pl.psychicLockBalls) {
                boolean hasMob = plb.mob != null;
                outPacket.encodeByte(plb.success);
                outPacket.encodeInt(plb.localKey);
                outPacket.encodeInt(plb.psychicLockKey);
                outPacket.encodeInt(pl.skillID);
                outPacket.encodeShort(pl.slv);
                outPacket.encodeInt(hasMob ? plb.mob.getObjectId() : 0);
                outPacket.encodeShort(plb.stuffID);
                outPacket.encodeInt(hasMob ? Util.maxInt(plb.mob.getMaxHp()) : 0);
                outPacket.encodeInt(hasMob ? Util.maxInt(plb.mob.getHp()) : 0);
                outPacket.encodeByte(plb.posRelID);
                outPacket.encodePositionInt(plb.start);
                outPacket.encodePositionInt(plb.rel);
            }
        }
        if (psychicAreas == null) {
            outPacket.encodeInt(0);
        } else {
            outPacket.encodeInt(psychicAreas.size());
            for (PsychicArea pa : psychicAreas) {
                pa.encode(outPacket);
            }
        }
        // indicate end
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket registerTeleport(int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.REGISTER_TELEPORT);

        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket bonusAttackDelayRequest(Map<Integer, Integer> bonusAttackDelayMap) {
        OutPacket outPacket = new OutPacket(OutHeader.BONUS_ATTACK_DELAY_REQUEST);

        outPacket.encodeInt(bonusAttackDelayMap.size());
        for (Map.Entry<Integer, Integer> entry : bonusAttackDelayMap.entrySet()) {
            outPacket.encodeInt(entry.getKey());
            outPacket.encodeInt(entry.getValue());
        }

        return outPacket;
    }

    public static OutPacket areaExplosionRequest(int skillId, List<Rect> shardRects) {
        OutPacket outPacket = new OutPacket(OutHeader.AREA_EXPLOSION_REQUEST);

        outPacket.encodeInt(skillId);
        outPacket.encodeInt(0); // ?
        outPacket.encodeInt(shardRects.size());
        int itr = 1;
        for (Rect rect : shardRects) {
            outPacket.encodeInt(itr); // iterator (might need to sniff again, might be a new key/id everytime)
            outPacket.encodeRectInt(rect);
            itr++;
        }

        return outPacket;
    }

    public static OutPacket createAreaDotInfo(int id, int skillID, Rect rect) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_AREA_DOT_INFO);

        outPacket.encodeInt(id);
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(0); // 243
        outPacket.encodeRectInt(rect);

        return outPacket;
    }

    public static OutPacket inviteGroupChair(InviteGroupChairResult type) {
        OutPacket outPacket = new OutPacket(OutHeader.INVITE_GROUP_CHAIR);

        outPacket.encodeInt(type.getVal());
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket requireChair(int id) {
        OutPacket outPacket = new OutPacket(OutHeader.REQUIRE_CHAIR);

        outPacket.encodeInt(id);

        return outPacket;
    }

    public static OutPacket setSlowDown(int fallingSpeed, int fallingTime) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_SLOW_DOWN);

        outPacket.encodeShort(fallingSpeed);
        outPacket.encodeShort(fallingTime);

        return outPacket;
    }

    public static OutPacket registerExtraSkill(int mainSkilId, List<ExtraSkill> skills) {
        OutPacket outPacket = new OutPacket(OutHeader.REGISTER_EXTRA_SKILL);

        outPacket.encodeInt(mainSkilId);
        outPacket.encodeShort(skills.size());
        for (ExtraSkill skill : skills) {
            skill.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket cameraSwitchNormal(String targetName, int time) {
        OutPacket outPacket = new OutPacket(OutHeader.CAMERA_SWITCH);
        outPacket.encodeByte(CameraSwitchType.NORMAL.getVal());

        outPacket.encodeString(targetName);
        outPacket.encodeInt(time);
        return outPacket;
    }

    public static OutPacket cameraSwitchByPosition(Position position, int time) {
        OutPacket outPacket = new OutPacket(OutHeader.CAMERA_SWITCH);
        outPacket.encodeByte(CameraSwitchType.POSITION.getVal());

        outPacket.encodePositionInt(position);
        outPacket.encodeInt(time);
        return outPacket;
    }

    public static OutPacket cameraSwitchBack() {
        OutPacket outPacket = new OutPacket(OutHeader.CAMERA_SWITCH);
        outPacket.encodeByte(CameraSwitchType.BACK.getVal());
        return outPacket;
    }

    public static OutPacket cameraSwitchPosByCID(int cid, boolean setCamera, int resetTime, String name) {
        OutPacket outPacket = new OutPacket(OutHeader.CAMERA_SWITCH);
        outPacket.encodeByte(CameraSwitchType.POSITION_BY_CID.getVal());

        outPacket.encodeInt(cid);
        outPacket.encodeByte(setCamera);
        outPacket.encodeInt(resetTime);
        outPacket.encodeString(name);
        return outPacket;
    }

    public static OutPacket setPartner(boolean add, int npcID, int skillID, boolean hasScript) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_PARTNER);

        outPacket.encodeByte(add);
        outPacket.encodeInt(npcID);
        outPacket.encodeInt(skillID);
        outPacket.encodeByte(hasScript);

        return outPacket;
    }

    public static OutPacket gatherRequestResult(int lifeId, boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.GATHER_REQUEST_RESULT);

        outPacket.encodeInt(lifeId);
        outPacket.encodeInt(success ? 13 : 0);

        return outPacket;
    }

    public static OutPacket setGun() {
        OutPacket outPacket = new OutPacket(OutHeader.SET_GUN);

        outPacket.encodeString("shotgun");
        outPacket.encodeString("shotgun");
        outPacket.encodeInt(1);
        outPacket.encodeInt(200);
        outPacket.encodeRectInt(new Rect(-8, -8, 16, 16));

        return outPacket;
    }

    public static OutPacket setAmmo(int ammo) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_AMMO);

        outPacket.encodeInt(ammo);

        return outPacket;
    }

    public static OutPacket createGun() {
        return new OutPacket(OutHeader.CREATE_GUN);
    }

    public static OutPacket clearGun() {
        return new OutPacket(OutHeader.CLEAR_GUN);
    }

    public static OutPacket shootFPS() {
        OutPacket outPacket = new OutPacket(OutHeader.RESULT_SHOOT_ATTACK_IN_FPS_MODE);

        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket openUrl(String url) {
        OutPacket outPacket = new OutPacket(OutHeader.OPEN_URL);

        outPacket.encodeByte(0);
        outPacket.encodeByte(true);
        outPacket.encodeString(url);

        return outPacket;
    }

    public static OutPacket zeroCombatRecovery(byte slv, int amount) {
        OutPacket outPacket = new OutPacket(OutHeader.ZERO_COMBAT_RECOVERY);

        outPacket.encodeByte(slv);
        outPacket.encodeInt(amount);

        return outPacket;
    }

    public static OutPacket skillCoolTimeReduce(int timeReduce, List<Integer> skillsReduced) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_COOLTIME_REDUCE);

        outPacket.encodeInt(timeReduce);
        outPacket.encodeByte(skillsReduced.size());
        for (Integer skillID : skillsReduced) {
            outPacket.encodeInt(skillID);
        }

        return outPacket;
    }

    public static OutPacket isUniverse() {
        OutPacket outPacket = new OutPacket(OutHeader.IS_UNIVERSE);

        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket doLotteryUI(short Pos, int ItemID, boolean irrevocable, Integer[] rewardItems) {
        OutPacket outPacket = new OutPacket(OutHeader.DO_LOTTERY_UI);

        outPacket.encodeShort(Pos);
        outPacket.encodeInt(ItemID);
        outPacket.encodeByte(irrevocable);
        outPacket.encodeInt(rewardItems.length);
        for (Integer itemId : rewardItems) {
            outPacket.encodeInt(itemId);
        }

        return outPacket;
    }

    public static OutPacket rouletteStart(int fResult) {
        OutPacket outPacket = new OutPacket(OutHeader.ROULETTE_START);

        outPacket.encodeInt(fResult);

        return outPacket;
    }

    public static OutPacket shootObjectCreated(ShootObjectSkillInfo shootObjectSkillInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOOT_OBJECT_CREATED);

        outPacket.encodeByte(true);
        outPacket.encodeInt(shootObjectSkillInfo.getSkillId());
        outPacket.encodeInt(shootObjectSkillInfo.getSlv());
        outPacket.encodeInt(shootObjectSkillInfo.getShootObjects().size());
        for (ShootObject shootObject : shootObjectSkillInfo.getShootObjects()) {
            outPacket.encodeInt(shootObject.getId());
        }

        return outPacket;
    }

    public static OutPacket lightitngCascadeEffect(int attackSkillId, int skillId, int skillLevel) {
        OutPacket outPacket = new OutPacket(OutHeader.LIGHTING_CASCADE_EFFECT);

        outPacket.encodeInt(attackSkillId);
        outPacket.encodeInt(skillId);
        outPacket.encodeInt(skillLevel);
        if (skillId == 400051093) {
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket giantShadowSpearAttack(Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.GIANT_SHADOW_SPEAR_ATTACK);

        outPacket.encodePositionInt(position);

        return outPacket;
    }

    public static OutPacket designateBurningCharacter(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.DESIGNATE_BURNING_CHARACTER);

        // 0: You have designated this character as your Burning character!
        // 1: You cannot designate this character for Burning.
        // 2: You cannot Burn Characters on reboot world
        // 3: An error occurred. Unable to designate character for Burning
        outPacket.encodeByte(type);

        return outPacket;
    }

    public static OutPacket summonSequenceAttackInstructions(int summonSkillId, List<Integer> sequence) {
        OutPacket outPacket = new OutPacket(OutHeader.SUMMON_SEQUENCE_ATTACK_INSTRUCTIONS);

        outPacket.encodeInt(summonSkillId);

        outPacket.encodeInt(sequence.size());
        sequence.forEach(outPacket::encodeInt);

        return outPacket;
    }

    public static OutPacket setMonsterDebuffMark(SpecialStack specialStack) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MONSTER_DEBUFF_MARK);

        outPacket.encodeByte(specialStack != null);
        if (specialStack != null) {
            specialStack.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket attackMonsterDebuffMark(int skillID, int attackSkillID, int maxStack, List<Integer> targets) {
        OutPacket outPacket = new OutPacket(OutHeader.ATTACK_MONSTER_DEBUFF_MARK);

        outPacket.encodeInt(skillID);
        outPacket.encodeInt(maxStack);
        outPacket.encodeInt(0);
        outPacket.encodeInt(targets.size());
        for (var entry : targets) {
            outPacket.encodeInt(entry); // objectId
            outPacket.encodeInt(Util.getRandom(200, 300));
        }
        outPacket.encodeInt(attackSkillID);

        return outPacket;
    }

    public static OutPacket setMobAdvMark(SpecialStack specialStack) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MOB_ADVENTURER_MARK);

        outPacket.encodeInt(specialStack.originalSkillID);
        outPacket.encodeByte(!specialStack.targets.isEmpty());
        if (!specialStack.targets.isEmpty()) {
            outPacket.encodeInt(specialStack.targets.size());
            for (var entry : specialStack.targets.int2ObjectEntrySet()) {
                outPacket.encodeInt(entry.getIntKey()); // objectId
                outPacket.encodeInt(1);
                outPacket.encodeInt(0);
                outPacket.encodeInt(specialStack.duration); // duration
                outPacket.encodeInt(0); // time left
                outPacket.encodeInt(1);
                outPacket.encodeByte(0);
            }
        }

        return outPacket;
    }

    /** Marks with a stack per mob (entry: mob, stack, 0, duration ms, 0, 1, 0); an empty map clears the skill's mark. */
    public static OutPacket setMobAdvMark(int skillID, java.util.Map<Integer, Integer> stackByMob, int durationMs) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MOB_ADVENTURER_MARK);

        outPacket.encodeInt(skillID);
        outPacket.encodeByte(!stackByMob.isEmpty());
        if (!stackByMob.isEmpty()) {
            outPacket.encodeInt(stackByMob.size());
            for (var entry : stackByMob.entrySet()) {
                outPacket.encodeInt(entry.getKey());
                outPacket.encodeInt(entry.getValue());
                outPacket.encodeInt(0);
                outPacket.encodeInt(durationMs);
                outPacket.encodeInt(0);
                outPacket.encodeInt(1);
                outPacket.encodeByte(0);
            }
        }

        return outPacket;
    }

    public static OutPacket setMobAdvMark(int skillID, boolean isSet, int mobObjectID) {
        OutPacket outPacket = new OutPacket(OutHeader.SET_MOB_ADVENTURER_MARK);

        outPacket.encodeInt(skillID);
        outPacket.encodeByte(isSet);
        if (isSet) {
            outPacket.encodeInt(1);
            outPacket.encodeInt(mobObjectID); // objectId
            outPacket.encodeInt(1);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(659); // duration
            outPacket.encodeInt(1);
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket attackAdvMark(int originalSkillID, int skillID, Int2IntMap targets) {
        OutPacket outPacket = new OutPacket(OutHeader.ATTACK_ADVENTURER_MARK);

        outPacket.encodeInt(originalSkillID);
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(0);
        outPacket.encodeInt(targets.size());
        for (var entry : targets.int2IntEntrySet()) {
            outPacket.encodeInt(entry.getIntValue()); // objectId
            outPacket.encodeInt(entry.getIntValue()); // duration
        }

        return outPacket;
    }

    public static OutPacket attackAdvMark(int originalSkillID, int skillID, int mobObjectID) {
        OutPacket outPacket = new OutPacket(OutHeader.ATTACK_ADVENTURER_MARK);

        outPacket.encodeInt(originalSkillID);
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(0);
        outPacket.encodeInt(1);
        outPacket.encodeInt(mobObjectID);
        outPacket.encodeInt(659);

        return outPacket;
    }

    public static OutPacket unreliableMemory(int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.UNRELIABLE_MEMORY);

        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket inHumanSpeedAfterAttack(int type, List<Integer> mobs) {
        OutPacket outPacket = new OutPacket(OutHeader.INHUMAN_SPEED_AFTER_ATTACK);

        outPacket.encodeInt(type);
        if (type > 0) {
            outPacket.encodeInt(mobs.size());
            for (var mob : mobs) {
                outPacket.encodeInt(mob);
            }
        }

        return outPacket;
    }

    public static OutPacket grandOrbitalFlameResult(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.GRAND_ORBITAL_FLAME_RESULT);

        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket infernoSphereResult(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.INFERNO_SPHERE_RESULT);

        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket holyAdventResult() {
        return new OutPacket(OutHeader.HOLY_ADVENT_RESULT);
    }

    public static OutPacket foxGodFlashResult() {
        return new OutPacket(OutHeader.FOX_GOD_FLASH_RESULT);
    }

    public static OutPacket hexaElemetalKnightsResult() {
        return new OutPacket(OutHeader.HEXA_ELEMENTAL_KNIGHTS_RESULT);
    }

    public static OutPacket showHexaSkillEff(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.SHOW_HEXA_SKILL_EFF);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeInt(chr.getJob());
        outPacket.encodeByte(0);
        outPacket.encodeString(chr.getName());

        return outPacket;
    }

    public static OutPacket createRoute(int fieldID) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_ROUTE);

        outPacket.encodeInt(fieldID);

        return outPacket;
    }

    public static OutPacket specialSitResult(int itemId) {
        OutPacket outPacket = new OutPacket(OutHeader.SPECIAL_SIT_RESULT);

        outPacket.encodeInt(itemId);

        return outPacket;
    }

    public static OutPacket openSkillGuide() {
        return new OutPacket(OutHeader.OPEN_SKILL_GUIDE);
    }

    public static OutPacket makeDescOnUI(int mapID, int type, String str, String str2, int... args) {
        OutPacket outPacket = new OutPacket(OutHeader.MAKE_DESC_ON_UI);

        outPacket.encodeInt(mapID);
        outPacket.encodeInt(type);
        if (type == 3) {
            outPacket.encodeString(str);
            outPacket.encodeInt(args[0]);
            outPacket.encodeInt(args[1]);
            outPacket.encodeInt(args[2]);
            outPacket.encodeInt(args[3]);
            outPacket.encodeInt(args[4]);
        } else if (type == 2) {
            outPacket.encodeInt(args[0]);
        } else if (type == 1) {
            outPacket.encodeInt(args[0]);
            outPacket.encodeString(str);
            outPacket.encodeString(str2);
        } else if (type == 0) {
            outPacket.encodeString(str);
        }

        return outPacket;
    }

    public static OutPacket beautyDataResult(BeautySalon beautySalon) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_BEAUTY_DATA_RESULT);

        outPacket.encodeInt(-1374040409);
        outPacket.encodeByte(3);
        outPacket.encodeInt(1);
        outPacket.encodeByte(127);
        outPacket.encodeInt(100);
        int hairSize = beautySalon.getHairSize();
        int faceSize = beautySalon.getFaceSize();
        int skinSize = beautySalon.getSkinSize();
        outPacket.encodeInt(hairSize);
        outPacket.encodeInt(faceSize);
        outPacket.encodeInt(skinSize);
        outPacket.encodeByte(hairSize*2);
        for (int i = 0; i < hairSize; i++) {
            outPacket.encodeInt(beautySalon.getHairByIndex(i)); //Hair ID
            outPacket.encodeByte(0); //mixBaseHairColor
            outPacket.encodeByte(0); //mixAddHairColor
            outPacket.encodeByte(0); //mixHairBaseProb
        }
        outPacket.encodeByte(faceSize*2);
        for (int i = 0; i < faceSize; i++) {
            outPacket.encodeInt(beautySalon.getFaceByIndex(i)); //Face ID
        }
        outPacket.encodeByte(skinSize*2);
        for (int i = 0; i < skinSize; i++) {
            outPacket.encodeInt(beautySalon.getSkinByIndex(i)); //Skin ID
            outPacket.encodeInt(0);
        }

        return outPacket;
    }

    public static OutPacket saveHairFace(int type, int index1, int index2) {
        OutPacket outPacket = new OutPacket(OutHeader.USER_BEAUTY_DATA_RESULT);

        outPacket.encodeInt(775702118);
        outPacket.encodeByte((byte) type);
        switch (type) {
            case 7: // update face
                outPacket.encodeInt(1);
                outPacket.encodeInt(index1); // 20000
                outPacket.encodeInt(index2); // 55241
                break;
            case 11: // update hair
                outPacket.encodeInt(1);
                outPacket.encodeInt(index1); // 30000
                outPacket.encodeInt(index2); // 65147
                outPacket.encodeByte(0); //mixBaseHairColor
                outPacket.encodeByte(0); //mixAddHairColor
                outPacket.encodeByte(0); //mixHairBaseProb
                break;
        }

        return outPacket;
    }

    public static OutPacket greaterDarkServantSwapResult(Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.GREATER_DARK_SERVANT_SWAP_RESULT);

        outPacket.encodePositionInt(position);

        return outPacket;
    }

    public static OutPacket skillRequestRequest(int skillId) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_REQUEST_REQUEST);

        outPacket.encodeInt(skillId);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket aceInTheHoleFinisher(int skillId, int slv, Position position) {
        OutPacket outPacket = new OutPacket(OutHeader.ACE_IN_THE_HOLE_FINISHER);

        outPacket.encodeInt(skillId);
        outPacket.encodeInt(slv);
        outPacket.encodeInt(1);
        outPacket.encodePositionInt(position);

        return outPacket;
    }

    public static OutPacket tideOfBattle(int skillId) {
        OutPacket outPacket = new OutPacket(OutHeader.TIDE_OF_BATTLE);

        outPacket.encodeInt(skillId);

        return outPacket;
    }

    public static OutPacket dodgeSkillReady() {
        return new OutPacket(OutHeader.DODGE_SKILL_READY);
    }

    public static OutPacket b2BodyResult(short requestType, B2Body b2Body) {
        OutPacket outPacket = new OutPacket(OutHeader.B2_BODY_RESULT);

        Char chr = b2Body.getChr();

        outPacket.encodeShort(requestType);
        outPacket.encodeInt(b2Body.getB2BodyUnkId()); // owner Id
        outPacket.encodeInt(chr.getFieldID());
        short loopsize = 1;

        switch (requestType) {
            case 0:
                outPacket.encodeShort(loopsize); // loop size
                for (int i = 0; i < loopsize; i++) {
                    outPacket.encodeInt(b2Body.getBodyId());
                    outPacket.encodeByte(b2Body.getType());
                    outPacket.encodeByte(false); // redraw
                    outPacket.encodePosition(b2Body.getPosition());
                    if (b2Body.getType() == 5) {
                        outPacket.encodeShort(b2Body.getnRadius());
                        outPacket.encodeShort(b2Body.getfRadius());
                    } else if (b2Body.getType() == 6) {
                        outPacket.encodeInt(0); // unk
                    }
                    outPacket.encodeShort(b2Body.getDuration()); // 60
                    outPacket.encodeShort(b2Body.getScale()); // 30000
                    outPacket.encodeShort(0); // 0
                    outPacket.encodeShort(10); // 10
                    outPacket.encodeInt(b2Body.getSkillId()); // 36121002 | HYPOGRAM_FIELD_FORCE_FIELD
                    outPacket.encodeShort(b2Body.getSlv());
                    outPacket.encodeByte(chr.isLeft());
                }
                break;
            case 3:
                outPacket.encodeInt(chr.getId());
                outPacket.encodeInt(b2Body.getSkillId());
                outPacket.encodeInt(b2Body.getMaxSpeedX());
                outPacket.encodeInt(b2Body.getMaxSpeedY());
                outPacket.encodeInt(b2Body.getBodyId());
                break;
            case 4:
                outPacket.encodeShort(loopsize);
                for (int i = 0; i < loopsize; i++) {
                    outPacket.encodeByte(true); // redraw
                    outPacket.encodePosition(b2Body.getPosition());
                    outPacket.encodeInt(b2Body.getDuration());
                    outPacket.encodeShort(0); // unk
                    outPacket.encodeShort(0); // unk
                    outPacket.encodeShort(0); // unk
                    outPacket.encodeInt(b2Body.getSkillId());
                    outPacket.encodeByte(chr.isLeft());
                    outPacket.encodeInt(b2Body.getMaxSpeedX());
                    outPacket.encodeInt(b2Body.getMaxSpeedY());
                }
                break;
            case 5:
                outPacket.encodeInt(b2Body.getSkillId()); // mob Skill Id
                outPacket.encodeInt(b2Body.getSlv()); // mob Skill Lv
                break;
        }

        return outPacket;
    }

    public static OutPacket b2BodyResultNew(short requestType, B2Body b2Body) {
        OutPacket outPacket = new OutPacket(OutHeader.B2_BODY_RESULT_NEW);

        outPacket.encodeInt(requestType);
        outPacket.encodeInt(b2Body.getBodyId());

        return outPacket;
    }

    public static OutPacket stopNoMovementKeyDownSkillRequest(int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.STOP_NO_MOVEMENT_KEY_DOWN_SKILL_REQUEST);

        outPacket.encodeInt(skillID);

        return outPacket;
    }

    public static OutPacket skillUseResult(byte unk, int skillID) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_USE_RESULT);

        outPacket.encodeByte(unk);
        outPacket.encodeInt(skillID);
        outPacket.encodeInt(-1);

        return outPacket;
    }

    public static OutPacket portalTeleport(String name) {
        OutPacket outPacket = new OutPacket(OutHeader.PORTAL_TELEPORT);

        outPacket.encodeString(name);

        return outPacket;
    }

    public static OutPacket characterDataLog(boolean init, List<String> strings) {
        OutPacket outPacket = new OutPacket(OutHeader.CHARACTER_DATA_LOG);

        outPacket.encodeByte(init);
        if (init) {
            outPacket.encodeInt(0);
        } else {
            int index = 0;
            outPacket.encodeInt(strings.size());
            for (String str : strings) {
                outPacket.encodeString(str);
                outPacket.encodeInt(index);
                index++;
            }
        }

        return outPacket;
    }

    public static OutPacket increasePointEventGauge(DailyCoin dailyCoin, int increasePoint, int increaseCoin) {
        OutPacket outPacket = new OutPacket(OutHeader.INCREASE_POINT_EVENT_GAUGE);

        outPacket.encodeInt(dailyCoin.getPoint()); //Total Point
        outPacket.encodeInt(increasePoint); //Increase Point
        outPacket.encodeByte(dailyCoin.isLock()); //bLock
        outPacket.encodeInt(dailyCoin.getCoin()); //Total Coin
        outPacket.encodeInt(increaseCoin); //Increase Coin

        //region sub_272A570
        outPacket.encodeInt(0);
        //endregion

        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        // sub_2ADD5F0
        outPacket.encodeInt(0);

        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        outPacket.encodeString("");

        return outPacket;
    }
}
