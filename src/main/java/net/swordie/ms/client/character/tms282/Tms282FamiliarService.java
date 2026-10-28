package net.swordie.ms.client.character.tms282;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.IntPredicate;

/** Pure owned-record edits only. No card creation, summoning, inventory consumption, persistence or packets.
 * The supplied predicate must use actual282 familiar appearance metadata, not273 lottery weights.
 */
public final class Tms282FamiliarService {
    private final IntPredicate knownFamiliarId;
    public Tms282FamiliarService(IntPredicate knownFamiliarId) { this.knownFamiliarId = Objects.requireNonNull(knownFamiliarId); }

    public Tms282FamiliarState favorite(Tms282FamiliarState state, int requestingCharacterId, long serial, boolean value) {
        return attribute(state, requestingCharacterId, serial, 0x04, value);
    }
    /** Lock RPC's string/int selectors are not resolved here; caller must supply a verified owned serial. */
    public Tms282FamiliarState lock(Tms282FamiliarState state, int requestingCharacterId, long serial, boolean value) {
        return attribute(state, requestingCharacterId, serial, 0x10, value);
    }
    public Tms282FamiliarState rename(Tms282FamiliarState state, int requestingCharacterId, long serial, String name) {
        var current = owned(state, requestingCharacterId, serial);
        Tms282FamiliarState.encodeName(name);
        return current.name().equals(name) ? state : replace(state, current.withName(name));
    }
    private Tms282FamiliarState attribute(Tms282FamiliarState state, int characterId, long serial, int mask, boolean value) {
        var current = owned(state, characterId, serial);
        int next = value ? current.attributes() | mask : current.attributes() & ~mask;
        return next == current.attributes() ? state : replace(state, current.withAttributes(next));
    }
    private Tms282FamiliarState.RegisteredInfo owned(Tms282FamiliarState state, int characterId, long serial) {
        if (state.characterId() != characterId) throw new IllegalArgumentException("Familiar owner mismatch");
        var current = state.registered().get(serial);
        if (current == null) throw new IllegalArgumentException("Unknown owned familiar serial");
        if (current.familiarId() > Integer.MAX_VALUE || !knownFamiliarId.test((int)current.familiarId()))
            throw new IllegalArgumentException("Familiar ID absent from282 metadata");
        return current;
    }
    private static Tms282FamiliarState replace(Tms282FamiliarState state, Tms282FamiliarState.RegisteredInfo record) {
        var records = new HashMap<>(state.registered()); records.put(record.serial(), record);
        return new Tms282FamiliarState(state.characterId(), Math.addExact(state.revision(),1), records, state.summonedSerial(), state.essence());
    }
}
