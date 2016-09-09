package ua.od.acros.dualsimtrafficcounter.receivers;

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
        }
        if (activityIntent != null) {
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
            Intent closePanelIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(closePanelIntent);
        }
    }
}
