package net.swordie.ms.constants;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.enums.WeaponType;
import net.swordie.ms.util.Util;

import java.util.*;

public class JobConstants {

    public static final boolean enableJobs = true;
    public static final int jobOrder = 103;

    public static String getJobNameById(short jobId) {
        // Explorer
        if (isHero(jobId)) {
            return "Hero ";
        } else if (isPaladin(jobId)) {
            return "Paladin ";
        } else if (isDarkKnight(jobId)) {
            return "Dark Knight ";
        } else if (isFirePoison(jobId)) {
            return "Arch Mage (Fire, Poison) ";
        } else if (isIceLightning(jobId)) {
            return "Arch Mage (Ice, Lightning) ";
        } else if (isBishop(jobId)) {
            return "Bishop ";
        } else if (isBowMaster(jobId)) {
            return "Bowmaster ";
        } else if (isMarksman(jobId)) {
            return "Marksman ";
        } else if (isPathFinder(jobId)) {
            return "Pathfinder ";
        } else if (isNightLord(jobId)) {
            return "Night Lord ";
        } else if (isShadower(jobId)) {
            return "Shadower ";
        } else if (isDualBlade(jobId)) {
            return "Dual Blade ";
        } else if (isBuccaneer(jobId)) {
            return "Buccaneer ";
        } else if (isCorsair(jobId)) {
            return "Corsair ";
        } else if (isCannoneer(jobId)) {
            return "Cannoneer ";

            // Cygnus Knights
        } else if (isDawnWarrior(jobId)) {
            return "Dawn Warrior ";
        } else if (isBlazeWizard(jobId)) {
            return "Blaze Wizard ";
        } else if (isWindArcher(jobId)) {
            return "Wind Archer ";
        } else if (isNightWalker(jobId)) {
            return "Night Walker ";
        } else if (isThunderBreaker(jobId)) {
            return "Thunder Breaker ";
        } else if (isMihile(jobId)) {
            return "Mihile ";

            // Heroes
        } else if (isAran(jobId)) {
            return "Aran ";
        } else if (isEvan(jobId)) {
            return "Evan ";
        } else if (isMercedes(jobId)) {
            return "Mercedes ";
        } else if (isPhantom(jobId)) {
            return "Phantom ";
        } else if (isShade(jobId)) {
            return "Shade ";
        } else if (isLuminous(jobId)) {
            return "Luminous ";

            // Resistance
        } else if (isDemonAvenger(jobId)) {
            return "Demon Avenger ";
        } else if (isDemonSlayer(jobId)) {
            return "Demon Slayer ";
        } else if (isBattleMage(jobId)) {
            return "Battle Mage ";
        } else if (isWildHunter(jobId)) {
            return "Wild Hunter ";
        } else if (isMechanic(jobId)) {
            return "Mechanic ";
        } else if (isXenon(jobId)) {
            return "Xenon ";
        } else if (isBlaster(jobId)) {
            return "Blaster ";

            // Nova / Flora / Anima / etc (newer jobs)
        } else if (isKaiser(jobId)) {
            return "Kaiser ";
        } else if (isAngelicBuster(jobId)) {
            return "Angelic Buster ";
        } else if (isCadena(jobId)) {
            return "Cadena ";
        } else if (isKain(jobId)) {
            return "Kain ";
        } else if (isAdele(jobId)) {
            return "Adele ";
        } else if (isIllium(jobId)) {
            return "Illium ";
        } else if (isArk(jobId)) {
            return "Ark ";
        } else if (isKhali(jobId)) {
            return "Khali ";
        } else if (isHoYoung(jobId)) {
            return "Hoyoung ";
        } else if (isLara(jobId)) {
            return "Lara ";
        } else if (isRen(jobId)) {
            return "Ren ";
        } else if (isLynn(jobId)) {
            return "Lynn ";

            // Sengoku
        } else if (isHayato(jobId)) {
            return "Hayato ";
        } else if (isKanna(jobId)) {
            return "Kanna ";

            // Special
        } else if (isZero(jobId)) {
            return "Zero ";
        } else if (isPinkBean(jobId)) {
            return "Pink Bean ";
        } else if (isTanjiro(jobId)) {
            return "Tanjiro Kamado ";
        } else if (isSaitama(jobId)) {
            return "Saitama ";
        } else if (isYeti(jobId)) {
            return "Yeti ";
        } else if (isKinesis(jobId)) {
            return "Kinesis ";
        } else if (isBeastTamer(jobId)) { // legacy (creation disabled; replaced by Lynn in some regions) :contentReference[oaicite:2]{index=2}
            return "Beast Tamer ";
        } else if (isJett(jobId)) { // removed around Feb 2024 in GMS :contentReference[oaicite:3]{index=3}
            return "Jett ";

        } else {
            return "Beginner ";
        }
    }

    public enum JobEnum {
        BEGINNER(0, 0),
        WARRIOR(100, 0),
        FIGHTER(110, 0),
        CRUSADER(111, 0),
        HERO(112, 0),
        PAGE(120, 0),
        WHITEKNIGHT(121, 0),
        PALADIN(122, 0),
        SPEARMAN(130, 0),
        DRAGONKNIGHT(131, 0),
        DARKKNIGHT(132, 0),
        MAGICIAN(200, 0),
        FP_WIZARD(210, 0),
        FP_MAGE(211, 0),
        FP_ARCHMAGE(212, 0),
        IL_WIZARD(220, 0),
        IL_MAGE(221, 0),
        IL_ARCHMAGE(222, 0),
        CLERIC(230, 0),
        PRIEST(231, 0),
        BISHOP(232, 0),
        BOWMAN(300, 0),
        HUNTER(310, 0),
        RANGER(311, 0),
        BOWMASTER(312, 0),
        CROSSBOWMAN(320, 0),
        SNIPER(321, 0),
        MARKSMAN(322, 0),
        PATHFINDER_1(301, 0),
        PATHFINDER_2(330, 0),
        PATHFINDER_3(331, 0),
        PATHFINDER_4(332, 0),
        THIEF(400, 0),
        ASSASSIN(410, 0),
        HERMIT(411, 0),
        NIGHTLORD(412, 0),
        BANDIT(420, 0),
        CHIEFBANDIT(421, 0),
        SHADOWER(422, 0),
        BLADE_RECRUIT(430, 0),
        BLADE_ACOLYTE(431, 0),
        BLADE_SPECIALIST(432, 0),
        BLADE_LORD(433, 0),
        BLADE_MASTER(434, 0),
        PIRATE(500, 0),
        PIRATE_CANNONNEER(501, 0),
        JETT1(508, 0),
        BRAWLER(510, 0),
        MARAUDER(511, 0),
        BUCCANEER(512, 0),
        GUNSLINGER(520, 0),
        OUTLAW(521, 0),
        CORSAIR(522, 0),
        CANNONEER(530, 0),
        CANNON_BLASTER(531, 0),
        CANNON_MASTER(532, 0),
        JETT2(570, 0),
        JETT3(571, 0),
        JETT4(572, 0),
        MANAGER(800, 0),
        GM(900, 0),
        SUPERGM(910, 0),
        NOBLESSE(1000, 1000),
        DAWNWARRIOR1(1100, 1000),
        DAWNWARRIOR2(1110, 1000),
        DAWNWARRIOR3(1111, 1000),
        DAWNWARRIOR4(1112, 1000),
        BLAZEWIZARD1(1200, 1000),
        BLAZEWIZARD2(1210, 1000),
        BLAZEWIZARD3(1211, 1000),
        BLAZEWIZARD4(1212, 1000),
        WINDARCHER1(1300, 1000),
        WINDARCHER2(1310, 1000),
        WINDARCHER3(1311, 1000),
        WINDARCHER4(1312, 1000),
        NIGHTWALKER1(1400, 1000),
        NIGHTWALKER2(1410, 1000),
        NIGHTWALKER3(1411, 1000),
        NIGHTWALKER4(1412, 1000),
        THUNDERBREAKER1(1500, 1000),
        THUNDERBREAKER2(1510, 1000),
        THUNDERBREAKER3(1511, 1000),
        THUNDERBREAKER4(1512, 1000),
        LEGEND(2000, 2000),
        EVAN_NOOB(2001, 2001),
        MERCEDES(2002, 2002),
        PHANTOM(2003, 2003),
        SHADE(2005, 2005),
        ARAN1(2100, 2000),
        ARAN2(2110, 2000),
        ARAN3(2111, 2000),
        ARAN4(2112, 2000),
        EVAN1(2210, 2001),
        EVAN2(2212, 2001),
        EVAN3(2214, 2001),
        EVAN4(2217, 2001),
        MERCEDES1(2300, 2002),
        MERCEDES2(2310, 2002),
        MERCEDES3(2311, 2002),
        MERCEDES4(2312, 2002),
        PHANTOM1(2400, 2003),
        PHANTOM2(2410, 2003),
        PHANTOM3(2411, 2003),
        PHANTOM4(2412, 2003),
        SHADE1(2500, 2005),
        SHADE2(2510, 2005),
        SHADE3(2511, 2005),
        SHADE4(2512, 2005),
        LUMINOUS(2004, 2004),
        LUMINOUS1(2700, 2004),
        LUMINOUS2(2710, 2004),
        LUMINOUS3(2711, 2004),
        LUMINOUS4(2712, 2004),
        CITIZEN(3000, 3000),
        DEMON(3001, 3001),
        XENON(3002, 3002),
        DEMON_SLAYER1(3100, 3001),
        DEMON_SLAYER2(3110, 3001),
        DEMON_SLAYER3(3111, 3001),
        DEMON_SLAYER4(3112, 3001),
        DEMON_AVENGER1(3101, 3001),
        DEMON_AVENGER2(3120, 3001),
        DEMON_AVENGER3(3121, 3001),
        DEMON_AVENGER4(3122, 3001),
        BATTLE_MAGE_1(3200, 3000),
        BATTLE_MAGE_2(3210, 3000),
        BATTLE_MAGE_3(3211, 3000),
        BATTLE_MAGE_4(3212, 3000),
        WILD_HUNTER_1(3300, 3000),
        WILD_HUNTER_2(3310, 3000),
        WILD_HUNTER_3(3311, 3000),
        WILD_HUNTER_4(3312, 3000),
        MECHANIC_1(3500, 3000),
        MECHANIC_2(3510, 3000),
        MECHANIC_3(3511, 3000),
        MECHANIC_4(3512, 3000),
        XENON1(3600, 3002),
        XENON2(3610, 3002),
        XENON3(3611, 3002),
        XENON4(3612, 3002),
        BLASTER_1(3700, 3000),
        BLASTER_2(3710, 3000),
        BLASTER_3(3711, 3000),
        BLASTER_4(3712, 3000),
        HAYATO(4001, 4001),
        KANNA(4002, 4002),
        HAYATO1(4100, 4001),
        HAYATO2(4110, 4001),
        HAYATO3(4111, 4001),
        HAYATO4(4112, 4001),
        KANNA1(4200, 4002),
        KANNA2(4210, 4002),
        KANNA3(4211, 4002),
        KANNA4(4212, 4002),
        NAMELESS_WARDEN(5000, 5000),
        MIHILE1(5100, 5000),
        MIHILE2(5110, 5000),
        MIHILE3(5111, 5000),
        MIHILE4(5112, 5000),
        KAISER(6000, 6000),
        KAISER1(6100, 6000),
        KAISER2(6110, 6000),
        KAISER3(6111, 6000),
        KAISER4(6112, 6000),
        CADENA(6002, 6002),
        CADENA_1(6400, 6002),
        CADENA_2(6410, 6002),
        CADENA_3(6411, 6002),
        CADENA_4(6412, 6002),
        KAIN(6003, 6003),
        KAIN_1(6300, 6003),
        KAIN_2(6310, 6003),
        KAIN_3(6311, 6003),
        KAIN_4(6312, 6003),
        ANGELIC_BUSTER(6001, 6001),
        ANGELIC_BUSTER1(6500, 6001),
        ANGELIC_BUSTER2(6510, 6001),
        ANGELIC_BUSTER3(6511, 6001),
        ANGELIC_BUSTER4(6512, 6001),
        ADDITIONAL_SKILLS(9000, 0),
        ZERO(10000, 10000),
        ZERO1(10100, 10000),
        ZERO2(10110, 10000),
        ZERO3(10111, 10000),
        ZERO4(10112, 10000),

