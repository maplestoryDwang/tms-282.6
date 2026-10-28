package net.swordie.ms.client.jobs;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.jobs.Jianghu.*;
import net.swordie.ms.client.jobs.adventurer.*;
import net.swordie.ms.client.jobs.adventurer.archer.Archer;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.archer.Marksman;
import net.swordie.ms.client.jobs.adventurer.archer.Pathfinder;
import net.swordie.ms.client.jobs.adventurer.magician.Bishop;
import net.swordie.ms.client.jobs.adventurer.magician.FirePoison;
import net.swordie.ms.client.jobs.adventurer.magician.IceLightning;
import net.swordie.ms.client.jobs.adventurer.magician.Magician;
import net.swordie.ms.client.jobs.adventurer.pirate.Buccaneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Cannoneer;
import net.swordie.ms.client.jobs.adventurer.pirate.Corsair;
import net.swordie.ms.client.jobs.adventurer.pirate.Pirate;
import net.swordie.ms.client.jobs.adventurer.thief.DualBlade;
import net.swordie.ms.client.jobs.adventurer.thief.NightLord;
import net.swordie.ms.client.jobs.adventurer.thief.Shadower;
import net.swordie.ms.client.jobs.adventurer.thief.Thief;
import net.swordie.ms.client.jobs.adventurer.warrior.DarkKnight;
import net.swordie.ms.client.jobs.adventurer.warrior.Hero;
import net.swordie.ms.client.jobs.adventurer.warrior.Paladin;
import net.swordie.ms.client.jobs.adventurer.warrior.Warrior;
import net.swordie.ms.client.jobs.anima.*;
import net.swordie.ms.client.jobs.cygnus.*;
import net.swordie.ms.client.jobs.flora.*;
import net.swordie.ms.client.jobs.legend.*;
import net.swordie.ms.client.jobs.nova.*;
import net.swordie.ms.client.jobs.resistance.*;
import net.swordie.ms.client.jobs.resistance.demon.Demon;
import net.swordie.ms.client.jobs.resistance.demon.DemonAvenger;
import net.swordie.ms.client.jobs.resistance.demon.DemonSlayer;
import net.swordie.ms.client.jobs.sengoku.*;
import net.swordie.ms.client.jobs.shine.SiaAstelle;
import net.swordie.ms.client.jobs.special.SpecialEventJob;
import net.swordie.ms.client.jobs.special.SpecialPinkBean;
import net.swordie.ms.constants.JobConstants;

import java.util.function.Function;
import java.util.function.IntPredicate;

import static net.swordie.ms.constants.JobConstants.LoginJob.*;

public class JobManager {

    private record JobEntry(IntPredicate handler, Function<Char, Job> factory) {}

