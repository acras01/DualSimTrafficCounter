package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;

import ua.od.acros.dualsimtrafficcounter.events.MobileConnectionEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoConnectivityEvent;
import ua.od.acros.dualsimtrafficcounter.services.FloatingWindowService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import wei.mark.standout.StandOutWindow;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        boolean floatingWindow = prefs.getBoolean(Constants.PREF_OTHER[32], false);
        boolean alwaysShow = !prefs.getBoolean(Constants.PREF_OTHER[41], false);
        if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
            if (floatingWindow && alwaysShow)
                closeFloatingWindow(context, prefs);
            if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                EventBus.getDefault().post(new NoConnectivityEvent());
        } else {
            if (MobileUtils.isMobileDataActive(context)) {
                if (floatingWindow && alwaysShow)
                    showFloatingWindow(context, prefs);
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
                if (floatingWindow && alwaysShow)
                    closeFloatingWindow(context, prefs);
            }
        }
    }

    private void showFloatingWindow(Context context, SharedPreferences preferences) {
        closeFloatingWindow(context, preferences);
        int id = Math.abs(new Random().nextInt());
        preferences.edit()
                .putInt(Constants.PREF_OTHER[38], id)
                .apply();
        StandOutWindow.show(context, FloatingWindowService.class, id);
    }

    private void closeFloatingWindow(Context context, SharedPreferences preferences) {
        int id = preferences.getInt(Constants.PREF_OTHER[38], -1);
        if (id >= 0)
            StandOutWindow.close(context, FloatingWindowService.class, id);
        else
            StandOutWindow.closeAll(context, FloatingWindowService.class);
    }
}
