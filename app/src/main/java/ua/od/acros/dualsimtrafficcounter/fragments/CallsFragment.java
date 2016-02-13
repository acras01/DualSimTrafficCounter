package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
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

import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.settings.CallsLimitFragment;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
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
public class CallsFragment extends Fragment implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView SIM1, SIM2, SIM3, TOT1, TOT2, TOT3, TIP;
    private ContentValues mCalls;
    private Button bLim1, bLim2, bLim3;
    private MyDatabase mDatabaseHelper;
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private OnFragmentInteractionListener mListener;
    private BroadcastReceiver callDataReceiver;
    private MenuItem mService;
    private boolean mIsRunning = false;

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
        mIsRunning = CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity());
        mDatabaseHelper = MyDatabase.getInstance(getActivity());
        mCalls = MyDatabase.readCallsData(mDatabaseHelper);
        mPrefs = getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getActivity())
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        callDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                long limit1, limit2, limit3;
                try {
                    limit1 = Long.valueOf(mPrefs.getString(Constants.PREF_SIM1_CALLS[1], "0")) * Constants.MINUTE;
                } catch (Exception e) {
                    limit1 = Long.MAX_VALUE;
                }
                try {
                    limit2 = Long.valueOf(mPrefs.getString(Constants.PREF_SIM2_CALLS[1], "0")) * Constants.MINUTE;
                } catch (Exception e) {
                    limit2 = Long.MAX_VALUE;
                }
                try {
                    limit3 = Long.valueOf(mPrefs.getString(Constants.PREF_SIM3_CALLS[1], "0")) * Constants.MINUTE;
                } catch (Exception e) {
                    limit3 = Long.MAX_VALUE;
                }
                switch (sim) {
                    case Constants.SIM1:
                        TOT1.setText(DataFormat.formatCallDuration(getActivity(), duration));
                        if (duration >= limit1)
                            TOT1.setTextColor(Color.RED);
                        else
                            TOT1.setTextColor(Color.WHITE);
                        break;
                    case Constants.SIM2:
                        TOT2.setText(DataFormat.formatCallDuration(getActivity(), duration));
                        if (duration >= limit2)
                            TOT2.setTextColor(Color.RED);
                        else
                            TOT2.setTextColor(Color.WHITE);
                        break;
                    case Constants.SIM3:
                        TOT3.setText(DataFormat.formatCallDuration(getActivity(), duration));
                        if (duration >= limit3)
                            TOT3.setTextColor(Color.RED);
                        else
                            TOT3.setTextColor(Color.WHITE);
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

        bLim1 = (Button) view.findViewById(R.id.limit1_calls);
        bLim2 = (Button) view.findViewById(R.id.limit2_calls);
        bLim3 = (Button) view.findViewById(R.id.limit3_calls);

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
        if (mSimQuantity >= 2)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                SIM2.setVisibility(View.VISIBLE);
                TOT2.setVisibility(View.VISIBLE);
                view.findViewById(R.id.buttonClear2).setVisibility(View.VISIBLE);
                bLim2.setVisibility(View.VISIBLE);
            } else
                view.findViewById(R.id.sim2row).setVisibility(View.VISIBLE);
        if (mSimQuantity == 3)
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

        String limit1 = mPrefs.getString(Constants.PREF_SIM1_CALLS[1], "0");
        String limit2 = mPrefs.getString(Constants.PREF_SIM2_CALLS[1], "0");
        String limit3 = mPrefs.getString(Constants.PREF_SIM3_CALLS[1], "0");
        long lim1 = Long.MAX_VALUE;
        long lim2 = Long.MAX_VALUE;
        long lim3 = Long.MAX_VALUE;
        if (!limit1.equals(""))
            lim1 = Long.valueOf(limit1) * Constants.MINUTE;
        if (!limit2.equals(""))
            lim2 = Long.valueOf(limit2) * Constants.MINUTE;
        if (!limit3.equals(""))
            lim3 = Long.valueOf(limit3) * Constants.MINUTE;

        mCalls = MyDatabase.readCallsData(mDatabaseHelper);
        TOT1.setText(DataFormat.formatCallDuration(getActivity(), (long) mCalls.get(Constants.CALLS1)));
        if ((long) mCalls.get(Constants.CALLS1) >= lim1)
            TOT1.setTextColor(Color.RED);
        else
            TOT1.setTextColor(Color.WHITE);
        TOT2.setText(DataFormat.formatCallDuration(getActivity(), (long) mCalls.get(Constants.CALLS2)));
        if ((long) mCalls.get(Constants.CALLS2) >= lim2)
            TOT2.setTextColor(Color.RED);
        else
            TOT2.setTextColor(Color.WHITE);
        TOT3.setText(DataFormat.formatCallDuration(getActivity(), (long) mCalls.get(Constants.CALLS3)));
        if ((long) mCalls.get(Constants.CALLS3) >= lim3)
            TOT3.setTextColor(Color.RED);
        else
            TOT3.setTextColor(Color.WHITE);

        SIM1.setText(MobileUtils.getName(getActivity(), Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
        SIM2.setText(MobileUtils.getName(getActivity(), Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
        SIM3.setText(MobileUtils.getName(getActivity(), Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));

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
                    Intent clear1Intent = new Intent(Constants.CLEAR_CALLS);
                    clear1Intent.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                    getActivity().sendBroadcast(clear1Intent);
                } else {
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                    mCalls.put(Constants.CALLS1, 0L);
                    mCalls.put(Constants.CALLS1_EX, 0L);
                    MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                }
                TOT1.setText(DataFormat.formatCallDuration(getActivity(), 0L));
                break;
            case R.id.buttonClear2:
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity())) {
                    Intent clear2Intent = new Intent(Constants.CLEAR_CALLS);
                    clear2Intent.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                    getActivity().sendBroadcast(clear2Intent);
                } else {
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                    mCalls.put(Constants.CALLS2, 0L);
                    mCalls.put(Constants.CALLS3_EX, 0L);
                    MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                }
                TOT2.setText(DataFormat.formatCallDuration(getActivity(), 0L));
                break;
            case R.id.buttonClear3:
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity())) {
                    Intent clear3Intent = new Intent(Constants.CLEAR_CALLS);
                    clear3Intent.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                    getActivity().sendBroadcast(clear3Intent);
                } else {
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                    mCalls.put(Constants.CALLS3, 0L);
                    mCalls.put(Constants.CALLS3_EX, 0L);
                    MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                }
                TOT3.setText(DataFormat.formatCallDuration(getActivity(), 0L));
                break;
            case R.id.limit1_calls:
            case R.id.limit2_calls:
            case R.id.limit3_calls:
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

        if (mIsRunning) {
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
                if (mIsRunning) {
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[24], true).apply();
                    getActivity().stopService(new Intent(getActivity(), CallLoggerService.class));
                    TIP.setText(getResources().getString(R.string.service_disabled));
                    item.setTitle(R.string.action_start);
                    mService.setIcon(R.drawable.ic_action_enable);
                    mIsRunning = CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity());
                }
                else {
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[24], false).apply();
                    getActivity().startService(new Intent(getActivity(), CallLoggerService.class));
                    TIP.setText(getResources().getString(R.string.tip_calls));
                    item.setTitle(R.string.action_stop);
                    mService.setIcon(R.drawable.ic_action_disable);
                    mIsRunning = CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, getActivity());
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

        String limit1 = String.format(getString(R.string.minutes), mPrefs.getString(Constants.PREF_SIM1_CALLS[1], ""));
        String limit2 = String.format(getString(R.string.minutes), mPrefs.getString(Constants.PREF_SIM2_CALLS[1], ""));
        String limit3 = String.format(getString(R.string.minutes), mPrefs.getString(Constants.PREF_SIM3_CALLS[1], ""));

        String[] listitems = getResources().getStringArray(R.array.period_values);
        String[] list = getResources().getStringArray(R.array.period);

        for (int i = 0; i < list.length; i++) {
            if (!limit1.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM1_CALLS[2], "0"))) {
                if (listitems[i].equals("2"))
                    limit1 += "/" + mPrefs.getString(Constants.PREF_SIM1_CALLS[5], "1") + getString(R.string.days);
                else
                    limit1 += "/" + list[i];

            }
            if (!limit2.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM2_CALLS[2], "0"))) {
                if (listitems[i].equals("2"))
                    limit2 += "/" + mPrefs.getString(Constants.PREF_SIM2_CALLS[5], "1") + getString(R.string.days);
                else
                    limit2 += "/" + list[i];

            }
            if (!limit3.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM3_CALLS[2], "0"))) {
                if (listitems[i].equals("2"))
                    limit3 += "/" + mPrefs.getString(Constants.PREF_SIM3_CALLS[5], "1") + getString(R.string.days);
                else
                    limit3 += "/" + list[i];

            }
        }

        bLim1.setText(limit1);
        bLim2.setText(limit2);
        bLim3.setText(limit3);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_SIM1[5]) || key.equals(Constants.PREF_SIM1[6]))
            SIM1.setText(MobileUtils.getName(getActivity(), Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
        if (key.equals(Constants.PREF_SIM2[5]) || key.equals(Constants.PREF_SIM2[6]))
            SIM2.setText(MobileUtils.getName(getActivity(), Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
        if (key.equals(Constants.PREF_SIM3[5]) || key.equals(Constants.PREF_SIM3[6]))
            SIM3.setText(MobileUtils.getName(getActivity(), Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
    }
}
