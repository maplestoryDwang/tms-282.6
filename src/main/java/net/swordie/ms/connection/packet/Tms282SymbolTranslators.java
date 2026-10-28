package net.swordie.ms.connection.packet;

import net.swordie.ms.handlers.header.OutHeader;

/**
 * 282 祕法 / 真實符文 enhance results (research/tms2823/union-282/REPORT.md, part B): LP0AAF (48B3930), LP0AB0
 * (48B3DC0) and LP0AB1 (48B4210) read the 265 UIContextPacket bodies (i32 type, i32 code; type 1 + i32 slot plays the
 * level-up effect, type 2 = error code). Before, the three were dropped and only the LP009D unlock reached the client.
 */
public final class Tms282SymbolTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282SymbolTranslators INSTANCE = new Tms282SymbolTranslators();

    public static final int ARCANE_SYMBOL_ENHANCE = 0x0AAF;
    public static final int SACRED_SYMBOL_ENHANCE = 0x0AB0;
    public static final int SACRED_SYMBOL_ENHANCE_2 = 0x0AB1;

    private Tms282SymbolTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.ARCANE_SYMBOL_ENHANCE, p -> Tms282SkillLegacyTranslators.copy(p, ARCANE_SYMBOL_ENHANCE))
                .on(OutHeader.SACRED_SYMBOL_ENHANCE, Tms282SymbolTranslators::sacred)
                .on(OutHeader.SACRED_SYMBOL_ENHANCE_2, p -> Tms282SkillLegacyTranslators.copy(p, SACRED_SYMBOL_ENHANCE_2));
    }

    /** The grand authentic symbols (slots 1750..1752) have their own result window (LP0AB1). */
    static java.util.Optional<net.swordie.ms.connection.OutPacket> sacred(net.swordie.ms.connection.OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        byte[] rest = body.rest();
        int op = SACRED_SYMBOL_ENHANCE;
        if (rest.length >= 12) {
            java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(rest).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int slot = Math.abs(b.getInt(8));
            if (b.getInt(0) == 1 && slot >= net.swordie.ms.constants.ItemConstants.GRAND_AUTHENTIC_SYMBOL_BASE
                    && slot < net.swordie.ms.constants.ItemConstants.GRAND_AUTHENTIC_SYMBOL_END) {
                op = SACRED_SYMBOL_ENHANCE_2;
            }
        }
        net.swordie.ms.connection.OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(rest);
        return java.util.Optional.of(out);
    }
}
