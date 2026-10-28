package net.swordie.ms.scripts.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;

import java.util.HashMap;
import java.util.Map;

/**
 * Swordie {@link Instance} view of an {@link EventInstance}: a member bound to the event resolves the event's own
 * map copies through {@code Char#getOrCreateFieldByCurrentInstanceType}, and every other map id to the ordinary
 * channel map, so portals, deaths and script warps inside the event stay in the event while leaving it goes to the
 * public world. Swordie's instance code clears {@link #getFields()} on a warp out; this view hands out copies so the
 * event keeps its maps until {@link EventInstance#destroyEvent()}.
 */
public final class EventFieldInstance extends Instance {

    private final EventInstance event;

    EventFieldInstance(EventInstance event, Char owner) {
        super(owner);
        this.event = event;
    }

    public EventInstance getEvent() {
        return event;
    }

    @Override
    public Field getField(int fieldID) {
        Field owned = event.ownedField(fieldID);
        return owned != null ? owned : event.channelField(fieldID);
    }

    /** Snapshot of the event-owned fields (EventManager ticks them; callers may clear the copy freely). */
    @Override
    public Map<Integer, Field> getFields() {
        return new HashMap<>(event.ownedFields());
    }

    /** Swordie timeouts / GM commands clear an instance: end the event instead. */
    @Override
    public void clear() {
        event.destroyEvent();
    }
}
