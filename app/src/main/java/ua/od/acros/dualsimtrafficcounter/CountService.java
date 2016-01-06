package ua.od.acros.dualsimtrafficcounter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.support.v4.app.NotificationCompat;

import org.acra.ACRA;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.dialogs.ChooseAction;
import ua.od.acros.dualsimtrafficcounter.settings.LimitFragment;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.DateCompare;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;
import ua.od.acros.dualsimtrafficcounter.widget.InfoWidget;


public class CountService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private long mLastUpdateTime;
    private long mStartRX1 = 0;
    private long mStartTX1 = 0;
    private long mStartRX2 = 0;
    private long mStartTX2 = 0;
    private long mStartRX3 = 0;
    private long mStartTX3 = 0;
    private long mReceived1 = 0;
    private long mTransmitted1 = 0;
    private long mReceived2 = 0;
    private long mTransmitted2 = 0;
    private long mReceived3 = 0;
    private long mTransmitted3 = 0;
    private boolean isSIM1OverLimit = false;
    private boolean isSIM2OverLimit = false;
    private boolean isSIM3OverLimit = false;
    private boolean isTimerCancelled = false;
    private boolean continueOverLimit = false;
    private boolean needsReset3 = false;
    private boolean needsReset2 = false;
    private boolean needsReset1 = false;
    private static boolean isNight1 = false;
    private static boolean isNight2 = false;
    private static boolean isNight3 = false;
    private int simChosen = Constants.DISABLED;
    private int simNumber = 0;
    private int mPriority;
    private static int activeSIM = Constants.DISABLED;
    private static int lastActiveSIM = Constants.DISABLED;

    private final long MB = 1024 * 1024;

    private DateTime resetTime1;
    private DateTime resetTime2;
    private DateTime resetTime3;
    private ContentValues dataMap;
    private BroadcastReceiver clear1Receiver, clear2Receiver, clear3Receiver, connReceiver, setUsage, actionReceive;
    private static Context context;
    private TrafficDatabase mDatabaseHelper;
    private Timer mTimer = null;
    private SharedPreferences prefs;
    private PendingIntent contentIntent;
    private NotificationManager nm;
    private NotificationCompat.Builder builder;
    private Notification n;


    public CountService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static int getLastActiveSIM() {
        return lastActiveSIM;
    }

    public static int getActiveSIM() {
        return activeSIM;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mPriority = prefs.getBoolean(Constants.PREF_OTHER[12], true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN;

        context = CountService.this;

        mDatabaseHelper = new TrafficDatabase(this, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
        dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
        if (dataMap.get(Constants.LAST_DATE).equals("")) {
            Calendar myCalendar = Calendar.getInstance();
            SimpleDateFormat formatDate = new SimpleDateFormat(Constants.DATE_FORMAT, context.getResources().getConfiguration().locale);
            SimpleDateFormat formatTime = new SimpleDateFormat(Constants.TIME_FORMAT + ":ss", context.getResources().getConfiguration().locale);
            dataMap.put(Constants.LAST_TIME, formatTime.format(myCalendar.getTime()));
            dataMap.put(Constants.LAST_DATE, formatDate.format(myCalendar.getTime()));
        }

        connReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    lastActiveSIM = activeSIM;
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;

                    if (prefs.getBoolean(Constants.PREF_SIM1[14], true) && lastActiveSIM == Constants.SIM1) {
                        dataMap.put(Constants.TOTAL1, DataFormat.getRoundLong((long) dataMap.get(Constants.TOTAL1),
                                prefs.getString(Constants.PREF_SIM1[15], "1"), prefs.getString(Constants.PREF_SIM1[16], "0")));
                        TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                    }

                    if (prefs.getBoolean(Constants.PREF_SIM2[14], true) && lastActiveSIM == Constants.SIM2) {
                        dataMap.put(Constants.TOTAL2, DataFormat.getRoundLong((long) dataMap.get(Constants.TOTAL2),
                                prefs.getString(Constants.PREF_SIM2[15], "1"), prefs.getString(Constants.PREF_SIM2[16], "0")));
                        TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                    }

                    if (prefs.getBoolean(Constants.PREF_SIM3[14], true) && lastActiveSIM == Constants.SIM3) {
                        dataMap.put(Constants.TOTAL3, DataFormat.getRoundLong((long) dataMap.get(Constants.TOTAL3),
                                prefs.getString(Constants.PREF_SIM3[15], "1"), prefs.getString(Constants.PREF_SIM3[16], "0")));
                        TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                    }

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        ACRA.getErrorReporter().handleException(e);
                    }                    
                } else
                    if (MobileDataControl.getMobileDataInfo(context)[0] == 2)
                        timerStart(Constants.COUNT);
            }
        };
        IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connReceiver, connFilter);

        actionReceive = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int simid = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                try {
                    switch (intent.getStringExtra(Constants.ACTION)) {
                        case Constants.CHOOSE_ACTION:
                            if (!isSIM2OverLimit && simid == Constants.SIM1) {
                                MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM2);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM3OverLimit && simid == Constants.SIM1) {
                                MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM3);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM1OverLimit && simid == Constants.SIM2) {
                                MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM1);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM3OverLimit && simid == Constants.SIM2) {
                                MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM3);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM1OverLimit && simid == Constants.SIM3) {
                                MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM1);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM2OverLimit && simid == Constants.SIM3) {
                                MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM2);
                                timerStart(Constants.COUNT);
                            } else
                                timerStart(Constants.CHECK);
                            break;
                        case Constants.LIMIT_ACTION:
                            Intent i = new Intent(context, SettingsActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, LimitFragment.class.getName());
                            i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                            startActivity(i);
                            timerStart(Constants.CHECK);
                            break;
                        case Constants.CONTINUE_ACTION:
                            MobileDataControl.toggleMobileDataConnection(true, context, simid);
                            continueOverLimit = true;
                            timerStart(Constants.COUNT);
                            break;
                        case Constants.OFF_ACTION:
                            timerStart(Constants.CHECK);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
            }
        };
        IntentFilter actionFilter = new IntentFilter(Constants.ACTION);
        registerReceiver(actionReceive, actionFilter);


        setUsage = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTimer.cancel();
                mTimer.purge();
                isTimerCancelled = true;
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                if (dataMap == null)
                    dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
                Bundle limitBundle = intent.getBundleExtra("data");
                simChosen = limitBundle.getInt("sim");
                switch (simChosen) {
                    case  Constants.SIM1:
                        mReceived1 = DataFormat.getFormatLong(limitBundle.getString("rcvd"), limitBundle.getInt("rxV"));
                        mTransmitted1 = DataFormat.getFormatLong(limitBundle.getString("trans"), limitBundle.getInt("txV"));
                        if (isNight1) {
                            dataMap.put(Constants.SIM1RX_N, mReceived1);
                            dataMap.put(Constants.SIM1TX_N, mTransmitted1);
                            dataMap.put(Constants.TOTAL1_N, mReceived1 + mTransmitted1);
                        } else {
                            dataMap.put(Constants.SIM1RX, mReceived1);
                            dataMap.put(Constants.SIM1TX, mTransmitted1);
                            dataMap.put(Constants.TOTAL1, mReceived1 + mTransmitted1);
                        }
                        TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                        break;
                    case  Constants.SIM2:
                        mReceived2 = DataFormat.getFormatLong(limitBundle.getString("rcvd"), limitBundle.getInt("rxV"));
                        mTransmitted2 = DataFormat.getFormatLong(limitBundle.getString("trans"), limitBundle.getInt("txV"));
                        if (isNight2) {
                            dataMap.put(Constants.SIM2RX_N, mReceived2);
                            dataMap.put(Constants.SIM2TX_N, mTransmitted2);
                            dataMap.put(Constants.TOTAL2_N, mReceived2 + mTransmitted2);
                        } else {
                            dataMap.put(Constants.SIM2RX, mReceived2);
                            dataMap.put(Constants.SIM2TX, mTransmitted2);
                            dataMap.put(Constants.TOTAL2, mReceived2 + mTransmitted2);
                        }
                        TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                        break;
                    case  Constants.SIM3:
                        mReceived3 = DataFormat.getFormatLong(limitBundle.getString("rcvd"), limitBundle.getInt("rxV"));
                        mTransmitted3 = DataFormat.getFormatLong(limitBundle.getString("trans"), limitBundle.getInt("txV"));
                        if (isNight3) {
                            dataMap.put(Constants.SIM3RX_N, mReceived3);
                            dataMap.put(Constants.SIM3TX_N, mTransmitted3);
                            dataMap.put(Constants.TOTAL3_N, mReceived3 + mTransmitted3);
                        } else {
                            dataMap.put(Constants.SIM3RX, mReceived3);
                            dataMap.put(Constants.SIM3TX, mTransmitted3);
                            dataMap.put(Constants.TOTAL3, mReceived3 + mTransmitted3);
                        }
                        TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                        break;
                    }
                n = builder.setContentIntent(contentIntent)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setPriority(mPriority)
                        .setContentText(DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                                + DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3)))
                                .build();
                nm.notify(Constants.STARTED_ID, n);
                timerStart(Constants.COUNT);
            }
        };

        clear1Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTimer.cancel();
                mTimer.purge();
                isTimerCancelled = true;
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                if (isNight1) {
                    dataMap.put(Constants.SIM1RX_N, 0L);
                    dataMap.put(Constants.SIM1TX_N, 0L);
                    dataMap.put(Constants.TOTAL1_N, 0L);
                } else {
                    dataMap.put(Constants.SIM1RX, 0L);
                    dataMap.put(Constants.SIM1TX, 0L);
                    dataMap.put(Constants.TOTAL1, 0L);
                }
                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                timerStart(Constants.COUNT);
            }
        };

        clear2Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTimer.cancel();
                mTimer.purge();
                isTimerCancelled = true;
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                if (isNight2) {
                    dataMap.put(Constants.SIM2RX_N, 0L);
                    dataMap.put(Constants.SIM2TX_N, 0L);
                    dataMap.put(Constants.TOTAL2_N, 0L);
                } else {
                    dataMap.put(Constants.SIM2RX, 0L);
                    dataMap.put(Constants.SIM2TX, 0L);
                    dataMap.put(Constants.TOTAL2, 0L);
                }
                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                timerStart(Constants.COUNT);
            }
        };

        clear3Receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTimer.cancel();
                mTimer.purge();
                isTimerCancelled = true;
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                if (isNight3) {
                    dataMap.put(Constants.SIM3RX_N, 0L);
                    dataMap.put(Constants.SIM3TX_N, 0L);
                    dataMap.put(Constants.TOTAL3_N, 0L);
                } else {
                    dataMap.put(Constants.SIM3RX, 0L);
                    dataMap.put(Constants.SIM3TX, 0L);
                    dataMap.put(Constants.TOTAL3, 0L);
                }
                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                timerStart(Constants.COUNT);
            }
        };

        IntentFilter setUsageFilter = new IntentFilter(Constants.SET_USAGE);
        IntentFilter clear1ServiceFilter = new IntentFilter(Constants.CLEAR1);
        IntentFilter clear2ServiceFilter = new IntentFilter(Constants.CLEAR2);
        IntentFilter clear3ServiceFilter = new IntentFilter(Constants.CLEAR3);
        registerReceiver(setUsage, setUsageFilter);
        registerReceiver(clear1Receiver, clear1ServiceFilter);
        registerReceiver(clear2Receiver, clear2ServiceFilter);
        registerReceiver(clear3Receiver, clear3ServiceFilter);

        activeSIM = Constants.DISABLED;
        lastActiveSIM = (int) dataMap.get(Constants.LAST_ACTIVE_SIM);

        // cancel if already existed
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = new Timer();
        } else {
            // recreate new
            mTimer = new Timer();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(mPriority)
                .setLargeIcon(bm)
                .setTicker(getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentTitle(context.getResources().getString(R.string.notification_title))
                .setContentText(DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                        + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                        + DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3)))
                .build();
        startForeground(Constants.STARTED_ID, n);
        // schedule task
        timerStart(Constants.COUNT);

        return START_STICKY;
    }

    protected static Context getAppContext() {
        return CountService.context;
    }

    public static boolean[] getIsNight() {
        return new boolean[]{isNight1, isNight2, isNight3};
    }

    private void timerStart(int task) {
        TimerTask tTask = null;
        simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileDataControl.isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        activeSIM = MobileDataControl.getMobileDataInfo(context)[1];
        sendDataBroadcast(0L, 0L);
        if (task == Constants.COUNT) {
            switch (activeSIM) {
                case Constants.SIM1:
                    mStartRX1 = TrafficStats.getMobileRxBytes();
                    mStartTX1 = TrafficStats.getMobileTxBytes();
                    tTask = new CountTimerTask1();
                    break;
                case Constants.SIM2:
                    mStartRX2 = TrafficStats.getMobileRxBytes();
                    mStartTX2 = TrafficStats.getMobileTxBytes();
                    tTask = new CountTimerTask2();
                    break;
                case Constants.SIM3:
                    mStartRX3 = TrafficStats.getMobileRxBytes();
                    mStartTX3 = TrafficStats.getMobileTxBytes();
                    tTask = new CountTimerTask3();
                    break;
            }
        } else
            tTask = new CheckTimerTask();
        if (isTimerCancelled)
            mTimer = new Timer();
        isTimerCancelled = false;
        if (tTask != null) {
            mTimer.scheduleAtFixedRate(tTask, 0, Constants.NOTIFY_INTERVAL);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[12])) {
            if (sharedPreferences.getBoolean(key, true))
                mPriority = sharedPreferences.getBoolean(key, true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN;
            nm.cancel(Constants.STARTED_ID);
        }
        if ((key.equals(Constants.PREF_SIM1[1]) || key.equals(Constants.PREF_SIM1[2])) && activeSIM == Constants.SIM1 && continueOverLimit)
            continueOverLimit = false;
        if ((key.equals(Constants.PREF_SIM2[1]) || key.equals(Constants.PREF_SIM2[2])) && activeSIM == Constants.SIM2 && continueOverLimit)
            continueOverLimit = false;
        if ((key.equals(Constants.PREF_SIM3[1]) || key.equals(Constants.PREF_SIM3[2])) && activeSIM == Constants.SIM3 && continueOverLimit)
            continueOverLimit = false;
    }

    private class CheckTimerTask extends TimerTask {

        @Override
        public void run() {

            context.sendBroadcast(new Intent(Constants.TIP));

            DateTimeFormatter fmt = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
            DateTime dt = fmt.parseDateTime((String) dataMap.get(Constants.LAST_DATE));

            String limit1 = isNight1 ? prefs.getString(Constants.PREF_SIM1[18], "") : prefs.getString(Constants.PREF_SIM1[1], "");
            String limit2 = isNight2 ? prefs.getString(Constants.PREF_SIM2[18], "") : prefs.getString(Constants.PREF_SIM2[1], "");
            String limit3 = isNight3 ? prefs.getString(Constants.PREF_SIM3[18], "") : prefs.getString(Constants.PREF_SIM3[1], "");
            String round1 = isNight1 ? prefs.getString(Constants.PREF_SIM1[22], "") : prefs.getString(Constants.PREF_SIM1[4], "0");
            String round2 = isNight2 ? prefs.getString(Constants.PREF_SIM2[22], "") : prefs.getString(Constants.PREF_SIM2[4], "0");
            String round3 = isNight3 ? prefs.getString(Constants.PREF_SIM3[22], "") : prefs.getString(Constants.PREF_SIM3[4], "0");
            int value1;
            if (prefs.getString(Constants.PREF_SIM1[2], "").equals(""))
                value1 = 0;
            else
                value1 = isNight1 ? Integer.valueOf(prefs.getString(Constants.PREF_SIM1[19], "")) :
                        Integer.valueOf(prefs.getString(Constants.PREF_SIM1[2], ""));
            int value2;
            if (prefs.getString(Constants.PREF_SIM2[2], "").equals(""))
                value2 = 0;
            else
                value2 = isNight2 ? Integer.valueOf(prefs.getString(Constants.PREF_SIM2[19], "")) :
                        Integer.valueOf(prefs.getString(Constants.PREF_SIM2[2], ""));
            int value3;
            if (prefs.getString(Constants.PREF_SIM3[2], "").equals(""))
                value3 = 0;
            else
                value3 = isNight3 ? Integer.valueOf(prefs.getString(Constants.PREF_SIM3[19], "")) :
                        Integer.valueOf(prefs.getString(Constants.PREF_SIM3[2], ""));
            float valuer1;
            float valuer2;
            float valuer3;
            double lim1 = Double.MAX_VALUE;
            double lim2 = Double.MAX_VALUE;
            double lim3 = Double.MAX_VALUE;
            if (!limit1.equals("")) {
                valuer1 = 1 - Float.valueOf(round1) / 100;
                lim1 = valuer1 * DataFormat.getFormatLong(limit1, value1);
            }
            if (!limit2.equals("")) {
                valuer2 = 1 - Float.valueOf(round2) / 100;
                lim2 = valuer2 * DataFormat.getFormatLong(limit2, value2);
            }
            if (!limit3.equals("")) {
                valuer3 = 1 - Float.valueOf(round3) / 100;
                lim3 = valuer3 * DataFormat.getFormatLong(limit3, value3);
            }
            long tot1 = isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1);
            long tot2 = isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2);
            long tot3 = isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3);
            try {
                if (isSIM1OverLimit && (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM1[3], ""))
                        || (tot1 <= (long) lim1 && (prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        || (!prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !prefs.getBoolean(Constants.PREF_SIM2[8], false) && !prefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM1);
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;
                    timerStart(Constants.COUNT);
                }
                if (isSIM2OverLimit && (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM2[3], ""))
                        || (tot2 <= (long) lim2 && (prefs.getBoolean(Constants.PREF_SIM2[8], false)
                        || (!prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !prefs.getBoolean(Constants.PREF_SIM2[8], false) && !prefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM2);
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;
                    timerStart(Constants.COUNT);
                }
                if (isSIM3OverLimit && (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM3[3], ""))
                        || (tot3 <= (long) lim3 && (prefs.getBoolean(Constants.PREF_SIM3[8], false)
                        || (!prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !prefs.getBoolean(Constants.PREF_SIM2[8], false) && !prefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM3);
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;
                    timerStart(Constants.COUNT);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private void isResetNeeded() {
        DateTimeFormatter fmtdate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
        DateTimeFormatter fmtnow = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
        DateTime dt = fmtdate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
        DateTime now = new DateTime();

        String reset1 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM1[9], "00:00");
        String reset2 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM2[9], "00:00");
        String reset3 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM3[9], "00:00");

        if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM1[3], "")) || needsReset1 ||
                (prefs.getString(Constants.PREF_SIM1[3], "0").equals("2") && DateCompare.isNextDayOrMonth(dt, "0"))) {
            needsReset1 = true;
            switch (prefs.getString(Constants.PREF_SIM1[3], "")) {
                case "0":
                    resetTime1 = fmtnow.parseDateTime(reset1);
                    break;
                case "1":
                    int day1 = Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1"));
                    if (day1 >= 28)
                        switch (now.getMonthOfYear()) {
                            case 2:
                                if (now.year().isLeap())
                                    day1 = 29;
                                else
                                    day1 = 28;
                                break;
                            case 4:
                            case 6:
                            case 9:
                            case 11:
                                if (day1 == 31)
                                    day1 = 30;
                                break;
                        }
                    if (day1 >= now.getDayOfMonth())
                        resetTime1 = fmtnow.parseDateTime(reset1);
                    break;
                case "2":
                    int day2 = Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1"));
                    if ((int) dataMap.get(Constants.PERIOD1) >= day2 + 1) {
                        resetTime1 = fmtnow.parseDateTime(reset1);
                        dataMap.put(Constants.PERIOD1, 0);
                    }
                    else
                        dataMap.put(Constants.PERIOD1, (int) dataMap.get(Constants.PERIOD1) + 1);
            }
        }
        if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM2[3], "")) || needsReset2 ||
                (prefs.getString(Constants.PREF_SIM2[3], "0").equals("2") && DateCompare.isNextDayOrMonth(dt, "0"))) {
            needsReset2 = true;
            switch (prefs.getString(Constants.PREF_SIM2[3], "")) {
                case "0":
                    resetTime2 = fmtnow.parseDateTime(reset2);
                    break;
                case "1":
                    int day1 = Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1"));
                    if (day1 >= 28)
                        switch (now.getMonthOfYear()) {
                            case 2:
                                if (now.year().isLeap())
                                    day1 = 29;
                                else
                                    day1 = 28;
                                break;
                            case 4:
                            case 6:
                            case 9:
                            case 11:
                                if (day1 == 31)
                                    day1 = 30;
                                break;
                        }
                    if (day1 >= now.getDayOfMonth())
                        resetTime2 = fmtnow.parseDateTime(reset2);
                    break;
                case "2":
                    int day2 = Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1"));
                    if ((int) dataMap.get(Constants.PERIOD2) >= day2 + 1) {
                        resetTime2 = fmtnow.parseDateTime(reset2);
                        dataMap.put(Constants.PERIOD2, 0);
                    }
                    else
                        dataMap.put(Constants.PERIOD2, (int) dataMap.get(Constants.PERIOD2) + 1);
            }
        }
        if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM3[3], "")) || needsReset3 ||
                (prefs.getString(Constants.PREF_SIM3[3], "0").equals("2") && DateCompare.isNextDayOrMonth(dt, "0"))) {
            needsReset3 = true;
            switch (prefs.getString(Constants.PREF_SIM3[3], "")) {
                case "0":
                    resetTime3 = fmtnow.parseDateTime(reset3);
                    break;
                case "1":
                    int day1 = Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1"));
                    if (day1 >= 28)
                        switch (now.getMonthOfYear()) {
                            case 2:
                                if (now.year().isLeap())
                                    day1 = 29;
                                else
                                    day1 = 28;
                                break;
                            case 4:
                            case 6:
                            case 9:
                            case 11:
                                if (day1 == 31)
                                    day1 = 30;
                                break;
                        }
                    if (day1 >= now.getDayOfMonth())
                        resetTime3 = fmtnow.parseDateTime(reset3);
                    break;
                case "2":
                    int day2 = Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1"));
                    if ((int) dataMap.get(Constants.PERIOD3) >= day2 + 1) {
                        resetTime3 = fmtnow.parseDateTime(reset3);
                        dataMap.put(Constants.PERIOD3, 0);
                    }
                    else
                        dataMap.put(Constants.PERIOD3, (int) dataMap.get(Constants.PERIOD3) + 1);
            }
        }
    }

    private class CountTimerTask1 extends TimerTask {

        @Override
        public void run() {
            try {
                int[] data = {2, Constants.SIM1};

                if (Arrays.equals(MobileDataControl.getMobileDataInfo(context), data) && !isTimerCancelled) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing dataMap
                    //begin
                    dataMap.put(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                    dataMap.put(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                    dataMap.put(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                    dataMap.put(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                    dataMap.put(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                    dataMap.put(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                    dataMap.put(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                    dataMap.put(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                    dataMap.put(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                    dataMap.put(Constants.SIM1RX_N, (long) dataMap.get(Constants.SIM1RX_N));
                    dataMap.put(Constants.SIM2RX_N, (long) dataMap.get(Constants.SIM2RX_N));
                    dataMap.put(Constants.SIM3RX_N, (long) dataMap.get(Constants.SIM3RX_N));
                    dataMap.put(Constants.SIM1TX_N, (long) dataMap.get(Constants.SIM1TX_N));
                    dataMap.put(Constants.SIM2TX_N, (long) dataMap.get(Constants.SIM2TX_N));
                    dataMap.put(Constants.SIM3TX_N, (long) dataMap.get(Constants.SIM3TX_N));
                    dataMap.put(Constants.TOTAL1_N, (long) dataMap.get(Constants.TOTAL1_N));
                    dataMap.put(Constants.TOTAL2_N, (long) dataMap.get(Constants.TOTAL2_N));
                    dataMap.put(Constants.TOTAL3_N, (long) dataMap.get(Constants.TOTAL3_N));
                    dataMap.put(Constants.LAST_TIME, (String) dataMap.get(Constants.LAST_TIME));
                    dataMap.put(Constants.LAST_DATE, (String) dataMap.get(Constants.LAST_DATE));
                    dataMap.put(Constants.LAST_ACTIVE_SIM, (int) dataMap.get(Constants.LAST_ACTIVE_SIM));
                    //end

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    boolean emptyDB = TrafficDatabase.isEmpty(mDatabaseHelper);

                    DateTimeFormatter fmtdate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
                    DateTimeFormatter fmtnow = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
                    DateTime dt = fmtdate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                    DateTime now = new DateTime();

                    if (prefs.getBoolean(Constants.PREF_SIM1[17], false)) {
                        String timeON = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM1[20], "23:00");
                        String timeOFF = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM1[21], "06:00");
                        isNight1 = DateTimeComparator.getInstance().compare(now, fmtnow.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, fmtnow.parseDateTime(timeOFF)) <= 0;
                    } else
                        isNight1 = false;

                    isResetNeeded();

                    if (emptyDB) {
                        dataMap.put(Constants.SIM1RX, 0L);
                        dataMap.put(Constants.SIM2RX, 0L);
                        dataMap.put(Constants.SIM3RX, 0L);
                        dataMap.put(Constants.SIM1TX, 0L);
                        dataMap.put(Constants.SIM2TX, 0L);
                        dataMap.put(Constants.SIM3TX, 0L);
                        dataMap.put(Constants.TOTAL1, 0L);
                        dataMap.put(Constants.TOTAL2, 0L);
                        dataMap.put(Constants.TOTAL3, 0L);
                        dataMap.put(Constants.SIM1RX_N, 0L);
                        dataMap.put(Constants.SIM2RX_N, 0L);
                        dataMap.put(Constants.SIM3RX_N, 0L);
                        dataMap.put(Constants.SIM1TX_N, 0L);
                        dataMap.put(Constants.SIM2TX_N, 0L);
                        dataMap.put(Constants.SIM3TX_N, 0L);
                        dataMap.put(Constants.TOTAL1_N, 0L);
                        dataMap.put(Constants.TOTAL2_N, 0L);
                        dataMap.put(Constants.TOTAL3_N, 0L);
                        dataMap.put(Constants.LAST_RX, 0L);
                        dataMap.put(Constants.LAST_TX, 0L);
                        dataMap.put(Constants.LAST_TIME, "");
                        dataMap.put(Constants.LAST_DATE, "");
                        dataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                    } else if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                            || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                            || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                        if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                            dataMap.put(Constants.SIM1RX, 0L);
                            dataMap.put(Constants.SIM1TX, 0L);
                            dataMap.put(Constants.TOTAL1, 0L);
                            dataMap.put(Constants.SIM1RX_N, 0L);
                            dataMap.put(Constants.SIM1TX_N, 0L);
                            dataMap.put(Constants.TOTAL1_N, 0L);
                            rx = tx = mReceived1 = mTransmitted1 = 0;
                            needsReset1 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                            dataMap.put(Constants.SIM2RX, 0L);
                            dataMap.put(Constants.SIM2TX, 0L);
                            dataMap.put(Constants.TOTAL2, 0L);
                            dataMap.put(Constants.SIM2RX_N, 0L);
                            dataMap.put(Constants.SIM2TX_N, 0L);
                            dataMap.put(Constants.TOTAL2_N, 0L);
                            if (!isNight1) {
                                rx = (long) dataMap.get(Constants.SIM1RX);
                                tx = (long) dataMap.get(Constants.SIM1TX);
                                tot = (long) dataMap.get(Constants.TOTAL1);
                            } else {
                                rx = (long) dataMap.get(Constants.SIM1RX_N);
                                tx = (long) dataMap.get(Constants.SIM1TX_N);
                                tot = (long) dataMap.get(Constants.TOTAL1_N);
                            }
                            mReceived2 = mTransmitted2 = 0;
                            needsReset2 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                            dataMap.put(Constants.SIM3RX, 0L);
                            dataMap.put(Constants.SIM3TX, 0L);
                            dataMap.put(Constants.TOTAL3, 0L);
                            dataMap.put(Constants.SIM3RX_N, 0L);
                            dataMap.put(Constants.SIM3TX_N, 0L);
                            dataMap.put(Constants.TOTAL3_N, 0L);
                            if (!isNight1) {
                                rx = (long) dataMap.get(Constants.SIM1RX);
                                tx = (long) dataMap.get(Constants.SIM1TX);
                                tot = (long) dataMap.get(Constants.TOTAL1);
                            } else {
                                rx = (long) dataMap.get(Constants.SIM1RX_N);
                                tx = (long) dataMap.get(Constants.SIM1TX_N);
                                tot = (long) dataMap.get(Constants.TOTAL1_N);
                            }
                            mReceived3 = mTransmitted3 = 0;
                            needsReset3 = false;
                        }
                    } else {
                        if (!isNight1) {
                            rx = (long) dataMap.get(Constants.SIM1RX);
                            tx = (long) dataMap.get(Constants.SIM1TX);
                            tot = (long) dataMap.get(Constants.TOTAL1);
                        } else {
                            rx = (long) dataMap.get(Constants.SIM1RX_N);
                            tx = (long) dataMap.get(Constants.SIM1TX_N);
                            tot = (long) dataMap.get(Constants.TOTAL1_N);
                        }
                    }


                    String limit, round;
                    int value;
                    float valuer;
                    double lim = Double.MAX_VALUE;

                    if (isNight1) {
                        limit = prefs.getString(Constants.PREF_SIM1[18], "");
                        round = prefs.getString(Constants.PREF_SIM1[22], "0");
                        if (prefs.getString(Constants.PREF_SIM1[19], "").equals(""))
                            value = 0;
                        else
                            value = Integer.valueOf(prefs.getString(Constants.PREF_SIM1[19], ""));
                    } else {
                        limit = prefs.getString(Constants.PREF_SIM1[1], "");
                        round = prefs.getString(Constants.PREF_SIM1[4], "0");
                        if (prefs.getString(Constants.PREF_SIM1[2], "").equals(""))
                            value = 0;
                        else
                            value = Integer.valueOf(prefs.getString(Constants.PREF_SIM1[2], ""));
                    }

                    if (!limit.equals("")) {
                        valuer = 1 - Float.valueOf(round) / 100;
                        lim = valuer * DataFormat.getFormatLong(limit, value);
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX1;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX1;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX1 = TrafficStats.getMobileRxBytes();
                    mStartTX1 = TrafficStats.getMobileTxBytes();
                    if ((tot <= (long) lim) || continueOverLimit) {
                        dataMap.put(Constants.LAST_ACTIVE_SIM, activeSIM);
                        rx += diffrx;
                        tx += difftx;
                        tot = tx + rx;
                        simChosen = Constants.DISABLED;
                        isSIM1OverLimit = false;
                    } else {
                        isSIM1OverLimit = true;
                        startCheck(Constants.SIM1);
                    }

                    if (!isSIM1OverLimit) {

                        if (!isNight1) {
                            dataMap.put(Constants.SIM1RX, rx);
                            dataMap.put(Constants.SIM1TX, tx);
                            dataMap.put(Constants.TOTAL1, tot);
                        } else {
                            dataMap.put(Constants.SIM1RX_N, rx);
                            dataMap.put(Constants.SIM1TX_N, tx);
                            dataMap.put(Constants.TOTAL1_N, tot);
                        }
                        dataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        dataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());

                        Calendar myCalendar = Calendar.getInstance();
                        SimpleDateFormat formatDate = new SimpleDateFormat(Constants.DATE_FORMAT, context.getResources().getConfiguration().locale);
                        SimpleDateFormat formatTime = new SimpleDateFormat(Constants.TIME_FORMAT + ":ss", context.getResources().getConfiguration().locale);
                        int choice = 0;
                        if ((diffrx + difftx > MB) || new SimpleDateFormat("ss", context.getResources().getConfiguration().locale).format(myCalendar.getTime()).equals("59")
                                || emptyDB) {
                            String last = (String) TrafficDatabase.readTrafficData(mDatabaseHelper).get(Constants.LAST_DATE);
                            DateTime dt_temp;
                            if (last.equals(""))
                                dt_temp = new org.joda.time.DateTime();
                            else
                                dt_temp = fmtdate.parseDateTime(last);
                            if (!DateCompare.isNextDayOrMonth(dt, "0") && !emptyDB
                                    && !DateCompare.isNextDayOrMonth(dt_temp, "0"))
                                choice = 1;
                            else
                                choice = 2;
                        }
                        dataMap.put(Constants.LAST_TIME, formatTime.format(myCalendar.getTime()));
                        dataMap.put(Constants.LAST_DATE, formatDate.format(myCalendar.getTime()));
                        switch (choice) {
                            default:
                                break;
                            case 1:
                                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                                break;
                            case 2:
                                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                                continueOverLimit = false;
                                break;
                        }

                        String text = "";
                        if (simNumber == 1)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1));
                        else if (simNumber == 2)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                    + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2));
                        else if (simNumber == 3)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                    + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                                    + DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3));

                        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
                        n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                                .setPriority(mPriority)
                                .setLargeIcon(bm)
                                .setWhen(System.currentTimeMillis())
                                .setContentTitle(context.getResources().getString(R.string.notification_title))
                                .setContentText(text)
                                .build();
                        nm.notify(Constants.STARTED_ID, n);
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds().length != 0) && isScreenOn(context))
                        sendDataBroadcast(speedRX, speedTX);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private class CountTimerTask2 extends TimerTask {

        @Override
        public void run() {
            try {
                int[] data = {2, Constants.SIM2};

                if (Arrays.equals(MobileDataControl.getMobileDataInfo(context), data) && !isTimerCancelled) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing dataMap
                    //begin
                    dataMap.put(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                    dataMap.put(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                    dataMap.put(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                    dataMap.put(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                    dataMap.put(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                    dataMap.put(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                    dataMap.put(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                    dataMap.put(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                    dataMap.put(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                    dataMap.put(Constants.SIM1RX_N, (long) dataMap.get(Constants.SIM1RX_N));
                    dataMap.put(Constants.SIM2RX_N, (long) dataMap.get(Constants.SIM2RX_N));
                    dataMap.put(Constants.SIM3RX_N, (long) dataMap.get(Constants.SIM3RX_N));
                    dataMap.put(Constants.SIM1TX_N, (long) dataMap.get(Constants.SIM1TX_N));
                    dataMap.put(Constants.SIM2TX_N, (long) dataMap.get(Constants.SIM2TX_N));
                    dataMap.put(Constants.SIM3TX_N, (long) dataMap.get(Constants.SIM3TX_N));
                    dataMap.put(Constants.TOTAL1_N, (long) dataMap.get(Constants.TOTAL1_N));
                    dataMap.put(Constants.TOTAL2_N, (long) dataMap.get(Constants.TOTAL2_N));
                    dataMap.put(Constants.TOTAL3_N, (long) dataMap.get(Constants.TOTAL3_N));
                    dataMap.put(Constants.LAST_TIME, (String) dataMap.get(Constants.LAST_TIME));
                    dataMap.put(Constants.LAST_DATE, (String) dataMap.get(Constants.LAST_DATE));
                    dataMap.put(Constants.LAST_ACTIVE_SIM, (int) dataMap.get(Constants.LAST_ACTIVE_SIM));
                    //end

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    DateTimeFormatter fmtdate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
                    DateTimeFormatter fmtnow = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
                    DateTime dt = fmtdate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                    DateTime now = new DateTime();

                    isResetNeeded();

                    boolean emptyDB = TrafficDatabase.isEmpty(mDatabaseHelper);

                    if (prefs.getBoolean(Constants.PREF_SIM2[17], false)) {
                        String timeON = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM2[20], "23:00");
                        String timeOFF = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM2[21], "06:00");
                        isNight2 = DateTimeComparator.getInstance().compare(now, fmtnow.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, fmtnow.parseDateTime(timeOFF)) <= 0;
                    } else
                        isNight2 = false;

                    isResetNeeded();

                    if (emptyDB) {
                        dataMap.put(Constants.SIM1RX, 0L);
                        dataMap.put(Constants.SIM2RX, 0L);
                        dataMap.put(Constants.SIM3RX, 0L);
                        dataMap.put(Constants.SIM1TX, 0L);
                        dataMap.put(Constants.SIM2TX, 0L);
                        dataMap.put(Constants.SIM3TX, 0L);
                        dataMap.put(Constants.TOTAL1, 0L);
                        dataMap.put(Constants.TOTAL2, 0L);
                        dataMap.put(Constants.TOTAL3, 0L);
                        dataMap.put(Constants.SIM1RX_N, 0L);
                        dataMap.put(Constants.SIM2RX_N, 0L);
                        dataMap.put(Constants.SIM3RX_N, 0L);
                        dataMap.put(Constants.SIM1TX_N, 0L);
                        dataMap.put(Constants.SIM2TX_N, 0L);
                        dataMap.put(Constants.SIM3TX_N, 0L);
                        dataMap.put(Constants.TOTAL1_N, 0L);
                        dataMap.put(Constants.TOTAL2_N, 0L);
                        dataMap.put(Constants.TOTAL3_N, 0L);
                        dataMap.put(Constants.LAST_RX, 0L);
                        dataMap.put(Constants.LAST_TX, 0L);
                        dataMap.put(Constants.LAST_TIME, "");
                        dataMap.put(Constants.LAST_DATE, "");
                        dataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                    } else if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                            || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                            || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                        if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                            dataMap.put(Constants.SIM1RX, 0L);
                            dataMap.put(Constants.SIM1TX, 0L);
                            dataMap.put(Constants.TOTAL1, 0L);
                            dataMap.put(Constants.SIM1RX_N, 0L);
                            dataMap.put(Constants.SIM1TX_N, 0L);
                            dataMap.put(Constants.TOTAL1_N, 0L);
                            if (!isNight2) {
                                rx = (long) dataMap.get(Constants.SIM2RX);
                                tx = (long) dataMap.get(Constants.SIM2TX);
                                tot = (long) dataMap.get(Constants.TOTAL2);
                            } else {
                                rx = (long) dataMap.get(Constants.SIM2RX_N);
                                tx = (long) dataMap.get(Constants.SIM2TX_N);
                                tot = (long) dataMap.get(Constants.TOTAL2_N);
                            }
                            mReceived1 = mTransmitted1 = 0;
                            needsReset1 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                            dataMap.put(Constants.SIM2RX, 0L);
                            dataMap.put(Constants.SIM2TX, 0L);
                            dataMap.put(Constants.TOTAL2, 0L);
                            dataMap.put(Constants.SIM2RX_N, 0L);
                            dataMap.put(Constants.SIM2TX_N, 0L);
                            dataMap.put(Constants.TOTAL2_N, 0L);
                            rx = tx = mReceived2 = mTransmitted2 = 0;
                            needsReset2 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                            dataMap.put(Constants.SIM3RX, 0L);
                            dataMap.put(Constants.SIM3TX, 0L);
                            dataMap.put(Constants.TOTAL3, 0L);
                            dataMap.put(Constants.SIM3RX_N, 0L);
                            dataMap.put(Constants.SIM3TX_N, 0L);
                            dataMap.put(Constants.TOTAL3_N, 0L);
                            if (!isNight2) {
                                rx = (long) dataMap.get(Constants.SIM2RX);
                                tx = (long) dataMap.get(Constants.SIM2TX);
                                tot = (long) dataMap.get(Constants.TOTAL2);
                            } else {
                                rx = (long) dataMap.get(Constants.SIM2RX_N);
                                tx = (long) dataMap.get(Constants.SIM2TX_N);
                                tot = (long) dataMap.get(Constants.TOTAL2_N);
                            }
                            mReceived3 = mTransmitted3 = 0;
                            needsReset3 = false;
                        }
                    } else {
                        if (!isNight2) {
                            rx = (long) dataMap.get(Constants.SIM2RX);
                            tx = (long) dataMap.get(Constants.SIM2TX);
                            tot = (long) dataMap.get(Constants.TOTAL2);
                        } else {
                            rx = (long) dataMap.get(Constants.SIM2RX_N);
                            tx = (long) dataMap.get(Constants.SIM2TX_N);
                            tot = (long) dataMap.get(Constants.TOTAL2_N);
                        }
                    }


                    String limit, round;
                    int value;
                    float valuer;
                    double lim = Double.MAX_VALUE;

                    if (isNight2) {
                        limit = prefs.getString(Constants.PREF_SIM2[18], "");
                        round = prefs.getString(Constants.PREF_SIM2[22], "0");
                        if (prefs.getString(Constants.PREF_SIM2[19], "").equals(""))
                            value = 0;
                        else
                            value = Integer.valueOf(prefs.getString(Constants.PREF_SIM2[19], ""));
                    } else {
                        limit = prefs.getString(Constants.PREF_SIM2[1], "");
                        round = prefs.getString(Constants.PREF_SIM2[4], "0");
                        if (prefs.getString(Constants.PREF_SIM2[2], "").equals(""))
                            value = 0;
                        else
                            value = Integer.valueOf(prefs.getString(Constants.PREF_SIM2[2], ""));
                    }

                    if (!limit.equals("")) {
                        valuer = 1 - Float.valueOf(round) / 100;
                        lim = valuer * DataFormat.getFormatLong(limit, value);
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX2;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX2;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX2 = TrafficStats.getMobileRxBytes();
                    mStartTX2 = TrafficStats.getMobileTxBytes();
                    if ((tot <= (long) lim) || continueOverLimit) {
                        dataMap.put(Constants.LAST_ACTIVE_SIM, activeSIM);
                        rx += diffrx;
                        tx += difftx;
                        tot = tx + rx;
                        simChosen = Constants.DISABLED;
                        isSIM2OverLimit = false;
                    } else {
                        isSIM2OverLimit = true;
                        startCheck(Constants.SIM2);
                    }

                    if (!isSIM2OverLimit) {

                        if (!isNight2) {
                            dataMap.put(Constants.SIM2RX, rx);
                            dataMap.put(Constants.SIM2TX, tx);
                            dataMap.put(Constants.TOTAL2, tot);
                        } else {
                            dataMap.put(Constants.SIM2RX_N, rx);
                            dataMap.put(Constants.SIM2TX_N, tx);
                            dataMap.put(Constants.TOTAL2_N, tot);
                        }
                        dataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        dataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());

                        Calendar myCalendar = Calendar.getInstance();
                        SimpleDateFormat formatDate = new SimpleDateFormat(Constants.DATE_FORMAT, context.getResources().getConfiguration().locale);
                        SimpleDateFormat formatTime = new SimpleDateFormat(Constants.TIME_FORMAT + ":ss", context.getResources().getConfiguration().locale);
                        int choice = 0;
                        if ((diffrx + difftx > MB) || new SimpleDateFormat("ss", context.getResources().getConfiguration().locale).format(myCalendar.getTime()).equals("59")
                                || emptyDB) {
                            String last = (String) TrafficDatabase.readTrafficData(mDatabaseHelper).get(Constants.LAST_DATE);
                            DateTime dt_temp;
                            if (last.equals(""))
                                dt_temp = new org.joda.time.DateTime();
                            else
                                dt_temp = fmtdate.parseDateTime(last);
                            if (!DateCompare.isNextDayOrMonth(dt, "0") && !emptyDB
                                    && !DateCompare.isNextDayOrMonth(dt_temp, "0"))
                                choice = 1;
                            else
                                choice = 2;
                        }
                        dataMap.put(Constants.LAST_TIME, formatTime.format(myCalendar.getTime()));
                        dataMap.put(Constants.LAST_DATE, formatDate.format(myCalendar.getTime()));
                        switch (choice) {
                            default:
                                break;
                            case 1:
                                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                                break;
                            case 2:
                                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                                continueOverLimit = false;
                                break;
                        }

                        String text = "";
                        if (simNumber == 1)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1));
                        else if (simNumber == 2)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                    + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2));
                        else if (simNumber == 3)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                    + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                                    + DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3));

                        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
                        n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                                .setPriority(mPriority)
                                .setLargeIcon(bm)
                                .setWhen(System.currentTimeMillis())
                                .setContentTitle(context.getResources().getString(R.string.notification_title))
                                .setContentText(text)
                                .build();
                        nm.notify(Constants.STARTED_ID, n);
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds().length != 0) && isScreenOn(context))
                        sendDataBroadcast(speedRX, speedTX);
                }
            }catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private class CountTimerTask3 extends TimerTask {

        @Override
        public void run() {
            try {
                int[] data = {2, Constants.SIM3};

                if (Arrays.equals(MobileDataControl.getMobileDataInfo(context), data) && !isTimerCancelled) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing dataMap
                    //begin
                    dataMap.put(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                    dataMap.put(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                    dataMap.put(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                    dataMap.put(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                    dataMap.put(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                    dataMap.put(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                    dataMap.put(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                    dataMap.put(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                    dataMap.put(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                    dataMap.put(Constants.SIM1RX_N, (long) dataMap.get(Constants.SIM1RX_N));
                    dataMap.put(Constants.SIM2RX_N, (long) dataMap.get(Constants.SIM2RX_N));
                    dataMap.put(Constants.SIM3RX_N, (long) dataMap.get(Constants.SIM3RX_N));
                    dataMap.put(Constants.SIM1TX_N, (long) dataMap.get(Constants.SIM1TX_N));
                    dataMap.put(Constants.SIM2TX_N, (long) dataMap.get(Constants.SIM2TX_N));
                    dataMap.put(Constants.SIM3TX_N, (long) dataMap.get(Constants.SIM3TX_N));
                    dataMap.put(Constants.TOTAL1_N, (long) dataMap.get(Constants.TOTAL1_N));
                    dataMap.put(Constants.TOTAL2_N, (long) dataMap.get(Constants.TOTAL2_N));
                    dataMap.put(Constants.TOTAL3_N, (long) dataMap.get(Constants.TOTAL3_N));
                    dataMap.put(Constants.LAST_TIME, (String) dataMap.get(Constants.LAST_TIME));
                    dataMap.put(Constants.LAST_DATE, (String) dataMap.get(Constants.LAST_DATE));
                    dataMap.put(Constants.LAST_ACTIVE_SIM, (int) dataMap.get(Constants.LAST_ACTIVE_SIM));
                    //end

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    DateTimeFormatter fmtdate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
                    DateTimeFormatter fmtnow = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
                    DateTime dt = fmtdate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                    DateTime now = new DateTime();

                    isResetNeeded();

                    if (prefs.getBoolean(Constants.PREF_SIM3[17], false)) {
                        String timeON = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM3[20], "23:00");
                        String timeOFF = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM3[21], "06:00");
                        isNight3 = DateTimeComparator.getInstance().compare(now, fmtnow.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, fmtnow.parseDateTime(timeOFF)) <= 0;
                    } else
                        isNight3 = false;

                    boolean emptyDB = TrafficDatabase.isEmpty(mDatabaseHelper);
                    if (emptyDB) {
                        dataMap.put(Constants.SIM1RX, 0L);
                        dataMap.put(Constants.SIM2RX, 0L);
                        dataMap.put(Constants.SIM3RX, 0L);
                        dataMap.put(Constants.SIM1TX, 0L);
                        dataMap.put(Constants.SIM2TX, 0L);
                        dataMap.put(Constants.SIM3TX, 0L);
                        dataMap.put(Constants.TOTAL1, 0L);
                        dataMap.put(Constants.TOTAL2, 0L);
                        dataMap.put(Constants.TOTAL3, 0L);
                        dataMap.put(Constants.SIM1RX_N, 0L);
                        dataMap.put(Constants.SIM2RX_N, 0L);
                        dataMap.put(Constants.SIM3RX_N, 0L);
                        dataMap.put(Constants.SIM1TX_N, 0L);
                        dataMap.put(Constants.SIM2TX_N, 0L);
                        dataMap.put(Constants.SIM3TX_N, 0L);
                        dataMap.put(Constants.TOTAL1_N, 0L);
                        dataMap.put(Constants.TOTAL2_N, 0L);
                        dataMap.put(Constants.TOTAL3_N, 0L);
                        dataMap.put(Constants.LAST_RX, 0L);
                        dataMap.put(Constants.LAST_TX, 0L);
                        dataMap.put(Constants.LAST_TIME, "");
                        dataMap.put(Constants.LAST_DATE, "");
                        dataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                    } else if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                            || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                            || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                        if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                            dataMap.put(Constants.SIM1RX, 0L);
                            dataMap.put(Constants.SIM1TX, 0L);
                            dataMap.put(Constants.TOTAL1, 0L);
                            dataMap.put(Constants.SIM1RX_N, 0L);
                            dataMap.put(Constants.SIM1TX_N, 0L);
                            dataMap.put(Constants.TOTAL1_N, 0L);
                            if (!isNight3) {
                                rx = (long) dataMap.get(Constants.SIM3RX);
                                tx = (long) dataMap.get(Constants.SIM3TX);
                                tot = (long) dataMap.get(Constants.TOTAL3);
                            } else {
                                rx = (long) dataMap.get(Constants.SIM3RX_N);
                                tx = (long) dataMap.get(Constants.SIM3TX_N);
                                tot = (long) dataMap.get(Constants.TOTAL3_N);
                            }
                            mReceived1 = mTransmitted1 = 0;
                            needsReset1 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                            dataMap.put(Constants.SIM2RX, 0L);
                            dataMap.put(Constants.SIM2TX, 0L);
                            dataMap.put(Constants.TOTAL2, 0L);
                            dataMap.put(Constants.SIM2RX_N, 0L);
                            dataMap.put(Constants.SIM2TX_N, 0L);
                            dataMap.put(Constants.TOTAL2_N, 0L);
                            if (!isNight3) {
                                rx = (long) dataMap.get(Constants.SIM3RX);
                                tx = (long) dataMap.get(Constants.SIM3TX);
                                tot = (long) dataMap.get(Constants.TOTAL3);
                            } else {
                                rx = (long) dataMap.get(Constants.SIM3RX_N);
                                tx = (long) dataMap.get(Constants.SIM3TX_N);
                                tot = (long) dataMap.get(Constants.TOTAL3_N);
                            }
                            mReceived2 = mTransmitted2 = 0;
                            needsReset2 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                            dataMap.put(Constants.SIM3RX, 0L);
                            dataMap.put(Constants.SIM3TX, 0L);
                            dataMap.put(Constants.TOTAL3, 0L);
                            dataMap.put(Constants.SIM3RX_N, 0L);
                            dataMap.put(Constants.SIM3TX_N, 0L);
                            dataMap.put(Constants.TOTAL3_N, 0L);
                            rx = tx = mReceived3 = mTransmitted3 = 0;
                            needsReset3 = false;
                        }
                    } else {
                        if (!isNight2) {
                            rx = (long) dataMap.get(Constants.SIM3RX);
                            tx = (long) dataMap.get(Constants.SIM3TX);
                            tot = (long) dataMap.get(Constants.TOTAL3);
                        } else {
                            rx = (long) dataMap.get(Constants.SIM3RX_N);
                            tx = (long) dataMap.get(Constants.SIM3TX_N);
                            tot = (long) dataMap.get(Constants.TOTAL3_N);
                        }
                    }


                    String limit, round;
                    int value;
                    float valuer;
                    double lim = Double.MAX_VALUE;

                    if (isNight3) {
                        limit = prefs.getString(Constants.PREF_SIM3[18], "");
                        round = prefs.getString(Constants.PREF_SIM3[22], "0");
                        if (prefs.getString(Constants.PREF_SIM3[19], "").equals(""))
                            value = 0;
                        else
                            value = Integer.valueOf(prefs.getString(Constants.PREF_SIM3[19], ""));
                    } else {
                        limit = prefs.getString(Constants.PREF_SIM3[1], "");
                        round = prefs.getString(Constants.PREF_SIM3[4], "0");
                        if (prefs.getString(Constants.PREF_SIM3[2], "").equals(""))
                            value = 0;
                        else
                            value = Integer.valueOf(prefs.getString(Constants.PREF_SIM3[2], ""));
                    }

                    if (!limit.equals("")) {
                        valuer = 1 - Float.valueOf(round) / 100;
                        lim = valuer * DataFormat.getFormatLong(limit, value);
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX3;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX3;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX3 = TrafficStats.getMobileRxBytes();
                    mStartTX3 = TrafficStats.getMobileTxBytes();
                    if ((tot <= (long) lim) || continueOverLimit) {
                        dataMap.put(Constants.LAST_ACTIVE_SIM, activeSIM);
                        rx += diffrx;
                        tx += difftx;
                        tot = tx + rx;
                        simChosen = Constants.DISABLED;
                        isSIM3OverLimit = false;
                    } else {
                        isSIM3OverLimit = true;
                        startCheck(Constants.SIM3);
                    }

                    if (!isSIM3OverLimit) {

                        if (!isNight3) {
                            dataMap.put(Constants.SIM3RX, rx);
                            dataMap.put(Constants.SIM3TX, tx);
                            dataMap.put(Constants.TOTAL3, tot);
                        } else {
                            dataMap.put(Constants.SIM3RX_N, rx);
                            dataMap.put(Constants.SIM3TX_N, tx);
                            dataMap.put(Constants.TOTAL3_N, tot);
                        }
                        dataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        dataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());

                        Calendar myCalendar = Calendar.getInstance();
                        SimpleDateFormat formatDate = new SimpleDateFormat(Constants.DATE_FORMAT, context.getResources().getConfiguration().locale);
                        SimpleDateFormat formatTime = new SimpleDateFormat(Constants.TIME_FORMAT + ":ss", context.getResources().getConfiguration().locale);
                        int choice = 0;
                        if ((diffrx + difftx > MB) || new SimpleDateFormat("ss", context.getResources().getConfiguration().locale).format(myCalendar.getTime()).equals("59")
                                || emptyDB) {
                            String last = (String) TrafficDatabase.readTrafficData(mDatabaseHelper).get(Constants.LAST_DATE);
                            DateTime dt_temp;
                            if (last.equals(""))
                                dt_temp = new org.joda.time.DateTime();
                            else
                                dt_temp = fmtdate.parseDateTime(last);
                            if (!DateCompare.isNextDayOrMonth(dt, "0") && !emptyDB
                                    && !DateCompare.isNextDayOrMonth(dt_temp, "0"))
                                choice = 1;
                            else
                                choice = 2;
                        }
                        dataMap.put(Constants.LAST_TIME, formatTime.format(myCalendar.getTime()));
                        dataMap.put(Constants.LAST_DATE, formatDate.format(myCalendar.getTime()));
                        switch (choice) {
                            default:
                                break;
                            case 1:
                                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                                break;
                            case 2:
                                TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                                continueOverLimit = false;
                                break;
                        }

                        String text = "";
                        if (simNumber == 1)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1));
                        else if (simNumber == 2)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                    + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2));
                        else if (simNumber == 3)
                            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                    + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                                    + DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3));

                        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
                        n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                                .setPriority(mPriority)
                                .setLargeIcon(bm)
                                .setWhen(System.currentTimeMillis())
                                .setContentTitle(context.getResources().getString(R.string.notification_title))
                                .setContentText(text)
                                .build();
                        nm.notify(Constants.STARTED_ID, n);
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds().length != 0) && isScreenOn(context))
                        sendDataBroadcast(speedRX, speedTX);
                }

            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private void sendDataBroadcast(long speedRX, long speedTX) {
        Intent intent = new Intent(Constants.BROADCAST_ACTION);
        intent.putExtra(Constants.WIDGET_IDS, getWidgetIds());
        intent.putExtra(Constants.SPEEDRX, speedRX);
        intent.putExtra(Constants.SPEEDTX, speedTX);
        intent.putExtra(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
        intent.putExtra(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
        intent.putExtra(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
        intent.putExtra(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
        intent.putExtra(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
        intent.putExtra(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
        intent.putExtra(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
        intent.putExtra(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
        intent.putExtra(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
        intent.putExtra(Constants.SIM1RX_N, (long) dataMap.get(Constants.SIM1RX_N));
        intent.putExtra(Constants.SIM2RX_N, (long) dataMap.get(Constants.SIM2RX_N));
        intent.putExtra(Constants.SIM3RX_N, (long) dataMap.get(Constants.SIM3RX_N));
        intent.putExtra(Constants.SIM1TX_N, (long) dataMap.get(Constants.SIM1TX_N));
        intent.putExtra(Constants.SIM2TX_N, (long) dataMap.get(Constants.SIM2TX_N));
        intent.putExtra(Constants.SIM3TX_N, (long) dataMap.get(Constants.SIM3TX_N));
        intent.putExtra(Constants.TOTAL1_N, (long) dataMap.get(Constants.TOTAL1_N));
        intent.putExtra(Constants.TOTAL2_N, (long) dataMap.get(Constants.TOTAL2_N));
        intent.putExtra(Constants.TOTAL3_N, (long) dataMap.get(Constants.TOTAL3_N));
        if (activeSIM == Constants.DISABLED)
            intent.putExtra(Constants.SIM_ACTIVE, lastActiveSIM);
        else
            intent.putExtra(Constants.SIM_ACTIVE, activeSIM);
        intent.putExtra(Constants.OPERATOR1, MobileDataControl.getName(context, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
        if (simNumber >= 2)
            intent.putExtra(Constants.OPERATOR2, MobileDataControl.getName(context, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
        if (simNumber == 3)
            intent.putExtra(Constants.OPERATOR3, MobileDataControl.getName(context, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
        context.sendBroadcast(intent);
    }

    private void startCheck(int alertID) {

        if (prefs.getBoolean(Constants.PREF_OTHER[3], false))
            alertNotify(alertID);

        try {
            MobileDataControl.toggleMobileDataConnection(false, context, Constants.DISABLED);
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }

        if (((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true)))) {

            isTimerCancelled = true;
            mTimer.cancel();
            mTimer.purge();

            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_disable);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(mPriority)
                    .setLargeIcon(bm)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(context.getResources().getString(R.string.service_stopped_title))
                    .build();
            nm.notify(Constants.STARTED_ID, n);
        }

        if (!prefs.getBoolean(Constants.PREF_SIM1[7], true) || !prefs.getBoolean(Constants.PREF_SIM2[7], true)
                || !prefs.getBoolean(Constants.PREF_SIM3[7], true) || !prefs.getBoolean(Constants.PREF_OTHER[10], true)) {
                Intent dialogIntent = new Intent(context, ChooseAction.class);
                dialogIntent.putExtra(Constants.SIM_ACTIVE, alertID);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!ChooseAction.isShown())
                    context.startActivity(dialogIntent);
        } else if (((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true))) &&
                prefs.getBoolean(Constants.PREF_OTHER[10], true)) {
            try {
                if (!isSIM2OverLimit && alertID == Constants.SIM1 && simNumber >=2) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM2);
                    timerStart(Constants.COUNT);
                } else if (!isSIM3OverLimit && alertID == Constants.SIM1 && simNumber == 3) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM3);
                    timerStart(Constants.COUNT);
                } else if (!isSIM1OverLimit && alertID == Constants.SIM2) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM1);
                    timerStart(Constants.COUNT);
                } else if (!isSIM3OverLimit && alertID == Constants.SIM2 && simNumber == 3) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM3);
                    timerStart(Constants.COUNT);
                } else if (!isSIM1OverLimit && alertID == Constants.SIM3) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM1);
                    timerStart(Constants.COUNT);
                } else if (!isSIM2OverLimit && alertID == Constants.SIM3) {
                    MobileDataControl.toggleMobileDataConnection(true, context, Constants.SIM2);
                    timerStart(Constants.COUNT);
                } else {
                    Intent intent = new Intent(Constants.TIP);
                    context.sendBroadcast(intent);
                    timerStart(Constants.CHECK);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        } else if ((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true))) {
            Intent dialogIntent = new Intent(context, ChooseAction.class);
            dialogIntent.putExtra(Constants.SIM_ACTIVE, alertID);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!ChooseAction.isShown())
                context.startActivity(dialogIntent);
        } else if (isSIM1OverLimit && isSIM2OverLimit && isSIM3OverLimit && prefs.getBoolean(Constants.PREF_OTHER[10], true)){
            Intent dialogIntent = new Intent(context, ChooseAction.class);
            dialogIntent.putExtra(Constants.SIM_ACTIVE, alertID);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!ChooseAction.isShown())
                context.startActivity(dialogIntent);
        } else if (isSIM1OverLimit && isSIM2OverLimit && isSIM2OverLimit) {
            isTimerCancelled = true;
            mTimer.cancel();
            mTimer.purge();
            try {
                MobileDataControl.toggleMobileDataConnection(false, context, Constants.DISABLED);
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }

            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_disable);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(mPriority)
                    .setLargeIcon(bm)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(context.getResources().getString(R.string.service_stopped_title))
                    .build();
            nm.notify(Constants.STARTED_ID, n);

            timerStart(Constants.CHECK);
        }
    }

    private void alertNotify(int alertID) {
        Intent notificationIntent;
        if ((prefs.getBoolean(Constants.PREF_SIM1[7], true) && isSIM1OverLimit) ||
                (prefs.getBoolean(Constants.PREF_SIM2[7], true) && isSIM2OverLimit) ||
                (prefs.getBoolean(Constants.PREF_SIM2[7], true) && isSIM3OverLimit))
            notificationIntent = new Intent(context, MainActivity.class);
        else {
            final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
            notificationIntent = new Intent(Intent.ACTION_MAIN);
            notificationIntent.setComponent(cn);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        if (prefs.getBoolean(Constants.PREF_OTHER[3], false) && prefs.getBoolean(Constants.PREF_OTHER[2], false))
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_alert);
        String opName;
        if (alertID == Constants.SIM1)
            opName = MobileDataControl.getName(context, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        else if (alertID == Constants.SIM2)
            opName = MobileDataControl.getName(context, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        else
            opName = MobileDataControl.getName(context, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        String txt;
        if ((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true)))
            txt = context.getResources().getString(R.string.data_dis);
        else
            txt = context.getResources().getString(R.string.data_dis_tip);

        Notification n = builder.setContentIntent(pIntent).setSmallIcon(R.drawable.ic_launcher_small)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLargeIcon(bm)
                .setTicker(context.getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(txt)
                .setContentText(opName + ": " + context.getResources().getString(R.string.over_limit))
                .build();
        if (prefs.getBoolean(Constants.PREF_OTHER[4], false) && !prefs.getString(Constants.PREF_OTHER[1], "").equals("")) {
            n.sound = Uri.parse(prefs.getString(Constants.PREF_OTHER[1], ""));
            n.flags = Notification.FLAG_ONLY_ALERT_ONCE;
        }
        nm.notify(alertID, n);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isTimerCancelled = true;
        mTimer.cancel();
        mTimer.purge();
        nm.cancel(Constants.STARTED_ID);
        TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(clear1Receiver);
        unregisterReceiver(clear2Receiver);
        unregisterReceiver(clear3Receiver);
        unregisterReceiver(setUsage);
        unregisterReceiver(actionReceive);
        unregisterReceiver(connReceiver);
    }

    private static int[] getWidgetIds() {
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, InfoWidget.class));
        if (ids.length == 0) {
            File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
            String[] children = dir.list();
            int i = 0;
            for (String aChildren : children) {
                if (aChildren.split("_")[1].equalsIgnoreCase("widget.xml")) {
                    ids[i] = Integer.valueOf(aChildren.split("_")[0]);
                    i++;
                }
            }
        }
        return ids;
    }
    
    private static boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
            return pm.isInteractive();
        else
            return pm.isScreenOn();
    }
}
