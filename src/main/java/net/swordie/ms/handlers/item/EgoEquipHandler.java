package net.swordie.ms.handlers.item;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.EgoEquipPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;

public class EgoEquipHandler {

    @Handler(op = InHeader.EGO_EQUIP_GAUGE_COMPLETE_RETURN)
    public static void handleEgoEquipGaugeCompleteReturn(Char chr, InPacket inPacket) {
        chr.write(EgoEquipPacket.EgoEquip_GaugeComplete());
        chr.dispose();
    }

    @Handler(op = InHeader.EGO_EQUIP_CREATE_UPGRADE_ITEM)
    public static void handleEgoEquipCreateUpgradeItem(Char chr, InPacket inPacket) {
        chr.dispose();
        int Idx = inPacket.decodeInt();
        int reqCubeMeso = 100000;
        int reqCubeWp = 1;
        boolean changePotential = false;
        boolean isUpgradeWeapon = false;
        chr.write(EgoEquipPacket.EgoEquip_CreateUpgradeItemCostInfo(Idx, reqCubeMeso, reqCubeWp, changePotential, isUpgradeWeapon));
        chr.dispose();
    }


    @Handler(op = InHeader.EGO_EQUIP_CREATE_UPGRADE_ITEM_COST_REQUEST)
    public static void handleEgoEquipCheckUpdateItemCostRequest(Char chr, InPacket inPacket) {
        //Index = 1 is Cube, other is Fire?
        int Idx = inPacket.decodeInt();
        int reqCubeMeso = 100000;
        int reqCubeWp = 696969;
        boolean changePotential = false;
        boolean isUpgradeWeapon = false;
        chr.write(EgoEquipPacket.EgoEquip_CreateUpgradeItemCostInfo(Idx, reqCubeMeso, reqCubeWp, changePotential, isUpgradeWeapon));
        chr.dispose();
    }

    @Handler(op = InHeader.EGO_EQUIP_CHECK_UPDATE_ITEM_REQUEST)
    public static void handleEgoEquipCheckUpdateItemRequest(Char chr, InPacket inPacket) {
        // This Function will put scroll in Slot
        // Inventory Type.
        int inventoryType = inPacket.decodeInt();
        // Position Of Item in Inventory.
        int itemPosition = inPacket.decodeInt();
        // IDK1: 1 Always 1
        // May be a var result for Zero Enhance?
        int idk1 = inPacket.decodeInt();
        // ID of type sub Slot (Shield Slot)
        int slotType = inPacket.decodeInt();
        // A slot between of 2 weapon in Weapon UI of Zero
        int weaponWindowSlot = inPacket.decodeInt();
        System.out.printf("Inv: %d, ItemSlot: %d, IDK1: %d, IDK2: %d, weaponWindowSlot: %d%n", inventoryType, itemPosition, idk1, slotType, weaponWindowSlot);
        chr.write(EgoEquipPacket.EgoEquip_CheckUpgradeItemResult(weaponWindowSlot, true));
        chr.dispose();
    }

    @Handler(op = InHeader.EGO_EQUIP_TALK_REQUEST)
    public static void handleEgoEquipTalkRequest(Char chr, InPacket inPacket) {
        chr.getScriptManager().startScript(chr, 0, 0, "ZeroTalk", ScriptType.Npc);
    }

    @Handler(op = InHeader.EGO_EQUIP_GROWTH_REQUEST)
    public static void handleEgoEquipGrowthRequest(Char chr, InPacket inPacket) {
        chr.getScriptManager().startScript(chr, 0, 0, "ZeroGrowth", ScriptType.Npc);
    }

    @Handler(op = InHeader.INHERITANCE_INFO_REQUEST)
    public static void handleInheritanceInfoRequest(Char chr, InPacket inPacket) {
        int lazuliWeaponID = inPacket.decodeInt();
        int lapisWeaponID = lazuliWeaponID - 10000;
        boolean isStatTrans = inPacket.decodeByte() != 0;
        byte idk1 = inPacket.decodeByte();
        byte idk2 = inPacket.decodeByte();
        if (!ItemConstants.isLongSword(lazuliWeaponID)) {
            //Ban this mtherfucker
            return;
        }
        int lazuliCurrentSlot = chr.getEquippedInventory().getItemBySlot(-11).getItemId();
        int lapisCurrentSlot = chr.getEquippedInventory().getItemBySlot(-10).getItemId();

        if (ItemConstants.isLongSword(lazuliCurrentSlot)) {
            if (lazuliCurrentSlot != lazuliWeaponID) {
                return;
            }
        }
        if (ItemConstants.isLongSword(lapisCurrentSlot)) {
            if (lapisCurrentSlot != lazuliWeaponID) {
                return;
            }
        }
        ScriptManagerImpl scriptManagerImp = new ScriptManagerImpl(chr);
        int nNextLevel = lazuliWeaponID - 1572000 + 1;
        if (nNextLevel == 8) {
            if (!scriptManagerImp.hasItem(4310216, 1)) {
                return;
            }
            scriptManagerImp.consumeItem(4310216, 1);
        } else if (nNextLevel == 9) {
            if (!scriptManagerImp.hasItem(4310217, 1)) {
                return;
            }
            scriptManagerImp.consumeItem(4310217, 1);
        }
        chr.write(WvsContext.inheritanceComplete());
        scriptManagerImp.upgradeZeroWeapon(lazuliWeaponID + 1, lapisWeaponID + 1, isStatTrans);
        chr.dispose();
    }
}