        BEAST_TAMER(11000, 11000),
        BEAST_TAMER_1(11200, 11000),
        BEAST_TAMER_2(11210, 11000),
        BEAST_TAMER_3(11211, 11000),
        BEAST_TAMER_4(11212, 11000),

        TANJIRO_BEGINNER(12005, 12005),
        TANJIRO_KAMADO(12100, 12005),
        SAITAMA_BEGINNER(12006, 12006),
        SAITAMA(12200, 12006),

        PINK_BEAN_0(13000, 13000),
        PINK_BEAN_1(13100, 13000),
        YETI_BEGINNER(13001, 13001),
        YETI(13500, 13001),

        KINESIS_0(14000, 14000),
        KINESIS_1(14200, 14000),
        KINESIS_2(14210, 14000),
        KINESIS_3(14211, 14000),
        KINESIS_4(14212, 14000),

        ARK(15001, 15001),
        ARK_1(15500, 15001),
        ARK_2(15510, 15001),
        ARK_3(15511, 15001),
        ARK_4(15512, 15001),

        ADELE(15002, 15002),
        ADELE_1(15100, 15002),
        ADELE_2(15110, 15002),
        ADELE_3(15111, 15002),
        ADELE_4(15112, 15002),

        KHALI(15003, 15003),
        KHALI_1(15400, 15003),
        KHALI_2(15410, 15003),
        KHALI_3(15411, 15003),
        KHALI_4(15412, 15003),

        ILLIUM(15000, 15000),
        ILLIUM_1(15200, 15000),
        ILLIUM_2(15210, 15000),
        ILLIUM_3(15211, 15000),
        ILLIUM_4(15212, 15000),

        HOYOUNG(16000, 16000),
        HOYOUNG_1(16400, 16000),
        HOYOUNG_2(16410, 16000),
        HOYOUNG_3(16411, 16000),
        HOYOUNG_4(16412, 16000),

        LARA(16001, 16001),
        LARA_1(16200, 16001),
        LARA_2(16210, 16001),
        LARA_3(16211, 16001),
        LARA_4(16212, 16001),

        REN(16002, 16002),
        REN_1(16100, 16002),
        REN_2(16110, 16002),
        REN_3(16111, 16002),
        REN_4(16112, 16002),

        LYNN(17001, 17001),
        LYNN_1(17200, 17001),
        LYNN_2(17210, 17001),
        LYNN_3(17211, 17001),
        LYNN_4(17212, 17001),

        MOXUAN(17000, 17000),
        MOXUAN_1(17500, 17000),
        MOXUAN_2(17510, 17000),
        MOXUAN_3(17511, 17000),
        MOXUAN_4(17512, 17000),

        SIA(18000, 18000),
        SIA_1(18200, 18000),
        SIA_2(18210, 18000),
        SIA_3(18211, 18000),
        SIA_4(18212, 18000),

        EMPTY_0(30000, 0),
        V_SKILLS_COMMON(40000, 0),
        V_SKILLS_WARRIOR(40001, 0),
        V_SKILLS_MAGE(40002, 0),
        V_SKILLS_ARCHER(40003, 0),
        V_SKILLS_THIEF(40004, 0),
        V_SKILLS_PIRATE(40005, 0),
        PINK_BEAN_EMPTY_0(800000, 13000),
        PINK_BEAN_EMPTY_1(800001, 13000),
        PINK_BEAN_EMPTY_2(800002, 13000),
        PINK_BEAN_EMPTY_3(800003, 13000),
        PINK_BEAN_EMPTY_4(800004, 13000),
        PINK_BEAN_EMPTY_5(800010, 13000),
        PINK_BEAN_EMPTY_6(800011, 13000),
        PINK_BEAN_EMPTY_7(800012, 13000),
        PINK_BEAN_EMPTY_8(800013, 13000),
        PINK_BEAN_EMPTY_9(800014, 13000),
        PINK_BEAN_EMPTY_10(800015, 13000),
        PINK_BEAN_EMPTY_11(800016, 13000),
        PINK_BEAN_EMPTY_12(800017, 13000),
        PINK_BEAN_EMPTY_13(800018, 13000),
        PINK_BEAN_EMPTY_14(800019, 13000),
        PINK_BEAN_EMPTY_15(800022, 13000);

        private final short jobId;
        private final short beginnerJobId;

        JobEnum(int jobId, int beginnerJobId) {
            this.jobId = (short) jobId;
            this.beginnerJobId = (short) beginnerJobId;
        }

        public static JobEnum getJobById(short id) {
            return Util.findWithPred(values(), j -> j.getJobId() == id);
        }

        public short getJobId() {
            return jobId;
        }

        public short getBeginnerJobId() {
            return beginnerJobId;
        }

