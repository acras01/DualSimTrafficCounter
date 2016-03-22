package ua.od.acros.dualsimtrafficcounter.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;

public class SettingsFragment extends Fragment implements View.OnClickListener, Switch.OnCheckedChangeListener {

    private SharedPreferences mPrefs;
    private SwitchCompat switchCompat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = CustomApplication.getAppContext();
        mPrefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.use_notification_title);
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        RelativeLayout calls = (RelativeLayout) view.findViewById(R.id.calls_layout);
        if (!mPrefs.getBoolean(Constants.PREF_OTHER[25], false))
            calls.setVisibility(View.GONE);
        switchCompat = (SwitchCompat) view.findViewById(R.id.notif_sw);
        switchCompat.setChecked(mPrefs.getBoolean(Constants.PREF_OTHER[3], true));
        view.findViewById(R.id.notif_touch_layout).setOnClickListener(this);
        view.findViewById(R.id.traff_layout).setOnClickListener(this);
        view.findViewById(R.id.calls_layout).setOnClickListener(this);
        view.findViewById(R.id.operator_layout).setOnClickListener(this);
        view.findViewById(R.id.other_layout).setOnClickListener(this);
        switchCompat.setOnCheckedChangeListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);;
        toolBar.setTitle(R.string.action_settings);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.notif_touch_layout:
                switchCompat.setChecked(true);
                ((SettingsActivity) getActivity()).replaceFragments(SoundFragment.class);
                break;
            case R.id.traff_layout:
                ((SettingsActivity) getActivity()).replaceFragments(TrafficLimitFragment.class);
                break;
            case R.id.calls_layout:
                ((SettingsActivity) getActivity()).replaceFragments(CallsLimitFragment.class);
                break;
            case R.id.operator_layout:
                ((SettingsActivity) getActivity()).replaceFragments(OperatorFragment.class);
                break;
            case R.id.other_layout:
                ((SettingsActivity) getActivity()).replaceFragments(OtherFragment.class);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mPrefs.edit()
                .putBoolean(Constants.PREF_OTHER[3], isChecked)
                .apply();
    }
}
