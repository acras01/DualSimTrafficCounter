package ua.od.acros.dualsimtrafficcounter.activities;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.MyArrayAdapter;

public class WhiteListActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private ListView listView;
    private MyArrayAdapter mArrayAdapter;
    private List<String> mNames, mNumbers, mList;
    private Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_list);

        listView = (ListView) findViewById(R.id.listView);

        switch (getIntent().getDataString()) {
            case "sim1":
                mList = new ArrayList<>();
                setTitle("SIM1");
                break;
            case "sim2":
                mList = new ArrayList<>();
                setTitle("SIM2");
                break;
            case "sim3":
                mList = new ArrayList<>();
                setTitle("SIM3");
                break;
        }
        mNumbers = new ArrayList<>();
        mNames = new ArrayList<>();
        loadContactsFromDB(mContext);
        mArrayAdapter = new MyArrayAdapter(mContext, mNames, mNumbers, mList);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemSelectedListener(this);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String result = "";
        List<String> resultList = mArrayAdapter.getCheckedItems();
        for (int i = 0; i < resultList.size(); i++) {
            result += String.valueOf(resultList.get(i)) + "\n";
        }
        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
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
                mNumbers.add(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
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
}
