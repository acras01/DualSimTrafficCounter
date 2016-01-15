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
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.support.v4.app.NotificationCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stericson.RootTools.RootTools;

import org.acra.ACRA;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.text.SimpleDateFormat;
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
import ua.od.acros.dualsimtrafficcounter.utils.MTKUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;
import ua.od.acros.dualsimtrafficcounter.widget.InfoWidget;


public class CountService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static Context context;
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
    private DateTimeFormatter fmtDate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
    private DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
    private DateTime resetTime1;
    private DateTime resetTime2;
    private DateTime resetTime3;
    private ContentValues dataMap;
    private BroadcastReceiver clear1Receiver, clear2Receiver, clear3Receiver, connReceiver, setUsage, actionReceive;
    private TrafficDatabase mDatabaseHelper;
    private Timer mTimer = null;
    private SharedPreferences prefs;
    private PendingIntent contentIntent;
    private NotificationManager nm;
    private NotificationCompat.Builder builder;
    private Bitmap bLarge;
    private Target target;
    private int idSmall;
    private boolean resetRuleChanged;
    private String[] operatorNames = new String[3];
    private static boolean actionChoosed = false;


    public CountService() {
    }

    public static void setActionChoosed(boolean actionChoosed) {
        CountService.actionChoosed = actionChoosed;
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

        context = CountService.this;

        prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mPriority = prefs.getBoolean(Constants.PREF_OTHER[12], true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN;

        mDatabaseHelper = new TrafficDatabase(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
        dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
        if (dataMap.get(Constants.LAST_DATE).equals("")) {
            Calendar myCalendar = Calendar.getInstance();
            SimpleDateFormat formatDate = new SimpleDateFormat(Constants.DATE_FORMAT, getResources().getConfiguration().locale);
            SimpleDateFormat formatTime = new SimpleDateFormat(Constants.TIME_FORMAT + ":ss", getResources().getConfiguration().locale);
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
                    if (MobileUtils.getMobileDataInfo(context, false)[0] == 2)
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
                        case Constants.CHANGE_ACTION:
                            if (!isSIM2OverLimit && simid == Constants.SIM1) {
                                MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM2);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM3OverLimit && simid == Constants.SIM1) {
                                MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM3);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM1OverLimit && simid == Constants.SIM2) {
                                MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM1);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM3OverLimit && simid == Constants.SIM2) {
                                MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM3);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM1OverLimit && simid == Constants.SIM3) {
                                MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM1);
                                timerStart(Constants.COUNT);
                            } else if (!isSIM2OverLimit && simid == Constants.SIM3) {
                                MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM2);
                                timerStart(Constants.COUNT);
                            } else
                                timerStart(Constants.CHECK);
                            break;
                        case Constants.SETTINGS_ACTION:
                            final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
                            Intent settIntent = new Intent(Intent.ACTION_MAIN);
                            settIntent.setComponent(cn);
                            settIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(settIntent);
                            break;
                        case Constants.LIMIT_ACTION:
                            Intent i = new Intent(context, SettingsActivity.class);
                            i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, LimitFragment.class.getName());
                            i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            i.putExtra(Constants.SIM_ACTIVE, simid);
                            startActivity(i);
                            timerStart(Constants.CHECK);
                            break;
                        case Constants.CONTINUE_ACTION:
                            if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && RootTools.isAccessGiven()) ||
                                    (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MTKUtils.isMtkDevice()))
                                MobileUtils.toggleMobileDataConnection(true, context, simid);
                            continueOverLimit = true;
                            if (isTimerCancelled)
                                timerStart(Constants.COUNT);
                            break;
                        case Constants.OFF_ACTION:
                            if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && RootTools.isAccessGiven()) ||
                                    (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MTKUtils.isMtkDevice()))
                                timerStart(Constants.CHECK);
                            else
                                continueOverLimit = true;
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
                builder = new NotificationCompat.Builder(context).setContentIntent(contentIntent)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setPriority(mPriority)
                        .setContentText(DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                                + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                                + DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3)));
                nm.notify(Constants.STARTED_ID, builder.build());
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

        Intent notificationIntent = new Intent(context, MainActivity.class);
        contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                bLarge = bitmap;
            }

            @Override
            public void onBitmapFailed(Drawable drawable) {
            }

            @Override
            public void onPrepareLoad(Drawable drawable) {
            }
        };
        Picasso.with(context).load(R.mipmap.ic_launcher).into(target);
        builder = new NotificationCompat.Builder(context).setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(mPriority)
                .setSmallIcon(R.drawable.ic_launcher_small)
                .setLargeIcon(bLarge)
                .setTicker(getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1)) + "   ||   "
                        + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2)) + "   ||   "
                        + DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3)));
        startForeground(Constants.STARTED_ID, builder.build());
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
        actionChoosed = false;
        simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        activeSIM = MobileUtils.getMobileDataInfo(context, true)[1];
        operatorNames[0] = MobileUtils.getName(context, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        operatorNames[1] = MobileUtils.getName(context, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        operatorNames[2] = MobileUtils.getName(context, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);

        if (prefs.getBoolean(Constants.PREF_OTHER[15], false)) {
            String[] pref = new String[25];
            switch (activeSIM) {
                case Constants.SIM1:
                    pref = Constants.PREF_SIM1;
                    break;
                case Constants.SIM2:
                    pref = Constants.PREF_SIM2;
                    break;
                case Constants.SIM3:
                    pref = Constants.PREF_SIM3;
                    break;
            }
            if (prefs.getString(pref[23], "none").equals("auto"))
                idSmall = getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(context, activeSIM), "drawable", context.getPackageName());
            else
                idSmall = getResources().getIdentifier(prefs.getString(pref[23], "logo_none"), "drawable", context.getPackageName());
        } else
            idSmall = R.drawable.ic_launcher_small;

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
        if ((key.equals(Constants.PREF_SIM1[1]) || key.equals(Constants.PREF_SIM1[2])) && activeSIM == Constants.SIM1 && continueOverLimit) {
            continueOverLimit = false;
            actionChoosed = false;
        }
        if ((key.equals(Constants.PREF_SIM2[1]) || key.equals(Constants.PREF_SIM2[2])) && activeSIM == Constants.SIM2 && continueOverLimit) {
            continueOverLimit = false;
            actionChoosed = false;
        }
        if ((key.equals(Constants.PREF_SIM3[1]) || key.equals(Constants.PREF_SIM3[2])) && activeSIM == Constants.SIM3 && continueOverLimit) {
            continueOverLimit = false;
            actionChoosed = false;
        }
        if (key.equals(Constants.PREF_OTHER[15]))
            if (sharedPreferences.getBoolean(key, false)) {
                String[] pref = new String[24];
                switch (activeSIM) {
                    case Constants.SIM1:
                        pref = Constants.PREF_SIM1;
                        break;
                    case Constants.SIM2:
                        pref = Constants.PREF_SIM2;
                        break;
                    case Constants.SIM3:
                        pref = Constants.PREF_SIM3;
                        break;
                }
                if (prefs.getString(pref[23], "none").equals("auto"))
                    idSmall = getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(context, activeSIM), "drawable", context.getPackageName());
                else
                    idSmall = getResources().getIdentifier(prefs.getString(pref[23], "none"), "drawable", context.getPackageName());
            } else
                idSmall = R.drawable.ic_launcher_small;
        if (sharedPreferences.getBoolean(Constants.PREF_OTHER[15], false)) {
            if (key.equals(Constants.PREF_SIM1[23]) || key.equals(Constants.PREF_SIM2[23]) ||
                    key.equals(Constants.PREF_SIM3[23]))
                if (prefs.getString(key, "none").equals("auto"))
                    idSmall = getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(context, activeSIM), "drawable", context.getPackageName());
                else
                    idSmall = getResources().getIdentifier(prefs.getString(key, "none"), "drawable", context.getPackageName());
        }
        if (key.equals(Constants.PREF_SIM1[3]) || key.equals(Constants.PREF_SIM1[9]) || key.equals(Constants.PREF_SIM1[10]) ||
                key.equals(Constants.PREF_SIM2[3]) || key.equals(Constants.PREF_SIM2[9]) || key.equals(Constants.PREF_SIM2[10]) ||
                key.equals(Constants.PREF_SIM3[3]) || key.equals(Constants.PREF_SIM3[9]) || key.equals(Constants.PREF_SIM3[10]))
            resetRuleChanged = true;
        if (key.equals(Constants.PREF_SIM1[5]))
            operatorNames[0] = MobileUtils.getName(context, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        if (key.equals(Constants.PREF_SIM2[5]))
            operatorNames[1] = MobileUtils.getName(context, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        if (key.equals(Constants.PREF_SIM3[5]))
            operatorNames[2] = MobileUtils.getName(context, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
    }

    private class CheckTimerTask extends TimerTask {

        @Override
        public void run() {

            context.sendBroadcast(new Intent(Constants.TIP));

            DateTime dt = fmtDate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));

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
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM1);
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;
                    timerStart(Constants.COUNT);
                }
                if (isSIM2OverLimit && (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM2[3], ""))
                        || (tot2 <= (long) lim2 && (prefs.getBoolean(Constants.PREF_SIM2[8], false)
                        || (!prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !prefs.getBoolean(Constants.PREF_SIM2[8], false) && !prefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM2);
                    mTimer.cancel();
                    mTimer.purge();
                    isTimerCancelled = true;
                    timerStart(Constants.COUNT);
                }
                if (isSIM3OverLimit && (DateCompare.isNextDayOrMonth(dt, prefs.getString(Constants.PREF_SIM3[3], ""))
                        || (tot3 <= (long) lim3 && (prefs.getBoolean(Constants.PREF_SIM3[8], false)
                        || (!prefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !prefs.getBoolean(Constants.PREF_SIM2[8], false) && !prefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM3);
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

    private DateTime getResetTime(int simid) {
        DateTime now = new DateTime();
        String[] pref = new String[25];
        int delta = 0;
        String period = "";
        switch (simid) {
            case Constants.SIM1:
                pref = Constants.PREF_SIM1;
                period = Constants.PERIOD1;
                break;
            case Constants.SIM2:
                pref = Constants.PREF_SIM2;
                period = Constants.PERIOD2;
                break;
            case Constants.SIM3:
                pref = Constants.PREF_SIM3;
                period = Constants.PERIOD3;
                break;
        }
        DateTime last;
        String date = prefs.getString(pref[24], "");
        if (!date.equals(""))
            last = fmtDateTime.parseDateTime(date);
        else
            last = fmtDateTime.parseDateTime("1970-01-01 00:00");
        switch (prefs.getString(pref[3], "")) {
            case "0":
                delta = 1;
                break;
            case "1":
                delta = Integer.parseInt(prefs.getString(pref[10], "1"));
                if (delta >= 28)
                    switch (now.getMonthOfYear()) {
                        case 2:
                            if (now.year().isLeap())
                                delta = 29;
                            else
                                delta = 28;
                            break;
                        case 4:
                        case 6:
                        case 9:
                        case 11:
                            if (delta == 31)
                                delta = 30;
                            break;
                    }
                break;
            case "2":
                delta = Integer.parseInt(prefs.getString(pref[10], "1"));
                break;
        }
        int diff = Days.daysBetween(last.toLocalDate(), now.toLocalDate()).getDays();
        if (prefs.getString(pref[3], "").equals("1")) {
            int month= now.getMonthOfYear();
            int daysInMonth = 31;
            switch (last.getMonthOfYear()) {
                case 2:
                    if (last.year().isLeap())
                        daysInMonth = 29;
                    else
                        daysInMonth = 28;
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    daysInMonth = 30;
                    break;
            }
            if (now.getDayOfMonth() > delta && diff < daysInMonth)
                month += 1;
            date = now.getYear() + "-" + month + "-" + delta;
            return fmtDateTime.parseDateTime(date + " " + prefs.getString(pref[9], "00:00"));
        } else {
            if (prefs.getString(pref[3], "").equals("2"))
                dataMap.put(period, diff);
            if (diff >= delta) {
                if (prefs.getString(pref[3], "").equals("2"))
                    dataMap.put(period, 0);
                return fmtDateTime.parseDateTime(now.toString(fmtDate) + " " + prefs.getString(pref[9], "00:00"));
            } else
                return null;
        }
    }

    private class CountTimerTask1 extends TimerTask {

        @Override
        public void run() {
            try {
                if (MobileUtils.getMobileDataInfo(context, false)[0] == 2 && !isTimerCancelled) {

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

                    DateTime dt = fmtDate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                    DateTime now = new DateTime();

                    if (prefs.getBoolean(Constants.PREF_SIM1[17], false)) {
                        String timeON = now.toString(fmtDate) + " " + prefs.getString(Constants.PREF_SIM1[20], "23:00");
                        String timeOFF = now.toString(fmtDate) + " " + prefs.getString(Constants.PREF_SIM1[21], "06:00");
                        isNight1 = DateTimeComparator.getInstance().compare(now, fmtDateTime.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, fmtDateTime.parseDateTime(timeOFF)) <= 0;
                    } else
                        isNight1 = false;

                    if (DateTimeComparator.getDateOnlyInstance().compare(now, dt) > 0 || resetRuleChanged) {
                        resetTime1 = getResetTime(Constants.SIM1);
                        if (resetTime1 != null)
                            needsReset1 = true;
                        resetTime2 = getResetTime(Constants.SIM2);
                        if (resetTime2 != null)
                            needsReset2 = true;
                        resetTime3 = getResetTime(Constants.SIM3);
                        if (resetTime3 != null)
                            needsReset3 = true;
                        resetRuleChanged = false;
                    }

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
                            rx = tx = mReceived1 = mTransmitted1 = 0;
                            prefs.edit().putString(Constants.PREF_SIM1[24], resetTime1.toString(fmtDateTime)).apply();
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
                            prefs.edit().putString(Constants.PREF_SIM2[24], resetTime2.toString(fmtDateTime)).apply();
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
                            prefs.edit().putString(Constants.PREF_SIM3[24], resetTime3.toString(fmtDateTime)).apply();
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
                    } else if (!actionChoosed) {
                        isSIM1OverLimit = true;
                        if (prefs.getBoolean(Constants.PREF_OTHER[3], false))
                            alertNotify(activeSIM);
                        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && RootTools.isAccessGiven()) ||
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MTKUtils.isMtkDevice()))
                            startCheck(activeSIM);
                        else if (!ChooseAction.isShown()) {
                                Intent dialogIntent = new Intent(context, ChooseAction.class);
                                dialogIntent.putExtra(Constants.SIM_ACTIVE, activeSIM);
                                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(dialogIntent);
                            }
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
                        writeToDataBase(diffrx, difftx, emptyDB, dt);
                        pushNotification(Constants.SIM1);
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds(context).length != 0) && isScreenOn(context))
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
                if (MobileUtils.getMobileDataInfo(context, false)[0] == 2 && !isTimerCancelled) {

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

                    DateTime dt = fmtDate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                    DateTime now = new DateTime();

                    if (prefs.getBoolean(Constants.PREF_SIM2[17], false)) {
                        String timeON = now.toString(fmtDate) + " " + prefs.getString(Constants.PREF_SIM2[20], "23:00");
                        String timeOFF = now.toString(fmtDate) + " " + prefs.getString(Constants.PREF_SIM2[21], "06:00");
                        isNight2 = DateTimeComparator.getInstance().compare(now, fmtDateTime.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, fmtDateTime.parseDateTime(timeOFF)) <= 0;
                    } else
                        isNight2 = false;

                    if (DateTimeComparator.getDateOnlyInstance().compare(now, dt) > 0 || resetRuleChanged) {
                        resetTime1 = getResetTime(Constants.SIM1);
                        if (resetTime1 != null)
                            needsReset1 = true;
                        resetTime2 = getResetTime(Constants.SIM2);
                        if (resetTime2 != null)
                            needsReset2 = true;
                        resetTime3 = getResetTime(Constants.SIM3);
                        if (resetTime3 != null)
                            needsReset3 = true;
                        resetRuleChanged = false;
                    }

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
                            prefs.edit().putString(Constants.PREF_SIM1[24], resetTime1.toString(fmtDateTime)).apply();
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
                            prefs.edit().putString(Constants.PREF_SIM2[24], resetTime2.toString(fmtDateTime)).apply();
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
                            prefs.edit().putString(Constants.PREF_SIM3[24], resetTime3.toString(fmtDateTime)).apply();
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
                    } else if (!actionChoosed) {
                        isSIM2OverLimit = true;
                        if (prefs.getBoolean(Constants.PREF_OTHER[3], false))
                            alertNotify(activeSIM);
                        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && RootTools.isAccessGiven()) ||
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MTKUtils.isMtkDevice()))
                            startCheck(activeSIM);
                        else if (!ChooseAction.isShown()) {
                                Intent dialogIntent = new Intent(context, ChooseAction.class);
                                dialogIntent.putExtra(Constants.SIM_ACTIVE, activeSIM);
                                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(dialogIntent);
                            }
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
                        writeToDataBase(diffrx, difftx, emptyDB, dt);
                        pushNotification(Constants.SIM2);
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds(context).length != 0) && isScreenOn(context))
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
                if (MobileUtils.getMobileDataInfo(context, false)[0] == 2 && !isTimerCancelled) {

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

                    DateTime dt = fmtDate.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
                    DateTime now = new DateTime();

                    if (prefs.getBoolean(Constants.PREF_SIM3[17], false)) {
                        String timeON = now.toString(fmtDate) + " " + prefs.getString(Constants.PREF_SIM3[20], "23:00");
                        String timeOFF = now.toString(fmtDate) + " " + prefs.getString(Constants.PREF_SIM3[21], "06:00");
                        isNight3 = DateTimeComparator.getInstance().compare(now, fmtDateTime.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, fmtDateTime.parseDateTime(timeOFF)) <= 0;
                    } else
                        isNight3 = false;

                    if (DateTimeComparator.getDateOnlyInstance().compare(now, dt) > 0 || resetRuleChanged) {
                        resetTime1 = getResetTime(Constants.SIM1);
                        if (resetTime1 != null)
                            needsReset1 = true;
                        resetTime2 = getResetTime(Constants.SIM2);
                        if (resetTime2 != null)
                            needsReset2 = true;
                        resetTime3 = getResetTime(Constants.SIM3);
                        if (resetTime3 != null)
                            needsReset3 = true;
                        resetRuleChanged = false;
                    }

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
                            prefs.edit().putString(Constants.PREF_SIM1[24], resetTime1.toString(fmtDateTime)).apply();
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
                            prefs.edit().putString(Constants.PREF_SIM2[24], resetTime2.toString(fmtDateTime)).apply();
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
                            prefs.edit().putString(Constants.PREF_SIM3[24], resetTime3.toString(fmtDateTime)).apply();
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
                    } else if (!actionChoosed) {
                        isSIM3OverLimit = true;
                        if (prefs.getBoolean(Constants.PREF_OTHER[3], false))
                            alertNotify(activeSIM);
                        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && RootTools.isAccessGiven()) ||
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MTKUtils.isMtkDevice()))
                            startCheck(activeSIM);
                        else if (!ChooseAction.isShown()) {
                                Intent dialogIntent = new Intent(context, ChooseAction.class);
                                dialogIntent.putExtra(Constants.SIM_ACTIVE, activeSIM);
                                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(dialogIntent);
                            }
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
                        writeToDataBase(diffrx, difftx, emptyDB, dt);
                        pushNotification(Constants.SIM3);
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds(context).length != 0) && isScreenOn(context))
                        sendDataBroadcast(speedRX, speedTX);
                }

            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private void writeToDataBase(long diffrx, long difftx, boolean emptyDB, DateTime dt) {

        Calendar myCalendar = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat(Constants.DATE_FORMAT, getResources().getConfiguration().locale);
        SimpleDateFormat formatTime = new SimpleDateFormat(Constants.TIME_FORMAT + ":ss", getResources().getConfiguration().locale);
        int choice = 0;
        long MB = 1024 * 1024;
        if ((diffrx + difftx > MB) || new SimpleDateFormat("ss", getResources().getConfiguration().locale).format(myCalendar.getTime()).equals("59")
                || emptyDB) {
            String last = (String) TrafficDatabase.readTrafficData(mDatabaseHelper).get(Constants.LAST_DATE);
            DateTime dt_temp;
            if (last.equals(""))
                dt_temp = new org.joda.time.DateTime();
            else
                dt_temp = fmtDate.parseDateTime(last);
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
    }

    private void sendDataBroadcast(long speedRX, long speedTX) {
        Intent intent = new Intent(Constants.BROADCAST_ACTION);
        intent.putExtra(Constants.WIDGET_IDS, getWidgetIds(context));
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
        intent.putExtra(Constants.OPERATOR1, operatorNames[0]);
        if (simNumber >= 2)
            intent.putExtra(Constants.OPERATOR2, operatorNames[1]);
        if (simNumber == 3)
            intent.putExtra(Constants.OPERATOR3, operatorNames[2]);
        context.sendBroadcast(intent);
    }

    private void pushNotification(int sim) {
        String text = "";
        if (prefs.getBoolean(Constants.PREF_OTHER[16], true)) {
            text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1));
            if (simNumber >= 2)
                text += "  ||  " + DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2));
            if (simNumber == 3)
                text += "  ||  " + DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3));
        } else {
            switch (sim) {
                case Constants.SIM1:
                    if (prefs.getBoolean(Constants.PREF_OTHER[15], false))
                        text = DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1));
                    else
                        text = operatorNames[0] + ": " +
                                DataFormat.formatData(context, isNight1 ? (long) dataMap.get(Constants.TOTAL1_N) : (long) dataMap.get(Constants.TOTAL1));
                    break;
                case Constants.SIM2:
                    if (prefs.getBoolean(Constants.PREF_OTHER[15], false))
                        text = DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2));
                    else
                        text = operatorNames[1] + ": " +
                                DataFormat.formatData(context, isNight2 ? (long) dataMap.get(Constants.TOTAL2_N) : (long) dataMap.get(Constants.TOTAL2));
                    break;
                case Constants.SIM3:
                    if (prefs.getBoolean(Constants.PREF_OTHER[15], false))
                        text = DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3));
                    else
                        text = operatorNames[2] + ": " +
                                DataFormat.formatData(context, isNight3 ? (long) dataMap.get(Constants.TOTAL3_N) : (long) dataMap.get(Constants.TOTAL3));
                    break;
            }
        }

        builder = new NotificationCompat.Builder(context).setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(mPriority)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(idSmall)
                .setLargeIcon(bLarge)
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(text);
        nm.notify(Constants.STARTED_ID, builder.build());
    }

    private void startCheck(int alertID) {
        try {
            MobileUtils.toggleMobileDataConnection(false, context, Constants.DISABLED);
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

            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_disable);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification n = builder.setContentIntent(contentIntent)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(mPriority)
                    .setLargeIcon(bm)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getResources().getString(R.string.service_stopped_title))
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
                if (!isSIM2OverLimit && alertID == Constants.SIM1 && simNumber >= 2) {
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM2);
                    timerStart(Constants.COUNT);
                } else if (!isSIM3OverLimit && alertID == Constants.SIM1 && simNumber == 3) {
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM3);
                    timerStart(Constants.COUNT);
                } else if (!isSIM1OverLimit && alertID == Constants.SIM2) {
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM1);
                    timerStart(Constants.COUNT);
                } else if (!isSIM3OverLimit && alertID == Constants.SIM2 && simNumber == 3) {
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM3);
                    timerStart(Constants.COUNT);
                } else if (!isSIM1OverLimit && alertID == Constants.SIM3) {
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM1);
                    timerStart(Constants.COUNT);
                } else if (!isSIM2OverLimit && alertID == Constants.SIM3) {
                    MobileUtils.toggleMobileDataConnection(true, context, Constants.SIM2);
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
        } else if (isSIM1OverLimit && isSIM2OverLimit && isSIM3OverLimit && prefs.getBoolean(Constants.PREF_OTHER[10], true)) {
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
                MobileUtils.toggleMobileDataConnection(false, context, Constants.DISABLED);
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }

            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_disable);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification n = builder.setContentIntent(contentIntent)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(mPriority)
                    .setLargeIcon(bm)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getResources().getString(R.string.service_stopped_title))
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
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alert);
        String opName;
        if (alertID == Constants.SIM1)
            opName = MobileUtils.getName(context, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        else if (alertID == Constants.SIM2)
            opName = MobileUtils.getName(context, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        else
            opName = MobileUtils.getName(context, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        String txt;
        if ((alertID == Constants.SIM1 && prefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && prefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && prefs.getBoolean(Constants.PREF_SIM3[7], true)))
            txt = getResources().getString(R.string.data_dis);
        else
            txt = getResources().getString(R.string.data_dis_tip);

        Notification n = builder.setContentIntent(pIntent).setSmallIcon(R.drawable.ic_launcher_small)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLargeIcon(bm)
                .setTicker(context.getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(txt)
                .setContentText(opName + ": " + getResources().getString(R.string.over_limit))
                .build();
        if (prefs.getBoolean(Constants.PREF_OTHER[4], false) && !prefs.getString(Constants.PREF_OTHER[1], "").equals("")) {
            n.sound = Uri.parse(prefs.getString(Constants.PREF_OTHER[1], ""));
            n.flags = Notification.FLAG_ONLY_ALERT_ONCE;
        }
        nm.notify(alertID, n);


    }

    private static int[] getWidgetIds(Context context) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Picasso.with(context).cancelRequest(target);
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
}
