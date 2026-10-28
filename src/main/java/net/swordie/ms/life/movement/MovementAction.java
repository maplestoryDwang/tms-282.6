package net.swordie.ms.life.movement;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.Life;

public class MovementAction extends MovementBase {
    public MovementAction(InPacket inPacket, byte command) {
        super();
        this.command = command;
        if (command == 94) {
            idk = inPacket.decodeInt();
        }
        moveAction = inPacket.decodeByte();
        fh = inPacket.decodeShort();
        forcedStop = inPacket.decodeByte();
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(getCommand());
        if (getCommand() == 94) {
            outPacket.encodeInt(getIdk());
        }
        outPacket.encodeByte(getMoveAction());
        outPacket.encodeShort(getFh());
        outPacket.encodeByte(getForcedStop());
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
