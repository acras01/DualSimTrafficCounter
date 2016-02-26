package ua.od.acros.dualsimtrafficcounter.activities;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.WhiteListAdapter;

public class WhiteListActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private WhiteListAdapter mArrayAdapter;
    private ArrayList<String> mNames, mNumbers, mList;
    private Context mContext = this;
    private int mKey;
    private MyDatabaseHelper mDatabaseHelper;
    private String[] mOperatorNames = new String[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_list);

        ListView listView = (ListView) findViewById(R.id.listView);

        mContext = getApplicationContext();
        mDatabaseHelper = MyDatabaseHelper.getInstance(mContext);
        mKey = Integer.valueOf(getIntent().getDataString());
        mList = MyDatabaseHelper.readWhiteList(mKey, mDatabaseHelper);

        String[] mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        setTitle(mOperatorNames[mKey]);

        mNumbers = new ArrayList<>();
        mNames = new ArrayList<>();
        loadContactsFromDB(mContext);
        ArrayList<String> extra = MyDatabaseHelper.readWhiteList(mKey, mDatabaseHelper);;
        for (Iterator<String> i = extra.iterator(); i.hasNext(); ) {
            if (mNumbers.contains(i.next())) {
                i.remove();
            }
        }
        for (Iterator<String> i = extra.iterator(); i.hasNext(); ) {
            mNumbers.add(i.next());
            mNames.add(getString(R.string.unknown));
            i.remove();
        }
        mArrayAdapter = new WhiteListAdapter(mContext, mNames, mNumbers, mList);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemSelectedListener(this);
        listView.setOnItemClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("list", mArrayAdapter.getCheckedItems());
    }

    @Override
    public void onClick(View v) {
        String result = "";
        List<String> resultList = mArrayAdapter.getCheckedItems();
        for (int i = 0; i < resultList.size(); i++) {
            result += String.valueOf(resultList.get(i)) + "\n";
        }
        Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mArrayAdapter.toggleChecked(position);
    }

    private void loadContactsFromDB(Context context) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(uri, new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                mNumbers.add(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("[\\s\\-()]", ""));
                mNames.add(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                cursor.moveToNext();
            }
            cursor.close();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mArrayAdapter.toggleChecked(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
            MyDatabaseHelper.writeWhiteList(mKey, mList, mDatabaseHelper);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Toast.makeText(mContext, R.string.saved, Toast.LENGTH_LONG).show();
        }
    }
}
