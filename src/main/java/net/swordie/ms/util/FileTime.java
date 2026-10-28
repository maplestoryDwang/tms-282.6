package net.swordie.ms.util;

import net.swordie.ms.connection.OutPacket;

import java.io.Serializable;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

import static net.swordie.ms.ServerConfig.ZONE;

public class FileTime implements Serializable {

    private int id;
    private int lowDateTime;
    private int highDateTime;
    private boolean isConvertedForClient;

    public FileTime(int lowDateTime, int highDateTime) {
        this.lowDateTime = lowDateTime;
        this.highDateTime = highDateTime;
        if (FileTime.fromType(Type.MAX_TIME).equals(this) || FileTime.fromType(Type.ZERO_TIME).equals(this)) {
            isConvertedForClient = true;
        }
    }

    public FileTime() {
    }

    public FileTime(long time, boolean isConvertedForClient) {
        this(time);
        this.isConvertedForClient = isConvertedForClient;
    }

    /**
     * Creates a new FileTime from a given long, by splitting up the long into a low and high part
     *
     * @param time the long the FileTime should be created from
     */
    public FileTime(long time) {
        lowDateTime = (int) time;
        highDateTime = (int) (time >>> 32);
    }

    public static FileTime fromType(Type type) {
        return new FileTime(type.getVal(), true);
    }

    /**
     * Creates a new FileTime from the current time (System.currentTimeMillis()). Ensures the date is correctly c
     * alculated for the client.
     *
     * @return FileTime corresponding to the current time
     */
    public static FileTime currentTime() {
        return fromEpochMillis(System.currentTimeMillis());
    }

    /**
     * Creates a new FileTime from a given time (millis since epoch).
     *
     * @param time millis since epoch that this FileTime should correspond to
     * @return FileTime corresponding to the given time
     */
    public static FileTime fromEpochMillis(long time) {
        return fromLong(time);
    }

    /**
     * Creates a new FileTime from a given date. Ensures the date is correctly calculated for the client.
     *
     * @param localDateTime date that this FileTime should correspond to
     * @return FileTime corresponding to the given date
     */
    public static FileTime fromDate(LocalDateTime localDateTime) {
        return fromEpochMillis(localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    /**
     * Creates a new FileTime from a given long, by splitting up the long into a low and high part
     *
     * @param value the long the FileTime should be created from
     * @return FileTime from the given log
     */
    public static FileTime fromLong(long value) {
        return new FileTime((int) value, (int) (value >>> 32));
    }

    public long getLongValue() {
        return getLowDateTime() + (long) getHighDateTime() << 32;
    }

    public boolean isConvertedForClient() {
        return isConvertedForClient;
    }

    public void setConvertedForClient(boolean convertedForClient) {
        isConvertedForClient = convertedForClient;
    }

    public static LocalDateTime nowUTC() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    }

    /**
     * Creates a new copy of this FileTime
     *
     * @return the new copy
     */
    public FileTime deepCopy() {
        return new FileTime(getLowDateTime(), getHighDateTime());
    }

    public int getLowDateTime() {
        return lowDateTime;
    }

    public int getHighDateTime() {
        return highDateTime;
    }

    /**
     * Converts this FileTime (storing epoch millis) to a format that the client expects.
     *
     * @return formatted FileTime
     */
    public FileTime toClientFormat() {
        FileTime ft = fromLong((long) (toLong() - 116444736000000000L) * 100000L);
        ft.setConvertedForClient(true);
        return ft;
    }

    /**
     * Returns this FileTime as an Instant.
     *
     * @return the Instant corresponding to this FileTime
     */
    public Instant toInstant() {
        return toLocalDateTime().atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Returns the millis since epoch that this FileTime corresponds to.
     *
     * @return millis since epoch
     */
    public long toMillis() {
        if (isConvertedForClient()) {
            return (toLong() - 116444736000000000L) / 10000L;
        }
        return toLong();
    }

    /**
     * Returns the LocalDateTime that this FileTime corresponds to.
     *
     * @return corresponding date and time
     */
    public LocalDateTime toLocalDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(toMillis()), ZoneId.systemDefault());
    }

