package net.swordie.ms.world.boss;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.MonsterPark;
import net.swordie.ms.client.character.SpiritSavior;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.union.UnionArtifact;
import net.swordie.ms.client.jobs.sengoku.Hayato;
import net.swordie.ms.client.social.Guild.Guild;
import net.swordie.ms.client.social.Guild.GuildMember;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.boss.demian.stigma.DemianStigma;
import net.swordie.ms.life.mob.skill.SpiderWeb;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.MobData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.event.SunnySunday;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Instance;
import net.swordie.ms.world.field.MobGen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class BossHelper {

    /** Supplemental entrance for encounters without a verified original cutscene. */
    public static void playBossEntry(Char chr, Runnable completion) {
        if (chr == null || chr.getField() == null || chr.getInstance() == null) {
            return;
        }
        Field source = chr.getField();
        Instance instance = chr.getInstance();
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.beginBossIntro(1000);
        sm.fadeInOut(250, 250, 500, 200);
        chr.getTimer().addEvent(() -> {
            if (chr.getField() != source || chr.getInstance() != instance) {
                return;
            }
            sm.endBossIntro();
            completion.run();
        }, 1000);
    }

    // Root Abyss:
    public static final int VON_BON = 8910100;
    public static final int CHAOS_VON_BON = 8910000;
    public static final int VELLUM = 8930100;
    public static final int CHAOS_VELLUM = 8930000;
    public static final int QUEEN_1 = 8920100;
    public static final int QUEEN_2 = 8920101;
    public static final int QUEEN_3 = 8920102;
    public static final int QUEEN_4 = 8920103;
    public static final int QUEEN_CHEST = 8920106;
    public static final int CHAOS_QUEEN_1 = 8920000;
    public static final int CHAOS_QUEEN_2 = 8920001;
    public static final int CHAOS_QUEEN_3 = 8920002;
    public static final int CHAOS_QUEEN_4 = 8920003;
    public static final int CHAOS_QUEEN_CHEST = 8920006;
    public static final int PIERRE_1 = 8900100;
    public static final int PIERRE_2 = 8900101;
    public static final int PIERRE_3 = 8900102;
    public static final int PIERRE_CHEST = 8900103;
    public static final int CHAOS_PIERRE_1 = 8900000;
    public static final int CHAOS_PIERRE_2 = 8900001;
    public static final int CHAOS_PIERRE_3 = 8900002;
    public static final int CHAOS_PIERRE_CHEST = 8900003;

    // Gollux:
    public static final int GOLLUX_FIRST_PHASE_HEAD = 9390600;
    public static final int GOLLUX_SECOND_PHASE_HEAD = 9390601;
    public static final int GOLLUX_THIRD_PHASE_HEAD = 9390602;
    public static final int GOLLUX_LEFT_SIDE_MOB = 9390622;
    public static final int GOLLUX_RIGHT_SIDE_MOB = 9390623;
    public static final int GOLLUX_RIGHT_SHOULDER = 9390610;
    public static final int GOLLUX_LEFT_SHOULDER = 9390611;
    public static final int GOLLUX_ABDOMEN = 9390612;

    // Ranmaru:
    public static final int RANMARU_NORMAL = 9421581;
    public static final int RANMARU_HARD = 9421583;

    // Chu Chu PQ
    public static final int[][] xy = {
            {-224, -380}, {-58, -380}, {124, -380}, {300, -380}, {530, -380}, {2320, -380}, {2510, -380},
            {2730, -380}, {2860, -380}, {3050, -380}, {-1287, -380}, {-1042, -380}, {-939, -380}, {-796, -380},
            {-583, -380}, {3363, -380}, {3511, -380}, {3748, -380}, {3964, -380}, {4177, -380}, {-1530, -890},
            {-1270, -890}, {-980, -890}, {-870, -890}, {-700, -890}, {3536, -890}, {3711, -890}, {3942, -890},
            {4189, -890}, {4365, -890}, {1120, -800}, {1400, -800}, {1730, -800}, {1413, -970}, {1413, -1100},
            {1113, -1650}, {1422, -1650}, {1720, -1650}, {1370, -1830}, {1646, -1950}};
    public static final int[][] setting = {
            {9833030, 9833031, 9833032, 9833033, 9833034, 9833035, 9833036, 9833037, 9833038, 9833039, 9833040, 9833041, 9833042, 9833043, 9833044, 9833045, 9833046},
            {9833050, 9833051, 9833052, 9833053, 9833054, 9833055, 9833056, 9833057, 9833058, 9833059, 9833060, 9833061, 9833062, 9833063, 9833064, 9833065, 9833066}};

    private static final int COMPATIBLE_BOSS_LOOT_WINDOW_MS = 30_000;

    public static boolean checkInstance(Char chr) {
        if (chr == null) {
            System.out.println("Player %s is null and tried to attempt Boss.");
            return true;
        }
        Instance instance = chr.getInstance();
        if (instance == null) {
            System.out.printf("Player %s tried to attempt Boss in Field ID: %d without an instance.%n", chr.getName(), chr.getFieldID());
            return true;
        }
        return false;
    }

    private static String resolveBossChineseName(int orderId) {
        return switch (orderId) {
            case 0 -> "蝙蝠怪";
            case 1 -> "扎昆";
            case 2 -> "暗黑龍王";
            case 3 -> "希拉";
            case 4 -> "皮埃爾";
            case 5 -> "半半";
            case 6 -> "血腥女王";
            case 7 -> "貝倫";
            case 8 -> "獅子王";
            case 9 -> "阿卡伊勒";
            case 10 -> "麥格納斯";
            case 11 -> "品克繽";
            case 12 -> "希納斯";
            case 13 -> "斯烏";
            case 14 -> "烏魯斯";
            case 15 -> "戴米安";
            case 16 -> "貝勒德";
            case 17 -> "森蘭丸";
            case 18 -> "濃姬";
            case 19 -> "露希妲";
            case 21 -> "卡翁";
            case 22 -> "鬧鐘";
            case 23 -> "威爾";
            case 24 -> "真希拉";
            case 25 -> "黑魔法師";
            case 26 -> "黑暗魔眼";
            case 27 -> "敦凱爾";
            case 28 -> "塞倫";
            case 29 -> "守護天使綠水靈";
            case 30 -> "卡洛斯";
            case 31 -> "卡凌";
            case 32 -> "極限怪物公園";
            case 33 -> "林波";
            case 34 -> "巴德里克斯";
            case 35 -> "第一使徒";
            case 107 -> "明智光秀";
            default -> null;
        };
    }

    private static String resolveCompatibleBossDisplayName(Char chr, Mob mob) {
        String orderStr = chr.getQRValueByKey(102401, "order");
        if (orderStr != null) {
            try {
                String bossName = resolveBossChineseName(Integer.parseInt(orderStr));
                if (bossName != null && !bossName.isBlank()) {
                    return bossName;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        String mobName = StringData.getMobStringById(mob.getTemplateId());
        if (mobName != null && !mobName.isBlank()) {
            return mobName;
        }
        return "首領";
    }

    public static void notifyCompatibleBossLootWindow(Field field, String bossName) {
        if (field == null || bossName == null || bossName.isBlank()) {
            return;
        }
        String message = String.format(
                "已擊殺%s首領，將在30秒後退出當前副本，請及時撿取獎勵！", bossName);
        for (Char player : field.getChars()) {
            player.chatScriptMessage(message);
        }
    }

    public static void scheduleCompatibleBossExit(Char chr, int forcedReturnFieldId) {
        Instance instance = chr.getInstance();
        if (instance == null) {
            return;
        }
        chr.getTimer().addEvent(() -> {
            Party party = chr.getParty();
            if (party != null) {
                for (Char player : party.getOnlineChars()) {
                    if (player.getInstance() == instance) {
                        player.getScriptManager().warpInstanceOut(player, forcedReturnFieldId);
                    }
                }
            } else if (chr.getInstance() == instance) {
                chr.getScriptManager().warpInstanceOut(chr, forcedReturnFieldId);
            }
        }, COMPATIBLE_BOSS_LOOT_WINDOW_MS);
    }

    /**
     * 282 boss lobby "return" or exit portal/NPC used while the party is still inside the boss instance. A plain
     * {@code sm.warp} there only moves the character into an instance copy of the town (the instance, its
     * timeout and the QR entry state stay behind); this leaves the instance the same way {@code exitBoss} does.
     *
     * @param targetFieldId the destination the original script used; 0 = the Boss UI return field (QR 102401 mapR)
     * @return false when nothing was done (265 profile, no instance) so the caller keeps its original behaviour
     */
    public static boolean leaveBossInstance(Char chr, int targetFieldId, int portalId) {
        if (chr == null || !Will.isTms282() || chr.getInstance() == null) {
            return false;
        }
        int target = targetFieldId > 0 ? targetFieldId : bossUiReturnField(chr);
        // multiplayer-0930: a lobby exit / return moves only this character; the party keeps its instance
        chr.getScriptManager().stopEvents();
        net.swordie.ms.world.field.InstanceMembership.leave(chr, target, Math.max(portalId, 0));
        return true;
    }

    /** QR 102401 mapR written by the Boss UI, if the active client still has that map; otherwise the home map. */
    public static int bossUiReturnField(Char chr) {
        String mapR = chr.getQRValueByKey(102401, "mapR");
        if (mapR != null && mapR.matches("\\d{9}")) {
            int field = Integer.parseInt(mapR);
            if (field != chr.getFieldID() && BossConstants.getBossEntranceMapForFightField(field) == 0
                    && net.swordie.ms.connection.protocol.codec.FieldAvailability.active().isAvailable(field)) {
                return field;
            }
        }
        return FieldConstants.HOME_MAP;
    }

    private static void completeCompatibleBoss(Char chr, Mob mob) {
        Instance instance = chr.getInstance();
        if (instance == null || instance.getProperty("compatibleBossComplete") != null) {
            return;
        }

        instance.addProperty("compatibleBossComplete", 1);
        Field field = mob.getField();
        net.swordie.ms.world.tms282.bossrun.BossRunService.onBossCleared(field, mob, chr.getParty()); // [bossrun] clear point (run-key de-dup with addPartyboss)
        field.removeMobs();
        chr.createQuestWithQRValue(34360, "boss=1");
        field.broadcast(UserPacket.effect(Effect.effectFromWZ(WzConstants.EFFECT_CLEAR)));
        notifyCompatibleBossLootWindow(field, resolveCompatibleBossDisplayName(chr, mob));
        scheduleCompatibleBossExit(chr, -1);
    }

    /** Root Abyss family of a Crimson Queen / Pierre form (normal / chaos), 0 = not one. */
    public static int rootAbyssFormFamily(int mobID) {
        if (mobID >= QUEEN_1 && mobID <= QUEEN_4) return QUEEN_1;
        if (mobID >= CHAOS_QUEEN_1 && mobID <= CHAOS_QUEEN_4) return CHAOS_QUEEN_1;
        if (mobID >= PIERRE_1 && mobID <= PIERRE_3) return PIERRE_1;
        if (mobID >= CHAOS_PIERRE_1 && mobID <= CHAOS_PIERRE_3) return CHAOS_PIERRE_1;
        return 0;
    }

    /**
     * bossflow-0930: {@code mob} (dead) is a Queen / Pierre form and no other living form of the same boss is on the
     * field (Chaos Pierre's blue/red split needs both). {@code -Dtms282.rootAbyss.formClear.enabled=false} = off.
     */
    public static boolean isRootAbyssLastForm(Field field, Mob mob) {
        int family = mob == null ? 0 : rootAbyssFormFamily(mob.getTemplateId());
        if (family == 0 || field == null || mob.getHp() > 0) return false;
        String sw = System.getProperty("tms282.rootAbyss.formClear.enabled");
        if (sw == null) {
            try {
                sw = net.swordie.ms.console.config.ServerRuntimeConfig.stringValue("tms282.rootAbyss.formClear.enabled");
            } catch (RuntimeException | LinkageError e) {
                sw = null;
            }
        }
        if ("false".equalsIgnoreCase(sw == null ? "" : sw.trim())) return false;
        for (Mob other : new ArrayList<>(field.getMobs())) {
            if (other != mob && other.getObjectId() != mob.getObjectId() && other.getHp() > 0
                    && rootAbyssFormFamily(other.getTemplateId()) == family) {
                return false;
            }
        }
        return true;
    }

    private static void trackSharedDifficultyBossClear(Char chr, int mobID, Party party) {
        if (Will.isTms282()) {
            // Clears that had no kill record in the per-template switches (daily/weekly limits never counted).
            int expectedOrder = switch (mobID) {
                case 8830010, 8830003 -> 0;          // Balrog damage sink
                case 8800022, 8800002, 8800102 -> 1; // Zakum final body
                case 8810214, 8810018, 8810122 -> 2; // Horntail
                case 8881000 -> 14;                  // Ursus
                case 8880410 -> 24;                  // Verus Hilla (both difficulties share the template)
                case 8880504 -> 25;                  // Black Mage Hard, last phase
                default -> -1;
            };
            if (expectedOrder >= 0) {
                BossPartyType selected = BossHpConstants.getCurrentBossParty(chr);
                if (selected != null && selected.getOrderId() == expectedOrder) {
                    if (party != null) chr.getScriptManager().addPartyBoss(party, selected);
                    else chr.addPartyboss(selected);
                }
                return;
            }
            if (mobID == 8881601) return; // Baldrix phase one is not a clear.
            if (mobID == 8881621 || mobID == 8881671 || mobID == 8881710 || mobID == 8881740
                    || mobID == 8881770 || mobID == 8881800) {
                BossPartyType selected = BossHpConstants.getCurrentBossParty(chr);
                if (selected != null) {
                    if (party != null) chr.getScriptManager().addPartyBoss(party, selected);
                    else chr.addPartyboss(selected);
                }
                return;
            }
        }
        if (mobID == 8881601) {
            BossPartyType boss = switch (chr.getQRValueByKey(102401, "diff")) {
                case "2" -> BossPartyType.BALDRIX_HARD;
                default -> BossPartyType.BALDRIX_NORMAL;
            };
            if (party != null) {
                chr.getScriptManager().addPartyBoss(party, boss);
            } else {
                chr.addPartyboss(boss);
            }
        } else if (mobID == 8881710) {
            BossPartyType boss = switch (chr.getQRValueByKey(102401, "diff")) {
                case "0" -> BossPartyType.FIRST_ADVERSARY_EASY;
                case "2" -> BossPartyType.FIRST_ADVERSARY_HARD;
                case "4" -> BossPartyType.FIRST_ADVERSARY_EXTREME;
                default -> BossPartyType.FIRST_ADVERSARY_NORMAL;
            };
            if (party != null) {
                chr.getScriptManager().addPartyBoss(party, boss);
            } else {
                chr.addPartyboss(boss);
            }
        }
    }

    public static void handleMobKilled(Mob mob, Char chr) {
        if (chr == null) return;
        if (mob.isBoss() && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && !mob.claimBossKillHandling()) return;

        ScriptManagerImpl sm = chr.getScriptManager();
        Party party = chr.getParty();
        Field field = mob.getField();
        int mobID = mob.getTemplateId();
        Instance instance = chr.getInstance();
        if (Will.isTms282()) {
            Tms282LinkedBodies.onMobDeath(mob); // bossflow-0930: a holder died -> remove its bodies
            if (Seren.onMobDeath(mob)) return;
            Kalos.onMobDeath(field, mobID);
            Kaling.onMobDeath(field, mobID);
            Limbo.onMobDeath(field, mobID);
            Malitia.onMobDeath(field, mobID);
            if (mob.getHp() <= 0 && !field.getMobs().contains(mob)) PrincessNo.onMobDeath(field, mobID);
            if (mobID == 8881200 || mobID == 8881201) {
                int expected = mobID == 8881200 ? 350068100 : 350068300;
                if (field.getId() == expected && instance != null) {
                    for (Char member : new ArrayList<>(field.getChars())) {
                        if (member.getInstance() == instance) member.warp(expected + 100);
                    }
                }
            }
            if (mobID == 8881621 || mobID == 8881671 || mobID == 8881740 || mobID == 8881770
                    || mobID == 8881800 || mobID == 9421589) completeCompatibleBoss(chr, mob);
        }
        // Zakum & Horntail
        if (mobID == 8800002 || mobID == 8800022 || mobID == 8800102 || mobID == 8810214 || mobID == 8810018 || mobID == 8810122) {
            if (instance != null) {
                for (Mob m : field.getMobs()) {
                    if (m.getObjectId() != mob.getObjectId()) {
                        m.removeWithAnimation();
                    }
                }
            }
        }
        // Field-scripted bosses that share the generic completion lifecycle.
        if (mobID == 8880405 || mobID == 8880410 || mobID == 8881000
                || mobID == 8870000 || mobID == 8870100
                || mobID == 8500002 || mobID == 8500012 || mobID == 8500022
                || mobID == 8644650 || mobID == 8644655
                || mobID == 8645009 || mobID == 8645066
                || mobID == 8880504 || mobID == 8880549
                || mobID == 8881202
                || mobID == 8880200
                || mobID == 9450022
                || mobID == 8880632 || mobID == 8880602 || mobID == 8880652
                || mobID == 8881012 || mobID == 8880802 || mobID == 8881032 || mobID == 8881052
                || mobID == 8880912 || mobID == 8880842 || mobID == 8880942 || mobID == 8880972
                || mobID == 8881332 || mobID == 8881382
                || mobID == 8881601 || mobID == 8881710
                || mobID == 6160003 || mobID == 9601623
                || mobID == 8880711 || mobID == 8880700
                || mobID == 8880000 || mobID == 8880002 || mobID == 8880010
                || mobID == 8910100 || mobID == 8910000
                || mobID == 8900100 || mobID == 8900000
                || mobID == 8920100 || mobID == 8920000
                || mobID == 8930100 || mobID == 8930000
                || mobID == 8840000 || mobID == 8840007 || mobID == 8840014
                // 282 Zakum bodies revive *0 -> *1 -> *2 (Mob.wz revive); only the last body clears the fight.
                || (!Will.isTms282() && (mobID == 8800020 || mobID == 8800000 || mobID == 8800100))
                || (Will.isTms282() && (mobID == 8800022 || mobID == 8800002 || mobID == 8800102))
                // 282 Arkarium Easy is summoned as 8860005 (firstenter_272020200).
                || (Will.isTms282() && mobID == 8860005)
                || mobID == 8810214 || mobID == 8810018 || mobID == 8810122
                || mobID == 8880342 || mobID == 8880302
                || mobID == 8820014 || mobID == 8820101
                || mobID == 8850111 || mobID == 8850011
                || mobID == 8950102 || mobID == 8950002
                || mobID == 8880111 || mobID == 8880101
                || mobID == 9421581 || mobID == 9421583
                || mobID == 8860000
                || mobID == 8830010 || mobID == 8830003) {
            if (!Will.isTms282() || (mobID != 8881601 && mobID != 9421583)) {
                completeCompatibleBoss(chr, mob);
            }
        } else if (Will.isTms282() && isRootAbyssLastForm(field, mob)) {
            // bossflow-0930: Crimson Queen changes face / Pierre changes hat or splits (remove + respawn, Mob HP
            // carried). The form that finally dies is often not the first template in the list above.
            completeCompatibleBoss(chr, mob);
        }
        // Will
        if (mobID == 8880300 || mobID == 8880340) { // Phase 1 main boss
            field.removeMobs();
            int nextField = field.getId() + (Will.isTms282() ? 50 : 100);
            for (Char x : party.getPartyMembersInSameFieldWithChr(chr)) {
                x.warp(nextField);
            }
        } else if (mobID == 8880301 || mobID == 8880341) { // Phase 2 main boss
            field.removeMobs();
            int nextField = field.getId() + (Will.isTms282() ? 50 : 100);
            for (Char x : party.getPartyMembersInSameFieldWithChr(chr)) {
                x.warp(nextField);
            }
        } else if (mobID == 8880302 || mobID == 8880342) {
            field.removeMobs();
            for (SpiderWeb spiderWeb : field.getSpiderWebs()) {
                field.removeLife(spiderWeb);
            }
        }
        // Lucid
        if (mobID == 8880140 || mobID == 8880141 || mobID == 8880151 || mobID == 8880153 || mobID == 8880155) {
            if (instance != null) {
                if (field.getSpawnButterflyTimer() != null) {
                    field.getSpawnButterflyTimer().cancel(false);
                }
                if (Will.isTms282() && field.getDestroyButterflyTimer() != null) {
                    field.getDestroyButterflyTimer().cancel(false);
                }
                field.broadcast(LucidPacket.butterflyAction(ButterFlyType.Erase, 0, null, 0, 0));
                for (Mob m : field.getMobs()) {
                    if (m.getObjectId() != mob.getObjectId()) {
                        m.removeWithAnimation();
                    }
                }
                if (party != null && party.getPartyMembersInSameFieldWithChr(chr).size() > 1) {
                    chr.getTimer().addEvent(() -> sm.warpParty(chr.getField().getId() + 50, party), 2000);
                } else {
                    chr.getTimer().addEvent(() -> chr.warp(chr.getField().getId() + 50), 2000);
                }
            }
            if (mobID == 8880155) { // Easy Lucid
                chr.createQuestWithQRValue(34360, "boss=1");
            }
            if (mobID == 8880151) { // Normal Lucid
                if (!chr.hasQuest(100089)) {
                    sm.startQuest(100089);
                }
                chr.completeQuest(100089);
                chr.createQuestWithQRValue(34360, "boss=1");
            }
            if (mobID == 8880153) { // Hard Lucid
                if (!chr.hasQuest(16995)) {
                    sm.startQuest(16995);
                }
                chr.completeQuest(16995);
                if (chr.hasQuest(35664)) {
                    chr.createQuestWithQRValue(35650, "clear=7");
                }
                if (!chr.hasQuest(36117)) {
                    sm.startQuest(36117);
                }
                chr.completeQuest(36117);
                if (!chr.hasQuest(100102)) {
                    sm.startQuest(100102);
                }
                chr.completeQuest(100102);
                if (!chr.hasQuest(16995)) {
                    sm.startQuest(16995);
                }
                chr.completeQuest(16995);
                chr.createQuestWithQRValue(34360, "boss=1");
            }
        } else if (mobID == 8880150) {
            if (instance != null) {
                if (chr.getLucidMode() == 2) {
                    instance.setTimeout(45, true);
                    field.spawnMob(8880153, 600, -490, false, 11000000000000L);
                    // bossmech1-0930: 282 Fury 3902AF0 does nothing for type 0; 1 = Fury/background (hard last phase
                    // start, 推定 from lucid-re-0930 §2). 265 keeps 0.
                    field.broadcast(LucidPacket.setLastPhaseTypeForHardMode(Will.isTms282() ? 1 : 0));
                } else {
                    if (field.getSpawnButterflyTimer() != null) {
                        field.getSpawnButterflyTimer().cancel(false);
                    }
                    if (Will.isTms282() && field.getDestroyButterflyTimer() != null) {
                        field.getDestroyButterflyTimer().cancel(false);
                    }
                    field.broadcast(LucidPacket.butterflyAction(ButterFlyType.Erase, 0, null, 0, 0));
                    for (Mob m : field.getMobs()) {
                        m.removeWithAnimation();
                    }
                    if (party != null && party.getPartyMembersInSameFieldWithChr(chr).size() > 1) {
                        chr.getTimer().addEvent(() -> sm.warpParty(chr.getField().getId() + 50, party), 2000);
                    } else {
                        chr.getTimer().addEvent(() -> chr.warp(chr.getField().getId() + 50), 2000);
                    }
                }
            }
        } else if (mobID == 8880190 || mobID == 8880196 || mobID == 8880191) {
            if (instance != null) {
                if (field.getSpawnButterflyTimer() != null) {
                    field.getSpawnButterflyTimer().cancel(false);
                }
                if (Will.isTms282() && field.getDestroyButterflyTimer() != null) {
                    field.getDestroyButterflyTimer().cancel(false);
                }
                field.broadcast(LucidPacket.butterflyAction(ButterFlyType.Erase, 0, null, 0, 0));
                if (mobID == 8880191) {
                    chr.createQuestWithQRValue(34360, "boss=1");
                    sm.warpInstanceOut(chr, 450004000);
                } else {
                    chr.warp(450004850);
                }
            }
        }
        // Damien
        if (mobID == BossConstants.DEMIAN_NORMAL_PHASE_1_TEMPLATE_ID || mobID == BossConstants.DEMIAN_HARD_PHASE_1_TEMPLATE_ID) {
            DemianStigma.startNextPhase(chr, false);
        }
        // Black Mage phase transitions
        if (field.getId() == 450013100 && (mobID == 8880500 || mobID == 8880501)) {
            if (instance != null && !field.hasMobById(8880500) && !field.hasMobById(8880501)) {
                field.removeMobs();
                if (party != null) {
                    for (Char x : party.getPartyMembersInSameFieldWithChr(chr)) {
                        x.warp(450013300);
                    }
                } else {
                    chr.warp(450013300);
                }
            }
        } else if (field.getId() == 450013300 && mobID == 8880502) {
            field.removeMobs();
            if (party != null) {
                for (Char x : party.getPartyMembersInSameFieldWithChr(chr)) {
                    x.warp(450013500);
                }
            } else {
                chr.warp(450013500);
            }
        } else if (field.getId() == 450013500 && mobID == 8880503) {
            field.removeMobs();
            if (party != null) {
                for (Char x : party.getPartyMembersInSameFieldWithChr(chr)) {
                    x.warp(450013700);
                }
            } else {
                chr.warp(450013700);
            }
        }
        // Lotus
        if (mobID == 8950000 || mobID == 8950100) {
            int fieldID = field.getId();
            if (fieldID == 350060700 || fieldID == 350060400) {
                for (Mob m : field.getMobs()) {
                    if (m.getObjectId() != mob.getObjectId()) {
                        m.removeWithAnimation();
                    }
                }
                if (party != null) {
                    chr.getTimer().addEvent(() -> sm.warpParty(fieldID + 100, party), 7360);
                } else {
                    chr.getTimer().addEvent(() -> chr.warp(fieldID + 100), 5100);
                }
            }
        } else if (mobID == 8950001 || mobID == 8950101) {
            int fieldID = field.getId();
            if (fieldID == 350060800 || fieldID == 350060500) {
                if (party != null) {
                    chr.getTimer().addEvent(() -> sm.warpParty(fieldID + 100, party), 5100);
                } else {
                    chr.getTimer().addEvent(() -> chr.warp(fieldID + 100), 5100);
                }
            }
        } else if (mobID == 8950002 || mobID == 8950102) {
            int fieldID = field.getId();
            if (fieldID == 350060900 || fieldID == 350060600) {
                sm.stopEvents();
                field.broadcast(UserPacket.effect(Effect.effectFromWZ("Map/Effect2.img/blackHeavenBossDie3")));
            }
        }
        // Root Abyss
        if (mobID == BossHelper.PIERRE_1 || mobID == BossHelper.PIERRE_2 || mobID == BossHelper.PIERRE_3) {
            BossHelper.initRootAbyss(chr, PIERRE_CHEST, 0, 497, 551);
        } else if (mobID == BossHelper.CHAOS_PIERRE_1 || mobID == BossHelper.CHAOS_PIERRE_2 || mobID == BossHelper.CHAOS_PIERRE_3) {
            BossHelper.initRootAbyss(chr, CHAOS_PIERRE_CHEST, 0, 497, 551);
        } else if (mobID == BossHelper.QUEEN_1 || mobID == BossHelper.QUEEN_2 || mobID == BossHelper.QUEEN_3 || mobID == BossHelper.QUEEN_4) {
            BossHelper.initRootAbyss(chr, QUEEN_CHEST, 0, 40, 135);
        } else if (mobID == BossHelper.CHAOS_QUEEN_1 || mobID == BossHelper.CHAOS_QUEEN_2 || mobID == BossHelper.CHAOS_QUEEN_3 || mobID == BossHelper.CHAOS_QUEEN_4) {
            BossHelper.initRootAbyss(chr, CHAOS_QUEEN_CHEST, 0, 40, 135);
        }
        // Gollux
        if (mobID == BossHelper.GOLLUX_FIRST_PHASE_HEAD) {
            field.broadcast(FieldPacket.syncDynamicFootHold("phase2-1", true, new Position(0, 0)));
            field.broadcast(FieldPacket.syncDynamicFootHold("phase2-2", true, new Position(0, 0)));
            BossHelper.spawnGollux(chr, 1);
        } else if (mobID == BossHelper.GOLLUX_SECOND_PHASE_HEAD) {
            if (instance != null) {
                field.broadcast(FieldPacket.syncDynamicFootHold("phase3", true, new Position(0, 0)));
                BossHelper.spawnGollux(chr, 2);
                sm.createTimerGauge(90);
                instance.setTimeout(90, true);
            }
        } else if (mobID == BossHelper.GOLLUX_THIRD_PHASE_HEAD) {
            notifyCompatibleBossLootWindow(field, "貝勒德");
            chr.getTimer().addEvent(() -> {
                if (party != null) {
                    sm.warpParty(BossConstants.GOLLUX_REWARD_MAP, party);
                } else {
                    sm.warpInstanceOut(chr, BossConstants.GOLLUX_REWARD_MAP);
                }
            }, COMPATIBLE_BOSS_LOOT_WINDOW_MS);
            field.broadcast(UserPacket.effect(Effect.effectFromWZ("Map/EffectTW.img/arisan/clear")));
        } else if (mobID == BossHelper.GOLLUX_LEFT_SHOULDER) {
            BossHelper.addClearedField(field, chr);
            BossHelper.openPortal(field, "clear", 7);
            field.broadcast(UserPacket.effect(Effect.effectFromWZ("Map/EffectTW.img/arisan/clear")));
        } else if (field.getOnUserEnter().equals("GiantBoss_field")) {
            int mobsize = field.getMobs().size();
            if (mobsize > 3) {
                sm.chatScript("這個區域還剩 " + (mobsize - 3) + " 股邪惡的氣息。");
            } else {
                BossHelper.addClearedField(field, chr);
                field.broadcast(UserPacket.effect(Effect.effectFromWZ("Map/EffectTW.img/arisan/clear")));
                if (chr.getField().getId() == 863010310 || chr.getField().getId() == 863010410) {
                    BossHelper.openPortal(field, "open", 1);
                    BossHelper.openPortal(field, "clear", 1);
                }
            }
        } else if (mobID == BossHelper.GOLLUX_ABDOMEN) {
            BossHelper.addClearedField(field, chr);
            BossHelper.openPortal(field, "clear2", 12);
            BossHelper.openPortal(field, "clear1", 12);
            field.broadcast(UserPacket.effect(Effect.effectFromWZ("Map/EffectTW.img/arisan/clear")));
        } else if (field.getOnUserEnter().equals("GiantBoss_RArm")) {
            int mobsize = field.getMobs().size();
            if (mobsize < 1) {
                BossHelper.addClearedField(field, chr);
                BossHelper.openPortal(field, "clear", 7);
                field.broadcast(UserPacket.effect(Effect.effectFromWZ("Map/EffectTW.img/arisan/clear")));
            }
        }
        // Tiger Chief
        if (mobID == 9300811 &&  chr.hasQuest(38022)) {
            chr.setQRValue(38022, "clear", false);
        }
        // Monster Park
        if (MonsterPark.isMonsterParkMob(mobID)) {
            long newExp = Long.parseLong(chr.getQRValue(GameConstants.MONSTER_PARK_EXP_QUEST)) + sm.getMPExpByMobId(mobID);
            var sunday = SunnySunday.SunnySundayLogic.get();
            newExp += newExp * (sunday != null ? sunday.mpIncEXP : 0) / 100;
            chr.setQRValue(GameConstants.MONSTER_PARK_EXP_QUEST, String.valueOf(newExp));
            field.broadcast(WvsContext.weatherEffectNotice(WeatherEffNoticeType.MonsterPark_ExpMsg, "已獲得經驗獎勵 " + sm.formatNumber(chr.getQRValue(GameConstants.MONSTER_PARK_EXP_QUEST)) + "！", 7000));
            if (!sm.hasMobsInField(chr.getFieldID())) {
                field.broadcast(UserPacket.effect(Effect.effectFromWZ(WzConstants.EFFECT_CLEAR)));
            }
        }
        // Mulung Dojo
        if (GameConstants.isMuLungDojoMob(mobID)) {
            if (instance != null) {
                instance.addProperty("dojoclear" + ((chr.getField().getId() % 10000) / 100), 1);
                sm.showFieldEffectToField(WzConstants.EFFECT_CLEAR);
                field.broadcast(UserPacket.effect(Effect.effectFromWZ(WzConstants.EFFECT_CLEAR)));
            }
        }
        // Red Leaf High Event
        if (EventConstants.RED_LEAF_HIGH_EVENT) {
            if (instance != null && GameConstants.isRedLeafHigh(mobID)) {
                if (mobID == 9410248) {
                    sm.showFieldEffect("Map/Effect.img/MapleHighSchool/clearF");
                    sm.spawnNpc(9330279, 21, 257);
                } else if (mobID == 9410218 || mobID == 9410219) {
                    sm.showFieldEffect("Map/Effect.img/MapleHighSchool/clear");
                    sm.spawnNpc(9330279, 34, 247);
                } else {
                    sm.showFieldEffect("Map/Effect.img/MapleHighSchool/clear");
                    sm.spawnNpc(9330279, 34, 247);
                }
            }
            if (mobID == 8642002) {
                if (chr.hasQuest(EventConstants.RED_LEAF_HIGH_MOB_KILLS_RECORD)) {
                    int currentMob = Integer.parseInt(chr.getQRValueByKey(EventConstants.RED_LEAF_HIGH_MOB_KILLS_RECORD, String.valueOf(mobID)));
                    if (currentMob < 200) {
                        currentMob++;
                        chr.setQRValueByKey(EventConstants.RED_LEAF_HIGH_MOB_KILLS_RECORD, String.valueOf(mobID), String.valueOf(currentMob));
                        chr.write(WvsContext.progressMessageFont(1, 14, 4, 0, String.format("%s (%d/200)", StringData.getMobStringById(mobID), currentMob)));
                    }
                }
            }
        }
        // [Elodin] Spring Cleaning
        if (chr.hasQuest(37164) && field.getId() == 101082200) { // 2nd Floor Attic 2
            // Eliminate Dust Bundles with the Makeshift Broomstick
            String count = chr.getQRValueByKey(37164, "count");
            if (count == null) {
                chr.createQuestWithQRValue(37164, "count=1");
                chr.chatScriptMessage("怪物已消滅 (1/50)");
            } else if (!count.equalsIgnoreCase("50")) {
                int inc = Integer.parseInt(count) + 1;
                if (inc == 10 || inc == 30 || inc == 50) {
                    chr.consumeItem(4036502, 1);
                }
                chr.setQRValueByKey(37164, "count", String.valueOf(inc));
                chr.chatScriptMessage(String.format("怪物已消滅 (%d/50)", inc));
            }
        }
        // Daily Quest Theme Dungeon
        if (instance != null) {
            if (field.getMobs().size() <= 1
                    && (field.getId() == 993162500
                    || field.getId() == 993162600
                    || field.getId() == 993162700
                    || field.getId() == 993162900
                    || field.getId() == 993163000
                    || field.getId() == 993163100)) {
                sm.showFieldEffect("Map/Effect.img/MapleHighSchool/clear");
            }
        }
        // Party Quests
        if (party != null && instance != null) {
            // Xerxes PQ
            if (field.getId() == GameConstants.XERXES_CHRYSE_FRONT_FIELD && field.getMobs().size() <= 1) {
                sm.showEffectToField("Map/Effect.img/killing/clear");
            } else if (mobID == 6160003 && field.getId() == GameConstants.XERXES_CHRYSE_MAP_BOSS
                    && !"32".equals(chr.getQRValueByKey(102401, "order"))) {
                sm.givePQRewards(party);
                for (Char player : party.getOnlineChars()) {
                    int amount = Util.getRandom(1, 15);
                    if (player.canHold(4001844)) {
                        player.addItemToInventory(4001844, amount);
                        player.chatScriptMessage("恭喜！擊敗薛西斯後，你獲得了 " + amount + " 個薛西斯的核心碎片！");
                    } else {
                        player.sendRewardToChar(4001844, amount, 0, "克里塞的薛西斯組隊任務獎勵。", 1);
                    }
                    player.getScriptManager().setAchieveRatio(100);
                }
                chr.getScriptManager().warpInstanceOut(chr, GameConstants.XERXES_CHRYSE_EXIT);
                party.broadcast(TemporarySkillMan.setTemporarySkillSet(0, 0));
            }
            // Alien Visitor
            if (field.getId() / 1000 == 861000) {
                int stage = (field.getId() % 1000) / 100;
                if (instance.getVisitorStageCount() < 180) {
                    if ((stage == 3 && mobID == 9390110) || (stage == 5 && (mobID == 9390111 || mobID == 9390112))) {
                        instance.setVisitorStageCount(1);
                    } else {
                        instance.setVisitorStageCount(instance.getVisitorStageCount() + 1);
                    }
                    for (Char player : party.getOnlineChars()) {
                        if ((stage == 3 && mobID == 9390110) || (stage == 5 && (mobID == 9390111 || mobID == 9390112))) {
                            player.chatScriptMessage("[外星訪客] 第 " + stage + " 關：" + instance.getVisitorStageCount() + "/1");
                        } else {
                            player.chatScriptMessage("[外星訪客] 第 " + stage + " 關：" + instance.getVisitorStageCount() + "/180");
                        }
                    }
                }
                if (((stage == 3 || stage == 5) && instance.getVisitorStageCount() == 1) || instance.getVisitorStageCount() == 180) {
                    for (MobGen mg : field.getMobGens()) {
                        field.removeLife(mg);
                    }
                    for (Mob m : field.getMobs()) {
                        if (m.getObjectId() != mob.getObjectId()) {
                            field.removeMob(m.getObjectId());
                        }
                    }
                    int timeConsumed = 5 * 60 - instance.getRemainingTime();
                    if (timeConsumed <= 90) {
                        for (Char pmChr : party.getOnlineChars()) {
                            pmChr.getVisitorStageResult().put(stage, "S");
                        }
                        sm.showEffectToField("Map/Effect.img/Visitor/RankS");
                    } else if (timeConsumed <= 150) {
                        for (Char pmChr : party.getOnlineChars()) {
                            pmChr.getVisitorStageResult().put(stage, "A");
                        }
                        sm.showEffectToField("Map/Effect.img/Visitor/RankA");
                    } else if (timeConsumed <= 210) {
                        for (Char pmChr : party.getOnlineChars()) {
                            pmChr.getVisitorStageResult().put(stage, "B");
                        }
                        sm.showEffectToField("Map/Effect.img/Visitor/RankB");
                    } else if (timeConsumed < 300) {
                        for (Char pmChr : party.getOnlineChars()) {
                            pmChr.getVisitorStageResult().put(stage, "C");
                        }
                        sm.showEffectToField("Map/Effect.img/Visitor/RankC");
                    }
                }
            }
            // Nett Pyramid
            if (field.getId() == GameConstants.NETT_PYRAMID_MAIN_MAP) {
                chr.getDefenseEventMember().plusPoint(2);
            }
            // Dimensional Invasion
            if (field.getId() == GameConstants.DIMENSIONAL_INVASION_MAIN_STAGE) {
                if (mobID >= 2500130 && mobID <= 2500133) {
                    field.decMonsterGauge(5);
                } else if (mobID >= 2500800 && mobID <= 2500803) {
                    field.decMonsterGauge(15);
                } else if (mobID == 9300622) {
                    field.decMonsterGauge(70);
                } else if (mobID >= 2500030 && mobID <= 2500033) {
                    field.decMonsterGauge(1);
                } else if (mobID == 9300621) {
                    field.decMonsterGauge(1);
                } else if (mobID == 9300634) {
                    field.decMonsterGauge(70);
                }
                field.broadcast(MultiStagePacket.setMonsterGauge(100, field.getMonsterGauge()));
            }
            // Dragon Rider
            if (field.getId() == 240080100 || field.getId() == 240080200) {
                int mobsize = field.getMobs().size();
                if (mobsize > 1) {
                    sm.chatScript("還剩 " + mobsize + " 隻怪物。");
                } else {
                    int stage = (sm.getFieldID() % 1000) / 100;
                    instance.addProperty("escape" + stage + "clear", sm.getField());
                    sm.showEffectToField(WzConstants.EFFECT_PQ_CLEAR);
                    sm.playSound(WzConstants.EFFECT_PQ_SOUND_CLEAR, true);
                    sm.showClearStageExpWindowToParty(sm.getPQExp());
                    sm.setAchieveRatio(stage * 20);
                    sm.chatScript("請使用傳送點前往下一關。");
                }
            } else if (mobID == 9300863 && field.getId() == 240080300) {
                int stage = (sm.getFieldID() % 1000) / 100;
                instance.addProperty("escape" + stage + "clear", sm.getField());
                sm.showEffectToField(WzConstants.EFFECT_PQ_CLEAR);
                sm.playSound(WzConstants.EFFECT_PQ_SOUND_CLEAR, true);
                sm.showClearStageExpWindowToParty(sm.getPQExp());
                sm.setAchieveRatio(stage * 20);
            } else if (mobID == 9303079 && field.getId() == 240080500) {
                int stage = (sm.getFieldID() % 1000) / 100;
                instance.addProperty("escape" + stage + "clear", sm.getField());
                sm.givePQRewards(party);
                sm.setAchieveRatio(100);
            }
            // Escape
            if (mobID == 9300454) {
                int stage = (sm.getFieldID() % 1000) / 100;
                instance.addProperty("escape" + stage + "clear", sm.getField());
                sm.givePQRewards(party);
                sm.setAchieveRatio(100);
            }
            // Evolution System
            if (mobID == 9306003 && field.getId() == 957013000) {
                spawnMobForEvolLink(field, 4, mob.getPosition());
            } else if (mobID == 9306004 && field.getId() == 957014000) {
                spawnMobForEvolLink(field, 5, mob.getPosition());
            } else if (mobID == 9306003 && field.getId() == 957017000) {
                spawnMobForEvolLink(field, 8, mob.getPosition());
            }
            // Chu Chu PQ
            int[] mobs = new int[]{};
            if (field.getId() == GameConstants.HUNGRY_MUTO_NORMAL_STAGE) {
                mobs = setting[0];
                spawnHungryMutoMobs(field, mob, sm, mobs);
            } else if (field.getId() == GameConstants.HUNGRY_MUTO_HARD_STAGE) {
                mobs = setting[1];
                spawnHungryMutoMobs(field, mob, sm, mobs);
            }
            // Juliet
            if (field.getId() == 926110001) {
                int mobsize = field.getMobs().size();
                if (mobsize > 1) {
                    sm.chatScript("還剩 " + mobsize + " 隻怪物。");
                } else {
                    sm.chatScript("請使用傳送點前往下一關。");
                }
            } else if (field.getId() == 926110401 && mobID == 9300152) {
                field.spawnNpc(2112006, 45, 150);
                for (Mob m : field.getMobs()) {
                    if (m.getObjectId() != mob.getObjectId()) {
                        field.removeMob(m.getObjectId());
                    }
                }
                sm.givePQRewards(party);
                sm.setAchieveRatio(100);
            }
            // Rome
            if (field.getId() == 926100001) {
                int mobsize = field.getMobs().size();
                if (mobsize > 1) {
                    sm.chatScript("還剩 " + mobsize + " 隻怪物。");
                } else {
                    sm.chatScript("請使用傳送點前往下一關。");
                }
            } else if (field.getId() == 926100401 && mobID == 9300140) {
                field.spawnNpc(2112005, 45, 150);
                for (Mob m : field.getMobs()) {
                    if (m.getObjectId() != mob.getObjectId()) {
                        field.removeMob(m.getObjectId());
                    }
                }
                sm.givePQRewards(party);
                sm.setAchieveRatio(100);
            }
        }
        // Spirit Savior
        if (field.getId() == SpiritSavior.SPIRIT_SAVIOR_MAP) {
            chr.getSpiritSavior().monsterkilled(mob);
        }
        // Hayato Passive
        if (JobConstants.isHayato(chr.getJob()) && chr.getJobHandler() instanceof Hayato hayato) {
            hayato.incrementSwordEnergy();
        }
        if (chr.getParty() != null) {
            if (chr.getInstance() != null) {
                int bossGuildContribution = GameConstants.getBossGuildContribution(mobID);
                long bossRewardPrice = GameConstants.getBossRewardPrice(GameConstants.getBossRewardID(mobID));
                if (bossRewardPrice != 1) {
                    Server.get().broadcastForWorld(UserLocal.chatMsg(ChatType.Expedition, "[公告] 角色 " + chr.getName() + " 成功擊敗了 " + StringData.getMobStringById(mobID) + "！"));
                }
                for (Char pmChr : chr.getParty().getOnlineChars()) {
                    if (pmChr.getGuild() != null && bossGuildContribution != -1) {
                        pmChr.getGuild().addContributionToChar(pmChr, bossGuildContribution);
                    }
                    // Intense Power Crystal
                    if (bossRewardPrice != 1) {
                        pmChr.setLastBossTemplateID(GameConstants.getBossRewardID(mobID));
                        Item item = ItemData.getItemDeepCopy(4001886);
                        item.setBossRewardID(GameConstants.getBossRewardID(mobID));
                        Drop drop = new Drop(item.getItemId(), item);
                        drop.setOwnerID(pmChr.getId());
                        field.drop(drop, pmChr.getPosition(), pmChr.getPosition(), true, 0, pmChr);
                    }
                }
            }
        }
        // Boss Reward for Guild Members
        if (chr.getParty() != null && chr.getParty().getOnlineChars().size() == 1) {
            // Solo party
            int type = BossHelper.getGuildRewardGradeByMobID(mobID);
            if (type != -1) {
                Guild guild = chr.getGuild();
                if (guild != null) {
                    for (GuildMember gm : guild.getMembers()) {
                        Char gmChar = Server.get().getWorld().getCharById(gm.getCharID());
                        if (gmChar != null) {
                            if (gmChar.getId() != chr.getId() && gmChar.getGuildID() == guild.getId()) {
                                gmChar.sendGuildReward(chr.getName(), guild.getName(), type);
                                gmChar.sendPacketRewards();
                            }
                        } else {
                            gmChar = Char.getCharByGuildID(gm.getCharID());
                            if (gmChar != null) {
                                gmChar.initRewardSystem();
                                gmChar.sendGuildReward(chr.getName(), guild.getName(), type);
                            }
                        }
                    }
                }
            }
        }
        // Boss Attempt Clear Limit
        if (chr.getParty() != null) {
            switch (mobID) {
                case 8860005 -> sm.addPartyBoss(party, BossPartyType.ARKARIUM_EASY);
                case 8860000 -> sm.addPartyBoss(party, BossPartyType.ARKARIUM_NORMAL);

                case 8850111 -> sm.addPartyBoss(party, BossPartyType.CYGNUS_EASY);
                case 8850011 -> sm.addPartyBoss(party, BossPartyType.CYGNUS_NORMAL);

                case 8880111 -> sm.addPartyBoss(party, BossPartyType.DAMIEN_NORMAL);
                case 8880101 -> sm.addPartyBoss(party, BossPartyType.DAMIEN_HARD);

                case 8870000 -> sm.addPartyBoss(party, BossPartyType.HILLA_NORMAL);
                case 8870100 -> sm.addPartyBoss(party, BossPartyType.HILLA_HARD);

                case 8950002 -> sm.addPartyBoss(party, Will.isTms282() ? BossPartyType.LOTUS_HARD : BossPartyType.LOTUS_NORMAL);
                case 8950102 -> sm.addPartyBoss(party, Will.isTms282() ? BossPartyType.LOTUS_NORMAL : BossPartyType.LOTUS_HARD);

                case 8880010 -> sm.addPartyBoss(party, BossPartyType.MAGNUS_EASY);
                case 8880002 -> sm.addPartyBoss(party, BossPartyType.MAGNUS_NORMAL);
                case 8880000 -> sm.addPartyBoss(party, BossPartyType.MAGNUS_HARD);

                case 8820001, 8820014 -> sm.addPartyBoss(party, BossPartyType.PINK_BEAN_NORMAL);
                case 8820212, 8820101 -> sm.addPartyBoss(party, BossPartyType.PINK_BEAN_CHAOS);

                case 9421581 -> sm.addPartyBoss(party, BossPartyType.RANMARU_NORMAL);
                case 9421583, 9421589 -> {
                    if (!Will.isTms282() || mobID == 9421589) sm.addPartyBoss(party, BossPartyType.RANMARU_HARD);
                }

                case VON_BON -> sm.addPartyBoss(party, BossPartyType.VON_BON_NORMAL);
                case CHAOS_VON_BON -> sm.addPartyBoss(party, BossPartyType.VON_BON_CHAOS);

                case QUEEN_CHEST -> sm.addPartyBoss(party, BossPartyType.QUEEN_NORMAL);
                case CHAOS_QUEEN_CHEST -> sm.addPartyBoss(party, BossPartyType.QUEEN_CHAOS);

                case VELLUM -> sm.addPartyBoss(party, BossPartyType.VELLUM_NORMAL);
                case CHAOS_VELLUM -> sm.addPartyBoss(party, BossPartyType.VELLUM_CHAOS);

                case PIERRE_CHEST -> sm.addPartyBoss(party, BossPartyType.PIERRE_NORMAL);
                case CHAOS_PIERRE_CHEST -> sm.addPartyBoss(party, BossPartyType.PIERRE_CHAOS);

                case 8840000 -> sm.addPartyBoss(party, BossPartyType.VON_LEON_EASY);
                case 8840007 -> sm.addPartyBoss(party, BossPartyType.VON_LEON_NORMAL);
                case 8840014 -> sm.addPartyBoss(party, BossPartyType.VON_LEON_HARD);

                case 8880177 -> sm.addPartyBoss(party, BossPartyType.LUCID_EASY);
                case 8880167 -> sm.addPartyBoss(party, BossPartyType.LUCID_NORMAL);
                case 8880156 -> sm.addPartyBoss(party, BossPartyType.LUCID_HARD);

                case 8880342 -> sm.addPartyBoss(party, BossPartyType.WILL_NORMAL);
                case 8880302 -> sm.addPartyBoss(party, BossPartyType.WILL_HARD);

                case 8500002 -> sm.addPartyBoss(party, BossPartyType.PAPULATUS_EASY);
                case 8500012 -> sm.addPartyBoss(party, BossPartyType.PAPULATUS_NORMAL);
                case 8500022 -> sm.addPartyBoss(party, BossPartyType.PAPULATUS_CHAOS);

                case 8644650 -> sm.addPartyBoss(party, BossPartyType.GLOOM_NORMAL);
                case 8644655 -> sm.addPartyBoss(party, BossPartyType.GLOOM_CHAOS);

                case 8645009 -> sm.addPartyBoss(party, BossPartyType.DARKNELL_NORMAL);
                case 8645066 -> sm.addPartyBoss(party, BossPartyType.DARKNELL_HARD);

                case 8880711 -> sm.addPartyBoss(party, BossPartyType.SLIME_NORMAL);
                case 8880700 -> sm.addPartyBoss(party, BossPartyType.SLIME_HARD);

                case 8881202 -> sm.addPartyBoss(party, BossPartyType.LOTUS_EXTREME);
                case 9450022 -> sm.addPartyBoss(party, BossPartyType.PRINCESS_NO);
                case 8880200 -> sm.addPartyBoss(party, BossPartyType.OMNI_CLN_NORMAL);
                case 8880549 -> sm.addPartyBoss(party, BossPartyType.BLACK_MAGE_EXTREME);
                case 8880632 -> sm.addPartyBoss(party, BossPartyType.CHOSEN_SEREN_NORMAL);
                case 8880602 -> sm.addPartyBoss(party, BossPartyType.CHOSEN_SEREN_HARD);
                case 8880652 -> sm.addPartyBoss(party, BossPartyType.CHOSEN_SEREN_EXTREME);
                case 8881012 -> sm.addPartyBoss(party, BossPartyType.KALOS_EASY);
                case 8880802 -> sm.addPartyBoss(party, BossPartyType.KALOS_NORMAL);
                case 8881032 -> sm.addPartyBoss(party, BossPartyType.KALOS_HARD);
                case 8881052 -> sm.addPartyBoss(party, BossPartyType.KALOS_EXTREME);
                case 8880912 -> sm.addPartyBoss(party, BossPartyType.KALING_EASY);
                case 8880842 -> sm.addPartyBoss(party, BossPartyType.KALING_NORMAL);
                case 8880942 -> sm.addPartyBoss(party, BossPartyType.KALING_HARD);
                case 8880972 -> sm.addPartyBoss(party, BossPartyType.KALING_EXTREME);
                case 6160003 -> sm.addPartyBoss(party, BossPartyType.MONSTER_PARK_EXTREME);
                case 8881332 -> sm.addPartyBoss(party, BossPartyType.LIMBO_NORMAL);
                case 8881382 -> sm.addPartyBoss(party, BossPartyType.LIMBO_HARD);
                case 9601623 -> sm.addPartyBoss(party, BossPartyType.AKECHI_MITSUHIDE);
            }
            trackSharedDifficultyBossClear(chr, mobID, party);
        } else {
            switch (mobID) {
                case 8860005 -> chr.addPartyboss(BossPartyType.ARKARIUM_EASY);
                case 8860000 -> chr.addPartyboss(BossPartyType.ARKARIUM_NORMAL);

                case 8850111 -> chr.addPartyboss(BossPartyType.CYGNUS_EASY);
                case 8850011 -> chr.addPartyboss(BossPartyType.CYGNUS_NORMAL);

                case 8880111 -> chr.addPartyboss(BossPartyType.DAMIEN_NORMAL);
                case 8880101 -> chr.addPartyboss(BossPartyType.DAMIEN_HARD);

                case 8870000 -> chr.addPartyboss(BossPartyType.HILLA_NORMAL);
                case 8870100 -> chr.addPartyboss(BossPartyType.HILLA_HARD);

                case 8950002 -> chr.addPartyboss(Will.isTms282() ? BossPartyType.LOTUS_HARD : BossPartyType.LOTUS_NORMAL);
                case 8950102 -> chr.addPartyboss(Will.isTms282() ? BossPartyType.LOTUS_NORMAL : BossPartyType.LOTUS_HARD);

                case 8880010 -> chr.addPartyboss(BossPartyType.MAGNUS_EASY);
                case 8880002 -> chr.addPartyboss(BossPartyType.MAGNUS_NORMAL);
                case 8880000 -> chr.addPartyboss(BossPartyType.MAGNUS_HARD);

                case 8820001, 8820014 -> chr.addPartyboss(BossPartyType.PINK_BEAN_NORMAL);
                case 8820212, 8820101 -> chr.addPartyboss(BossPartyType.PINK_BEAN_CHAOS);

                case 9421581 -> chr.addPartyboss(BossPartyType.RANMARU_NORMAL);
                case 9421583, 9421589 -> {
                    if (!Will.isTms282() || mobID == 9421589) chr.addPartyboss(BossPartyType.RANMARU_HARD);
                }

                case VON_BON -> chr.addPartyboss(BossPartyType.VON_BON_NORMAL);
                case CHAOS_VON_BON -> chr.addPartyboss(BossPartyType.VON_BON_CHAOS);

                case QUEEN_CHEST -> chr.addPartyboss(BossPartyType.QUEEN_NORMAL);
                case CHAOS_QUEEN_CHEST -> chr.addPartyboss(BossPartyType.QUEEN_CHAOS);

                case VELLUM -> chr.addPartyboss(BossPartyType.VELLUM_NORMAL);
                case CHAOS_VELLUM -> chr.addPartyboss(BossPartyType.VELLUM_CHAOS);

                case PIERRE_CHEST -> chr.addPartyboss(BossPartyType.PIERRE_NORMAL);
                case CHAOS_PIERRE_CHEST -> chr.addPartyboss(BossPartyType.PIERRE_CHAOS);

                case 8840000 -> chr.addPartyboss(BossPartyType.VON_LEON_EASY);
                case 8840007 -> chr.addPartyboss(BossPartyType.VON_LEON_NORMAL);
                case 8840014 -> chr.addPartyboss(BossPartyType.VON_LEON_HARD);

                case 8880177 -> chr.addPartyboss(BossPartyType.LUCID_EASY);
                case 8880167 -> chr.addPartyboss(BossPartyType.LUCID_NORMAL);
                case 8880156 -> chr.addPartyboss(BossPartyType.LUCID_HARD);

                case 8880342 -> chr.addPartyboss(BossPartyType.WILL_NORMAL);
                case 8880302 -> chr.addPartyboss(BossPartyType.WILL_HARD);

                case 8500002 -> chr.addPartyboss(BossPartyType.PAPULATUS_EASY);
                case 8500012 -> chr.addPartyboss(BossPartyType.PAPULATUS_NORMAL);
                case 8500022 -> chr.addPartyboss(BossPartyType.PAPULATUS_CHAOS);

                case 8644650 -> chr.addPartyboss(BossPartyType.GLOOM_NORMAL);
                case 8644655 -> chr.addPartyboss(BossPartyType.GLOOM_CHAOS);

                case 8645009 -> chr.addPartyboss(BossPartyType.DARKNELL_NORMAL);
                case 8645066 -> chr.addPartyboss(BossPartyType.DARKNELL_HARD);

                case 8880711 -> chr.addPartyboss(BossPartyType.SLIME_NORMAL);
                case 8880700 -> chr.addPartyboss(BossPartyType.SLIME_HARD);

                case 8881202 -> chr.addPartyboss(BossPartyType.LOTUS_EXTREME);
                case 9450022 -> chr.addPartyboss(BossPartyType.PRINCESS_NO);
                case 8880200 -> chr.addPartyboss(BossPartyType.OMNI_CLN_NORMAL);
                case 8880549 -> chr.addPartyboss(BossPartyType.BLACK_MAGE_EXTREME);
                case 8880632 -> chr.addPartyboss(BossPartyType.CHOSEN_SEREN_NORMAL);
                case 8880602 -> chr.addPartyboss(BossPartyType.CHOSEN_SEREN_HARD);
                case 8880652 -> chr.addPartyboss(BossPartyType.CHOSEN_SEREN_EXTREME);
                case 8881012 -> chr.addPartyboss(BossPartyType.KALOS_EASY);
                case 8880802 -> chr.addPartyboss(BossPartyType.KALOS_NORMAL);
                case 8881032 -> chr.addPartyboss(BossPartyType.KALOS_HARD);
                case 8881052 -> chr.addPartyboss(BossPartyType.KALOS_EXTREME);
                case 8880912 -> chr.addPartyboss(BossPartyType.KALING_EASY);
                case 8880842 -> chr.addPartyboss(BossPartyType.KALING_NORMAL);
                case 8880942 -> chr.addPartyboss(BossPartyType.KALING_HARD);
                case 8880972 -> chr.addPartyboss(BossPartyType.KALING_EXTREME);
                case 6160003 -> chr.addPartyboss(BossPartyType.MONSTER_PARK_EXTREME);
                case 8881332 -> chr.addPartyboss(BossPartyType.LIMBO_NORMAL);
                case 8881382 -> chr.addPartyboss(BossPartyType.LIMBO_HARD);
                case 9601623 -> chr.addPartyboss(BossPartyType.AKECHI_MITSUHIDE);
            }
            trackSharedDifficultyBossClear(chr, mobID, null);
        }
        if (chr.getAccount().getDailyGift() != null) {
            chr.getAccount().getDailyGift().updateMonsterCount(chr, mob.getLevel());
        }
        if (chr.hasQuest(QuestConstants.UNION_ARTIFACT)) {
            UnionArtifact.updateCommonMission(chr, mob);
            UnionArtifact.updateHuntMission(chr, mob);
        }
        if (EventConstants.HYPER_BURNING_MAX && field.getId() == EventConstants.TERA_BLINK_FIELD) {
            if (chr.hasQuest(102444)) {
                if (mob.getTemplateId() != 9834328 && mob.getTemplateId() != 9834329) {
                    // Spiegelmann's Orange Mushroom
                    // Spiegelmann's Slime
                    if (field.getMobs().size() <= 1 && !"1".equals(chr.getQRValueByKey(102444, "eliteMob"))) {
                        field.removeMobsBySvr(chr);
                        field.spawnMob(Util.succeedProp(50) ? 9834328 : 9834329, mob.getX(), mob.getY(), false, 53662500, 0);
                        sm.createQuestWithQRValue(102444, "step=2");
                    }
                } else {
                    sm.removeBlowWeather();
                    sm.blowWeather(5120243, "菁英怪物已被擊敗！請與休菲凱曼對話並繼續任務！", 4);
                    sm.createQuestWithQRValue(102444, "step=2;eliteMob=1");
                }
            } else if (chr.hasQuest(102447)) {
                if (mob.getTemplateId() != 9834330) {
                    // Spiegelmann's Black Knight
                    if (field.getMobs().size() <= 1 && !"1".equals(chr.getQRValueByKey(102447, "eliteBoss"))) {
                        field.removeMobsBySvr(chr);
                        field.spawnMob(9834330, mob.getX(), mob.getY(), false, 53662500, 0);
                    }
                } else {
                    sm.removeBlowWeather();
                    sm.blowWeather(5120243, "菁英BOSS已被擊敗！請與休菲凱曼對話並繼續任務！", 4);
                    sm.createQuestWithQRValue(102447, "eliteBoss=1;step=done;crEqp=1");
                    field.drop(new Drop(-1, GameConstants.RARE_TREASURE_CHEST), mob.getPosition());
                }
            } else if (chr.hasQuest(102448)) {
                if (mob.getTemplateId() == 9834332) {
                    // Spiegelmann's Door
                    sm.startScript(chr, 102448, "q102448e", ScriptType.Quest);
                }
            }
        }
        // Arcane Stones
        var itemID = chr.hasQuest(1473) ? Integer.parseInt(chr.getQRValueByKey(1473, "itemID")) : 0;
        if (itemID >= 2435734 && itemID <= 2435736) {
            var mobExp = mob.getExp() * ServerConfig.QUEST_EXP_RATE;
            var questID = 1470 + itemID - 2435734;
            if (!chr.hasQuestCompleted(questID + 4)) {
                long exp = Long.parseLong(chr.getQRValueByKey(questID, "exp"));
                if (exp + mobExp < 500_000_000L) {
                    chr.setQRValueByKey(questID, "exp", (Math.min(500_000_000L, (exp + mobExp)) + ""));
                } else {
                    chr.createQuestWithQRValue(questID,  "on=1;u=1;exp=500000000");
                }
            }
        }
    }

    public static void spawnHungryMutoMobs(Field field, Mob mob, ScriptManagerImpl sm, int[] mobs) {
        if (mob.getTemplateId() == 9833046 || mob.getTemplateId() == 9833066) {
            sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg7");
            field.getTimer().addEvent(() -> {
                for (Integer integer : mobs) {
                    if (integer == mob.getTemplateId()) {
                        int[] zz = xy[(int) Math.floor(Math.random() * xy.length)];
                        sm.spawnMob(mob.getTemplateId(), zz[0], zz[1], false, 0);
                        sm.showEffectToField("Map/Effect3.img/hungryMutoMsg/msg8");
                        break;
                    }
                }
            }, field.getId() == GameConstants.HUNGRY_MUTO_HARD_STAGE ? 15000 : 10000);
        } else {
            field.spawnMob(mob.getTemplateId(), mob.getHomePosition().getX(), mob.getHomePosition().getY(), true, 0);
        }
    }

    public static void spawnMobForEvolLink(Field field, int link, Position pos) {
        int mobID = 0;
        if (!Util.succeedProp(20)) {
            return;
        }
        switch (link) {
            case 4 -> {
                mobID = 9306006;
                break;
            }
            case 5 -> {
                mobID = Util.succeedProp(10) ? 9306100 : 9306005;
                break;
            }
            case 8 -> {
                mobID = 9306200;
                break;
            }
        }
        if (mobID == 0) {
            return;
        }
        field.spawnMob(mobID, pos.getX(), pos.getY(), false);
    }

    public static int getGuildRewardGradeByMobID(int mobID) {
        switch (mobID) {
            case 8800002: // Zakum (Normal)
            case 8810018: // Horntail (Normal)
            case 8880002: // Magnus (Normal)
            case 8870000: // Hilla (Normal)
            case 8900103: // Pierre (Normal)
            case 8910100: // Von Bon (Normal)
            case 8920106: // Queen (Normal)
            case VELLUM: // Vellum (Normal)
            case 9421581: // Mori Ranmaru (Normal)
            case 8840007: // Von Leon (Normal)
            case 8860000: // Arkarium (Normal)
            case 8820001: // Pink Bean (Normal)
            case 8850011: // Cygnus (Normal)
                return 0; // Quà thường
            case 8800102: // Zakum Chaos
            case 8810122: // Horntail Chaos
            case 8880000: // Magnus Hard
            case 8870100: // Hilla Hard
            case 8910000: // Chaos Von Bon
            case CHAOS_VELLUM: // Chaos Vellum
            case 8920006: // Chaos Crimson Queen
            case 8900003: // Chaos Pierre
            case 8820212: // Chaos Pink Bean
            case 9421589: // Mori Ranmaru (Hard)
            case 8840014: // Von Leon (Hard)
                return 1; // Quà Khó < 100,000
            case 8950002: // Lotus Normal
            case 8880111: // Damien Normal
            case 8950102: // Lotus Hard
            case 8880101: // Damien Hard
            case 8880177: // Final Music Box (Lucid Easy)
            case 8880167: // Final Music Box (Lucid Normal)
            case 8880156: // Final Music Box (Lucid Hard)
                return 2; // Quà Siêu khó 100,000 - 500,000
        }
        return -1;
    }

    public static void initRootAbyss(Char chr, int chestID, int reactorID, int spawnX, int spawnY) {
        if (checkInstance(chr)) {
            return;
        }

        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        Life reactor = field.getLifeByTemplateId(reactorID);
        if (reactor != null) {
            field.removeReactorByTemplateId(reactorID);
        }
        Party party = chr.getParty();
        if (chestID != 0) {
            if (!field.hasMobById(chestID)) {
                field.spawnMob(chestID, spawnX, spawnY, false);
            }
            if (party != null && party.getPartyMembersInSameFieldWithChr(chr).size() >= 2) {
                for (Char member : party.getPartyMembersInSameFieldWithChr(chr)) {
                    ScriptManagerImpl sm = member.getScriptManager();
                    if (chr.hasQuest(30009) && !chr.hasQuestCompleted(30009)) {
                        chr.completeQuest(30009); // [Root Abyss] Defeat the First Guardian
                    } else if (chr.hasQuest(30010) && !chr.hasQuestCompleted(30010)) {
                        chr.completeQuest(30010); // [Root Abyss] Defeat the Second Guardian
                    } else if (chr.hasQuest(30011) && !chr.hasQuestCompleted(30011)) {
                        chr.completeQuest(30011); // [Root Abyss] Defeat the Third Seal Guardian
                    } else if (chr.hasQuest(30012) && !chr.hasQuestCompleted(30012)) {
                        chr.completeQuest(30012); // [Root Abyss] Defeat the Final Guardian
                    }
                }
            } else {
                ScriptManagerImpl sm = chr.getScriptManager();
                if (chr.hasQuest(30009) && !chr.hasQuestCompleted(30009)) {
                    chr.completeQuest(30009); // [Root Abyss] Defeat the First Guardian
                } else if (chr.hasQuest(30010) && !chr.hasQuestCompleted(30010)) {
                    chr.completeQuest(30010); // [Root Abyss] Defeat the Second Guardian
                } else if (chr.hasQuest(30011) && !chr.hasQuestCompleted(30011)) {
                    chr.completeQuest(30011); // [Root Abyss] Defeat the Third Seal Guardian
                } else if (chr.hasQuest(30012) && !chr.hasQuestCompleted(30012)) {
                    chr.completeQuest(30012); // [Root Abyss] Defeat the Final Guardian
                }
            }
        }
    }

    public static void spawnGollux(Char chr, int phase) {
        if (checkInstance(chr)) {
            return;
        }

        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());
        int mobId = 9390600 + phase;
        Mob gollux = MobData.getMobDeepCopyById(mobId);
        int hpMultiplier = BossConstants.GOLLUX_HP_MULTIPLIERS[phase][chr.getScriptManager().getGolluxDifficulty().getVal()];
        field.spawnMob(mobId, 0, 0, false, gollux.getHp() * 5L * (long) hpMultiplier);
        //blockAttacks(chr);
    }

    public static void blockAttacks(Char chr) {
        Mob mob = null;
        Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getField().getId());

        for (int i = 9390600; i <= 9390602; i++) {
            mob = (Mob) field.getLifeByTemplateId(i);
            if (mob != null) {
                Map<String, Object> golluxMaps = chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties();
                ArrayList<Integer> blockedSkills = new ArrayList<>();
                if ((int) golluxMaps.getOrDefault(String.valueOf(BossConstants.GOLLUX_RIGHT_SHOULDER), 0) == 2) {
                    blockedSkills.addAll(Arrays.stream(BossConstants.GOLLUX_RIGHT_HAND_SKILLS).boxed().toList());
                }
                if ((int) golluxMaps.getOrDefault(String.valueOf(BossConstants.GOLLUX_LEFT_SHOULDER), 0) == 2) {
                    blockedSkills.addAll(Arrays.stream(BossConstants.GOLLUX_LEFT_HAND_SKILLS).boxed().toList());
                }
                if ((int) golluxMaps.getOrDefault(String.valueOf(BossConstants.GOLLUX_ABDOMEN), 0) == 2) {
                    blockedSkills.add(BossConstants.GOLLUX_BREATH_ATTACK);
                }
                mob.getField().broadcast(MobPool.mobAttackBlock(mob, blockedSkills));
                break;
            }
        }
    }

    public static void clearMaps(Char chr) {
        chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties().clear();
    }

    public static void openPortal(Field field, String action, int show) {
        field.broadcast(FieldPacket.golluxOpenPortal(action, show));
    }

    public static void addClearedField(Field field, Char chr) {
        chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties().put(String.valueOf(chr.getFieldID()), 2);
        updateField(field, chr);
    }

    public static void addCurrentField(Field field, Char chr) {
        int type = field.getMobs().size() == 0 ? 2 : 1;
        chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties().put(String.valueOf(chr.getFieldID()), type);
        updateField(field, chr);
    }

    public static void updateField(Field field, Char chr) {
        field.broadcast(FieldPacket.golluxUpdateMiniMap(chr));
    }

    public static boolean isAlreadyVisited(Char chr) {
        return chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties().containsKey(String.valueOf(chr.getFieldID()));
    }

    public static void setBlockAttack(Mob mob) {
        ArrayList<Integer> blockedSkills = new ArrayList<>();
        switch (mob.getTemplateId()) {
            case 8800102:
                if (mob.getPhase() != 2) {
                    mob.getField().removeMobsByTemplateID(8800117);
                    blockedSkills.add(1);
                }
                if (mob.getPhase() != 3) {
                    blockedSkills.add(2);
                    blockedSkills.add(3);
                    blockedSkills.add(4);
                    blockedSkills.add(5);
                }
                if (mob.getPhase() != 4) {
                    blockedSkills.add(6);
                    blockedSkills.add(7);
                    blockedSkills.add(8);
                }
                break;
            case 8850011:
            case 8850111:
                blockedSkills.add(4);
                break;
            case 8910000:
            case 8910100:
                if (mob.getHPPercent() > 10) {
                    blockedSkills.add(3);
                    blockedSkills.add(4);
                    blockedSkills.add(5);
                    blockedSkills.add(6);
                    blockedSkills.add(7);
                }
                if (mob.getHPPercent() > 70) {
                    blockedSkills.add(2);
                }
                break;
            case 8930000:
            case 8930100:
                if (mob.getTemplateId() == 8930100 && mob.getHPPercent() > 70) {
                    blockedSkills.add(2);
                    blockedSkills.add(3);
                    blockedSkills.add(4);
                }
                if (mob.getHPPercent() > 40) {
                    blockedSkills.add(8);
                    blockedSkills.add(9);
                    blockedSkills.add(10);
                    blockedSkills.add(12);
                    blockedSkills.add(13);
                    blockedSkills.add(14);
                    blockedSkills.add(15);
                }
                break;
            case 8880300:
            case 8880301:
            case 8880303:
            case 8880304:
            case 8880321:
            case 8880322:
            case 8880325:
            case 8880326:
            case 8880340:
            case 8880341:
            case 8880343:
            case 8880344:
            case 8880351:
            case 8880352:
            case 8880355:
            case 8880356:
                return;
        }
        mob.getField().broadcast(MobPool.mobAttackBlock(mob, blockedSkills));
    }
}
