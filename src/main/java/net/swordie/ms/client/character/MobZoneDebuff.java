package net.swordie.ms.client.character;

public class MobZoneDebuff {

    private Char chr;
    private int damage;
    private int healRate;

    public MobZoneDebuff(Char chr) {
        this.chr = chr;
        this.damage = 0;
        this.healRate = 0;
    }

    public Char getChr() {
        return chr;
    }

    public void setChr(Char chr) {
        this.chr = chr;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getHealRate() {
        return healRate;
    }

    public void setHealRate(int healRate) {
        this.healRate = healRate;
    }
}
