package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
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
import org.joda.time.DateTimeComparator;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.receivers.OnOffReceiver;
import ua.od.acros.dualsimtrafficcounter.receivers.ResetReceiver;
import ua.od.acros.dualsimtrafficcounter.widgets.CallsInfoWidget;
import ua.od.acros.dualsimtrafficcounter.widgets.TrafficInfoWidget;

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
    private static SharedPreferences mPrefs;

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
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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
        if (mPrefs.getBoolean(Constants.PREF_OTHER[25], true)) {
            Intent iReset = new Intent(mContext, ResetReceiver.class);
            iReset.setAction(Constants.RESET_ACTION);
            final int RESET = 1981;
            PendingIntent piReset = PendingIntent.getBroadcast(mContext, RESET, iReset, 0);
            if (alarmTime.getMillis() < System.currentTimeMillis())
                alarmTime = alarmTime.plusDays(1);
            am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, piReset);
        }
        //Scheduled ON/OFF
        if (!mPrefs.getString(Constants.PREF_SIM1[11], "0").equals("3") || !mPrefs.getString(Constants.PREF_SIM2[11], "0").equals("3")
                || !mPrefs.getString(Constants.PREF_SIM3[11], "0").equals("3")) {
            if (mPrefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    mPrefs.getString(Constants.PREF_SIM1[11], "0").equals("1")) {
                Intent i1Off = new Intent(mContext, OnOffReceiver.class);
                i1Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1Off.putExtra(Constants.ON_OFF, false);
                i1Off.setAction(Constants.ALARM_ACTION);
                final int SIM1_OFF = 100;
                PendingIntent pi1Off = PendingIntent.getBroadcast(mContext, SIM1_OFF, i1Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1Off);
            }
            if (mPrefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    mPrefs.getString(Constants.PREF_SIM1[11], "0").equals("2")) {
                Intent i1On = new Intent(mContext, OnOffReceiver.class);
                i1On.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1On.putExtra(Constants.ON_OFF, true);
                i1On.setAction(Constants.ALARM_ACTION);
                final int SIM1_ON = 101;
                PendingIntent pi1On = PendingIntent.getBroadcast(mContext, SIM1_ON, i1On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1On);
            }
            if (mPrefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    mPrefs.getString(Constants.PREF_SIM2[11], "0").equals("1")) {
                Intent i2Off = new Intent(mContext, OnOffReceiver.class);
                i2Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2Off.putExtra(Constants.ON_OFF, false);
                i2Off.setAction(Constants.ALARM_ACTION);
                final int SIM2_OFF = 110;
                PendingIntent pi2Off = PendingIntent.getBroadcast(mContext, SIM2_OFF, i2Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2Off);
            }
            if (mPrefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    mPrefs.getString(Constants.PREF_SIM2[11], "0").equals("2")) {
                Intent i2On = new Intent(mContext, OnOffReceiver.class);
                i2On.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2On.putExtra(Constants.ON_OFF, true);
                i2On.setAction(Constants.ALARM_ACTION);
                final int SIM2_ON = 111;
                PendingIntent pi2On = PendingIntent.getBroadcast(mContext, SIM2_ON, i2On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2On);
            }
            if (mPrefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    mPrefs.getString(Constants.PREF_SIM3[11], "0").equals("1")) {
                Intent i3Off = new Intent(mContext, OnOffReceiver.class);
                i3Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3Off.putExtra(Constants.ON_OFF, false);
                i3Off.setAction(Constants.ALARM_ACTION);
                final int SIM3_OFF = 120;
                PendingIntent pi3Off = PendingIntent.getBroadcast(mContext, SIM3_OFF, i3Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[12], "23:35").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3Off);
            }
            if (mPrefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    mPrefs.getString(Constants.PREF_SIM3[11], "0").equals("2")) {
                Intent i3On = new Intent(mContext, OnOffReceiver.class);
                i3On.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3On.putExtra(Constants.ON_OFF, true);
                i3On.setAction(Constants.ALARM_ACTION);
                final int SIM3_ON = 121;
                PendingIntent pi3On = PendingIntent.getBroadcast(mContext, SIM3_ON, i3On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[1]))
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

    public static void isActivityResumed() {
        mIsActivityVisible = true;
    }

    public static void isActivityPaused() {
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

    public static void putObject(SharedPreferences.Editor editor, String key, Object o) {
        if (o == null)
            editor.putString(key, "null");
        else if (o instanceof String)
            editor.putString(key, (String) o);
        else if (o instanceof Boolean)
            editor.putBoolean(key, (boolean) o);
    }

    public static int[] getWidgetIds(String name) {
        Class c;
        if (name.equals(Constants.CALLS_TABLE))
            c = CallsInfoWidget.class;
        else
            c = TrafficInfoWidget.class;
        int[] ids = AppWidgetManager.getInstance(mContext).getAppWidgetIds(new ComponentName(mContext, c));
        if (ids.length == 0) {
            try {
                File dir = new File(mContext.getFilesDir().getParent() + "/shared_prefs/");
                String[] children = dir.list();
                int i = 0;
                for (String aChildren : children) {
                    String[] str = aChildren.split("_");
                    if (str.length > 0 && str[1].equalsIgnoreCase(name) && str[2].equalsIgnoreCase("widget")) {
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

    public static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static long[] getCallsSimLimitsValues() {
        long limit1, limit2, limit3;
        try {
            limit1 = Long.valueOf(mPrefs.getString(Constants.PREF_SIM1_CALLS[1], "0")) * Constants.MINUTE;
        } catch (Exception e) {
            limit1 = Long.MAX_VALUE;
        }
        try {
            limit2 = Long.valueOf(mPrefs.getString(Constants.PREF_SIM2_CALLS[1], "0")) * Constants.MINUTE;
        } catch (Exception e) {
            limit2 = Long.MAX_VALUE;
        }
        try {
            limit3 = Long.valueOf(mPrefs.getString(Constants.PREF_SIM3_CALLS[1], "0")) * Constants.MINUTE;
        } catch (Exception e) {
            limit3 = Long.MAX_VALUE;
        }
        return new long[]{limit1, limit2, limit3};
    }

    public static long[] getTrafficSimLimitsValues() {
        boolean[] isNight = getIsNightState();
        String limit1 = isNight[0] ? mPrefs.getString(Constants.PREF_SIM1[18], "") : mPrefs.getString(Constants.PREF_SIM1[1], "");
        String limit2 = isNight[1] ? mPrefs.getString(Constants.PREF_SIM2[18], "") : mPrefs.getString(Constants.PREF_SIM2[1], "");
        String limit3 = isNight[2] ? mPrefs.getString(Constants.PREF_SIM3[18], "") : mPrefs.getString(Constants.PREF_SIM3[1], "");
        String round1 = isNight[0] ? mPrefs.getString(Constants.PREF_SIM1[22], "0") : mPrefs.getString(Constants.PREF_SIM1[4], "0");
        String round2 = isNight[1] ? mPrefs.getString(Constants.PREF_SIM2[22], "0") : mPrefs.getString(Constants.PREF_SIM2[4], "0");
        String round3 = isNight[2] ? mPrefs.getString(Constants.PREF_SIM3[22], "0") : mPrefs.getString(Constants.PREF_SIM3[4], "0");
        int value1;
        if (mPrefs.getString(Constants.PREF_SIM1[2], "").equals(""))
            value1 = 0;
        else
            value1 = isNight[0] ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[19], "")) :
                    Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[2], ""));
        int value2;
        if (mPrefs.getString(Constants.PREF_SIM2[2], "").equals(""))
            value2 = 0;
        else
            value2 = isNight[1] ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[19], "")) :
                    Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[2], ""));
        int value3;
        if (mPrefs.getString(Constants.PREF_SIM3[2], "").equals(""))
            value3 = 0;
        else
            value3 = isNight[2] ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[19], "")) :
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

    public static boolean[] getIsNightState() {
        DateTime now = new DateTime();
        boolean isNight1, isNight2, isNight3;
        if (mPrefs.getBoolean(Constants.PREF_SIM1[17], false)) {
            String timeON = now.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM1[20], "23:00");
            String timeOFF = now.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM1[21], "06:00");
            isNight1 = DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
        } else
            isNight1 = false;
        if (mPrefs.getBoolean(Constants.PREF_SIM2[17], false)) {
            String timeON = now.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM2[20], "23:00");
            String timeOFF = now.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM2[21], "06:00");
            isNight2 = DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
        } else
            isNight2 = false;
        if (mPrefs.getBoolean(Constants.PREF_SIM3[17], false)) {
            String timeON = now.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM3[20], "23:00");
            String timeOFF = now.toString(Constants.DATE_FORMATTER) + " " + mPrefs.getString(Constants.PREF_SIM3[21], "06:00");
            isNight3 = DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
        } else
            isNight3 = false;

        return new boolean[] {isNight1, isNight2, isNight3};
    }
}
