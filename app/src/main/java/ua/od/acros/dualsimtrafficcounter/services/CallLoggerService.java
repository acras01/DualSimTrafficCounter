package ua.od.acros.dualsimtrafficcounter.services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabase;
import ua.od.acros.dualsimtrafficcounter.utils.MyNotification;
import ua.od.acros.dualsimtrafficcounter.widgets.CallsInfoWidget;

public class CallLoggerService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static Context mContext;
    private MyDatabase mDatabaseHelper;
    private ContentValues mCalls;
    private DateTimeFormatter fmtDate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
    private DateTimeFormatter fmtTime = DateTimeFormat.forPattern(Constants.TIME_FORMAT + ":ss");
    private DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
    private BroadcastReceiver callDataReceiver, setUsageReceiver, clearReceiver, callDurationReceiver, outgoingCallReceiver;
    private String[] mOperatorNames = new String[3];
    private SharedPreferences mPrefs;
    private int mSimQuantity;
    private CountDownTimer mCountTimer;
    private Vibrator mVibrator;
    private boolean mResetRuleHasChanged = false;
    private boolean mIsResetNeeded3 = false;
    private boolean mIsResetNeeded2 = false;
    private boolean mIsResetNeeded1 = false;
    private DateTime mResetTime1;
    private DateTime mResetTime2;
    private DateTime mResetTime3;
    private boolean mIsOutgoing = false;
    private boolean mIsDialogShown = false;
    private final String[] number = new String[1];

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
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mDatabaseHelper = MyDatabase.getInstance(mContext);
        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mCalls = MyDatabase.readCallsData(mDatabaseHelper);
        mOperatorNames[0] = MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        mOperatorNames[1] = MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        mOperatorNames[2] = MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
        callDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mIsOutgoing) {
                    mIsDialogShown = false;
                    mIsOutgoing = false;
                    if (mCountTimer != null)
                        mCountTimer.cancel();
                    mVibrator.cancel();
                    int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                    long duration = intent.getLongExtra(Constants.CALL_DURATION, 0L);
                    Toast.makeText(context, mOperatorNames[sim] + ": " +
                            DataFormat.formatCallDuration(context, duration), Toast.LENGTH_LONG).show();
                    DateTime now = new DateTime();
                    mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
                    mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
                    switch (sim) {
                        case Constants.SIM1:
                            mCalls.put(Constants.CALLS1_EX, duration + (long) mCalls.get(Constants.CALLS1_EX));
                            if (mPrefs.getString(Constants.PREF_SIM1_CALLS[6], "0").equals("1"))
                                duration = (long) Math.ceil((double) duration / Constants.MINUTE) * Constants.MINUTE;
                            mCalls.put(Constants.CALLS1, duration + (long) mCalls.get(Constants.CALLS1));
                            break;
                        case Constants.SIM2:
                            mCalls.put(Constants.CALLS2_EX, duration + (long) mCalls.get(Constants.CALLS2_EX));
                            if (mPrefs.getString(Constants.PREF_SIM2_CALLS[6], "0").equals("1"))
                                duration = (long) Math.ceil((double) duration / Constants.MINUTE) * Constants.MINUTE;
                            mCalls.put(Constants.CALLS2, duration + (long) mCalls.get(Constants.CALLS2));
                            break;
                        case Constants.SIM3:
                            mCalls.put(Constants.CALLS3_EX, duration + (long) mCalls.get(Constants.CALLS3_EX));
                            if (mPrefs.getString(Constants.PREF_SIM3_CALLS[6], "0").equals("1"))
                                duration = (long) Math.ceil((double) duration / Constants.MINUTE) * Constants.MINUTE;
                            mCalls.put(Constants.CALLS3, duration + (long) mCalls.get(Constants.CALLS3));
                            break;
                    }
                    MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(Constants.STARTED_ID, buildNotification());
                    Intent callsIntent = new Intent(Constants.CALLS);
                    callsIntent.putExtra(Constants.SIM_ACTIVE, sim);
                    callsIntent.putExtra(Constants.CALL_DURATION, duration);
                    sendBroadcast(callsIntent);
                    String out = "Call Ends\n";
                    try {
                        // to this path add a new directory path
                        File dir = new File(String.valueOf(context.getFilesDir()));
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
                    }
                }
            }
        };
        IntentFilter callDataFilter = new IntentFilter(Constants.OUTGOING_CALL);
        registerReceiver(callDataReceiver, callDataFilter);

        setUsageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mCalls == null)
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                Bundle limitBundle = intent.getBundleExtra("data");
                DateTime now = new DateTime();
                mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
                mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
                long mTotal = DataFormat.getDuration(limitBundle.getString("duration"), limitBundle.getInt("spinner"));
                switch (limitBundle.getInt("sim")) {
                    case Constants.SIM1:
                        mCalls.put(Constants.CALLS1, mTotal);
                        mCalls.put(Constants.CALLS1_EX, mTotal);
                        break;
                    case Constants.SIM2:
                        mCalls.put(Constants.CALLS2, mTotal);
                        mCalls.put(Constants.CALLS2_EX, mTotal);
                        break;
                    case Constants.SIM3:
                        mCalls.put(Constants.CALLS3, mTotal);
                        mCalls.put(Constants.CALLS3_EX, mTotal);
                        break;
                }
                MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(Constants.STARTED_ID, buildNotification());
            }
        };
        IntentFilter setUsageFilter = new IntentFilter(Constants.SET_DURATION);
        registerReceiver(setUsageReceiver, setUsageFilter);

        clearReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DateTime now = new DateTime();
                mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
                mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
                switch (intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED)) {
                    case Constants.SIM1:
                        mCalls.put(Constants.CALLS1, 0L);
                        mCalls.put(Constants.CALLS1_EX, 0L);
                        break;
                    case Constants.SIM2:
                        mCalls.put(Constants.CALLS2, 0L);
                        mCalls.put(Constants.CALLS2_EX, 0L);
                        break;
                    case Constants.SIM3:
                        mCalls.put(Constants.CALLS3, 0L);
                        mCalls.put(Constants.CALLS3_EX, 0L);
                        break;
                }
                MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
            }
        };
        IntentFilter clearSimDataFilter = new IntentFilter(Constants.CLEAR_CALLS);
        registerReceiver(clearReceiver, clearSimDataFilter);

        callDurationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                if (mIsOutgoing) {
                    final String[] out = {"Call Starts\n"};
                    mCalls = MyDatabase.readCallsData(mDatabaseHelper);
                    String lim, inter;
                    long currentDuration = 0;
                    int interval = 10;
                    long limit = Long.MAX_VALUE;
                    int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
                    DateTime now = new DateTime();
                    DateTime dt;
                    String lastDate = (String) mCalls.get(Constants.LAST_DATE);
                    if (lastDate.equals(""))
                        dt = now;
                    else
                        dt = fmtDate.parseDateTime(lastDate);
                    if (DateTimeComparator.getDateOnlyInstance().compare(now, dt) > 0 || mResetRuleHasChanged) {
                        mResetTime1 = getResetTime(Constants.SIM1);
                        if (mResetTime1 != null) {
                            mIsResetNeeded1 = true;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1_CALLS[9], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1_CALLS[8], mResetTime1.toString(fmtDateTime))
                                    .apply();
                        }
                        if (mSimQuantity >= 2) {
                            mResetTime2 = getResetTime(Constants.SIM2);
                            if (mResetTime2 != null) {
                                mIsResetNeeded2 = true;
                                mPrefs.edit()
                                        .putBoolean(Constants.PREF_SIM2_CALLS[9], mIsResetNeeded2)
                                        .putString(Constants.PREF_SIM2_CALLS[8], mResetTime2.toString(fmtDateTime))
                                        .apply();
                            }
                        }
                        if (mSimQuantity == 3) {
                            mResetTime3 = getResetTime(Constants.SIM3);
                            if (mResetTime3 != null) {
                                mIsResetNeeded3 = true;
                                mPrefs.edit()
                                        .putBoolean(Constants.PREF_SIM3_CALLS[9], mIsResetNeeded3)
                                        .putString(Constants.PREF_SIM3_CALLS[8], mResetTime3.toString(fmtDateTime))
                                        .apply();
                            }
                        }
                        mResetRuleHasChanged = false;
                    }
                    switch (sim) {
                        case Constants.SIM1:
                            if (DateTimeComparator.getInstance().compare(now, mResetTime1) >= 0 && mIsResetNeeded1) {
                                mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
                                mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
                                mCalls.put(Constants.CALLS1, 0L);
                                mCalls.put(Constants.CALLS1_EX, 0L);
                                MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                                mIsResetNeeded1 = false;
                                mPrefs.edit()
                                        .putBoolean(Constants.PREF_SIM1_CALLS[9], mIsResetNeeded1)
                                        .putString(Constants.PREF_SIM1_CALLS[8], mResetTime1.toString(fmtDateTime))
                                        .apply();
                            }
                            currentDuration = (long) mCalls.get(Constants.CALLS1);
                            lim = mPrefs.getString(Constants.PREF_SIM1_CALLS[1], "0");
                            inter = mPrefs.getString(Constants.PREF_SIM1_CALLS[3], "0");
                            if (!inter.equals(""))
                                interval = Integer.valueOf(inter) * Constants.SECOND;
                            if (!lim.equals(""))
                                limit = Long.valueOf(lim) * Constants.MINUTE;
                            break;
                        case Constants.SIM2:
                            if (DateTimeComparator.getInstance().compare(now, mResetTime2) >= 0 && mIsResetNeeded2) {
                                mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
                                mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
                                mCalls.put(Constants.CALLS2, 0L);
                                mCalls.put(Constants.CALLS3_EX, 0L);
                                MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                                mIsResetNeeded2 = false;
                                mPrefs.edit()
                                        .putBoolean(Constants.PREF_SIM2_CALLS[9], mIsResetNeeded2)
                                        .putString(Constants.PREF_SIM2_CALLS[8], mResetTime2.toString(fmtDateTime))
                                        .apply();
                            }
                            currentDuration = (long) mCalls.get(Constants.CALLS2);
                            lim = mPrefs.getString(Constants.PREF_SIM2_CALLS[1], "0");
                            inter = mPrefs.getString(Constants.PREF_SIM2_CALLS[3], "0");
                            if (!inter.equals(""))
                                interval = Integer.valueOf(inter) * Constants.SECOND;
                            if (!lim.equals(""))
                                limit = Long.valueOf(lim) * Constants.MINUTE;
                            break;
                        case Constants.SIM3:
                            if (DateTimeComparator.getInstance().compare(now, mResetTime3) >= 0 && mIsResetNeeded3) {
                                mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
                                mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
                                mCalls.put(Constants.CALLS3, 0L);
                                mCalls.put(Constants.CALLS3_EX, 0L);
                                MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
                                mIsResetNeeded3 = false;
                                mPrefs.edit()
                                        .putBoolean(Constants.PREF_SIM3_CALLS[9], mIsResetNeeded3)
                                        .putString(Constants.PREF_SIM3_CALLS[8], mResetTime3.toString(fmtDateTime))
                                        .apply();
                            }
                            currentDuration = (long) mCalls.get(Constants.CALLS3);
                            lim = mPrefs.getString(Constants.PREF_SIM3_CALLS[1], "0");
                            inter = mPrefs.getString(Constants.PREF_SIM3_CALLS[3], "0");
                            if (!inter.equals(""))
                                interval = Integer.valueOf(inter) * Constants.SECOND;
                            if (!lim.equals(""))
                                limit = Long.valueOf(lim) * Constants.MINUTE;
                            break;
                    }
                    long timeToVibrate;
                    if (limit - currentDuration <= interval)
                        timeToVibrate = 0;
                    else
                        timeToVibrate = limit - currentDuration - interval;
                    out[0] += String.valueOf(timeToVibrate / Constants.SECOND) + "\n";
                    mCountTimer = new android.os.CountDownTimer(timeToVibrate, Constants.SECOND) {
                        public void onTick(long millisUntilFinished) {
                            out[0] += String.valueOf(millisUntilFinished / Constants.SECOND) + "\n";
                        }

                        public void onFinish() {
                            if (mVibrator.hasVibrator())
                                vibrate(mVibrator, Constants.SECOND, Constants.SECOND / 2);
                            out[0] += "Limit reached\n";
                        }
                    }.start();
                    try {
                        // to this path add a new directory path
                        File dir = new File(String.valueOf(context.getFilesDir()));
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
                    }
                }
            }
        };
        IntentFilter callDurationFilter = new IntentFilter(Constants.OUTGOING_CALL_COUNT);
        registerReceiver(callDurationReceiver, callDurationFilter);

        outgoingCallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                    final Context ctx = context;
                    number[0] = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER).replaceAll("[\\s\\-()]", "");
                    //number[0] = MobileUtils.getFullNumber(ctx, intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
                    final TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
                    tm.listen(new PhoneStateListener() {
                        @Override
                        public void onCallStateChanged(int state, String incomingNumber) {
                            if (!mIsOutgoing)
                                switch (state) {
                                    case TelephonyManager.CALL_STATE_RINGING:
                                        mIsOutgoing = false;
                                        break;
                                    case TelephonyManager.CALL_STATE_OFFHOOK:
                                        final int sim = MobileUtils.getSimId(ctx);
                                        String out = sim + " " + number[0] + "\n";
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
                                        }
                                        final ArrayList<String> whiteList = MyDatabase.readWhiteList(sim, mDatabaseHelper);
                                        final ArrayList<String> blackList = MyDatabase.readBlackList(sim, mDatabaseHelper);
                                        if (!whiteList.contains(number[0]) && !blackList.contains(number[0]) && !mIsDialogShown) {
                                            mIsDialogShown = true;
                                            Dialog dialog = new AlertDialog.Builder(ctx)
                                                    .setTitle(number[0])
                                                    .setMessage(R.string.is_out_of_home_network)
                                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            mIsOutgoing = true;
                                                            blackList.add(number[0]);
                                                            MyDatabase.writeBlackList(sim, blackList, mDatabaseHelper);
                                                            dialog.dismiss();
                                                        }
                                                    })
                                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            mIsOutgoing = false;
                                                            whiteList.add(number[0]);
                                                            MyDatabase.writeWhiteList(sim, whiteList, mDatabaseHelper);
                                                            dialog.dismiss();
                                                        }
                                                    })
                                                    .create();
                                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                            dialog.show();
                                        } else if (blackList.contains(number[0]))
                                            mIsOutgoing = true;
                                        break;
                                    case TelephonyManager.CALL_STATE_IDLE:
                                        mIsOutgoing = false;
                                        break;
                                    default:
                                        break;
                                }
                        }
                    }, PhoneStateListener.LISTEN_CALL_STATE);
                }
            }
        };
        IntentFilter outgoingCallFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(outgoingCallReceiver, outgoingCallFilter);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_SIM1_CALLS[2]) || key.equals(Constants.PREF_SIM1_CALLS[4]) || key.equals(Constants.PREF_SIM1[4]) ||
                key.equals(Constants.PREF_SIM2_CALLS[2]) || key.equals(Constants.PREF_SIM2_CALLS[4]) || key.equals(Constants.PREF_SIM2_CALLS[5]) ||
                key.equals(Constants.PREF_SIM3_CALLS[2]) || key.equals(Constants.PREF_SIM3_CALLS[4]) || key.equals(Constants.PREF_SIM3_CALLS[5]))
            mResetRuleHasChanged = true;
        if (key.equals(Constants.PREF_OTHER[5]) && sharedPreferences.getBoolean(key, false)) {
            CountDownTimer timer = new CountDownTimer(2000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(Constants.STARTED_ID, buildNotification());
                }
            };
            timer.start();
        }
        if (key.equals(Constants.PREF_OTHER[12])) {
            MyNotification.setPriorityNeedsChange(true);
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(Constants.STARTED_ID, buildNotification());
        }
    }

    private DateTime getResetTime(int sim) {
        DateTime now = new DateTime().withTimeAtStartOfDay();
        String[] pref = new String[10];
        int delta = 0;
        String period = "";
        switch (sim) {
            case Constants.SIM1:
                pref = Constants.PREF_SIM1_CALLS;
                period = Constants.PERIOD1;
                break;
            case Constants.SIM2:
                pref = Constants.PREF_SIM2_CALLS;
                period = Constants.PERIOD2;
                break;
            case Constants.SIM3:
                pref = Constants.PREF_SIM3_CALLS;
                period = Constants.PERIOD3;
                break;
        }
        DateTime last;
        String date = mPrefs.getString(pref[8], "");
        if (!date.equals(""))
            last = fmtDateTime.parseDateTime(date).withTimeAtStartOfDay();
        else
            last = fmtDate.parseDateTime("1970-01-01");
        switch (mPrefs.getString(pref[2], "")) {
            case "0":
                delta = 1;
                break;
            case "1":
                delta = Integer.parseInt(mPrefs.getString(pref[5], "1"));
                if (delta >= 28)
                    switch (now.getMonthOfYear()) {
                        case 2:
                            if (now.year().isLeap())
                                delta = 29;
                            else
                                delta = 28;
                            break;
                        case 4:
                        case 6:
                        case 9:
                        case 11:
                            if (delta == 31)
                                delta = 30;
                            break;
                    }
                break;
            case "2":
                delta = Integer.parseInt(mPrefs.getString(pref[5], "1"));
                break;
        }
        int diff = Days.daysBetween(last.toLocalDate(), now.toLocalDate()).getDays();
        if (mPrefs.getString(pref[2], "").equals("1")) {
            int month = now.getMonthOfYear();
            int daysInMonth = 31;
            switch (last.getMonthOfYear()) {
                case 2:
                    if (last.year().isLeap())
                        daysInMonth = 29;
                    else
                        daysInMonth = 28;
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    daysInMonth = 30;
                    break;
            }
            if (now.getDayOfMonth() > delta && diff < daysInMonth)
                month += 1;
            date = now.getYear() + "-" + month + "-" + delta;
            return fmtDateTime.parseDateTime(date + " " + mPrefs.getString(pref[4], "00:00"));
        } else {
            if (mPrefs.getString(pref[2], "").equals("2"))
                mCalls.put(period, diff);
            if (diff >= delta) {
                if (mPrefs.getString(pref[3], "").equals("2"))
                    mCalls.put(period, 0);
                return fmtDateTime.parseDateTime(now.toString(fmtDate) + " " + mPrefs.getString(pref[4], "00:00"));
            } else
                return null;
        }
    }

    private static void vibrate(Vibrator v, int v1, int p1) {
        long[] pattern = new long[] {0, v1, p1};
        v.vibrate(pattern, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Constants.STARTED_ID, buildNotification());
        return START_STICKY;
    }

    private Notification buildNotification() {
        String text = DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS1));
        if (mSimQuantity >= 2)
            text += "  ||  " + DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS2));
        if (mSimQuantity == 3)
            text += "  ||  " + DataFormat.formatCallDuration(mContext, (long) mCalls.get(Constants.CALLS3));
        return MyNotification.getNotification(mContext, "", text);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(callDataReceiver);
        unregisterReceiver(clearReceiver);
        unregisterReceiver(setUsageReceiver);
        unregisterReceiver(callDurationReceiver);
        unregisterReceiver(outgoingCallReceiver);
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(Constants.STARTED_ID);
        MyDatabase.writeCallsData(mCalls, mDatabaseHelper);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    public static Context getCallLoggerServiceContext() {
        return CallLoggerService.mContext;
    }

    private static int[] getWidgetIds(Context context) {
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, CallsInfoWidget.class));
        if (ids.length == 0) {
            try {
                File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
                String[] children = dir.list();
                int i = 0;
                for (String aChildren : children) {
                    String[] str = aChildren.split("_");
                    if (str.length > 0 && str[1].equalsIgnoreCase("traffic") && str[2].equalsIgnoreCase("widget")) {
                        ids[i] = Integer.valueOf(aChildren.split("_")[0]);
                        i++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ids;
    }

}