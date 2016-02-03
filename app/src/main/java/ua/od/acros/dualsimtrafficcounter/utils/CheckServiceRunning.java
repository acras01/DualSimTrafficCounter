package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class CheckServiceRunning {

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager;
        if (context != null)
            manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        else
            return false;
        if (manager != null) {
            List<ActivityManager.RunningServiceInfo> list = manager.getRunningServices(Integer.MAX_VALUE);
            if (list != null && list.size() > 0)
                for (ActivityManager.RunningServiceInfo service : list) {
                    if (serviceClass.getName().equals(service.service.getClassName()))
                        return true;
                }
            else
                return false;
        } else
            return false;
        return false;
    }

}