    private static final JobEntry[] JOBS = new JobEntry[] {
            new JobEntry(id -> JobConstants.isHero((short) id), Hero::new),
            new JobEntry(id -> JobConstants.isPaladin((short) id), Paladin::new),
            new JobEntry(id -> JobConstants.isDarkKnight((short) id), DarkKnight::new),
            new JobEntry(id -> JobConstants.isAdventurerWarrior((short) id), Warrior::new),

            new JobEntry(id -> JobConstants.isIceLightning((short) id), IceLightning::new),
            new JobEntry(id -> JobConstants.isFirePoison((short) id), FirePoison::new),
            new JobEntry(id -> JobConstants.isBishop((short) id), Bishop::new),
            new JobEntry(id -> JobConstants.isAdventurerMage((short) id), Magician::new),

            new JobEntry(id -> JobConstants.isBowMaster((short) id), BowMaster::new),
            new JobEntry(id -> JobConstants.isMarksman((short) id), Marksman::new),
            new JobEntry(id -> JobConstants.isPathFinder((short) id), Pathfinder::new),
            new JobEntry(id -> JobConstants.isAdventurerArcher((short) id), Archer::new),

            new JobEntry(id -> JobConstants.isNightLord((short) id), NightLord::new),
            new JobEntry(id -> JobConstants.isShadower((short) id), Shadower::new),
            new JobEntry(id -> JobConstants.isDualBlade((short) id), DualBlade::new),
            new JobEntry(id -> JobConstants.isAdventurerThief((short) id), Thief::new),

            new JobEntry(id -> JobConstants.isCannoneer((short) id), Cannoneer::new),
            new JobEntry(id -> JobConstants.isCorsair((short) id), Corsair::new),
            new JobEntry(id -> JobConstants.isBuccaneer((short) id), Buccaneer::new),
            new JobEntry(id -> JobConstants.isAdventurerPirate((short) id), Pirate::new),

            // Anima
            new JobEntry(id -> JobConstants.isHoYoung((short) id), HoYoung::new),
            new JobEntry(id -> JobConstants.isLara((short) id), Lara::new),
            new JobEntry(id -> JobConstants.isRen((short) id), Ren::new),

            // Cygnus
            new JobEntry(i -> JobConstants.isNoblesse((short) i), Noblesse::new),
            new JobEntry(i -> JobConstants.isDawnWarrior((short) i), DawnWarrior::new),
            new JobEntry(i -> JobConstants.isBlazeWizard((short) i), BlazeWizard::new),
            new JobEntry(i -> JobConstants.isWindArcher((short) i), WindArcher::new),
            new JobEntry(i -> JobConstants.isNightWalker((short) i), NightWalker::new),
            new JobEntry(i -> JobConstants.isThunderBreaker((short) i), ThunderBreaker::new),
            new JobEntry(i -> JobConstants.isMihile((short) i), Mihile::new),

            // Flora
            new JobEntry(id -> JobConstants.isIllium((short) id), Illium::new),
            new JobEntry(id -> JobConstants.isArk((short) id), Ark::new),
            new JobEntry(id -> JobConstants.isAdele((short) id), Adele::new),
            new JobEntry(id -> JobConstants.isKhali((short) id), Khali::new),

            // Jianghu
            new JobEntry(id -> JobConstants.isLynn((short) id), Lynn::new),
            new JobEntry(id -> JobConstants.isMoXuan((short) id), MoXuan::new),

            // Nova
            new JobEntry(id -> JobConstants.isAngelicBuster((short) id), AngelicBuster::new),
            new JobEntry(id -> JobConstants.isKaiser((short) id), Kaiser::new),
            new JobEntry(id -> JobConstants.isKain((short) id), Kain::new),
            new JobEntry(id -> JobConstants.isCadena((short) id), Cadena::new),

            // Heroes
            new JobEntry(id -> JobConstants.isLegend((short) id), Legend::new),
            new JobEntry(id -> JobConstants.isAran((short) id), Aran::new),
            new JobEntry(id -> JobConstants.isEvan((short) id), Evan::new),
            new JobEntry(id -> JobConstants.isLuminous((short) id), Luminous::new),
            new JobEntry(id -> JobConstants.isMercedes((short) id), Mercedes::new),
            new JobEntry(id -> JobConstants.isPhantom((short) id), Phantom::new),
            new JobEntry(id -> JobConstants.isShade((short) id), Shade::new),

            // Resistance
            new JobEntry(id -> JobConstants.isCitizen((short) id), Citizen::new),
            new JobEntry(id -> JobConstants.isBattleMage((short) id), BattleMage::new),
            new JobEntry(id -> JobConstants.isBlaster((short) id), Blaster::new),
            new JobEntry(id -> JobConstants.isMechanic((short) id), Mechanic::new),
            new JobEntry(id -> JobConstants.isWildHunter((short) id), WildHunter::new),
            new JobEntry(id -> JobConstants.isXenon((short) id), Xenon::new),

            new JobEntry(id -> JobConstants.isDemonSlayer((short) id), DemonSlayer::new),
            new JobEntry(id -> JobConstants.isDemonAvenger((short) id), DemonAvenger::new),
            new JobEntry(id -> JobConstants.isDemon((short) id), Demon::new),

            // Sengoku
            new JobEntry(id -> JobConstants.isHayato((short) id), Hayato::new),
            new JobEntry(id -> JobConstants.isKanna((short) id), Kanna::new),

            // Shine
            new JobEntry(id -> JobConstants.isSiaAstelle((short) id), SiaAstelle::new),

            // Others
            new JobEntry(id -> JobConstants.isZero((short) id), Zero::new),
            new JobEntry(id -> JobConstants.isKinesis((short) id), Kinesis::new),
            new JobEntry(id -> JobConstants.isPinkBean((short) id), SpecialPinkBean::new),
            new JobEntry(id -> JobConstants.isTanjiro((short) id) || JobConstants.isSaitama((short) id)
                    || JobConstants.isYeti((short) id), SpecialEventJob::new),
    };

