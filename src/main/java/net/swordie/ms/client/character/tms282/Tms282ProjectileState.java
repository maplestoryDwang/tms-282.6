package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.skills.ShootObjectSkillInfo;
import net.swordie.ms.connection.packet.Tms282ProjectileMotionRequest;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.constants.JobConstants;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

/** Ownership for the native Khali physical objects whose ID is physical+70 (107291B -> DTO+38).
 * Keys, clients and field references are weak; values never retain a Char, Client, Field or ShootObject.
 */
public final class Tms282ProjectileState {
    public static final int MAX_OBJECTS = 256;
    public static final long MAX_AGE_MS = 60_000;
    // ponytail: one short global lock; use per-character locks only if packet throughput requires it.
    private static final Map<Char, Registry> OWNERS = new WeakHashMap<>();

    private static final class Registry {
        final WeakReference<Field> field;
        final WeakReference<Client> client;
        final Map<Integer, Entry> objects = new HashMap<>();
        Registry(Char chr) {
            this.field = new WeakReference<>(chr.getField());
            this.client = new WeakReference<>(chr.getClient());
        }
    }
    private record Entry(int skill, int level, long expires, Tms282ProjectileMotionRequest motion) { }

    private Tms282ProjectileState() { }

    /** After successful CP03CB job handling, before its remote broadcast. Never overwrite a live object ID. */
    public static synchronized void created(Char chr, ShootObjectSkillInfo info) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3
                || (info.getSkillId() != 154121001 && info.getSkillId() != 154141009)) return;
        if (chr.getField() == null || chr.getClient() == null || !JobConstants.isKhali(chr.getJob())
                || Tms282HiddenSkillParents.authorisedLevel(chr, info.getSkillId()) != info.getSlv())
            throw new IllegalArgumentException("Unowned Khali physical objects");
        var registry = registry(chr, true);
        long now = System.currentTimeMillis();
        registry.objects.values().removeIf(e -> e.expires <= now);
        var ids = new HashSet<Integer>();
        for (var object : info.getShootObjects()) {
            if (object.getOwner() != chr || object.getSkillId() != info.getSkillId() || object.getSlv() != info.getSlv()
                    || object.getId() <= 0 || !ids.add(object.getId()) || registry.objects.containsKey(object.getId()))
                throw new IllegalArgumentException("Foreign or duplicate physical object");
        }
        if (registry.objects.size() + ids.size() > MAX_OBJECTS)
            throw new IllegalArgumentException("Too many physical objects");
        for (int id : ids) registry.objects.put(id, new Entry(info.getSkillId(), info.getSlv(), now + MAX_AGE_MS, null));
    }

    /** Change the previously accepted object's motion. Unknown IDs and cross-field replays are rejected. */
    public static synchronized void motion(Char chr, Tms282ProjectileMotionRequest request) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3)
            throw new IllegalArgumentException("Physical-object motion requires TMS282");
        var registry = registry(chr, false);
        Entry entry = registry == null ? null : registry.objects.get(request.objectId());
        if (entry == null || entry.expires <= System.currentTimeMillis()
                || Tms282HiddenSkillParents.authorisedLevel(chr, entry.skill) < entry.level)
            throw new IllegalArgumentException("Unknown or expired physical object");
        if (request.option() != 0 && request.option() != SkillData.getSkillInfoById(entry.skill).getValue(SkillStat.v, entry.level))
            throw new IllegalArgumentException("Foreign physical-object motion option");
        registry.objects.put(request.objectId(), new Entry(entry.skill, entry.level, entry.expires, request));
    }

    public static synchronized Tms282ProjectileMotionRequest motionOf(Char chr, int objectId) {
        var registry = registry(chr, false);
        Entry entry = registry == null ? null : registry.objects.get(objectId);
        return entry == null || entry.expires <= System.currentTimeMillis() ? null : entry.motion;
    }

    /** Call before every field assignment, including returning to the same cached Field. */
    public static synchronized void clear(Char chr) {
        if (Protocol.active().version() == ProtocolVersion.TMS_282_3) OWNERS.remove(chr);
    }

    private static Registry registry(Char chr, boolean create) {
        var registry = OWNERS.get(chr);
        if (registry != null && (registry.field.get() != chr.getField() || registry.client.get() != chr.getClient())) {
            OWNERS.remove(chr);
            registry = null;
        }
        if (registry == null && create) {
            registry = new Registry(chr);
            OWNERS.put(chr, registry);
        }
        return registry;
    }
}
