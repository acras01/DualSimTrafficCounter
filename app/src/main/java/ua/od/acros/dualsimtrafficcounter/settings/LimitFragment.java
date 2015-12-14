package ua.od.acros.dualsimtrafficcounter.settings;


import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputFilter;

import com.stericson.RootTools.RootTools;

import java.util.Calendar;

import ua.od.acros.dualsimtrafficcounter.OnOffReceiver;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.TimePreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.InputFilterMinMax;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;

public class LimitFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private EditTextPreference limit1, limit2, limit3, round1, round2, round3, day1, day2, day3,opLimit1, opLimit2, opLimit3;
    private ListPreference value1, period1, value2, period2, value3, period3, opValue1, opValue2, opValue3;
    private TwoLineCheckPreference prefer1, prefer2, prefer3; //everyday1, everyday2, everyday3;
    private TimePreference time1, time2, time3, tOn1, tOff1, tOn2, tOff2, tOn3, tOff3;

    private final int SIM1_OFF = 100;
    private final int SIM1_ON = 101;
    private final int SIM2_OFF = 110;
    private final int SIM2_ON = 111;
    private final int SIM3_OFF = 120;
    private final int SIM3_ON = 121;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.limit);
        ActionBar actionbar = getActivity().getActionBar();
        if (actionbar != null)
            actionbar.setTitle(R.string.limit_title);

        limit1 = (EditTextPreference) findPreference(Constants.PREF_SIM1[1]);
        limit2 = (EditTextPreference) findPreference(Constants.PREF_SIM2[1]);
        limit3 = (EditTextPreference) findPreference(Constants.PREF_SIM3[1]);
        value1 = (ListPreference) findPreference(Constants.PREF_SIM1[2]);
        value2 = (ListPreference) findPreference(Constants.PREF_SIM2[2]);
        value3 = (ListPreference) findPreference(Constants.PREF_SIM3[2]);
        period1 = (ListPreference) findPreference(Constants.PREF_SIM1[3]);
        period2 = (ListPreference) findPreference(Constants.PREF_SIM2[3]);
        period3 = (ListPreference) findPreference(Constants.PREF_SIM3[3]);
        round1 = (EditTextPreference) findPreference(Constants.PREF_SIM1[4]);
        round2 = (EditTextPreference) findPreference(Constants.PREF_SIM2[4]);
        round3 = (EditTextPreference) findPreference(Constants.PREF_SIM3[4]);
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
        day1 = (EditTextPreference) findPreference(Constants.PREF_SIM1[10]);
        day2 = (EditTextPreference) findPreference(Constants.PREF_SIM2[10]);
        day3 = (EditTextPreference) findPreference(Constants.PREF_SIM3[10]);
        //everyday1 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM1[11]);
        //everyday2 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM2[11]);
        //everyday3 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM3[11]);
        tOn1 = (TimePreference) findPreference(Constants.PREF_SIM1[13]);
        tOn2 = (TimePreference) findPreference(Constants.PREF_SIM2[13]);
        tOn3 = (TimePreference) findPreference(Constants.PREF_SIM3[13]);
        tOff1 = (TimePreference) findPreference(Constants.PREF_SIM1[12]);
        tOff2 = (TimePreference) findPreference(Constants.PREF_SIM2[12]);
        tOff3 = (TimePreference) findPreference(Constants.PREF_SIM3[12]);
        opLimit1 = (EditTextPreference) findPreference(Constants.PREF_SIM1[15]);
        opLimit2 = (EditTextPreference) findPreference(Constants.PREF_SIM2[15]);
        opLimit3 = (EditTextPreference) findPreference(Constants.PREF_SIM3[15]);
        opValue1 = (ListPreference) findPreference(Constants.PREF_SIM1[16]);
        opValue2 = (ListPreference) findPreference(Constants.PREF_SIM2[16]);
        opValue3 = (ListPreference) findPreference(Constants.PREF_SIM3[16]);

        PreferenceCategory sim2 = (PreferenceCategory) getPreferenceScreen().findPreference("sim2");
        PreferenceCategory sim3 = (PreferenceCategory) getPreferenceScreen().findPreference("sim3");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && !RootTools.isRootAvailable()) {
            autoff1.setChecked(false);
            autoff1.setEnabled(false);
            autoff2.setChecked(false);
            autoff2.setEnabled(false);
            autoff3.setChecked(false);
            autoff3.setEnabled(false);
        }
        if (MobileDataControl.isMultiSim(getActivity()) == 1) {
            getPreferenceScreen().removePreference(sim2);
            getPreferenceScreen().removePreference(sim3);
            getPreferenceScreen().removePreference(changeSIM);
            prefer1.setEnabled(false);
        }
        if (MobileDataControl.isMultiSim(getActivity()) == 2) {
            getPreferenceScreen().removePreference(sim3);
        }
        updateSummary();

        day1.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
        day2.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
        day3.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
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
        if (period1 != null) {
            period1.setSummary(period1.getEntry());
            if (period1.getValue().equals("0") && day1 != null)
                day1.setEnabled(false);
            if (period1.getValue().equals("1") && day1 != null)
                day1.setEnabled(true);
        }
        if (period2 != null) {
            period2.setSummary(period2.getEntry());
            if (period2.getValue().equals("0") && day2 != null)
                day2.setEnabled(false);
            if (period2.getValue().equals("1") && day2 != null)
                day2.setEnabled(true);
        }
        if (period3 != null) {
            period3.setSummary(period3.getEntry());
            if (period3.getValue().equals("0") && day3 != null)
                day3.setEnabled(false);
            if (period3.getValue().equals("1") && day3 != null)
                day3.setEnabled(true);
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
        if (MobileDataControl.isMultiSim(getActivity()) == 3)
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
        if (MobileDataControl.isMultiSim(getActivity()) == 2)
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
            time1.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM1[9], "00:00"));
        if (time2 != null)
            time2.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM2[9], "00:00"));
        if (time3 != null)
            time3.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM3[9], "00:00"));

        if (tOn1 != null)
            tOn1.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM1[13], "00:05"));
        if (tOn2 != null)
            tOn2.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM2[13], "00:05"));
        if (tOn3 != null)
            tOn3.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM3[13], "00:05"));
        if (tOff1 != null)
            tOff1.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM1[12], "23:55"));
        if (tOff2 != null)
            tOff2.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM2[12], "23:55"));
        if (tOff3 != null)
            tOff3.setSummary(getPreferenceScreen().getSharedPreferences().getString(Constants.PREF_SIM3[12], "23:55"));

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
        AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Calendar clndr = Calendar.getInstance();
        if (key.equals(Constants.PREF_SIM1[11]) || key.equals(Constants.PREF_SIM1[12]) || key.equals(Constants.PREF_SIM1[13])) {
            Intent i1Off = new Intent(getActivity(), OnOffReceiver.class);
            i1Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
            i1Off.putExtra(Constants.ON_OFF, false);
            i1Off.setAction(Constants.ALARM_ACTION);
            PendingIntent pi1Off = PendingIntent.getBroadcast(getActivity(), SIM1_OFF, i1Off, 0);
            if (sharedPreferences.getBoolean(Constants.PREF_SIM1[11], false)) {
                am.cancel(pi1Off);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM1[12], "23:55").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM1[12], "23:55").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi1Off);
            } else
                am.cancel(pi1Off);

            Intent i1On = new Intent(getActivity(), OnOffReceiver.class);
            i1On.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
            i1On.putExtra(Constants.ON_OFF, true);
            i1On.setAction(Constants.ALARM_ACTION);
            PendingIntent pi1On = PendingIntent.getBroadcast(getActivity(), SIM1_ON, i1On, 0);
            if (sharedPreferences.getBoolean(Constants.PREF_SIM1[11], false)) {
                am.cancel(pi1On);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM1[13], "00:05").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM1[13], "00:05").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi1On);
            } else
                am.cancel(pi1On);
        }
        if (key.equals(Constants.PREF_SIM2[11]) || key.equals(Constants.PREF_SIM2[12]) || key.equals(Constants.PREF_SIM2[13])) {
            Intent i2Off = new Intent(getActivity(), OnOffReceiver.class);
            i2Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
            i2Off.putExtra(Constants.ON_OFF, false);
            i2Off.setAction(Constants.ALARM_ACTION);
            PendingIntent pi2Off = PendingIntent.getBroadcast(getActivity(), SIM2_OFF, i2Off, 0);
            if (sharedPreferences.getBoolean(Constants.PREF_SIM2[11], false)) {
                am.cancel(pi2Off);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM2[12], "23:55").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM2[12], "23:55").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi2Off);
            } else
                am.cancel(pi2Off);

            Intent i2On = new Intent(getActivity(), OnOffReceiver.class);
            i2On.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
            i2On.putExtra(Constants.ON_OFF, true);
            i2On.setAction(Constants.ALARM_ACTION);
            PendingIntent pi2On = PendingIntent.getBroadcast(getActivity(), SIM2_ON, i2On, 0);
            if (sharedPreferences.getBoolean(Constants.PREF_SIM2[11], false)) {
                am.cancel(pi2On);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM2[13], "00:05").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM2[13], "00:05").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi2On);
            } else
                am.cancel(pi2On);
        }
        if (key.equals(Constants.PREF_SIM3[11]) || key.equals(Constants.PREF_SIM3[12]) || key.equals(Constants.PREF_SIM3[13])) {
            Intent i3Off = new Intent(getActivity(), OnOffReceiver.class);
            i3Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
            i3Off.putExtra(Constants.ON_OFF, false);
            i3Off.setAction(Constants.ALARM_ACTION);
            PendingIntent pi3Off = PendingIntent.getBroadcast(getActivity(), SIM3_OFF, i3Off, 0);
            if (sharedPreferences.getBoolean(Constants.PREF_SIM3[11], false)) {
                am.cancel(pi3Off);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM3[12], "23:35").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM3[12], "23:55").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi3Off);
            } else
                am.cancel(pi3Off);

            Intent i3On = new Intent(getActivity(), OnOffReceiver.class);
            i3On.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
            i3On.putExtra(Constants.ON_OFF, true);
            i3On.setAction(Constants.ALARM_ACTION);
            PendingIntent pi3On = PendingIntent.getBroadcast(getActivity(), SIM3_ON, i3On, 0);
            if (sharedPreferences.getBoolean(Constants.PREF_SIM3[11], false)) {
                am.cancel(pi3On);
                clndr.setTimeInMillis(System.currentTimeMillis());
                clndr.set(Calendar.HOUR_OF_DAY, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM3[13], "00:05").split(":")[0]));
                clndr.set(Calendar.MINUTE, Integer.valueOf(sharedPreferences.getString(Constants.PREF_SIM3[13], "00:05").split(":")[1]));
                clndr.set(Calendar.SECOND, 0);
                clndr.set(Calendar.MILLISECOND, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, clndr.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi3On);
            } else
                am.cancel(pi3On);
        }
    }
}
