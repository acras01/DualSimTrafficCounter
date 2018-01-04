package ua.od.acros.dualsimtrafficcounter.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomSwitch;

public class SettingsFragment extends Fragment implements View.OnClickListener {

    private SharedPreferences mPrefs;
    private CustomSwitch mSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mSwitch = new CustomSwitch(context, new SwitchCompat(context), Constants.PREF_OTHER[3]);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle(R.string.action_settings);
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        RelativeLayout calls = view.findViewById(R.id.calls_layout);
        if (!mPrefs.getBoolean(Constants.PREF_OTHER[25], false))
            calls.setVisibility(View.GONE);
        RelativeLayout widgets = view.findViewById(R.id.widgets_layout);
        if (CustomApplication.getWidgetIds(Constants.TRAFFIC).length == 0 &&
                CustomApplication.getWidgetIds(Constants.CALLS).length == 0)
            widgets.setVisibility(View.GONE);
        mSwitch.setSwitch((SwitchCompat) view.findViewById(R.id.notif_sw));
        view.findViewById(R.id.notif_touch_layout).setOnClickListener(this);
        view.findViewById(R.id.traff_layout).setOnClickListener(this);
        calls.setOnClickListener(this);
        view.findViewById(R.id.operator_layout).setOnClickListener(this);
        view.findViewById(R.id.other_layout).setOnClickListener(this);
        widgets.setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitch.resume();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setTitle(R.string.action_settings);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwitch.pause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.notif_touch_layout:
                mSwitch.setChecked(true);
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
            case R.id.widgets_layout:
                ((SettingsActivity) getActivity()).replaceFragments(WidgetsFragment.class);
                break;
            case R.id.other_layout:
                ((SettingsActivity) getActivity()).replaceFragments(OtherFragment.class);
                break;
        }
    }
}
