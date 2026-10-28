package net.swordie.ms.console;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;

/** 管理控制檯的台灣繁體用語：日誌行與介面文字（標籤、按鈕、分頁、框線標題、提示）在顯示前統一轉換。 */
public final class TaiwanText {
    /** 依序替換；「連接→連線」必須在「端口→連接埠」之前，否則會變成「連線埠」。 */
    private static final String[][] TERMS = {
            {"服務端", "伺服器"}, {"數據庫", "資料庫"}, {"連接池", "連線池"}, {"數據", "資料"},
            {"連接", "連線"}, {"端口", "連接埠"}, {"啓", "啟"}, {"加載", "載入"}, {"緩存", "快取"},
            {"網絡", "網路"}, {"異常", "例外"}, {"字節", "位元組"}, {"保存", "儲存"}, {"當前", "目前"},
            {"默認", "預設"}, {"設置", "設定"}, {"線程", "執行緒"}, {"內存", "記憶體"}, {"信息", "訊息"},
            {"調試", "除錯"}, {"運行", "執行"}, {"串行", "循序"}, {"文件", "檔案"},
    };

    private static final String WATCHED = "tms.taiwanText";

    private TaiwanText() {
    }

    public static String convert(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        for (String[] t : TERMS) {
            if (s.contains(t[0])) {
                s = s.replace(t[0], t[1]);
            }
        }
        return s.replace("連線埠", "連接埠"); // 「連接→連線」不可動到已是台灣用語的「連接埠」
    }

    /** 轉換整個視窗，並讓之後才加入的元件（延遲建立的分頁、對話框）也自動轉換。 */
    public static void install(Component root) {
        apply(root);
        AWTEventListener added = e -> {
            if (e instanceof ContainerEvent ce && ce.getID() == ContainerEvent.COMPONENT_ADDED) {
                apply(ce.getChild());
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(added, AWTEvent.CONTAINER_EVENT_MASK);
    }

    public static void apply(Component c) {
        if (c == null) {
            return;
        }
        if (c instanceof JLabel l) {
            l.setText(convert(l.getText()));
        } else if (c instanceof AbstractButton b) {
            b.setText(convert(b.getText()));
        }
        if (c instanceof JComponent jc && (c instanceof JLabel || c instanceof AbstractButton)
                && jc.getClientProperty(WATCHED) == null) {
            jc.putClientProperty(WATCHED, Boolean.TRUE);
            jc.addPropertyChangeListener("text", e -> { // 之後程式再 setText（例如啟動/停止按鈕）也轉換
                String now = (String) e.getNewValue(), fixed = convert(now);
                if (now != null && !now.equals(fixed)) {
                    if (c instanceof JLabel l) l.setText(fixed);
                    else ((AbstractButton) c).setText(fixed);
                }
            });
        }
        if (c instanceof JComponent jc) {
            String tip = jc.getToolTipText();
            if (tip != null) {
                jc.setToolTipText(convert(tip));
            }
            Border border = jc.getBorder();
            if (border instanceof TitledBorder tb) {
                tb.setTitle(convert(tb.getTitle()));
            }
        }
        if (c instanceof JTabbedPane tabs) {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setTitleAt(i, convert(tabs.getTitleAt(i)));
            }
        }
        if (c instanceof Frame f) {
            f.setTitle(convert(f.getTitle()));
        } else if (c instanceof Dialog d) {
            d.setTitle(convert(d.getTitle()));
        }
        if (c instanceof JMenu m) {
            for (Component item : m.getMenuComponents()) {
                apply(item);
            }
        }
        if (c instanceof Container ct) {
            for (Component child : ct.getComponents()) {
                apply(child);
            }
        }
    }
}
