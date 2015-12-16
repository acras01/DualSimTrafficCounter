package ua.od.acros.dualsimtrafficcounter;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.stericson.RootTools.RootTools;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;

import ua.od.acros.dualsimtrafficcounter.dialogs.OnOffDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.SetUsageDialog;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.DateCompare;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;
import ua.od.acros.dualsimtrafficcounter.widget.InfoWidget;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, Button.OnClickListener{

    TextView SIM, TOT1, TOT2, TOT3, TX1, TX2, TX3, RX1, RX2, RX3, TIP, SIM1, SIM2, SIM3;

    Map<String, Object> dataMap = new HashMap<>();

    BroadcastReceiver dataReceiver, tipReceiver, onoffReceiver;

    private MenuItem mService, mMobileData;
    private static Context mContext;
    private TrafficDatabase mDatabaseHelper;
    private SharedPreferences prefs;
    private boolean needsRestart = false;
    private Button b1clear, b2clear, b3clear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.activityResumed();
        mContext = MainActivity.this;
        mDatabaseHelper = new TrafficDatabase(this, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
        dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
        prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
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
        b1clear = (Button) findViewById(R.id.buttonClear1);
        b2clear = (Button) findViewById(R.id.buttonClear2);
        b3clear = (Button) findViewById(R.id.buttonClear3);

        b1clear.setOnClickListener(this);
        b2clear.setOnClickListener(this);
        b3clear.setOnClickListener(this);

        if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
            RX1.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM1RX)));
            TX1.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM1TX)));
            RX2.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM2RX)));
            TX2.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM2TX)));
            RX3.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM3RX)));
            TX3.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM3TX)));
        }
        TOT1.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1)));
        TOT2.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL2)));
        TOT3.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL3)));
        SIM.setText((String) dataMap.get(Constants.SIM_ACTIVE));

        dataReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {

                TOT1.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.TOTAL1, 0)));
                TOT2.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.TOTAL2, 0)));
                TOT3.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.TOTAL3, 0)));
                if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                    if (RX1 != null)
                        RX1.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.SIM1RX, 0)));
                    if (TX1 != null)
                        TX1.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.SIM1TX, 0)));
                    if (RX2 != null)
                        RX2.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.SIM2RX, 0)));
                    if (TX2 != null)
                        TX2.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.SIM2TX, 0)));
                    if (RX3 != null)
                        RX3.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.SIM3RX, 0)));
                    if (TX3 != null)
                        TX3.setText(DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.SIM3TX, 0)));
                }
                if (intent.getStringExtra(Constants.OPERATOR1).equals("") || !intent.hasExtra(Constants.OPERATOR1))
                    SIM1.setText("SIM1");
                else
                    SIM1.setText(intent.getStringExtra(Constants.OPERATOR1));
                if (MobileDataControl.isMultiSim(getAppContext()) < 2) {
                    SIM2.setText(getResources().getString(R.string.single_sim));
                    SIM3.setText(getResources().getString(R.string.single_sim));
                } else {
                    if (MobileDataControl.isMultiSim(getAppContext()) >= 2) {
                        if (!intent.hasExtra(Constants.OPERATOR2) || intent.getStringExtra(Constants.OPERATOR2).equals(""))
                            SIM2.setText("SIM2");
                        else
                            SIM2.setText(intent.getStringExtra(Constants.OPERATOR2));
                    }
                    if (MobileDataControl.isMultiSim(getAppContext()) == 3) {
                        if (!intent.hasExtra(Constants.OPERATOR3) || intent.getStringExtra(Constants.OPERATOR3).equals(""))
                            SIM3.setText("SIM3");
                        else
                            SIM3.setText(intent.getStringExtra(Constants.OPERATOR3));
                    }
                }
                if (!intent.getBooleanExtra(Constants.TIP, false))
                    TIP.setText(getResources().getString(R.string.tip));
                else
                    TIP.setText(getResources().getString(R.string.service_disabled_tip));
                String rxSpeed = DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.SPEEDRX, 0L));
                String txSpeed = DataFormat.formatData(getAppContext(), intent.getLongExtra(Constants.SPEEDTX, 0L));
                int swtch = MobileDataControl.getMobileDataInfo(getAppContext())[0];
                switch (intent.getIntExtra(Constants.SIM_ACTIVE, 0)) {
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
                            SIM.setText(String.format(getResources().getString(R.string.sim1_act), txSpeed, rxSpeed));
                        else if (swtch == 1)
                            SIM.setText(R.string.other_network);
                        if (swtch == 0)
                            SIM.setText(R.string.data_dis);
                        break;
                    case Constants.SIM2:
                        if (swtch == 2)
                            SIM.setText(String.format(getResources().getString(R.string.sim2_act), txSpeed, rxSpeed));
                        else if (swtch == 1)
                            SIM.setText(R.string.other_network);
                        if (swtch == 0)
                            SIM.setText(R.string.data_dis);
                        break;
                    case Constants.SIM3:
                        if (swtch == 2)
                            SIM.setText(String.format(getResources().getString(R.string.sim3_act), txSpeed, rxSpeed));
                        else if (swtch == 1)
                            SIM.setText(R.string.other_network);
                        if (swtch == 0)
                            SIM.setText(R.string.data_dis);
                        break;
                }
                invalidateOptionsMenu();
            }
        };
        IntentFilter countServiceFilter = new IntentFilter(Constants.BROADCAST_ACTION);
        registerReceiver(dataReceiver, countServiceFilter);
        tipReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                TIP.setText(getResources().getString(R.string.service_disabled_tip));
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
                        MobileDataControl.toggleMobileDataConnection(true, getAppContext(), simChosen);
                    else
                        MobileDataControl.toggleMobileDataConnection(false, getAppContext(), Constants.DISABLED);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                invalidateOptionsMenu();
            }
        };
        IntentFilter onoffFilter = new IntentFilter(Constants.ON_OFF);
        registerReceiver(onoffReceiver, onoffFilter);
        if (prefs.getBoolean(Constants.PREF_OTHER[9], true)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Constants.PREF_OTHER[9], false);
            editor.apply();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && !RootTools.isAccessGiven()) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.attention)
                        .setMessage(R.string.need_root)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .create();
                dialog.show();
            }
        }
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
            SIM3.setVisibility(View.GONE);
            TOT3.setVisibility(View.GONE);
            findViewById(R.id.buttonClear3).setVisibility(View.GONE);
        } else {
            findViewById(R.id.sim2row).setVisibility(View.GONE);
            findViewById(R.id.sim3row).setVisibility(View.GONE);
        }
        if (MobileDataControl.isMultiSim(getAppContext()) >= 2)
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
            } else
                findViewById(R.id.sim2row).setVisibility(View.VISIBLE);
        if (MobileDataControl.isMultiSim(getAppContext()) == 3)
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
            } else
                findViewById(R.id.sim3row).setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, InfoWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), InfoWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
        if (!isMyServiceRunning(WatchDogService.class) && prefs.getBoolean(Constants.PREF_OTHER[4], true))
            startService(new Intent(this, WatchDogService.class));
        if (!isMyServiceRunning(CountService.class) && !prefs.getBoolean(Constants.PREF_OTHER[5], false))
            startService(new Intent(this, CountService.class));
        /*else
            Toast.makeText(this, R.string.service_running, Toast.LENGTH_LONG).show();*/
    }

    public static Context getAppContext() {
        return MainActivity.mContext;
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mService = menu.getItem(0);
        mMobileData = menu.getItem(1);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        if (isMyServiceRunning(CountService.class)) {
            mService.setTitle(R.string.action_stop);
            mService.setIcon(R.drawable.ic_action_disable);
        }
        else {
            mService.setTitle(R.string.action_start);
            mService.setIcon(R.drawable.ic_action_enable);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && !RootTools.isAccessGiven()) {
            mMobileData.setEnabled(false);
            mMobileData.setVisible(false);
        } else {
            switch (MobileDataControl.getMobileDataInfo(getAppContext())[0]) {
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
                Intent intent = new Intent(getAppContext(), SettingsActivity.class);
                startActivityForResult(intent, 0);
                break;
            case R.id.action_service_start_stop:
                if (item.getTitle().toString().equals(getResources().getString(R.string.action_stop))) {
                    prefs.edit().putBoolean(Constants.PREF_OTHER[5], true).apply();
                    stopService(new Intent(this, CountService.class));
                    TIP.setText(getResources().getString(R.string.service_disabled));
                    item.setTitle(R.string.action_start);
                    mService.setIcon(R.drawable.ic_action_enable);
                }
                else {
                    prefs.edit().putBoolean(Constants.PREF_OTHER[5], false).apply();
                    startService(new Intent(this, CountService.class));
                    TIP.setText(getResources().getString(R.string.tip));
                    item.setTitle(R.string.action_stop);
                    mService.setIcon(R.drawable.ic_action_disable);
                }
                break;
            case R.id.action_mobile_data_on_off:
                DialogFragment frg1 = OnOffDialog.newInstance();
                frg1.show(getFragmentManager(), "dialog");
                break;
            case R.id.action_set_usage:
                DialogFragment frg2 = SetUsageDialog.newInstance();
                frg2.show(getFragmentManager(), "dialog");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
        outState.putLong(Constants.SIM1RX, (Long) dataMap.get(Constants.SIM1RX));
        outState.putLong(Constants.SIM2RX, (Long) dataMap.get(Constants.SIM2RX));
        outState.putLong(Constants.SIM3RX, (Long) dataMap.get(Constants.SIM3RX));
        outState.putLong(Constants.SIM1TX, (Long) dataMap.get(Constants.SIM1TX));
        outState.putLong(Constants.SIM2TX, (Long) dataMap.get(Constants.SIM2TX));
        outState.putLong(Constants.SIM3TX, (Long) dataMap.get(Constants.SIM3TX));
        outState.putLong(Constants.TOTAL1, (Long) dataMap.get(Constants.TOTAL1));
        outState.putLong(Constants.TOTAL2, (Long) dataMap.get(Constants.TOTAL2));
        outState.putLong(Constants.TOTAL3, (Long) dataMap.get(Constants.TOTAL3));
        outState.putString(Constants.SIM_ACTIVE, SIM.getText().toString());
    }

    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
            RX1.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.SIM1RX)));
            TX1.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.SIM1TX)));
            RX2.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.SIM2RX)));
            TX2.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.SIM2TX)));
            RX3.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.SIM3RX)));
            TX3.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.SIM3TX)));
        }
        TOT1.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.TOTAL1)));
        TOT2.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.TOTAL2)));
        TOT3.setText(DataFormat.formatData(getAppContext(), savedInstanceState.getLong(Constants.TOTAL3)));
        SIM.setText(savedInstanceState.getString(Constants.SIM_ACTIVE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
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
            //SharedPreferences.Editor edit = prefs.edit();
            if (!sharedPreferences.getBoolean(key, false)) {
                stopService(new Intent(this, WatchDogService.class));
                prefs.edit().putBoolean(Constants.PREF_OTHER[6], true).apply();
            } else {
                startService(new Intent(this, WatchDogService.class));
                prefs.edit().putBoolean(Constants.PREF_OTHER[6], false).apply();
            }
            //edit.apply();
        }
        if (key.equals(Constants.PREF_OTHER[5]))
            needsRestart = true;
    }

    @Override
    public void onClick(View v) {

        DateTime dt;
        if (!dataMap.get(Constants.LAST_DATE).equals("")) {
            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
            dt = fmt.parseDateTime((String) dataMap.get(Constants.LAST_DATE));
        } else
            dt = new DateTime();
        switch (v.getId()) {
            case (R.id.buttonClear1):
                if (isMyServiceRunning(CountService.class)) {
                    Intent clear1Intent = new Intent(Constants.CLEAR1);
                    sendBroadcast(clear1Intent);
                } else {
                    dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
                    dataMap.put(Constants.SIM1RX, (long) 0);
                    dataMap.put(Constants.SIM1TX, (long) 0);
                    dataMap.put(Constants.TOTAL1, (long) 0);
                    if (DateCompare.isNextDayOrMonth(dt, "0") && !TrafficDatabase.isEmpty(mDatabaseHelper))
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                    else
                        TrafficDatabase.read_writeTrafficData(Constants.WRITE, dataMap, mDatabaseHelper);
                    if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        RX1.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM1RX)));
                        TX1.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM1TX)));
                    }
                    TOT1.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL1)));
                }
                break;
            case (R.id.buttonClear2):
                if (isMyServiceRunning(CountService.class)) {
                    Intent clear2Intent = new Intent(Constants.CLEAR2);
                    sendBroadcast(clear2Intent);
                } else {
                    dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
                    dataMap.put(Constants.SIM2RX, (long) 0);
                    dataMap.put(Constants.SIM2TX, (long) 0);
                    dataMap.put(Constants.TOTAL2, (long) 0);
                    if (DateCompare.isNextDayOrMonth(dt, "0") && !TrafficDatabase.isEmpty(mDatabaseHelper))
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                    else
                        TrafficDatabase.read_writeTrafficData(Constants.WRITE, dataMap, mDatabaseHelper);
                    if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        RX2.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM2RX)));
                        TX2.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM2TX)));
                    }
                    TOT2.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL2)));
                }
                break;
            case (R.id.buttonClear3):
                if (isMyServiceRunning(CountService.class)) {
                    Intent clear2Intent = new Intent(Constants.CLEAR3);
                    sendBroadcast(clear2Intent);
                } else {
                    dataMap = TrafficDatabase.read_writeTrafficData(Constants.READ, dataMap, mDatabaseHelper);
                    dataMap.put(Constants.SIM3RX, (long) 0);
                    dataMap.put(Constants.SIM3TX, (long) 0);
                    dataMap.put(Constants.TOTAL3, (long) 0);
                    if (DateCompare.isNextDayOrMonth(dt, "0") && !TrafficDatabase.isEmpty(mDatabaseHelper))
                        TrafficDatabase.read_writeTrafficData(Constants.UPDATE, dataMap, mDatabaseHelper);
                    else
                        TrafficDatabase.read_writeTrafficData(Constants.WRITE, dataMap, mDatabaseHelper);
                    if (prefs.getBoolean(Constants.PREF_OTHER[7], true)) {
                        RX3.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM3RX)));
                        TX3.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.SIM3TX)));
                    }
                    TOT3.setText(DataFormat.formatData(getAppContext(), (long) dataMap.get(Constants.TOTAL3)));
                }
                break;
        }
    }
}
