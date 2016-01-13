package ua.od.acros.dualsimtrafficcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import org.acra.ACRA;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class OnOffReceiver extends BroadcastReceiver {

    public OnOffReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        try {
            int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
            if (intent.getBooleanExtra(Constants.ON_OFF, true) && CountService.getLastActiveSIM() == sim &&
                    MobileUtils.getMobileDataInfo(context, false)[0] == 0)
                MobileUtils.toggleMobileDataConnection(true, context, sim);
            else if (!intent.getBooleanExtra(Constants.ON_OFF, true) && CountService.getActiveSIM() == sim &&
                    MobileUtils.getMobileDataInfo(context, false)[0] == 2)
                MobileUtils.toggleMobileDataConnection(false, context, Constants.DISABLED);

            /*String out = new SimpleDateFormat(Constants.TIME_FORMAT, context.getResources().getConfiguration().locale).format(new Date()) + " " + String.valueOf(intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED)) + " | " +
                    String.valueOf(intent.getBooleanExtra(Constants.ON_OFF, true)) + "\n";
            // to this path add a new directory path
            File dir = new File(String.valueOf(context.getFilesDir()));
            // create this directory if not already created
            dir.mkdir();
            // create the file in which we will write the contents
            String fileName ="log_alarm.txt";
            File file = new File(dir, fileName);
            FileOutputStream os = new FileOutputStream(file, true);
            os.write(out.getBytes());
            os.close();*/
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
    }
}
