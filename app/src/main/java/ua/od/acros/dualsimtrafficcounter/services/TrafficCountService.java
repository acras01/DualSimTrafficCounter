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
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.DateTimeFieldType;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.ChooseActionDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ChooseSimDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ManualSimDialog;
import ua.od.acros.dualsimtrafficcounter.events.ActionTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.MobileConnectionEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoConnectivityEvent;
import ua.od.acros.dualsimtrafficcounter.events.SetSimEvent;
import ua.od.acros.dualsimtrafficcounter.events.SetTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.TipTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.settings.TrafficLimitFragment;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.CustomNotification;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.DateUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.widgets.TrafficInfoWidget;
import wei.mark.standout.StandOutWindow;


public class TrafficCountService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static Context mContext;
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
    private static boolean mIsNight1 = false;
    private static boolean mIsNight2 = false;
    private static boolean mIsNight3 = false;
    private int mSimQuantity, mPriority;
    private static int mActiveSIM = Constants.DISABLED;
    private static int mLastActiveSIM = Constants.DISABLED;
    private DateTime mResetTime1, mResetTime2, mResetTime3, mLastDate, mNowDate;
    private ContentValues mTrafficData;
    private CustomDatabaseHelper mDbHelper;
    private ScheduledExecutorService mTaskExecutor = null;
    private ScheduledFuture<?> mTaskResult = null;
    private SharedPreferences mPrefs;
    private String[] mOperatorNames = new String[3];
    private boolean mHasActionChosen1, mHasActionChosen2, mHasActionChosen3,
            mHasPreLimitNotificationShown1, mHasPreLimitNotificationShown2, mHasPreLimitNotificationShown3,
            mSIM1ContinueOverLimit, mSIM2ContinueOverLimit, mSIM3ContinueOverLimit,
            mIsResetNeeded3, mIsResetNeeded2, mIsResetNeeded1, mResetRuleHasChanged;
    private long[] mLimits = new long[3];
    private boolean mLimitHasChanged = false;
    private Handler mHandler;
    private boolean[] mFlashPreOverLimit;
    private ArrayList<String> mIMSI = null;

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

        mContext = CustomApplication.getAppContext();
        EventBus.getDefault().register(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));

        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            mIMSI = MobileUtils.getSimIds(mContext);
            String path = mContext.getFilesDir().getParent() + "/shared_prefs/";
            SharedPreferences.Editor editor = mPrefs.edit();
            SharedPreferences prefSim;
            Map<String, ?> prefs;
            String name = "data_" + mIMSI.get(0);
            if (new File(path + name + ".xml").exists()) {
                prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                prefs = prefSim.getAll();
                if (prefs.size() != 0)
                    for (int i = 0; i < prefs.size(); i++) {
                        String key = Constants.PREF_SIM_DATA[i] + 1;
                        Object o = prefs.get(Constants.PREF_SIM_DATA[i]);
                        CustomApplication.putObject(editor, key, o);
                    }
                prefSim = null;
            }
            if (mSimQuantity >= 2) {
                name = "data_" + mIMSI.get(1);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (int i = 0; i < prefs.size(); i++) {
                            String key = Constants.PREF_SIM_DATA[i] + 2;
                            Object o = prefs.get(Constants.PREF_SIM_DATA[i]);
                            CustomApplication.putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            if (mSimQuantity == 3) {
                name = "data_" + mIMSI.get(2);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (int i = 0; i < prefs.size(); i++) {
                            String key = Constants.PREF_SIM_DATA[i] + 3;
                            Object o = prefs.get(Constants.PREF_SIM_DATA[i]);
                            CustomApplication.putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            editor.apply();
        }

        mPrefs = null;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        mDbHelper = CustomDatabaseHelper.getInstance(mContext);
        mTrafficData = new ContentValues();
        readFromDatabase();
        if (mTrafficData.get(Constants.LAST_DATE).equals("")) {
            DateTime dateTime = new DateTime();
            mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
            mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
        }

        mActiveSIM = Constants.DISABLED;
        mLastActiveSIM = mPrefs.getInt(Constants.PREF_OTHER[46], Constants.DISABLED);

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
        if (mPrefs.getBoolean(Constants.PREF_OTHER[43], true)) {
            mActiveSIM = MobileUtils.getActiveSimForData(mContext);
            if (mActiveSIM != Constants.DISABLED)
                timerStart(Constants.COUNT);
            else {
                Intent dialogIntent = new Intent(mContext, ManualSimDialog.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!ManualSimDialog.isActive())
                    mContext.startActivity(dialogIntent);
            }
        } else {
            Intent dialogIntent = new Intent(mContext, ChooseSimDialog.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!ChooseSimDialog.isActive())
                mContext.startActivity(dialogIntent);
        }
    }

    @Subscribe
    public void onMessageEvent(SetSimEvent event) {
        if (event.action != null) {
            if (mTaskResult != null) {
                mTaskResult.cancel(false);
                mTaskExecutor.shutdown();
            }
            mActiveSIM = event.sim;
            timerStart(Constants.COUNT);
        }
    }

    @Subscribe
    public void onMessageEvent(NoConnectivityEvent event) {
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        mLastActiveSIM = mActiveSIM;

        if (mPrefs.getBoolean(Constants.PREF_SIM1[14], true) && mLastActiveSIM == Constants.SIM1)
            mTrafficData.put(Constants.TOTAL1, DataFormat.getRoundLong((long) mTrafficData.get(Constants.TOTAL1),
                    mPrefs.getString(Constants.PREF_SIM1[15], "1"), mPrefs.getString(Constants.PREF_SIM1[16], "0")));

        if (mPrefs.getBoolean(Constants.PREF_SIM2[14], true) && mLastActiveSIM == Constants.SIM2)
            mTrafficData.put(Constants.TOTAL2, DataFormat.getRoundLong((long) mTrafficData.get(Constants.TOTAL2),
                    mPrefs.getString(Constants.PREF_SIM2[15], "1"), mPrefs.getString(Constants.PREF_SIM2[16], "0")));

        if (mPrefs.getBoolean(Constants.PREF_SIM3[14], true) && mLastActiveSIM == Constants.SIM3)
            mTrafficData.put(Constants.TOTAL3, DataFormat.getRoundLong((long) mTrafficData.get(Constants.TOTAL3),
                    mPrefs.getString(Constants.PREF_SIM3[15], "1"), mPrefs.getString(Constants.PREF_SIM3[16], "0")));

        writeToDataBase();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
    }

    @Subscribe
    public void onMessageEvent(ActionTrafficEvent event) {
        int sim = event.sim;
        switch (sim) {
            case Constants.SIM1:
                mHasActionChosen1 = true;
                break;
            case Constants.SIM2:
                mHasActionChosen2 = true;
                break;
            case Constants.SIM3:
                mHasActionChosen3 = true;
                break;
        }
        mPrefs.edit().putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                .apply();
        try {
            switch (event.action) {
                case Constants.CHANGE_ACTION:
                    if (!mIsSIM2OverLimit && sim == Constants.SIM1)
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
                    else if (!mIsSIM3OverLimit && sim == Constants.SIM1)
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                    else if (!mIsSIM1OverLimit && sim == Constants.SIM2)
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                    else if (!mIsSIM3OverLimit && sim == Constants.SIM2)
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                    else if (!mIsSIM1OverLimit && sim == Constants.SIM3)
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                    else if (!mIsSIM2OverLimit && sim == Constants.SIM3)
                        MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
                    else
                        timerStart(Constants.CHECK);
                    break;
                case Constants.SETTINGS_ACTION:
                    startActivity(CustomApplication.getSettingsIntent());
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
                    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && CustomApplication.hasRoot()) ||
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && CustomApplication.isOldMtkDevice()))
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
                    mPrefs.edit().putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                            .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                            .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                            .apply();
                    break;
                case Constants.OFF_ACTION:
                    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && CustomApplication.hasRoot()) ||
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && CustomApplication.isOldMtkDevice()))
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
                        mPrefs.edit().putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                                .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                                .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                                .apply();
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }

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
        if (mTrafficData == null)
            readFromDatabase();
        mNowDate = DateTime.now();
        mTrafficData.put(Constants.LAST_TIME, mNowDate.toString(Constants.TIME_FORMATTER));
        mTrafficData.put(Constants.LAST_DATE, mNowDate.toString(Constants.DATE_FORMATTER));
        int sim = event.sim;
        switch (sim) {
            case Constants.SIM1:
                mReceived1 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted1 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight1) {
                    mTrafficData.put(Constants.SIM1RX_N, mReceived1);
                    mTrafficData.put(Constants.SIM1TX_N, mTransmitted1);
                    mTrafficData.put(Constants.TOTAL1_N, mReceived1 + mTransmitted1);
                } else {
                    mTrafficData.put(Constants.SIM1RX, mReceived1);
                    mTrafficData.put(Constants.SIM1TX, mTransmitted1);
                    mTrafficData.put(Constants.TOTAL1, mReceived1 + mTransmitted1);
                }
                CustomDatabaseHelper.writeTrafficData(mTrafficData, mDbHelper);
                break;
            case Constants.SIM2:
                mReceived2 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted2 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight2) {
                    mTrafficData.put(Constants.SIM2RX_N, mReceived2);
                    mTrafficData.put(Constants.SIM2TX_N, mTransmitted2);
                    mTrafficData.put(Constants.TOTAL2_N, mReceived2 + mTransmitted2);
                } else {
                    mTrafficData.put(Constants.SIM2RX, mReceived2);
                    mTrafficData.put(Constants.SIM2TX, mTransmitted2);
                    mTrafficData.put(Constants.TOTAL2, mReceived2 + mTransmitted2);
                }
                CustomDatabaseHelper.writeTrafficData(mTrafficData, mDbHelper);
                break;
            case Constants.SIM3:
                mReceived3 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted3 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight3) {
                    mTrafficData.put(Constants.SIM3RX_N, mReceived3);
                    mTrafficData.put(Constants.SIM3TX_N, mTransmitted3);
                    mTrafficData.put(Constants.TOTAL3_N, mReceived3 + mTransmitted3);
                } else {
                    mTrafficData.put(Constants.SIM3RX, mReceived3);
                    mTrafficData.put(Constants.SIM3TX, mTransmitted3);
                    mTrafficData.put(Constants.TOTAL3, mReceived3 + mTransmitted3);
                }
                CustomDatabaseHelper.writeTrafficData(mTrafficData, mDbHelper);
                break;
        }
        if (CustomApplication.isScreenOn()) {
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(Constants.STARTED_ID, buildNotification(sim));
        }
        if ((CustomApplication.isActivityVisible() || getWidgetIds().length != 0) && CustomApplication.isScreenOn())
            sendDataBroadcast(0L, 0L);
        timerStart(Constants.COUNT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mHandler = new Handler();
        mHasPreLimitNotificationShown1 = false;
        mHasPreLimitNotificationShown2 = false;
        mHasPreLimitNotificationShown3 = false;
        mFlashPreOverLimit = new boolean[] {false, false, false};
        mSIM1ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM1[27], false);
        mSIM2ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM2[27], false);
        mSIM3ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM3[27], false);
        mHasActionChosen1 = mPrefs.getBoolean(Constants.PREF_SIM1[28], false);
        mHasActionChosen2 = mPrefs.getBoolean(Constants.PREF_SIM2[28], false);
        mHasActionChosen3 = mPrefs.getBoolean(Constants.PREF_SIM3[28], false);
        mIsResetNeeded1 = mPrefs.getBoolean(Constants.PREF_SIM1[25], false);
        if (mIsResetNeeded1)
            mResetTime1 = Constants.DATE_TIME_FORMATTER.parseDateTime(mPrefs.getString(Constants.PREF_SIM1[26], "1970-01-01 00:00"));
        mIsResetNeeded2 = mPrefs.getBoolean(Constants.PREF_SIM2[25], false);
        if (mIsResetNeeded2)
            mResetTime2 = Constants.DATE_TIME_FORMATTER.parseDateTime(mPrefs.getString(Constants.PREF_SIM2[26], "1970-01-01 00:00"));
        mIsResetNeeded3 = mPrefs.getBoolean(Constants.PREF_SIM3[25], false);
        if (mIsResetNeeded3)
            mResetTime3 = Constants.DATE_TIME_FORMATTER.parseDateTime(mPrefs.getString(Constants.PREF_SIM3[26], "1970-01-01 00:00"));

        mLimits = getSIMLimits();

        mPriority = mPrefs.getBoolean(Constants.PREF_OTHER[12], true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN;

        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};

        readFromDatabase();
        if (mTrafficData.get(Constants.LAST_DATE).equals("")) {
            DateTime dateTime = new DateTime();
            mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
            mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
        }

        mActiveSIM = Constants.DISABLED;
        mLastActiveSIM = mPrefs.getInt(Constants.PREF_OTHER[46], Constants.DISABLED);;

        sendDataBroadcast(0L, 0L);

        CustomNotification.setIdNeedsChange(true);
        startForeground(Constants.STARTED_ID, buildNotification(mLastActiveSIM));

        // schedule task
        if (mPrefs.getBoolean(Constants.PREF_OTHER[43], true)) {
            mActiveSIM = MobileUtils.getActiveSimForData(mContext);
            if (mActiveSIM != Constants.DISABLED)
                timerStart(Constants.COUNT);
            else {
                Intent dialogIntent = new Intent(mContext, ManualSimDialog.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!ManualSimDialog.isActive())
                    mContext.startActivity(dialogIntent);
            }
        } else {
            Intent dialogIntent = new Intent(mContext, ChooseSimDialog.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!ChooseSimDialog.isActive())
                mContext.startActivity(dialogIntent);
        }

        return START_STICKY;
    }

    private void readFromDatabase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            ContentValues cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(0));
            mTrafficData.put(Constants.SIM1RX, (long) cv.get("rx"));
            mTrafficData.put(Constants.SIM1TX, (long) cv.get("tx"));
            mTrafficData.put(Constants.TOTAL1, (long) cv.get("total"));
            mTrafficData.put(Constants.SIM1RX_N, (long) cv.get("rx_n"));
            mTrafficData.put(Constants.SIM1TX_N, (long) cv.get("tx_n"));
            mTrafficData.put(Constants.TOTAL1_N, (long) cv.get("total_n"));
            mTrafficData.put(Constants.PERIOD1, (int) cv.get("period"));
            mTrafficData.put(Constants.SIM2RX, 0L);
            mTrafficData.put(Constants.SIM3RX, 0L);
            mTrafficData.put(Constants.SIM2TX, 0L);
            mTrafficData.put(Constants.SIM3TX, 0L);
            mTrafficData.put(Constants.TOTAL2, 0L);
            mTrafficData.put(Constants.TOTAL3, 0L);
            mTrafficData.put(Constants.SIM2RX_N, 0L);
            mTrafficData.put(Constants.SIM3RX_N, 0L);
            mTrafficData.put(Constants.SIM2TX_N, 0L);
            mTrafficData.put(Constants.SIM3TX_N, 0L);
            mTrafficData.put(Constants.TOTAL2_N, 0L);
            mTrafficData.put(Constants.TOTAL3_N, 0L);
            mTrafficData.put(Constants.LAST_TIME, (String) cv.get(Constants.LAST_TIME));
            mTrafficData.put(Constants.LAST_DATE, (String) cv.get(Constants.LAST_DATE));
            if (mSimQuantity >= 2) {
                cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(1));
                mTrafficData.put(Constants.SIM2RX, (long) cv.get("rx"));
                mTrafficData.put(Constants.SIM2TX, (long) cv.get("tx"));
                mTrafficData.put(Constants.TOTAL2, (long) cv.get("total"));
                mTrafficData.put(Constants.SIM2RX_N, (long) cv.get("rx_n"));
                mTrafficData.put(Constants.SIM2TX_N, (long) cv.get("tx_n"));
                mTrafficData.put(Constants.TOTAL2_N, (long) cv.get("total_n"));
                mTrafficData.put(Constants.PERIOD2, (int) cv.get("period"));
            }
            if (mSimQuantity == 3) {
                cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(2));
                mTrafficData.put(Constants.SIM3RX, (long) cv.get("rx"));
                mTrafficData.put(Constants.SIM3TX, (long) cv.get("tx"));
                mTrafficData.put(Constants.TOTAL3, (long) cv.get("total"));
                mTrafficData.put(Constants.SIM3RX_N, (long) cv.get("rx_n"));
                mTrafficData.put(Constants.SIM3TX_N, (long) cv.get("tx_n"));
                mTrafficData.put(Constants.TOTAL3_N, (long) cv.get("total_n"));
                mTrafficData.put(Constants.PERIOD3, (int) cv.get("period"));
            }
        } else
            mTrafficData = CustomDatabaseHelper.readTrafficData(mDbHelper);
    }

    public static boolean[] getIsNight() {
        return new boolean[]{mIsNight1, mIsNight2, mIsNight3};
    }

    private void timerStart(int task) {
        TimerTask tTask = null;
        CustomNotification.setIdNeedsChange(true);
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
        if (key.equals(Constants.PREF_SIM1[1]) || key.equals(Constants.PREF_SIM1[2]) || key.equals(Constants.PREF_SIM1[3])
                || key.equals(Constants.PREF_SIM1[4]) || key.equals(Constants.PREF_SIM1[9]) || key.equals(Constants.PREF_SIM1[10])) {
            mSIM1ContinueOverLimit = false;
            mHasActionChosen1 = false;
            mPrefs.edit().putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                    .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                    .apply();
        }
        if (key.equals(Constants.PREF_SIM2[1]) || key.equals(Constants.PREF_SIM2[2]) || key.equals(Constants.PREF_SIM2[3])
                || key.equals(Constants.PREF_SIM2[4]) || key.equals(Constants.PREF_SIM2[9]) || key.equals(Constants.PREF_SIM2[10]))  {
            mSIM2ContinueOverLimit = false;
            mHasActionChosen2 = false;
            mPrefs.edit().putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                    .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                    .apply();
        }
        if (key.equals(Constants.PREF_SIM3[1]) || key.equals(Constants.PREF_SIM3[2]) || key.equals(Constants.PREF_SIM3[3])
                || key.equals(Constants.PREF_SIM3[4]) || key.equals(Constants.PREF_SIM3[9]) || key.equals(Constants.PREF_SIM3[10]))  {
            mSIM3ContinueOverLimit = false;
            mHasActionChosen3 = false;
            mPrefs.edit().putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                    .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                    .apply();
        }
        if (key.equals(Constants.PREF_SIM1[3]) || key.equals(Constants.PREF_SIM1[9]) || key.equals(Constants.PREF_SIM1[10]) ||
                key.equals(Constants.PREF_SIM2[3]) || key.equals(Constants.PREF_SIM2[9]) || key.equals(Constants.PREF_SIM2[10]) ||
                key.equals(Constants.PREF_SIM3[3]) || key.equals(Constants.PREF_SIM3[9]) || key.equals(Constants.PREF_SIM3[10]))
            mResetRuleHasChanged = true;
        if (key.equals(Constants.PREF_SIM1[1]) || key.equals(Constants.PREF_SIM1[2]) || key.equals(Constants.PREF_SIM1[4]) ||
                key.equals(Constants.PREF_SIM1[29]) || key.equals(Constants.PREF_SIM1[30]) ||
                key.equals(Constants.PREF_SIM2[1]) || key.equals(Constants.PREF_SIM2[2]) || key.equals(Constants.PREF_SIM2[4]) ||
                key.equals(Constants.PREF_SIM2[29]) || key.equals(Constants.PREF_SIM2[30]) ||
                key.equals(Constants.PREF_SIM3[1]) || key.equals(Constants.PREF_SIM3[2]) || key.equals(Constants.PREF_SIM3[4]) ||
                key.equals(Constants.PREF_SIM3[29]) || key.equals(Constants.PREF_SIM3[30])) {
            mLimitHasChanged = true;
            mHasPreLimitNotificationShown1 = false;
            mHasPreLimitNotificationShown2 = false;
            mHasPreLimitNotificationShown3 = false;
            mFlashPreOverLimit = new boolean[] {false, false, false};

        }
        if (key.equals(Constants.PREF_SIM1[5]) || key.equals(Constants.PREF_SIM1[6]))
            mOperatorNames[0] = MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        if (key.equals(Constants.PREF_SIM2[5]) || key.equals(Constants.PREF_SIM2[6]))
            mOperatorNames[1] = MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        if (key.equals(Constants.PREF_SIM3[5]) || key.equals(Constants.PREF_SIM3[6]))
            mOperatorNames[2] = MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        if (key.equals(Constants.PREF_OTHER[24]) && sharedPreferences.getBoolean(key, false)) {
            new CountDownTimer(2000, 2000) {
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
            }.start();
        }
    }

    private long[] getSIMLimits() {
        String limit1 = mIsNight1 ? mPrefs.getString(Constants.PREF_SIM1[18], "") : mPrefs.getString(Constants.PREF_SIM1[1], "");
        String limit2 = mIsNight2 ? mPrefs.getString(Constants.PREF_SIM2[18], "") : mPrefs.getString(Constants.PREF_SIM2[1], "");
        String limit3 = mIsNight3 ? mPrefs.getString(Constants.PREF_SIM3[18], "") : mPrefs.getString(Constants.PREF_SIM3[1], "");
        String round1 = mIsNight1 ? mPrefs.getString(Constants.PREF_SIM1[22], "0") : mPrefs.getString(Constants.PREF_SIM1[4], "0");
        String round2 = mIsNight2 ? mPrefs.getString(Constants.PREF_SIM2[22], "0") : mPrefs.getString(Constants.PREF_SIM2[4], "0");
        String round3 = mIsNight3 ? mPrefs.getString(Constants.PREF_SIM3[22], "0") : mPrefs.getString(Constants.PREF_SIM3[4], "0");
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
            mResetTime1 = DateUtils.getResetDate(Constants.SIM1, mTrafficData, mPrefs, simPref);
            if (mResetTime1 != null) {
                mIsResetNeeded1 = true;
                mPrefs.edit()
                        .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                        .putString(Constants.PREF_SIM1[26], mResetTime1.toString(Constants.DATE_TIME_FORMATTER))
                        .apply();
            }
            if (mSimQuantity >= 2) {
                simPref = new String[] {Constants.PREF_SIM2[3], Constants.PREF_SIM2[9],
                        Constants.PREF_SIM2[10], Constants.PREF_SIM2[24]};
                mResetTime2 = DateUtils.getResetDate(Constants.SIM2, mTrafficData, mPrefs, simPref);
                if (mResetTime2 != null) {
                    mIsResetNeeded2 = true;
                    mPrefs.edit()
                            .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                            .putString(Constants.PREF_SIM2[26], mResetTime2.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                }
            }
            if (mSimQuantity == 3) {
                simPref = new String[] {Constants.PREF_SIM3[3], Constants.PREF_SIM3[9],
                        Constants.PREF_SIM3[10], Constants.PREF_SIM3[24]};
                mResetTime3 = DateUtils.getResetDate(Constants.SIM3, mTrafficData, mPrefs, simPref);
                if (mResetTime3 != null) {
                    mIsResetNeeded3 = true;
                    mPrefs.edit()
                            .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                            .putString(Constants.PREF_SIM3[26], mResetTime3.toString(Constants.DATE_TIME_FORMATTER))
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

            DateTime dt = Constants.DATE_FORMATTER.parseDateTime((String) mTrafficData.get(Constants.LAST_DATE));

            if (mLimitHasChanged) {
                mLimits = getSIMLimits();
                mLimitHasChanged = false;
            }

            long tot1 = mIsNight1 ? (long) mTrafficData.get(Constants.TOTAL1_N) : (long) mTrafficData.get(Constants.TOTAL1);
            long tot2 = mIsNight2 ? (long) mTrafficData.get(Constants.TOTAL2_N) : (long) mTrafficData.get(Constants.TOTAL2);
            long tot3 = mIsNight3 ? (long) mTrafficData.get(Constants.TOTAL3_N) : (long) mTrafficData.get(Constants.TOTAL3);
            try {
                if (mPrefs.getBoolean(Constants.PREF_SIM1[31], false) && mIsSIM1OverLimit && (DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM1[3], ""))
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
                if (mPrefs.getBoolean(Constants.PREF_SIM2[31], false) && mIsSIM2OverLimit && (DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM2[3], ""))
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
                if (mPrefs.getBoolean(Constants.PREF_SIM3[31], false) && mIsSIM3OverLimit && (DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM3[3], ""))
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
                if (MobileUtils.isMobileDataActive(mContext)
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mTrafficData
                    //begin
                    mTrafficData.put(Constants.SIM1RX, (long) mTrafficData.get(Constants.SIM1RX));
                    mTrafficData.put(Constants.SIM2RX, (long) mTrafficData.get(Constants.SIM2RX));
                    mTrafficData.put(Constants.SIM3RX, (long) mTrafficData.get(Constants.SIM3RX));
                    mTrafficData.put(Constants.SIM1TX, (long) mTrafficData.get(Constants.SIM1TX));
                    mTrafficData.put(Constants.SIM2TX, (long) mTrafficData.get(Constants.SIM2TX));
                    mTrafficData.put(Constants.SIM3TX, (long) mTrafficData.get(Constants.SIM3TX));
                    mTrafficData.put(Constants.TOTAL1, (long) mTrafficData.get(Constants.TOTAL1));
                    mTrafficData.put(Constants.TOTAL2, (long) mTrafficData.get(Constants.TOTAL2));
                    mTrafficData.put(Constants.TOTAL3, (long) mTrafficData.get(Constants.TOTAL3));
                    mTrafficData.put(Constants.SIM1RX_N, (long) mTrafficData.get(Constants.SIM1RX_N));
                    mTrafficData.put(Constants.SIM2RX_N, (long) mTrafficData.get(Constants.SIM2RX_N));
                    mTrafficData.put(Constants.SIM3RX_N, (long) mTrafficData.get(Constants.SIM3RX_N));
                    mTrafficData.put(Constants.SIM1TX_N, (long) mTrafficData.get(Constants.SIM1TX_N));
                    mTrafficData.put(Constants.SIM2TX_N, (long) mTrafficData.get(Constants.SIM2TX_N));
                    mTrafficData.put(Constants.SIM3TX_N, (long) mTrafficData.get(Constants.SIM3TX_N));
                    mTrafficData.put(Constants.TOTAL1_N, (long) mTrafficData.get(Constants.TOTAL1_N));
                    mTrafficData.put(Constants.TOTAL2_N, (long) mTrafficData.get(Constants.TOTAL2_N));
                    mTrafficData.put(Constants.TOTAL3_N, (long) mTrafficData.get(Constants.TOTAL3_N));
                    mTrafficData.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                    mTrafficData.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
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

                    mLastDate = Constants.DATE_FORMATTER.parseDateTime((String) mTrafficData.get(Constants.LAST_DATE));
                    mNowDate = new DateTime();

                    if (mPrefs.getBoolean(Constants.PREF_SIM1[17], false)) {
                        String timeON = mNowDate.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM1[20], "23:00");
                        String timeOFF = mNowDate.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM1[21], "06:00");
                        mIsNight1 = DateTimeComparator.getInstance().compare(mNowDate, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(mNowDate, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
                    } else
                        mIsNight1 = false;

                    checkIfResetNeeded();

                    boolean emptyDB = CustomDatabaseHelper.isTrafficTableEmpty(mDbHelper);

                    if (emptyDB) {
                        mTrafficData.put(Constants.SIM1RX, 0L);
                        mTrafficData.put(Constants.SIM2RX, 0L);
                        mTrafficData.put(Constants.SIM3RX, 0L);
                        mTrafficData.put(Constants.SIM1TX, 0L);
                        mTrafficData.put(Constants.SIM2TX, 0L);
                        mTrafficData.put(Constants.SIM3TX, 0L);
                        mTrafficData.put(Constants.TOTAL1, 0L);
                        mTrafficData.put(Constants.TOTAL2, 0L);
                        mTrafficData.put(Constants.TOTAL3, 0L);
                        mTrafficData.put(Constants.SIM1RX_N, 0L);
                        mTrafficData.put(Constants.SIM2RX_N, 0L);
                        mTrafficData.put(Constants.SIM3RX_N, 0L);
                        mTrafficData.put(Constants.SIM1TX_N, 0L);
                        mTrafficData.put(Constants.SIM2TX_N, 0L);
                        mTrafficData.put(Constants.SIM3TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL1_N, 0L);
                        mTrafficData.put(Constants.TOTAL2_N, 0L);
                        mTrafficData.put(Constants.TOTAL3_N, 0L);
                        mTrafficData.put(Constants.LAST_RX, 0L);
                        mTrafficData.put(Constants.LAST_TX, 0L);
                        mTrafficData.put(Constants.LAST_TIME, "");
                        mTrafficData.put(Constants.LAST_DATE, "");
                    } else if ((mIsResetNeeded1 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0)
                            || (mIsResetNeeded2 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0)
                            || (mIsResetNeeded3 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0)) {
                        mTrafficData.put(Constants.LAST_TIME, mNowDate.toString(Constants.TIME_FORMATTER));
                        mTrafficData.put(Constants.LAST_DATE, mNowDate.toString(Constants.DATE_FORMATTER));
                        if (mIsResetNeeded1 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0) {
                            mTrafficData.put(Constants.SIM1RX, 0L);
                            mTrafficData.put(Constants.SIM1TX, 0L);
                            mTrafficData.put(Constants.TOTAL1, 0L);
                            mTrafficData.put(Constants.SIM1RX_N, 0L);
                            mTrafficData.put(Constants.SIM1TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL1_N, 0L);
                            rx = tx = mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mHasActionChosen1 = false;
                            mFlashPreOverLimit[Constants.SIM1] = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM1);
                        }
                        if (mIsResetNeeded2 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0) {
                            mTrafficData.put(Constants.SIM2RX, 0L);
                            mTrafficData.put(Constants.SIM2TX, 0L);
                            mTrafficData.put(Constants.TOTAL2, 0L);
                            mTrafficData.put(Constants.SIM2RX_N, 0L);
                            mTrafficData.put(Constants.SIM2TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL2_N, 0L);
                            if (!mIsNight1) {
                                rx = (long) mTrafficData.get(Constants.SIM1RX);
                                tx = (long) mTrafficData.get(Constants.SIM1TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL1);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM1RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM1TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL1_N);
                            }
                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mHasActionChosen2 = false;
                            mFlashPreOverLimit[Constants.SIM2] = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM2);
                        }
                        if (mIsResetNeeded3 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0) {
                            mTrafficData.put(Constants.SIM3RX, 0L);
                            mTrafficData.put(Constants.SIM3TX, 0L);
                            mTrafficData.put(Constants.TOTAL3, 0L);
                            mTrafficData.put(Constants.SIM3RX_N, 0L);
                            mTrafficData.put(Constants.SIM3TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL3_N, 0L);
                            if (!mIsNight1) {
                                rx = (long) mTrafficData.get(Constants.SIM1RX);
                                tx = (long) mTrafficData.get(Constants.SIM1TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL1);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM1RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM1TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL1_N);
                            }
                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mHasActionChosen3 = false;
                            mFlashPreOverLimit[Constants.SIM3] = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight1) {
                            rx = (long) mTrafficData.get(Constants.SIM1RX);
                            tx = (long) mTrafficData.get(Constants.SIM1TX);
                            tot = (long) mTrafficData.get(Constants.TOTAL1);
                        } else {
                            rx = (long) mTrafficData.get(Constants.SIM1RX_N);
                            tx = (long) mTrafficData.get(Constants.SIM1TX_N);
                            tot = (long) mTrafficData.get(Constants.TOTAL1_N);
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX1;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX1;
                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX1 = TrafficStats.getMobileRxBytes();
                    mStartTX1 = TrafficStats.getMobileTxBytes();

                    if (mLimitHasChanged) {
                        mLimits = getSIMLimits();
                        mLimitHasChanged = false;
                    }

                    if ((tot <= mLimits[0]) || mSIM1ContinueOverLimit) {
                        mPrefs.edit().putInt(Constants.PREF_OTHER[46], mActiveSIM)
                                .apply();
                        rx += diffrx;
                        tx += difftx;
                        if (mPrefs.getBoolean(Constants.PREF_SIM1[32], false))
                            tot = rx;
                        else
                            tot = tx + rx;
                        mIsSIM1OverLimit = false;
                        if (mPrefs.getBoolean(Constants.PREF_SIM1[29], false) && !mHasPreLimitNotificationShown1 && !mHasActionChosen1) {
                            int left = (int) (100 * (1.0 - (double) tot / (double) mLimits[0]));
                            if (left < Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[30], "0"))) {
                                mHasPreLimitNotificationShown1 = true;
                                mFlashPreOverLimit[Constants.SIM1] = true;
                                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1000);
                                showPreLimitToast(Constants.SIM1);
                            }
                        }
                    } else if (!mHasActionChosen1) {
                        mIsSIM1OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            pushAlertNotification(mActiveSIM);
                        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && CustomApplication.hasRoot()) ||
                                (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && CustomApplication.isOldMtkDevice()))
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
                            mTrafficData.put(Constants.SIM1RX, rx);
                            mTrafficData.put(Constants.SIM1TX, tx);
                            mTrafficData.put(Constants.TOTAL1, tot);
                        } else {
                            mTrafficData.put(Constants.SIM1RX_N, rx);
                            mTrafficData.put(Constants.SIM1TX_N, tx);
                            mTrafficData.put(Constants.TOTAL1_N, tot);
                        }
                        mTrafficData.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        mTrafficData.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());
                        DateTime dateTime = new DateTime();
                        final long MB = 1024 * 1024;
                        if ((diffrx + difftx > MB) || dateTime.get(DateTimeFieldType.secondOfMinute()) == 59
                                || emptyDB) {
                            mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
                            mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
                            writeToDataBase();
                        }
                        if (CustomApplication.isScreenOn()) {
                            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.notify(Constants.STARTED_ID, buildNotification(Constants.SIM1));
                        }
                    }

                    if ((CustomApplication.isActivityVisible() || getWidgetIds().length != 0 || mPrefs.getBoolean(Constants.PREF_OTHER[32], false)) && CustomApplication.isScreenOn())
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
                if (MobileUtils.isMobileDataActive(mContext)
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mTrafficData
                    //begin
                    mTrafficData.put(Constants.SIM1RX, (long) mTrafficData.get(Constants.SIM1RX));
                    mTrafficData.put(Constants.SIM2RX, (long) mTrafficData.get(Constants.SIM2RX));
                    mTrafficData.put(Constants.SIM3RX, (long) mTrafficData.get(Constants.SIM3RX));
                    mTrafficData.put(Constants.SIM1TX, (long) mTrafficData.get(Constants.SIM1TX));
                    mTrafficData.put(Constants.SIM2TX, (long) mTrafficData.get(Constants.SIM2TX));
                    mTrafficData.put(Constants.SIM3TX, (long) mTrafficData.get(Constants.SIM3TX));
                    mTrafficData.put(Constants.TOTAL1, (long) mTrafficData.get(Constants.TOTAL1));
                    mTrafficData.put(Constants.TOTAL2, (long) mTrafficData.get(Constants.TOTAL2));
                    mTrafficData.put(Constants.TOTAL3, (long) mTrafficData.get(Constants.TOTAL3));
                    mTrafficData.put(Constants.SIM1RX_N, (long) mTrafficData.get(Constants.SIM1RX_N));
                    mTrafficData.put(Constants.SIM2RX_N, (long) mTrafficData.get(Constants.SIM2RX_N));
                    mTrafficData.put(Constants.SIM3RX_N, (long) mTrafficData.get(Constants.SIM3RX_N));
                    mTrafficData.put(Constants.SIM1TX_N, (long) mTrafficData.get(Constants.SIM1TX_N));
                    mTrafficData.put(Constants.SIM2TX_N, (long) mTrafficData.get(Constants.SIM2TX_N));
                    mTrafficData.put(Constants.SIM3TX_N, (long) mTrafficData.get(Constants.SIM3TX_N));
                    mTrafficData.put(Constants.TOTAL1_N, (long) mTrafficData.get(Constants.TOTAL1_N));
                    mTrafficData.put(Constants.TOTAL2_N, (long) mTrafficData.get(Constants.TOTAL2_N));
                    mTrafficData.put(Constants.TOTAL3_N, (long) mTrafficData.get(Constants.TOTAL3_N));
                    mTrafficData.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                    mTrafficData.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
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

                    mLastDate = Constants.DATE_FORMATTER.parseDateTime((String) mTrafficData.get(Constants.LAST_DATE));
                    mNowDate = new DateTime();

                    if (mPrefs.getBoolean(Constants.PREF_SIM2[17], false)) {
                        String timeON = mNowDate.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM2[20], "23:00");
                        String timeOFF = mNowDate.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM2[21], "06:00");
                        mIsNight2 = DateTimeComparator.getInstance().compare(mNowDate, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(mNowDate, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
                    } else
                        mIsNight2 = false;

                    checkIfResetNeeded();

                    boolean emptyDB = CustomDatabaseHelper.isTrafficTableEmpty(mDbHelper);

                    if (emptyDB) {
                        mTrafficData.put(Constants.SIM1RX, 0L);
                        mTrafficData.put(Constants.SIM2RX, 0L);
                        mTrafficData.put(Constants.SIM3RX, 0L);
                        mTrafficData.put(Constants.SIM1TX, 0L);
                        mTrafficData.put(Constants.SIM2TX, 0L);
                        mTrafficData.put(Constants.SIM3TX, 0L);
                        mTrafficData.put(Constants.TOTAL1, 0L);
                        mTrafficData.put(Constants.TOTAL2, 0L);
                        mTrafficData.put(Constants.TOTAL3, 0L);
                        mTrafficData.put(Constants.SIM1RX_N, 0L);
                        mTrafficData.put(Constants.SIM2RX_N, 0L);
                        mTrafficData.put(Constants.SIM3RX_N, 0L);
                        mTrafficData.put(Constants.SIM1TX_N, 0L);
                        mTrafficData.put(Constants.SIM2TX_N, 0L);
                        mTrafficData.put(Constants.SIM3TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL1_N, 0L);
                        mTrafficData.put(Constants.TOTAL2_N, 0L);
                        mTrafficData.put(Constants.TOTAL3_N, 0L);
                        mTrafficData.put(Constants.LAST_RX, 0L);
                        mTrafficData.put(Constants.LAST_TX, 0L);
                        mTrafficData.put(Constants.LAST_TIME, "");
                        mTrafficData.put(Constants.LAST_DATE, "");
                    } else if ((mIsResetNeeded1 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0)
                            || (mIsResetNeeded2 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0)
                            || (mIsResetNeeded3 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0)) {
                        mTrafficData.put(Constants.LAST_TIME, mNowDate.toString(Constants.TIME_FORMATTER));
                        mTrafficData.put(Constants.LAST_DATE, mNowDate.toString(Constants.DATE_FORMATTER));
                        if (mIsResetNeeded1 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0) {
                            mTrafficData.put(Constants.SIM1RX, 0L);
                            mTrafficData.put(Constants.SIM1TX, 0L);
                            mTrafficData.put(Constants.TOTAL1, 0L);
                            mTrafficData.put(Constants.SIM1RX_N, 0L);
                            mTrafficData.put(Constants.SIM1TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL1_N, 0L);
                            if (!mIsNight2) {
                                rx = (long) mTrafficData.get(Constants.SIM2RX);
                                tx = (long) mTrafficData.get(Constants.SIM2TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL2);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM2RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM2TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL2_N);
                            }
                            mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mHasActionChosen1 = false;
                            mFlashPreOverLimit[Constants.SIM1] = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM1);
                        }
                        if (mIsResetNeeded2 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0) {
                            mTrafficData.put(Constants.SIM2RX, 0L);
                            mTrafficData.put(Constants.SIM2TX, 0L);
                            mTrafficData.put(Constants.TOTAL2, 0L);
                            mTrafficData.put(Constants.SIM2RX_N, 0L);
                            mTrafficData.put(Constants.SIM2TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL2_N, 0L);
                            rx = tx = mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mHasActionChosen2 = false;
                            mFlashPreOverLimit[Constants.SIM2] = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM2);
                        }
                        if (mIsResetNeeded3 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0) {
                            mTrafficData.put(Constants.SIM3RX, 0L);
                            mTrafficData.put(Constants.SIM3TX, 0L);
                            mTrafficData.put(Constants.TOTAL3, 0L);
                            mTrafficData.put(Constants.SIM3RX_N, 0L);
                            mTrafficData.put(Constants.SIM3TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL3_N, 0L);
                            if (!mIsNight2) {
                                rx = (long) mTrafficData.get(Constants.SIM2RX);
                                tx = (long) mTrafficData.get(Constants.SIM2TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL2);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM2RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM2TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL2_N);
                            }
                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mHasActionChosen3 = false;
                            mFlashPreOverLimit[Constants.SIM3] = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight2) {
                            rx = (long) mTrafficData.get(Constants.SIM2RX);
                            tx = (long) mTrafficData.get(Constants.SIM2TX);
                            tot = (long) mTrafficData.get(Constants.TOTAL2);
                        } else {
                            rx = (long) mTrafficData.get(Constants.SIM2RX_N);
                            tx = (long) mTrafficData.get(Constants.SIM2TX_N);
                            tot = (long) mTrafficData.get(Constants.TOTAL2_N);
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX2;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX2;
                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX2 = TrafficStats.getMobileRxBytes();
                    mStartTX2 = TrafficStats.getMobileTxBytes();

                    if (mLimitHasChanged) {
                        mLimits = getSIMLimits();
                        mLimitHasChanged = false;
                    }

                    if ((tot <= mLimits[1]) || mSIM2ContinueOverLimit) {
                        mPrefs.edit().putInt(Constants.PREF_OTHER[46], mActiveSIM)
                                .apply();
                        rx += diffrx;
                        tx += difftx;
                        if (mPrefs.getBoolean(Constants.PREF_SIM2[32], false))
                            tot = rx;
                        else
                            tot = tx + rx;
                        mIsSIM2OverLimit = false;
                        if (mPrefs.getBoolean(Constants.PREF_SIM2[29], false) && !mHasPreLimitNotificationShown2 && !mHasActionChosen2) {
                            int left = (int) (100 * (1.0 - (double) tot / (double) mLimits[1]));
                            if (left < Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[30], "0"))) {
                                mHasPreLimitNotificationShown2 = true;
                                mFlashPreOverLimit[Constants.SIM2] = true;
                                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1000);
                                showPreLimitToast(Constants.SIM2);
                            }
                        }
                    } else if (!mHasActionChosen2) {
                        mIsSIM2OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            pushAlertNotification(mActiveSIM);
                        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && CustomApplication.hasRoot()) ||
                                (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && CustomApplication.isOldMtkDevice()))
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
                            mTrafficData.put(Constants.SIM2RX, rx);
                            mTrafficData.put(Constants.SIM2TX, tx);
                            mTrafficData.put(Constants.TOTAL2, tot);
                        } else {
                            mTrafficData.put(Constants.SIM2RX_N, rx);
                            mTrafficData.put(Constants.SIM2TX_N, tx);
                            mTrafficData.put(Constants.TOTAL2_N, tot);
                        }
                        mTrafficData.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        mTrafficData.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());
                        DateTime dateTime = new DateTime();
                        final long MB = 1024 * 1024;
                        if ((diffrx + difftx > MB) || dateTime.get(DateTimeFieldType.secondOfMinute()) == 59
                                || emptyDB) {
                            mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
                            mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
                            writeToDataBase();
                        }
                        if (CustomApplication.isScreenOn()) {
                            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.notify(Constants.STARTED_ID, buildNotification(Constants.SIM2));
                        }
                    }

                    if ((CustomApplication.isActivityVisible() || getWidgetIds().length != 0 || mPrefs.getBoolean(Constants.PREF_OTHER[32], false)) && CustomApplication.isScreenOn())
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
                if (MobileUtils.isMobileDataActive(mContext)
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mTrafficData
                    //begin
                    mTrafficData.put(Constants.SIM1RX, (long) mTrafficData.get(Constants.SIM1RX));
                    mTrafficData.put(Constants.SIM2RX, (long) mTrafficData.get(Constants.SIM2RX));
                    mTrafficData.put(Constants.SIM3RX, (long) mTrafficData.get(Constants.SIM3RX));
                    mTrafficData.put(Constants.SIM1TX, (long) mTrafficData.get(Constants.SIM1TX));
                    mTrafficData.put(Constants.SIM2TX, (long) mTrafficData.get(Constants.SIM2TX));
                    mTrafficData.put(Constants.SIM3TX, (long) mTrafficData.get(Constants.SIM3TX));
                    mTrafficData.put(Constants.TOTAL1, (long) mTrafficData.get(Constants.TOTAL1));
                    mTrafficData.put(Constants.TOTAL2, (long) mTrafficData.get(Constants.TOTAL2));
                    mTrafficData.put(Constants.TOTAL3, (long) mTrafficData.get(Constants.TOTAL3));
                    mTrafficData.put(Constants.SIM1RX_N, (long) mTrafficData.get(Constants.SIM1RX_N));
                    mTrafficData.put(Constants.SIM2RX_N, (long) mTrafficData.get(Constants.SIM2RX_N));
                    mTrafficData.put(Constants.SIM3RX_N, (long) mTrafficData.get(Constants.SIM3RX_N));
                    mTrafficData.put(Constants.SIM1TX_N, (long) mTrafficData.get(Constants.SIM1TX_N));
                    mTrafficData.put(Constants.SIM2TX_N, (long) mTrafficData.get(Constants.SIM2TX_N));
                    mTrafficData.put(Constants.SIM3TX_N, (long) mTrafficData.get(Constants.SIM3TX_N));
                    mTrafficData.put(Constants.TOTAL1_N, (long) mTrafficData.get(Constants.TOTAL1_N));
                    mTrafficData.put(Constants.TOTAL2_N, (long) mTrafficData.get(Constants.TOTAL2_N));
                    mTrafficData.put(Constants.TOTAL3_N, (long) mTrafficData.get(Constants.TOTAL3_N));
                    mTrafficData.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                    mTrafficData.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
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

                    mLastDate = Constants.DATE_FORMATTER.parseDateTime((String) mTrafficData.get(Constants.LAST_DATE));
                    mNowDate = new DateTime();

                    if (mPrefs.getBoolean(Constants.PREF_SIM3[17], false)) {
                        String timeON = mNowDate.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM3[20], "23:00");
                        String timeOFF = mNowDate.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM3[21], "06:00");
                        mIsNight3 = DateTimeComparator.getInstance().compare(mNowDate, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(mNowDate, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
                    } else
                        mIsNight3 = false;

                    checkIfResetNeeded();

                    boolean emptyDB = CustomDatabaseHelper.isTrafficTableEmpty(mDbHelper);

                    if (emptyDB) {
                        mTrafficData.put(Constants.SIM1RX, 0L);
                        mTrafficData.put(Constants.SIM2RX, 0L);
                        mTrafficData.put(Constants.SIM3RX, 0L);
                        mTrafficData.put(Constants.SIM1TX, 0L);
                        mTrafficData.put(Constants.SIM2TX, 0L);
                        mTrafficData.put(Constants.SIM3TX, 0L);
                        mTrafficData.put(Constants.TOTAL1, 0L);
                        mTrafficData.put(Constants.TOTAL2, 0L);
                        mTrafficData.put(Constants.TOTAL3, 0L);
                        mTrafficData.put(Constants.SIM1RX_N, 0L);
                        mTrafficData.put(Constants.SIM2RX_N, 0L);
                        mTrafficData.put(Constants.SIM3RX_N, 0L);
                        mTrafficData.put(Constants.SIM1TX_N, 0L);
                        mTrafficData.put(Constants.SIM2TX_N, 0L);
                        mTrafficData.put(Constants.SIM3TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL1_N, 0L);
                        mTrafficData.put(Constants.TOTAL2_N, 0L);
                        mTrafficData.put(Constants.TOTAL3_N, 0L);
                        mTrafficData.put(Constants.LAST_RX, 0L);
                        mTrafficData.put(Constants.LAST_TX, 0L);
                        mTrafficData.put(Constants.LAST_TIME, "");
                        mTrafficData.put(Constants.LAST_DATE, "");
                    } else if ((mIsResetNeeded1 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0)
                            || (mIsResetNeeded2 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0)
                            || (mIsResetNeeded3 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0)) {
                        mTrafficData.put(Constants.LAST_TIME, mNowDate.toString(Constants.TIME_FORMATTER));
                        mTrafficData.put(Constants.LAST_DATE, mNowDate.toString(Constants.DATE_FORMATTER));
                        if (mIsResetNeeded1 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime1) >= 0) {
                            mTrafficData.put(Constants.SIM1RX, 0L);
                            mTrafficData.put(Constants.SIM1TX, 0L);
                            mTrafficData.put(Constants.TOTAL1, 0L);
                            mTrafficData.put(Constants.SIM1RX_N, 0L);
                            mTrafficData.put(Constants.SIM1TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL1_N, 0L);
                            if (!mIsNight3) {
                                rx = (long) mTrafficData.get(Constants.SIM3RX);
                                tx = (long) mTrafficData.get(Constants.SIM3TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL3);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM3RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM3TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL3_N);
                            }
                            mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mHasActionChosen1 = false;
                            mFlashPreOverLimit[Constants.SIM1] = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM1);
                        }
                        if (mIsResetNeeded2 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime2) >= 0) {
                            mTrafficData.put(Constants.SIM2RX, 0L);
                            mTrafficData.put(Constants.SIM2TX, 0L);
                            mTrafficData.put(Constants.TOTAL2, 0L);
                            mTrafficData.put(Constants.SIM2RX_N, 0L);
                            mTrafficData.put(Constants.SIM2TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL2_N, 0L);
                            if (!mIsNight3) {
                                rx = (long) mTrafficData.get(Constants.SIM3RX);
                                tx = (long) mTrafficData.get(Constants.SIM3TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL3);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM3RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM3TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL3_N);
                            }
                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mHasActionChosen2 = false;
                            mFlashPreOverLimit[Constants.SIM2] = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM2);
                        }
                        if (mIsResetNeeded3 && DateTimeComparator.getInstance().compare(mNowDate, mResetTime3) >= 0) {
                            mTrafficData.put(Constants.SIM3RX, 0L);
                            mTrafficData.put(Constants.SIM3TX, 0L);
                            mTrafficData.put(Constants.TOTAL3, 0L);
                            mTrafficData.put(Constants.SIM3RX_N, 0L);
                            mTrafficData.put(Constants.SIM3TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL3_N, 0L);
                            rx = tx = mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mHasActionChosen3 = false;
                            mFlashPreOverLimit[Constants.SIM3] = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight2) {
                            rx = (long) mTrafficData.get(Constants.SIM3RX);
                            tx = (long) mTrafficData.get(Constants.SIM3TX);
                            tot = (long) mTrafficData.get(Constants.TOTAL3);
                        } else {
                            rx = (long) mTrafficData.get(Constants.SIM3RX_N);
                            tx = (long) mTrafficData.get(Constants.SIM3TX_N);
                            tot = (long) mTrafficData.get(Constants.TOTAL3_N);
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX3;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX3;
                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX3 = TrafficStats.getMobileRxBytes();
                    mStartTX3 = TrafficStats.getMobileTxBytes();

                    if (mLimitHasChanged) {
                        mLimits = getSIMLimits();
                        mLimitHasChanged = false;
                    }

                    if ((tot <= mLimits[2]) || mSIM3ContinueOverLimit) {
                        mPrefs.edit().putInt(Constants.PREF_OTHER[46], mActiveSIM)
                                .apply();
                        rx += diffrx;
                        tx += difftx;
                        if (mPrefs.getBoolean(Constants.PREF_SIM3[32], false))
                            tot = rx;
                        else
                            tot = tx + rx;
                        mIsSIM3OverLimit = false;
                        if (mPrefs.getBoolean(Constants.PREF_SIM3[29], false) && !mHasPreLimitNotificationShown3 && !mHasActionChosen3) {
                            int left = (int) (100 * (1.0 - (double) tot / (double) mLimits[2]));
                            if (left < Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[30], "0"))) {
                                mHasPreLimitNotificationShown3 = true;
                                mFlashPreOverLimit[Constants.SIM3] = true;
                                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1000);
                                showPreLimitToast(Constants.SIM3);
                            }
                        }
                    } else if (!mHasActionChosen3) {
                        mIsSIM3OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            pushAlertNotification(mActiveSIM);
                        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && CustomApplication.hasRoot()) ||
                                (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && CustomApplication.isOldMtkDevice()))
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
                            mTrafficData.put(Constants.SIM3RX, rx);
                            mTrafficData.put(Constants.SIM3TX, tx);
                            mTrafficData.put(Constants.TOTAL3, tot);
                        } else {
                            mTrafficData.put(Constants.SIM3RX_N, rx);
                            mTrafficData.put(Constants.SIM3TX_N, tx);
                            mTrafficData.put(Constants.TOTAL3_N, tot);
                        }
                        mTrafficData.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
                        mTrafficData.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());
                        DateTime dateTime = new DateTime();
                        final long MB = 1024 * 1024;
                        if ((diffrx + difftx > MB) || dateTime.get(DateTimeFieldType.secondOfMinute()) == 59
                                || emptyDB) {
                            mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
                            mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
                            writeToDataBase();
                        }
                        if (CustomApplication.isScreenOn()) {
                            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            nm.notify(Constants.STARTED_ID, buildNotification(Constants.SIM3));
                        }
                    }

                    if ((CustomApplication.isActivityVisible() || getWidgetIds().length != 0 || mPrefs.getBoolean(Constants.PREF_OTHER[32], false)) && CustomApplication.isScreenOn())
                        sendDataBroadcast(speedRX, speedTX);
                }

            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private void showPreLimitToast(final int sim) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, String.format(getResources().getString(R.string.pre_limit), mOperatorNames[sim]), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void pushResetNotification(int simID) {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alert);
        String text = "";
        int id;
        if (mPrefs.getBoolean(Constants.PREF_OTHER[15], false)) {
            String[] pref = new String[Constants.PREF_SIM1.length];
            switch (simID) {
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
                id = getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(mContext, simID), "drawable", mContext.getPackageName());
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
        nm.notify(simID + 1977, builder.build());
    }

    private void writeToDataBase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            ContentValues cv = new ContentValues();
            cv.put("rx", (long) mTrafficData.get(Constants.SIM1RX));
            cv.put("tx", (long) mTrafficData.get(Constants.SIM1TX));
            cv.put("total", (long) mTrafficData.get(Constants.TOTAL1));
            cv.put("rx_n", (long) mTrafficData.get(Constants.SIM1RX_N));
            cv.put("tx_n", (long) mTrafficData.get(Constants.SIM1TX_N));
            cv.put("total_n", (long) mTrafficData.get(Constants.TOTAL1_N));
            cv.put("period", (int) mTrafficData.get(Constants.PERIOD1));
            cv.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
            cv.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
            CustomDatabaseHelper.writeTrafficDataForSim(cv, mDbHelper, mIMSI.get(0));
            if (mSimQuantity >= 2) {
                cv = new ContentValues();;
                cv.put("rx", (long) mTrafficData.get(Constants.SIM2RX));
                cv.put("tx", (long) mTrafficData.get(Constants.SIM2TX));
                cv.put("total", (long) mTrafficData.get(Constants.TOTAL2));
                cv.put("rx_n", (long) mTrafficData.get(Constants.SIM2RX_N));
                cv.put("tx_n", (long) mTrafficData.get(Constants.SIM2TX_N));
                cv.put("total_n", (long) mTrafficData.get(Constants.TOTAL2_N));
                cv.put("period", (int) mTrafficData.get(Constants.PERIOD2));
                cv.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                cv.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
                CustomDatabaseHelper.writeTrafficDataForSim(cv, mDbHelper, mIMSI.get(1));
            }
            if (mSimQuantity == 3) {
                cv = new ContentValues();;
                cv.put("rx", (long) mTrafficData.get(Constants.SIM3RX));
                cv.put("tx", (long) mTrafficData.get(Constants.SIM3TX));
                cv.put("total", (long) mTrafficData.get(Constants.TOTAL3));
                cv.put("rx_n", (long) mTrafficData.get(Constants.SIM3RX_N));
                cv.put("tx_n", (long) mTrafficData.get(Constants.SIM3TX_N));
                cv.put("total_n", (long) mTrafficData.get(Constants.TOTAL3_N));
                cv.put("period", (int) mTrafficData.get(Constants.PERIOD3));
                cv.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                cv.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
                CustomDatabaseHelper.writeTrafficDataForSim(cv, mDbHelper, mIMSI.get(2));
            }
        } else
            CustomDatabaseHelper.writeTrafficData(mTrafficData, mDbHelper);
    }

    private void sendDataBroadcast(long speedRX, long speedTX) {
        Intent intent = new Intent(Constants.TRAFFIC_BROADCAST_ACTION);
        intent.putExtra(Constants.WIDGET_IDS, getWidgetIds());
        intent.putExtra(Constants.SPEEDRX, speedRX);
        intent.putExtra(Constants.SPEEDTX, speedTX);
        intent.putExtra(Constants.SIM1RX, (long) mTrafficData.get(Constants.SIM1RX));
        intent.putExtra(Constants.SIM1TX, (long) mTrafficData.get(Constants.SIM1TX));
        intent.putExtra(Constants.TOTAL1, (long) mTrafficData.get(Constants.TOTAL1));
        intent.putExtra(Constants.SIM1RX_N, (long) mTrafficData.get(Constants.SIM1RX_N));
        intent.putExtra(Constants.SIM1TX_N, (long) mTrafficData.get(Constants.SIM1TX_N));
        intent.putExtra(Constants.TOTAL1_N, (long) mTrafficData.get(Constants.TOTAL1_N));
        if (mSimQuantity >= 2) {
            intent.putExtra(Constants.SIM2RX, (long) mTrafficData.get(Constants.SIM2RX));
            intent.putExtra(Constants.SIM2TX, (long) mTrafficData.get(Constants.SIM2TX));
            intent.putExtra(Constants.TOTAL2, (long) mTrafficData.get(Constants.TOTAL2));
            intent.putExtra(Constants.SIM2RX_N, (long) mTrafficData.get(Constants.SIM2RX_N));
            intent.putExtra(Constants.SIM2TX_N, (long) mTrafficData.get(Constants.SIM2TX_N));
            intent.putExtra(Constants.TOTAL2_N, (long) mTrafficData.get(Constants.TOTAL2_N));
        }
        if (mSimQuantity == 3) {
            intent.putExtra(Constants.SIM3RX, (long) mTrafficData.get(Constants.SIM3RX));
            intent.putExtra(Constants.SIM3TX, (long) mTrafficData.get(Constants.SIM3TX));
            intent.putExtra(Constants.TOTAL3, (long) mTrafficData.get(Constants.TOTAL3));
            intent.putExtra(Constants.SIM3RX_N, (long) mTrafficData.get(Constants.SIM3RX_N));
            intent.putExtra(Constants.SIM3TX_N, (long) mTrafficData.get(Constants.SIM3TX_N));
            intent.putExtra(Constants.TOTAL3_N, (long) mTrafficData.get(Constants.TOTAL3_N));
        }
        if (mActiveSIM == Constants.DISABLED)
            intent.putExtra(Constants.SIM_ACTIVE, mLastActiveSIM);
        else
            intent.putExtra(Constants.SIM_ACTIVE, mActiveSIM);
        intent.putExtra(Constants.OPERATOR1, mOperatorNames[0]);
        intent.putExtra(Constants.OPERATOR2, mOperatorNames[1]);
        intent.putExtra(Constants.OPERATOR3, mOperatorNames[2]);
        mContext.sendBroadcast(intent);
        if (mPrefs.getBoolean(Constants.PREF_OTHER[32], false) && mActiveSIM != Constants.DISABLED) {
            long total = 0;
            switch (mActiveSIM) {
                case Constants.SIM1:
                    if (mIsNight1)
                        total = (long) mTrafficData.get(Constants.TOTAL1_N);
                    else
                        total = (long) mTrafficData.get(Constants.TOTAL1);
                    break;
                case Constants.SIM2:
                    if (mIsNight2)
                        total = (long) mTrafficData.get(Constants.TOTAL2_N);
                    else
                        total = (long) mTrafficData.get(Constants.TOTAL2);
                    break;
                case Constants.SIM3:
                    if (mIsNight3)
                        total = (long) mTrafficData.get(Constants.TOTAL3_N);
                    else
                        total = (long) mTrafficData.get(Constants.TOTAL3);
                    break;
            }
            if (mPrefs.getBoolean(Constants.PREF_OTHER[39], false)) {
                total = mLimits[mActiveSIM] - total;
                if (total < 0)
                    total = 0;
            }
            Bundle bundle = new Bundle();
            bundle.putLong("total", total);
            bundle.putBoolean("flash", mFlashPreOverLimit[mActiveSIM]);
            bundle.putLong(Constants.SPEEDRX, speedRX);
            bundle.putLong(Constants.SPEEDTX, speedTX);
            if (!CustomApplication.isMyServiceRunning(FloatingWindowService.class))
                FloatingWindowService.showFloatingWindow(mContext, mPrefs);
            StandOutWindow.sendData(mContext, FloatingWindowService.class,
                    mPrefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID), Constants.FLOATING_WINDOW, bundle, null, -1);
        }
    }

    private Notification buildNotification(int sim) {
        String text = "";
        long tot1, tot2 = 0, tot3 = 0;
        if (mPrefs.getBoolean(Constants.PREF_OTHER[19], false)) {
            if (mLimitHasChanged) {
                mLimits = getSIMLimits();
                mLimitHasChanged = false;
            }
            tot1 = mIsNight1 ? mLimits[0] - (long) mTrafficData.get(Constants.TOTAL1_N) : mLimits[0] - (long) mTrafficData.get(Constants.TOTAL1);
            if (tot1 < 0)
                tot1 = 0;
            if (mSimQuantity >= 2) {
                tot2 = mIsNight2 ? mLimits[1] - (long) mTrafficData.get(Constants.TOTAL2_N) : mLimits[1] - (long) mTrafficData.get(Constants.TOTAL2);
                if (tot2 < 0)
                    tot2 = 0;
            }
            if (mSimQuantity == 3) {
                tot3 = mIsNight3 ? mLimits[2] - (long) mTrafficData.get(Constants.TOTAL3_N) : mLimits[2] - (long) mTrafficData.get(Constants.TOTAL3);
                if (tot3 < 0)
                    tot3 = 0;
            }
        } else {
            tot1 = mIsNight1 ? (long) mTrafficData.get(Constants.TOTAL1_N) : (long) mTrafficData.get(Constants.TOTAL1);
            tot2 = mIsNight2 ? (long) mTrafficData.get(Constants.TOTAL2_N) : (long) mTrafficData.get(Constants.TOTAL2);
            tot3 = mIsNight3 ? (long) mTrafficData.get(Constants.TOTAL3_N) : (long) mTrafficData.get(Constants.TOTAL3);
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
        return CustomNotification.getNotification(mContext, text, "");
    }

    private void startCheck(int sim) {

        String[] keys = new String[Constants.PREF_SIM_DATA.length];
        switch (sim) {
            case Constants.SIM1:
                keys = Constants.PREF_SIM1;
                break;
            case Constants.SIM2:
                keys = Constants.PREF_SIM2;
                break;
            case Constants.SIM3:
                keys = Constants.PREF_SIM3;
                break;
        }
        if (mPrefs.getBoolean(keys[7], false)) {
            try {
                MobileUtils.toggleMobileDataConnection(false, mContext, Constants.DISABLED);
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }

        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }

        boolean choice = false;

        if (((sim == Constants.SIM1 && mPrefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (sim == Constants.SIM2 && mPrefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (sim == Constants.SIM3 && mPrefs.getBoolean(Constants.PREF_SIM3[7], true))) &&
                mPrefs.getBoolean(Constants.PREF_OTHER[10], true)) {
            try {
                if (!mIsSIM2OverLimit && sim == Constants.SIM1 && mSimQuantity >= 2)
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
                else if (!mIsSIM3OverLimit && sim == Constants.SIM1 && mSimQuantity == 3)
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                else if (!mIsSIM1OverLimit && sim == Constants.SIM2)
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                else if (!mIsSIM3OverLimit && sim == Constants.SIM2 && mSimQuantity == 3)
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM3);
                else if (!mIsSIM1OverLimit && sim == Constants.SIM3)
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM1);
                else if (!mIsSIM2OverLimit && sim == Constants.SIM3)
                    MobileUtils.toggleMobileDataConnection(true, mContext, Constants.SIM2);
                else
                    choice = true;
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        } else if (((sim == Constants.SIM1 && mPrefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (sim == Constants.SIM2 && mPrefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (sim == Constants.SIM3 && mPrefs.getBoolean(Constants.PREF_SIM3[7], true))) &&
                !mPrefs.getBoolean(Constants.PREF_OTHER[10], true))
            choice = true;
        else if ((sim == Constants.SIM1 && !mPrefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (sim == Constants.SIM2 && !mPrefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (sim == Constants.SIM3 && !mPrefs.getBoolean(Constants.PREF_SIM3[7], true)) ||
                (mIsSIM1OverLimit && mIsSIM2OverLimit && mIsSIM3OverLimit && mPrefs.getBoolean(Constants.PREF_OTHER[10], true))) {
            Intent dialogIntent = new Intent(mContext, ChooseActionDialog.class);
            dialogIntent.putExtra(Constants.SIM_ACTIVE, sim);
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

    private void pushAlertNotification(int alertID) {
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

    private static int[] getWidgetIds() {
        int[] ids = AppWidgetManager.getInstance(mContext).getAppWidgetIds(new ComponentName(mContext, TrafficInfoWidget.class));
        if (ids.length == 0) {
            try {
                File dir = new File(mContext.getFilesDir().getParent() + "/shared_prefs/");
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
                .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                .putInt(Constants.PREF_OTHER[46], mLastActiveSIM)
                .apply();
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(Constants.STARTED_ID);
        writeToDataBase();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
    }
}