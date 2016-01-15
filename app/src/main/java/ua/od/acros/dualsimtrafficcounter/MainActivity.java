package ua.od.acros.dualsimtrafficcounter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.stericson.RootTools.RootTools;

import org.acra.ACRA;

import ua.od.acros.dualsimtrafficcounter.dialogs.OnOffDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.SetUsageDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ShowTrafficForDateDialog;
import ua.od.acros.dualsimtrafficcounter.settings.LimitFragment;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MTKUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;
import ua.od.acros.dualsimtrafficcounter.widget.InfoWidget;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, Button.OnClickListener{

    private static final String FIRST_RUN = "first_run";
    private static final String ANDROID_5_0 = "API21";
    private static final String MTK = "mtk";
    private TextView SIM, TOT1, TOT2, TOT3, TX1, TX2, TX3, RX1, RX2, RX3, TIP, SIM1, SIM2, SIM3;

    private ContentValues dataMap;

    private BroadcastReceiver dataReceiver, tipReceiver, onoffReceiver;

    private Button bLim1, bLim2, bLim3;

    private MenuItem mService, mMobileData;
    private static Context context;
    private TrafficDatabase mDatabaseHelper;
    private SharedPreferences prefs;
    private boolean needsRestart = false;
    private boolean showNight1, showNight2, showNight3;
    private String opName1, opName2, opName3;
    private boolean[] isNight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyApplication.activityResumed();
        showNight1 = showNight2 = showNight3 = false;
        opName1 = opName2 = opName3 = "";
        context = MainActivity.this;
        mDatabaseHelper = new TrafficDatabase(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
        dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
        prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        final int simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
            setContentView(R.layout.activity_main);
            RX1 = (TextView) findViewById(R.id.RX1);
            TX1 = (TextView) findViewById(R.id.TX1);
            RX2 = (TextView) findViewById(R.id.RX2);
            TX2 = (TextView) findViewById(R.id.TX2);
            RX3 = (TextView) findViewById(R.id.RX3);
            TX3 = (TextView) findViewById(R.id.TX3);
        } else
            setContentView(R.layout.activity_main_short);

        TOT1 = (TextView) findViewById(R.id.Tot1);
        TOT2 = (TextView) findViewById(R.id.Tot2);
        TOT3 = (TextView) findViewById(R.id.Tot3);
        SIM = (TextView) findViewById(R.id.sim);
        TIP = (TextView) findViewById(R.id.tip);
        SIM1 = (TextView) findViewById(R.id.sim1_name);
        SIM2 = (TextView) findViewById(R.id.sim2_name);
        SIM3 = (TextView) findViewById(R.id.sim3_name);

        bLim1 = (Button) findViewById(R.id.limit1);
        bLim2 = (Button) findViewById(R.id.limit2);
        bLim3 = (Button) findViewById(R.id.limit3);

        findViewById(R.id.buttonClear1).setOnClickListener(this);
        findViewById(R.id.buttonClear2).setOnClickListener(this);
        findViewById(R.id.buttonClear3).setOnClickListener(this);
        findViewById(R.id.settings).setOnClickListener(this);
        SIM1.setOnClickListener(this);
        SIM2.setOnClickListener(this);
        SIM3.setOnClickListener(this);
        bLim1.setOnClickListener(this);
        bLim2.setOnClickListener(this);
        bLim3.setOnClickListener(this);

        isNight =  CountService.getIsNight();

        setButtonLimitText();

        if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
            RX1.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.SIM1RX_N) :
                    (long) dataMap.get(Constants.SIM1RX)));
            TX1.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.SIM1TX_N) :
                    (long) dataMap.get(Constants.SIM1TX)));
            RX2.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.SIM2RX_N) :
                    (long) dataMap.get(Constants.SIM2RX)));
            TX2.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.SIM2TX_N) :
                    (long) dataMap.get(Constants.SIM2TX)));
            RX3.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.SIM3RX_N) :
                    (long) dataMap.get(Constants.SIM3RX)));
            TX3.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.SIM3TX_N) :
                    (long) dataMap.get(Constants.SIM3TX)));
        }
        TOT1.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.TOTAL1_N) :
                (long) dataMap.get(Constants.TOTAL1)));
        TOT2.setText(DataFormat.formatData(context, isNight[1] ? (long) dataMap.get(Constants.TOTAL2_N) :
                (long) dataMap.get(Constants.TOTAL2)));
        TOT3.setText(DataFormat.formatData(context, isNight[2] ? (long) dataMap.get(Constants.TOTAL3_N) :
                (long) dataMap.get(Constants.TOTAL3)));

        setLabelText((int) dataMap.get(Constants.LAST_ACTIVE_SIM), "0", "0");

        dataReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean[] isNight =  CountService.getIsNight();
                if (!showNight1)
                    TOT1.setText(DataFormat.formatData(context, isNight[0] ? intent.getLongExtra(Constants.TOTAL1_N, 0L) :
                        intent.getLongExtra(Constants.TOTAL1, 0L)));
                if (!showNight2)
                    TOT2.setText(DataFormat.formatData(context, isNight[1] ? intent.getLongExtra(Constants.TOTAL2_N, 0L) :
                        intent.getLongExtra(Constants.TOTAL2, 0L)));
                if (!showNight3)
                    TOT3.setText(DataFormat.formatData(context, isNight[2] ? intent.getLongExtra(Constants.TOTAL3_N, 0L) :
                        intent.getLongExtra(Constants.TOTAL3, 0L)));
                if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                    if (!showNight1) {
                        if (RX1 != null)
                            RX1.setText(DataFormat.formatData(context, isNight[0] ? intent.getLongExtra(Constants.SIM1RX_N, 0L) :
                                    intent.getLongExtra(Constants.SIM1RX, 0L)));
                        if (TX1 != null)
                            TX1.setText(DataFormat.formatData(context, isNight[0] ? intent.getLongExtra(Constants.SIM1TX_N, 0L) :
                                    intent.getLongExtra(Constants.SIM1TX, 0L)));
                    }
                    if (!showNight2) {
                        if (RX2 != null)
                            RX2.setText(DataFormat.formatData(context, isNight[1] ? intent.getLongExtra(Constants.SIM2RX_N, 0L) :
                                    intent.getLongExtra(Constants.SIM2RX, 0L)));
                        if (TX2 != null)
                            TX2.setText(DataFormat.formatData(context, isNight[1] ? intent.getLongExtra(Constants.SIM2TX_N, 0L) :
                                    intent.getLongExtra(Constants.SIM2TX, 0L)));
                    }
                    if (!showNight3) {
                        if (RX3 != null)
                            RX3.setText(DataFormat.formatData(context, isNight[2] ? intent.getLongExtra(Constants.SIM3RX_N, 0L) :
                                    intent.getLongExtra(Constants.SIM3RX, 0L)));
                        if (TX3 != null)
                            TX3.setText(DataFormat.formatData(context, isNight[2] ? intent.getLongExtra(Constants.SIM3TX_N, 0L) :
                                    intent.getLongExtra(Constants.SIM3TX, 0L)));
                    }
                }
                if (!showNight1) {
                    if (intent.getStringExtra(Constants.OPERATOR1).equals("") || !intent.hasExtra(Constants.OPERATOR1))
                        SIM1.setText(isNight[0] ? "SIM1" + getResources().getString(R.string.night) : "SIM1");
                    else {
                        opName1 = intent.getStringExtra(Constants.OPERATOR1);
                        SIM1.setText(isNight[0] ? opName1 + getResources().getString(R.string.night) : opName1);
                    }
                }
                if (simNumber < 2) {
                    SIM2.setText(getResources().getString(R.string.single_sim));
                    SIM3.setText(getResources().getString(R.string.single_sim));
                } else {
                    if (!showNight2) {
                        if (simNumber >= 2) {
                            if (!intent.hasExtra(Constants.OPERATOR2) || intent.getStringExtra(Constants.OPERATOR2).equals(""))
                                SIM2.setText(isNight[1] ? "SIM2" + getResources().getString(R.string.night) : "SIM2");
                            else {
                                opName2 = intent.getStringExtra(Constants.OPERATOR2);
                                SIM2.setText(isNight[1] ? opName2 + getResources().getString(R.string.night) : opName2);
                            }
                        }
                    }
                    if (simNumber == 3) {
                        if (!showNight3) {
                            if (!intent.hasExtra(Constants.OPERATOR3) || intent.getStringExtra(Constants.OPERATOR3).equals(""))
                                SIM3.setText(isNight[2] ? "SIM3" + getResources().getString(R.string.night) : "SIM3");
                            else {
                                opName3 = intent.getStringExtra(Constants.OPERATOR3);
                                SIM3.setText(isNight[2] ? opName3 + getResources().getString(R.string.night) : opName3);
                            }
                        }
                    }
                }
                if (!intent.getBooleanExtra(Constants.TIP, false))
                    TIP.setText(getResources().getString(R.string.tip));
                else
                    TIP.setText(getResources().getString(R.string.count_stopped_tip));
                String rxSpeed = DataFormat.formatData(context, intent.getLongExtra(Constants.SPEEDRX, 0L));
                String txSpeed = DataFormat.formatData(context, intent.getLongExtra(Constants.SPEEDTX, 0L));
                setLabelText(intent.getIntExtra(Constants.SIM_ACTIVE, 0), txSpeed, rxSpeed);
                invalidateOptionsMenu();
            }
        };
        IntentFilter countServiceFilter = new IntentFilter(Constants.BROADCAST_ACTION);
        registerReceiver(dataReceiver, countServiceFilter);

        tipReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                TIP.setText(getResources().getString(R.string.count_stopped_tip));
            }
        };
        IntentFilter tipFilter = new IntentFilter(Constants.TIP);
        registerReceiver(tipReceiver, tipFilter);

        onoffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int simChosen = intent.getIntExtra("sim", Constants.NULL);
                try {
                    if (simChosen > Constants.DISABLED)
                        MobileUtils.toggleMobileDataConnection(true, context, simChosen);
                    else
                        MobileUtils.toggleMobileDataConnection(false, context, Constants.DISABLED);
                } catch (Exception e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                invalidateOptionsMenu();
            }
        };
        IntentFilter onoffFilter = new IntentFilter(Constants.ON_OFF);
        registerReceiver(onoffReceiver, onoffFilter);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
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
            findViewById(R.id.buttonClear2).setVisibility(View.GONE);
            bLim2.setVisibility(View.GONE);
            SIM3.setVisibility(View.GONE);
            TOT3.setVisibility(View.GONE);
            findViewById(R.id.buttonClear3).setVisibility(View.GONE);
            bLim3.setVisibility(View.GONE);
        } else {
            findViewById(R.id.sim2row).setVisibility(View.GONE);
            findViewById(R.id.sim3row).setVisibility(View.GONE);
        }
        if (simNumber >= 2)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                    if (TX2 != null)
                        TX2.setVisibility(View.VISIBLE);
                    if (RX2 != null)
                        RX2.setVisibility(View.VISIBLE);
                }
                SIM2.setVisibility(View.VISIBLE);
                TOT2.setVisibility(View.VISIBLE);
                findViewById(R.id.buttonClear2).setVisibility(View.VISIBLE);
                bLim2.setVisibility(View.VISIBLE);
            } else
                findViewById(R.id.sim2row).setVisibility(View.VISIBLE);
        if (simNumber == 3)
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                    if (TX3 != null)
                        TX3.setVisibility(View.VISIBLE);
                    if (RX3 != null)
                        RX3.setVisibility(View.VISIBLE);
                }
                SIM3.setVisibility(View.VISIBLE);
                TOT3.setVisibility(View.VISIBLE);
                findViewById(R.id.buttonClear3).setVisibility(View.VISIBLE);
                bLim3.setVisibility(View.VISIBLE);
            } else
                findViewById(R.id.sim3row).setVisibility(View.VISIBLE);
        Intent intent = new Intent(context, InfoWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), InfoWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
        if (!CheckServiceRunning.isMyServiceRunning(WatchDogService.class, context) && prefs.getBoolean(Constants.PREF_OTHER[4], true))
            startService(new Intent(context, WatchDogService.class));
        if (!CheckServiceRunning.isMyServiceRunning(CountService.class, context) && !prefs.getBoolean(Constants.PREF_OTHER[5], false))
            startService(new Intent(context, CountService.class));

        if (prefs.getBoolean(Constants.PREF_OTHER[9], true)) {
            showDialog(FIRST_RUN);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP &&
                    !RootTools.isAccessGiven())
                showDialog(ANDROID_5_0);
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP_MR1 &&
                    !MTKUtils.isMtkDevice())
                showDialog(MTK);
            prefs.edit().putBoolean(Constants.PREF_OTHER[9], false).apply();
        }
    }

    public static Context getAppContext() {
        return MainActivity.context;
    }

    private void setLabelText(int sim, String rx, String tx) {
        int swtch = MobileUtils.getMobileDataInfo(context, false)[0];
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
                    SIM.setText(String.format(getResources().getString(R.string.sim1_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
            case Constants.SIM2:
                if (swtch == 2)
                    SIM.setText(String.format(getResources().getString(R.string.sim2_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
            case Constants.SIM3:
                if (swtch == 2)
                    SIM.setText(String.format(getResources().getString(R.string.sim3_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mService = menu.getItem(0);
        mMobileData = menu.getItem(1);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (CheckServiceRunning.isMyServiceRunning(CountService.class, context)) {
            mService.setTitle(R.string.action_stop);
            mService.setIcon(R.drawable.ic_action_disable);
        }
        else {
            mService.setTitle(R.string.action_start);
            mService.setIcon(R.drawable.ic_action_enable);
        }
        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && !RootTools.isAccessGiven()) ||
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && !MTKUtils.isMtkDevice())) {
            mMobileData.setEnabled(false);
            mMobileData.setVisible(false);
        } else {
            switch (MobileUtils.getMobileDataInfo(context, false)[0]) {
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
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivityForResult(intent, 0);
                break;
            case R.id.action_service_start_stop:
                if (item.getTitle().toString().equals(getResources().getString(R.string.action_stop))) {
                    prefs.edit().putBoolean(Constants.PREF_OTHER[5], true).apply();
                    stopService(new Intent(context, CountService.class));
                    TIP.setText(getResources().getString(R.string.service_disabled));
                    item.setTitle(R.string.action_start);
                    mService.setIcon(R.drawable.ic_action_enable);
                }
                else {
                    prefs.edit().putBoolean(Constants.PREF_OTHER[5], false).apply();
                    startService(new Intent(context, CountService.class));
                    TIP.setText(getResources().getString(R.string.tip));
                    item.setTitle(R.string.action_stop);
                    mService.setIcon(R.drawable.ic_action_disable);
                }
                break;
            case R.id.action_mobile_data_on_off:
                showDialog(Constants.ON_OFF);
                break;
            case R.id.action_set_usage:
                showDialog(Constants.SET_USAGE);
                break;
            case R.id.action_show_history:
                showDialog(Constants.TRAFFIC_FOR_DATE);
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
            case Constants.SET_USAGE:
                dialog = SetUsageDialog.newInstance();
                break;
            case Constants.TRAFFIC_FOR_DATE:
                dialog = ShowTrafficForDateDialog.newInstance(false);
                break;
            case FIRST_RUN:
                new AlertDialog.Builder(context)
                        .setTitle(R.string.attention)
                        .setMessage(R.string.set_sim_number)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case MTK:
                new AlertDialog.Builder(context)
                        .setTitle(R.string.attention)
                        .setMessage(R.string.on_off_not_supported)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case ANDROID_5_0:
                new AlertDialog.Builder(context)
                        .setTitle(R.string.attention)
                        .setMessage(R.string.need_root)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
        }
        if (dialog != null)
            dialog.show(getFragmentManager(), "dialog");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
        outState.putLong(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
        outState.putLong(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
        outState.putLong(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
        outState.putLong(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
        outState.putLong(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
        outState.putLong(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
        outState.putLong(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
        outState.putLong(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
        outState.putLong(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
        outState.putString(Constants.SIM_ACTIVE, SIM.getText().toString());
    }

    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
            RX1.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.SIM1RX)));
            TX1.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.SIM1TX)));
            RX2.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.SIM2RX)));
            TX2.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.SIM2TX)));
            RX3.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.SIM3RX)));
            TX3.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.SIM3TX)));
        }
        TOT1.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.TOTAL1)));
        TOT2.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.TOTAL2)));
        TOT3.setText(DataFormat.formatData(context, savedInstanceState.getLong(Constants.TOTAL3)));
        SIM.setText(savedInstanceState.getString(Constants.SIM_ACTIVE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
        setButtonLimitText();
        if (needsRestart)
            finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication.activityPaused();
        if (dataReceiver != null)
            unregisterReceiver(dataReceiver);
        if (tipReceiver != null)
            unregisterReceiver(tipReceiver);
        if (onoffReceiver != null)
            unregisterReceiver(onoffReceiver);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[4])) {
            if (!sharedPreferences.getBoolean(key, false)) {
                stopService(new Intent(context, WatchDogService.class));
                prefs.edit().putBoolean(Constants.PREF_OTHER[6], true).apply();
            } else {
                startService(new Intent(context, WatchDogService.class));
                prefs.edit().putBoolean(Constants.PREF_OTHER[6], false).apply();
            }
        }
        if (key.equals(Constants.PREF_OTHER[7]))
            needsRestart = true;
    }

    @Override
    public void onClick(View v) {
        boolean[] isNight =  CountService.getIsNight();
        switch (v.getId()) {
            case R.id.settings:
                final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
                Intent settIntent = new Intent(Intent.ACTION_MAIN);
                settIntent.setComponent(cn);
                settIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settIntent);
                break;
            case R.id.buttonClear1:
                if (CheckServiceRunning.isMyServiceRunning(CountService.class, context)) {
                    Intent clear1Intent = new Intent(Constants.CLEAR1);
                    sendBroadcast(clear1Intent);
                } else {
                    dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
                    if (isNight[0]) {
                        dataMap.put(Constants.SIM1RX_N, 0L);
                        dataMap.put(Constants.SIM1TX_N, 0L);
                        dataMap.put(Constants.TOTAL1_N, 0L);
                    } else {
                        dataMap.put(Constants.SIM1RX, 0L);
                        dataMap.put(Constants.SIM1TX, 0L);
                        dataMap.put(Constants.TOTAL1, 0L);
                    }
                    TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                    if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX1 != null)
                            RX1.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.SIM1RX_N) :
                                    (long) dataMap.get(Constants.SIM1RX)));
                        if (TX1 != null)
                            TX1.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.SIM1TX_N) :
                                    (long) dataMap.get(Constants.SIM1TX)));
                    }
                    TOT1.setText(DataFormat.formatData(context, isNight[0] ? (long) dataMap.get(Constants.TOTAL1_N) :
                            (long) dataMap.get(Constants.TOTAL1)));
                }
                break;
            case R.id.buttonClear2:
                if (CheckServiceRunning.isMyServiceRunning(CountService.class, context)) {
                    Intent clear2Intent = new Intent(Constants.CLEAR2);
                    sendBroadcast(clear2Intent);
                } else {
                    dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
                    if (isNight[1]) {
                        dataMap.put(Constants.SIM2RX_N, 0L);
                        dataMap.put(Constants.SIM2TX_N, 0L);
                        dataMap.put(Constants.TOTAL2_N, 0L);
                    } else {
                        dataMap.put(Constants.SIM2RX, 0L);
                        dataMap.put(Constants.SIM2TX, 0L);
                        dataMap.put(Constants.TOTAL2, 0L);
                    }
                    TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                    if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX2 != null)
                            RX2.setText(DataFormat.formatData(context, isNight[1] ? (long) dataMap.get(Constants.SIM2RX_N) :
                                    (long) dataMap.get(Constants.SIM2RX)));
                        if (TX2 != null)
                            TX2.setText(DataFormat.formatData(context, isNight[1] ? (long) dataMap.get(Constants.SIM2TX_N) :
                                    (long) dataMap.get(Constants.SIM2TX)));
                    }
                    TOT2.setText(DataFormat.formatData(context, isNight[1] ? (long) dataMap.get(Constants.TOTAL2_N) :
                            (long) dataMap.get(Constants.TOTAL2)));
                }
                break;
            case R.id.buttonClear3:
                if (CheckServiceRunning.isMyServiceRunning(CountService.class, context)) {
                    Intent clear2Intent = new Intent(Constants.CLEAR3);
                    sendBroadcast(clear2Intent);
                } else {
                    dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
                    if (isNight[2]) {
                        dataMap.put(Constants.SIM3RX_N, 0L);
                        dataMap.put(Constants.SIM3TX_N, 0L);
                        dataMap.put(Constants.TOTAL3_N, 0L);
                    } else {
                        dataMap.put(Constants.SIM3RX, 0L);
                        dataMap.put(Constants.SIM3TX, 0L);
                        dataMap.put(Constants.TOTAL3, 0L);
                    }
                    TrafficDatabase.writeTrafficData(dataMap, mDatabaseHelper);
                    if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        if (RX3 != null)
                            RX3.setText(DataFormat.formatData(context, isNight[2] ? (long) dataMap.get(Constants.SIM3RX_N) :
                                    (long) dataMap.get(Constants.SIM3RX)));
                        if (TX3 != null)
                            TX3.setText(DataFormat.formatData(context, isNight[2] ? (long) dataMap.get(Constants.SIM3TX_N) :
                                    (long) dataMap.get(Constants.SIM3TX)));
                    }
                    TOT3.setText(DataFormat.formatData(context, isNight[2] ? (long) dataMap.get(Constants.TOTAL3_N) :
                            (long) dataMap.get(Constants.TOTAL3)));
                }
                break;
            case R.id.sim1_name:
                if (prefs.getBoolean(Constants.PREF_SIM1[17], false)) {
                    showNight1 = !showNight1;
                    if (showNight1) {
                        if (opName1.equals(""))
                            SIM1.setText(!isNight[0] ? "SIM1" + getResources().getString(R.string.night) : "SIM1");
                        else
                            SIM1.setText(!isNight[0] ? opName1 + getResources().getString(R.string.night) : opName1);
                        if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                            if (RX1 != null)
                                RX1.setText(DataFormat.formatData(context, !isNight[0] ? (long) dataMap.get(Constants.SIM1RX_N) :
                                        (long) dataMap.get(Constants.SIM1RX)));
                            if (TX1 != null)
                                TX1.setText(DataFormat.formatData(context, !isNight[0] ? (long) dataMap.get(Constants.SIM1TX_N) :
                                        (long) dataMap.get(Constants.SIM1TX)));
                        }
                        TOT1.setText(DataFormat.formatData(context, !isNight[0] ? (long) dataMap.get(Constants.TOTAL1_N) :
                                (long) dataMap.get(Constants.TOTAL1)));
                    }
                }
                break;
            case R.id.sim2_name:
                if (prefs.getBoolean(Constants.PREF_SIM2[17], false)) {
                    showNight2 = !showNight2;
                    if (showNight2) {
                        if (opName2.equals(""))
                            SIM2.setText(!isNight[1] ? "SIM2" + getResources().getString(R.string.night) : "SIM2");
                        else
                            SIM2.setText(!isNight[1] ? opName2 + getResources().getString(R.string.night) : opName2);
                        if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                            if (RX2 != null)
                                RX2.setText(DataFormat.formatData(context, !isNight[1] ? (long) dataMap.get(Constants.SIM2RX_N) :
                                        (long) dataMap.get(Constants.SIM2RX)));
                            if (TX2 != null)
                                TX2.setText(DataFormat.formatData(context, !isNight[1] ? (long) dataMap.get(Constants.SIM2TX_N) :
                                        (long) dataMap.get(Constants.SIM2TX)));
                        }
                        TOT2.setText(DataFormat.formatData(context, !isNight[1] ? (long) dataMap.get(Constants.TOTAL2_N) :
                                (long) dataMap.get(Constants.TOTAL2)));
                    }
                }
                break;
            case R.id.sim3_name:
                if (prefs.getBoolean(Constants.PREF_SIM3[17], false)) {
                    showNight3 = !showNight3;
                    if (showNight3) {
                        if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                            if (opName3.equals(""))
                                SIM3.setText(!isNight[2] ? "SIM3" + getResources().getString(R.string.night) : "SIM3");
                            else
                                SIM3.setText(!isNight[2] ? opName3 + getResources().getString(R.string.night) : opName3);
                            if (RX3 != null)
                                RX3.setText(DataFormat.formatData(context, !isNight[1] ? (long) dataMap.get(Constants.SIM3RX_N) :
                                        (long) dataMap.get(Constants.SIM3RX)));
                            if (TX3 != null)
                                TX3.setText(DataFormat.formatData(context, !isNight[1] ? (long) dataMap.get(Constants.SIM3TX_N) :
                                        (long) dataMap.get(Constants.SIM2TX)));
                        }
                        TOT3.setText(DataFormat.formatData(context, !isNight[1] ? (long) dataMap.get(Constants.TOTAL3_N) :
                                (long) dataMap.get(Constants.TOTAL3)));
                    }
                }
                break;
            case R.id.limit1:
            case R.id.limit2:
            case R.id.limit3:
                Intent intent = new Intent(context, SettingsActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, LimitFragment.class.getName());
                intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                intent.putExtra(Constants.SIM_ACTIVE, v.getId());
                startActivity(intent);
                break;
        }
    }

    private void setButtonLimitText() {

        String limit1 = isNight[0] ? prefs.getString(Constants.PREF_SIM1[18], "") : prefs.getString(Constants.PREF_SIM1[1], "");
        String limit2 = isNight[1] ? prefs.getString(Constants.PREF_SIM2[18], "") : prefs.getString(Constants.PREF_SIM2[1], "");
        String limit3 = isNight[2] ? prefs.getString(Constants.PREF_SIM3[18], "") : prefs.getString(Constants.PREF_SIM3[1], "");

        int value1;
        if (prefs.getString(Constants.PREF_SIM1[2], "").equals(""))
            value1 = 0;
        else
            value1 = isNight[0] ? Integer.valueOf(prefs.getString(Constants.PREF_SIM1[19], "")) :
                    Integer.valueOf(prefs.getString(Constants.PREF_SIM1[2], ""));
        int value2;
        if (prefs.getString(Constants.PREF_SIM2[2], "").equals(""))
            value2 = 0;
        else
            value2 = isNight[1] ? Integer.valueOf(prefs.getString(Constants.PREF_SIM2[19], "")) :
                    Integer.valueOf(prefs.getString(Constants.PREF_SIM2[2], ""));
        int value3;
        if (prefs.getString(Constants.PREF_SIM3[2], "").equals(""))
            value3 = 0;
        else
            value3 = isNight[2] ? Integer.valueOf(prefs.getString(Constants.PREF_SIM3[19], "")) :
                    Integer.valueOf(prefs.getString(Constants.PREF_SIM3[2], ""));

        double lim1 = !limit1.equals("") ? DataFormat.getFormatLong(limit1, value1) : Double.MAX_VALUE;
        double lim2 = !limit2.equals("") ? DataFormat.getFormatLong(limit2, value2) : Double.MAX_VALUE;
        double lim3 = !limit3.equals("") ? DataFormat.getFormatLong(limit3, value3) : Double.MAX_VALUE;

        limit1 = !limit1.equals("") ? DataFormat.formatData(context, (long) lim1) : getResources().getString(R.string.not_set);
        limit2 = !limit2.equals("") ? DataFormat.formatData(context, (long) lim2) : getResources().getString(R.string.not_set);
        limit3 = !limit3.equals("") ? DataFormat.formatData(context, (long) lim3) : getResources().getString(R.string.not_set);

        String[] listitems = getResources().getStringArray(R.array.period_values);
        String[] list = getResources().getStringArray(R.array.period);

        for (int i = 0; i < list.length; i++) {
            if (!limit1.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(prefs.getString(Constants.PREF_SIM1[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit1 += "/" + prefs.getString(Constants.PREF_SIM1[10], "1") + getResources().getString(R.string.days);
                else
                    limit1 += "/" + list[i];

            }
            if (!limit2.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(prefs.getString(Constants.PREF_SIM2[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit2 += "/" + prefs.getString(Constants.PREF_SIM2[10], "1") + getResources().getString(R.string.days);
                else
                    limit2 += "/" + list[i];

            }
            if (!limit3.equals(getResources().getString(R.string.not_set)) && listitems[i].equals(prefs.getString(Constants.PREF_SIM3[3], "0"))) {
                if (listitems[i].equals("2"))
                    limit3 += "/" + prefs.getString(Constants.PREF_SIM3[10], "1") + getResources().getString(R.string.days);
                else
                    limit3 += "/" + list[i];

            }
        }

        bLim1.setText(limit1);
        bLim2.setText(limit2);
        bLim3.setText(limit3);
    }
}
