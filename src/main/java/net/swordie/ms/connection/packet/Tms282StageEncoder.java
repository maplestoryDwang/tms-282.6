package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.StagePacketEncoder;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;

/**
 * TMS 282.3 field change SetField: LP02EF with characterData = 0 ({@link Tms282StagePackets#setFieldChangeField},
 * traced CLEAN in research/tms2823/setfield-trace builder-change). {@code Char.warp} builds it through the 265
 * {@code Stage.setField} ({@link StagePacketEncoder}), so every server-side warp - portal, script, command, revive -
 * sends it at the 265 point of the warp: after the old field was told the user left and the new field that it
 * arrived, before the new field's users, NPCs and mobs are sent to the client.
 */
public final class Tms282StageEncoder implements StagePacketEncoder {
    public static final Tms282StageEncoder INSTANCE = new Tms282StageEncoder();

    private Tms282StageEncoder() {
    }

    /**
     * The 265 channel argument is the 1-based server channel; the 282 header wants the 0-based wire channel, which
     * is {@code channelId - 1} for every normal channel (a VIP channel shown as wire 4..8 is served by server channel
     * 1 and would be shown as wire 0 after a warp: the ticket's wire channel is not kept after CP007E).
     */
    @Override
    public OutPacket changeField(Char chr, Field field, int channelId, int portal, Position position,
                                 boolean usingBuffProtector) {
        return Tms282StagePackets.setFieldChangeField(Math.max(0, channelId - 1), Tms282StagePackets.FieldSize.of(field),
                field.getId(), portal, chr.getHP(), position, usingBuffProtector,
                JobConstants.isExtendSpJob(chr.getJob()), FileTime.currentTime(),
                net.swordie.ms.client.character.tms282.Tms282MyHomeRuntime.templateMapId(chr, field));
    }
}
