package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.HyperStat;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.user.UserStatHandler;
import net.swordie.ms.loaders.SkillData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 282 極限屬性 (hyper stat) and hyper skill windows (research/tms2823/hyperstat-282/REPORT.md). Every sender locks
 * the client after sending (5E04DA0(ctx, 1)); each request ends with {@code chr.dispose()} (LP009D unlock), also when
 * it is refused. The 265 replies reach the client through {@code Tms282SkillLegacyTranslators} (LP00A2 skill record,
 * LP0269 presets, LP01FA / LP014E reset results).
 * <ul>
 *     <li>CP0239 (5EDFBF0): i32 tick, i32 skillId - hyper skill up; checked here (hyper SP of its kind, max level),
 *     then the 265 handler.</li>
 *     <li>CP023A (5EDFCA0): i32 tick, u8, u8 - hyper skill reset. 265 refunds the points into the job SP; 282 counts
 *     hyper SP by level (LP0121 "hyper"), so only the skills go back to 0.</li>
 *     <li>CP023B (5EDFD60): i32 tick, i32 preset, i32 skillId, i32 amount - hyper stat up.</li>
 *     <li>CP023C (5EDFE40): i32 tick, i32 preset - hyper stat reset (10m mesos).</li>
 *     <li>CP023D (5EDFF00): i32 tick, i32 preset - change the active preset.</li>
 * </ul>
 * The 265 stat handlers accept any preset (their range check is never true) and write the preset's level into the
 * active skill even for another preset; the stat operations are done here instead. A character that reached 140 in
 * this session has no hyper stat rows yet (265 creates them at login): they are created on the first request.
 */
final class Tms282HyperStatHandlers {
    static final int HYPER_SKILL_UP = 0x0239;
    static final int HYPER_SKILL_RESET = 0x023A;
    static final int HYPER_STAT_UP = 0x023B;
    static final int HYPER_STAT_RESET = 0x023C;
    static final int HYPER_STAT_PRESET = 0x023D;
    static final int PRESETS = 3;

