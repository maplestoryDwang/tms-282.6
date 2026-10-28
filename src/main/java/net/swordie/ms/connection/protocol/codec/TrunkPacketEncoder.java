package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.trunk.TrunkDlg;
import net.swordie.ms.connection.OutPacket;

/**
 * Codec family of the storage (trunk) dialog packet (265 OutHeader.TRUNK_DLG; TMS 282.3 LP097B). The 265
 * {@code FieldPacket.trunkDlg} builds through {@link #active()}: GMS 265 registers no encoder and falls back to
 * {@link Gms265TrunkPackets} (the unchanged 265 bytes); TMS 282.3 registers {@code Tms282TrunkPackets}.
 */
public interface TrunkPacketEncoder {

    /** One trunk dialog message (open, an update with the whole storage, or a plain result). */
    OutPacket trunkDlg(TrunkDlg trunkDlg);

    /** The active profile's encoder (or its version's fallback), else the GMS 265 one. */
    static TrunkPacketEncoder active() {
        return ProfileEncoders.find(TrunkPacketEncoder.class).orElse(Gms265TrunkPackets.INSTANCE);
    }
}
