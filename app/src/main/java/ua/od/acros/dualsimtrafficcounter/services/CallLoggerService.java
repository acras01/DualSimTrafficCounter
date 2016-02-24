package ua.od.acros.dualsimtrafficcounter.services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.ClearCallsEvent;
import ua.od.acros.dualsimtrafficcounter.events.DurationCallEvent;
import ua.od.acros.dualsimtrafficcounter.events.ProcessCallEvent;
import ua.od.acros.dualsimtrafficcounter.events.SetCallsEvent;
import ua.od.acros.dualsimtrafficcounter.events.NewOutgoingCallEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.DateUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MyDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.MyNotification;
import ua.od.acros.dualsimtrafficcounter.widgets.CallsInfoWidget;

public class CallLoggerService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static Context mContext;
    private MyDatabaseHelper mDatabaseHelper;
    private ContentValues mCalls;
    private DateTimeFormatter fmtDate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
    private DateTimeFormatter fmtTime = DateTimeFormat.forPattern(Constants.TIME_FORMAT + ":ss");
    private DateTimeFormatter fmtDateTime = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT);
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
    private boolean mLimitHasChanged;
    private long[] mLimits = new long[3];

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
        mContext = getApplicationContext();
        EventBus.getDefault().register(this);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mDatabaseHelper = MyDatabaseHelper.getInstance(mContext);
        mPrefs = getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mCalls = MyDatabaseHelper.readCallsData(mDatabaseHelper);
        if (mCalls.get(Constants.LAST_DATE).equals("")) {
            DateTime dateTime = new DateTime();
            mCalls.put(Constants.LAST_TIME, dateTime.toString(fmtTime));
            mCalls.put(Constants.LAST_DATE, dateTime.toString(fmtDate));
        }
        mLimits = getSIMLimits();
        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        mSimQuantity = mPrefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(mContext)
                : Integer.valueOf(mPrefs.getString(Constants.PREF_OTHER[14], "1"));
    }

    @Subscribe
    public void onMessageEvent(DurationCallEvent event) {
        if (mIsOutgoing) {
            mIsDialogShown = false;
            mIsOutgoing = false;
            if (mCountTimer != null)
                mCountTimer.cancel();
            mVibrator.cancel();
            int sim = event.sim;
            long duration = event.duration;
            Toast.makeText(mContext, mOperatorNames[sim] + ": " +
                    DataFormat.formatCallDuration(mContext, duration), Toast.LENGTH_LONG).show();
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
            MyDatabaseHelper.writeCallsData(mCalls, mDatabaseHelper);
            refreshWidgetAndNotification(mContext, sim, duration);
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
        }
    }

    @Subscribe
    public void onMessageEvent(ProcessCallEvent event) {
        if (mIsOutgoing) {
            //final String[] out = {"Call Starts\n"};
            mCalls = MyDatabaseHelper.readCallsData(mDatabaseHelper);
            String lim, inter;
            long currentDuration = 0;
            int interval = 10;
            long limit = Long.MAX_VALUE;
            int sim = event.sim;
            DateTime now = new DateTime();
            DateTime dt;
            String lastDate = (String) mCalls.get(Constants.LAST_DATE);
            if (lastDate.equals(""))
                dt = now;
            else
                dt = fmtDate.parseDateTime(lastDate);
            String[] simPref;
            if (DateTimeComparator.getDateOnlyInstance().compare(now, dt) > 0 || mResetRuleHasChanged) {
                simPref = new String[] {Constants.PREF_SIM1_CALLS[2], Constants.PREF_SIM1_CALLS[4],
                        Constants.PREF_SIM1_CALLS[5], Constants.PREF_SIM1_CALLS[8]};
                mResetTime1 = DateUtils.getResetTime(Constants.SIM1, mCalls, mPrefs, simPref);
                if (mResetTime1 != null) {
                    mIsResetNeeded1 = true;
                    mPrefs.edit()
                            .putBoolean(Constants.PREF_SIM1_CALLS[9], mIsResetNeeded1)
                            .putString(Constants.PREF_SIM1_CALLS[8], mResetTime1.toString(fmtDateTime))
                            .apply();
                }
                if (mSimQuantity >= 2) {
                    simPref = new String[] {Constants.PREF_SIM2_CALLS[2], Constants.PREF_SIM2_CALLS[4],
                            Constants.PREF_SIM2_CALLS[5], Constants.PREF_SIM2_CALLS[8]};
                    mResetTime2 = DateUtils.getResetTime(Constants.SIM2, mCalls, mPrefs, simPref);
                    if (mResetTime2 != null) {
                        mIsResetNeeded2 = true;
                        mPrefs.edit()
                                .putBoolean(Constants.PREF_SIM2_CALLS[9], mIsResetNeeded2)
                                .putString(Constants.PREF_SIM2_CALLS[8], mResetTime2.toString(fmtDateTime))
                                .apply();
                    }
                }
                if (mSimQuantity == 3) {
                    simPref = new String[] {Constants.PREF_SIM3_CALLS[2], Constants.PREF_SIM3_CALLS[4],
                            Constants.PREF_SIM3_CALLS[5], Constants.PREF_SIM3_CALLS[8]};
                    mResetTime3 = DateUtils.getResetTime(Constants.SIM3, mCalls, mPrefs, simPref);
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
                        MyDatabaseHelper.writeCallsData(mCalls, mDatabaseHelper);
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
                        MyDatabaseHelper.writeCallsData(mCalls, mDatabaseHelper);
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
                        MyDatabaseHelper.writeCallsData(mCalls, mDatabaseHelper);
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
            //out[0] += String.valueOf(timeToVibrate / Constants.SECOND) + "\n";
            mCountTimer = new android.os.CountDownTimer(timeToVibrate, Constants.SECOND) {
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

    @Subscribe
    public void onMessageEvent(NewOutgoingCallEvent event) {
        startTask(mContext, event.number);
    }

    @Subscribe
    public void onMessageEvent(SetCallsEvent event) {
        if (mCalls == null)
            mCalls = MyDatabaseHelper.readCallsData(mDatabaseHelper);
        DateTime now = new DateTime();
        mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
        mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
        int sim = event.sim;
        long duration = DataFormat.getDuration(event.calls, event.callsv);
        switch (sim) {
            case Constants.SIM1:
                mCalls.put(Constants.CALLS1, duration);
                mCalls.put(Constants.CALLS1_EX, duration);
                break;
            case Constants.SIM2:
                mCalls.put(Constants.CALLS2, duration);
                mCalls.put(Constants.CALLS2_EX, duration);
                break;
            case Constants.SIM3:
                mCalls.put(Constants.CALLS3, duration);
                mCalls.put(Constants.CALLS3_EX, duration);
                break;
        }
        MyDatabaseHelper.writeCallsData(mCalls, mDatabaseHelper);
        refreshWidgetAndNotification(mContext, sim, duration);
    }

    @Subscribe
    public void onMessageEvent(ClearCallsEvent event) {
        DateTime now = new DateTime();
        mCalls.put(Constants.LAST_DATE, now.toString(fmtDate));
        mCalls.put(Constants.LAST_TIME, now.toString(fmtTime));
        int sim = event.sim;
        switch (sim) {
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
        MyDatabaseHelper.writeCallsData(mCalls, mDatabaseHelper);
        refreshWidgetAndNotification(mContext, sim, 0L);
    }

    private void startTask(Context context, String number) {
        final Context ctx = context;
        this.number[0] = number.replaceAll("[\\s\\-()]", "");
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
                            /*String out = sim + " " + CallLoggerService.this.number[0] + "\n";
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
                            final ArrayList<String> whiteList = MyDatabaseHelper.readWhiteList(sim, mDatabaseHelper);
                            final ArrayList<String> blackList = MyDatabaseHelper.readBlackList(sim, mDatabaseHelper);
                            if (!whiteList.contains(CallLoggerService.this.number[0]) && !blackList.contains(CallLoggerService.this.number[0]) && !mIsDialogShown) {
                                mIsDialogShown = true;
                                Dialog dialog = new AlertDialog.Builder(ctx)
                                        .setTitle(CallLoggerService.this.number[0])
                                        .setMessage(R.string.is_out_of_home_network)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                mIsOutgoing = true;
                                                blackList.add(CallLoggerService.this.number[0]);
                                                MyDatabaseHelper.writeBlackList(sim, blackList, mDatabaseHelper);
                                                dialog.dismiss();
                                            }
                                        })
                                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                mIsOutgoing = false;
                                                whiteList.add(CallLoggerService.this.number[0]);
                                                MyDatabaseHelper.writeWhiteList(sim, whiteList, mDatabaseHelper);
                                                dialog.dismiss();
                                            }
                                        })
                                        .create();
                                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                dialog.show();
                            } else if (blackList.contains(CallLoggerService.this.number[0]))
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

    private void refreshWidgetAndNotification(Context context, int sim, long duration) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(Constants.STARTED_ID, buildNotification());
        int[] ids = getWidgetIds(context);
        if ((MyApplication.isActivityVisible() && MyApplication.isScreenOn(context)) || ids.length != 0) {
            Intent callsIntent = new Intent(Constants.CALLS_BROADCAST_ACTION);
            callsIntent.putExtra(Constants.SIM_ACTIVE, sim);
            callsIntent.putExtra(Constants.CALL_DURATION, duration);
            callsIntent.putExtra(Constants.WIDGET_IDS, ids);
            sendBroadcast(callsIntent);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_SIM1_CALLS[2]) || key.equals(Constants.PREF_SIM1_CALLS[4]) || key.equals(Constants.PREF_SIM1[5]) ||
                key.equals(Constants.PREF_SIM2_CALLS[2]) || key.equals(Constants.PREF_SIM2_CALLS[4]) || key.equals(Constants.PREF_SIM2_CALLS[5]) ||
                key.equals(Constants.PREF_SIM3_CALLS[2]) || key.equals(Constants.PREF_SIM3_CALLS[4]) || key.equals(Constants.PREF_SIM3_CALLS[5]))
            mResetRuleHasChanged = true;
        if (key.equals(Constants.PREF_SIM1_CALLS[1]) || key.equals(Constants.PREF_SIM2_CALLS[1]) || key.equals(Constants.PREF_SIM3_CALLS[1]))
            mLimitHasChanged = true;
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

    private static void vibrate(Vibrator v, int v1, int p1) {
        long[] pattern = new long[] {0, v1, p1};
        v.vibrate(pattern, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Constants.STARTED_ID, buildNotification());
        int[] ids = getWidgetIds(mContext);
        if (ids.length != 0) {
            Intent i = new Intent(Constants.CALLS_BROADCAST_ACTION);
            i.putExtra(Constants.WIDGET_IDS, ids);
            sendBroadcast(i);
        }
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
            startTask(mContext, intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
        return START_STICKY;
    }

    private Notification buildNotification() {
        long tot1, tot2 = 0, tot3 = 0;
        String text = "";
        if (mPrefs.getBoolean(Constants.PREF_OTHER[19], false)) {
            text = getString(R.string.remain_calls);
            if (mLimitHasChanged) {
                mLimits = getSIMLimits();
                mLimitHasChanged = false;
            }
            tot1 = mLimits[0] - (long) mCalls.get(Constants.CALLS1);
            if (mSimQuantity >= 2)
                tot2 = mLimits[1] - (long) mCalls.get(Constants.CALLS2);
            if (mSimQuantity == 3)
                tot3 = mLimits[2] - (long) mCalls.get(Constants.CALLS3);
        } else {
            tot1 = (long) mCalls.get(Constants.CALLS1);
            tot2 = (long) mCalls.get(Constants.CALLS2);
            tot3 = (long) mCalls.get(Constants.CALLS3);
        }

        if (mLimits[0] != Long.MAX_VALUE)
            text += DataFormat.formatCallDuration(mContext, tot1);
        else
            text += getString(R.string.not_set);
        if (mSimQuantity >= 2)
            if (mLimits[1] != Long.MAX_VALUE)
                text += "  ||  " + DataFormat.formatCallDuration(mContext, tot2);
            else
                text += "  ||  " + getString(R.string.not_set);
        if (mSimQuantity == 3)
            if (mLimits[2] != Long.MAX_VALUE)
                text += "  ||  " + DataFormat.formatCallDuration(mContext, tot3);
            else
                text += "  ||  " + getString(R.string.not_set);
        return MyNotification.getNotification(mContext, "", text);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(Constants.STARTED_ID);
        MyDatabaseHelper.writeCallsData(mCalls, mDatabaseHelper);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        EventBus.getDefault().unregister(this);
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
                    if (str.length > 0 && str[1].equalsIgnoreCase("calls") && str[2].equalsIgnoreCase("widget")) {
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

    private long[] getSIMLimits() {
        long lim1 = Long.MAX_VALUE;
        long lim2 = Long.MAX_VALUE;
        long lim3 = Long.MAX_VALUE;
        String limit1 = mPrefs.getString(Constants.PREF_SIM1_CALLS[1], "");
        String limit2 = mPrefs.getString(Constants.PREF_SIM2_CALLS[1], "");
        String limit3 = mPrefs.getString(Constants.PREF_SIM3_CALLS[1], "");
        if (!limit1.equals(""))
            lim1 = Long.valueOf(limit1) * Constants.MINUTE;
        if (!limit2.equals(""))
            lim2 = Long.valueOf(limit2) * Constants.MINUTE;
        if (!limit3.equals(""))
            lim3 = Long.valueOf(limit3) * Constants.MINUTE;

        return new long[] {lim1, lim2, lim3};
    }
}