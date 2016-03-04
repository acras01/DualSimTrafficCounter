package ua.od.acros.dualsimtrafficcounter.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class SettingsFragment extends Fragment implements View.OnClickListener, Switch.OnCheckedChangeListener {

    private Context mContext;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        mPrefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        RelativeLayout calls = (RelativeLayout) view.findViewById(R.id.calls_layout);
        if (!mPrefs.getBoolean(Constants.PREF_OTHER[25], false))
            calls.setVisibility(View.GONE);
        TextView notif = (TextView) view.findViewById(R.id.notif_tv);
        TextView notifSum = (TextView) view.findViewById(R.id.notif_sum);
        Switch notifSw = (Switch) view.findViewById(R.id.notif_sw);
        notif.setOnClickListener(this);
        notifSum.setOnClickListener(this);
        notifSw.setOnCheckedChangeListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.notif_tv:
            case R.id.notif_sum:

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }
}
