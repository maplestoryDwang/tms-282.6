package net.swordie.ms.connection.packet;

import net.swordie.ms.client.social.Friend.Friend;
import net.swordie.ms.client.social.Friend.FriendResult;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.FriendPacketEncoder;
import net.swordie.ms.connection.protocol.codec.Gms265FriendPackets;
import net.swordie.ms.enums.social.Friend.FriendType;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TMS 282.3 FRIEND_RESULT (LP00DB): CWvsContext::OnFriendResult 6010070 (bails out while the friend manager
 * ctx+0x2540 is null; Decode1 type, jump table 6011A60 for types 13..46). Built from the 265 {@link FriendResult}
 * object, not from 265 bytes: the 265 fixed 13-byte names are UTF-8 and overflow for CJK names.
 * <p>
 * 282 GW_Friend (field decoder 8E6F90, in-memory record 0x178 bytes):
 * <pre>
 * +0x00 i32 friend id        +0x04 buf15 name        +0x13 u8 flag       +0x14 i32 channel
 * +0x18 buf17 group          +0x29 i32 account id    +0x2D buf15 nickname
 * flag == 7: +0x3C buf256 memo, +0x16F u8, +0x170 buf8      else: +0x13C buf51
 * </pre>
 * The list (type 13) is {@code i32 n, n x 0x178} raw records (600BB00 DecodeBuffer n*0x178), every other record goes
 * through the field decoder. Compared to 265 (Friend.encode, 317 bytes): names 13 -> 15 bytes, no mobile byte, memo
 * only for flag 7, no in-shop int. Fields the 265 object does not have are sent as 0.
 * <p>
 * Result types (282 reader traced, research/tms2823/social-trace friend-* fixtures; the 265 -> 282 numbering is
 * 推定 from the read shapes):
 * <pre>
 * 265 FriendType                    282  282 read shape
 * Response_Load_Success 21           13  i32 n, n x raw record
 * Response_NotifyChange_FriendInfo 24 15 i32 id, i32 account id, record     (265: + u8 0 before the record)
 * Response_Invite 26                 17  i32 id, i32 acc, Str name, i32 level, i32 job, i32 sub job, record
 *                                        (265: u8 isAccount first)
 * Response_Set_Success 27            18  Str name
 * Response_Set_CurrentSlotFull 28 .. Response_Set_UnknownUser 35  -> 19 .. 26, no payload (messages)
 * Response_Set_SingleFriendInfo 40   32  record
 * Response_Delete_Success 43         35  i32 id (account id for an account friend; 265: u8 isAccount first)
 * Response_Notify 45                 40  i32 id, i32 acc, i32 channel, u8 flag, u8 online, Str name, buf8
 * Response_Notice_Deleted 51         42  Str name
 * Response_InviteEventBestFriend 52  44  i32 id, Str name
 * Response_RejectEventBestFriend 53  45  Str name
 * </pre>
 * Other types (the *_Messenger error lines, MessengerMode, IncreaseMaxSlot) have no matched 282 shape and go out as the
 * 265 packet, which the OutboundGuard drops.
 */
public final class Tms282FriendPackets implements FriendPacketEncoder {
    public static final int FRIEND_RESULT = 0x00DB;
    public static final int RECORD_SIZE = 0x178;

    /** 265 FriendType value -> 282 type for the no-payload message results 28..35 (-9, 推定). */
    private static final Map<FriendType, Integer> MESSAGES = Map.of(
            FriendType.Response_Set_CurrentSlotFull, 19, FriendType.Response_Set_TargetSlotFull, 20,
            FriendType.Response_Set_AlreadySet, 21, FriendType.Response_Set_AlreadyRequested, 22,
            FriendType.Response_Set_Ready, 23, FriendType.Response_Set_CantAddYourSelf, 24,
            FriendType.Response_Set_isGameMaster, 25, FriendType.Response_Set_UnknownUser, 26);

    private final Charset charset;

