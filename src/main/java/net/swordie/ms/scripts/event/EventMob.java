package net.swordie.ms.scripts.event;

import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;

/** The {@code mob} argument of MetaMS event callbacks ({@code mobDied(mob)}, {@code mobHit(player, mob, damage)}). */
public final class EventMob {

    private final Mob mob;

    public EventMob(Mob mob) {
        this.mob = mob;
    }

    public Mob getMob() {
        return mob;
    }

    public int getDataId() {
        return mob.getTemplateId();
    }

    public int getId() {
        return mob.getTemplateId();
    }

    public int getTemplateId() {
        return mob.getTemplateId();
    }

    public int getObjectId() {
        return mob.getObjectId();
    }

    public long getHp() {
        return mob.getHp();
    }

    public long getMaxHp() {
        return mob.getMaxHp();
    }

    public boolean isBoss() {
        return mob.isBoss();
    }

    public int getX() {
        return mob.getPosition() == null ? 0 : mob.getPosition().getX();
    }

    public int getY() {
        return mob.getPosition() == null ? 0 : mob.getPosition().getY();
    }

    public int getMapId() {
        Field f = mob.getField();
        return f == null ? 0 : f.getId();
    }

    @Override
    public String toString() {
        return "EventMob[" + getDataId() + "#" + getObjectId() + "]";
    }
}
