package net.swordie.ms.connection.hikariCP;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {

    protected static String url = ServerRuntimeConfig.jdbcUrl();
    protected static String username = ServerRuntimeConfig.stringValue("db.user");
    protected static String password = ServerRuntimeConfig.stringValue("db.password");
    protected static HikariDataSource con;

    public static boolean isInitialized() {
        return con != null && !con.isClosed();
    }

    public static synchronized boolean reloadRuntimeSettingsIfIdle() {
        if (isInitialized()) {
            return false;
        }
        ServerRuntimeConfig.reload();
        url = ServerRuntimeConfig.jdbcUrl();
        username = ServerRuntimeConfig.stringValue("db.user");
        password = ServerRuntimeConfig.stringValue("db.password");
        return true;
    }

    public static synchronized void ensureInitialized() throws SQLException {
        initPool(false);
    }

    public static synchronized void init() {
        try {
            initPool(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        DatabaseManager.executeStatement("UPDATE users SET clientstate = 0");
        DatabaseManager.executeStatement("UPDATE characters SET party = 0");
    }

    private static synchronized void initPool(boolean exitOnFailure) throws SQLException {
        if (isInitialized()) {
            return;
        }
        if (Boolean.getBoolean("tms282.tests.rejectRealDatabase")) {
            throw new SQLException("Package checks require an injected database double; real connection refused");
        }
        long startNow = System.currentTimeMillis();
        ServerRuntimeConfig.reload();
        url = ServerRuntimeConfig.jdbcUrl();
        username = ServerRuntimeConfig.stringValue("db.user");
        password = ServerRuntimeConfig.stringValue("db.password");
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMinimumIdle(10);
            hikariConfig.setMaximumPoolSize(100);
            hikariConfig.setConnectionTimeout(5000L);
            hikariConfig.setIdleTimeout(TimeUnit.MINUTES.toMillis(30L));
            hikariConfig.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(60));
            hikariConfig.setValidationTimeout(3000);
            hikariConfig.setMaxLifetime(TimeUnit.MINUTES.toMillis(60));
            con = new HikariDataSource(hikariConfig);
            System.out.println("[HikariCP Database] Connect: Success");
            System.out.printf("[HikariCP Database] Loaded Database in %d ms%n", System.currentTimeMillis() - startNow);
        } catch (Exception exception) {
            System.err.println("[HikariCP Database] Exception during initialization: " + exception);
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
            if (exitOnFailure) {
                System.exit(1);
            }
            throw exception instanceof SQLException sqlException
                    ? sqlException
                    : new SQLException("數據庫連接失敗: " + exception.getMessage(), exception);
        }
    }

    public static Connection getConnection() throws SQLException {
        initPool(false);
        return con.getConnection();
    }

    public static ResultSet executeQuery(String query) {
        if (query.contains("DROP TABLE") || query.contains("TRUNCATE TABLE") || query.contains("DROP DATABASE")) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, query);
            return null;
        }

        try {
            DataPrinter.send(DataPrinter.HIKARICP, query, true);
            Connection connection = DatabaseManager.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            // Extract the result set data in-memory
            CachedRowSet cachedRowSet = RowSetProvider.newFactory().createCachedRowSet();
            cachedRowSet.populate(resultSet);

            // Close the resources
            statement.close();
            connection.close();

            return cachedRowSet;
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
            return null;
        }
    }


    public static void closeQuery(ResultSet rs, Statement ps, Connection con, String query) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception exception) {
            System.err.println("[HikariCP Database] Exception closing ResultSet: " + exception);
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }

        try {
            if (ps != null) {
                ps.close();
            }
        } catch (Exception exception) {
            System.err.println("[HikariCP Database] Exception closing Statement: " + exception);
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }

        try {
            if (con != null) {
                con.close();
            }
        } catch (Exception exception) {
            System.err.println("[HikariCP Database] Exception closing Connection: " + exception);
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }

        if (ServerConfig.SQL_DEBUG) {
            System.out.println("\u001B[35m[Query] " + query + "\u001B[0m");
        }
        // FilePrinter.print(FilePrinter.HIKARICP, query);
    }

    public static boolean executeStatement(String query) {
        if (query.contains("DROP TABLE") || query.contains("TRUNCATE TABLE") || query.contains("DROP DATABASE")) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, query);
            return false;
        }

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            DataPrinter.send(DataPrinter.HIKARICP, query, true);
            ps.execute();
            return true;
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
            return false;
        }
    }

    public static long executeStatementReturnID(String query) {
        if (query.contains("DROP TABLE") || query.contains("TRUNCATE TABLE") || query.contains("DROP DATABASE")) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, query);
            return 0;
        }

        long id = 0;

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            DataPrinter.send(DataPrinter.HIKARICP, query, true);
            ps.execute();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    id = rs.getLong(1);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }

        return id;
    }

    public static ArrayList<String> getTablesList() {

        ArrayList<String> listOfTable = new ArrayList<>();
        try {
            DatabaseMetaData md = getConnection().getMetaData();
            ResultSet rs = md.getTables(null, null, "%", null);
            while (rs.next()) {
                if (rs.getString(4).equalsIgnoreCase("TABLE")) {
                    listOfTable.add(rs.getString(3));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return listOfTable;
    }

    public static FileTime getFileTimeFromString(String value) throws ParseException {
        //type 0: yyyy-MM-dd HH:mm:ss.SSS
        //type 1: yyyy-MM-dd HH:mm:ss
        if (value != null) {
            boolean isMilliSecond = value.length() == 23;
            if (isMilliSecond) {
                return FileTime.fromLong(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(value).getTime());
            } else {
                return FileTime.fromLong(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value).getTime());
            }
        }
        return null;
    }

    public static Timestamp convertToDateTimeSQL(FileTime fileTime) {
        return new FileTimeConverter().convertToDatabaseColumn(fileTime);
    }

    public static String getSQLStringSyntax(boolean isUpdate, String columnName, Object object, boolean isLastColumn) {
        String syntax = "";
        if (isUpdate) {
            syntax = String.format("%s = ", columnName);
        }
        if (object != null) {
            if (object instanceof FileTime) {
                syntax += String.format("'%s'", new FileTimeConverter().convertToDatabaseColumn((FileTime) object));
            } else {
                syntax += String.format("'%s'", object);
            }
        } else {
            syntax += "NULL";
        }
        if (!isLastColumn) {
            syntax += ", ";
        }
        return syntax;
    }

    public static String getStringFilter(String value) {
        return value.contains("'") ? value.replace("'", "''") : value;
    }
}
