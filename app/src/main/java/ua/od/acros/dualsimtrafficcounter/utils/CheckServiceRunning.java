package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.ActivityManager;
import android.content.Context;

public class CheckServiceRunning {

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName()))
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

}
