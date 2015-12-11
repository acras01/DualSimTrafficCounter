package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;

public class ShowSimDialog extends DialogFragment {

    Button bOK;
    private SharedPreferences.Editor edit;
    static int widgetID;

    public static ShowSimDialog newInstance(int id) {
        widgetID = id;
        return new ShowSimDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences prefs = getActivity().getSharedPreferences(String.valueOf(widgetID) + "_" + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
        edit = prefs.edit();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.showsim_dialog, null);
        CheckBox sim1 = (CheckBox) view.findViewById(R.id.sim1);
        CheckBox sim2 = (CheckBox) view.findViewById(R.id.sim2);
        CheckBox sim3 = (CheckBox) view.findViewById(R.id.sim3);

        if (MobileDataControl.isMultiSim(getActivity()) == 1) {
            sim2.setEnabled(false);
            sim2.setChecked(false);
            edit.putBoolean(Constants.PREF_WIDGET[19], false).apply();
            sim3.setEnabled(false);
            sim3.setChecked(false);
            edit.putBoolean(Constants.PREF_WIDGET[20], false).apply();
        }
        if (MobileDataControl.isMultiSim(getActivity()) == 2) {
            sim3.setEnabled(false);
            sim3.setChecked(false);
            edit.putBoolean(Constants.PREF_WIDGET[20], false).apply();
        }
        CompoundButton.OnCheckedChangeListener simCheck= new CompoundButton.OnCheckedChangeListener() {
            /**
             * Called when the checked state of a compound button has changed.
             *
             * @param buttonView The compound button view whose state has changed.
             * @param isChecked  The new checked state of buttonView.
             */
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.sim1:
                        edit.putBoolean(Constants.PREF_WIDGET[18], isChecked);
                        break;
                    case R.id.sim2:
                        edit.putBoolean(Constants.PREF_WIDGET[19], isChecked);
                        break;
                    case R.id.sim3:
                        edit.putBoolean(Constants.PREF_WIDGET[20], isChecked);
                        break;
                }
            }
        };

        sim1.setOnCheckedChangeListener(simCheck);
        sim2.setOnCheckedChangeListener(simCheck);
        sim3.setOnCheckedChangeListener(simCheck);

        sim1.setChecked(prefs.getBoolean(Constants.PREF_WIDGET[18], true));
        sim2.setChecked(prefs.getBoolean(Constants.PREF_WIDGET[19], true));
        sim3.setChecked(prefs.getBoolean(Constants.PREF_WIDGET[20], true));

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
                        edit.apply();
                        dismiss();
                    }
                });
            }
        });
        return dialog;
    }

}
