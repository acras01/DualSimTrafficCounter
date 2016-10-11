package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

public class NotificationSwitch implements CompoundButton.OnCheckedChangeListener {

    protected final Context mContext;
    private SharedPreferences mPrefs;
    private SwitchCompat mSwitch;

    public NotificationSwitch(Context context, SwitchCompat swtch) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        setSwitch(swtch);
    }

    public void setSwitch(SwitchCompat swtch) {
        if (mSwitch == swtch)
            return;
        if (mSwitch != null)
            mSwitch.setOnCheckedChangeListener(null);
        mSwitch = swtch;
        mSwitch.setOnCheckedChangeListener(this);
        mSwitch.setChecked(isSwitchOn());
    }

    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(Constants.PREF_OTHER[3], isChecked)
                .apply();
    }

    public boolean isSwitchOn() {
        return mPrefs.getBoolean(Constants.PREF_OTHER[3], true);
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        mSwitch.setChecked(isSwitchOn());
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }
}

