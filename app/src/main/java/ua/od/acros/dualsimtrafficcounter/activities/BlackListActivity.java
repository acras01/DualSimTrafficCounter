package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Iterator;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.BlackListAdapter;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabaseHelper;

public class BlackListActivity extends AppCompatActivity {

    private ArrayList<String> mList;
    private Context mContext;
    private int mKey;
    private MyDatabaseHelper mDatabaseHelper;
    private BlackListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (savedInstanceState == null) {
            if (prefs.getBoolean(Constants.PREF_OTHER[29], true))
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            else {
                if (prefs.getBoolean(Constants.PREF_OTHER[28], false))
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            // Now recreate for it to take effect
            recreate();
        }
        mContext = getApplicationContext();
        mDatabaseHelper = MyDatabaseHelper.getInstance(mContext);
        mKey = Integer.valueOf(getIntent().getDataString());
        mList = MyDatabaseHelper.readBlackList(mKey, mDatabaseHelper);
        mAdapter = new BlackListAdapter(mList);

        String[] mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};

        setContentView(R.layout.activity_recyclerview);

        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDefaultDisplayHomeAsUpEnabled(true);
            bar.setTitle(mOperatorNames[mKey] + ": " + getString(R.string.black_list));
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
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

    class SaveTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            ArrayList<String> blackList = mAdapter.getCheckedItems();
            for (Iterator<String> i = mList.iterator(); i.hasNext(); ) {
                if (blackList.contains(i.next())) {
                    i.remove();
                }
            }
            MyDatabaseHelper.writeBlackList(mKey, mList, mDatabaseHelper);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Toast.makeText(mContext, R.string.saved, Toast.LENGTH_LONG).show();
        }
    }
}