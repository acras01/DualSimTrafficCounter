package ua.od.acros.dualsimtrafficcounter.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import org.acra.ACRA;

import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.BlackListAdapter;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class BlackListActivity extends Activity {

    private List<String> mList;
    private Context mContext = this;
    private int mKey;
    private MyDatabase mDatabaseHelper;
    private String[] mOperatorNames = new String[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_black_list);

        ListView listView = (ListView) findViewById(R.id.listView);

        mDatabaseHelper = MyDatabase.getInstance(mContext);
        mKey = Integer.valueOf(getIntent().getDataString());
        mList = MyDatabase.readBlackList(mKey, mDatabaseHelper);
        mOperatorNames[0] = MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        mOperatorNames[1] = MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        mOperatorNames[2] = MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        setTitle(mOperatorNames[mKey]);
        BlackListAdapter myAdapter = new BlackListAdapter(mContext, mList);
        listView.setAdapter(myAdapter);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        List<String> checked = BlackListAdapter.getCheckedItems();
        if (mList.removeAll(checked))
            try {
                if (item.getItemId() == R.id.save) {
                    MyDatabase.writeBlackList(mKey, mList, mDatabaseHelper);
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        else
            finish();
        return super.onOptionsItemSelected(item);
    }
}

