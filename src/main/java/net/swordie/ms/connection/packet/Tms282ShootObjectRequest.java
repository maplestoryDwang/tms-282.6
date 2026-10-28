package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.ShootObject;
import net.swordie.ms.client.character.skills.ShootObjectSkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;

/** CP03CB (5A3CAAA): shoot-object creation, not an extra damage attack.
 * Native ProcessType and each object's final i32 differ from 265. The transport trailer is never game data.
 * Captures: unknown-skills-1007/captures.tsv (Wild Hunter, Khali, Illium).
 */
public record Tms282ShootObjectRequest(int skillId, int slv, int extraByte, byte[] legacyBody, byte[] nativeObjects) {
    public static final int OPCODE = 0x03CB;
    public static final int REMOTE = 0x04B2;

    public Tms282ShootObjectRequest {
        legacyBody = legacyBody.clone();
        nativeObjects = nativeObjects.clone();
    }
    @Override public byte[] legacyBody() { return legacyBody.clone(); }
    @Override public byte[] nativeObjects() { return nativeObjects.clone(); }

    public static Tms282ShootObjectRequest parse(byte[] packet) {
        var r = Tms282FieldRequestReader.open(packet, OPCODE, "CP03CB");
        r.i32(); // channel/field identifier (-1 in the current captures)
        int skill = r.i32(), level = r.i32();
        if (skill <= 0 || level <= 0 || level > 255) throw new IllegalArgumentException("Invalid shoot skill/level");
        try {
            var process = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            process.position(r.position());
            process.limit(packet.length - 4);
            Tms282SkillProcess.decode(process, new SkillUseInfo(skill, level));
            r.seek(process.position());
        } catch (java.nio.BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated shoot ProcessType", e);
        }
        OutPacket out = new OutPacket();
        out.encodeInt(skill); out.encodeInt(level);
        // The legacy shoot handler discards ProcessType; it must not decode native tags with the 265 schema.
        out.encodeInt(0); out.encodeByte(0);
        out.encodeArr(r.bytes(28)); // action/speed/item/options/position
        int extra = flag(r);
        out.encodeByte(extra);
        int extraByte = 0;
        if (extra != 0) {
            out.encodeArr(r.bytes(46));
            extraByte = r.u8(); // 1072E60's final byte, absent from the 265 DTO
        }
        out.encodeArr(r.bytes(32)); // tick, four CRC buffers, three skill checksums
        int count = r.i32();
        if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid shoot object count " + count);
        out.encodeInt(count);
        int objectStart = r.position();
        var ids = new HashSet<Integer>();
        for (int i = 0; i < count; i++) {
            byte[] head = r.bytes(35);
            var b = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
            if (b.getInt() != skill || b.getInt() != level || !ids.add(b.getInt()))
                throw new IllegalArgumentException("Foreign or duplicate shoot object");
            out.encodeArr(head);
            for (int j = 0; j < 2; j++) {
                int on = flag(r); out.encodeByte(on);
                if (on != 0) out.encodeArr(r.bytes(8));
            }
            out.encodeInt(r.i32());
            r.i32(); // 282 object field (4324F8E); retained for the native remote reader
        }
        byte[] objects = java.util.Arrays.copyOfRange(packet, objectStart, r.position());
        r.i32(); // 346A070 trailer
        r.end();
        return new Tms282ShootObjectRequest(skill, level, extraByte, out.getData(), objects);
    }

    private static int flag(Tms282FieldRequestReader r) {
        int value = r.u8();
        if (value > 1) throw new IllegalArgumentException("Invalid shoot flag");
        return value;
    }

    /** The same DTO the legacy handler builds, without its obsolete local acknowledgement. */
    public ShootObjectSkillInfo info(Char chr) {
        var in = new InPacket(legacyBody);
        var info = new ShootObjectSkillInfo(chr.getId());
        info.setSkillId(in.decodeInt()); info.setSlv(in.decodeInt());
        in.decodeInt(); in.decodeByte(); // discarded ProcessType
        info.setAction(in.decodeInt()); info.setActionSpeed(in.decodeInt());
        info.setUnknownBool(in.decodeByte()); info.setProjectileItemId(in.decodeInt());
        info.setProjectileItemPosition(in.decodeInt());
        in.decodeByte(); in.decodeInt(); in.decodeByte(); in.decodeByte();
        info.setPosition(in.decodePosition());
        info.setEncodeExtra(in.decodeByte() != 0);
        if (info.isEncodeExtra()) {
            info.extraEncodeInt1 = in.decodeInt(); info.extraEncodeInt2 = in.decodeInt();
            info.extraEncodeInt3 = in.decodeInt(); info.extraEncodeInt4 = in.decodeInt();
            info.extraEncodeInt5 = in.decodeInt(); info.extraEncodeInt6 = in.decodeInt();
            info.extraEncodeByte1 = in.decodeByte(); info.extraEncodeInt7 = in.decodeInt();
            info.extraEncodeByte2 = in.decodeByte(); info.extraEncodeLong1 = in.decodeLong();
            info.extraEncodeInt8 = in.decodeInt(); info.extraEncodeInt9 = in.decodeInt();
        }
        in.decodeArr(32);
        int count = in.decodeInt();
        for (int i = 0; i < count; i++) info.getShootObjects().add(new ShootObject(chr, in));
        if (in.getUnreadAmount() != 0) throw new IllegalArgumentException("Trailing legacy shoot data");
        return info;
    }

    /** CUserRemote 5AFB4A0/1072640: native object records, including their last i32. */
    public OutPacket remote(ShootObjectSkillInfo info) {
        byte[] legacy = UserRemote.shootObject(info).getData();
        int head = 26 + (info.isEncodeExtra() ? 46 : 0);
        OutPacket out = Tms282LoginPacketSupport.packet(REMOTE);
        out.encodeArr(java.util.Arrays.copyOfRange(legacy, 2, 2 + head));
        if (info.isEncodeExtra()) out.encodeByte(extraByte);
        if (skillId == 154121001 || skillId == 154141009)
            out.encodeArr(java.util.Arrays.copyOfRange(legacy, 2 + head, 2 + head + 9));
        out.encodeInt(info.getShootObjects().size());
        out.encodeArr(nativeObjects);
        return out;
    }
}
