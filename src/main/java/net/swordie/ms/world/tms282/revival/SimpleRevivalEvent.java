package net.swordie.ms.world.tms282.revival;

import java.util.List;

/** An event whose behaviour is fully described by its spec plus fixed map / NPC lists. */
public record SimpleRevivalEvent(Spec spec, List<Integer> maps, List<Integer> npcs) implements RevivalEvent {
    public SimpleRevivalEvent(Spec spec) {
        this(spec, spec.entryMap() == 0 ? List.of() : List.of(spec.entryMap()), List.of());
    }

    public SimpleRevivalEvent {
        maps = List.copyOf(maps);
        npcs = List.copyOf(npcs);
    }

    @Override
    public List<Integer> requiredMaps() {
        return maps;
    }

    @Override
    public List<Integer> scriptedNpcs() {
        return npcs;
    }
}
