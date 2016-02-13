package ua.od.acros.dualsimtrafficcounter.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import ua.od.acros.dualsimtrafficcounter.R;

public class TimePreference extends DialogPreference {
    private int mLastHour = 0;
    private int mLastMinute = 0;
    private TimePicker mPicker = null;

    public static int getHour(String time) {
        String[] pieces=time.split(":");
        return(Integer.parseInt(pieces[0]));
    }

    public static int getMinute(String time) {
        String[] pieces = time.split(":");
        return(Integer.parseInt(pieces[1]));
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        setPositiveButtonText(R.string.set);
        setNegativeButtonText(android.R.string.cancel);
    }

    /*@Override
    protected View onCreateDialogView() {
        mPicker = new TimePicker(getContext());
        if (!DateFormat.is24HourFormat(getContext()))
            mPicker.setIs24HourView(false);
        else
            mPicker.setIs24HourView(true);
        return(mPicker);
    }*/

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mPicker = (TimePicker) v.findViewById(R.id.prefTimePicker);
        if (!DateFormat.is24HourFormat(getContext()))
            mPicker.setIs24HourView(false);
        else
            mPicker.setIs24HourView(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mPicker.setHour(mLastHour);
            mPicker.setMinute(mLastMinute);
        } else {
            mPicker.setCurrentHour(mLastHour);
            mPicker.setCurrentMinute(mLastMinute);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mLastHour = mPicker.getHour();
                mLastMinute = mPicker.getMinute();
            } else {
                mLastHour = mPicker.getCurrentHour();
                mLastMinute = mPicker.getCurrentMinute();
            }
            String hour, minute;
            if (mLastHour <= 9)
                hour = "0" + String.valueOf(mLastHour);
            else
                hour = String.valueOf(mLastHour);
            if (mLastMinute <= 9)
                minute = "0" + String.valueOf(mLastMinute);
            else
                minute = String.valueOf(mLastMinute);
            String time = hour + ":" + minute;
            if (callChangeListener(time)) {
                persistString(time);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return(a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time;
        if (restoreValue) {
            if (defaultValue == null) {
                time = getPersistedString("00:00");
            }
            else {
                time = getPersistedString(defaultValue.toString());
            }
        }
        else {
            time = defaultValue.toString();
        }
        mLastHour = getHour(time);
        mLastMinute = getMinute(time);
    }
}