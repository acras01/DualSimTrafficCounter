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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class SetUsageDialog extends DialogFragment implements CompoundButton.OnCheckedChangeListener,
        RadioGroup.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {

    EditText txInput, rxInput;

    int mTXSpinnerSel, mRXSpinnerSel;
    int mSimChecked = Constants.DISABLED;
    private Button bSetUsageOK;
    private Spinner rxSpinner;


    public static SetUsageDialog newInstance() {
        return new SetUsageDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.usage_dialog, null);
        txInput = (EditText) view.findViewById(R.id.txamount);
        rxInput = (EditText) view.findViewById(R.id.rxamount);
        Spinner txSpinner = (Spinner) view.findViewById(R.id.spinnertx);
        rxSpinner = (Spinner) view.findViewById(R.id.spinnerrx);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        RadioButton sim1 = (RadioButton) view.findViewById(R.id.sim1RB);
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        int simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getActivity())
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (simNumber == 1) {
            sim1.setChecked(true);
            view.findViewById(R.id.sim2RB).setEnabled(false);
            view.findViewById(R.id.sim3RB).setEnabled(false);
        }
        if (simNumber == 2)
            view.findViewById(R.id.sim3RB).setEnabled(false);
        final CheckBox total = (CheckBox) view.findViewById(R.id.checktotal);
        total.setChecked(false);

        total.setOnCheckedChangeListener(this);
        radioGroup.setOnCheckedChangeListener(this);
        txSpinner.setOnItemSelectedListener(this);
        rxSpinner.setOnItemSelectedListener(this);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.action_set_usage)
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
                        Bundle bundle = new Bundle();
                        if ((mSimChecked != Constants.DISABLED && !rxInput.getText().toString().equals("") && !txInput.getText().toString().equals("")) ||
                                (mSimChecked != Constants.DISABLED && total.isChecked() && !txInput.getText().toString().equals(""))) {
                            bundle.putInt("sim", mSimChecked);
                            bundle.putString("trans", txInput.getText().toString());
                            bundle.putInt("txV", mTXSpinnerSel);
                            if (total.isChecked()) {
                                bundle.putString("rcvd", "0");
                                bundle.putInt("rxV", 0);
                            } else {
                                bundle.putString("rcvd", rxInput.getText().toString());
                                bundle.putInt("rxV", mRXSpinnerSel);
                            }
                            dialog.dismiss();
                            Intent intent = new Intent(Constants.SET_USAGE);
                            intent.putExtra("data", bundle);
                            getActivity().sendBroadcast(intent);
                        } else
                            Toast.makeText(getActivity(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        return dialog;
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked =  Constants.SIM1;
                break;
            case R.id.sim2RB:
                mSimChecked =  Constants.SIM2;
                break;
            case R.id.sim3RB:
                mSimChecked =  Constants.SIM3;
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case (R.id.spinnertx):
                mTXSpinnerSel = parent.getSelectedItemPosition();
                break;
            case (R.id.spinnerrx):
                mRXSpinnerSel = parent.getSelectedItemPosition();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        rxSpinner.setEnabled(!isChecked);
        rxInput.setEnabled(!isChecked);
        if (isChecked)
            txInput.setHint(R.string.total);
        else
            txInput.setHint(R.string.transmitted);
    }
}