        public Set<WeaponType> getUsingWeapons() {
            Set<WeaponType> wts = new HashSet<>();
            switch (this) {
                case TANJIRO_BEGINNER:
                case TANJIRO_KAMADO:
                    wts.add(WeaponType.TwoHandedSword);
                    break;
                case SAITAMA_BEGINNER:
                case SAITAMA:
                case YETI_BEGINNER:
                case YETI:
                    wts.add(WeaponType.Knuckle);
                    break;
                case BEGINNER:
                case WARRIOR:
                case NOBLESSE:
                case LEGEND:
                case CITIZEN:
                    wts.add(WeaponType.OneHandedSword);
                    wts.add(WeaponType.OneHandedAxe);
                    wts.add(WeaponType.OneHandedMace);
                    wts.add(WeaponType.TwoHandedSword);
                    wts.add(WeaponType.TwoHandedAxe);
                    wts.add(WeaponType.TwoHandedMace);
                    break;
                case FIGHTER:
                case CRUSADER:
                case HERO:
                    wts.add(WeaponType.OneHandedSword);
                    wts.add(WeaponType.OneHandedAxe);
                    wts.add(WeaponType.TwoHandedSword);
                    wts.add(WeaponType.TwoHandedAxe);
                    break;
                case PAGE:
                case WHITEKNIGHT:
                case PALADIN:
                    wts.add(WeaponType.OneHandedSword);
                    wts.add(WeaponType.OneHandedMace);
                    wts.add(WeaponType.TwoHandedSword);
                    wts.add(WeaponType.TwoHandedMace);
                    break;
                case SPEARMAN:
                case DRAGONKNIGHT:
                case DARKKNIGHT:
                    wts.add(WeaponType.Spear);
                    wts.add(WeaponType.Polearm);
                    break;
                case MAGICIAN:
                case FP_WIZARD:
                case FP_MAGE:
                case FP_ARCHMAGE:
                case IL_WIZARD:
                case IL_MAGE:
                case IL_ARCHMAGE:
                case CLERIC:
                case PRIEST:
                case BISHOP:
                case EVAN_NOOB:
                case EVAN1:
                case EVAN2:
                case EVAN3:
                case EVAN4:
                case BLAZEWIZARD1:
                case BLAZEWIZARD2:
                case BLAZEWIZARD3:
                case BLAZEWIZARD4:
                    wts.add(WeaponType.Wand);
                    wts.add(WeaponType.Staff);
                    break;
                case BOWMAN:
                case HUNTER:
                case RANGER:
                case BOWMASTER:
                case WINDARCHER1:
                case WINDARCHER2:
                case WINDARCHER3:
                case WINDARCHER4:
                    wts.add(WeaponType.Bow);
                    break;
                case CROSSBOWMAN:
                case SNIPER:
                case MARKSMAN:
                case WILD_HUNTER_1:
                case WILD_HUNTER_2:
                case WILD_HUNTER_3:
                case WILD_HUNTER_4:
                    wts.add(WeaponType.Crossbow);
                    break;
                case THIEF:
                    wts.add(WeaponType.Dagger);
                    wts.add(WeaponType.Claw);
                    break;
                case ASSASSIN:
                case HERMIT:
                case NIGHTLORD:
                case NIGHTWALKER1:
                case NIGHTWALKER2:
                case NIGHTWALKER3:
                case NIGHTWALKER4:
                    wts.add(WeaponType.Claw);
                    break;
                case BANDIT:
                case CHIEFBANDIT:
                case SHADOWER:
                    wts.add(WeaponType.Dagger);
                    break;
                case BLADE_RECRUIT:
                case BLADE_ACOLYTE:
                case BLADE_SPECIALIST:
                case BLADE_LORD:
                case BLADE_MASTER:
                    wts.add(WeaponType.Dagger);
                    wts.add(WeaponType.Katara);
                    break;
                case PIRATE:
                    wts.add(WeaponType.Knuckle);
                    wts.add(WeaponType.Gun);
                    break;
                case BRAWLER:
                case MARAUDER:
                case BUCCANEER:
                case SHADE:
                case SHADE1:
                case SHADE2:
                case SHADE3:
                case SHADE4:
                case THUNDERBREAKER1:
                case THUNDERBREAKER2:
                case THUNDERBREAKER3:
                case THUNDERBREAKER4:
                    wts.add(WeaponType.Knuckle);
                    break;
                case GUNSLINGER:
                case OUTLAW:
                case CORSAIR:
                case JETT1:
                case JETT2:
                case JETT3:
                case JETT4:
                case MECHANIC_1:
                case MECHANIC_2:
                case MECHANIC_3:
                case MECHANIC_4:
                    wts.add(WeaponType.Gun);
                    break;
                case PIRATE_CANNONNEER:
                case CANNONEER:
                case CANNON_BLASTER:
                case CANNON_MASTER:
                    wts.add(WeaponType.HandCannon);
                    break;
                case DAWNWARRIOR1:
                case DAWNWARRIOR2:
                case DAWNWARRIOR3:
                case DAWNWARRIOR4:
                    wts.add(WeaponType.OneHandedSword);
                    wts.add(WeaponType.TwoHandedSword);
                    break;
                case KAISER:
                case KAISER1:
                case KAISER2:
                case KAISER3:
                case KAISER4:
                    wts.add(WeaponType.TwoHandedSword);
                    break;
                case ARAN1:
                case ARAN2:
                case ARAN3:
                case ARAN4:
                    wts.add(WeaponType.Polearm);
                    break;
                case MERCEDES:
                case MERCEDES1:
                case MERCEDES2:
                case MERCEDES3:
                case MERCEDES4:
                    wts.add(WeaponType.DualBowgun);
                    break;
                case PHANTOM:
                case PHANTOM1:
                case PHANTOM2:
                case PHANTOM3:
                case PHANTOM4:
                    wts.add(WeaponType.Cane);
                    break;
                case LUMINOUS:
                case LUMINOUS1:
                case LUMINOUS2:
                case LUMINOUS3:
                case LUMINOUS4:
                    wts.add(WeaponType.ShiningRod);
                    break;
                case DEMON:
                    wts.add(WeaponType.OneHandedAxe);
                    wts.add(WeaponType.OneHandedMace);
                    wts.add(WeaponType.Desperado);
                    break;
                case DEMON_SLAYER1:
                case DEMON_SLAYER2:
                case DEMON_SLAYER3:
                case DEMON_SLAYER4:
                    wts.add(WeaponType.OneHandedAxe);
                    wts.add(WeaponType.OneHandedMace);
                    break;
                case DEMON_AVENGER1:
                case DEMON_AVENGER2:
                case DEMON_AVENGER3:
                case DEMON_AVENGER4:
                    wts.add(WeaponType.Desperado);
                    break;
                case BATTLE_MAGE_1:
                case BATTLE_MAGE_2:
                case BATTLE_MAGE_3:
                case BATTLE_MAGE_4:
                    wts.add(WeaponType.Staff);
                    break;
                case BLASTER_1:
                case BLASTER_2:
                case BLASTER_3:
                case BLASTER_4:
                    wts.add(WeaponType.ArmCannon);
                    break;
                case XENON:
                case XENON1:
                case XENON2:
                case XENON3:
                case XENON4:
                    wts.add(WeaponType.ChainSword);
                    break;
                case HAYATO:
                case HAYATO1:
                case HAYATO2:
                case HAYATO3:
                case HAYATO4:
                    wts.add(WeaponType.Katana);
                    break;
                case KANNA:
                case KANNA1:
                case KANNA2:
                case KANNA3:
                case KANNA4:
                    wts.add(WeaponType.Fan);
                    break;
                case NAMELESS_WARDEN:
                case MIHILE1:
                case MIHILE2:
                case MIHILE3:
                case MIHILE4:
                    wts.add(WeaponType.OneHandedSword);
                    break;
                case ANGELIC_BUSTER:
                case ANGELIC_BUSTER1:
                case ANGELIC_BUSTER2:
                case ANGELIC_BUSTER3:
                case ANGELIC_BUSTER4:
                    wts.add(WeaponType.SoulShooter);
                    break;
                case ZERO:
                case ZERO1:
                case ZERO2:
                case ZERO3:
                case ZERO4:
                    wts.add(WeaponType.LongSword);
                    wts.add(WeaponType.BigSword);
                    break;
                case KINESIS_0:
                case KINESIS_1:
                case KINESIS_2:
                case KINESIS_3:
                case KINESIS_4:
                    wts.add(WeaponType.PsyLimiter);
                    break;
                case ADELE:
                case ADELE_1:
                case ADELE_2:
                case ADELE_3:
                case ADELE_4:
                    wts.add(WeaponType.Bladecaster);
                    break;
                case KAIN:
                case KAIN_1:
                case KAIN_2:
                case KAIN_3:
                case KAIN_4:
                    wts.add(WeaponType.Whispershot);
                    break;
                case REN:
                case REN_1:
                case REN_2:
                case REN_3:
                case REN_4:
                    wts.add(WeaponType.RenSword);
                    break;
                case PATHFINDER_1:
                case PATHFINDER_2:
                case PATHFINDER_3:
                case PATHFINDER_4:
                    wts.add(WeaponType.AncientBow);
                    break;
                case CADENA:
                case CADENA_1:
                case CADENA_2:
                case CADENA_3:
                case CADENA_4:
                    wts.add(WeaponType.Chain);
                    break;
                case ILLIUM:
                case ILLIUM_1:
                case ILLIUM_2:
                case ILLIUM_3:
                case ILLIUM_4:
                    wts.add(WeaponType.Gauntlet);
                    break;
                case ARK:
                case ARK_1:
                case ARK_2:
                case ARK_3:
                case ARK_4:
                    wts.add(WeaponType.Knuckle);
                    break;
                case HOYOUNG:
                case HOYOUNG_1:
                case HOYOUNG_2:
                case HOYOUNG_3:
                case HOYOUNG_4:
                    wts.add(WeaponType.RitualFan);
                    break;
                case LARA:
                case LARA_1:
                case LARA_2:
                case LARA_3:
                case LARA_4:
                    wts.add(WeaponType.Wand);
                    break;
                case KHALI:
                case KHALI_1:
                case KHALI_2:
                case KHALI_3:
                case KHALI_4:
                    wts.add(WeaponType.Chakram);
                    break;
                case LYNN:
                case LYNN_1:
                case LYNN_2:
                case LYNN_3:
                case LYNN_4:
                    wts.add(WeaponType.MemoryStaff);
                    break;
                case MOXUAN:
                case MOXUAN_1:
                case MOXUAN_2:
                case MOXUAN_3:
                case MOXUAN_4:
                    wts.add(WeaponType.Fist);
                    break;
            }
            return wts;
        }
    }

