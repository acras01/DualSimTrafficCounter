package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DateUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ResetReceiver extends BroadcastReceiver {
    public ResetReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        String[] simPref = new String[]{Constants.PREF_SIM1_CALLS[2], Constants.PREF_SIM1_CALLS[4],
                Constants.PREF_SIM1_CALLS[5], Constants.PREF_SIM1_CALLS[8]};
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
        if (!prefs.getBoolean(Constants.PREF_SIM1_CALLS[9], false)) {
            DateTime mResetTime1 = DateUtils.setResetDate(prefs, simPref);
            if (mResetTime1 != null) {
                prefs.edit()
                        .putBoolean(Constants.PREF_SIM1_CALLS[9], true)
                        .putString(Constants.PREF_SIM1_CALLS[8], mResetTime1.toString(fmtDateTime))
                        .apply();
            }
        }
        if (simQuantity >= 2) {
            if (!prefs.getBoolean(Constants.PREF_SIM2_CALLS[9], false)) {
                simPref = new String[]{Constants.PREF_SIM2_CALLS[2], Constants.PREF_SIM2_CALLS[4],
                        Constants.PREF_SIM2_CALLS[5], Constants.PREF_SIM2_CALLS[8]};
                DateTime mResetTime2 = DateUtils.setResetDate(prefs, simPref);
                if (mResetTime2 != null) {
                    prefs.edit()
                            .putBoolean(Constants.PREF_SIM2_CALLS[9], true)
                            .putString(Constants.PREF_SIM2_CALLS[8], mResetTime2.toString(fmtDateTime))
                            .apply();
                }
            }
        }
        if (simQuantity == 3) {
            if (!prefs.getBoolean(Constants.PREF_SIM3_CALLS[9], false)) {
                simPref = new String[]{Constants.PREF_SIM3_CALLS[2], Constants.PREF_SIM3_CALLS[4],
                        Constants.PREF_SIM3_CALLS[5], Constants.PREF_SIM3_CALLS[8]};
                DateTime mResetTime3 = DateUtils.setResetDate(prefs, simPref);
                if (mResetTime3 != null) {
                    prefs.edit()
                            .putBoolean(Constants.PREF_SIM3_CALLS[9], true)
                            .putString(Constants.PREF_SIM3_CALLS[8], mResetTime3.toString(fmtDateTime))
                            .apply();
                }
            }
        }
        if (wl.isHeld())
            wl.release();
    }
}
