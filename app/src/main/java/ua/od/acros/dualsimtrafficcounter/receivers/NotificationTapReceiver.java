package ua.od.acros.dualsimtrafficcounter.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class NotificationTapReceiver extends BroadcastReceiver {
    public NotificationTapReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent activityIntent = null;
        switch (intent.getAction()) {
            case Constants.TRAFFIC_TAP:
                activityIntent = new Intent(context, MainActivity.class);
                activityIntent.setAction(Constants.TRAFFIC_TAP);
                break;
            case Constants.CALLS_TAP:
                activityIntent = new Intent(context, MainActivity.class);
                activityIntent.setAction(Constants.CALLS_TAP);
                break;
            case Constants.SETTINGS_TAP:
                activityIntent = new Intent(context, SettingsActivity.class);
                activityIntent.setAction(Constants.SETTINGS_TAP);
                break;
            case Constants.HIDE:
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(Constants.STARTED_ID);
                /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                prefs.edit()
                        .putBoolean(Constants.PREF_OTHER[50], !prefs.getBoolean(Constants.PREF_OTHER[50], true))
                        .apply();
                Intent i;
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class)) {
                    i = new Intent(context, TrafficCountService.class);
                    context.stopService(i);
                    context.startService(i);
                } else if (CustomApplication.isMyServiceRunning(CallLoggerService.class)) {
                    i = new Intent(context, CallLoggerService.class);
                    context.stopService(i);
                    context.startService(i);
                }*/
                break;
        }
        if (activityIntent != null) {
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
            Intent closePanelIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(closePanelIntent);
        }
    }
}
