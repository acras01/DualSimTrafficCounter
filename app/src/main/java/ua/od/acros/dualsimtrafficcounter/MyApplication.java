package ua.od.acros.dualsimtrafficcounter;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(mailTo = "acras1@gmail.com",
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.SETTINGS_GLOBAL, ReportField.SETTINGS_SYSTEM,
                ReportField.STACK_TRACE, ReportField.LOGCAT, ReportField.SHARED_PREFERENCES },
        logcatArguments = { "-t", "200", "-v", "long", "*:S" },
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_toast_text,
        resDialogOkToast = R.string.crash_toast_text)
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    private static boolean activityVisible;
}
