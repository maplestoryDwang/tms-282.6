package net.swordie.ms.connection.hikariCP;

import net.swordie.ms.util.FileTime;

import java.sql.Timestamp;
import java.time.*;

public class FileTimeConverter {

    // create "custom" LDTs for our own min/max filetime values (max FT value is invalid, as it has 5 year digits)
    private static final LocalDateTime MAX_LDT = LocalDateTime.of(LocalDate.of(9999, 1, 1), LocalTime.of(0, 0, 1));
    private static final LocalDateTime MIN_LDT = LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(0, 0, 1));

    public Timestamp convertToDatabaseColumn(FileTime fileTime) {
        Instant instant;
        if (fileTime == null) {
            instant = null;
        } else if (fileTime.isMaxTime()) {
            instant = MAX_LDT.atZone(ZoneId.systemDefault()).toInstant();
        } else if (fileTime.isMinTime()) {
            instant = MIN_LDT.atZone(ZoneId.systemDefault()).toInstant();
        } else {
            instant = fileTime.toInstant();
        }
        return instant == null ? null : Timestamp.from(instant);
    }
}
