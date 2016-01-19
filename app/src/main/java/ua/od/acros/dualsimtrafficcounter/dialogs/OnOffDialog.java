package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class OnOffDialog extends DialogFragment {

    private int mSimChecked = Constants.NULL;
    private Button bOK;

    public static OnOffDialog newInstance() {
        return new OnOffDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.onoff_dialog, null);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (android.os.Build.VERSION.SDK_INT != android.os.Build.VERSION_CODES.LOLLIPOP) {
            int simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getActivity())
                    : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
            if (simNumber == 1) {
                view.findViewById(R.id.sim2RB).setEnabled(false);
                view.findViewById(R.id.sim3RB).setEnabled(false);
            }
            if (simNumber == 2)
                view.findViewById(R.id.sim3RB).setEnabled(false);
        } else {
            view.findViewById(R.id.sim1RB).setEnabled(false);
            view.findViewById(R.id.sim2RB).setEnabled(false);
            view.findViewById(R.id.sim3RB).setEnabled(false);
        }
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
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
                bOK = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                bOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mSimChecked != Constants.NULL) {
                            dialog.dismiss();
                            Intent intent = new Intent(Constants.ON_OFF);
                            intent.putExtra("sim", mSimChecked);
                            getActivity().sendBroadcast(intent);
                        } else
                            Toast.makeText(getActivity(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        return dialog;
    }
}
