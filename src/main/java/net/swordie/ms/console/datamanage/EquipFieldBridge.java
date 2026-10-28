package net.swordie.ms.console.datamanage;

/** 裝備自定義字段與 {@link GiveItemSpec.EquipCustomize} 的讀寫橋接。 */
final class EquipFieldBridge {
    private EquipFieldBridge() {
    }

    static void setApply(GiveItemSpec.EquipCustomize c, String key, boolean v) {
        switch (key) {
            case "applyStr" -> c.applyStr = v;
            case "applyDex" -> c.applyDex = v;
            case "applyInt" -> c.applyInt = v;
            case "applyLuk" -> c.applyLuk = v;
            case "applyHp" -> c.applyHp = v;
            case "applyMp" -> c.applyMp = v;
            case "applyWatk" -> c.applyWatk = v;
            case "applyMatk" -> c.applyMatk = v;
            case "applyWdef" -> c.applyWdef = v;
            case "applyMdef" -> c.applyMdef = v;
            case "applyAcc" -> c.applyAcc = v;
            case "applyAvoid" -> c.applyAvoid = v;
            case "applySpeed" -> c.applySpeed = v;
            case "applyJump" -> c.applyJump = v;
            case "applyEnhance" -> c.applyEnhance = v;
            case "applyUpgradeSlots" -> c.applyUpgradeSlots = v;
            case "applyLevel" -> c.applyLevel = v;
            case "applyState" -> c.applyState = v;
            case "applyLines" -> c.applyLines = v;
            case "applyPotential1" -> c.applyPotential1 = v;
            case "applyPotential2" -> c.applyPotential2 = v;
            case "applyPotential3" -> c.applyPotential3 = v;
            case "applyPotential4" -> c.applyPotential4 = v;
            case "applyPotential5" -> c.applyPotential5 = v;
            case "applyPotential6" -> c.applyPotential6 = v;
            case "applyBossDamage" -> c.applyBossDamage = v;
            case "applyIgnorePdr" -> c.applyIgnorePdr = v;
            case "applyAllStat" -> c.applyAllStat = v;
            case "applyTotalDamage" -> c.applyTotalDamage = v;
            case "applyOwner" -> c.applyOwner = v;
            default -> {
            }
        }
    }

    static boolean getApply(GiveItemSpec.EquipCustomize c, String key) {
        return switch (key) {
            case "applyStr" -> c.applyStr;
            case "applyDex" -> c.applyDex;
            case "applyInt" -> c.applyInt;
            case "applyLuk" -> c.applyLuk;
            case "applyHp" -> c.applyHp;
            case "applyMp" -> c.applyMp;
            case "applyWatk" -> c.applyWatk;
            case "applyMatk" -> c.applyMatk;
            case "applyWdef" -> c.applyWdef;
            case "applyMdef" -> c.applyMdef;
            case "applyAcc" -> c.applyAcc;
            case "applyAvoid" -> c.applyAvoid;
            case "applySpeed" -> c.applySpeed;
            case "applyJump" -> c.applyJump;
            case "applyEnhance" -> c.applyEnhance;
            case "applyUpgradeSlots" -> c.applyUpgradeSlots;
            case "applyLevel" -> c.applyLevel;
            case "applyState" -> c.applyState;
            case "applyLines" -> c.applyLines;
            case "applyPotential1" -> c.applyPotential1;
            case "applyPotential2" -> c.applyPotential2;
            case "applyPotential3" -> c.applyPotential3;
            case "applyPotential4" -> c.applyPotential4;
            case "applyPotential5" -> c.applyPotential5;
            case "applyPotential6" -> c.applyPotential6;
            case "applyBossDamage" -> c.applyBossDamage;
            case "applyIgnorePdr" -> c.applyIgnorePdr;
            case "applyAllStat" -> c.applyAllStat;
            case "applyTotalDamage" -> c.applyTotalDamage;
            case "applyOwner" -> c.applyOwner;
            default -> false;
        };
    }

    static void setValue(GiveItemSpec.EquipCustomize c, String key, int val) {
        switch (key) {
            case "str" -> c.str = (short) val;
            case "dex" -> c.dex = (short) val;
            case "int_" -> c.int_ = (short) val;
            case "luk" -> c.luk = (short) val;
            case "hp" -> c.hp = (short) val;
            case "mp" -> c.mp = (short) val;
            case "watk" -> c.watk = (short) val;
            case "matk" -> c.matk = (short) val;
            case "wdef" -> c.wdef = (short) val;
            case "mdef" -> c.mdef = (short) val;
            case "acc" -> c.acc = (short) val;
            case "avoid" -> c.avoid = (short) val;
            case "speed" -> c.speed = (short) val;
            case "jump" -> c.jump = (short) val;
            case "enhance" -> c.enhance = (byte) val;
            case "upgradeSlots" -> c.upgradeSlots = (byte) val;
            case "level" -> c.level = (byte) val;
            case "state" -> c.state = (byte) val;
            case "lines" -> c.lines = (byte) val;
            case "potential1" -> c.potential1 = val;
            case "potential2" -> c.potential2 = val;
            case "potential3" -> c.potential3 = val;
            case "potential4" -> c.potential4 = val;
            case "potential5" -> c.potential5 = val;
            case "potential6" -> c.potential6 = val;
            case "bossDamage" -> c.bossDamage = (short) val;
            case "ignorePdr" -> c.ignorePdr = (short) val;
            case "allStat" -> c.allStat = (short) val;
            case "totalDamage" -> c.totalDamage = (short) val;
            default -> {
            }
        }
    }

    static int getValue(GiveItemSpec.EquipCustomize c, String key) {
        return switch (key) {
            case "str" -> c.str;
            case "dex" -> c.dex;
            case "int_" -> c.int_;
            case "luk" -> c.luk;
            case "hp" -> c.hp;
            case "mp" -> c.mp;
            case "watk" -> c.watk;
            case "matk" -> c.matk;
            case "wdef" -> c.wdef;
            case "mdef" -> c.mdef;
            case "acc" -> c.acc;
            case "avoid" -> c.avoid;
            case "speed" -> c.speed;
            case "jump" -> c.jump;
            case "enhance" -> c.enhance;
            case "upgradeSlots" -> c.upgradeSlots;
            case "level" -> c.level;
            case "state" -> c.state;
            case "lines" -> c.lines;
            case "potential1" -> c.potential1;
            case "potential2" -> c.potential2;
            case "potential3" -> c.potential3;
            case "potential4" -> c.potential4;
            case "potential5" -> c.potential5;
            case "potential6" -> c.potential6;
            case "bossDamage" -> c.bossDamage;
            case "ignorePdr" -> c.ignorePdr;
            case "allStat" -> c.allStat;
            case "totalDamage" -> c.totalDamage;
            default -> 0;
        };
    }
}
