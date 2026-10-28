package net.swordie.ms.console.datamanage;

/**
 * 控制檯發物參數：通用字段 + 裝備定製（僅裝備類生效）。
 */
public final class GiveItemSpec {
    public int quantity = 1;
    /** 0 = 永久 */
    public int periodDays = 0;

    public EquipPreset equipPreset = EquipPreset.CLEAN;
    public final EquipCustomize equip = new EquipCustomize();

    public enum EquipPreset {
        DEFAULT("默認模板"),
        CLEAN("白板"),
        CUSTOM("自定義");

        public final String label;

        EquipPreset(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class EquipCustomize {
        public boolean applyStr;
        public boolean applyDex;
        public boolean applyInt;
        public boolean applyLuk;
        public boolean applyHp;
        public boolean applyMp;
        public boolean applyWatk;
        public boolean applyMatk;
        public boolean applyWdef;
        public boolean applyMdef;
        public boolean applyAcc;
        public boolean applyAvoid;
        public boolean applySpeed;
        public boolean applyJump;
        public boolean applyEnhance;
        public boolean applyUpgradeSlots;
        public boolean applyLevel;
        public boolean applyState;
        public boolean applyLines;
        public boolean applyPotential1;
        public boolean applyPotential2;
        public boolean applyPotential3;
        public boolean applyPotential4;
        public boolean applyPotential5;
        public boolean applyPotential6;
        public boolean applyBossDamage;
        public boolean applyIgnorePdr;
        public boolean applyAllStat;
        public boolean applyTotalDamage;
        public boolean applyOwner;

        public short str;
        public short dex;
        public short int_;
        public short luk;
        public short hp;
        public short mp;
        public short watk;
        public short matk;
        public short wdef;
        public short mdef;
        public short acc;
        public short avoid;
        public short speed;
        public short jump;
        public byte enhance;
        public byte upgradeSlots;
        public byte level;
        public byte state;
        public byte lines;
        public int potential1;
        public int potential2;
        public int potential3;
        public int potential4;
        public int potential5;
        public int potential6;
        public short bossDamage;
        public short ignorePdr;
        public short allStat;
        public short totalDamage;
        public String owner = "";
    }
}
