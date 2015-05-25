package com.words.controller.utils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.NavigableSet;

public class DateTimeUtils {
    
    private static final String DATE_PATTERN = "dd.MM.yyyy";
    private static final String TIME_PATTERN = "HH:mm:ss";
    
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern(TIME_PATTERN);
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm " + DATE_PATTERN);
    
    private static final long TODAY_AS_MILLIS =
        getCurrentLocalDate().toEpochDay() * 1000 * 24 * 3600;
    
    private DateTimeUtils() {
        throw new AssertionError("Unable to instantiate utility class");
    }
    
    /**
     * Parse string to local date.
     * @param date string to parse, should adhere {DATE_PATTERN}
     * @return parsed date or epoch if date is not correct
     */
    public static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch(DateTimeParseException dtpe) {
            return LocalDate.MIN;
        }
    }
    
    /**
     * Check if specified date is valid
     * @param date string to check
     * @return true if date is valid
     */
    public static boolean isValidDate(String date) {
        if (date.length() != DATE_PATTERN.length()) return false;
        
        try {
            LocalDate.parse(date, DATE_FORMAT);
            return true;
        } catch(DateTimeParseException dtpe) {
            return false;
        }
    }
    
    /**
     * Get current local date. New day starts at 6 AM.
     * @return local date
     */
    public static LocalDate getCurrentLocalDate() {
        return LocalDateTime.now().minusHours(6).toLocalDate();
    }
    
    /**
     * Today as string
     * @return current day as string (like dd.MM.yyyy)
     */
    public static String todayAsString() {
        return DATE_FORMAT.format(getCurrentLocalDate());
    }
    
    /**
     * Yesterday as string
     * @return yesterday as string (dd.MM.yyyy)
     */
    public static String yesterdayAsString() {
        return DATE_FORMAT.format(getCurrentLocalDate().minusDays(1));
    }
    
    /**
     * Day of the week
     * @return day of week enum constant
     */
    public static DayOfWeek getDayOfWeek() {
        return getCurrentLocalDate().getDayOfWeek();
    }
    
    /**
     * Current time as string [HH:mm:ss]
     * @return string representation of current time
     */
    public static String nowAsString() {
        return TIME_FORMAT.format(LocalTime.now());
    }
    
    /**
     * Get amount of days since epoch.
     * @param date date to check
     * @return amount of days passed since epoch
     */
    public static long daysPassedSinceEpoch(String date) {
        try {
            LocalDate ld = parseDate(date);
            return ld.toEpochDay();
        } catch(DateTimeParseException dtpe) {
            return 0L;
        }
    }
    
    /**
     * Human representation of period between to dates.
     * @param date local date (like dd.MM.yyyy)
     * @return formatted representation of the period
     */
    public static String getFormattedPeriod(LocalDate date) {
        Period period = Period.between(date, getCurrentLocalDate());
        
        if (period.isNegative()) return "in the future";
        
        StringBuilder periodBuilder = new StringBuilder();
        
        if (period.toTotalMonths() > 0) {
            int years = period.getYears();
            if (years != 0) {
                periodBuilder.append(Utils.getNumeralWithWord(years, "year"));
            }
            
            int months = period.getMonths();
            if (months != 0) {
                if (periodBuilder.length() != 0) periodBuilder.append(" ");
                periodBuilder.append(
                    Utils.getNumeralWithWord(months, "month"));
            }
        } else {
            int days = period.getDays();
            if (days < 7) {
                if (days == 0) return "today";
                if (days == 1) return "yesterday";
                
                periodBuilder.append(Utils.getNumeralWithWord(days, "day"));
            } else
                periodBuilder.append(
                    Utils.getNumeralWithWord(days / 7, "week"));
        }
        
        return periodBuilder.append(" ago").toString();
    }
    
    /**
     * Human representation of period current moment and other moment in millis.
     * @param millis milliseconds after epoch
     * @return formatted representation of the period
     */
    public static String getFormattedPeriodFromMillis(long millis) {
        millis -= 21_600_000L;
        LocalDate ld = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(millis), ZoneId.systemDefault()).toLocalDate();
        return getFormattedPeriod(ld);
    }
    
    
    /**
     * Check if required number of days passed since specified date.
     * @param dateToCheck string to check in format of {DATE_PATTERN}
     * @param daysPassed amount of days to pass
     * @return true if required days passed
     */
    public static boolean isExpired(String dateToCheck, int daysPassed) {
        LocalDate date = parseDate(dateToCheck);
        return date.isBefore(getCurrentLocalDate().minusDays(daysPassed - 1));
    }
    
    /**
     * Parse local date into string
     * @param localDate date to parse
     * @return formatted string
     */
    public static String localDateToString(LocalDate localDate) {
        return localDate.format(DATE_FORMAT);
    }
    
    /**
     * Gets nearest day to {currentDate - days}.
     * @param days days to subtract
     * @param bundles available bundles
     * @return date as string or null if nearest date is not found (or last date)
     */
    public static LocalDate getNearestBundle(int days, NavigableSet<LocalDate> bundles) {
        if (days < 1 || bundles.size() < 2) return null;
        
        LocalDate date = getCurrentLocalDate().minusDays(days);
        
        LocalDate nearestDate = bundles.floor(date);
        if (nearestDate == null) return null;
        
        Period period = Period.between(nearestDate, date);
        if (Math.abs(period.getDays()) > 10) return null;
        
        // last bundle doesn't count
        if (bundles.last().equals(nearestDate)) return null;
        
        return nearestDate;
    }
    
    /**
     * Define if timestamp corresponds to today.
     * @param millis time in milliseconds to check
     * @return true if it is today
     */
    public static boolean isToday(long millis) {
        return millis >= TODAY_AS_MILLIS;
    }
    
    public static String getStringFromMillis(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime ldt =
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return DATE_FORMAT.format(ldt);
    }
    
    /**
     * Get current day as milliseconds after epoch
     * @return milliseconds after epoch
     */
    public static long getTodayAsMillis() {
        return TODAY_AS_MILLIS;
    }
    
    /**
     * Determines if today is an odd day
     * @return true if this is an odd day otherwise false
     */
    public static boolean isOddDayOfMonth() {
        return getCurrentLocalDate().getDayOfMonth() % 2 == 1;
    }
    
    /**
     * Defines amount of days between today and specified date.
     * @param date start date
     * @return difference in days between today and specified date
     */
    public static int daysPassedSince(LocalDate date) {
        Period p = Period.between(date, getCurrentLocalDate());
        return p.getDays();
    }
}
