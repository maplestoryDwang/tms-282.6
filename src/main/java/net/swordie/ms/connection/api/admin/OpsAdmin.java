package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.world.field.Field;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operations tabs of the console (ops-1007): who is online, the process's health, the grouped problem lines of
 * {@link IssueCapture}, and one character's state with the two repairs a tester needs most (back to town, clear
 * cooldowns). One nested {@link AdminAction} per command, like {@link GiftAdmin}.
 */
public final class OpsAdmin {
    private OpsAdmin() {
    }

    /** An online character by id or exact name (case-insensitive), or null. */
    static Char online(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String q = query.trim();
        Map<Integer, Char> chars = Server.get().getChars();
        try {
            Char byId = chars.get(Integer.parseInt(q));
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
            // a name
        }
        for (Char c : new ArrayList<>(chars.values())) {
            if (c != null && q.equalsIgnoreCase(c.getName())) {
                return c;
            }
        }
        return null;
    }

    static Map<String, Object> row(Char c) {
        Map<String, Object> m = new LinkedHashMap<>();
        Client client = c.getClient();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("job", (int) c.getJob());
        m.put("level", (int) c.getLevel());
        m.put("field", c.getFieldID());
        m.put("channel", client == null ? 0 : (int) client.getChannel());
        m.put("user", c.getUser() == null ? 0 : c.getUser().getId());
        m.put("ip", client == null ? "" : String.valueOf(client.getIP()));
        return m;
    }

