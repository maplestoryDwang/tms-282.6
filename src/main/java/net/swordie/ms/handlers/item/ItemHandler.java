package net.swordie.ms.handlers.item;

import net.swordie.ms.Server;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.BeautySalon;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.damage.DamageSkinSaveData;
import net.swordie.ms.client.character.damage.DamageSkinType;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.potential.CharacterPotentialMan;
import net.swordie.ms.client.character.potential.CharacterPotentialValueHolder;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.character.union.UnionArtifact;
import net.swordie.ms.client.social.Guild.GuildSkill;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.life.pet.PetSkill;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.MakingSkillRecipe;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.*;
import net.swordie.ms.world.event.SunnySunday;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Field.BlowWeather;
import net.swordie.ms.world.field.FieldInstanceType;
import net.swordie.ms.world.field.Portal;

import java.time.LocalDateTime;
import java.util.*;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.ChatType.*;
import static net.swordie.ms.enums.EquipBaseStat.*;
import static net.swordie.ms.enums.InvType.*;
import static net.swordie.ms.enums.InventoryOperation.*;

public class ItemHandler {

    private static final int EXPAND_MENU_ITEM_ID = 2432022;
    private static final String EXPAND_MENU_CONTENT_SCRIPT = "expand/menu";

    private static void startScriptItem(Char chr, int itemID) {
        chr.getScriptManager().startScript(chr, itemID, resolveScriptItemName(itemID), ScriptType.Item);
    }

    private static void startExpandMenuItem(Char chr) {
        chr.getScriptManager().startScript(chr, EXPAND_MENU_ITEM_ID, EXPAND_MENU_CONTENT_SCRIPT, ScriptType.Content);
    }

    private static String resolveScriptItemName(int itemID) {
        ItemInfo ii = ItemData.getItemInfoByID(itemID);
        if (ii != null && ii.getScript() != null && !"".equals(ii.getScript())) {
            return ii.getScript();
        }
        String consumeScript = "consume_" + itemID;
        if (ScriptManagerImpl.hasScriptFile(ScriptType.Item, consumeScript)) {
            return consumeScript;
        }
        return String.valueOf(itemID);
    }

    private static boolean hasScriptItem(int itemID) {
        ItemInfo ii = ItemData.getItemInfoByID(itemID);
        if (ii != null && ii.getScript() != null && !"".equals(ii.getScript())) {
            return true;
        }
        return ScriptManagerImpl.hasScriptFile(ScriptType.Item, "consume_" + itemID);
    }

    /** Spec-less ids that Char.useStatChangeItem handles explicitly (debuff cures); every other spec-less id is unhandled. */
    private static final java.util.Set<Integer> SPECLESS_STAT_CHANGE_ITEMS =
            java.util.Set.of(2050000, 2050001, 2050002, 2050003, 2001556, 2050004, 2050008, 2350011, 2050007);

    private static boolean isUnhandledStatChangeItem(int itemID) {
        ItemInfo info = ItemData.getItemInfoByID(itemID);
        return info != null && info.getSpecStats().isEmpty() && !SPECLESS_STAT_CHANGE_ITEMS.contains(itemID);
    }

