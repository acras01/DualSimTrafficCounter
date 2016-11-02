package ua.od.acros.dualsimtrafficcounter.receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        //start CountService
        if (!prefs.getBoolean(Constants.PREF_OTHER[5], false))
            context.startService(new Intent(context, TrafficCountService.class));
        //start CallLoggerService
        if (!prefs.getBoolean(Constants.PREF_OTHER[24], false))
            context.startService(new Intent(context, CallLoggerService.class));

        //start WatchDogService
        /*if (prefs.getBoolean(Constants.PREF_OTHER[4], true))
            context.startService(new Intent(context, WatchDogService.class));*/

        //start FloatingWindow
        /*if (prefs.getBoolean(Constants.PREF_OTHER[32], false) &&
                ((prefs.getBoolean(Constants.PREF_OTHER[41], false) && MobileUtils.isMobileDataActive(context)) ||
                        !prefs.getBoolean(Constants.PREF_OTHER[41], false)))
            StandOutWindow.show(context, FloatingWindowService.class, prefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID));*/

        /*CustomApplication.setOnOffAlarms(context);
        CustomApplication.setCallResetAlarm(context);*/
    }
}
