package ua.od.acros.dualsimtrafficcounter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class CallLoggerService extends Service {

    private static Context mContext;
    private MyDatabase mDatabaseHelper;
    private ContentValues mCalls;
    private DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT + ":ss");
    private BroadcastReceiver callDataReceiver;

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

        callDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "SIM" + intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED) + ": " +
                        String.format("%.2f", (double) intent.getLongExtra(Constants.CALL_DURATION, 0L)/1000) + "s", Toast.LENGTH_LONG).show();
            }
        };
        IntentFilter callDataFilter = new IntentFilter(Constants.CALLS);
        registerReceiver(callDataReceiver, callDataFilter);
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
        unregisterReceiver(callDataReceiver);
    }

    public static Context getAppContext() {
        return CallLoggerService.mContext;
    }
}