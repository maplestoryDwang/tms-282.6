package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.world.Channel;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.DEFAULT_CHARSET;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkWorldId;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 world and channel selection: LP000B world record and terminator, LP000C latest world,
 * LP000F world-list refresh, LP0010 client key, LP0011 physical world, LP0012 view channels,
 * LP005F channel extra and LP006B VIP channels.
 * See {@link Tms282LoginPacketSupport} for the opcodes deliberately left out.
 */
public final class Tms282WorldPackets {
    private Tms282WorldPackets() {
    }

    /** One LP000B channel record; {@code wireChannelId} is zero-based. */
    private record ChannelRecord(String name, int gauge, int wireChannelId) {
    }

    /** Channel model IDs are one-based; advertised channel IDs are zero-based. */
    public static OutPacket worldInformation(int worldId, String name, List<Channel> channels,
                                             int characterCount) {
        checkWorldId(worldId);
        Objects.requireNonNull(channels, "channels");
        List<ChannelRecord> records = new ArrayList<>(channels.size());
        for (Channel channel : channels) {
            if (channel.getWorldId() != worldId) {
                throw new IllegalArgumentException("Channel belongs to another world");
            }
            records.add(new ChannelRecord(channel.getName(), channel.getGaugePx(), channel.getChannelId() - 1));
        }
        return encodeWorld(worldId, name, records, characterCount, "", DEFAULT_CHARSET);
    }

    /**
     * LP000B record without {@link Channel} objects (no Server/World initialization): channel i is
     * advertised as wire channel i with gauge 0, and the world's character count is 0. Byte-identical to
     * the record the login probe sent (sessions/1790062939853/send-042-000b.bin).
     */
    public static OutPacket worldInformation(int worldId, String name, List<String> channelNames) {
        return worldInformation(worldId, name, channelNames, "", DEFAULT_CHARSET);
    }

    /** As {@link #worldInformation(int, String, List)} with the world message string (GameSettings world.message.&lt;id&gt;; empty = old bytes). */
    public static OutPacket worldInformation(int worldId, String name, List<String> channelNames, String worldMessage,
                                             java.nio.charset.Charset messageCharset) {
        Objects.requireNonNull(channelNames, "channelNames");
        List<ChannelRecord> records = new ArrayList<>(channelNames.size());
        for (int i = 0; i < channelNames.size(); i++) {
            records.add(new ChannelRecord(channelNames.get(i), 0, i));
        }
        return encodeWorld(worldId, name, records, 0, worldMessage == null ? "" : worldMessage,
                messageCharset == null ? DEFAULT_CHARSET : messageCharset);
    }

