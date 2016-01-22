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

    private String mAction = "";
    private int mSimID;
    private static boolean mIsShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsShown = true;
        SharedPreferences prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        setContentView(R.layout.action_dialog);
        RadioButton change = (RadioButton)findViewById(R.id.actionchange);
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(this)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if ((android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP && !RootTools.isAccessGiven()) ||
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && !MTKUtils.isMtkDevice()) ||
                android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.LOLLIPOP)
            change.setEnabled(false);
        if (prefs.getBoolean(Constants.PREF_OTHER[10], true) || simQuantity == 1)
            change.setEnabled(false);
        mSimID = getIntent().getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
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
                        mAction = Constants.SETTINGS_ACTION;
                        break;
                    case R.id.actionsettings:
                        mAction = Constants.LIMIT_ACTION;
                        break;
                    case R.id.actionchange:
                        mAction = Constants.CHANGE_ACTION;
                        break;
                    case R.id.actioncontinue:
                        mAction = Constants.CONTINUE_ACTION;
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
                intent.putExtra(Constants.SIM_ACTIVE, mSimID);
                intent.putExtra(Constants.ACTION, mAction);
                break;
            case R.id.buttonCancel:
                intent.putExtra(Constants.ACTION, Constants.OFF_ACTION);
                break;
        }
        CountService.setIsActionChosen(true);
        sendBroadcast(intent);
        finish();
    }

    public static boolean isShown() {
        return mIsShown;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsShown = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsShown = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsShown = false;
    }
}
