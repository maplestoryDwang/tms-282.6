package net.swordie.ms.world.tms282.dojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.constants.WzConstants;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mu Lung Dojo floors 1..{@link #finalFloor()} ({@code data/resources/tms282/dojang/floors.json}, generated from the
 * MetaMS 273 {@code dojang_spawn.js} table). Floor n is map {@code 925070000 + n * 100}.
 * <p>
 * {@link #spawnFloor} spawns the floor boss (with the 273 HP) and its adds, and marks the floor cleared
 * ({@code dojoclear<n>} instance property, read by {@code portal/dojang_up.py}) when every spawned mob is dead,
 * like the 273 event's "active mob count 0". BossHelper also sets the property for the legacy boss id list.
 */
public final class DojoFloorService {
    public static final int FLOOR_MAP_BASE = 925070000;
    public static final int EXIT_MAP = 925020002;

    public record Add(int mob, int x, int y) {
    }

    public record Floor(int floor, int mob, long hp, Integer x, int y, List<Add> adds) {
    }

    private static volatile DojoFloorService instance;

    private final Map<Integer, Floor> floors;
    private final int finalFloor;

    private DojoFloorService(Map<Integer, Floor> floors, int finalFloor) {
        this.floors = Map.copyOf(floors);
        this.finalFloor = finalFloor;
    }

    public static DojoFloorService get() {
        DojoFloorService s = instance;
        if (s == null) {
            synchronized (DojoFloorService.class) {
                if (instance == null) instance = load();
                s = instance;
            }
        }
        return s;
    }

    private static DojoFloorService load() {
        Path path = Path.of(ServerConstants.RESOURCES_DIR, "tms282", "dojang", "floors.json");
        try {
            JsonNode root = new ObjectMapper().readTree(path.toFile());
            Map<Integer, Floor> floors = new HashMap<>();
            for (JsonNode n : root.path("floors")) {
                List<Add> adds = new ArrayList<>();
                for (JsonNode a : n.path("adds")) adds.add(new Add(a.path("mob").asInt(), a.path("x").asInt(), a.path("y").asInt(7)));
                floors.put(n.path("floor").asInt(), new Floor(n.path("floor").asInt(), n.path("mob").asInt(), n.path("hp").asLong(),
                        n.has("x") ? n.path("x").asInt() : null, n.path("y").asInt(7), List.copyOf(adds)));
            }
            return new DojoFloorService(floors, root.path("finalFloor").asInt(80));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
    }

    public int finalFloor() {
        return finalFloor;
    }

    public static boolean isFloorMap(int fieldId) {
        return fieldId / 10000 == FLOOR_MAP_BASE / 10000 && fieldId % 100 == 0 && stageOf(fieldId) >= 1;
    }

    public static int stageOf(int fieldId) {
        return (fieldId - FLOOR_MAP_BASE) / 100;
    }

    public int mapOf(int stage) {
        return FLOOR_MAP_BASE + stage * 100;
    }

    /** Map to go to after clearing {@code stage}: next floor, or the dojo exit after the final floor. */
    public int nextMap(int stage) {
        return stage >= finalFloor ? EXIT_MAP : mapOf(stage + 1);
    }

    public Floor floor(int stage) {
        return floors.get(stage);
    }

    /** Spawns the boss and adds of {@code stage} on the character's current field. */
    public boolean spawnFloor(Char chr, int stage) {
        Floor f = floors.get(stage);
        Field field = chr.getField();
        if (f == null || field == null) return false;
        Instance inst = chr.getInstance();
        String clearKey = "dojoclear" + stage;
        List<Mob> spawned = new ArrayList<>();
        int x = f.x() != null ? f.x() : -400 + Randomizer.nextInt(17) * 50;
        Mob boss = field.spawnMob(f.mob(), x, f.y(), false, f.hp());
        if (boss != null) spawned.add(boss);
        for (Add a : f.adds()) {
            Mob add = field.spawnMob(a.mob(), a.x(), a.y(), false, 0);
            if (add != null) spawned.add(add);
        }
        if (spawned.isEmpty()) return false;
        if (inst != null) inst.addProperty("dojospawned" + stage, 1);
        AtomicInteger alive = new AtomicInteger(spawned.size());
        for (Mob m : spawned) {
            m.addObserver(new Observer() {
                private boolean seen;

                @Override
                public synchronized void update(Observable o, Object arg) {
                    if (seen) return;
                    seen = true;
                    o.deleteObserver(this);
                    if (alive.decrementAndGet() == 0 && inst != null && !inst.hasProperty(clearKey)) {
                        inst.addProperty(clearKey, 1);
                        field.broadcast(UserPacket.effect(Effect.effectFromWZ(WzConstants.EFFECT_CLEAR)));
                    }
                }
            });
        }
        return true;
    }
}
