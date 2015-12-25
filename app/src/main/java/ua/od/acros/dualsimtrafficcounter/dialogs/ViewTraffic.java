package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ua.od.acros.dualsimtrafficcounter.CountService;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;

public class ViewTraffic extends Activity {

    private TextView RX, TX, RXN, TXN, day, night, TOT, TOTN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_traffic);

        SharedPreferences prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);

        RX = (TextView) findViewById(R.id.rx);
        TX = (TextView) findViewById(R.id.tx);
        RXN = (TextView) findViewById(R.id.rxnight);
        TXN = (TextView) findViewById(R.id.txnight);
        TOT = (TextView) findViewById(R.id.total);
        TOTN = (TextView) findViewById(R.id.totalnight);
        day = (TextView) findViewById(R.id.day);
        night = (TextView) findViewById(R.id.night);

        Button bOK = (Button) findViewById(R.id.buttonOK);
        bOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Bundle bundle = getIntent().getBundleExtra("data");
        int sim = getIntent().getIntExtra("sim", Constants.DISABLED);

        RXN.setVisibility(View.GONE);
        TXN.setVisibility(View.GONE);
        TOTN.setVisibility(View.GONE);
        night.setVisibility(View.GONE);

        String[] prefsConst = new String[0];
        switch (sim) {
            case Constants.SIM1:
                prefsConst = Constants.PREF_SIM1;
                break;
            case Constants.SIM2:
                prefsConst = Constants.PREF_SIM2;
                break;
            case Constants.SIM3:
                prefsConst = Constants.PREF_SIM3;
                break;
        }

        String opName = CountService.getName(prefsConst[5], prefsConst[6], sim);

        day.setText(opName);
        night.setText(opName + getResources().getString(R.string.night));

        if (bundle != null) {
            RX.setText(DataFormat.formatData(this, bundle.getLong("rx")));
            TX.setText(DataFormat.formatData(this, bundle.getLong("tx")));
            TOT.setText(DataFormat.formatData(this, bundle.getLong("tot")));

            if (prefs.getBoolean(prefsConst[17], false)) {
                RXN.setVisibility(View.VISIBLE);
                TXN.setVisibility(View.VISIBLE);
                TOTN.setVisibility(View.VISIBLE);
                night.setVisibility(View.VISIBLE);
                RXN.setText(DataFormat.formatData(this, bundle.getLong("rx_n")));
                TXN.setText(DataFormat.formatData(this, bundle.getLong("tx_n")));
                TOTN.setText(DataFormat.formatData(this, bundle.getLong("tot_n")));
            }
        }
    }
}
