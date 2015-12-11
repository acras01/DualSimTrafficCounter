package ua.od.acros.dualsimtrafficcounter.utils;

import org.joda.time.DateTime;


public class DateCompare {

    public static boolean isNextDayOrMonth(DateTime date, String period) {
        DateTime now = new DateTime().withTimeAtStartOfDay();
        switch (period) {
            case "0":
                return now.getDayOfYear() != date.getDayOfYear();
            case "1":
                return now.getMonthOfYear() != date.getMonthOfYear();
            default:
                return false;
        }
    }
}
