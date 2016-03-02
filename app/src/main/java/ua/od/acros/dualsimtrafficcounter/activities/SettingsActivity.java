package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MyPrefsHeaderAdapter;

public class SettingsActivity extends PreferenceActivity {

    private List<Header> mHeaders;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_OTHER[29], true)) {
            int currentNightMode = getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode) {
                case Configuration.UI_MODE_NIGHT_NO:
                    // Night mode is not active, we're in day time
                    setTheme(R.style.AppTheme_AppBarOverlay_Light);
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    // Night mode is active, we're at night!
                    setTheme(R.style.AppTheme_AppBarOverlay);
                    break;
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    // We don't know what mode we're in, assume notnight
                    setTheme(R.style.AppTheme_AppBarOverlay_Light);
                    break;
            }
        } else {
            if (getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE)
                    .getBoolean(Constants.PREF_OTHER[28], false))
                setTheme(R.style.AppTheme_AppBarOverlay_Light);
            else
                setTheme(R.style.AppTheme_AppBarOverlay);
        }

        super.onCreate(savedInstanceState);
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
        if (getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_OTHER[25], false))
            loadHeadersFromResource(R.xml.headers_xposed, target);
        else
            loadHeadersFromResource(R.xml.headers, target);
        setTitle(R.string.action_settings);
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