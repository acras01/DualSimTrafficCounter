package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import ua.od.acros.dualsimtrafficcounter.R;

public class MyNotification extends Notification {

    private static String mTraffic, mCalls;
    private static final String XPOSED = "de.robv.android.xposed.installer";

    public static Notification build(NotificationCompat.Builder builder, Context context, String traffic, String calls) {
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
        return new NotificationCompat.BigTextStyle(builder)
                .bigText(bigText).build();
    }

}