    private static OutPacket encodeWorld(int worldId, String name, List<ChannelRecord> channels,
                                         int characterCount, String worldMessage,
                                         java.nio.charset.Charset messageCharset) {
        checkWorldId(worldId);
        Objects.requireNonNull(name, "name");
        if (channels.size() > 255 || characterCount < 0) {
            throw new IllegalArgumentException("Invalid channel count or character count");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(0x000B);
        out.encodeByte(worldId);
        str(out, name, DEFAULT_CHARSET);
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeByte(0); // Optional world metadata absent.
        out.encodeByte(0);
        str(out, worldMessage, messageCharset); // world message (empty by default)
        out.encodeInt(characterCount);
        out.encodeInt(-1);
        out.encodeByte(channels.size());
        Set<Integer> channelIds = new HashSet<>();
        for (ChannelRecord channel : channels) {
            checkByte(channel.wireChannelId(), "Wire channel ID");
            if (!channelIds.add(channel.wireChannelId())) {
                throw new IllegalArgumentException("Duplicate channel ID");
            }
            str(out, Objects.requireNonNull(channel.name(), "channel name"), DEFAULT_CHARSET);
            out.encodeInt(channel.gauge());
            out.encodeByte(worldId);
            out.encodeByte(channel.wireChannelId());
            out.encodeByte(0); // Legacy adult flag is not the VIP channel flag.
        }
        out.encodeShort(0);
        out.encodeInt(0);
        out.encodeByte(1);
        out.encodeInt(0);
        return out;
    }

    /** LP0010 (sent right before the LP0013 character list): one i64 client key. */
    public static OutPacket clientKey(long key) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x0010);
        out.encodeLong(key);
        return out;
    }

    /** LP0011 (sent right before the LP0013 character list): i32 1, i32 worldId. */
    public static OutPacket physicalWorld(int worldId) {
        checkWorldId(worldId);
        OutPacket out = Tms282LoginPacketSupport.packet(0x0011);
        out.encodeInt(1);
        out.encodeInt(worldId);
        return out;
    }

    /** LP 0x6B: count followed by int32 (world, zero-based channel, minimum VIP grade). */
    public static OutPacket vipChannels(int worldId, Map<Integer, Integer> zeroBasedChannelGrades) {
        checkWorldId(worldId);
        Objects.requireNonNull(zeroBasedChannelGrades, "zeroBasedChannelGrades");
        return vipChannels(Map.of(worldId,zeroBasedChannelGrades));
    }

    public static OutPacket vipChannels(Map<Integer,Map<Integer,Integer>> gradesByWorld) {
        Objects.requireNonNull(gradesByWorld,"gradesByWorld");
        OutPacket out = Tms282LoginPacketSupport.packet(0x006B);
        out.encodeInt(gradesByWorld.values().stream().mapToInt(Map::size).reduce(0,Math::addExact));
        for(var world:new TreeMap<>(gradesByWorld).entrySet()) {
          int worldId=world.getKey();checkWorldId(worldId);
          for (Map.Entry<Integer, Integer> entry : new TreeMap<>(world.getValue()).entrySet()) {
            checkByte(entry.getKey(), "Wire channel ID");
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException("Minimum VIP grade must not be negative");
            }
            out.encodeInt(worldId);
            out.encodeInt(entry.getKey());
            out.encodeInt(entry.getValue());
          }
        }
        return out;
    }

    /** The probe's 93-byte terminator, including its character-slot field at offset 10. */
    public static OutPacket worldEnd(int worldId, int slots) {
        return worldEnd(worldId, List.of(worldId), slots);
    }

    /**
     * Terminator listing every advertised world. Reader3CBA900 fills CLogin+3D0 (int map),
     * +3E0 (int vector), then3CBA680 fills +3F8 and +408 (int maps). The single-world form is
     * the layout that displayed world0, so each advertised world gets the same entries.
     */
    public static OutPacket worldEnd(int defaultWorldId, List<Integer> worldIds, int slots) {
        return worldEnd(defaultWorldId, worldIds, slots, null, null);
    }

    /** Native3CA495E..3CA4A1E reads the two quick-selection tuples. 3CA4F86/3CA4FA7
     * store them at ctx+E0/E4/E8/EC and ctx+F0/F4/F8 before LP0B7F supplies its avatar. */
    public static OutPacket worldEnd(int defaultWorldId, List<Integer> worldIds, int slots,
                                     Tms282QuickSelection favorite, Tms282QuickSelection previous) {
        checkWorldId(defaultWorldId);
        Objects.requireNonNull(worldIds, "worldIds");
        if (slots < 0) {
            throw new IllegalArgumentException("Character slots must not be negative");
        }
        if (worldIds.isEmpty() || !worldIds.contains(defaultWorldId) || new HashSet<>(worldIds).size() != worldIds.size()) {
            throw new IllegalArgumentException("World list must be non-empty, unique and contain the default world");
        }
        worldIds.forEach(Tms282LoginPacketSupport::checkWorldId);
        if (favorite != null && !worldIds.contains(favorite.worldId())
                || previous != null && !worldIds.contains(previous.worldId()))
            throw new IllegalArgumentException("Quick selection world is not advertised");
        int worldId = defaultWorldId;
        Tms282QuickSelection quick = favorite != null ? favorite : previous;
        OutPacket out = Tms282LoginPacketSupport.packet(0x000B);
        out.encodeByte(255);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeByte(1);
        out.encodeInt(-1);
        out.encodeInt(slots);
        out.encodeInt(-1);
        out.encodeInt(quick == null ? worldId : quick.worldId());
        out.encodeInt(quick == null ? 0 : quick.channelId());
        out.encodeInt(quick == null ? 0 : quick.characterId());
        out.encodeByte(favorite != null);
        out.encodeByte(1);
        out.encodeInt(0);
        out.encodeByte(0);
        out.encodeInt(previous == null ? worldId : previous.worldId());
        out.encodeInt(previous == null ? 0 : previous.channelId());
        out.encodeInt(previous == null ? 0 : previous.characterId());
        out.encodeInt(worldIds.size()); // +3D0 map: world -> 1
        for (int id : worldIds) {
            out.encodeInt(id);
            out.encodeInt(1);
        }
        out.encodeInt(worldIds.size()); // +3E0 vector
        for (int id : worldIds) {
            out.encodeInt(id);
        }
        out.encodeInt(worldIds.size()); // +3F8 map: world -> 1
        for (int id : worldIds) {
            out.encodeInt(id);
            out.encodeInt(1);
        }
        out.encodeInt(1); // +408 map, unchanged: {1 -> 1}
        out.encodeInt(1);
        out.encodeInt(1);
        return out;
    }

    public static OutPacket viewChannels(int worldId) {
        checkWorldId(worldId);
        OutPacket out = Tms282LoginPacketSupport.packet(0x0012);
        out.encodeByte(0);
        str(out, "", DEFAULT_CHARSET);
        out.encodeInt(worldId);
        out.encodeInt(-1);
        return out;
    }

    /** LP000C (inline 3C930BE, read only in world select): preselects this world's button. */
    public static OutPacket latestConnectedWorld(int worldId) {
        checkWorldId(worldId);
        OutPacket out = Tms282LoginPacketSupport.packet(0x000C);
        out.encodeInt(worldId);
        return out;
    }

    /**
     * LP005F (inline 3C93277): u8 worldId, i32 n, n x (u8,u8,u8) into that world record +0x30
     * (reader 135BDC0, same as the LP000B record tail); reopens an open channel list.
     */
    public static OutPacket worldChannelExtra(int worldId, List<int[]> triples) {
        checkWorldId(worldId);
        Objects.requireNonNull(triples, "triples");
        OutPacket out = Tms282LoginPacketSupport.packet(0x005F);
        out.encodeByte(worldId);
        out.encodeInt(triples.size());
        for (int[] triple : triples) {
            Objects.requireNonNull(triple, "triple");
            if (triple.length != 3) {
                throw new IllegalArgumentException("Each channel entry is exactly three bytes");
            }
            for (int value : triple) {
                checkByte(value, "Channel entry value");
                out.encodeByte(value);
            }
        }
        return out;
    }

    /**
     * LP000F (3CA3950): str token -> app+0x3AF8; clears the world list and makes the client send CP0083
     * at once. The server then resends LP000B records and the terminator.
     */
    public static OutPacket worldListRefresh(String token, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x000F);
        str(out, token, charset);
        return out;
    }
}
