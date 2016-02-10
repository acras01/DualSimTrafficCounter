package ua.od.acros.dualsimtrafficcounter.settings;


import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputFilter;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.TimePreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.InputFilterMinMax;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class CallsLimitFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private EditTextPreference limit1, limit2, limit3,
            day1, day2, day3;
    private TwoLineEditTextPreference round1, round2, round3;
    private ListPreference period1, period2, period3, opValue1, opValue2, opValue3;
    private TimePreference time1, time2, time3;
    private SharedPreferences mPrefs;
    private int mSimQuantity;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.calls_settings);
        ActionBar actionbar = getActivity().getActionBar();
        if (actionbar != null)
            actionbar.setTitle(R.string.limit_title);

        limit1 = (EditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[1]);
        limit2 = (EditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[1]);
        limit3 = (EditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[1]);
        period1 = (ListPreference) findPreference(Constants.PREF_SIM1_CALLS[2]);
        period2 = (ListPreference) findPreference(Constants.PREF_SIM2_CALLS[2]);
        period3 = (ListPreference) findPreference(Constants.PREF_SIM3_CALLS[2]);
        round1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[3]);
        round2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[3]);
        round3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[3]);
        time1 = (TimePreference) findPreference(Constants.PREF_SIM1_CALLS[4]);
        time2 = (TimePreference) findPreference(Constants.PREF_SIM2_CALLS[4]);
        time3 = (TimePreference) findPreference(Constants.PREF_SIM3_CALLS[4]);
        day1 = (EditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[5]);
        day2 = (EditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[5]);
        day3 = (EditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[5]);
        opValue1 = (ListPreference) findPreference(Constants.PREF_SIM1_CALLS[6]);
        opValue2 = (ListPreference) findPreference(Constants.PREF_SIM2_CALLS[6]);
        opValue3 = (ListPreference) findPreference(Constants.PREF_SIM3_CALLS[6]);

        PreferenceScreen sim2 = (PreferenceScreen) getPreferenceScreen().findPreference("calls_sim2");
        PreferenceScreen sim3 = (PreferenceScreen) getPreferenceScreen().findPreference("calls_sim3");

        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getActivity())
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));

        if (mSimQuantity == 1) {
            getPreferenceScreen().removePreference(sim2);
            getPreferenceScreen().removePreference(sim3);
        }
        if (mSimQuantity == 2) {
            getPreferenceScreen().removePreference(sim3);
        }
        updateSummary();

        day1.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
        day2.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
        day3.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});


        int sim = getActivity().getIntent().getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
        if (sim != Constants.DISABLED) {
            String key = "";
            // the preference screen your item is in must be known
            switch (sim) {
                case R.id.limit1_calls:
                case Constants.SIM1:
                    key = "calls_sim1";
                    break;
                case R.id.limit2_calls:
                case Constants.SIM2:
                    key = "calls_sim2";
                    break;
                case R.id.limit3_calls:
                case Constants.SIM3:
                    key = "calls_sim3";
                    break;
            }
            // the position of your item inside the preference screen above
            if (!key.equals("")) {
                int pos = getPreferenceScreen().findPreference(key).getOrder();
                // simulate a click / call it!!
                getPreferenceScreen().onItemClick(null, null, pos, 0);
            }
        }
    }

    private void updateSummary() {
        if (limit1 != null)
            limit1.setSummary(String.format(getResources().getString(R.string.minutes), limit1.getText()));
        if (limit2 != null)
            limit2.setSummary(String.format(getResources().getString(R.string.minutes), limit2.getText()));
        if (limit3 != null)
            limit3.setSummary(String.format(getResources().getString(R.string.minutes), limit3.getText()));

        if (period1 != null) {
            period1.setSummary(period1.getEntry());
            if (period1.getValue().equals("0") && day1 != null)
                day1.setEnabled(false);
            if ((period1.getValue().equals("1") || period1.getValue().equals("2")) && day1 != null) {
                day1.setEnabled(true);
                if (period1.getValue().equals("1"))
                    day1.setTitle(getActivity().getResources().getString(R.string.day));
                else
                    day1.setTitle(getActivity().getResources().getString(R.string.day_in_period));
            }
        }

        if (period2 != null) {
            period2.setSummary(period2.getEntry());
            if (period2.getValue().equals("0") && day2 != null)
                day2.setEnabled(false);
            if ((period2.getValue().equals("1") || period2.getValue().equals("2")) && day2 != null) {
                day2.setEnabled(true);
                if (period2.getValue().equals("1"))
                    day2.setTitle(getActivity().getResources().getString(R.string.day));
                else
                    day2.setTitle(getActivity().getResources().getString(R.string.day_in_period));
            }
        }

        if (period3 != null) {
            period3.setSummary(period3.getEntry());
            if (period3.getValue().equals("0") && day3 != null)
                day3.setEnabled(false);
            if ((period3.getValue().equals("1") || period3.getValue().equals("2")) && day3 != null) {
                day3.setEnabled(true);
                if (period3.getValue().equals("1"))
                    day3.setTitle(getActivity().getResources().getString(R.string.day));
                else
                    day3.setTitle(getActivity().getResources().getString(R.string.day_in_period));
            }
        }

        if (day1 != null && day1.isEnabled())
            day1.setSummary(day1.getText());
        if (day2 != null && day2.isEnabled())
            day2.setSummary(day2.getText());
        if (day3 != null && day3.isEnabled())
            day3.setSummary(day3.getText());

        if (round1 != null)
            round1.setSummary(String.format(getResources().getString(R.string.seconds), round1.getText()));
        if (round2 != null)
            round2.setSummary(String.format(getResources().getString(R.string.seconds), round2.getText()));
        if (round3 != null)
            round3.setSummary(String.format(getResources().getString(R.string.seconds), round3.getText()));

        if (time1 != null)
            time1.setSummary(mPrefs.getString(Constants.PREF_SIM1_CALLS[4], "00:00"));
        if (time2 != null)
            time2.setSummary(mPrefs.getString(Constants.PREF_SIM2_CALLS[4], "00:00"));
        if (time3 != null)
            time3.setSummary(mPrefs.getString(Constants.PREF_SIM3_CALLS[4], "00:00"));

        if (opValue1 != null)
            opValue1.setSummary(opValue1.getEntry());
        if (opValue2 != null)
            opValue2.setSummary(opValue2.getEntry());
        if (opValue3 != null)
            opValue3.setSummary(opValue3.getEntry());
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