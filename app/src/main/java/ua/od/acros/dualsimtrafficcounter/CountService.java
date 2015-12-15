package ua.od.acros.dualsimtrafficcounter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.dialogs.ChooseAction;
import ua.od.acros.dualsimtrafficcounter.settings.LimitFragment;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.CheckVisibility;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.DateCompare;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;
import ua.od.acros.dualsimtrafficcounter.widget.InfoWidget;


public class CountService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static PendingIntent contentIntent;
    private static NotificationManager nm;
    private static NotificationCompat.Builder builder;
    private static Notification n;

    private static long totalRxBytes;
    private static long totalTxBytes;
    private static long lastUpdateTime;
    private static long mStartRX1 = 0;
    private static long mStartTX1 = 0;
    private static long mStartRX2 = 0;
    private static long mStartTX2 = 0;
    private static long mStartRX3 = 0;
    private static long mStartTX3 = 0;
    private static long rcvd1 = 0;
    private static long trans1 = 0;
    private static long rcvd2 = 0;
    private static long trans2 = 0;
    private static long rcvd3 = 0;
    private static long trans3 = 0;
    private static boolean isFirstRun = false;
    private static boolean isSIM1OverLimit = false;
    private static boolean isSIM2OverLimit = false;
    private static boolean isSIM3OverLimit = false;
    private static boolean isTimerCancelled = false;
    private static boolean continueOverLimit = false;
    private static boolean needsReset3 = false;
    private static boolean needsReset2 = false;
    private static boolean needsReset1 = false;
    private static int simChosen;
    private static int simNumber;
    private static int priority;
    private static DateTime resetTime1;
    private static DateTime resetTime2;
    private static DateTime resetTime3;

    static Map<String, Object> dataMap;
    private static int activeSIM;
    private static int lastActiveSIM;
    BroadcastReceiver clear1Receiver, clear2Receiver, clear3Receiver, connReceiver, /*simChange,*/ setUsage, actionReceive;

    private static Context context;
    private static TrafficDatabase mDatabaseHelper;

    private static Timer mTimer = null;
    private static SharedPreferences prefs;


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

        priority = prefs.getBoolean(Constants.PREF_OTHER[12], true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN;

        context = CountService.this;

        dataMap = new HashMap<>();
        mDatabaseHelper = new TrafficDatabase(this, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
        dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
        if (dataMap.get(Constants.LAST_DATE).equals("")) {
            Calendar myCalendar = Calendar.getInstance();
            SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", getAppContext().getResources().getConfiguration().locale);
            SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm:ss", getAppContext().getResources().getConfiguration().locale);
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
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                    }

                    if (prefs.getBoolean(Constants.PREF_SIM2[14], true) && lastActiveSIM == Constants.SIM2) {
                        dataMap.put(Constants.TOTAL2, DataFormat.getRoundLong((long) dataMap.get(Constants.TOTAL2),
                                prefs.getString(Constants.PREF_SIM2[15], "1"), prefs.getString(Constants.PREF_SIM2[16], "0")));
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                    }

                    if (prefs.getBoolean(Constants.PREF_SIM3[14], true) && lastActiveSIM == Constants.SIM3) {
                        dataMap.put(Constants.TOTAL3, DataFormat.getRoundLong((long) dataMap.get(Constants.TOTAL3),
                                prefs.getString(Constants.PREF_SIM3[15], "1"), prefs.getString(Constants.PREF_SIM3[16], "0")));
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                    }

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }                    
                } else
                    if (MobileDataControl.getMobileDataInfo(getAppContext())[0] == 2)
                        timerStart(Constants.COUNT);
            }
        };
        IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connReceiver, connFilter);

        /*simChange = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch ((int) intent.getLongExtra("simid", Constants.DISABLED + 1)) {
                    case Constants.DISABLED + 1:
                        Toast.makeText(context, R.string.data_dis, Toast.LENGTH_LONG).show();
                        break;
                    case Constants.SIM1 + 1:
                        Toast.makeText(context, R.string.sim1_act, Toast.LENGTH_LONG).show();
                        break;
                    case  Constants.SIM2 + 1:
                        Toast.makeText(context, R.string.sim2_act, Toast.LENGTH_LONG).show();
                            break;
                    case  Constants.SIM3 + 1:
                        Toast.makeText(context, R.string.sim3_act, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
        IntentFilter simChangeFilter = new IntentFilter("android.intent.action.DATA_DEFAULT_SIM");
        registerReceiver(simChange, simChangeFilter);*/

        actionReceive = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int simid = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                try {
                switch (intent.getStringExtra(Constants.ACTION)) {
                    case Constants.CHOOSE_ACTION:
                        if (!isSIM2OverLimit && simid == Constants.SIM1) {
                            MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                            MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM2);
                            timerStart(Constants.COUNT);
                        }
                        else if (!isSIM3OverLimit && simid == Constants.SIM1) {
                            MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                            MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM3);
                            timerStart(Constants.COUNT);
                        }
                        else if (!isSIM1OverLimit && simid == Constants.SIM2) {
                            MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                            MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM1);
                            timerStart(Constants.COUNT);
                        }
                        else if (!isSIM3OverLimit && simid == Constants.SIM2) {
                            MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                            MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM3);
                            timerStart(Constants.COUNT);
                        } else if (!isSIM1OverLimit && simid == Constants.SIM3) {
                            MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                            MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM1);
                            timerStart(Constants.COUNT);
                        } else if (!isSIM2OverLimit && simid == Constants.SIM3) {
                            MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                            MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM2);
                            timerStart(Constants.COUNT);
                        } else {
                            MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                            timerStart(Constants.CHECK);
                        }
                        break;
                    case Constants.LIMIT_ACTION:
                        Intent i = new Intent(getAppContext(), SettingsActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, LimitFragment.class.getName());
                        i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                        startActivity(i);
                        timerStart(Constants.CHECK);
                        break;
                    case Constants.CONTINUE_ACTION:
                        MobileDataControl.toggleMobileDataConnection(true, getAppContext(), simid);
                        continueOverLimit = true;
                        timerStart(Constants.COUNT);
                        break;
                    case Constants.OFF_ACTION:
                        timerStart(Constants.CHECK);
                        break;
                }
                } catch (Exception e) {
                    e.printStackTrace();
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
                }
                if (dataMap == null)
                    dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
                Bundle limitBundle = intent.getBundleExtra("data");
                simChosen = limitBundle.getInt("sim");
                switch (simChosen) {
                    case  Constants.SIM1:
                        rcvd1 = DataFormat.getFormatLong(limitBundle.getString("rcvd"), limitBundle.getInt("rxV"));
                        trans1 = DataFormat.getFormatLong(limitBundle.getString("trans"), limitBundle.getInt("txV"));
                        dataMap.put(Constants.SIM1RX, rcvd1);
                        dataMap.put(Constants.SIM1TX, trans1);
                        dataMap.put(Constants.TOTAL1, rcvd1 + trans1);
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                        break;
                    case  Constants.SIM2:
                        rcvd2 = DataFormat.getFormatLong(limitBundle.getString("rcvd"), limitBundle.getInt("rxV"));
                        trans2 = DataFormat.getFormatLong(limitBundle.getString("trans"), limitBundle.getInt("txV"));
                        dataMap.put(Constants.SIM2RX, rcvd2);
                        dataMap.put(Constants.SIM2TX, trans2);
                        dataMap.put(Constants.TOTAL2, rcvd2 + trans2);
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                        break;
                    case  Constants.SIM3:
                        rcvd3 = DataFormat.getFormatLong(limitBundle.getString("rcvd"), limitBundle.getInt("rxV"));
                        trans3 = DataFormat.getFormatLong(limitBundle.getString("trans"), limitBundle.getInt("txV"));
                        dataMap.put(Constants.SIM3RX, rcvd3);
                        dataMap.put(Constants.SIM3TX, trans3);
                        dataMap.put(Constants.TOTAL3, rcvd3 + trans3);
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                        break;
                    }
                n = builder.setContentIntent(contentIntent)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setPriority(priority)
                        .setContentText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1))
                                + "   ||   " + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL2))
                                + "   ||   " + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL3)))
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
                }
                dataMap.put(Constants.SIM1RX, 0L);
                dataMap.put(Constants.SIM1TX, 0L);
                dataMap.put(Constants.TOTAL1, 0L);
                TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
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
                }
                dataMap.put(Constants.SIM2RX, 0L);
                dataMap.put(Constants.SIM2TX, 0L);
                dataMap.put(Constants.TOTAL2, 0L);
                TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
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
                }
                dataMap.put(Constants.SIM3RX, 0L);
                dataMap.put(Constants.SIM3TX, 0L);
                dataMap.put(Constants.TOTAL3, 0L);
                TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
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
        lastActiveSIM = Constants.DISABLED;

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

        isFirstRun = true;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(priority)
                .setLargeIcon(bm)
                .setTicker(getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentTitle(getAppContext().getResources().getString(R.string.notification_title))
                .setContentText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1))
                        + "   ||   " + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL2))
                        + "   ||   " + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL3)))
                .build();
        startForeground(Constants.STARTED_ID, n);

        // schedule task
        timerStart(Constants.COUNT);

        return START_STICKY;
    }

    protected static Context getAppContext() {
        return CountService.context;
    }

    protected static void timerStart(int task) {
        TimerTask tTask = null;
        simNumber = MobileDataControl.isMultiSim(getAppContext());
        activeSIM = MobileDataControl.getMobileDataInfo(getAppContext())[1];
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

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     * <p/>
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link SharedPreferences} that received
     *                          the change.
     * @param key               The key of the preference that was changed, added, or
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[12]))
            if (sharedPreferences.getBoolean(key, true))
                priority = NotificationCompat.PRIORITY_MAX;
        else
                priority = NotificationCompat.PRIORITY_MIN;
    }

    private static class CheckTimerTask extends TimerTask {

        @Override
        public void run() {

            getAppContext().sendBroadcast(new Intent(Constants.TIP));

            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
            DateTime dt = fmt.parseDateTime((String) dataMap.get(Constants.LAST_DATE));

            String limit1 = prefs.getString(Constants.PREF_SIM1[1], "");
            String limit2 = prefs.getString(Constants.PREF_SIM2[1], "");
            String limit3 = prefs.getString(Constants.PREF_SIM3[1], "");
            String round1 = prefs.getString(Constants.PREF_SIM1[4], "0");
            String round2 = prefs.getString(Constants.PREF_SIM2[4], "0");
            String round3 = prefs.getString(Constants.PREF_SIM3[4], "0");
            int value1;
            if (prefs.getString(Constants.PREF_SIM1[2], "").equals(""))
                value1 = 0;
            else
                value1 = Integer.valueOf(prefs.getString(Constants.PREF_SIM1[2], ""));
            int value2;
            if (prefs.getString(Constants.PREF_SIM2[2], "").equals(""))
                value2 = 0;
            else
                value2 = Integer.valueOf(prefs.getString(Constants.PREF_SIM2[2], ""));
            int value3;
            if (prefs.getString(Constants.PREF_SIM3[2], "").equals(""))
                value3 = 0;
            else
                value3 = Integer.valueOf(prefs.getString(Constants.PREF_SIM3[2], ""));
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
            try {
                if (isSIM1OverLimit && (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM1[3], ""))
                        || ((long) dataMap.get(Constants.TOTAL1) <= (long) lim1 && (prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        || (!prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !prefs.getBoolean(Constants.PREF_SIM2[8], false) && !prefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM1);
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;
                    CountService.timerStart(Constants.COUNT);
                    isFirstRun = true;
                }
                if (isSIM2OverLimit && (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM2[3], ""))
                        || ((long) dataMap.get(Constants.TOTAL2) <= (long) lim2 && (prefs.getBoolean(Constants.PREF_SIM2[8], false)
                        || (!prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !prefs.getBoolean(Constants.PREF_SIM2[8], false) && !prefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM2);
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;
                    CountService.timerStart(Constants.COUNT);
                    isFirstRun = true;
                }
                if (isSIM3OverLimit && (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM3[3], ""))
                        || ((long) dataMap.get(Constants.TOTAL3) <= (long) lim3 && (prefs.getBoolean(Constants.PREF_SIM3[8], false)
                        || (!prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !prefs.getBoolean(Constants.PREF_SIM2[8], false) && !prefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM3);
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;
                    CountService.timerStart(Constants.COUNT);
                    isFirstRun = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getName(String key1, String key2, int sim) {
        if (prefs.getBoolean(key1, true))
            return MobileDataControl.getOperatorNames(getAppContext()).get(sim);
        else
            return prefs.getString(key2, "");
    }

    private static class CountTimerTask1 extends TimerTask {

        private static final long MB = 1024 * 1024;

        @Override
        public void run() {
            int[] data = {2, Constants.SIM1};
            if (Arrays.equals(MobileDataControl.getMobileDataInfo(getAppContext()), data) && !isTimerCancelled) {

                long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
                lastUpdateTime = SystemClock.elapsedRealtime();

                long newTotalRxBytes = TrafficStats.getTotalRxBytes();
                long newTotalTxBytes = TrafficStats.getTotalTxBytes();

                long rxData = newTotalRxBytes - totalRxBytes;
                long txData = newTotalTxBytes - totalTxBytes;

                long speedRX = (long)(rxData / (timeDelta / 1000F));
                long speedTX = (long)(txData / (timeDelta / 1000F));

                totalRxBytes = newTotalRxBytes;
                totalTxBytes = newTotalTxBytes;

                long rx = 0;
                long tx = 0;
                long tot = 0;
                
                DateTimeFormatter fmtdate = DateTimeFormat.forPattern("yyyy-MM-dd");
                DateTimeFormatter fmtnow = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
                DateTime dt = fmtdate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                DateTime now = new DateTime();

                String reset1 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM1[9], "00:00");
                String reset2 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM2[9], "00:00");
                String reset3 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM3[9], "00:00");

                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM1[3], "")) || needsReset1) {
                    needsReset1 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM1[3], "").equals("0"))
                        resetTime1 = fmtnow.parseDateTime(reset1);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime1 = fmtnow.parseDateTime(reset1);
                    }
                }
                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM2[3], "")) || needsReset2) {
                    needsReset2 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM2[3], "").equals("0"))
                        resetTime2 = fmtnow.parseDateTime(reset2);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime2 = fmtnow.parseDateTime(reset2);
                    }
                }
                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM3[3], "")) || needsReset3) {
                    needsReset3 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM3[3], "").equals("0"))
                        resetTime3 = fmtnow.parseDateTime(reset3);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime3 = fmtnow.parseDateTime(reset3);
                    }
                }
                boolean emptyDB = TrafficDatabase.isEmpty(mDatabaseHelper);
                if (emptyDB){
                    dataMap.put(Constants.SIM1RX, 0L);
                    dataMap.put(Constants.SIM2RX, 0L);
                    dataMap.put(Constants.SIM3RX, 0L);
                    dataMap.put(Constants.SIM1TX, 0L);
                    dataMap.put(Constants.SIM2TX, 0L);
                    dataMap.put(Constants.SIM3TX, 0L);
                    dataMap.put(Constants.TOTAL1, 0L);
                    dataMap.put(Constants.TOTAL2, 0L);
                    dataMap.put(Constants.TOTAL3, 0L);
                    dataMap.put(Constants.LAST_RX, 0L);
                    dataMap.put(Constants.LAST_TX, 0L);
                    dataMap.put(Constants.LAST_TIME, "");
                    dataMap.put(Constants.LAST_DATE, "");
                    dataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                } else if (isFirstRun) {
                    if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                            || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                            || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                        if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                            dataMap.put(Constants.SIM1RX, 0L);
                            dataMap.put(Constants.SIM1TX, 0L);
                            dataMap.put(Constants.TOTAL1, 0L);
                            rx = tx = rcvd1 = trans1 = 0;
                            needsReset1 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                            dataMap.put(Constants.SIM2RX, 0L);
                            dataMap.put(Constants.SIM2TX, 0L);
                            dataMap.put(Constants.TOTAL2, 0L);
                            rx = (long) dataMap.get(Constants.SIM1RX);
                            tx = (long) dataMap.get(Constants.SIM1TX);
                            tot = (long) dataMap.get(Constants.TOTAL1);
                            rcvd2 = trans2 = 0;
                            needsReset2 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                            dataMap.put(Constants.SIM3RX, 0L);
                            dataMap.put(Constants.SIM3TX, 0L);
                            dataMap.put(Constants.TOTAL3, 0L);
                            rx = (long) dataMap.get(Constants.SIM1RX);
                            tx = (long) dataMap.get(Constants.SIM1TX);
                            tot = (long) dataMap.get(Constants.TOTAL1);
                            rcvd3 = trans3 = 0;
                            needsReset3 = false;
                        }
                    } else {
                        dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
                        rx = (long) dataMap.get(Constants.SIM1RX);
                        tx = (long) dataMap.get(Constants.SIM1TX);
                        tot = (long) dataMap.get(Constants.TOTAL1);
                    }
                } else if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                        || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                        || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                    if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                        dataMap.put(Constants.SIM1RX, 0L);
                        dataMap.put(Constants.SIM1TX, 0L);
                        dataMap.put(Constants.TOTAL1, 0L);
                        rx = tx = rcvd1 = trans1 = 0;
                        needsReset1 = false;
                    }
                    if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                        dataMap.put(Constants.SIM2RX, 0L);
                        dataMap.put(Constants.SIM2TX, 0L);
                        dataMap.put(Constants.TOTAL2, 0L);
                        rx = (long) dataMap.get(Constants.SIM1RX);
                        tx = (long) dataMap.get(Constants.SIM1TX);
                        tot = (long) dataMap.get(Constants.TOTAL1);
                        rcvd2 = trans2 = 0;
                        needsReset2 = false;
                    }
                    if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                        dataMap.put(Constants.SIM3RX, 0L);
                        dataMap.put(Constants.SIM3TX, 0L);
                        dataMap.put(Constants.TOTAL3, 0L);
                        rx = (long) dataMap.get(Constants.SIM1RX);
                        tx = (long) dataMap.get(Constants.SIM1TX);
                        tot = (long) dataMap.get(Constants.TOTAL1);
                        rcvd3 = trans3 = 0;
                        needsReset3 = false;
                    }
                }else {
                    rx = (long) dataMap.get(Constants.SIM1RX);
                    tx = (long) dataMap.get(Constants.SIM1TX);
                    tot = (long) dataMap.get(Constants.TOTAL1);
                }


                String limit = prefs.getString(Constants.PREF_SIM1[1], "");
                String round = prefs.getString(Constants.PREF_SIM1[4], "0");

                int value;
                if (prefs.getString(Constants.PREF_SIM1[2], "").equals(""))
                    value = 0;
                else
                    value = Integer.valueOf(prefs.getString(Constants.PREF_SIM1[2], ""));

                float valuer;

                double lim = Double.MAX_VALUE;

                if (!limit.equals("")) {
                    valuer = 1 - Float.valueOf(round) / 100;
                    lim = valuer * DataFormat.getFormatLong(limit, value);
                }

                long diffrx = TrafficStats.getMobileRxBytes() - mStartRX1;
                long difftx = TrafficStats.getMobileTxBytes() - mStartTX1;
                mStartRX1 = TrafficStats.getMobileRxBytes();
                mStartTX1 = TrafficStats.getMobileTxBytes();
                if (((long) dataMap.get(Constants.TOTAL1) <= (long) lim) || continueOverLimit) {
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

                    dataMap.put(Constants.SIM1RX, rx);
                    dataMap.put(Constants.SIM1TX, tx);
                    dataMap.put(Constants.TOTAL1, tot);
                    dataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                    dataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());

                    Calendar myCalendar = Calendar.getInstance();
                    SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", getAppContext().getResources().getConfiguration().locale);
                    SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm:ss", getAppContext().getResources().getConfiguration().locale);
                    int choice = 0;
                    if ((diffrx > MB || difftx > MB) || new SimpleDateFormat("ss", getAppContext().getResources().getConfiguration().locale).format(myCalendar.getTime()).equals("59")
                            || emptyDB) {
                        String last = (String) TrafficDatabase.read_writeTrafficData(Constants.READ, new HashMap<String, Object>(), mDatabaseHelper).get(Constants.LAST_DATE);
                        DateTime dt_temp;
                        if (last.equals(""))
                            dt_temp = new org.joda.time.DateTime();
                        else
                            dt_temp = fmtdate.parseDateTime(last);
                        if (!DateCompare.isNextDayOrMonth(dt, "0")  && !emptyDB
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
                            TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                            break;
                        case 2:
                            TrafficDatabase.read_writeTrafficData(Constants.WRITE, dataMap, mDatabaseHelper);
                            continueOverLimit = false;
                            break;
                    }

                    if (CheckVisibility.isActivityVisible() ||
                            (AppWidgetManager.getInstance(getAppContext()).
                                    getAppWidgetIds(new ComponentName(getAppContext(), InfoWidget.class)).length != 0) &&
                            isScreenOn(getAppContext())) {
                        Intent intent = new Intent(Constants.BROADCAST_ACTION);
                        intent.putExtra(Constants.SPEEDRX, speedRX);
                        intent.putExtra(Constants.SPEEDTX, speedTX);
                        intent.putExtra(Constants.SIM1RX, rx);
                        intent.putExtra(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                        intent.putExtra(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                        intent.putExtra(Constants.SIM1TX, tx);
                        intent.putExtra(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                        intent.putExtra(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                        intent.putExtra(Constants.TOTAL1, tot);
                        intent.putExtra(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                        intent.putExtra(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                        intent.putExtra(Constants.SIM_ACTIVE, activeSIM);
                        intent.putExtra(Constants.TIP, (isSIM1OverLimit || isSIM2OverLimit || isSIM3OverLimit) && !continueOverLimit);
                        intent.putExtra(Constants.OPERATOR1, getName(Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                        if (simNumber >= 2)
                            intent.putExtra(Constants.OPERATOR2, getName(Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                        if (simNumber == 3)
                            intent.putExtra(Constants.OPERATOR3, getName(Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                        getAppContext().sendBroadcast(intent);
                    }

                    String text = "";
                    if (simNumber == 1)
                        text = DataFormat.formatData(getAppContext(), tot);
                    else if (simNumber == 2)
                        text = DataFormat.formatData(getAppContext(), tot) + "   ||   "
                                + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL2));
                    else if (simNumber == 3)
                        text = DataFormat.formatData(getAppContext(), tot) + "   ||   "
                                + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                                + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL3));

                    Bitmap bm = BitmapFactory.decodeResource(getAppContext().getResources(), R.mipmap.ic_launcher);
                    n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE)
                            .setPriority(priority)
                            .setLargeIcon(bm)
                            .setWhen(System.currentTimeMillis())
                            .setContentTitle(getAppContext().getResources().getString(R.string.notification_title))
                            .setContentText(text)
                            .build();
                    nm.notify(Constants.STARTED_ID, n);
                }
                isFirstRun = false;
            } else {
                Intent intent = new Intent(Constants.BROADCAST_ACTION);
                intent.putExtra(Constants.SPEEDRX, 0L);
                intent.putExtra(Constants.SPEEDTX, 0L);
                intent.putExtra(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                intent.putExtra(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                intent.putExtra(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                intent.putExtra(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                intent.putExtra(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                intent.putExtra(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                intent.putExtra(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                intent.putExtra(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                intent.putExtra(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                intent.putExtra(Constants.SIM_ACTIVE, MobileDataControl.getMobileDataInfo(getAppContext())[1]);
                intent.putExtra(Constants.OPERATOR1, getName(Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                if (simNumber >= 2)
                    intent.putExtra(Constants.OPERATOR2, getName(Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                if (simNumber == 3)
                    intent.putExtra(Constants.OPERATOR3, getName(Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                getAppContext().sendBroadcast(intent);
            }
        }
    }

    private static class CountTimerTask2 extends TimerTask {

        private static final long MB = 1024 * 1024;

        @Override
        public void run() {
            int[] data = {2, Constants.SIM2};
            if (Arrays.equals(MobileDataControl.getMobileDataInfo(getAppContext()), data) && !isTimerCancelled) {

                long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
                lastUpdateTime = SystemClock.elapsedRealtime();

                long newTotalRxBytes = TrafficStats.getTotalRxBytes();
                long newTotalTxBytes = TrafficStats.getTotalTxBytes();

                long rxData = newTotalRxBytes - totalRxBytes;
                long txData = newTotalTxBytes - totalTxBytes;

                long speedRX = (long)(rxData / (timeDelta / 1000F));
                long speedTX = (long)(txData / (timeDelta / 1000F));

                totalRxBytes = newTotalRxBytes;
                totalTxBytes = newTotalTxBytes;

                long rx = 0;
                long tx = 0;
                long tot = 0;

                DateTimeFormatter fmtdate = DateTimeFormat.forPattern("yyyy-MM-dd");
                DateTimeFormatter fmtnow = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
                DateTime dt = fmtdate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                DateTime now = new DateTime();

                String reset1 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM1[9], "00:00");
                String reset2 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM2[9], "00:00");
                String reset3 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM3[9], "00:00");

                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM1[3], "")) || needsReset1) {
                    needsReset1 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM1[3], "").equals("0"))
                        resetTime1 = fmtnow.parseDateTime(reset1);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime1 = fmtnow.parseDateTime(reset1);
                    }
                }
                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM2[3], "")) || needsReset2) {
                    needsReset2 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM2[3], "").equals("0"))
                        resetTime2 = fmtnow.parseDateTime(reset2);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime2 = fmtnow.parseDateTime(reset2);
                    }
                }
                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM3[3], "")) || needsReset3) {
                    needsReset3 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM3[3], "").equals("0"))
                        resetTime3 = fmtnow.parseDateTime(reset3);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime3 = fmtnow.parseDateTime(reset3);
                    }
                }
                boolean emptyDB = TrafficDatabase.isEmpty(mDatabaseHelper);
                if (emptyDB){
                    dataMap.put(Constants.SIM1RX, 0L);
                    dataMap.put(Constants.SIM2RX, 0L);
                    dataMap.put(Constants.SIM3RX, 0L);
                    dataMap.put(Constants.SIM1TX, 0L);
                    dataMap.put(Constants.SIM2TX, 0L);
                    dataMap.put(Constants.SIM3TX, 0L);
                    dataMap.put(Constants.TOTAL1, 0L);
                    dataMap.put(Constants.TOTAL2, 0L);
                    dataMap.put(Constants.TOTAL3, 0L);
                    dataMap.put(Constants.LAST_RX, 0L);
                    dataMap.put(Constants.LAST_TX, 0L);
                    dataMap.put(Constants.LAST_TIME, "");
                    dataMap.put(Constants.LAST_DATE, "");
                    dataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                } else if (isFirstRun) {
                    if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                            || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                            || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                        if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                            dataMap.put(Constants.SIM1RX, 0L);
                            dataMap.put(Constants.SIM1TX, 0L);
                            dataMap.put(Constants.TOTAL1, 0L);
                            rx = (long) dataMap.get(Constants.SIM2RX);
                            tx = (long) dataMap.get(Constants.SIM2TX);
                            tot = (long) dataMap.get(Constants.TOTAL2);
                            rcvd1 = trans1 = 0;
                            needsReset1 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                            dataMap.put(Constants.SIM2RX, 0L);
                            dataMap.put(Constants.SIM2TX, 0L);
                            dataMap.put(Constants.TOTAL2, 0L);
                            rx = tx = rcvd2 = trans2 = 0;
                            needsReset2 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                            dataMap.put(Constants.SIM3RX, 0L);
                            dataMap.put(Constants.SIM3TX, 0L);
                            dataMap.put(Constants.TOTAL3, 0L);
                            rx = (long) dataMap.get(Constants.SIM2RX);
                            tx = (long) dataMap.get(Constants.SIM2TX);
                            tot = (long) dataMap.get(Constants.TOTAL2);
                            rcvd3 = trans3 = 0;
                            needsReset3 = false;
                        }
                    } else {
                        dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
                        rx = (long) dataMap.get(Constants.SIM2RX);
                        tx = (long) dataMap.get(Constants.SIM2TX);
                        tot = (long) dataMap.get(Constants.TOTAL2);
                    }
                } else if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                        || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                        || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                    if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                        dataMap.put(Constants.SIM1RX, 0L);
                        dataMap.put(Constants.SIM1TX, 0L);
                        dataMap.put(Constants.TOTAL1, 0L);
                        rx = (long) dataMap.get(Constants.SIM2RX);
                        tx = (long) dataMap.get(Constants.SIM2TX);
                        tot = (long) dataMap.get(Constants.TOTAL2);
                        rcvd1 = trans1 = 0;
                        needsReset1 = false;
                    }
                    if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                        dataMap.put(Constants.SIM2RX, 0L);
                        dataMap.put(Constants.SIM2TX, 0L);
                        dataMap.put(Constants.TOTAL2, 0L);
                        rx = tx = rcvd2 = trans2 = 0;
                        needsReset2 = false;
                    }
                    if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                        dataMap.put(Constants.SIM3RX, 0L);
                        dataMap.put(Constants.SIM3TX, 0L);
                        dataMap.put(Constants.TOTAL3, 0L);
                        rx = (long) dataMap.get(Constants.SIM2RX);
                        tx = (long) dataMap.get(Constants.SIM2TX);
                        tot = (long) dataMap.get(Constants.TOTAL2);
                        rcvd3 = trans3 = 0;
                        needsReset3 = false;
                    }
                }else {
                    rx = (long) dataMap.get(Constants.SIM2RX);
                    tx = (long) dataMap.get(Constants.SIM2TX);
                    tot = (long) dataMap.get(Constants.TOTAL2);
                }

                String limit = prefs.getString(Constants.PREF_SIM2[1], "");
                String round = prefs.getString(Constants.PREF_SIM2[4], "0");

                int value;
                if (prefs.getString(Constants.PREF_SIM2[2], "").equals(""))
                    value = 0;
                else
                    value = Integer.valueOf(prefs.getString(Constants.PREF_SIM2[2], ""));

                float valuer;

                double lim = Double.MAX_VALUE;

                if (!limit.equals("")) {
                    valuer = 1 - Float.valueOf(round) / 100;
                    lim = valuer * DataFormat.getFormatLong(limit, value);
                }

                long diffrx = TrafficStats.getMobileRxBytes() - mStartRX2;
                long difftx = TrafficStats.getMobileTxBytes() - mStartTX2;
                mStartRX2 = TrafficStats.getMobileRxBytes();
                mStartTX2 = TrafficStats.getMobileTxBytes();
                if (((long) dataMap.get(Constants.TOTAL2) <= (long) lim) || continueOverLimit) {
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

                    dataMap.put(Constants.SIM2RX, rx);
                    dataMap.put(Constants.SIM2TX, tx);
                    dataMap.put(Constants.TOTAL2, tot);
                    dataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                    dataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());

                    Calendar myCalendar = Calendar.getInstance();
                    SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", getAppContext().getResources().getConfiguration().locale);
                    SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm:ss", getAppContext().getResources().getConfiguration().locale);
                    int choice = 0;
                    if ((diffrx > MB || difftx > MB) || new SimpleDateFormat("ss", getAppContext().getResources().getConfiguration().locale).format(myCalendar.getTime()).equals("59")
                            || emptyDB) {
                        String last = (String) TrafficDatabase.read_writeTrafficData(Constants.READ, new HashMap<String, Object>(), mDatabaseHelper).get(Constants.LAST_DATE);
                        DateTime dt_temp;
                        if (last.equals(""))
                            dt_temp = new org.joda.time.DateTime();
                        else
                            dt_temp = fmtdate.parseDateTime(last);
                        if (!DateCompare.isNextDayOrMonth(dt, "0")  && !emptyDB
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
                            TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                            break;
                        case 2:
                            TrafficDatabase.read_writeTrafficData(Constants.WRITE, dataMap, mDatabaseHelper);
                            continueOverLimit = false;
                            break;
                    }

                    if (CheckVisibility.isActivityVisible() ||
                            (AppWidgetManager.getInstance(getAppContext()).
                                    getAppWidgetIds(new ComponentName(getAppContext(), InfoWidget.class)).length != 0) &&
                                    isScreenOn(getAppContext())) {
                        Intent intent = new Intent(Constants.BROADCAST_ACTION);
                        intent.putExtra(Constants.SPEEDRX, speedRX);
                        intent.putExtra(Constants.SPEEDTX, speedTX);
                        intent.putExtra(Constants.SIM2RX, rx);
                        intent.putExtra(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                        intent.putExtra(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                        intent.putExtra(Constants.SIM2TX, tx);
                        intent.putExtra(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                        intent.putExtra(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                        intent.putExtra(Constants.TOTAL2, tot);
                        intent.putExtra(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                        intent.putExtra(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                        intent.putExtra(Constants.SIM_ACTIVE, activeSIM);
                        intent.putExtra(Constants.TIP, (isSIM1OverLimit || isSIM2OverLimit || isSIM3OverLimit) && !continueOverLimit);
                        intent.putExtra(Constants.OPERATOR1, getName(Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                        if (simNumber >= 2)
                            intent.putExtra(Constants.OPERATOR2, getName(Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                        if (simNumber == 3)
                            intent.putExtra(Constants.OPERATOR3, getName(Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                        getAppContext().sendBroadcast(intent);
                    }

                    String text = "";
                    if (simNumber == 1)
                        text = DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1));
                    else if (simNumber == 2)
                        text = DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                + DataFormat.formatData(getAppContext(), tot);
                    else if (simNumber == 3)
                        text = DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                + DataFormat.formatData(getAppContext(), tot) + "   ||   "
                                + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL3));

                    Bitmap bm = BitmapFactory.decodeResource(getAppContext().getResources(), R.mipmap.ic_launcher);
                    n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE)
                            .setPriority(priority)
                            .setLargeIcon(bm)
                            .setWhen(System.currentTimeMillis())
                            .setContentTitle(getAppContext().getResources().getString(R.string.notification_title))
                            .setContentText(text)
                            .build();
                    nm.notify(Constants.STARTED_ID, n);
                }
                isFirstRun = false;
            } else {
                Intent intent = new Intent(Constants.BROADCAST_ACTION);
                intent.putExtra(Constants.SPEEDRX, 0L);
                intent.putExtra(Constants.SPEEDTX, 0L);
                intent.putExtra(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                intent.putExtra(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                intent.putExtra(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                intent.putExtra(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                intent.putExtra(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                intent.putExtra(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                intent.putExtra(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                intent.putExtra(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                intent.putExtra(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                intent.putExtra(Constants.SIM_ACTIVE, MobileDataControl.getMobileDataInfo(getAppContext())[1]);
                intent.putExtra(Constants.OPERATOR1, getName(Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                if (simNumber >= 2)
                    intent.putExtra(Constants.OPERATOR2, getName(Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                if (simNumber == 3)
                    intent.putExtra(Constants.OPERATOR3, getName(Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                getAppContext().sendBroadcast(intent);
            }
        }
    }

    private static class CountTimerTask3 extends TimerTask {

        private static final long MB = 1024 * 1024;

        @Override
        public void run() {
            int[] data = {2, Constants.SIM3};
            if (Arrays.equals(MobileDataControl.getMobileDataInfo(getAppContext()), data) && !isTimerCancelled){

                long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
                lastUpdateTime = SystemClock.elapsedRealtime();

                long newTotalRxBytes = TrafficStats.getTotalRxBytes();
                long newTotalTxBytes = TrafficStats.getTotalTxBytes();

                long rxData = newTotalRxBytes - totalRxBytes;
                long txData = newTotalTxBytes - totalTxBytes;

                long speedRX = (long)(rxData / (timeDelta / 1000F));
                long speedTX = (long)(txData / (timeDelta / 1000F));

                totalRxBytes = newTotalRxBytes;
                totalTxBytes = newTotalTxBytes;

                long rx = 0;
                long tx = 0;
                long tot = 0;

                DateTimeFormatter fmtdate = DateTimeFormat.forPattern("yyyy-MM-dd");
                DateTimeFormatter fmtnow = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
                DateTime dt = fmtdate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                DateTime now = new DateTime();

                String reset1 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM1[9], "00:00");
                String reset2 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM2[9], "00:00");
                String reset3 = new DateTime().toString(fmtdate) + " " + prefs.getString(Constants.PREF_SIM3[9], "00:00");

                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM1[3], "")) || needsReset1) {
                    needsReset1 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM1[3], "").equals("0"))
                        resetTime1 = fmtnow.parseDateTime(reset1);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM1[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime1 = fmtnow.parseDateTime(reset1);
                    }
                }
                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM2[3], "")) || needsReset2) {
                    needsReset2 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM2[3], "").equals("0"))
                        resetTime2 = fmtnow.parseDateTime(reset2);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM2[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime2 = fmtnow.parseDateTime(reset2);
                    }
                }
                if (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM3[3], "")) || needsReset3) {
                    needsReset3 = true;
                    int day = 1;
                    if (prefs.getString(Constants.PREF_SIM3[3], "").equals("0"))
                        resetTime3 = fmtnow.parseDateTime(reset3);
                    else {
                        if (Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1")) >= 28)
                            switch (now.getMonthOfYear()) {
                                case 2:
                                    if (now.year().isLeap())
                                        day = 29;
                                    else
                                        day = 28;
                                    break;
                                case 4:
                                case 6:
                                case 9:
                                case 11:
                                    if (Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1")) == 31)
                                        day = 30;
                                    else
                                        day = Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1"));
                                    break;
                                default:
                                    day = Integer.parseInt(prefs.getString(Constants.PREF_SIM3[10], "1"));
                                    break;
                            }
                        if (day >= now.getDayOfMonth())
                            resetTime3 = fmtnow.parseDateTime(reset3);
                    }
                }
                boolean emptyDB = TrafficDatabase.isEmpty(mDatabaseHelper);
                if (emptyDB){
                    dataMap.put(Constants.SIM1RX, 0L);
                    dataMap.put(Constants.SIM2RX, 0L);
                    dataMap.put(Constants.SIM3RX, 0L);
                    dataMap.put(Constants.SIM1TX, 0L);
                    dataMap.put(Constants.SIM2TX, 0L);
                    dataMap.put(Constants.SIM3TX, 0L);
                    dataMap.put(Constants.TOTAL1, 0L);
                    dataMap.put(Constants.TOTAL2, 0L);
                    dataMap.put(Constants.TOTAL3, 0L);
                    dataMap.put(Constants.LAST_RX, 0L);
                    dataMap.put(Constants.LAST_TX, 0L);
                    dataMap.put(Constants.LAST_TIME, "");
                    dataMap.put(Constants.LAST_DATE, "");
                    dataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                } else if (isFirstRun) {
                    if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                            || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                            || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                        if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                            dataMap.put(Constants.SIM1RX, 0L);
                            dataMap.put(Constants.SIM1TX, 0L);
                            dataMap.put(Constants.TOTAL1, 0L);
                            rx = (long) dataMap.get(Constants.SIM3RX);
                            tx = (long) dataMap.get(Constants.SIM3TX);
                            tot = (long) dataMap.get(Constants.TOTAL3);
                            rcvd1 = trans1 = 0;
                            needsReset1 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                            dataMap.put(Constants.SIM2RX, 0L);
                            dataMap.put(Constants.SIM2TX, 0L);
                            dataMap.put(Constants.TOTAL2, 0L);
                            rx = (long) dataMap.get(Constants.SIM3RX);
                            tx = (long) dataMap.get(Constants.SIM3TX);
                            tot = (long) dataMap.get(Constants.TOTAL3);
                            rcvd2 = trans2 = 0;
                            needsReset2 = false;
                        }
                        if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                            dataMap.put(Constants.SIM3RX, 0L);
                            dataMap.put(Constants.SIM3TX, 0L);
                            dataMap.put(Constants.TOTAL3, 0L);
                            rx = tx = rcvd3 = trans3 = 0;
                            needsReset3 = false;
                        }
                    } else {
                        dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
                        rx = (long) dataMap.get(Constants.SIM3RX);
                        tx = (long) dataMap.get(Constants.SIM3TX);
                        tot = (long) dataMap.get(Constants.TOTAL3);
                    }
                } else if ((DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1)
                        || (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2)
                        || (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3)) {
                    if (DateTimeComparator.getInstance().compare(now, resetTime1) >= 0 && needsReset1) {
                        dataMap.put(Constants.SIM1RX, 0L);
                        dataMap.put(Constants.SIM1TX, 0L);
                        dataMap.put(Constants.TOTAL1, 0L);
                        rx = (long) dataMap.get(Constants.SIM3RX);
                        tx = (long) dataMap.get(Constants.SIM3TX);
                        tot = (long) dataMap.get(Constants.TOTAL3);
                        rcvd1 = trans1 = 0;
                        needsReset1 = false;
                    }
                    if (DateTimeComparator.getInstance().compare(now, resetTime2) >= 0 && needsReset2) {
                        dataMap.put(Constants.SIM2RX, 0L);
                        dataMap.put(Constants.SIM2TX, 0L);
                        dataMap.put(Constants.TOTAL2, 0L);
                        rx = (long) dataMap.get(Constants.SIM3RX);
                        tx = (long) dataMap.get(Constants.SIM3TX);
                        tot = (long) dataMap.get(Constants.TOTAL3);
                        rcvd2 = trans2 = 0;
                        needsReset2 = false;
                    }
                    if (DateTimeComparator.getInstance().compare(now, resetTime3) >= 0 && needsReset3) {
                        dataMap.put(Constants.SIM3RX, 0L);
                        dataMap.put(Constants.SIM3TX, 0L);
                        dataMap.put(Constants.TOTAL3, 0L);
                        rx = tx = rcvd3 = trans3 = 0;
                        needsReset3 = false;
                    }
                } else {
                    rx = (long) dataMap.get(Constants.SIM3RX);
                    tx = (long) dataMap.get(Constants.SIM3TX);
                    tot = (long) dataMap.get(Constants.TOTAL3);
                }

                String limit = prefs.getString(Constants.PREF_SIM3[1], "");
                String round = prefs.getString(Constants.PREF_SIM3[4], "0");

                int value;
                if (prefs.getString(Constants.PREF_SIM3[2], "").equals(""))
                    value = 0;
                else
                    value = Integer.valueOf(prefs.getString(Constants.PREF_SIM3[2], ""));

                float valuer;

                double lim = Double.MAX_VALUE;

                if (!limit.equals("")) {
                    valuer = 1 - Float.valueOf(round) / 100;
                    lim = valuer * DataFormat.getFormatLong(limit, value);
                }

                long diffrx = TrafficStats.getMobileRxBytes() - mStartRX3;
                long difftx = TrafficStats.getMobileTxBytes() - mStartTX3;
                mStartRX3 = TrafficStats.getMobileRxBytes();
                mStartTX3 = TrafficStats.getMobileTxBytes();
                if (((long) dataMap.get(Constants.TOTAL3) <= (long) lim) || continueOverLimit) {
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

                    dataMap.put(Constants.SIM3RX, rx);
                    dataMap.put(Constants.SIM3TX, tx);
                    dataMap.put(Constants.TOTAL3, tot);
                    dataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                    dataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());

                    Calendar myCalendar = Calendar.getInstance();
                    SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", getAppContext().getResources().getConfiguration().locale);
                    SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm:ss", getAppContext().getResources().getConfiguration().locale);
                    int choice = 0;
                    if ((diffrx > MB || difftx > MB) || new SimpleDateFormat("ss", getAppContext().getResources().getConfiguration().locale).format(myCalendar.getTime()).equals("59")
                            || emptyDB) {
                        String last = (String) TrafficDatabase.read_writeTrafficData(Constants.READ, new HashMap<String, Object>(), mDatabaseHelper).get(Constants.LAST_DATE);
                        DateTime dt_temp;
                        if (last.equals(""))
                            dt_temp = new org.joda.time.DateTime();
                        else
                            dt_temp = fmtdate.parseDateTime(last);
                        if (!DateCompare.isNextDayOrMonth(dt, "0")  && !emptyDB
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
                            TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                            break;
                        case 2:
                            TrafficDatabase.read_writeTrafficData(Constants.WRITE, dataMap, mDatabaseHelper);
                            continueOverLimit = false;
                            break;
                    }

                    if (CheckVisibility.isActivityVisible() ||
                            (AppWidgetManager.getInstance(getAppContext()).
                                    getAppWidgetIds(new ComponentName(getAppContext(), InfoWidget.class)).length != 0) &&
                                    isScreenOn(getAppContext())) {
                        Intent intent = new Intent(Constants.BROADCAST_ACTION);
                        intent.putExtra(Constants.SPEEDRX, speedRX);
                        intent.putExtra(Constants.SPEEDTX, speedTX);
                        intent.putExtra(Constants.SIM3RX, rx);
                        intent.putExtra(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                        intent.putExtra(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                        intent.putExtra(Constants.SIM3TX, tx);
                        intent.putExtra(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                        intent.putExtra(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                        intent.putExtra(Constants.TOTAL3, tot);
                        intent.putExtra(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                        intent.putExtra(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                        intent.putExtra(Constants.SIM_ACTIVE, activeSIM);
                        intent.putExtra(Constants.TIP, (isSIM1OverLimit || isSIM2OverLimit || isSIM3OverLimit) && !continueOverLimit);
                        intent.putExtra(Constants.OPERATOR1, getName(Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                        if (simNumber >= 2)
                            intent.putExtra(Constants.OPERATOR2, getName(Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                        if (simNumber == 3)
                            intent.putExtra(Constants.OPERATOR3, getName(Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                        getAppContext().sendBroadcast(intent);
                    }

                    String text = "";
                    if (simNumber == 1)
                        text = DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1));
                    else if (simNumber == 2)
                        text = DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL2));
                    else if (simNumber == 3)
                        text = DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                + DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                                + DataFormat.formatData(getAppContext(), tot);

                    Bitmap bm = BitmapFactory.decodeResource(getAppContext().getResources(), R.mipmap.ic_launcher);
                    n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE)
                            .setPriority(priority)
                            .setLargeIcon(bm)
                            .setWhen(System.currentTimeMillis())
                            .setContentTitle(getAppContext().getResources().getString(R.string.notification_title))
                            .setContentText(text)
                            .build();
                    nm.notify(Constants.STARTED_ID, n);
                }
                isFirstRun = false;
            } else {
                Intent intent = new Intent(Constants.BROADCAST_ACTION);
                intent.putExtra(Constants.SPEEDRX, 0L);
                intent.putExtra(Constants.SPEEDTX, 0L);
                intent.putExtra(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                intent.putExtra(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                intent.putExtra(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                intent.putExtra(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                intent.putExtra(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                intent.putExtra(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                intent.putExtra(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                intent.putExtra(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                intent.putExtra(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                intent.putExtra(Constants.SIM_ACTIVE, MobileDataControl.getMobileDataInfo(getAppContext())[1]);
                intent.putExtra(Constants.OPERATOR1, getName(Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
                if (simNumber >= 2)
                    intent.putExtra(Constants.OPERATOR2, getName(Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
                if (simNumber == 3)
                    intent.putExtra(Constants.OPERATOR3, getName(Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
                getAppContext().sendBroadcast(intent);
            }
        }
    }

    private static void startCheck(int alertID) {

        if (prefs.getBoolean(Constants.PREF_OTHER[3], false))
            alertNotify(alertID);

        if (((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true)))) {

            isTimerCancelled = true;
            mTimer.cancel();
            mTimer.purge();

            try {
                MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Bitmap bm = BitmapFactory.decodeResource(getAppContext().getResources(), R.drawable.ic_disable);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getAppContext());
            NotificationManager nm = (NotificationManager) getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
            Notification n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(priority)
                    .setLargeIcon(bm)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getAppContext().getResources().getString(R.string.service_stopped_title))
                    .build();
            nm.notify(Constants.STARTED_ID, n);
        }

        if (!prefs.getBoolean(Constants.PREF_SIM1[7], true) || !prefs.getBoolean(Constants.PREF_SIM2[7], true)
                || !prefs.getBoolean(Constants.PREF_SIM3[7], true) || !prefs.getBoolean(Constants.PREF_OTHER[10], true)) {
                Intent dialogIntent = new Intent(getAppContext(), ChooseAction.class);
                dialogIntent.putExtra(Constants.SIM_ACTIVE, alertID);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getAppContext().startActivity(dialogIntent);
        } else if (((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true))) &&
                prefs.getBoolean(Constants.PREF_OTHER[10], true)) {
            try {
                if (!isSIM2OverLimit && alertID == Constants.SIM1) {
                    MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM2);
                    timerStart(Constants.COUNT);
                } else if (!isSIM3OverLimit && alertID == Constants.SIM1) {
                    MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM3);
                    timerStart(Constants.COUNT);
                } else if (!isSIM1OverLimit && alertID == Constants.SIM2) {
                    MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM1);
                    timerStart(Constants.COUNT);
                } else if (!isSIM3OverLimit && alertID == Constants.SIM2) {
                    MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM3);
                    timerStart(Constants.COUNT);
                } else if (!isSIM1OverLimit && alertID == Constants.SIM3) {
                    MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM1);
                    timerStart(Constants.COUNT);
                } else if (!isSIM2OverLimit && alertID == Constants.SIM3) {
                    MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                    MobileDataControl.toggleMobileDataConnection(true, getAppContext(), Constants.SIM2);
                    timerStart(Constants.COUNT);
                } else {
                    Intent intent = new Intent(Constants.TIP);
                    getAppContext().sendBroadcast(intent);
                    timerStart(Constants.CHECK);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true))) {
            Intent dialogIntent = new Intent(getAppContext(), ChooseAction.class);
            dialogIntent.putExtra(Constants.SIM_ACTIVE, alertID);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getAppContext().startActivity(dialogIntent);
        } else if (isSIM1OverLimit && isSIM2OverLimit && isSIM3OverLimit && prefs.getBoolean(Constants.PREF_OTHER[10], true)){
            Intent dialogIntent = new Intent(getAppContext(), ChooseAction.class);
            dialogIntent.putExtra(Constants.SIM_ACTIVE, alertID);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getAppContext().startActivity(dialogIntent);
        } else if (isSIM1OverLimit && isSIM2OverLimit && isSIM2OverLimit) {
            isTimerCancelled = true;
            mTimer.cancel();
            mTimer.purge();
            try {
                MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Bitmap bm = BitmapFactory.decodeResource(getAppContext().getResources(), R.drawable.ic_disable);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getAppContext());
            NotificationManager nm = (NotificationManager) getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
            Notification n = builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.ic_launcher_small)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(priority)
                    .setLargeIcon(bm)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getAppContext().getResources().getString(R.string.service_stopped_title))
                    .build();
            nm.notify(Constants.STARTED_ID, n);

            timerStart(Constants.CHECK);
        }
    }

    private static void alertNotify(int alertID) {
        Intent notificationIntent;
        if ((prefs.getBoolean(Constants.PREF_SIM1[7], true) && isSIM1OverLimit) ||
                (prefs.getBoolean(Constants.PREF_SIM2[7], true) && isSIM2OverLimit) ||
                (prefs.getBoolean(Constants.PREF_SIM2[7], true) && isSIM3OverLimit))
            notificationIntent = new Intent(getAppContext(), MainActivity.class);
        else {
            final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
            notificationIntent = new Intent(Intent.ACTION_MAIN);
            notificationIntent.setComponent(cn);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent pIntent = PendingIntent.getActivity(getAppContext(), 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationManager nm = (NotificationManager) getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getAppContext());
        if (prefs.getBoolean(Constants.PREF_OTHER[3], false) && prefs.getBoolean(Constants.PREF_OTHER[2], false))
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
        Bitmap bm = BitmapFactory.decodeResource(getAppContext().getResources(), R.drawable.ic_alert);
        String opName = "";
        if (alertID == Constants.SIM1)
            opName = getName(Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        else if (alertID == Constants.SIM2)
            opName = getName(Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        else
            opName = getName(Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        String txt;
        if ((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true)))
            txt = getAppContext().getResources().getString(R.string.data_dis);
        else
            txt = getAppContext().getResources().getString(R.string.data_dis_tip);

        Notification n = builder.setContentIntent(pIntent).setSmallIcon(R.drawable.ic_launcher_small)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLargeIcon(bm)
                .setTicker(getAppContext().getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(txt)
                .setContentText(opName + ": " + getAppContext().getResources().getString(R.string.over_limit))
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
        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(clear1Receiver);
        unregisterReceiver(clear2Receiver);
        unregisterReceiver(clear3Receiver);
        //unregisterReceiver(simChange);
        unregisterReceiver(setUsage);
        unregisterReceiver(actionReceive);
        unregisterReceiver(connReceiver);
    }
    
    private static boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
            return pm.isInteractive();
        else
            return pm.isScreenOn();
    }
}
