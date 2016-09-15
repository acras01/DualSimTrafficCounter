package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.receivers.NotificationTapReceiver;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;

public class CustomNotification extends Notification {

    private static String mTraffic = "", mCalls = "";
    private static NotificationCompat.Builder mBuilder;
    private final static int TAP = 19810506;
    private static SharedPreferences mPrefs;
    private static PendingIntent piTraffic, piCalls, piSettings;

    private static NotificationCompat.Builder newInstance(Context context) {
        if (mBuilder == null) {
            //traffic button
            Intent trafficIntent = new Intent(context, NotificationTapReceiver.class);
            trafficIntent.setAction(Constants.TRAFFIC_TAP);
            piTraffic = PendingIntent.getBroadcast(context, TAP, trafficIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            //calls button
            Intent callsIntent = new Intent(context, NotificationTapReceiver.class);
            callsIntent.setAction(Constants.CALLS_TAP);
            piCalls = PendingIntent.getBroadcast(context, TAP, callsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            //settings button
            Intent settingsIntent = new Intent(context, NotificationTapReceiver.class);
            settingsIntent.setAction(Constants.SETTINGS_TAP);
            piSettings = PendingIntent.getBroadcast(context, TAP, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            Intent hideIntent = new Intent(context, NotificationTapReceiver.class);
            hideIntent.setAction(Constants.HIDE);
            PendingIntent piHide = PendingIntent.getBroadcast(context, TAP, hideIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder = new NotificationCompat.Builder(context)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentIntent(piHide)
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setLargeIcon(bm)
                    .setContentTitle(context.getString(R.string.app_name));
        }
        return mBuilder;
    }

    public static Notification getNotification(Context context, String traffic, String calls, boolean id) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        int mActiveSIM = TrafficCountService.getActiveSIM();
        if (mActiveSIM == Constants.DISABLED)
            mActiveSIM = TrafficCountService.getLastActiveSIM();
        if (traffic.equals(""))
            traffic = mTraffic;
        if (calls.equals(""))
            calls = mCalls;
        NotificationCompat.Builder b = newInstance(context);
        if (id)
            b.setSmallIcon(getOperatorLogoID(context, mActiveSIM));
        b.setPriority(mPrefs.getBoolean(Constants.PREF_OTHER[12], true) ? NotificationCompat.PRIORITY_MAX :
                NotificationCompat.PRIORITY_MIN);
        b.setContentText(traffic);
        String bigText;
        if (mPrefs.getBoolean(Constants.PREF_OTHER[25], false))
            bigText = context.getString(R.string.traffic) + "\n" + traffic + "\n" +
                    context.getString(R.string.calls) + "\n" + calls + "\n";
        else
            bigText = context.getString(R.string.traffic) + "\n" + traffic;
        mTraffic = traffic;
        mCalls = calls;

        if (mPrefs.getBoolean(Constants.PREF_OTHER[50], true)) {
            b.mActions.clear();
            b.addAction(R.drawable.ic_action_traffic, context.getString(R.string.action_traffic), piTraffic);
            if (mPrefs.getBoolean(Constants.PREF_OTHER[25], false))
                b.addAction(R.drawable.ic_action_calls, context.getString(R.string.action_calls), piCalls);
            b.addAction(R.drawable.ic_action_settings, context.getString(R.string.action_settings), piSettings);
        } else
            b.mActions.clear();

        return new NotificationCompat.BigTextStyle(b).bigText(bigText).build();
    }

    private static int getOperatorLogoID(Context context, int sim) {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[15], false) && sim >= 0) {
            String[] pref = new String[Constants.PREF_SIM1.length];
            switch (sim) {
                case Constants.SIM1:
                    pref = Constants.PREF_SIM1;
                    break;
                case Constants.SIM2:
                    pref = Constants.PREF_SIM2;
                    break;
                case Constants.SIM3:
                    pref = Constants.PREF_SIM3;
                    break;
            }
            if (mPrefs.getString(pref[23], "none").equals("auto"))
                return context.getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(context, sim), "drawable", context.getPackageName());
            else
                return context.getResources().getIdentifier(mPrefs.getString(pref[23], "logo_none"), "drawable", context.getPackageName());
        } else
            return R.drawable.ic_launcher_small;
    }
}