    public enum LoginJob {
        RESISTANCE(0, JobFlag.DISABLED, JobEnum.CITIZEN),
        EXPLORER(1, JobFlag.ENABLED, JobEnum.BEGINNER),
        CYGNUS(2, JobFlag.ENABLED, JobEnum.NOBLESSE),
        ARAN(3, JobFlag.DISABLED, JobEnum.LEGEND),
        EVAN(4, JobFlag.DISABLED, JobEnum.EVAN_NOOB),
        MERCEDES(5, JobFlag.DISABLED, JobEnum.MERCEDES),
        DEMON(6, JobFlag.DISABLED, JobEnum.DEMON),
        PHANTOM(7, JobFlag.ENABLED, JobEnum.PHANTOM),
        DUAL_BLADE(8, JobFlag.ENABLED, JobEnum.BEGINNER),
        MIHILE(9, JobFlag.DISABLED, JobEnum.NAMELESS_WARDEN),
        LUMINOUS(10, JobFlag.DISABLED, JobEnum.LUMINOUS),
        KAISER(11, JobFlag.ENABLED, JobEnum.KAISER),
        ANGELIC(12, JobFlag.DISABLED, JobEnum.ANGELIC_BUSTER),
        CANNONER(13, JobFlag.ENABLED, JobEnum.BEGINNER),
        XENON(14, JobFlag.ENABLED, JobEnum.XENON),
        ZERO(15, JobFlag.DISABLED, JobEnum.ZERO),
        SHADE(16, JobFlag.DISABLED, JobEnum.SHADE),
        PINK_BEAN(17, JobFlag.DISABLED, JobEnum.PINK_BEAN_0),
        KINESIS(18, JobFlag.DISABLED, JobEnum.KINESIS_0),
        CADENA(19, JobFlag.DISABLED, JobEnum.CADENA),
        ILLIUM(20, JobFlag.DISABLED, JobEnum.ILLIUM),
        ARK(21, JobFlag.DISABLED, JobEnum.ARK),
        PATHFINDER(22, JobFlag.DISABLED, JobEnum.PATHFINDER_1),
        HOYOUNG(23, JobFlag.DISABLED, JobEnum.HOYOUNG),
        ADELE(24, JobFlag.ENABLED, JobEnum.ADELE),
        KAIN(25, JobFlag.DISABLED, JobEnum.KAIN),
        YETI(26, JobFlag.DISABLED, JobEnum.YETI),
        LARA(27, JobFlag.DISABLED, JobEnum.LARA),
        KHALI(28, JobFlag.DISABLED, JobEnum.KHALI),
        TANJIRO(29, JobFlag.ENABLED, JobEnum.TANJIRO_BEGINNER),
        REN(30, JobFlag.ENABLED, JobEnum.REN),
        SAITAMA(31, JobFlag.ENABLED, JobEnum.SAITAMA_BEGINNER),
        MOXUAN(1000, JobFlag.DISABLED, JobEnum.MOXUAN),
        HAYATO(1001, JobFlag.ENABLED, JobEnum.HAYATO),
        KANNA(1002, JobFlag.ENABLED, JobEnum.KANNA),
        LYNN(1003, JobFlag.ENABLED, JobEnum.LYNN),
        SIA(1005, JobFlag.DISABLED, JobEnum.SIA),
        ;

        private final int jobType, flag;
        private final JobEnum beginJob;

        LoginJob(int jobType, JobFlag flag, JobEnum beginJob) {
            this.jobType = jobType;
            this.flag = flag.getFlag();
            this.beginJob = beginJob;
        }

        public static LoginJob getLoginJobById(int id) {
            return Arrays.stream(LoginJob.values()).filter(j -> j.getJobType() == id).findFirst().orElse(null);
        }

        public int getJobType() {
            return jobType;
        }

        public int getFlag() {
            return flag;
        }

        public JobEnum getBeginJob() {
            return beginJob;
        }

        public enum JobFlag {

            DISABLED(0),
            ENABLED(1);
            private final int flag;

            JobFlag(int flag) {
                this.flag = flag;
            }

            public int getFlag() {
                return flag;
            }
        }
    }

    public static void encode(OutPacket outPacket) {
        outPacket.encodeByte(enableJobs);
        outPacket.encodeByte(jobOrder);
        for (int slot=0;slot<36;slot++) {
            LoginJob loginJobId=getTms282LoginJob(slot);
            int flag = ServerRuntimeConfig.isJobCreationEnabled(loginJobId) ? 1 : 0;
            outPacket.encodeByte(flag);
            outPacket.encodeShort(flag);
        }
    }

    /** Client 3D4BEB0/3D4BEE0 maps race1000..1003 to slots32..35. */
    public static LoginJob getTms282LoginJob(int slot) {
        if(slot<0 || slot>=36)throw new IllegalArgumentException("TMS 282 creation slot must be 0..35");
        return Objects.requireNonNull(LoginJob.getLoginJobById(slot<32?slot:slot+968), "Missing TMS creation race");
    }

    // Explorer character creation maps:
    public static final int BEGINNER_CREATION_MAP_1 = 103050900; // Victoria Road - Starting Place
    public static final int BEGINNER_CREATION_MAP_2 = 3000000;//3000600; // Coco Island - Towards Maple Island
    public static final int BEGINNER_CREATION_MAP_3 = 4000011; // Maple Road - Maple Tree Hill
    // Cygnus character creation maps:
    public static final int NOBLESSE_CREATION_MAP = 130030000; // All cygnus jobs except Mihile
    public static final int MIHILE_CREATION_MAP = 913070000;
    // Legend character creation maps:
    public static final int ARAN_CREATION_MAP = 914000000;
    public static final int EVAN_CREATION_MAP = 900010000;
    public static final int LUMINOUS_CREATION_MAP = 927020080;
    public static final int MERCEDES_CREATION_MAP = 910150000;
    public static final int PHANTOM_CREATION_MAP = 915000000;
    public static final int SHADE_CREATION_MAP = 927030050;
    // Nova character creation maps:
    public static final int ANGELIC_BUSTER_CREATION_MAP = 940011000;
    public static final int KAISER_CREATION_MAP = 940001000;
    // Resistance character creation maps:
    public static final int CITIZEN_CREATION_MAP = 931000000;
    public static final int DEMON_CREATION_MAP = 927000000;
    public static final int XENON_CREATION_MAP = 931050900;
    // Sengoku character creation maps:
    public static final int HAYATO_CREATION_MAP = 807100000;
    public static final int KANNA_CREATION_MAP = 807100100;
    // Special character creation maps:
    public static final int KINESIS_CREATION_MAP = 331001110;

    //LOCKED
    public static final int ZERO_CREATION_MAP = 321000000;
    public static final int BREAST_TAMER_CREATION_MAP = 866101000;

    public static boolean isXenon(short id) {
        return id == JobEnum.XENON1.getBeginnerJobId()
                || id == JobEnum.XENON1.getJobId()
                || id == JobEnum.XENON2.getJobId()
                || id == JobEnum.XENON3.getJobId()
                || id == JobEnum.XENON4.getJobId();
    }

    public static boolean isBeastTamer(short job) {
        return job / 1000 == 11;
    }

