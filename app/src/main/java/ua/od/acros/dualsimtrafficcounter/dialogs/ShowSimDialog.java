package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ShowSimDialog extends DialogFragment implements CompoundButton.OnCheckedChangeListener {

    private Button bOK;
    private static String mActivity;
    private static boolean[] mSim = new boolean[3];

    public static ShowSimDialog newInstance(String activity, boolean[] sim) {
        mActivity = activity;
        mSim = sim;
        return new ShowSimDialog();
    }

    public interface ShowSimDialogClosedListener {
        void OnDialogClosed(String activity, boolean[] sim);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity().getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        View view = View.inflate(getActivity(), R.layout.showsim_dialog, null);
        CheckBox sim1 = (CheckBox) view.findViewById(R.id.sim1);
        CheckBox sim2 = (CheckBox) view.findViewById(R.id.sim2);
        CheckBox sim3 = (CheckBox) view.findViewById(R.id.sim3);
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (simQuantity == 1) {
            sim2.setEnabled(false);
            sim2.setChecked(false);
            mSim[1] = false;
            sim3.setEnabled(false);
            sim3.setChecked(false);
            mSim[2] = false;
        }
        if (simQuantity == 2) {
            sim3.setEnabled(false);
            sim3.setChecked(false);
            mSim[2] = false;
        }

        sim1.setOnCheckedChangeListener(this);
        sim2.setOnCheckedChangeListener(this);
        sim3.setOnCheckedChangeListener(this);

        sim1.setChecked(mSim[0]);
        sim2.setChecked(mSim[1]);
        sim3.setChecked(mSim[2]);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
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
                        ShowSimDialogClosedListener listener = (ShowSimDialogClosedListener) getActivity();
                        listener.OnDialogClosed(mActivity, mSim);
                        dismiss();
                    }
                });
            }
        });
        return dialog;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.sim1:
                mSim[0] = isChecked;
                break;
            case R.id.sim2:
                mSim[1] = isChecked;
                break;
            case R.id.sim3:
                mSim[2] = isChecked;
                break;
        }
    }

}
