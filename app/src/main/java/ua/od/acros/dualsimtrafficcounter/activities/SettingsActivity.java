package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsFragment;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences mPrefs;

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
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

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
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}