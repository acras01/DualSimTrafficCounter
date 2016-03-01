package ua.od.acros.dualsimtrafficcounter.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.View;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineListPreference;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;


public class OperatorFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TwoLineCheckPreference auto1, auto2, auto3, showLogo;
    private EditTextPreference name1, name2, name3;
    private TwoLineListPreference logo1, logo2, logo3;
    private SharedPreferences mPrefs;
    private Toolbar mToolBar;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Context context = getActivity().getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.operator_settings);

        mToolBar = SettingsActivity.getBar();
        if (mToolBar != null)
            mToolBar.setTitle(R.string.name_title);

        name1 = (EditTextPreference) findPreference(Constants.PREF_SIM1[6]);
        name2 = (EditTextPreference) findPreference(Constants.PREF_SIM2[6]);
        name3 = (EditTextPreference) findPreference(Constants.PREF_SIM3[6]);
        auto1 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM1[5]);
        auto2 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM2[5]);
        auto3 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM3[5]);
        showLogo = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[15]);
        logo1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1[23]);
        logo2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2[23]);
        logo3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3[23]);
        PreferenceScreen sim2 = (PreferenceScreen) getPreferenceScreen().findPreference("sim2");
        PreferenceScreen sim3 = (PreferenceScreen) getPreferenceScreen().findPreference("sim3");

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

        updateSummary();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // If the user has clicked on a preference screen, set up the screen
        if (preference instanceof PreferenceScreen) {
            setUpNestedScreen((PreferenceScreen) preference);
        }

        return false;
    }

    public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();
        if (mToolBar != null) {
            mToolBar.setTitle(preferenceScreen.getTitle());
            mToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
            mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
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
    public void onResume() {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary();
    }
}