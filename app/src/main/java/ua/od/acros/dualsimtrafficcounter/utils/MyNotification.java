package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;

public class MyNotification extends Notification {

    private static String mTraffic, mCalls;
    private static NotificationCompat.Builder builder;
    private static final String XPOSED = "de.robv.android.xposed.installer";

    private static NotificationCompat.Builder newInstance(Context context) {
        if (builder == null) {
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder = new NotificationCompat.Builder(context)
                    .setContentIntent(contentIntent)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher_small)
                    .setOngoing(true)
                    .setContentTitle(context.getString(R.string.app_name));
        }
        return builder;
    }

    public static Notification getNotification(Context context, String traffic, String calls, Bitmap bm, int id, int priority) {
        if (traffic.equals(""))
            traffic = mTraffic;
        if (calls.equals(""))
            calls = mCalls;
        String bigText;
        if (XposedUtils.isPackageExisted(context, XPOSED))
            bigText = context.getString(R.string.traffic)  + "\n" + traffic + "\n" +
                    context.getString(R.string.calls) + "\n" + calls + "\n";
        else
            bigText = context.getString(R.string.traffic)  + "\n" + traffic;
        mTraffic = traffic;
        mCalls = calls;
        NotificationCompat.Builder b = newInstance(context);
        b.setLargeIcon(bm);
        if (id != 0)
            b.setSmallIcon(id);
        if (priority > NotificationCompat.PRIORITY_MIN)
            b.setPriority(priority);
        return new NotificationCompat.BigTextStyle(b).bigText(bigText).build();
    }

}
