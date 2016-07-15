package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.stericson.RootShell.RootShell;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.receivers.OnOffReceiver;
import ua.od.acros.dualsimtrafficcounter.receivers.ResetReceiver;

@ReportsCrashes(mailTo = "acras1@gmail.com",
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.BUILD, ReportField.BRAND,
                ReportField.SETTINGS_GLOBAL, ReportField.SETTINGS_SYSTEM,
                ReportField.STACK_TRACE, ReportField.LOGCAT, ReportField.SHARED_PREFERENCES },
        logcatArguments = { "-t", "300", "MyAppTag:V", "System.err:V", "AndroidRuntime:V", "*:S" },
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_toast_text,
        resDialogOkToast = R.string.crash_toast_text_ok)
public class CustomApplication extends Application {

    private static Context mContext;
    private static Boolean mIsOldMtkDevice = null;
    private static Boolean mHasRoot = null;
    private static Boolean mHasGeminiSupport = null;
    private static boolean mIsActivityVisible;
    private static Intent mSettingsIntent;
    private static boolean mIsDataUsageAvailable = true;

        /*static {
        SharedPreferences prefs = MyApplication.getAppContext().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.PREF_OTHER[29], true))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        else {
            if (prefs.getBoolean(Constants.PREF_OTHER[28], false))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }*/

    @Override
    public void onCreate() {
        super.onCreate();
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        mContext = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        //Check if Data Usage Fragment available
        final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
        mSettingsIntent = new Intent(Intent.ACTION_MAIN);
        mSettingsIntent.setComponent(cn);
        mSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (mContext.getPackageManager().queryIntentActivities(mSettingsIntent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0)
            mIsDataUsageAvailable = false;

        //Reschedule alarms
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        DateTime alarmTime = new DateTime().withTimeAtStartOfDay();
        //Calls reset
        if (prefs.getBoolean(Constants.PREF_OTHER[25], true)) {
            Intent iReset = new Intent(mContext, ResetReceiver.class);
            iReset.setAction(Constants.RESET_ACTION);
            final int RESET = 1981;
            PendingIntent piReset = PendingIntent.getBroadcast(mContext, RESET, iReset, 0);
            if (alarmTime.getMillis() < System.currentTimeMillis())
                alarmTime = alarmTime.plusDays(1);
            am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, piReset);
        }
        //Scheduled ON/OFF
        if (!prefs.getString(Constants.PREF_SIM1[11], "0").equals("3") || !prefs.getString(Constants.PREF_SIM2[11], "0").equals("3")
                || !prefs.getString(Constants.PREF_SIM3[11], "0").equals("3")) {
            if (prefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM1[11], "0").equals("1")) {
                Intent i1Off = new Intent(mContext, OnOffReceiver.class);
                i1Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1Off.putExtra(Constants.ON_OFF, false);
                i1Off.setAction(Constants.ALARM_ACTION);
                final int SIM1_OFF = 100;
                PendingIntent pi1Off = PendingIntent.getBroadcast(mContext, SIM1_OFF, i1Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1Off);
            }
            if (prefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM1[11], "0").equals("2")) {
                Intent i1On = new Intent(mContext, OnOffReceiver.class);
                i1On.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1On.putExtra(Constants.ON_OFF, true);
                i1On.setAction(Constants.ALARM_ACTION);
                final int SIM1_ON = 101;
                PendingIntent pi1On = PendingIntent.getBroadcast(mContext, SIM1_ON, i1On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1On);
            }
            if (prefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM2[11], "0").equals("1")) {
                Intent i2Off = new Intent(mContext, OnOffReceiver.class);
                i2Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2Off.putExtra(Constants.ON_OFF, false);
                i2Off.setAction(Constants.ALARM_ACTION);
                final int SIM2_OFF = 110;
                PendingIntent pi2Off = PendingIntent.getBroadcast(mContext, SIM2_OFF, i2Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2Off);
            }
            if (prefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM2[11], "0").equals("2")) {
                Intent i2On = new Intent(mContext, OnOffReceiver.class);
                i2On.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2On.putExtra(Constants.ON_OFF, true);
                i2On.setAction(Constants.ALARM_ACTION);
                final int SIM2_ON = 111;
                PendingIntent pi2On = PendingIntent.getBroadcast(mContext, SIM2_ON, i2On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2On);
            }
            if (prefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM3[11], "0").equals("1")) {
                Intent i3Off = new Intent(mContext, OnOffReceiver.class);
                i3Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3Off.putExtra(Constants.ON_OFF, false);
                i3Off.setAction(Constants.ALARM_ACTION);
                final int SIM3_OFF = 120;
                PendingIntent pi3Off = PendingIntent.getBroadcast(mContext, SIM3_OFF, i3Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[12], "23:35").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3Off);
            }
            if (prefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM3[11], "0").equals("2")) {
                Intent i3On = new Intent(mContext, OnOffReceiver.class);
                i3On.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3On.putExtra(Constants.ON_OFF, true);
                i3On.setAction(Constants.ALARM_ACTION);
                final int SIM3_ON = 121;
                PendingIntent pi3On = PendingIntent.getBroadcast(mContext, SIM3_ON, i3On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3On);
            }
        }
    }

    public static Intent getSettingsIntent() {
        return mSettingsIntent;
    }

    public static boolean isDataUsageAvailable() {
        return mIsDataUsageAvailable;
    }

    public static Context getAppContext() {
        return  mContext;
    }

    public static boolean isActivityVisible() {
        return mIsActivityVisible;
    }

    public static void activityResumed() {
        mIsActivityVisible = true;
    }

    public static void activityPaused() {
        mIsActivityVisible = false;
    }

    public static boolean isScreenOn() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
            return pm.isInteractive();
        else
            return pm.isScreenOn();
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName()))
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static boolean isPackageExisted(String targetPackage){
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    // Supported MTK devices
    private static final Set<String> OLD_MTK_DEVICES = new HashSet<>(Arrays.asList(
            new String[]{
                    // Single-core SoC
                    "mt6575",
                    // Dual-core SoC
                    "mt6572",
                    "mt6577",
                    "mt8377",
                    // Quad-core SoC
                    "mt6582",
                    "mt6582m",
                    "mt6589",
                    "mt8389",
                    // Octa-core SoC
                    "mt6592"
            }
    ));

    public static boolean isOldMtkDevice() {
        if (mIsOldMtkDevice == null)
            mIsOldMtkDevice = OLD_MTK_DEVICES.contains(Build.HARDWARE.toLowerCase()) ||
                    OLD_MTK_DEVICES.contains(System.getProperty("ro.mediatek.platform", "")) ||
                    OLD_MTK_DEVICES.contains(System.getProperty("ro.board.platform", ""));
        return mIsOldMtkDevice;
    }

    public static boolean hasGeminiSupport() {
        if (mHasGeminiSupport == null)
            mHasGeminiSupport = System.getProperty("ro.mediatek.gemini_support", "").equals("true") &&
                    Build.VERSION.SDK_INT < 22;
        return mHasGeminiSupport;
    }

    public static boolean hasRoot() {
        if (mHasRoot == null)
            mHasRoot = RootShell.isRootAvailable() && RootShell.isAccessGiven();
        return mHasRoot;
    }
}
