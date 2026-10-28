package net.swordie.ms.handlers.login.tms282;

import io.netty.channel.Channel;
import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.world.field.Field;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Who receives a TMS 282.3 field packet (strategy). {@link #FIELD}: every client whose character is in the
 * speaker's {@link Field} instance (same field serial number, so the same channel) plus the speaker itself, through
 * the 265 {@link Server#broadcast} path (OutboundGuard included). The server runs one protocol profile, so every
 * client in the field is a 282 client.
 */
@FunctionalInterface
public interface Tms282FieldAudience {
    /**
     * Sends {@code packet} to the audience of {@code speaker}'s character {@code chr}.
     *
     * @return number of sockets it was written to, the speaker included
     */
    int broadcast(Tms282LoginState speaker, Char chr, OutPacket packet);

    /** The speaker only (the research probe, which has no other user). */
    Tms282FieldAudience SPEAKER_ONLY = (speaker, chr, packet) -> {
        speaker.send(packet);
        return 1;
    };

    /** The speaker's field; the speaker is included even when the 265 field entry did not register it. */
    Tms282FieldAudience FIELD = (speaker, chr, packet) -> {
        Set<Channel> channels = new LinkedHashSet<>();
        channels.add(speaker.client().getCh());
        Field field = chr.getField();
        if (field != null) {
            for (Char other : Server.get().getCharsByFieldId(field.getSN())) {
                if (other != null && other.getClient() != null && other.getClient().getCh() != null) {
                    channels.add(other.getClient().getCh());
                }
            }
        }
        Server.get().broadcast(packet, channels, "Fields");
        return channels.size();
    };
}
