package ua.od.acros.dualsimtrafficcounter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class CallLoggerService extends Service {

    private static Context mContext;
    private MyDatabase mDatabaseHelper;
    private ContentValues mCalls;
    private DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT + ":ss");
    private BroadcastReceiver callDataReceiver, setUsageReceiver, clearReceiver;
    private String[] mOperatorNames = new String[3];

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
        mOperatorNames[0] = MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        mOperatorNames[1] = MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        mOperatorNames[2] = MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);

        callDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                Toast.makeText(context, mOperatorNames[sim] + ": " +
                        DataFormat.formatCallDuration(context, duration), Toast.LENGTH_LONG).show();
                Intent callsIntent = new Intent(Constants.CALLS);
                callsIntent.putExtra(Constants.SIM_ACTIVE, sim);
                callsIntent.putExtra(Constants.CALL_DURATION, duration);
                sendBroadcast(callsIntent);
            }
        };
        IntentFilter callDataFilter = new IntentFilter(Constants.OUTGOING_CALL);
        registerReceiver(callDataReceiver, callDataFilter);

        setUsageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mCalls == null)
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                Bundle limitBundle = intent.getBundleExtra("data");
                switch (limitBundle.getInt("sim")) {
                    case Constants.SIM1:
                        long mTotal1 = DataFormat.getFormatLong(limitBundle.getString("tot1"), limitBundle.getInt("tot1"));
                        mCalls.put(Constants.CALLS1, mTotal1);
                        mCalls.put(Constants.CALLS1_EX, mTotal1);
                        MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                        break;
                    case Constants.SIM2:
                        long mTotal2 = DataFormat.getFormatLong(limitBundle.getString("tot2"), limitBundle.getInt("tot2"));
                        mCalls.put(Constants.CALLS2, mTotal2);
                        mCalls.put(Constants.CALLS2_EX, mTotal2);
                        MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                        break;
                    case Constants.SIM3:
                        long mTotal3 = DataFormat.getFormatLong(limitBundle.getString("tot3"), limitBundle.getInt("tot3"));
                        mCalls.put(Constants.CALLS3, mTotal3);
                        mCalls.put(Constants.CALLS3_EX, mTotal3);
                        MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                        break;
                }
                Intent notificationIntent = new Intent(context, MainActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setContentIntent(contentIntent)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setContentText(DataFormat.formatData(context, (long) mCalls.get(Constants.CALLS1)) + "   ||   "
                                + DataFormat.formatData(context, (long) mCalls.get(Constants.CALLS2)) + "   ||   "
                                + DataFormat.formatData(context, (long) mCalls.get(Constants.CALLS3)));
                nm.notify(Constants.STARTED_ID + 1000, builder.build());
            }
        };

        clearReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED)) {
                    case Constants.SIM1:
                        mCalls.put(Constants.CALLS1, 0L);
                        mCalls.put(Constants.CALLS1_EX, 0L);
                        break;
                    case Constants.SIM2:
                        mCalls.put(Constants.CALLS2, 0L);
                        mCalls.put(Constants.CALLS2_EX, 0L);
                        break;
                    case Constants.SIM3:
                        mCalls.put(Constants.CALLS3, 0L);
                        mCalls.put(Constants.CALLS3_EX, 0L);
                        break;
                }
                MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
            }
        };

        IntentFilter setUsageFilter = new IntentFilter(Constants.SET_USAGE);
        IntentFilter clearSimDataFilter = new IntentFilter(Constants.CLEAR);
        registerReceiver(setUsageReceiver, setUsageFilter);
        registerReceiver(clearReceiver, clearSimDataFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(mContext, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);
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
        unregisterReceiver(clearReceiver);
        unregisterReceiver(setUsageReceiver);
    }

    public static Context getCallLoggerContext() {
        return CallLoggerService.mContext;
    }
}