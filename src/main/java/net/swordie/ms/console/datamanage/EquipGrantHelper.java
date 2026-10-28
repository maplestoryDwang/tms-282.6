package net.swordie.ms.console.datamanage;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;

public final class EquipGrantHelper {
    private EquipGrantHelper() {
    }

    public static Equip createEquip(int itemId, GiveItemSpec spec) {
        Equip eq = ItemData.getEquipDeepCopyFromID(itemId, false);
        if (eq == null) {
            eq = new Equip();
            eq.setItemId(itemId);
            eq.setInvType(ItemConstants.isEquip(itemId) ? InvType.EQUIP : InvType.DECORATION);
        }
        if (spec.equipPreset == GiveItemSpec.EquipPreset.CUSTOM) {
            applyCustomize(eq, spec.equip);
        }
        return eq;
    }

    public static void applyCustomize(Equip eq, GiveItemSpec.EquipCustomize c) {
        if (c.applyStr) {
            eq.setiStr(c.str);
        }
        if (c.applyDex) {
            eq.setiDex(c.dex);
        }
        if (c.applyInt) {
            eq.setiInt(c.int_);
        }
        if (c.applyLuk) {
            eq.setiLuk(c.luk);
        }
        if (c.applyHp) {
            eq.setiMaxHp(c.hp);
        }
        if (c.applyMp) {
            eq.setiMaxMp(c.mp);
        }
        if (c.applyWatk) {
            eq.setiPad(c.watk);
        }
        if (c.applyMatk) {
            eq.setiMad(c.matk);
        }
        if (c.applyWdef) {
            eq.setiPDD(c.wdef);
        }
        if (c.applyMdef) {
            eq.setiMDD(c.mdef);
        }
        if (c.applyAcc) {
            eq.setiAcc(c.acc);
        }
        if (c.applyAvoid) {
            eq.setiEva(c.avoid);
        }
        if (c.applySpeed) {
            eq.setiSpeed(c.speed);
        }
        if (c.applyJump) {
            eq.setiJump(c.jump);
        }
        if (c.applyEnhance) {
            eq.setChuc(c.enhance, true);
        }
        if (c.applyUpgradeSlots) {
            eq.setTuc(c.upgradeSlots);
        }
        if (c.applyLevel) {
            eq.setItemLevel(c.level);
        }
        if (c.applyPotential1) {
            eq.setOptionBase(0, c.potential1);
        }
        if (c.applyPotential2) {
            eq.setOptionBase(1, c.potential2);
        }
        if (c.applyPotential3) {
            eq.setOptionBase(2, c.potential3);
        }
        if (c.applyPotential4) {
            eq.setOptionBonus(0, c.potential4);
        }
        if (c.applyPotential5) {
            eq.setOptionBonus(1, c.potential5);
        }
        if (c.applyPotential6) {
            eq.setOptionBonus(2, c.potential6);
        }
        if (c.applyBossDamage) {
            eq.getFlameStat().setBossDamageR(c.bossDamage);
        }
        if (c.applyAllStat) {
            eq.getFlameStat().setAllStatR(c.allStat);
        }
        if (c.applyTotalDamage) {
            eq.getFlameStat().setDamage(c.totalDamage);
        }
        if (c.applyOwner && c.owner != null && !c.owner.isBlank()) {
            eq.setOwner(c.owner.trim());
        }
    }

    public static void loadTemplateIntoCustomize(int itemId, GiveItemSpec.EquipCustomize c) {
        Equip tpl = ItemData.getEquipDeepCopyFromID(itemId, false);
        if (tpl == null) {
            return;
        }
        c.str = tpl.getiStr();
        c.dex = tpl.getiDex();
        c.int_ = tpl.getiInt();
        c.luk = tpl.getiLuk();
        c.hp = tpl.getiMaxHp();
        c.mp = tpl.getiMaxMp();
        c.watk = tpl.getiPad();
        c.matk = tpl.getiMad();
        c.wdef = tpl.getiPDD();
        c.mdef = tpl.getiMDD();
        c.acc = tpl.getiAcc();
        c.avoid = tpl.getiEva();
        c.speed = tpl.getiSpeed();
        c.jump = tpl.getiJump();
        c.enhance = (byte) tpl.getChuc();
        c.upgradeSlots = (byte) tpl.getTuc();
        c.level = (byte) tpl.getItemLevel();
        c.potential1 = tpl.getOptionBase(0);
        c.potential2 = tpl.getOptionBase(1);
        c.potential3 = tpl.getOptionBase(2);
        c.potential4 = tpl.getOptionBonus(0);
        c.potential5 = tpl.getOptionBonus(1);
        c.potential6 = tpl.getOptionBonus(2);
        c.bossDamage = (short) tpl.getFlameStat().getBossDamageR();
        c.allStat = (short) tpl.getFlameStat().getAllStatR();
        c.totalDamage = (short) tpl.getFlameStat().getDamage();
        c.owner = tpl.getOwner() == null ? "" : tpl.getOwner();
    }

    public static void applyPeriod(Item item, int periodDays) {
        if (periodDays > 0) {
            long expire = System.currentTimeMillis() + periodDays * 86_400_000L;
            item.setDateExpire(FileTime.fromEpochMillis(expire));
        }
    }

    public static boolean isGrantableEquip(int itemId) {
        InvType type = ItemConstants.getInvTypeByItemID(itemId);
        return type == InvType.EQUIP || type == InvType.DECORATION;
    }
}
