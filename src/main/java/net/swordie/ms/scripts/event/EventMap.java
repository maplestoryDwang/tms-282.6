package net.swordie.ms.scripts.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.ReactorPool;
import net.swordie.ms.enums.ClockType;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Clock;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.MobGen;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code event.getMap(id)} / {@code destination} of MetaMS event scripts. Wraps either an event-owned map copy
 * ({@link #isInstanced()}) or, for events that run on public maps (ghost hunting, world bosses), the channel map.
 */
public final class EventMap {

    private final EventInstance event;
    private final Field field;
    private final boolean instanced;
    private volatile boolean eventStarted;
    private volatile Object overrideFirstEnter;

    EventMap(EventInstance event, Field field, boolean instanced) {
        this.event = event;
        this.field = field;
        this.instanced = instanced;
    }

    public Field getField() {
        return field;
    }

    public int getId() {
        return field == null ? 0 : field.getId();
    }

    public int getFieldId() {
        return getId();
    }

    public int getMapId() {
        return getId();
    }

    public boolean isInstanced() {
        return instanced;
    }

    // ---- lifecycle -------------------------------------------------------------------------------------------------

    /** Clears mobs and drops and respawns the map's own mobs (MetaMS {@code reset()}); only for event-owned maps. */
    public void reset() {
        if (field == null || !instanced) {
            return;
        }
        for (Mob mob : new ArrayList<>(field.getMobs())) {
            event.ignoreMob(mob);
            field.removeMob(mob.getObjectId());
        }
        field.removeDrops();
        for (MobGen gen : new ArrayList<>(field.getMobGens())) {
            if (gen == null || gen.getMob() == null) {
                continue;
            }
            gen.setHasSpawned(false);
            gen.setNextPossibleSpawnTime(0);
            if (gen.canSpawnOnField(field)) {
                gen.spawnMob(field);
            }
        }
        eventStarted = false;
    }

    /** Map mechanic toggles of the MetaMS boss runtime; Swordie runs map mechanics from field scripts. */
    public void startEvent() {
        eventStarted = true;
    }

    public void endEvent() {
        eventStarted = false;
    }

    public boolean isEventStarted() {
        return eventStarted;
    }

    /**
     * Disables the map's own onUserEnter / onFirstUserEnter / field scripts on this event copy (e.g. the native
     * 30-second fire-wolf script on 993000500 when an event runs its own boss there). Event-owned maps only.
     */
    public void clearFieldScripts() {
        if (field == null || !instanced) {
            return;
        }
        field.setOnUserEnter("");
        field.setOnFirstUserEnter("");
        field.setFieldScript("");
    }

    public void setOverrideFirstEnter(Object value) {
        overrideFirstEnter = value;
    }

    public Object getOverrideFirstEnter() {
        return overrideFirstEnter;
    }

    // ---- mobs ------------------------------------------------------------------------------------------------------

    public EventMob spawnMob(int mobId, int x, int y) {
        return spawnMob(mobId, x, y, 0d);
    }

    /** Spawns a non-respawning mob; {@code hp > 0} overrides its max HP. The event receives its {@code mobDied}. */
    public EventMob spawnMob(int mobId, int x, int y, double hp) {
        if (field == null) {
            return null;
        }
        Mob mob = field.spawnMob(mobId, x, y, false, hp > 0 ? (long) hp : 0L);
        if (mob == null) {
            return null;
        }
        event.watchMob(mob);
        return new EventMob(mob);
    }

    public int killAllMob() {
        if (field == null) {
            return 0;
        }
        int n = 0;
        for (Mob mob : new ArrayList<>(field.getMobs())) {
            event.ignoreMob(mob);
            mob.removeWithAnimation();
            n++;
        }
        return n;
    }

    public int killMob(int mobId) {
        if (field == null) {
            return 0;
        }
        int n = 0;
        for (Mob mob : new ArrayList<>(field.getMobs())) {
            if (mob.getTemplateId() == mobId) {
                event.ignoreMob(mob);
                mob.removeWithAnimation();
                n++;
            }
        }
        return n;
    }

    public int getMobCount() {
        return field == null ? 0 : field.getMobs().size();
    }

    public int getMobSize() {
        return getMobCount();
    }

    public int getMobCount(int mobId) {
        if (field == null) {
            return 0;
        }
        int n = 0;
        for (Mob mob : field.getMobs()) {
            if (mob.getTemplateId() == mobId) {
                n++;
            }
        }
        return n;
    }

    public boolean hasMob(int mobId) {
        return getMobCount(mobId) > 0;
    }

    // ---- players ---------------------------------------------------------------------------------------------------

    public int getPlayerCount() {
        return field == null ? 0 : field.getChars().size();
    }

    public int getCharacterCount() {
        return getPlayerCount();
    }

    /** Players on this map as script objects. */
    public List<Object> getPlayers() {
        List<Object> out = new ArrayList<>();
        if (field != null) {
            for (Char chr : new ArrayList<>(field.getChars())) {
                out.add(event.manager().host().player(chr));
            }
        }
        return out;
    }

    /** Chat line to everyone on the map. */
    public void broadcastMessage(String msg) {
        if (field == null) {
            return;
        }
        for (Char chr : new ArrayList<>(field.getChars())) {
            chr.chatMessage(msg);
        }
    }

    public void broadcastMessage(int type, String msg) {
        broadcastMessage(msg);
    }

    /** Map-wide seconds clock. */
    public void showTimer(int seconds) {
        if (field != null && seconds > 0) {
            new Clock(ClockType.SecondsClock, field, seconds);
        }
    }

    /** Sets the state of the reactors named {@code name} (or with template id {@code name}) on this map. */
    public int setReactorState(Object name, int state) {
        if (field == null || name == null) {
            return 0;
        }
        String key = String.valueOf(name);
        int n = 0;
        for (Reactor reactor : new ArrayList<>(field.getReactors())) {
            if (key.equals(reactor.getName()) || key.equals(String.valueOf(reactor.getTemplateId()))) {
                reactor.setState((byte) state);
                field.broadcast(ReactorPool.reactorChangeState(reactor, (short) 0, 0));
                n++;
            }
        }
        return n;
    }

    @Override
    public String toString() {
        return "EventMap[" + getId() + (instanced ? ",instanced" : ",public") + "]";
    }
}
