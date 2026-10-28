package net.swordie.ms.enums;

import net.swordie.ms.constants.JobConstants;

import java.util.Arrays;

public enum WeaponType {
    // WT_ Enum, but renamed some to make more sense
    None(1.43f, 0),
    ShiningRod(1.2f, 212),          // Luminous
    Bladecaster(1.3f, 213),                // Adele
    Whispershot(1.3f, 214),          // Kain
    RenSword(1.3f, 215),             // Ren
    SoulShooter(1.7f, 222),          // Angelic Buster
    Desperado(1.3f, 232),            // Demon Avenger
    ChainSword(1.3125f, 242),       // Xenon
    MemoryStaff(1.34f, 252),         // Lynn (Lynn magician)
    PsyLimiter(1.2f, 262),           // Kinesis
    Chain(1.3f, 272),                // Cadena
    Gauntlet(1.3f, 282),             // Illium
    RitualFan(1.3f, 292),            // HoYoung
    OneHandedSword(1.2f, 302),       // Hero, Mihile, Paladin
    OneHandedAxe(1.2f, 312),         // Demon Slayer
    OneHandedMace(1.2f, 322),
    Dagger(1.3f, 332),               // Night Lord, Shadower
    Katara(1.3f, 342),               // Dual Blade
    Cane(1.3f, 362),                 // Phantom
    Wand(1.0f, 372),                 // Mage classes
    Staff(1.0f, 382),                // Flame Wizard
    TwoHandedSword(1.34f, 402),      // Dawn Warrior, Kaiser
    Fist(1.7f, 403),                 // Mo Xuan
    Chakram(1.3f, 404),              // Khali
    TwoHandedAxe(1.34f, 412),
    TwoHandedMace(1.34f, 422),
    Spear(1.49f, 432),               // Dark Knight
    Polearm(1.49f, 442),             // Aran
    Bow(1.3f, 452),                  // Bowmaster, Wind Archer
    Crossbow(1.35f, 462),            // Marksman, Wild Hunter
    Claw(1.75f, 472),                // Night Lord, Night Walker
    Knuckle(1.7f, 482),              // Shade, Thunder Breaker, Ark, Buccaneer
    Gun(1.5f, 492),                  // Corsair, Mechanic
    DualBowgun(1.3f, 522),           // Mercedes
    HandCannon(1.5f, 532),           // Cannoneer
    Katana(1.25f, 542),              // Hayato
    Fan(1.35f, 552),                 // Kanna
    BigSword(1.49f, 562),            // Zero (Ryude)
    LongSword(1.34f, 572),           // Zero (Lui)
    ArmCannon(1.7f, 582),            // Blaster
    AncientBow(1.3f, 592),           // Pathfinder
    ;

    private final float damageMultiplier;
    private final int val;

    WeaponType(float maxDamageMultiplier, int val) {
        this.damageMultiplier = maxDamageMultiplier;
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public static WeaponType getByVal(int val) {
        return Arrays.stream(values()).filter(wt -> wt.getVal() == val).findAny().orElse(None);
    }

    public float getMaxDamageMultiplier(short job) {
        if (this == None && !JobConstants.isPirateEquipJob(job)) {
            return 0;
        }
        if ((JobConstants.isAdventurerMage(job) || JobConstants.isBlazeWizard(job)) && (this == Wand || this == Staff)) {
            return 1.2f;
        }
        if ((JobConstants.isHero(job) || JobConstants.isPaladin(job)) && (this == TwoHandedSword || this == TwoHandedAxe)) {
            return 1.44f;
        }
        if ((JobConstants.isHero(job) || JobConstants.isPaladin(job)) && (this == OneHandedSword || this == OneHandedAxe)) {
            return 1.3f;
        }
        return damageMultiplier;
    }
}
