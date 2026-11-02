package com.nutriworks.app.util;

import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for formatting dates, timestamps, computing day boundaries,
 * and user-friendly relative labels.
 */
public final class DateTimeUtils {

    private static final SimpleDateFormat FRIENDLY_DATE_FORMAT =
            new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
    private static final SimpleDateFormat SHORT_DATE_FORMAT =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("h:mm a", Locale.getDefault());

    private DateTimeUtils() {
        // Prevent instantiation
    }

    /**
     * Returns the epoch millisecond representing 00:00:00.000 for the given timestamp's day.
     */
    public static long getStartOfDay(long timestampMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Returns the epoch millisecond representing 23:59:59.999 for the given timestamp's day.
     */
    public static long getEndOfDay(long timestampMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * Formats an epoch timestamp into a user-friendly date string (e.g. "Monday, October 14").
     */
    public static String formatFriendlyDate(long timestampMillis) {
        if (timestampMillis <= 0) {
            return "";
        }
        return FRIENDLY_DATE_FORMAT.format(new Date(timestampMillis));
    }

    /**
     * Returns a human relative date ("Today", "Yesterday", "Tomorrow", or "Nov 2, 2026").
     */
    public static String getRelativeDateString(long timestampMillis) {
        long startOfTargetDay = getStartOfDay(timestampMillis);
        long startOfToday = getStartOfDay(System.currentTimeMillis());

        long dayDiff = (startOfTargetDay - startOfToday) / (24 * 60 * 60 * 1000);

        if (dayDiff == 0) {
            return "Today";
        } else if (dayDiff == -1) {
            return "Yesterday";
        } else if (dayDiff == 1) {
            return "Tomorrow";
        } else {
            return SHORT_DATE_FORMAT.format(new Date(timestampMillis));
        }
    }

    /**
     * Formats an epoch timestamp into a friendly time string (e.g. "12:30 PM").
     */
    public static String formatMealTime(long timestampMillis) {
        if (timestampMillis <= 0) {
            return "";
        }
        return TIME_FORMAT.format(new Date(timestampMillis));
    }
}
