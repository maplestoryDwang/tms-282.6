package net.swordie.ms.handlers;

import net.swordie.ms.connection.netty.PacketHandler;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolProfile;
import net.swordie.ms.handlers.dispatch.HandlerRegistry;
import net.swordie.ms.enums.BossPartyType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.item.*;
import net.swordie.ms.handlers.life.*;
import net.swordie.ms.handlers.script.QuestHandler;
import net.swordie.ms.handlers.script.ScriptHandler;
import net.swordie.ms.handlers.social.*;
import net.swordie.ms.handlers.ui.*;
import net.swordie.ms.handlers.user.*;

/**
 * The shared GMS 265 handlers (base layer of the dispatch). Dispatch goes through {@link HandlerRegistry}:
 * {@link #getHandler} answers from the base with the active protocol profile's overlay on top. Registering a
 * header twice throws while this class initializes.
 */
public class PacketProcessor {
    private static final HandlerRegistry.Builder BASE = HandlerRegistry.builder("gms265-base");

    static {
        reg(InHeader.CLIENT_GAME_EXIT_STAGE, (c, chr, in) -> handleGameExitStage(c, chr, in));
        reg(InHeader.USER_SKILL_STATE_ACK, (c, chr, in) -> acknowledgeClientStage(c, chr));
        // v265 script/exit UI: client blocks until EXCL_REQUEST (TMS268 GameExit -> dispose pattern)
        reg(InHeader.CLIENT_SCRIPT_UI_STATE, (c, chr, in) -> handleClientScriptUiState(c, chr, in));
        reg(InHeader.CLIENT_SCRIPT_UI_STATE_CONFIRM, (c, chr, in) -> handleClientScriptUiState(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_EQUIP_1, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_EQUIP_2, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_EQUIP_3, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_SKILL, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_USER, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_QUEST, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_INVENTORY, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_STAGE, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_FIELD, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_SUMMARY, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.CLIENT_EXIT_TELEMETRY_STAGE_END, (c, chr, in) -> handleClientExitTelemetryAck(c, chr, in));
        reg(InHeader.MOB_CRC_DATA_RESULT, (c, chr, in) -> handleMobCrcDataResult(c, chr, in));
        regSilentTelemetry(
                InHeader.CLIENT_HARDWARE_PROFILE,
                InHeader.CLIENT_HARDWARE_PROFILE_EX
        );
        reg(InHeader.AUCTION_HOUSE_REQUEST, (c, chr, in) -> AuctionHouseHandler.handleActionHouseRequest(c, in));
        reg(InHeader.USER_RENAME_RESULT, (c, chr, in) -> UserHandler.userRenameResult(c, in));
        reg(InHeader.USER_MOVE, (c, chr, in) -> UserHandler.handleUserMove(chr, in));
        reg(InHeader.USER_HIT, (c, chr, in) -> UserHandler.handleUserHit(c, in));
        reg(InHeader.USER_EMOTION, (c, chr, in) -> UserHandler.handleUserEmotion(chr, in));
        reg(InHeader.INVITE_CHAIR, (c, chr, in) -> UserHandler.handleInviteChair(chr, in));
        reg(InHeader.CHECK_PROCESS, (c, chr, in) -> UserHandler.handleCheckProcess(c, in));
        reg(InHeader.USER_RENAME_REQUEST, (c, chr, in) -> UserHandler.userRenameRequest(c, in));
        reg(InHeader.USER_FOLLOW_CHARACTER_REQUEST, (c, chr, in) -> UserHandler.handleUserFollowCharacterRequest(chr, in));
        reg(InHeader.USER_CHARACTER_INFO_SELF_REQUEST, (c, chr, in) -> UserHandler.handleUserCharacterInfoSelfRequest(chr, in));
        reg(InHeader.USER_CHARACTER_INFO_REQUEST, (c, chr, in) -> UserHandler.handleUserCharacterInfoRequest(chr, in));
        reg(InHeader.USER_SOUL_EFFECT_REQUEST, (c, chr, in) -> UserHandler.handleUserSoulEffectRequest(chr, in));
        reg(InHeader.USER_FOLLOW_CHARACTER_WITHDRAW, (c, chr, in) -> UserHandler.handleUserFollowCharacterWithdraw(chr, in));
        reg(InHeader.USER_SET_CUSTOMIZE_EFFECT, (c, chr, in) -> UserHandler.handleUserSetCustomizeEffect(chr, in));
        reg(InHeader.BATTLE_RECORD_ON_OFF_REQUEST, (c, chr, in) -> UserHandler.handleBattleRecordOnOffRequest(c, in));
        reg(InHeader.USER_SIT_REQUEST, (c, chr, in) -> UserHandler.handleUserSitRequest(chr, in));
        reg(InHeader.RUNE_STONE_USE_REQ, (c, chr, in) -> UserHandler.handleRuneStoneUseRequest(chr, in));
        reg(InHeader.BATTLE_RECORD_SKILL_DAMAGE_LOG, (c, chr, in) -> UserHandler.handleBattleRecordSkillDamageLog(chr, in));
        reg(InHeader.USER_EFFECT_LOCAL, (c, chr, in) -> UserHandler.handleUserEffectLocal(chr, in));
        reg(InHeader.MONSTER_BOOK_MOB_INFO, (c, chr, in) -> UserHandler.handleMonsterBookMobInfo(chr, in));
        reg(InHeader.SET_PASSENGER_RESULT, (c, chr, in) -> UserHandler.handleSetPassengerResult(chr, in));
        reg(InHeader.USER_CATCH_DEBUFF_COLLISION, (c, chr, in) -> UserHandler.handleUserCatchDebuffCollision(chr, in));
        reg(InHeader.GATHER_REQUEST, (c, chr, in) -> UserHandler.handleGatherRequest(chr, in));
        reg(InHeader.FUNC_KEY_MAPPED_MODIFIED, (c, chr, in) -> UserHandler.handleFuncKeyMappedModified(chr, in));
        reg(InHeader.USER_PORTABLE_CHAIR_SIT_REQUEST, (c, chr, in) -> UserHandler.handleUserPortableChairSitRequest(chr, in));
        reg(InHeader.USER_EMOTION_ITEM_USE_REQUEST, (c, chr, in) -> UserHandler.handleUserEmotionItemUseRequest(chr, in));
        reg(InHeader.RUNE_STONE_SKILL_REQ, (c, chr, in) -> UserHandler.handleRuneStoneSkillRequest(chr, in));
        reg(InHeader.MONSTER_COLLECTION_EXPLORE_REQ, (c, chr, in) -> UserHandler.handleMonsterCollectionExploreReq(chr, in));
        reg(InHeader.USER_DELETE_DAMAGE_SKIN_REQUEST, (c, chr, in) -> UserHandler.handleUserDeleteDamageSkinRequest(chr, in));
        reg(InHeader.COMMERCI_DEPART, (c, chr, in) -> UserHandler.handleCommerciDepart(chr, in));
        reg(InHeader.USER_AD_BOARD_CLOSE, (c, chr, in) -> UserHandler.handleUserADBoardClose(chr, in));
        reg(InHeader.USER_APPLY_DAMAGE_SKIN_REQUEST, (c, chr, in) -> UserHandler.handleUserApplyDamageSkinRequest(chr, in));
        reg(InHeader.TRY_REGISTER_TELEPORT, (c, chr, in) -> UserHandler.handleTryRegisterTeleport(chr, in));
        reg(InHeader.DEFENSE_GAME_REQUEST, (c, chr, in) -> UserHandler.handleDefenseGameRequest(chr, in));
        reg(InHeader.USER_BUFF_FAVORITE_REQUEST, (c, chr, in) -> UserHandler.handleBuffFavoriteRequest(chr, in));
        reg(InHeader.REQUEST_RECEIVE_STUFFS, (c, chr, in) -> UserHandler.handleRequestReceiveStuffs(chr, in));
        reg(InHeader.GATHER_END_NOTICE, (c, chr, in) -> UserHandler.handleGatherEndNotice(chr, in));
        reg(InHeader.USER_TOWER_CHAIR_SETTING, (c, chr, in) -> UserHandler.handleUserTowerChairSetting(chr, in));
        reg(InHeader.USER_CLAIM_REQUEST, (c, chr, in) -> UserHandler.handleUserClaimRequest(chr, in));
        reg(InHeader.MEMO_IN_GAME_REQUEST, (c, chr, in) -> UserHandler.handleMemoInGameRequest(chr, in));
        reg(InHeader.USER_BEAUTY_DATA_REQUEST, (c, chr, in) -> UserHandler.handleUserBeautyDataRequest(chr, in));
        reg(InHeader.USER_REVIVE_IN_BOSS_INSTANCE, (c, chr, in) -> UserHandler.handleUserReviveInBossInstance(chr, in));
        reg(InHeader.LEVEL_UP_GUIDE_REQUEST, (c, chr, in) -> UserHandler.handleLevelUpGuideRequest(chr, in));
        reg(InHeader.USER_SHOOT_ATTACK_IN_FPS, (c, chr, in) -> UserHandler.handleUserShootAttackInFPS(chr, in));
        reg(InHeader.USER_SAVE_DAMAGE_SKIN_REQUEST, (c, chr, in) -> UserHandler.handleUserSaveDamageSkinRequest(chr, in));
        reg(InHeader.REQUEST_EVENT_LIST, (c, chr, in) -> UserHandler.handleRequestEventList(chr, in));
        reg(InHeader.POLO_FRITO_TOWN_DEFENSE_REQUEST, (c, chr, in) -> UserHandler.handlePoloFritoTownDefenseRequest(chr, in));
        reg(InHeader.DIMENSION_MIRROR_REQUEST, (c, chr, in) -> UserHandler.dimensionMirrorRequest(chr, in));
        reg(InHeader.POLO_FRITO_BOUNTY_HUNTING_REQUEST, (c, chr, in) -> UserHandler.handlePoloFritoBountyHuntingRequest(chr, in));
        reg(InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_VALUE_SET, (c, chr, in) -> UserHandler.handleUserRequestCharacterPotentialSkillRandSetUi(chr, in));
        reg(InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_SET_UI, (c, chr, in) -> ItemHandler.handleUserRequestCharacterPotentialSkillRandSetUI(chr, in));
        reg(InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_CHANGE_PRESET, (c, chr, in) -> UserHandler.handleUserRequestCharacterPotentialSkillChangePreset(chr, in));
        reg(InHeader.POLO_FRITO_COURTSHIP_DANCE_RESULT, (c, chr, in) -> UserHandler.handlePoloFritoCourtShipDanceResult(chr, in));
        reg(InHeader.MONSTER_COLLECTION_COMPLETE_REWARD_REQ, (c, chr, in) -> UserHandler.handleMonsterCollectionCompleteRewardReq(chr, in));
        reg(InHeader.USER_GROWTH_HELPER_REQUEST, (c, chr, in) -> UserHandler.handleUserGrowthHelperRequest(chr, in));
        reg(InHeader.USER_CONTENTS_MAP_REQUEST, (c, chr, in) -> UserHandler.handleUserContentsMapRequest(chr, in));
        reg(InHeader.QUICK_MOVE_NAVIGATION_REQUEST, (c, chr, in) -> UserHandler.handleUserContentsMapRequest(chr, in));
        reg(InHeader.ACHIEVEMENT_REQUEST, (c, chr, in) -> UserHandler.handleAchievementRequest(chr, in));
        reg(InHeader.SPECIAL_GAME_EXIT, (c, chr, in) -> UserHandler.handleSpecialGameExit(c, in));
        reg(InHeader.DREAM_BREAKER_SKILL, (c, chr, in) -> UserHandler.handleDreamBreakerSkill(c, in));
        reg(InHeader.LUCID_ACTIVATE_STATUE_REQUEST, (c, chr, in) -> UserHandler.handleLucidActivateStatueRequest(chr, in));
        reg(InHeader.DAILY_GIFT_REQUEST, (c, chr, in) -> UserHandler.handleUserReceiveDailyGiftRequest(chr, in));
        reg(InHeader.USER_EVENT_GAUGE_GAIN_COIN_REQUEST, (c, chr, in) -> UserHandler.handleUserEventGaugeGainCoinRequest(chr, in));
        reg(InHeader.QUICK_PASS_REQUEST, (c, chr, in) -> UserHandler.handleQuickPassRequest(chr, in));
        reg(InHeader.USER_SKILL_SEQUENCE_SKILLS_REQUEST, (c, chr, in) -> UserHandler.handleUserSkillSequenceSkillsRequest(chr, in));
        reg(InHeader.USER_SKILL_SEQUENCE_BUFFS_REQUEST, (c, chr, in) -> UserHandler.handleUserSkillSequenceBuffsRequest(chr, in));
        reg(InHeader.USER_SKILL_ALARM_REQUEST, (c, chr, in) -> UserHandler.handleUserSkillAlarmRequest(chr, in));
        reg(InHeader.HEXA_MATRIX_OPREATION, (c, chr, in) -> UserHandler.handleHexaMatrixOperationRequest(chr, in));
        reg(InHeader.HEXA_ERDA_CONVERSION, (c, chr, in) -> UserHandler.handleErdaConversion(chr, in));
        reg(InHeader.USER_OPEN_QUICK_MOVE_REQUEST, (c, chr, in) -> UserHandler.handleUserOpenQuickMoveRequest(chr, in));
        reg(InHeader.USER_SECOND_ATOM_COMMAND_REQUEST, (c, chr, in) -> SecondAtomHandler.handleUserSecondAtomCommandRequest(chr, in));
        reg(InHeader.USER_SECOND_ATOM_REMOVE_REQUEST, (c, chr, in) -> SecondAtomHandler.handleUserSecondAtomRemoveRequest(chr, in));
        reg(InHeader.USER_SECOND_ATOM_COLLISION_REQUEST, (c, chr, in) -> SecondAtomHandler.handleUserSecondAtomCollisionRequest(chr, in));
        reg(InHeader.USER_FARVORITE_MAP_REQUEST, (c, chr, in) -> UserHandler.handleUserFarvoriteMapRequest(chr, in));
        reg(InHeader.EXTRA_SYSTEM_REQUEST, (c, chr, in) -> UserHandler.handleExtraSystemRequest(chr, in));
        reg(InHeader.MO_XUAN_STACK_REQUEST, (c, chr, in) -> UserHandler.handleMoXuanStackRequest(chr, in));
        reg(InHeader.USER_SAVE_NAVIGATION_REQUEST, (c, chr, in) -> UserHandler.handleUserSaveNavigationRequest(chr, in));
        reg(InHeader.USER_FILTER_MAP_OBJECTS_REQUEST, (c, chr, in) -> UserHandler.handleUserFilterMapObjectsRequest(chr, in));
        reg(InHeader.USER_MEDAL_MODIFIED, (c, chr, in) -> UserHandler.handleUserMedalModified(chr, in));
        reg(InHeader.USER_TITLE_MODIFIED, (c, chr, in) -> UserHandler.handleUserTitleModified(chr, in));
        reg(InHeader.USER_DECORATION_INVENTORY_MODIFIED, (c, chr, in) -> UserHandler.handleUserDecorationInventoryModified(chr, in));
        reg(InHeader.USER_CASH_EQUIPMENT_PRESET_MODIFIED, (c, chr, in) -> UserHandler.handleUserCashEquipmentPresetModified(chr, in));
        reg(InHeader.USER_EQUIPMENT_PRESET_MODIFIED, (c, chr, in) -> UserHandler.handleUserEquipmentPresetModified(chr, in));
        reg(InHeader.USER_CHARACTER_PRESET_MODIFIED, (c, chr, in) -> UserHandler.handleCharacterPresetModified(chr, in));
        reg(InHeader.USER_CHANGE_CUSTOMIZED_BACKGROUND_REQUEST, (c, chr, in) -> UserHandler.handleChangeCustomizedBackgroundRequest(chr, in));
        reg(InHeader.USER_QUICK_SLOT_MODIFIED, (c, chr, in) -> UserHandler.handleUserQuickSlotModified(chr, in));
        reg(InHeader.USER_QUICK_MOVE_SCRIPT, (c, chr, in) -> UserHandler.handleUserQuickMoveScript(chr, in));
        reg(InHeader.USER_NODE_STONE_REQUEST, (c, chr, in) -> UserHandler.handleUserNodeStoneInBulkRequest(chr, in));
        reg(InHeader.USER_MEDAL_HONOR_REQUEST, (c, chr, in) -> UserHandler.handleUserMedalOfHonorRequest(chr, in));
        reg(InHeader.USER_MAPLE_PLANNER_MODIFIED, (c, chr, in) -> UserHandler.handleUserMaplePlannerModified(chr, in));
        reg(InHeader.USER_EVENT_LIST_OPERATION, (c, chr, in) -> UserHandler.handleUserEventListOperation(chr, in));
        reg(InHeader.USER_OPEN_FAMILIAR_SHOP, (c, chr, in) -> UserHandler.handleUserOpenFamiliarShop(chr, in));
        reg(InHeader.USER_SPECIAL_EXIT, (c, chr, in) -> UserHandler.handleUserSpecialExit(chr, in));
        reg(InHeader.INHERITANCE_INFO_REQUEST, (c, chr, in) -> EgoEquipHandler.handleInheritanceInfoRequest(chr, in));
        reg(InHeader.EGO_EQUIP_TALK_REQUEST, (c, chr, in) -> EgoEquipHandler.handleEgoEquipTalkRequest(chr, in));
        reg(InHeader.EGO_EQUIP_GROWTH_REQUEST, (c, chr, in) -> EgoEquipHandler.handleEgoEquipGrowthRequest(chr, in));
        reg(InHeader.EGO_EQUIP_GAUGE_COMPLETE_RETURN, (c, chr, in) -> EgoEquipHandler.handleEgoEquipGaugeCompleteReturn(chr, in));
        reg(InHeader.EGO_EQUIP_CREATE_UPGRADE_ITEM, (c, chr, in) -> EgoEquipHandler.handleEgoEquipCreateUpgradeItem(chr, in));
        reg(InHeader.EGO_EQUIP_CREATE_UPGRADE_ITEM_COST_REQUEST, (c, chr, in) -> EgoEquipHandler.handleEgoEquipCheckUpdateItemCostRequest(chr, in));
        reg(InHeader.EGO_EQUIP_CHECK_UPDATE_ITEM_REQUEST, (c, chr, in) -> EgoEquipHandler.handleEgoEquipCheckUpdateItemRequest(chr, in));
        reg(InHeader.PARTY_INVITABLE_SET, (c, chr, in) -> PartyHandler.handlePartyInvitableSet(chr, in));
        reg(InHeader.PARTY_CANDIDATE_REQUEST, (c, chr, in) -> PartyHandler.handlePartyCandidateRequest(chr, in));
        reg(InHeader.PARTY_MEMBER_CANDIDATE_REQUEST, (c, chr, in) -> PartyHandler.handlePartyMemberCandidateRequest(chr, in));
        reg(InHeader.PARTY_REQUEST, (c, chr, in) -> PartyHandler.handlePartyRequest(chr, in));
        reg(InHeader.PARTY_RESULT, (c, chr, in) -> PartyHandler.handlePartyResult(chr, in));
        reg(InHeader.USER_GATHER_ITEM_REQUEST, (c, chr, in) -> InventoryHandler.handleUserGatherItemRequest(chr, in));
        reg(InHeader.USER_SORT_ITEM_REQUEST, (c, chr, in) -> InventoryHandler.handleUserSortItemRequest(chr, in));
        reg(InHeader.USER_CHANGE_SLOT_POSITION_REQUEST, (c, chr, in) -> InventoryHandler.handleUserChangeSlotPositionRequest(chr, in));
        reg(InHeader.USER_LOCK_SORT_ITEM_REQUEST, (c, chr, in) -> InventoryHandler.handleLockSortItemRequest(chr, in));
        reg(InHeader.MOB_EXPLOSION_START, (c, chr, in) -> MobHandler.handleMobExplosionStart(chr, in));
        reg(InHeader.MOB_ESCORT_COLLISION, (c, chr, in) -> MobHandler.handleMobEscortCollision(chr, in));
        reg(InHeader.MOB_SKILL_DELAY_END, (c, chr, in) -> MobHandler.handleMobSkillDelayEnd(chr, in));
        reg(InHeader.STIGMA_DELEVERY_REQUEST, (c, chr, in) -> MobHandler.handleStigmaDeliveryRequest(chr, in));
        reg(InHeader.DEMIAN_OBJECT_ERR__RECREATE, (c, chr, in) -> MobHandler.handleDemianObjectErrRecreate(chr, in));
        reg(InHeader.MOB_AREA_ATTACK_DISEASE, (c, chr, in) -> MobHandler.handleMobAreaAttackDisease(chr, in));
        reg(InHeader.MOB_UPDATE_FIXED_MOVE_DIR, (c, chr, in) -> MobHandler.handleMobUpdateFixedMoveDir(chr, in));
        reg(InHeader.MOB_CREATE_AFFECTED_AREA, (c, chr, in) -> MobHandler.handleMobCreateAffectedArea(chr, in));
        reg(InHeader.USER_DAZZLE_HIT, (c, chr, in) -> MobHandler.handleUserDazzleHit(chr, in));
        reg(InHeader.MOB_BLACK_HAND, (c, chr, in) -> MobHandler.handleBlackHand(chr, in));
        reg(InHeader.WILL_USE_MOON_GAUGE, (c, chr, in) -> MobHandler.handleWillUseMoonGauge(chr, in));
        reg(InHeader.WILL_BEHOLDER_EYES_HIT, (c, chr, in) -> MobHandler.handleWillBeholderEyesHit(chr, in));
        reg(InHeader.WILL_SPIDER_WEB_HIT, (c, chr, in) -> MobHandler.handleWillSpiderWebHit(chr, in));
        reg(InHeader.MOB_CREATE_FIRE_WALK, (c, chr, in) -> MobHandler.handleMobCreateFireWalk(chr, in));
        reg(InHeader.MOB_LIFTING_END, (c, chr, in) -> MobHandler.handleMobLiftingEnd(chr, in));
        reg(InHeader.MOB_REQUEST_ESCORT_INFO, (c, chr, in) -> MobHandler.handleMobRequestEscortInfo(chr, in));
        reg(InHeader.DEMIAN_OBJECT_NODE_END, (c, chr, in) -> MobHandler.handleDemianObjectNodeEnd(chr, in));
        reg(InHeader.MOB_DAMAGE_SHARE_INFO, (c, chr, in) -> MobHandler.handleMobDamageShareInfo(chr, in));
        reg(InHeader.MOB_MOVE, (c, chr, in) -> MobHandler.handleMobMove(chr, in));
        reg(InHeader.MOB_APPLY_CTRL, (c, chr, in) -> MobHandler.handleMobApplyCtrl(chr, in));
        reg(InHeader.USER_BAN_MAP_BY_MOB, (c, chr, in) -> MobHandler.handleBanMapByMob(chr, in));
        reg(InHeader.USER_REQUEST_CHANGE_MOB_ZONE_STATE, (c, chr, in) -> MobHandler.handleUserRequestChangeMobZoneState(chr, in));
        reg(InHeader.MOB_HIT_BY_MOB, (c, chr, in) -> MobHandler.handleMobHitByMob(chr, in));
        reg(InHeader.MOB_ATTACK_MOB, (c, chr, in) -> MobHandler.handleMobAttackMob(chr, in));
        reg(InHeader.MOB_SELF_DESTRUCT, (c, chr, in) -> MobHandler.handleMobSelfDestruct(chr, in));
        reg(InHeader.MOB_SELF_DESTRUCT_COLLISION_GROUP, (c, chr, in) -> MobHandler.handleMobSelfDestruct(chr, in));
        reg(InHeader.USER_STAT_CHANGE_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserStatChangeItemUseRequest(chr, in));
        reg(InHeader.USER_LOTTERY_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserLotteryItemUseRequest(chr, in));
        reg(InHeader.USER_KAISER_TRANSFORM_WING, (c, chr, in) -> ItemHandler.handleUserKaierTransformWing(chr, in));
        reg(InHeader.USER_SCRIPT_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserScriptItemUseRequest(chr, in));
        reg(InHeader.USER_PORTAL_SCROLL_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserPortalScrollUseRequest(chr, in));
        reg(InHeader.USER_ACTIVATE_EFFECT_ITEM, (c, chr, in) -> ItemHandler.handleUserActivateEffectItem(chr, in));
        reg(InHeader.USER_ACTIVATE_DAMAGE_SKIN, (c, chr, in) -> ItemHandler.handleUserActivateDamageSkin(chr, in));
        reg(InHeader.USER_RECIPE_OPEN_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserRecipeOpenItemUseRequest(chr, in));
        reg(InHeader.USER_SKILL_LEARN_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserLearnItemUseRequest(chr, in));
        reg(InHeader.USER_UPGRADE_TOMB_EFFECT, (c, chr, in) -> ItemHandler.handleUserUpgradeTombEffect(chr, in));
        reg(InHeader.USER_SKILL_RESET_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserSkillResetItemUseRequest(chr, in));
        reg(InHeader.USER_MOB_SUMMON_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserMobSummonItemUseRequest(chr, in));
        reg(InHeader.USER_UI_OPEN_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserOpenItemUseRequest(chr, in));
        reg(InHeader.USER_DAMAGE_SKIN_SAVE_REQUEST, (c, chr, in) -> ItemHandler.handleUserDamageSkinSaveRequest(chr, in));
        reg(InHeader.USER_KAISER_TRANSFORM_TAIL, (c, chr, in) -> ItemHandler.handleUserKaierTransformTail(chr, in));
        reg(InHeader.USER_DEFAULT_WING_ITEM, (c, chr, in) -> ItemHandler.handleUserDefaultWingItem(chr, in));
        reg(InHeader.USER_BRIDLE_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserBridleItemUseRequest(chr, in));
        reg(InHeader.USER_MONKEY_EFFECT_ITEM, (c, chr, in) -> ItemHandler.handleUserMonkeyEffectItem(chr, in));
        reg(InHeader.USER_ACTIVATE_NICK_ITEM, (c, chr, in) -> ItemHandler.handleUserActivateNickItem(chr, in));
        reg(InHeader.USER_WEAPON_TEMP_ITEM_OPTION_REQUEST, (c, chr, in) -> ItemHandler.handleUserWeaponTempItemOptionRequest(chr, in));
        reg(InHeader.USER_STAT_CHANGE_ITEM_CANCEL_REQUEST, (c, chr, in) -> ItemHandler.handleUserStatChangeItemCancelRequest(chr, in));
        reg(InHeader.USER_UI_ARCANE_SYMBOL_ENCHANCE_REQUEST, (c, chr, in) -> ItemHandler.handleUserArcaneSymbolEnchanceRequest(chr, in));
        reg(InHeader.USER_UI_SACRED_SYMBOL_ENCHANCE_REQUEST, (c, chr, in) -> ItemHandler.handleUserSacredSymbolEnchanceRequest(chr, in));
        reg(InHeader.USER_EQUIPMENT_ENCHANT_WITH_SINGLE_UI_REQUEST, (c, chr, in) -> ItemHandler.handleUserEquipmentEnchantWithSingleUIRequest(chr, in));
        reg(InHeader.USER_ITEM_SKILL_SOCKET_UPGRADE_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserItemSkillSocketUpdateItemUseRequest(chr, in));
        reg(InHeader.USER_KAISER_COLOR_CHANGE_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserKaiserColorChangeItemUseRequest(chr, in));
        reg(InHeader.USER_CHAR_SLOT_INC_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserCharSlotIncItemUseRequest(chr, in));
        reg(InHeader.USER_STAT_CHANGE_BY_PORTABLE_CHAIR_REQUEST, (c, chr, in) -> ItemHandler.handleUserStatChangePortalChairRequest(chr, in));
        reg(InHeader.USER_ACTIVATE_DAMAGE_SKIN_PREMIUM, (c, chr, in) -> ItemHandler.handleUserActivateDamageSkinPremium(chr, in));
        reg(InHeader.USER_CONSUME_CASH_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserConsumeCashItemUseRequest(chr, in));
        reg(InHeader.USER_ABILITY_CHANGE_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserAbilityChangeItemUseRequest(chr, in));
        reg(InHeader.USER_KARMA_CONSUME_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserKarmaConsumeItemUseRequest(chr, in));
        reg(InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_SET, (c, chr, in) -> ItemHandler.handleUserRequestCharacterPotentialSkillRandSet(chr, in));
        reg(InHeader.USER_ABILITY_RESET_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserAbilityResetItemUseRequest(chr, in));
        reg(InHeader.USER_CONSUME_HAIR_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserConsumeHairItemUseRequest(chr, in));
        reg(InHeader.USER_ITEM_SKILL_OPTION_UPGRADE_ITEM_USE_REQUEST, (c, chr, in) -> ItemHandler.handleUserItemSkillOptionUpdateItemUseRequest(chr, in));
        reg(InHeader.USER_RENT_ITEM_REQUEST, (c, chr, in) -> ItemHandler.handleUserRentItemRequest(chr, in));
        reg(InHeader.USER_RETURN_EFFECT_RESPONSE, (c, chr, in) -> ItemHandler.handleUserReturnEffectResponse(chr, in));
        reg(InHeader.FOX_MAN_MOVE, (c, chr, in) -> FoxManHandler.handleAndroidMove(chr, in));
        reg(InHeader.FOX_MAN_ACTION_SET_USE_REQUEST, (c, chr, in) -> FoxManHandler.handleFoxManActionSetUseRequest(chr, in));
        reg(InHeader.FRIEND_REQUEST, (c, chr, in) -> FriendHandler.handleFriendRequest(chr, in));
        reg(InHeader.USER_PET_FOOD_ITEM_USE_REQUEST, (c, chr, in) -> PetHandler.handleUserPetFoodItemUseRequest(chr, in));
        reg(InHeader.PET_STAT_CHANGE_ITEM_USE_REQUEST, (c, chr, in) -> PetHandler.handlePetStatChangeItemUseRequest(chr, in));
        reg(InHeader.PET_DROP_PICK_UP_REQUEST, (c, chr, in) -> PetHandler.handlePetDropPickUpRequest(chr, in));
        reg(InHeader.USER_ACTIVATE_PET_REQUEST, (c, chr, in) -> PetHandler.handleUserActivatePetRequest(chr, in));
        reg(InHeader.PET_INTERACTION_REQUEST, (c, chr, in) -> PetHandler.handlePetInteractionRequest(chr, in));
        reg(InHeader.PET_UPDATE_EXCEPTION_LIST_REQUEST, (c, chr, in) -> PetHandler.handlePetUpdateExceptionListRequest(chr, in));
        reg(InHeader.PET_OPEN_SHOP, (c, chr, in) -> PetHandler.handlePetOpenShop(chr, in));
        reg(InHeader.USER_CASH_PET_PICK_UP_ON_OFF_REQUEST, (c, chr, in) -> PetHandler.handleUserCashPetPickUpOnOffRequest(chr, in));
        reg(InHeader.USER_REGISTER_PET_AUTO_BUFF_REQUEST, (c, chr, in) -> PetHandler.handleUserRegisterPetAutoBuffRequest(chr, in));
        reg(InHeader.PET_ACTION, (c, chr, in) -> PetHandler.handlePetAction(chr, in));
        reg(InHeader.PET_MOVE, (c, chr, in) -> PetHandler.handlePetMove(chr, in));
        reg(InHeader.USER_MEDAL_REISSUE_REQUEST, (c, chr, in) -> QuestHandler.handleUserMedalReissueRequest(chr, in));
        reg(InHeader.USER_QUEST_REQUEST, (c, chr, in) -> QuestHandler.handleUserQuestRequest(chr, in));
        reg(InHeader.ELODIN_SMALL_BOTTLE_CHECK, (c, chr, in) -> QuestHandler.handleElodinSmallBottleCheck(chr, in));
        reg(InHeader.ELODIN_SMALL_BOTTLE_SETUP, (c, chr, in) -> QuestHandler.handleElodinSmallBottleSetup(chr, in));
        reg(InHeader.ELODIN_PURE_WATER, (c, chr, in) -> QuestHandler.handleElodinPureWater(chr, in));
        reg(InHeader.REWARD_MOB_LIST_RESULT, (c, chr, in) -> QuestHandler.handleRewardMobListResult(chr, in));
        reg(InHeader.UPDATE_MATRIX, (c, chr, in) -> MatrixHandler.handleUpdateMatrix(chr, in));
        reg(InHeader.NODE_STONE_CRAFTING, (c, chr, in) -> MatrixHandler.handleNodeStoneCrafting(chr, in));
        reg(InHeader.USER_MESO_EXCHANGE_REQUEST, (c, chr, in) -> MesoMarketHandler.handleUserMesoExchangeRequest(chr, in));
        reg(InHeader.DEC_PSYCHIC_POINT_REQUEST, (c, chr, in) -> SkillHandler.handleDecPsychicPointRequest(chr, in));
        reg(InHeader.USER_KEY_DOWN_AREA_MOVING, (c, chr, in) -> SkillHandler.handleKeyDownAreaMoving(chr, in));
        reg(InHeader.USER_SKILL_CANCEL_REQUEST, (c, chr, in) -> SkillHandler.handleTemporaryStatResetRequest(chr, in));
        reg(InHeader.USER_DEBUFF_OBJ_COLLISION, (c, chr, in) -> SkillHandler.handleDebuffObjCollision(chr, in));
        reg(InHeader.USER_FINAL_ATTACK_REQUEST, (c, chr, in) -> SkillHandler.handleUserFinalAttackRequest(chr, in));
        reg(InHeader.USER_PINKBEAN_YO_YO_STACK_REQUEST, (c, chr, in) -> SkillHandler.handleUserPinkBeanYoYoStack(chr, in));
        reg(InHeader.MAKING_SKILL_REQUEST, (c, chr, in) -> SkillHandler.handleMakingSkillRequest(chr, in));
        reg(InHeader.USER_CREATE_AURA_BY_GRENADE, (c, chr, in) -> SkillHandler.handleUserCreateAuraByGrenade(chr, in));
        reg(InHeader.LINK_SKILL_REQUEST, (c, chr, in) -> SkillHandler.handleLinkSkillRequest(chr, in));
        reg(InHeader.USER_CREATE_AREA_DOT_REQUEST, (c, chr, in) -> SkillHandler.handleUserCreateAreaDoTRequest(chr, in));
        reg(InHeader.USER_SKILL_USE_REQUEST, (c, chr, in) -> SkillHandler.handleUserSkillUseRequest(chr, in));
        reg(InHeader.RETURN_TELEPORT_DEBUFF, (c, chr, in) -> SkillHandler.handleReturnTeleportDebuff(chr, in));
        reg(InHeader.USER_UPDATE_LAPIDIFICATION, (c, chr, in) -> SkillHandler.handleUpdateLapidification(chr, in));
        reg(InHeader.USER_CREATE_LUCK_OF_THE_DRAW_FORCE_ATOM, (c, chr, in) -> SkillHandler.handleUserCreateLuckOfTheDrawForceAtom(chr, in));
        reg(InHeader.USER_UPDATE_BENEDICTION_AREA, (c, chr, in) -> SkillHandler.handleUserUpdateBenedictionArea(chr, in));
        reg(InHeader.USER_B2_BODY_REQUEST, (c, chr, in) -> SkillHandler.handleB2BodyRequest(chr, in));
        reg(InHeader.USER_SKILL_KEY_DOWN_ATTACK_REQUEST, (c, chr, in) -> SkillHandler.handleUserKeyDownAttackRequest(chr, in));
        reg(InHeader.CHOOSE_LOAD_DICE_NUMBER_REQUEST, (c, chr, in) -> SkillHandler.handleChooseLoadDiceNumberRequest(chr, in));
        reg(InHeader.USER_THROW_GRENADE, (c, chr, in) -> SkillHandler.handleUserThrowGrenade(chr, in));
        reg(InHeader.USER_FORCE_ATOM_COLLISION, (c, chr, in) -> SkillHandler.handleForceAtomCollision(chr, in));
        reg(InHeader.USER_SET_MOVE_GRENADE, (c, chr, in) -> SkillHandler.handleUserSetMoveGrenade(chr, in));
        reg(InHeader.USER_DESTROY_GRENADE, (c, chr, in) -> SkillHandler.handleUserDestroyGrenade(chr, in));
        reg(InHeader.USER_SKILL_PREPARE_STOP, (c, chr, in) -> SkillHandler.handleUserSkillPrepareStop(chr, in));
        reg(InHeader.USER_SKILL_PREPARE_START, (c, chr, in) -> SkillHandler.handleUserSkillPrepareStart(chr, in));
        reg(InHeader.USER_SKILL_MOVING_SHOOT_ATTACK_PREPARE_REQUEST, (c, chr, in) -> SkillHandler.handleUsermovingShootAttackPrepareRequest(chr, in));
        reg(InHeader.USER_CHECK_TEMPORARY_STAT_DURATION_REQUEST, (c, chr, in) -> SkillHandler.handleUserCheckTemporaryStatDurationRequest(chr, in));
        reg(InHeader.SHOOT_OBJECT_INCREMENT_REQUEST, (c, chr, in) -> SkillHandler.handleShootObjectIncrementRequest(chr, in));
        reg(InHeader.DEMONIC_BLAST_KEYDOWN_COST, (c, chr, in) -> SkillHandler.handleDemonicBlastKeydownCost(chr, in));
        reg(InHeader.CLIENT_SYNC_COOLTIME_REQUEST, (c, chr, in) -> SkillHandler.handleClientSyncCooltimeRequest(chr, in));
        reg(InHeader.V_BLESSING_UPDATE_REQUEST, (c, chr, in) -> SkillHandler.handleVBlessingUpdateRequest(chr, in));
        reg(InHeader.AURA_SKILL_UPDATE_REQUEST, (c, chr, in) -> SkillHandler.handleBuffIncStackRequest(chr, in));
        reg(InHeader.JUPITER_THUNDER_CREATE_REQUEST, (c, chr, in) -> SkillHandler.handleJupiterThunderCreateRequest(chr, in));
        reg(InHeader.JUPITER_THUNDER_UPDATE_REQUEST, (c, chr, in) -> SkillHandler.handleJupiterThunderUpdateRequest(chr, in));
        reg(InHeader.JUPITER_THUNDER_REMOVE_REQUEST, (c, chr, in) -> SkillHandler.handleJupiterThunderRemoveRequest(chr, in));
        reg(InHeader.FINAL_PACT_REQUEST, (c, chr, in) -> SkillHandler.handleFinalPactRequest(chr, in));
        reg(InHeader.DIMENSIONAL_SWORD_CHANGE, (c, chr, in) -> SkillHandler.handleDimensionalSwordChange(chr, in));
        reg(InHeader.GREATER_DARK_SERVANT_SWAP_REQUEST, (c, chr, in) -> SkillHandler.handleGreaterDarkServantSwapRequest(chr, in));
        reg(InHeader.INHUMAN_SPEED_FORCE_ATOM_REQUEST, (c, chr, in) -> SkillHandler.handleInhumanSpeedForceAtomRequest(chr, in));
        reg(InHeader.SPOTLIGHT_STACK_REQUEST, (c, chr, in) -> SkillHandler.handleSpotlightStackRequest(chr, in));
        reg(InHeader.CRYSTAL_SKILL_REQUEST, (c, chr, in) -> SkillHandler.handleCrystalSkillRequest(chr, in));
        reg(InHeader.UPDATE_ARK_SPECTRA_ENERGY, (c, chr, in) -> SkillHandler.handleUpdateArkSpectraEnergy(chr, in));
        reg(InHeader.USER_CHARGE_WIND_ENERGY_REQUEST, (c, chr, in) -> SkillHandler.handleUserWAChargeWindEnergyRequest(chr, in));
        reg(InHeader.INCREASE_SKILL_STACK_REQUEST, (c, chr, in) -> SkillHandler.updateSolusRequest(chr, in));
        reg(InHeader.HEXA_SOL_JANUS_REQUEST, (c, chr, in) -> SkillHandler.handleHexaSolJanusRequest(chr, in));
        reg(InHeader.HEXA_FATE_SHUFFLE_REQUEST, (c, chr, in) -> SkillHandler.handleFateShuffleRequest(chr, in));
        reg(InHeader.REN_UNITY_REQUEST, (c, chr, in) -> SkillHandler.handleRenUnityRequest(chr, in));
        reg(InHeader.BROADCAST_EFFECT_TO_SPLIT, (c, chr, in) -> FieldHandler.handleBroadcastEffectToSplit(chr, in));
        reg(InHeader.BROADCAST_ONE_TIME_ACTION_TO_SPLIT, (c, chr, in) -> FieldHandler.handleBroadcastOneTimeActionToSplit(chr, in));
        reg(InHeader.BROADCAST_AFFECTED_EFFECT_TO_SPLIT, (c, chr, in) -> FieldHandler.handleBroadcastAffectedEffectToSplit(chr, in));
        reg(InHeader.USER_AFFECTED_AREA_REMOVE_BY_TIME, (c, chr, in) -> AffectedAreaHandler.handleUserAffectedAreaRemoveByTime(chr, in));
        reg(InHeader.SKILL_PET_MOVE, (c, chr, in) -> SkillPetHandler.handleSkillPetMove(chr, in));
        reg(InHeader.SKILL_PET_DROP_PICK_UP_REQUEST, (c, chr, in) -> SkillPetHandler.handleSkillPetDropPickUpRequest(chr, in));
        reg(InHeader.SKILL_PET_ACTION, (c, chr, in) -> SkillPetHandler.handleSkillPetAction(chr, in));
        reg(InHeader.USER_ITEM_OPTION_UPGRADE_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserItemOptionUpgradeItemUseRequest(chr, in));
        reg(InHeader.USER_ITEM_SLOT_EXTEND_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserItemSlotExtendItemUseRequest(chr, in));
        reg(InHeader.USER_MIRACLE_CIRCULATOR_OPTION_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserMiracleCirculatorOptionRequest(chr, in));
        reg(InHeader.USER_TOADS_HAMMER_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserToadsHammerRequest(chr, in));
        reg(InHeader.USER_TOADS_HAMMER_HELP_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserToadsHammerHelpRequest(chr, in));
        reg(InHeader.USER_ADDITIONAL_SLOT_EXTEND_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserAdditionalSlotExtendItemUseRequest(chr, in));
        reg(InHeader.USER_HYPER_UPGRADE_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserHyperUpgradeItemUseRequest(chr, in));
        reg(InHeader.USER_UPGRADE_ASSIST_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserUpgradeAssistItemUseRequest(chr, in));
        reg(InHeader.USER_MEMORIAL_EX_UPGRADE_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleMemorialExUpgradeItemUseRequest(chr, in));
        reg(InHeader.USER_MEMORIAL_EX_UPGRADE_ITEM_OPTION_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleMemorialExUpgradeItemOptionRequest(chr, in));
        reg(InHeader.USER_ADDITIONAL_OPT_UPGRADE_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserAdditionalOptUpgradeItemUseRequest(chr, in));
        reg(InHeader.USER_LUCKY_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserLuckyItemUseRequest(chr, in));
        reg(InHeader.GOLD_HAMMER_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleGoldHammerRequest(chr, in));
        reg(InHeader.USER_ITEM_RELEASE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserItemReleaseRequest(chr, in));
        reg(InHeader.USER_REQUEST_JEWEL_CRAFT, (c, chr, in) -> ItemUpgradeHandler.handleUserRequestJewelCraft(chr, in));
        reg(InHeader.USER_UPGRADE_ITEM_USE_REQUEST, (c, chr, in) -> ItemUpgradeHandler.handleUserUpgradeItemUseRequest(chr, in));
        reg(InHeader.GOLD_HAMMER_COMPLETE, (c, chr, in) -> ItemUpgradeHandler.handleGoldHammerComplete(chr, in));
        reg(InHeader.BOSS_UI_REQUEST, (c, chr, in) -> ScriptHandler.handleBossUIRequest(chr, in));
        reg(InHeader.MONSTER_PARK_UI_REQUEST, (c, chr, in) -> ScriptHandler.handleMonsterParkUIRequest(chr, in));
        reg(InHeader.CHECK_BOSS_PARTY_BY_SCRIPT, (c, chr, in) -> ScriptHandler.handleCheckBossPartyByScript(chr, in));
        reg(InHeader.LIBRARY_START_SCRIPT, (c, chr, in) -> ScriptHandler.handleLibraryStartScript(chr, in));
        reg(InHeader.USER_SCRIPT_MESSAGE_ANSWER, (c, chr, in) -> ScriptHandler.handleUserScriptMessageAnswer(chr, in));
        reg(InHeader.USER_RUN_SCRIPT, (c, chr, in) -> ScriptHandler.handleUserRunScript(chr, in));
        reg(InHeader.USER_UI_HELPER, (c, chr, in) -> ScriptHandler.handleUserUIHelper(chr, in));
        reg(InHeader.ENTER_RANDOM_PORTAL_REQUEST, (c, chr, in) -> ScriptHandler.handleEnterRandomPortalRequest(chr, in));
        reg(InHeader.DIRECTION_NODE_COLLISION, (c, chr, in) -> ScriptHandler.handleDirectionNodeCollision(chr, in));
        reg(InHeader.USER_SKIP_TERA_BLINK, (c, chr, in) -> ScriptHandler.handleUserSkipTeraBlinkRequest(chr, in));
        reg(InHeader.SUMMONED_ACTION, (c, chr, in) -> SummonedHandler.handleSummonedAction(chr, in));
        reg(InHeader.SUMMONED_SKILL, (c, chr, in) -> SummonedHandler.handleSummonedSkill(chr, in));
        reg(InHeader.SUMMONED_CREATE_SHOOT_OBJ, (c, chr, in) -> SummonedHandler.handleSummonedCreateShootOBj(chr, in));
        reg(InHeader.SUMMONED_REMOVE, (c, chr, in) -> SummonedHandler.handleSummonedRemove(chr, in));
        reg(InHeader.SUMMONED_MOVE, (c, chr, in) -> SummonedHandler.handleSummonedMove(chr, in));
        reg(InHeader.SUMMONED_HIT, (c, chr, in) -> SummonedHandler.handleSummonedHit(chr, in));
        reg(InHeader.SUMMONED_SKILL_ATTACK, (c, chr, in) -> SummonedHandler.handleSummonedSkillAttack(chr, in));
        reg(InHeader.RECALL_SUMMON, (c, chr, in) -> SummonedHandler.handleRecallSummon(chr, in));
        reg(InHeader.SUMMONED_SEQUENCE_ATTACK, (c, chr, in) -> SummonedHandler.handleSquenceAttack(chr, in));
        reg(InHeader.AUTH_HEARTBEAT, (c, chr, in) -> LoginHandler.handleAuthHeartBeat(c, in));
        reg(InHeader.PERMISSION_REQUEST, (c, chr, in) -> LoginHandler.handlePermissionRequest(c, in));
        reg(InHeader.WVS_SET_UP_STEP, (c, chr, in) -> LoginHandler.handleWvsSetUpStep(c, in));
        reg(InHeader.CLIENT_LOADING_TIME_LOG, (c, chr, in) -> LoginHandler.handleClientLoadingTimeLong(c, in));
        reg(InHeader.CHECK_SPW_REQUEST, (c, chr, in) -> LoginHandler.handleCheckSpwRequest(c, in));
        reg(InHeader.WORLD_STATUS_REQUEST, (c, chr, in) -> LoginHandler.handleWorldStatusRequest(c, in));
        reg(InHeader.WORLD_LIST_REQUEST, (c, chr, in) -> LoginHandler.handleWorldListRequest(c, in));
        reg(InHeader.WORLD_INFO_REQUEST, (c, chr, in) -> LoginHandler.handleWorldListRequest(c, in));
        reg(InHeader.LOGOUT_WORLD, (c, chr, in) -> LoginHandler.handleLogOutWorld(c, in));
        reg(InHeader.LOGIN_AFTER_CHAR_CREATION, (c, chr, in) -> LoginHandler.LoginAfterCharacterCreation(c, in));
        reg(InHeader.CHANGE_PIC_REQUEST, (c, chr, in) -> LoginHandler.handleChangePicRequest(c, in));
        reg(InHeader.SPECIAL_LOGIN_AUTH_REQUEST, (c, chr, in) -> LoginHandler.handleSpecialLogin_Auth_Request(c, in));
        reg(InHeader.AUTH_FAILURE, (c, chr, in) -> LoginHandler.handleAuthFailure(c, in));
        reg(InHeader.WVS_CRASH_CALLBACK, (c, chr, in) -> LoginHandler.handleWvsCrashCallback(c, in));
        reg(InHeader.CHANGE_CHARACTER_SLOT, (c, chr, in) -> LoginHandler.handleChangeCharacterSlot(c, in));
        reg(InHeader.PRIVATE_SERVER_PACKET, (c, chr, in) -> LoginHandler.handlePrivateServerPacket(c, in));
        reg(InHeader.SELECT_CHARACTER, (c, chr, in) -> LoginHandler.handleSelectCharacter(c, in));
        reg(InHeader.DELETE_CHARACTER, (c, chr, in) -> LoginHandler.handleDeleteCharacter(c, in));
        reg(InHeader.RESERVED_DELETE_CHARACTER_CONFIRM, (c, chr, in) -> LoginHandler.handleReservedDeleteCharacterConfirm(c, in));
        reg(InHeader.RESERVED_DELETE_CHARACTER_CANCEL, (c, chr, in) -> LoginHandler.handleReservedDeleteCharacterCancel(c, in));
        reg(InHeader.CHECK_DUPLICATE_ID, (c, chr, in) -> LoginHandler.handleCheckDuplicatedID(c, in));
        reg(InHeader.CREATE_NEW_CHARACTER, (c, chr, in) -> LoginHandler.handleCreateNewCharacter(c, in));
        reg(InHeader.APPLIED_HOT_FIX, (c, chr, in) -> LoginHandler.handleAppliedHotFix(c, in));
        reg(InHeader.CHECK_LOGIN_AUTH_INFO, (c, chr, in) -> LoginHandler.handleCheckLoginAuthInfo(c, in));
        reg(InHeader.VIEW_CHANNEL_REQUEST, (c, chr, in) -> LoginHandler.handleViewChannelRequest(c, in));
        reg(InHeader.CREATE_NEW_CHARACTER_CHECKER, (c, chr, in) -> LoginHandler.handleCreateNewCharacterChecker(c, in));
        reg(InHeader.CHAR_SELECT_NO_PIC, (c, chr, in) -> LoginHandler.handleCharSelectNoPic(c, in));
        reg(InHeader.SERVERSTATUS_REQUEST, (c, chr, in) -> LoginHandler.handleServerStatusRequest(c, in));
        reg(InHeader.USE_AUTH_SERVER, (c, chr, in) -> LoginHandler.handleAuthServer(c, in));
        reg(InHeader.PONG, (c, chr, in) -> LoginHandler.handlePong(c, in));
        reg(InHeader.USER_GET_TIME_REQUEST, (c, chr, in) -> LoginHandler.handleServerTimeRequest(c, in));
        reg(InHeader.SELECT_WORLD, (c, chr, in) -> LoginHandler.handleSelectWorld(c, in));
        reg(InHeader.CLIENT_ERROR, (c, chr, in) -> LoginHandler.handleClientError(c, in));
        reg(InHeader.CHAR_SELECT, (c, chr, in) -> LoginHandler.handleCharSelect(c, in));
        reg(InHeader.EXCEPTION_LOG, (c, chr, in) -> LoginHandler.handleExceptionLog(c, in));
        reg(InHeader.EXCEPTION_LOG_2, (c, chr, in) -> LoginHandler.handleExceptionLog(c, in));
        reg(InHeader.USER_DROP_MONEY_REQUEST, (c, chr, in) -> DropHandler.handleUserDropMoneyRequest(chr, in));
        reg(InHeader.DROP_PICK_UP_REQUEST, (c, chr, in) -> DropHandler.handleDropPickUpRequest(chr, in));
        reg(InHeader.SHOOT_OBJECT_CREATE_REQUEST, (c, chr, in) -> AttackHandler.handleShootObjectCreateRequest(chr, in));
        reg(InHeader.SHOOT_OBJECT_EXPLODE_REQUEST, (c, chr, in) -> AttackHandler.handleShootObjectExplodeRequest(chr, in));
        reg(InHeader.FAMILIAR_ATTACK, (c, chr, in) -> AttackHandler.handleFamiliarAttack(chr, in));
        reg(InHeader.ADD_ATTACK_RESET, (c, chr, in) -> AttackHandler.handleAddAttackReset(chr, in));
        reg(InHeader.SUMMONED_ATTACK, (c, chr, in) -> AttackHandler.handleSummonedAttack(chr, in));
        reg(InHeader.USER_MOVING_SHOOT_ATTACK_PREPARE, (c, chr, in) -> AttackHandler.handleMovingShootAttackPrepare(chr, in));
        reg(InHeader.CHARGE_STACKING_MATRIX_SKILL_REQUEST, (c, chr, in) -> AttackHandler.handleStackingMatrixSkillRequest(chr, in));
        reg(InHeader.DIVINE_ECHO_EXPIRE_REQUEST, (c, chr, in) -> AttackHandler.handleDivineEchoExpireRequest(chr, in));
        reg(InHeader.STACK_OVER_TIME_SKILL_INCREASE_REQUEST, (c, chr, in) -> AttackHandler.handleStackOverTimeSkillIncreaseRequest(chr, in));
        reg(InHeader.LARA_STACK_OVER_TIME_SKILL_INCREASE_REQUEST, (c, chr, in) -> AttackHandler.handleStackOverTimeSkillIncreaseRequest(chr, in));
        reg(InHeader.CREEPING_TOXIN_AREA_REQUEST, (c, chr, in) -> AttackHandler.handleCreepingToxinAreaRequest(chr, in));
        reg(InHeader.USER_BODY_ATTACK, (c, chr, in) -> AttackHandler.handleBodyAttack(chr, in));
        reg(InHeader.USER_MELEE_ATTACK, (c, chr, in) -> AttackHandler.handleAttack(chr, in, InHeader.USER_MELEE_ATTACK));
        reg(InHeader.USER_SHOOT_ATTACK, (c, chr, in) -> AttackHandler.handleAttack(chr, in, InHeader.USER_SHOOT_ATTACK));
        reg(InHeader.USER_MAGIC_ATTACK, (c, chr, in) -> AttackHandler.handleAttack(chr, in, InHeader.USER_MAGIC_ATTACK));
        reg(InHeader.USER_NON_TARGET_FORCE_ATOM_ATTACK, (c, chr, in) -> AttackHandler.handleAttack(chr, in, InHeader.USER_NON_TARGET_FORCE_ATOM_ATTACK));
        reg(InHeader.USER_AREA_DOT_ATTACK, (c, chr, in) -> AttackHandler.handleAttack(chr, in, InHeader.USER_AREA_DOT_ATTACK));
        reg(InHeader.USER_AFFECTED_AREA_FOR_SCREEN_ATTACK, (c, chr, in) -> AttackHandler.handleAttack(chr, in, InHeader.USER_AFFECTED_AREA_FOR_SCREEN_ATTACK));
        reg(InHeader.DRAGON_GLIDE, (c, chr, in) -> DragonHandler.handleDragonGlide(chr, in));
        reg(InHeader.DRAGON_MOVE, (c, chr, in) -> DragonHandler.handleDragonMove(chr, in));
        reg(InHeader.CASH_SHOP_QUERY_CASH_REQUEST, (c, chr, in) -> CashShopHandler.handleCashShopQueryCashRequest(c, in));
        reg(InHeader.CASH_SHOP_CASH_ITEM_REQUEST, (c, chr, in) -> CashShopHandler.handleCashShopCashItemRequest(c, in));
        reg(InHeader.M_V_P__DAILY_PACK__REQUEST, (c, chr, in) -> CashShopHandler.handleCashShopMVPDailyPackRequest(c, in));
        reg(InHeader.M_V_P__SPECIAL_PACK__REQUEST, (c, chr, in) -> CashShopHandler.handleCashShopMVPSpecialPackRequest(c, in));
        reg(InHeader.USER_DRAGON_BREATH_EARTH_EFFECT, (c, chr, in) -> JobSkillHandler.handleUserDragonBreathEarthEffectRequest(chr, in));
        reg(InHeader.REQUEST_ARROW_PLATTER_OBJ, (c, chr, in) -> JobSkillHandler.handleRequestArrowPlatterObj(chr, in));
        reg(InHeader.USER_CREATE_HOLIDOM_REQUEST, (c, chr, in) -> JobSkillHandler.handleUserCreateHolidomRequest(chr, in));
        reg(InHeader.REQUEST_DEC_COMBO, (c, chr, in) -> JobSkillHandler.handleRequestDecCombo(chr, in));
        reg(InHeader.RW_ACTION_CANCEL, (c, chr, in) -> JobSkillHandler.handleRWActionCancel(chr, in));
        reg(InHeader.CYGNUS_SKILL_COMMAND_LOCK, (c, chr, in) -> JobSkillHandler.handleCygnusSkillCommandLock(chr, in));
        reg(InHeader.SKILL_COMMAND_LOCK, (c, chr, in) -> JobSkillHandler.handleSkillCommandLock(chr, in));
        reg(InHeader.USER_FLAME_ORB_REQUEST, (c, chr, in) -> JobSkillHandler.handleUserFlameOrbRequest(chr, in));
        reg(InHeader.REQUEST_MATRIX_SUMMON_ATTACK, (c, chr, in) -> JobSkillHandler.handleRequestMatrixSummonAttack(chr, in));
        reg(InHeader.USER_REQUEST_STEAL_SKILL_LIST, (c, chr, in) -> JobSkillHandler.handleUserRequestStealSkillList(chr, in));
        reg(InHeader.USER_SET_DRESS_CHANGED_REQUEST, (c, chr, in) -> JobSkillHandler.handleUserSetDressChangedRequest(chr, in));
        reg(InHeader.USER_JAGUAR_CHANGE_REQUEST, (c, chr, in) -> JobSkillHandler.handleUserJaguarChangeRequest(chr, in));
        reg(InHeader.CREATE_KINESIS_PSYCHIC_AREA, (c, chr, in) -> JobSkillHandler.handleCreateKinesisPsychicArea(chr, in));
        reg(InHeader.DO_ACTIVE_PSYCHIC_AREA, (c, chr, in) -> JobSkillHandler.handleDoActivePsychicArea(chr, in));
        reg(InHeader.USER_KEY_DOWN_STEP_REQUEST, (c, chr, in) -> JobSkillHandler.handleKeyDownStepRequest(chr, in));
        reg(InHeader.ENTER_OPEN_GATE_REQUEST, (c, chr, in) -> JobSkillHandler.handleEnterOpenGateRequest(chr, in));
        reg(InHeader.REQUEST_SET_HP_BASE_DAMAGE, (c, chr, in) -> JobSkillHandler.handleRequestSetHpBaseDamage(chr, in));
        reg(InHeader.RELEASE_PSYCHIC_AREA, (c, chr, in) -> JobSkillHandler.handleReleasePsychicArea(chr, in));
        reg(InHeader.RELEASE_RW_GRAB, (c, chr, in) -> JobSkillHandler.handleReleaseRWGrab(chr, in));
        reg(InHeader.RW_CLEAR_CURRENT_ATTACK_REQUEST, (c, chr, in) -> JobSkillHandler.handleRWClearCurrentAttackRequest(chr, in));
        reg(InHeader.USER_REQUEST_SET_OFF_TRINITY, (c, chr, in) -> JobSkillHandler.handleUserRequestSetOffTrinity(chr, in));
        reg(InHeader.PSYCHIC_OVER_REQUEST, (c, chr, in) -> JobSkillHandler.handlePsychicOverRequest(chr, in));
        reg(InHeader.CREATE_PSYCHIC_LOCK, (c, chr, in) -> JobSkillHandler.handleCreatePsychicLock(chr, in));
        reg(InHeader.DEBUFF_PSYCHIC_AREA, (c, chr, in) -> JobSkillHandler.handleDebuffPsychicArea(chr, in));
        reg(InHeader.USER_REQUEST_SET_STEAL_SKILL_SLOT, (c, chr, in) -> JobSkillHandler.handleUserRequestSetStealSkillSlot(chr, in));
        reg(InHeader.USER_REQUEST_STEAL_SKILL_MEMORY, (c, chr, in) -> JobSkillHandler.handleUserRequestStealSkillMemory(chr, in));
        reg(InHeader.ZERO_LAST_ASSIST_STATE, (c, chr, in) -> JobSkillHandler.handleZeroLastAssistState(chr, in));
        reg(InHeader.RESET_AIR_HIT_COUNT_REQUEST, (c, chr, in) -> JobSkillHandler.handleResetAirHitCountRequest(chr, in));
        reg(InHeader.USER_DRAGON_ACTION, (c, chr, in) -> JobSkillHandler.handleUserDragonActionRequest(chr, in));
        reg(InHeader.ZERO_SHARE_CASH_EQUIP_PART, (c, chr, in) -> JobSkillHandler.handleZeroShareCashEquipPart(chr, in));
        reg(InHeader.USER_REQUEST_FLYING_SWORD_START, (c, chr, in) -> JobSkillHandler.handleUserRequestFlyingSwordStart(chr, in));
        reg(InHeader.REQUEST_SET_BLESS_OF_DARKNESS, (c, chr, in) -> JobSkillHandler.handleRequestSetBlessOfDarkness(chr, in));
        reg(InHeader.REQUEST_INC_COMBO, (c, chr, in) -> JobSkillHandler.handleRequestIncCombo(chr, in));
        reg(InHeader.RELEASE_PSYCHIC_LOCK, (c, chr, in) -> JobSkillHandler.handleReleasePsychicLock(chr, in));
        reg(InHeader.RW_LIFT_PRESS_REQUEST, (c, chr, in) -> JobSkillHandler.handleRWLiftPressRequest(chr, in));
        reg(InHeader.RW_MULTI_CHARGE_CANCEL_REQUEST, (c, chr, in) -> JobSkillHandler.handleRWMultiChargeCancelRequest(chr, in));
        reg(InHeader.ZERO_TAG, (c, chr, in) -> JobSkillHandler.handleZeroTag(chr, in));
        reg(InHeader.KEY_DOWN_SKILL_COST, (c, chr, in) -> JobSkillHandler.handleKeyDownSkillCost(chr, in));
        reg(InHeader.SKILL_COMMAND_LOCK_ARK, (c, chr, in) -> JobSkillHandler.handleSkillCommandLockArk(chr, in));
        reg(InHeader.USER_SHADOW_MOMENTUM_REQUEST, (c, chr, in) -> JobSkillHandler.handleShadowMomentumRequest(chr, in));
        reg(InHeader.USER_GIVE_POPULARITY_REQUEST, (c, chr, in) -> RoomHandler.handleUserGivePopularityRequest(chr, in));
        reg(InHeader.USER_ENTRUSTED_SHOP_REQUEST, (c, chr, in) -> RoomHandler.handleUserEntrustedShopRequest(chr, in));
        reg(InHeader.MINI_ROOM, (c, chr, in) -> RoomHandler.handleMiniRoom(chr, in));
        reg(InHeader.USER_ANDROID_SHOP, (c, chr, in) -> AndroidHandler.handleAndroidShop(chr, in));
        reg(InHeader.ANDROID_ACTION_SET, (c, chr, in) -> AndroidHandler.handleAndroidActionSet(chr, in));
        reg(InHeader.ANDROID_EMOTION, (c, chr, in) -> AndroidHandler.handleAndroidEmotion(chr, in));
        reg(InHeader.ANDROID_MOVE, (c, chr, in) -> AndroidHandler.handleAndroidMove(chr, in));
        reg(InHeader.SET_EVENT_NAME_TAG, (c, chr, in) -> EventNameTagHandler.handleSetEventNameTag(chr, in));
        reg(InHeader.USER_HYPER_STAT_RESET_REQUEST, (c, chr, in) -> UserStatHandler.handleUserHyperStatResetRequest(chr, in));
        reg(InHeader.USER_HYPER_STAT_CHANGE_PRESET_REQUEST, (c, chr, in) -> UserStatHandler.handleUserHyperStatChangePresetRequest(chr, in));
        reg(InHeader.USER_CHANGE_STAT_REQUEST_BY_ITEM_OPTION, (c, chr, in) -> UserStatHandler.handleUserChangeStatByItemOptionRequest(chr, in));
        reg(InHeader.USER_ABILITY_MASS_UP_REQUEST, (c, chr, in) -> UserStatHandler.handleUserAbilityMassUpRequest(chr, in));
        reg(InHeader.USER_HYPER_STAT_UP_REQUEST, (c, chr, in) -> UserStatHandler.handleUserHyperStatUpRequest(chr, in));
        reg(InHeader.USER_HYPER_SKILL_RESET_REQUEST, (c, chr, in) -> UserStatHandler.handleUserHyperSkillsResetRequest(chr, in));
        reg(InHeader.USER_SKILL_UP_REQUEST, (c, chr, in) -> UserStatHandler.handleUserSkillUpRequest(chr, in));
        reg(InHeader.USER_REQUEST_INSTANCE_TABLE, (c, chr, in) -> UserStatHandler.handleUserRequestInstanceTable(chr, in));
        reg(InHeader.USER_CHANGE_STAT_REQUEST, (c, chr, in) -> UserStatHandler.handleUserChangeStatRequest(chr, in));
        reg(InHeader.USER_HYPER_SKILL_UP_REQUEST, (c, chr, in) -> UserStatHandler.handleUserHyperSkillUpRequest(chr, in));
        reg(InHeader.USER_ABILITY_UP_REQUEST, (c, chr, in) -> UserStatHandler.handleUserAbilityUpRequest(chr, in));
        reg(InHeader.USER_DOT_HEAL, (c, chr, in) -> UserStatHandler.handleUserDotHeal(chr, in));
        reg(InHeader.EVOLVING_REQUEST, (c, chr, in) -> EvolvingSystemHandler.handleEvolvingRequest(chr, in));
        reg(InHeader.GROUP_MESSAGE, (c, chr, in) -> ChatHandler.handleGroupMessage(chr, in));
        reg(InHeader.ITEM_LINKED_GROUP_MESSAGE, (c, chr, in) -> ChatHandler.handleItemLinkedGroupMessage(chr, in));
        reg(InHeader.GUILD_CHAT, (c, chr, in) -> ChatHandler.handleGuildChat(chr, in));
        reg(InHeader.USER_CHAT, (c, chr, in) -> ChatHandler.handleUserChat(chr, in));
        reg(InHeader.USER_ITEM_LINKED_CHAT, (c, chr, in) -> ChatHandler.handleUserItemLinkedChat(chr, in));
        reg(InHeader.FRIEND_CHAT, (c, chr, in) -> ChatHandler.handleFriendChat(chr, in));
        reg(InHeader.WHISPER, (c, chr, in) -> ChatHandler.handleWhisper(chr, in, InHeader.WHISPER));
        reg(InHeader.WHISPER_ITEM, (c, chr, in) -> ChatHandler.handleWhisper(chr, in, InHeader.WHISPER_ITEM));
        reg(InHeader.REACTOR_RECT_IN_MOB, (c, chr, in) -> ReactorHandler.handleReactorRectInMob(chr, in));
        reg(InHeader.REACTOR_CLICK, (c, chr, in) -> ReactorHandler.handleReactorClick(chr, in));
        reg(InHeader.REACTOR_HIT, (c, chr, in) -> ReactorHandler.handleReactorHit(chr, in));
        reg(InHeader.REACTOR_KEY, (c, chr, in) -> ReactorHandler.handleReactorKey(chr, in));
        reg(InHeader.REACTOR_BY_SPACE, (c, chr, in) -> ReactorHandler.handleReactorBySpace(chr, in));
        reg(InHeader.USER_TRUNK_REQUEST, (c, chr, in) -> UserTrunkHandler.handleUserTrunkRequest(chr, in));
        reg(InHeader.JOB_FREE_CHANGE_REQUEST, (c, chr, in) -> JobFreeChangeHandler.handleJobFreeChangeRequest(chr, in));
        reg(InHeader.FAMILIAR_SKILL, (c, chr, in) -> FamiliarHandler.handleFamiliarSkill(chr, in));
        reg(InHeader.FAMILIAR_ADD_REQUEST, (c, chr, in) -> FamiliarHandler.handleFamiliarAddRequest(chr, in));
        reg(InHeader.FAMILIAR_RENAME_REQUEST, (c, chr, in) -> FamiliarHandler.handleFamiliarRenameRequest(chr, in));
        reg(InHeader.FAMILIAR_SPAWN_REQUEST, (c, chr, in) -> FamiliarHandler.handleFamiliarSpawnRequest(chr, in));
        reg(InHeader.FAMILIAR_MOVE, (c, chr, in) -> FamiliarHandler.handleFamiliarMove(chr, in));
        reg(InHeader.ALLIANCE_REQUEST, (c, chr, in) -> GuildHandler.handleAllianceRequest(chr, in));
        reg(InHeader.GUILD_JOIN_REJECT, (c, chr, in) -> GuildHandler.handleGuildJoinReject(chr, in));
        reg(InHeader.GUILD_JOIN_ACCEPT, (c, chr, in) -> GuildHandler.handleGuildJoinAccept(chr, in));
        reg(InHeader.GUILD_JOIN_REQUEST, (c, chr, in) -> GuildHandler.handleGuildJoinRequest(chr, in));
        reg(InHeader.GUILD_JOIN_CANCEL_REQUEST, (c, chr, in) -> GuildHandler.handleGuildJoinCancelRequest(chr, in));
        reg(InHeader.GUILD_REQUEST, (c, chr, in) -> GuildHandler.handleGuildRequest(chr, in));
        reg(InHeader.GUILD_BBS, (c, chr, in) -> GuildHandler.handleGuildBBS(chr, in));
        reg(InHeader.GUILD_RESULT, (c, chr, in) -> GuildHandler.handleGuildResult(chr, in));
        reg(InHeader.SURPRISE_BOX, (c, chr, in) -> CashShopHandler.handleSurpriseBox(c, in));
        reg(InHeader.VIOLET_CUBE_REQUEST, (c, chr, in) -> CubeHandler.handleVioletCubeRequest(chr, in));
        reg(InHeader.USER_MEMORIAL_CUBE_OPTION_REQUEST, (c, chr, in) -> CubeHandler.handleUserMemorialCubeOptionRequest(chr, in));
        reg(InHeader.USER_CONSUME_CUBE_ITEM_USE_REQUEST, (c, chr, in) -> CubeHandler.handleUserConsumeCubeItemUserRequest(chr, in));
        reg(InHeader.USER_BONUS_MYSTICAL_ITEM_USE_REQUEST, (c, chr, in) -> CubeHandler.handleUserBonusMysticalCubeItemUseRequest(chr, in));
        reg(InHeader.USER_BONUS_BRIGHT_CUBE_ITEM_USE_REQUEST, (c, chr, in) -> CubeHandler.handleUserBonusCubeItemUseRequest(chr, in));
        reg(InHeader.USER_CASH_CUBE_ITEM_USE_REQUEST, (c, chr, in) -> CubeHandler.handleUserCashCubeItemUseRequest(chr, in));
        reg(InHeader.USER_COMPLETE_NPC_SPEECH, (c, chr, in) -> NpcHandler.handleUserCompleteNpcSpeech(chr, in));
        reg(InHeader.GACHAPON_REQUEST, (c, chr, in) -> NpcHandler.handleGachaponRequest(chr, in));
        reg(InHeader.NPC_SPECIAL_ACTION, (c, chr, in) -> NpcHandler.handleNpcSpecialAction(chr, in));
        reg(InHeader.USER_SELECT_NPC, (c, chr, in) -> NpcHandler.handleUserSelectNpc(chr, in));
        reg(InHeader.USER_SHOP_REQUEST, (c, chr, in) -> NpcHandler.handleUserShopRequest(chr, in));
        reg(InHeader.NPC_MOVE, (c, chr, in) -> NpcHandler.handleNpcMove(chr, in));
        reg(InHeader.USER_MACRO_SYS_DATA_MODIFIED, (c, chr, in) -> MacroHandler.handleUserMacroSysDataModified(chr, in));
        reg(InHeader.USER_ANTI_MACRO_QUESTION_RESULT, (c, chr, in) -> MacroHandler.handleUserAntiMacroQuestionResult(chr, in));
        reg(InHeader.USER_ANTI_MACRO_REFRESH_REQUEST, (c, chr, in) -> MacroHandler.handleUserAntiMacroRefreshResult(chr, in));
        reg(InHeader.USER_TRANSFER_FIELD_REQUEST, (c, chr, in) -> MigrationHandler.handleUserTransferFieldRequest(chr, in));
        reg(InHeader.USER_MAP_TRANSFER_REQUEST, (c, chr, in) -> MigrationHandler.handleUserMapTransferRequest(chr, in));
        reg(InHeader.USER_MIGRATE_TO_CASH_SHOP_REQUEST, (c, chr, in) -> MigrationHandler.handleUserMigrateToCashShopRequest(c, in));
        reg(InHeader.USER_MIGRATE_TO_AUCTION_HOUSE_REQUEST, (c, chr, in) -> MigrationHandler.handleUserMigrateToAuctionHouseRequest(c, in));
        reg(InHeader.AUCTION_LEAVE_REQUEST, (c, chr, in) -> MigrationHandler.handleUserAuctionLeaveRequest(c, in));
        reg(InHeader.ENTER_TOWN_PORTAL_REQUEST, (c, chr, in) -> MigrationHandler.handleEnterTownPortalRequest(chr, in));
        reg(InHeader.USER_PORTAL_SCRIPT_REQUEST, (c, chr, in) -> MigrationHandler.handleUserPortalScriptRequest(chr, in));
        reg(InHeader.USER_PORTAL_SCRIPT_REQUEST_2, (c, chr, in) -> MigrationHandler.handleUserPortalScriptRequest(chr, in));
        reg(InHeader.USER_PORTAL_SCRIPT_REQUEST_3, (c, chr, in) -> MigrationHandler.handleUserPortalScriptRequest(chr, in));
        reg(InHeader.USER_PORTAL_TELEPORT_REQUEST, (c, chr, in) -> MigrationHandler.handleUserPortalTeleportRequest(chr, in));
        reg(InHeader.USER_TRANSFER_CHANNEL_REQUEST, (c, chr, in) -> MigrationHandler.handleUserTransferChannelRequest(c, in));
        reg(InHeader.USER_FIELD_TRANSFER_REQUEST, (c, chr, in) -> MigrationHandler.handleUserFieldTransferRequest(chr, in));
        reg(InHeader.USER_FIELD_TRANSFER_REQUEST_2, (c, chr, in) -> MigrationHandler.handleUserFieldTransferRequest(chr, in));
        reg(InHeader.REQUEST_RELOGIN_COOKIE, (c, chr, in) -> MigrationHandler.handleReloginCookieRequest(chr, in));
        reg(InHeader.SWITCH_CHARACTER, (c, chr, in) -> MigrationHandler.handleSwitchCharacter(chr, in));
        reg(InHeader.GOLLUX_OUT_REQUEST, (c, chr, in) -> MigrationHandler.handleGolluxOutReqeust(chr, in));
        reg(InHeader.USER_TRANSFER_FREE_MARKET_REQUEST, (c, chr, in) -> MigrationHandler.handleTransferFreeMarketRequest(chr, in));
        reg(InHeader.FPS_LOG, (c, chr, in) -> MigrationHandler.handleFPSLog(chr, in));
        reg(InHeader.MIGRATE_IN, (c, chr, in) -> MigrationHandler.handleMigrateIn(c, in));
        reg(InHeader.UNION_REQUEST, (c, chr, in) -> UnionHandler.handleUnionRequest(chr, in));
        reg(InHeader.UNION_ASSIGN_REQUEST, (c, chr, in) -> UnionHandler.handleUnionAssignRequest(chr, in));
        reg(InHeader.UNION_PRESET_CHANGE, (c, chr, in) -> UnionHandler.handleUnionPresetChange(chr, in));
        reg(InHeader.UNION_PRESET_INFO_REQUEST, (c, chr, in) -> UnionHandler.handleUnionPresetInfoRequest(chr, in));
        reg(InHeader.UNION_RAID_RETREAT, (c, chr, in) -> UnionHandler.handleUnionRaidRetreat(chr, in));
        reg(InHeader.UNION_ARTIFACT_UPDATE_REQUEST, (c, chr, in) -> UnionHandler.handleUnionArtifactUpdateRequest(chr, in));
        reg(InHeader.REQUEST_STATUS_CHECK, (c, chr, in) -> SecurityHandler.handleRequestStatusCheck(c, in));
    }

    private static final HandlerRegistry BASE_REGISTRY = BASE.build();
    /** Base + overlay of the profile it was composed for; recomposed when the active profile changes. */
    private static volatile ComposedRegistry composed;

    private record ComposedRegistry(ProtocolProfile profile, HandlerRegistry registry) {
    }

    private static void handleGameExitStage(net.swordie.ms.client.Client c,
                                           net.swordie.ms.client.character.Char chr,
                                           net.swordie.ms.connection.InPacket in) {
        consumeSilently(in);
        prepareForGameExit(c, chr);
        acknowledgeClientStage(c, chr);
    }

    private static void handleClientScriptUiState(net.swordie.ms.client.Client c,
                                                  net.swordie.ms.client.character.Char chr,
                                                  net.swordie.ms.connection.InPacket in) {
        consumeSilently(in);
        acknowledgeClientStage(c, chr);
    }

    private static void handleClientExitTelemetryAck(net.swordie.ms.client.Client c,
                                                     net.swordie.ms.client.character.Char chr,
                                                     net.swordie.ms.connection.InPacket in) {
        consumeSilently(in);
        prepareForGameExit(c, chr);
        acknowledgeClientStage(c, chr);
    }

    private static void handleMobCrcDataResult(net.swordie.ms.client.Client c,
                                               net.swordie.ms.client.character.Char chr,
                                               net.swordie.ms.connection.InPacket in) {
        consumeSilently(in);
        if (c != null && c.isExiting()) {
            acknowledgeClientStage(c, chr);
        }
    }

    /**
     * Handles unknown opcodes during exit without registering them in InHeader.
     * Opcode 1342 with an empty body is only used as the first exit-telemetry packet in v265.3 logs.
     */
    public static boolean tryAckUnknownExitPacket(net.swordie.ms.client.Client c,
                                                  net.swordie.ms.client.character.Char chr,
                                                  short op,
                                                  net.swordie.ms.connection.InPacket in) {
        if (c == null) {
            return false;
        }
        if (c.isExiting()) {
            acknowledgeExitingUnknownPacket(c, chr, in);
            return true;
        }
        if (op == 1342 && in.getUnreadAmount() == 0) {
            consumeSilently(in);
            prepareForGameExit(c, chr);
            acknowledgeClientStage(c, chr);
            return true;
        }
        return false;
    }

    public static void acknowledgeExitingUnknownPacket(net.swordie.ms.client.Client c,
                                                       net.swordie.ms.client.character.Char chr,
                                                       net.swordie.ms.connection.InPacket in) {
        consumeSilently(in);
        acknowledgeClientStage(c, chr);
    }

    private static void prepareForGameExit(net.swordie.ms.client.Client c,
                                           net.swordie.ms.client.character.Char chr) {
        if (c != null) {
            c.setExiting(true);
        }
        if (chr == null || chr.getJobHandler() == null) {
            return;
        }
        if (chr.getJobHandler() instanceof net.swordie.ms.client.jobs.Jianghu.MoXuan moXuan) {
            moXuan.prepareForGameExit();
        }
    }

    private static void acknowledgeClientStage(net.swordie.ms.client.Client c, net.swordie.ms.client.character.Char chr) {
        if (chr != null) {
            chr.dispose();
        } else if (c != null) {
            c.dispose();
        }
    }

    private static void consumeSilently(net.swordie.ms.connection.InPacket in) {
        int unread = in.getUnreadAmount();
        if (unread > 0) {
            in.decodeArr(unread);
        }
    }

    private static void regSilentTelemetry(InHeader... headers) {
        for (InHeader header : headers) {
            reg(header, (c, chr, in) -> consumeSilently(in));
        }
    }

    private static void reg(InHeader h, PacketHandler handler) {
        BASE.register(h, handler);
    }

    /** The 265 handlers alone, without any profile overlay. */
    public static HandlerRegistry baseRegistry() {
        return BASE_REGISTRY;
    }

    /** Dispatch registry for {@code profile}: base handlers with the profile's overlay on top. */
    public static HandlerRegistry registryFor(ProtocolProfile profile) {
        ComposedRegistry current = composed;
        if (current != null && current.profile() == profile) {
            return current.registry();
        }
        HandlerRegistry registry = BASE_REGISTRY.withOverlay(profile.handlerOverlay());
        composed = new ComposedRegistry(profile, registry);
        return registry;
    }

    /**
     * Startup check: refuses a profile whose PROFILE_CODEC opcodes would still reach 265 handlers.
     *
     * @throws IllegalStateException listing the offending opcodes
     */
    public static void validate(ProtocolProfile profile) {
        registryFor(profile).validate(profile);
    }

    public static PacketHandler getHandler(InHeader h) {
        return registryFor(Protocol.active()).get(h);
    }
}