    void register(Tms282Routes routes) {
        routes.channel(HYPER_SKILL_UP, InHeader.USER_HYPER_SKILL_UP_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("hyper skill up -> LP00A2 + LP009D", this::onHyperSkillUp);
        routes.channel(HYPER_SKILL_RESET, InHeader.USER_HYPER_SKILL_RESET_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("hyper skill reset -> LP00A2 + LP014E", this::onHyperSkillReset);
        routes.channel(HYPER_STAT_UP, InHeader.USER_HYPER_STAT_UP_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("hyper stat up -> LP0269 (+ LP00A2)", this::onHyperStatUp);
        routes.channel(HYPER_STAT_RESET, InHeader.USER_HYPER_STAT_RESET_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("hyper stat reset -> LP0269 + LP01FA", this::onHyperStatReset);
        routes.channel(HYPER_STAT_PRESET, InHeader.USER_HYPER_STAT_CHANGE_PRESET_REQUEST).when(Tms282Guards.IN_FIELD)
                .handle("hyper stat preset -> LP00A2 + LP0269", this::onHyperStatPreset);
    }

    // ---- CP0239 / CP023A ---------------------------------------------------------------------------------------

    void onHyperSkillUp(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        ByteBuffer in = body(packet, 8);
        if (in == null) {
            refuse(session, chr, op, packet, "length");
            return;
        }
        in.getInt(); // tick
        int skillId = in.getInt();
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (si == null || (si.getHyper() != 1 && si.getHyper() != 2)) {
            refuse(session, chr, op, packet, "skill " + skillId + " is no hyper skill");
            return;
        }
        if (!SkillConstants.isHyperSkill(chr.getJob(), skillId) && !SkillConstants.isMatching(si.getRootId(), chr.getJob())) {
            refuse(session, chr, op, packet, "skill " + skillId + " is not of job " + chr.getJob());
            return;
        }
        if (si.isInvisible() || !net.swordie.ms.client.character.tms282.Tms282SkillLearning.belongs(chr, si.getRootId())
                || !net.swordie.ms.client.character.tms282.Tms282SkillLearning.requirementsMet(chr, si)) {
            refuse(session, chr, op, packet, "hyper skill level/job/prerequisite requirement is not met");
            return;
        }
        Skill current = chr.getSkill(skillId);
        int level = current == null ? 0 : current.getCurrentLevel();
        if (si.getMaxLevel() > 0 && level >= si.getMaxLevel()) {
            refuse(session, chr, op, packet, "skill " + skillId + " already at max level " + level);
            return;
        }
        boolean active = si.getHyper() == 2;
        int total = hyperSkillSp(chr.getLevel(), active);
        int spent = active ? chr.getSpentActiveHyperSkillSp() : chr.getSpentPassiveHyperSkillSp();
        if (spent >= total) {
            refuse(session, chr, op, packet, (active ? "active" : "passive") + " hyper SP " + spent + "/" + total);
            return;
        }
        UserStatHandler.handleUserHyperSkillUpRequest(chr, new InPacket(java.util.Arrays.copyOfRange(packet, 2, 10)));
        log(chr, op, "hyper skill " + skillId + " " + level + " -> " + (level + 1));
    }

    void onHyperSkillReset(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        if (body(packet, 6) == null) {
            refuse(session, chr, op, packet, "length");
            return;
        }
        List<Skill> reset = new ArrayList<>();
        for (Skill skill : new ArrayList<>(chr.getSkills())) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (si != null && (si.getHyper() == 1 || si.getHyper() == 2) && skill.getCurrentLevel() > 0) {
                reset.add(skill);
            }
        }
        if (chr.getLevel() < 140 || reset.isEmpty()) {
            refuse(session, chr, op, packet, "nothing to reset");
            return;
        }
        if (chr.getMoney() < GameConstants.HYPER_SKILL_RESET_COST) {
            chr.chatPopup(String.format("楓幣不足，無法重置。（需要：%,d 楓幣）", GameConstants.HYPER_SKILL_RESET_COST));
            refuse(session, chr, op, packet, "mesos");
            return;
        }
        chr.deductMoney(GameConstants.HYPER_SKILL_RESET_COST);
        for (Skill skill : reset) {
            chr.removeFromBaseStatCache(skill);
            skill.setCurrentLevel(0);
            chr.addSkill(skill);
        }
        chr.write(WvsContext.changeSkillRecordResult(reset, true, false, false));
        chr.write(WvsContext.receiveHyperSkillResetResult(chr.getId(), true, true));
        chr.dispose();
        log(chr, op, "hyper skills reset " + reset.stream().map(Skill::getSkillId).toList());
    }

    /** The hyper skill SP of one kind up to {@code level} (the LP0121 "hyper" table summed). */
    static int hyperSkillSp(int level, boolean active) {
        int sp = 0;
        for (int lv = 140; lv <= level; lv++) {
            sp += active ? SkillConstants.getHyperActiveSkillSpByLv(lv) : SkillConstants.getHyperPassiveSkillSpByLv(lv);
        }
        return sp;
    }

    // ---- CP023B / CP023C / CP023D ------------------------------------------------------------------------------

    void onHyperStatUp(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        ByteBuffer in = body(packet, 16);
        if (in == null) {
            refuse(session, chr, op, packet, "length");
            return;
        }
        in.getInt(); // tick
        int preset = in.getInt();
        int skillId = in.getInt();
        int amount = in.getInt();
        if (preset < 0 || preset >= PRESETS || amount < 1 || !SkillConstants.isHyperStat(skillId) || chr.getLevel() < 140) {
            refuse(session, chr, op, packet, "preset " + preset + " skill " + skillId + " amount " + amount);
            return;
        }
        ensureHyperStats(chr);
        HyperStat row = chr.getHyperStats(preset, skillId);
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (row == null || si == null) {
            refuse(session, chr, op, packet, "no hyper stat row / data for " + skillId);
            return;
        }
        int from = row.getSkillLevel();
        int to = from + amount;
        int max = si.getMaxLevel() > 0 ? si.getMaxLevel() : 15;
        int cost = SkillConstants.getTotalNeededSpForHyperStatSkill(to) - SkillConstants.getTotalNeededSpForHyperStatSkill(from);
        int available = SkillConstants.getTotalHyperStatSpByLevel(chr.getLevel()) - chr.getSpentHyperSp(preset);
        if (to > max || cost <= 0 || cost > available) {
            refuse(session, chr, op, packet, "level " + from + " -> " + to + " (max " + max + "), cost " + cost + ", available " + available);
            return;
        }
        row.setSkillLevel(to);
        if (preset == activePreset(chr)) {
            Skill skill = setSkillLevel(chr, skillId, to);
            chr.write(WvsContext.changeSkillRecordResult(skill));
            if (skillId == Job.HYPER_STAT_ARCANE_FORCE && chr.getJobHandler() != null) {
                chr.getJobHandler().giveHyperArcaneForceBuff();
            }
        }
        chr.write(WvsContext.updateHyperPresets(chr, preset, (byte) 1));
        chr.dispose();
        log(chr, op, "hyper stat preset " + preset + " skill " + skillId + " " + from + " -> " + to + " (cost " + cost + ", left " + (available - cost) + ")");
    }

    void onHyperStatReset(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        ByteBuffer in = body(packet, 8);
        if (in == null) {
            refuse(session, chr, op, packet, "length");
            return;
        }
        in.getInt(); // tick
        int preset = in.getInt();
        if (preset < 0 || preset >= PRESETS || chr.getLevel() < 140) {
            refuse(session, chr, op, packet, "preset " + preset);
            return;
        }
        if (chr.getMoney() < GameConstants.HYPER_STAT_RESET_COST) {
            chr.chatPopup(String.format("楓幣不足，無法重置。（需要：%,d 楓幣）", GameConstants.HYPER_STAT_RESET_COST));
            refuse(session, chr, op, packet, "mesos");
            return;
        }
        ensureHyperStats(chr);
        boolean active = preset == activePreset(chr);
        chr.deductMoney(GameConstants.HYPER_STAT_RESET_COST);
        List<Skill> changed = new ArrayList<>();
        for (HyperStat row : chr.getHyperStats(preset)) {
            if (row.getSkillLevel() == 0) {
                continue;
            }
            row.setSkillLevel(0);
            if (active) {
                changed.add(setSkillLevel(chr, row.getSkillID(), 0));
            }
        }
        if (!changed.isEmpty()) {
            chr.write(WvsContext.changeSkillRecordResult(changed, true, false, false));
        }
        chr.write(WvsContext.updateHyperPresets(chr, preset, (byte) 1));
        chr.write(WvsContext.receiveHyperStatResetResult(chr.getId(), true, true));
        chr.dispose();
        log(chr, op, "hyper stat preset " + preset + " reset" + (active ? " (active)" : ""));
    }

    void onHyperStatPreset(Tms282LoginState session, int op, byte[] packet) {
        Char chr = session.fieldCharacter();
        ByteBuffer in = body(packet, 8);
        if (in == null) {
            refuse(session, chr, op, packet, "length");
            return;
        }
        in.getInt(); // tick
        int preset = in.getInt();
        if (preset < 0 || preset >= PRESETS || chr.getLevel() < 140) {
            refuse(session, chr, op, packet, "preset " + preset);
            return;
        }
        ensureHyperStats(chr);
        List<Skill> changed = new ArrayList<>();
        for (HyperStat row : chr.getHyperStats(preset)) {
            Skill skill = chr.getSkill(row.getSkillID());
            int level = skill == null ? 0 : skill.getCurrentLevel();
            if (level != row.getSkillLevel()) {
                changed.add(setSkillLevel(chr, row.getSkillID(), row.getSkillLevel()));
            }
        }
        chr.createQuestWithQRValue(QuestConstants.HYPER_STATS_PRESET, "hyperstats=" + preset);
        if (!changed.isEmpty()) {
            chr.write(WvsContext.changeSkillRecordResult(changed, true, false, false));
        }
        if (changed.stream().anyMatch(s -> s.getSkillId() == Job.HYPER_STAT_ARCANE_FORCE) && chr.getJobHandler() != null) {
            chr.getJobHandler().giveHyperArcaneForceBuff();
        }
        chr.write(WvsContext.updateHyperPresets(chr, preset, (byte) 0));
        chr.dispose();
        log(chr, op, "hyper stat preset -> " + preset + ", " + changed.size() + " skills changed");
    }

    // ---- shared ------------------------------------------------------------------------------------------------

    /** 265 creates the rows at login from level 140; a character that got there in this session gets them now. */
    static void ensureHyperStats(Char chr) {
        if (chr.getLevel() >= 140 && chr.getHyperStats().isEmpty()) {
            chr.initHyperStats();
        }
    }

    static int activePreset(Char chr) {
        String value = chr.getQRValueByKey(QuestConstants.HYPER_STATS_PRESET, "hyperstats");
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** The skill at {@code level}, the base stat cache moved with it (as the 265 handlers do). */
    static Skill setSkillLevel(Char chr, int skillId, int level) {
        Skill skill = SkillData.getSkillDeepCopyById(skillId);
        skill.setCurrentLevel(level);
        chr.addSkill(skill);
        return skill;
    }

    /** The body after the opcode when it is exactly {@code length} bytes, else null. */
    static ByteBuffer body(byte[] packet, int length) {
        if (packet.length != 2 + length) {
            return null;
        }
        return ByteBuffer.wrap(packet, 2, length).slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void refuse(Tms282LoginState session, Char chr, int op, byte[] packet, String why) {
        session.send(Tms282StatPackets.unlock());
        System.out.printf("[TMS282] REFUSED CP%04X len=%d character=%s: %s; SENT LP009D unlock%n", op, packet.length,
                chr == null ? "-" : chr.getId(), why);
    }

    private static void log(Char chr, int op, String what) {
        System.out.printf("[TMS282] CP%04X %s character=%d%n", op, what, chr.getId());
    }
}
