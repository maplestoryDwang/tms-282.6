package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.world.tms282.newbie.NewbieFlowConfig;
import net.swordie.ms.world.tms282.newbie.NewbieFlowService;
import net.swordie.ms.world.tms282.newbie.NewbieFlowSource;
import net.swordie.ms.world.tms282.newbie.NewbieProgress;
import net.swordie.ms.world.tms282.newbie.NewbieStep;
import net.swordie.ms.world.tms282.newbie.NewbieStore;
import net.swordie.ms.world.tms282.newbie.steps.FirstJobStep;
import net.swordie.ms.world.tms282.newbie.steps.SpawnStep;
import net.swordie.ms.world.tms282.newbie.steps.StarterGiftStep;
import net.swordie.ms.world.tms282.newbie.steps.StoryDialogStep;
import net.swordie.ms.world.tms282.newbie.steps.WarpMenuStep;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新手流程 admin actions (step 7): {@code newbie.get} (the configuration in use, its state and file),
 * {@code newbie.progress} (characters' rows, newest first), {@code newbie.reload} (read the file again now).
 */
final class NewbieAdmin {
    private NewbieAdmin() {
    }

    static final class Jobs implements AdminAction {
        public String name() { return "newbie.jobs"; }
        public boolean mutating() { return false; }
        public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
            List<Map<String,Object>> rows = new ArrayList<>();
            String sql = "SELECT cs.job, COUNT(*) FROM characters c JOIN avatardata a ON a.id=c.avatardata "
                    + "JOIN characterstats cs ON cs.id=a.characterstat GROUP BY cs.job ORDER BY COUNT(*) DESC,cs.job";
            try (var con=net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection(); var p=con.prepareStatement(sql); var r=p.executeQuery()) {
                while(r.next()) rows.add(Map.of("job", r.getInt(1), "name", net.swordie.ms.constants.JobNameCatalog.lineName(r.getInt(1)), "count", r.getInt(2)));
            }
            return LauncherResult.ok(AdminCodes.OK).with("jobs", rows);
        }
    }

    static Map<String, Object> status(NewbieFlowSource.Status s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("state", s.state());
        m.put("problem", s.problem() == null ? "" : s.problem());
        m.put("file", s.file());
        m.put("loadedAt", s.loadedAt());
        m.put("warnings", s.warnings());
        NewbieFlowConfig c = s.config();
        m.put("version", c.version());
        m.put("enabled", c.enabled());
        m.put("lobbyMap", c.lobbyMap());
        m.put("lobbyPortal", c.lobbyPortal());
        m.put("speakerNpc", c.speakerNpc());
        m.put("popupDelayMs", c.popupDelayMs());
        m.put("resumeOnLogin", c.resumeOnLogin());
        m.put("summary", c.summary());
        List<Map<String, Object>> steps = new ArrayList<>();
        for (NewbieStep st : c.steps()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", st.id());
            x.put("type", st.type());
            x.put("detail", detail(st, c));
            steps.add(x);
        }
        m.put("steps", steps);
        return m;
    }

    static String detail(NewbieStep st, NewbieFlowConfig c) {
        if (st instanceof SpawnStep s) {
            String def = s.defaultTarget() == SpawnStep.LOBBY ? "新手大廳 " + c.lobbyMap()
                    : s.defaultTarget() == SpawnStep.ORIGINAL ? "原本的開場地圖" : String.valueOf(s.defaultTarget());
            return "預設：" + def + (s.byLoginJob().isEmpty() ? "" : "；例外 " + s.byLoginJob());
        }
        if (st instanceof StoryDialogStep s) {
            return s.pagesFor("").size() + " 頁" + (s.byLoginJob().isEmpty() ? "" : "；依種族 " + s.byLoginJob().keySet());
        }
        if (st instanceof FirstJobStep s) {
            return s.rules().size() + " 種初始職業，等級補到 " + s.levelTo();
        }
        if (st instanceof StarterGiftStep s) {
            return "楓幣 " + s.meso() + "，共通 " + s.items().size() + " 項，裝備組 " + s.equipment().keySet()
                    + (s.nxPrepaid() > 0 ? "；樂豆點 " + s.nxPrepaid() + "（登入帳號一次）" : "")
                    + (s.maxClaimsPerUser() > 0 ? "，每個登入帳號最多 " + s.maxClaimsPerUser() + " 次" : "");
        }
        if (st instanceof WarpMenuStep s) {
            List<String> d = new ArrayList<>();
            s.destinations().forEach(x -> d.add(x.label() + " " + x.map()));
            return String.join("、", d);
        }
        return "";
    }

    static String fileText(NewbieFlowSource.Status s) {
        try {
            Path p = Path.of(s.file());
            if (!Files.isRegularFile(p) || Files.size(p) > NewbieFlowConfig.MAX_FILE_BYTES) return "";
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    static long ms(Instant i) {
        return i == null ? 0 : i.toEpochMilli();
    }

    // ------------------------------------------------------------------ newbie.get

    static final class Get implements AdminAction {
        @Override
        public String name() {
            return "newbie.get";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            NewbieFlowSource.Status s = NewbieFlowService.get().status();
            return LauncherResult.ok(AdminCodes.OK).with("flow", status(s)).with("fileText", fileText(s));
        }
    }

    // ------------------------------------------------------------------ newbie.reload

    static final class Reload implements AdminAction {
        @Override
        public String name() {
            return "newbie.reload";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            NewbieFlowSource.Status s = NewbieFlowService.get().reload();
            return LauncherResult.ok(AdminCodes.OK).with("flow", status(s)).with("fileText", fileText(s));
        }
    }

    // ------------------------------------------------------------------ newbie.progress

    static final class Progress implements AdminAction {
        @Override
        public String name() {
            return "newbie.progress";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
            String q = AdminParams.text(request, "query").strip();
            if (q.length() > 32) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "newbie.query");
            }
            Long limit = AdminParams.number(request, "limit", 1, 500);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (NewbieStore.Row r : NewbieFlowService.get().progress(q, limit == null ? 200 : limit.intValue())) {
                NewbieProgress p = r.progress();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("characterId", p.characterId());
                m.put("name", r.name() == null ? "" : r.name());
                m.put("level", r.level());
                m.put("job", r.job());
                m.put("posMap", r.posMap());
                m.put("loginJob", p.loginJob());
                m.put("stepId", p.stepId());
                m.put("branch", p.branch());
                m.put("giftClaimedAt", ms(p.giftClaimedAt()));
                m.put("jobDoneAt", ms(p.jobDoneAt()));
                m.put("completedAt", ms(p.completedAt()));
                m.put("createdAt", ms(p.createdAt()));
                m.put("updatedAt", ms(p.updatedAt()));
                rows.add(m);
            }
            return LauncherResult.ok(AdminCodes.OK).with("rows", rows);
        }
    }
}
