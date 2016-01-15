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

import com.stericson.RootTools.RootTools;

import ua.od.acros.dualsimtrafficcounter.CountService;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MTKUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ChooseAction extends Activity implements View.OnClickListener {

    private String action = "";
    private int simid;
    private static boolean shown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shown = true;
        SharedPreferences prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        setContentView(R.layout.action_dialog);
        RadioButton change = (RadioButton)findViewById(R.id.actionchange);
        int simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(this)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 && !RootTools.isAccessGiven()) ||
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && !MTKUtils.isMtkDevice()) ||
                android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.LOLLIPOP)
            change.setEnabled(false);
        if (prefs.getBoolean(Constants.PREF_OTHER[10], true) || simNumber == 1)
            change.setEnabled(false);
        simid = getIntent().getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
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
                    case R.id.actionmobiledata:
                        action = Constants.SETTINGS_ACTION;
                        break;
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
        CountService.setActionChoosed(true);
        sendBroadcast(intent);
        finish();
    }

    public static boolean isShown() {
        return shown;
    }

    @Override
    protected void onResume() {
        super.onResume();
        shown = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        shown = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shown = false;
    }
}
