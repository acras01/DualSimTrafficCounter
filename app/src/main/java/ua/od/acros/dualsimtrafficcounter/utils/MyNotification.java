package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import ua.od.acros.dualsimtrafficcounter.R;

public class MyNotification extends Notification {

    private static String mTraffic, mCalls;

    public static Notification build(NotificationCompat.Builder builder, Context context, String traffic, String calls) {
        if (traffic.equals(""))
            traffic = mTraffic;
        if (calls.equals(""))
            calls = mCalls;
        String bigText = context.getString(R.string.traffic)  + "\n" + traffic + "\n" +
                context.getString(R.string.calls) + "\n" + calls + "\n";
        mTraffic = traffic;
        mCalls = calls;
        return new NotificationCompat.BigTextStyle(builder)
                .bigText(bigText).build();
    }

}
