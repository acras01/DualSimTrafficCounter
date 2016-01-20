package ua.od.acros.dualsimtrafficcounter;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, CountService.class));

        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.PREF_OTHER[4], true))
            context.startService(new Intent(context, WatchDogService.class));
    }
}
