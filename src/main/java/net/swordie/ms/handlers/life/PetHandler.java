package net.swordie.ms.handlers.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.achievement.AchievementHandler;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.PetItem;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.jobs.adventurer.thief.NightLord;
import net.swordie.ms.client.jobs.adventurer.thief.Thief;
import net.swordie.ms.client.jobs.cygnus.NightWalker;
import net.swordie.ms.client.jobs.cygnus.ThunderBreaker;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.DropPool;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.PetPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.EventConstants;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.enums.InventoryOperation;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.life.pet.PetSkill;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PetHandler {

    @Handler(op = InHeader.USER_ACTIVATE_PET_REQUEST)
    public static void handleUserActivatePetRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.NoPet.getVal()) > 0) {
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); // tick
        short slot = inPacket.decodeShort();
        Item item = chr.getCashInventory().getItemBySlot(slot);
        if (!(item instanceof PetItem)) {
            item = chr.getConsumeInventory().getItemBySlot(slot);
        }
        // Two of the same condition, as item had to be re-assigned
        if (!(item instanceof PetItem petItem)) {
            chr.chatMessage(String.format("發生未知錯誤。未在欄位 %s 找到物品。", slot));
            return;
        }
        if (petItem.getActiveState() == 0) {
            if (!petItem.canSummon() || chr.getPets().size() >= GameConstants.MAX_PET_AMOUNT) {
                chr.dispose();
                return;
            }
            Pet pet = petItem.createPet(chr);
            petItem.setActiveState((byte) (pet.getIdx() + 1));
            chr.addPet(pet);
            chr.getJobHandler().givePetPassiveBuffs(true);
            chr.initPets();
            if (EventConstants.HYPER_BURNING_MAX && chr.hasQuest(102431)
                    && !"2".equals(chr.getQRValueByKey(102431, "step"))) {
                chr.getScriptManager().startScript(chr, 102431, "q102431s_1", ScriptType.Quest);
            }
        } else {
            Pet pet = chr.getPets().stream().filter(p -> p.getItem() == petItem).findFirst().orElse(null);
            petItem.setActiveState((byte) 0);
            if (pet != null) {
                chr.removePet(pet);
                chr.getJobHandler().givePetPassiveBuffs(false);
                chr.getField().broadcast(PetPacket.deactivated(chr.getId(), pet.getIdx()));
            }
        }
        petItem.updateToChar(chr);
        chr.dispose();
    }

    @Handler(op = InHeader.USER_PET_FOOD_ITEM_USE_REQUEST)
    public static void handleUserPetFoodItemUseRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.NoPet.getVal()) > 0) {
            chr.dispose();
            return;
        }
        inPacket.decodeInt(); // update_time
        short slot = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        Item item = chr.getConsumeInventory().getItemBySlot(slot);
        if (item != null && item.getItemId() == itemID && item.getQuantity() > 0
                && !chr.getPets().isEmpty()) {
            chr.consumeItem(item, 1);
            for (Pet pet : chr.getPets()) {
                PetItem pi = pet.getItem();
                pi.setRepleteness((byte) (Math.min(100, pi.getRepleteness() + 30)));
                chr.getField().broadcast(PetPacket.actionCommand_GiveFood(chr.getId(), pet.getIdx(), 2));
                pi.updateToChar(chr);
            }
            if (EventConstants.HYPER_BURNING_MAX && chr.hasQuest(102431)
                    && !"5".equals(chr.getQRValueByKey(102431, "step"))) {
                chr.getScriptManager().startScript(chr, 102431, "q102431s_3", ScriptType.Quest);
            }
            AchievementHandler.handlePetFood(chr);
        }
    }

    @Handler(op = InHeader.USER_CASH_PET_PICK_UP_ON_OFF_REQUEST)
    public static void handleUserCashPetPickUpOnOffRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        final short mode = inPacket.decodeShort();
        chr.setPetLoot(mode == 1);
        chr.write(WvsContext.cashPetPickUpOnOffResult(mode));
    }

    @Handler(op = InHeader.PET_MOVE)
    public static void handlePetMove(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.NoPet.getVal()) > 0) {
            chr.dispose();
            return;
        }
        int petID = inPacket.decodeInt();
        inPacket.decodeByte(); // ?
        MovementInfo movementInfo = new MovementInfo(inPacket);
        Pet pet = chr.getPetByIdx(petID);
        if (pet != null) {
            movementInfo.applyTo(pet);
            chr.getField().broadcast(PetPacket.move(chr.getId(), petID, movementInfo), chr);
        }
    }

    @Handler(op = InHeader.PET_DROP_PICK_UP_REQUEST)
    public static void handlePetDropPickUpRequest(Char chr, InPacket inPacket) {
        Field field = chr.getField();
        if ((field.getFieldLimit() & FieldOption.NoPet.getVal()) > 0) {
            return;
        }
        int petID = inPacket.decodeInt();
        byte fieldKey = inPacket.decodeByte();
        inPacket.decodeInt(); // update_time
        inPacket.decodeInt(); // v15
        Position pos = inPacket.decodePosition();
        int dropID = inPacket.decodeInt();
        inPacket.decodeInt(); // v16
        Pet picker = chr.getPetByIdx(petID);
        if (picker == null || !picker.getItem().hasPetSkill(PetSkill.ITEM_PICKUP)) {
            return;
        }
        Life life = field.getLifeByObjectID(dropID);
        if (life == null) {
            field.broadcast(DropPool.dropLeaveField(dropID, petID));
            return;
        }
        if (life instanceof Drop drop) {
            List<Integer> ignored = picker.getItem().getExceptionList();
            if (drop.getItem() != null && picker.getItem().hasPetSkill(PetSkill.IGNORE_ITEM)
                    && ignored != null && ignored.contains(drop.getItem().getItemId())) {
                return;
            }
            boolean success = drop.canBePickedUpByPet() && drop.canBePickedUpBy(chr) && chr.addDrop(drop, true);
            if (success) {
                AchievementHandler.handleLoot(chr, drop);
                field.removeDrop(dropID, chr.getId(), false, petID);
            } else {
                chr.dispose();
            }
        }
    }

    @Handler(op = InHeader.PET_STAT_CHANGE_ITEM_USE_REQUEST)
    public static void handlePetStatChangeItemUseRequest(Char chr, InPacket inPacket) {
        inPacket.decodeByte();
        inPacket.decodeInt(); // update_time
        short pos = inPacket.decodeShort();
        int itemID = inPacket.decodeInt();
        Item item = chr.getConsumeInventory().getItemBySlot(pos);
        if (item == null || itemID != item.getItemId()) {
            return;
        }
        if (Arrays.stream(FieldConstants.BLOCKED_RUNE_MAPS).anyMatch(m -> chr.getField() != null && m == chr.getField().getId())) {
            chr.dispose();
            return;
        }
        chr.useStatChangeItem(item, true);
    }

    @Handler(op = InHeader.PET_ACTION)
    public static void handlePetAction(Char chr, InPacket inPacket) {
        int petID = inPacket.decodeInt();
        inPacket.decodeInt(); // tick
        byte command1 = inPacket.decodeByte();
        byte command2 = inPacket.decodeByte();
        String msg = inPacket.decodeString();
        chr.getField().broadcast(PetPacket.action(chr.getId(), petID, command1, command2, msg), chr);
        chr.getField().broadcast(PetPacket.actionSpeak(chr.getId(), petID, msg), chr);
    }

    @Handler(op = InHeader.PET_INTERACTION_REQUEST)
    public static void handlePetInteractionRequest(Char chr, InPacket inPacket) {
        int petID = inPacket.decodeInt();
        Pet pet = chr.getPetByIdx(petID);
        if (pet == null) {
            return;
        }

        PetItem pi = pet.getItem();
        inPacket.decodeByte();
        byte command = inPacket.decodeByte();

        if (Util.succeedProp(ItemData.getPetCommands().getOrDefault(command, 100))) {
            pi.setTameness((short) Math.min(pi.getTameness() + 100, 30000));
            pi.updateToChar(chr);
            chr.getField().broadcast(PetPacket.actionCommand(chr.getId(), pet.getIdx(), command, true));
        } else {
            chr.getField().broadcast(PetPacket.actionCommand(chr.getId(), pet.getIdx(), command, false));
        }
    }

    @Handler(op = InHeader.USER_REGISTER_PET_AUTO_BUFF_REQUEST)
    public static void handleUserRegisterPetAutoBuffRequest(Char chr, InPacket inPacket) {
        int petIdx = inPacket.decodeInt();
        int buffIndex = inPacket.decodeInt();
        int skillID = inPacket.decodeInt();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        Skill skill = chr.getSkill(skillID);
        Pet pet = chr.getPetByIdx(petIdx);
        if (pet == null || buffIndex < 0 || buffIndex > 1) {
            chr.dispose();
            return;
        }
        int coolTime = si == null ? 0 : si.getValue(SkillStat.cooltime, 1);
        if (skillID != 0 && (si == null || pet == null || !pet.getItem().hasPetSkill(PetSkill.AUTO_BUFF)
                || skill == null)) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character %s tried to illegally add a pet skill (skillID = %d, skill = %s, "
                    + "pet = %s)", chr.getName(), skillID, skill, pet));
            chr.dispose();
            return;
        }
        //pet.getItem().setAutoBuffSkill(skillID);
        chr.setQRValueByKey(101080 + petIdx, 10 * petIdx + buffIndex, skillID);
        //pet.getItem().updateToChar(chr);
    }

    @Handler(op = InHeader.PET_UPDATE_EXCEPTION_LIST_REQUEST)
    public static void handlePetUpdateExceptionListRequest(Char chr, InPacket inPacket) {
        int petIdx = inPacket.decodeInt();
        long petSn = inPacket.decodeLong();
        int size = Byte.toUnsignedInt(inPacket.decodeByte());
        Pet pet = chr.getPetByIdx(petIdx);
        if (pet == null || pet.getPetLockerSN() != petSn || !pet.getItem().hasPetSkill(PetSkill.IGNORE_ITEM)
                || inPacket.getUnreadAmount() != size * Integer.BYTES) {
            return;
        }
        List<Integer> exceptionList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int id = inPacket.decodeInt();
            exceptionList.add(id);
        }
        pet.getItem().setExceptionList(exceptionList);
        pet.getItem().updateToChar(chr);
    }

    @Handler(op = InHeader.PET_OPEN_SHOP)
    public static void handlePetOpenShop(Char chr, InPacket inPacket) {
        int petID = inPacket.decodeInt();
        Pet pet = chr.getPetByIdx(petID);
        if (pet == null || !pet.getItem().hasPetSkill(PetSkill.PET_SHOP)) {
            return;
        }
        chr.getScriptManager().openShop(1012004);
    }
}
