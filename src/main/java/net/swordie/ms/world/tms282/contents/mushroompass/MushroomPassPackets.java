package net.swordie.ms.world.tms282.contents.mushroompass;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.world.tms282.contents.common.AccountLedger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * The 282 packets of the pass window {@code CUIHyperMushPass} ({@code UI/UIWindowSG2.img/HyperMushPass}, uiType
 * 120018), static analysis of MapleStory.exe 282.4 (research/tms2823/content-1005/feasibility.md section 2):
 * <ul>
 *     <li>LP065D season table (CUserLocal 578DA38 -> 60E17F0 -> 431820, every field read with DecodeBuffer):
 *     {@code i32 n, n x {FILETIME start, FILETIME end, i32 passQuest(+0x14), i32 unknown(+0x10), i32 claimQuest(+0x18),
 *     i32 m, m x {i32 level, rewards basic, i32 tier, rewards premium}}}, rewards (431C20) =
 *     {@code i32 k (<= 1,000,000), k x {i32 itemId, i32 count, str}}. The active season is the first whose
 *     [start, end] holds the client's "now" (431A10).</li>
 *     <li>LP04D9 OPEN_UI {@code i32 120018}: UI_Open 5DDD8A0 has no explicit case for 120018; its default path looks
 *     the id up in the registry where 326C650 registered the class.</li>
 *     <li>CP04FC claim (sender 3269C5E in the window's button handler): {@code 4 zero bytes, i32 idx (page*4+slot),
 *     u8 basicClaimable, u8 premiumClaimable}, then the socket's u32 tail. The window has no reply decoder: the
 *     server answers with the QR-ex update of {@code claimQuest} "b&lt;idx&gt;" and the items.</li>
 * </ul>
 */
public final class MushroomPassPackets {
    public static final int SEASONS = 0x065D;
    public static final int OPEN_UI = 0x04D9;
    public static final int CLAIM_REQUEST = 0x04FC;
    public static final int UI_TYPE = 120018;

    private MushroomPassPackets() {
    }

    public static OutPacket seasons(List<MushroomPassSeason> seasons) {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, SEASONS);
        out.encodeInt(seasons.size());
        for (MushroomPassSeason s : seasons) {
            out.encodeLong(MushroomPassSeason.fileTime(s.start()));
            out.encodeLong(MushroomPassSeason.fileTime(s.end()));
            out.encodeInt(s.passQuest());
            out.encodeInt(s.unknown());
            out.encodeInt(s.claimQuest());
            out.encodeInt(s.levels().size());
            for (MushroomPassSeason.Level l : s.levels()) {
                out.encodeInt(l.level());
                rewards(out, l.basic());
                out.encodeInt(l.tier());
                rewards(out, l.premium());
            }
        }
        return out;
    }

    private static void rewards(OutPacket out, List<AccountLedger.Reward> rewards) {
        out.encodeInt(rewards.size());
        for (AccountLedger.Reward r : rewards) {
            out.encodeInt(r.itemId());
            out.encodeInt(r.quantity());
            out.encodeString("");
        }
    }

    public static OutPacket openWindow() {
        OutPacket out = OutPacket.forProfile(ProtocolVersion.TMS_282_3, OPEN_UI);
        out.encodeInt(UI_TYPE);
        return out;
    }

    /** CP04FC body. */
    public record ClaimRequest(int head, int index, boolean basic, boolean premium) {
        public static ClaimRequest parse(byte[] packet) {
            if (packet == null || packet.length < 2) {
                throw new IllegalArgumentException("CP04FC: no opcode");
            }
            int op = (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
            if (op != CLAIM_REQUEST) {
                throw new IllegalArgumentException(String.format("CP04FC: opcode %04X", op));
            }
            int body = packet.length - 2;
            if (body != 10 && body != 14) {
                throw new IllegalArgumentException("CP04FC: body of " + body + " bytes");
            }
            ByteBuffer b = ByteBuffer.wrap(packet, 2, body).order(ByteOrder.LITTLE_ENDIAN);
            int head = b.getInt();
            int index = b.getInt();
            int basic = b.get() & 0xFF;
            int premium = b.get() & 0xFF;
            if (basic > 1 || premium > 1) {
                throw new IllegalArgumentException("CP04FC: flags " + basic + "/" + premium);
            }
            return new ClaimRequest(head, index, basic == 1, premium == 1);
        }
    }
}
