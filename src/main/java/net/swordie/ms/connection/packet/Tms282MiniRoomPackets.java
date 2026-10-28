package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.TradeRoom;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.Gms265MiniRoomPackets;
import net.swordie.ms.connection.protocol.codec.MiniRoomPacketEncoder;
import net.swordie.ms.enums.MiniRoomType;
import net.swordie.ms.enums.RoomLeaveType;
import net.swordie.ms.util.Util;

import java.nio.charset.Charset;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 LP097F MINI_ROOM_BASE_DLG for the trading room (research/tms2823/miniroom-trace).
 * <p>
 * Dispatcher 3E48A40: {@code Decode4 type}; types 0x14..0x25 are the base cases (jump table 3E48E48), every other type
 * goes to the open dialog's own OnPacket (vtable +0x218; trading room vtable 844F760 -> 198EC00, switch 0..0x11).
 * The base-case types are the 265 values + 2 [verified per case by the tracer]; the dialog cases keep the 265 values:
 * <pre>
 *  265  282  meaning                  282 reader
 *   21   23  enter result             3E48FE0 (only while no dialog is open): u8 0, u8 room type, then the new
 *                                     dialog's OnEnterBase 3E4B830: u8 max users, u8 my slot, per user {u8 slot,
 *                                     AvatarLook, Str name, i16 job, i32 wing}, u8 -1, u8</pre><pre>
 *   19   21  visitor enter (accept)   dialog vtable +0x228 -> 3E4B600
 *   22   24  trade invite             3E49E40 (u8 room type, i32 inviter, Str name, i32 invitee, i32 serial)
 *   25   27  chat                     dialog vtable +0x248 -> 3E4BE30
 *   29   31  leave / trade result     dialog vtable +0x200 -> 3E4BA80 (u8 slot, u8 reason)
 *   32   33  check SSN2               3E4AD60 (u8, u8, u8 flag; flag != 0 ends the read, no i32)
 *    0    0  put item                 198EC00 (u8 user, u8 slot, 282 GW_ItemSlotBase)
 *    4    4  put mesos                198EC00 (u8 user, i64)
 *    8    8  trade (partner confirmed)198EC00 (nothing)
 * </pre>
 * The character blocks carry the 282 AvatarLook ({@link Tms282AvatarEncoder#encodeLook}).
 * <p>
 * Battle RPS (room type 3, dialog 1FEB7C0, vtable 8483080, OnPacket +0x218 = 1FED500) knows only two types, both the
 * 265 value - 24: 74 (0x4A) start, no body [traced CLEAN]; 91 (0x5B) finish {@code u8 result, u8 opponent's hand}
 * (1FEDFA0; both must be 0..2) [reads exact; the fake dialog then lacks the button widgets]. 265 resultRPS (100) is a
 * client request type the 265 server never sends, so it stays 265 (dropped by the guard).
 * Not ported: TradeRestraintItem (282 type 17 reads one more byte of unknown meaning; 265 never sends it).
 * Personal shop / hired merchant / cash trading room (room types 5/6/7) do not exist in the 282 client: the room
 * dialog factory 3E507F0 returns null for them (research/tms2823/miniroom-trace/README.md, shop section).
 */
public final class Tms282MiniRoomPackets implements MiniRoomPacketEncoder {
    public static final int OPCODE = 0x097F;
    static final int ENTER_RESULT = 23;
    static final int VISITOR_ENTER = 21;
    static final int INVITE = 24;
    static final int CHAT = 27;
    static final int LEAVE = 31;
    static final int CHECK_SSN2 = 33;
    static final int RPS_START = 74;
    static final int RPS_FINISH = 91;
    static final int PUT_ITEM = 0;
    static final int PUT_MONEY = 4;
    static final int TRADE = 8;
    /** The chat sub-type byte after the chat type; 265 writes MiniRoomType.Chat (26) there. */
    static final int CHAT_SUBTYPE = 28;

    private final Charset charset;

    public Tms282MiniRoomPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    private static OutPacket packet(int type) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(type);
        return out;
    }

    /** One user block of the enter result / visitor enter: 282 AvatarLook, name, job, wing item. */
    private void user(OutPacket out, Char chr) {
        Tms282AvatarEncoder.encodeLook(out, chr.getAvatarData().getAvatarLook(), chr.getName(), charset);
        str(out, chr.getName(), charset);
        out.encodeShort(chr.getJob());
        out.encodeInt(0); // wing item
    }

    private OutPacket enterHead(MiniRoomType roomType, int myPosition) {
        OutPacket out = packet(ENTER_RESULT);
        out.encodeByte(0); // success
        out.encodeByte(roomType.getVal());
        out.encodeByte(2); // max users
        out.encodeByte(myPosition);
        return out;
    }

    @Override
    public OutPacket enterTrade(Char chr, MiniRoomType miniRoomType) {
        OutPacket out = enterHead(miniRoomType, 0);
        out.encodeByte(0);
        user(out, chr);
        out.encodeByte(-1);
        out.encodeByte(0);
        return out;
    }

    @Override
    public OutPacket enterTrade(TradeRoom tradeRoom, Char chr, MiniRoomType miniRoomType) {
        OutPacket out = enterHead(miniRoomType, 1);
        Char other = tradeRoom.getOtherChar(chr.getId());
        out.encodeByte(0);
        user(out, other);
        out.encodeByte(1);
        user(out, chr);
        out.encodeByte(-1);
        out.encodeByte(0); // read after the user list (3E4BA11); 265 omits it in this variant
        return out;
    }

    @Override
    public OutPacket accept(Char chr) {
        OutPacket out = packet(VISITOR_ENTER);
        out.encodeByte(1);
        user(out, chr);
        return out; // 282 reads nothing after the wing item (3E4B70A); the 265 trailing byte is dropped
    }

    @Override
    public OutPacket checkSSN2(Char chr) {
        OutPacket out = packet(CHECK_SSN2);
        out.encodeByte(16);
        out.encodeByte(7);
        out.encodeByte(1);
        return out;
    }

    @Override
    public OutPacket putItem(int user, int pos, Item item) {
        OutPacket out = packet(PUT_ITEM);
        out.encodeByte(user);
        out.encodeByte(pos);
        Tms282ItemEncoder.encode(out, item, charset);
        return out;
    }

    @Override
    public OutPacket putMoney(int user, long money) {
        OutPacket out = packet(PUT_MONEY);
        out.encodeByte(user);
        out.encodeLong(money);
        return out;
    }

    @Override
    public OutPacket tradeRestraintItem() {
        return Gms265MiniRoomPackets.INSTANCE.tradeRestraintItem();
    }

    @Override
    public OutPacket tradeInvite(Char chr, Char other, MiniRoomType miniRoomType) {
        OutPacket out = packet(INVITE);
        out.encodeByte(miniRoomType.getVal());
        out.encodeInt(chr.getId());
        str(out, chr.getName(), charset);
        out.encodeInt(other.getId());
        out.encodeInt(Util.getRandom(50, 60));
        return out;
    }

    @Override
    public OutPacket cancelTrade() {
        return leave(0, RoomLeaveType.TRLeave_TradeFail_Denied);
    }

    @Override
    public OutPacket tradeComplete() {
        return leave(1, RoomLeaveType.TRLeave_TradeDone);
    }

    private static OutPacket leave(int slot, RoomLeaveType reason) {
        OutPacket out = packet(LEAVE);
        out.encodeByte(slot);
        out.encodeByte(reason.getVal());
        return out;
    }

    @Override
    public OutPacket tradeConfirm() {
        return packet(TRADE);
    }

    @Override
    public OutPacket chat(Char chr, Char other, int pos, String msg) {
        OutPacket out = packet(CHAT);
        out.encodeByte(CHAT_SUBTYPE);
        out.encodeByte(pos);
        str(out, chr.getName(), charset);
        str(out, msg, charset);
        out.encodeInt(other.getId());
        out.encodeInt(chr.getId());
        // 265 Char.encodeChatInfo reads the account; an offline/test Char without one falls back to accId, world 0.
        int accountId = chr.getAccount() != null ? chr.getAccount().getId() : chr.getAccId();
        int worldId = chr.getAccount() != null ? chr.getAccount().getWorldId() : 0;
        Tms282ChatPackets.ChatSender s = new Tms282ChatPackets.ChatSender(chr.getId(), accountId, chr.getName(), worldId);
        chatInfo(out, s, msg);
        return out;
    }

    /** 265 Char.encodeChatInfo + the 282 tail byte (Tms282ChatPackets). */
    private void chatInfo(OutPacket out, Tms282ChatPackets.ChatSender s, String msg) {
        str(out, s.name(), charset);
        str(out, msg, charset);
        out.encodeInt((long) s.accountId());
        out.encodeInt((long) s.characterId());
        out.encodeByte(s.worldId());
        out.encodeInt((long) s.characterId());
        out.encodeInt(0L);
        str(out, "", charset);
        out.encodeInt(0L);
        str(out, "", charset);
        out.encodeInt(0L);
        out.encodeByte(0);
    }

    @Override
    public OutPacket startRPS() {
        return packet(RPS_START);
    }

    @Override
    public OutPacket resultRPS(byte type) {
        return Gms265MiniRoomPackets.INSTANCE.resultRPS(type);
    }

    @Override
    public OutPacket finishRPS(byte result, byte type) {
        OutPacket out = packet(RPS_FINISH);
        out.encodeByte(result);
        out.encodeByte(type);
        return out;
    }
}
