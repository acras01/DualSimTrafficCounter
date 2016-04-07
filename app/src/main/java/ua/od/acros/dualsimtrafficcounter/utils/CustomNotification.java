package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;

public class CustomNotification extends Notification {

    private static String mTraffic = "", mCalls = "";
    private static NotificationCompat.Builder mBuilder;
    private static boolean mIdChanged = false;
    private static boolean mPriorityChanged = false;

    private static NotificationCompat.Builder newInstance(Context context, int priority) {
        if (mBuilder == null) {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setAction("tap");
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            mBuilder = new NotificationCompat.Builder(context)
                    .setPriority(priority)
                    .setContentIntent(contentIntent)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setLargeIcon(bm)
                    .setContentTitle(context.getString(R.string.app_name));
        }
        return mBuilder;
    }

    public static Notification getNotification(Context context, String traffic, String calls) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        int mActiveSIM = TrafficCountService.getActiveSIM();
        if (mActiveSIM == Constants.DISABLED)
            mActiveSIM = TrafficCountService.getLastActiveSIM();
        if (traffic.equals(""))
            traffic = mTraffic;
        if (calls.equals(""))
            calls = mCalls;
        String bigText;
        if (!prefs.getBoolean(Constants.PREF_OTHER[24], false))
            bigText = context.getString(R.string.traffic) + "\n" + traffic + "\n" +
                    context.getString(R.string.calls) + "\n" + calls + "\n";
        else
            bigText = context.getString(R.string.traffic) + "\n" + traffic;
        mTraffic = traffic;
        mCalls = calls;
        int p = prefs.getBoolean(Constants.PREF_OTHER[12], true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN;
        NotificationCompat.Builder b = newInstance(context, p);
        if (mIdChanged) {
            mIdChanged = false;
            b.setSmallIcon(getOperatorLogoID(context, mActiveSIM));
        }
        if (mPriorityChanged) {
            mPriorityChanged = false;
            b.setPriority(prefs.getBoolean(Constants.PREF_OTHER[12], true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN);
        }
        b.setContentText(traffic);
        return new NotificationCompat.BigTextStyle(b).bigText(bigText).build();
    }

    private static int getOperatorLogoID(Context context, int sim) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.PREF_OTHER[15], false) && sim >= 0) {
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
            if (prefs.getString(pref[23], "none").equals("auto"))
                return context.getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(context, sim), "drawable", context.getPackageName());
            else
                return context.getResources().getIdentifier(prefs.getString(pref[23], "logo_none"), "drawable", context.getPackageName());
        } else
            return R.drawable.ic_launcher_small;
    }

    public static void setPriorityNeedsChange(boolean priorityNeedsChange) {
        mPriorityChanged = priorityNeedsChange;
    }

    public static void setIdNeedsChange(boolean idNeedsChange) {
        mIdChanged = idNeedsChange;
    }
}