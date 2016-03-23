package ua.od.acros.dualsimtrafficcounter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.fragments.CallsFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.SetCallsDurationFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.SetTrafficUsageFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TestFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TrafficForDateFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TrafficFragment;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.services.WatchDogService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener, TrafficFragment.OnFragmentInteractionListener,
        TrafficForDateFragment.OnFragmentInteractionListener, TestFragment.OnFragmentInteractionListener,
        SetTrafficUsageFragment.OnFragmentInteractionListener, CallsFragment.OnFragmentInteractionListener,
        SetCallsDurationFragment.OnFragmentInteractionListener {

    private static Context mContext;
    private static SharedPreferences mPrefs;
    private static final String FIRST_RUN = "first_run";
    private static final String ANDROID_5_0 = "API21";
    private static final String EMAIL = "email";
    private static final String MTK = "mtk";
    private static final String XPOSED = "de.robv.android.xposed.installer";
    private android.support.v4.app.Fragment mTrafficForDate, mTraffic, mTest, mSetUsage, mCalls, mSetDuration;
    private boolean mNeedsRestart = false;
    private MenuItem mCallsItem;
    private NavigationView mNavigationView;

    /*static {
        SharedPreferences prefs = MyApplication.getAppContext().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.PREF_OTHER[29], true))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        else {
            if (prefs.getBoolean(Constants.PREF_OTHER[28], false))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = CustomApplication.getAppContext();
        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        if (savedInstanceState == null) {
            if (mPrefs.getBoolean(Constants.PREF_OTHER[29], true))
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            else {
                if (mPrefs.getBoolean(Constants.PREF_OTHER[28], false))
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            // Now recreate for it to take effect
            recreate();
        }

        setContentView(R.layout.activity_main);

        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        //Prepare Navigation View Menu
        MenuItem mTestItem = mNavigationView.getMenu().findItem(R.id.nav_test);
        if (CustomApplication.isOldMtkDevice() &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mTestItem.setVisible(true);
            mTestItem.setEnabled(true);
        } else {
            mTestItem.setVisible(false);
            mTestItem.setEnabled(false);
        }

        mCallsItem = mNavigationView.getMenu().findItem(R.id.nav_calls_menu);
        if (mPrefs.getBoolean(Constants.PREF_OTHER[25], false)) {
            mCallsItem.setVisible(true);
            mCallsItem.setEnabled(true);
        } else {
            mCallsItem.setVisible(false);
            mCallsItem.setEnabled(false);
        }

        //set Version in Navigation View Header
        //View headerLayout = mNavigationView.findViewById(R.id.headerLayout);
        View headerLayout = mNavigationView.getHeaderView(0);
        TextView versionView = (TextView) headerLayout.findViewById(R.id.versioninfo);
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            version = getResources().getString(R.string.not_available);
        }
        versionView.setText(String.format(getResources().getString(R.string.app_version), version));

        mTraffic = new TrafficFragment();
        mTrafficForDate = new TrafficForDateFragment();
        mTest = new TestFragment();
        mSetUsage = new SetTrafficUsageFragment();
        mSetDuration = new SetCallsDurationFragment();
        mCalls = new CallsFragment();

        MobileUtils.getTelephonyManagerMethods(mContext);

        if (!CustomApplication.isMyServiceRunning(WatchDogService.class, mContext) && mPrefs.getBoolean(Constants.PREF_OTHER[4], true))
            startService(new Intent(mContext, WatchDogService.class));
        if (!CustomApplication.isMyServiceRunning(TrafficCountService.class, mContext) && !mPrefs.getBoolean(Constants.PREF_OTHER[5], false))
            startService(new Intent(mContext, TrafficCountService.class));
        if (!CustomApplication.isPackageExisted(mContext, XPOSED)) {
            mPrefs.edit()
                    .putBoolean(Constants.PREF_OTHER[24], true)
                    .putBoolean(Constants.PREF_OTHER[25], false)
                    .apply();
        }
        if (!CustomApplication.isMyServiceRunning(CallLoggerService.class, mContext) && !mPrefs.getBoolean(Constants.PREF_OTHER[24], true))
            startService(new Intent(mContext, CallLoggerService.class));

        String action = getIntent().getAction();

        if (mPrefs.getBoolean(Constants.PREF_OTHER[9], true)) {
            mPrefs.edit()
                    .putBoolean(Constants.PREF_OTHER[9], false)
                    .apply();
            showDialog(FIRST_RUN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    !CustomApplication.hasRoot())
                showDialog(ANDROID_5_0);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 &&
                    !CustomApplication.isOldMtkDevice())
                showDialog(MTK);
            if (CustomApplication.isOldMtkDevice() &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                if (savedInstanceState == null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.content_frame, mTest)
                            .commit();
                    mNavigationView.setCheckedItem(R.id.nav_test);
                }
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.content_frame, mTraffic)
                        .addToBackStack(Constants.TRAFFIC_TAG)
                        .commit();
                mNavigationView.setCheckedItem(R.id.nav_traffic);
            }
        } else if (action != null && action.equals("tap") && savedInstanceState == null) {
            if (mPrefs.getBoolean(Constants.PREF_OTHER[26], true)) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, mTraffic)
                        .addToBackStack(Constants.TRAFFIC_TAG)
                        .commit();
                mNavigationView.setCheckedItem(R.id.nav_traffic);
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, mCalls)
                        .addToBackStack(Constants.CALLS_TAG)
                        .commit();
                mNavigationView.setCheckedItem(R.id.nav_calls);
            }
        } else {
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.content_frame, mTraffic)
                        .addToBackStack(Constants.TRAFFIC_TAG)
                        .commit();
                mNavigationView.setCheckedItem(R.id.nav_traffic);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (fragment instanceof TrafficFragment)
                finish();
            else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content_frame, mTraffic)
                        .commit();
                mNavigationView.setCheckedItem(R.id.nav_traffic);
            }
        }
    }

    public void showDialog(String key) {
        switch (key) {
            case FIRST_RUN:
                new AlertDialog.Builder(this)
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
                new AlertDialog.Builder(this)
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
                new AlertDialog.Builder(this)
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
            case EMAIL:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.send_email)
                        .setMessage(R.string.why_email)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                emailIntent.setType("text/plain");
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"acras1@gmail.com"});
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DualSim Traffic Counter");
                                ArrayList<Uri> uris = new ArrayList<>();
                                File dir = new File(String.valueOf(mContext.getFilesDir()));
                                //TelephonyMethods
                                String fileName = "telephony.txt";
                                String content = getString(R.string.body) + "\n";
                                File file = new File(dir, fileName);
                                try {
                                    uris.add(Uri.fromFile(file));
                                    InputStream is = openFileInput(fileName);
                                    if (is != null) {
                                        InputStreamReader isr = new InputStreamReader(is);
                                        BufferedReader br = new BufferedReader(isr);
                                        String read;
                                        StringBuilder sb = new StringBuilder();
                                        while ((read = br.readLine()) != null ) {
                                            read += "\n";
                                            sb.append(read);
                                        }
                                        is.close();
                                        content += sb.toString();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                //Active SIM
                                int sim = MobileUtils.getMobileDataInfo(mContext, true)[1];
                                fileName = "sim_log.txt";
                                content += "\n" + "Active SIM " + sim + "\n";
                                file = new File(dir, fileName);
                                try {
                                    uris.add(Uri.fromFile(file));
                                    InputStream is = openFileInput(fileName);
                                    if (is != null) {
                                        InputStreamReader isr = new InputStreamReader(is);
                                        BufferedReader br = new BufferedReader(isr);
                                        String read;
                                        StringBuilder sb = new StringBuilder();
                                        while ((read = br.readLine()) != null ) {
                                            read += "\n";
                                            sb.append(read);
                                        }
                                        is.close();
                                        content += sb.toString();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                //Operator Names
                                ArrayList<String> names = MobileUtils.getOperatorNames(mContext);
                                fileName = "name_log.txt";
                                content += "\n" + "Operator names " + names.toString() + "\n";
                                file = new File(dir, fileName);
                                try {
                                    uris.add(Uri.fromFile(file));
                                    InputStream is = openFileInput(fileName);
                                    if (is != null) {
                                        InputStreamReader isr = new InputStreamReader(is);
                                        BufferedReader br = new BufferedReader(isr);
                                        String read;
                                        StringBuilder sb = new StringBuilder();
                                        while ((read = br.readLine()) != null ) {
                                            read += "\n";
                                            sb.append(read);
                                        }
                                        is.close();
                                        content += sb.toString();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                emailIntent.putExtra(Intent.EXTRA_TEXT, content);
                                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                                startActivity(Intent.createChooser(emailIntent, getString(R.string.choose_client)));
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
        if (!mPrefs.getBoolean(Constants.PREF_OTHER[25], true)) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (currentFragment instanceof CallsFragment) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(currentFragment)
                        .replace(R.id.content_frame, mTraffic)
                        .commit();
                mNavigationView.setCheckedItem(R.id.nav_traffic);
            }
        }
        if (mNeedsRestart && mTraffic.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .detach(mTraffic)
                    .attach(mTraffic)
                    .commit();
            mNavigationView.setCheckedItem(R.id.nav_traffic);
        }
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
        if (key.equals(Constants.PREF_OTHER[25])) {
            if (sharedPreferences.getBoolean(key, true)) {
                mCallsItem.setVisible(true);
                mCallsItem.setEnabled(true);
            } else {
                mCallsItem.setVisible(false);
                mCallsItem.setEnabled(false);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        mNavigationView.setCheckedItem(id);
        item.setChecked(true);
        Fragment newFragment = null;
        FragmentManager fm = getSupportFragmentManager();
        String tag = "";
        switch (id) {
            case R.id.nav_traffic:
                tag = Constants.TRAFFIC_TAG;
                newFragment = mTraffic;
                break;
            case R.id.nav_calls:
                tag = Constants.CALLS_TAG;
                newFragment = mCalls;
                break;
            case R.id.nav_traf_for_date:
                newFragment = mTrafficForDate;
                break;
            case R.id.nav_test:
                newFragment = mTest;
                break;
            case R.id.nav_set_usage:
                newFragment = mSetUsage;
                break;
            case R.id.nav_set_duration:
                newFragment = mSetDuration;
                break;
            case R.id.nav_settings:
                Intent intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_email:
                showDialog(EMAIL);
                break;
        }
        if (newFragment != null) {
            Fragment frg = getSupportFragmentManager().findFragmentByTag(tag);
            if (tag.equals("") || frg != null)
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

    @Override
    public void onCallsFragmentInteraction(Uri uri) {
        // Do different stuff
    }

    @Override
    public void onSetDurationFragmentInteraction(Uri uri) {
        // Do different stuff
    }
}
