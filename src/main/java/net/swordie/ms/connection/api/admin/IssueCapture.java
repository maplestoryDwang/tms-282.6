package net.swordie.ms.connection.api.admin;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the server's own problem lines (refused skill uses, packets the outbound guard dropped, failed combat gates,
 * buffs that could not be sent, handler exceptions) grouped in memory for the console's 「技能問題」 tab, so a test
 * session can be read without searching the log file. It only watches what the server already prints: a
 * {@link PrintStream} in front of {@code System.out} that passes every character on unchanged.
 * <p>
 * One {@link Rule} per kind of line (registry); adding a kind is one entry in {@link #RULES}.
 */
public final class IssueCapture extends PrintStream {
    /** Most groups kept; the oldest (by last occurrence) is dropped beyond this. */
    static final int MAX_GROUPS = 1500;
    private static final int MAX_LINE = 4000;
    private static final int SAMPLE = 320;

    /** A grouped problem. */
    public record Issue(String kind, String subject, int character, int skill, String reason, int count, long firstMs,
                        long lastMs, String sample) {
    }

    /** kind + pattern; groups: named {@code subject}, {@code chr}, {@code skill}, {@code reason} (each optional). */
    record Rule(String kind, Pattern pattern) {
        static Rule of(String kind, String regex) {
            return new Rule(kind, Pattern.compile(regex));
        }
    }

    static final List<Rule> RULES = List.of(
            Rule.of("攻擊被丟棄", "REJECTED (?<subject>CP\\w+) len=\\d+ character=(?<chr>\\d+): (?<reason>[^;(]{1,90})"),
            Rule.of("技能被拒絕", "REJECTED (?<subject>CP\\w+)(?: skill=(?<skill>\\d+))?[^:]*: (?<reason>[^;]{1,90})"),
            Rule.of("封包被擋", "\\[OutboundGuard\\] #\\d+ dropped (?<subject>\\w+ op=0x\\w+) len=\\d+ to \\S+: (?<reason>.{1,90})"),
            Rule.of("判定未過", "COMBAT GATE (?<reason>\\w+) failed for skill (?<skill>\\d+) character (?<chr>\\d+)"),
            Rule.of("狀態未送出", "buff (?:set|reset) not shown \\([^)]*\\): (?<subject>\\w+) - (?<reason>.{1,90})"),
            Rule.of("處理失敗", "(?<subject>\\w+\\.handle\\w+) FAILED: (?<reason>.{1,120})"),
            Rule.of("任務無法接取", "CP01A6 act \\d+ npc-side NOT_READY quest=(?<subject>\\d+) \\((?<reason>[^\\[)]{1,90})"),
            Rule.of("沒有效果", "CP04B4 skill=(?<skill>\\d+) slv=\\d+ .*kind=(?<reason>no-CTS).*character=(?<chr>\\d+)"));

    private static final Object INSTALL = new Object();
    private static volatile IssueCapture installed;

    private final StringBuilder line = new StringBuilder();
    private final Map<String, Issue> groups = new LinkedHashMap<>();
    private long clearedAt = System.currentTimeMillis();

    private IssueCapture(PrintStream out, Charset charset) {
        super(out, true, charset);
    }

    /** Puts the capture in front of System.out once; later calls return the same instance. */
    public static IssueCapture install() {
        IssueCapture c = installed;
        if (c != null) {
            return c;
        }
        synchronized (INSTALL) {
            if (installed == null) {
                PrintStream out = System.out;
                Charset charset = out.charset();
                IssueCapture capture = new IssueCapture(out, charset);
                System.setOut(capture);
                installed = capture;
            }
            return installed;
        }
    }

    /** The installed capture, or null when the server never installed one (checks). */
    public static IssueCapture current() {
        return installed;
    }

    /** For checks: a capture that is not installed anywhere. */
    static IssueCapture detached(PrintStream sink) {
        return new IssueCapture(sink, sink.charset());
    }

    /** Set while this thread is inside println: a PrintStream subclass prints the text through print(String). */
    private final ThreadLocal<Boolean> inPrintln = ThreadLocal.withInitial(() -> false);

    @Override
    public void print(String s) {
        super.print(s);
        if (!inPrintln.get()) {
            feed(s);
        }
    }

    @Override
    public void println(String s) {
        inPrintln.set(true);
        try {
            super.println(s);
        } finally {
            inPrintln.set(false);
        }
        feed(s);
        feed("\n");
    }

    @Override
    public void println() {
        super.println();
        feed("\n");
    }

    @Override
    public void println(Object x) {
        println(String.valueOf(x));
    }

    private void feed(String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        synchronized (line) {
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '\n') {
                    String done = line.toString();
                    line.setLength(0);
                    if (!done.isEmpty()) {
                        accept(done, System.currentTimeMillis());
                    }
                } else if (ch != '\r' && line.length() < MAX_LINE) {
                    line.append(ch);
                }
            }
        }
    }

    /** Classifies one complete line (first matching rule wins). */
    void accept(String text, long now) {
        for (Rule rule : RULES) {
            Matcher m = rule.pattern().matcher(text);
            if (!m.find()) {
                continue;
            }
            String subject = group(m, "subject");
            String reason = group(m, "reason").trim();
            int chr = number(group(m, "chr"));
            int skill = number(group(m, "skill"));
            if (chr == 0) chr = find(CHARACTER, text);
            if (skill == 0) skill = find(SKILL, text);
            String key = rule.kind() + '|' + subject + '|' + chr + '|' + skill + '|' + reason;
            synchronized (groups) {
                Issue old = groups.remove(key);
                String sample = text.length() > SAMPLE ? text.substring(0, SAMPLE) + "…" : text;
                groups.put(key, old == null
                        ? new Issue(rule.kind(), subject, chr, skill, reason, 1, now, now, sample)
                        : new Issue(old.kind(), old.subject(), old.character(), old.skill(), old.reason(), old.count() + 1,
                        old.firstMs(), now, sample));
                if (groups.size() > MAX_GROUPS) {
                    String eldest = groups.keySet().iterator().next();
                    groups.remove(eldest);
                }
            }
            return;
        }
    }

    private static final Pattern CHARACTER = Pattern.compile("character[= ](\\d+)");
    private static final Pattern SKILL = Pattern.compile("skill[= ](\\d+)");

    private static int find(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? number(m.group(1)) : 0;
    }

    private static String group(Matcher m, String name) {
        try {
            String v = m.group(name);
            return v == null ? "" : v;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static int number(String v) {
        try {
            return v.isEmpty() ? 0 : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Groups whose last occurrence is at or after {@code sinceMs}, newest first, optionally of one character. */
    public List<Issue> snapshot(long sinceMs, int character) {
        List<Issue> out;
        synchronized (groups) {
            out = new ArrayList<>(groups.values());
        }
        out.removeIf(i -> i.lastMs() < sinceMs || (character > 0 && i.character() != character));
        out.sort(Comparator.comparingLong(Issue::lastMs).reversed());
        return out;
    }

    public long clearedAt() {
        return clearedAt;
    }

    public void clear() {
        synchronized (groups) {
            groups.clear();
            clearedAt = System.currentTimeMillis();
        }
    }
}
