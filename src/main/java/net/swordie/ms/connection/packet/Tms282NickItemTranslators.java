package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Title (稱號) packets of TMS 282.3 (research/tms2823/collab-frieren/README.md, item 1 of the follow-up):
 * REMOTE_SET_ACTIVE_NICK_ITEM -> LP0485. CUserPool::OnUserRemotePacket reads the i32 character id and dispatches
 * 0x485 to 5ADA8A0 -> 5456F80, which reads {@code D4 itemId, D1 hasMessage, [DecodeStr]}; traced CLEAN with
 * skill-trace/remote_trace.py (--entry 5456F80) for (3700821, 0), (0, 0) and (3700821, 1, "abc"). The 265 body
 * is the same except the custom-name tail of 3700623, which 282 does not read: that title is sent without it.
 * The static SAME_AS_265 row alone gives no profile opcode, hence this translator.
 */
public final class Tms282NickItemTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282NickItemTranslators INSTANCE = new Tms282NickItemTranslators();
    public static final int REMOTE_SET_ACTIVE_NICK_ITEM = 0x0485;

    private Tms282NickItemTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.REMOTE_SET_ACTIVE_NICK_ITEM, Tms282NickItemTranslators::remoteNickItem);
    }

    static Optional<OutPacket> remoteNickItem(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int charId = body.i32();
        int itemId = body.i32();
        int hasMessage = body.u8();
        if (hasMessage > 1) {
            return Optional.empty();
        }
        byte[] message = null;
        if (hasMessage == 1) {
            int n = body.u16();
            message = body.bytes(n);
        }
        // 3700623 custom-name tail (265 Char.encodeCustomNickName) is not read by 282: dropped.
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_SET_ACTIVE_NICK_ITEM);
        out.encodeInt(charId);
        out.encodeInt(itemId);
        out.encodeByte(hasMessage);
        if (message != null) {
            out.encodeShort(message.length);
            out.encodeArr(message);
        }
        return Optional.of(out);
    }
}
