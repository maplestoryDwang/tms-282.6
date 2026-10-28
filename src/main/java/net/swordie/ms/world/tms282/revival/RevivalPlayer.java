package net.swordie.ms.world.tms282.revival;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.scripts.event.EventManager;

/** What an entry command needs from a character; {@link #of(Char)} is the live adapter, checks use fakes. */
public interface RevivalPlayer {
    int level();

    int fieldId();

    boolean mapExists(int mapId);

    /** In an event instance or another blocking state: entering would strand the instance. */
    boolean busy();

    void warp(int mapId, int portal);

    static RevivalPlayer of(Char chr) {
        return new RevivalPlayer() {
            @Override
            public int level() {
                return chr.getLevel();
            }

            @Override
            public int fieldId() {
                return chr.getField() == null ? 0 : chr.getField().getId();
            }

            @Override
            public boolean mapExists(int mapId) {
                return FieldData.getFieldById(mapId) != null;
            }

            @Override
            public boolean busy() {
                return EventManager.get().getEvent(chr) != null || chr.getInstance() != null;
            }

            @Override
            public void warp(int mapId, int portal) {
                chr.warp(mapId, portal);
            }
        };
    }
}
