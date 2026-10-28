package net.swordie.ms.client.character.items;

public class IntensePowerCrystalData {
    private int realMonster;
    private int dropMonster;
    private int namingMonster;
    private long meso;

    public IntensePowerCrystalData(int realMonster, int namingMonster, long meso) {
        this.realMonster = realMonster;
        this.namingMonster = namingMonster;
        this.meso = meso;
    }

    public int getRealMonster() {
        return this.realMonster;
    }

    public void setRealMonster(int realMonster) {
        this.realMonster = realMonster;
    }

    public int getDropMonster() {
        return this.dropMonster;
    }

    public void setDropMonster(int dropMonster) {
        this.dropMonster = dropMonster;
    }

    public int getNamingMonster() {
        return this.namingMonster;
    }

    public void setNamingMonster(int namingMonster) {
        this.namingMonster = namingMonster;
    }

    public long getMeso() {
        return this.meso;
    }

    public void setMeso(long meso) {
        this.meso = meso;
    }
}
