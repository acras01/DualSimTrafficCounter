package ua.od.acros.dualsimtrafficcounter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ua.od.acros.dualsimtrafficcounter.fragments.CallsFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TrafficFragment;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
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
                int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                Toast.makeText(context, "SIM" + sim + ": " +
                        DataFormat.formatCallDuration(context, duration), Toast.LENGTH_LONG).show();
                Intent callsIntent = new Intent(Constants.CALLS);
                callsIntent.putExtra(Constants.SIM_ACTIVE, sim);
                callsIntent.putExtra(Constants.CALL_DURATION, duration);
                sendBroadcast(callsIntent);
            }
        };
        IntentFilter callDataFilter = new IntentFilter(Constants.OUTGOING_CALL);
        registerReceiver(callDataReceiver, callDataFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setAction("calls");
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);   // To open only one activity on launch.
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n =  new NotificationCompat.Builder(mContext)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN)
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

    public static Context getCallLoggerServiceContext() {
        return CallLoggerService.mContext;
    }
}