    public static boolean isPinkBean(short job) {
        return job == JobEnum.PINK_BEAN_0.getJobId() || job == JobEnum.PINK_BEAN_1.getJobId();
    }

    public static boolean isYeti(short job) {
        return job == JobEnum.YETI_BEGINNER.getJobId() || job == JobEnum.YETI.getJobId();
    }

    public static boolean isTanjiro(short job) {
        return job == JobEnum.TANJIRO_BEGINNER.getJobId() || job == JobEnum.TANJIRO_KAMADO.getJobId();
    }

    public static boolean isSaitama(short job) {
        return job == JobEnum.SAITAMA_BEGINNER.getJobId() || job == JobEnum.SAITAMA.getJobId();
    }

    public static boolean isSpecialEventJob(short job) {
        return isTanjiro(job) || isSaitama(job) || isPinkBean(job) || isYeti(job);
    }

    public static short getSpecialEventMainJob(short job) {
        if (isTanjiro(job)) return JobEnum.TANJIRO_KAMADO.getJobId();
        if (isSaitama(job)) return JobEnum.SAITAMA.getJobId();
        if (isPinkBean(job)) return JobEnum.PINK_BEAN_1.getJobId();
        if (isYeti(job)) return JobEnum.YETI.getJobId();
        throw new IllegalArgumentException("Not a special event job: " + job);
    }

    public static JobEnum getJobEnumById(short jobId) {
        return Arrays.stream(JobEnum.values()).filter(job -> job.getJobId() == jobId).findFirst().orElse(null);
    }

    public static boolean isWildHunter(short id) {
        return id == JobEnum.WILD_HUNTER_1.getJobId()
                || id == JobEnum.WILD_HUNTER_2.getJobId()
                || id == JobEnum.WILD_HUNTER_3.getJobId()
                || id == JobEnum.WILD_HUNTER_4.getJobId();
    }

    public static boolean isAngelicBuster(int id) {
        return id == JobEnum.ANGELIC_BUSTER.getJobId()
                || id == JobEnum.ANGELIC_BUSTER1.getJobId()
                || id == JobEnum.ANGELIC_BUSTER2.getJobId()
                || id == JobEnum.ANGELIC_BUSTER3.getJobId()
                || id == JobEnum.ANGELIC_BUSTER4.getJobId();
    }

    public static boolean isDawnWarrior(short id) {
        return id == JobEnum.DAWNWARRIOR1.getJobId()
                || id == JobEnum.DAWNWARRIOR2.getJobId()
                || id == JobEnum.DAWNWARRIOR3.getJobId()
                || id == JobEnum.DAWNWARRIOR4.getJobId();
    }

    public static boolean isBlazeWizard(short id) {
        return id == JobEnum.BLAZEWIZARD1.getJobId()
                || id == JobEnum.BLAZEWIZARD2.getJobId()
                || id == JobEnum.BLAZEWIZARD3.getJobId()
                || id == JobEnum.BLAZEWIZARD4.getJobId();
    }

    public static boolean isWindArcher(short id) {
        return id == JobEnum.WINDARCHER1.getJobId()
                || id == JobEnum.WINDARCHER2.getJobId()
                || id == JobEnum.WINDARCHER3.getJobId()
                || id == JobEnum.WINDARCHER4.getJobId();
    }

    public static boolean isNightWalker(short id) {
        return id == JobEnum.NIGHTWALKER1.getJobId()
                || id == JobEnum.NIGHTWALKER2.getJobId()
                || id == JobEnum.NIGHTWALKER3.getJobId()
                || id == JobEnum.NIGHTWALKER4.getJobId();
    }

    public static boolean isThunderBreaker(short id) {
        return id == JobEnum.THUNDERBREAKER1.getJobId()
                || id == JobEnum.THUNDERBREAKER2.getJobId()
                || id == JobEnum.THUNDERBREAKER3.getJobId()
                || id == JobEnum.THUNDERBREAKER4.getJobId();
    }

    public static boolean isMihile(short id) {
        return id == JobEnum.NAMELESS_WARDEN.getJobId()
                || id == JobEnum.MIHILE1.getJobId()
                || id == JobEnum.MIHILE2.getJobId()
                || id == JobEnum.MIHILE3.getJobId()
                || id == JobEnum.MIHILE4.getJobId();
    }

    public static boolean isHero(short id) {
        return id == JobEnum.FIGHTER.getJobId()
                || id == JobEnum.CRUSADER.getJobId()
                || id == JobEnum.HERO.getJobId();
    }

    public static boolean isPaladin(short id) {
        return id == JobEnum.PAGE.getJobId()
                || id == JobEnum.WHITEKNIGHT.getJobId()
                || id == JobEnum.PALADIN.getJobId();
    }

    public static boolean isDarkKnight(short id) {
        return id == JobEnum.SPEARMAN.getJobId()
                || id == JobEnum.DRAGONKNIGHT.getJobId()
                || id == JobEnum.DARKKNIGHT.getJobId();
    }

    public static boolean isFirePoison(short id) {
        return id == JobEnum.FP_WIZARD.getJobId()
                || id == JobEnum.FP_MAGE.getJobId()
                || id == JobEnum.FP_ARCHMAGE.getJobId();
    }

    public static boolean isIceLightning(short id) {
        return id == JobEnum.IL_WIZARD.getJobId()
                || id == JobEnum.IL_MAGE.getJobId()
                || id == JobEnum.IL_ARCHMAGE.getJobId();
    }

    public static boolean isBishop(short id) {
        return id == JobEnum.CLERIC.getJobId()
                || id == JobEnum.PRIEST.getJobId()
                || id == JobEnum.BISHOP.getJobId();
    }

    public static boolean isBowMaster(short id) {
        return id == JobEnum.HUNTER.getJobId()
                || id == JobEnum.RANGER.getJobId()
                || id == JobEnum.BOWMASTER.getJobId();
    }

    public static boolean isMarksman(short id) {
        return id == JobEnum.CROSSBOWMAN.getJobId()
                || id == JobEnum.SNIPER.getJobId()
                || id == JobEnum.MARKSMAN.getJobId();
    }

    public static boolean isShadower(short id) {
        return id == JobEnum.BANDIT.getJobId()
                || id == JobEnum.CHIEFBANDIT.getJobId()
                || id == JobEnum.SHADOWER.getJobId();
    }

    public static boolean isNightLord(short id) {
        return id == JobEnum.ASSASSIN.getJobId()
                || id == JobEnum.HERMIT.getJobId()
                || id == JobEnum.NIGHTLORD.getJobId();
    }

    public static boolean isDualBlade(short id) {
        return id == JobEnum.BLADE_RECRUIT.getJobId()
                || id == JobEnum.BLADE_ACOLYTE.getJobId()
                || id == JobEnum.BLADE_SPECIALIST.getJobId()
                || id == JobEnum.BLADE_LORD.getJobId()
                || id == JobEnum.BLADE_MASTER.getJobId();
    }

    public static boolean isBuccaneer(short id) {
        return id == JobEnum.BRAWLER.getJobId()
                || id == JobEnum.MARAUDER.getJobId()
                || id == JobEnum.BUCCANEER.getJobId();
    }

    public static boolean isCorsair(short id) {
        return id == JobEnum.GUNSLINGER.getJobId()
                || id == JobEnum.OUTLAW.getJobId()
                || id == JobEnum.CORSAIR.getJobId();
    }

    public static boolean isCannoneer(short id) {
        return id == JobEnum.PIRATE_CANNONNEER.getJobId()
                || id == JobEnum.CANNONEER.getJobId()
                || id == JobEnum.CANNON_BLASTER.getJobId()
                || id == JobEnum.CANNON_MASTER.getJobId();
    }

    public static boolean isJett(short id) {
        return id == JobEnum.JETT1.getJobId()
                || id == JobEnum.JETT2.getJobId()
                || id == JobEnum.JETT3.getJobId()
                || id == JobEnum.JETT4.getJobId();
    }

    public static boolean isDemon(short id) {
        return isDemonSlayer(id) || isDemonAvenger(id);
    }

    public static boolean isDemonSlayer(short id) {
        return id == JobEnum.DEMON_SLAYER1.getBeginnerJobId()
                || id == JobEnum.DEMON_SLAYER1.getJobId()
                || id == JobEnum.DEMON_SLAYER2.getJobId()
                || id == JobEnum.DEMON_SLAYER3.getJobId()
                || id == JobEnum.DEMON_SLAYER4.getJobId();
    }

    public static boolean isDemonAvenger(short id) {
        return id == JobEnum.DEMON_AVENGER1.getBeginnerJobId()
                || id == JobEnum.DEMON_AVENGER1.getJobId()
                || id == JobEnum.DEMON_AVENGER2.getJobId()
                || id == JobEnum.DEMON_AVENGER3.getJobId()
                || id == JobEnum.DEMON_AVENGER4.getJobId();
    }

