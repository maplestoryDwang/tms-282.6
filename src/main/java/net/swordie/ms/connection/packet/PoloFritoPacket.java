package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.List;

public class PoloFritoPacket {

    public static OutPacket setBountyHuntingStage(int stage) {
        OutPacket outPacket = new OutPacket(OutHeader.POLO_FRITO_BOUNTY_HUNTING_STAGE);

        outPacket.encodeInt(stage);

        return outPacket;
    }

    public static OutPacket setTowerDefenseWave(int wave) {
        OutPacket outPacket = new OutPacket(OutHeader.POLO_FRITO_TOWN_DEFENSE_WAVE);

        outPacket.encodeInt(wave);

        return outPacket;
    }

    public static OutPacket setTowerDefenseLife(int life) {
        OutPacket outPacket = new OutPacket(OutHeader.POLO_FRITO_TOWN_DEFENSE_LIFE);

        outPacket.encodeInt(life);

        return outPacket;
    }

    public static OutPacket courtShipDanceState(int state) {
        OutPacket outPacket = new OutPacket(OutHeader.POLO_FRITO_COURTSHIP_DANCE_STATE);

        outPacket.encodeInt(state);

        return outPacket;
    }

    public static OutPacket courtShipDanceCommand(List<List<Integer>> list) {
        OutPacket outPacket = new OutPacket(OutHeader.POLO_FRITO_COURTSHIP_DANCE_COMMAND);

        outPacket.encodeInt(list.size());
        for (List<Integer> list1 : list) {
            outPacket.encodeInt(list1.size());
            for (Integer i : list1) {
                outPacket.encodeInt(i);
            }
        }

        return outPacket;
    }
}
