package ua.od.acros.dualsimtrafficcounter.activities;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.BlackListAdapter;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class BlackListActivity extends Activity {

    private ArrayList<String> mList;
    private BlackListAdapter mArrayAdapter, mSavedAdapter;
    private Context mContext = this;
    private int mKey;
    private MyDatabase mDatabaseHelper;
    private String[] mOperatorNames = new String[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_black_list);

        ListView listView = (ListView) findViewById(R.id.listView);

        mOperatorNames[0] = MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        mOperatorNames[1] = MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        mOperatorNames[2] = MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        mDatabaseHelper = MyDatabase.getInstance(mContext);
        mKey = Integer.valueOf(getIntent().getDataString());
        mList = MyDatabase.readBlackList(mKey, mDatabaseHelper);
        setTitle(mOperatorNames[mKey]);
        mArrayAdapter = new BlackListAdapter(mContext, mList);
        listView.setAdapter(mArrayAdapter);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.save) {
                new SaveTask().execute();
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
            MyDatabase.writeBlackList(mKey, mList, mDatabaseHelper);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Toast.makeText(mContext, R.string.saved, Toast.LENGTH_LONG).show();
        }
    }
}