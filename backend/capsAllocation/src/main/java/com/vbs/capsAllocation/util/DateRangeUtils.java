package com.vbs.capsAllocation.util;

import com.vbs.capsAllocation.common.DateRange;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateRangeUtils {

    /**
     * Calculates the number of working days (excluding weekends) between two dates
     * (inclusive).
     * Weekends are defined as Saturday and Sunday.
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return Number of working days
     */
    public static int getWorkingDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        int workingDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();
            // Exclude Saturday and Sunday
            if (dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    /**
     * Calculates the number of working days excluding weekends AND holidays.
     * 
     * @param startDate    Start date (inclusive)
     * @param endDate      End date (inclusive)
     * @param holidayDates Set of holiday dates to exclude (from Holiday table)
     * @return Number of working days
     */
    public static int getWorkingDaysExcludingHolidays(LocalDate startDate, LocalDate endDate,
            java.util.Set<LocalDate> holidayDates) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (holidayDates == null) {
            holidayDates = java.util.Collections.emptySet();
        }

        int workingDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();
            // Exclude Saturday, Sunday, AND holidays
            boolean isWeekend = dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                    dayOfWeek == java.time.DayOfWeek.SUNDAY;
            boolean isHoliday = holidayDates.contains(current);

            if (!isWeekend && !isHoliday) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    /**
     * Calculates working days for a specific user, considering their actual
     * entries.
     * Base working days = weekdays - holidays
     * Plus any weekend/holiday days where user has actually submitted entries
     * 
     * @param startDate      Start date (inclusive)
     * @param endDate        End date (inclusive)
     * @param holidayDates   Set of holiday dates to exclude
     * @param userEntryDates Set of dates where user has submitted entries
     * @return Number of effective working days for this user
     */
    public static int getEffectiveWorkingDaysForUser(LocalDate startDate, LocalDate endDate,
            java.util.Set<LocalDate> holidayDates,
            java.util.Set<LocalDate> userEntryDates) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (holidayDates == null) {
            holidayDates = java.util.Collections.emptySet();
        }
        if (userEntryDates == null) {
            userEntryDates = java.util.Collections.emptySet();
        }

        int workingDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            java.time.DayOfWeek dayOfWeek = current.getDayOfWeek();
            boolean isWeekend = dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                    dayOfWeek == java.time.DayOfWeek.SUNDAY;
            boolean isHoliday = holidayDates.contains(current);

            // Count as working day if:
            // 1. It's a normal weekday and not a holiday, OR
            // 2. The user has submitted entries on this day (even if weekend/holiday)
            if ((!isWeekend && !isHoliday) || userEntryDates.contains(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    /**
     * Calculates the previous equivalent date range based on the current range.
     * Logic:
     * 1. Calculate days in current range: days = ChronoUnit.DAYS.between(startDate,
     * endDate) + 1
     * 2. previousEnd = startDate.minusDays(1)
     * 3. previousStart = previousEnd.minusDays(days - 1)
     *
     * @param startDate Current range start date
     * @param endDate   Current range end date
     * @return DateRange object containing previous start and end dates
     */
    public static DateRange getPreviousRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate previousEnd = startDate.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1);

        return new DateRange(previousStart, previousEnd);
    }
}
