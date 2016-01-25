package ua.od.acros.dualsimtrafficcounter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class CallLoggerService extends Service {

    private static Context mContext;

    public CallLoggerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = CallLoggerService.this;
    }

    public static Context getAppContext() {
        return CallLoggerService.mContext;
    }
}
