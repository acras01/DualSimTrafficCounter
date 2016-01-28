package ua.od.acros.dualsimtrafficcounter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.stericson.RootTools.RootTools;

import ua.od.acros.dualsimtrafficcounter.fragments.SetUsageFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TestFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TrafficForDateFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TrafficFragment;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.CheckServiceRunning;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MTKUtils;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener, TrafficFragment.OnFragmentInteractionListener,
        TrafficForDateFragment.OnFragmentInteractionListener, TestFragment.OnFragmentInteractionListener,
        SetUsageFragment.OnFragmentInteractionListener{

    private static final String TRAFFIC_TAG = "traffic";
    private static Context mContext;
    private SharedPreferences mPrefs;
    private static final String FIRST_RUN = "first_run";
    private static final String ANDROID_5_0 = "API21";
    private static final String MTK = "mtk";
    private android.support.v4.app.Fragment mTrafficForDate, mTraffic, mTest, mSet;
    private boolean mNeedsRestart = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = MainActivity.this;
        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        setContentView(R.layout.activity_main);

        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        MenuItem mTestItem = navigationView.getMenu().findItem(R.id.nav_test);
        if (MTKUtils.isMtkDevice() && MTKUtils.hasGeminiSupport() &&
                android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            mTestItem.setVisible(true);
            mTestItem.setEnabled(true);
        } else {
            mTestItem.setVisible(false);
            mTestItem.setEnabled(false);
        }

        mTraffic = new TrafficFragment();
        mTrafficForDate = new TrafficForDateFragment();
        mTest = new TestFragment();
        mSet = new SetUsageFragment();

        if (!CheckServiceRunning.isMyServiceRunning(WatchDogService.class, mContext) && mPrefs.getBoolean(Constants.PREF_OTHER[4], true))
            mContext.startService(new Intent(mContext, WatchDogService.class));
        if (!CheckServiceRunning.isMyServiceRunning(CountService.class, mContext) && !mPrefs.getBoolean(Constants.PREF_OTHER[5], false))
            mContext.startService(new Intent(mContext, CountService.class));
        //if (!CheckServiceRunning.isMyServiceRunning(CallLoggerService.class, mContext))
        //    startService(new Intent(mContext, CallLoggerService.class));

        if (mPrefs.getBoolean(Constants.PREF_OTHER[9], true)) {
            showDialog(FIRST_RUN);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP &&
                    !RootTools.isAccessGiven())
                showDialog(ANDROID_5_0);
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP_MR1 &&
                    !MTKUtils.isMtkDevice())
                showDialog(MTK);
            if (MTKUtils.isMtkDevice() && MTKUtils.hasGeminiSupport() &&
                    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP)
                if (savedInstanceState == null)
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.content_frame, mTest)
                            .commit();
            mPrefs.edit().putBoolean(Constants.PREF_OTHER[9], false).apply();
        } else {
            if (savedInstanceState == null)
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.content_frame, mTraffic)
                        .addToBackStack(TRAFFIC_TAG)
                        .commit();
        }
    }

    public static Context getAppContext() {
        return MainActivity.mContext;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!mTraffic.isVisible())
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, mTraffic)
                    .commit();
            else
                finish();

    }

    public void showDialog(String key) {
        switch (key) {
            case FIRST_RUN:
                new AlertDialog.Builder(mContext)
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
                new AlertDialog.Builder(mContext)
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
                new AlertDialog.Builder(mContext)
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNeedsRestart && mTraffic.isVisible())
            getSupportFragmentManager()
                    .beginTransaction()
                    .detach(mTraffic)
                    .attach(mTraffic)
                    .commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[4])) {
            if (!sharedPreferences.getBoolean(key, false)) {
                stopService(new Intent(mContext, WatchDogService.class));
                mPrefs.edit().putBoolean(Constants.PREF_OTHER[6], true).apply();
            } else {
                startService(new Intent(mContext, WatchDogService.class));
                mPrefs.edit().putBoolean(Constants.PREF_OTHER[6], false).apply();
            }
        }
        if (key.equals(Constants.PREF_OTHER[7]))
            mNeedsRestart = true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment newFragment = null;
        FragmentManager fm = getSupportFragmentManager();
        String tag = "";
        switch (item.getItemId()) {
            case R.id.nav_traffic:
                tag = TRAFFIC_TAG;
                newFragment = mTraffic;
                break;
            case R.id.nav_traf_for_date:
                newFragment = mTrafficForDate;
                break;
            case R.id.nav_test:
                newFragment = mTest;
                break;
            case R.id.nav_set_usage:
                newFragment = mSet;
                break;
            case R.id.nav_settings:
                Intent intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        if (newFragment != null) {
            Fragment frg = getSupportFragmentManager().findFragmentByTag(tag);
            if (!tag.equals(TRAFFIC_TAG) || frg != null)
                fm.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.content_frame, newFragment)
                        .commit();
            else
                fm.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.content_frame, newFragment)
                        .addToBackStack(tag)
                        .commit();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onTrafficFragmentInteraction(Uri uri) {
        // Do stuff
    }

    @Override
    public void onTrafficForDateFragmentInteraction(Uri uri) {
        // Do different stuff
    }

    @Override
    public void onTestFragmentInteraction(Uri uri) {
        // Do different stuff
    }

    @Override
    public void onSetUsageFragmentInteraction(Uri uri) {
        // Do different stuff
    }
}