    public static boolean isKanna(short id) {
        return id == JobEnum.KANNA.getJobId()
                || id == JobEnum.KANNA1.getJobId()
                || id == JobEnum.KANNA2.getJobId()
                || id == JobEnum.KANNA3.getJobId()
                || id == JobEnum.KANNA4.getJobId();
    }

    public static boolean isHayato(short id) {
        return id == JobEnum.HAYATO.getJobId()
                || id == JobEnum.HAYATO1.getJobId()
                || id == JobEnum.HAYATO2.getJobId()
                || id == JobEnum.HAYATO3.getJobId()
                || id == JobEnum.HAYATO4.getJobId();
    }

    public static boolean isBlaster(short id) {
        return id == JobEnum.BLASTER_1.getJobId()
                || id == JobEnum.BLASTER_2.getJobId()
                || id == JobEnum.BLASTER_3.getJobId()
                || id == JobEnum.BLASTER_4.getJobId();
    }

    public static boolean isShade(short id) {
        return id == JobEnum.SHADE.getJobId()
                || id == JobEnum.SHADE1.getJobId()
                || id == JobEnum.SHADE2.getJobId()
                || id == JobEnum.SHADE3.getJobId()
                || id == JobEnum.SHADE4.getJobId();
    }

    public static boolean isLegend(short id) {
        return id == JobConstants.JobEnum.LEGEND.getJobId();
    }

    public static boolean isNoblesse(short id) {
        return id == JobConstants.JobEnum.NOBLESSE.getJobId();
    }

    public static double getDamageConstant(short job) {
        // get_job_damage_const 
        if (job > 222) {
            if (job > 1200) {
                if (job >= 1210 && job <= 1212)
                    return 0.2;
            } else if (job == 1200 || job >= 230 && job <= 232) {
                return 0.2;
            }
            return 0.0;
        }
        if (job < 220) {
            switch (job) {
                case 110:
                case 111:
                case 112:
                    return 0.1;
                case 200:
                case 210:
                case 211:
                case 212:
                    return 0.2;
                default:
                    return 0.0;
            }
        }
        return 0.2;
    }

    public static int getJobCategory(short job) {
        if (isTanjiro(job) || isPinkBean(job)) return 1;
        if (isSaitama(job) || isYeti(job)) return 5;
        int res = 0;
        switch (job / 100) {
            case 27:
            case 140:
            case 142:
                res = 2;
                break;
            case 36:
                res = 4;
                break;
            case 37:
                res = 1;
                break;
            default:
                res = job % 1000 / 100;
        }
        return res;
    }

    public static byte getJobLevelByZeroSkillID(int skillID) {
        int prefix = (skillID % 1000) / 100;
        return (byte) (prefix == 1 ? 2 : prefix == 2 ? 1 : 3);
    }

    public static boolean isMechanic(short id) {
        return id == JobEnum.MECHANIC_1.getJobId()
                || id == JobEnum.MECHANIC_2.getJobId()
                || id == JobEnum.MECHANIC_3.getJobId()
                || id == JobEnum.MECHANIC_4.getJobId();
    }

    public static boolean isBattleMage(short id) {
        return id == JobEnum.BATTLE_MAGE_1.getJobId()
                || id == JobEnum.BATTLE_MAGE_2.getJobId()
                || id == JobEnum.BATTLE_MAGE_3.getJobId()
                || id == JobEnum.BATTLE_MAGE_4.getJobId();
    }

    public static boolean isGmJob(short id) {
        return isGm(id) || isSuperGm(id);
    }

    public static boolean isGm(short id) {
        return id == JobEnum.GM.getJobId();
    }

    public static boolean isSuperGm(short id) {
        return id == JobEnum.SUPERGM.getJobId();
    }

    public static boolean isManager(short id) {
        return id == JobEnum.MANAGER.getJobId();
    }

    public static boolean isExplorer(short id) {
        return isAdventurerWarrior(id)
                || isAdventurerMage(id)
                || isAdventurerArcher(id)
                || isAdventurerThief(id)
                || isAdventurerPirate(id);
    }

    public static boolean isAdventurerWarrior(short id) {
        return id == JobEnum.WARRIOR.getJobId() || isHero(id) || isPaladin(id) || isDarkKnight(id);
    }

    public static boolean isAdventurerMage(short id) {
        return id == JobEnum.MAGICIAN.getJobId() || isFirePoison(id) || isIceLightning(id) || isBishop(id);
    }

    public static boolean isAdventurerArcher(short id) {
        return id == JobEnum.BOWMAN.getJobId() || isBowMaster(id) || isMarksman(id) || isPathFinder(id);
    }

    public static boolean isAdventurerThief(short id) {
        return id == JobEnum.THIEF.getJobId() || isNightLord(id) || isShadower(id) || isDualBlade(id);
    }

    public static boolean isAdventurerPirate(short id) {
        return id == JobEnum.PIRATE.getJobId() || isBuccaneer(id) || isCorsair(id) || isCannoneer(id) || isJett(id);
    }

    public static boolean isCygnusKnight(short id) {
        return isNoblesse(id) || isDawnWarrior(id) || isBlazeWizard(id) || isWindArcher(id) || isNightWalker(id) || isThunderBreaker(id);
    }

    public static boolean isResistance(short id) {
        return isCitizen(id) || isBattleMage(id) || isWildHunter(id) || isMechanic(id) || isXenon(id) || isBlaster(id) || isDemon(id);
    }

    public static boolean isSengoku(short id) {
        return isHayato(id) || isKanna(id);
    }

    public static boolean isEvan(short id) {
        return id == JobEnum.EVAN_NOOB.getJobId()
                || id == JobEnum.EVAN1.getJobId()
                || id == JobEnum.EVAN2.getJobId()
                || id == JobEnum.EVAN3.getJobId()
                || id == JobEnum.EVAN4.getJobId();
    }

    public static boolean isMercedes(short id) {
        return id == JobEnum.MERCEDES1.getBeginnerJobId()
                || id == JobEnum.MERCEDES1.getJobId()
                || id == JobEnum.MERCEDES2.getJobId()
                || id == JobEnum.MERCEDES3.getJobId()
                || id == JobEnum.MERCEDES4.getJobId();
    }

    public static boolean isPhantom(short id) {
        return id == JobEnum.PHANTOM1.getBeginnerJobId()
                || id == JobEnum.PHANTOM1.getJobId()
                || id == JobEnum.PHANTOM2.getJobId()
                || id == JobEnum.PHANTOM3.getJobId()
                || id == JobEnum.PHANTOM4.getJobId();
    }

    public static boolean isLuminous(short id) {
        return id == JobEnum.LUMINOUS.getJobId()
                || id == JobEnum.LUMINOUS1.getJobId()
                || id == JobEnum.LUMINOUS2.getJobId()
                || id == JobEnum.LUMINOUS3.getJobId()
                || id == JobEnum.LUMINOUS4.getJobId();
    }

    public static boolean isKaiser(short id) {
        return id == JobEnum.KAISER.getJobId()
                || id == JobEnum.KAISER1.getJobId()
                || id == JobEnum.KAISER2.getJobId()
                || id == JobEnum.KAISER3.getJobId()
                || id == JobEnum.KAISER4.getJobId();
    }

    public static boolean isZero(short id) {
        return id == JobEnum.ZERO.getJobId()
                || id == JobEnum.ZERO1.getJobId()
                || id == JobEnum.ZERO2.getJobId()
                || id == JobEnum.ZERO3.getJobId()
                || id == JobEnum.ZERO4.getJobId();
    }

    public static boolean isArk(short id) {
        return id == JobEnum.ARK.getJobId()
                || id == JobEnum.ARK_1.getJobId()
                || id == JobEnum.ARK_2.getJobId()
                || id == JobEnum.ARK_3.getJobId()
                || id == JobEnum.ARK_4.getJobId();
    }

    public static boolean isCadena(short id) {
        return id == JobEnum.CADENA.getJobId()
                || id == JobEnum.CADENA_1.getJobId()
                || id == JobEnum.CADENA_2.getJobId()
                || id == JobEnum.CADENA_3.getJobId()
                || id == JobEnum.CADENA_4.getJobId();
    }

    public static boolean isIllium(short id) {
        return id == JobEnum.ILLIUM.getJobId()
                || id == JobEnum.ILLIUM_1.getJobId()
                || id == JobEnum.ILLIUM_2.getJobId()
                || id == JobEnum.ILLIUM_3.getJobId()
                || id == JobEnum.ILLIUM_4.getJobId();
    }

