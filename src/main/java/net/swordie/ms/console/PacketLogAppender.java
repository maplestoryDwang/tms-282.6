package net.swordie.ms.console;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 封包日誌專用輸出：異步隊列 + 按類型存儲行，支持過濾後重建顯示，避免封包洪峯拖死 EDT。
 */
public final class PacketLogAppender {
    public static final Color COLOR_CLIENT = new Color(0, 102, 204);
    public static final Color COLOR_SERVER = new Color(0, 130, 70);
    public static final Color COLOR_UNKNOWN = new Color(0xCC6600);

    private static final int MAX_QUEUE_ITEMS = 8_192;
    private static final int MAX_QUEUE_CHARS = 1_024_000;
    private static final int DRAIN_BATCH_LINES = 256;
    private static final int EDT_BATCH_LINES = 48;
    private static final int EDT_BATCH_LINES_BURST = 160;
    private static final int EDT_BATCH_MAX_CHARS = 24_000;
    private static final int EDT_BATCH_MAX_CHARS_BURST = 96_000;
    private static final int EDT_BURST_THRESHOLD = 128;
    private static final int SCROLL_THROTTLE_MS = 80;
    private static final int TRIM_HYSTERESIS_LINES = 96;
    private static final int EDT_PUMP_INTERVAL_MS = 40;
    private static final int EDT_PUMP_EXTRA_PASSES = 3;
    private static final int REBUILD_BATCH_LINES = 64;
    private static final int REBUILD_BATCH_MAX_CHARS = 24_000;
    private static final int MAX_STORED_LINE_CHARS = 4_096;
    private static final int DEFAULT_MAX_DISPLAY_LINES = 2_000;

    private static volatile PacketLogAppender instance;

    private final JTextPane textArea;
    private final JScrollPane scroll;
    private final SimpleAttributeSet clientAttr;
    private final SimpleAttributeSet serverAttr;
    private final SimpleAttributeSet unknownAttr;
    private final AdjustmentListener scrollListener;
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queuedChars = new AtomicInteger();
    private final AtomicBoolean rebuildScheduled = new AtomicBoolean();
    private final AtomicLong droppedLines = new AtomicLong();
    private final ConcurrentLinkedQueue<StoredLine> edtQueue = new ConcurrentLinkedQueue<>();
    private final Thread drainThread;
    private final Timer edtPumpTimer;
    private final List<StoredLine> storedLines = new ArrayList<>();
    private final LinkedHashSet<String> knownTypes = new LinkedHashSet<>();
    private final LinkedHashSet<String> visibleTypes = new LinkedHashSet<>();
    private volatile Consumer<String> onNewType;
    private volatile Consumer<Set<String>> onTypesPruned;
    private volatile boolean closed;
    private volatile boolean enabled = true;
    private int visibleLineCount;
    private int maxDisplayLines = DEFAULT_MAX_DISPLAY_LINES;
    private boolean autoScroll = true;
    private boolean rebuildScrollWhenDone;
    private Deque<StoredLine> rebuildPending = new ArrayDeque<>();
    private long lastScrollAtMs;
    private int edtFlushGeneration;
    private boolean edtPumping;

