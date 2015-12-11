package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;

public class ChooseAction extends Activity  implements View.OnClickListener {
    private String action = "";
    private SharedPreferences prefs;
    private int simid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        setContentView(R.layout.action_dialog);
        RadioButton change = (RadioButton)findViewById(R.id.actionchange);
         if (!prefs.getBoolean(Constants.PREF_OTHER[10], true) || MobileDataControl.isMultiSim(getApplicationContext()) > 1)
             change.setEnabled(true);
        final Intent intent = getIntent();
        simid = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
        final Button bOK = (Button)findViewById(R.id.buttonOK);
        bOK.setEnabled(false);
        Button bCancel = (Button)findViewById(R.id.buttonCancel);
        bOK.setOnClickListener(this);
        bCancel.setOnClickListener(this);
        RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.actionsettings:
                        action = Constants.LIMIT_ACTION;
                        break;
                    case R.id.actionchange:
                        action = Constants.CHOOSE_ACTION;
                        break;
                    case R.id.actioncontinue:
                        action = Constants.CONTINUE_ACTION;
                        break;
                }
                bOK.setEnabled(true);
            }
        });
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(Constants.ACTION);
        switch (v.getId()) {
            case R.id.buttonOK:
                intent.putExtra(Constants.SIM_ACTIVE, simid);
                intent.putExtra(Constants.ACTION, action);
                break;
            case R.id.buttonCancel:
                intent.putExtra(Constants.ACTION, Constants.OFF_ACTION);
                break;
        }
        sendBroadcast(intent);
        finish();
    }
}
