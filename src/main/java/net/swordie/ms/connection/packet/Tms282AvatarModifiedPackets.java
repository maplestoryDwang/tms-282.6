package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.social.tms282.RingRecordService;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.AvatarModifiedEncoder;
import net.swordie.ms.enums.AvatarModifiedMask;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * TMS 282.3 LP048D REMOTE_AVATAR_MODIFIED (CUserPool::OnUserRemotePacket case 0x48D -> CUserRemote handler
 * 5ADAD20). Opcode: ingame-lp-map-2823.tsv pairs 0x48D with 265 1115 between the verified anchors 0x48B and 0x48E
 * (both +50); the read sequence below (mask, the four mask blocks in 265 order, three ring flags, set-item ints) is
 * the 265 packet's, which confirms the pairing. Layout read by the emulated client (research/tms2823/combat-trace,
 * {@code --entry 5ADAD20}, the dispatcher reads the i32 id first) [verified, CLEAN]:
 * <pre>
 * i32 characterId                    (dispatcher)
 * u8  mask                           5ADAD63
 * [mask &amp; 1] AvatarLook (901AF0) + str   5ADAE19 / 5ADAE26   (265: look + "")
 * [mask &amp; 8] AvatarLook               5ADAFC9               (265 SubAvatarLook)
 * [mask &amp; 9] u32, {u8 slot, u32 item}* 0xFF, u32, {u8 slot, u32 item}* 0xFF   8D75E0 (282 only; sent empty:
 *                                    0, FF, 0, FF -- two slot-to-item maps, slots 1..0x27 but 0x0B, item 0 ignored)
 * [mask &amp; 2] u8 speed                 5ADB047
 * [mask &amp; 4] u8 carry item effect     5ADB085
 * u8 couple [i32 n, n x (Buf16 sn+pairSn, str, i32 item)]   5ADB098 / 5ADB144 / 5ADB17D / 5ADB18A / 5ADB1E5
 * u8 friendship [i32 n, n x (Buf16, i32 item)]             5ADB72A / 5ADB7E4 / 5ADB80D / 5ADB815
 * u8 marriage [i32 groom, i32 bride, i32 item]             5ADBD34 / 5ADBD40 / 5ADBD53 / 5ADBD61
 * 6 x i32                            5ADBE10..5ADBE56   265 writes 4: set item id, star force, arcane, authentic
 * u8                                 5ADBE64   282 only
 * u8                                 5694E07 (5694DD0)  282 only; non-zero reads more (not sent)
 * </pre>
 * [presumed] the first four ints keep the 265 meaning and the two new ints and bytes are 0. The rings are the
 * wearer's worn rings ({@link RingRecordService#remoteRings}, traced CLEAN in research/tms2823/ring-records;
 * 265 wrote the full 48-byte MarriageRecord where 282 reads three ints). The AvatarLook is the 282 one ({@link Tms282AvatarEncoder#encodeLook}, ends with the name).
 */
public final class Tms282AvatarModifiedPackets implements AvatarModifiedEncoder {
    public static final int REMOTE_AVATAR_MODIFIED = 0x048D;

    private final Charset charset;

    public Tms282AvatarModifiedPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public OutPacket avatarModified(Char chr, byte mask, byte carryItemEffect) {
        Objects.requireNonNull(chr, "chr");
        AvatarLook look = chr.getAvatarData().getAvatarLook();
        int speed = 0;
        if ((mask & AvatarModifiedMask.Speed.getVal()) != 0 && chr.getTemporaryStatManager() != null) {
            speed = chr.getTemporaryStatManager().getOption(CharacterTemporaryStat.Speed).nOption;
        }
        return avatarModified(chr.getId(), chr.getName(), look, mask, speed, carryItemEffect,
                chr.getCompletedSetItemID(), chr.getTotalChuc(false), chr.getTotalArc(), chr.getTotalAut(),
                RingRecordService.remoteRings(chr));
    }

    /** LP048D from plain values (checks and callers without a {@link Char}). */
    public OutPacket avatarModified(int characterId, String name, AvatarLook look, byte mask, int speed,
                                    byte carryItemEffect, int setItemId, int starForce, int arcane, int authentic) {
        return avatarModified(characterId, name, look, mask, speed, carryItemEffect, setItemId, starForce, arcane,
                authentic, RingRecordService.RemoteRings.NONE);
    }

    /** LP048D with the wearer's rings ({@code RemoteRings.NONE}: the ring-less bytes). */
    public OutPacket avatarModified(int characterId, String name, AvatarLook look, byte mask, int speed,
                                    byte carryItemEffect, int setItemId, int starForce, int arcane, int authentic,
                                    RingRecordService.RemoteRings rings) {
        Objects.requireNonNull(rings, "rings");
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE_AVATAR_MODIFIED);
        out.encodeInt(characterId);
        out.encodeByte(mask);
        if ((mask & AvatarModifiedMask.AvatarLook.getVal()) != 0) {
            Tms282AvatarEncoder.encodeLook(out, look, name, charset);
            out.encodeString("");
        }
        if ((mask & AvatarModifiedMask.SubAvatarLook.getVal()) != 0) {
            Tms282AvatarEncoder.encodeLook(out, look, name, charset);
        }
        if ((mask & (AvatarModifiedMask.AvatarLook.getVal() | AvatarModifiedMask.SubAvatarLook.getVal())) != 0) {
            // 8D75E0 (282 only): u32, {u8 slot, u32 item}* until slot 0xFF, u32, {u8 slot, u32 item}* until 0xFF.
            // Slots 1..0x27 except 0x0B; an item of 0 is ignored. Sent empty.
            out.encodeInt(0);
            out.encodeByte(0xFF);
            out.encodeInt(0);
            out.encodeByte(0xFF);
        }
        if ((mask & AvatarModifiedMask.Speed.getVal()) != 0) {
            out.encodeByte(speed);
        }
        if ((mask & AvatarModifiedMask.CarryItemEffect.getVal()) != 0) {
            out.encodeByte(carryItemEffect);
        }
        rings.encode282(out, charset); // couple, friendship, marriage (5ADB098..5ADBD66)
        out.encodeInt(setItemId);
        out.encodeInt(starForce);
        out.encodeInt(arcane);
        out.encodeInt(authentic);
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeByte(0);
        out.encodeByte(0);
        return out;
    }
}