    public PacketLogAppender(JTextPane textArea, JScrollPane scroll) {
        this.textArea = textArea;
        this.scroll = scroll;
        clientAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(clientAttr, COLOR_CLIENT);
        serverAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(serverAttr, COLOR_SERVER);
        unknownAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(unknownAttr, COLOR_UNKNOWN);
        instance = this;
        drainThread = new Thread(this::drainLoop, "packet-log-drain");
        drainThread.setDaemon(true);
        drainThread.start();
        edtPumpTimer = new Timer(EDT_PUMP_INTERVAL_MS, e -> pumpEdtQueue());
        edtPumpTimer.setRepeats(true);
        edtPumpTimer.setCoalesce(true);
        edtPumpTimer.start();
        scrollListener = (AdjustmentEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                JScrollBar bar = scroll.getVerticalScrollBar();
                autoScroll = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 8;
            }
        };
        scroll.getVerticalScrollBar().addAdjustmentListener(scrollListener);
    }

    public static PacketLogAppender getInstance() {
        return instance;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getDroppedLineCount() {
        return droppedLines.get();
    }

    public int getPendingLineCount() {
        return queue.size() + edtQueue.size();
    }

    public void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        if (instance == this) {
            instance = null;
        }
        edtPumpTimer.stop();
        queue.clear();
        edtQueue.clear();
        rebuildPending.clear();
        queuedChars.set(0);
        rebuildScheduled.set(false);
        onNewType = null;
        onTypesPruned = null;
        synchronized (storedLines) {
            storedLines.clear();
            knownTypes.clear();
            visibleTypes.clear();
        }
        drainThread.interrupt();
        scroll.getVerticalScrollBar().removeAdjustmentListener(scrollListener);
        runOnEdt(() -> {
            clearTextArea();
            visibleLineCount = 0;
        });
    }

    public void setOnNewType(Consumer<String> callback) {
        onNewType = callback;
    }

    public void setOnTypesPruned(Consumer<Set<String>> callback) {
        onTypesPruned = callback;
    }

    public Set<String> getKnownTypes() {
        synchronized (storedLines) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(knownTypes));
        }
    }

    public boolean isTypeVisible(String type) {
        synchronized (storedLines) {
            return visibleTypes.contains(type);
        }
    }

    public void setTypeVisible(String type, boolean visible) {
        if (type == null || type.isEmpty()) {
            return;
        }
        runOnEdt(() -> {
            synchronized (storedLines) {
                if (!knownTypes.contains(type)) {
                    return;
                }
                if (visible) {
                    visibleTypes.add(type);
                } else {
                    visibleTypes.remove(type);
                }
            }
            rebuildDisplay();
        });
    }

    public void setAllTypesVisible(boolean visible) {
        runOnEdt(() -> {
            synchronized (storedLines) {
                if (visible) {
                    visibleTypes.addAll(knownTypes);
                } else {
                    visibleTypes.clear();
                }
            }
            rebuildDisplay();
        });
    }

    public void enqueue(String text) {
        if (closed || !enabled || text == null || text.isEmpty()) {
            return;
        }
        if (shouldDropPacket()) {
            droppedLines.incrementAndGet();
            return;
        }
        queue.add(text);
        queuedChars.addAndGet(text.length());
    }

    public void clear() {
        queue.clear();
        edtQueue.clear();
        rebuildPending.clear();
        queuedChars.set(0);
        rebuildScheduled.set(false);
        runOnEdt(() -> {
            synchronized (storedLines) {
                storedLines.clear();
                knownTypes.clear();
                visibleTypes.clear();
            }
            clearTextArea();
            visibleLineCount = 0;
            autoScroll = true;
        });
    }

    /** 簡單日誌開關切換時，後臺批量截斷已存行並分塊重繪，避免 EDT 卡頓。 */
    public void reapplySimpleLogMode() {
        if (closed) {
            return;
        }
        Thread worker = new Thread(() -> {
            boolean simple = PacketLogFilter.isSimpleLogEnabled();
            synchronized (storedLines) {
                for (int i = 0; i < storedLines.size(); i++) {
                    StoredLine old = storedLines.get(i);
                    String text = PacketLogDisplayUtil.applySimpleLogToLine(old.text, simple);
                    storedLines.set(i, new StoredLine(old.type, text, old.recv, old.unknown));
                }
            }
            runOnEdt(this::rebuildDisplay);
        }, "packet-log-reapply");
        worker.setDaemon(true);
        worker.start();
    }

    public String getText() {
        return textArea.getText();
    }

    public String getSelectedText() {
        return textArea.getSelectedText();
    }

    public void saveToFile(Component owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("gms265-packet.log"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
            writer.write(getText());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner, "保存失敗: " + e.getMessage(), "封包日誌", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean shouldDropPacket() {
        return queue.size() >= MAX_QUEUE_ITEMS || queuedChars.get() >= MAX_QUEUE_CHARS;
    }

    private void drainLoop() {
        List<String> batch = new ArrayList<>(DRAIN_BATCH_LINES);
        while (!Thread.currentThread().isInterrupted() && !closed) {
            String item = queue.poll();
            if (item == null) {
                flushDrainBatch(batch);
                sleepQuiet(8);
                continue;
            }
            queuedChars.addAndGet(-item.length());
            batch.add(item);
            while (batch.size() < DRAIN_BATCH_LINES) {
                item = queue.poll();
                if (item == null) {
                    break;
                }
                queuedChars.addAndGet(-item.length());
                batch.add(item);
            }
            flushDrainBatch(batch);
        }
    }

    private void flushDrainBatch(List<String> batch) {
        if (batch.isEmpty()) {
            return;
        }
        List<StoredLine> visibleLines = ingestLinesOnDrainThread(batch);
        batch.clear();
        if (visibleLines.isEmpty()) {
            return;
        }
        edtQueue.addAll(visibleLines);
        scheduleEdtFlush();
    }

    private List<StoredLine> ingestLinesOnDrainThread(List<String> chunks) {
        List<StoredLine> visible = new ArrayList<>();
        List<String> newTypes = new ArrayList<>();
        int trimVisible;
        synchronized (storedLines) {
            for (String chunk : chunks) {
                int from = 0;
                for (int i = 0; i <= chunk.length(); i++) {
                    if (i < chunk.length() && chunk.charAt(i) != '\n') {
                        continue;
                    }
                    if (i > from) {
                        ingestOneLine(chunk.substring(from, i), visible, newTypes);
                    }
                    from = i + 1;
                }
            }
            trimVisible = trimStoredLines();
        }
        Consumer<String> typeCb = onNewType;
        if (typeCb != null && !newTypes.isEmpty()) {
            List<String> typesToPublish = List.copyOf(newTypes);
            runOnEdt(() -> {
                for (String type : typesToPublish) {
                    typeCb.accept(type);
                }
            });
        }
        if (trimVisible > 0) {
            runOnEdt(() -> trimVisibleLinesOnEdt(trimVisible));
        }
        return visible;
    }

    private void ingestOneLine(String line, List<StoredLine> visible, List<String> newTypes) {
        if (line.isEmpty()) {
            return;
        }
        String localizedLine = ConsoleLogText.toChinesePacketLine(line);
        String opcode = PacketLogLineType.parseOpcode(localizedLine);
        if (PacketLogFilter.shouldDropHighFrequency(opcode)) {
            return;
        }
        String safeLine = localizedLine.length() > MAX_STORED_LINE_CHARS
                ? localizedLine.substring(0, MAX_STORED_LINE_CHARS) + " ...(行過長已截斷)"
                : localizedLine;
        String type = PacketLogLineType.parseType(safeLine);
        boolean recv = PacketLogLineType.isRecvLine(safeLine);
        boolean unknown = PacketLogLineType.isUnknownLine(safeLine);
        String text = safeLine + '\n';
        StoredLine stored = new StoredLine(type, text, recv, unknown);
        boolean isNewType = knownTypes.add(type);
        if (isNewType) {
            visibleTypes.add(type);
            newTypes.add(type);
        }
        storedLines.add(stored);
        if (visibleTypes.contains(type)) {
            visible.add(stored);
        }
    }

    private int trimStoredLines() {
        int limit = maxDisplayLines;
        if (limit <= 0) {
            return 0;
        }
        int removedVisible = 0;
        boolean trimmed = false;
        while (storedLines.size() > limit) {
            StoredLine removed = storedLines.remove(0);
            trimmed = true;
            if (visibleTypes.contains(removed.type)) {
                removedVisible++;
            }
        }
        if (trimmed) {
            pruneStaleTypes();
        }
        return removedVisible;
    }

    private void pruneStaleTypes() {
        Set<String> active = new LinkedHashSet<>();
        for (StoredLine entry : storedLines) {
            active.add(entry.type);
        }
        Set<String> removed = new HashSet<>(knownTypes);
        removed.removeAll(active);
        if (removed.isEmpty()) {
            return;
        }
        knownTypes.retainAll(active);
        visibleTypes.retainAll(active);
        Consumer<Set<String>> cb = onTypesPruned;
        if (cb != null) {
            cb.accept(Collections.unmodifiableSet(removed));
        }
    }

    private void scheduleEdtFlush() {
        if (closed) {
            edtQueue.clear();
            return;
        }
        SwingUtilities.invokeLater(this::pumpEdtQueue);
    }

    private void pumpEdtQueue() {
        if (closed || edtQueue.isEmpty()) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::pumpEdtQueue);
            return;
        }
        if (edtPumping) {
            return;
        }
        edtPumping = true;
        try {
            int passes = 0;
            do {
                flushEdtBatchOnce();
                passes++;
            } while (passes <= EDT_PUMP_EXTRA_PASSES
                    && edtQueue.size() >= EDT_BURST_THRESHOLD
                    && !closed);
            if (edtQueue.isEmpty() && autoScroll) {
                lastScrollAtMs = 0;
                scrollToEnd();
            }
        } finally {
            edtPumping = false;
        }
    }

    private void flushEdtBatchOnce() {
        if (closed) {
            edtQueue.clear();
            return;
        }
        int backlog = edtQueue.size();
        int maxLines = backlog >= EDT_BURST_THRESHOLD ? EDT_BATCH_LINES_BURST : EDT_BATCH_LINES;
        int maxChars = backlog >= EDT_BURST_THRESHOLD ? EDT_BATCH_MAX_CHARS_BURST : EDT_BATCH_MAX_CHARS;
        int appended = 0;
        int chars = 0;
        StoredLine line;
        while (appended < maxLines && chars < maxChars && (line = edtQueue.poll()) != null) {
            appendLineOnEdt(line);
            chars += line.text.length();
            appended++;
        }
        if (appended > 0) {
            edtFlushGeneration++;
            if (shouldTrimVisibleNow()) {
                trimVisibleLinesOnEdt(0);
            }
            if (autoScroll) {
                scrollToEndThrottled();
            }
        }
    }

    private boolean shouldTrimVisibleNow() {
        int limit = maxDisplayLines;
        if (limit <= 0) {
            return false;
        }
        if (visibleLineCount > limit + TRIM_HYSTERESIS_LINES) {
            return true;
        }
        return edtQueue.isEmpty() && (edtFlushGeneration & 3) == 0;
    }

    private void scrollToEndThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastScrollAtMs < SCROLL_THROTTLE_MS) {
            return;
        }
        lastScrollAtMs = now;
        scrollToEnd();
    }

    private void appendLineOnEdt(StoredLine line) {
        if (closed || !textArea.isDisplayable()) {
            return;
        }
        try {
            Document doc = textArea.getDocument();
            AttributeSet attr = line.unknown ? unknownAttr : (line.recv ? clientAttr : serverAttr);
            doc.insertString(doc.getLength(), line.text, attr);
            visibleLineCount++;
        } catch (BadLocationException ignored) {
        }
    }

    private void trimVisibleLinesOnEdt(int linesToRemove) {
        if (closed) {
            return;
        }
        try {
            if (linesToRemove > 0) {
                removeHeadLines(linesToRemove);
                visibleLineCount = Math.max(0, visibleLineCount - linesToRemove);
            }
            int limit = maxDisplayLines;
            if (limit > 0) {
                while (visibleLineCount > limit) {
                    int excess = Math.min(visibleLineCount - limit, 128);
                    removeHeadLines(excess);
                    visibleLineCount -= excess;
                }
            }
        } catch (BadLocationException ex) {
            rebuildDisplay();
        }
    }

    private void removeHeadLines(int lineCount) throws BadLocationException {
        if (lineCount <= 0) {
            return;
        }
        Document doc = textArea.getDocument();
        if (doc.getLength() == 0) {
            return;
        }
        Element root = doc.getDefaultRootElement();
        int totalLines = root.getElementCount();
        if (totalLines <= lineCount) {
            clearTextArea();
            return;
        }
        int removeTo = root.getElement(lineCount - 1).getEndOffset();
        doc.remove(0, removeTo);
    }

    private void clearTextArea() {
        try {
            Document doc = textArea.getDocument();
            if (doc.getLength() > 0) {
                doc.remove(0, doc.getLength());
            }
        } catch (BadLocationException ignored) {
        }
    }

    private void rebuildDisplay() {
        if (closed) {
            return;
        }
        Deque<StoredLine> pending = new ArrayDeque<>();
        synchronized (storedLines) {
            for (StoredLine entry : storedLines) {
                if (visibleTypes.contains(entry.type)) {
                    pending.add(entry);
                }
            }
        }
        rebuildPending = pending;
        rebuildScrollWhenDone = true;
        clearTextArea();
        visibleLineCount = 0;
        scheduleRebuildChunk();
    }

    private void scheduleRebuildChunk() {
        if (closed) {
            rebuildPending.clear();
            return;
        }
        if (!rebuildScheduled.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(this::flushRebuildChunk);
    }

    private void flushRebuildChunk() {
        rebuildScheduled.set(false);
        if (closed) {
            rebuildPending.clear();
            return;
        }
        int appended = 0;
        int chars = 0;
        StoredLine line;
        while (appended < REBUILD_BATCH_LINES && chars < REBUILD_BATCH_MAX_CHARS
                && (line = rebuildPending.poll()) != null) {
            appendLineOnEdt(line);
            chars += line.text.length();
            appended++;
        }
        if (!rebuildPending.isEmpty()) {
            scheduleRebuildChunk();
            return;
        }
        if (rebuildScrollWhenDone && autoScroll) {
            rebuildScrollWhenDone = false;
            lastScrollAtMs = 0;
            scrollToEnd();
        }
    }

    private void scrollToEnd() {
        try {
            textArea.setCaretPosition(textArea.getDocument().getLength());
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        } catch (Exception ignored) {
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static final class StoredLine {
        final String type;
        final String text;
        final boolean recv;
        final boolean unknown;

        StoredLine(String type, String text, boolean recv, boolean unknown) {
            this.type = type;
            this.text = text;
            this.recv = recv;
            this.unknown = unknown;
        }
    }
}