    /**
     * Encodes this FileTime to a packet
     *
     * @param outPacket the packet to encode to
     */
    public void encode(OutPacket outPacket) {
        if (!isConvertedForClient()) {
            // https://stackoverflow.com/questions/46201834/how-to-convert-milli-seconds-time-to-filetime-in-c
            outPacket.encodeLong(toLong() * 10000L + 116444736000000000L);
        } else {
            outPacket.encodeInt(getHighDateTime());
            outPacket.encodeInt(getLowDateTime());
        }
    }

    /**
     * Returns this FileTime as a long, by adding up the high and low part
     *
     * @return addition of the low and high part
     */
    public long toLong() {
        return (getLowDateTime() & 0xFFFFFFFFL) | ((long) getHighDateTime() << 32);
    }

    /**
     * Returns true if this FileTime's time is before the current time.
     *
     * @return expiredness
     */
    public boolean isExpired() {
        return !isPermanent() && toMillis() < System.currentTimeMillis();
    }

    private boolean isPermanent() {
        return equals(FileTime.fromType(Type.MAX_TIME)) || equals(new FileTime(21968699, -35635200));
    }

    /**
     * Checks if this FileTime is before a given date.
     *
     * @param localDateTime the given date
     * @return if this FileTime is before the given date
     */
    public boolean isBefore(LocalDateTime localDateTime) {
        return toLocalDateTime().isBefore(localDateTime);
    }