    private static boolean handleStatChangeConsumeItem(Char chr, Item item, int itemID, boolean warnInvalid) {
        if (item == null || item.getItemId() != itemID || item.getQuantity() < 1) {
            chr.dispose();
            return true;
        }
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.StatChangeItemConsumeLimit.getVal()) > 0) {
            chr.dispose();
            return true;
        }
        if (Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).anyMatch(m -> chr.getField() != null && m == chr.getField().getId())) {
            chr.chatMessage("當前地圖無法使用該物品。");
            chr.dispose();
            return true;
        }
        if (!ItemConstants.isChangeStatItem(itemID)) {
            if (warnInvalid) {
                String msg = String.format("Character %d tried to use a uncorrected (id %d) on an equip via Use inventory. OPCode : %d",
                        chr.getId(), itemID, InHeader.USER_STAT_CHANGE_ITEM_USE_REQUEST.getValue());
                DataPrinter.send(DataPrinter.AUTOBAN_WARNING, msg);
                chr.dispose();
            }
            return false;
        }
        if (isUnhandledStatChangeItem(itemID)) {
            // Spec-less items fall into Char.useStatChangeItem's default branch, which consumes them without any
            // effect. Boxes the Tms282BoxUse registry owns were opened before this (CP0148 entry); an item with a
            // spec/script runs its item script; anything else is refused without consuming it.
            if (hasScriptItem(itemID)) {
                startScriptItem(chr, itemID);
                return true;
            }
            chr.chatMessage("此道具目前尚未支援使用，道具未消耗。");
            chr.dispose();
            return true;
        }
        chr.useStatChangeItem(item, true);
        if (field.getConsumeItemCoolTime() > 0) {
            chr.write(UserLocal.consumeItemCooltime());
        }
        if (itemID == 2000068 && chr.hasQuest(102429)) {
            chr.setQRValueByKey(102429, "potion", "1");
            String step = chr.getQRValueByKey(102429, "step");
            if (step == null) {
                chr.setQRValueByKey(102429, "step", "1");
            } else if (step.equals("1")) {
                chr.setQRValueByKey(102429, "step", "2");
            } else if (step.equals("2")) {
                chr.setQRValueByKey(102429, "step", "done");
            }
        }
        return true;
    }

    @Handler(op = InHeader.USER_PORTAL_SCROLL_USE_REQUEST)
    public static void handleUserPortalScrollUseRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.PortalScrollLimit.getVal()) > 0 || !field.isChannelField()) {
            chr.chatMessage("這張地圖無法使用回家卷軸。");
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); //tick
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();

        Item item = chr.getConsumeInventory().getItemBySlot(slot);

        if (item == null || item.getItemId() != itemID || item.getQuantity() < 1) {
            chr.chatMessage("當前無法使用該傳送卷軸。");
            chr.dispose();
            return;
        }

        ItemInfo ii = ItemData.getItemInfoByID(itemID);
        Field toField;

        if (itemID != 2030000 && itemID != 2030059 && ii != null) {
            toField = chr.getOrCreateFieldByCurrentInstanceType(ii.getMoveTo());
        } else {
            toField = chr.getOrCreateFieldByCurrentInstanceType(field.getReturnMap());
        }
        Portal portal = toField.getDefaultPortal();
        chr.warp(toField, portal);
        chr.consumeItem(itemID, 1);
    }

    @Handler(op = InHeader.USER_STAT_CHANGE_ITEM_CANCEL_REQUEST)
    public static void handleUserStatChangeItemCancelRequest(Char chr, InPacket inPacket) {
        if (chr == null) return;

        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int itemID = inPacket.decodeInt();
        tsm.removeStatsBySkill(itemID);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_CONSUME_CASH_ITEM_USE_REQUEST)
    public static void handleUserConsumeCashItemUseRequest(Char chr, InPacket inPacket) {
        Inventory cashInv = chr.getInventoryByType(InvType.CASH);
        inPacket.decodeInt(); // 0
        inPacket.decodeInt(); // tick
        short pos = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        Item item = cashInv.getItemBySlot(pos);
        ItemInfo itemInfo = ItemData.getItemInfoByID(itemID);
        if (item == null || item.getItemId() != itemID || item.getQuantity() < 1) {
            System.out.println("[" + chr.getName() + "] Unknown item or mismatching use items.");
            chr.dispose();
            return;
        }
        // <================================= CASH ITEM HANDLING =================================>
        // TMS282 data-driven boxes (raffle/incubator/Luna Crystal/random beauty coupons); others keep legacy paths.
        if (net.swordie.ms.world.tms282.boxes.Tms282BoxUse.tryUse(chr, item)) {
            return;
        }
        if (ItemConstants.isRewardCashItem(itemID)) {

            Item reward = null;
            if (itemInfo != null) {
                reward = itemInfo.getRandomReward();
            }
            if (reward == null) {
                chr.chatMessage("獎勵資料尚未就緒，未消耗此道具。");chr.dispose();return;
            }
            Inventory target=chr.getInventoryByType(reward.getInvType());
            if(target==null || !target.canPickUp(reward)) {
                chr.chatMessage("請先保留足夠的背包空間，未消耗此道具。");chr.dispose();return;
            }
            chr.addItemToInventory(reward);
            chr.consumeItem(item, 1);

        } else if (ItemConstants.isAvatarMegaphoneItem(itemID)) {

            List<String> lineList = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                String line = inPacket.decodeString();
                lineList.add(line);
            }
            boolean whisperIcon = inPacket.decodeByte() != 0;
            Server.get().broadcastForWorld(WvsContext.setAvatarMegaphone(chr, itemID, lineList, whisperIcon, false));
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isAPResetItem(itemID)) {

            resetAbilityItemUseRequest(chr, pos, itemID, true);

        } else if (ItemConstants.isAPChangeItem(itemID)) {

            changeAbility(chr, inPacket, pos, itemID, true);

        } else if (ItemConstants.isSkillResetItem(itemID)) {

            resetSkills(chr, pos, itemID, true);

        } else if (ItemConstants.isCirculatorItem(itemID)) {

            byte preset = Byte.parseByte(chr.getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential"));
            resetCharacterPotentials(chr, preset, itemID, pos, false);

        } else if (ItemConstants.isJukeBoxItem(itemID)) {

            chr.getField().broadcast(FieldPacket.playJukeBox(itemID, chr.getName()));
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isChangeMapEffectItem(itemID)) {

            if (Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).anyMatch(m -> chr.getField() != null && m == chr.getField().getId())) {
                chr.chatMessage("當前地圖無法使用該物品。");
                chr.dispose();
                return;
            }
            if (chr.getField().getBw() != null) {
                chr.chatMessage(SystemNotice, "當前地圖已有地圖效果運行，無法使用。");
                chr.dispose();
                return;
            }
            BlowWeather bw = new BlowWeather(false, itemID, String.format("%s : %s", chr.getName(), inPacket.decodeString()));
            chr.getField().startFieldEffect(bw, 300);
            if (itemInfo != null && itemInfo.getStateChangeItem() != 0) {
                Item statChangeItem = ItemData.getItemDeepCopy(itemInfo.getStateChangeItem());
                for (Char player : chr.getField().getChars()) {
                    player.useStatChangeItem(statChangeItem, true);
                    if (itemID == 5121058 || itemID == 5121059 || itemID == 5121060) {
                        ItemBuffs.giveItemBuffsFromItemID(player, player.getTemporaryStatManager(), 2023558);
                    }
                }
            }
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isMesoBagItem(itemID)) {

            if (itemInfo != null) {
                chr.addMoney(itemInfo.getMeso());
                chr.consumeItem(itemID, 1);
                chr.chatMessage("你從楓幣袋中獲得了 " + itemInfo.getMeso() + " 楓幣。");
            }

        } else if (ItemConstants.isRandomMesoBagItem(itemID)) {

            int fromRand = 0;
            int toRand = 0;
            if (itemID == 5202000) {
                fromRand = 2000000;
                toRand = Util.succeedProp(5) ? 54000000 : 15400000;
            } else if (itemID == 5202001) {
                fromRand = 6000000;
                toRand = Util.succeedProp(5) ? 750000000 : 17500000;
            } else if (itemID == 5202002) {
                fromRand = 4000000;
                toRand = Util.succeedProp(5) ? 208000000 : 20800000;
            }
            int moneyReceived = Util.getRandom(fromRand, toRand);
            chr.addMoney(moneyReceived);
            chr.consumeItem(itemID, 1);
            chr.chatMessage("你從楓幣袋中獲得了 " + moneyReceived + " 楓幣。");

        } else if (ItemConstants.isChalkboardtem(itemID)) {

            chr.setADBoardRemoteMsg(inPacket.decodeString());
            chr.getField().broadcast(UserPacket.setADBoard(chr, true));

        } else if (ItemConstants.isPetSkillItem(itemID)) {
            long SN = inPacket.decodeLong();
            PetSkill ps = ItemConstants.getPetSkillFromID(itemID);
            if (ps == null) {
                System.out.printf("Unhandled pet skill item %d%n", itemID);
                chr.dispose();
                return;
            }
            Item pi = chr.getCashInventory().getItemBySN(SN);
            if (!(pi instanceof PetItem petItem)) {
                chr.dispose();
                return;
            }
            boolean add = itemID != 5190014; // Only Slimming Medicine removes a skill; 5191xxx also adds.
            if (petItem.hasPetSkill(ps) == add) {
                chr.dispose();
                return;
            }
            if (add) {
                petItem.addPetSkill(ps);
            } else {
                petItem.removePetSkill(ps);
            }
            petItem.updateToChar(chr);
            chr.consumeItem(item, 1);

        } else if (ItemConstants.isPetNameTag(itemID)) {

            long SN = inPacket.decodeLong();
            String newName = inPacket.decodeString();
            Item pi = chr.getCashInventory().getItemBySN(SN);
            if (!(pi instanceof PetItem petItem)) {
                chr.dispose();
                return;
            }
            if (newName.getBytes().length > 13) {
                chr.chatMessage("寵物名稱不能超過 13 個字符。");
                chr.dispose();
                return;
            }
            if (newName.matches(".*\\s.*")) {
                chr.chatMessage("寵物名稱無效。");
                chr.dispose();
                return;
            }
            petItem.setName(newName);
            petItem.updateToChar(chr);
            if (petItem.getActiveState() != 0) {
                int petIdx = chr.getPets().stream().filter(pet -> pet.getPetLockerSN() == SN).findAny().orElse(null).getIdx();
                chr.getField().broadcast(PetPacket.nameChanged(chr.getId(), petIdx, newName));
            }
            chr.consumeItem(itemID, 1);
            chr.dispose();

        } else if (ItemConstants.isPetFood(itemID)) {

            for (Pet pet : chr.getPets()) {
                if (!pet.canConsume(itemID)) {
                    PetItem pi = pet.getItem();
                    pi.setRepleteness((byte) 100);
                    if (pi.getTameness() < 30000) {
                        pi.setTameness((short) Math.min(pi.getTameness() + 100, 30000));
                        if (pi.getTameness() >= GameConstants.petExp[pi.getLevel() + 1]) {
                            pi.setLevel((byte) (pi.getLevel() + 1));
                            chr.write(UserPacket.effect(Effect.pet((byte) 0, pet.getIdx())));
                        }
                    }
                    chr.getField().broadcast(PetPacket.actionCommand_GiveFood(chr.getId(), pet.getIdx(), 2));
                    var effect = Effect.pet((byte) 0, pet.getIdx());
                    chr.write(UserPacket.effect(effect));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
                }
            }
            chr.consumeItem(itemID, 1);
            chr.dispose();

        } else if (ItemConstants.isViciousHammerItem(itemID)) {

            int delay = 2700; // 2.7 sec delay to match golden hammer's animation
            inPacket.decodeInt(); // use hammer? useless though
            int ePos = inPacket.decodeInt();
            chr.getTimer().addEvent(() -> {
                Equip equip = (Equip) chr.getEquipInventory().getItemBySlot((short) ePos);
                if (equip == null || !ItemConstants.canEquipGoldHammer(equip)) {
                    chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Error, 1, 0));
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to use hammer (id %d) on an invalid equip (id %d)", chr.getId(), item == null ? 0 : item.getItemId(), equip == null ? 0 : equip.getItemId()));
                    return;
                }
                if (equip.getIuc() > ItemConstants.MAX_HAMMER_SLOTS) {
                    chr.write(WvsContext.goldHammerItemUpgradeResult(GoldHammerResult.Error, 2, 0));
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to use hammer (id %d) an invalid equip (id %d)", chr.getId(), itemID, equip.getItemId()));
                    return;
                }
                equip.addStat(iuc, 1); // +1 hammer used
                equip.addStat(tuc, 1); // +1 upgrades available
                equip.updateToChar(chr);
                chr.chatPopup("成功使用黃金鐵槌。");
                chr.consumeItem(itemID, 1);
                chr.write(FieldPacket.closeUI(UIType.UI_ITEMREPLACE));
            }, delay);

        } else if (ItemConstants.isItemTagItem(itemID)) {

            short ePos = inPacket.decodeShort();
            Equip equip = (Equip) chr.getEquippedInventory().getItemBySlot(ePos);
            if (equip == null) {
                chr.chatMessage(SystemNotice, "發生未知錯誤。");
                chr.dispose();
                return;
            }
            equip.setOwner(chr.getName());
            equip.updateToChar(chr);
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isItemGuardItem(itemID)) {

            byte itemType = (byte) inPacket.decodeInt();
            Inventory equipInv = chr.getInventoryByType(InvType.getInvTypeByVal(itemType));
            int ePos = inPacket.decodeInt();
            Equip eq = (Equip) equipInv.getItemBySlot(ePos);
            if (eq == null) {
                chr.chatMessage(SystemNotice, "該道具只能在已裝備時使用，或未找到裝備。");
                chr.dispose();
                return;
            }
            if (eq.getDateExpire().isExpired()) {
                chr.chatMessage(SystemNotice, "該道具只能用於未過期的裝備。");
                chr.dispose();
                return;
            }
            eq.addAttribute(EquipAttribute.Locked);
            long period = 0;
            if (itemID == 5061000) {
                period = 7;
            } else if (itemID == 5061001) {
                period = 30;
            } else if (itemID == 5061002) {
                period = 90;
            } else if (itemID == 5061003) {
                period = 365;
            }
            if (period > 0) {
                eq.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(period)));
            }
            eq.updateToChar(chr);
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isKarmaItem(itemID)) {

            int equipID = inPacket.decodeInt(); // success?
            short equipSlot = (short) inPacket.decodeInt();
            useKarma(chr, pos, itemID, equipSlot, true);

        } else if (ItemConstants.isMiuMiuMerchant(itemID)) {

            chr.getScriptManager().openShop(GameConstants.GENERAL_SHOP);

        } else if (ItemConstants.isPortableStorage(itemID)) {

            chr.getScriptManager().openTrunk(1022005);

        } else if (ItemConstants.isUpgradeAssistScroll(itemID)) {

            inPacket.decodeShort(); //Eqp Position
            short ePos = inPacket.decodeShort(); //Eqp Position
            byte bEnchantSkill = inPacket.decodeByte(); //no clue what this means exactly
            InvType invType = ePos < 0 ? EQUIPPED : EQUIP;
            Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
            if (item == null || equip == null) {
                chr.chatMessage(SystemNotice, "發生未知錯誤。");
                chr.dispose();
                return;
            }
            int scrollID = item.getItemId();
            Map<ScrollStat, Integer> vals = ItemData.getItemInfoByID(scrollID).getScrollStats();
            if (vals.containsKey(ScrollStat.maxSuperiorEqp)) {
                if (equip.isSuperiorEqp()) {
                    if (equip.getCuc() < 7) {
                        equip.addAttribute(EquipAttribute.ProtectionScroll);
                    } else {
                        chr.chatMessage(SystemNotice, "無法用於已強化 7 次以上的道具。");
                        chr.dispose();
                        return;
                    }
                } else {
                    chr.chatMessage(SystemNotice, "只能用於高級道具。");
                    chr.dispose();
                    return;
                }
            }
            else if ((scrollID >= 5064000 && scrollID <= 5064004 && scrollID != 5064003) || scrollID == 5068100) {
                if (scrollID == 5064002) {
                    if (equip.getRequiredLevel() > 105) {
                        chr.chatMessage(SystemNotice, "請在需求等級 105 以下的裝備上使用該功能。");
                        chr.dispose();
                        return;
                    }
                }
                equip.addAttribute(EquipAttribute.ProtectionScroll);
            }
            else if (vals.containsKey(ScrollStat.reset)) {
                if (scrollID == 5064201) { 
                    // Resets all stats to their original values (Potential excluded)
                    if (equip.getRequiredLevel() > 105) {
                        chr.chatMessage(SystemNotice, "請在需求等級 105 以下的裝備上使用該功能。");
                        chr.dispose();
                        return;
                    }
                    equip.reset(false);
                } else {
                    equip.reset(true);
                }
            }
            else if (scrollID == 5064100 || scrollID == 5064101) {
                if (scrollID == 5064101) {
                    if (equip.getRequiredLevel() > 105) {
                        chr.chatMessage(SystemNotice, "請在需求等級 105 以下的裝備上使用該功能。");
                        chr.dispose();
                        return;
                    }
                }
                equip.addAttribute(EquipAttribute.UpgradeCountProtection);
            }
            else if (scrollID == 5064300 || scrollID == 5064301 || scrollID == 5068200) { // Guardian
                if (scrollID == 5064301) {
                    if (equip.getRequiredLevel() > 105) {
                        chr.chatMessage(SystemNotice, "請在需求等級 105 以下的裝備上使用該功能。");
                        chr.dispose();
                        return;
                    }
                }
                equip.addAttribute(EquipAttribute.ScrollProtection);
            }
            else if (scrollID == ReturnEffectInfo.RETURN_SCROLL) {
                equip.addAttribute(EquipAttribute.ReturnScroll);
                chr.write(FieldPacket.showScrollVestigeCompensationResult(true));
            }
            else if (scrollID == 5063000 || scrollID == 5063100 || scrollID == 5068000) {
                if (scrollID == 5063100) {
                    equip.addAttribute(EquipAttribute.ProtectionScroll);
                }
                equip.addAttribute(EquipAttribute.LuckyDay);
            }
            chr.write(UserLocal.hyperEnchantScrollRegister(true, scrollID, equip.getItemId()));
            equip.updateToChar(chr);
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isFusionAnvil(itemID)) {

            int appearancePos = inPacket.decodeInt();
            int functionPos = inPacket.decodeInt();
            Inventory inv = chr.getEquipInventory();
            Equip appearance = (Equip) inv.getItemBySlot((short) appearancePos);
            Equip function = (Equip) inv.getItemBySlot((short) functionPos);
            if (appearance == null || function == null || ItemConstants.getItemPrefix(appearance.getItemId()) != ItemConstants.getItemPrefix(function.getItemId())) {
                System.out.println("[" + chr.getName() + "] Unknown fusion anvil item or mismatching use fusion anvil items.");
                chr.dispose();
                return;
            }
            function.getOptions().set(6, appearance.getItemId() % 10000);
            function.updateToChar(chr);
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isCube(itemID)) {

            if (itemID == ItemConstants.VIOLET_CUBE) {
                CubeHandler.useVioletCube(chr, inPacket);
            } else {
                if (itemID == ItemConstants.GLOWING_CUBE) {
                    CubeHandler.handleCube(chr, item, 0, inPacket, ItemGrade.Legendary, false);
                } else if (itemID == ItemConstants.BRIGHT_CUBE) {
                    CubeHandler.handleBrightCube(chr, item, inPacket, ItemGrade.Legendary, false, true);
                } else {
                    chr.chatMessage("該道具已從遊戲中移除，GM 將回收該道具。");
                    chr.dispose();
                }
            }

        } else if (ItemConstants.isSpecialCube(itemID)) {

            // 5062100 : 8th Anniversary Miracle Cube
            // 5062103 : Octacular Miracle Cube
            // 5062101 : Tot's Trial Miracle Cube
            chr.chatMessage("該道具已從遊戲中移除，GM 將回收該道具。");
            chr.dispose();

        } else if (ItemConstants.isBonusCube(itemID)) {

            if (itemID == ItemConstants.BONUS_BRIGHT_CUBE_1 || itemID == ItemConstants.BONUS_BRIGHT_CUBE_2) {
                CubeHandler.handleBrightCube(chr, item, inPacket, ItemGrade.Legendary, true, true);
            } else if (itemID == ItemConstants.BONUS_GLOWING_CUBE) {
                CubeHandler.handleCube(chr, item, 0, inPacket, ItemGrade.Legendary, true);
            } else {
                // 5062500 : Bonus Potential Cube
                // 5062501 : [Special] Bonus Potential Cube
                chr.chatMessage("該道具已從遊戲中移除，GM 將回收該道具。");
                chr.dispose();
            }

        } else if (ItemConstants.isNebuliteDiffuser(itemID)) {

            chr.chatMessage("該道具已從遊戲中移除，GM 將回收該道具。");
            chr.consumeItem(item);

        } else if (itemID == ItemConstants.HYPER_TELEPORT_ROCK) {

            byte useWorld = inPacket.decodeByte();
            byte type = inPacket.decodeByte();
            if (useWorld == 1) {
                int fieldId = inPacket.decodeInt();
                int portalId = inPacket.decodeInt();
                Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldId);
                if (field == null || (field.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) > 0 || !FieldData.getWorldMapFields().contains(fieldId)) {
                    chr.chatMessage("你無法瞬間移動到該地圖。");
                    chr.dispose();
                    return;
                }
                if (chr.getLevel() < field.getLvLimit()) {
                    chr.chatMessage("你的等級不足，無法移動到該地圖。");
                    chr.dispose();
                    return;
                }
                chr.warp(fieldId, portalId);
            } else {
                if (type == 0) {
                    int fieldId = inPacket.decodeInt();
                    Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldId);
                    if (field == null || (field.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) > 0 || !FieldData.getWorldMapFields().contains(fieldId)) {
                        chr.chatMessage("你無法瞬間移動到該地圖。");
                        chr.dispose();
                        return;
                    }
                    if (chr.getLevel() < field.getLvLimit()) {
                        chr.chatMessage("你的等級不足，無法移動到該地圖。");
                        chr.dispose();
                        return;
                    }
                    chr.warp(field);
                } else if (type == 1) {
                    String targetName = inPacket.decodeString();
                    Char targetChr = Server.get().getWorld().getCharByName(targetName);
                    if (targetChr == null) {
                        chr.chatMessage(String.format("%s 當前離線或不存在。", targetName));
                        chr.dispose();
                        return;
                    }

                    Position targetPosition = targetChr.getPosition();

                    Field targetField = targetChr.getField();
                    if (targetField == null || (targetField.getFieldLimit() & FieldOption.TeleportItemLimit.getVal()) > 0) {
                        chr.chatMessage("你無法瞬間移動到該地圖。");
                        chr.dispose();
                        return;
                    }
                    if (chr.getLevel() < targetField.getLvLimit()) {
                        chr.chatMessage("你的等級不足，無法移動到該地圖。");
                        chr.dispose();
                        return;
                    }
                    // Target is in an instanced Map
                    if (targetChr.getFieldInstanceType() != FieldInstanceType.CHANNEL) {
                        chr.chatMessage(String.format("角色 %s 當前在隱藏地圖中。", targetName));
                        // Change channels & warp & teleport
                    } else if (targetChr.getClient().getChannel() != chr.getClient().getChannel()) {
                        int fieldId = targetChr.getFieldID();
                        chr.changeChannelAndWarp(targetChr.getClient().getChannel(), fieldId);
                        // warp & teleport
                    } else if (targetChr.getFieldID() != chr.getFieldID()) {
                        chr.warp(targetField);
                        chr.write(FieldPacket.teleport(targetPosition, chr));
                        // teleport
                    } else {
                        chr.write(FieldPacket.teleport(targetPosition, chr));
                    }
                }
            }

        } else if (ItemConstants.isMegaphoneItem(itemID)) {

            if (itemID == 5071000 && chr.getLevel() < 10) {
                chr.chatMessage(SystemNotice, "僅限等級 10 以上的角色使用。");
                chr.dispose();
                return;
            }
            String text = inPacket.decodeString();
            BroadcastMsg mega = BroadcastMsg.megaphone(String.format("%s : %s", chr.getName(), text), (byte) chr.getClient().getChannelInstance().getChannelId(), false, chr);
            chr.getField().broadcast(WvsContext.broadcastMsg(mega));
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isSpecialMegaphoneItem(itemID)) {

            // TMS 282 wears the medal in 21 (Tms282BodySlots.MEDAL, 265 Medal_OLD); 265 in Medal 49
            Equip medal = (Equip) chr.getEquippedInventory().getFirstItemByBodyPart(
                    net.swordie.ms.connection.protocol.Protocol.active().version()
                            == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 ? BodyPart.Medal_OLD : BodyPart.Medal);
            int medalInt = 0;
            if (medal != null) {
                medalInt = (medal.getAnvilId() == 0 ? medal.getItemId() : medal.getAnvilId());
            }
            String medalString = (medalInt == 0 ? "" : String.format("<%s> ", StringData.getItemStringById(medalInt)));
            if (itemID == 5072000 || itemID == 5072001) { // Super Megaphone
                if (chr.getLevel() < 10) {
                    chr.chatMessage(SystemNotice, "僅限等級 10 以上的角色使用。");
                    chr.dispose();
                    return;
                }
                String text = inPacket.decodeString();
                boolean whisperIcon = inPacket.decodeByte() != 0;
                BroadcastMsg smega = BroadcastMsg.megaphone(String.format("%s%s : %s", medalString, chr.getName(), text), (byte) chr.getClient().getChannelInstance().getChannelId(), whisperIcon, chr);
                Server.get().broadcastForWorld(WvsContext.broadcastMsg(smega));
            } else if (itemID == 5076000 || itemID == 5076100) { // Item Megaphone
                String text = inPacket.decodeString();
                boolean whisperIcon = inPacket.decodeByte() != 0;
                boolean eqpSelected = inPacket.decodeByte() != 0;
                if (inPacket.getUnreadAmount() < 1) {
                    BroadcastMsg smega = BroadcastMsg.itemMegaphoneNoItem(String.format("%s%s : %s", medalString, chr.getName(), text), (byte) chr.getClient().getChannelInstance().getChannelId());
                    Server.get().broadcastForWorld(WvsContext.broadcastMsg(smega));
                } else {
                    InvType invType = EQUIP;
                    int itemPosition = 0;
                    if (eqpSelected) {
                        invType = InvType.getInvTypeByVal(inPacket.decodeInt());
                        itemPosition = inPacket.decodeInt();
                        if (invType == EQUIP && itemPosition < 0) {
                            invType = EQUIPPED;
                        }
                    }
                    Item broadcastedItem = chr.getInventoryByType(invType).getItemBySlot((short) itemPosition);
                    BroadcastMsg smega = BroadcastMsg.itemMegaphone(String.format("%s%s : %s", medalString, chr.getName(), text), (byte) chr.getClient().getChannelInstance().getChannelId(), whisperIcon, eqpSelected, broadcastedItem, chr);
                    Server.get().broadcastForWorld(WvsContext.broadcastMsg(smega));
                }
            } else if (itemID == 5077000) {
                byte stringListSize = inPacket.decodeByte();
                List<String> stringList = new ArrayList<>();
                for (int i = 0; i < stringListSize; i++) {
                    stringList.add(String.format("%s%s : %s", medalString, chr.getName(), inPacket.decodeString()));
                }
                boolean whisperIcon = inPacket.decodeByte() != 0;
                BroadcastMsg smega = BroadcastMsg.tripleMegaphone(stringList, (byte) chr.getClient().getChannelInstance().getChannelId(), whisperIcon, chr);
                Server.get().broadcastForWorld(WvsContext.broadcastMsg(smega));
            }
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isSpecialConsumeEffectItem(itemID)) {

            if (Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).anyMatch(m -> chr.getField() != null && m == chr.getField().getId())) {
                chr.chatMessage("當前地圖無法使用該物品。");
                chr.dispose();
                return;
            }
            if (itemInfo != null && itemInfo.getStateChangeItem() != 0) {
                Item statChangeItem = ItemData.getItemDeepCopy(itemInfo.getStateChangeItem());
                Rect rect = new Rect(new Position(-110, -82), new Position(110, 83));
                for (Char player : chr.getField().getChars()) {
                    if (chr.getRectAround(rect).hasPositionInside(player.getPosition())) {
                        player.useStatChangeItem(statChangeItem, false);
                        player.write(UserLocal.randomEmotion(itemID));
                    }
                }
            }
            chr.write(UserPacket.effect(Effect.consumeEffect(itemID)));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.consumeEffect(itemID)), chr);
            chr.consumeItem(itemID, 1);

        } else if (ItemConstants.isMaplePointsCouponItem(itemID)) {

            ScriptManagerImpl sm = chr.getScriptManager();
            if (itemInfo != null) {
                chr.addMaplePoint(itemInfo.getMaplepoint());
            }
            sm.sendOK(String.format("獲得了 #b#e%d 楓點#n#k（來自 #z%d#）！", itemInfo.getMaplepoint(), itemID), 9010000);
            chr.consumeItem(itemID, 1);

        } else if (itemID >= 5620000 && itemID <= 5620008) { //TODO: Make Item Type but nah lazy

            ScriptManagerImpl sm = chr.getScriptManager();
            int skillID = 0;
            if (itemInfo != null) {
                skillID = itemInfo.getSkills().stream().toList().getFirst();
            }
            int reqLevel = itemInfo.getReqSkillLv();
            int masterLevel = itemInfo.getMasterLv();
            if (skillID == 4341007) {
                skillID = 4340007; //Nexon Bug LOL.
                masterLevel = 20;
            }
            int currentLevel = chr.getSkillLevel(skillID);
            if (chr.getMasterySkillLevel(skillID) != masterLevel) {
                if (chr.getSkillLevel(skillID) >= reqLevel) {
                    chr.setMasterySkillLevel(skillID, masterLevel);
                    sm.consumeItem(itemID);
                } else {
                    chr.chatMessage(SystemNotice, String.format("技能等級為 %d，需要 %d。", currentLevel, reqLevel));
                }
            } else {
                chr.chatMessage(SystemNotice, "已經學會這個技能了。");
            }

        } else if (itemID == 5552000) { // Beauty Salon Face Slot Coupon

            BeautySalon beautySalon = chr.getBeautySalon();
            int currentSize = beautySalon.getFaceSize();
            if (currentSize == 100) {
                chr.chatPopup("美容沙龍的臉型欄位已達到上限（100/100）。");
            } else {
                int newSlots = currentSize + 1;
                beautySalon.setFaceSize(newSlots);
                beautySalon.saveToSQL();
                chr.consumeItem(itemID, 1);
                chr.write(UserLocal.beautyDataResult(chr.getBeautySalon()));
            }

        } else if (itemID == 5553000) { // Beauty Salon Hair Slot Coupon

            BeautySalon beautySalon = chr.getBeautySalon();
            int currentSize = beautySalon.getHairSize();
            if (currentSize == 100) {
                chr.chatPopup("美容沙龍的髮型欄位已達到上限（100/100）。");
            } else {
                int newSlots = currentSize + 1;
                beautySalon.setHairSize(newSlots);
                beautySalon.saveToSQL();
                chr.consumeItem(itemID, 1);
                chr.write(UserLocal.beautyDataResult(chr.getBeautySalon()));
            }

        } else if (itemID == 5554000) { // Beauty Salon Skin Slot Coupon

            BeautySalon beautySalon = chr.getBeautySalon();
            int currentSize = beautySalon.getHairSize();
            if (currentSize == 100) {
                chr.chatPopup("美容沙龍的皮膚欄位已達到上限（100/100）。");
            } else {
                int newSlots = currentSize + 1;
                beautySalon.setSkinSize(newSlots);
                beautySalon.saveToSQL();
                chr.consumeItem(itemID, 1);
                chr.write(UserLocal.beautyDataResult(chr.getBeautySalon()));
            }

        } else {
            if (!ItemConstants.isLockedCashConsumeItem(itemID)) {
                System.out.printf("Cash item %d is not implemented.%n", itemID);
                chr.chatMessage(SystemNotice, String.format("物品 %d 尚未開放，請聯繫開發者。", itemID));
            }

        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_STAT_CHANGE_ITEM_USE_REQUEST)
    public static void handleUserStatChangeItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        Item item = chr.getConsumeInventory().getItemBySlot(slot);
        if (item == null || item.getItemId() != itemID) {
            chr.dispose();
            return;
        }
        if (itemID == EXPAND_MENU_ITEM_ID) {
            startExpandMenuItem(chr);
            return;
        }
        // TMS282 CP0148 boxes (2028xxx with WZ reward/, research tables, 265 pools): opened before the stat-change
        // path, which would consume them without effect. The 282 route diffs CONSUME and stats; the other bags here.
        if (!isExpiredItem(item) && net.swordie.ms.world.tms282.boxes.Tms282BoxUse.tryUse(chr, item,
                net.swordie.ms.world.tms282.boxes.Tms282BoxUse.Replies.OTHER_THAN_CONSUME)) {
            return;
        }
        handleStatChangeConsumeItem(chr, item, itemID, true);
    }

    private static boolean isExpiredItem(Item item) {
        return item.getDateExpire() != null && item.getDateExpire().isExpired();
    }

    @Handler(op = InHeader.USER_STAT_CHANGE_BY_PORTABLE_CHAIR_REQUEST)
    public static void handleUserStatChangePortalChairRequest(Char chr, InPacket inPacket) {
        ItemInfo ii = ItemData.getItemInfoByID(chr.getChair().getItemID());
        if (ii != null) {
            chr.heal(ii.getRecoveryHP());
            chr.healMP(ii.getRecoveryMP());
        } else {
            chr.heal(50);
            chr.healMP(50);
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_SCRIPT_ITEM_USE_REQUEST)
    public static void handleUserScriptItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        int quant = inPacket.decodeInt();
        Item item = chr.getConsumeInventory().getItemBySlot(slot);
        if (item == null || item.getItemId() != itemID) {
            item = chr.getCashInventory().getItemBySlot(slot);
        }
        if (item == null || item.getItemId() != itemID) {
            chr.dispose();
            return;
        }
        // e.g. 5680946 (spec script), exchange coupons, choice boxes; the 282 route only unlocks, so every bag here
        if (!isExpiredItem(item) && net.swordie.ms.world.tms282.boxes.Tms282BoxUse.tryUse(chr, item,
                net.swordie.ms.world.tms282.boxes.Tms282BoxUse.Replies.ALL_BAGS)) {
            return;
        }
        if (ItemConstants.isChangeStatItem(itemID) && !hasScriptItem(itemID)) {
            handleStatChangeConsumeItem(chr, item, itemID, false);
            return;
        }
        if (itemID == EXPAND_MENU_ITEM_ID) {
            startExpandMenuItem(chr);
            return;
        }
        if (!hasScriptItem(itemID)) {
            // 282 item without script: apply what its item data defines (ScriptManagerImpl.start covers the script-without-file case)
            if (net.swordie.ms.world.tms282.items.ItemDataEffects.tryApply(chr, itemID)) {
                chr.dispose();
                return;
            }
            chr.chatMessage(SystemNotice, String.format("物品 %d 尚未開放，請聯繫開發者。", itemID));
            chr.dispose();
            return;
        }
        startScriptItem(chr, itemID);
    }

    @Handler(op = InHeader.USER_EQUIPMENT_ENCHANT_WITH_SINGLE_UI_REQUEST)
    public static void handleUserEquipmentEnchantWithSingleUIRequest(Char chr, InPacket inPacket) {
        byte equipmentEnchantType = inPacket.decodeByte();
        EquipmentEnchantType eeType = EquipmentEnchantType.getByVal(equipmentEnchantType);
        if (eeType == null) {
            System.out.printf("Unknown enchant UI request %d%n", equipmentEnchantType);
            chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
            return;
        }
        int vipGrade = chr.getUser().getVipGrade();
        FileTime vipGradeExpiredDate = chr.getUser().getVipExpiredDate();
        Equip equip;
        Equip zeroWeapon = null;
        boolean feverTime = EventConstants.STAR_FORCE_FEVER_TIME_EVENT // GM toggle
                || net.swordie.ms.world.event.SunnySundayBonuses.isActive(net.swordie.ms.world.event.SunnySundayBonuses.Bonus.SPELL_TRACE_FEVER);
        switch (eeType) {
            //region Scroll Upgrade
            case ScrollUpgradeRequest:
                inPacket.decodeInt();// tick
                short pos = inPacket.decodeShort();
                int scrollID = inPacket.decodeInt();
                Inventory inv = pos < 0 ? chr.getEquippedInventory() : chr.getEquipInventory();
                if (inv == null) {
                    return;
                }
                pos = (short) Math.abs(pos);
                equip = (Equip) inv.getItemBySlot(pos);
                if (equip == null) {
                    chr.write(FieldPacket.closeUI(201));
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to enchant a non-scrollable equip (pos %d, itemid %d).", chr.getId(), pos, 0));
                    chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
                    return;
                }
                Equip prevEquip = equip.deepCopy();
                if (equip.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
                    chr.write(FieldPacket.closeUI(201));
                }
                if (ItemConstants.isLongSword(equip.getItemId())) {
                    zeroWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-10);
                } else if (ItemConstants.isBigSword(equip.getItemId())) {
                    zeroWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-11);
                }
                List<ScrollUpgradeInfo> suis = ItemConstants.getScrollUpgradeInfosByEquip(chr, equip, feverTime);
                if (scrollID < 0 || scrollID >= suis.size()) {
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to spell trace scroll with an invalid scroll ID (%d, itemID %d).", chr.getId(), scrollID, equip.getItemId()));
                    chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
                    return;
                }
                ScrollUpgradeInfo sui = suis.get(scrollID);
                if (sui.getType() == SpellTraceScrollType.CleanSlate) {
                    if (!equip.isLastScrollFail()) {
                        chr.chatMessage(SystemNotice, "只能用於曾經強化失敗的裝備。");
                        chr.write(FieldPacket.showScrollUpgradeResult(feverTime, 0, "只能用於曾經強化失敗的裝備。", prevEquip, equip));
                        chr.dispose();
                        return;
                    }
                }
                int spellTraceCost = sui.getCost();
                if (equip.getItemId() == 1122444 || equip.getItemId() == 1122445) {
                    // Spiegelmann's Ordinary Necklace | Spiegelmann's Sparkling Necklace
                    spellTraceCost = 0;
                }
                chr.consumeItem(ItemConstants.SPELL_TRACE_ID, spellTraceCost);

                boolean success = sui.applyTo(chr, sui.getType() == SpellTraceScrollType.Innocence && sui.getIconID() == 6, equip, zeroWeapon);
                equip.recalcEnchantmentStats();
                if (zeroWeapon != null) {
                    zeroWeapon.recalcEnchantmentStats();
                    zeroWeapon.updateToChar(chr);
                }
                String desc = success ? "你的物品已強化成功。" : "強化失敗。";
                chr.write(FieldPacket.showScrollUpgradeResult(feverTime, success ? 1 : 0, desc, prevEquip, equip));
                equip.updateToChar(chr);
                if (equip.getBaseStat(tuc) > 0) {
                    suis = ItemConstants.getScrollUpgradeInfosByEquip(chr, equip, feverTime);
                    chr.write(FieldPacket.scrollUpgradeDisplay(feverTime, suis));
                } else {
                    suis = ItemConstants.getScrollUpgradeInfosForFullSlotsEquip(chr, equip);
                    chr.write(FieldPacket.scrollUpgradeDisplay(feverTime, suis));
                }
                AchievementHandler.handleSpellTrace(chr, sui);
                break;
            //endregion
            //region Star Force
            case HyperUpgradeResult:
                inPacket.decodeInt(); //tick
                int eqpPos = inPacket.decodeInt();
                boolean extraChanceFromMiniGame = inPacket.decodeByte() != 0;
                InvType invType = eqpPos < 0 ? EQUIPPED : EQUIP;
                equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(eqpPos);
                if (extraChanceFromMiniGame) {
                    inPacket.decodeInt();
                }
                inPacket.decodeInt();
                inPacket.decodeInt();
                inPacket.decodeByte(); // 0?
                boolean safeGuardFromUI = inPacket.decodeByte() != 0;
                inPacket.decodeByte(); // 1 ?
                boolean safeGuard = equip.hasAttribute(EquipAttribute.ProtectionScroll);

                if (equip == null) {
                    chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
                    return;
                }
                if (ItemConstants.isLongSword(equip.getItemId())) {
                    zeroWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-10);
                } else if (ItemConstants.isBigSword(equip.getItemId())) {
                    zeroWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-11);
                }
                if (!ItemConstants.isUpgradable(equip.getItemId())
                        || chr.getEquipInventory().getEmptySlots() == 0
                        || equip.getChuc() >= GameConstants.getMaxStars(equip)
                        || equip.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
                    chr.chatPopup("無法強化該裝備。");
                    chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
                    return;
                }
                long cost = GameConstants.getEnchantmentMesoCost(equip.getrLevel() + equip.getiIncReq(), equip.getChuc(), equip.isSuperiorEqp());
                if (equip.getItemId() == 1122444 || equip.getItemId() == 1122445) {
                    // Spiegelmann's Ordinary Necklace | Spiegelmann's Sparkling Necklace
                    cost = 0;
                }
                cost *= safeGuardFromUI ? 3 : 1;
                if (chr.getMoney() < cost) {
                    chr.chatPopup("你需要足夠的 " + Util.getNumberFormat(cost) + " 楓幣才能強化。");
                    chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
                    return;
                }
                Equip oldEquip = equip.deepCopy();
                GameConstants.EnchantResult res = GameConstants.rollEnchant(
                        equip.getChuc(),
                        vipGrade,
                        extraChanceFromMiniGame,
                        safeGuard,
                        safeGuardFromUI,
                        equip.canSafeguardHyperUpgrade());

                boolean upgrade = res == GameConstants.EnchantResult.SUCCESS;
                boolean boom    = res == GameConstants.EnchantResult.DESTROY;

                short oldChuc = equip.getChuc();
                short newChuc = oldChuc;

                if (upgrade) {
                    newChuc = (short) (oldChuc + 1);
                    equip.setChuc(newChuc, true);
                    equip.setDropStreak(0);
                    Equip.notifyUnionChuc(chr, equip, oldChuc, newChuc);
                    if (EventConstants.HYPER_BURNING_MAX && equip.getItemId() == 1122444) {
                        if (newChuc == 8) {
                            chr.getScriptManager().startScript(chr, 102442, "q102442s_2", ScriptType.Quest);
                        } else {
                            chr.createQuestWithQRValue(102442, "step=4;curStar="+ newChuc);
                        }
                    }
                } else if (boom) {
                    if (chr.getGuild() != null) {
                        GuildSkill guildSkill = chr.getGuild().getSkillById(GuildConstants.ITEM_SALVATION);
                        SkillInfo skillInfo = SkillData.getSkillInfoById(GuildConstants.ITEM_SALVATION);
                        if (guildSkill != null && skillInfo != null) {
                            int chanceNoBoom = skillInfo.getValue(SkillStat.itemCursedProtectR, guildSkill.getLevel());
                            chr.chatMessage("[公會技能] 強化時你將獲得 " + chanceNoBoom + "% 的裝備破壞率降低效果。");
                            if (Util.succeedProp(chanceNoBoom)) {
                                chr.sendPopupSay("[公會技能] 你的裝備因 Item Salvation 技能而沒有被破壞。");
                                boom = false;
                            }
                        }
                    }
                    if (boom) {
                        if (!ItemConstants.isLongOrBigSword(equip.getItemId())) {
                            equip.addSpecialAttribute(EquipSpecialAttribute.Vestige);
                            if (invType == EQUIPPED) {
                                equip.setInventoryID(chr.getEquipInventory().getId());
                                equip.setInvType(InvType.EQUIP);
                                chr.unequip(equip, chr.getEquipInventory().getFirstOpenSlot());
                                equip.updateToChar(chr);
                                chr.write(WvsContext.inventoryOperation(true, false, Move,
                                        (short) eqpPos, (short) equip.getBagIndex(), 0, equip));
                            }
                            if (!equip.isSuperiorEqp()) {
                                newChuc = (short) Math.min(30, oldChuc);
                                equip.setChuc(newChuc, true);
                            } else {
                                equip.setChuc((short) 0, true);
                            }
                            Equip.notifyUnionChuc(chr, equip, oldChuc, newChuc);
                        } else {
                            Equip vestigeEquip = equip.deepCopy();
                            vestigeEquip.addSpecialAttribute(EquipSpecialAttribute.Vestige);
                            vestigeEquip.setInvType(EQUIP);
                            if (!vestigeEquip.isSuperiorEqp()) {
                                newChuc = (short) Math.min(30, oldChuc);
                                vestigeEquip.setChuc(newChuc, true);
                            } else {
                                vestigeEquip.setChuc((short) 0, true);
                            }
                            Equip.notifyUnionChuc(chr, vestigeEquip, oldChuc, newChuc);
                            chr.addItemToInventory(vestigeEquip);
                            chr.getScriptManager().startScript(chr, 2400009, "ZeroRefund", ScriptType.Npc);
                        }
                    }
                }
                if (zeroWeapon != null) {
                    zeroWeapon.setChuc(equip.getChuc(), true);
                    zeroWeapon.recalcEnchantmentStats();
                    zeroWeapon.updateToChar(chr);
                }
                chr.deductMoney(cost);
                equip.recalcEnchantmentStats();
                oldEquip.recalcEnchantmentStats();
                if (!safeGuardFromUI) {
                    equip.removeAttribute(EquipAttribute.ProtectionScroll);
                    if (zeroWeapon != null) {
                        zeroWeapon.removeAttribute(EquipAttribute.ProtectionScroll);
                    }
                }
                equip.updateToChar(chr);
                if (zeroWeapon != null) {
                    zeroWeapon.updateToChar(chr);
                }
                chr.write(FieldPacket.showUpgradeResult(upgrade, boom));
                AchievementHandler.handleStarForce(chr, cost, newChuc, upgrade, boom, extraChanceFromMiniGame);
                chr.dispose();
                break;
            //endregion
            //region IDK
            case TransmissionResult:
                inPacket.decodeInt(); // tick
                short toPos = inPacket.decodeShort();
                short fromPos = inPacket.decodeShort();

                InvType invTypeToPos = toPos < 0 ? EQUIPPED : EQUIP;
                InvType invTypeFromPos = fromPos < 0 ? EQUIPPED : EQUIP;

                Equip fromEq = (Equip) chr.getInventoryByType(invTypeFromPos).getItemBySlot(fromPos);
                Equip toEq = (Equip) chr.getInventoryByType(invTypeToPos).getItemBySlot(toPos);

                if (fromEq == null || toEq == null || fromEq.getItemId() != toEq.getItemId() || !fromEq.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
                    System.out.printf("Equip transmission failed: from = %s, to = %s%n", fromEq, toEq);
                    chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
                    return;
                }
                if (ItemConstants.isLongSword(toEq.getItemId())) {
                    zeroWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-10);
                } else if (ItemConstants.isBigSword(toEq.getItemId())) {
                    zeroWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(-11);
                }
                if (ItemConstants.isLongOrBigSword(fromEq.getItemId())) {
                    toEq.setScrollStatForTransmission(fromEq);
                    toEq.updateToChar(chr);
                    if (zeroWeapon != null) {
                        zeroWeapon.setScrollStatForTransmission(fromEq);
                        zeroWeapon.updateToChar(chr);
                    }
                    chr.consumeItem(fromEq);
                } else {
                    fromEq.removeSpecialAttribute(EquipSpecialAttribute.Vestige);
                    chr.consumeItem(toEq);
                    fromEq.updateToChar(chr);
                }
                chr.write(FieldPacket.showTranmissionResult(fromEq, toEq));
                chr.dispose();
                break;
            //endregion
            //region Scroll Upgrade Display
            case ScrollUpgradeDisplay:
                int ePos = inPacket.decodeInt();
                inv = ePos < 0 ? chr.getEquippedInventory() : chr.getEquipInventory();
                ePos = Math.abs(ePos);
                equip = (Equip) inv.getItemBySlot(ePos);
                if (equip == null || !ItemConstants.isUpgradable(equip.getItemId())) {
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to scroll a non-scrollable equip (pos %d, itemid %d).", chr.getId(), ePos, equip == null ? 0 : equip.getItemId()));
                    chr.dispose();
                    return;
                } else if (equip.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
                    chr.write(FieldPacket.closeUI(201));
                }
                if (equip.getBaseStat(tuc) <= 0) {
                    suis = ItemConstants.getScrollUpgradeInfosForFullSlotsEquip(chr, equip);
                    chr.write(FieldPacket.scrollUpgradeDisplay(feverTime, suis));
                } else {
                    suis = ItemConstants.getScrollUpgradeInfosByEquip(chr, equip, feverTime);
                    chr.write(FieldPacket.scrollUpgradeDisplay(feverTime, suis));
                }
                break;
            //endregion
            //region Star Force Display
            case HyperUpgradeDisplay:
                ePos = inPacket.decodeInt();
                safeGuardFromUI = inPacket.decodeByte() != 0;
                inPacket.decodeByte();
                inv = ePos < 0 ? chr.getEquippedInventory() : chr.getEquipInventory();
                ePos = Math.abs(ePos);
                equip = (Equip) inv.getItemBySlot(ePos);
                if (equip == null || !ItemConstants.isUpgradable(equip.getItemId())) {
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %d tried to enchant a non-enchantable equip (pos %d, itemid %d).", chr.getId(), ePos, equip == null ? 0 : equip.getItemId()));
                    chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
                    return;
                } else if (equip.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
                    chr.write(FieldPacket.closeUI(201));
                }
                cost = GameConstants.getEnchantmentMesoCost(equip.getrLevel() + equip.getiIncReq(), equip.getChuc(), equip.isSuperiorEqp());
                if (equip.getItemId() == 1122444 || equip.getItemId() == 1122445) {
                    // Spiegelmann's Ordinary Necklace | Spiegelmann's Sparkling Necklace
                    cost = 0;
                }
                safeGuard = equip.hasAttribute(EquipAttribute.ProtectionScroll);
                boolean canSafe = equip.canSafeguardHyperUpgrade();
                boolean useSafe = (safeGuard || safeGuardFromUI) && canSafe;
                long costBeforeDiscount = cost;
                if (useSafe) {
                    cost *= 3;
                }
                long costFinal = cost;
                long costBeforeMVP = 0;
                long costBeforePC = 0;
                if (vipGrade > 0) {
                    costBeforeMVP = cost;
                    costFinal = (long) (cost * (100 - vipGrade * 5L) / 100.0D);
                }
                int successProp = GameConstants.enchantRates[equip.getChuc()][0];
                int destroyProp = GameConstants.enchantRates[equip.getChuc()][1];
                if (vipGrade > 0) {
                    successProp = (int) Math.round(successProp * (1.0 + 0.025 * vipGrade));
                }
                if (useSafe) {
                    destroyProp = 0;
                }
                if (successProp > GameConstants.RATE_SCALE - destroyProp) {
                    successProp = GameConstants.RATE_SCALE - destroyProp;
                }
                chr.write(FieldPacket.hyperUpgradeDisplay(
                        equip,
                        costFinal,
                        costBeforeDiscount,
                        costBeforeMVP,
                        costBeforePC,
                        successProp / 10,
                        destroyProp / 10,
                        equip.getDropStreak() >= 2,
                        equip.getChuc() + 1));
                if (EventConstants.HYPER_BURNING_MAX
                        && equip.getItemId() == 1122444
                        && chr.hasQuest(102442)
                        && !"4".equals(chr.getQRValueByKey(102442, "step"))) {
                    chr.getScriptManager().startScript(chr, 102442, "q102442s_1", ScriptType.Quest);
                }
                break;
            //endregion
            case MiniGameDisplay:
                chr.write(FieldPacket.miniGameDisplay(eeType));
                break;
            //case ShowScrollUpgradeResult:
            case ScrollTimerEffective:
                chr.write(FieldPacket.scrollTimerEffective());
                break;
            case ShowHyperUpgradeResult:
                //case ShowScrollVestigeCompensationResult:
                //case ShowTransmissionResult:
                //case ShowUnknownFailResult:
                break;
            default:
                System.out.println("Unhandled Equipment Enchant Type: " + eeType);
                chr.write(FieldPacket.showUnknownEnchantFailResult((byte) 0));
                break;
        }
    }

    @Handler(op = InHeader.USER_SKILL_LEARN_ITEM_USE_REQUEST)
    public static void handleUserLearnItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); //tick
        short pos = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();

        ItemInfo ii = ItemData.getItemInfoByID(itemID);
        if (ii == null || !chr.hasItem(itemID)) {
            return;
        }
        int masterLevel = ii.getMasterLv();
        int reqSkillLv = ii.getReqSkillLv();
        int skillid = 0;
        Map<ScrollStat, Integer> vals = ii.getScrollStats();
        int chance = vals.getOrDefault(ScrollStat.success, 100);

        // the first listed skill the character can raise with this book (current >= reqSkillLv, master < masterLv),
        // else the first one it has (for the refusal message)
        for (int skill : ii.getSkills()) {
            Skill owned = chr.hasSkill(skill) ? chr.getSkill(skill) : null;
            if (owned == null) {
                continue;
            }
            if (skillid == 0) {
                skillid = skill;
            }
            if (owned.getCurrentLevel() >= reqSkillLv && owned.getMasterLevel() < masterLevel) {
                skillid = skill;
                break;
            }
        }
        Skill skill = chr.getSkill(skillid);
        if (skill == null) {
            chr.chatMessage(Notice2, "發生錯誤。能力之書 ID：" + itemID + "，技能 ID：" + skillid + ".");
            chr.dispose();
            return;
        }
        if (skillid == 0 || (skill.getMasterLevel() >= masterLevel) || skill.getCurrentLevel() < reqSkillLv) {
            chr.chatMessage(SystemNotice, "你無法使用這本精通書。");
            chr.dispose();
            return;
        }

        // official: usable once the skill is at reqSkillLv (265 required > reqSkillLv, so == did nothing)
        if (skill.getCurrentLevel() >= reqSkillLv && skill.getMasterLevel() < masterLevel) {
            //chr.chatMessage(Mob, "Success Chance: " + chance + "%.");
            Item book = chr.getConsumeInventory().getItemBySlot(pos);
            if (book != null && book.getItemId() == itemID && book.getQuantity() > 0) {
                chr.consumeItem(book, 1);
            } else {
                chr.consumeItem(itemID, 1);
            }
            if (Util.succeedProp(chance)) {
                skill.setMasterLevel(masterLevel);
                chr.addSkill(skill);
                chr.write(WvsContext.changeSkillRecordResult(skill));
                //chr.chatMessage(Notice2, "[Mastery Book] Item id: " + itemID + "  set Skill id: " + skillid + "'s Master Level to: " + masterLevel + ".");
            } else {
                //chr.chatMessage(Notice2, "[Mastery Book] Item id: " + itemID + " was used, however it was unsuccessful.");
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_ITEM_SKILL_SOCKET_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserItemSkillSocketUpdateItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        Item item = chr.getConsumeInventory().getItemBySlot(uPos);
        InvType invType = ePos < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        Equip zeroWeapon = null;
        if (item == null || equip == null) {
            System.out.println("[" + chr.getName() + "] Mismatch equip or item was not found when upgrating.");
            chr.dispose();
            return;
        }
        if (ItemConstants.isLongOrBigSword(equip.getItemId()) && JobConstants.isZero(chr.getJob())) {
            zeroWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(10);
        }
        if (!ItemConstants.isSoulEnchanter(item.getItemId())) {
            String msg = String.format("Character %d tried to use a uncorrected (id %d) on an equip via Use inventory. OPCode : %d",
                    chr.getId(), item.getItemId(), InHeader.USER_ITEM_SKILL_SOCKET_UPGRADE_ITEM_USE_REQUEST.getValue());
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, msg);
            chr.dispose();
            return;
        }
        ItemInfo ii = ItemData.getItemInfoByID(item.getItemId());
        if (ii == null) {
            chr.dispose();
            return;
        }
        if (!ii.getReqItemIds().contains(equip.getItemId()) && ii.getReqItemIds().size() > 0) {
            chr.chatMessage(SystemNotice, "該道具無法用於這件裝備。");
            chr.dispose();
            return;
        }
        if (!ItemConstants.isWeapon(equip.getItemId()) || equip.getrLevel() + equip.getiIncReq() < ItemConstants.MIN_LEVEL_FOR_SOUL_SOCKET) {
            chr.chatMessage(SystemNotice, "無法爲這件裝備安裝靈魂插槽。");
            chr.dispose();
            return;
        }
        int successProp = ii.getScrollStats().get(ScrollStat.success);
        boolean success = Util.succeedProp(successProp);
        if (success) {
            equip.setSoulSocketId((short) (item.getItemId() % ItemConstants.SOUL_ENCHANTER_BASE_ID));
            equip.updateToChar(chr);
            if (zeroWeapon != null) {
                zeroWeapon.setSoulSocketId(equip.getSoulSocketId());
                zeroWeapon.updateToChar(chr);
            }
        }
        chr.write(UserLocal.ItemSkillSocketUpdateItemUse(success));
        //chr.getField().broadcast(UserPacket.showItemSkillSocketUpgradeEffect(chr.getId(), success));
        chr.consumeItem(item.getItemId(), 1);
    }

    @Handler(op = InHeader.USER_ITEM_SKILL_OPTION_UPGRADE_ITEM_USE_REQUEST)
    public static void handleUserItemSkillOptionUpdateItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short uPos = inPacket.decodeShort();
        short ePos = inPacket.decodeShort();
        Item item = chr.getConsumeInventory().getItemBySlot(uPos);
        InvType invType = ePos < 0 ? EQUIPPED : EQUIP;
        Equip equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(ePos);
        Equip zeroWeapon = null;
        if (item == null || equip == null) {
            System.out.println("[" + chr.getName() + "] Mismatch equip or item was not found when upgrating.");
            chr.dispose();
            return;
        }
        if (ItemConstants.isLongOrBigSword(equip.getItemId()) && JobConstants.isZero(chr.getJob())) {
            zeroWeapon = (Equip) chr.getEquippedInventory().getItemBySlot(10);
        }
        if (!ItemConstants.isSoul(item.getItemId())) {
            String msg = String.format("Character %d tried to use a uncorrected (id %d) on an equip via Use inventory. OPCode : %d", chr.getId(), item.getItemId(), InHeader.USER_ITEM_SKILL_OPTION_UPGRADE_ITEM_USE_REQUEST.getValue());
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, msg);
            chr.dispose();
            return;
        }
        if (!ItemConstants.isWeapon(equip.getItemId()) || equip.getSoulSocketId() == 0) {
            chr.chatMessage(SystemNotice, "無法將靈魂注入這件裝備。");
            chr.dispose();
            return;
        }
        int soulID = 1 + item.getItemId() % ItemConstants.SOUL_ITEM_BASE_ID;
        short option;
        try { // resolved before the equip changes; 282: by soul id, not by the (Chinese) display name
            option = (short) net.swordie.ms.client.character.tms282.Tms282SoulOptions.optionFor(item.getItemId());
        } catch (RuntimeException e) {
            System.out.println("[" + chr.getName() + "] no soul option for " + item.getItemId() + ": " + e);
            chr.dispose();
            return;
        }
        equip.setSoulOptionId((short) soulID);
        equip.setSoulItemId(item.getItemId());
        equip.setSoulOption(option);
        equip.updateToChar(chr);
        if (zeroWeapon != null) {
            zeroWeapon.setSoulOptionId(equip.getSoulOptionId());
            zeroWeapon.setSoulItemId(equip.getSoulItemId());
            zeroWeapon.setSoulOption(equip.getSoulOption());
            zeroWeapon.updateToChar(chr);
            int SoulSkillID = ItemConstants.getSoulSkillFromSoulID(item.getItemId());
            if (SoulSkillID != 0) {
                Skill soulSkill = SkillData.getSkillDeepCopyById(SoulSkillID);
                soulSkill.setCurrentLevel(1);
                chr.addSkill(soulSkill);
                chr.write(WvsContext.changeSkillRecordResult(soulSkill));
            }
            chr.initSoulMP();
        }
        chr.consumeItem(item.getItemId(), 1);
        chr.write(UserLocal.ItemSkillOptionUpdateItemUse(true, equip.getItemId(), option));
        //chr.getField().broadcast(UserPacket.showItemSkillOptionUpgradeEffect(chr.getId(), true, false, equip.getItemId(), item.getItemId()));
    }

    @Handler(op = InHeader.USER_RENT_ITEM_REQUEST)
    public static void handleUserRentItemRequest(Char chr, InPacket inPacket) {
        ScriptManagerImpl sm = chr.getScriptManager();
        inPacket.decodeByte();
        inPacket.decodeShort();
        short index = inPacket.decodeShort();
        // UI:3011
        int itemID = 0;
        long cost = 0;
        switch (index) {
            case 0:
                itemID = 1012902; // Red Beryl Face Accessory
                cost = 200_000_000;
                break;
            case 1:
                itemID = 1022375; // Red Beryl Eye Accessory
                cost = 200_000_000;
                break;
            case 2:
                itemID = 1032361; // Red Beryl Earrings
                cost = 300_000_000;
                break;
            case 3:
                itemID = 1122453; // Red Beryl Pendant
                cost = 300_000_000;
                break;
            case 4:
                itemID = 1113356; // Finite Red Beryl Ring
                cost = 100_000_000;
                break;
            case 5:
                itemID = 1113355; // Infinite Red Beryl Ring
                cost = 100_000_000;
                break;
            case 6:
                itemID = 1202326; // Arthur Totem
                cost = 100_000_000;
                break;
            case 7:
                itemID = 1202327; // Flash Totem
                cost = 100_000_000;
                break;
            case 8:
                itemID = 1202328; // Red Beryl Totem
                cost = 100_000_000;
                break;
            case 9:
                itemID = 1132336; // Red Beryl Belt
                cost = 300_000_000;
                break;
        }
        if (itemID != 0 && chr.canHold(itemID) && chr.getMoney() >= cost) {
            int questID = 68431 + index;
            int remain = 3;
            if (chr.hasQuest(questID)) {
                remain = Integer.parseInt(chr.getQRValueByKey(questID, "Remain"));
            }
            remain -= 1;
            chr.deductMoney(cost);
            final FileTime now = FileTime.fromDate(FileTime.nowUTC());
            String RegisterDateTime = now.toRentalFormat();
            String EndDateTime = FileTime.fromDate(FileTime.nowUTC().plusHours(24)).toRentalFormat();
            chr.createQuestWithQRValue(questID, "EndDateTime=00/01/01/00/00;RegisterDateTime=00/01/01/00/00;Remain=" + remain);
            chr.createQuestWithQRValue(questID, "EndDateTime=00/01/01/00/00;RegisterDateTime="+RegisterDateTime+";Remain=" + remain);
            chr.createQuestWithQRValue(questID, "EndDateTime="+EndDateTime+";RegisterDateTime="+RegisterDateTime+";Remain=" + remain);
            chr.createQuestWithQRValue(68430, "rMap="+chr.getFieldID()+";rent_type="+questID+";lastPlayWeek=12;debug=0");
            sm.chat("裝備租賃成功！請檢查你的裝備欄。");
            sm.gainQuestItem(itemID, 1);
            chr.write(WvsContext.receiveEquipRentalResult(0));
            if (itemID == 1012902 || itemID == 1022375 || itemID == 1032361 || itemID == 1122453 || itemID == 1132336) {
                Equip equip = ItemData.getEquipDeepCopyFromID(itemID, false);
                equip.setChuc((short) 20, true);
                int stat;
                if (JobConstants.isDemonAvenger(chr.getJob())) {
                    stat = 45;
                } else if (JobConstants.isXenon(chr.getJob())) {
                    stat = 86;
                } else {
                    stat = switch (GameConstants.getMainBaseStatForJob(chr.getJob())) {
                        case str  -> 41;
                        case dex  -> 42;
                        case inte -> 43;
                        case luk  -> 44;
                        default   -> 0;
                    };
                }
                if (stat != 0) {
                    equip.setOptionBase(0, 30000 + stat);
                    equip.setOptionBase(1, 20000 + stat);
                    equip.setOptionBase(2, 20000 + stat);
                    equip.setOptionBonus(0, 32000 + stat);
                    equip.setOptionBonus(1, 22000 + stat);
                }
                equip.setCuc((short) 4);
                equip.flame(FlameType.Black);
                equip.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(1)));
                chr.addItemToInventory(equip);
            } else if (itemID == 1113356 || itemID == 1113355) {
                Equip equip = ItemData.getEquipDeepCopyFromID(itemID, false);
                equip.setItemLevel((byte) 4);
                equip.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(1)));
                chr.addItemToInventory(equip);
            } else {
                chr.addItemToInventory(itemID, 1, "day", 1);
            }
        } else {
            sm.chat("請檢查你的裝備欄是否還有空位，或確認楓幣是否足夠。");
        }
    }

    @Handler(op = InHeader.USER_WEAPON_TEMP_ITEM_OPTION_REQUEST)
    public static void handleUserWeaponTempItemOptionRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        boolean set = inPacket.decodeByte() != 0;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (set) {
            if (!tsm.hasStat(FullSoulMP)) {
                Option o1 = new Option();
                o1.nOption = 1;
                o1.rOption = ItemConstants.getSoulSkillFromSoulID(((Equip) chr.getEquippedItemByBodyPart(BodyPart.Weapon)).getSoulOptionId());
                o1.xOption = 500; // Soul count to release the effect
                tsm.sendStat(FullSoulMP, o1);
            }
        } else {
            if (tsm.hasStat(SoulMP) && tsm.hasStat(FullSoulMP)) {
                tsm.removeStat(FullSoulMP);
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_ACTIVATE_EFFECT_ITEM)
    public static void handleUserActivateEffectItem(Char chr, InPacket inPacket) {
        int itemId = inPacket.decodeInt();
        chr.setActiveEffectItemID(itemId);
        chr.getField().broadcast(UserRemote.setActiveEffectItem(chr, itemId), chr);
    }

    @Handler(op = InHeader.USER_MONKEY_EFFECT_ITEM)
    public static void handleUserMonkeyEffectItem(Char chr, InPacket inPacket) {
        int itemId = inPacket.decodeInt();
        chr.setMonkeyEffectItemID(itemId);
        chr.getField().broadcast(UserRemote.setMonkeyEffectItem(chr, itemId), chr);
    }

    @Handler(op = InHeader.USER_ACTIVATE_NICK_ITEM)
    public static void handleUserActivateNickItem(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        short slot = (short) inPacket.decodeInt();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Item item = chr.getInventoryByType(INSTALL).getItemBySlot(slot);
        if (itemID == 0) {
            tsm.removeStatsBySkill(chr.getActiveNickSkillID());
            chr.setActiveNickSkillID(0);
            chr.setActiveNickItemID(0);
            chr.write(WvsContext.nickSkillExpired(1));

            if (item != null) {
                item.setTitleOn(false);
                chr.write(WvsContext.removeAndAddItem(item));
            }

            chr.createQuestWithQRValue(7290, itemID + "");
            chr.createQuestWithQRValue(1955, "date=" + FileTime.currentTime().toYYMMDDHHMMSS());
            chr.createQuestWithQRValue(19019, "expired=1;date=0;id=0;slotpos=0");
            chr.getField().broadcast(UserRemote.setActiveNickItem(chr, null), chr);
        } else {
            ItemInfo ii = ItemData.getItemInfoByID(itemID);
            if (ii == null) {
                return;
            }
            if (chr.getActiveNickSkillID() != 0) { // Change Title
                tsm.removeStatsBySkill(chr.getActiveNickSkillID());
            }
            if (!item.hasObtainedOnce()) {
                if (ii.getCharismaEXP() != 0) {
                    chr.addTraitExp(Stat.charismaEXP, ii.getCharismaEXP());
                }
                if (ii.getCharmEXP() != 0) {
                    chr.addTraitExp(Stat.charmEXP, ii.getCharmEXP());
                }
                if (ii.getSenseEXP() != 0) {
                    chr.addTraitExp(Stat.senseEXP, ii.getSenseEXP());
                }
                if (ii.getCraftEXP() != 0) {
                    chr.addTraitExp(Stat.craftEXP, ii.getCraftEXP());
                }
                if (ii.getWillEXP() != 0) {
                    chr.addTraitExp(Stat.willEXP, ii.getWillEXP());
                }
                item.setObtainedOnce(true);
            }
            if (ii.getNickSkill() != 0) {
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                SkillInfo si = SkillData.getSkillInfoById(ii.getNickSkill());
                Option o1 = new Option();
                int padX = si.getValue(SkillStat.padX, 1);
                if (padX != 0) {
                    o1.nValue = padX;
                    o1.nReason = ii.getNickSkill();
                    newStats.put(IndiePAD, o1);
                }
                Option o2 = new Option();
                int madX = si.getValue(SkillStat.madX, 1);
                if (madX != 0) {
                    o2.nValue = madX;
                    o2.nReason = ii.getNickSkill();
                    newStats.put(IndieMAD, o2);
                }
                Option o3 = new Option();
                int pddX = si.getValue(SkillStat.pddX, 1);
                if (pddX != 0) {
                    o3.nValue = pddX;
                    o3.nReason = ii.getNickSkill();
                    newStats.put(IndiePDD, o3);
                }
                Option o4 = new Option();
                int strX = si.getValue(SkillStat.strX, 1);
                if (strX != 0) {
                    o4.nValue = strX;
                    o4.nReason = ii.getNickSkill();
                    newStats.put(IndieAllStat, o4);
                }
                Option o5 = new Option();
                int mhpX = si.getValue(SkillStat.mhpX, 1);
                if (mhpX != 0) {
                    o5.nValue = mhpX;
                    o5.nReason = ii.getNickSkill();
                    newStats.put(IndieMHP, o5);
                }
                Option o6 = new Option();
                int mmpX = si.getValue(SkillStat.mmpX, 1);
                if (mmpX != 0) {
                    o6.nValue = mmpX;
                    o6.nReason = ii.getNickSkill();
                    newStats.put(IndieMMP, o6);
                }
                Option o7 = new Option();
                int bdR = si.getValue(SkillStat.bdR, 1);
                if (bdR != 0) {
                    o7.nValue = bdR;
                    o7.nReason = ii.getNickSkill();
                    newStats.put(IndieBDR, o7);
                }
                Option o8 = new Option();
                int ignoreMobpdpR = si.getValue(SkillStat.ignoreMobpdpR, 1);
                if (ignoreMobpdpR != 0) {
                    o8.nValue = ignoreMobpdpR;
                    o8.nReason = ii.getNickSkill();
                    newStats.put(IndieIgnoreMobpdpR, o8);
                }
                Option o9 = new Option();
                int expR = si.getValue(SkillStat.expR, 1);
                if (expR != 0) {
                    o9.nValue = expR;
                    o9.nReason = ii.getNickSkill();
                    newStats.put(IndieEXP, o9);
                }
                Option o10 = new Option();
                int arcX = si.getValue(SkillStat.arcX, 1);
                if (arcX != 0) {
                    o10.nValue = arcX;
                    o10.nReason = ii.getNickSkill();
                    newStats.put(IndieArc, o10);
                }
                Option o11 = new Option();
                int autX = si.getValue(SkillStat.autX, 1);
                if (autX != 0) {
                    o11.nValue = autX;
                    o11.nReason = ii.getNickSkill();
                    newStats.put(IndieAut, o11);
                }
                chr.setActiveNickSkillID(ii.getNickSkill());
                tsm.sendStat(newStats);
            }

            chr.setActiveNickItemID(itemID);

            chr.write(WvsContext.nickSkillExpired(0));

            item.setTitleOn(true);
            chr.write(WvsContext.removeAndAddItem(item));

            chr.createQuestWithQRValue(7290, itemID + "");
            chr.createQuestWithQRValue(1955, "date=" + FileTime.currentTime().toYYMMDDHHMMSS());
            chr.createQuestWithQRValue(19019, "expired=0;date="+
                    FileTime.currentTime().toYYYYMMDD_HHMMssSSS_QR()+";id="+itemID+";slotpos="+item.getBagIndex());

            chr.getField().broadcast(UserRemote.setActiveNickItem(chr, null), chr);
        }
    }

    @Handler(op = InHeader.USER_ACTIVATE_DAMAGE_SKIN)
    public static void handleUserActivateDamageSkin(Char chr, InPacket inPacket) {
        int damageSkin = inPacket.decodeInt();
        if (chr.getActiveDamageSkin().getDamageSkinID() != damageSkin) {
            chr.setActiveDamageSkin(chr.getDamageSkinBySkinID(damageSkin));
            chr.write(UserPacket.setActiveDamageSkin(chr));
        }
    }

    @Handler(op = InHeader.USER_ACTIVATE_DAMAGE_SKIN_PREMIUM)
    public static void handleUserActivateDamageSkinPremium(Char chr, InPacket inPacket) {
        int damageSkin = inPacket.decodeInt();
        if (chr.getPremiumDamageSkin().getDamageSkinID() != damageSkin) {
            chr.setPremiumDamageSkin(chr.getDamageSkinBySkinID(damageSkin));
        }
    }

    @Handler(op = InHeader.USER_DAMAGE_SKIN_SAVE_REQUEST)
    public static void handleUserDamageSkinSaveRequest(Char chr, InPacket inPacket) {
        byte typeVal = inPacket.decodeByte();
        DamageSkinType dst = DamageSkinType.getByVal(typeVal);
        if (dst == null || dst.getVal() >= DamageSkinType.Res_Success.getVal()) {
            System.out.println("Unknown DamageSkinType " + dst);
            return;
        }
        switch (dst) {
            case Req_Reg: {
                DamageSkinSaveData dssd = chr.getActiveDamageSkin();
                if (chr.getDamageSkins().contains(dssd)) {
                    // phần nếu đã có thì không add thêm.
                    return;
                }
                dssd.setCharId(chr.getId());
                dssd.setActivateTime(FileTime.currentTime());
                chr.addDamageSkin(dssd);
                chr.write(UserLocal.damageSkinSaveResult(dst, DamageSkinType.Req_Reg, chr));
                break;
            }
            case Req_Remove: {
                short skinId = inPacket.decodeShort();
                DamageSkinSaveData dssd = chr.getDamageSkinBySkinID(skinId);
                if (dssd == null) {
                    // không có sao remove?
                    return;
                }
                DamageSkinSaveData curSkin = chr.getActiveDamageSkin();
                if (curSkin != null && dssd.getDamageSkinID() == curSkin.getDamageSkinID()) {
                    // đang là active không remove được
                    return;
                }
                chr.removeDamageSkin(dssd);
                chr.write(UserLocal.damageSkinSaveResult(dst, DamageSkinType.Req_Remove, chr));
                break;
            }
            case Req_Active: {
                short skinId = inPacket.decodeShort();
                DamageSkinSaveData dssd = chr.getDamageSkinBySkinID(skinId);
                if (dssd == null) {
                    chr.write(UserLocal.damageSkinSaveResult(dst, DamageSkinType.Res_Fail_Unknown, chr));
                    return;
                }
                DamageSkinSaveData curSkin = chr.getActiveDamageSkin();
                if (curSkin != null && dssd.getDamageSkinID() == curSkin.getDamageSkinID()) {
                    chr.write(UserLocal.damageSkinSaveResult(dst, DamageSkinType.Res_Fail_AlreadyActive, chr));
                    return;
                }
                chr.setActiveDamageSkin(dssd);
                chr.write(UserLocal.damageSkinSaveResult(dst, DamageSkinType.Req_Active, chr));

                Quest q = chr.getQuestById(QuestConstants.DAMAGE_SKIN);
                if (q == null) {
                    q = new Quest(chr.getId(), QuestConstants.DAMAGE_SKIN, QuestStatus.Started);
                    chr.addQuest(q);
                }
                q.setQrValue(dssd.getDamageSkinID() + "");
                chr.write(WvsContext.questRecordMessage(q));
                chr.write(UserPacket.setActiveDamageSkin(chr));
                break;
            }
        }
    }

    @Handler(op = InHeader.USER_DEFAULT_WING_ITEM)
    public static void handleUserDefaultWingItem(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        chr.getAvatarData().getAvatarLook().setDemonWingID(itemID);
        chr.getField().broadcast(UserRemote.setDefaultWingItem(chr, itemID));
    }

    @Handler(op = InHeader.USER_KAISER_TRANSFORM_WING)
    public static void handleUserKaierTransformWing(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        chr.getAvatarData().getAvatarLook().setKaiserWingID(itemID);
        chr.getField().broadcast(UserRemote.setKaiserTransformItem(chr, itemID, (byte) 0));
    }

    @Handler(op = InHeader.USER_KAISER_TRANSFORM_TAIL)
    public static void handleUserKaierTransformTail(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        chr.getAvatarData().getAvatarLook().setKaiserTailID(itemID);
        chr.getField().broadcast(UserRemote.setKaiserTransformItem(chr, itemID, (byte) 1));
    }

    @Handler(op = InHeader.USER_UPGRADE_TOMB_EFFECT)
    public static void handleUserUpgradeTombEffect(Char chr, InPacket inPacket) {
        int itemId = inPacket.decodeInt(); // 5510000 | Respawn Token
        int posX = inPacket.decodeInt();
        int posY = inPacket.decodeInt();
        Position pos = new Position(posX, posY);
        chr.getField().broadcast(UserRemote.showUpgradeTombEffect(chr, itemId, pos));
    }

    @Handler(op = InHeader.USER_RECIPE_OPEN_ITEM_USE_REQUEST)
    public static void handleUserRecipeOpenItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt();// tick
        short pos = inPacket.decodeShort();// // nPOS
        int itemID = inPacket.decodeInt();// nItemID

        Item item = chr.getInventoryByType(CONSUME).getItemBySlot(pos);
        if (item.getItemId() != itemID) {
            chr.dispose();
            return;
        }
        if (chr.getHP() > 0 && ItemConstants.isRecipeOpenItem(itemID)) {
            ItemInfo recipe = ItemData.getItemInfoByID(itemID);
            if (recipe != null) {
                int recipeID = recipe.getSpecStats().getOrDefault(SpecStat.recipe, 0);
                int reqSkillLevel = recipe.getSpecStats().getOrDefault(SpecStat.reqSkillLevel, 0);
                MakingSkillRecipe msr = SkillData.getRecipeById(recipeID);
                if (msr != null && msr.isNeedOpenItem()) {
                    if (chr.getSkillLevel(msr.getReqSkillID()) < reqSkillLevel || chr.getSkillLevel(recipeID) > 0) {
                        return;
                    }
                    chr.addSkill(recipeID, 1, 1);
                    chr.consumeItem(itemID, 1);
                }
            }
        }
    }

    @Handler(op = InHeader.USER_LOTTERY_ITEM_USE_REQUEST)
    public static void handleUserLotteryItemUseRequest(Char chr, InPacket inPacket) {
        if (chr.getHP() <= 0) {
            chr.chatMessage(SystemNotice, "死亡狀態下無法使用該物品。");
            chr.dispose();
            return;
        }
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        boolean sendForUI = inPacket.decodeByte() != 0;
        boolean logStart = inPacket.decodeByte() != 0;
        Item item = chr.getConsumeInventory().getItemBySlot(slot);
        if (item == null || item.getItemId() != itemID) {
            return;
        }
        if (itemID >= 2028263 && itemID <= 2028272) {
            Integer[] rewards;
            if (itemID >= 2028268 && itemID <= 2028272) {
                rewards = ItemConstants.AliciaRingBox_6thRank;
            } else {
                rewards = ItemConstants.AliciaRingBox_2ndRank;
            }
            if (sendForUI) {
                int rewardID = Util.getRandomFromCollection(rewards);
                chr.addItemToInventory(rewardID, rewardID == 4001832 ? Util.getRandom(100, 500) : (rewardID == 2432468 || rewardID == 2432503) ? Util.getRandom(1, 5) : 1);
                chr.write(UserPacket.effect(Effect.lotteryUIResult(0, rewardID, 1)));
                chr.consumeItem(itemID, 1);
            } else {
                chr.write(UserLocal.doLotteryUI(slot, itemID, logStart, rewards));
            }
        } else if (ItemConstants.isSealedBox(itemID)) {
            Integer[] rewards = null;
            if (itemID == 2028154) {
                rewards = ItemConstants.sealedBox_Hat;
            } else if (itemID == 2028155) {
                rewards = ItemConstants.sealedBox_Overall_Top_Bottom;
            } else if (itemID == 2028156) {
                rewards = ItemConstants.sealedBox_Shoes_Gloves;
            } else if (itemID == 2028161) {
                rewards = ItemConstants.sealedBox_Weapon;
            } else if (itemID == 2028162) {
                rewards = ItemConstants.sealedBox_Hat_Chaos;
            } else if (itemID == 2028163) {
                rewards = ItemConstants.sealedBox_Overall_Top_Bottom_Chaos;
            } else if (itemID == 2028164) {
                rewards = ItemConstants.sealedBox_Shoes_Gloves_Chaos;
            } else if (itemID == 2028165) {
                rewards = ItemConstants.sealedBox_Weapon_Chaos;
            }
            if (sendForUI) {
                int rewardID = Util.getRandomFromCollection(rewards);
                chr.addItemToInventory(rewardID, 1);
                chr.write(UserPacket.effect(Effect.lotteryUIResult(0, rewardID, 1)));
                chr.consumeItem(itemID, 1);
            } else {
                chr.write(UserLocal.doLotteryUI(slot, itemID, logStart, rewards));
            }
        } else if (ItemConstants.isGuildRewardBox(itemID)) {
            Integer[] rewards = null;
            int quantity = 1;
            if (itemID == 2028338) {
                rewards = ItemConstants.guildReward_TierS;
                quantity = Util.getRandom(1, 2);
            } else if (itemID == 2028342) {
                rewards = ItemConstants.guildReward_TierA;
                quantity = Util.getRandom(1, 3);
            } else if (itemID == 2028343) {
                rewards = ItemConstants.guildReward_TierB;
            } else if (itemID == 2028344) {
                rewards = ItemConstants.guildReward_TierC;
            } else if (itemID == 2028345) {
                rewards = ItemConstants.guildReward_TierD_F;
                quantity = Util.getRandom(5, 7);
            } else if (itemID == 2028346) {
                rewards = ItemConstants.guildReward_TierD_F;
                quantity = Util.getRandom(1, 3);
            }
            int rewardID = Util.getRandomFromCollection(rewards);
            if (rewardID == 1) {
                chr.setPartyboss(new HashSet<>());
                chr.chatMessage("你獲得了 [Boss 邀請券] x 1。所有 Boss 入場次數已被清除。");
                chr.consumeItem(itemID, 1);
            } else if (chr.canHold(rewardID)) {
                chr.addItemToInventory(rewardID, quantity);
                chr.consumeItem(itemID, 1);
            } else {
                chr.chatMessage("你的背包沒有空位。");
            }
        } else if (itemID == 2028197) { // New Year's Challenge Box
            int rewardID = ItemConstants.getNewYearBox();
            if (chr.canHold(rewardID)) {
                Equip equip = ItemData.getEquipDeepCopyFromID(rewardID, false);
                equip.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(15)));
                chr.addItemToInventory(equip.getInvType(), equip, false, false);
                chr.write(WvsContext.inventoryOperation(true, false, Add, (short) equip.getBagIndex(), (byte) -1, 0, equip));
                chr.consumeItem(2028197, 1);
            } else {
                chr.chatMessage("你的背包沒有空位。");
            }
        } else {
            ItemInfo ii = ItemData.getItemInfoByID(itemID);
            if (!ii.getItemRewardInfos().isEmpty()) {
                if (ItemConstants.isMesoPouch(itemID)) {
                    int mesoReward = ii.getRandomMesoReward();
                    chr.chatScriptMessage(String.format("你從 %s 獲得了 %d 楓幣。", StringData.getItemStringById(itemID), mesoReward));
                    chr.addMoney(mesoReward);
                    chr.consumeItem(itemID, 1);
                } else {
                    Item reward = ii.getRandomReward();
                    if (reward != null) {
                        if (chr.canHold(reward.getItemId())) {
                            chr.chatScriptMessage(String.format("你從 %s 獲得了 %s。", StringData.getItemStringById(itemID), StringData.getItemStringById(reward.getItemId())));
                            chr.addItemToInventory(reward);
                            chr.consumeItem(itemID, 1);
                        } else {
                            chr.chatMessage("你的背包沒有空位。");
                        }
                    }
                }
            }
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_ABILITY_RESET_ITEM_USE_REQUEST)
    public static void handleUserAbilityResetItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        resetAbilityItemUseRequest(chr, slot, itemID, false);
    }

    @Handler(op = InHeader.USER_SKILL_RESET_ITEM_USE_REQUEST)
    public static void handleUserSkillResetItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        resetSkills(chr, slot, itemID, false);
    }

    @Handler(op = InHeader.USER_ABILITY_CHANGE_ITEM_USE_REQUEST)
    public static void handleUserAbilityChangeItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        changeAbility(chr, inPacket, slot, itemID, false);
    }

    private static void resetAbilityItemUseRequest(Char chr, short slot, int itemID, boolean isCash) {
        if (chr.getHP() <= 0) {
            chr.chatMessage(SystemNotice, "死亡狀態下無法使用該物品。");
            chr.dispose();
            return;
        }
        Item item = null;
        if (isCash) {
            item = chr.getInventoryByType(InvType.CASH).getItemByItemID(itemID);
        } else {
            item = chr.getInventoryByType(InvType.CONSUME).getItemByItemID(itemID);
        }
        if (item == null || item.getItemId() != itemID) {
            System.out.println("[" + chr.getName() + "] Unknown equip or mismatching use items.");
            chr.dispose();
            return;
        }
        if (!ItemConstants.isAPResetItem(itemID)) {
            String msg = String.format("Character %d tried to use a uncorrected (id %d) to reset Abilities. OPCode : %d",
                    chr.getId(), item.getItemId(), InHeader.USER_ABILITY_RESET_ITEM_USE_REQUEST.getValue());
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, msg);
            chr.dispose();
            return;
        }
        if (chr.getLevel() <= 10) {
            if (JobConstants.isBeginnerJob(chr.getJob()) || JobConstants.isDemonAvenger(chr.getJob())) {
                chr.chatMessage(SystemNotice, "無法使用該物品。");
                chr.dispose();
                return;
            }
        }
        int newAdditionalAP = 0;
        Map<Stat, Object> stats = new HashMap<>();
        Map<Stat, Object> newStats = new HashMap<>();
        stats.put(Stat.str, chr.getAvatarData().getCharacterStat().getStr());
        stats.put(Stat.dex, chr.getAvatarData().getCharacterStat().getDex());
        stats.put(Stat.luk, chr.getAvatarData().getCharacterStat().getLuk());
        stats.put(Stat.inte, chr.getAvatarData().getCharacterStat().getInt());
        int buffer = 0;
        for (Map.Entry<Stat, Object> stat : stats.entrySet()) {
            buffer = ((short) stat.getValue() - 4);
            if (buffer > 0 && stat.getKey() != Stat.mhp) {
                newAdditionalAP += buffer;
                newStats.put(stat.getKey(), (short) 4);
            }
        }

        if ((newAdditionalAP - 1) > 19979) {
            chr.chatMessage(SystemNotice, "無法使用該物品。");
            stats.clear();
            newStats.clear();
            chr.dispose();
            return;
        }
        for (Map.Entry<Stat, Object> newstat : newStats.entrySet()) {
            if (newstat.getKey() == Stat.mhp) {
                chr.setStat(newstat.getKey(), (int) newstat.getValue());
            } else {
                chr.setStat(newstat.getKey(), (short) newstat.getValue());
            }
        }
        chr.addStat(Stat.ap, (short) newAdditionalAP);
        newStats.put(Stat.ap, (short) chr.getStat(Stat.ap));
        chr.consumeItem(itemID, 1);
        chr.write(WvsContext.abilityResetItemResult(chr.getId()));
        chr.sendStatsPacket(newStats);
    }

    private static void changeAbility(Char chr, InPacket inPacket, short slot, int itemID, boolean isCash) {
        if (chr.getHP() <= 0) {
            chr.chatMessage(SystemNotice, "死亡狀態下無法使用該物品。");
            chr.dispose();
            return;
        }
        Item item = null;
        int inc = inPacket.decodeInt();
        inPacket.decodeInt();
        int dec = inPacket.decodeInt();
        inPacket.decodeInt();
        if (isCash) {
            item = chr.getInventoryByType(InvType.CASH).getItemBySlot(slot);
        } else {
            item = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(slot);
        }
        if (item == null || item.getItemId() != itemID) {
            System.out.println("[" + chr.getName() + "] Unknown equip or mismatching use items.");
            chr.dispose();
            return;
        }
        if (!ItemConstants.isAPChangeItem(itemID)) {
            String msg = String.format("Character %d tried to use a uncorrected (id %d) to change Ability. OPCode : %d",
                    chr.getId(), item.getItemId(), InHeader.USER_ABILITY_CHANGE_ITEM_USE_REQUEST.getValue());
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, msg);
            chr.dispose();
            return;
        }
        Stat toStat = Stat.getByValue(inc);
        Stat fromStat = Stat.getByValue(dec);

        int currentFromStat = chr.getStat(fromStat);
        boolean isAbleFromStat = fromStat == Stat.str || fromStat == Stat.dex || fromStat == Stat.inte || fromStat == Stat.luk;
        if (toStat == fromStat && currentFromStat <= 4 && !isAbleFromStat) {
            chr.chatMessage(SystemNotice, "該屬性無法變更。");
            chr.dispose();
            return;
        }

        int[] hpMpToAdd = GameConstants.getHpMpPerLevel(chr.getJob());
        int hp = hpMpToAdd[0];
        int mp = hpMpToAdd[1];
        Map<Stat, Object> stats = new HashMap<>();
        if (fromStat == Stat.mhp) {
            chr.addStat(fromStat, -hp);
            stats.put(fromStat, chr.getStat(fromStat));
        } else if (fromStat == Stat.mmp && !JobConstants.isNoManaJob(chr.getJob())) {
            chr.addStat(fromStat, -mp);
            stats.put(fromStat, chr.getStat(fromStat));
        } else if (fromStat == Stat.str || fromStat == Stat.dex || fromStat == Stat.inte || fromStat == Stat.luk) {
            chr.addStat(fromStat, -1);
            stats.put(fromStat, (short) chr.getStat(fromStat));
        }
        if (toStat == Stat.mhp) {
            chr.addStat(toStat, hp);
            stats.put(toStat, chr.getStat(toStat));
        } else if (toStat == Stat.mmp && !JobConstants.isNoManaJob(chr.getJob())) {
            chr.addStat(toStat, mp);
            stats.put(toStat, chr.getStat(toStat));
        } else if (toStat == Stat.str || toStat == Stat.dex || toStat == Stat.inte || toStat == Stat.luk) {
            chr.addStat(toStat, 1);
            stats.put(toStat, (short) chr.getStat(toStat));
        }
        chr.consumeItem(itemID, 1);
        chr.sendStatsPacket(stats);
    }

    private static void resetSkills(Char chr, short slot, int itemID, boolean isCash) {
        if (chr.getHP() <= 0) {
            chr.chatMessage(SystemNotice, "死亡狀態下無法使用該物品。");
            chr.dispose();
            return;
        }
        Item item = null;
        if (isCash) {
            item = chr.getInventoryByType(InvType.CASH).getItemBySlot(slot);
        } else {
            item = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(slot);
        }
        if (item == null || item.getItemId() != itemID) {
            System.out.println("[" + chr.getName() + "] Unknown equip or mismatching use items.");
            chr.dispose();
            return;
        }
        if (!ItemConstants.isSkillResetItem(itemID)) {
            String msg = String.format("Character %d tried to use a uncorrected (id %d) to reset skills. OPCode : %d",
                    chr.getId(), item.getItemId(), InHeader.USER_SKILL_RESET_ITEM_USE_REQUEST.getValue());
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, msg);
            chr.dispose();
            return;
        }
        if (chr.getLevel() <= 10) {
            if (JobConstants.isBeginnerJob(chr.getJob())) {
                chr.chatMessage(SystemNotice, "無法使用該物品。");
                chr.dispose();
                return;
            }
        }

        int newSP_JobII = 0;
        int newSP_JobIII = 0;
        int newSP_JobIV = 0;

        int db_newSP_JobII = 0;
        int db_newSP_JobIII = 0;
        int db_newSP_JobIV = 0;
        int db_newSP_JobV = 0;
        int db_newSP_JobVI = 0;

        int evan_newSP_JobII = 0;
        int evan_newSP_JobIII = 0;
        int evan_newSP_JobIV = 0;

        short currentJobId = chr.getJob();
        int currentJobLevel = JobConstants.getJobLevel(currentJobId); // 1212 / 1211 / 1210 / 1200
        List<Skill> newSkills = new ArrayList<>();
        for (Skill skill : chr.getSkills()) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (!si.isInvisible() && !SkillConstants.isHyperStat(skill.getSkillId()) && !SkillConstants.isHyperSkill(currentJobId, skill.getSkillId())) {
                if (JobConstants.isDualBlade(currentJobId)) {
                    if ((skill.getSkillId() / 10000) == 430) {
                        db_newSP_JobII += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                    if ((skill.getSkillId() / 10000) == 431) {
                        db_newSP_JobIII += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                    if ((skill.getSkillId() / 10000) == 432) {
                        db_newSP_JobIV += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                    if ((skill.getSkillId() / 10000) == 433) {
                        db_newSP_JobV += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                    if ((skill.getSkillId() / 10000) == 434) {
                        db_newSP_JobVI += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                } else if (JobConstants.isEvan(currentJobId)) {
                    if ((skill.getSkillId() / 10000) == 2211 || (skill.getSkillId() / 1000) == 22111) {
                        evan_newSP_JobII += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                    if ((skill.getSkillId() / 10000) == 2214 || (skill.getSkillId() / 1000) == 22141) {
                        evan_newSP_JobIII += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                    if ((skill.getSkillId() / 10000) == 2217 || (skill.getSkillId() / 1000) == 22171) {
                        evan_newSP_JobIV += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                } else {
                    if (currentJobLevel >= 2 && JobConstants.getJobLevel((short) (skill.getSkillId() / 10000)) == 2) {
                        newSP_JobII += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                    if (currentJobLevel >= 3 && JobConstants.getJobLevel((short) (skill.getSkillId() / 10000)) == 3) {
                        newSP_JobIII += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                    if (currentJobLevel >= 4 && JobConstants.getJobLevel((short) (skill.getSkillId() / 10000)) == 4) {
                        newSP_JobIV += skill.getCurrentLevel();
                        skill.setCurrentLevel(0);
                        newSkills.add(skill);
                    }
                }
            }
        }
        if (JobConstants.isDualBlade(currentJobId)) {
            if (db_newSP_JobII > 0) {
                chr.addSpToSpecificJob((short) (currentJobLevel == 2 ? currentJobId : (currentJobId - currentJobLevel + 2)), db_newSP_JobII);
            }
            if (db_newSP_JobIII > 0) {
                chr.addSpToSpecificJob((short) (currentJobLevel == 3 ? currentJobId : (currentJobId - currentJobLevel + 3)), db_newSP_JobIII);
            }
            if (db_newSP_JobIV > 0) {
                chr.addSpToSpecificJob((short) (currentJobLevel == 4 ? currentJobId : (currentJobId - currentJobLevel + 4)), db_newSP_JobIV);
            }
            if (db_newSP_JobV > 0) {
                chr.addSpToSpecificJob((short) (currentJobLevel == 5 ? currentJobId : (currentJobId - currentJobLevel + 5)), db_newSP_JobV);
            }
            if (db_newSP_JobVI > 0) {
                chr.addSpToSpecificJob(currentJobId, newSP_JobIV);
            }
        } else if (JobConstants.isEvan(currentJobId)) {
            if (evan_newSP_JobII > 0) {
                chr.addSpToSpecificJob((short) (currentJobLevel == 2 ? currentJobId : currentJobLevel == 3 ? currentJobId - 2 : currentJobId - 6), evan_newSP_JobII);
            }
            if (evan_newSP_JobIII > 0) {
                chr.addSpToSpecificJob((short) (currentJobLevel == 3 ? currentJobId : currentJobId - 4), evan_newSP_JobIII);
            }
            if (evan_newSP_JobIV > 0) {
                chr.addSpToSpecificJob(currentJobId, evan_newSP_JobIV);
            }
        } else {
            if (newSP_JobII > 0) {
                chr.addSpToSpecificJob((short) (currentJobLevel == 2 ? currentJobId : (currentJobId - currentJobLevel + 2)), newSP_JobII);
            }
            if (newSP_JobIII > 0) {
                chr.addSpToSpecificJob((short) (currentJobLevel == 3 ? currentJobId : (currentJobId - currentJobLevel + 3)), newSP_JobIII);
            }
            if (newSP_JobIV > 0) {
                chr.addSpToSpecificJob(currentJobId, newSP_JobIV);
            }
        }
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
        chr.sendStatsPacket(stats);
        chr.write(WvsContext.changeSkillRecordResult(newSkills, true, false, false));
        chr.write(WvsContext.skillResetItemResult(chr.getId()));
        chr.consumeItem(itemID, 1);
        chr.getTemporaryStatManager().removeAllStats();
    }

    @Handler(op = InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_SET)
    public static void handleUserRequestCharacterPotentialSkillRandSet(Char chr, InPacket inPacket) {
        int itemID = inPacket.decodeInt();
        int size = inPacket.decodeInt();
        byte preset = Byte.parseByte(chr.getQRValueByKey(QuestConstants.CHARACTER_POTENTIAL_PRESET, "potential"));
        resetCharacterPotentials(chr, preset, itemID, size, false);
    }

    @Handler(op = InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_SET_UI)
    public static void handleUserRequestCharacterPotentialSkillRandSetUI(Char chr, InPacket inPacket) {
        byte preset = inPacket.decodeByte();
        int itemID = inPacket.decodeInt();
        int size = inPacket.decodeInt();
        resetCharacterPotentials(chr, preset, itemID, size, false);
    }

    private static void resetCharacterPotentials(Char chr, byte preset, int itemID, int size, boolean isCash) {
        Item item;
        if (isCash) {
            item = chr.getInventoryByType(InvType.CASH).getItemByItemID(itemID);
        } else {
            item = chr.getInventoryByType(InvType.CONSUME).getItemByItemID(itemID);
        }
        if (item == null || item.getItemId() != itemID) {
            System.out.println("[" + chr.getName() + "] Mismatching use items.");
            chr.dispose();
            return;
        }
        if (!ItemConstants.isCirculatorItem(itemID)) {
            String msg = String.format("Character %d tried to use a uncorrected (id %d) to reset character potentials. OPCode : %d",
                    chr.getId(), item.getItemId(), InHeader.USER_REQUEST_CHARACTER_POTENTIAL_SKILL_RAND_SET.getValue());
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, msg);
            chr.dispose();
            return;
        }
        CharacterPotentialMan cpm = chr.getPotentialMan();
        if (chr.getPotentials().size() != 3) {
            chr.chatMessage(SystemNotice, "至少需要 3 行潛能。");
            chr.dispose();
            return;
        }
        byte grade = cpm.getGrade(preset);
        if (itemID == 5062800 || itemID == 5062801) {
            if (grade < CharPotGrade.Epic.ordinal()) {
                chr.chatMessage(SystemNotice, "該物品只能用於稀有以上等級的潛能。");
                chr.dispose();
                return;
            }
            List<CharacterPotentialValueHolder> characterPotentialValueHolders = new LinkedList<>();
            boolean gradeUp;
            // Ability ranks will not be reduced by this Circulator
            if (grade == CharPotGrade.Unique.ordinal()) {
                gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_UNIQUE_POT_UP_RATE);
            } else if (grade == CharPotGrade.Legendary.ordinal()) {
                gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_LEGENDARY_POT_UP_RATE);
            } else {
                gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_EPIC_POT_UP_RATE);
            }
            // update grades
            if (grade < CharPotGrade.Legendary.ordinal() && gradeUp) {
                grade++;
            }
            // set new potentials that weren't locked
            chr.getCharacterPotentialValueHolder().clear();
            for (int i = 0; i < 6; i++) {
                CharacterPotentialValueHolder cpvh = cpm.generateRandomPotentialTemp(preset, (byte) (i + 1));
                cpvh.setGrade(grade);
                characterPotentialValueHolders.add(cpvh);
            }
            chr.setCharacterPotentialValueHolders(characterPotentialValueHolders);
            chr.write(WvsContext.miracleCirculatorResult(characterPotentialValueHolders, itemID));
            if (grade >= CharPotGrade.Epic.ordinal() && gradeUp) {
                AchievementHandler.handleResetFirstCharPotentialRank(chr, grade);
            }
        } else if (itemID / 1000 == 2702) {
            if (grade == CharPotGrade.Legendary.ordinal()) {
                chr.chatMessage(SystemNotice, "該物品不能用於傳說潛能。");
                chr.dispose();
                return;
            }
            List<CharacterPotentialValueHolder> characterPotentialValueHolders = new LinkedList<>();
            boolean gradeUp;
            boolean gradeDown;
            if (grade == CharPotGrade.Epic.ordinal()) {
                gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_EPIC_POT_UP_RATE);
                gradeDown = Util.succeedProp(GameConstants.BASE_CHAR_EPIC_POT_DOWN_RATE);
            } else if (grade == CharPotGrade.Unique.ordinal()) {
                gradeUp = Util.succeedProp(GameConstants.BASE_CHAR_UNIQUE_POT_UP_RATE);
                gradeDown = Util.succeedProp(GameConstants.BASE_CHAR_UNIQUE_POT_DOWN_RATE);
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
            chr.getCharacterPotentialValueHolder().clear();
            for (int i = 0; i < 6; i++) {
                CharacterPotentialValueHolder cpvh = cpm.generateRandomPotentialTemp(preset, (byte) (i + 1));
                cpvh.setGrade(grade);
                characterPotentialValueHolders.add(cpvh);
            }
            chr.setCharacterPotentialValueHolders(characterPotentialValueHolders);
            chr.write(WvsContext.miracleCirculatorResult(characterPotentialValueHolders, itemID));

            if (grade >= CharPotGrade.Epic.ordinal() && gradeUp) {
                AchievementHandler.handleResetFirstCharPotentialRank(chr, grade);
            }

            int amount = Util.getRandom(0, 1);
            if (chr.getConsumeInventory().getEmptySlots() > amount) {
                chr.addItemToInventory(itemID, amount);
            }
            if (amount > 0) {
                chr.write(WvsContext.characterHonorGift(chr.getConsumeInventory().getEmptySlots(), amount));
            }
        }
        chr.consumeItem(itemID, 1);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_KARMA_CONSUME_ITEM_USE_REQUEST)
    public static void handleUserKarmaConsumeItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        short equipSlot = inPacket.decodeShort();
        useKarma(chr, slot, 0, equipSlot, false);
    }

    private static void useKarma(Char chr, short slot, int itemID, int equipSlot, boolean isCash) {
        Item item = null;
        if (isCash) {
            item = chr.getInventoryByType(InvType.CASH).getItemBySlot(slot);
        } else {
            item = chr.getInventoryByType(InvType.CONSUME).getItemBySlot(slot);
            itemID = item.getItemId();
        }
        if (item == null || item.getItemId() != itemID) {
            System.out.println("[" + chr.getName() + "] Mismatching use items.");
            chr.dispose();
            return;
        }
        if (!ItemConstants.isKarmaItem(itemID)) {
            String msg = String.format("Character %d tried to use a uncorrected (id %d) to use Karma. OPCode : %d",
                    chr.getId(), item.getItemId(), InHeader.USER_KARMA_CONSUME_ITEM_USE_REQUEST.getValue());
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, msg);
            chr.dispose();
            return;
        }
        Equip equip = (Equip) chr.getInventoryByType(InvType.EQUIP).getItemBySlot(equipSlot);
        if (equip == null) {
            System.out.println("[" + chr.getName() + "] Mismatching use equip.");
            chr.dispose();
            return;
        }
        if (equip.isCash()) {
            chr.chatMessage(SystemNotice, "不能對現金裝備使用神奇剪刀。");
            chr.dispose();
            return;
        }
        if (itemID == 5520000 && equip.getItemLevel() > 130) {
            chr.chatMessage(SystemNotice, "這把神奇剪刀不能用於 130 等以上的裝備。");
            chr.dispose();
            return;
        }
        equip.setTradeBlock(false);
        equip.removeAttribute(EquipAttribute.UnTradable);
        equip.addAttribute(EquipAttribute.UnTradableAfterTransaction);
        equip.updateToChar(chr);
        chr.consumeItem(itemID, 1);
    }

    @Handler(op = InHeader.USER_KAISER_COLOR_CHANGE_ITEM_USE_REQUEST)
    public static void handleUserKaiserColorChangeItemUseRequest(Char chr, InPacket inPacket) {
        int POS = inPacket.decodeInt();
        int itemID = inPacket.decodeInt();
        Item item = chr.getConsumeInventory().getItemBySlot(POS);
        if (item.getItemId() != itemID && !JobConstants.isKaiser(chr.getJob())) {
            chr.dispose();
            return;
        }
        int[] colors = {
                841, 842, 843, 758, 291, 317, 338, 339, 444, 445,
                446, 458, 461, 447, 450, 454, 455, 456, 457, 459,
                460, 462, 463, 464, 289, 4, 34, 35, 64, 9,
                10, 12, 11, 16, 17, 22, 24, 53, 61, 62,
                63, 67, 68, 109, 110, 111, 112, 113, 114, 115,
                116, 117, 121, 125, 128, 129, 145, 150};
        String extern = chr.getQRValueByKey(QuestConstants.KAISER_COLOR_CHANGE, "extern");
        String inner = chr.getQRValueByKey(QuestConstants.KAISER_COLOR_CHANGE, "inner");
        String premium = chr.getQRValueByKey(QuestConstants.KAISER_COLOR_CHANGE, "premium");
        if (extern == null || extern.isEmpty() || extern.equals("null")) {
            chr.addQRValue(QuestConstants.KAISER_COLOR_CHANGE, "extern=0");
        }
        if (inner == null || inner.isEmpty() || inner.equals("null")) {
            chr.addQRValue(QuestConstants.KAISER_COLOR_CHANGE, "inner=0");
        }
        if (premium == null || premium.isEmpty() || premium.equals("null")) {
            chr.addQRValue(QuestConstants.KAISER_COLOR_CHANGE, "primium=0");
        }
        if (itemID == 2350004) {
            chr.setQRValue(QuestConstants.KAISER_COLOR_CHANGE, "extern=" + colors[Randomizer.nextInt(colors.length)] + ";inner=" + inner + ";primium=" + premium);
        } else if (itemID == 2350005) {
            chr.setQRValue(QuestConstants.KAISER_COLOR_CHANGE, "extern=" + extern + ";inner=" + colors[Randomizer.nextInt(colors.length)] + ";primium=" + premium);
        } else if (itemID == 2350006) {
            chr.setQRValue(QuestConstants.KAISER_COLOR_CHANGE, "extern=842;inner=" + inner + ";premium=" + premium);
        } else if (itemID == 2350007) {
            chr.createQuestWithQRValue(QuestConstants.KAISER_COLOR_CHANGE, "extern=0;inner=0;primium=0");
        }
        chr.getField().broadcast(UserRemote.kaiserColorOrMorphChange(chr,
                Integer.parseInt(chr.getQRValueByKey(QuestConstants.KAISER_COLOR_CHANGE, "extern")),
                Integer.parseInt(chr.getQRValueByKey(QuestConstants.KAISER_COLOR_CHANGE, "inner")),
                Integer.parseInt(chr.getQRValueByKey(QuestConstants.KAISER_COLOR_CHANGE, "primium"))));
        chr.consumeItem(itemID, 1);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_CHAR_SLOT_INC_ITEM_USE_REQUEST)
    public static void handleUserCharSlotIncItemUseRequest(Char chr, InPacket inPacket) {
        int POS = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        Item item = chr.getConsumeInventory().getItemBySlot(POS);
        User user = chr.getUser();
        if (item == null || item.getItemId() != itemID) {
            chr.chatMessage("無法使用該物品。");
            chr.dispose();
            return;
        }
        if (user.getCharacterSlots() == GameConstants.MAX_CHARACTER_SLOTS) {
            chr.chatMessage("你的賬號無法再開啓更多角色欄位。");
            chr.dispose();
            return;
        }
        user.addCharacterSlots(1);
        user.updateUserCharacterSlotToSQL();
        chr.consumeItem(itemID, 1);
        chr.chatMessage("你的賬號已增加 01 個角色欄位。");
        chr.dispose();
    }

    @Handler(op = InHeader.USER_UI_ARCANE_SYMBOL_ENCHANCE_REQUEST)
    public static void handleUserArcaneSymbolEnchanceRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();
        switch (type) {
            case 0: {
                // Merge Arcane Symbol
                int equipPos = inPacket.decodeInt();
                Equip remove = (Equip) chr.getEquipInventory().getItemBySlot(equipPos);
                if (remove == null || !ItemConstants.isArcaneSymbol(remove.getItemId())
                        || remove.getQuantity() <= 0 || !remove.getSymbol().isPlainMergeMaterial()) {
                    chr.dispose();
                    return;
                }
                if (remove.getItemId() == 1712000) {
                    chr.dispose();
                    return;
                }
                Equip equip = (Equip) chr.getEquippedInventory().getItemByItemID(remove.getItemId());
                if (equip == null || equip.getSymbol().getLevel() < 1
                        || equip.getSymbol().getLevel() >= 20
                        || equip.getSymbol().getExp() >= equip.getSymbol().getGrowthCost()) {
                    chr.dispose();
                    return;
                }
                chr.consumeItem(remove);
                equip.getSymbol().setExp(equip.getSymbol().getExp() + 1);
                equip.saveToSQL();
                equip.updateToChar(chr);
                chr.dispose();
                break;
            }
            case 1: {
                int slot = inPacket.decodeInt() * -1;
                Equip item = (Equip) chr.getEquippedInventory().getItemBySlot(slot);
                if (item == null || !ItemConstants.isArcaneSymbol(item.getItemId())
                        || item.getQuantity() <= 0 || !item.getSymbol().canLevelUp()) {
                    System.out.println("[" + chr.getName() + "] Unknown item or mismatching use items.");
                    chr.dispose();
                    return;
                }
                int curLvl = item.getSymbol().getLevel();
                long arcCost = item.getSymbol().getGrowthCost();
                long cost = net.swordie.ms.client.character.items.EquipSymbol.mesoCost(item.getItemId(), item.getSymbol());
                if (chr.getMoney() < cost) {
                    chr.chatMessage(SystemNotice, "楓幣不足，無法升級該祕法符文。");
                    chr.dispose();
                    return;
                }
                int newArcExp = item.getSymbol().getExp() - arcCost <= 0 ? 0 : (int) (item.getSymbol().getExp() - arcCost);
                item.getSymbol().setLevel(curLvl + 1);
                item.getSymbol().setExp(newArcExp);
                item.getSymbol().setInc((short) Math.min(220, 10 * item.getSymbol().getLevel() + 20));
                if (JobConstants.isDemonAvenger(chr.getJob())) {
                    item.setiMaxHp((short) (item.getiMaxHp() + 2100));
                } else if (JobConstants.isXenon(chr.getJob())) {
                    item.setiStr((short) (item.getiStr() + 48));
                    item.setiDex((short) (item.getiDex() + 48));
                    item.setiLuk((short) (item.getiLuk() + 48));
                } else {
                    item.setBaseStat(chr.calculateMainStatForChar(), item.getBaseStat(chr.calculateMainStatForChar()) + 100);
                }
                if (chr.hasQuest(QuestConstants.UNION_ARTIFACT)) {
                    UnionArtifact.updateSpecialItemMission(chr, item.getItemId(), item.getSymbol().getLevel());
                }
                chr.deductMoney(cost);
                item.saveToSQL();
                item.updateToChar(chr);
                chr.write(UIContextPacket.ArcLevelUpEffect(slot));
                chr.dispose();
                break;
            }
            case 2: {
                // Merge Arcane Symbol
                int itemID = inPacket.decodeInt();
                Equip item = (Equip) chr.getEquippedInventory().getItemByItemID(itemID);
                int incExp = 0;
                if (itemID == 1712000 || item == null || !ItemConstants.isArcaneSymbol(itemID)
                        || item.getSymbol().getLevel() < 1 || item.getSymbol().getLevel() >= 20) {
                    chr.dispose();
                    return;
                }
                int curLvl = item.getSymbol().getLevel();
                int maxExpCost = Math.max(0, item.getSymbol().getGrowthCost() - item.getSymbol().getExp());
                List<Item> removes = new ArrayList<>(maxExpCost);
                for (Item i : chr.getEquipInventory().getItems()) {
                    if (i.getItemId() == item.getItemId() && i.getItemId() == itemID && incExp < maxExpCost
                            && i instanceof Equip material && material.getSymbol().isPlainMergeMaterial()
                            && material.getQuantity() > 0) {
                        incExp += 1;
                        removes.add(i);
                    }
                }
                for (Item removedItem : removes) {
                    chr.consumeItem(removedItem);
                }
                item.getSymbol().setExp(item.getSymbol().getExp() + incExp);
                item.saveToSQL();
                item.updateToChar(chr);
                //chr.chatMessage(ChatType.Notice, "[Notice]: " + StringData.getItemStringById(item.getItemId()) + " EXP got increased by " + incExp);
                chr.dispose();
                break;
            }
        }
    }

    @Handler(op = InHeader.USER_UI_SACRED_SYMBOL_ENCHANCE_REQUEST)
    public static void handleUserSacredSymbolEnchanceRequest(Char chr, InPacket inPacket) {
        int type = inPacket.decodeInt();
        switch (type) {
            case 0: {
                // Merge Sacred Symbol
                int equipPos = inPacket.decodeInt();
                Equip remove = (Equip) chr.getEquipInventory().getItemBySlot(equipPos);
                if (remove == null || !ItemConstants.isSacredSymbol(remove.getItemId())
                        || remove.getQuantity() <= 0 || !remove.getSymbol().isPlainMergeMaterial()) {
                    chr.dispose();
                    return;
                }
                Equip equip = (Equip) chr.getEquippedInventory().getItemByItemID(remove.getItemId());
                if (equip == null || equip.getSymbol().getLevel() < 1
                        || equip.getSymbol().getLevel() >= 11
                        || equip.getSymbol().getExp() >= equip.getSymbol().getGrowthCost()) {
                    chr.dispose();
                    return;
                }
                chr.consumeItem(remove);
                equip.getSymbol().setExp(equip.getSymbol().getExp() + 1);
                equip.saveToSQL();
                equip.updateToChar(chr);
                chr.dispose();
                break;
            }
            case 1: {
                int slot = inPacket.decodeInt() * -1;
                Equip item = (Equip) chr.getEquippedInventory().getItemBySlot(slot);
                if (item == null || !ItemConstants.isSacredSymbol(item.getItemId())
                        || item.getQuantity() <= 0 || !item.getSymbol().canLevelUp()) {
                    System.out.println("[" + chr.getName() + "] Unknown item or mismatching use items.");
                    chr.dispose();
                    return;
                }
                int curLvl = item.getSymbol().getLevel();
                long autCost = item.getSymbol().getGrowthCost();
                long cost = net.swordie.ms.client.character.items.EquipSymbol.mesoCost(item.getItemId(), item.getSymbol());
                if (chr.getMoney() < cost) {
                    chr.chatMessage(SystemNotice, "楓幣不足，無法升級該真實符文。");
                    chr.dispose();
                    return;
                }
                int newArcExp = item.getSymbol().getExp() - autCost <= 0 ? 0 : (int) (item.getSymbol().getExp() - autCost);
                item.getSymbol().setLevel(curLvl + 1);
                item.getSymbol().setExp(newArcExp);
                item.getSymbol().setInc((short) Math.min(110, 10 * item.getSymbol().getLevel()));
                if (JobConstants.isDemonAvenger(chr.getJob())) {
                    item.setiMaxHp((short) (item.getiMaxHp() + 4200));
                } else if (JobConstants.isXenon(chr.getJob())) {
                    item.setiStr((short) (item.getiStr() + 96));
                    item.setiDex((short) (item.getiDex() + 96));
                    item.setiLuk((short) (item.getiLuk() + 96));
                } else {
                    item.setBaseStat(chr.calculateMainStatForChar(), item.getBaseStat(chr.calculateMainStatForChar()) + 200);
                }
                if (chr.hasQuest(QuestConstants.UNION_ARTIFACT)) {
                    UnionArtifact.updateSpecialItemMission(chr, item.getItemId(), item.getSymbol().getLevel());
                }
                chr.deductMoney(cost);
                item.saveToSQL();
                item.updateToChar(chr);
                chr.write(UIContextPacket.AutLevelUpEffect(slot));
                chr.dispose();
                break;
            }
            case 2: {
                // Merge Sacred Symbol
                int itemID = inPacket.decodeInt();
                Equip item = (Equip) chr.getEquippedInventory().getItemByItemID(itemID);
                int incExp = 0;
                if (item == null || !ItemConstants.isSacredSymbol(itemID)
                        || item.getSymbol().getLevel() < 1 || item.getSymbol().getLevel() >= 11) {
                    chr.dispose();
                    return;
                }
                int curLvl = item.getSymbol().getLevel();
                int maxExpCost = Math.max(0, item.getSymbol().getGrowthCost() - item.getSymbol().getExp());
                List<Item> removes = new ArrayList<>(maxExpCost);
                for (Item i : chr.getEquipInventory().getItems()) {
                    if (i.getItemId() == item.getItemId() && i.getItemId() == itemID && incExp < maxExpCost
                            && i instanceof Equip material && material.getSymbol().isPlainMergeMaterial()
                            && material.getQuantity() > 0) {
                        incExp += 1;
                        removes.add(i);
                    }
                }
                for (Item removedItem : removes) {
                    chr.consumeItem(removedItem);
                }
                item.getSymbol().setExp(item.getSymbol().getExp() + incExp);
                item.saveToSQL();
                item.updateToChar(chr);
                //chr.chatMessage(ChatType.Notice, "[Notice]: " + StringData.getItemStringById(item.getItemId()) + " EXP got increased by " + incExp);
                chr.dispose();
                break;
            }
        }
    }

    @Handler(op = InHeader.USER_MOB_SUMMON_ITEM_USE_REQUEST)
    public static void handleUserMobSummonItemUseRequest(Char chr, InPacket inPacket) {
        removeItemUseRequest(chr, inPacket);
    }

    @Handler(op = InHeader.USER_UI_OPEN_ITEM_USE_REQUEST)
    public static void handleUserOpenItemUseRequest(Char chr, InPacket inPacket) {
        removeItemUseRequest(chr, inPacket);
    }

    private static void removeItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        Item item = chr.getConsumeInventory().getItemBySlot(slot);
        if (item == null || item.getItemId() != itemID || item.getQuantity() < 0) {
            chr.dispose();
            return;
        }
        chr.consumeItem(itemID, 1);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_BRIDLE_ITEM_USE_REQUEST)
    public static void handleUserBridleItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        short pos = inPacket.decodeShort();// // nPOS
        int itemID = inPacket.decodeInt();// nItemID
        int mobId = inPacket.decodeInt(); // apMob

        Item item = chr.getInventoryByType(CONSUME).getItemBySlot(pos);
        Mob mob = (Mob) chr.getField().getLifeByObjectID(mobId);
        if (item.getItemId() != itemID || (mob == null || mob.getHp() <= 0)) {
            chr.dispose();
            return;
        }
        if (chr.getHP() > 0 && ItemConstants.isBridleItem(itemID)) {
            ItemInfo bridle = ItemData.getItemInfoByID(itemID);
            if (bridle != null && mob.getTemplateId() == bridle.getMobID()) {
                if (mob.getHp() < (mob.getMaxHp() * bridle.getMobHP() / 100)) { //success/failure hp check
                    if (chr.canHold(bridle.getCreateID())) { // check if we have space
                        chr.write(MobPool.effectByItem(mob, item.getItemId(), true)); // do success handler
                        chr.consumeItem(itemID, 1); // consume the bridle
                        chr.addItemToInventory(bridle.getCreateID(), 1);//gives the bridle reward
                        mob.remove(false);
                    } else {
                        chr.chatMessage("你的背包沒有空位。");
                    }
                } else {
                    // do fail handler
                    chr.write(WvsContext.bridleMobCatchFail(item.getItemId(), item.getItemId() == 2270002));
                }
            }
        }
    }

    @Handler(op = InHeader.USER_CONSUME_HAIR_ITEM_USE_REQUEST)
    public static void handleUserConsumeHairItemUseRequest(Char chr, InPacket inPacket) {
        short pos = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();

        Item item = chr.getInventoryByType(CONSUME).getItemBySlot(pos);
        if (item == null || item.getItemId() != itemID || item.getQuantity() < 0) {
            System.out.println("[" + chr.getName() + "] Unknown item or mismatching use items.");
            chr.dispose();
            return;
        }
        ItemInfo ii = ItemData.getItemInfoByID(itemID);
        int cosmetic = 0;
        if (ii != null) {
            cosmetic = ii.getSpecStats().getOrDefault(SpecStat.cosmetic, 0);
        }
        if (cosmetic != 0) {
            chr.getScriptManager().changeCharacterLook(cosmetic);
            chr.consumeItem(itemID, 1);
        }
        chr.dispose();
    }

    @Handler(op = InHeader.USER_CONSUME_HAIR_ITEM_USE_REQUEST)
    public static void handleUserReturnEffectResponse(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        boolean isReturn = inPacket.decodeByte() != 0;
        var returnEquip = chr.returnEffectInfo.equip;
        if (returnEquip != null) {
            int pos = returnEquip.getBagIndex();
            InvType invType = returnEquip.getInvType();
            var equip = (Equip) chr.getInventoryByType(invType).getItemBySlot(pos);
            if (equip != null) {
                if (isReturn) {
                    for (EquipBaseStat ebs : ScrollStat.equipBaseStat) {
                        int cur = (int) returnEquip.getBaseStat(ebs);
                        equip.setBaseStat(ebs, cur);
                    }
                    equip.addStat(EquipBaseStat.tuc, -1);
                    equip.addStat(EquipBaseStat.cuc, 1);
                    equip.updateToChar(chr);
                }
                equip.removeAttribute(EquipAttribute.ReturnScroll);
                equip.updateToChar(chr);
            }
        }
        chr.returnEffectInfo.equip = null;
        chr.chatMessage("回家卷軸的保護效果已消失。");
    }
}
