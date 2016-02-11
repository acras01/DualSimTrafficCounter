package ua.od.acros.dualsimtrafficcounter.settings;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputFilter;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.InputFilterMinMax;
import ua.od.acros.dualsimtrafficcounter.utils.XposedUtils;


public class OtherFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


    private TwoLineEditTextPreference timer, simQuantity;
    private TwoLineCheckPreference callLogger;

    private static final String XPOSED = "de.robv.android.xposed.installer";

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.other_settings);

        ActionBar actionbar = getActivity().getActionBar();
        if (actionbar != null)
            actionbar.setTitle(R.string.other_title);

        timer = (TwoLineEditTextPreference) findPreference(Constants.PREF_OTHER[8]);
        timer.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, Integer.MAX_VALUE)});
        simQuantity = (TwoLineEditTextPreference) findPreference(Constants.PREF_OTHER[14]);
        simQuantity.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 3)});
        callLogger = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[25]);
        if (!XposedUtils.isPackageExisted(getActivity(), XPOSED)) {
            callLogger.setChecked(false);
            callLogger.setEnabled(false);
        }
        updateSummary();
    }

    private void updateSummary() {
        if (timer != null && timer.isEnabled())
            timer.setSummary(String.format(getResources().getString(R.string.minutes), timer.getText()));
        if (simQuantity != null && simQuantity.isEnabled())
            simQuantity.setSummary(simQuantity.getText());
        if (callLogger != null)
            getPreferenceScreen().getSharedPreferences().edit()
                    .putBoolean(Constants.PREF_OTHER[24], callLogger.isChecked())
                    .apply();
    }

    @Override
    public void onResume() {
        super.onResume();
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
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity()))
                    getActivity().stopService(new Intent(getActivity(), CallLoggerService.class));
            } else
                getActivity().startService(new Intent(getActivity(), CallLoggerService.class));
            sharedPreferences.edit()
                    .putBoolean(Constants.PREF_OTHER[24], sharedPreferences.getBoolean(key, false))
                    .apply();
        }
        updateSummary();
    }
}