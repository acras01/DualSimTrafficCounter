package ua.od.acros.dualsimtrafficcounter.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.support.v4.app.NotificationCompat;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.ChooseActionDialog;
import ua.od.acros.dualsimtrafficcounter.events.ActionTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.ClearTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.MobileConnectionEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoConnectivityEvent;
import ua.od.acros.dualsimtrafficcounter.events.SetTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.TipTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.settings.TrafficLimitFragment;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.DateUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.MyNotification;
import ua.od.acros.dualsimtrafficcounter.widgets.TrafficInfoWidget;


public class TrafficCountService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
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
    private boolean mIsSIM1OverLimit = false;
    private boolean mIsSIM2OverLimit = false;
    private boolean mIsSIM3OverLimit = false;
    private boolean mSIM1ContinueOverLimit;
    private boolean mSIM2ContinueOverLimit;
    private boolean mSIM3ContinueOverLimit;
    private boolean mIsResetNeeded3 = false;
    private boolean mIsResetNeeded2 = false;
    private boolean mIsResetNeeded1 = false;
    private static boolean mIsNight1 = false;
    private static boolean mIsNight2 = false;
    private static boolean mIsNight3 = false;
    private int mSimQuantity = 0;
    private int mPriority;
    private static int mActiveSIM = Constants.DISABLED;
    private static int mLastActiveSIM = Constants.DISABLED;
    private DateTimeFormatter fmtDate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
    private DateTimeFormatter fmtTime = DateTimeFormat.forPattern(Constants.TIME_FORMAT  + ":ss");
    private DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
    private DateTime mResetTime1;
    private DateTime mResetTime2;
    private DateTime mResetTime3;
    private ContentValues mDataMap;
    private MyDatabaseHelper mDbHelper;
    private ScheduledExecutorService mTaskExecutor = null;
    private ScheduledFuture<?> mTaskResult = null;
    private SharedPreferences mPrefs;
    private boolean mResetRuleHasChanged;
    private String[] mOperatorNames = new String[3];
    private boolean mHasActionChosen;
    private long[] mLimits = new long[3];
    private boolean mLimitHasChanged = false;
    private DateTime mLastDate, mNowDate;


    public TrafficCountService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static int getLastActiveSIM() {
        return mLastActiveSIM;
    }

    public static int getActiveSIM() {
        return mActiveSIM;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        EventBus.getDefault().register(this);

        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        mDbHelper = MyDatabaseHelper.getInstance(mContext);
        mDataMap = MyDatabaseHelper.readTrafficData(mDbHelper);
        if (mDataMap.get(Constants.LAST_DATE).equals("")) {
            DateTime dateTime = new DateTime();
            mDataMap.put(Constants.LAST_TIME, dateTime.toString(fmtTime));
            mDataMap.put(Constants.LAST_DATE, dateTime.toString(fmtDate));
        }

        mActiveSIM = Constants.DISABLED;
        mLastActiveSIM = (int) mDataMap.get(Constants.LAST_ACTIVE_SIM);

        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};

        sendDataBroadcast(0L, 0L);

        // cancel if already existed
        if (mTaskExecutor != null) {
            mTaskResult.cancel(true);
            mTaskExecutor.shutdown();
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        } else {
            // recreate new
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    @Subscribe
    public void onMessageEvent(MobileConnectionEvent event) {
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        timerStart(Constants.COUNT);
    }

    @Subscribe
    public void onMessageEvent(NoConnectivityEvent event) {
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        mLastActiveSIM = mActiveSIM;
        if (mPrefs.getBoolean(Constants.PREF_SIM1[14], true) && mLastActiveSIM == Constants.SIM1) {
            mDataMap.put(Constants.TOTAL1, DataFormat.getRoundLong((long) mDataMap.get(Constants.TOTAL1),
                    mPrefs.getString(Constants.PREF_SIM1[15], "1"), mPrefs.getString(Constants.PREF_SIM1[16], "0")));
            MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
        }

        if (mPrefs.getBoolean(Constants.PREF_SIM2[14], true) && mLastActiveSIM == Constants.SIM2) {
            mDataMap.put(Constants.TOTAL2, DataFormat.getRoundLong((long) mDataMap.get(Constants.TOTAL2),
                    mPrefs.getString(Constants.PREF_SIM2[15], "1"), mPrefs.getString(Constants.PREF_SIM2[16], "0")));
            MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
        }

        if (mPrefs.getBoolean(Constants.PREF_SIM3[14], true) && mLastActiveSIM == Constants.SIM3) {
            mDataMap.put(Constants.TOTAL3, DataFormat.getRoundLong((long) mDataMap.get(Constants.TOTAL3),
                    mPrefs.getString(Constants.PREF_SIM3[15], "1"), mPrefs.getString(Constants.PREF_SIM3[16], "0")));
            MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
        }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
    }

    @Subscribe
    public void onMessageEvent(ActionTrafficEvent event) {
        mHasActionChosen = true;
        int sim = event.sim;
        try {
            switch (event.action) {
                case Constants.CHANGE_ACTION:
                    if (!mIsSIM2OverLimit && sim == Constants.SIM1) {
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
                        timerStart(Constants.COUNT);
                    } else if (!mIsSIM3OverLimit && sim == Constants.SIM1) {
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                        timerStart(Constants.COUNT);
                    } else if (!mIsSIM1OverLimit && sim == Constants.SIM2) {
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                        timerStart(Constants.COUNT);
                    } else if (!mIsSIM3OverLimit && sim == Constants.SIM2) {
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                        timerStart(Constants.COUNT);
                    } else if (!mIsSIM1OverLimit && sim == Constants.SIM3) {
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                        timerStart(Constants.COUNT);
                    } else if (!mIsSIM2OverLimit && sim == Constants.SIM3) {
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
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
                    Intent i = new Intent(mContext, SettingsActivity.class);
                    i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, TrafficLimitFragment.class.getName());
                    i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra(Constants.SIM_ACTIVE, sim);
                    startActivity(i);
                    timerStart(Constants.CHECK);
                    break;
                case Constants.CONTINUE_ACTION:
                    if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && MyApplication.hasRoot()) ||
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MyApplication.isMtkDevice()))
                        MobileUtils.toggleMobileDataConnection(true, mContext, sim);
                    switch (sim) {
                        case Constants.SIM1:
                            mSIM1ContinueOverLimit = true;
                            break;
                        case Constants.SIM2:
                            mSIM2ContinueOverLimit = true;
                            break;
                        case Constants.SIM3:
                            mSIM3ContinueOverLimit = true;
                            break;
                    }
                    if (mTaskResult.isCancelled())
                        timerStart(Constants.COUNT);
                    break;
                case Constants.OFF_ACTION:
                    if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && MyApplication.hasRoot()) ||
                            (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MyApplication.isMtkDevice()))
                        timerStart(Constants.CHECK);
                    else {
                        switch (mActiveSIM) {
                            case Constants.SIM1:
                                mSIM1ContinueOverLimit = true;
                                break;
                            case Constants.SIM2:
                                mSIM2ContinueOverLimit = true;
                                break;
                            case Constants.SIM3:
                                mSIM3ContinueOverLimit = true;
                                break;
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }

    }

    @Subscribe
    public void onMessageEvent(ClearTrafficEvent event) {
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        int sim = event.sim;
        switch (sim) {
            case Constants.SIM1:
                if (mIsNight1) {
                    mDataMap.put(Constants.SIM1RX_N, 0L);
                    mDataMap.put(Constants.SIM1TX_N, 0L);
                    mDataMap.put(Constants.TOTAL1_N, 0L);
                } else {
                    mDataMap.put(Constants.SIM1RX, 0L);
                    mDataMap.put(Constants.SIM1TX, 0L);
                    mDataMap.put(Constants.TOTAL1, 0L);
                }
                break;
            case Constants.SIM2:
                if (mIsNight2) {
                    mDataMap.put(Constants.SIM2RX_N, 0L);
                    mDataMap.put(Constants.SIM2TX_N, 0L);
                    mDataMap.put(Constants.TOTAL2_N, 0L);
                } else {
                    mDataMap.put(Constants.SIM2RX, 0L);
                    mDataMap.put(Constants.SIM2TX, 0L);
                    mDataMap.put(Constants.TOTAL2, 0L);
                }
                break;
            case Constants.SIM3:
                if (mIsNight3) {
                    mDataMap.put(Constants.SIM3RX_N, 0L);
                    mDataMap.put(Constants.SIM3TX_N, 0L);
                    mDataMap.put(Constants.TOTAL3_N, 0L);
                } else {
                    mDataMap.put(Constants.SIM3RX, 0L);
                    mDataMap.put(Constants.SIM3TX, 0L);
                    mDataMap.put(Constants.TOTAL3, 0L);
                }
                break;
        }
        MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
        if (MyApplication.isScreenOn(mContext)) {
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(Constants.STARTED_ID, buildNotification(sim));
        }
        if ((MyApplication.isActivityVisible() || getWidgetIds(mContext).length != 0) && MyApplication.isScreenOn(mContext))
            sendDataBroadcast(0L, 0L);
        timerStart(Constants.COUNT);
    }

    @Subscribe
    public void onMessageEvent(SetTrafficEvent event) {
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        if (mDataMap == null)
            mDataMap = MyDatabaseHelper.readTrafficData(mDbHelper);
        int sim = event.sim;
        switch (sim) {
            case Constants.SIM1:
                mReceived1 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted1 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight1) {
                    mDataMap.put(Constants.SIM1RX_N, mReceived1);
                    mDataMap.put(Constants.SIM1TX_N, mTransmitted1);
                    mDataMap.put(Constants.TOTAL1_N, mReceived1 + mTransmitted1);
                } else {
                    mDataMap.put(Constants.SIM1RX, mReceived1);
                    mDataMap.put(Constants.SIM1TX, mTransmitted1);
                    mDataMap.put(Constants.TOTAL1, mReceived1 + mTransmitted1);
                }
                MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
                break;
            case Constants.SIM2:
                mReceived2 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted2 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight2) {
                    mDataMap.put(Constants.SIM2RX_N, mReceived2);
                    mDataMap.put(Constants.SIM2TX_N, mTransmitted2);
                    mDataMap.put(Constants.TOTAL2_N, mReceived2 + mTransmitted2);
                } else {
                    mDataMap.put(Constants.SIM2RX, mReceived2);
                    mDataMap.put(Constants.SIM2TX, mTransmitted2);
                    mDataMap.put(Constants.TOTAL2, mReceived2 + mTransmitted2);
                }
                MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
                break;
            case Constants.SIM3:
                mReceived3 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted3 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight3) {
                    mDataMap.put(Constants.SIM3RX_N, mReceived3);
                    mDataMap.put(Constants.SIM3TX_N, mTransmitted3);
                    mDataMap.put(Constants.TOTAL3_N, mReceived3 + mTransmitted3);
                } else {
                    mDataMap.put(Constants.SIM3RX, mReceived3);
                    mDataMap.put(Constants.SIM3TX, mTransmitted3);
                    mDataMap.put(Constants.TOTAL3, mReceived3 + mTransmitted3);
                }
                MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
                break;
        }
        if (MyApplication.isScreenOn(mContext)) {
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(Constants.STARTED_ID, buildNotification(sim));
        }
        if ((MyApplication.isActivityVisible() || getWidgetIds(mContext).length != 0) && MyApplication.isScreenOn(mContext))
            sendDataBroadcast(0L, 0L);
        timerStart(Constants.COUNT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mSIM1ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM1[27], false);
        mSIM2ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM2[27], false);
        mSIM3ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM3[27], false);
        mHasActionChosen = mPrefs.getBoolean(Constants.PREF_OTHER[18], false);
        mIsResetNeeded1 = mPrefs.getBoolean(Constants.PREF_SIM1[25], false);
        if (mIsResetNeeded1)
            mResetTime1 = fmtDateTime.parseDateTime(mPrefs.getString(Constants.PREF_SIM1[26], "1970-01-01 00:00"));
        mIsResetNeeded2 = mPrefs.getBoolean(Constants.PREF_SIM2[25], false);
        if (mIsResetNeeded2)
            mResetTime2 = fmtDateTime.parseDateTime(mPrefs.getString(Constants.PREF_SIM2[26], "1970-01-01 00:00"));
        mIsResetNeeded3 = mPrefs.getBoolean(Constants.PREF_SIM3[25], false);
        if (mIsResetNeeded3)
            mResetTime3 = fmtDateTime.parseDateTime(mPrefs.getString(Constants.PREF_SIM3[26], "1970-01-01 00:00"));

        mLimits = getSIMLimits();

        mPriority = mPrefs.getBoolean(Constants.PREF_OTHER[12], true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN;

        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};

        mDataMap = MyDatabaseHelper.readTrafficData(mDbHelper);
        if (mDataMap.get(Constants.LAST_DATE).equals("")) {
            DateTime dateTime = new DateTime();
            mDataMap.put(Constants.LAST_TIME, dateTime.toString(fmtTime));
            mDataMap.put(Constants.LAST_DATE, dateTime.toString(fmtDate));
        }

        mActiveSIM = Constants.DISABLED;
        mLastActiveSIM = (int) mDataMap.get(Constants.LAST_ACTIVE_SIM);

        sendDataBroadcast(0L, 0L);

        MyNotification.setIdNeedsChange(true);
        startForeground(Constants.STARTED_ID, buildNotification(mLastActiveSIM));

        // schedule task
        timerStart(Constants.COUNT);

        return START_STICKY;
    }

    public static boolean[] getIsNight() {
        return new boolean[]{mIsNight1, mIsNight2, mIsNight3};
    }

    private void timerStart(int task) {
        TimerTask tTask = null;
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        mActiveSIM = MobileUtils.getMobileDataInfo(mContext, true)[1];
        MyNotification.setIdNeedsChange(true);
        if (task == Constants.COUNT) {
            switch (mActiveSIM) {
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
        if (mTaskResult == null || mTaskResult.isCancelled())
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        if (tTask != null) {
            mTaskResult = mTaskExecutor.scheduleAtFixedRate(tTask, 0, Constants.NOTIFY_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ((key.equals(Constants.PREF_SIM1[1]) || key.equals(Constants.PREF_SIM1[2])) && mActiveSIM == Constants.SIM1 && mSIM1ContinueOverLimit) {
            mSIM1ContinueOverLimit = false;
            mHasActionChosen = false;
        }
        if ((key.equals(Constants.PREF_SIM2[1]) || key.equals(Constants.PREF_SIM2[2])) && mActiveSIM == Constants.SIM2 && mSIM2ContinueOverLimit) {
            mSIM2ContinueOverLimit = false;
            mHasActionChosen = false;
        }
        if ((key.equals(Constants.PREF_SIM3[1]) || key.equals(Constants.PREF_SIM3[2])) && mActiveSIM == Constants.SIM3 && mSIM3ContinueOverLimit) {
            mSIM3ContinueOverLimit = false;
            mHasActionChosen = false;
        }
        if (key.equals(Constants.PREF_SIM1[3]) || key.equals(Constants.PREF_SIM1[9]) || key.equals(Constants.PREF_SIM1[10]) ||
                key.equals(Constants.PREF_SIM2[3]) || key.equals(Constants.PREF_SIM2[9]) || key.equals(Constants.PREF_SIM2[10]) ||
                key.equals(Constants.PREF_SIM3[3]) || key.equals(Constants.PREF_SIM3[9]) || key.equals(Constants.PREF_SIM3[10]))
            mResetRuleHasChanged = true;
        if (key.equals(Constants.PREF_SIM1[1]) || key.equals(Constants.PREF_SIM1[2]) || key.equals(Constants.PREF_SIM1[4]) ||
                key.equals(Constants.PREF_SIM2[1]) || key.equals(Constants.PREF_SIM2[2]) || key.equals(Constants.PREF_SIM2[4]) ||
                key.equals(Constants.PREF_SIM3[1]) || key.equals(Constants.PREF_SIM3[2]) || key.equals(Constants.PREF_SIM3[4]))
            mLimitHasChanged = true;
        if (key.equals(Constants.PREF_SIM1[5]) || key.equals(Constants.PREF_SIM1[6]))
            mOperatorNames[0] = MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        if (key.equals(Constants.PREF_SIM2[5]) || key.equals(Constants.PREF_SIM2[6]))
            mOperatorNames[1] = MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        if (key.equals(Constants.PREF_SIM3[5]) || key.equals(Constants.PREF_SIM3[6]))
            mOperatorNames[2] = MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        if (key.equals(Constants.PREF_OTHER[24]) && sharedPreferences.getBoolean(key, false)) {
            CountDownTimer timer = new CountDownTimer(2000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    int sim;
                    if (mActiveSIM == Constants.DISABLED)
                        sim = mLastActiveSIM;
                    else
                        sim = mActiveSIM;
                    NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(Constants.STARTED_ID, buildNotification(sim));
                }
            };
            timer.start();
        }
        if (key.equals(Constants.PREF_OTHER[12])) {
            MyNotification.setPriorityNeedsChange(true);
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(Constants.STARTED_ID, buildNotification(mActiveSIM));
        }
        if (key.equals(Constants.PREF_OTHER[15]) || key.equals(Constants.PREF_SIM1[23]) ||
                key.equals(Constants.PREF_SIM2[23]) || key.equals(Constants.PREF_SIM3[23])) {
            MyNotification.setIdNeedsChange(true);
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(Constants.STARTED_ID, buildNotification(mActiveSIM));
        }
    }

    private long[] getSIMLimits() {
        String limit1 = mIsNight1 ? mPrefs.getString(Constants.PREF_SIM1[18], "") : mPrefs.getString(Constants.PREF_SIM1[1], "");
        String limit2 = mIsNight2 ? mPrefs.getString(Constants.PREF_SIM2[18], "") : mPrefs.getString(Constants.PREF_SIM2[1], "");
        String limit3 = mIsNight3 ? mPrefs.getString(Constants.PREF_SIM3[18], "") : mPrefs.getString(Constants.PREF_SIM3[1], "");
        String round1 = mIsNight1 ? mPrefs.getString(Constants.PREF_SIM1[22], "") : mPrefs.getString(Constants.PREF_SIM1[4], "0");
        String round2 = mIsNight2 ? mPrefs.getString(Constants.PREF_SIM2[22], "") : mPrefs.getString(Constants.PREF_SIM2[4], "0");
        String round3 = mIsNight3 ? mPrefs.getString(Constants.PREF_SIM3[22], "") : mPrefs.getString(Constants.PREF_SIM3[4], "0");
        int value1;
        if (mPrefs.getString(Constants.PREF_SIM1[2], "").equals(""))
            value1 = 0;
        else
            value1 = mIsNight1 ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[19], "")) :
                    Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[2], ""));
        int value2;
        if (mPrefs.getString(Constants.PREF_SIM2[2], "").equals(""))
            value2 = 0;
        else
            value2 = mIsNight2 ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[19], "")) :
                    Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[2], ""));
        int value3;
        if (mPrefs.getString(Constants.PREF_SIM3[2], "").equals(""))
            value3 = 0;
        else
            value3 = mIsNight3 ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[19], "")) :
                    Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[2], ""));
        float valuer1;
        float valuer2;
        float valuer3;
        long lim1 = Long.MAX_VALUE;
        long lim2 = Long.MAX_VALUE;
        long lim3 = Long.MAX_VALUE;
        if (!limit1.equals("")) {
            valuer1 = 1 - Float.valueOf(round1) / 100;
            lim1 = (long) (valuer1 * DataFormat.getFormatLong(limit1, value1));
        }
        if (!limit2.equals("")) {
            valuer2 = 1 - Float.valueOf(round2) / 100;
            lim2 = (long) (valuer2 * DataFormat.getFormatLong(limit2, value2));
        }
        if (!limit3.equals("")) {
            valuer3 = 1 - Float.valueOf(round3) / 100;
            lim3 = (long) (valuer3 * DataFormat.getFormatLong(limit3, value3));
        }
        return new long[] {lim1, lim2, lim3};
    }

    private void checkIfResetNeeded() {
        String[] simPref;
        if (DateTimeComparator.getDateOnlyInstance().compare(mNowDate, mLastDate) > 0 || mResetRuleHasChanged) {
            simPref = new String[] {Constants.PREF_SIM1[3], Constants.PREF_SIM1[9],
                    Constants.PREF_SIM1[10], Constants.PREF_SIM1[24]};
            mResetTime1 = DateUtils.getResetDate(Constants.SIM1, mDataMap, mPrefs, simPref);
            if (mResetTime1 != null) {
                mIsResetNeeded1 = true;
                mPrefs.edit()
                        .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                        .putString(Constants.PREF_SIM1[26], mResetTime1.toString(fmtDateTime))
                        .apply();
            }
            if (mSimQuantity >= 2) {
                simPref = new String[] {Constants.PREF_SIM2[3], Constants.PREF_SIM2[9],
                        Constants.PREF_SIM2[10], Constants.PREF_SIM2[24]};
                mResetTime2 = DateUtils.getResetDate(Constants.SIM2, mDataMap, mPrefs, simPref);
                if (mResetTime2 != null) {
                    mIsResetNeeded2 = true;
                    mPrefs.edit()
                            .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                            .putString(Constants.PREF_SIM2[26], mResetTime2.toString(fmtDateTime))
                            .apply();
                }
            }
            if (mSimQuantity == 3) {
                simPref = new String[] {Constants.PREF_SIM3[3], Constants.PREF_SIM3[9],
                        Constants.PREF_SIM3[10], Constants.PREF_SIM3[24]};
                mResetTime3 = DateUtils.getResetDate(Constants.SIM3, mDataMap, mPrefs, simPref);
                if (mResetTime3 != null) {
                    mIsResetNeeded3 = true;
                    mPrefs.edit()
                            .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                            .putString(Constants.PREF_SIM3[26], mResetTime3.toString(fmtDateTime))
                            .apply();
                }
            }
            mResetRuleHasChanged = false;
        }
    }

    private class CheckTimerTask extends TimerTask {

        @Override
        public void run() {

            EventBus.getDefault().post(new TipTrafficEvent());

            DateTime dt = fmtDate.parseDateTime((String) mDataMap.get(Constants.LAST_DATE));

            if (mLimitHasChanged) {
                mLimits = getSIMLimits();
                mLimitHasChanged = false;
            }

            long tot1 = mIsNight1 ? (long) mDataMap.get(Constants.TOTAL1_N) : (long) mDataMap.get(Constants.TOTAL1);
            long tot2 = mIsNight2 ? (long) mDataMap.get(Constants.TOTAL2_N) : (long) mDataMap.get(Constants.TOTAL2);
            long tot3 = mIsNight3 ? (long) mDataMap.get(Constants.TOTAL3_N) : (long) mDataMap.get(Constants.TOTAL3);
            try {
                if (mIsSIM1OverLimit && (DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM1[3], ""))
                        || (tot1 <= mLimits[0] && (mPrefs.getBoolean(Constants.PREF_SIM1[8], false)
                        || (!mPrefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) && !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    timerStart(Constants.COUNT);
                }
                if (mIsSIM2OverLimit && (DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM2[3], ""))
                        || (tot2 <= mLimits[1] && (mPrefs.getBoolean(Constants.PREF_SIM2[8], false)
                        || (!mPrefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) && !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    timerStart(Constants.COUNT);
                }
                if (mIsSIM3OverLimit && (DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM3[3], ""))
                        || (tot3 <= mLimits[2] && (mPrefs.getBoolean(Constants.PREF_SIM3[8], false)
                        || (!mPrefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) && !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    timerStart(Constants.COUNT);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private class CountTimerTask1 extends TimerTask {

        @Override
        public void run() {
            try {
                if (MobileUtils.getMobileDataInfo(mContext, false)[0] == 2
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mDataMap
                    //begin
                    mDataMap.put(Constants.SIM1RX, (long) mDataMap.get(Constants.SIM1RX));
                    mDataMap.put(Constants.SIM2RX, (long) mDataMap.get(Constants.SIM2RX));
                    mDataMap.put(Constants.SIM3RX, (long) mDataMap.get(Constants.SIM3RX));
                    mDataMap.put(Constants.SIM1TX, (long) mDataMap.get(Constants.SIM1TX));
                    mDataMap.put(Constants.SIM2TX, (long) mDataMap.get(Constants.SIM2TX));
                    mDataMap.put(Constants.SIM3TX, (long) mDataMap.get(Constants.SIM3TX));
                    mDataMap.put(Constants.TOTAL1, (long) mDataMap.get(Constants.TOTAL1));
                    mDataMap.put(Constants.TOTAL2, (long) mDataMap.get(Constants.TOTAL2));
                    mDataMap.put(Constants.TOTAL3, (long) mDataMap.get(Constants.TOTAL3));
                    mDataMap.put(Constants.SIM1RX_N, (long) mDataMap.get(Constants.SIM1RX_N));
                    mDataMap.put(Constants.SIM2RX_N, (long) mDataMap.get(Constants.SIM2RX_N));
                    mDataMap.put(Constants.SIM3RX_N, (long) mDataMap.get(Constants.SIM3RX_N));
                    mDataMap.put(Constants.SIM1TX_N, (long) mDataMap.get(Constants.SIM1TX_N));
                    mDataMap.put(Constants.SIM2TX_N, (long) mDataMap.get(Constants.SIM2TX_N));
                    mDataMap.put(Constants.SIM3TX_N, (long) mDataMap.get(Constants.SIM3TX_N));
                    mDataMap.put(Constants.TOTAL1_N, (long) mDataMap.get(Constants.TOTAL1_N));
                    mDataMap.put(Constants.TOTAL2_N, (long) mDataMap.get(Constants.TOTAL2_N));
                    mDataMap.put(Constants.TOTAL3_N, (long) mDataMap.get(Constants.TOTAL3_N));
                    mDataMap.put(Constants.LAST_TIME, (String) mDataMap.get(Constants.LAST_TIME));
                    mDataMap.put(Constants.LAST_DATE, (String) mDataMap.get(Constants.LAST_DATE));
                    mDataMap.put(Constants.LAST_ACTIVE_SIM, (int) mDataMap.get(Constants.LAST_ACTIVE_SIM));
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

                    mLastDate = fmtDate.parseDateTime((String) mDataMap.get(Constants.LAST_DATE));
                    mNowDate = new DateTime();

                    if (mPrefs.getBoolean(Constants.PREF_SIM1[17], false)) {
                        String timeON = mNowDate.toString(fmtDate) + " " + mPrefs.getString(Constants.PREF_SIM1[20], "23:00");
                        String timeOFF = mNowDate.toString(fmtDate) + " " + mPrefs.getString(Constants.PREF_SIM1[21], "06:00");
                        mIsNight1 = DateTimeComparator.getInstance().compare(mNowDate, fmtDateTime.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(mNowDate, fmtDateTime.parseDateTime(timeOFF)) <= 0;
                    } else
                        mIsNight1 = false;

                    checkIfResetNeeded();

                    boolean emptyDB = MyDatabaseHelper.isTrafficTableEmpty(mDbHelper);

                    if (emptyDB) {
                        mDataMap.put(Constants.SIM1RX, 0L);
                        mDataMap.put(Constants.SIM2RX, 0L);
                        mDataMap.put(Constants.SIM3RX, 0L);
                        mDataMap.put(Constants.SIM1TX, 0L);
                        mDataMap.put(Constants.SIM2TX, 0L);
                        mDataMap.put(Constants.SIM3TX, 0L);
                        mDataMap.put(Constants.TOTAL1, 0L);
                        mDataMap.put(Constants.TOTAL2, 0L);
                        mDataMap.put(Constants.TOTAL3, 0L);
                        mDataMap.put(Constants.SIM1RX_N, 0L);
                        mDataMap.put(Constants.SIM2RX_N, 0L);
                        mDataMap.put(Constants.SIM3RX_N, 0L);
                        mDataMap.put(Constants.SIM1TX_N, 0L);
                        mDataMap.put(Constants.SIM2TX_N, 0L);
                        mDataMap.put(Constants.SIM3TX_N, 0L);
                        mDataMap.put(Constants.TOTAL1_N, 0L);
                        mDataMap.put(Constants.TOTAL2_N, 0L);
                        mDataMap.put(Constants.TOTAL3_N, 0L);
                        mDataMap.put(Constants.LAST_RX, 0L);
                        mDataMap.put(Constants.LAST_TX, 0L);
                        mDataMap.put(Constants.LAST_TIME, "");
                        mDataMap.put(Constants.LAST_DATE, "");
                        mDataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                    } else if ((DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0 && mIsResetNeeded1)
                            || (DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0 && mIsResetNeeded2)
                            || (DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0 && mIsResetNeeded3)) {
                        mHasActionChosen = false;
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0 && mIsResetNeeded1) {
                            mDataMap.put(Constants.SIM1RX, 0L);
                            mDataMap.put(Constants.SIM1TX, 0L);
                            mDataMap.put(Constants.TOTAL1, 0L);
                            mDataMap.put(Constants.SIM1RX_N, 0L);
                            mDataMap.put(Constants.SIM1TX_N, 0L);
                            mDataMap.put(Constants.TOTAL1_N, 0L);
                            rx = tx = mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM1[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM1);
                        }
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0 && mIsResetNeeded2) {
                            mDataMap.put(Constants.SIM2RX, 0L);
                            mDataMap.put(Constants.SIM2TX, 0L);
                            mDataMap.put(Constants.TOTAL2, 0L);
                            mDataMap.put(Constants.SIM2RX_N, 0L);
                            mDataMap.put(Constants.SIM2TX_N, 0L);
                            mDataMap.put(Constants.TOTAL2_N, 0L);
                            if (!mIsNight1) {
                                rx = (long) mDataMap.get(Constants.SIM1RX);
                                tx = (long) mDataMap.get(Constants.SIM1TX);
                                tot = (long) mDataMap.get(Constants.TOTAL1);
                            } else {
                                rx = (long) mDataMap.get(Constants.SIM1RX_N);
                                tx = (long) mDataMap.get(Constants.SIM1TX_N);
                                tot = (long) mDataMap.get(Constants.TOTAL1_N);
                            }
                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM2[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM2);
                        }
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0 && mIsResetNeeded3) {
                            mDataMap.put(Constants.SIM3RX, 0L);
                            mDataMap.put(Constants.SIM3TX, 0L);
                            mDataMap.put(Constants.TOTAL3, 0L);
                            mDataMap.put(Constants.SIM3RX_N, 0L);
                            mDataMap.put(Constants.SIM3TX_N, 0L);
                            mDataMap.put(Constants.TOTAL3_N, 0L);
                            if (!mIsNight1) {
                                rx = (long) mDataMap.get(Constants.SIM1RX);
                                tx = (long) mDataMap.get(Constants.SIM1TX);
                                tot = (long) mDataMap.get(Constants.TOTAL1);
                            } else {
                                rx = (long) mDataMap.get(Constants.SIM1RX_N);
                                tx = (long) mDataMap.get(Constants.SIM1TX_N);
                                tot = (long) mDataMap.get(Constants.TOTAL1_N);
                            }
                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM3[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight1) {
                            rx = (long) mDataMap.get(Constants.SIM1RX);
                            tx = (long) mDataMap.get(Constants.SIM1TX);
                            tot = (long) mDataMap.get(Constants.TOTAL1);
                        } else {
                            rx = (long) mDataMap.get(Constants.SIM1RX_N);
                            tx = (long) mDataMap.get(Constants.SIM1TX_N);
                            tot = (long) mDataMap.get(Constants.TOTAL1_N);
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX1;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX1;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX1 = TrafficStats.getMobileRxBytes();
                    mStartTX1 = TrafficStats.getMobileTxBytes();

                    if (mLimitHasChanged) {
                        mLimits = getSIMLimits();
                        mLimitHasChanged = false;
                    }

                    if ((tot <= mLimits[0]) || mSIM1ContinueOverLimit) {
                        mDataMap.put(Constants.LAST_ACTIVE_SIM, mActiveSIM);
                        rx += diffrx;
                        tx += difftx;
                        tot = tx + rx;
                        mIsSIM1OverLimit = false;
                    } else if (!mHasActionChosen) {
                        mIsSIM1OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            alertNotify(mActiveSIM);
                        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && MyApplication.hasRoot()) ||
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MyApplication.isMtkDevice()))
                            startCheck(mActiveSIM);
                        else if (!ChooseActionDialog.isActive()) {
                            Intent dialogIntent = new Intent(mContext, ChooseActionDialog.class);
                            dialogIntent.putExtra(Constants.SIM_ACTIVE, mActiveSIM);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(dialogIntent);
                        }
                    }

                    if (!mIsSIM1OverLimit) {
                        if (!mIsNight1) {
                            mDataMap.put(Constants.SIM1RX, rx);
                            mDataMap.put(Constants.SIM1TX, tx);
                            mDataMap.put(Constants.TOTAL1, tot);
                        } else {
                            mDataMap.put(Constants.SIM1RX_N, rx);
                            mDataMap.put(Constants.SIM1TX_N, tx);
                            mDataMap.put(Constants.TOTAL1_N, tot);
                        }
                        mDataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        mDataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());
                        writeToDataBase(diffrx, difftx, emptyDB);
                        if (MyApplication.isScreenOn(mContext)) {
                            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.notify(Constants.STARTED_ID, buildNotification(Constants.SIM1));
                        }
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds(mContext).length != 0) && MyApplication.isScreenOn(mContext))
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
                if (MobileUtils.getMobileDataInfo(mContext, false)[0] == 2
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mDataMap
                    //begin
                    mDataMap.put(Constants.SIM1RX, (long) mDataMap.get(Constants.SIM1RX));
                    mDataMap.put(Constants.SIM2RX, (long) mDataMap.get(Constants.SIM2RX));
                    mDataMap.put(Constants.SIM3RX, (long) mDataMap.get(Constants.SIM3RX));
                    mDataMap.put(Constants.SIM1TX, (long) mDataMap.get(Constants.SIM1TX));
                    mDataMap.put(Constants.SIM2TX, (long) mDataMap.get(Constants.SIM2TX));
                    mDataMap.put(Constants.SIM3TX, (long) mDataMap.get(Constants.SIM3TX));
                    mDataMap.put(Constants.TOTAL1, (long) mDataMap.get(Constants.TOTAL1));
                    mDataMap.put(Constants.TOTAL2, (long) mDataMap.get(Constants.TOTAL2));
                    mDataMap.put(Constants.TOTAL3, (long) mDataMap.get(Constants.TOTAL3));
                    mDataMap.put(Constants.SIM1RX_N, (long) mDataMap.get(Constants.SIM1RX_N));
                    mDataMap.put(Constants.SIM2RX_N, (long) mDataMap.get(Constants.SIM2RX_N));
                    mDataMap.put(Constants.SIM3RX_N, (long) mDataMap.get(Constants.SIM3RX_N));
                    mDataMap.put(Constants.SIM1TX_N, (long) mDataMap.get(Constants.SIM1TX_N));
                    mDataMap.put(Constants.SIM2TX_N, (long) mDataMap.get(Constants.SIM2TX_N));
                    mDataMap.put(Constants.SIM3TX_N, (long) mDataMap.get(Constants.SIM3TX_N));
                    mDataMap.put(Constants.TOTAL1_N, (long) mDataMap.get(Constants.TOTAL1_N));
                    mDataMap.put(Constants.TOTAL2_N, (long) mDataMap.get(Constants.TOTAL2_N));
                    mDataMap.put(Constants.TOTAL3_N, (long) mDataMap.get(Constants.TOTAL3_N));
                    mDataMap.put(Constants.LAST_TIME, (String) mDataMap.get(Constants.LAST_TIME));
                    mDataMap.put(Constants.LAST_DATE, (String) mDataMap.get(Constants.LAST_DATE));
                    mDataMap.put(Constants.LAST_ACTIVE_SIM, (int) mDataMap.get(Constants.LAST_ACTIVE_SIM));
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

                    mLastDate = fmtDate.parseDateTime((String) mDataMap.get(Constants.LAST_DATE));
                    mNowDate = new DateTime();

                    if (mPrefs.getBoolean(Constants.PREF_SIM2[17], false)) {
                        String timeON = mNowDate.toString(fmtDate) + " " + mPrefs.getString(Constants.PREF_SIM2[20], "23:00");
                        String timeOFF = mNowDate.toString(fmtDate) + " " + mPrefs.getString(Constants.PREF_SIM2[21], "06:00");
                        mIsNight2 = DateTimeComparator.getInstance().compare(mNowDate, fmtDateTime.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(mNowDate, fmtDateTime.parseDateTime(timeOFF)) <= 0;
                    } else
                        mIsNight2 = false;

                    checkIfResetNeeded();

                    boolean emptyDB = MyDatabaseHelper.isTrafficTableEmpty(mDbHelper);

                    if (emptyDB) {
                        mDataMap.put(Constants.SIM1RX, 0L);
                        mDataMap.put(Constants.SIM2RX, 0L);
                        mDataMap.put(Constants.SIM3RX, 0L);
                        mDataMap.put(Constants.SIM1TX, 0L);
                        mDataMap.put(Constants.SIM2TX, 0L);
                        mDataMap.put(Constants.SIM3TX, 0L);
                        mDataMap.put(Constants.TOTAL1, 0L);
                        mDataMap.put(Constants.TOTAL2, 0L);
                        mDataMap.put(Constants.TOTAL3, 0L);
                        mDataMap.put(Constants.SIM1RX_N, 0L);
                        mDataMap.put(Constants.SIM2RX_N, 0L);
                        mDataMap.put(Constants.SIM3RX_N, 0L);
                        mDataMap.put(Constants.SIM1TX_N, 0L);
                        mDataMap.put(Constants.SIM2TX_N, 0L);
                        mDataMap.put(Constants.SIM3TX_N, 0L);
                        mDataMap.put(Constants.TOTAL1_N, 0L);
                        mDataMap.put(Constants.TOTAL2_N, 0L);
                        mDataMap.put(Constants.TOTAL3_N, 0L);
                        mDataMap.put(Constants.LAST_RX, 0L);
                        mDataMap.put(Constants.LAST_TX, 0L);
                        mDataMap.put(Constants.LAST_TIME, "");
                        mDataMap.put(Constants.LAST_DATE, "");
                        mDataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                    } else if ((DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0 && mIsResetNeeded1)
                            || (DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0 && mIsResetNeeded2)
                            || (DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0 && mIsResetNeeded3)) {
                        mHasActionChosen = false;
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0 && mIsResetNeeded1) {
                            mDataMap.put(Constants.SIM1RX, 0L);
                            mDataMap.put(Constants.SIM1TX, 0L);
                            mDataMap.put(Constants.TOTAL1, 0L);
                            mDataMap.put(Constants.SIM1RX_N, 0L);
                            mDataMap.put(Constants.SIM1TX_N, 0L);
                            mDataMap.put(Constants.TOTAL1_N, 0L);
                            if (!mIsNight2) {
                                rx = (long) mDataMap.get(Constants.SIM2RX);
                                tx = (long) mDataMap.get(Constants.SIM2TX);
                                tot = (long) mDataMap.get(Constants.TOTAL2);
                            } else {
                                rx = (long) mDataMap.get(Constants.SIM2RX_N);
                                tx = (long) mDataMap.get(Constants.SIM2TX_N);
                                tot = (long) mDataMap.get(Constants.TOTAL2_N);
                            }
                            mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM1[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM1);
                        }
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0 && mIsResetNeeded2) {
                            mDataMap.put(Constants.SIM2RX, 0L);
                            mDataMap.put(Constants.SIM2TX, 0L);
                            mDataMap.put(Constants.TOTAL2, 0L);
                            mDataMap.put(Constants.SIM2RX_N, 0L);
                            mDataMap.put(Constants.SIM2TX_N, 0L);
                            mDataMap.put(Constants.TOTAL2_N, 0L);
                            rx = tx = mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM2[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM2);
                        }
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0 && mIsResetNeeded3) {
                            mDataMap.put(Constants.SIM3RX, 0L);
                            mDataMap.put(Constants.SIM3TX, 0L);
                            mDataMap.put(Constants.TOTAL3, 0L);
                            mDataMap.put(Constants.SIM3RX_N, 0L);
                            mDataMap.put(Constants.SIM3TX_N, 0L);
                            mDataMap.put(Constants.TOTAL3_N, 0L);
                            if (!mIsNight2) {
                                rx = (long) mDataMap.get(Constants.SIM2RX);
                                tx = (long) mDataMap.get(Constants.SIM2TX);
                                tot = (long) mDataMap.get(Constants.TOTAL2);
                            } else {
                                rx = (long) mDataMap.get(Constants.SIM2RX_N);
                                tx = (long) mDataMap.get(Constants.SIM2TX_N);
                                tot = (long) mDataMap.get(Constants.TOTAL2_N);
                            }
                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM3[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight2) {
                            rx = (long) mDataMap.get(Constants.SIM2RX);
                            tx = (long) mDataMap.get(Constants.SIM2TX);
                            tot = (long) mDataMap.get(Constants.TOTAL2);
                        } else {
                            rx = (long) mDataMap.get(Constants.SIM2RX_N);
                            tx = (long) mDataMap.get(Constants.SIM2TX_N);
                            tot = (long) mDataMap.get(Constants.TOTAL2_N);
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX2;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX2;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX2 = TrafficStats.getMobileRxBytes();
                    mStartTX2 = TrafficStats.getMobileTxBytes();

                    if (mLimitHasChanged) {
                        mLimits = getSIMLimits();
                        mLimitHasChanged = false;
                    }

                    if ((tot <= mLimits[1]) || mSIM2ContinueOverLimit) {
                        mDataMap.put(Constants.LAST_ACTIVE_SIM, mActiveSIM);
                        rx += diffrx;
                        tx += difftx;
                        tot = tx + rx;
                        mIsSIM2OverLimit = false;
                    } else if (!mHasActionChosen) {
                        mIsSIM2OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            alertNotify(mActiveSIM);
                        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && MyApplication.hasRoot()) ||
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MyApplication.isMtkDevice()))
                            startCheck(mActiveSIM);
                        else if (!ChooseActionDialog.isActive()) {
                            Intent dialogIntent = new Intent(mContext, ChooseActionDialog.class);
                            dialogIntent.putExtra(Constants.SIM_ACTIVE, mActiveSIM);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(dialogIntent);
                        }
                    }

                    if (!mIsSIM2OverLimit) {
                        if (!mIsNight2) {
                            mDataMap.put(Constants.SIM2RX, rx);
                            mDataMap.put(Constants.SIM2TX, tx);
                            mDataMap.put(Constants.TOTAL2, tot);
                        } else {
                            mDataMap.put(Constants.SIM2RX_N, rx);
                            mDataMap.put(Constants.SIM2TX_N, tx);
                            mDataMap.put(Constants.TOTAL2_N, tot);
                        }
                        mDataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        mDataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());
                        writeToDataBase(diffrx, difftx, emptyDB);
                        if (MyApplication.isScreenOn(mContext)) {
                            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.notify(Constants.STARTED_ID, buildNotification(Constants.SIM2));
                        }
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds(mContext).length != 0) && MyApplication.isScreenOn(mContext))
                        sendDataBroadcast(speedRX, speedTX);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private class CountTimerTask3 extends TimerTask {

        @Override
        public void run() {
            try {
                if (MobileUtils.getMobileDataInfo(mContext, false)[0] == 2
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mDataMap
                    //begin
                    mDataMap.put(Constants.SIM1RX, (long) mDataMap.get(Constants.SIM1RX));
                    mDataMap.put(Constants.SIM2RX, (long) mDataMap.get(Constants.SIM2RX));
                    mDataMap.put(Constants.SIM3RX, (long) mDataMap.get(Constants.SIM3RX));
                    mDataMap.put(Constants.SIM1TX, (long) mDataMap.get(Constants.SIM1TX));
                    mDataMap.put(Constants.SIM2TX, (long) mDataMap.get(Constants.SIM2TX));
                    mDataMap.put(Constants.SIM3TX, (long) mDataMap.get(Constants.SIM3TX));
                    mDataMap.put(Constants.TOTAL1, (long) mDataMap.get(Constants.TOTAL1));
                    mDataMap.put(Constants.TOTAL2, (long) mDataMap.get(Constants.TOTAL2));
                    mDataMap.put(Constants.TOTAL3, (long) mDataMap.get(Constants.TOTAL3));
                    mDataMap.put(Constants.SIM1RX_N, (long) mDataMap.get(Constants.SIM1RX_N));
                    mDataMap.put(Constants.SIM2RX_N, (long) mDataMap.get(Constants.SIM2RX_N));
                    mDataMap.put(Constants.SIM3RX_N, (long) mDataMap.get(Constants.SIM3RX_N));
                    mDataMap.put(Constants.SIM1TX_N, (long) mDataMap.get(Constants.SIM1TX_N));
                    mDataMap.put(Constants.SIM2TX_N, (long) mDataMap.get(Constants.SIM2TX_N));
                    mDataMap.put(Constants.SIM3TX_N, (long) mDataMap.get(Constants.SIM3TX_N));
                    mDataMap.put(Constants.TOTAL1_N, (long) mDataMap.get(Constants.TOTAL1_N));
                    mDataMap.put(Constants.TOTAL2_N, (long) mDataMap.get(Constants.TOTAL2_N));
                    mDataMap.put(Constants.TOTAL3_N, (long) mDataMap.get(Constants.TOTAL3_N));
                    mDataMap.put(Constants.LAST_TIME, (String) mDataMap.get(Constants.LAST_TIME));
                    mDataMap.put(Constants.LAST_DATE, (String) mDataMap.get(Constants.LAST_DATE));
                    mDataMap.put(Constants.LAST_ACTIVE_SIM, (int) mDataMap.get(Constants.LAST_ACTIVE_SIM));
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

                    mLastDate = fmtDate.parseDateTime((String) mDataMap.get(Constants.LAST_DATE));
                    mNowDate = new DateTime();

                    if (mPrefs.getBoolean(Constants.PREF_SIM3[17], false)) {
                        String timeON = mNowDate.toString(fmtDate) + " " + mPrefs.getString(Constants.PREF_SIM3[20], "23:00");
                        String timeOFF = mNowDate.toString(fmtDate) + " " + mPrefs.getString(Constants.PREF_SIM3[21], "06:00");
                        mIsNight3 = DateTimeComparator.getInstance().compare(mNowDate, fmtDateTime.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(mNowDate, fmtDateTime.parseDateTime(timeOFF)) <= 0;
                    } else
                        mIsNight3 = false;

                    checkIfResetNeeded();

                    boolean emptyDB = MyDatabaseHelper.isTrafficTableEmpty(mDbHelper);

                    if (emptyDB) {
                        mDataMap.put(Constants.SIM1RX, 0L);
                        mDataMap.put(Constants.SIM2RX, 0L);
                        mDataMap.put(Constants.SIM3RX, 0L);
                        mDataMap.put(Constants.SIM1TX, 0L);
                        mDataMap.put(Constants.SIM2TX, 0L);
                        mDataMap.put(Constants.SIM3TX, 0L);
                        mDataMap.put(Constants.TOTAL1, 0L);
                        mDataMap.put(Constants.TOTAL2, 0L);
                        mDataMap.put(Constants.TOTAL3, 0L);
                        mDataMap.put(Constants.SIM1RX_N, 0L);
                        mDataMap.put(Constants.SIM2RX_N, 0L);
                        mDataMap.put(Constants.SIM3RX_N, 0L);
                        mDataMap.put(Constants.SIM1TX_N, 0L);
                        mDataMap.put(Constants.SIM2TX_N, 0L);
                        mDataMap.put(Constants.SIM3TX_N, 0L);
                        mDataMap.put(Constants.TOTAL1_N, 0L);
                        mDataMap.put(Constants.TOTAL2_N, 0L);
                        mDataMap.put(Constants.TOTAL3_N, 0L);
                        mDataMap.put(Constants.LAST_RX, 0L);
                        mDataMap.put(Constants.LAST_TX, 0L);
                        mDataMap.put(Constants.LAST_TIME, "");
                        mDataMap.put(Constants.LAST_DATE, "");
                        mDataMap.put(Constants.LAST_ACTIVE_SIM, Constants.DISABLED);
                    } else if ((DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0 && mIsResetNeeded1)
                            || (DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0 && mIsResetNeeded2)
                            || (DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0 && mIsResetNeeded3)) {
                        mHasActionChosen = false;
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0 && mIsResetNeeded1) {
                            mDataMap.put(Constants.SIM1RX, 0L);
                            mDataMap.put(Constants.SIM1TX, 0L);
                            mDataMap.put(Constants.TOTAL1, 0L);
                            mDataMap.put(Constants.SIM1RX_N, 0L);
                            mDataMap.put(Constants.SIM1TX_N, 0L);
                            mDataMap.put(Constants.TOTAL1_N, 0L);
                            if (!mIsNight3) {
                                rx = (long) mDataMap.get(Constants.SIM3RX);
                                tx = (long) mDataMap.get(Constants.SIM3TX);
                                tot = (long) mDataMap.get(Constants.TOTAL3);
                            } else {
                                rx = (long) mDataMap.get(Constants.SIM3RX_N);
                                tx = (long) mDataMap.get(Constants.SIM3TX_N);
                                tot = (long) mDataMap.get(Constants.TOTAL3_N);
                            }
                            mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM1[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM1);
                        }
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0 && mIsResetNeeded2) {
                            mDataMap.put(Constants.SIM2RX, 0L);
                            mDataMap.put(Constants.SIM2TX, 0L);
                            mDataMap.put(Constants.TOTAL2, 0L);
                            mDataMap.put(Constants.SIM2RX_N, 0L);
                            mDataMap.put(Constants.SIM2TX_N, 0L);
                            mDataMap.put(Constants.TOTAL2_N, 0L);
                            if (!mIsNight3) {
                                rx = (long) mDataMap.get(Constants.SIM3RX);
                                tx = (long) mDataMap.get(Constants.SIM3TX);
                                tot = (long) mDataMap.get(Constants.TOTAL3);
                            } else {
                                rx = (long) mDataMap.get(Constants.SIM3RX_N);
                                tx = (long) mDataMap.get(Constants.SIM3TX_N);
                                tot = (long) mDataMap.get(Constants.TOTAL3_N);
                            }
                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM2[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM2);
                        }
                        if (DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0 && mIsResetNeeded3) {
                            mDataMap.put(Constants.SIM3RX, 0L);
                            mDataMap.put(Constants.SIM3TX, 0L);
                            mDataMap.put(Constants.TOTAL3, 0L);
                            mDataMap.put(Constants.SIM3RX_N, 0L);
                            mDataMap.put(Constants.SIM3TX_N, 0L);
                            mDataMap.put(Constants.TOTAL3_N, 0L);
                            rx = tx = mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .apply();
                            mPrefs.edit().putString(Constants.PREF_SIM3[24], mNowDate.toString(fmtDateTime)).apply();
                            pushResetNotification(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight2) {
                            rx = (long) mDataMap.get(Constants.SIM3RX);
                            tx = (long) mDataMap.get(Constants.SIM3TX);
                            tot = (long) mDataMap.get(Constants.TOTAL3);
                        } else {
                            rx = (long) mDataMap.get(Constants.SIM3RX_N);
                            tx = (long) mDataMap.get(Constants.SIM3TX_N);
                            tot = (long) mDataMap.get(Constants.TOTAL3_N);
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX3;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX3;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX3 = TrafficStats.getMobileRxBytes();
                    mStartTX3 = TrafficStats.getMobileTxBytes();

                    if (mLimitHasChanged) {
                        mLimits = getSIMLimits();
                        mLimitHasChanged = false;
                    }

                    if ((tot <= mLimits[2]) || mSIM3ContinueOverLimit) {
                        mDataMap.put(Constants.LAST_ACTIVE_SIM, mActiveSIM);
                        rx += diffrx;
                        tx += difftx;
                        tot = tx + rx;
                        mIsSIM3OverLimit = false;
                    } else if (!mHasActionChosen) {
                        mIsSIM3OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            alertNotify(mActiveSIM);
                        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && MyApplication.hasRoot()) ||
                                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && MyApplication.isMtkDevice()))
                            startCheck(mActiveSIM);
                        else if (!ChooseActionDialog.isActive()) {
                            Intent dialogIntent = new Intent(mContext, ChooseActionDialog.class);
                            dialogIntent.putExtra(Constants.SIM_ACTIVE, mActiveSIM);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(dialogIntent);
                        }
                    }

                    if (!mIsSIM3OverLimit) {
                        if (!mIsNight3) {
                            mDataMap.put(Constants.SIM3RX, rx);
                            mDataMap.put(Constants.SIM3TX, tx);
                            mDataMap.put(Constants.TOTAL3, tot);
                        } else {
                            mDataMap.put(Constants.SIM3RX_N, rx);
                            mDataMap.put(Constants.SIM3TX_N, tx);
                            mDataMap.put(Constants.TOTAL3_N, tot);
                        }
                        mDataMap.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        mDataMap.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());
                        writeToDataBase(diffrx, difftx, emptyDB);
                        if (MyApplication.isScreenOn(mContext)) {
                            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.notify(Constants.STARTED_ID, buildNotification(Constants.SIM3));
                        }
                    }

                    if ((MyApplication.isActivityVisible() || getWidgetIds(mContext).length != 0) && MyApplication.isScreenOn(mContext))
                        sendDataBroadcast(speedRX, speedTX);
                }

            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private void pushResetNotification(int simid) {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alert);
        String text = "";
        int id;
        if (mPrefs.getBoolean(Constants.PREF_OTHER[15], false)) {
            String[] pref = new String[27];
            switch (simid) {
                case Constants.SIM1:
                    pref = Constants.PREF_SIM1;
                    text = mOperatorNames[0];
                    break;
                case Constants.SIM2:
                    pref = Constants.PREF_SIM2;
                    text = mOperatorNames[1];
                    break;
                case Constants.SIM3:
                    pref = Constants.PREF_SIM3;
                    text = mOperatorNames[2];
                    break;
            }
            if (mPrefs.getString(pref[23], "none").equals("auto"))
                id = getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(mContext, simid), "drawable", mContext.getPackageName());
            else
                id = getResources().getIdentifier(mPrefs.getString(pref[23], "none"), "drawable", mContext.getPackageName());
        } else
            id = R.drawable.ic_launcher_small;
        text = String.format(getResources().getString(R.string.data_reset), text);
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setAction("traffic");
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(id)
                .setLargeIcon(bm)
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(text);
        nm.notify(simid + 1977, builder.build());
    }

    private void writeToDataBase(long diffrx, long difftx, boolean emptyDB) {
        DateTime dateTime = new DateTime();
        final long MB = 1024 * 1024;
        if ((diffrx + difftx > MB) || dateTime.get(DateTimeFieldType.secondOfMinute()) == 59
                || emptyDB) {
            mDataMap.put(Constants.LAST_TIME, dateTime.toString(fmtTime));
            mDataMap.put(Constants.LAST_DATE, dateTime.toString(fmtDate));
            MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
        }
    }

    private void sendDataBroadcast(long speedRX, long speedTX) {
        Intent intent = new Intent(Constants.TRAFFIC_BROADCAST_ACTION);
        intent.putExtra(Constants.WIDGET_IDS, getWidgetIds(mContext));
        intent.putExtra(Constants.SPEEDRX, speedRX);
        intent.putExtra(Constants.SPEEDTX, speedTX);
        intent.putExtra(Constants.SIM1RX, (long) mDataMap.get(Constants.SIM1RX));
        intent.putExtra(Constants.SIM2RX, (long) mDataMap.get(Constants.SIM2RX));
        intent.putExtra(Constants.SIM3RX, (long) mDataMap.get(Constants.SIM3RX));
        intent.putExtra(Constants.SIM1TX, (long) mDataMap.get(Constants.SIM1TX));
        intent.putExtra(Constants.SIM2TX, (long) mDataMap.get(Constants.SIM2TX));
        intent.putExtra(Constants.SIM3TX, (long) mDataMap.get(Constants.SIM3TX));
        intent.putExtra(Constants.TOTAL1, (long) mDataMap.get(Constants.TOTAL1));
        intent.putExtra(Constants.TOTAL2, (long) mDataMap.get(Constants.TOTAL2));
        intent.putExtra(Constants.TOTAL3, (long) mDataMap.get(Constants.TOTAL3));
        intent.putExtra(Constants.SIM1RX_N, (long) mDataMap.get(Constants.SIM1RX_N));
        intent.putExtra(Constants.SIM2RX_N, (long) mDataMap.get(Constants.SIM2RX_N));
        intent.putExtra(Constants.SIM3RX_N, (long) mDataMap.get(Constants.SIM3RX_N));
        intent.putExtra(Constants.SIM1TX_N, (long) mDataMap.get(Constants.SIM1TX_N));
        intent.putExtra(Constants.SIM2TX_N, (long) mDataMap.get(Constants.SIM2TX_N));
        intent.putExtra(Constants.SIM3TX_N, (long) mDataMap.get(Constants.SIM3TX_N));
        intent.putExtra(Constants.TOTAL1_N, (long) mDataMap.get(Constants.TOTAL1_N));
        intent.putExtra(Constants.TOTAL2_N, (long) mDataMap.get(Constants.TOTAL2_N));
        intent.putExtra(Constants.TOTAL3_N, (long) mDataMap.get(Constants.TOTAL3_N));
        if (mActiveSIM == Constants.DISABLED)
            intent.putExtra(Constants.SIM_ACTIVE, mLastActiveSIM);
        else
            intent.putExtra(Constants.SIM_ACTIVE, mActiveSIM);
        intent.putExtra(Constants.OPERATOR1, mOperatorNames[0]);
        intent.putExtra(Constants.OPERATOR2, mOperatorNames[1]);
        intent.putExtra(Constants.OPERATOR3, mOperatorNames[2]);
        mContext.sendBroadcast(intent);
    }

    private Notification buildNotification(int sim) {
        String text = "";
        long tot1, tot2 = 0, tot3 = 0;
        if (mPrefs.getBoolean(Constants.PREF_OTHER[19], false)) {
            if (mLimitHasChanged) {
                mLimits = getSIMLimits();
                mLimitHasChanged = false;
            }
            tot1 = mIsNight1 ? mLimits[0] - (long) mDataMap.get(Constants.TOTAL1_N) : mLimits[0] - (long) mDataMap.get(Constants.TOTAL1);
            if (tot1 < 0)
                tot1 = 0;
            if (mSimQuantity >= 2) {
                tot2 = mIsNight2 ? mLimits[1] - (long) mDataMap.get(Constants.TOTAL2_N) : mLimits[1] - (long) mDataMap.get(Constants.TOTAL2);
                if (tot2 < 0)
                    tot2 = 0;
            }
            if (mSimQuantity == 3) {
                tot3 = mIsNight3 ? mLimits[2] - (long) mDataMap.get(Constants.TOTAL3_N) : mLimits[2] - (long) mDataMap.get(Constants.TOTAL3);
                if (tot3 < 0)
                    tot3 = 0;
            }
        } else {
            tot1 = mIsNight1 ? (long) mDataMap.get(Constants.TOTAL1_N) : (long) mDataMap.get(Constants.TOTAL1);
            tot2 = mIsNight2 ? (long) mDataMap.get(Constants.TOTAL2_N) : (long) mDataMap.get(Constants.TOTAL2);
            tot3 = mIsNight3 ? (long) mDataMap.get(Constants.TOTAL3_N) : (long) mDataMap.get(Constants.TOTAL3);
        }
        if (mPrefs.getBoolean(Constants.PREF_OTHER[16], true)) {
            if (mLimits[0] != Long.MAX_VALUE)
                text = DataFormat.formatData(mContext, tot1);
            else
                text = getString(R.string.not_set);
            if (mSimQuantity >= 2)
                if (mLimits[1] != Long.MAX_VALUE)
                    text += "  ||  " + DataFormat.formatData(mContext, tot2);
                else
                    text += "  ||  " + getString(R.string.not_set);
            if (mSimQuantity == 3)
                if (mLimits[2] != Long.MAX_VALUE)
                    text += "  ||  " + DataFormat.formatData(mContext, tot3);
                else
                    text += "  ||  " + getString(R.string.not_set);
        } else {
            switch (sim) {
                case Constants.SIM1:
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[19], false))
                        if (mLimits[0] != Long.MAX_VALUE)
                            text = String.format(getString(R.string.traffic_rest), mOperatorNames[0],
                                    DataFormat.formatData(mContext, tot1));
                        else
                            text = String.format(getString(R.string.traffic_rest), mOperatorNames[0],
                                    getString(R.string.not_set));
                    else
                        text = mOperatorNames[0] + ": " +
                            DataFormat.formatData(mContext, tot1);
                    break;
                case Constants.SIM2:
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[19], false))
                        if (mLimits[1] != Long.MAX_VALUE)
                            text = String.format(getString(R.string.traffic_rest), mOperatorNames[1],
                                    DataFormat.formatData(mContext, tot2));
                        else
                            text = String.format(getString(R.string.traffic_rest), mOperatorNames[1],
                                    getString(R.string.not_set));
                    else
                        text = mOperatorNames[1] + ": " +
                                DataFormat.formatData(mContext, tot2);
                    break;
                case Constants.SIM3:
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[19], false))
                        if (mLimits[2] != Long.MAX_VALUE)
                            text = String.format(getString(R.string.traffic_rest), mOperatorNames[2],
                                    DataFormat.formatData(mContext, tot3));
                        else
                            text = String.format(getString(R.string.traffic_rest), mOperatorNames[2],
                                    getString(R.string.not_set));
                    else
                        text = mOperatorNames[2] + ": " +
                            DataFormat.formatData(mContext, tot3);
                    break;
            }
        }
        return MyNotification.getNotification(mContext, text, "");
    }

    private void startCheck(int alertID) {
        try {
            MobileUtils.toggleMobileDataConnection(false, mContext, Constants.DISABLED);
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }

        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        boolean choice = false;

        if (((alertID == Constants.SIM1 && mPrefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && mPrefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && mPrefs.getBoolean(Constants.PREF_SIM3[7], true))) &&
                mPrefs.getBoolean(Constants.PREF_OTHER[10], true)) {
            try {
                if (!mIsSIM2OverLimit && alertID == Constants.SIM1 && mSimQuantity >= 2) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
                    timerStart(Constants.COUNT);
                } else if (!mIsSIM3OverLimit && alertID == Constants.SIM1 && mSimQuantity == 3) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                    timerStart(Constants.COUNT);
                } else if (!mIsSIM1OverLimit && alertID == Constants.SIM2) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                    timerStart(Constants.COUNT);
                } else if (!mIsSIM3OverLimit && alertID == Constants.SIM2 && mSimQuantity == 3) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                    timerStart(Constants.COUNT);
                } else if (!mIsSIM1OverLimit && alertID == Constants.SIM3) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                    timerStart(Constants.COUNT);
                } else if (!mIsSIM2OverLimit && alertID == Constants.SIM3) {
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
                    timerStart(Constants.COUNT);
                } else
                    choice = true;
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        } else if (((alertID == Constants.SIM1 && mPrefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && mPrefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && mPrefs.getBoolean(Constants.PREF_SIM3[7], true))) &&
                !mPrefs.getBoolean(Constants.PREF_OTHER[10], true))
            choice = true;
        else if ((alertID == Constants.SIM1 && !mPrefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && !mPrefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && !mPrefs.getBoolean(Constants.PREF_SIM3[7], true)) ||
                (mIsSIM1OverLimit && mIsSIM2OverLimit && mIsSIM3OverLimit && mPrefs.getBoolean(Constants.PREF_OTHER[10], true))) {
            Intent dialogIntent = new Intent(mContext, ChooseActionDialog.class);
            dialogIntent.putExtra(Constants.SIM_ACTIVE, alertID);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!ChooseActionDialog.isActive())
                mContext.startActivity(dialogIntent);
        } else if (mIsSIM1OverLimit && mIsSIM2OverLimit && mIsSIM2OverLimit)
            choice = true;
        if (choice) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_disable);
            Intent notificationIntent = new Intent(mContext, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_disable_small)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(mPriority)
                    .setLargeIcon(bm)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getString(R.string.service_stopped_title));
            nm.notify(Constants.STARTED_ID, builder.build());

            EventBus.getDefault().post(new TipTrafficEvent());

            timerStart(Constants.CHECK);
        }
    }

    private void alertNotify(int alertID) {
        Intent notificationIntent;
        if ((mPrefs.getBoolean(Constants.PREF_SIM1[7], true) && mIsSIM1OverLimit) ||
                (mPrefs.getBoolean(Constants.PREF_SIM2[7], true) && mIsSIM2OverLimit) ||
                (mPrefs.getBoolean(Constants.PREF_SIM2[7], true) && mIsSIM3OverLimit)) {
            notificationIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            notificationIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, TrafficLimitFragment.class.getName());
            notificationIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            notificationIntent.putExtra(Constants.SIM_ACTIVE, alertID);
        } else {
            final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
            notificationIntent = new Intent(Intent.ACTION_MAIN);
            notificationIntent.setComponent(cn);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        if (mPrefs.getBoolean(Constants.PREF_OTHER[2], false))
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alert);
        String opName;
        if (alertID == Constants.SIM1)
            opName = mOperatorNames[0];
        else if (alertID == Constants.SIM2)
            opName = mOperatorNames[1];
        else
            opName = mOperatorNames[2];
        String txt;
        if ((alertID == Constants.SIM1 && mPrefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (alertID == Constants.SIM2 && mPrefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (alertID == Constants.SIM3 && mPrefs.getBoolean(Constants.PREF_SIM3[7], true)))
            txt = getString(R.string.data_dis);
        else
            txt = getString(R.string.data_dis_tip);

        Notification n = builder
                .setContentIntent(pIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_alert_small)
                .setLargeIcon(bm)
                .setTicker(getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(txt)
                .setContentText(opName + ": " + getString(R.string.over_limit))
                .build();
        if (!mPrefs.getString(Constants.PREF_OTHER[1], "").equals("")) {
            n.sound = Uri.parse(mPrefs.getString(Constants.PREF_OTHER[1], ""));
            n.flags = Notification.FLAG_ONLY_ALERT_ONCE;
        }
        nm.notify(alertID, n);


    }

    private static int[] getWidgetIds(Context context) {
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, TrafficInfoWidget.class));
        if (ids.length == 0) {
            try {
                File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
                String[] children = dir.list();
                int i = 0;
                for (String aChildren : children) {
                    String[] str = aChildren.split("_");
                    if (str.length > 0 && str[1].equalsIgnoreCase("traffic") && str[2].equalsIgnoreCase("widget")) {
                        ids[i] = Integer.valueOf(aChildren.split("_")[0]);
                        i++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ids;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPrefs.edit().putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                .putBoolean(Constants.PREF_OTHER[18], mHasActionChosen)
                .apply();
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(Constants.STARTED_ID);
        MyDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
    }
}