package ua.od.acros.dualsimtrafficcounter;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, CountService.class));

        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);

        //Scheduled ON/OFF
        if (!prefs.getString(Constants.PREF_SIM1[11], "0").equals("3") || !prefs.getString(Constants.PREF_SIM2[11], "0").equals("3")
                || !prefs.getString(Constants.PREF_SIM3[11], "0").equals("3")) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Calendar clndr = Calendar.getInstance();
            if (prefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM1[11], "0").equals("1")) {
                Intent i1Off = new Intent(context, OnOffReceiver.class);
                i1Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1Off.putExtra(Constants.ON_OFF, false);
                i1Off.setAction(Constants.ALARM_ACTION);
                int SIM1_OFF = 100;
                PendingIntent pi1Off = PendingIntent.getBroadcast(context, SIM1_OFF, i1Off, 0);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(prefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(prefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi1Off);
            }
            if (prefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM1[11], "0").equals("2")) {
                Intent i1On = new Intent(context, OnOffReceiver.class);
                i1On.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1On.putExtra(Constants.ON_OFF, true);
                i1On.setAction(Constants.ALARM_ACTION);
                int SIM1_ON = 101;
                PendingIntent pi1On = PendingIntent.getBroadcast(context, SIM1_ON, i1On, 0);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(prefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(prefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi1On);
            }
            if (prefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM2[11], "0").equals("1")) {
                Intent i2Off = new Intent(context, OnOffReceiver.class);
                i2Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2Off.putExtra(Constants.ON_OFF, false);
                i2Off.setAction(Constants.ALARM_ACTION);
                int SIM2_OFF = 110;
                PendingIntent pi2Off = PendingIntent.getBroadcast(context, SIM2_OFF, i2Off, 0);
                am.cancel(pi2Off);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(prefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(prefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi2Off);
            }
            if (prefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM2[11], "0").equals("2")) {
                Intent i2On = new Intent(context, OnOffReceiver.class);
                i2On.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2On.putExtra(Constants.ON_OFF, true);
                i2On.setAction(Constants.ALARM_ACTION);
                int SIM2_ON = 111;
                PendingIntent pi2On = PendingIntent.getBroadcast(context, SIM2_ON, i2On, 0);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(prefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(prefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi2On);
            }
            if (prefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM3[11], "0").equals("1")) {
                Intent i3Off = new Intent(context, OnOffReceiver.class);
                i3Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3Off.putExtra(Constants.ON_OFF, false);
                i3Off.setAction(Constants.ALARM_ACTION);
                int SIM3_OFF = 120;
                PendingIntent pi3Off = PendingIntent.getBroadcast(context, SIM3_OFF, i3Off, 0);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(prefs.getString(Constants.PREF_SIM3[12], "23:35").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(prefs.getString(Constants.PREF_SIM3[12], "23:55").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi3Off);
            }
            if (prefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM3[11], "0").equals("2")) {
                Intent i3On = new Intent(context, OnOffReceiver.class);
                i3On.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3On.putExtra(Constants.ON_OFF, true);
                i3On.setAction(Constants.ALARM_ACTION);
                int SIM3_ON = 121;
                PendingIntent pi3On = PendingIntent.getBroadcast(context, SIM3_ON, i3On, 0);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(prefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(prefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi3On);
            }
        }
        //start CountService
        if (prefs.getBoolean(Constants.PREF_OTHER[4], true))
            context.startService(new Intent(context, WatchDogService.class));
    }
}
