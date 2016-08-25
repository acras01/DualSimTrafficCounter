package ua.od.acros.dualsimtrafficcounter.settings;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.TimePreferenceDialog;
import ua.od.acros.dualsimtrafficcounter.preferences.PreferenceFragmentCompatFix;
import ua.od.acros.dualsimtrafficcounter.preferences.TimePreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineListPreference;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.InputFilterMinMax;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class CallsLimitFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private TwoLineEditTextPreference limit1, limit2, limit3,
            day1, day2, day3, round1, round2, round3;
    private TwoLineListPreference period1, period2, period3, opValue1, opValue2, opValue3;
    private TimePreference time1, time2, time3;
    private SharedPreferences mPrefs;
    private boolean mIsAttached = false;
    private Preference save1, save2, save3;
    private Context mContext;
    private ArrayList<String> mIMSI = null;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        int simQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));

        mIMSI = MobileUtils.getSimIds(mContext);
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            String path = mContext.getFilesDir().getParent() + "/shared_prefs/";
            SharedPreferences.Editor editor = mPrefs.edit();
            SharedPreferences prefSim;
            Map<String, ?> prefs;
            String name = "calls_" + mIMSI.get(0);
            if (new File(path + name + ".xml").exists()) {
                prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                prefs = prefSim.getAll();
                if (prefs.size() != 0)
                    for (int i = 0; i < prefs.size(); i++) {
                        String key = Constants.PREF_SIM_CALLS[i] + 1;
                        Object o = prefs.get(Constants.PREF_SIM_CALLS[i]);
                        CustomApplication.putObject(editor, key, o);
                    }
                prefSim = null;
            }
            if (simQuantity >= 2) {
                name = "calls_" + mIMSI.get(1);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (int i = 0; i < prefs.size(); i++) {
                            String key = Constants.PREF_SIM_CALLS[i] + 2;
                            Object o = prefs.get(Constants.PREF_SIM_CALLS[i]);
                            CustomApplication.putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            if (simQuantity == 3) {
                name = "calls_" + mIMSI.get(2);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (int i = 0; i < prefs.size(); i++) {
                            String key = Constants.PREF_SIM_CALLS[i] + 3;
                            Object o = prefs.get(Constants.PREF_SIM_CALLS[i]);
                            CustomApplication.putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            editor.apply();
        }

        addPreferencesFromResource(R.xml.calls_settings);

        limit1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[1]);
        limit2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[1]);
        limit3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[1]);
        period1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1_CALLS[2]);
        period2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2_CALLS[2]);
        period3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3_CALLS[2]);
        round1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[3]);
        round2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[3]);
        round3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[3]);
        time1 = (TimePreference) findPreference(Constants.PREF_SIM1_CALLS[4]);
        time2 = (TimePreference) findPreference(Constants.PREF_SIM2_CALLS[4]);
        time3 = (TimePreference) findPreference(Constants.PREF_SIM3_CALLS[4]);
        day1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[5]);
        day2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[5]);
        day3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[5]);
        opValue1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1_CALLS[6]);
        opValue2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2_CALLS[6]);
        opValue3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3_CALLS[6]);

        PreferenceScreen sim2 = (PreferenceScreen) getPreferenceScreen().findPreference("calls_sim2");
        PreferenceScreen sim3 = (PreferenceScreen) getPreferenceScreen().findPreference("calls_sim3");

        TwoLineCheckPreference save = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[45]);
        if (save != null && (mIMSI == null || mIMSI.size() != simQuantity || mIMSI.contains(null)))
            save.setEnabled(false);

        save1 = findPreference("save_profile_calls1");
        save2 = findPreference("save_profile_calls2");
        save3 = findPreference("save_profile_calls3");
        if (!mPrefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            if (save1 != null)
                save1.setEnabled(false);
            if (save2 != null)
                save2.setEnabled(false);
            if (save3 != null)
                save3.setEnabled(false);
        }  else {
            if (save1 != null)
                save1.setOnPreferenceClickListener(this);
            if (save2 != null)
                save2.setOnPreferenceClickListener(this);
            if (save3 != null)
                save3.setOnPreferenceClickListener(this);
        }

        if (simQuantity == 1) {
            getPreferenceScreen().removePreference(sim2);
            getPreferenceScreen().removePreference(sim3);
        }
        if (simQuantity == 2) {
            getPreferenceScreen().removePreference(sim3);
        }
        if (mIsAttached)
            updateSummary();

        day1.setOnPreferenceChangeListener(this);
        day2.setOnPreferenceChangeListener(this);
        day3.setOnPreferenceChangeListener(this);

        day1.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
        day2.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});

        if (getArguments() != null) {
            String sim = getArguments().getString("sim");
            SettingsActivity.openPreferenceScreen(this, (PreferenceScreen) getPreferenceScreen().findPreference(sim));
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;
        if (preference instanceof TimePreference) {
            dialogFragment = TimePreferenceDialog.newInstance(preference);
            Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
        }
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
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
                    day1.setTitle(getResources().getString(R.string.day));
                else
                    day1.setTitle(getResources().getString(R.string.day_in_period));
            }
        }

        if (period2 != null) {
            period2.setSummary(period2.getEntry());
            if (period2.getValue().equals("0") && day2 != null)
                day2.setEnabled(false);
            if ((period2.getValue().equals("1") || period2.getValue().equals("2")) && day2 != null) {
                day2.setEnabled(true);
                if (period2.getValue().equals("1"))
                    day2.setTitle(getResources().getString(R.string.day));
                else
                    day2.setTitle(getResources().getString(R.string.day_in_period));
            }
        }

        if (period3 != null) {
            period3.setSummary(period3.getEntry());
            if (period3.getValue().equals("0") && day3 != null)
                day3.setEnabled(false);
            if ((period3.getValue().equals("1") || period3.getValue().equals("2")) && day3 != null) {
                day3.setEnabled(true);
                if (period3.getValue().equals("1"))
                    day3.setTitle(getResources().getString(R.string.day));
                else
                    day3.setTitle(getResources().getString(R.string.day_in_period));
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
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setTitle(R.string.calls_limit_title);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mIsAttached)
            updateSummary();
        if (key.equals(Constants.PREF_OTHER[45])) {
            boolean state = sharedPreferences.getBoolean(key, true);
            if (save1 != null)
                save1.setEnabled(state);
            if (save2 != null)
                save2.setEnabled(state);
            if (save3 != null)
                save3.setEnabled(state);
            if (!sharedPreferences.getBoolean(key, false)) {
                CustomDatabaseHelper dbHelper = CustomDatabaseHelper.getInstance(mContext);
                CustomDatabaseHelper.deleteWhiteBlackListTables(dbHelper, mIMSI);
            }
        }
        if (sharedPreferences.getBoolean(Constants.PREF_OTHER[45], false)) {
            int sim = Constants.DISABLED;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM1_CALLS)).contains(key))
                sim = Constants.SIM1;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM2_CALLS)).contains(key))
                sim = Constants.SIM2;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM3_CALLS)).contains(key))
                sim = Constants.SIM3;
            if (sim >= 0) {
                Map prefs = sharedPreferences.getAll();
                Object o = prefs.get(key);
                SharedPreferences.Editor editor = mContext.getSharedPreferences("calls_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
                CustomApplication.putObject(editor, key.substring(0, key.length() - 1), o);
                editor.apply();
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        switch (preference.getKey()) {
            case "calls_day1":
            case "calls_day2":
            case "calls_day3":
                String input = o.toString();
                if (input.matches("[0-9]+") && (Integer.valueOf(input) >= 1 && Integer.valueOf(input) <= 31))
                    return true;
                break;
        }
        Toast.makeText(getActivity(), R.string.check_input, Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mIMSI != null) {
            new SaveTask().execute(preference);
            return true;
        } else
            return false;
    }

    private class SaveTask extends AsyncTask<Preference, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Preference... params) {
            Map<String, ?> prefs = mPrefs.getAll();
            String[] keys = new String[Constants.PREF_SIM_CALLS.length];
            int sim = Constants.DISABLED;
            switch (params[0].getKey()) {
                case "save_profile_calls1":
                    keys = Constants.PREF_SIM1_CALLS;
                    sim = Constants.SIM1;
                    break;
                case "save_profile_calls2":
                    keys = Constants.PREF_SIM2_CALLS;
                    sim = Constants.SIM2;
                    break;
                case "save_profile_calls3":
                    keys = Constants.PREF_SIM3_CALLS;
                    sim = Constants.SIM3;
                    break;
            }
            SharedPreferences.Editor editor = mContext.getSharedPreferences("calls_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
            Set<String> keySet = prefs.keySet();
            ArrayList<String> simKeys = new ArrayList<>(Arrays.asList(keys));
            for (String key : keySet) {
                if (simKeys.contains(key)) {
                    Object o = prefs.get(key);
                    CustomApplication.putObject(editor, key.substring(0, key.length() - 1), o);
                }
            }
            editor.apply();
            CustomDatabaseHelper dbHelper = CustomDatabaseHelper.getInstance(mContext);
            if (CustomDatabaseHelper.isTableEmpty(dbHelper, "black_" + mIMSI.get(sim), false))
                CustomDatabaseHelper.writeBlackList(sim, CustomDatabaseHelper.readBlackList(sim, dbHelper, null), dbHelper, mIMSI);
            if (CustomDatabaseHelper.isTableEmpty(dbHelper, "white_" + mIMSI.get(sim), false))
                CustomDatabaseHelper.writeWhiteList(sim, CustomDatabaseHelper.readWhiteList(sim, dbHelper, null), dbHelper, mIMSI);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(mContext, R.string.saved, Toast.LENGTH_LONG).show();
        }
    }
}