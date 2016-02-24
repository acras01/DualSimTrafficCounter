package ua.od.acros.dualsimtrafficcounter.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import org.acra.ACRA;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabaseHelper;

public class WatchDogService extends Service{

    private SharedPreferences mPrefs;
    private Context mContext;
    private MyDatabaseHelper mDatabaseHelper;
    private Timer mTimer;
    private boolean mIsFirstRun;

    public WatchDogService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mContext = WatchDogService.this;
        mDatabaseHelper = MyDatabaseHelper.getInstance(mContext);
        // cancel if already existed
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = new Timer();
        } else {
            // recreate new
            mTimer = new Timer();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mIsFirstRun = true;
        // schedule task
        mTimer.scheduleAtFixedRate(new WatchDogTask(), 0, Long.parseLong(mPrefs.getString(Constants.PREF_OTHER[8], "1")) * 60 * 1000);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        mTimer.purge();
        mPrefs.edit().putBoolean(Constants.PREF_OTHER[6], true).apply();
    }

    private class WatchDogTask extends TimerTask {
        @Override
        public void run() {

            if (mIsFirstRun) {
                try {
                    TimeUnit.MINUTES.sleep(Long.parseLong(mPrefs.getString(Constants.PREF_OTHER[8], "1")));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                mIsFirstRun = false;
            }
            ContentValues dataMap = MyDatabaseHelper.readTrafficData(mDatabaseHelper);
            if (dataMap.get(Constants.LAST_DATE).equals("")) {
                Calendar myCalendar = Calendar.getInstance();
                SimpleDateFormat formatDate = new SimpleDateFormat(Constants.DATE_FORMAT, getResources().getConfiguration().locale);
                SimpleDateFormat formatTime = new SimpleDateFormat(Constants.TIME_FORMAT + ":ss", getResources().getConfiguration().locale);
                dataMap.put(Constants.LAST_TIME, formatTime.format(myCalendar.getTime()));
                dataMap.put(Constants.LAST_DATE, formatDate.format(myCalendar.getTime()));
            }
            String lastUpdate = dataMap.get(Constants.LAST_DATE) + " " + dataMap.get(Constants.LAST_TIME);
            DateTimeFormatter fmt = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT + ":ss");
            DateTime last = fmt.parseDateTime(lastUpdate);
            DateTime now = new DateTime();
            if ((now.getMillis() - last.getMillis()) > 61 * 1000 &&
                    (MobileUtils.getMobileDataInfo(mContext, false)[0] == 2 &&
                            MobileUtils.getMobileDataInfo(mContext, true)[1] > Constants.DISABLED) &&
                    !mPrefs.getBoolean(Constants.PREF_OTHER[5], false)) {
                stopService(new Intent(mContext, TrafficCountService.class));
                startService(new Intent(mContext, TrafficCountService.class));
            }
        }
    }
}
