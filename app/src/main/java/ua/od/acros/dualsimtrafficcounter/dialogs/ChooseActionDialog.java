package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.ActionTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyApplication;

public class ChooseActionDialog extends AppCompatActivity implements View.OnClickListener {

    private String mAction = "";
    private int mSimID;
    private static boolean mIsActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsActive = true;
        SharedPreferences prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (savedInstanceState == null) {
            if (prefs.getBoolean(Constants.PREF_OTHER[29], true))
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            else {
                if (prefs.getBoolean(Constants.PREF_OTHER[28], false))
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            // Now recreate for it to take effect
            recreate();
        }
        setContentView(R.layout.action_dialog);
        RadioButton change = (RadioButton)findViewById(R.id.actionchange);
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getApplicationContext())
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if ((android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP && !MyApplication.hasRoot()) ||
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && !MyApplication.isMtkDevice()) ||
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
        int sim = Constants.DISABLED;
        String action = "";
        switch (v.getId()) {
            case R.id.buttonOK:
                sim = mSimID;
                action = mAction;
                break;
            case R.id.buttonCancel:
                action = Constants.OFF_ACTION;
                break;
        }
        EventBus.getDefault().post(new ActionTrafficEvent(sim, action));
        finish();
    }

    public static boolean isActive() {
        return mIsActive;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsActive = false;
    }
}