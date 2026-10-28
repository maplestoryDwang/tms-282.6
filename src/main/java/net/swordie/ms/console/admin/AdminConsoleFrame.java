package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.console.ConsoleUiStyle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;
import java.util.Optional;

/**
 * The admin console window: a header with the server state (and a retry button when it is not running), the eight tabs
 * and a status line. Closing it never affects the server. While the server is down every 5 s a {@code server.ping}
 * checks whether it came back; while it runs the visible monitor tab refreshes every 5 s.
 */
final class AdminConsoleFrame implements AdminConsoleContext.StatusSink {
    static final int WIDTH = 1240;
    static final int HEIGHT = 800;

    private final JPanel content = AdminUi.panel(new BorderLayout());
    private final JLabel serverState = new JLabel("連線中…");
    private final JLabel status = AdminUi.hint(" ");
    private final JButton retry = AdminUi.button("重新連線");
    private final JCheckBox auto = new JCheckBox("自動更新（5 秒）", true);
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<AdminTab> tabList = new ArrayList<>();
    private final AdminConsoleContext ctx;
    private final ApiMonitorTab api;
    private boolean up;
    private long pid;

    AdminConsoleFrame(AdminGateway gateway, boolean synchronous, LongFunction<Optional<Boolean>> alive) {
        ctx = new AdminConsoleContext(gateway, synchronous, this);
        ctx.dialogParent(content);
        api = new ApiMonitorTab(ctx);
        tabList.add(api);
        tabList.add(new AccountTab(ctx));
        tabList.add(new NewsTab(ctx));
        tabList.add(new DailyTab(ctx));
        tabList.add(new NetSecurityTab(ctx));
        tabList.add(new SaveShutdownTab(ctx, alive));
        tabList.add(new ReleaseTab(ctx));
        tabList.add(new GiftCodeTab(ctx));
        tabList.add(new NewbieTab(ctx));
        tabList.add(new OnlineRewardTab(ctx));
        // ops-1007: health + who is online, grouped problem lines, one character's state and repairs, deployment
        tabList.add(new OpsTab(ctx));
        tabList.add(new IssuesTab(ctx));
        tabList.add(new CharacterTab(ctx));
        tabList.add(new DeployTab(ctx, java.nio.file.Path.of(System.getProperty(DeployTab.HOME_KEY, "")).toAbsolutePath()));
        // launcher 1.2.8: what players sent with the launcher's report button
        tabList.add(new ReportsTab(ctx));

        JPanel header = AdminUi.panel(new BorderLayout(12, 0));
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ConsoleUiStyle.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JLabel title = new JLabel("新楓之谷 282 管理主控台");
        title.setFont(AdminUi.font(Font.BOLD, 17));
        header.add(title, BorderLayout.WEST);
        serverState.setFont(AdminUi.font(Font.BOLD, 13));
        header.add(serverState, BorderLayout.CENTER);
        auto.setFont(AdminUi.font(Font.PLAIN, 13));
        auto.setBackground(ConsoleUiStyle.PANEL_BG);
        retry.addActionListener(e -> ping());
        header.add(AdminUi.row(auto, retry), BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);

        tabs.setFont(AdminUi.font(Font.PLAIN, 14));
        for (AdminTab t : tabList) {
            tabs.addTab(t.title(), t.component());
        }
        tabs.addChangeListener(e -> refreshShown());
        content.add(tabs, BorderLayout.CENTER);
        JPanel foot = AdminUi.panel(new BorderLayout());
        foot.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ConsoleUiStyle.BORDER),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        foot.add(status, BorderLayout.CENTER);
        foot.add(AdminUi.hint("伺服器照常 headless 執行；這個視窗開關都不影響遊戲。資料一律經由伺服器的管理 API（" + gateway.describe() + "）。"),
                BorderLayout.EAST);
        content.add(foot, BorderLayout.SOUTH);
    }

    JPanel content() {
        return content;
    }

    JTabbedPane tabs() {
        return tabs;
    }

    List<AdminTab> tabList() {
        return tabList;
    }

    AdminConsoleContext context() {
        return ctx;
    }

    AdminTab current() {
        return tabList.get(Math.max(0, tabs.getSelectedIndex()));
    }

    void showWindow() {
        JFrame f = new JFrame("新楓之谷 282 管理主控台");
        ConsoleUiStyle.applyFrame(f);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setContentPane(content);
        f.setSize(WIDTH, HEIGHT);
        f.setMinimumSize(new Dimension(1000, 640));
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        ping();
        new Timer(5000, e -> tick()).start();
    }

    private void tick() {
        if (!up) {
            ping();
        } else if (auto.isSelected() && current().autoRefresh()) {
            current().refresh();
        }
    }

    /** server.ping, then the visible tab. */
    void ping() {
        ctx.call("server.ping", (JsonNode r) -> {
            pid = r.path("pid").asLong();
            connected(true, null);
            refreshShown();
        });
    }

    /**
     * Reloads the visible tab - unless it holds edits not saved yet (公告、連線安全): those are never replaced
     * behind the user's back; the status line says how to reload on purpose.
     */
    void refreshShown() {
        AdminTab t = current();
        if (t.hasUnsavedChanges()) {
            status("「" + t.title() + "」有還沒儲存的變更，所以沒有自動重新載入；按該頁的「重新載入」才會改用伺服器上的內容（會先確認）。");
            return;
        }
        t.refresh();
    }

    @Override
    public void connected(boolean isUp, String message) {
        boolean was = up;
        up = isUp;
        if (isUp) {
            AdminUi.colored(serverState, "● 伺服器執行中　" + ctx.gateway().describe() + (pid > 0 ? "　PID " + pid : ""), AdminUi.OK);
            retry.setVisible(false);
            if (!was) {
                status(" ");
            }
        } else {
            AdminUi.colored(serverState, "● " + (message == null ? "伺服器未啟動" : message), AdminUi.BAD);
            retry.setVisible(true);
            api.serverDown();
            status("伺服器未啟動：主控台每 5 秒自動重試，也可以按「重新連線」。");
        }
    }

    @Override
    public void status(String message) {
        status.setText(message == null || message.isBlank() ? " " : message);
    }
}
