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
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;

import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.events.SetCallsEvent;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class CallsFragment extends Fragment implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView SIM1, SIM2, SIM3, TOT1, TOT2, TOT3, TIP;
    private ContentValues mCallsData;
    private AppCompatButton bLim1, bLim2, bLim3, bClear1, bClear2, bClear3;
    private CustomDatabaseHelper mDbHelper;
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private OnFragmentInteractionListener mListener;
    private BroadcastReceiver mCallDataReceiver;
    private Context mContext;
    private ArrayList<String> mIMSI = null;
    private String[] mOperatorNames = new String[3];

    public static CallsFragment newInstance() {
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
            mContext = CustomApplication.getAppContext();
        mDbHelper = CustomDatabaseHelper.getInstance(mContext);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false))
            mIMSI = MobileUtils.getSimIds(mContext);
        mCallsData = new ContentValues();

        mCallDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                long[] limit = CustomApplication.getCallsSimLimitsValues(true);
                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getActivity().getTheme();
                theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                TypedArray arr = getActivity().obtainStyledAttributes(typedValue.data, new int[]{
                        android.R.attr.textColorPrimary});
                int primaryColor = arr.getColor(0, -1);
                try {
                    switch (sim) {
                        case Constants.SIM1:
                            TOT1.setText(DataFormat.formatCallDuration(context, duration));
                            if (duration >= limit[0])
                                TOT1.setTextColor(Color.RED);
                            else
                                TOT1.setTextColor(primaryColor);
                            break;
                        case Constants.SIM2:
                            TOT2.setText(DataFormat.formatCallDuration(context, duration));
                            if (duration >= limit[1])
                                TOT2.setTextColor(Color.RED);
                            else
                                TOT2.setTextColor(primaryColor);
                            break;
                        case Constants.SIM3:
                            TOT3.setText(DataFormat.formatCallDuration(context, duration));
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
        mContext.registerReceiver(mCallDataReceiver, callDataFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calls_fragment, container, false);
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        TOT1 = (TextView) view.findViewById(R.id.Tot1);
        TOT2 = (TextView) view.findViewById(R.id.Tot2);
        TOT3 = (TextView) view.findViewById(R.id.Tot3);
        SIM1 = (TextView) view.findViewById(R.id.sim1_name);
        SIM2 = (TextView) view.findViewById(R.id.sim2_name);
        SIM3 = (TextView) view.findViewById(R.id.sim3_name);
        TIP = (TextView) view.findViewById(R.id.tip);

        bLim1 = (AppCompatButton) view.findViewById(R.id.limit1_calls);
        bLim2 = (AppCompatButton) view.findViewById(R.id.limit2_calls);
        bLim3 = (AppCompatButton) view.findViewById(R.id.limit3_calls);

        bClear1 = (AppCompatButton) view.findViewById(R.id.buttonClear1);
        bClear2 = (AppCompatButton) view.findViewById(R.id.buttonClear2);
        bClear3 = (AppCompatButton) view.findViewById(R.id.buttonClear3);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            SIM2.setVisibility(View.GONE);
            TOT2.setVisibility(View.GONE);
            bClear2.setVisibility(View.GONE);
            bLim2.setVisibility(View.GONE);
            SIM3.setVisibility(View.GONE);
            TOT3.setVisibility(View.GONE);
            bClear3.setVisibility(View.GONE);
            bLim3.setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.sim2row).setVisibility(View.GONE);
            view.findViewById(R.id.sim3row).setVisibility(View.GONE);
        }
        if (mSimQuantity >= 2)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                SIM2.setVisibility(View.VISIBLE);
                TOT2.setVisibility(View.VISIBLE);
                bClear2.setVisibility(View.VISIBLE);
                bLim2.setVisibility(View.VISIBLE);
            } else
                view.findViewById(R.id.sim2row).setVisibility(View.VISIBLE);
        if (mSimQuantity == 3)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                SIM3.setVisibility(View.VISIBLE);
                TOT3.setVisibility(View.VISIBLE);
                bClear3.setVisibility(View.VISIBLE);
                bLim3.setVisibility(View.VISIBLE);
            } else
                view.findViewById(R.id.sim3row).setVisibility(View.VISIBLE);

        // Inflate the layout for this fragment
        return view;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onCallsFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = null;
        if (context instanceof Activity)
            activity = (Activity) context;
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        try {
            mContext.unregisterReceiver(mCallDataReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setSubtitle(R.string.calls_fragment);

        readCallsDataFromDatabase();
        long[] limit = CustomApplication.getCallsSimLimitsValues(true);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        TypedArray arr = getActivity().obtainStyledAttributes(typedValue.data, new int[]{
                android.R.attr.textColorPrimary});
        int primaryColor = arr.getColor(0, -1);

        TOT1.setText(DataFormat.formatCallDuration(mContext, (long) mCallsData.get(Constants.CALLS1)));
        if ((long) mCallsData.get(Constants.CALLS1) >= limit[0])
            TOT1.setTextColor(Color.RED);
        else
            TOT1.setTextColor(primaryColor);
        SIM1.setText(mOperatorNames[0]);
        bLim1.setOnClickListener(this);
        bClear1.setOnClickListener(this);
        if (mSimQuantity >= 2) {
            TOT2.setText(DataFormat.formatCallDuration(mContext, (long) mCallsData.get(Constants.CALLS2)));
            if ((long) mCallsData.get(Constants.CALLS2) >= limit[1])
                TOT2.setTextColor(Color.RED);
            else
                TOT2.setTextColor(primaryColor);
            SIM2.setText(mOperatorNames[1]);
            bLim2.setOnClickListener(this);
            bClear2.setOnClickListener(this);
        }
        if (mSimQuantity == 3) {
            TOT3.setText(DataFormat.formatCallDuration(mContext, (long) mCallsData.get(Constants.CALLS3)));
            if ((long) mCallsData.get(Constants.CALLS3) >= limit[2])
                TOT3.setTextColor(Color.RED);
            else
                TOT3.setTextColor(primaryColor);
            SIM3.setText(mOperatorNames[2]);
            bLim3.setOnClickListener(this);
           bClear3.setOnClickListener(this);
        }
        arr.recycle();
        TIP.setText(getResources().getString(R.string.tip_calls));
        setButtonLimitText();
        CustomApplication.resumeActivity();
    }

    @Override
    public void onPause() {
        super.onPause();
        bLim1.setOnClickListener(null);
        bClear1.setOnClickListener(this);
        if (mSimQuantity >= 2) {
            bLim2.setOnClickListener(null);
            bClear2.setOnClickListener(this);
        }
        if (mSimQuantity == 3) {
            bLim3.setOnClickListener(null);
            bClear3.setOnClickListener(this);
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        CustomApplication.pauseActivity();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonClear1:
                if (CustomApplication.isMyServiceRunning(CallLoggerService.class))
                    EventBus.getDefault().post(new SetCallsEvent(Constants.SIM1, "0", 1));
                else {
                    readCallsDataFromDatabase();
                    mCallsData.put(Constants.CALLS1, 0L);
                    mCallsData.put(Constants.CALLS1_EX, 0L);
                    writeCallsDataToDataBase();
                }
                TOT1.setText(DataFormat.formatCallDuration(mContext, 0L));
                break;
            case R.id.buttonClear2:
                if (CustomApplication.isMyServiceRunning(CallLoggerService.class))
                    EventBus.getDefault().post(new SetCallsEvent(Constants.SIM2, "0", 1));
                else {
                    readCallsDataFromDatabase();
                    mCallsData.put(Constants.CALLS2, 0L);
                    mCallsData.put(Constants.CALLS3_EX, 0L);
                    writeCallsDataToDataBase();
                }
                TOT2.setText(DataFormat.formatCallDuration(mContext, 0L));
                break;
            case R.id.buttonClear3:
                if (CustomApplication.isMyServiceRunning(CallLoggerService.class))
                    EventBus.getDefault().post(new SetCallsEvent(Constants.SIM3, "0", 1));
                else {
                    readCallsDataFromDatabase();
                    mCallsData.put(Constants.CALLS3, 0L);
                    mCallsData.put(Constants.CALLS3_EX, 0L);
                    writeCallsDataToDataBase();
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

    public interface OnFragmentInteractionListener {
        public void onCallsFragmentInteraction(Uri uri);
    }

    private void setButtonLimitText() {

        String limit1, limit2, limit3;
        long[] limit = CustomApplication.getCallsSimLimitsValues(false);
        limit1 = limit[0] < Long.MAX_VALUE ? String.format(getString(R.string.minutes), String.valueOf(limit[0])) : getString(R.string.not_set);
        limit2 = limit[1] < Long.MAX_VALUE ? String.format(getString(R.string.minutes), String.valueOf(limit[1])) : getString(R.string.not_set);
        limit3 = limit[2] < Long.MAX_VALUE ? String.format(getString(R.string.minutes), String.valueOf(limit[2])) : getString(R.string.not_set);

        String[] listitems = getResources().getStringArray(R.array.period_values);
        String[] list = getResources().getStringArray(R.array.limit);

        for (int i = 0; i < listitems.length; i++) {
            if (!limit1.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM1_CALLS[2], "0"))) {
                if (listitems[i].equals("2"))
                    limit1 += "/" + mPrefs.getString(Constants.PREF_SIM1_CALLS[5], "1") + getString(R.string.days);
                else
                    limit1 += "/" + list[i];
                String date = mPrefs.getString(Constants.PREF_SIM1_CALLS[8], getString(R.string.not_set));
                try {
                    date = date.substring(0, 10);
                    limit1 += getString(R.string.next_reset) + date;
                } catch (Exception e) {
                    limit1 += "\n" + date;
                }
            }
            if (mSimQuantity >= 2)
                if (!limit2.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM2_CALLS[2], "0"))) {
                    if (listitems[i].equals("2"))
                        limit2 += "/" + mPrefs.getString(Constants.PREF_SIM2_CALLS[5], "1") + getString(R.string.days);
                    else
                        limit2 += "/" + list[i];
                    String date = mPrefs.getString(Constants.PREF_SIM2_CALLS[8], getString(R.string.not_set));
                    try {
                        date = date.substring(0, 10);
                        limit2 += getString(R.string.next_reset) + date;
                    } catch (Exception e) {
                        limit2 += "\n" + date;
                    }
                }
            if (mSimQuantity == 3)
                if (!limit3.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM3_CALLS[2], "0"))) {
                    if (listitems[i].equals("2"))
                        limit3 += "/" + mPrefs.getString(Constants.PREF_SIM3_CALLS[5], "1") + getString(R.string.days);
                    else
                        limit3 += "/" + list[i];
                    String date = mPrefs.getString(Constants.PREF_SIM3_CALLS[8], getString(R.string.not_set));
                    try {
                        date = date.substring(0, 10);
                        limit3 += getString(R.string.next_reset) + date;
                    } catch (Exception e) {
                        limit3 += "\n" + date;
                    }
                }
        }

        bLim1.setText(limit1);
        bLim2.setText(limit2);
        bLim3.setText(limit3);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_SIM1[5]) || key.equals(Constants.PREF_SIM1[6]))
            SIM1.setText(MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1));
        if (key.equals(Constants.PREF_SIM2[5]) || key.equals(Constants.PREF_SIM2[6]))
            SIM2.setText(MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2));
        if (key.equals(Constants.PREF_SIM3[5]) || key.equals(Constants.PREF_SIM3[6]))
            SIM3.setText(MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3));
    }

    private void writeCallsDataToDataBase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            ContentValues cv = new ContentValues();
            cv.put("calls", (long) mCallsData.get(Constants.CALLS1));
            cv.put("calls_ex", (long) mCallsData.get(Constants.CALLS1_EX));
            cv.put("period", (int) mCallsData.get(Constants.PERIOD1));
            cv.put(Constants.LAST_TIME, (String) mCallsData.get(Constants.LAST_TIME));
            cv.put(Constants.LAST_DATE, (String) mCallsData.get(Constants.LAST_DATE));
            CustomDatabaseHelper.writeData(cv, mDbHelper, Constants.CALLS + "_" + mIMSI.get(0));
            if (mSimQuantity >= 2) {
                cv = new ContentValues();
                cv.put("calls", (long) mCallsData.get(Constants.CALLS2));
                cv.put("calls_ex", (long) mCallsData.get(Constants.CALLS2_EX));
                cv.put("period", (int) mCallsData.get(Constants.PERIOD2));
                cv.put(Constants.LAST_TIME, (String) mCallsData.get(Constants.LAST_TIME));
                cv.put(Constants.LAST_DATE, (String) mCallsData.get(Constants.LAST_DATE));
                CustomDatabaseHelper.writeData(cv, mDbHelper, Constants.CALLS + "_" + mIMSI.get(1));
            }
            if (mSimQuantity == 3) {
                cv = new ContentValues();
                cv.put("calls", (long) mCallsData.get(Constants.CALLS3));
                cv.put("calls_ex", (long) mCallsData.get(Constants.CALLS3_EX));
                cv.put("period", (int) mCallsData.get(Constants.PERIOD3));
                cv.put(Constants.LAST_TIME, (String) mCallsData.get(Constants.LAST_TIME));
                cv.put(Constants.LAST_DATE, (String) mCallsData.get(Constants.LAST_DATE));
                CustomDatabaseHelper.writeData(cv, mDbHelper, Constants.CALLS + "_" + mIMSI.get(2));
            }
        } else
            CustomDatabaseHelper.writeData(mCallsData, mDbHelper, Constants.CALLS);
    }

    private void readCallsDataFromDatabase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            ContentValues cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(0));
            mCallsData.put(Constants.CALLS1, (long) cv.get("calls"));
            mCallsData.put(Constants.CALLS1_EX, (long) cv.get("calls_ex"));
            mCallsData.put(Constants.PERIOD1, (int) cv.get("period"));
            mCallsData.put(Constants.CALLS2, 0L);
            mCallsData.put(Constants.CALLS2_EX, 0L);
            mCallsData.put(Constants.PERIOD2, 0);
            mCallsData.put(Constants.CALLS3, 0L);
            mCallsData.put(Constants.CALLS3_EX, 0L);
            mCallsData.put(Constants.PERIOD3, 0);
            mCallsData.put(Constants.LAST_TIME, (String) cv.get(Constants.LAST_TIME));
            mCallsData.put(Constants.LAST_DATE, (String) cv.get(Constants.LAST_DATE));
            if (mSimQuantity >= 2) {
                cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(1));
                mCallsData.put(Constants.CALLS2, (long) cv.get("calls"));
                mCallsData.put(Constants.CALLS2_EX, (long) cv.get("calls_ex"));
                mCallsData.put(Constants.PERIOD2, (int) cv.get("period"));
            }
            if (mSimQuantity == 3) {
                cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(2));
                mCallsData.put(Constants.CALLS3, (long) cv.get("calls"));
                mCallsData.put(Constants.CALLS3_EX, (long) cv.get("calls_ex"));
                mCallsData.put(Constants.PERIOD3, (int) cv.get("period"));
            }
        } else
            mCallsData = CustomDatabaseHelper.readCallsData(mDbHelper);
        if (mCallsData.get(Constants.LAST_DATE).equals("")) {
            DateTime dateTime = new DateTime();
            mCallsData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
            mCallsData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
        }
    }
}
