package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native282 LP0244 WvsContext housing notifications. Send only after durable state changes.
 * Initial Housing_SetStatus and actor replication are separate prerequisites; these do not open a home. */
public final class Tms282MyHomePackets {
    public static final int OPCODE = 0x0244;
    public static final int INTERFACE = 0x3F7D24B3;
    private Tms282MyHomePackets() { }
    /** Exact A18FB00 private-status fields; caller supplies already-authorized durable ownership. */
    public record Snapshot(int accountId, int templateId, String name, String comment, String record,
                           long instanceSerial, Map<Long,Integer> furniture, Set<Integer> unlockedInventory,
                           int furnitureSlots, long visitors, long likes, int presetSlots, long authority) {
        public Snapshot {
            if (accountId <= 0 || templateId <= 0 || instanceSerial <= 0 || furnitureSlots < 0
                    || visitors < 0 || likes < 0 || presetSlots < 0) throw new IllegalArgumentException("Invalid home snapshot");
            text(name,15); text(comment,15); text(record,254);
            if(record.chars().anyMatch(c->c>127))throw new IllegalArgumentException("Home record must use ASCII keys/values");
            furniture = Collections.unmodifiableMap(new TreeMap<>(furniture));
            unlockedInventory = Collections.unmodifiableSet(new TreeSet<>(unlockedInventory));
            if (furniture.size() > furnitureSlots || furniture.size() > 32767 || unlockedInventory.size() > 65534)
                throw new IllegalArgumentException("Home inventory exceeds wire capacity");
            furniture.forEach((serial,item)->{if (serial <= 0 || item <= 0) throw new IllegalArgumentException("Invalid furniture");});
            if (unlockedInventory.stream().anyMatch(item -> item <= 0)) throw new IllegalArgumentException("Invalid inventory");
        }
    }
    private static void text(String text, int maximum) {
        if (text == null || text.length() > maximum || text.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid native housing text");
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(Character.isHighSurrogate(c)) {
                if(++i==text.length() || !Character.isLowSurrogate(text.charAt(i))) throw new IllegalArgumentException("Invalid UTF16");
            } else if(Character.isLowSurrogate(c)) throw new IllegalArgumentException("Invalid UTF16");
        }
    }
    private static void le(ByteArrayOutputStream out,long value,int bytes) { for(int i=0;i<bytes;i++)out.write((int)(value >>> (8*i))&255); }
    private static void positiveVarInt(ByteArrayOutputStream out,int value) {
        long encoded=(long)value << 1;
        while(encoded>=128){out.write((int)(encoded&127)|128);encoded>>>=7;}out.write((int)encoded);
    }
    private static void nativeString(ByteArrayOutputStream out,String value) {
        positiveVarInt(out,value.length());out.writeBytes(value.getBytes(StandardCharsets.UTF_16LE));
    }
    private static void scalar(ByteArrayOutputStream out,int ordinal,long value,int width) {le(out,ordinal,2);le(out,value,width);}
    public static OutPacket status(Snapshot state) {
        ByteArrayOutputStream body=new ByteArrayOutputStream();
        scalar(body,1,state.accountId,4);scalar(body,2,state.templateId,4);
        le(body,3,2);nativeString(body,state.name);le(body,4,2);nativeString(body,state.comment);
        le(body,5,2);positiveVarInt(body,state.record.length());body.writeBytes(state.record.getBytes(StandardCharsets.US_ASCII));
        scalar(body,6,state.instanceSerial,8);
        le(body,7,2);positiveVarInt(body,state.furniture.size());int element=1;
        for(var item:state.furniture.entrySet()){scalar(body,element++,item.getKey(),8);scalar(body,element++,item.getValue(),4);}
        le(body,0,2);
        le(body,10,2);positiveVarInt(body,state.unlockedInventory.size());element=1;
        for(int item:state.unlockedInventory)scalar(body,element++,item,4);
        le(body,0,2);
        // No guest subscriptions are exposed yet; reset the vector instead of retaining stale client state.
        le(body,12,2);positiveVarInt(body,0);le(body,0,2);
        scalar(body,19,state.furnitureSlots,4);scalar(body,20,state.visitors,8);scalar(body,21,state.likes,8);
        scalar(body,22,state.presetSlots,4);scalar(body,23,state.authority,8);le(body,0,2);le(body,0,2);
        byte[] bytes=body.toByteArray();
        if(bytes.length>60000)throw new IllegalArgumentException("Home status exceeds native packet capacity");
        ByteArrayOutputStream length=new ByteArrayOutputStream();positiveVarInt(length,bytes.length);
        OutPacket out=packet(20);out.encodeArr(length.toByteArray());out.encodeArr(bytes);return out;
    }
    private static OutPacket packet(int method) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPCODE);
        out.encodeInt(INTERFACE);
        out.encodeShort(method);
        return out;
    }
    public static OutPacket inventoryRegistered(int itemId) {
        if (itemId <= 0) throw new IllegalArgumentException("Invalid housing inventory item");
        OutPacket out = packet(21); out.encodeInt(itemId); return out;
    }
    public static OutPacket furnitureRegistered(long serial, int itemId) {
        if (serial <= 0 || itemId <= 0) throw new IllegalArgumentException("Invalid housing furniture");
        OutPacket out = packet(22); out.encodeLong(serial); out.encodeInt(itemId); return out;
    }
    public static OutPacket furnitureDeleted(long serial) {
        if (serial <= 0) throw new IllegalArgumentException("Invalid housing furniture serial");
        OutPacket out = packet(23); out.encodeLong(serial); return out;
    }
    public static OutPacket shoppingCompleted() { return packet(25); }
    /** 66B2C80 ->5864250 ->35932E0 consumes the actual furniture item template, NOT a boolean. */
    public static OutPacket useChair(int furnitureItemId){
        if(furnitureItemId!=0 && (furnitureItemId<2670000 || furnitureItemId>=2680000))throw new IllegalArgumentException("Invalid housing chair template");
        var out=packet(26);out.encodeInt(furnitureItemId);return out;
    }
    /** LP245 transport callback63D8780 parses exactly one signed int for ActivateFurniture. */
    public static OutPacket activationResult(int token,int result){
        var out=Tms282LoginPacketSupport.packet(0x245);out.encodeInt(token);out.encodeInt(0);out.encodeInt(result);return out;
    }
    /** Error15 is the native666F1A7 transport failure value; no unproven gameplay result codes. */
    public static OutPacket activationUnavailable(int token){var out=Tms282LoginPacketSupport.packet(0x245);out.encodeInt(token);out.encodeInt(15);return out;}
}
