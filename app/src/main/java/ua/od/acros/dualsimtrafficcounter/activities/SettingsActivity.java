package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.preference.PreferenceActivity;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MyPrefsHeaderAdapter;

public class SettingsActivity extends PreferenceActivity {

    private List<Header> mHeaders;
    private static Context mContext;

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
        mContext = getApplicationContext();
        if (mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE)
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
