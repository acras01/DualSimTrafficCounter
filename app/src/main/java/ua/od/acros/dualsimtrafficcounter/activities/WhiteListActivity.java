package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.ListItem;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.WhiteListAdapter;

public class WhiteListActivity extends AppCompatActivity {


    private Context mContext = this;
    private int mKey;
    private CustomDatabaseHelper mDbHelper;
    private ProgressBar pb;
    private WhiteListAdapter mAdapter;

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
        mContext = CustomApplication.getAppContext();
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
            bar.setTitle(getString(R.string.white_list));
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            List<ListItem> whiteList = new ArrayList<>();
            mAdapter = new WhiteListAdapter(whiteList);
            recyclerView.setAdapter(mAdapter);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            new LoadContactsTask(recyclerView).execute();
        }
    }

    private List<ListItem> loadContactsFromDB(Context context, ArrayList<String> list) {
        List<ListItem> whiteList = new ArrayList<>();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(uri, new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("[\\s\\-()]", "");
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                whiteList.add(new ListItem(name, number, list.contains(number)));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return whiteList;
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
            CustomDatabaseHelper.writeWhiteList(mKey, mAdapter.getCheckedItems(), mDbHelper);
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
            ArrayList<String> whiteList= CustomDatabaseHelper.readWhiteList(mKey, mDbHelper);
            List<ListItem> listItems = loadContactsFromDB(mContext, whiteList);
            List<String> numbers = new ArrayList<>();
            for (ListItem item : listItems)
                numbers.add(item.getNumber());
            for (Iterator<String> i = whiteList.iterator(); i.hasNext(); ) {
                if (numbers.contains(i.next())) {
                    i.remove();
                }
            }
            for (Iterator<String> i = whiteList.iterator(); i.hasNext(); ) {
                listItems.add(new ListItem(getString(R.string.unknown), i.next(), true));
                i.remove();
            }
            return listItems;
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
