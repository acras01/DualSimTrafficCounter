package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class TestDialog extends DialogFragment {

    private String mSimChecked = "";
    private Button bOK;
    private boolean mAlternative = false;
    private SharedPreferences.Editor edit;


    public static TestDialog newInstance() {
        return new TestDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.test_dialog, null);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        edit = prefs.edit();
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(getActivity())
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (simQuantity == 1) {
            view.findViewById(R.id.sim2RB).setEnabled(false);
            view.findViewById(R.id.sim3RB).setEnabled(false);
        }
        if (simQuantity == 2)
            view.findViewById(R.id.sim3RB).setEnabled(false);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.sim1RB:
                        mSimChecked = "sim1";
                        break;
                    case R.id.sim2RB:
                        mSimChecked = "sim2";
                        break;
                    case R.id.sim3RB:
                        mSimChecked = "sim3";
                        break;
                }
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        edit.putBoolean(Constants.PREF_OTHER[20], mAlternative);
                        edit.apply();
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
                        if (!mSimChecked.equals("")) {
                            int sim = Constants.DISABLED;
                            try {
                                sim = (int) Settings.System.getLong(getActivity().getContentResolver(), "gprs_connection_sim_setting");
                                edit.putInt(mSimChecked, sim);
                                mAlternative = true;
                            } catch (Settings.SettingNotFoundException e0) {
                                e0.printStackTrace();
                                try {
                                    sim = (int) Settings.System.getLong(getActivity().getContentResolver(), "gprs_connection_setting");
                                    edit.putInt(mSimChecked, sim);
                                    mAlternative = true;
                                } catch (Settings.SettingNotFoundException e1) {
                                    e1.printStackTrace();
                                }
                            }
                            if (mAlternative)
                                Toast.makeText(getActivity(), mSimChecked + ": " + sim, Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                        } else
                            Toast.makeText(getActivity(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        return dialog;
    }
}
