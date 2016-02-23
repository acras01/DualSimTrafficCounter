package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import org.acra.ACRA;

import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class OnOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        try {
            int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
            if (intent.getBooleanExtra(Constants.ON_OFF, true) && TrafficCountService.getLastActiveSIM() == sim &&
                    MobileUtils.getMobileDataInfo(context, false)[0] == 0)
                MobileUtils.toggleMobileDataConnection(true, context, sim);
            else if (!intent.getBooleanExtra(Constants.ON_OFF, true) && TrafficCountService.getActiveSIM() == sim &&
                    MobileUtils.getMobileDataInfo(context, false)[0] == 2)
                MobileUtils.toggleMobileDataConnection(false, context, Constants.DISABLED);
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        if (wl.isHeld())
            wl.release();
    }
}
