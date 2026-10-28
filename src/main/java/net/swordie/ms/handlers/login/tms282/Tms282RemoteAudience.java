package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.world.field.Field;

/**
 * Who receives a TMS 282.3 CUserRemote packet about a character (strategy): {@link #FIELD_OTHERS} is every other
 * client in the character's {@link Field} instance, through the 265 {@link Field#broadcast(OutPacket, Char)} path
 * (OutboundGuard included) that REMOTE_MOVE already uses. Unlike {@link Tms282FieldAudience} the character's own
 * client is never included: LP048E / LP048F / LP0490 describe a remote user.
 */
@FunctionalInterface
public interface Tms282RemoteAudience {
    void broadcast(Char chr, OutPacket packet);

    Tms282RemoteAudience FIELD_OTHERS = (chr, packet) -> {
        Field field = chr.getField();
        if (field != null) {
            field.broadcast(packet, chr);
        }
    };
}
