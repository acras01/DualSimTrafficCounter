package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.events.MobileConnectionEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoConnectivityEvent;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyApplication;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
            if (MyApplication.isMyServiceRunning(TrafficCountService.class, context))
                EventBus.getDefault().post(new NoConnectivityEvent());
        } else if (MobileUtils.getMobileDataInfo(context, false)[0] == 2) {
            SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
            if (!MyApplication.isMyServiceRunning(TrafficCountService.class, context) &&
                    !prefs.getBoolean(Constants.PREF_OTHER[5], false)) {
                Intent i = new Intent(context, TrafficCountService.class);
                i.setAction(intent.getAction());
                i.putExtras(intent.getExtras());
                i.setFlags(intent.getFlags());
                context.startService(i);
            } else if (MyApplication.isMyServiceRunning(TrafficCountService.class, context))
                EventBus.getDefault().post(new MobileConnectionEvent());
        }
    }
}
