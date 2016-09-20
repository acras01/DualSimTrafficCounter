package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.CustomDialogEvent;

public class CustomDialog extends DialogFragment {

    private static final String FIRST_RUN = "first_run";
    private static final String ANDROID_5_0 = "API21";
    private static final String MTK = "mtk";

    public static CustomDialog newInstance(String key) {
        CustomDialog d = new CustomDialog();
        Bundle b = new Bundle();
        b.putString("key", key);
        d.setArguments(b);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setTitle(R.string.attention)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        EventBus.getDefault().post(new CustomDialogEvent());
                    }
                })
                .create();
        String key = getArguments().getString("key");
        if (key != null) {
            switch (key) {
                case FIRST_RUN:
                    dialog.setMessage(getString(R.string.set_sim_number));
                    break;
                case MTK:
                    dialog.setMessage(getString(R.string.on_off_not_supported));
                    break;
                case ANDROID_5_0:
                    dialog.setMessage(getString(R.string.need_root));
                    break;
            }
        }
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