    /**
     * Checks if this FileTime is before a given FileTime.
     *
     * @param fileTime the given date
     * @return if this FileTime is before the given FileTime
     */
    public boolean isBefore(FileTime fileTime) {
        return toLocalDateTime().isBefore(fileTime.toLocalDateTime());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileTime fileTime = (FileTime) o;
        return lowDateTime == fileTime.lowDateTime &&
                highDateTime == fileTime.highDateTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowDateTime, highDateTime);
    }

    @Override
    public String toString() {
        return "FileTime{" +
                "lowDateTime=" + lowDateTime +
                ", highDateTime=" + highDateTime +
                '}';
    }

    public static FileTime MAX_TIME() {
        return fromType(Type.MAX_TIME);
    }

    public static FileTime MIN_TIME() {
        return fromType(Type.ZERO_TIME);
    }

    public boolean isMaxTime() {
        return equals(FileTime.fromType(Type.MAX_TIME));
    }

    public boolean isMinTime() {
        return equals(FileTime.fromType(Type.ZERO_TIME));
    }

    public String toSqlFormat() {
        if (isMaxTime()) {
            return "9999-01-01 00:00:01.000";
        } else if (isMinTime()) {
            return "1970-01-01 00:00:01.000";
        }
        LocalDateTime ldt = toLocalDateTime();
        return String.format("%04d-%02d-%02d %02d:%02d:%02d.%03d", ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), 0);
    }

    /**
     * Returns the current date as YY/MM/DD format.
     *
     * @return created string
     */
    public String toYYMMDD() {
        if (isMaxTime()) {
            return "99/01/01";
        } else if (isMinTime()) {
            return "70/01/01";
        }
        LocalDateTime ldt = toLocalDateTime();
        return String.format("%02d/%02d/%02d", ldt.getYear() % 100, ldt.getMonthValue(), ldt.getDayOfMonth());
    }

    public String toYYYYMMDD() {
        if (isMaxTime()) {
            return "99990101";
        } else if (isMinTime()) {
            return "19700101";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    public String toYYMMDD_QR() {
        if (isMaxTime()) {
            return "990101";
        } else if (isMinTime()) {
            return "700101";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yyMMdd"));
    }

    /**
     * Returns the current date+time as YYMMDDHHMMSS format.
     *
     * @return created string
     */
    public String toYYMMDDHHMMSS() {
        if (isMaxTime()) {
            return "990101010101";
        } else if (isMinTime()) {
            return "700101010101";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
    }

    public String toYYYYMMDDHHMMSS() {
        if (isMaxTime()) {
            return "99990101010101";
        } else if (isMinTime()) {
            return "19700101010101";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    public String toYYYYMMDDHHMMSSSSS() {
        if (isMaxTime()) {
            return "99990101010101000";
        } else if (isMinTime()) {
            return "19700101010101000";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    public String toYYYYMMDD_HHMMSS() {
        if (isMaxTime()) {
            return "9999-01-01 01:01:01.000";
        } else if (isMinTime()) {
            return "1970-01-01 01:01:01.000";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    public String toYYYYMMDD_HHMMssSSS_QR() {
        if (isMaxTime()) {
            return "9999/01/01 01:01:01:000";
        } else if (isMinTime()) {
            return "1970/01/01 01:01:01:000";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss:SSS"));
    }

    public String toYYYYMMDD_HHMMss() {
        if (isMaxTime()) {
            return "9999/01/01 01:01:01";
        } else if (isMinTime()) {
            return "1970/01/01 01:01:01";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
    }

    public String toRentalFormat() {
        if (isMaxTime()) {
            return "99/01/01/01/01";
        } else if (isMinTime()) {
            return "70/01/01/01/01";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yy/MM/dd/HH/mm/ss"));
    }

    public String weeklyFormat() {
        if (isMaxTime()) {
            return "99/01/01/01/01";
        } else if (isMinTime()) {
            return "70/01/01/01/01";
        }
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("yy/MM/dd/HH/mm"));
    }

    public int toYYMMDDintValue() {
        return Long.valueOf(toYYMMDD().replaceAll("/", "")).intValue();
    }

    public static FileTime getNextWeeklyResetTime() {
        var now = ZonedDateTime.now(ZONE); // Asia/Ho_Chi_Minh
        var next = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
                .withHour(7).withMinute(0).withSecond(0).withNano(0);
        if (!now.isBefore(next)) {
            next = next.plusWeeks(1);
        }
        return FileTime.fromInstant(next.toInstant());
    }

    public static FileTime fromInstant(Instant instant) {
        return new FileTime(instant.toEpochMilli());
    }

    public static boolean hasCheckedInToday(String qr) {
        if (qr == null || qr.isEmpty()) return false;
        var fmt = DateTimeFormatter.ofPattern("yy/MM/dd/HH/mm");
        try {
            var last = LocalDateTime.parse(qr, fmt).toLocalDate();
            var today = LocalDate.now(ZONE);
            return last.equals(today);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isWeeklyResetDue(String resetDate) {
        if (resetDate == null || resetDate.isEmpty()) return true;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yy/MM/dd/HH/mm");
        LocalDateTime ldt = LocalDateTime.parse(resetDate, fmt);
        ZonedDateTime due = ldt.atZone(ZONE);
        return !ZonedDateTime.now(ZONE).isBefore(due);
    }

    private static LocalDate parseOrNull(String s){
        try { return LocalDate.parse(s, DateTimeFormatter.BASIC_ISO_DATE); }
        catch (Exception e) { return null; }
    }

    public static boolean isNewDay(String s){
        var d = parseOrNull(s);
        if (d == null) return true;
        return d.isBefore(LocalDate.now(ZONE));
    }

    public static boolean isNewWeek(String s){
        var d = parseOrNull(s);
        if (d == null) return true;
        var now = LocalDate.now(ZONE);
        var lastResetWeek = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
        var currentResetWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
        return currentResetWeek.isAfter(lastResetWeek);
    }

    public static boolean isNewMonth(String s){
        LocalDate d = parseOrNull(s);
        if (d == null) return true;
        LocalDate now = LocalDate.now();
        return d.getYear() != now.getYear() || d.getMonthValue() != now.getMonthValue();
    }

    public static boolean isExpired(long time) {
        return time < System.currentTimeMillis();
    }

    public enum Type {
        // Mushy
        MAX_TIME(35120710, -1157267456),
        ZERO_TIME(21968699, -35635200),
        FT_UT_OFFSET(116444592000000000L),
        QUEST_TIME(27111903),
        PLAIN_ZERO(0);

        private long val;

        Type(long val) {
            this.val = val;
        }

        Type(int lowPart, int highPart) {
            val = lowPart + ((long) highPart << 32);
        }

        public long getVal() {
            return val;
        }
    }
}
