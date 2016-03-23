package ua.od.acros.dualsimtrafficcounter.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.PreferenceFragmentCompatFix;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineListPreference;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;


public class OperatorFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TwoLineCheckPreference auto1, auto2, auto3, showLogo;
    private TwoLineEditTextPreference name1, name2, name3;
    private TwoLineListPreference logo1, logo2, logo3;
    private SharedPreferences mPrefs;
    private boolean mIsAttached;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        Context context = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        addPreferencesFromResource(R.xml.operator_settings);

        showLogo = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[15]);

        name1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1[6]);
        auto1 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM1[5]);
        logo1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1[23]);

        auto2 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM2[5]);
        name2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2[6]);
        logo2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2[23]);

        auto3 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM3[5]);
        name3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3[6]);

        logo3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3[23]);

        android.support.v7.preference.PreferenceScreen sim2 = (android.support.v7.preference.PreferenceScreen) getPreferenceScreen().findPreference("sim2");
        android.support.v7.preference.PreferenceScreen sim3 = (android.support.v7.preference.PreferenceScreen) getPreferenceScreen().findPreference("sim3");

        int simNumber = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(context)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));

        if (simNumber == 1) {
            getPreferenceScreen().removePreference(sim2);
            getPreferenceScreen().removePreference(sim3);
            logo2.setEnabled(false);
            logo3.setEnabled(false);
        }
        if (simNumber == 2) {
            getPreferenceScreen().removePreference(sim3);
            logo3.setEnabled(false);
        }
        if (mIsAttached)
            updateSummary();
    }

    private void updateSummary() {
        if (auto1 != null && !auto1.isChecked())
            name1.setSummary(name1.getText());
        if (auto2 != null &&  !auto2.isChecked())
            name2.setSummary(name2.getText());
        if (auto3 != null &&  !auto3.isChecked())
            name3.setSummary(name3.getText());

        String[] listitems = getResources().getStringArray(R.array.logo_values);
        String[] list = getResources().getStringArray(R.array.logo);
        for (int i = 0; i < list.length; i++) {
            if (showLogo != null) {
                if (showLogo.isChecked() && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM1[23], "none")))
                    if (logo1 != null)
                        logo1.setSummary(list[i]);
                if (showLogo.isChecked() && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM2[23], "none")))
                    if (logo2 != null)
                        logo2.setSummary(list[i]);
                if (showLogo.isChecked() && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM3[23], "none")))
                    if (logo3 != null)
                        logo3.setSummary(list[i]);
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
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);;
        toolBar.setTitle(R.string.name_title);
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
    }
}