package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.AlarmManager;
import android.content.ContentValues;
import android.content.SharedPreferences;

import org.joda.time.DateTime;
import org.joda.time.Days;

public class DateUtils {

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

    public static DateTime getResetDate(int sim, ContentValues contentValues, SharedPreferences preferences, String[] simPref) {
        DateTime now = new DateTime().withTimeAtStartOfDay();
        int delta = 0;
        String period = "";
        DateTime last;
        String date = preferences.getString(simPref[3], "");
        switch (sim) {
            case Constants.SIM1:
                period = Constants.PERIOD1;
                break;
            case Constants.SIM2:
                period = Constants.PERIOD2;
                break;
            case Constants.SIM3:
                period = Constants.PERIOD3;
                break;
        }
        if (!date.equals(""))
            last = Constants.DATE_TIME_FORMATTER.parseDateTime(date).withTimeAtStartOfDay();
        else
            last = Constants.DATE_FORMATTER.parseDateTime("1970-01-01");
        switch (preferences.getString(simPref[0], "")) {
            case "0":
                delta = 1;
                break;
            case "1":
                delta = Integer.parseInt(preferences.getString(simPref[2], "1"));
                if (delta >= 28)
                    switch (now.getMonthOfYear()) {
                        case 2:
                            if (now.year().isLeap())
                                delta = 29;
                            else
                                delta = 28;
                            break;
                        case 4:
                        case 6:
                        case 9:
                        case 11:
                            if (delta == 31)
                                delta = 30;
                            break;
                    }
                break;
            case "2":
                delta = Integer.parseInt(preferences.getString(simPref[2], "1"));
                break;
        }
        int diff = Days.daysBetween(last.toLocalDate(), now.toLocalDate()).getDays();
        if (preferences.getString(simPref[0], "").equals("1")) {
            int month = now.getMonthOfYear();
            int daysInMonth = 31;
            switch (last.getMonthOfYear()) {
                case 2:
                    if (last.year().isLeap())
                        daysInMonth = 29;
                    else
                        daysInMonth = 28;
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    daysInMonth = 30;
                    break;
            }
            if (now.getDayOfMonth() > delta && diff < daysInMonth)
                month += 1;
            date = now.getYear() + "-" + month + "-" + delta;
            return Constants.DATE_TIME_FORMATTER.parseDateTime(date + " " + preferences.getString(simPref[1], "00:00"));
        } else {
            if (preferences.getString(simPref[0], "").equals("2"))
                contentValues.put(period, diff);
            if (diff >= delta) {
                if (preferences.getString(simPref[0], "").equals("2"))
                    contentValues.put(period, 0);
                return Constants.DATE_TIME_FORMATTER.parseDateTime(now.toString(Constants.DATE_FORMATTER) + " " + preferences.getString(simPref[1], "00:00"));
            } else
                return null;
        }
    }

    public static DateTime setResetDate(SharedPreferences preferences, String[] simPref) {
        DateTime now;
        String time = preferences.getString(simPref[1], "00:00");
        now = new DateTime()
                .withHourOfDay(Integer.valueOf(time.split(":")[0]))
                .withMinuteOfHour(Integer.valueOf(time.split(":")[1]));
        int delta;
        switch (preferences.getString(simPref[0], "")) {
            case "0":
                delta = 1;
                return now.plusDays(delta);
            case "1":
                delta = Integer.parseInt(preferences.getString(simPref[2], "1"));
                if (delta >= now.getDayOfMonth())
                    return now.withDayOfMonth(delta);
                else
                    return now.plusMonths(1).withDayOfMonth(delta);
            case "2":
                delta = Integer.parseInt(preferences.getString(simPref[2], "1"));
                return now.plusDays(delta);
        }
        return null;
    }

    public static long getInterval(SharedPreferences sharedPreferences, int sim) {
        long interval;
        DateTime now = new DateTime().withTimeAtStartOfDay();
        String[] pref = new String[Constants.PREF_SIM1.length];
        switch (sim) {
            case Constants.SIM1:
                pref = Constants.PREF_SIM1;
                break;
            case Constants.SIM2:
                pref = Constants.PREF_SIM2;
                break;
            case Constants.SIM3:
                pref = Constants.PREF_SIM3;
                break;
        }
        switch (sharedPreferences.getString(pref[3], "")) {
            default:
            case "0":
                interval = AlarmManager.INTERVAL_DAY;
                break;
            case "1":
                switch (now.getMonthOfYear()) {
                    case 2:
                        if (now.year().isLeap())
                            interval = 29;
                        else
                            interval = 28;
                        break;
                    case 4:
                    case 6:
                    case 9:
                    case 11:
                        interval = 30;
                        break;
                    default:
                        interval = 31;
                }
                interval *= AlarmManager.INTERVAL_DAY;
                break;
            case "2":
                interval = Integer.parseInt(sharedPreferences.getString(pref[10], "1")) * AlarmManager.INTERVAL_DAY;
                break;
        }
        return interval;
    }
}
