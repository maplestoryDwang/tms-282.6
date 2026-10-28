package net.swordie.ms.world.tms282.audit;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asynchronous, batched audit writer.
 * <ul>
 *     <li>Game threads only call {@link #submit}, which is a non-blocking {@code offer} into a bounded queue.</li>
 *     <li>When the queue is full the row is dropped and counted ({@link #dropped()}); game threads never wait.</li>
 *     <li>A single daemon thread drains the queue every {@code tms282.audit.flushMs} (default 3000 ms) and writes
 *     each table with one JDBC batch per transaction.</li>
 *     <li>The same thread executes the (rare) ban-table statements via {@link #executor()}, so DB work never
 *     happens on netty / game threads.</li>
 * </ul>
 * System properties: {@code tms282.audit.enabled} (default true), {@code tms282.audit.queue} (default 65536),
 * {@code tms282.audit.flushMs} (default 3000), {@code tms282.audit.batch} (max rows per flush pass, default 5000).
 */
public final class AuditWriter {

    record Row(String table, Object[] values) {
    }

    // Off under the package checks (-Dtms282.tests.rejectRealDatabase=true): the writer thread would reach the DB.
    private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("tms282.audit.enabled", "true"))
            && !Boolean.getBoolean("tms282.tests.rejectRealDatabase");
    private static final int CAPACITY = Math.max(1024, Integer.getInteger("tms282.audit.queue", 65536));
    private static final long FLUSH_MS = Math.max(250L, Long.getLong("tms282.audit.flushMs", 3000L));
    private static final int MAX_PER_PASS = Math.max(100, Integer.getInteger("tms282.audit.batch", 5000));
    private static final int MAX_STRING = 1000;

    private static final ArrayBlockingQueue<Row> QUEUE = new ArrayBlockingQueue<>(CAPACITY);
    private static final AtomicLong DROPPED = new AtomicLong();
    private static final AtomicLong WRITTEN = new AtomicLong();
    private static final AtomicLong FAILED = new AtomicLong();
    private static final AtomicLong LAST_DROP_WARN = new AtomicLong();
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static volatile boolean schemaReady;

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tms282-audit-writer");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private AuditWriter() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        EXEC.scheduleWithFixedDelay(AuditWriter::flushSafely, FLUSH_MS, FLUSH_MS, TimeUnit.MILLISECONDS);
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    EXEC.submit(AuditWriter::flushSafely).get(5, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }, "tms282-audit-shutdown"));
        } catch (IllegalStateException ignored) {
            // already shutting down
        }
    }

    /** The audit background thread; used for ban-table statements so they never run on game threads. */
    static ScheduledExecutorService executor() {
        start();
        return EXEC;
    }

    /** Non-blocking enqueue. Returns false (and counts a drop) if disabled or the queue is full. */
    static boolean submit(String table, Object... values) {
        if (!ENABLED) {
            return false;
        }
        if (!STARTED.get()) {
            start();
        }
        if (QUEUE.offer(new Row(table, values))) {
            return true;
        }
        long dropped = DROPPED.incrementAndGet();
        long now = System.currentTimeMillis();
        long last = LAST_DROP_WARN.get();
        if (now - last > 60_000L && LAST_DROP_WARN.compareAndSet(last, now)) {
            System.err.println("[AUDIT] queue full (" + CAPACITY + "), dropped rows so far: " + dropped);
        }
        return false;
    }

    public static long dropped() {
        return DROPPED.get();
    }

    public static long written() {
        return WRITTEN.get();
    }

    public static long failed() {
        return FAILED.get();
    }

    public static int pending() {
        return QUEUE.size();
    }

    public static int capacity() {
        return CAPACITY;
    }

    /** Requests an immediate flush on the writer thread (non-blocking). */
    public static void flushAsync() {
        executor().execute(AuditWriter::flushSafely);
    }

    private static void flushSafely() {
        try {
            flush();
        } catch (Throwable t) {
            System.err.println("[AUDIT] flush error: " + t);
        }
    }

    private static void flush() {
        if (QUEUE.isEmpty() || !DatabaseManager.isInitialized()) {
            return; // keep rows until the pool is up; the bounded queue caps memory
        }
        if (!ensureSchema()) {
            return;
        }
        List<Row> drained = new ArrayList<>(Math.min(QUEUE.size(), MAX_PER_PASS));
        while (QUEUE.drainTo(drained, MAX_PER_PASS) > 0) {
            writeBatch(drained);
            drained.clear();
            if (!DatabaseManager.isInitialized()) {
                return;
            }
        }
    }

    static boolean ensureSchema() {
        if (schemaReady) {
            return true;
        }
        if (!DatabaseManager.isInitialized() || Boolean.getBoolean("tms282.tests.rejectRealDatabase")) {
            return false; // package checks: never touch the database (ban lists stay empty)
        }
        try (Connection con = DatabaseManager.getConnection(); Statement st = con.createStatement()) {
            for (String ddl : AuditSchema.DDL) {
                st.execute(ddl);
            }
            schemaReady = true;
            return true;
        } catch (SQLException e) {
            System.err.println("[AUDIT] could not create audit tables: " + e.getMessage());
            return false;
        }
    }

    private static void writeBatch(List<Row> rows) {
        Map<String, List<Object[]>> byTable = new LinkedHashMap<>();
        for (Row row : rows) {
            byTable.computeIfAbsent(row.table(), k -> new ArrayList<>()).add(row.values());
        }
        try (Connection con = DatabaseManager.getConnection()) {
            boolean autoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                for (Map.Entry<String, List<Object[]>> e : byTable.entrySet()) {
                    try (PreparedStatement ps = con.prepareStatement(AuditSchema.insertSql(e.getKey()))) {
                        for (Object[] values : e.getValue()) {
                            bind(ps, values);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                con.commit();
                WRITTEN.addAndGet(rows.size());
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                FAILED.addAndGet(rows.size());
                DataPrinter.send("log-audit-error", "[AUDIT] batch of " + rows.size() + " rows failed: " + e.getMessage(), true);
            } finally {
                try {
                    con.setAutoCommit(autoCommit);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            FAILED.addAndGet(rows.size());
            System.err.println("[AUDIT] no connection for audit batch: " + e.getMessage());
        }
    }

    static void bind(PreparedStatement ps, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            Object v = values[i];
            if (v == null) {
                ps.setNull(i + 1, Types.NULL);
            } else if (v instanceof String s) {
                ps.setString(i + 1, s.length() > MAX_STRING ? s.substring(0, MAX_STRING) : s);
            } else {
                ps.setObject(i + 1, v);
            }
        }
    }
}
