package ua.od.acros.dualsimtrafficcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileDataControl;

public class OnOffReceiver extends BroadcastReceiver {
    public OnOffReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
            if (intent.getBooleanExtra(Constants.ON_OFF, true) && CountService.getLastActiveSIM() == sim &&
                    MobileDataControl.getMobileDataInfo(context)[0] == 0)
                MobileDataControl.toggleMobileDataConnection(true, context, sim);
            else if (!intent.getBooleanExtra(Constants.ON_OFF, true) && CountService.getActiveSIM() == sim &&
                    MobileDataControl.getMobileDataInfo(context)[0] == 2)
                MobileDataControl.toggleMobileDataConnection(false, context, sim);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String out = String.valueOf(intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED)) + " | " +
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
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
