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
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ShowSimDialog extends DialogFragment implements CompoundButton.OnCheckedChangeListener {

    private Button bOK;
    private static boolean[] mSim = new boolean[3];
    private SharedPreferences mPrefs;
    private String mActivity;
    private Context mContext;
    private String[] mOperatorNames;

    public static ShowSimDialog newInstance(String activity, boolean[] sim) {
        ShowSimDialog f = new ShowSimDialog();
        Bundle b = new Bundle();
        b.putBooleanArray("sim", sim);
        b.putString("activity", activity);
        f.setArguments(b);
        return f;
    }

    public interface ShowSimDialogClosedListener {
        void OnDialogClosed(String activity, boolean[] sim);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = CustomApplication.getAppContext();
        mPrefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mActivity = getArguments().getString("activity");
        mSim = getArguments().getBooleanArray("sim");
        mOperatorNames = new String[] {MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = View.inflate(getActivity(), R.layout.showsim_dialog, null);
        CheckBox sim1 = (CheckBox) view.findViewById(R.id.sim1);
        CheckBox sim2 = (CheckBox) view.findViewById(R.id.sim2);
        CheckBox sim3 = (CheckBox) view.findViewById(R.id.sim3);
        int simQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        sim1.setText(mOperatorNames[0]);
        sim2.setText(mOperatorNames[1]);
        sim3.setText(mOperatorNames[2]);
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
