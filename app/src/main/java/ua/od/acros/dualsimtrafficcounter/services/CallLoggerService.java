package ua.od.acros.dualsimtrafficcounter.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.dialogs.ChooseOperatorDialog;
import ua.od.acros.dualsimtrafficcounter.events.ListEvent;
import ua.od.acros.dualsimtrafficcounter.events.NewOutgoingCallEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoListEvent;
import ua.od.acros.dualsimtrafficcounter.events.SetCallsEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.CustomNotification;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.DateUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class CallLoggerService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static Context mContext;
    private CustomDatabaseHelper mDbHelper;
    private ContentValues mCallsData;
    private String[] mOperatorNames = new String[3];
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private CountDownTimer mCountTimer;
    private Vibrator mVibrator;
    private boolean mIsResetNeeded3 = false;
    private boolean mIsResetNeeded2 = false;
    private DateTime mResetTime2, mResetTime3;
    private boolean mIsOutgoing = false;
    private boolean mIsDialogShown = false;
    private final String[] mNumber = new String[1];
    private boolean mLimitHasChanged;
    private long[] mLimits = new long[3];
    private BroadcastReceiver mCallAnsweredReceiver, mCallEndedReceiver;
    private ArrayList<String> mIMSI = null;
    private Service mService = null;
    private boolean mIdChanged;

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
        mService = this;
        mContext = CustomApplication.getAppContext();
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mDbHelper = CustomDatabaseHelper.getInstance(mContext);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefs.edit()
                .putBoolean(Constants.PREF_OTHER[49], true)
                .apply();
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            mIMSI = MobileUtils.getSimIds(mContext);
            String path = mContext.getFilesDir().getParent() + "/shared_prefs/";
            SharedPreferences.Editor editor = mPrefs.edit();
            SharedPreferences prefSim;
            Map<String, ?> prefs;
            String name = Constants.CALLS + "_" + mIMSI.get(0);
            if (new File(path + name + ".xml").exists()) {
                prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                prefs = prefSim.getAll();
                if (prefs.size() != 0)
                    for (String key : prefs.keySet()) {
                        Object o = prefs.get(key);
                        key = key + 1;
                        CustomApplication.putObject(editor, key, o);
                    }
                prefSim = null;
            }
            if (mSimQuantity >= 2) {
                name = Constants.CALLS + "_" + mIMSI.get(1);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (String key : prefs.keySet()) {
                            Object o = prefs.get(key);
                            key = key + 2;
                            CustomApplication.putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            if (mSimQuantity == 3) {
                name = Constants.CALLS + "_" + mIMSI.get(2);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (String key : prefs.keySet()) {
                            Object o = prefs.get(key);
                            key = key + 3;
                            CustomApplication.putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            editor.apply();
        }

        mPrefs = null;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mIdChanged = true;
        mCallsData = new ContentValues();
        readCallsDataFromDatabase();
        mLimits = CustomApplication.getCallsSimLimitsValues();
        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));

        mCallAnsweredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mIsOutgoing) {
                    //final String[] out = {"Call Starts\n"};
                    readCallsDataFromDatabase();
                    String lim, inter;
                    long currentDuration = 0;
                    int interval = 10;
                    long limit = Long.MAX_VALUE;
                    int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                    switch (sim) {
                        case Constants.SIM1:
                            currentDuration = (long) mCallsData.get(Constants.CALLS1);
                            lim = mPrefs.getString(Constants.PREF_SIM1_CALLS[1], "0");
                            inter = mPrefs.getString(Constants.PREF_SIM1_CALLS[3], "0");
                            if (!inter.equals(""))
                                interval = Integer.valueOf(inter) * Constants.SECOND;
                            if (!lim.equals(""))
                                limit = Long.valueOf(lim) * Constants.MINUTE;
                            break;
                        case Constants.SIM2:
                            currentDuration = (long) mCallsData.get(Constants.CALLS2);
                            lim = mPrefs.getString(Constants.PREF_SIM2_CALLS[1], "0");
                            inter = mPrefs.getString(Constants.PREF_SIM2_CALLS[3], "0");
                            if (!inter.equals(""))
                                interval = Integer.valueOf(inter) * Constants.SECOND;
                            if (!lim.equals(""))
                                limit = Long.valueOf(lim) * Constants.MINUTE;
                            break;
                        case Constants.SIM3:
                            currentDuration = (long) mCallsData.get(Constants.CALLS3);
                            lim = mPrefs.getString(Constants.PREF_SIM3_CALLS[1], "0");
                            inter = mPrefs.getString(Constants.PREF_SIM3_CALLS[3], "0");
                            if (!inter.equals(""))
                                interval = Integer.valueOf(inter) * Constants.SECOND;
                            if (!lim.equals(""))
                                limit = Long.valueOf(lim) * Constants.MINUTE;
                            break;
                    }
                    long timeToVibrate = limit - currentDuration - interval;
                    if (timeToVibrate < 0)
                        timeToVibrate = 0;
                    //out[0] += String.valueOf(timeToVibrate / Constants.SECOND) + "\n";
                    mCountTimer = new CountDownTimer(timeToVibrate, timeToVibrate) {
                        public void onTick(long millisUntilFinished) {
                            //out[0] += String.valueOf(millisUntilFinished / Constants.SECOND) + "\n";
                        }

                        public void onFinish() {
                            if (mVibrator.hasVibrator())
                                vibrate(mVibrator, Constants.SECOND, Constants.SECOND / 2);
                            //out[0] += "Limit reached\n";
                        }
                    }.start();
                    /*try {
                        // to this path add a new directory path
                        File dir = new File(String.valueOf(mContext.getFilesDir()));
                        // create this directory if not already created
                        dir.mkdir();
                        // create the file in which we will write the contents
                        String fileName = "call_log.txt";
                        File file = new File(dir, fileName);
                        FileOutputStream os = new FileOutputStream(file, true);
                        os.write(out[0].getBytes());
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                }
            }
        };
        IntentFilter answer = new IntentFilter(Constants.OUTGOING_CALL_ANSWERED);
        registerReceiver(mCallAnsweredReceiver, answer);

        mCallEndedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mIsOutgoing) {
                    mIsDialogShown = mIsOutgoing = false;
                    if (mCountTimer != null)
                        mCountTimer.cancel();
                    mVibrator.cancel();
                    int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                    long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                    Toast.makeText(context, mOperatorNames[sim] + ": " +
                            DataFormat.formatCallDuration(context, duration), Toast.LENGTH_LONG).show();
                    DateTime now = new DateTime();
                    mCallsData.put(Constants.LAST_DATE, now.toString(Constants.DATE_FORMATTER));
                    mCallsData.put(Constants.LAST_TIME, now.toString(Constants.TIME_FORMATTER));
                    switch (sim) {
                        case Constants.SIM1:
                            mCallsData.put(Constants.CALLS1_EX, duration + (long) mCallsData.get(Constants.CALLS1_EX));
                            if (mPrefs.getString(Constants.PREF_SIM1_CALLS[6], "0").equals("1"))
                                duration = (long) Math.ceil((double) duration / Constants.MINUTE) * Constants.MINUTE;
                            mCallsData.put(Constants.CALLS1, duration + (long) mCallsData.get(Constants.CALLS1));
                            duration = (long) mCallsData.get(Constants.CALLS1);
                            break;
                        case Constants.SIM2:
                            mCallsData.put(Constants.CALLS2_EX, duration + (long) mCallsData.get(Constants.CALLS2_EX));
                            if (mPrefs.getString(Constants.PREF_SIM2_CALLS[6], "0").equals("1"))
                                duration = (long) Math.ceil((double) duration / Constants.MINUTE) * Constants.MINUTE;
                            mCallsData.put(Constants.CALLS2, duration + (long) mCallsData.get(Constants.CALLS2));
                            duration = (long) mCallsData.get(Constants.CALLS2);
                            break;
                        case Constants.SIM3:
                            mCallsData.put(Constants.CALLS3_EX, duration + (long) mCallsData.get(Constants.CALLS3_EX));
                            if (mPrefs.getString(Constants.PREF_SIM3_CALLS[6], "0").equals("1"))
                                duration = (long) Math.ceil((double) duration / Constants.MINUTE) * Constants.MINUTE;
                            mCallsData.put(Constants.CALLS3, duration + (long) mCallsData.get(Constants.CALLS3));
                            duration = (long) mCallsData.get(Constants.CALLS3);
                            break;
                    }
                    writeCallsDataToDatabase();
                    refreshWidgetAndNotification(sim, duration);
                    /*String out = "Call Ends\n";
                    try {
                        // to this path add a new directory path
                        File dir = new File(String.valueOf(mContext.getFilesDir()));
                        // create this directory if not already created
                        dir.mkdir();
                        // create the file in which we will write the contents
                        String fileName = "call_log.txt";
                        File file = new File(dir, fileName);
                        FileOutputStream os = new FileOutputStream(file, true);
                        os.write(out.getBytes());
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                    mService.stopSelf();
                }
            }
        };
        IntentFilter end = new IntentFilter(Constants.OUTGOING_CALL_ENDED);
        registerReceiver(mCallEndedReceiver, end);
    }

    @Subscribe
    public void onMessageEvent(NewOutgoingCallEvent event) {
        startTask(mContext, event.number);
    }

    @Subscribe(sticky = true)
    public void onMessageEvent(SetCallsEvent event) {
        if (mCallsData == null)
            readCallsDataFromDatabase();
        DateTime now = new DateTime();
        mCallsData.put(Constants.LAST_DATE, now.toString(Constants.DATE_FORMATTER));
        mCallsData.put(Constants.LAST_TIME, now.toString(Constants.TIME_FORMATTER));
        int sim = event.sim;
        long duration = DataFormat.getDuration(event.calls, event.callsv);
        switch (sim) {
            case Constants.SIM1:
                mCallsData.put(Constants.CALLS1, duration);
                mCallsData.put(Constants.CALLS1_EX, duration);
                break;
            case Constants.SIM2:
                mCallsData.put(Constants.CALLS2, duration);
                mCallsData.put(Constants.CALLS2_EX, duration);
                break;
            case Constants.SIM3:
                mCallsData.put(Constants.CALLS3, duration);
                mCallsData.put(Constants.CALLS3_EX, duration);
                break;
        }
        writeCallsDataToDatabase();
        refreshWidgetAndNotification(sim, duration);
        EventBus.getDefault().removeStickyEvent(event);
        if(!mIsOutgoing)
            mService.stopSelf();
    }

    @Subscribe
    public void onMessageEvent(ListEvent event) {
        mIsOutgoing = event.bundle.getBoolean("black");
        new SaveListTask().execute(event.bundle);
        if(!mIsOutgoing)
            mService.stopSelf();
    }

    @Subscribe
    public void onMessageEvent(NoListEvent event) {
        mService.stopSelf();
    }

    class SaveListTask extends AsyncTask<Bundle, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Bundle... params) {
            Bundle bundle = params[0];
            ArrayList<String> list = bundle.getStringArrayList("list");
            int sim = bundle.getInt("sim");
            if (list != null) {
                list.add(bundle.getString("number"));
                if (bundle.getBoolean("black", false)) {
                    CustomDatabaseHelper.writeList(sim, list, mDbHelper, mIMSI, "black");
                } else {
                    CustomDatabaseHelper.writeList(sim, list, mDbHelper, mIMSI, "white");
                }
                return true;
            } else
                return false;

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(mContext, R.string.saved, Toast.LENGTH_LONG).show();
        }
    }

    private void startTask(Context context, String number) {
        DateTime now = DateTime.now();
        DateTime mResetTime1 = Constants.DATE_TIME_FORMATTER.parseDateTime(mPrefs.getString(Constants.PREF_SIM1_CALLS[8], now.toString(Constants.DATE_TIME_FORMATTER)));
        boolean mIsResetNeeded1 = mPrefs.getBoolean(Constants.PREF_SIM1_CALLS[9], true);
        if (mSimQuantity >= 2) {
            mResetTime2 = Constants.DATE_TIME_FORMATTER.parseDateTime(mPrefs.getString(Constants.PREF_SIM2_CALLS[8], now.toString(Constants.DATE_TIME_FORMATTER)));
            mIsResetNeeded2 = mPrefs.getBoolean(Constants.PREF_SIM2_CALLS[9], true);
        }
        if (mSimQuantity == 3) {
            mResetTime3 = Constants.DATE_TIME_FORMATTER.parseDateTime(mPrefs.getString(Constants.PREF_SIM3_CALLS[8], now.toString(Constants.DATE_TIME_FORMATTER)));
            mIsResetNeeded3 = mPrefs.getBoolean(Constants.PREF_SIM3_CALLS[9], true);
        }
        if (DateTimeComparator.getInstance().compare(now, mResetTime1) >= 0 && mIsResetNeeded1) {
            mCallsData.put(Constants.LAST_DATE, now.toString(Constants.DATE_FORMATTER));
            mCallsData.put(Constants.LAST_TIME, now.toString(Constants.TIME_FORMATTER));
            mCallsData.put(Constants.CALLS1, 0L);
            mCallsData.put(Constants.CALLS1_EX, 0L);
            writeCallsDataToDatabase();
            mIsResetNeeded1 = false;
            mPrefs.edit()
                    .putBoolean(Constants.PREF_SIM1_CALLS[9], mIsResetNeeded1)
                    .putString(Constants.PREF_SIM1_CALLS[10], now.toString(Constants.DATE_TIME_FORMATTER))
                    .apply();
            if (mPrefs.getBoolean(Constants.PREF_OTHER[31], false))
                pushResetNotification(Constants.SIM1);
        }
        if (DateTimeComparator.getInstance().compare(now, mResetTime2) >= 0 && mIsResetNeeded2) {
            mCallsData.put(Constants.LAST_DATE, now.toString(Constants.DATE_FORMATTER));
            mCallsData.put(Constants.LAST_TIME, now.toString(Constants.TIME_FORMATTER));
            mCallsData.put(Constants.CALLS2, 0L);
            mCallsData.put(Constants.CALLS2_EX, 0L);
            writeCallsDataToDatabase();
            mIsResetNeeded2 = false;
            mPrefs.edit()
                    .putBoolean(Constants.PREF_SIM2_CALLS[9], mIsResetNeeded2)
                    .putString(Constants.PREF_SIM2_CALLS[10], now.toString(Constants.DATE_TIME_FORMATTER))
                    .apply();
            if (mPrefs.getBoolean(Constants.PREF_OTHER[31], false))
                pushResetNotification(Constants.SIM2);
        }
        if (DateTimeComparator.getInstance().compare(now, mResetTime3) >= 0 && mIsResetNeeded3) {
            mCallsData.put(Constants.LAST_DATE, now.toString(Constants.DATE_FORMATTER));
            mCallsData.put(Constants.LAST_TIME, now.toString(Constants.TIME_FORMATTER));
            mCallsData.put(Constants.CALLS3, 0L);
            mCallsData.put(Constants.CALLS3_EX, 0L);
            writeCallsDataToDatabase();
            mIsResetNeeded3 = false;
            mPrefs.edit()
                    .putBoolean(Constants.PREF_SIM3_CALLS[9], mIsResetNeeded3)
                    .putString(Constants.PREF_SIM3_CALLS[10], now.toString(Constants.DATE_TIME_FORMATTER))
                    .apply();
            if (mPrefs.getBoolean(Constants.PREF_OTHER[31], false))
                pushResetNotification(Constants.SIM3);
        }
        mIsOutgoing = false;
        final Context ctx = context;
        this.mNumber[0] = number.replaceAll("[\\s\\-()]", "");
        //this.mNumber[0] = MobileUtils.getFullNumber(ctx, intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        tm.listen(new PhoneStateListener() {

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (CustomApplication.isMyServiceRunning(CallLoggerService.class)) {
                    if (!mIsOutgoing && state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        final int sim = MobileUtils.getActiveSimForCall(ctx);
                            /*String out = sim + " " + CallLoggerService.this.mNumber[0] + "\n";
                            try {
                                // to this path add a new directory path
                                File dir = new File(String.valueOf(ctx.getFilesDir()));
                                // create this directory if not already created
                                dir.mkdir();
                                // create the file in which we will write the contents
                                String fileName = "call_log.txt";
                                File file = new File(dir, fileName);
                                FileOutputStream os = new FileOutputStream(file, true);
                                os.write(out.getBytes());
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }*/
                        final ArrayList<String> whiteList = CustomDatabaseHelper.readList(sim, mDbHelper, mIMSI, "white");
                        final ArrayList<String> blackList = CustomDatabaseHelper.readList(sim, mDbHelper, mIMSI, "black");
                        boolean white = whiteList.contains(CallLoggerService.this.mNumber[0]);
                        boolean black = blackList.contains(CallLoggerService.this.mNumber[0]);
                        if (!white && !black && !mIsDialogShown) {
                            mIsDialogShown = true;
                            final Bundle bundle = new Bundle();
                            bundle.putString("number", CallLoggerService.this.mNumber[0]);
                            bundle.putInt("sim", sim);
                            Intent dialogIntent = new Intent(mContext, ChooseOperatorDialog.class);
                            dialogIntent.putExtra("bundle", bundle);
                            dialogIntent.putExtra("whitelist", whiteList);
                            dialogIntent.putExtra("blacklist", blackList);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            CustomApplication.sleep(500);
                            mContext.startActivity(dialogIntent);
                        } else if (black)
                            mIsOutgoing = true;
                        else if (white)
                            mService.stopSelf();
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void refreshWidgetAndNotification(int sim, long duration) {
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(Constants.STARTED_ID, buildNotification());
        mIdChanged = false;
        int[] ids = CustomApplication.getWidgetIds(Constants.CALLS);
        if ((CustomApplication.isActivityVisible() && CustomApplication.isScreenOn()) || ids.length != 0) {
            Intent callsIntent = new Intent(Constants.CALLS_BROADCAST_ACTION);
            callsIntent.putExtra(Constants.SIM_ACTIVE, sim);
            callsIntent.putExtra(Constants.CALL_DURATION, duration);
            callsIntent.putExtra(Constants.WIDGET_IDS, ids);
            sendBroadcast(callsIntent);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sharedPreferences.getBoolean(Constants.PREF_OTHER[45], false)) {
            int sim = Constants.DISABLED;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM1_CALLS)).contains(key))
                sim = Constants.SIM1;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM2_CALLS)).contains(key))
                sim = Constants.SIM2;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM3_CALLS)).contains(key))
                sim = Constants.SIM3;
            if (sim >= 0) {
                Map prefs = sharedPreferences.getAll();
                Object o = prefs.get(key);
                SharedPreferences.Editor editor = mContext.getSharedPreferences(Constants.CALLS + "_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
                CustomApplication.putObject(editor, key.substring(0, key.length() - 1), o);
                editor.apply();
            }
        }
        if (key.equals(Constants.PREF_OTHER[45])) {
            writeCallsDataToDatabase();
            readCallsDataFromDatabase();
        }
        if (key.equals(Constants.PREF_SIM1_CALLS[2]) || key.equals(Constants.PREF_SIM1_CALLS[4]) || key.equals(Constants.PREF_SIM1_CALLS[5]) ||
                key.equals(Constants.PREF_SIM2_CALLS[2]) || key.equals(Constants.PREF_SIM2_CALLS[4]) || key.equals(Constants.PREF_SIM2_CALLS[5]) ||
                key.equals(Constants.PREF_SIM3_CALLS[2]) || key.equals(Constants.PREF_SIM3_CALLS[4]) || key.equals(Constants.PREF_SIM3_CALLS[5])) {
            int simQuantity = sharedPreferences.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                    : Integer.valueOf(sharedPreferences.getString(Constants.PREF_OTHER[14], "1"));
            DateTime now = DateTime.now();
            DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
            String[] simPref = new String[]{Constants.PREF_SIM1_CALLS[2], Constants.PREF_SIM1_CALLS[4],
                    Constants.PREF_SIM1_CALLS[5], Constants.PREF_SIM1_CALLS[8]};
            DateTime mResetTime1 = DateUtils.setResetDate(sharedPreferences, simPref);
            if (mResetTime1 != null && mResetTime1.isAfter(now)) {
                sharedPreferences.edit()
                        .putBoolean(Constants.PREF_SIM1_CALLS[9], true)
                        .putString(Constants.PREF_SIM1_CALLS[8], mResetTime1.toString(fmtDateTime))
                        .apply();
            }
            if (simQuantity >= 2) {
                simPref = new String[]{Constants.PREF_SIM2_CALLS[2], Constants.PREF_SIM2_CALLS[4],
                        Constants.PREF_SIM2_CALLS[5], Constants.PREF_SIM2_CALLS[8]};
                DateTime mResetTime2 = DateUtils.setResetDate(sharedPreferences, simPref);
                if (mResetTime2 != null && mResetTime2.isAfter(now)) {
                    sharedPreferences.edit()
                            .putBoolean(Constants.PREF_SIM2_CALLS[9], true)
                            .putString(Constants.PREF_SIM2_CALLS[8], mResetTime2.toString(fmtDateTime))
                            .apply();
                }
            }
            if (simQuantity == 3) {
                simPref = new String[]{Constants.PREF_SIM3_CALLS[2], Constants.PREF_SIM3_CALLS[4],
                        Constants.PREF_SIM3_CALLS[5], Constants.PREF_SIM3_CALLS[8]};
                DateTime mResetTime3 = DateUtils.setResetDate(sharedPreferences, simPref);
                if (mResetTime3 != null && mResetTime3.isAfter(now)) {
                    sharedPreferences.edit()
                            .putBoolean(Constants.PREF_SIM3_CALLS[9], true)
                            .putString(Constants.PREF_SIM3_CALLS[8], mResetTime3.toString(fmtDateTime))
                            .apply();
                }
            }
        }
        if (key.equals(Constants.PREF_SIM1_CALLS[1]) || key.equals(Constants.PREF_SIM2_CALLS[1]) || key.equals(Constants.PREF_SIM3_CALLS[1]))
            mLimitHasChanged = true;
        if (key.equals(Constants.PREF_OTHER[5]) && sharedPreferences.getBoolean(key, false)) {
            new CountDownTimer(2000, 2000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(Constants.STARTED_ID, buildNotification());
                    mIdChanged = false;
                }
            }.start();
        }
        if (!CustomApplication.isMyServiceRunning(TrafficCountService.class)) {
            if (key.equals(Constants.PREF_OTHER[15]) || key.equals(Constants.PREF_SIM1[23]) ||
                    key.equals(Constants.PREF_SIM2[23]) || key.equals(Constants.PREF_SIM3[23])) {
                mIdChanged = true;
                NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(Constants.STARTED_ID, buildNotification());
                mIdChanged = false;
            }
        }
    }

    private static void vibrate(Vibrator v, int v1, int p1) {
        long[] pattern = new long[] {0, v1, p1};
        v.vibrate(pattern, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        EventBus.getDefault().register(this);
        int[] ids = CustomApplication.getWidgetIds(Constants.CALLS);
        if (ids.length != 0) {
            Intent i = new Intent(Constants.CALLS_BROADCAST_ACTION);
            i.putExtra(Constants.WIDGET_IDS, ids);
            sendBroadcast(i);
        }
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            startForeground(Constants.STARTED_ID, buildNotification());
            mIdChanged = false;
            startTask(mContext, intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
        } else
            mService.stopSelf();
        return START_STICKY;
    }

    private void pushResetNotification(int simid) {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alert);
        String text = "";
        int id;
        if (mPrefs.getBoolean(Constants.PREF_OTHER[15], false)) {
            String[] pref = new String[Constants.PREF_SIM1.length];
            switch (simid) {
                case Constants.SIM1:
                    pref = Constants.PREF_SIM1;
                    text = mOperatorNames[0];
                    break;
                case Constants.SIM2:
                    pref = Constants.PREF_SIM2;
                    text = mOperatorNames[1];
                    break;
                case Constants.SIM3:
                    pref = Constants.PREF_SIM3;
                    text = mOperatorNames[2];
                    break;
            }
            if (mPrefs.getString(pref[23], "none").equals("auto"))
                id = getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(mContext, simid), "drawable", mContext.getPackageName());
            else
                id = getResources().getIdentifier(mPrefs.getString(pref[23], "none"), "drawable", mContext.getPackageName());
        } else
            id = R.drawable.ic_launcher_small;
        text = String.format(getResources().getString(R.string.calls_reset), text);
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setAction(Constants.CALLS);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(id)
                .setLargeIcon(bm)
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(text);
        nm.notify(simid + 1981, builder.build());
    }

    private Notification buildNotification() {
        long tot1, tot2 = 0, tot3 = 0;
        String calls = "";
        if (mPrefs.getBoolean(Constants.PREF_OTHER[19], false)) {
            calls = getString(R.string.remain_calls);
            if (mLimitHasChanged) {
                mLimits = CustomApplication.getCallsSimLimitsValues();
                mLimitHasChanged = false;
            }
            tot1 = mLimits[0] - (long) mCallsData.get(Constants.CALLS1);
            if (tot1 < 0)
                tot1 = 0;
            if (mSimQuantity >= 2) {
                tot2 = mLimits[1] - (long) mCallsData.get(Constants.CALLS2);
                if (tot2 < 0)
                    tot2 = 0;
            }
            if (mSimQuantity == 3) {
                tot3 = mLimits[2] - (long) mCallsData.get(Constants.CALLS3);
                if (tot3 < 0)
                    tot3 = 0;
            }
        } else {
            tot1 = (long) mCallsData.get(Constants.CALLS1);
            tot2 = (long) mCallsData.get(Constants.CALLS2);
            tot3 = (long) mCallsData.get(Constants.CALLS3);
        }

        if (mLimits[0] != Long.MAX_VALUE)
            calls += DataFormat.formatCallDuration(mContext, tot1);
        else
            calls += getString(R.string.not_set);
        if (mSimQuantity >= 2)
            if (mLimits[1] != Long.MAX_VALUE)
                calls += "  ||  " + DataFormat.formatCallDuration(mContext, tot2);
            else
                calls += "  ||  " + getString(R.string.not_set);
        if (mSimQuantity == 3)
            if (mLimits[2] != Long.MAX_VALUE)
                calls += "  ||  " + DataFormat.formatCallDuration(mContext, tot3);
            else
                calls += "  ||  " + getString(R.string.not_set);
        String traffic = "";
        if (!CustomApplication.isMyServiceRunning(TrafficCountService.class)) {
            ContentValues cv;
            boolean[] isNight = CustomApplication.getIsNightState();
            if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
                if (mIMSI == null)
                    mIMSI = MobileUtils.getSimIds(mContext);
                cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(0));
                tot1 = isNight[0] ? (long) cv.get("total_n") : (long) cv.get("total");
                if (mSimQuantity >= 2) {
                    cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(1));
                    tot2 = isNight[1] ? (long) cv.get("total_n") : (long) cv.get("total");
                }
                if (mSimQuantity == 3) {
                    cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(2));
                    tot3 = isNight[2] ? (long) cv.get("total_n") : (long) cv.get("total");
                }
            } else {
                cv = CustomDatabaseHelper.readTrafficData(mDbHelper);
                tot1 = isNight[0] ? (long) cv.get(Constants.TOTAL1_N) : (long) cv.get(Constants.TOTAL1);
                tot2 = isNight[1] ? (long) cv.get(Constants.TOTAL2_N) : (long) cv.get(Constants.TOTAL2);
                tot3 = isNight[2] ? (long) cv.get(Constants.TOTAL3_N) : (long) cv.get(Constants.TOTAL3);
            }
            long[] limits = CustomApplication.getTrafficSimLimitsValues();
            if (limits[0] != Long.MAX_VALUE)
                traffic = DataFormat.formatData(mContext, tot1);
            else
                traffic = getString(R.string.not_set);
            if (mSimQuantity >= 2)
                if (limits[1] != Long.MAX_VALUE)
                    traffic += "  ||  " + DataFormat.formatData(mContext, tot2);
                else
                    traffic += "  ||  " + getString(R.string.not_set);
            if (mSimQuantity == 3)
                if (limits[2] != Long.MAX_VALUE)
                    traffic += "  ||  " + DataFormat.formatData(mContext, tot3);
                else
                    traffic += "  ||  " + getString(R.string.not_set);
        }
        return CustomNotification.getNotification(mContext, traffic, calls, mIdChanged);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(Constants.STARTED_ID);
        mPrefs.edit()
                .putBoolean(Constants.PREF_OTHER[49], false)
                .apply();
        writeCallsDataToDatabase();
        unregisterReceiver(mCallAnsweredReceiver);
        unregisterReceiver(mCallEndedReceiver);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
    }

    private void writeCallsDataToDatabase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            ContentValues cv = new ContentValues();
            cv.put("calls", (long) mCallsData.get(Constants.CALLS1));
            cv.put("calls_ex", (long) mCallsData.get(Constants.CALLS1_EX));
            cv.put("period", (int) mCallsData.get(Constants.PERIOD1));
            cv.put(Constants.LAST_TIME, (String) mCallsData.get(Constants.LAST_TIME));
            cv.put(Constants.LAST_DATE, (String) mCallsData.get(Constants.LAST_DATE));
            CustomDatabaseHelper.writeDataForSim(cv, mDbHelper, Constants.CALLS + "_" + mIMSI.get(0));
            if (mSimQuantity >= 2) {
                cv = new ContentValues();;
                cv.put("calls", (long) mCallsData.get(Constants.CALLS2));
                cv.put("calls_ex", (long) mCallsData.get(Constants.CALLS2_EX));
                cv.put("period", (int) mCallsData.get(Constants.PERIOD2));
                cv.put(Constants.LAST_TIME, (String) mCallsData.get(Constants.LAST_TIME));
                cv.put(Constants.LAST_DATE, (String) mCallsData.get(Constants.LAST_DATE));
                CustomDatabaseHelper.writeDataForSim(cv, mDbHelper, Constants.CALLS + "_" + mIMSI.get(1));
            }
            if (mSimQuantity == 3) {
                cv = new ContentValues();;
                cv.put("calls", (long) mCallsData.get(Constants.CALLS3));
                cv.put("calls_ex", (long) mCallsData.get(Constants.CALLS3_EX));
                cv.put("period", (int) mCallsData.get(Constants.PERIOD3));
                cv.put(Constants.LAST_TIME, (String) mCallsData.get(Constants.LAST_TIME));
                cv.put(Constants.LAST_DATE, (String) mCallsData.get(Constants.LAST_DATE));
                CustomDatabaseHelper.writeDataForSim(cv, mDbHelper, Constants.CALLS + "_" + mIMSI.get(2));
            }
        } else
            CustomDatabaseHelper.writeData(mCallsData, mDbHelper, Constants.CALLS);
    }

    private void readCallsDataFromDatabase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            ContentValues cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(0));
            mCallsData.put(Constants.CALLS1, (long) cv.get("calls"));
            mCallsData.put(Constants.CALLS1_EX, (long) cv.get("calls_ex"));
            mCallsData.put(Constants.PERIOD1, (int) cv.get("period"));
            mCallsData.put(Constants.CALLS2, 0L);
            mCallsData.put(Constants.CALLS2_EX, 0L);
            mCallsData.put(Constants.PERIOD2, 0);
            mCallsData.put(Constants.CALLS3, 0L);
            mCallsData.put(Constants.CALLS3_EX, 0L);
            mCallsData.put(Constants.PERIOD3, 0);
            mCallsData.put(Constants.LAST_TIME, (String) cv.get(Constants.LAST_TIME));
            mCallsData.put(Constants.LAST_DATE, (String) cv.get(Constants.LAST_DATE));
            if (mSimQuantity >= 2) {
                cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(1));
                mCallsData.put(Constants.CALLS2, (long) cv.get("calls"));
                mCallsData.put(Constants.CALLS2_EX, (long) cv.get("calls_ex"));
                mCallsData.put(Constants.PERIOD2, (int) cv.get("period"));
            }
            if (mSimQuantity == 3) {
                cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(2));
                mCallsData.put(Constants.CALLS3, (long) cv.get("calls"));
                mCallsData.put(Constants.CALLS3_EX, (long) cv.get("calls_ex"));
                mCallsData.put(Constants.PERIOD3, (int) cv.get("period"));
            }
        } else
            mCallsData = CustomDatabaseHelper.readCallsData(mDbHelper);
        if (mCallsData.get(Constants.LAST_DATE).equals("")) {
            DateTime dateTime = new DateTime();
            mCallsData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
            mCallsData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
        }
    }
}