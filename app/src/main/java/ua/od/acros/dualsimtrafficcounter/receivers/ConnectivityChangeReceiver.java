package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.events.MobileConnectionEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoConnectivityEvent;
import ua.od.acros.dualsimtrafficcounter.services.FloatingWindowService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        boolean floatingWindow = prefs.getBoolean(Constants.PREF_OTHER[32], false);
        boolean alwaysShow = !prefs.getBoolean(Constants.PREF_OTHER[41], false);
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                if (floatingWindow && !alwaysShow)
                    FloatingWindowService.closeFloatingWindow(context, prefs);
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    EventBus.getDefault().post(new NoConnectivityEvent());
            } else {
                if (MobileUtils.hasActiveNetworkInfo(context) == 2) {
                    if (floatingWindow && alwaysShow)
                        FloatingWindowService.showFloatingWindow(context, prefs);
                    if (!CustomApplication.isMyServiceRunning(TrafficCountService.class) &&
                            !prefs.getBoolean(Constants.PREF_OTHER[5], false)) {
                        Intent i = new Intent(context, TrafficCountService.class);
                        i.setAction(intent.getAction());
                        i.putExtras(intent.getExtras());
                        i.setFlags(intent.getFlags());
                        context.startService(i);
                    } else if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                        EventBus.getDefault().post(new MobileConnectionEvent());
                } else {
                    if (floatingWindow && !alwaysShow)
                        FloatingWindowService.closeFloatingWindow(context, prefs);
                }
            }
        }
    }
}
