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
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.joda.time.DateTime;
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
    private DateTimeFormatter fmtDate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
    private DateTimeFormatter fmtTime = DateTimeFormat.forPattern(Constants.TIME_FORMAT + ":ss");
    private BroadcastReceiver callDataReceiver, setUsageReceiver, clearReceiver, callDurationReceiver;
    private String[] mOperatorNames = new String[3];
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private CountDownTimer mCountTimer;
    private Vibrator mVibrator;

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
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mDatabaseHelper = MyDatabase.getInstance(mContext);
        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mCalls = MyDatabase.readCallsData(mDatabaseHelper);
        mOperatorNames[0] = MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        mOperatorNames[1] = MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        mOperatorNames[2] = MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        callDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCountTimer.cancel();
                mVibrator.cancel();
                int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                Toast.makeText(context, mOperatorNames[sim] + ": " +
                        DataFormat.formatCallDuration(context, duration), Toast.LENGTH_LONG).show();
                final int minute = 60 * 1000;
                DateTime now = new DateTime();
                mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
                mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
                switch (sim) {
                    case Constants.SIM1:
                        mCalls.put(Constants.CALLS1_EX, duration + (long) mCalls.get(Constants.CALLS1_EX));
                        if (mPrefs.getString(Constants.PREF_SIM1_CALLS[6], "0").equals("1"))
                            duration = (long) Math.ceil((double) duration / minute) * minute;
                        mCalls.put(Constants.CALLS1, duration + (long) mCalls.get(Constants.CALLS1));
                        break;
                    case Constants.SIM2:
                        mCalls.put(Constants.CALLS2_EX, duration + (long) mCalls.get(Constants.CALLS2_EX));
                        if (mPrefs.getString(Constants.PREF_SIM2_CALLS[6], "0").equals("1"))
                            duration = (long) Math.ceil((double) duration / minute) * minute;
                        mCalls.put(Constants.CALLS2, duration + (long) mCalls.get(Constants.CALLS2));
                        break;
                    case Constants.SIM3:
                        mCalls.put(Constants.CALLS3_EX, duration + (long) mCalls.get(Constants.CALLS3_EX));
                        if (mPrefs.getString(Constants.PREF_SIM3_CALLS[6], "0").equals("1"))
                            duration = (long) Math.ceil((double) duration / minute) * minute;
                        mCalls.put(Constants.CALLS3, duration + (long) mCalls.get(Constants.CALLS3));
                        break;
                }
                MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(Constants.STARTED_ID + 1000, buildNotification());
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
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(Constants.STARTED_ID + 1000, buildNotification());
            }
        };
        IntentFilter setUsageFilter = new IntentFilter(Constants.SET_USAGE);
        registerReceiver(setUsageReceiver, setUsageFilter);

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
        IntentFilter clearSimDataFilter = new IntentFilter(Constants.CLEAR);
        registerReceiver(clearReceiver, clearSimDataFilter);

        callDurationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                long currentDuration = 0;
                int interval = 0;
                long limit = 0;
                switch (intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED)) {
                    case Constants.SIM1:
                        currentDuration = (long) mCalls.get(Constants.CALLS1);
                        interval = Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1_CALLS[3], "0")) *  Constants.SECOND;
                        limit = Long.valueOf(mPrefs.getString(Constants.PREF_SIM1_CALLS[1], "0")) * Constants.MINUTE;
                        break;
                    case Constants.SIM2:
                        currentDuration = (long) mCalls.get(Constants.CALLS2);
                        interval = Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2_CALLS[3], "0")) * Constants.SECOND;
                        limit = Long.valueOf(mPrefs.getString(Constants.PREF_SIM2_CALLS[1], "0")) * Constants.MINUTE;
                        break;
                    case Constants.SIM3:
                        currentDuration = (long) mCalls.get(Constants.CALLS3);
                        interval = Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3_CALLS[3], "0")) *  Constants.SECOND;
                        limit = Long.valueOf(mPrefs.getString(Constants.PREF_SIM3_CALLS[1], "0")) * Constants.MINUTE;
                        break;
                }
                long timeToVibrate;
                if (limit - currentDuration <= interval)
                    timeToVibrate = 0;
                else
                    timeToVibrate = limit - currentDuration - interval;
                mCountTimer = new CountDownTimer(timeToVibrate,  Constants.SECOND) {
                    public void onTick(long millisUntilFinished) {

                    }

                    public void onFinish() {
                        if (mVibrator.hasVibrator())
                            vibrate(mVibrator,  Constants.SECOND,  Constants.SECOND / 2);
                    }
                }.start();
            }
        };
        IntentFilter callDurationFilter = new IntentFilter(Constants.OUTGOING_CALL_COUNT);
        registerReceiver(callDurationReceiver, callDurationFilter);
    }

    private static void vibrate(Vibrator v, int v1, int p1) {
        long[] pattern = new long[] {0, v1, p1};
        v.vibrate(pattern, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Constants.STARTED_ID + 1000, buildNotification());
        return START_STICKY;
    }

    private Notification buildNotification() {
        String text = DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS1));
        if (mSimQuantity >= 2)
            text += "  ||  " + DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS2));
        if (mSimQuantity == 3)
            text += "  ||  " + DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS3));
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setAction("calls");
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(mContext)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_small)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getString(R.string.calls_fragment))
                .setContentText(text)
                .build();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(callDataReceiver);
        unregisterReceiver(clearReceiver);
        unregisterReceiver(setUsageReceiver);
        unregisterReceiver(callDurationReceiver);
    }

    public static Context getCallLoggerContext() {
        return CallLoggerService.mContext;
    }
}