    /** {@code ops.online}: every character in the game world. */
    public static final class Online implements AdminAction {
        @Override
        public String name() {
            return "ops.online";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Char c : new ArrayList<>(Server.get().getChars().values())) {
                if (c != null) {
                    rows.add(row(c));
                }
            }
            rows.sort(Comparator.comparingInt(r -> (Integer) r.get("id")));
            return LauncherResult.ok(AdminCodes.OK).with("characters", rows).with("users", ctx.env().server().onlineUsers())
                    .with("now", ctx.env().now());
        }
    }

    /** {@code ops.kick {character}}: closes that character's connection (Client.close saves). */
    public static final class Kick implements AdminAction {
        @Override
        public String name() {
            return "ops.kick";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public String auditSummary(JsonNode request) {
            return "character=" + AdminParams.text(request, "character");
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            Char c = online(AdminParams.text(request, "character"));
            if (c == null || c.getClient() == null) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND);
            }
            int id = c.getId();
            c.getClient().close();
            return LauncherResult.ok(AdminCodes.OK).with("character", id);
        }
    }

    /** {@code ops.health}: heap, threads, garbage collection, uptime, connections. */
    public static final class Health implements AdminAction {
        @Override
        public String name() {
            return "ops.health";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            long gcCount = 0;
            long gcMs = 0;
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                gcCount += Math.max(0, gc.getCollectionCount());
                gcMs += Math.max(0, gc.getCollectionTime());
            }
            var threads = ManagementFactory.getThreadMXBean();
            return LauncherResult.ok(AdminCodes.OK)
                    .with("pid", ProcessHandle.current().pid())
                    .with("uptimeSec", ctx.env().server().uptimeSec())
                    .with("heapUsedMb", heap.getUsed() >> 20)
                    .with("heapCommittedMb", heap.getCommitted() >> 20)
                    .with("heapMaxMb", heap.getMax() < 0 ? 0 : heap.getMax() >> 20)
                    .with("threads", threads.getThreadCount())
                    .with("peakThreads", threads.getPeakThreadCount())
                    .with("gcCount", gcCount)
                    .with("gcMs", gcMs)
                    .with("processors", Runtime.getRuntime().availableProcessors())
                    .with("loadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
                    .with("onlineUsers", ctx.env().server().onlineUsers())
                    .with("onlineCharacters", ctx.env().server().onlineCharacters())
                    .with("connections", Server.get().getClients().size())
                    .with("now", ctx.env().now());
        }
    }

    /** {@code ops.issues {minutes?, character?}}: grouped problem lines, newest first. */
    public static final class Issues implements AdminAction {
        @Override
        public String name() {
            return "ops.issues";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            IssueCapture capture = IssueCapture.current();
            if (capture == null) {
                return LauncherResult.fail(AdminCodes.UNAVAILABLE);
            }
            Long minutes = AdminParams.number(request, "minutes", 0, 100_000);
            Long character = AdminParams.number(request, "character", 0, Integer.MAX_VALUE);
            long since = minutes == null || minutes == 0 ? 0 : ctx.env().now() - minutes * 60_000L;
            List<Map<String, Object>> rows = new ArrayList<>();
            for (IssueCapture.Issue i : capture.snapshot(since, character == null ? 0 : character.intValue())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("kind", i.kind());
                m.put("subject", i.subject());
                m.put("character", i.character());
                m.put("skill", i.skill());
                m.put("reason", i.reason());
                m.put("count", i.count());
                m.put("first", i.firstMs());
                m.put("last", i.lastMs());
                m.put("sample", i.sample());
                rows.add(m);
                if (rows.size() >= 400) {
                    break;
                }
            }
            return LauncherResult.ok(AdminCodes.OK).with("issues", rows).with("clearedAt", capture.clearedAt())
                    .with("now", ctx.env().now());
        }
    }

    /** {@code ops.issuesClear}: forget the groups collected so far (before a new test). */
    public static final class IssuesClear implements AdminAction {
        @Override
        public String name() {
            return "ops.issuesClear";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            IssueCapture capture = IssueCapture.current();
            if (capture == null) {
                return LauncherResult.fail(AdminCodes.UNAVAILABLE);
            }
            capture.clear();
            return LauncherResult.ok(AdminCodes.OK);
        }
    }

    /** {@code char.inspect {character}}: an online character's state (job, level, map, HP/MP, skills, cooldowns). */
    public static final class Inspect implements AdminAction {
        @Override
        public String name() {
            return "char.inspect";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            Char c = online(AdminParams.text(request, "character"));
            if (c == null) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND);
            }
            long now = System.currentTimeMillis();
            List<Map<String, Object>> skills = new ArrayList<>();
            for (Skill s : new ArrayList<>(c.getSkills())) {
                if (s == null) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getSkillId());
                m.put("level", s.getCurrentLevel());
                m.put("master", s.getMasterLevel());
                skills.add(m);
            }
            skills.sort(Comparator.comparingInt(m -> (Integer) m.get("id")));
            List<Map<String, Object>> cooldowns = new ArrayList<>();
            c.getSkillCoolTimes().forEach((skill, until) -> {
                if (until > now) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("skill", skill);
                    m.put("leftSec", (until - now + 999) / 1000);
                    cooldowns.add(m);
                }
            });
            List<Map<String, Object>> links = new ArrayList<>();
            if (c.getAccount() != null && c.getAccount().getLinkSkills() != null) {
                for (var ls : new ArrayList<>(c.getAccount().getLinkSkills())) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("owner", ls.getOwnerID());
                    m.put("skill", ls.getLinkSkillID());
                    m.put("level", ls.getLevel());
                    m.put("linkedTo", ls.getLinkCount());
                    links.add(m);
                }
            }
            return LauncherResult.ok(AdminCodes.OK).with("character", row(c))
                    .with("hp", c.getHP()).with("maxHp", c.getMaxHP()).with("mp", c.getMP()).with("maxMp", c.getMaxMP())
                    .with("skills", skills).with("cooldowns", cooldowns).with("linkSkills", links);
        }
    }

    /** {@code char.home {character}}: warps an online character to the home town (stuck in an event map). */
    public static final class Home implements AdminAction {
        @Override
        public String name() {
            return "char.home";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public String auditSummary(JsonNode request) {
            return "character=" + AdminParams.text(request, "character");
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            Char c = online(AdminParams.text(request, "character"));
            if (c == null) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND);
            }
            Field town = c.getOrCreateFieldByCurrentInstanceType(FieldConstants.HOME_MAP);
            if (town == null) {
                return LauncherResult.fail(AdminCodes.UNAVAILABLE);
            }
            int from = c.getFieldID();
            c.warp(town);
            return LauncherResult.ok(AdminCodes.OK).with("character", c.getId()).with("from", from)
                    .with("to", FieldConstants.HOME_MAP);
        }
    }

    /** {@code char.cooldowns {character}}: clears every running skill cooldown of an online character. */
    public static final class Cooldowns implements AdminAction {
        @Override
        public String name() {
            return "char.cooldowns";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public String auditSummary(JsonNode request) {
            return "character=" + AdminParams.text(request, "character");
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            Char c = online(AdminParams.text(request, "character"));
            if (c == null) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND);
            }
            long now = System.currentTimeMillis();
            List<Integer> running = new ArrayList<>();
            c.getSkillCoolTimes().forEach((skill, until) -> {
                if (until > now) {
                    running.add(skill);
                }
            });
            running.forEach(c::resetSkillCoolTime);
            return LauncherResult.ok(AdminCodes.OK).with("character", c.getId()).with("cleared", running.size());
        }
    }
}
