package ua.od.acros.dualsimtrafficcounter.settings;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import org.joda.time.DateTime;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.TimePreferenceDialog;
import ua.od.acros.dualsimtrafficcounter.preferences.TimePreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineListPreference;
import ua.od.acros.dualsimtrafficcounter.receivers.OnOffReceiver;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyApplication;

public class TrafficLimitFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    private TwoLineEditTextPreference limit1, limit2, limit3, limit1N, limit2N, limit3N,
            round1, round2, round3, round1N, round2N, round3N,
            day1, day2, day3,
            opLimit1, opLimit2, opLimit3;
    private TwoLineListPreference value1, period1, value2, period2, value3, period3, opValue1, opValue2, opValue3, value1N, value2N, value3N;
    private TwoLineCheckPreference prefer1, prefer2, prefer3;
    private TwoLineListPreference everyday1, everyday2, everyday3;
    private TimePreference time1, time2, time3, tOn1, tOff1, tOn2, tOff2, tOn3, tOff3, tOn1N, tOff1N, tOn2N, tOff2N, tOn3N, tOff3N;
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private Context mContext;
    private boolean mIsAttached = false;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        mContext = getActivity().getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.traffic_settings);

        limit1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1[1]);
        limit2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2[1]);
        limit3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3[1]);
        value1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1[2]);
        value2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2[2]);
        value3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3[2]);
        period1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1[3]);
        period2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2[3]);
        period3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3[3]);
        round1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1[4]);
        round2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2[4]);
        round3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3[4]);
        TwoLineCheckPreference autoff1 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM1[7]);
        TwoLineCheckPreference autoff2 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM2[7]);
        TwoLineCheckPreference autoff3 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM3[7]);
        prefer1 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM1[8]);
        prefer2 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM2[8]);
        prefer3 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM3[8]);
        time1 = (TimePreference) findPreference(Constants.PREF_SIM1[9]);
        time2 = (TimePreference) findPreference(Constants.PREF_SIM2[9]);
        time3 = (TimePreference) findPreference(Constants.PREF_SIM3[9]);
        TwoLineCheckPreference changeSIM = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[10]);
        day1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1[10]);
        day2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2[10]);
        day3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3[10]);
        everyday1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1[11]);
        everyday2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2[11]);
        everyday3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3[11]);
        tOn1 = (TimePreference) findPreference(Constants.PREF_SIM1[13]);
        tOn2 = (TimePreference) findPreference(Constants.PREF_SIM2[13]);
        tOn3 = (TimePreference) findPreference(Constants.PREF_SIM3[13]);
        tOff1 = (TimePreference) findPreference(Constants.PREF_SIM1[12]);
        tOff2 = (TimePreference) findPreference(Constants.PREF_SIM2[12]);
        tOff3 = (TimePreference) findPreference(Constants.PREF_SIM3[12]);
        opLimit1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1[15]);
        opLimit2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2[15]);
        opLimit3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3[15]);
        opValue1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1[16]);
        opValue2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2[16]);
        opValue3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3[16]);
        //night
        limit1N = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1[18]);
        limit2N = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2[18]);
        limit3N = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3[18]);
        value1N = (TwoLineListPreference) findPreference(Constants.PREF_SIM1[19]);
        value2N = (TwoLineListPreference) findPreference(Constants.PREF_SIM2[19]);
        value3N = (TwoLineListPreference) findPreference(Constants.PREF_SIM3[19]);
        tOn1N = (TimePreference) findPreference(Constants.PREF_SIM1[20]);
        tOn2N = (TimePreference) findPreference(Constants.PREF_SIM2[20]);
        tOn3N = (TimePreference) findPreference(Constants.PREF_SIM3[20]);
        tOff1N = (TimePreference) findPreference(Constants.PREF_SIM1[21]);
        tOff2N = (TimePreference) findPreference(Constants.PREF_SIM2[21]);
        tOff3N = (TimePreference) findPreference(Constants.PREF_SIM3[21]);
        round1N = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1[22]);
        round2N = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2[22]);
        round3N = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3[22]);

        PreferenceScreen sim2 = (PreferenceScreen) getPreferenceScreen().findPreference("traff_sim2");
        PreferenceScreen sim3 = (PreferenceScreen) getPreferenceScreen().findPreference("traff_sim3");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && !MyApplication.hasRoot()) {
            changeSIM.setEnabled(false);
            changeSIM.setChecked(false);
            autoff1.setChecked(false);
            autoff1.setEnabled(false);
            autoff2.setChecked(false);
            autoff2.setEnabled(false);
            autoff3.setChecked(false);
            autoff3.setEnabled(false);
            getPreferenceScreen().findPreference("everyday1").setEnabled(false);
            getPreferenceScreen().findPreference("everyday2").setEnabled(false);
            getPreferenceScreen().findPreference("everyday3").setEnabled(false);
            everyday1.setEnabled(false);
            everyday2.setEnabled(false);
            everyday3.setEnabled(false);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ||
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && !MyApplication.isMtkDevice())) {
            changeSIM.setEnabled(false);
            changeSIM.setChecked(false);
            everyday1.setEntries(getResources().getStringArray(R.array.onoff_LP));
            everyday1.setEntryValues(getResources().getStringArray(R.array.onoff_values_LP));
            everyday2.setEntries(getResources().getStringArray(R.array.onoff_LP));
            everyday2.setEntryValues(getResources().getStringArray(R.array.onoff_values_LP));
            everyday3.setEntries(getResources().getStringArray(R.array.onoff_LP));
            everyday3.setEntryValues(getResources().getStringArray(R.array.onoff_values_LP));
        }
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));

        if (mSimQuantity == 1) {
            getPreferenceScreen().removePreference(sim2);
            getPreferenceScreen().removePreference(sim3);
            getPreferenceScreen().removePreference(changeSIM);
            prefer1.setEnabled(false);
        }
        if (mSimQuantity == 2) {
            getPreferenceScreen().removePreference(sim3);
        }

        if (mIsAttached)
            updateSummary();

        day1.setOnPreferenceChangeListener(this);
        day2.setOnPreferenceChangeListener(this);
        day3.setOnPreferenceChangeListener(this);

        /*day1.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
        day2.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
        day3.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});*/


        /*int sim = getActivity().getIntent().getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
        if (sim != Constants.DISABLED) {
            String key = "";
            // the preference screen your item is in must be known
            switch (sim) {
                case R.id.limit1:
                case Constants.SIM1:
                    key = "sim1";
                    break;
                case R.id.limit2:
                case Constants.SIM2:
                    key = "sim2";
                    break;
                case R.id.limit3:
                case Constants.SIM3:
                    key = "sim3";
                    break;
            }
            // the position of your item inside the preference screen above
            if (!key.equals("")) {
                int pos = getPreferenceScreen().findPreference(key).getOrder();
                // simulate a click / call it!!
                getPreferenceScreen().onItemClick(null, null, pos, 0);
            }
        }*/

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
            limit1.setSummary(limit1.getText());
        if (limit2 != null)
            limit2.setSummary(limit2.getText());
        if (limit3 != null)
            limit3.setSummary(limit3.getText());
        if (value1 != null)
            value1.setSummary(value1.getEntry());
        if (value2 != null)
            value2.setSummary(value2.getEntry());
        if (value3 != null)
            value3.setSummary(value3.getEntry());
        //night
        if (limit1N != null)
            limit1N.setSummary(limit1N.getText());
        if (limit2N != null)
            limit2N.setSummary(limit2N.getText());
        if (limit3N != null)
            limit3N.setSummary(limit3N.getText());
        if (value1N != null)
            value1N.setSummary(value1N.getEntry());
        if (value2N != null)
            value2N.setSummary(value2N.getEntry());
        if (value3N != null)
            value3N.setSummary(value3N.getEntry());

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
            round1.setSummary(round1.getText() + "%");
        if (round2 != null)
            round2.setSummary(round2.getText() + "%");
        if (round3 != null)
            round3.setSummary(round3.getText() + "%");
        if (mSimQuantity == 3)
            if (prefer1 != null && prefer2 != null && prefer3 != null) {
                if (prefer1.isChecked()) {
                    prefer2.setEnabled(false);
                    prefer2.setChecked(false);
                    prefer3.setEnabled(false);
                    prefer3.setChecked(false);
                } else if (prefer2.isChecked()) {
                    prefer1.setEnabled(false);
                    prefer1.setChecked(false);
                    prefer3.setEnabled(false);
                    prefer3.setChecked(false);
                } else if (prefer3.isChecked()) {
                    prefer1.setEnabled(false);
                    prefer1.setChecked(false);
                    prefer2.setEnabled(false);
                    prefer2.setChecked(false);
                } else {
                    prefer1.setEnabled(true);
                    prefer2.setEnabled(true);
                    prefer3.setEnabled(true);
                }
            }
        if (mSimQuantity == 2)
            if (prefer1 != null && prefer2 != null && prefer3 != null) {
                if (prefer1.isChecked()) {
                    prefer2.setEnabled(false);
                    prefer2.setChecked(false);
                } else if (prefer2.isChecked()) {
                    prefer1.setEnabled(false);
                    prefer1.setChecked(false);
                } else {
                    prefer1.setEnabled(true);
                    prefer2.setEnabled(true);
                }
            }
        if (time1 != null)
            time1.setSummary(mPrefs.getString(Constants.PREF_SIM1[9], "00:00"));
        if (time2 != null)
            time2.setSummary(mPrefs.getString(Constants.PREF_SIM2[9], "00:00"));
        if (time3 != null)
            time3.setSummary(mPrefs.getString(Constants.PREF_SIM3[9], "00:00"));

        if (everyday1 != null) {
            everyday1.setSummary(everyday1.getEntry());
            switch (everyday1.getValue()) {
                case "0":
                    tOff1.setEnabled(true);
                    tOn1.setEnabled(true);
                    break;
                case "1":
                    tOff1.setEnabled(true);
                    tOn1.setEnabled(false);
                    break;
                case "2":
                    tOff1.setEnabled(false);
                    tOn1.setEnabled(true);
                    break;
                default:
                case "3":
                    tOff1.setEnabled(false);
                    tOn1.setEnabled(false);
                    break;
            }
        }
        if (everyday2 != null) {
            everyday2.setSummary(everyday2.getEntry());
            switch (everyday2.getValue()) {
                case "0":
                    tOff2.setEnabled(true);
                    tOn2.setEnabled(true);
                    break;
                case "1":
                    tOff2.setEnabled(true);
                    tOn2.setEnabled(false);
                    break;
                case "2":
                    tOff2.setEnabled(false);
                    tOn2.setEnabled(true);
                    break;
                default:
                case "3":
                    tOff2.setEnabled(false);
                    tOn2.setEnabled(false);
                    break;
            }
        }
        if (everyday3 != null) {
            everyday3.setSummary(everyday3.getEntry());
            switch (everyday3.getValue()) {
                case "0":
                    tOff3.setEnabled(true);
                    tOn3.setEnabled(true);
                    break;
                case "1":
                    tOff3.setEnabled(true);
                    tOn3.setEnabled(false);
                    break;
                case "2":
                    tOff3.setEnabled(false);
                    tOn3.setEnabled(true);
                    break;
                default:
                case "3":
                    tOff3.setEnabled(false);
                    tOn3.setEnabled(false);
                    break;
            }
        }

        if (tOn1 != null)
            tOn1.setSummary(mPrefs.getString(Constants.PREF_SIM1[13], "00:05"));
        if (tOn2 != null)
            tOn2.setSummary(mPrefs.getString(Constants.PREF_SIM2[13], "00:05"));
        if (tOn3 != null)
            tOn3.setSummary(mPrefs.getString(Constants.PREF_SIM3[13], "00:05"));
        if (tOff1 != null)
            tOff1.setSummary(mPrefs.getString(Constants.PREF_SIM1[12], "23:55"));
        if (tOff2 != null)
            tOff2.setSummary(mPrefs.getString(Constants.PREF_SIM2[12], "23:55"));
        if (tOff3 != null)
            tOff3.setSummary(mPrefs.getString(Constants.PREF_SIM3[12], "23:55"));

        //night
        if (round1N != null)
            round1N.setSummary(round1N.getText() + "%");
        if (round2N != null)
            round2N.setSummary(round2N.getText() + "%");
        if (round3N != null)
            round3N.setSummary(round3N.getText() + "%");
        if (tOn1N != null)
            tOn1N.setSummary(mPrefs.getString(Constants.PREF_SIM1[20], "23:00"));
        if (tOn2N != null)
            tOn2N.setSummary(mPrefs.getString(Constants.PREF_SIM2[20], "23:00"));
        if (tOn3N != null)
            tOn3N.setSummary(mPrefs.getString(Constants.PREF_SIM3[20], "23:00"));
        if (tOff1N != null)
            tOff1N.setSummary(mPrefs.getString(Constants.PREF_SIM1[21], "06:00"));
        if (tOff2N != null)
            tOff2N.setSummary(mPrefs.getString(Constants.PREF_SIM2[21], "06:00"));
        if (tOff3N != null)
            tOff3N.setSummary(mPrefs.getString(Constants.PREF_SIM3[21], "06:00"));

        if (opLimit1 != null)
            opLimit1.setSummary(opLimit1.getText());
        if (opLimit2 != null)
            opLimit2.setSummary(opLimit2.getText());
        if (opLimit3 != null)
            opLimit3.setSummary(opLimit3.getText());
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
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);;
        toolBar.setTitle(R.string.limit_title);
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

        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        DateTime alarmTime;
        //Scheduled ON/OFF
        if (key.equals(Constants.PREF_SIM1[11]) || key.equals(Constants.PREF_SIM1[12]) || key.equals(Constants.PREF_SIM1[13])) {
            Intent i1Off = new Intent(mContext, OnOffReceiver.class);
            i1Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
            i1Off.putExtra(Constants.ON_OFF, false);
            i1Off.setAction(Constants.ALARM_ACTION);
            final int SIM1_OFF = 100;
            PendingIntent pi1Off = PendingIntent.getBroadcast(mContext, SIM1_OFF, i1Off, 0);
            if (sharedPreferences.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    sharedPreferences.getString(Constants.PREF_SIM1[11], "0").equals("1")) {
                am.cancel(pi1Off);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM1[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM1[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1Off);
            } else
                am.cancel(pi1Off);

            Intent i1On = new Intent(mContext, OnOffReceiver.class);
            i1On.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
            i1On.putExtra(Constants.ON_OFF, true);
            i1On.setAction(Constants.ALARM_ACTION);
            final int SIM1_ON = 101;
            PendingIntent pi1On = PendingIntent.getBroadcast(mContext, SIM1_ON, i1On, 0);
            if (sharedPreferences.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    sharedPreferences.getString(Constants.PREF_SIM1[11], "0").equals("2")) {
                am.cancel(pi1On);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM1[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM1[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);;
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1On);
            } else
                am.cancel(pi1On);
        }
        if (key.equals(Constants.PREF_SIM2[11]) || key.equals(Constants.PREF_SIM2[12]) || key.equals(Constants.PREF_SIM2[13])) {
            Intent i2Off = new Intent(mContext, OnOffReceiver.class);
            i2Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
            i2Off.putExtra(Constants.ON_OFF, false);
            i2Off.setAction(Constants.ALARM_ACTION);
            final int SIM2_OFF = 110;
            PendingIntent pi2Off = PendingIntent.getBroadcast(mContext, SIM2_OFF, i2Off, 0);
            if (sharedPreferences.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    sharedPreferences.getString(Constants.PREF_SIM2[11], "0").equals("1")) {
                am.cancel(pi2Off);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM2[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM2[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2Off);
            } else
                am.cancel(pi2Off);

            Intent i2On = new Intent(mContext, OnOffReceiver.class);
            i2On.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
            i2On.putExtra(Constants.ON_OFF, true);
            i2On.setAction(Constants.ALARM_ACTION);
            final int SIM2_ON = 111;
            PendingIntent pi2On = PendingIntent.getBroadcast(mContext, SIM2_ON, i2On, 0);
            if (sharedPreferences.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    sharedPreferences.getString(Constants.PREF_SIM2[11], "0").equals("2")) {
                am.cancel(pi2On);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM2[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM2[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2On);
            } else
                am.cancel(pi2On);
        }
        if (key.equals(Constants.PREF_SIM3[11]) || key.equals(Constants.PREF_SIM3[12]) || key.equals(Constants.PREF_SIM3[13])) {
            Intent i3Off = new Intent(mContext, OnOffReceiver.class);
            i3Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
            i3Off.putExtra(Constants.ON_OFF, false);
            i3Off.setAction(Constants.ALARM_ACTION);
            final int SIM3_OFF = 120;
            PendingIntent pi3Off = PendingIntent.getBroadcast(mContext, SIM3_OFF, i3Off, 0);
            if (sharedPreferences.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    sharedPreferences.getString(Constants.PREF_SIM3[11], "0").equals("1")) {
                am.cancel(pi3Off);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM3[12], "23:35").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM3[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3Off);
            } else
                am.cancel(pi3Off);

            Intent i3On = new Intent(mContext, OnOffReceiver.class);
            i3On.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
            i3On.putExtra(Constants.ON_OFF, true);
            i3On.setAction(Constants.ALARM_ACTION);
            final int SIM3_ON = 121;
            PendingIntent pi3On = PendingIntent.getBroadcast(mContext, SIM3_ON, i3On, 0);
            if (sharedPreferences.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    sharedPreferences.getString(Constants.PREF_SIM3[11], "0").equals("2")) {
                am.cancel(pi3On);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM3[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM3[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3On);
            } else
                am.cancel(pi3On);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        switch (preference.getKey()) {
            case "day1":
            case "day2":
            case "day3":
                String input = o.toString();
                if (input.matches("[0-9]+") && (Integer.valueOf(input) >= 1 && Integer.valueOf(input) <= 31))
                    return true;
                break;
        }
        Toast.makeText(getActivity(), R.string.check_input, Toast.LENGTH_LONG).show();
        return false;
    }
}
