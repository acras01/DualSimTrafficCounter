package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SoundEnabler implements CompoundButton.OnCheckedChangeListener {

	protected final Context mContext;
	private Switch mSwitch;

	public SoundEnabler(Context context, Switch swtch) {
		mContext = context;
		setSwitch(swtch);
	}

	public void setSwitch(Switch swtch) {
		if (mSwitch == swtch)
			return;
		/*if (mSwitch != null)
			mSwitch.setOnCheckedChangeListener(null);*/
		mSwitch = swtch;
		mSwitch.setOnCheckedChangeListener(this);
		mSwitch.setChecked(isSwitchOn());
	}

	public void onCheckedChanged(CompoundButton view, boolean isChecked) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
		editor.putBoolean(Constants.PREF_OTHER[3], isChecked);
		editor.apply();
	}

	public boolean isSwitchOn() {
		return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Constants.PREF_OTHER[3], true);
	}

	public void resume() {
		mSwitch.setOnCheckedChangeListener(this);
		mSwitch.setChecked(isSwitchOn());
	}

	public void pause() {
		mSwitch.setOnCheckedChangeListener(null);
	}

}
