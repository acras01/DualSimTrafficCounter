package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.Format;
import java.text.SimpleDateFormat;

import ua.od.acros.dualsimtrafficcounter.CountService;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;

public class ViewTraffic extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_traffic);

        DateTimeFormatter fmt = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
        DateTime date = fmt.parseDateTime(getIntent().getStringExtra(Constants.LAST_DATE));

        Format dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        String pattern = ((SimpleDateFormat) dateFormat).toLocalizedPattern();

        setTitle(String.format(getResources().getString(R.string.traffic_data), new SimpleDateFormat(pattern).format(date.toDate())));

        SharedPreferences prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);

        TextView RX = (TextView) findViewById(R.id.rx);
        TextView TX = (TextView) findViewById(R.id.tx);
        TextView RXN = (TextView) findViewById(R.id.rxnight);
        TextView TXN = (TextView) findViewById(R.id.txnight);
        TextView TOT = (TextView) findViewById(R.id.total);
        TextView TOTN = (TextView) findViewById(R.id.totalnight);
        TextView day = (TextView) findViewById(R.id.day);
        TextView night = (TextView) findViewById(R.id.night);

        findViewById(R.id.buttonOK).setOnClickListener(this);
        //findViewById(R.id.choosedate).setOnClickListener(this);

        Bundle bundle = getIntent().getBundleExtra(Constants.SET_USAGE);
        int sim = getIntent().getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);

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

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonOK:
                finish();
                break;
            /*case R.id.choosedate:
                DialogFragment frg = ShowTrafficForDateDialog.newInstance();
                frg.show(getFragmentManager(), "dialog");
                finish();
                break;*/
        }
    }
}
