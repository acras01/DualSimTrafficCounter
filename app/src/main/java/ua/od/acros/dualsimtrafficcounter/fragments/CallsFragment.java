package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import ua.od.acros.dualsimtrafficcounter.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.settings.CallsLimitFragment;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CallsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CallsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CallsFragment extends Fragment implements View.OnClickListener {

    private TextView TOT1, TOT2, TOT3, SIM1, SIM2, SIM3, TIP;
    private ContentValues mCalls;
    private Button bLim1, bLim2, bLim3;
    private MyDatabase mDatabaseHelper;
    private SharedPreferences mPrefs;
    private String[] mOperatorNames = new String[3];
    private int simQuantity;

    private OnFragmentInteractionListener mListener;
    private BroadcastReceiver callDataReceiver;
    private MenuItem mService;

    public static CallsFragment newInstance(String param1, String param2) {
        return new CallsFragment();
    }

    public CallsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mOperatorNames[0] = MobileUtils.getName(getActivity(), Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        mOperatorNames[1] = MobileUtils.getName(getActivity(), Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        mOperatorNames[2] = MobileUtils.getName(getActivity(), Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        mDatabaseHelper = MyDatabase.getInstance(getActivity());
        mCalls = MyDatabase.readTrafficData(mDatabaseHelper);
        mPrefs = getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        simQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getActivity())
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));

        callDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                switch (sim) {
                    case Constants.SIM1:
                        TOT1.setText(DataFormat.formatCallDuration(getActivity(), duration));
                        break;
                    case Constants.SIM2:
                        TOT2.setText(DataFormat.formatCallDuration(getActivity(), duration));
                        break;
                    case Constants.SIM3:
                        TOT3.setText(DataFormat.formatCallDuration(getActivity(), duration));
                        break;
                }
            }
        };
        IntentFilter callDataFilter = new IntentFilter(Constants.CALLS);
        getActivity().registerReceiver(callDataReceiver, callDataFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calls_fragment, container, false);
        TOT1 = (TextView) view.findViewById(R.id.Tot1);
        TOT2 = (TextView) view.findViewById(R.id.Tot2);
        TOT3 = (TextView) view.findViewById(R.id.Tot3);
        SIM1 = (TextView) view.findViewById(R.id.sim1_name);
        SIM2 = (TextView) view.findViewById(R.id.sim2_name);
        SIM3 = (TextView) view.findViewById(R.id.sim3_name);
        TIP = (TextView) view.findViewById(R.id.tip);

        bLim1 = (Button) view.findViewById(R.id.limit1);
        bLim2 = (Button) view.findViewById(R.id.limit2);
        bLim3 = (Button) view.findViewById(R.id.limit3);

        view.findViewById(R.id.buttonClear1).setOnClickListener(this);
        view.findViewById(R.id.buttonClear2).setOnClickListener(this);
        view.findViewById(R.id.buttonClear3).setOnClickListener(this);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            SIM2.setVisibility(View.GONE);
            TOT2.setVisibility(View.GONE);
            view.findViewById(R.id.buttonClear2).setVisibility(View.GONE);
            bLim2.setVisibility(View.GONE);
            SIM3.setVisibility(View.GONE);
            TOT3.setVisibility(View.GONE);
            view.findViewById(R.id.buttonClear3).setVisibility(View.GONE);
            bLim3.setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.sim2row).setVisibility(View.GONE);
            view.findViewById(R.id.sim3row).setVisibility(View.GONE);
        }
        if (simQuantity >= 2)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                SIM2.setVisibility(View.VISIBLE);
                TOT2.setVisibility(View.VISIBLE);
                view.findViewById(R.id.buttonClear2).setVisibility(View.VISIBLE);
                bLim2.setVisibility(View.VISIBLE);
            } else
                view.findViewById(R.id.sim2row).setVisibility(View.VISIBLE);
        if (simQuantity == 3)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                SIM3.setVisibility(View.VISIBLE);
                TOT3.setVisibility(View.VISIBLE);
                view.findViewById(R.id.buttonClear3).setVisibility(View.VISIBLE);
                bLim3.setVisibility(View.VISIBLE);
            } else
                view.findViewById(R.id.sim3row).setVisibility(View.VISIBLE);

        bLim1.setOnClickListener(this);
        bLim2.setOnClickListener(this);
        bLim3.setOnClickListener(this);

        TOT1.setText(DataFormat.formatCallDuration(getActivity(), (long) mCalls.get(Constants.CALLS1)));
        TOT2.setText(DataFormat.formatCallDuration(getActivity(), (long) mCalls.get(Constants.CALLS2)));
        TOT3.setText(DataFormat.formatCallDuration(getActivity(), (long) mCalls.get(Constants.CALLS3)));

        SIM1.setText(mOperatorNames[0]);
        SIM2.setText(mOperatorNames[1]);
        SIM3.setText(mOperatorNames[2]);

        setButtonLimitText();

        // Inflate the layout for this fragment
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onCallsFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (callDataReceiver != null)
            getActivity().unregisterReceiver(callDataReceiver);
    }

    @Override
    public void onResume(){
        super.onResume();
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);;
        toolBar.setSubtitle(R.string.calls_fragment);
        setButtonLimitText();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonClear1:
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity())) {
                    Intent clear1Intent = new Intent(Constants.CLEAR);
                    clear1Intent.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                    getActivity().sendBroadcast(clear1Intent);
                } else {
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                    mCalls.put(Constants.CALLS1, 0L);
                    mCalls.put(Constants.CALLS1_EX, 0L);
                    MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                    TOT1.setText(DataFormat.formatCallDuration(getActivity(), 0L));
                }
                break;
            case R.id.buttonClear2:
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity())) {
                    Intent clear2Intent = new Intent(Constants.CLEAR);
                    clear2Intent.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                    getActivity().sendBroadcast(clear2Intent);
                } else {
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                    mCalls.put(Constants.CALLS2, 0L);
                    mCalls.put(Constants.CALLS3_EX, 0L);
                    MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                    TOT2.setText(DataFormat.formatCallDuration(getActivity(), 0L));
                }
                break;
            case R.id.buttonClear3:
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity())) {
                    Intent clear3Intent = new Intent(Constants.CLEAR);
                    clear3Intent.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                    getActivity().sendBroadcast(clear3Intent);
                } else {
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                    mCalls.put(Constants.CALLS3, 0L);
                    mCalls.put(Constants.CALLS3_EX, 0L);
                    MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                    TOT3.setText(DataFormat.formatCallDuration(getActivity(), 0L));
                }
                break;
            case R.id.limit1:
            case R.id.limit2:
            case R.id.limit3:
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, CallsLimitFragment.class.getName());
                intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                intent.putExtra(Constants.SIM_ACTIVE, v.getId());
                startActivity(intent);
                break;
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.calls_menu, menu);
        mService = menu.getItem(0);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        /*MenuInflater inflater = new MenuInflater(getActivity().getApplicationContext());
        menu.clear();
        onCreateOptionsMenu(menu, inflater);*/

        if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity())) {
            mService.setTitle(R.string.action_stop);
            mService.setIcon(R.drawable.ic_action_disable);
        }
        else {
            mService.setTitle(R.string.action_start);
            mService.setIcon(R.drawable.ic_action_enable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_service_start_stop:
                if (item.getTitle().toString().equals(getResources().getString(R.string.action_stop))) {
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[24], true).apply();
                    getActivity().stopService(new Intent(getActivity(), CallLoggerService.class));
                    TIP.setText(getResources().getString(R.string.service_disabled));
                    item.setTitle(R.string.action_start);
                    mService.setIcon(R.drawable.ic_action_enable);
                }
                else {
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[24], false).apply();
                    getActivity().startService(new Intent(getActivity(), CallLoggerService.class));
                    TIP.setText(getResources().getString(R.string.tip));
                    item.setTitle(R.string.action_stop);
                    mService.setIcon(R.drawable.ic_action_disable);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onCallsFragmentInteraction(Uri uri);
    }

    private void setButtonLimitText() {

        String limit1 = "";//mIsNight[0] ? mPrefs.getString(Constants.PREF_SIM1[18], "") : mPrefs.getString(Constants.PREF_SIM1[1], "");
        String limit2 = "";//mIsNight[1] ? mPrefs.getString(Constants.PREF_SIM2[18], "") : mPrefs.getString(Constants.PREF_SIM2[1], "");
        String limit3 = "";//mIsNight[2] ? mPrefs.getString(Constants.PREF_SIM3[18], "") : mPrefs.getString(Constants.PREF_SIM3[1], "");

        /*int value1;
        if (mPrefs.getString(Constants.PREF_SIM1[2], "").equals(""))
            value1 = 0;
        else
            value1 = mIsNight[0] ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[19], "")) :
                    Integer.valueOf(mPrefs.getString(Constants.PREF_SIM1[2], ""));
        int value2;
        if (mPrefs.getString(Constants.PREF_SIM2[2], "").equals(""))
            value2 = 0;
        else
            value2 = mIsNight[1] ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[19], "")) :
                    Integer.valueOf(mPrefs.getString(Constants.PREF_SIM2[2], ""));
        int value3;
        if (mPrefs.getString(Constants.PREF_SIM3[2], "").equals(""))
            value3 = 0;
        else
            value3 = mIsNight[2] ? Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[19], "")) :
                    Integer.valueOf(mPrefs.getString(Constants.PREF_SIM3[2], ""));

        double lim1 = !limit1.equals("") ? DataFormat.getFormatLong(limit1, value1) : Double.MAX_VALUE;
        double lim2 = !limit2.equals("") ? DataFormat.getFormatLong(limit2, value2) : Double.MAX_VALUE;
        double lim3 = !limit3.equals("") ? DataFormat.getFormatLong(limit3, value3) : Double.MAX_VALUE;

        limit1 = !limit1.equals("") ? DataFormat.formatData(getActivity(), (long) lim1) : getResources().getString(R.string.not_set);
        limit2 = !limit2.equals("") ? DataFormat.formatData(getActivity(), (long) lim2) : getResources().getString(R.string.not_set);
        limit3 = !limit3.equals("") ? DataFormat.formatData(getActivity(), (long) lim3) : getResources().getString(R.string.not_set);

        String[] listitems = getResources().getStringArray(R.array.period_values);
        String[] list = getResources().getStringArray(R.array.period);

        for (int i = 0; i < list.length; i++) {
            if (!limit1.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM1[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit1 += "/" + mPrefs.getString(Constants.PREF_SIM1[10], "1") + getResources().getString(R.string.days);
                else
                    limit1 += "/" + list[i];

            }
            if (!limit2.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM2[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit2 += "/" + mPrefs.getString(Constants.PREF_SIM2[10], "1") + getResources().getString(R.string.days);
                else
                    limit2 += "/" + list[i];

            }
            if (!limit3.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM3[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit3 += "/" + mPrefs.getString(Constants.PREF_SIM3[10], "1") + getResources().getString(R.string.days);
                else
                    limit3 += "/" + list[i];

            }
        }*/

        bLim1.setText(limit1);
        bLim2.setText(limit2);
        bLim3.setText(limit3);
    }
}
