package net.swordie.ms.console.admin;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code --screenshots <dir>}: builds the console offscreen (never shown), loads every tab (made-up data, or the test
 * server with {@code --live}), paints each to a PNG and checks the layout: a label or button narrower than its text
 * (clipped), components of one panel overlapping, a component sticking out of its parent, and table cells whose text
 * does not fit their column (listed as notes - a table may scroll or be widened). Writes layout-check.txt next to
 * the images; the return value counts the problems (not the notes).
 */
final class AdminScreenshots {
    private AdminScreenshots() {
    }

    static int render(AdminGateway gateway, boolean live, Path dir) throws Exception {
        Files.createDirectories(dir);
        String prefix = live ? "live-" : "";
        String[] names = {"1-api-monitor", "2-account", "3-news", "4-daily", "5-net-security", "6-save-shutdown", "7-release", "8-gift", "9-newbie", "10-online-reward",
                "11-ops", "12-issues", "13-character", "14-deploy", "15-reports"};
        List<String> report = new ArrayList<>();
        AtomicInteger problems = new AtomicInteger();
        for (int i = 0; i < names.length; i++) {
            int index = i;
            SwingUtilities.invokeAndWait(() -> {
                try {
                    AdminConsoleFrame f = new AdminConsoleFrame(gateway, true, pid -> Optional.of(true));
                    f.ping();
                    f.tabs().setSelectedIndex(index);
                    AdminTab tab = f.tabList().get(index);
                    if (index == 0) {
                        tab.refresh();
                    }
                    if (tab instanceof AccountTab a) {
                        a.query(live ? System.getProperty("admin.shots.query", "1") : "雪花冒險家");
                        a.refresh();
                    } else if (tab instanceof DailyTab d) {
                        d.query(live ? System.getProperty("admin.shots.query", "1") : "SnowyMapleHero");
                        d.refresh();
                    } else if (tab instanceof GiftCodeTab g) {
                        g.refresh();
                        if (!live) {
                            g.search("楓葉");
                            g.addItem(4001126, "楓葉", 100);
                            g.addItem(1142000, "楓之谷新手勳章（超長名稱測試用的道具）", 1);
                            g.fill("開服紀念禮包（十月）", "給 Radmin 朋友的第一批", 50, 1, 0, 10, 3);
                        }
                    } else if (tab instanceof NetSecurityTab n) {
                        n.showFirewall(live ? FirewallReader.read() : fakeRules());
                    }
                    problems.addAndGet(shoot(f, dir.resolve(prefix + names[index] + ".png"), names[index], report));
                    if (tab instanceof NewbieTab n) {
                        n.selectJobs();
                        problems.addAndGet(shoot(f, dir.resolve(prefix + "9b-job-statistics.png"), "9b-job-statistics", report));
                    }
                    if (tab instanceof GiftCodeTab g && !live) {
                        // second picture of 禮包序號: the codes right after 建立 (shown once)
                        g.showCreated(gateway.call("gift.create", java.util.Map.of()));
                        problems.addAndGet(shoot(f, dir.resolve("8b-gift-created.png"), "8b-gift-created", report));
                    }
                    if (tab instanceof NewsTab nt) {
                        // second picture of 公告: the preview scrolled to its end (generated items, grey)
                        nt.scrollPreviewToEnd();
                        problems.addAndGet(shoot(f, dir.resolve(prefix + "3b-news-preview-end.png"), "3b-news-preview-end", report));
                    }
                } catch (Exception e) {
                    report.add(names[index] + ": FAILED " + e);
                    problems.incrementAndGet();
                }
            });
        }
        if (!live) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    AdminConsoleFrame down = new AdminConsoleFrame(new FakeAdminGateway(true), true, pid -> Optional.of(false));
                    down.ping();
                    down.current().refresh();
                    problems.addAndGet(shoot(down, dir.resolve("0-server-down.png"), "0-server-down", report));
                    AdminConsoleFrame stopping = new AdminConsoleFrame(gateway, true, pid -> Optional.of(true));
                    stopping.ping();
                    stopping.tabs().setSelectedIndex(5);
                    ((SaveShutdownTab) stopping.tabList().get(5)).showWaiting(65640, 12);
                    problems.addAndGet(shoot(stopping, dir.resolve("6b-save-shutdown-waiting.png"), "6b-save-shutdown-waiting", report));
                } catch (Exception e) {
                    report.add("extra: FAILED " + e);
                    problems.incrementAndGet();
                }
            });
        }
        report.add(0, "admin console layout check (" + (live ? "test server data" : "made-up data") + "): " + problems.get() + " problem(s)");
        Files.write(dir.resolve(prefix + "layout-check.txt"), report, StandardCharsets.UTF_8);
        report.forEach(System.out::println);
        return problems.get();
    }

    private static List<FirewallReader.Rule> fakeRules() {
        return List.of(
                new FirewallReader.Rule("TMS 282 遊戲埠（Radmin）", "True", "Allow", "Private, Public", "C:\\Program Files\\Java\\jdk-22\\bin\\java.exe",
                        "TCP", "8484,8585-8588,8600", "26.0.0.0/255.0.0.0"),
                new FirewallReader.Rule("Java(TM) Platform SE binary", "True", "Allow", "Private", "C:\\Program Files\\Java\\jdk-22\\bin\\java.exe",
                        "Any", "Any", "Any"),
                new FirewallReader.Rule("Java(TM) Platform SE binary", "True", "Block", "Public", "C:\\Program Files\\Java\\jdk-22\\bin\\javaw.exe",
                        "UDP", "Any", "Any"));
    }

    private static int shoot(AdminConsoleFrame f, Path png, String name, List<String> report) throws Exception {
        JFrame frame = new JFrame();
        frame.setContentPane(f.content());
        frame.pack();
        frame.setSize(AdminConsoleFrame.WIDTH, AdminConsoleFrame.HEIGHT);
        frame.validate();
        layoutAll(f.content());
        JComponent c = f.content();
        BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.printAll(g);
        g.dispose();
        ImageIO.write(img, "png", png.toFile());
        List<String> problems = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        check(c, problems, notes);
        frame.dispose();
        report.add("");
        report.add(name + ": " + png.getFileName() + " " + img.getWidth() + "x" + img.getHeight() + ", " + problems.size() + " problem(s), "
                + notes.size() + " note(s)");
        problems.forEach(p -> report.add("  PROBLEM " + p));
        notes.forEach(n -> report.add("  note " + n));
        return problems.size();
    }

    private static void layoutAll(Component c) {
        if (c instanceof Container k) {
            k.doLayout();
            for (Component ch : k.getComponents()) {
                layoutAll(ch);
            }
        }
    }

    /** Walks the visible tree. */
    static void check(Component c, List<String> problems, List<String> notes) {
        if (!c.isVisible() || c.getWidth() <= 0 || c.getHeight() <= 0) {
            return;
        }
        if (c instanceof JLabel l && l.getText() != null && !l.getText().isBlank()) {
            int need = l.getPreferredSize().width;
            if (need > l.getWidth() + 1) {
                problems.add("label clipped (" + l.getWidth() + "/" + need + " px): " + l.getText());
            }
        } else if (c instanceof AbstractButton b && b.getText() != null && !b.getText().isBlank()) {
            int need = b.getPreferredSize().width;
            if (need > b.getWidth() + 1) {
                problems.add("button clipped (" + b.getWidth() + "/" + need + " px): " + b.getText());
            }
        }
        if (c instanceof JTable t) {
            cells(t, notes);
        }
        if (c instanceof Container k && !(c instanceof JViewport) && !(c instanceof JLayeredPane) && !(c instanceof JScrollPane)
                && !(c instanceof JTable) && !(c instanceof JTableHeader) && !(c instanceof JTabbedPane)) {
            Component[] ch = k.getComponents();
            for (int i = 0; i < ch.length; i++) {
                if (!ch[i].isVisible() || ch[i].getWidth() <= 0) {
                    continue;
                }
                Rectangle a = ch[i].getBounds();
                if (a.x < 0 || a.y < 0 || a.x + a.width > k.getWidth() + 1 || a.y + a.height > k.getHeight() + 1) {
                    problems.add("sticks out of its parent (" + a + " in " + k.getWidth() + "x" + k.getHeight() + "): " + describe(ch[i]));
                }
                for (int j = i + 1; j < ch.length; j++) {
                    if (ch[j].isVisible() && ch[j].getWidth() > 0 && a.intersects(ch[j].getBounds())) {
                        problems.add("overlap: " + describe(ch[i]) + " / " + describe(ch[j]));
                    }
                }
            }
        }
        if (c instanceof Container k) {
            for (Component ch : k.getComponents()) {
                check(ch, problems, notes);
            }
        }
    }

    private static void cells(JTable t, List<String> notes) {
        for (int col = 0; col < t.getColumnCount(); col++) {
            int width = t.getColumnModel().getColumn(col).getWidth();
            TableCellRenderer hr = t.getTableHeader().getDefaultRenderer();
            Component h = hr.getTableCellRendererComponent(t, t.getColumnName(col), false, false, -1, col);
            if (h.getPreferredSize().width > width + 2) {
                notes.add("header clipped (" + width + "/" + h.getPreferredSize().width + " px): " + t.getColumnName(col));
            }
            for (int row = 0; row < t.getRowCount(); row++) {
                Component r = t.prepareRenderer(t.getCellRenderer(row, col), row, col);
                int need = r.getPreferredSize().width;
                if (need > width + 2) {
                    notes.add("cell clipped [" + t.getColumnName(col) + "] (" + width + "/" + need + " px): " + t.getValueAt(row, col));
                }
            }
        }
    }

    private static String describe(Component c) {
        if (c instanceof JLabel l) {
            return "label '" + l.getText() + "'";
        }
        if (c instanceof AbstractButton b) {
            return "button '" + b.getText() + "'";
        }
        if (c instanceof JComponent j && j.getBorder() instanceof javax.swing.border.CompoundBorder cb
                && cb.getOutsideBorder() instanceof javax.swing.border.TitledBorder tb) {
            return "section '" + tb.getTitle() + "'";
        }
        return c.getClass().getSimpleName();
    }
}
