package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.GoldHammerResult;
import net.swordie.ms.handlers.header.OutHeader;

public class EgoEquipPacket {

    public static OutPacket EgoEquip_GaugeComplete() {
        return new OutPacket(OutHeader.EGO_GAUGE_COMPLETE);
    }

    public static OutPacket EgoEquip_CreateUpgradeItemCostInfo(int Idx, int reqCubeMeso, int reqCubeWp, boolean changePotential, boolean isUpgradeWeapon) {
        OutPacket outPacket = new OutPacket(OutHeader.EGO_CREATE_UPGRADE_ITEM_COST_INFO);

        outPacket.encodeInt(Idx);
        outPacket.encodeInt(reqCubeMeso);
        outPacket.encodeInt(reqCubeWp);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket EgoEquip_CheckUpgradeItemResult(int index, boolean show) {
        OutPacket outPacket = new OutPacket(OutHeader.EGO_CHECK_UPGRADE_ITEM_RESULT);

        outPacket.encodeShort(1);
        outPacket.encodeByte(0);
        outPacket.encodeInt(index);

        return outPacket;
    }

    public static OutPacket EgoEquip_ItemUpgradeEffect(boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.EGO_ITEM_UPGRADE_EFFECT);

        outPacket.encodeByte(success ? 1 : 0);
        outPacket.encodeByte(1);

        return outPacket;
    }

    public static OutPacket EgoEquip_SocketEffect(boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.EGO_ITEM_SOCKET_EFFECT);

        outPacket.encodeByte(success ? 2 : 3);

        return outPacket;
    }
}