    public Tms282FriendPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public OutPacket friendResult(FriendResult r) {
        Objects.requireNonNull(r, "friendResult");
        FriendType type = r.getFriendType();
        Integer message = MESSAGES.get(type);
        if (message != null) {
            return start(message);
        }
        return switch (type) {
            case Response_Load_Success -> list(r.getFriends());
            case Response_NotifyChange_FriendInfo -> {
                OutPacket out = start(15);
                out.encodeInt(r.getFriend().getFriendID());
                out.encodeInt(r.getFriend().getFriendAccountID());
                record(out, r.getFriend());
                yield out;
            }
            case Response_Invite -> {
                Friend f = r.getFriend();
                OutPacket out = start(17);
                out.encodeInt(f.getFriendID());
                out.encodeInt(f.getFriendAccountID());
                Tms282LoginPacketSupport.str(out, f.getName(), charset);
                out.encodeInt(r.getLevel());
                out.encodeInt(r.getJob());
                out.encodeInt(r.getSubJob());
                record(out, f);
                yield out;
            }
            case Response_Set_Success -> name(18, r.getName());
            case Response_Set_SingleFriendInfo -> singleFriendInfo(r.getFriend());
            case Response_Delete_Success -> {
                OutPacket out = start(35);
                Friend f = r.getFriend();
                out.encodeInt(f.isAccount() ? f.getFriendAccountID() : f.getFriendID());
                yield out;
            }
            case Response_Notify -> {
                Friend f = r.getFriend();
                OutPacket out = start(40);
                out.encodeInt(f.getFriendID());
                out.encodeInt(f.getFriendAccountID());
                out.encodeInt(r.getChannel());
                out.encodeByte(f.getFlag());
                out.encodeByte(r.isOnline() ? 1 : 0);
                Tms282LoginPacketSupport.str(out, f.getName(), charset);
                out.encodeArr(new byte[8]);
                yield out;
            }
            case Response_Notice_Deleted -> name(42, r.getName());
            case Response_InviteEventBestFriend -> {
                OutPacket out = start(44);
                out.encodeInt(r.getFriend().getFriendID());
                Tms282LoginPacketSupport.str(out, r.getFriend().getName(), charset);
                yield out;
            }
            case Response_RejectEventBestFriend -> name(45, r.getName());
            default -> Gms265FriendPackets.INSTANCE.friendResult(r);
        };
    }

    /** Type 32: one field-decoded record (265 Response_Set_SingleFriendInfo 40, which 265 never builds). */
    public OutPacket singleFriendInfo(Friend friend) {
        OutPacket out = start(32);
        record(out, friend);
        return out;
    }

    /** Type 13: i32 n, n raw 0x178-byte records. */
    OutPacket list(Collection<Friend> friends) {
        List<Friend> all = friends == null ? List.of() : List.copyOf(friends);
        OutPacket out = start(13);
        out.encodeInt(all.size());
        for (Friend f : all) {
            out.encodeArr(rawRecord(f));
        }
        return out;
    }

    /** The in-memory 0x178-byte record (the list's raw layout). */
    byte[] rawRecord(Friend f) {
        byte[] r = new byte[RECORD_SIZE];
        putInt(r, 0x00, f.getFriendID());
        putFixed(r, 0x04, 15, f.getName());
        r[0x13] = f.getFlag();
        putInt(r, 0x14, f.getChannelID());
        putFixed(r, 0x18, 17, f.getGroup());
        putInt(r, 0x29, f.getFriendAccountID());
        putFixed(r, 0x2D, 15, f.getNickname());
        putFixed(r, 0x3C, 256, f.getMemo());
        return r;
    }

    /** The field-decoded record (8E6F90). */
    void record(OutPacket out, Friend f) {
        out.encodeInt(f.getFriendID());
        out.encodeArr(fixed(15, f.getName()));
        out.encodeByte(f.getFlag());
        out.encodeInt(f.getChannelID());
        out.encodeArr(fixed(17, f.getGroup()));
        out.encodeInt(f.getFriendAccountID());
        out.encodeArr(fixed(15, f.getNickname()));
        if (f.getFlag() == 7) {
            out.encodeArr(fixed(256, f.getMemo()));
            out.encodeByte(0);
            out.encodeArr(new byte[8]);
        } else {
            out.encodeArr(new byte[51]);
        }
    }

    private OutPacket name(int type, String name) {
        OutPacket out = start(type);
        Tms282LoginPacketSupport.str(out, name, charset);
        return out;
    }

    private static OutPacket start(int type) {
        OutPacket out = Tms282LoginPacketSupport.packet(FRIEND_RESULT);
        out.encodeByte(type);
        return out;
    }

    /** {@code s} in the 282 charset, cut to {@code n - 1} bytes on a character boundary, NUL padded to {@code n}. */
    byte[] fixed(int n, String s) {
        byte[] b = new byte[n];
        if (s == null) {
            return b;
        }
        int len = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            byte[] c = new String(Character.toChars(cp)).getBytes(charset);
            if (len + c.length > n - 1) {
                break;
            }
            System.arraycopy(c, 0, b, len, c.length);
            len += c.length;
            i += Character.charCount(cp);
        }
        return b;
    }

    private void putFixed(byte[] r, int off, int n, String s) {
        System.arraycopy(fixed(n, s), 0, r, off, n);
    }

    private static void putInt(byte[] r, int off, int v) {
        r[off] = (byte) v;
        r[off + 1] = (byte) (v >> 8);
        r[off + 2] = (byte) (v >> 16);
        r[off + 3] = (byte) (v >> 24);
    }
}
