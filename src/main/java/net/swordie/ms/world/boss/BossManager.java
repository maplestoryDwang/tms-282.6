package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;

public class BossManager {

    public static boolean check(int parentID, String scriptName) {
        if (parentID == 9390126) {
            return true;
        } else {
            switch (scriptName) {
                case "Zakum00":
                case "Zakum05":
                case "balrog_accept":
                case "gollux_accept":
                case "gloom_accept":
                case "darknell_accept":
                case "slime_accept":
                case "ursus_accept":
                case "zakum_accept":
                case "Zakum06":
                case "Akayrum_accept":
                case "cygnus_accept":
                case "hillah_accept":
                case "papulatus_accept":
                case "pierreEnter":
                case "rootafirstDoor":
                case "banbanEnter":
                case "rootasecondDoor":
                case "bloodyqueenEnter":
                case "rootathirdDoor":
                case "bellumEnter":
                case "rootaforthDoor":
                case "outrootaBoss":
                case "rootabyssOUT":
                case "magnus_boss":
                case "magnus_easy_enter":
                case "out_magnusDoor":
                case "blackHeaven_boss":
                case "bh_bossOut":
                case "bh_bossOutN":
                case "fallenWT_boss":
                case "DemianOut":
                case "hontale_accept":
                case "hontale_out":
                case "LionCastle_accept":
                case "lucid_accept":
                case "Ranmaru_check_eNum":
                case "Ranmaru_ptlNPC":
                case "Ranmaru_ptlNPC2":
                case "PinkBeen_accept":
                case "urusEnter":
                case "JinHillah_enter":
                case "out_450004250":
                case "out_450004300":
                case "west_450004150":
                case "PinkBeen_Out":
                case "testGotoBigBoss":
                case "GiantBossQuit1":
                case "GolluxOutReqeust":
                case "lotus_accept":
                case "princess_no_accept":
                case "omni_cln_accept":
                case "seren_accept":
                case "kalos_accept":
                case "kaling_accept":
                case "limbo_accept":
                case "baldrix_accept":
                case "first_adversary_accept":
                case "monster_park_extreme_accept":
                case "akechi_accept":
                case "black_mage_accept":
                case "will_enterGate":
                case "will_out":
                case "will_direction1":
                case "will_direction2":
                case "will_direction3":
                case "will_phase1":
                case "will_phase2":
                case "will_phase3":
                case "hid00":
                    return true;
                default:
                    return false;
            }
        }
    }

    public static boolean handle(Char chr, int parentID, String scriptName) {
        if (parentID == 9130095) {
            Arkarium.spawn(chr);
            return true;
        } else {
            switch (scriptName) {
                case "cygnus_Summon_Easy":
                    Cygnus.spawn(chr, Cygnus.CygnusMode.EASY);
                    return true;
                case "cygnus_Summon":
                    Cygnus.spawn(chr, Cygnus.CygnusMode.NORMAL);
                    return true;
                case "hillah_next":
                    Hilla.check(chr);
                    return true;
                case "pierre_Summon":
                    RootAbyss.spawnPierre(chr, RootAbyss.PierreMode.NORMAL);
                    return true;
                case "pierre_Summon1":
                    RootAbyss.spawnPierre(chr, RootAbyss.PierreMode.CHAOS);
                    return true;
                case "banbanNormal":
                case "banbanChaos":
                case "bellumNormal":
                case "bellumChaos":
                case "queenNormal":
                case "queenChaos":
                    RootAbyss.spawn(chr, scriptName);
                    return true;
                case "magnus_summon_E":
                    Magnus.spawn(chr, Magnus.MagnusMode.EASY);
                    return true;
                case "magnus_summon_N":
                    Magnus.spawn(chr, Magnus.MagnusMode.NORMAL);
                    return true;
                case "magnus_summon":
                    Magnus.spawn(chr, Magnus.MagnusMode.HARD);
                    return true;
                case "blackHeavenBoss1n_summon":
                    Lotus.spawn(chr, Lotus.LotusPhase.FIRST, Lotus.LotusMode.NORMAL);
                    return true;
                case "blackHeavenBoss1_summon":
                    Lotus.spawn(chr, Lotus.LotusPhase.FIRST, Lotus.LotusMode.HARD);
                    return true;
                case "blackHeavenBoss2n_summon":
                    Lotus.spawn(chr, Lotus.LotusPhase.SECOND, Lotus.LotusMode.NORMAL);
                    return true;
                case "blackHeavenBoss2_summon":
                    Lotus.spawn(chr, Lotus.LotusPhase.SECOND, Lotus.LotusMode.HARD);
                    return true;
                case "blackHeavenBoss3n_summon":
                    Lotus.spawn(chr, Lotus.LotusPhase.LAST, Lotus.LotusMode.NORMAL);
                    return true;
                case "blackHeavenBoss3_summon":
                    Lotus.spawn(chr, Lotus.LotusPhase.LAST, Lotus.LotusMode.HARD);
                    return true;
                case "first_DemianNormal1":
                case "first_DemianHard1":
                    Damien.spawn(chr, Damien.DamienPhase.FIRST);
                    return true;
                case "first_DemianNormal2":
                case "first_DemianHard2":
                    Damien.spawn(chr, Damien.DamienPhase.LAST);
                    return true;
                case "VanLeon_Summon":
                    VonLeon.spawn(chr);
                    return true;
                case "Fenter_450004150":
                    Lucid.spawn(chr, Lucid.LucidPhase.FIRST);
                    return true;
                case "Fenter_450004250":
                    Lucid.spawn(chr, Lucid.LucidPhase.SECOND);
                    return true;
                case "Fenter_450004300":
                case "enter_450004300":
                    Lucid.spawn(chr, Lucid.LucidPhase.REWARD);
                    return true;
                case "enter_450004200":
                    Lucid.cutScene(chr);
                    return true;
                case "Ranmaru_Before":
                    Ranmaru.spawn(chr, Ranmaru.RanmaruMode.NORMAL);
                    return true;
                case "Ranmaru_Before2":
                    Ranmaru.spawn(chr, Ranmaru.RanmaruMode.HARD);
                    return true;
                case "PinkBeen_Summon":
                    PinkBean.spawn(chr);
                    return true;
                case "GiantBoss_Head":
                    Gollux.init(chr, 0);
                    return true;
                case "GiantBoss_LArm":
                    Gollux.init(chr, 1);
                    return true;
                case "GiantBoss_RArm":
                    Gollux.init(chr, 2);
                    return true;
                case "GiantBoss_field":
                    Gollux.init(chr, 3);
                    return true;
                case "GiantBoss_Hip":
                    Gollux.init(chr, 4);
                    return true;
                case "onUserEnter_863010100":
                    Gollux.init(chr, 5);
                    return true;
                case "onUserEnter_863010700":
                    Gollux.init(chr, 6);
                    return true;
                default:
                    return false;
            }
        }
    }
}
