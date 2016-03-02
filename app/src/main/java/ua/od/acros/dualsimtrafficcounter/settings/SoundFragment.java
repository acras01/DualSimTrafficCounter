package ua.od.acros.dualsimtrafficcounter.settings;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.Switch;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.SoundEnabler;

public class SoundFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SoundEnabler mSoundEnabler;
    private Context mContext;

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContext = getActivity().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .registerOnSharedPreferenceChangeListener(this);

        addPreferencesFromResource(R.xml.notification);

        ActionBar actionbar = getActivity().getActionBar();
        Switch actionBarSwitch = new Switch(mContext);
        if (actionbar != null) {
            actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            actionbar.setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
                    | Gravity.RIGHT));
            actionbar.setTitle(R.string.use_notification_title);
        }
        mSoundEnabler = new SoundEnabler(mContext, actionBarSwitch);
        updateSettings();
    }

    protected void updateSettings() {
        boolean available = mSoundEnabler.isSwitchOn();
        int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; ++i) {
            Preference pref = getPreferenceScreen().getPreference(i);
            pref.setEnabled(available);
            if (pref.getKey().equals(Constants.PREF_OTHER[1]))
                pref.setSummary(RingtoneManager.getRingtone(mContext,
                        Uri.parse(pref.getSharedPreferences().getString(Constants.PREF_OTHER[1], getResources().getString(R.string.not_set))))
                        .getTitle(mContext));
        }
    }

    public void onResume() {
        super.onResume();
        mSoundEnabler.resume();
        updateSettings();
    }

    public void onPause() {
        super.onPause();
        mSoundEnabler.pause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[3]))
            updateSettings();
    }
}
