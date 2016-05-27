package ua.od.acros.dualsimtrafficcounter.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.util.Random;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.PreferenceFragmentCompatFix;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.receivers.ResetReceiver;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomNotification;
import ua.od.acros.dualsimtrafficcounter.services.FloatingWindow;
import ua.od.acros.dualsimtrafficcounter.utils.InputFilterMinMax;
import wei.mark.standout.StandOutWindow;


public class OtherFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    private TwoLineEditTextPreference timer, simQuantity, floatWindow;

    private static final String XPOSED = "de.robv.android.xposed.installer";
    private Context mContext;
    private boolean mIsAttached;
    private SharedPreferences mPrefs;

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
        TwoLineCheckPreference callLogger = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[25]);
        findPreference("hud_reset").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().equals("hud_reset")) {
                    StandOutWindow.closeAll(mContext, FloatingWindow.class);
                    int id = Math.abs(new Random().nextInt());
                    mPrefs.edit()
                            .putInt(Constants.PREF_OTHER[38], id)
                            .putInt(Constants.PREF_OTHER[36], -1)
                            .putInt(Constants.PREF_OTHER[37], -1)
                            .apply();
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[32], false))
                        StandOutWindow.show(mContext, FloatingWindow.class, id);
                    return true;
                } else
                    return false;
            }
        });
        if (!CustomApplication.isPackageExisted(XPOSED)) {
            callLogger.setChecked(false);
            callLogger.setEnabled(false);
            getPreferenceScreen().getSharedPreferences().edit()
                    .putBoolean(Constants.PREF_OTHER[24], true)
                    .apply();
        }
        if (mIsAttached)
            updateSummary();
    }

    private void updateSummary() {
        if (timer != null && timer.isEnabled())
            timer.setSummary(String.format(getResources().getString(R.string.minutes), timer.getText()));
        if (simQuantity != null && simQuantity.isEnabled())
            simQuantity.setSummary(simQuantity.getText());
        if (floatWindow != null && floatWindow.isEnabled())
            floatWindow.setSummary(floatWindow.getText());
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
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
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
            if (!sharedPreferences.getBoolean(key, false)) {
                if (CustomApplication.isMyServiceRunning(CallLoggerService.class)) {
                    mContext.stopService(new Intent(mContext, CallLoggerService.class));
                    sharedPreferences.edit()
                            .putBoolean(Constants.PREF_OTHER[24], true)
                            .apply();
                    am.cancel(piReset);
                }
            } else {
                mContext.startService(new Intent(mContext, CallLoggerService.class));
                sharedPreferences.edit()
                        .putBoolean(Constants.PREF_OTHER[24], false)
                        .apply();
                am.cancel(piReset);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, piReset);
            }
        }
        if (key.equals(Constants.PREF_OTHER[12])) {
            CustomNotification.setPriorityNeedsChange(true);
            if (CustomApplication.isMyServiceRunning(TrafficCountService.class)) {
                mContext.stopService(new Intent(mContext, TrafficCountService.class));
                mContext.startService(new Intent(mContext, TrafficCountService.class));
            } else if (CustomApplication.isMyServiceRunning(CallLoggerService.class)) {
                mContext.stopService(new Intent(mContext, CallLoggerService.class));
                mContext.startService(new Intent(mContext, CallLoggerService.class));
            }
        }
        if (key.equals(Constants.PREF_OTHER[32])) {
            if (sharedPreferences.getBoolean(key, true)) {
                int id = Math.abs(new Random().nextInt());
                mPrefs.edit()
                        .putInt(Constants.PREF_OTHER[38], id)
                        .apply();
                StandOutWindow.show(mContext, FloatingWindow.class, id);
            } else {
                int id = mPrefs.getInt(Constants.PREF_OTHER[38], -1);
                if (id >= 0)
                    StandOutWindow.close(mContext, FloatingWindow.class, id);
                else
                    StandOutWindow.closeAll(mContext, FloatingWindow.class);
            }
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
}