package ua.od.acros.dualsimtrafficcounter.settings;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;


public class NameFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TwoLineCheckPreference auto1, auto2, auto3;
    private EditTextPreference name1, name2, name3;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.operator_names);

        ActionBar actionbar = getActivity().getActionBar();
        if (actionbar != null)
            actionbar.setTitle(R.string.name_title);

        name1 = (EditTextPreference) findPreference(Constants.PREF_SIM1[6]);
        name2 = (EditTextPreference) findPreference(Constants.PREF_SIM2[6]);
        name3 = (EditTextPreference) findPreference(Constants.PREF_SIM3[6]);
        auto1 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM1[5]);
        auto2 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM2[5]);
        auto3 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM3[5]);
        PreferenceCategory sim2 = (PreferenceCategory) getPreferenceScreen().findPreference("sim2");
        PreferenceCategory sim3 = (PreferenceCategory) getPreferenceScreen().findPreference("sim3");

        int simNumber = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Constants.PREF_OTHER[13], true) ? MobileDataControl.isMultiSim(getActivity())
                : Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Constants.PREF_OTHER[14], "1"));

        if (simNumber == 1) {
            getPreferenceScreen().removePreference(sim2);
            getPreferenceScreen().removePreference(sim3);
        }
        if (simNumber == 2)
            getPreferenceScreen().removePreference(sim3);

        updateSummary();
    }

    private void updateSummary() {
        if (auto1 != null && !auto1.isChecked())
            name1.setSummary(name1.getText());
        if (auto2 != null &&  !auto2.isChecked())
            name2.setSummary(name2.getText());
        if (auto3 != null &&  !auto3.isChecked())
            name3.setSummary(name3.getText());
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
    }
}