    public static boolean isHoYoung(short jobId) {
        return jobId == JobEnum.HOYOUNG.getJobId()
                || jobId == JobEnum.HOYOUNG_1.getJobId()
                || jobId == JobEnum.HOYOUNG_2.getJobId()
                || jobId == JobEnum.HOYOUNG_3.getJobId()
                || jobId == JobEnum.HOYOUNG_4.getJobId();
    }

    public static boolean isAdele(short jobId) {
        return jobId == JobEnum.ADELE.getJobId()
                || jobId == JobEnum.ADELE_1.getJobId()
                || jobId == JobEnum.ADELE_2.getJobId()
                || jobId == JobEnum.ADELE_3.getJobId()
                || jobId == JobEnum.ADELE_4.getJobId();
    }

    public static boolean isPathFinder(short id) {
        return id == JobEnum.PATHFINDER_1.getJobId()
                || id == JobEnum.PATHFINDER_2.getJobId()
                || id == JobEnum.PATHFINDER_3.getJobId()
                || id == JobEnum.PATHFINDER_4.getJobId();
    }

    public static boolean isKain(short jobId) {
        return jobId == JobEnum.KAIN.getJobId()
                || jobId == JobEnum.KAIN_1.getJobId()
                || jobId == JobEnum.KAIN_2.getJobId()
                || jobId == JobEnum.KAIN_3.getJobId()
                || jobId == JobEnum.KAIN_4.getJobId();
    }

    public static boolean isKhali(short jobId) {
        return jobId == JobEnum.KHALI.getJobId()
                || jobId == JobEnum.KHALI_1.getJobId()
                || jobId == JobEnum.KHALI_2.getJobId()
                || jobId == JobEnum.KHALI_3.getJobId()
                || jobId == JobEnum.KHALI_4.getJobId();
    }

    public static boolean isLara(short jobId) {
        return jobId == JobEnum.LARA.getJobId()
                || jobId == JobEnum.LARA_1.getJobId()
                || jobId == JobEnum.LARA_2.getJobId()
                || jobId == JobEnum.LARA_3.getJobId()
                || jobId == JobEnum.LARA_4.getJobId();
    }

    public static boolean isLynn(short jobId) {
        return jobId == JobEnum.LYNN.getJobId()
                || jobId == JobEnum.LYNN_1.getJobId()
                || jobId == JobEnum.LYNN_2.getJobId()
                || jobId == JobEnum.LYNN_3.getJobId()
                || jobId == JobEnum.LYNN_4.getJobId();
    }

    public static boolean isSiaAstelle(short jobId) {
        return jobId == JobEnum.SIA.getJobId()
                || jobId == JobEnum.SIA_1.getJobId()
                || jobId == JobEnum.SIA_2.getJobId()
                || jobId == JobEnum.SIA_3.getJobId()
                || jobId == JobEnum.SIA_4.getJobId();
    }

    public static boolean isMoXuan(short jobId) {
        return jobId == JobEnum.MOXUAN.getJobId()
                || jobId == JobEnum.MOXUAN_1.getJobId()
                || jobId == JobEnum.MOXUAN_2.getJobId()
                || jobId == JobEnum.MOXUAN_3.getJobId()
                || jobId == JobEnum.MOXUAN_4.getJobId();
    }

    public static boolean isRen(short jobId) {
        return jobId == JobEnum.REN.getJobId()
                || jobId == JobEnum.REN_1.getJobId()
                || jobId == JobEnum.REN_2.getJobId()
                || jobId == JobEnum.REN_3.getJobId()
                || jobId == JobEnum.REN_4.getJobId();
    }

    public static boolean isFlora(short id) {
        return isAdele(id) || isIllium(id) || isArk(id) || isKhali(id);
    }

    public static boolean isHeroes(short id) {
        return isAran(id) || isEvan(id) || isMercedes(id) || isPhantom(id) || isLuminous(id) || isShade(id);
    }

    public static boolean isNova(short id) {
        return isKaiser(id) || isAngelicBuster(id) || isCadena(id) || isKain(id);
    }

    public static boolean isJianghu(short id) {
        return isLynn(id) || isMoXuan(id);
    }

    public static boolean isCitizen(short id) {
        return id == JobConstants.JobEnum.CITIZEN.getJobId();
    }

    public static boolean isAran(short jobId) {
        return jobId == JobEnum.ARAN1.getJobId()
                || jobId == JobEnum.ARAN2.getJobId()
                || jobId == JobEnum.ARAN3.getJobId()
                || jobId == JobEnum.ARAN4.getJobId();
    }

    public static boolean isKinesis(short jobId) {
        return jobId == JobEnum.KINESIS_0.getJobId()
                || jobId == JobEnum.KINESIS_1.getJobId()
                || jobId == JobEnum.KINESIS_2.getJobId()
                || jobId == JobEnum.KINESIS_3.getJobId()
                || jobId == JobEnum.KINESIS_4.getJobId();
    }

    public static boolean isExtendSpJob(short jobId) {
        // TMS 282 native08AD930; broad exclusions misencode event/removed jobs.
        for(int[] range:TMS_282_EXTENDED_SP_RANGES) {
            if(jobId<range[0])return false;
            if(jobId<=range[1])return true;
        }
        return false;
    }

    private static final int[][] TMS_282_EXTENDED_SP_RANGES={
            {0,0},{100,100},{110,112},{120,122},{130,132},{200,200},{210,212},{220,222},{230,232},
            {300,301},{310,312},{320,322},{330,332},{400,400},{410,412},{420,422},{430,439},
            {500,501},{510,512},{520,522},{530,539},{1000,2005},{2100,2599},{2700,2799},{3000,6999},
            {10000,10000},{10100,10199},{14000,14000},{14200,14200},{14210,14212},{15000,15003},
            {15100,15299},{15400,15599},{16000,16002},{16100,16299},{16400,16499},
            {17000,17001},{17200,17299},{17500,17599}
    };

    public static boolean isBeginnerJob(short jobId) {
        switch (jobId) {
            case 12005:
            case 12006:
            case 13001:
            case 8001:
            case 13000:
            case 14000:
            case 15000:
            case 15001:
            case 15002:
            case 15003:
            case 16000:
            case 16001:
            case 17001:
            case 17000:
            case 18000:
            case 6000:
            case 6001:
            case 6002:
            case 5000:
            case 4001:
            case 4002:
            case 3001:
            case 3002:
            case 2001:
            case 2002:
            case 2003:
            case 2004:
            case 2005:
                return true;
            default:
                return jobId % 1000 == 0 || jobId / 100 == 8000 || jobId / 100 == 8001;
        }
    }

    public static int getJobLevel(short jobId) {
        int prefix;
        if (isBeginnerJob(jobId) || (jobId % 100 == 0) || jobId == 301 || jobId == 501 || jobId == 508 || jobId == 3101) {
            return 1;
        }
        if (isEvan(jobId)) {
            return getEvanJobLevel(jobId);
        }
        if (isDualBlade(jobId)) {
            prefix = (jobId % 10) + 2;
            if (prefix < 2) {
                return 0;
            } else if (prefix <= 6) {
                return jobId % 10 + 2;
            }
        } else {
            //Jett needs its own reference since it doesnt follow the usual job id curve
            if (jobId == 570) {
                return 2;
            }
            if (jobId == 571) {
                return 3;
            }
            if (jobId == 572) {
                return 4;
            }
            prefix = jobId % 10;
        }
        return prefix <= 2 ? prefix + 2 : 0;
    }

    public static int getJobLevelByCharLevel(short job, int charLevel, int subJob) {
        if (JobConstants.isDualBlade(job) || (subJob == 1 && job == 400)) {
            if (charLevel <= 10) {
                return 0;
            } else if (charLevel <= 20) {
                return 1;
            } else if (charLevel <= 30) {
                return 2;
            } else if (charLevel <= 45) {
                return 3;
            } else if (charLevel <= 60) {
                return 4;
            } else if (charLevel <= 100) {
                return 5;
            } else {
                return 6;
            }
        }
        if (charLevel <= 10) {
            return 0;
        } else if (charLevel <= 30) {
            return 1;
        } else if (charLevel <= 60) {
            return 2;
        } else if (charLevel <= 100) {
            return 3;
        } else {
            return 4;
        }
    }

    private static int getEvanJobLevel(short jobId) {
        int result;
        switch (jobId) {
            case 2200:
            case 2210:
                result = 1;
                break;
            case 2211:
            case 2212:
            case 2213:
                result = 2;
                break;
            case 2214:
            case 2215:
            case 2216:
                result = 3;
                break;
            case 2217:
            case 2218:
                result = 4;
                break;
            default:
                result = 0;
                break;
        }
        return result;
    }

    public static boolean isNoManaJob(short job) {
        return isDemon(job) || isAngelicBuster(job) && !kinesisHasMp() || isZero(job) || isKinesis(job) && !kinesisHasMp() || isKanna(job) && !kinesisHasMp();
    }

