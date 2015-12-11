package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;

public class OnOffDialog extends DialogFragment {

    int chkSIM = Constants.NULL;
    Button bSetUsageOK;

    public static OnOffDialog newInstance() {
        return new OnOffDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.onoff_dialog, null);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        if (MobileDataControl.isMultiSim(getActivity()) == 1) {
            view.findViewById(R.id.sim2RB).setEnabled(false);
            view.findViewById(R.id.sim3RB).setEnabled(false);
        }
        if (MobileDataControl.isMultiSim(getActivity()) == 2)
            view.findViewById(R.id.sim3RB).setEnabled(false);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.sim1RB:
                        chkSIM = Constants.SIM1;
                        break;
                    case R.id.sim2RB:
                        chkSIM = Constants.SIM2;
                        break;
                    case R.id.sim3RB:
                        chkSIM = Constants.SIM3;
                        break;
                    case R.id.offRB:
                        chkSIM = Constants.DISABLED;
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
                bSetUsageOK = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                bSetUsageOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (chkSIM != Constants.NULL) {
                            dialog.dismiss();
                            Intent intent = new Intent(Constants.ON_OFF);
                            intent.putExtra("sim", chkSIM);
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
