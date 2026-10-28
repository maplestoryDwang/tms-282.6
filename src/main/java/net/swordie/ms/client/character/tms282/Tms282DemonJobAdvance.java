package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.jobs.resistance.demon.DemonSlayer;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.scripts.ScriptManagerImpl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 282 automatic job advancement for the Demon branch (demon-0930). {@link Tms282JobAdvance} left Demon Slayer out
 * because its {@code handleLevelUp} override only grants honor / max DF again, and Demon Avenger's override is empty:
 * neither ever changed the job (265 advances Demons only through the Mastema quest chain / the skip dialog).
 * <p>
 * The steps mirror the official 282 quests (Quest.wz 23210-23221, end scripts q23212e / q23214e / q23215e /
 * q23221e): Lv.30 23210-23212 -> 3110 / 3120, Lv.60 23213 + 23214 (DS) / 23218 (DA) and QR 23206 -> 3111 / 3121,
 * Lv.100 23215 (DS) / 23221 (DA) -> 3112 / 3122; each gives the job medal and the next Demon aegis. The quest fights
 * are skipped (as every other 282 auto advance), SP like the other 282 auto advances (+5 previous, +3 new job).
 * A Demon Slayer's base max DF follows the job ({@link DemonSlayer#baseMaxFury}).
 */
public final class Tms282DemonJobAdvance {
    private static final int DEMON_QR = 23206;

    /** One advancement: job {@code from} -> {@code to} from character level {@code level}. */
    record Step(short from, short to, int level, int medal, int aegis, int[] quests, String qr) {
        Step(int from, int to, int level, int medal, int aegis, int[] quests, String qr) {
            this((short) from, (short) to, level, medal, aegis, quests, qr);
        }
    }

    static final List<Step> STEPS = List.of(
            // Demon Slayer
            new Step(3100, 3110, 30, 1142342, 1099002, new int[]{23210, 23211, 23212}, null),
            new Step(3110, 3111, 60, 1142343, 1099003, new int[]{23213, 23214}, "1"),
            new Step(3111, 3112, 100, 1142344, 1099004, new int[]{23215}, null),
            // Demon Avenger
            new Step(3101, 3120, 30, 1142554, 1099007, new int[]{23210, 23211, 23212}, null),
            new Step(3120, 3121, 60, 1142555, 1099008, new int[]{23213, 23218}, "1"),
            new Step(3121, 3122, 100, 1142556, 1099009, new int[]{23221}, null));

    /** Standard Demon aegis ids by tier (Slayer 1099000/2/3/4, Avenger 1099006/7/8/9); index = tier. */
    private static final int[] SLAYER_AEGIS = {1099000, 1099002, 1099003, 1099004};
    private static final int[] AVENGER_AEGIS = {1099006, 1099007, 1099008, 1099009};

    private Tms282DemonJobAdvance() {
    }

    /** True for every Demon job (3001, 3100-3112, 3101-3122): these never go through the 265 job handler. */
    public static boolean handles(short job) {
        return JobConstants.isDemon(job);
    }

    static Step stepFrom(short job) {
        for (Step step : STEPS) {
            if (step.from() == job) {
                return step;
            }
        }
        return null;
    }

    /**
     * Runs every Demon advancement the character qualifies for up to {@code upToLevel} (also a missed earlier one:
     * a Demon Slayer still 3100 at Lv.60 goes 3110 then 3111). Returns the number of advancements.
     */
    public static int advance(Char chr, int upToLevel) {
        int count = 0;
        int limit = Math.min(upToLevel, chr.getLevel());
        for (int guard = 0; guard < STEPS.size(); guard++) {
            Step step = stepFrom(chr.getJob());
            if (step == null || step.level() > limit) {
                break;
            }
            apply(chr, step);
            if (chr.getJob() != step.to()) {
                break; // setJob refused
            }
            count++;
        }
        return count;
    }

    private static void apply(Char chr, Step step) {
        ScriptManagerImpl sm = chr.getScriptManager();
        sm.setJob(step.to()); // new handler; DemonSlayer's constructor raises the base max DF (silent)
        if (chr.getJob() != step.to()) {
            return;
        }
        for (int quest : step.quests()) {
            if (!chr.hasQuestCompleted(quest)) {
                sm.completeQuestNoRewards(quest);
            }
        }
        if (step.qr() != null) {
            chr.addQRValue(DEMON_QR, step.qr());
        }
        sm.addSPJobAdv(step.from(), 5);
        sm.addSPJobAdv(step.to(), 3);
        giveAegis(chr, sm, step.aegis());
        if (!chr.hasItem(step.medal()) && chr.canHold(step.medal())) {
            sm.giveItem(step.medal());
        }
        if (JobConstants.isDemonSlayer(step.to())) {
            DemonSlayer.raiseMaxFury(chr);
            Map<Stat, Object> stats = new EnumMap<>(Stat.class);
            stats.put(Stat.mmp, chr.getStat(Stat.mmp));
            chr.sendStatsPacket(stats);
        }
        System.out.println("[TMS282] DEMON JOB ADVANCE character=" + chr.getId() + " level " + chr.getLevel() + ": "
                + step.from() + " -> " + step.to());
    }

    /**
     * Equips the new aegis only in place of nothing or a lower standard Demon aegis of the same branch (the old one
     * goes back to the equip inventory, so there must be a free slot); otherwise it is put in the inventory.
     */
    private static void giveAegis(Char chr, ScriptManagerImpl sm, int aegis) {
        int[] line = contains(SLAYER_AEGIS, aegis) ? SLAYER_AEGIS : AVENGER_AEGIS;
        int tier = indexOf(line, aegis);
        try {
            int bodyPart = ItemConstants.getBodyPartFromItem(aegis, chr.getAvatarData().getAvatarLook().getGender());
            Item current = chr.getEquippedInventory().getItemBySlot((short) bodyPart);
            int currentTier = current == null ? -1 : indexOf(line, current.getItemId());
            boolean replaceable = current == null
                    || (currentTier >= 0 && currentTier < tier && chr.getEquipInventory().getEmptySlots() > 0);
            if (replaceable) {
                sm.giveAndEquip(aegis);
                return;
            }
            if (current != null && current.getItemId() == aegis) {
                return;
            }
        } catch (RuntimeException e) {
            System.out.println("[TMS282] demon aegis " + aegis + " not equipped for character " + chr.getId() + ": " + e);
        }
        if (chr.canHold(aegis)) {
            sm.giveItem(aegis);
        }
    }

    private static boolean contains(int[] ids, int id) {
        return indexOf(ids, id) >= 0;
    }

    private static int indexOf(int[] ids, int id) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == id) {
                return i;
            }
        }
        return -1;
    }
}