    private short id;

    /**
     * Returns the level-10 secondary weapon for a creation job type, or null if none is required.
     */
    public static Integer getCreationSecondaryWeaponId(int jobType) {
        JobConstants.LoginJob loginJob = JobConstants.LoginJob.getLoginJobById(jobType);
        if (loginJob == null) {
            return null;
        }
        return switch (loginJob) {
            case MERCEDES -> 1352000;
            case KAISER -> 1352500;
            case ANGELIC -> 1352601;
            case PHANTOM -> 1352100;
            case XENON -> 1353001;
            case KINESIS -> 1353200;
            case CADENA -> 1353300;
            case KAIN -> 1354010;
            case ILLIUM -> 1353500;
            case ARK -> 1353600;
            case PATHFINDER -> 1353700;
            case HOYOUNG -> 1353800;
            case ADELE -> 1354000;
            case LARA -> 1354020;
            case KHALI -> 1354030;
            case MOXUAN -> 1352860;
            case HAYATO -> 1352800;
            case LYNN -> 1352810;
            case SIA -> 1353900;
            default -> null;
        };
    }

    public static Job getCreationJobById(int jobType, Char chr) {
        JobConstants.LoginJob loginJob = JobConstants.LoginJob.getLoginJobById(jobType);
        if (loginJob == null) {
            return new Beginner(chr);
        }
        return switch (loginJob) {
            case RESISTANCE -> new Citizen(chr);
            case EXPLORER -> new Beginner(chr);
            case CYGNUS -> new Noblesse(chr);
            case ARAN -> new Aran(chr);
            case EVAN -> new Evan(chr);
            case MERCEDES -> new Mercedes(chr);
            case DEMON -> new Demon(chr);
            case PHANTOM -> new Phantom(chr);
            case DUAL_BLADE -> new DualBlade(chr);
            case MIHILE -> new Mihile(chr);
            case LUMINOUS -> new Luminous(chr);
            case KAISER -> new Kaiser(chr);
            case ANGELIC -> new AngelicBuster(chr);
            case CANNONER -> new Cannoneer(chr);
            case XENON -> new Xenon(chr);
            case SHADE -> new Shade(chr);
            case KINESIS -> new Kinesis(chr);
            case CADENA -> new Cadena(chr);
            case ILLIUM -> new Illium(chr);
            case ARK -> new Ark(chr);
            case PATHFINDER -> new Pathfinder(chr);
            case HOYOUNG -> new HoYoung(chr);
            case ADELE -> new Adele(chr);
            case KAIN -> new Kain(chr);
            case LARA -> new Lara(chr);
            case KHALI -> new Khali(chr);
            case REN -> new Ren(chr);
            case MOXUAN -> new MoXuan(chr);
            case HAYATO -> new Hayato(chr);
            case KANNA -> new Kanna(chr);
            case LYNN -> new Lynn(chr);
            case SIA -> new SiaAstelle(chr);
            case ZERO -> new Zero(chr);
            case PINK_BEAN -> new SpecialPinkBean(chr);
            case YETI, TANJIRO, SAITAMA -> new SpecialEventJob(chr);
        };
    }

    /** Local tooling uses actual job IDs; these are not client character-selection IDs. */
    public static Job getSpecialCreationJobByActualId(short actualJob, Char chr) {
        if (!JobConstants.isSpecialEventJob(actualJob) || chr.getJob() != actualJob)
            throw new IllegalArgumentException("Special creation requires a matching actual job ID");
        return getJobById(actualJob, chr);
    }

    public static Job getJobById(short id, Char chr) {
        if (id == 0) {
            return new Beginner(chr);
        }

        int iid = id & 0xFFFF;
        for (JobEntry e : JOBS) {
            if (e.handler.test(iid)) {
                return e.factory.apply(chr);
            }
        }
        return new Beginner(chr);
    }

    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }
}
