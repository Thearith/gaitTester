package org.smcnus.irace_gaittester.Helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTime {

    private static final String SINGPAORE_TIMEZONE = "Asia/Singapore";

    public static final int MILLISECOND_RATE = 1000;
    public static final int SECOND_RATE = 60;

    private static final String DATE_FORMAT = "MMMM dd yyyy | HH:mm a";
    private static final String TIMESTAMP_FORMAT = "MMMM dd yyyy | HH:mm:ss:SSS";
    private static final String TIME_FORMAT = "HH:mm:ss:SSS";


    public static long getIntegerCurrentTimestamp() {
        Date date = getTimezoneTimestamp();
        return date.getTime();
    }

    /*
    * Converts back and forth from milliseconds to "MMMM dd yyyy | HH:mm a" format
    * */

    public static String getCurrentDate() {
        Date date = getTimezoneTimestamp();
        String dateFormat = getDateFormat(date.getTime(), DATE_FORMAT);
        return dateFormat;
    }

    public static long getDate(String dateString) {
        return getDateFormat(dateString, DATE_FORMAT);
    }


    /*
    * Converts back and forth from milliseconds to "MMMM dd yyyy | HH:mm:ss:SSS" format
    * */

    public static String getCurrentTimestamp() {
        Date date = getTimezoneTimestamp();
        String dateFormat = getDateFormat(date.getTime(), TIMESTAMP_FORMAT);
        return dateFormat;
    }

    public static long getTimestamp(String dateString) {
        return getDateFormat(dateString, TIMESTAMP_FORMAT);
    }

    public static String getTimestamp(long timestamp) {
        String dateFormat = getDateFormat(timestamp, TIMESTAMP_FORMAT);
        return dateFormat;
    }


    /*
    * Converts back and forth from milliseconds to "HH:mm:ss:SSS" format
    * */

    public static String getCurrentTime() {
        Date date = getTimezoneTimestamp();
        return getCurrentTime(date.getTime());
    }

    public static String getCurrentTime(long timestamp) {
        String dateFormat = getDateFormat(timestamp, TIME_FORMAT);
        return dateFormat;
    }

    // converts from "MMMM dd yyyy | HH:mm:ss:SSS" to "HH:mm:ss:SSS"
    public static String getCurrentTime(String dateString) {
        long timestamp = getTimestamp(dateString);
        return getCurrentTime(timestamp);
    }

    public static long getIntegerCurrentTime(String dateString) {
        return getDateFormat(dateString, TIME_FORMAT);
    }


    /*
    * Private helper methods
    * */

    private static Date getTimezoneTimestamp() {
        TimeZone timeZone = TimeZone.getTimeZone(SINGPAORE_TIMEZONE);
        return Calendar.getInstance(timeZone).getTime();
    }

    private static long getDateFormat(String dateString, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);

        try {
            Date date = formatter.parse(dateString);
            return date.getTime();

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static String getDateFormat(long milliseconds, String format) {
        Date date = new Date(milliseconds);
        SimpleDateFormat formatter = new SimpleDateFormat(format);

        return formatter.format(date);
    }
}
