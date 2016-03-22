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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
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

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.OnOffDialog;
import ua.od.acros.dualsimtrafficcounter.events.ClearTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.OnOffTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.TipTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.widgets.TrafficInfoWidget;

import static android.support.v4.app.ActivityCompat.invalidateOptionsMenu;

public class TrafficFragment extends Fragment implements View.OnClickListener {

    private TextView SIM, TOT1, TOT2, TOT3, TX1, TX2, TX3, RX1, RX2, RX3, TIP, SIM1, SIM2, SIM3;
    private ContentValues mDataMap;
    private BroadcastReceiver mTrafficDataReceiver;
    private AppCompatButton bLim1, bLim2, bLim3;
    private MenuItem mService, mMobileData;
    private CustomDatabaseHelper mDbHelper;
    private SharedPreferences mPrefs;
    private boolean mShowNightTraffic1, mShowNightTraffic2, mShowNightTraffic3;
    private String[] mOperatorNames = new String[3];
    private boolean[] mIsNight;
    private int mSimQuantity;
    private OnFragmentInteractionListener mListener;
    private boolean mIsRunning = false;
    private Context mContext;

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
        EventBus.getDefault().register(this);
        mIsRunning = CustomApplication.isMyServiceRunning(TrafficCountService.class, mContext);
        mShowNightTraffic1 = mShowNightTraffic2 = mShowNightTraffic3 = false;
        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        mDbHelper = CustomDatabaseHelper.getInstance(mContext);
        mDataMap = CustomDatabaseHelper.readTrafficData(mDbHelper);
        mPrefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));

        mIsNight =  TrafficCountService.getIsNight();

        mTrafficDataReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (isVisible()) {
                    try {
                        boolean[] isNight = TrafficCountService.getIsNight();
                        if (!mShowNightTraffic1)
                            TOT1.setText(DataFormat.formatData(context, isNight[0] ? intent.getLongExtra(Constants.TOTAL1_N, 0L) :
                                    intent.getLongExtra(Constants.TOTAL1, 0L)));
                        if (!mShowNightTraffic2)
                            TOT2.setText(DataFormat.formatData(context, isNight[1] ? intent.getLongExtra(Constants.TOTAL2_N, 0L) :
                                    intent.getLongExtra(Constants.TOTAL2, 0L)));
                        if (!mShowNightTraffic3)
                            TOT3.setText(DataFormat.formatData(context, isNight[2] ? intent.getLongExtra(Constants.TOTAL3_N, 0L) :
                                    intent.getLongExtra(Constants.TOTAL3, 0L)));
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                            if (!mShowNightTraffic1) {
                                if (RX1 != null)
                                    RX1.setText(DataFormat.formatData(context, isNight[0] ? intent.getLongExtra(Constants.SIM1RX_N, 0L) :
                                            intent.getLongExtra(Constants.SIM1RX, 0L)));
                                if (TX1 != null)
                                    TX1.setText(DataFormat.formatData(context, isNight[0] ? intent.getLongExtra(Constants.SIM1TX_N, 0L) :
                                            intent.getLongExtra(Constants.SIM1TX, 0L)));
                            }
                            if (!mShowNightTraffic2) {
                                if (RX2 != null)
                                    RX2.setText(DataFormat.formatData(context, isNight[1] ? intent.getLongExtra(Constants.SIM2RX_N, 0L) :
                                            intent.getLongExtra(Constants.SIM2RX, 0L)));
                                if (TX2 != null)
                                    TX2.setText(DataFormat.formatData(context, isNight[1] ? intent.getLongExtra(Constants.SIM2TX_N, 0L) :
                                            intent.getLongExtra(Constants.SIM2TX, 0L)));
                            }
                            if (!mShowNightTraffic3) {
                                if (RX3 != null)
                                    RX3.setText(DataFormat.formatData(context, isNight[2] ? intent.getLongExtra(Constants.SIM3RX_N, 0L) :
                                            intent.getLongExtra(Constants.SIM3RX, 0L)));
                                if (TX3 != null)
                                    TX3.setText(DataFormat.formatData(context, isNight[2] ? intent.getLongExtra(Constants.SIM3TX_N, 0L) :
                                            intent.getLongExtra(Constants.SIM3TX, 0L)));
                            }
                        }
                        if (!mShowNightTraffic1) {
                            if (intent.getStringExtra(Constants.OPERATOR1).equals("") || !intent.hasExtra(Constants.OPERATOR1))
                                SIM1.setText(isNight[0] ? "SIM1" + getString(R.string.night) : "SIM1");
                            else {
                                mOperatorNames[0] = intent.getStringExtra(Constants.OPERATOR1);
                                SIM1.setText(isNight[0] ? mOperatorNames[0] + getString(R.string.night) : mOperatorNames[0]);
                            }
                        }
                        if (mSimQuantity < 2) {
                            SIM2.setText(getString(R.string.not_available));
                            SIM3.setText(getString(R.string.not_available));
                        } else {
                            if (!mShowNightTraffic2) {
                                if (mSimQuantity >= 2) {
                                    if (!intent.hasExtra(Constants.OPERATOR2) || intent.getStringExtra(Constants.OPERATOR2).equals(""))
                                        SIM2.setText(isNight[1] ? "SIM2" + getString(R.string.night) : "SIM2");
                                    else {
                                        mOperatorNames[1] = intent.getStringExtra(Constants.OPERATOR2);
                                        SIM2.setText(isNight[1] ? mOperatorNames[1] + getString(R.string.night) : mOperatorNames[1]);
                                    }
                                }
                            }
                            if (mSimQuantity == 3) {
                                if (!mShowNightTraffic3) {
                                    if (!intent.hasExtra(Constants.OPERATOR3) || intent.getStringExtra(Constants.OPERATOR3).equals(""))
                                        SIM3.setText(isNight[2] ? "SIM3" + getString(R.string.night) : "SIM3");
                                    else {
                                        mOperatorNames[2] = intent.getStringExtra(Constants.OPERATOR3);
                                        SIM3.setText(isNight[2] ? mOperatorNames[2] + getString(R.string.night) : mOperatorNames[2]);
                                    }
                                }
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
        try {
            if (sim > Constants.DISABLED)
                MobileUtils.toggleMobileDataConnection(true, mContext, sim);
            else
                MobileUtils.toggleMobileDataConnection(false, mContext, Constants.DISABLED);
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
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
    public void onResume(){
        super.onResume();
        android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);;
        toolBar.setSubtitle(R.string.notification_title);
        setButtonLimitText();
        CustomApplication.activityResumed();
    }

    @Override
    public void onPause() {
        super.onPause();
        CustomApplication.activityPaused();
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.traffic_menu, menu);
        mService = menu.getItem(0);
        mMobileData = menu.getItem(1);
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && !CustomApplication.hasRoot()) {
            mMobileData.setEnabled(false);
            mMobileData.setVisible(false);
        } else {
            switch (MobileUtils.getMobileDataInfo(mContext, false)[0]) {
                case 0:
                    mMobileData.setEnabled(true);
                    mMobileData.setTitle(R.string.action_enable);
                    mMobileData.setIcon(R.drawable.ic_action_mobile_on);
                    break;
                case 1:
                    mMobileData.setEnabled(false);
                    mMobileData.setIcon(R.drawable.ic_action_mobile_off);
                    break;
                case 2:
                    mMobileData.setEnabled(true);
                    mMobileData.setTitle(R.string.action_disable);
                    mMobileData.setIcon(R.drawable.ic_action_mobile_off);
                    break;
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
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[5], true).apply();
                    mContext.stopService(new Intent(mContext, TrafficCountService.class));
                    TIP.setText(getString(R.string.service_disabled));
                    item.setTitle(R.string.action_start);
                    mService.setIcon(R.drawable.ic_action_enable);
                    mIsRunning = CustomApplication.isMyServiceRunning(TrafficCountService.class, mContext);
                }
                else {
                    mPrefs.edit().putBoolean(Constants.PREF_OTHER[5], false).apply();
                    mContext.startService(new Intent(mContext, TrafficCountService.class));
                    TIP.setText(getString(R.string.tip));
                    item.setTitle(R.string.action_stop);
                    mService.setIcon(R.drawable.ic_action_disable);
                    mIsRunning = CustomApplication.isMyServiceRunning(TrafficCountService.class, mContext);
                }
                break;
            case R.id.action_mobile_data_on_off:
                showDialog(Constants.ON_OFF);
                break;
        }
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

        view.findViewById(R.id.buttonClear1).setOnClickListener(this);
        view.findViewById(R.id.buttonClear2).setOnClickListener(this);
        view.findViewById(R.id.buttonClear3).setOnClickListener(this);
        view.findViewById(R.id.settings).setOnClickListener(this);

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
                if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                    if (TX2 != null)
                        TX2.setVisibility(View.VISIBLE);
                    if (RX2 != null)
                        RX2.setVisibility(View.VISIBLE);
                }
                SIM2.setVisibility(View.VISIBLE);
                TOT2.setVisibility(View.VISIBLE);
                view.findViewById(R.id.buttonClear2).setVisibility(View.VISIBLE);
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
                view.findViewById(R.id.buttonClear3).setVisibility(View.VISIBLE);
                bLim3.setVisibility(View.VISIBLE);
            } else
                view.findViewById(R.id.sim3row).setVisibility(View.VISIBLE);

        SIM1.setOnClickListener(this);
        SIM2.setOnClickListener(this);
        SIM3.setOnClickListener(this);
        bLim1.setOnClickListener(this);
        bLim2.setOnClickListener(this);
        bLim3.setOnClickListener(this);

        setButtonLimitText();

        mDataMap = CustomDatabaseHelper.readTrafficData(mDbHelper);
        if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
            RX1.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mDataMap.get(Constants.SIM1RX_N) :
                    (long) mDataMap.get(Constants.SIM1RX)));
            TX1.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mDataMap.get(Constants.SIM1TX_N) :
                    (long) mDataMap.get(Constants.SIM1TX)));
            RX2.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mDataMap.get(Constants.SIM2RX_N) :
                    (long) mDataMap.get(Constants.SIM2RX)));
            TX2.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mDataMap.get(Constants.SIM2TX_N) :
                    (long) mDataMap.get(Constants.SIM2TX)));
            RX3.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mDataMap.get(Constants.SIM3RX_N) :
                    (long) mDataMap.get(Constants.SIM3RX)));
            TX3.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mDataMap.get(Constants.SIM3TX_N) :
                    (long) mDataMap.get(Constants.SIM3TX)));
        }
        TOT1.setText(DataFormat.formatData(mContext, mIsNight[0] ? (long) mDataMap.get(Constants.TOTAL1_N) :
                (long) mDataMap.get(Constants.TOTAL1)));
        TOT2.setText(DataFormat.formatData(mContext, mIsNight[1] ? (long) mDataMap.get(Constants.TOTAL2_N) :
                (long) mDataMap.get(Constants.TOTAL2)));
        TOT3.setText(DataFormat.formatData(mContext, mIsNight[2] ? (long) mDataMap.get(Constants.TOTAL3_N) :
                (long) mDataMap.get(Constants.TOTAL3)));

        setLabelText((int) mDataMap.get(Constants.LAST_ACTIVE_SIM), "0", "0");
        
        SIM1.setText(mOperatorNames[0]);
        SIM2.setText(mOperatorNames[1]);
        SIM3.setText(mOperatorNames[2]);
        

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setLabelText(int sim, String rx, String tx) {
        int swtch = MobileUtils.getMobileDataInfo(mContext, false)[0];
        switch (sim) {
            default:
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
        boolean[] isNight =  TrafficCountService.getIsNight();
        switch (v.getId()) {
            case R.id.settings:
                final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
                Intent settIntent = new Intent(Intent.ACTION_MAIN);
                settIntent.setComponent(cn);
                settIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settIntent);
                break;
            case R.id.buttonClear1:
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class, mContext))
                    EventBus.getDefault().post(new ClearTrafficEvent(Constants.SIM1));
                else {
                    mDataMap = CustomDatabaseHelper.readTrafficData(mDbHelper);
                    if (isNight[0]) {
                        mDataMap.put(Constants.SIM1RX_N, 0L);
                        mDataMap.put(Constants.SIM1TX_N, 0L);
                        mDataMap.put(Constants.TOTAL1_N, 0L);
                    } else {
                        mDataMap.put(Constants.SIM1RX, 0L);
                        mDataMap.put(Constants.SIM1TX, 0L);
                        mDataMap.put(Constants.TOTAL1, 0L);
                    }
                    CustomDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX1 != null)
                            RX1.setText(DataFormat.formatData(mContext, isNight[0] ? (long) mDataMap.get(Constants.SIM1RX_N) :
                                    (long) mDataMap.get(Constants.SIM1RX)));
                        if (TX1 != null)
                            TX1.setText(DataFormat.formatData(mContext, isNight[0] ? (long) mDataMap.get(Constants.SIM1TX_N) :
                                    (long) mDataMap.get(Constants.SIM1TX)));
                    }
                    TOT1.setText(DataFormat.formatData(mContext, isNight[0] ? (long) mDataMap.get(Constants.TOTAL1_N) :
                            (long) mDataMap.get(Constants.TOTAL1)));
                }
                break;
            case R.id.buttonClear2:
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class, mContext))
                    EventBus.getDefault().post(new ClearTrafficEvent(Constants.SIM2));
                else {
                    mDataMap = CustomDatabaseHelper.readTrafficData(mDbHelper);
                    if (isNight[1]) {
                        mDataMap.put(Constants.SIM2RX_N, 0L);
                        mDataMap.put(Constants.SIM2TX_N, 0L);
                        mDataMap.put(Constants.TOTAL2_N, 0L);
                    } else {
                        mDataMap.put(Constants.SIM2RX, 0L);
                        mDataMap.put(Constants.SIM2TX, 0L);
                        mDataMap.put(Constants.TOTAL2, 0L);
                    }
                    CustomDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX2 != null)
                            RX2.setText(DataFormat.formatData(mContext, isNight[1] ? (long) mDataMap.get(Constants.SIM2RX_N) :
                                    (long) mDataMap.get(Constants.SIM2RX)));
                        if (TX2 != null)
                            TX2.setText(DataFormat.formatData(mContext, isNight[1] ? (long) mDataMap.get(Constants.SIM2TX_N) :
                                    (long) mDataMap.get(Constants.SIM2TX)));
                    }
                    TOT2.setText(DataFormat.formatData(mContext, isNight[1] ? (long) mDataMap.get(Constants.TOTAL2_N) :
                            (long) mDataMap.get(Constants.TOTAL2)));
                }
                break;
            case R.id.buttonClear3:
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class, mContext))
                    EventBus.getDefault().post(new ClearTrafficEvent(Constants.SIM3));
                else {
                    mDataMap = CustomDatabaseHelper.readTrafficData(mDbHelper);
                    if (isNight[2]) {
                        mDataMap.put(Constants.SIM3RX_N, 0L);
                        mDataMap.put(Constants.SIM3TX_N, 0L);
                        mDataMap.put(Constants.TOTAL3_N, 0L);
                    } else {
                        mDataMap.put(Constants.SIM3RX, 0L);
                        mDataMap.put(Constants.SIM3TX, 0L);
                        mDataMap.put(Constants.TOTAL3, 0L);
                    }
                    CustomDatabaseHelper.writeTrafficData(mDataMap, mDbHelper);
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX3 != null)
                            RX3.setText(DataFormat.formatData(mContext, isNight[2] ? (long) mDataMap.get(Constants.SIM3RX_N) :
                                    (long) mDataMap.get(Constants.SIM3RX)));
                        if (TX3 != null)
                            TX3.setText(DataFormat.formatData(mContext, isNight[2] ? (long) mDataMap.get(Constants.SIM3TX_N) :
                                    (long) mDataMap.get(Constants.SIM3TX)));
                    }
                    TOT3.setText(DataFormat.formatData(mContext, isNight[2] ? (long) mDataMap.get(Constants.TOTAL3_N) :
                            (long) mDataMap.get(Constants.TOTAL3)));
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
                                RX1.setText(DataFormat.formatData(mContext, !isNight[0] ? (long) mDataMap.get(Constants.SIM1RX_N) :
                                        (long) mDataMap.get(Constants.SIM1RX)));
                            if (TX1 != null)
                                TX1.setText(DataFormat.formatData(mContext, !isNight[0] ? (long) mDataMap.get(Constants.SIM1TX_N) :
                                        (long) mDataMap.get(Constants.SIM1TX)));
                        }
                        TOT1.setText(DataFormat.formatData(mContext, !isNight[0] ? (long) mDataMap.get(Constants.TOTAL1_N) :
                                (long) mDataMap.get(Constants.TOTAL1)));
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
                                RX2.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mDataMap.get(Constants.SIM2RX_N) :
                                        (long) mDataMap.get(Constants.SIM2RX)));
                            if (TX2 != null)
                                TX2.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mDataMap.get(Constants.SIM2TX_N) :
                                        (long) mDataMap.get(Constants.SIM2TX)));
                        }
                        TOT2.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mDataMap.get(Constants.TOTAL2_N) :
                                (long) mDataMap.get(Constants.TOTAL2)));
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
                                RX3.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mDataMap.get(Constants.SIM3RX_N) :
                                        (long) mDataMap.get(Constants.SIM3RX)));
                            if (TX3 != null)
                                TX3.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mDataMap.get(Constants.SIM3TX_N) :
                                        (long) mDataMap.get(Constants.SIM2TX)));
                        }
                        TOT3.setText(DataFormat.formatData(mContext, !isNight[1] ? (long) mDataMap.get(Constants.TOTAL3_N) :
                                (long) mDataMap.get(Constants.TOTAL3)));
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

        String limit1 = mIsNight[0] ? mPrefs.getString(Constants.PREF_SIM1[18], "") : mPrefs.getString(Constants.PREF_SIM1[1], "");
        String limit2 = mIsNight[1] ? mPrefs.getString(Constants.PREF_SIM2[18], "") : mPrefs.getString(Constants.PREF_SIM2[1], "");
        String limit3 = mIsNight[2] ? mPrefs.getString(Constants.PREF_SIM3[18], "") : mPrefs.getString(Constants.PREF_SIM3[1], "");

        int value1;
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

        limit1 = !limit1.equals("") ? DataFormat.formatData(mContext, (long) lim1) : getString(R.string.not_set);
        limit2 = !limit2.equals("") ? DataFormat.formatData(mContext, (long) lim2) : getString(R.string.not_set);
        limit3 = !limit3.equals("") ? DataFormat.formatData(mContext, (long) lim3) : getString(R.string.not_set);

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
}
