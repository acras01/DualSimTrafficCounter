package ua.od.acros.dualsimtrafficcounter.settings;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.widget.Toast;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.InputFilterMinMax;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLinePreference;


public class OtherFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    //private CheckBoxPreference watchdog, fullInfo;
    private TwoLinePreference timer;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.other_settings);

        ActionBar actionbar = getActivity().getActionBar();
        if (actionbar != null)
            actionbar.setTitle(R.string.other_title);

        //watchdog = (CheckBoxPreference) findPreference(Constants.PREF[49]);
        //fullInfo = (CheckBoxPreference) findPreference(Constants.PREF[52]);
        timer = (TwoLinePreference) findPreference(Constants.PREF_OTHER[8]);
        timer.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, Integer.MAX_VALUE)});

        updateSummary();
    }

    private void updateSummary() {
        if (timer != null && timer.isEnabled())
            timer.setSummary(timer.getText() + getResources().getString(R.string.minute));

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
        updateSummary();
        if (key.equals(Constants.PREF_OTHER[7]))
            Toast.makeText(getActivity(), R.string.restart, Toast.LENGTH_LONG).show();
    }
}