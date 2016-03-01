package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.view.ActionMode;
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

public class SettingsActivity extends AppCompatPreferenceActivity implements AppCompatCallback {

    private List<Header> mHeaders;
    private static Toolbar mToolBar;
    private SharedPreferences mPrefs;

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {
        //let's leave this empty, for now
    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {
        // let's leave this empty, for now
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        /*AppCompatDelegate delegate = getDelegate();

        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (savedInstanceState == null) {
            if (mPrefs.getBoolean(Constants.PREF_OTHER[29], true))
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            else {
                if (mPrefs.getBoolean(Constants.PREF_OTHER[28], false))
                    delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else
                    delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            // Now recreate for it to take effect
            recreate();
        }*/
        setTitle(R.string.action_settings);
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        View content = root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_settings, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);
        mToolBar = (Toolbar) toolbarContainer.findViewById(R.id.settings_toolbar);
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
        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
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
}
