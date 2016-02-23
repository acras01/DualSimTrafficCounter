package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class OutgoingCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (!CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, context) &&
                !prefs.getBoolean(Constants.PREF_OTHER[24], true) && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            Intent i = new Intent(context, CallLoggerService.class);
            i.setAction(intent.getAction());
            i.putExtras(intent.getExtras());
            i.setFlags(intent.getFlags());
            context.startService(i);
        } else if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, context)
                && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
            EventBus.getDefault().post(new NewOutgoingCallEvent(intent.getExtras()));
    }
}
