package ua.od.acros.dualsimtrafficcounter.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.lang.ref.WeakReference;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.PreferenceFragmentCompatFix;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineListPreference;
import ua.od.acros.dualsimtrafficcounter.receivers.ResetReceiver;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.FloatingWindowService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.services.WatchDogService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomSwitch;
import ua.od.acros.dualsimtrafficcounter.utils.InputFilterMinMax;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;


public class OtherFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private TwoLineEditTextPreference timer, simQuantity, floatWindow;
    private TwoLineListPreference theme, fullInfo, dataRemain, infoStatus, callsRemain, hudRemain;
    private Context mContext;
    private boolean mIsAttached;
    private SharedPreferences mPrefs;
    private static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE_FLOAT = 5469;
    private static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE_CALL = 5470;
    private static WeakReference<CustomSwitch> mSwitch;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        addPreferencesFromResource(R.xml.other_settings);

        timer = (TwoLineEditTextPreference) findPreference(Constants.PREF_OTHER[8]);
        timer.setOnPreferenceChangeListener(this);
        timer.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, Integer.MAX_VALUE)});
        simQuantity = (TwoLineEditTextPreference) findPreference(Constants.PREF_OTHER[14]);
        simQuantity.setOnPreferenceChangeListener(this);
        simQuantity.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 3)});
        floatWindow = (TwoLineEditTextPreference) findPreference(Constants.PREF_OTHER[33]);
        floatWindow.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, Integer.MAX_VALUE)});
        theme = (TwoLineListPreference) findPreference(Constants.PREF_OTHER[28]);
        fullInfo = (TwoLineListPreference) findPreference(Constants.PREF_OTHER[7]);
        dataRemain = (TwoLineListPreference) findPreference(Constants.PREF_OTHER[19]);
        infoStatus = (TwoLineListPreference) findPreference(Constants.PREF_OTHER[16]);
        callsRemain = (TwoLineListPreference) findPreference(Constants.PREF_OTHER[27]);
        hudRemain = (TwoLineListPreference) findPreference(Constants.PREF_OTHER[39]);
        if (mIsAttached) {
            findPreference(Constants.PREF_OTHER[41]).setEnabled(!mPrefs.getBoolean(Constants.PREF_OTHER[47], false));
            findPreference("hud_reset").setOnPreferenceClickListener(this);
            updateSummary();
        }
    }

    private void updateSummary() {
        if (timer != null && timer.isEnabled())
            timer.setSummary(String.format(getResources().getString(R.string.minutes), timer.getText()));
        if (simQuantity != null && simQuantity.isEnabled())
            simQuantity.setSummary(simQuantity.getText());
        if (floatWindow != null && floatWindow.isEnabled()) {
            floatWindow.setSummary(floatWindow.getText());
        }
        if (theme != null)
            theme.setSummary(theme.getEntry());
        if (fullInfo != null)
            fullInfo.setSummary(fullInfo.getEntry());
        if (dataRemain != null)
            dataRemain.setSummary(dataRemain.getEntry());
        if (infoStatus != null)
            infoStatus.setSummary(infoStatus.getEntry());
        if (callsRemain != null)
            callsRemain.setSummary(callsRemain.getEntry());
        if (hudRemain != null)
            hudRemain.setSummary(hudRemain.getEntry());
        PreferenceScreen ps = getPreferenceScreen();
        if (ps != null && ps.getKey() != null && ps.getKey().equals("float")) {
            int count = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < count; ++i) {
                android.support.v7.preference.Preference pref = getPreferenceScreen().getPreference(i);
                boolean state = (mSwitch == null || mSwitch.get() == null) ? mPrefs.getBoolean(Constants.PREF_OTHER[32], true)
                        : mSwitch.get().isSwitchOn();
                pref.setEnabled(state);
                if (pref.getKey().equals(Constants.PREF_OTHER[41]))
                    pref.setEnabled(state && !mPrefs.getBoolean(Constants.PREF_OTHER[47], false));
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mIsAttached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mIsAttached = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setTitle(R.string.other_title);
        if (mSwitch != null && mSwitch.get() != null)
            mSwitch.get().resume();
        updateSummary();
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSwitch != null && mSwitch.get() != null)
            mSwitch.get().pause();
        updateSummary();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mIsAttached)
            updateSummary();
        if (key.equals(Constants.PREF_OTHER[25])) {
            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            DateTime alarmTime = new DateTime().withTimeAtStartOfDay();
            Intent iReset = new Intent(mContext, ResetReceiver.class);
            iReset.setAction(Constants.RESET_ACTION);
            final int RESET = 1981;
            PendingIntent piReset = PendingIntent.getBroadcast(mContext, RESET, iReset, 0);
            if (sharedPreferences.getBoolean(key, false)) {
                sharedPreferences.edit()
                        .putBoolean(Constants.PREF_OTHER[24], false)
                        .apply();
                am.cancel(piReset);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, piReset);
            } else {
                if (CustomApplication.isMyServiceRunning(CallLoggerService.class))
                    mContext.stopService(new Intent(mContext, CallLoggerService.class));
                sharedPreferences.edit()
                        .putBoolean(Constants.PREF_OTHER[24], true)
                        .apply();
                am.cancel(piReset);
            }
        }
        if (key.equals(Constants.PREF_OTHER[12]) && CustomApplication.isMyServiceRunning(WatchDogService.class)) {
            Intent i = new Intent(mContext, WatchDogService.class);
            mContext.stopService(i);
            mContext.startService(i);
        }
        if (key.equals(Constants.PREF_OTHER[12])) {
            Intent i;
            if (CustomApplication.isMyServiceRunning(TrafficCountService.class)) {
                i = new Intent(mContext, TrafficCountService.class);
                mContext.stopService(i);
                mContext.startService(i);
            } else if (CustomApplication.isMyServiceRunning(CallLoggerService.class)) {
                i = new Intent(mContext, CallLoggerService.class);
                mContext.stopService(i);
                mContext.startService(i);
            }
        }
        if (key.equals(Constants.PREF_OTHER[25])) {
            if (sharedPreferences.getBoolean(key, false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.canDrawOverlays(mContext)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + mContext.getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE_CALL);
            }
        }
        boolean autoLoad = sharedPreferences.getBoolean(Constants.PREF_OTHER[47], false);
        boolean floatingWindow = sharedPreferences.getBoolean(Constants.PREF_OTHER[32], false);
        boolean alwaysShow = !sharedPreferences.getBoolean(Constants.PREF_OTHER[41], false);
        boolean mobileData = MobileUtils.hasActiveNetworkInfo(mContext) == 2;
        boolean bool = (autoLoad && mobileData) || (!autoLoad && ((!alwaysShow && mobileData) || alwaysShow));
        boolean show = false;
        if (key.equals(Constants.PREF_OTHER[32])) {
            if (sharedPreferences.getBoolean(key, false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.canDrawOverlays(mContext)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + mContext.getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE_FLOAT);
            } else
                show = floatingWindow && bool;
        }
        if (key.contains("hud") && !(key.equals(Constants.PREF_OTHER[32]) ||
                key.equals(Constants.PREF_OTHER[36]) || key.equals(Constants.PREF_OTHER[37]) ||
                key.equals(Constants.PREF_OTHER[38])))
            show = bool;
        if (key.contains("hud") && !(key.equals(Constants.PREF_OTHER[36]) || key.equals(Constants.PREF_OTHER[37]) ||
                key.equals(Constants.PREF_OTHER[38]))) {
            if (show)
                FloatingWindowService.showFloatingWindow(mContext, mPrefs);
            else
                FloatingWindowService.closeFloatingWindow(mContext, mPrefs);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(mContext)) {
            if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE_CALL)
                ((TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[25])).setChecked(false);
            else if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE_FLOAT)
                ((TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[32])).setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String input = o.toString();
        switch (preference.getKey()) {
            case "watchdog_timer":
                if (input.matches("[0-9]+") && Integer.valueOf(input) >= 1)
                    return true;
                break;
            case "user_sim":
                if (input.matches("[0-9]+") && (Integer.valueOf(input) >= 1 && Integer.valueOf(input) <= 3))
                    return true;
                break;
            case "hud_textsize":
                if (input.matches("[0-9]+") && (Integer.valueOf(input) >= 1 && Integer.valueOf(input) <= 3))
                    return true;
                break;
        }
        Toast.makeText(getActivity(), R.string.check_input, Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("hud_reset")) {
            mPrefs.edit()
                    .putInt(Constants.PREF_OTHER[36], -1)
                    .putInt(Constants.PREF_OTHER[37], -1)
                    .putBoolean(Constants.PREF_OTHER[40], true)
                    .apply();
            ((TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[40])).setChecked(true);
            if (mPrefs.getBoolean(Constants.PREF_OTHER[32], false) &&
                    (!mPrefs.getBoolean(Constants.PREF_OTHER[41], false) || MobileUtils.isMobileDataActive(mContext)))
                FloatingWindowService.showFloatingWindow(mContext, mPrefs);
            return true;
        } else
            return false;
    }

    public static void setSwitch(CustomSwitch swtch) {
        mSwitch = new WeakReference<>(swtch);
    }
}