package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.View;
import android.widget.RadioGroup;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.ActionTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ChooseActionDialog extends AppCompatActivity {

    private String mAction = "";
    private int mSimID;
    private static boolean mIsActive;
    private AppCompatButton bOK;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = CustomApplication.getAppContext();
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
        View view = View.inflate(this, R.layout.action_dialog, null);
        AppCompatRadioButton change = (AppCompatRadioButton) view.findViewById(R.id.actionchange);
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (((android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP && !CustomApplication.hasRoot()) ||
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP && !CustomApplication.isMtkDevice()) ||
                android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.LOLLIPOP) ||
                prefs.getBoolean(Constants.PREF_OTHER[10], true) || simQuantity == 1)
            change.setEnabled(false);
        mSimID = getIntent().getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        final ColorStateList[] textColor = new ColorStateList[] {ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorAccent))};
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                bOK.setEnabled(true);
                bOK.setTextColor(textColor[0]);
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
            }
        });
        mDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.attention)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EventBus.getDefault().post(new ActionTrafficEvent(mSimID, Constants.OFF_ACTION));
                        finish();
                    }
                })
                .create();

        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                bOK = (AppCompatButton) mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                textColor[0] = bOK.getTextColors();
                bOK.setEnabled(false);
                bOK.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                bOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EventBus.getDefault().post(new ActionTrafficEvent(mSimID, mAction));
                        finish();
                    }
                });
            }
        });
        if(!this.isFinishing()){
            mDialog.show();
        }
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
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }
}