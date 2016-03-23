package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.OnOffTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class OnOffDialog extends DialogFragment {

    private int mSimChecked = Constants.NULL;
    private AppCompatButton bOK;
    private Context mContext;


    public static OnOffDialog newInstance() {
        return new OnOffDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mContext = CustomApplication.getAppContext();
        String[] operatorNames = new String[] {MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};

        View view = View.inflate(getActivity(), R.layout.onoff_dialog, null);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        AppCompatRadioButton sim1rb = (AppCompatRadioButton) view.findViewById(R.id.sim1RB);
        sim1rb.setText(operatorNames[0]);
        AppCompatRadioButton sim2rb = (AppCompatRadioButton) view.findViewById(R.id.sim2RB);
        sim2rb.setText(operatorNames[1]);
        AppCompatRadioButton sim3rb = (AppCompatRadioButton) view.findViewById(R.id.sim3RB);
        sim3rb.setText(operatorNames[2]);
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && CustomApplication.isOldMtkDevice()) {
            int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                    : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
            if (simQuantity == 1) {
                sim2rb.setEnabled(false);
                sim3rb.setEnabled(false);
            }
            if (simQuantity == 2)
                sim3rb.setEnabled(false);
        } else {
            sim1rb.setEnabled(false);
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        final ColorStateList[] textColor = {ColorStateList.valueOf(getResources().getColor(R.color.colorAccent))};
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                bOK.setEnabled(true);
                bOK.setTextColor(textColor[0]);
                switch (checkedId) {
                    case R.id.sim1RB:
                        mSimChecked = Constants.SIM1;
                        break;
                    case R.id.sim2RB:
                        mSimChecked = Constants.SIM2;
                        break;
                    case R.id.sim3RB:
                        mSimChecked = Constants.SIM3;
                        break;
                    case R.id.offRB:
                        mSimChecked = Constants.DISABLED;
                        break;
                }
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.choose_sim)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                bOK = (AppCompatButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                textColor[0] = bOK.getTextColors();
                bOK.setEnabled(false);
                bOK.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                bOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mSimChecked != Constants.NULL) {
                            dialog.dismiss();
                            EventBus.getDefault().post(new OnOffTrafficEvent(mSimChecked));
                        } else
                            Toast.makeText(mContext, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        return dialog;
    }
}
