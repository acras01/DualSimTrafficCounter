package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.Window;
import android.view.WindowManager;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.ListEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoListEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;

public class ChooseOperatorDialog extends AppCompatActivity {

    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        final ArrayList<String> whiteList = getIntent().getStringArrayListExtra("whitelist");
        final ArrayList<String> blackList = getIntent().getStringArrayListExtra("blacklist");
        final Bundle bundle = getIntent().getBundleExtra("bundle");
        String number = bundle.getString("number");

        mDialog = new AlertDialog.Builder(this)
                .setTitle(number)
                .setCancelable(false)
                .setMessage(R.string.is_out_of_home_network)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EventBus.getDefault().post(new ListEvent(true, blackList, bundle));
                        finish();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EventBus.getDefault().post(new ListEvent(false, whiteList, bundle));
                        finish();
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EventBus.getDefault().post(new NoListEvent());
                        finish();
                    }
                })
                .create();
        Window dialogWindow = mDialog.getWindow();
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialogWindow.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        /*dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        Window mainWindow = getWindow();
        WindowManager.LayoutParams dialoglp = dialogWindow.getAttributes();
        WindowManager.LayoutParams mainlp = mainWindow.getAttributes();
        mainlp.width = dialoglp.width; // Width
        mainlp.height = dialoglp.height; // Height
        mainWindow.setAttributes(mainlp);*/

        if(!this.isFinishing()){
            mDialog.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }
}