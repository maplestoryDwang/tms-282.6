package net.swordie.ms.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class Common {

    public static String getCurrentDateAsFormat(String format) {
        return OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(format));
    }

    public static long getMillSecondsOfDate(String format, String dateString) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date date = formatter.parse(dateString);
        return date.getTime();
    }

    public static String getTimeStringWidthFormat(String format, long currentTime) {
        Date date = new Date(currentTime);
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static int getDateFromCurrentTime(long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(currentTime));
        return calendar.get(Calendar.DATE);
    }

    public static int getValueFromTime(long time, int type) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(time));
        return calendar.get(type);
    }

    public static boolean isNextDay(long time) {
        long currentTime = System.currentTimeMillis();
        //Dif date
        if (getValueFromTime(time, Calendar.DATE) != getValueFromTime(currentTime, Calendar.DATE)) {
            return true;
        }
        //Same date, dif month
        else if (getValueFromTime(time, Calendar.DATE) == getValueFromTime(currentTime, Calendar.DATE) &&
                getValueFromTime(time, Calendar.MONTH) != getValueFromTime(currentTime, Calendar.MONTH)) {
            return true;
        }
        //Same date, same month, dif year.
        else if (getValueFromTime(time, Calendar.DATE) == getValueFromTime(currentTime, Calendar.DATE) &&
                getValueFromTime(time, Calendar.MONTH) == getValueFromTime(currentTime, Calendar.MONTH) &&
                getValueFromTime(time, Calendar.YEAR) != getValueFromTime(currentTime, Calendar.YEAR)) {
            return true;
        }
        else {
            return false;
        }
    }
}
