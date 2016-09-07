package ua.od.acros.dualsimtrafficcounter.fragments;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;

import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.OnOffDialog;
import ua.od.acros.dualsimtrafficcounter.events.OnOffTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.SetTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.TipTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.services.FloatingWindowService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.widgets.TrafficInfoWidget;

import static android.support.v4.app.ActivityCompat.invalidateOptionsMenu;

public class TrafficFragment extends Fragment implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView SIM, TOT1, TOT2, TOT3, TX1, TX2, TX3, RX1, RX2, RX3, TIP, SIM1, SIM2, SIM3;
    private ContentValues mTrafficData;
    private BroadcastReceiver mTrafficDataReceiver;
    private AppCompatButton bLim1, bLim2, bLim3, bClear1, bClear2, bClear3, bSet;
    private CustomDatabaseHelper mDbHelper;
    private SharedPreferences mPrefs;
    private boolean mShowNightTraffic1, mShowNightTraffic2, mShowNightTraffic3;
    private String[] mOperatorNames = new String[3];
    private boolean[] mIsNight;
    private int mSimQuantity;
    private OnFragmentInteractionListener mListener;
    private boolean mIsRunning = false;
    private Context mContext;
    private ArrayList<String> mIMSI = null;
    private MenuItem mService;

    public static TrafficFragment newInstance() {
        return new TrafficFragment();
    }

    public TrafficFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        mShowNightTraffic1 = mShowNightTraffic2 = mShowNightTraffic3 = false;
        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        mDbHelper = CustomDatabaseHelper.getInstance(mContext);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false))
            mIMSI = MobileUtils.getSimIds(mContext);
        mTrafficData = new ContentValues();
        mIsNight =  CustomApplication.getIsNightState();

        mTrafficDataReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (isVisible()) {
                    try {
                        if (!mShowNightTraffic1) {
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                                if (RX1 != null)
                                    RX1.setText(DataFormat.formatData(context, mIsNight[0] ? intent.getLongExtra(Constants.SIM1RX_N, 0L) :
                                            intent.getLongExtra(Constants.SIM1RX, 0L)));
                                if (TX1 != null)
                                    TX1.setText(DataFormat.formatData(context, mIsNight[0] ? intent.getLongExtra(Constants.SIM1TX_N, 0L) :
                                            intent.getLongExtra(Constants.SIM1TX, 0L)));
                            }
                            TOT1.setText(DataFormat.formatData(context, mIsNight[0] ? intent.getLongExtra(Constants.TOTAL1_N, 0L) :
                                    intent.getLongExtra(Constants.TOTAL1, 0L)));
                            if (intent.getStringExtra(Constants.OPERATOR1).equals("") || !intent.hasExtra(Constants.OPERATOR1))
                                SIM1.setText(mIsNight[0] ? "SIM1" + getString(R.string.night) : "SIM1");
                            else {
                                mOperatorNames[0] = intent.getStringExtra(Constants.OPERATOR1);
                                SIM1.setText(mIsNight[0] ? mOperatorNames[0] + getString(R.string.night) : mOperatorNames[0]);
                            }
                        }
                        if (mSimQuantity >= 2)
                            if (!mShowNightTraffic2) {
                                TOT2.setText(DataFormat.formatData(context, mIsNight[1] ? intent.getLongExtra(Constants.TOTAL2_N, 0L) :
                                        intent.getLongExtra(Constants.TOTAL2, 0L)));
                                if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                                    if (RX2 != null)
                                        RX2.setText(DataFormat.formatData(context, mIsNight[1] ? intent.getLongExtra(Constants.SIM2RX_N, 0L) :
                                                intent.getLongExtra(Constants.SIM2RX, 0L)));
                                    if (TX2 != null)
                                        TX2.setText(DataFormat.formatData(context, mIsNight[1] ? intent.getLongExtra(Constants.SIM2TX_N, 0L) :
                                                intent.getLongExtra(Constants.SIM2TX, 0L)));
                                }
                                if (!intent.hasExtra(Constants.OPERATOR2) || intent.getStringExtra(Constants.OPERATOR2).equals(""))
                                    SIM2.setText(mIsNight[1] ? "SIM2" + getString(R.string.night) : "SIM2");
                                else {
                                    mOperatorNames[1] = intent.getStringExtra(Constants.OPERATOR2);
                                    SIM2.setText(mIsNight[1] ? mOperatorNames[1] + getString(R.string.night) : mOperatorNames[1]);
                                }
                            }
                        if (mSimQuantity == 3)
                            if (!mShowNightTraffic3) {
                                TOT3.setText(DataFormat.formatData(context, mIsNight[2] ? intent.getLongExtra(Constants.TOTAL3_N, 0L) :
                                        intent.getLongExtra(Constants.TOTAL3, 0L)));
                                if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                                    if (RX3 != null)
                                        RX3.setText(DataFormat.formatData(context, mIsNight[2] ? intent.getLongExtra(Constants.SIM3RX_N, 0L) :
                                                intent.getLongExtra(Constants.SIM3RX, 0L)));
                                    if (TX3 != null)
                                        TX3.setText(DataFormat.formatData(context, mIsNight[2] ? intent.getLongExtra(Constants.SIM3TX_N, 0L) :
                                                intent.getLongExtra(Constants.SIM3TX, 0L)));
                                }
                                if (!intent.hasExtra(Constants.OPERATOR3) || intent.getStringExtra(Constants.OPERATOR3).equals(""))
                                    SIM3.setText(mIsNight[2] ? "SIM3" + getString(R.string.night) : "SIM3");
                                else {
                                    mOperatorNames[2] = intent.getStringExtra(Constants.OPERATOR3);
                                    SIM3.setText(mIsNight[2] ? mOperatorNames[2] + getString(R.string.night) : mOperatorNames[2]);
                                }
                            }
                        if (!intent.getBooleanExtra(Constants.TIP, false))
                            TIP.setText(getString(R.string.tip));
                        else
                            TIP.setText(getString(R.string.count_stopped_tip));
                        String rxSpeed = DataFormat.formatData(context, intent.getLongExtra(Constants.SPEEDRX, 0L));
                        String txSpeed = DataFormat.formatData(context, intent.getLongExtra(Constants.SPEEDTX, 0L));
                        setLabelText(intent.getIntExtra(Constants.SIM_ACTIVE, 0), txSpeed, rxSpeed);
                        invalidateOptionsMenu(getActivity());
                    } catch (Exception e) {
                        e.printStackTrace();
                        ACRA.getErrorReporter().handleException(e);
                    }
                }
            }
        };
        IntentFilter countServiceFilter = new IntentFilter(Constants.TRAFFIC_BROADCAST_ACTION);
        mContext.registerReceiver(mTrafficDataReceiver, countServiceFilter);

        Intent intent = new Intent(mContext, TrafficInfoWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(mContext).getAppWidgetIds(new ComponentName(mContext, TrafficInfoWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        mContext.sendBroadcast(intent);
    }

    @Subscribe
    public void onMessageEvent(OnOffTrafficEvent event) {
        int sim = event.sim;
        boolean close = event.close;
        try {
            if (sim > Constants.DISABLED)
                MobileUtils.toggleMobileDataConnection(true, mContext, sim);
            else
                MobileUtils.toggleMobileDataConnection(false, mContext, Constants.DISABLED);
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        if (close)
            getActivity().finish();
        else
            invalidateOptionsMenu(getActivity());
    }

    @Subscribe
    public void onMessageEvent(TipTrafficEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isVisible()) {
                    try {
                        TIP.setText(getString(R.string.count_stopped_tip));
                    } catch (Exception e) {
                        e.printStackTrace();
                        ACRA.getErrorReporter().handleException(e);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setSubtitle(R.string.notification_title);

        mIsRunning = CustomApplication.isMyServiceRunning(TrafficCountService.class);
        bSet.setOnClickListener(this);
        readTrafficDataFromDatabase();
        if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
            RX1.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mTrafficData.get(Constants.SIM1RX_N) :
                    (long) mTrafficData.get(Constants.SIM1RX)));
            TX1.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mTrafficData.get(Constants.SIM1TX_N) :
                    (long) mTrafficData.get(Constants.SIM1TX)));
            if (mSimQuantity >= 2) {
                RX2.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mTrafficData.get(Constants.SIM2RX_N) :
                        (long) mTrafficData.get(Constants.SIM2RX)));
                TX2.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mTrafficData.get(Constants.SIM2TX_N) :
                        (long) mTrafficData.get(Constants.SIM2TX)));
            }
            if (mSimQuantity == 3) {
                RX3.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mTrafficData.get(Constants.SIM3RX_N) :
                        (long) mTrafficData.get(Constants.SIM3RX)));
                TX3.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mTrafficData.get(Constants.SIM3TX_N) :
                        (long) mTrafficData.get(Constants.SIM3TX)));
            }
        }
        TOT1.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mTrafficData.get(Constants.TOTAL1_N) :
                (long) mTrafficData.get(Constants.TOTAL1)));
        SIM1.setText(mOperatorNames[0]);
        SIM1.setOnClickListener(this);
        bLim1.setOnClickListener(this);
        bClear1.setOnClickListener(this);
        if (mSimQuantity >= 2) {
            TOT2.setText(DataFormat.formatData(mContext, mIsNight[1] ? (long) mTrafficData.get(Constants.TOTAL2_N) :
                    (long) mTrafficData.get(Constants.TOTAL2)));
            SIM2.setText(mOperatorNames[1]);
            SIM2.setOnClickListener(this);
            bLim2.setOnClickListener(this);
            bClear2.setOnClickListener(this);
        }
        if (mSimQuantity == 3) {
            TOT3.setText(DataFormat.formatData(mContext, mIsNight[2] ? (long) mTrafficData.get(Constants.TOTAL3_N) :
                    (long) mTrafficData.get(Constants.TOTAL3)));
            SIM3.setText(mOperatorNames[2]);
            SIM3.setOnClickListener(this);
            bLim3.setOnClickListener(this);
            bClear3.setOnClickListener(this);
        }

        setButtonLimitText();

        setLabelText(mPrefs.getInt(Constants.PREF_OTHER[46], Constants.DISABLED), "0", "0");

        CustomApplication.isActivityResumed();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        bSet.setOnClickListener(null);
        SIM1.setOnClickListener(null);
        bLim1.setOnClickListener(null);
        bClear1.setOnClickListener(null);
        if (mSimQuantity >= 2) {
            SIM2.setOnClickListener(null);
            bLim2.setOnClickListener(null);
            bClear2.setOnClickListener(null);
        }
        if (mSimQuantity == 3) {
            SIM3.setOnClickListener(null);
            bLim3.setOnClickListener(null);
            bClear3.setOnClickListener(null);
        }

        CustomApplication.isActivityPaused();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.traffic_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        /*MenuInflater inflater = new MenuInflater(mContext.getApplicationContext());
        menu.clear();
        onCreateOptionsMenu(menu, inflater);*/

        mService = menu.getItem(0);
        if (mService != null) {
            if (mIsRunning) {
                mService.setTitle(R.string.action_stop);
                mService.setIcon(R.drawable.ic_action_disable);
            } else {
                mService.setTitle(R.string.action_start);
                mService.setIcon(R.drawable.ic_action_enable);
            }
        }

        MenuItem mobileData = menu.getItem(1);
        if (mobileData != null) {
            if (!CustomApplication.isOldMtkDevice() &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && !CustomApplication.hasRoot()) {
                mobileData.setEnabled(false);
                mobileData.setVisible(false);
            } else {
                switch (MobileUtils.hasActiveNetworkInfo(mContext)) {
                    case 0:
                        mobileData.setEnabled(true);
                        mobileData.setTitle(R.string.action_enable);
                        mobileData.setIcon(R.drawable.ic_action_mobile_on);
                        break;
                    case 1:
                        mobileData.setEnabled(false);
                        mobileData.setIcon(R.drawable.ic_action_mobile_off);
                        break;
                    case 2:
                        mobileData.setEnabled(true);
                        mobileData.setTitle(R.string.action_disable);
                        mobileData.setIcon(R.drawable.ic_action_mobile_off);
                        break;
                }
            }
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
                    FloatingWindowService.closeFloatingWindow(mContext, mPrefs);
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[5], true).apply();
                    mContext.stopService(new Intent(mContext, TrafficCountService.class));
                    TIP.setText(getString(R.string.service_disabled));
                    item.setTitle(R.string.action_start);
                    item.setIcon(R.drawable.ic_action_enable);
                }
                else {
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[32], false) &&
                            ((mPrefs.getBoolean(Constants.PREF_OTHER[41], false) && MobileUtils.hasActiveNetworkInfo(mContext) == 2) ||
                                    !mPrefs.getBoolean(Constants.PREF_OTHER[41], false)))
                        FloatingWindowService.showFloatingWindow(mContext, mPrefs);
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[5], false).apply();
                    mContext.startService(new Intent(mContext, TrafficCountService.class));
                    TIP.setText(getString(R.string.tip));
                    item.setTitle(R.string.action_stop);
                    item.setIcon(R.drawable.ic_action_disable);
                }
                break;
            case R.id.action_mobile_data_on_off:
                showDialog(Constants.ON_OFF);
                break;
        }
        mIsRunning = CustomApplication.isMyServiceRunning(TrafficCountService.class);
        return super.onOptionsItemSelected(item);
    }

    public void showDialog(String key) {
        DialogFragment dialog = null;
        switch (key) {
            case Constants.ON_OFF:
                dialog = OnOffDialog.newInstance();
                break;
        }
        if (dialog != null)
            dialog.show(getActivity().getSupportFragmentManager(), "dialog");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
            view = inflater.inflate(R.layout.traffic_fragment, container, false);
            RX1 = (TextView) view.findViewById(R.id.RX1);
            TX1 = (TextView) view.findViewById(R.id.TX1);
            RX2 = (TextView) view.findViewById(R.id.RX2);
            TX2 = (TextView) view.findViewById(R.id.TX2);
            RX3 = (TextView) view.findViewById(R.id.RX3);
            TX3 = (TextView) view.findViewById(R.id.TX3);
        } else
            view = inflater.inflate(R.layout.traffic_fragment_short, container, false);

        TOT1 = (TextView) view.findViewById(R.id.Tot1);
        TOT2 = (TextView) view.findViewById(R.id.Tot2);
        TOT3 = (TextView) view.findViewById(R.id.Tot3);
        SIM = (TextView) view.findViewById(R.id.sim);
        TIP = (TextView) view.findViewById(R.id.tip);
        SIM1 = (TextView) view.findViewById(R.id.sim1_name);
        SIM2 = (TextView) view.findViewById(R.id.sim2_name);
        SIM3 = (TextView) view.findViewById(R.id.sim3_name);

        bLim1 = (AppCompatButton) view.findViewById(R.id.limit1);
        bLim2 = (AppCompatButton) view.findViewById(R.id.limit2);
        bLim3 = (AppCompatButton) view.findViewById(R.id.limit3);

        bClear1 = (AppCompatButton) view.findViewById(R.id.buttonClear1);
        bClear2 = (AppCompatButton) view.findViewById(R.id.buttonClear2);
        bClear3 = (AppCompatButton) view.findViewById(R.id.buttonClear3);

        bSet = (AppCompatButton) view.findViewById(R.id.settings);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                if (TX2 != null)
                    TX2.setVisibility(View.GONE);
                if (RX2 != null)
                    RX2.setVisibility(View.GONE);
                if (TX3 != null)
                    TX3.setVisibility(View.GONE);
                if (RX3 != null)
                    RX3.setVisibility(View.GONE);
            }
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
                if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                    if (TX2 != null)
                        TX2.setVisibility(View.VISIBLE);
                    if (RX2 != null)
                        RX2.setVisibility(View.VISIBLE);
                }
                SIM2.setVisibility(View.VISIBLE);
                TOT2.setVisibility(View.VISIBLE);
                bClear2.setVisibility(View.VISIBLE);
                bLim2.setVisibility(View.VISIBLE);
            } else
                view.findViewById(R.id.sim2row).setVisibility(View.VISIBLE);
        if (mSimQuantity == 3)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                    if (TX3 != null)
                        TX3.setVisibility(View.VISIBLE);
                    if (RX3 != null)
                        RX3.setVisibility(View.VISIBLE);
                }
                SIM3.setVisibility(View.VISIBLE);
                TOT3.setVisibility(View.VISIBLE);
                bClear3.setVisibility(View.VISIBLE);
                bLim3.setVisibility(View.VISIBLE);
            } else
                view.findViewById(R.id.sim3row).setVisibility(View.VISIBLE);

        if (!CustomApplication.isDataUsageAvailable())
            view.findViewById(R.id.settings).setEnabled(false);

        // Inflate the layout for this fragment
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onTrafficFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[48])) {
            if (sharedPreferences.getBoolean(key, false)) {
                mService.setTitle(R.string.action_stop);
                mService.setIcon(R.drawable.ic_action_disable);
            } else {
                mService.setTitle(R.string.action_start);
                mService.setIcon(R.drawable.ic_action_enable);
            }
            mIsRunning = CustomApplication.isMyServiceRunning(TrafficCountService.class);
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onTrafficFragmentInteraction(Uri uri);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        try {
            mContext.unregisterReceiver(mTrafficDataReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLabelText(int sim, String rx, String tx) {
        int swtch = MobileUtils.hasActiveNetworkInfo(mContext);
        switch (sim) {
            case Constants.DISABLED:
                if (swtch== 0)
                    SIM.setText(R.string.data_dis);
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                else if (swtch == 2)
                    SIM.setText(R.string.not_supported);
                break;
            case Constants.SIM1:
                if (swtch == 2)
                    SIM.setText(String.format(getString(R.string.sim1_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
            case Constants.SIM2:
                if (swtch == 2)
                    SIM.setText(String.format(getString(R.string.sim2_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
            case Constants.SIM3:
                if (swtch == 2)
                    SIM.setText(String.format(getString(R.string.sim3_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        boolean[] isNight =  CustomApplication.getIsNightState();
        switch (v.getId()) {
            case R.id.settings:
                try {
                    startActivity(CustomApplication.getSettingsIntent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.buttonClear1:
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    EventBus.getDefault().post(new SetTrafficEvent("0", "0", Constants.SIM1, 0, 0));
                else {
                    readTrafficDataFromDatabase();
                    if (isNight[0]) {
                        mTrafficData.put(Constants.SIM1RX_N, 0L);
                        mTrafficData.put(Constants.SIM1TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL1_N, 0L);
                    } else {
                        mTrafficData.put(Constants.SIM1RX, 0L);
                        mTrafficData.put(Constants.SIM1TX, 0L);
                        mTrafficData.put(Constants.TOTAL1, 0L);
                    }
                    writeTrafficDataToDatabase();
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX1 != null)
                            RX1.setText(DataFormat.formatData(mContext, isNight[0] ? (long) mTrafficData.get(Constants.SIM1RX_N) :
                                    (long) mTrafficData.get(Constants.SIM1RX)));
                        if (TX1 != null)
                            TX1.setText(DataFormat.formatData(mContext, isNight[0] ? (long) mTrafficData.get(Constants.SIM1TX_N) :
                                    (long) mTrafficData.get(Constants.SIM1TX)));
                    }
                    TOT1.setText(DataFormat.formatData(mContext, isNight[0] ? (long) mTrafficData.get(Constants.TOTAL1_N) :
                            (long) mTrafficData.get(Constants.TOTAL1)));
                }
                break;
            case R.id.buttonClear2:
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    EventBus.getDefault().post(new SetTrafficEvent("0", "0", Constants.SIM2, 0, 0));
                else {
                    readTrafficDataFromDatabase();
                    if (isNight[1]) {
                        mTrafficData.put(Constants.SIM2RX_N, 0L);
                        mTrafficData.put(Constants.SIM2TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL2_N, 0L);
                    } else {
                        mTrafficData.put(Constants.SIM2RX, 0L);
                        mTrafficData.put(Constants.SIM2TX, 0L);
                        mTrafficData.put(Constants.TOTAL2, 0L);
                    }
                    writeTrafficDataToDatabase();
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX2 != null)
                            RX2.setText(DataFormat.formatData(mContext, isNight[1] ? (long) mTrafficData.get(Constants.SIM2RX_N) :
                                    (long) mTrafficData.get(Constants.SIM2RX)));
                        if (TX2 != null)
                            TX2.setText(DataFormat.formatData(mContext, isNight[1] ? (long) mTrafficData.get(Constants.SIM2TX_N) :
                                    (long) mTrafficData.get(Constants.SIM2TX)));
                    }
                    TOT2.setText(DataFormat.formatData(mContext, isNight[1] ? (long) mTrafficData.get(Constants.TOTAL2_N) :
                            (long) mTrafficData.get(Constants.TOTAL2)));
                }
                break;
            case R.id.buttonClear3:
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    EventBus.getDefault().post(new SetTrafficEvent("0", "0", Constants.SIM3, 0, 0));
                else {
                    readTrafficDataFromDatabase();
                    if (isNight[2]) {
                        mTrafficData.put(Constants.SIM3RX_N, 0L);
                        mTrafficData.put(Constants.SIM3TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL3_N, 0L);
                    } else {
                        mTrafficData.put(Constants.SIM3RX, 0L);
                        mTrafficData.put(Constants.SIM3TX, 0L);
                        mTrafficData.put(Constants.TOTAL3, 0L);
                    }
                    writeTrafficDataToDatabase();
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX3 != null)
                            RX3.setText(DataFormat.formatData(mContext, isNight[2] ? (long) mTrafficData.get(Constants.SIM3RX_N) :
                                    (long) mTrafficData.get(Constants.SIM3RX)));
                        if (TX3 != null)
                            TX3.setText(DataFormat.formatData(mContext, isNight[2] ? (long) mTrafficData.get(Constants.SIM3TX_N) :
                                    (long) mTrafficData.get(Constants.SIM3TX)));
                    }
                    TOT3.setText(DataFormat.formatData(mContext, isNight[2] ? (long) mTrafficData.get(Constants.TOTAL3_N) :
                            (long) mTrafficData.get(Constants.TOTAL3)));
                }
                break;
            case R.id.sim1_name:
                if (mPrefs.getBoolean(Constants.PREF_SIM1[17], false)) {
                    mShowNightTraffic1 = !mShowNightTraffic1;
                    if (mShowNightTraffic1) {
                        if (mOperatorNames[0].equals(""))
                            SIM1.setText(!isNight[0] ? "SIM1" + getString(R.string.night) : "SIM1");
                        else
                            SIM1.setText(!isNight[0] ? mOperatorNames[0] + getString(R.string.night) : mOperatorNames[0]);
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                            if (RX1 != null)
                                RX1.setText(DataFormat.formatData(mContext, !isNight[0] ? (long) mTrafficData.get(Constants.SIM1RX_N) :
                                        (long) mTrafficData.get(Constants.SIM1RX)));
                            if (TX1 != null)
                                TX1.setText(DataFormat.formatData(mContext, !isNight[0] ? (long) mTrafficData.get(Constants.SIM1TX_N) :
                                        (long) mTrafficData.get(Constants.SIM1TX)));
                        }
                        TOT1.setText(DataFormat.formatData(mContext, !isNight[0] ? (long) mTrafficData.get(Constants.TOTAL1_N) :
                                (long) mTrafficData.get(Constants.TOTAL1)));
                    }
                }
                break;
            case R.id.sim2_name:
                if (mPrefs.getBoolean(Constants.PREF_SIM2[17], false)) {
                    mShowNightTraffic2 = !mShowNightTraffic2;
                    if (mShowNightTraffic2) {
                        if (mOperatorNames[1].equals(""))
                            SIM2.setText(!isNight[1] ? "SIM2" + getString(R.string.night) : "SIM2");
                        else
                            SIM2.setText(!isNight[1] ? mOperatorNames[1] + getString(R.string.night) : mOperatorNames[1]);
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                            if (RX2 != null)
                                RX2.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mTrafficData.get(Constants.SIM2RX_N) :
                                        (long) mTrafficData.get(Constants.SIM2RX)));
                            if (TX2 != null)
                                TX2.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mTrafficData.get(Constants.SIM2TX_N) :
                                        (long) mTrafficData.get(Constants.SIM2TX)));
                        }
                        TOT2.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mTrafficData.get(Constants.TOTAL2_N) :
                                (long) mTrafficData.get(Constants.TOTAL2)));
                    }
                }
                break;
            case R.id.sim3_name:
                if (mPrefs.getBoolean(Constants.PREF_SIM3[17], false)) {
                    mShowNightTraffic3 = !mShowNightTraffic3;
                    if (mShowNightTraffic3) {
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                            if (mOperatorNames[2].equals(""))
                                SIM3.setText(!isNight[2] ? "SIM3" + getString(R.string.night) : "SIM3");
                            else
                                SIM3.setText(!isNight[2] ? mOperatorNames[2] + getString(R.string.night) : mOperatorNames[2]);
                            if (RX3 != null)
                                RX3.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mTrafficData.get(Constants.SIM3RX_N) :
                                        (long) mTrafficData.get(Constants.SIM3RX)));
                            if (TX3 != null)
                                TX3.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mTrafficData.get(Constants.SIM3TX_N) :
                                        (long) mTrafficData.get(Constants.SIM2TX)));
                        }
                        TOT3.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mTrafficData.get(Constants.TOTAL3_N) :
                                (long) mTrafficData.get(Constants.TOTAL3)));
                    }
                }
                break;
            case R.id.limit1:
            case R.id.limit2:
            case R.id.limit3:
                Intent intent = new Intent(mContext, SettingsActivity.class);
                intent.putExtra("show", Constants.TRAFFIC_TAG);
                String sim = "";
                switch (v.getId()) {
                    case R.id.limit1:
                        sim = "traff_sim1";
                        break;
                    case R.id.limit2:
                        sim = "traff_sim2";
                        break;
                    case R.id.limit3:
                        sim = "traff_sim3";
                        break;
                }
                intent.putExtra("sim", sim);
                startActivity(intent);
                break;
        }
    }

    private void setButtonLimitText() {

        String limit1, limit2, limit3;
        long[] limit = CustomApplication.getTrafficSimLimitsValues();
        limit1 = limit[0] < Long.MAX_VALUE ? DataFormat.formatData(mContext, limit[0]) : getString(R.string.not_set);
        limit2 = limit[1] < Long.MAX_VALUE ? DataFormat.formatData(mContext, limit[1]) : getString(R.string.not_set);
        limit3 = limit[2] < Long.MAX_VALUE ? DataFormat.formatData(mContext, limit[2]) : getString(R.string.not_set);

        String[] listitems = getResources().getStringArray(R.array.period_values);
        String[] list = getResources().getStringArray(R.array.period);

        for (int i = 0; i < list.length; i++) {
            if (!limit1.equals(getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM1[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit1 += "/" + mPrefs.getString(Constants.PREF_SIM1[10], "1") + getString(R.string.days);
                else
                    limit1 += "/" + list[i];

            }
            if (!limit2.equals(getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM2[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit2 += "/" + mPrefs.getString(Constants.PREF_SIM2[10], "1") + getString(R.string.days);
                else
                    limit2 += "/" + list[i];

            }
            if (!limit3.equals(getString(R.string.not_set)) && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM3[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit3 += "/" + mPrefs.getString(Constants.PREF_SIM3[10], "1") + getString(R.string.days);
                else
                    limit3 += "/" + list[i];

            }
        }

        bLim1.setText(limit1);
        bLim2.setText(limit2);
        bLim3.setText(limit3);
    }

    private void readTrafficDataFromDatabase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            ContentValues cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(0));
            mTrafficData.put(Constants.SIM1RX, (long) cv.get("rx"));
            mTrafficData.put(Constants.SIM1TX, (long) cv.get("tx"));
            mTrafficData.put(Constants.TOTAL1, (long) cv.get("total"));
            mTrafficData.put(Constants.SIM1RX_N, (long) cv.get("rx_n"));
            mTrafficData.put(Constants.SIM1TX_N, (long) cv.get("tx_n"));
            mTrafficData.put(Constants.TOTAL1_N, (long) cv.get("total_n"));
            mTrafficData.put(Constants.PERIOD1, (int) cv.get("period"));
            mTrafficData.put(Constants.SIM2RX, 0L);
            mTrafficData.put(Constants.SIM3RX, 0L);
            mTrafficData.put(Constants.SIM2TX, 0L);
            mTrafficData.put(Constants.SIM3TX, 0L);
            mTrafficData.put(Constants.TOTAL2, 0L);
            mTrafficData.put(Constants.TOTAL3, 0L);
            mTrafficData.put(Constants.SIM2RX_N, 0L);
            mTrafficData.put(Constants.SIM3RX_N, 0L);
            mTrafficData.put(Constants.SIM2TX_N, 0L);
            mTrafficData.put(Constants.SIM3TX_N, 0L);
            mTrafficData.put(Constants.TOTAL2_N, 0L);
            mTrafficData.put(Constants.TOTAL3_N, 0L);
            mTrafficData.put(Constants.LAST_TIME, (String) cv.get(Constants.LAST_TIME));
            mTrafficData.put(Constants.LAST_DATE, (String) cv.get(Constants.LAST_DATE));
            if (mSimQuantity >= 2) {
                cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(1));
                mTrafficData.put(Constants.SIM2RX, (long) cv.get("rx"));
                mTrafficData.put(Constants.SIM2TX, (long) cv.get("tx"));
                mTrafficData.put(Constants.TOTAL2, (long) cv.get("total"));
                mTrafficData.put(Constants.SIM2RX_N, (long) cv.get("rx_n"));
                mTrafficData.put(Constants.SIM2TX_N, (long) cv.get("tx_n"));
                mTrafficData.put(Constants.TOTAL2_N, (long) cv.get("total_n"));
                mTrafficData.put(Constants.PERIOD2, (int) cv.get("period"));
            }
            if (mSimQuantity == 3) {
                cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(2));
                mTrafficData.put(Constants.SIM3RX, (long) cv.get("rx"));
                mTrafficData.put(Constants.SIM3TX, (long) cv.get("tx"));
                mTrafficData.put(Constants.TOTAL3, (long) cv.get("total"));
                mTrafficData.put(Constants.SIM3RX_N, (long) cv.get("rx_n"));
                mTrafficData.put(Constants.SIM3TX_N, (long) cv.get("tx_n"));
                mTrafficData.put(Constants.TOTAL3_N, (long) cv.get("total_n"));
                mTrafficData.put(Constants.PERIOD3, (int) cv.get("period"));
            }
        } else
            mTrafficData = CustomDatabaseHelper.readTrafficData(mDbHelper);
        if (mTrafficData.get(Constants.LAST_DATE).equals("")) {
            DateTime dateTime = new DateTime();
            mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
            mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
        }
    }

    private void writeTrafficDataToDatabase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            ContentValues cv = new ContentValues();
            cv.put("rx", (long) mTrafficData.get(Constants.SIM1RX));
            cv.put("tx", (long) mTrafficData.get(Constants.SIM1TX));
            cv.put("total", (long) mTrafficData.get(Constants.TOTAL1));
            cv.put("rx_n", (long) mTrafficData.get(Constants.SIM1RX_N));
            cv.put("tx_n", (long) mTrafficData.get(Constants.SIM1TX_N));
            cv.put("total_n", (long) mTrafficData.get(Constants.TOTAL1_N));
            cv.put("period", (int) mTrafficData.get(Constants.PERIOD1));
            cv.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
            cv.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
            CustomDatabaseHelper.writeDataForSim(cv, mDbHelper, Constants.DATA_TABLE + "_" + mIMSI.get(0));
            if (mSimQuantity >= 2) {
                cv = new ContentValues();;
                cv.put("rx", (long) mTrafficData.get(Constants.SIM2RX));
                cv.put("tx", (long) mTrafficData.get(Constants.SIM2TX));
                cv.put("total", (long) mTrafficData.get(Constants.TOTAL2));
                cv.put("rx_n", (long) mTrafficData.get(Constants.SIM2RX_N));
                cv.put("tx_n", (long) mTrafficData.get(Constants.SIM2TX_N));
                cv.put("total_n", (long) mTrafficData.get(Constants.TOTAL2_N));
                cv.put("period", (int) mTrafficData.get(Constants.PERIOD2));
                cv.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                cv.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
                CustomDatabaseHelper.writeDataForSim(cv, mDbHelper, Constants.DATA_TABLE + "_" + mIMSI.get(1));
            }
            if (mSimQuantity == 3) {
                cv = new ContentValues();;
                cv.put("rx", (long) mTrafficData.get(Constants.SIM3RX));
                cv.put("tx", (long) mTrafficData.get(Constants.SIM3TX));
                cv.put("total", (long) mTrafficData.get(Constants.TOTAL3));
                cv.put("rx_n", (long) mTrafficData.get(Constants.SIM3RX_N));
                cv.put("tx_n", (long) mTrafficData.get(Constants.SIM3TX_N));
                cv.put("total_n", (long) mTrafficData.get(Constants.TOTAL3_N));
                cv.put("period", (int) mTrafficData.get(Constants.PERIOD3));
                cv.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                cv.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
                CustomDatabaseHelper.writeDataForSim(cv, mDbHelper, Constants.DATA_TABLE + "_" + mIMSI.get(2));
            }
        } else
            CustomDatabaseHelper.writeData(mTrafficData, mDbHelper, Constants.DATA_TABLE);
    }
}