    /**
     * TMS 282: Kinesis has a real MP bar next to its PP gauge (live character 37: MP 49..229 while PP caps at 30) and 17
     * of its 41 visible 282 skills carry mpCon (e.g. 142121001 110+3x). 265 folded Kinesis MP into PP (max MP = max PP),
     * which made MP potions and skill MP costs no-ops. PP stays in the Kinesis job handler.
     * Kanna too (2026-10-06): its 282 skills cost mpCon (hyper 42121052: 300 MP) and 40020000 raises Max MP 15%;
     * 265 kept Kanna's max MP at a fixed 100.
     * Angelic Buster too (2026-10-07): her 282 skills cost mpCon (65001100: 3+d(x/6)) and the 282 client refuses them
     * with "MP 不足" while the server kept her max MP at 0 (live character 71).
     */
    private static boolean kinesisHasMp() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    public static boolean isWarriorEquipJob(short id) {
        return isAdventurerWarrior(id) || isTanjiro(id) || isPinkBean(id) || isDawnWarrior(id) || isMihile(id) ||
                isAran(id) || isKaiser(id) || isBlaster(id) || isDemon(id) || isHayato(id) ||
                isZero(id) || isArk(id) || isAdele(id) || isRen(id);
    }

    public static boolean isMageEquipJob(short id) {
        return isBeastTamer(id) || isKinesis(id) || isAdventurerMage(id) || isBlazeWizard(id) ||
                isEvan(id) || isLuminous(id) || isBattleMage(id) || isKanna(id) || isIllium(id)
                || isLara(id) || isLynn(id) || isSiaAstelle(id);
    }

    public static boolean isArcherEquipJob(short id) {
        return isAdventurerArcher(id) || isWindArcher(id) || isMercedes(id)
                || isWildHunter(id) || isPathFinder(id);
    }

    public static boolean isThiefEquipJob(short id) {
        return isAdventurerThief(id) || isNightWalker(id) || isPhantom(id)
                || isXenon(id) || isCadena(id) || isHoYoung(id) || isKhali(id);
    }

    public static boolean isPirateEquipJob(short id) {
        return isAdventurerPirate(id) || isThunderBreaker(id) || isShade(id) || isAngelicBuster(id) ||
                isXenon(id) || isMechanic(id) || isJett(id) || isArk(id) || isMoXuan(id) || isYeti(id) || isSaitama(id);
    }

    public static boolean canJobAdvance(short jobId) {
        if (isSpecialEventJob(jobId)) return isBeginnerJob(jobId);
        int last = Math.abs(jobId) % 10;
        return last <= 2;
    }

    public static short nextJob(short jobId) {
        if (isSpecialEventJob(jobId)) return getSpecialEventMainJob(jobId);
        return (short) (jobId + 1);
    }

    private static final Set<Short> MULTI_PATH_BASE_JOBS = Set.of(
            (short) 0, (short) 100, (short) 200, (short) 300, (short) 400, (short) 500,
            (short) 1000, (short) 2000, (short) 2001, (short) 2002, (short) 2003, (short) 2004, (short) 2005,
            (short) 3000, (short) 3001, (short) 3002,
            (short) 5000, (short) 6000, (short) 6001, (short) 6002,
            (short) 13000, (short) 14000, (short) 15000, (short) 15001, (short) 15002, (short) 15003,
            (short) 16000, (short) 16001, (short) 17000, (short) 17001, (short) 18000,
            (short) 10000, (short) 11000
    );

    public static boolean isReadyForFifthJob(short jobId, int subJob) {
        if (MULTI_PATH_BASE_JOBS.contains(jobId)) {
            return false;
        }
        if (subJob == 1 && jobId == JobEnum.THIEF.getJobId()) {
            return false;
        }
        if (isDualBlade(jobId)) {
            return jobId == JobEnum.BLADE_MASTER.getJobId();
        }
        if (isEvan(jobId)) {
            return jobId >= JobEnum.EVAN4.getJobId();
        }
        if (isJett(jobId)) {
            return jobId >= JobEnum.JETT4.getJobId();
        }
        if (isPathFinder(jobId)) {
            return jobId >= JobEnum.PATHFINDER_4.getJobId();
        }
        if (isCannoneer(jobId)) {
            return jobId >= JobEnum.CANNON_MASTER.getJobId();
        }
        if (isDemonAvenger(jobId)) {
            return jobId >= JobEnum.DEMON_AVENGER4.getJobId();
        }
        if (isKhali(jobId)) {
            return jobId >= JobEnum.KHALI_4.getJobId();
        }
        if (isRen(jobId)) {
            return jobId >= JobEnum.REN_4.getJobId();
        }
        if (isLynn(jobId)) {
            return jobId >= JobEnum.LYNN_4.getJobId();
        }
        if (isMoXuan(jobId)) {
            return jobId >= JobEnum.MOXUAN_4.getJobId();
        }
        if (isSiaAstelle(jobId)) {
            return jobId >= JobEnum.SIA_4.getJobId();
        }
        if (isAdele(jobId)) {
            return jobId >= JobEnum.ADELE_4.getJobId();
        }
        if (isArk(jobId)) {
            return jobId >= JobEnum.ARK_4.getJobId();
        }
        if (isIllium(jobId)) {
            return jobId >= JobEnum.ILLIUM_4.getJobId();
        }
        if (isHoYoung(jobId)) {
            return jobId >= JobEnum.HOYOUNG_4.getJobId();
        }
        if (isLara(jobId)) {
            return jobId >= JobEnum.LARA_4.getJobId();
        }
        if (isKain(jobId)) {
            return jobId >= JobEnum.KAIN_4.getJobId();
        }
        if (isKinesis(jobId)) {
            return jobId >= JobEnum.KINESIS_4.getJobId();
        }
        if (isZero(jobId)) {
            return jobId >= JobEnum.ZERO4.getJobId();
        }
        if (isBeginnerJob(jobId) || jobId % 100 == 0 || jobId == 301 || jobId == 501 || jobId == 508 || jobId == 3101) {
            return false;
        }
        return getJobLevel(jobId) >= 4;
    }

    public static Short getNextJobForAdvancement(short jobId, int subJob) {
        if (isSpecialEventJob(jobId)) return isBeginnerJob(jobId) ? getSpecialEventMainJob(jobId) : null;
        if (MULTI_PATH_BASE_JOBS.contains(jobId)) {
            return null;
        }
        if (subJob == 1 && jobId == JobEnum.THIEF.getJobId()) {
            return JobEnum.BLADE_RECRUIT.getJobId();
        }
        if (isDualBlade(jobId)) {
            if (jobId < JobEnum.BLADE_MASTER.getJobId()) {
                return (short) (jobId + 1);
            }
            return null;
        }
        if (isEvan(jobId)) {
            return switch (jobId) {
                case 2200, 2210 -> JobEnum.EVAN2.getJobId();
                case 2211, 2212 -> JobEnum.EVAN3.getJobId();
                case 2213, 2214 -> (short) 2216;
                case 2216 -> JobEnum.EVAN4.getJobId();
                default -> null;
            };
        }
        if (jobId == JobEnum.JETT1.getJobId()) {
            return JobEnum.JETT2.getJobId();
        }
        if (jobId == JobEnum.JETT2.getJobId()) {
            return JobEnum.JETT3.getJobId();
        }
        if (jobId == JobEnum.JETT3.getJobId()) {
            return JobEnum.JETT4.getJobId();
        }
        if (jobId == JobEnum.PATHFINDER_1.getJobId()) {
            return JobEnum.PATHFINDER_2.getJobId();
        }
        if (jobId == JobEnum.PATHFINDER_2.getJobId()) {
            return JobEnum.PATHFINDER_3.getJobId();
        }
        if (jobId == JobEnum.PATHFINDER_3.getJobId()) {
            return JobEnum.PATHFINDER_4.getJobId();
        }
        if (jobId == JobEnum.PIRATE_CANNONNEER.getJobId()) {
            return JobEnum.CANNONEER.getJobId();
        }
        if (jobId == JobEnum.DEMON_AVENGER1.getJobId()) {
            return JobEnum.DEMON_AVENGER2.getJobId();
        }
        if (jobId == JobEnum.KAIN.getJobId()) {
            return JobEnum.KAIN_1.getJobId();
        }
        // 新職業及標準職業鏈：一轉(xxx0) -> +10，其餘按 jobLevel 遞增
        if (jobId % 100 == 0 && !isBeginnerJob(jobId)) {
            return (short) (jobId + 10);
        }
        int jobLevel = getJobLevel(jobId);
        if (jobLevel <= 0 || jobLevel >= 4) {
            return null;
        }
        return (short) (jobId - jobLevel + (jobLevel + 1));
    }
}
