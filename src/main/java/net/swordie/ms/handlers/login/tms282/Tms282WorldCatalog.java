package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.packet.Tms282CharacterPackets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The worlds a TMS 282.3 client is shown (the research probe's WORLDS / WORLD_TYPES, unchanged): every world has
 * {@link #NORMAL_CHANNELS} normal channels followed by one VIP channel per {@link #vipMinimumGrades()} entry.
 * <ul>
 *     <li>0 "Local": world type {@code tms282.world0Type} (default 6, collaboration);</li>
 *     <li>47 "Yeti x Pink Bean": type 5, Pink Bean/Yeti in class select;</li>
 *     <li>55 "Special", 56 "Special 2": type 6, the Saitama/Tanjiro collaboration worlds (jobSlot1220 only).</li>
 * </ul>
 * Wire names stay ASCII until the client's string charset is verified. All worlds are served by the one game
 * {@code World} of this server; the world ID selects the per-world account (Account.getOrCreateWorldAccount).
 */
public final class Tms282WorldCatalog {
    /** Zero-based wire channels 0..3 are normal; VIP membership is advertised separately by LP006B. */
    public static final int NORMAL_CHANNELS = 4;
    /** Channel UI has button:vip1..vip5; entry N is the minimum VIP grade of VIP channel N. */
    private static final int[] VIP_MINIMUM_GRADES = {1, 2, 3, 4, 5};

    /** One advertised world. */
    public record World(int id, String name, int type) {
        public World {
            Objects.requireNonNull(name, "name");
            checkWorldType(type);
        }
    }

    private final Map<Integer, World> worlds;

    public Tms282WorldCatalog(List<World> worlds) {
        Map<Integer, World> byId = new TreeMap<>();
        for (World w : worlds) {
            if (byId.put(w.id(), w) != null) {
                throw new IllegalArgumentException("Duplicate world " + w.id());
            }
        }
        this.worlds = Collections.unmodifiableMap(byId);
    }

    /** The probe's four worlds. */
    public static Tms282WorldCatalog standard(int world0Type) {
        return new Tms282WorldCatalog(List.of(
                new World(0, "Local", world0Type),
                new World(47, "Yeti x Pink Bean", Tms282CharacterPackets.WORLD_TYPE_PINK_BEAN_YETI),
                new World(55, "Special", Tms282CharacterPackets.WORLD_TYPE_COLLABORATION),
                new World(56, "Special 2", Tms282CharacterPackets.WORLD_TYPE_COLLABORATION)));
    }

    /** Worlds in ID order. */
    public List<World> worlds() {
        return List.copyOf(worlds.values());
    }

    public List<Integer> worldIds() {
        return List.copyOf(worlds.keySet());
    }

    public boolean contains(int worldId) {
        return worlds.containsKey(worldId);
    }

    public Optional<World> find(int worldId) {
        return Optional.ofNullable(worlds.get(worldId));
    }

    /**
     * {@code world.message.<id>} from server.properties (empty when not configured). Meant for the event-description
     * string of the LP000B world record (Tms282WorldPackets.encodeWorld, the {@code str(out, "", ...)} after the
     * five bytes), which is not wired yet.
     */
    public String worldMessage(int worldId) {
        return net.swordie.ms.GameSettings.worldMessage(worldId);
    }

    /** LP0013/LP0014 world type; {@link Tms282CharacterPackets#WORLD_TYPE_NORMAL} for an unknown world. */
    public int worldType(int worldId) {
        World w = worlds.get(worldId);
        return w == null ? Tms282CharacterPackets.WORLD_TYPE_NORMAL : w.type();
    }

    public int channelCount() {
        return NORMAL_CHANNELS + VIP_MINIMUM_GRADES.length;
    }

    public int vipChannelCount() {
        return VIP_MINIMUM_GRADES.length;
    }

    /** Minimum VIP grade of a zero-based wire channel; 0 for a normal channel. */
    public int minimumVipGrade(int wireChannel) {
        if (wireChannel < 0 || wireChannel >= channelCount()) {
            throw new IllegalArgumentException("No channel " + wireChannel);
        }
        return wireChannel < NORMAL_CHANNELS ? 0 : VIP_MINIMUM_GRADES[wireChannel - NORMAL_CHANNELS];
    }

    /** "name-1".."name-9", as the probe advertised them. */
    public List<String> channelNames(World world) {
        List<String> names = new ArrayList<>();
        for (int channel = 0; channel < channelCount(); channel++) {
            names.add(world.name() + "-" + (channel + 1));
        }
        return names;
    }

    /** LP006B map: world -> (zero-based VIP channel -> minimum grade). */
    public Map<Integer, Map<Integer, Integer>> vipGrades() {
        Map<Integer, Map<Integer, Integer>> out = new TreeMap<>();
        for (int worldId : worlds.keySet()) {
            Map<Integer, Integer> grades = new LinkedHashMap<>();
            for (int i = 0; i < VIP_MINIMUM_GRADES.length; i++) {
                grades.put(NORMAL_CHANNELS + i, VIP_MINIMUM_GRADES[i]);
            }
            out.put(worldId, grades);
        }
        return out;
    }

    /** Only the three LP0013 world types the client is known to accept. */
    public static void checkWorldType(int type) {
        if (type != Tms282CharacterPackets.WORLD_TYPE_NORMAL && type != Tms282CharacterPackets.WORLD_TYPE_PINK_BEAN_YETI
                && type != Tms282CharacterPackets.WORLD_TYPE_COLLABORATION) {
            throw new IllegalArgumentException("world type must be 1, 5 or 6, got " + type);
        }
    }

    public static int parseWorldType(String configured) {
        int type;
        try {
            type = Integer.parseInt(configured.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("world0Type must be 1, 5 or 6, got \"" + configured + "\"", e);
        }
        checkWorldType(type);
        return type;
    }
}
