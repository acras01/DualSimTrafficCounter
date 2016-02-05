package ua.od.acros.dualsimtrafficcounter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;

import ua.od.acros.dualsimtrafficcounter.R;

public class WhiteListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_list);

        Intent intent = getIntent();
        String key = intent.getStringExtra("key");
    }

}
