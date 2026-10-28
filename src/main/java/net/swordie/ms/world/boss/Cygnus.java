package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.world.field.Field;

public class Cygnus {

    public static void spawn(Char chr, CygnusMode mode) {
        if (BossHelper.checkInstance(chr)) {
            return;
        }

        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        if (mode.getVal() == 0 && !field.isBossSpawned()) {
            field.getTimer().addEvent(() -> field.spawnMob(8850111, -147, 115, false, 10500000000L), 2000);
            field.setBossSpawned(true);
        } else if (mode.getVal() == 1 && !field.isBossSpawned()) {
            field.getTimer().addEvent(() -> field.spawnMob(8850011, -147, 115, false, 63000000000L), 2000);
            field.setBossSpawned(true);
        }
    }

    public enum CygnusMode {
        EASY(0),
        NORMAL(1),
        HARD(2),
        ;

        private final int val;

        CygnusMode(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }
}
