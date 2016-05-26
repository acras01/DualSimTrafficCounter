package ua.od.acros.dualsimtrafficcounter.receivers;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.joda.time.DateTime;

import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.services.WatchDogService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.FloatingWindow;
import wei.mark.standout.StandOutWindow;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        //start CountService
        if (!prefs.getBoolean(Constants.PREF_OTHER[5], false))
            context.startService(new Intent(context, TrafficCountService.class));
        //start CallLoggerService
        if (!prefs.getBoolean(Constants.PREF_OTHER[24], false))
            context.startService(new Intent(context, CallLoggerService.class));

        //start WatchDogService
        if (prefs.getBoolean(Constants.PREF_OTHER[4], true))
            context.startService(new Intent(context, WatchDogService.class));

        //start FloatingWindow
        if (prefs.getBoolean(Constants.PREF_OTHER[32], false))
            StandOutWindow.show(context, FloatingWindow.class, prefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID));

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        DateTime alarmTime = new DateTime().withTimeAtStartOfDay();
        //Calls reset
        if (prefs.getBoolean(Constants.PREF_OTHER[25], true)) {
            Intent iReset = new Intent(context, ResetReceiver.class);
            iReset.setAction(Constants.RESET_ACTION);
            final int RESET = 1981;
            PendingIntent piReset = PendingIntent.getBroadcast(context, RESET, iReset, 0);
            if (alarmTime.getMillis() < System.currentTimeMillis())
                alarmTime = alarmTime.plusDays(1);
            am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, piReset);
        }
        //Scheduled ON/OFF
        if (!prefs.getString(Constants.PREF_SIM1[11], "0").equals("3") || !prefs.getString(Constants.PREF_SIM2[11], "0").equals("3")
                || !prefs.getString(Constants.PREF_SIM3[11], "0").equals("3")) {
            if (prefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM1[11], "0").equals("1")) {
                Intent i1Off = new Intent(context, OnOffReceiver.class);
                i1Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1Off.putExtra(Constants.ON_OFF, false);
                i1Off.setAction(Constants.ALARM_ACTION);
                final int SIM1_OFF = 100;
                PendingIntent pi1Off = PendingIntent.getBroadcast(context, SIM1_OFF, i1Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1Off);
            }
            if (prefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM1[11], "0").equals("2")) {
                Intent i1On = new Intent(context, OnOffReceiver.class);
                i1On.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1On.putExtra(Constants.ON_OFF, true);
                i1On.setAction(Constants.ALARM_ACTION);
                final int SIM1_ON = 101;
                PendingIntent pi1On = PendingIntent.getBroadcast(context, SIM1_ON, i1On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1On);
            }
            if (prefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM2[11], "0").equals("1")) {
                Intent i2Off = new Intent(context, OnOffReceiver.class);
                i2Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2Off.putExtra(Constants.ON_OFF, false);
                i2Off.setAction(Constants.ALARM_ACTION);
                final int SIM2_OFF = 110;
                PendingIntent pi2Off = PendingIntent.getBroadcast(context, SIM2_OFF, i2Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2Off);
            }
            if (prefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM2[11], "0").equals("2")) {
                Intent i2On = new Intent(context, OnOffReceiver.class);
                i2On.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2On.putExtra(Constants.ON_OFF, true);
                i2On.setAction(Constants.ALARM_ACTION);
                final int SIM2_ON = 111;
                PendingIntent pi2On = PendingIntent.getBroadcast(context, SIM2_ON, i2On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2On);
            }
            if (prefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM3[11], "0").equals("1")) {
                Intent i3Off = new Intent(context, OnOffReceiver.class);
                i3Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3Off.putExtra(Constants.ON_OFF, false);
                i3Off.setAction(Constants.ALARM_ACTION);
                final int SIM3_OFF = 120;
                PendingIntent pi3Off = PendingIntent.getBroadcast(context, SIM3_OFF, i3Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[12], "23:35").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3Off);
            }
            if (prefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM3[11], "0").equals("2")) {
                Intent i3On = new Intent(context, OnOffReceiver.class);
                i3On.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3On.putExtra(Constants.ON_OFF, true);
                i3On.setAction(Constants.ALARM_ACTION);
                final int SIM3_ON = 121;
                PendingIntent pi3On = PendingIntent.getBroadcast(context, SIM3_ON, i3On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3On);
            }
        }
    }
}
