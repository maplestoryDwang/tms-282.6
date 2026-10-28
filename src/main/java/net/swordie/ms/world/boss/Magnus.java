package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.world.field.Field;

public class Magnus {

    public static void spawn(Char chr, MagnusMode mode) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        switch (mode) {
            case EASY: // Easy
                if (!field.isBossSpawned()) {
                    field.setBossSpawned(true);
                    field.getTimer().addEvent(() -> field.spawnMob(8880010, 1900, -1347, false), 2000);
                }
                break;
            case NORMAL: // Normal
                if (!field.isBossSpawned()) {
                    field.setBossSpawned(true);
                    field.getTimer().addEvent(() -> field.spawnMob(8880002, 1900, -1347, false), 2000);
                }
                break;
            case HARD: // Hard
                if (!field.isBossSpawned()) {
                    field.setBossSpawned(true);
                    field.getTimer().addEvent(() -> field.spawnMob(8880000, 1900, -1347, false), 2000);
                }
                break;
        }
    }

    public enum MagnusMode {
        EASY(0),
        NORMAL(1),
        HARD(2),
        ;

        private final int val;

        MagnusMode(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }
}
