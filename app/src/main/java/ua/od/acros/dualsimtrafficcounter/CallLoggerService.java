package ua.od.acros.dualsimtrafficcounter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class CallLoggerService extends Service {

    private static Context mContext;
    private MyDatabase mDatabaseHelper;
    private long[] mCalls;

    public CallLoggerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = CallLoggerService.this;
        mDatabaseHelper = MyDatabase.getInstance(mContext);
        mCalls = MyDatabase.readCallsData(mDatabaseHelper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(mContext, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification n =  new NotificationCompat.Builder(mContext)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_small)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText("Hi!")
                .build();
        startForeground(Constants.STARTED_ID + 1000, n);
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public static Context getAppContext() {
        return CallLoggerService.mContext;
    }
}
