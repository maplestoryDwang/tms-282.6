package net.swordie.ms.console;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ConsoleLogAppender {
    private static final int MAX_CHARS = 500_000;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Color NORMAL = new Color(0x222222);
    private static final Color ERROR = new Color(0xC75050);
    private static final Color SUCCESS = new Color(0xD00000);

    private final JTextPane pane;
    private final SimpleAttributeSet normalStyle = style(NORMAL, false);
    private final SimpleAttributeSet errorStyle = style(ERROR, false);
    private final SimpleAttributeSet successStyle = style(SUCCESS, true);
    private PrintStream mirrorOut;
    private PrintStream mirrorErr;

    public ConsoleLogAppender(JTextPane pane) {
        this.pane = pane;
    }

    public void setMirrorStream(PrintStream out, PrintStream err) {
        this.mirrorOut = out;
        this.mirrorErr = err;
    }

    public void append(String text, boolean error) {
        append(text, error, false);
    }

    public void appendSuccess(String text) {
        append(text, false, true);
    }

    /** 在所有已排隊的運行日誌刷入面板後再顯示成功行，保證出現在日誌最末尾。 */
    public void appendSuccessLast(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String normalized = normalizeLine(ConsoleLogText.toChineseRunLine(text));
        if (!ConsoleLogText.shouldShowRunLine(normalized, false)) {
            return;
        }
        if (mirrorOut != null) {
            mirrorOut.println(normalized);
        }
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> appendOnEdt(normalized, successStyle)));
    }

    public void append(String text, boolean error, boolean success) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String normalized = normalizeLine(ConsoleLogText.toChineseRunLine(text));
        if (!ConsoleLogText.shouldShowRunLine(normalized, error)) {
            return;
        }
        PrintStream mirror = error ? mirrorErr : mirrorOut;
        if (mirror != null) {
            mirror.println(normalized);
        }
        SwingUtilities.invokeLater(() -> appendOnEdt(normalized, success ? successStyle : error ? errorStyle : normalStyle));
    }

    public String getText() {
        return pane.getText();
    }

    public void clear() {
        pane.setText("");
    }

    public boolean saveToFile(JFrame owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("gms265-console.log"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        return saveToFile(owner, chooser.getSelectedFile());
    }

    public boolean saveToFile(JFrame owner, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(getText());
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner, "保存失敗: " + e.getMessage(), "運行日誌", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void appendOnEdt(String line, SimpleAttributeSet style) {
        Document doc = pane.getDocument();
        try {
            doc.insertString(doc.getLength(), "[" + TS.format(LocalDateTime.now()) + "] " + line + "\n", style);
            trim(doc);
            pane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    private void trim(Document doc) throws BadLocationException {
        int extra = doc.getLength() - MAX_CHARS;
        if (extra > 0) {
            doc.remove(0, extra);
        }
    }

    private static String normalizeLine(String text) {
        return text.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private static SimpleAttributeSet style(Color color, boolean bold) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, color);
        StyleConstants.setBold(set, bold);
        return set;
    }
}