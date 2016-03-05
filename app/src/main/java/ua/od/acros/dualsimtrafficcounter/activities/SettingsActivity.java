package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.settings.CallsLimitFragment;
import ua.od.acros.dualsimtrafficcounter.settings.OperatorFragment;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsFragment;
import ua.od.acros.dualsimtrafficcounter.settings.TrafficLimitFragment;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private SharedPreferences mPrefs;
    private ActionBar mActionBar;
    private String mTag;
    private PreferenceFragmentCompat mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
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
        setContentView(R.layout.activity_settings);
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null)
            mActionBar.setDisplayHomeAsUpEnabled(true);
        if (getIntent().getStringExtra("show") != null)
            switch (getIntent().getStringExtra("show")) {
                case Constants.TRAFFIC_TAG:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new TrafficLimitFragment())
                        .commit();
                    break;
                case Constants.CALLS_TAG:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, new CallsLimitFragment())
                            .commit();
                    break;
                default:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, new SettingsFragment())
                            .commit();
                    break;
            }
        else
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (mTag != null && mTag.contains("sim")) {
            mTag = "";
            if (mFragment instanceof TrafficLimitFragment)
                replaceFragments(TrafficLimitFragment.class);
            else if (mFragment instanceof CallsLimitFragment)
                replaceFragments(CallsLimitFragment.class);
            else if (mFragment instanceof OperatorFragment)
                replaceFragments(OperatorFragment.class);
        } else if (fragment instanceof SettingsFragment)
            finish();
        else
            replaceFragments(SettingsFragment.class);
    }

    public void replaceFragments(Class fragmentClass) {
        invalidateOptionsMenu();
        if (mActionBar != null)
            mActionBar.setDisplayShowCustomEnabled(false);
        Fragment fragment = null;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        mTag = preferenceScreen.getKey();
        mFragment = preferenceFragmentCompat;
        preferenceFragmentCompat.setPreferenceScreen(preferenceScreen);
        return true;
    }
}