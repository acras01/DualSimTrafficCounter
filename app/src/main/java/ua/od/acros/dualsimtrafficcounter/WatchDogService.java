package ua.od.acros.dualsimtrafficcounter;

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
import ua.od.acros.dualsimtrafficcounter.utils.TrafficDatabase;

public class WatchDogService extends Service{

    private SharedPreferences prefs;
    private Context context;
    private TrafficDatabase mDatabaseHelper;
    private Timer mTimer;
    private boolean isFirstRun;

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
        prefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        context = WatchDogService.this;
        mDatabaseHelper = new TrafficDatabase(this, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
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

        isFirstRun = true;
        // schedule task
        mTimer.scheduleAtFixedRate(new WatchDogTask(), 0, Long.parseLong(prefs.getString(Constants.PREF_OTHER[8], "1")) * 60 * 1000);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        mTimer.purge();
        prefs.edit().putBoolean(Constants.PREF_OTHER[6], true).apply();
    }

    private class WatchDogTask extends TimerTask {
        /**
         * The task to run should be specified in the implementation of the {@code run()}
         * method.
         */
        @Override
        public void run() {

            if (isFirstRun) {
                try {
                    TimeUnit.MINUTES.sleep(Long.parseLong(prefs.getString(Constants.PREF_OTHER[8], "1")));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                isFirstRun = false;
            }
            ContentValues dataMap = TrafficDatabase.readTrafficData(mDatabaseHelper);
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
                    (MobileUtils.getMobileDataInfo(context, false)[0] == 2 && MobileUtils.getMobileDataInfo(context, true)[1] > Constants.DISABLED) &&
                    !prefs.getBoolean(Constants.PREF_OTHER[5], false)) {
                stopService(new Intent(context, CountService.class));
                /*String out = lastUpdate + " | " + now.toString(fmt) + "\n";
                File dir = new File(String.valueOf(context.getFilesDir()));
                // create this directory if not already created
                dir.mkdir();
                // create the file in which we will write the contents
                String fileName ="watchdog_log.txt";
                File file = new File(dir, fileName);
                FileOutputStream os;
                try {
                    os = new FileOutputStream(file, true);
                    os.write(out.getBytes());
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }*/
                startService(new Intent(context, CountService.class));
            }
        }
    }
}
