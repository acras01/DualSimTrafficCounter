package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.events.ClearCallsEvent;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabaseHelper;

public class CallsFragment extends Fragment implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView SIM1, SIM2, SIM3, TOT1, TOT2, TOT3, TIP;
    private ContentValues mCalls;
    private Button bLim1, bLim2, bLim3;
    private MyDatabaseHelper mDbHelper;
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private OnFragmentInteractionListener mListener;
    private BroadcastReceiver callDataReceiver;
    private MenuItem mService;
    private boolean mIsRunning = false;
    private Context mContext;

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
        if (mContext == null)
            mContext = getActivity().getApplicationContext();
        mIsRunning = CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext);
        mDbHelper = MyDatabaseHelper.getInstance(mContext);
        mCalls = MyDatabaseHelper.readCallsData(mDbHelper);
        mPrefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        callDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                long[] limit = setTotalText();
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getActivity().getTheme();
                theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                TypedArray arr = getActivity().obtainStyledAttributes(typedValue.data, new int[]{
                        android.R.attr.textColorPrimary});
                int primaryColor = arr.getColor(0, -1);
                try {
                    switch (sim) {
                        case Constants.SIM1:
                            TOT1.setText(DataFormat.formatCallDuration(mContext, duration));
                            if (duration >= limit[0])
                                TOT1.setTextColor(Color.RED);
                            else
                                TOT1.setTextColor(primaryColor);
                            break;
                        case Constants.SIM2:
                            TOT2.setText(DataFormat.formatCallDuration(mContext, duration));
                            if (duration >= limit[1])
                                TOT2.setTextColor(Color.RED);
                            else
                                TOT2.setTextColor(primaryColor);
                            break;
                        case Constants.SIM3:
                            TOT3.setText(DataFormat.formatCallDuration(mContext, duration));
                            if (duration >= limit[2])
                                TOT3.setTextColor(Color.RED);
                            else
                                TOT3.setTextColor(primaryColor);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                arr.recycle();
            }
        };
        IntentFilter callDataFilter = new IntentFilter(Constants.CALLS_BROADCAST_ACTION);
        mContext.registerReceiver(callDataReceiver, callDataFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calls_fragment, container, false);
        if (mContext == null)
            mContext = getActivity().getApplicationContext();
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

        mCalls = MyDatabaseHelper.readCallsData(mDbHelper);
        TOT1.setText(DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS1)));

        long[] limit = setTotalText();
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        TypedArray arr = getActivity().obtainStyledAttributes(typedValue.data, new int[]{
                android.R.attr.textColorPrimary});
        int primaryColor = arr.getColor(0, -1);
        if ((long) mCalls.get(Constants.CALLS1) >= limit[0])
            TOT1.setTextColor(Color.RED);
        else
            TOT1.setTextColor(primaryColor);
        TOT2.setText(DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS2)));
        if ((long) mCalls.get(Constants.CALLS2) >= limit[1])
            TOT2.setTextColor(Color.RED);
        else
            TOT2.setTextColor(primaryColor);
        TOT3.setText(DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS3)));
        if ((long) mCalls.get(Constants.CALLS3) >= limit[2])
            TOT3.setTextColor(Color.RED);
        else
            TOT3.setTextColor(primaryColor);
        arr.recycle();

        SIM1.setText(MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
        SIM2.setText(MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
        SIM3.setText(MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));

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
        try {
            mContext.unregisterReceiver(callDataReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);;
        toolBar.setSubtitle(R.string.calls_fragment);
        setButtonLimitText();
        MyApplication.activityResumed();
    }

    @Override
    public void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonClear1:
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext))
                    EventBus.getDefault().post(new ClearCallsEvent(Constants.SIM1));
                else {
                    mCalls = MyDatabaseHelper.readCallsData(mDbHelper);
                    mCalls.put(Constants.CALLS1, 0L);
                    mCalls.put(Constants.CALLS1_EX, 0L);
                    MyDatabaseHelper.writeCallsData(mCalls, mDbHelper);
                }
                TOT1.setText(DataFormat.formatCallDuration(mContext, 0L));
                break;
            case R.id.buttonClear2:
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext))
                    EventBus.getDefault().post(new ClearCallsEvent(Constants.SIM2));
                else {
                    mCalls = MyDatabaseHelper.readCallsData(mDbHelper);
                    mCalls.put(Constants.CALLS2, 0L);
                    mCalls.put(Constants.CALLS3_EX, 0L);
                    MyDatabaseHelper.writeCallsData(mCalls, mDbHelper);
                }
                TOT2.setText(DataFormat.formatCallDuration(mContext, 0L));
                break;
            case R.id.buttonClear3:
                if (CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext))
                    EventBus.getDefault().post(new ClearCallsEvent(Constants.SIM3));
                else {
                    mCalls = MyDatabaseHelper.readCallsData(mDbHelper);
                    mCalls.put(Constants.CALLS3, 0L);
                    mCalls.put(Constants.CALLS3_EX, 0L);
                    MyDatabaseHelper.writeCallsData(mCalls, mDbHelper);
                }
                TOT3.setText(DataFormat.formatCallDuration(mContext, 0L));
                break;
            case R.id.limit1_calls:
            case R.id.limit2_calls:
            case R.id.limit3_calls:
                Intent intent = new Intent(mContext, SettingsActivity.class);
                intent.putExtra("show", Constants.CALLS_TAG);
                String sim = "";
                switch (v.getId()) {
                    case R.id.limit1_calls:
                        sim = "calls_sim1";
                        break;
                    case R.id.limit2_calls:
                        sim = "calls_sim2";
                        break;
                    case R.id.limit3_calls:
                        sim = "calls_sim3";
                        break;
                }
                intent.putExtra("sim", sim);
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
        /*MenuInflater inflater = new MenuInflater(mContext.getApplicationContext());
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
                    mContext.stopService(new Intent(mContext, CallLoggerService.class));
                    TIP.setText(getResources().getString(R.string.service_disabled));
                    item.setTitle(R.string.action_start);
                    mService.setIcon(R.drawable.ic_action_enable);
                    mIsRunning = CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext);
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[24], true).apply();
                }
                else {
                    mContext.startService(new Intent(mContext, CallLoggerService.class));
                    TIP.setText(getResources().getString(R.string.tip_calls));
                    item.setTitle(R.string.action_stop);
                    mService.setIcon(R.drawable.ic_action_disable);
                    mIsRunning = CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext);
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[24], false).apply();
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

    private long[] setTotalText() {
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
        return new long[]{limit1, limit2, limit3};
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_SIM1[5]) || key.equals(Constants.PREF_SIM1[6]))
            SIM1.setText(MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
        if (key.equals(Constants.PREF_SIM2[5]) || key.equals(Constants.PREF_SIM2[6]))
            SIM2.setText(MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
        if (key.equals(Constants.PREF_SIM3[5]) || key.equals(Constants.PREF_SIM3[6]))
            SIM3.setText(MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
        if (key.equals(Constants.PREF_OTHER[25]))
            mIsRunning = CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext);
    }
}
