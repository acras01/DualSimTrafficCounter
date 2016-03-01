package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.AppCompatPreferenceActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MyPrefsHeaderAdapter;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private List<Header> mHeaders;

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
        getLayoutInflater().inflate(R.layout.toolbar, (ViewGroup)findViewById(android.R.id.content));
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
