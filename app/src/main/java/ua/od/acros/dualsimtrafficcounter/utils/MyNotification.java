package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;

public class MyNotification extends Notification implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static String mTraffic = "", mCalls = "";
    private static NotificationCompat.Builder mBuilder;
    private static int mId, mPriority, mActiveSIM;
    private static Context mContext;

    private static NotificationCompat.Builder newInstance() {
        if (mBuilder == null) {
            Intent notificationIntent = new Intent(mContext, MainActivity.class);
            notificationIntent.setAction("tap");
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
            mBuilder = new NotificationCompat.Builder(mContext)
                    .setContentIntent(contentIntent)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setLargeIcon(bm)
                    .setContentTitle(mContext.getString(R.string.app_name));
        }
        return mBuilder;
    }

    public static Notification getNotification(Context context, String traffic, String calls) {
        mContext = context.getApplicationContext();
        mActiveSIM = TrafficCountService.getActiveSIM();
        if (mActiveSIM == Constants.DISABLED)
            mActiveSIM = TrafficCountService.getLastActiveSIM();
        if (traffic.equals(""))
            traffic = mTraffic;
        if (calls.equals(""))
            calls = mCalls;
        String bigText;
        SharedPreferences prefs = mContext.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(Constants.PREF_OTHER[24], false))
            bigText = mContext.getString(R.string.traffic)  + "\n" + traffic + "\n" +
                    mContext.getString(R.string.calls) + "\n" + calls + "\n";
        else
            bigText = mContext.getString(R.string.traffic)  + "\n" + traffic;
        mTraffic = traffic;
        mCalls = calls;
        NotificationCompat.Builder b = newInstance();
        mId = getOperatorLogoID(mContext, mActiveSIM);
        b.setSmallIcon(mId);
        b.setPriority(mPriority);
        return new NotificationCompat.BigTextStyle(b).bigText(bigText).build();
    }

    private static int getOperatorLogoID (Context context, int sim) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.PREF_OTHER[15], false) && sim >= 0) {
            String[] pref = new String[27];
            switch (sim) {
                case Constants.SIM1:
                    pref = Constants.PREF_SIM1;
                    break;
                case Constants.SIM2:
                    pref = Constants.PREF_SIM2;
                    break;
                case Constants.SIM3:
                    pref = Constants.PREF_SIM3;
                    break;
            }
            if (prefs.getString(pref[23], "none").equals("auto"))
                return context.getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(context, sim), "drawable", context.getPackageName());
            else
                return context.getResources().getIdentifier(prefs.getString(pref[23], "logo_none"), "drawable", context.getPackageName());
        } else
            return R.drawable.ic_launcher_small;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[15]))
            if (sharedPreferences.getBoolean(key, false)) {
                String[] pref = new String[27];
                switch (mActiveSIM) {
                    case Constants.SIM1:
                        pref = Constants.PREF_SIM1;
                        break;
                    case Constants.SIM2:
                        pref = Constants.PREF_SIM2;
                        break;
                    case Constants.SIM3:
                        pref = Constants.PREF_SIM3;
                        break;
                }
                if (sharedPreferences.getString(pref[23], "none").equals("auto"))
                    mId = mContext.getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(mContext, mActiveSIM), "drawable", mContext.getPackageName());
                else
                    mId = mContext.getResources().getIdentifier(sharedPreferences.getString(pref[23], "none"), "drawable", mContext.getPackageName());
            } else
                mId = R.drawable.ic_launcher_small;
        if (sharedPreferences.getBoolean(Constants.PREF_OTHER[15], false)) {
            if (key.equals(Constants.PREF_SIM1[23]) || key.equals(Constants.PREF_SIM2[23]) ||
                    key.equals(Constants.PREF_SIM3[23]))
                if (sharedPreferences.getString(key, "none").equals("auto"))
                    mId = mContext.getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(mContext, mActiveSIM), "drawable", mContext.getPackageName());
                else
                    mId = mContext.getResources().getIdentifier(sharedPreferences.getString(key, "none"), "drawable", mContext.getPackageName());
        }
        if (key.equals(Constants.PREF_OTHER[12]))
            mPriority = sharedPreferences.getBoolean(key, true) ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_MIN;
    }
}
