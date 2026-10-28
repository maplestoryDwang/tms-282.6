package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.connection.packet.Tms282SkillProcessFinishedRequest;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.world.field.Field;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Close the accepted native Khali attack process; later incoming attacks cannot replay its completed ID.
 * Already queued, legitimately accepted damage is untouched. No extra attack, cooldown or guessed ACK.
 */
public final class Tms282SkillProcessLifecycleHandlers {
    public static final int MAX_PROCESSES = 128;
    public static final long REPLAY_WINDOW_MS = 120_000;
    // ponytail: a bounded two-minute window, not permanent session replay protection.
    private static final Map<Char, Registry> OWNERS = new WeakHashMap<>();
    private static final class Registry {
        final WeakReference<Client> client;
        final WeakReference<Field> field;
        final Map<Integer, Entry> processes = new HashMap<>();
        Registry(Char chr) { client = new WeakReference<>(chr.getClient()); field = new WeakReference<>(chr.getField()); }
    }
    private record Entry(int skill, int level, long expires, boolean closed) { }

    void register(Tms282Routes routes, InHeader completion) {
        routes.channel(Tms282SkillProcessFinishedRequest.OPCODE, completion).when(Tms282Guards.IN_FIELD)
                .handle("owned Khali process completion -> reject later process replays", (s, op, p) -> {
                    try { finish(s.fieldCharacter(),Tms282SkillProcessFinishedRequest.parse(p)); }
                    catch (IllegalArgumentException e) { Tms282SocialHandlers.rejected(op,p,e); s.fieldCharacter().dispose(); }
                    catch (RuntimeException | LinkageError e) {
                        System.out.printf("[TMS282] FAILED CP%04X character=%d: %s%n",op,s.fieldCharacter().getId(),e);
                        s.fieldCharacter().dispose();
                    }
                });
    }

    public static boolean supports(int skill) { return skill == 154141008 || skill == 154141500; }

    /** After the combat guard accepts a complete native prefix, immediately before entering the damage queue. */
    public static synchronized void beforeAttack(Char chr, AttackInfo attack) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || !supports(attack.skillId)) return;
        owned(chr,attack.skillId,attack.slv);
        int id = attack.processType == null ? 0 : attack.processType.nativeProcessId;
        if (id <= 0) throw new IllegalArgumentException("Missing complete native attack process");
        var registry = registry(chr,true);
        long now = System.currentTimeMillis();
        registry.processes.values().removeIf(e -> e.expires <= now);
        var previous = registry.processes.get(id);
        if (previous != null && (previous.closed || previous.skill != attack.skillId || previous.level != attack.slv))
            throw new IllegalArgumentException("Closed or foreign native attack process");
        if (previous == null && registry.processes.size() >= MAX_PROCESSES)
            throw new IllegalArgumentException("Too many native attack processes");
        registry.processes.put(id,new Entry(attack.skillId,attack.slv,now+REPLAY_WINDOW_MS,false));
    }

    public static synchronized void finish(Char chr, Tms282SkillProcessFinishedRequest request) {
        owned(chr,request.skillId(),request.level());
        var registry = registry(chr,false);
        var entry = registry == null ? null : registry.processes.get(request.processId());
        long now = System.currentTimeMillis();
        if (entry == null || entry.closed || entry.expires <= now || entry.skill != request.skillId() || entry.level != request.level())
            throw new IllegalArgumentException("Unknown or completed native attack process");
        registry.processes.put(request.processId(),new Entry(entry.skill,entry.level,now+REPLAY_WINDOW_MS,true));
    }

    /** Close field-scoped live processes without discarding or extending the current replay window. */
    public static synchronized void onFieldChange(Char chr) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3) return;
        var registry = OWNERS.get(chr);
        if (registry == null) return;
        if (registry.client.get() != chr.getClient()) OWNERS.remove(chr);
        else closeOpen(registry);
    }

    private static void closeOpen(Registry registry) {
        registry.processes.replaceAll((id, entry) -> entry.closed ? entry
                : new Entry(entry.skill,entry.level,entry.expires,true));
    }

    private static void owned(Char chr,int skill,int level) {
        if (Protocol.active().version() != ProtocolVersion.TMS_282_3 || !supports(skill) || !JobConstants.isKhali(chr.getJob())
                || chr.getClient() == null || chr.getField() == null || level <= 0
                || Tms282HiddenSkillParents.authorisedLevel(chr,skill) != level)
            throw new IllegalArgumentException("Unowned Khali attack process");
    }

    private static Registry registry(Char chr,boolean create) {
        var registry = OWNERS.get(chr);
        if (registry != null && registry.client.get() != chr.getClient()) {
            OWNERS.remove(chr); registry = null;
        } else if (registry != null && registry.field.get() != chr.getField()) {
            closeOpen(registry);
            var next = new Registry(chr);
            next.processes.putAll(registry.processes); // Same session: a map change must not restore a closed nonce.
            OWNERS.put(chr,next); registry = next;
        }
        if (registry == null && create) { registry = new Registry(chr); OWNERS.put(chr,registry); }
        return registry;
    }
}
