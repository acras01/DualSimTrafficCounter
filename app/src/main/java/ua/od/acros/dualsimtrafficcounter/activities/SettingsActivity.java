package ua.od.acros.dualsimtrafficcounter.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.AppCompatPreferenceActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MyPrefsHeaderAdapter;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private List<Header> mHeaders;
    private static Toolbar mToolBar;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        setTitle(R.string.action_settings);
        mToolBar = setToolBar();
        if (mToolBar != null) {
            mToolBar.setTitle(getTitle());
            mToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
            mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    public static Toolbar getBar() {
        return mToolBar;
    }

    private Toolbar setToolBar() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        View content = root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_settings, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        return (Toolbar) toolbarContainer.findViewById(R.id.settings_toolbar);
    }

    protected void onResume() {
        super.onResume();
        if (getListAdapter() instanceof MyPrefsHeaderAdapter)
            ((MyPrefsHeaderAdapter) getListAdapter()).resume();
        invalidateHeaders();
    }

    protected void onPause() {
        super.onPause();
        if (getListAdapter() instanceof MyPrefsHeaderAdapter)
            ((MyPrefsHeaderAdapter) getListAdapter()).pause();
    }

    public void onBuildHeaders(List<Header> target) {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[25], false))
            loadHeadersFromResource(R.xml.headers_xposed, target);
        else
            loadHeadersFromResource(R.xml.headers, target);
        mHeaders = target;
    }

    public void setListAdapter(ListAdapter adapter) {
        int i, count;
        if (mHeaders == null) {
            mHeaders = new ArrayList<>();
            count = adapter.getCount();
            for (i = 0; i < count; ++i)
                mHeaders.add((Header) adapter.getItem(i));
        }
        super.setListAdapter(new MyPrefsHeaderAdapter(this, mHeaders));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // If the user has clicked on a preference screen, set up the screen
        if (preference instanceof PreferenceScreen) {
            setUpNestedScreen((PreferenceScreen) preference);
        }

        return false;
    }

    public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();
        Toolbar toolbar = setToolBar();
        if (toolbar != null) {
            toolbar.setTitle(preferenceScreen.getTitle());
            toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
    }
}
