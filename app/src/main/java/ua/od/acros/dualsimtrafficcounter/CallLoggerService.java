package ua.od.acros.dualsimtrafficcounter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.CallLog;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.lang.ref.WeakReference;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MTKUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;

public class CallLoggerService extends Service {

    private static Context mContext;
    private MyDatabase mDatabaseHelper;
    private ContentValues mCalls;
    private DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT + ":ss");
    private BroadcastReceiver callDataReceiver;
    private MyCallObserver outgoingCallsObserver;

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
        mDatabaseHelper = MyDatabase.getInstance(mContext);
        mCalls = MyDatabase.readCallsData(mDatabaseHelper);

        outgoingCallsObserver = new MyCallObserver(new MyHandler(this));
        getContentResolver().registerContentObserver(android.provider.CallLog.Calls.CONTENT_URI, true, outgoingCallsObserver);

        callDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, intent.getStringExtra(Constants.SIM_ACTIVE) + ": " +
                        String.format("%.2f", (double) intent.getLongExtra(Constants.CALL_DURATION, 0L)/1000) + "s", Toast.LENGTH_LONG).show();
            }
        };
        IntentFilter callDataFilter = new IntentFilter(Constants.CALLS);
        registerReceiver(callDataReceiver, callDataFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(mContext, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification n =  new NotificationCompat.Builder(mContext)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_small)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText("Hi!")
                .build();
        startForeground(Constants.STARTED_ID + 1000, n);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(callDataReceiver);
        getContentResolver().unregisterContentObserver(outgoingCallsObserver);
    }

    public static Context getAppContext() {
        return CallLoggerService.mContext;
    }

    class MyCallObserver extends ContentObserver {

        private Handler myHandler;

        public MyCallObserver(Handler h) {
            super(h);
            this.myHandler = h;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            MyTask myTask = new MyTask();
            myTask.execute();
        }

        private class MyTask extends AsyncTask<Void, Void, Bundle>{
            @Override
            protected Bundle doInBackground(Void... params) {
                return queryCallHistory();
            }

            @Override
            protected void onPostExecute(Bundle result) {
                super.onPostExecute(result);
                Message msg = new Message();
                msg.setData(result);
                myHandler.sendMessage(msg);
            }
        }
    }

    public static int getSimIdColumn(Cursor paramCursor) {
        String[] arrayOfString = new String[7];
        arrayOfString[0] = "sim_id";
        arrayOfString[1] = "simid";
        arrayOfString[2] = "sub_id";
        arrayOfString[3] = "simId";
        arrayOfString[4] = "sim_sn";
        arrayOfString[5] = "simsn";
        arrayOfString[6] = "simSn";
        for (String anArrayOfString : arrayOfString) {
            int ind = paramCursor.getColumnIndex(anArrayOfString);
            if (ind >= 0)
                return ind;
        }
        return -1;
    }

    private Bundle queryCallHistory() {
        Bundle bundle = new Bundle();
        //query the call history
        int id = -1;
        Cursor mCallCursor = getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI, null,
                null, null,android.provider.CallLog.Calls.DATE + " DESC");
        if (mCallCursor != null) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
                id = getSimIdColumn(mCallCursor);
            else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
                id = mCallCursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID);
            if (mCallCursor.getCount() > 0) {
                //if there is more than 1 call
                mCallCursor.moveToFirst();
                try {
                    int callType = mCallCursor.getInt(mCallCursor.getColumnIndex(android.provider.CallLog.Calls.TYPE));
                    if (callType == CallLog.Calls.OUTGOING_TYPE) {
                        int simid = Integer.valueOf(mCallCursor.getString(id));
                        long callDate = mCallCursor.getLong(mCallCursor.getColumnIndex(android.provider.CallLog.Calls.DATE));
                        DateTime callTime = new DateTime(callDate);
                        long callDuration = mCallCursor.getLong(mCallCursor.getColumnIndex(android.provider.CallLog.Calls.DURATION));
                        bundle.putInt(Constants.SIM_ACTIVE, simid);
                        bundle.putString(Constants.LAST_DATE, callTime.toString(fmtDateTime));
                        bundle.putLong(Constants.CALL_DURATION, callDuration);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCallCursor.close();
            }
        }
        return bundle;
    }

    static class MyHandler extends Handler {
        private final WeakReference<CallLoggerService> mService;

        MyHandler(CallLoggerService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            CallLoggerService service = mService.get();
            if (service != null)
                service.handleMessage(msg);
        }
    }

    private void handleMessage(Message msg) {
        Bundle bundle = msg.getData();
        int sim = bundle.getInt(Constants.SIM_ACTIVE);
        boolean what = false;
        if (MTKUtils.isMtkDevice() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
            what = true;
        else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
            what = false;
        sim = MobileUtils.getSIMFromId(what, sim, mContext);
        mCalls.put(Constants.SIM_ACTIVE, sim);
        mCalls.put(Constants.LAST_DATE, bundle.getString(Constants.LAST_DATE));
        mCalls.put(Constants.CALL_DURATION, bundle.getLong(Constants.CALL_DURATION));
    }
}