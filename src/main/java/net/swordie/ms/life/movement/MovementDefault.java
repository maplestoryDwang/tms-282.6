package net.swordie.ms.life.movement;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.Life;

public class MovementDefault extends MovementBase {

    public MovementDefault(InPacket inPacket, byte command) {
        super();
        this.command = command;
        if (command == 81 || command == 94) {
            MPA = inPacket.decodeShort();
            Param1 = inPacket.decodeShort();
            Param2 = inPacket.decodeShort();
            Param3 = inPacket.decodeShort();
            Param4 = inPacket.decodeShort();
            Param5 = inPacket.decodeShort();
            Param6 = inPacket.decodeShort();
        } else {
            moveAction = inPacket.decodeByte();
            fh = inPacket.decodeShort();
            forcedStop = inPacket.decodeByte();
        }
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(getCommand());
        if (getCommand() == 81 || getCommand() == 94) {
            outPacket.encodeShort(getMPA());
            outPacket.encodeShort(getParam1());
            outPacket.encodeShort(getParam2());
            outPacket.encodeShort(getParam3());
            outPacket.encodeShort(getParam4());
            outPacket.encodeShort(getParam5());
            outPacket.encodeShort(getParam6());
        } else {
            outPacket.encodeByte(getMoveAction());
            outPacket.encodeShort(getFh());
            outPacket.encodeByte(getForcedStop());
        }
    }

    @Override
    public void applyTo(Char chr) {
        chr.setFoothold(getFh());
        chr.setMoveAction(getMoveAction());
    }

    @Override
    public void applyTo(Life life) {
        life.setFh(getFh());
        life.setMoveAction(getMoveAction());
    }
}
