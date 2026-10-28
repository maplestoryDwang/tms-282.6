package net.swordie.ms.connection.api.launcher.account;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@code dashboard} (session, read-only aggregate): everything the launcher's dash shows after login in one request -
 * one JVM on the CLI, one connection - built from {@link DashboardSection}s in order: {@code me}, {@code characters} +
 * {@code lastPlayed}, {@code worlds}, {@code dailyStatus}, {@code newsVersion} (P6), then {@code partial} (names of the
 * optional sections that failed; their fields are null). A required section failing fails the dashboard.
 */
public final class DashboardAction extends SessionReadAction {
    private final List<DashboardSection> sections;

    public DashboardAction() {
        this(DashboardSections.standard());
    }

    public DashboardAction(List<DashboardSection> sections) {
        this.sections = List.copyOf(Objects.requireNonNull(sections, "sections"));
    }

    @Override
    public String name() {
        return "dashboard";
    }

    public List<DashboardSection> sections() {
        return sections;
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        DashboardContext dc = new DashboardContext(ctx, request, con, userId);
        LauncherResult out = LauncherResult.ok(LauncherCodes.OK);
        List<String> partial = new ArrayList<>();
        for (DashboardSection section : sections) {
            if (section.required()) {
                LauncherResult refusal = section.contribute(dc, out);
                if (refusal != null) {
                    return refusal;
                }
                continue;
            }
            LauncherResult refusal;
            String failure;
            try {
                refusal = section.contribute(dc, out);
                failure = refusal == null ? null : refusal.code();
            } catch (Exception e) {
                failure = e.getClass().getSimpleName();
            }
            if (failure != null) {
                for (String field : section.fields()) {
                    out.with(field, null);
                }
                partial.add(section.name());
                // Section name and exception class only (CLI: stdout is discarded).
                System.out.printf("[HttpAuthApi] dashboard section %s failed: %s%n", section.name(), failure);
            }
        }
        return out.with("partial", partial);
    }
}
