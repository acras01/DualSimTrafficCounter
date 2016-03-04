package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

import ua.od.acros.dualsimtrafficcounter.preferences.TimePreference;

public class TimePreferenceDialog extends PreferenceDialogFragmentCompat implements DialogPreference.TargetFragment
{
    TimePicker mTimePicker = null;

    @Override
    protected View onCreateDialogView(Context context)
    {
        mTimePicker = new TimePicker(context);
        return (mTimePicker);
    }

    @Override
    protected void onBindDialogView(View v)
    {
        super.onBindDialogView(v);
        if (!DateFormat.is24HourFormat(getContext()))
            mTimePicker.setIs24HourView(false);
        else
            mTimePicker.setIs24HourView(true);

        TimePreference pref = (TimePreference) getPreference();
        mTimePicker.setCurrentHour(pref.hour);
        mTimePicker.setCurrentMinute(pref.minute);
    }

    @Override
    public void onDialogClosed(boolean positiveResult)
    {
        if (positiveResult)
        {
            TimePreference pref = (TimePreference) getPreference();
            pref.hour = mTimePicker.getCurrentHour();
            pref.minute = mTimePicker.getCurrentMinute();

            String value = TimePreference.timeToString(pref.hour, pref.minute);
            if (pref.callChangeListener(value)) pref.persistStringValue(value);
        }
    }

    @Override
    public Preference findPreference(CharSequence charSequence)
    {
        return getPreference();
    }
}