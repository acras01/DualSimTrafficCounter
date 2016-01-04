package ua.od.acros.dualsimtrafficcounter.settings;

import android.content.Context;
import android.preference.PreferenceActivity;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.MyPrefsHeaderAdapter;

public class SettingsActivity extends PreferenceActivity {

    private List<Header> headers;
    private static Context context;

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
        loadHeadersFromResource(R.xml.headers, target);
        setTitle(R.string.action_settings);
        headers = target;
        context = SettingsActivity.this;
    }

    public static Context getAppContext() {
        return SettingsActivity.context;
    }

    public void setListAdapter(ListAdapter adapter) {
        int i, count;
        if (headers == null) {
            headers = new ArrayList<>();
            count = adapter.getCount();
            for (i = 0; i < count; ++i)
                headers.add((Header) adapter.getItem(i));
        }
        super.setListAdapter(new MyPrefsHeaderAdapter(this, headers));
    }
}
