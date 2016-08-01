package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.BlackListAdapter;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.ListItem;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class BlackListActivity extends AppCompatActivity {

    private Context mContext;
    private int mKey;
    private CustomDatabaseHelper mDbHelper;
    private BlackListAdapter mAdapter;
    private ArrayList<String> mList;
    private ProgressBar pb;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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
        mDbHelper = CustomDatabaseHelper.getInstance(mContext);
        mKey = Integer.valueOf(getIntent().getDataString());
        String[] mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        setContentView(R.layout.activity_recyclerview);
        pb = (ProgressBar) findViewById(R.id.progressBar);
        if (pb != null) {
            pb.setVisibility(View.GONE);
        }
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDefaultDisplayHomeAsUpEnabled(true);
            bar.setSubtitle(mOperatorNames[mKey]);
            bar.setTitle(getString(R.string.black_list));
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            List<ListItem> blackList = new ArrayList<>();
            mAdapter = new BlackListAdapter(blackList);
            recyclerView.setAdapter(mAdapter);
            new LoadContactsTask(recyclerView).execute();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.save:
                    new SaveTask().execute();
                default:
                    finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return super.onOptionsItemSelected(item);
    }

    private class SaveTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            ArrayList<String> list = mAdapter.getCheckedItems();
            for (Iterator<String> i = mList.iterator(); i.hasNext(); ) {
                if (list.contains(i.next())) {
                    i.remove();
                }
            }
            ArrayList<String> imsi = null;
            if (mPrefs.getBoolean(Constants.PREF_OTHER[45], true))
                imsi = MobileUtils.getSimIds(mContext);
            CustomDatabaseHelper.writeBlackList(mKey, mList, mDbHelper, imsi);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(mContext, R.string.saved, Toast.LENGTH_LONG).show();
        }
    }

    private class LoadContactsTask extends AsyncTask<Void, Void, List<ListItem>> {

        RecyclerView rv;

        LoadContactsTask(RecyclerView rv) {
            this.rv = rv;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<ListItem> doInBackground(Void... params) {
            ArrayList<String> imsi = null;
            if (mPrefs.getBoolean(Constants.PREF_OTHER[45], true))
                imsi = MobileUtils.getSimIds(mContext);
            mList = CustomDatabaseHelper.readBlackList(mKey, mDbHelper, imsi);
            List<ListItem> blackList = new ArrayList<>();
            for (String number : mList)
                blackList.add(new ListItem(number, false));
            return blackList;
        }

        @Override
        protected void onPostExecute(List<ListItem> result) {
            pb.setVisibility(View.GONE);
            if (result != null) {
                mAdapter.swapItems(result);
            }
        }
    }
}