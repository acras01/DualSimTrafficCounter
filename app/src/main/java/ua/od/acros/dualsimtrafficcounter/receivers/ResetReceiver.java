package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.events.ClearCallsEvent;
import ua.od.acros.dualsimtrafficcounter.events.ClearTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class ResetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        switch (intent.getAction()) {
            case Constants.RESET_ACTION:
                if (!CheckServiceRunning.isMyServiceRunning(TrafficCountService.class, context) &&
                        !prefs.getBoolean(Constants.PREF_OTHER[5], false)) {
                    Intent i = new Intent(context, TrafficCountService.class);
                    i.setAction(intent.getAction());
                    i.putExtras(intent.getExtras());
                    i.setFlags(intent.getFlags());
                    context.startService(i);
                } else if (CheckServiceRunning.isMyServiceRunning(TrafficCountService.class, context))
                    EventBus.getDefault().post(new ClearTrafficEvent(intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED)));
                break;
            case Constants.RESET_ACTION_CALLS:
                if (!CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, context) &&
                        !prefs.getBoolean(Constants.PREF_OTHER[24], false)) {
                    Intent i = new Intent(context, CallLoggerService.class);
                    i.setAction(intent.getAction());
                    i.putExtras(intent.getExtras());
                    i.setFlags(intent.getFlags());
                    context.startService(i);
                } else if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, context))
                    EventBus.getDefault().post(new ClearCallsEvent(intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED)));
                break;
        }

        if (wl.isHeld())
            wl.release();
    }
}
