package ua.od.acros.dualsimtrafficcounter.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.XposedUtils;


public class OtherFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    private TwoLineEditTextPreference timer, simQuantity;

    private static final String XPOSED = "de.robv.android.xposed.installer";
    private Context mContext;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        mContext = getActivity().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.other_settings);

        timer = (TwoLineEditTextPreference) findPreference(Constants.PREF_OTHER[8]);
        timer.setOnPreferenceChangeListener(this);
        //timer.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, Integer.MAX_VALUE)});
        simQuantity = (TwoLineEditTextPreference) findPreference(Constants.PREF_OTHER[14]);
        simQuantity.setOnPreferenceChangeListener(this);
        //simQuantity.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 3)});
        TwoLineCheckPreference callLogger = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[25]);
        if (!XposedUtils.isPackageExisted(mContext, XPOSED)) {
            callLogger.setChecked(false);
            callLogger.setEnabled(false);
            getPreferenceScreen().getSharedPreferences().edit()
                    .putBoolean(Constants.PREF_OTHER[24], true)
                    .apply();
        }
        updateSummary();
    }

    private void updateSummary() {
        if (timer != null && timer.isEnabled())
            timer.setSummary(String.format(getResources().getString(R.string.minutes), timer.getText()));
        if (simQuantity != null && simQuantity.isEnabled())
            simQuantity.setSummary(simQuantity.getText());
    }

    @Override
    public void onResume() {
        super.onResume();
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);;
        toolBar.setTitle(R.string.other_title);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[25])) {
            if (!sharedPreferences.getBoolean(key, false)) {
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext)) {
                    mContext.stopService(new Intent(mContext, CallLoggerService.class));
                    sharedPreferences.edit()
                            .putBoolean(Constants.PREF_OTHER[24], true)
                            .apply();
                }
            } else {
                mContext.startService(new Intent(mContext, CallLoggerService.class));
                sharedPreferences.edit()
                        .putBoolean(Constants.PREF_OTHER[24], false)
                        .apply();
            }
        }
        updateSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        switch (preference.getKey()) {
            case "watchdog_timer":
            case "user_sim":
                String input = o.toString();
                return input.matches("[0-9]+") && (Integer.valueOf(input) >= 1 || Integer.valueOf(input) >= 31);
            default:
                Toast.makeText(mContext, R.string.check_input, Toast.LENGTH_LONG).show();
                return false;
        }
    }
}