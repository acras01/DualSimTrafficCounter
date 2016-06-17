package ua.od.acros.dualsimtrafficcounter.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.execution.Command;

import org.acra.ACRA;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.R;

public class MobileUtils {

    private static final String MEDIATEK = "com.mediatek.telephony.TelephonyManagerEx";
    private static final String GENERIC = "android.telephony.TelephonyManager";
    private static final String GET_NAME = "getNetworkOperatorName";
    private static final String GET_IMEI = "getDeviceId";
    private static final String GET_CODE = "getSimOperator";
    private static final String GET_CALL = "getCallState";
    private static final String GET_DATA = "getDataState";
    private static final String GET_SUBID = "getSubIdBySlot";
    private static int mLastActiveSIM;
    private static ArrayList<Long> mSubIds = null;
    private static Class<?> mTelephonyClass = null;
    private static Class<?> mSubscriptionManagerClass;
    private static Method mGetDeviceId = null;
    private static Method mGetNetworkOperatorName = null;
    private static Method mGetSimOperator = null;
    private static Method mGetCallState = null;
    private static Method mGetDataState = null;
    private static Method mGetSubIdBySlot = null;
    private static Method mGetITelephony = null;
    private static Method mFrom = null;
    private static Method mGetDefaultDataSubscriptionInfo = null;
    private static Method mGetDefaultDataSubId = null;
    private static Method mGetSimId = null;

    private static Method getMethod (Class c, String name, int params) {
        Method[] cm = c.getDeclaredMethods();
        for (Method m : cm) {
            if (m.getName().equalsIgnoreCase(name)) {
                m.setAccessible(true);
                if (m.getParameterTypes().length == params)
                    return m;
            }
        }
        return null;
    }

    public static int isMultiSim(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            if (sl != null)
                return sl.size();
            else
                return 0;
        } else {
            int simQuantity = 1;
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephonyClass == null)
                try {
                    mTelephonyClass = Class.forName(tm.getClass().getName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            if (mTelephonyClass != null) {
                if (CustomApplication.isOldMtkDevice()) {
                    try {
                        Class<?> c = Class.forName(MEDIATEK);
                        if (mGetDeviceId == null)
                            mGetDeviceId = getMethod(c, GET_IMEI, 1);
                        for (int i = 0; i < 2; i++) {
                            String id = (String) mGetDeviceId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i);
                            String idNext = (String) mGetDeviceId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i + 1);
                            if (idNext != null && !id.equals(idNext))
                                simQuantity++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (simQuantity == 1)
                        try {
                            Class<?> c = Class.forName(MEDIATEK);
                            if (mGetDeviceId == null) {
                                mGetDeviceId = getMethod(c, GET_IMEI, 1);
                            }
                            for (int i = 0; i < 2; i++) {
                                String id = (String) mGetDeviceId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i);
                                String idNext = (String) mGetDeviceId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i + 1);
                                if (idNext != null && !id.equals(idNext))
                                    simQuantity++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                } else {
                    try {
                        if (mGetSubIdBySlot == null)
                            mGetDeviceId = getMethod(mTelephonyClass, GET_IMEI, 1);
                        for (int i = 0; i < 2; i++) {
                            long id = (long) mGetSubIdBySlot.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i);
                            long idNext = (long) mGetSubIdBySlot.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i + 1);
                            if (idNext != 0 && id != idNext)
                                simQuantity++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (simQuantity == 1)
                        try {
                            if (mGetDeviceId == null)
                                mGetDeviceId = getMethod(mTelephonyClass, GET_IMEI + "Ext", 1);
                            for (int i = 0; i < 2; i++) {
                                String id = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i);
                                String idNext = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i + 1);
                                if (idNext != null && !id.equals(idNext))
                                    simQuantity++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    if (simQuantity == 1)
                        try {
                            if (mGetDeviceId == null)
                                mGetDeviceId = getMethod(mTelephonyClass, GET_IMEI, 1);
                            for (int i = 0; i < 2; i++) {
                                String id = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), (long) i);
                                String idNext = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), (long) (i + 1));
                                if (idNext != null && !id.equals(idNext))
                                    simQuantity++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    if (simQuantity == 1)
                        try {
                            if (mGetITelephony == null)
                                mGetITelephony = getMethod(mTelephonyClass, "getITelephony", 1);
                            for (int i = 0; i < 2; i++) {
                                Object mTelephonyStub = null;
                                if (mGetITelephony != null) {
                                    mTelephonyStub = mGetITelephony.invoke(tm, i);
                                }
                                if (mTelephonyStub != null)
                                    simQuantity++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    if (simQuantity == 1)
                        try {
                            if (mFrom == null)
                                mFrom = getMethod(mTelephonyClass, "from", 2);
                            for (int i = 0; i < 2; i++) {
                                final Object[] params = {context, i};
                                Object mTelephonyStub = null;
                                if (mFrom != null) {
                                    mTelephonyStub = mFrom.invoke(tm, params);
                                }
                                if (mTelephonyStub != null)
                                    simQuantity++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
            return simQuantity;
        }
    }

    public static int getActiveSIM(Context context) {
        String out = " ";
        if (mSubIds == null)
            mSubIds = new ArrayList<>();
        int sim = Constants.DISABLED;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (mSubscriptionManagerClass == null)
                try {
                    mSubscriptionManagerClass = Class.forName("android.telephony.SubscriptionManager");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            try {
                if (mGetDefaultDataSubscriptionInfo == null) {
                    Method[] cm = mSubscriptionManagerClass.getDeclaredMethods();
                    for (Method m : cm) {
                        if (m.getName().equalsIgnoreCase("getDefaultDataSubscriptionInfo")) {
                            m.setAccessible(true);
                            mGetDefaultDataSubscriptionInfo = m;
                            break;
                        }
                    }
                }
                SubscriptionInfo si = (SubscriptionInfo) mGetDefaultDataSubscriptionInfo.invoke(mSubscriptionManagerClass.getConstructor(android.content.Context.class).newInstance(context));
                sim = si.getSimSlotIndex();
                out = "getDefaultDataSubscriptionInfo " + sim;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (sim == Constants.DISABLED) {
                try {
                    if (mGetDefaultDataSubId == null) {
                        Method[] cm = mSubscriptionManagerClass.getDeclaredMethods();
                        for (Method m : cm) {
                            if (m.getName().equalsIgnoreCase("getDefaultDataSubId")) {
                                m.setAccessible(true);
                                mGetDefaultDataSubId = m;
                                break;
                            }
                        }
                    }
                    sim = (int) mGetDefaultDataSubId.invoke(mSubscriptionManagerClass.getConstructor(android.content.Context.class).newInstance(context)) - 1;
                    out = "getDefaultDataSubId " + sim;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (sim == Constants.DISABLED) {
                final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    SubscriptionManager sm = SubscriptionManager.from(context);
                    List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
                    if (sl != null)
                        for (int i = 0; i < sl.size(); i++) {
                            if (getNetworkFromApnsFile(String.valueOf(sl.get(i).getMcc()) + String.valueOf(sl.get(i).getMnc()), activeNetworkInfo.getExtraInfo())) {
                                sim = sl.get(i).getSimSlotIndex();
                                out = "getNetworkFromApnsFile " + sim;
                                break;
                            }
                        }
                }
            }
            if (sim == Constants.DISABLED) {
                try {
                    sim = Settings.Global.getInt(context.getContentResolver(), "multi_sim_data_call") - 1;
                    out = "getFromSettingsGlobal " + sim;
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? isMultiSim(context)
                    : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
            if (simQuantity > 1) {
                final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    Class c = null;
                    try {
                        c = Class.forName(activeNetworkInfo.getClass().getName());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (c != null)
                        try {
                            if (mGetSimId == null)
                                mGetSimId = getMethod(c, "getSimId", 0);
                            if (mGetSimId != null) {
                                sim = (int) mGetSimId.invoke(activeNetworkInfo);
                            }
                            out = "getSimId " + sim;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
                if (sim == Constants.DISABLED) {
                    if (CustomApplication.isOldMtkDevice()) {
                        for (int i = 0; i < simQuantity; i++) {
                            int state = Constants.DISABLED;
                            try {
                                Class<?> c = Class.forName(MEDIATEK);
                                if (mGetDataState == null)
                                    mGetDataState = getMethod(c, GET_DATA, 1);
                                state = (int) mGetDataState.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (state == TelephonyManager.DATA_CONNECTED
                                    || state == TelephonyManager.DATA_CONNECTING
                                    || state == TelephonyManager.DATA_SUSPENDED) {
                                sim = i;
                                out = "getDataStateExInt " + sim;
                                break;
                            }
                        }
                        if (sim == Constants.DISABLED) {
                            for (int i = 0; i < simQuantity; i++) {
                                int state = Constants.DISABLED;
                                try {
                                    Class<?> c = Class.forName(MEDIATEK);
                                    if (mGetDataState == null)
                                        mGetDataState = getMethod(c, GET_DATA, 1);
                                    state = (int) mGetDataState.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (state == TelephonyManager.DATA_CONNECTED
                                        || state == TelephonyManager.DATA_CONNECTING
                                        || state == TelephonyManager.DATA_SUSPENDED) {
                                    sim = i;
                                    out = "getDataStateExLong " + sim;
                                    break;
                                }
                            }
                        }
                    } else {
                        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                        if (mTelephonyClass == null)
                            try {
                                mTelephonyClass = Class.forName(tm.getClass().getName());
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        if (mTelephonyClass != null) {
                            for (int i = 0; i < simQuantity; i++) {
                                int state = Constants.DISABLED;
                                try {
                                    if (mSubIds == null)
                                        mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                    if (mGetDataState == null)
                                        mGetDataState = getMethod(mTelephonyClass, GET_DATA, 1);
                                    state = (int) mGetDataState.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), mSubIds.get(i));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (state == TelephonyManager.DATA_CONNECTED
                                        || state == TelephonyManager.DATA_CONNECTING
                                        || state == TelephonyManager.DATA_SUSPENDED) {
                                    sim = i;
                                    out = "getDataStateSubId " + sim;
                                    break;
                                }
                            }
                        }
                        if (sim == Constants.DISABLED) {
                            for (int i = 0; i < simQuantity; i++) {
                                int state = Constants.DISABLED;
                                try {
                                    if (mGetDataState == null)
                                        mGetDataState = getMethod(mTelephonyClass, GET_DATA + "Ext", 1);
                                    state = (int) mGetDataState.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (state == TelephonyManager.DATA_CONNECTED
                                        || state == TelephonyManager.DATA_CONNECTING
                                        || state == TelephonyManager.DATA_SUSPENDED) {
                                    sim = i;
                                    out = "getDataStateExt " + sim;
                                    break;
                                }
                            }
                        }
                        if (sim == Constants.DISABLED) {
                            for (int i = 0; i < simQuantity; i++) {
                                int state = Constants.DISABLED;
                                try {
                                    if (mGetITelephony == null)
                                        mGetITelephony = getMethod(mTelephonyClass, "getITelephony", 1);
                                    Object mTelephonyStub = null;
                                    if (mGetITelephony != null) {
                                        mTelephonyStub = mGetITelephony.invoke(tm, i);
                                    }
                                    Class<?> mTelephonyStubClass = null;
                                    if (mTelephonyStub != null) {
                                        mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
                                    }
                                    Class<?> mClass = null;
                                    if (mTelephonyStubClass != null) {
                                        mClass = mTelephonyStubClass.getDeclaringClass();
                                    }
                                    Method getState = null;
                                    if (mClass != null) {
                                        getState = mClass.getDeclaredMethod(GET_DATA);
                                    }
                                    if (getState != null) {
                                        state = (int) getState.invoke(mClass);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (state == TelephonyManager.DATA_CONNECTED
                                        || state == TelephonyManager.DATA_CONNECTING
                                        || state == TelephonyManager.DATA_SUSPENDED) {
                                    sim = i;
                                    out = "getITelephony " + sim;
                                    break;
                                }
                            }
                        }
                        if (sim == Constants.DISABLED) {
                            try {
                                if (mFrom == null)
                                    mFrom = getMethod(mTelephonyClass, "from", 2);
                                for (int i = 0; i < simQuantity; i++) {
                                    int state = Constants.DISABLED;
                                    final Object[] params = {context, i};
                                    TelephonyManager mTelephonyStub = null;
                                    if (mFrom != null) {
                                        mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                    }
                                    if (mTelephonyStub != null)
                                        state = mTelephonyStub.getDataState();
                                    if (state == TelephonyManager.DATA_CONNECTED
                                            || state == TelephonyManager.DATA_CONNECTING
                                            || state == TelephonyManager.DATA_SUSPENDED) {
                                        sim = i;
                                        out = "TelephonyManager.from " + sim;
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (sim == Constants.DISABLED && android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                            try {
                                sim = Settings.Global.getInt(context.getContentResolver(), "multi_sim_data_call") - 1;
                                out = "getFromSettingsGlobal " + sim;
                            } catch (Settings.SettingNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else
                sim = Constants.SIM1;
        }
        try {
            // to this path add a new directory path
            File dir = new File(String.valueOf(context.getFilesDir()));
            // create the file in which we will write the contents
            String fileName = "sim_log.txt";
            File file = new File(dir, fileName);
            FileOutputStream os = new FileOutputStream(file);
            os.write(out.getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sim;
    }

    public static int getSimId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyClass == null)
            try {
                mTelephonyClass = Class.forName(tm.getClass().getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        if (mTelephonyClass != null) {
                if (simQuantity > 1) {
                    int sim = Constants.DISABLED;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                        if (mGetCallState == null)
                            mGetCallState = getMethod(mTelephonyClass, GET_CALL, 1);
                        for (int i = 0; i < simQuantity; i++) {
                            try {
                                int state = (int) mGetCallState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i);
                                if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                    sim = i;
                                    break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                        if (CustomApplication.isOldMtkDevice()) {
                            try {
                                Class<?> c = Class.forName(MEDIATEK);
                                if (mGetCallState == null)
                                    mGetCallState = getMethod(c, GET_CALL, 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    try {
                                        int state = (int) mGetCallState.invoke(c.getConstructor(Context.class).newInstance(context), i);
                                        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                            sim = i;
                                            break;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (sim == Constants.DISABLED) {
                                try {
                                    Class<?> c = Class.forName(MEDIATEK);
                                    if (mGetCallState == null)
                                        mGetCallState = getMethod(c, GET_CALL, 1);
                                    for (int i = 0; i < simQuantity; i++) {
                                        try {
                                            int state = (int) mGetCallState.invoke(c.getConstructor(Context.class).newInstance(context), (long) i);
                                            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                                sim = i;
                                                break;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            if (mSubIds == null)
                                mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                            if (mGetCallState == null)
                                mGetCallState = getMethod(mTelephonyClass, GET_CALL, 1);
                            for (long subId : mSubIds) {
                                try {
                                    int state = (int) mGetCallState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), subId);
                                    if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                        sim = mSubIds.indexOf(subId);
                                        break;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (sim == Constants.DISABLED) {
                                if (mGetCallState == null)
                                    mGetCallState = getMethod(mTelephonyClass, GET_CALL + "Ext", 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    try {
                                        int state = (int) mGetCallState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i);
                                        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                            sim = i;
                                            break;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (sim == Constants.DISABLED) {
                                if (mGetITelephony == null)
                                    mGetITelephony = getMethod(mTelephonyClass, "getITelephony", 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    try {
                                        Object mTelephonyStub = null;
                                        if (mGetITelephony != null) {
                                            mTelephonyStub = mGetITelephony.invoke(tm, i);
                                        }
                                        Class<?> mTelephonyStubClass = null;
                                        if (mTelephonyStub != null) {
                                            mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
                                        }
                                        Class<?> mClass = null;
                                        if (mTelephonyStubClass != null) {
                                            mClass = mTelephonyStubClass.getDeclaringClass();
                                        }
                                        Method getState = null;
                                        if (mClass != null) {
                                            getState = mClass.getDeclaredMethod(GET_CALL);
                                        }
                                        int state = 0;
                                        if (getState != null) {
                                            state = (int) getState.invoke(mClass);
                                        }
                                        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                            sim = i;
                                            break;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (sim == Constants.DISABLED) {
                                try {
                                    if (mFrom == null)
                                        mFrom = getMethod(mTelephonyClass, "from", 2);
                                    if (mFrom != null)
                                        for (int i = 0; i < simQuantity; i++) {
                                            final Object[] params = {context, i};
                                            final TelephonyManager mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                            int state = mTelephonyStub.getCallState();
                                            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                                sim = i;
                                                break;
                                            }
                                        }
                                } catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    return sim;
                } else
                    return Constants.SIM1;
            } else
                return Constants.SIM1;
    }

    private static ArrayList<Long> getSubIds(Class<?> telephonyClass, int simQuantity, Context context) {
        ArrayList<Long> subIds = new ArrayList<>();
        try {
            if (mGetSubIdBySlot == null)
                mGetSubIdBySlot = getMethod(telephonyClass, GET_SUBID, 1);
            for (int i = 0; i < simQuantity; i++) {
                try {
                    subIds.add(i, (long) mGetSubIdBySlot.invoke(telephonyClass.getConstructor(Context.class).newInstance(context), i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subIds;
    }

    public static ArrayList<String> getOperatorNames(Context context) {
        String out = "";
        ArrayList<String> name = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            if (sl != null)
                for (SubscriptionInfo si : sl) {
                    name.add((String) si.getCarrierName());
                }
            if (name.size() > 0)
                out = "Subscription " + name.size();
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? isMultiSim(context)
                    : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephonyClass == null)
                try {
                    mTelephonyClass = Class.forName(tm.getClass().getName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            if (mTelephonyClass != null) {
                if (simQuantity > 1) {
                    if (CustomApplication.isOldMtkDevice()) {
                        try {
                            Class<?> c = Class.forName(MEDIATEK);
                            if (mGetNetworkOperatorName == null)
                                mGetNetworkOperatorName = getMethod(c, GET_NAME, 1);
                            for (int i = 0; i < simQuantity; i++) {
                                name.add(i, (String) mGetNetworkOperatorName.invoke(c.getConstructor(Context.class).newInstance(context), i));
                            }
                            if (name.size() > 0)
                                out = GET_NAME + "GeminiInt " + name.size();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (name.size() == 0) {
                            try {
                                Class<?> c = Class.forName(MEDIATEK);
                                if (mGetNetworkOperatorName == null)
                                    mGetNetworkOperatorName = getMethod(c, GET_NAME, 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    name.add(i, (String) mGetNetworkOperatorName.invoke(c.getConstructor(Context.class).newInstance(context), (long) i));
                                }
                                if (name.size() > 0)
                                    out = GET_NAME + "GeminiLong " + name.size();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (name.size() == 0) {
                            try {
                                if (mSubIds == null)
                                    mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                if (mGetNetworkOperatorName == null)
                                    mGetNetworkOperatorName = getMethod(mTelephonyClass, GET_NAME, 1);
                                for (long subId : mSubIds) {
                                    String nameCurr = (String) mGetNetworkOperatorName.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), subId);
                                    if (!nameCurr.equals(""))
                                        name.add(nameCurr);
                                }
                                if (name.size() > 0)
                                    out = GET_NAME + " " + name.size();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (name.size() == 0) {
                            try {
                                if (mGetNetworkOperatorName == null)
                                    mGetNetworkOperatorName = getMethod(mTelephonyClass, GET_NAME + "Ext", 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    name.add(i, (String) mGetNetworkOperatorName.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i));
                                }
                                if (name.size() > 0)
                                    out = GET_NAME + "Ext " + name.size();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (name.size() == 0) {
                            try {
                                if (mFrom == null)
                                    mFrom = getMethod(mTelephonyClass, "from", 2);
                                for (int i = 0; i < simQuantity; i++) {
                                    final Object[] params = {context, i};
                                    TelephonyManager mTelephonyStub = null;
                                    if (mFrom != null) {
                                        mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                    }
                                    if (mTelephonyStub != null)
                                        name.add(i, mTelephonyStub.getNetworkOperatorName());
                                }
                                if (name.size() > 0)
                                    out = "from " + name.size();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else
                    name.add(tm.getNetworkOperatorName());
            }
        }
        try {
            File dir = new File(String.valueOf(context.getFilesDir()));
            // create the file in which we will write the contents
            String fileName = "name_log.txt";
            File file = new File(dir, fileName);
            FileOutputStream os = new FileOutputStream(file);
            os.write(out.getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }

    public static ArrayList<String> getOperatorCodes(Context context) {
        ArrayList<String> code = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            if (sl != null)
                for (SubscriptionInfo si : sl) {
                    code.add(String.valueOf(si.getMcc()) + String.valueOf(si.getMnc()));
                }
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? isMultiSim(context)
                    : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephonyClass == null)
            try {
                mTelephonyClass = Class.forName(tm.getClass().getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (mTelephonyClass != null) {
                if (simQuantity > 1) {
                    if (CustomApplication.isOldMtkDevice()) {
                        try {
                            Class<?> c = Class.forName(MEDIATEK);
                            if (mGetSimOperator == null)
                                mGetSimOperator = getMethod(c, GET_CODE, 1);
                            for (int i = 0; i < simQuantity; i++) {
                                code.add(i, (String) mGetSimOperator.invoke(c.getConstructor(Context.class).newInstance(context), i));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (code.size() == 0) {
                            try {
                                Class<?> c = Class.forName(MEDIATEK);
                                if (mGetSimOperator == null)
                                    mGetSimOperator = getMethod(c, GET_CODE, 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    code.add(i, (String) mGetSimOperator.invoke(c.getConstructor(Context.class).newInstance(context), (long) i));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (code.size() == 0) {
                            try {
                                if (mGetSimOperator == null)
                                    mGetSimOperator = getMethod(mTelephonyClass, GET_CODE, 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    code.add(i, (String) mGetSimOperator.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (code.size() == 0) {
                            try {
                                if (mSubIds == null)
                                    mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                if (mGetSimOperator == null)
                                    mGetSimOperator = getMethod(mTelephonyClass, GET_CODE, 1);
                                for (long subId : mSubIds) {
                                    String codeCurr = (String) mGetSimOperator.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), subId);
                                    if (!codeCurr.equals(""))
                                        code.add(codeCurr);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (code.size() == 0) {
                            try {
                                if (mFrom == null)
                                    mFrom = getMethod(mTelephonyClass, "from", 2);
                                for (int i = 0; i < simQuantity; i++) {
                                    final Object[] params = {context, i};
                                    TelephonyManager mTelephonyStub = null;
                                    if (mFrom != null) {
                                        mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                    }
                                    if (mTelephonyStub != null)
                                        code.add(i, mTelephonyStub.getSimOperator());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else
                    code.add(tm.getSimOperator());
            }
        }
        return code;
    }

    public static ArrayList<String> getSimIMEI(Context context) {
        ArrayList<String> imei = new ArrayList<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int simQuantity = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            for (int i = 0; i < simQuantity; i++) {
                imei.add(i, tm.getDeviceId(i));
            }
        else if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                if (mTelephonyClass == null)
                    mTelephonyClass = Class.forName(tm.getClass().getName());
                if (mGetDeviceId == null)
                    mGetDeviceId = getMethod(mTelephonyClass, GET_IMEI, 1);
                for (int i = 0; i < simQuantity; i++) {
                    imei.add(i, (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (mTelephonyClass == null)
                try {
                    mTelephonyClass = Class.forName(tm.getClass().getName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            if (mTelephonyClass != null) {
                if (simQuantity > 1) {
                    if (CustomApplication.isOldMtkDevice()) {
                        try {
                            Class<?> c = Class.forName(MEDIATEK);
                            if (mGetDeviceId == null)
                                mGetDeviceId = getMethod(c, GET_IMEI, 1);
                            for (int i = 0; i < simQuantity; i++) {
                                imei.add(i, (String) mGetDeviceId.invoke(c.getConstructor(Context.class).newInstance(context), i));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (imei.size() == 0) {
                            try {
                                Class<?> c = Class.forName(MEDIATEK);
                                if (mGetDeviceId == null)
                                    mGetDeviceId = getMethod(c, GET_IMEI, 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    imei.add(i, (String) mGetDeviceId.invoke(c.getConstructor(Context.class).newInstance(context), (long) i));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (imei.size() == 0) {
                            try {
                                if (mSubIds == null)
                                    mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                if (mGetDeviceId == null)
                                    mGetDeviceId = getMethod(mTelephonyClass, GET_IMEI, 1);
                                for (long subId : mSubIds) {
                                    String imeiCurr = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), subId);
                                    if (!imeiCurr.equals(""))
                                        imei.add(imeiCurr);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (imei.size() == 0) {
                            try {
                                if (mGetDeviceId == null)
                                    mGetDeviceId = getMethod(mTelephonyClass, GET_IMEI + "Ext", 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    imei.add(i, (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (imei.size() == 0) {
                            try {
                                Method[] cm = mTelephonyClass.getDeclaredMethods();
                                for (Method m : cm) {
                                    if (m.getName().equalsIgnoreCase("getSubscriberInfo")) {
                                        m.setAccessible(true);
                                        m.getParameterTypes();
                                        if (m.getParameterTypes().length == 1) {
                                            for (int i = 0; i < simQuantity; i++) {
                                                final Object mTelephonyStub = m.invoke(tm, i);
                                                final Class<?> mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
                                                final Class<?> mClass = mTelephonyStubClass.getDeclaringClass();
                                                Method getId = mClass.getDeclaredMethod(GET_IMEI);
                                                imei.add(i, (String) getId.invoke(mClass));
                                            }
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (imei.size() == 0) {
                            try {
                                if (mFrom == null)
                                    mFrom = getMethod(mTelephonyClass, "from", 2);
                                for (int i = 0; i < simQuantity; i++) {
                                    final Object[] params = {context, i};
                                    TelephonyManager mTelephonyStub = null;
                                    if (mFrom != null) {
                                        mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                    }
                                    if (mTelephonyStub != null)
                                        imei.add(i, mTelephonyStub.getDeviceId());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else
                    imei.add(tm.getDeviceId());
            }
        }
        return imei;
    }

    private static boolean getNetworkFromDB(Context context, String code, String apn) {
        boolean operatorFound = false;
        final Uri APN_TABLE_URI = Uri.parse("content://telephony/carriers");
        context.enforceCallingOrSelfPermission("android.permission.WRITE_APN_SETTINGS", "No permission to write APN settings");
        Cursor cursor = context.getContentResolver().query(APN_TABLE_URI, new String[]{"_id", "numeric"}, "apn=?", new String[]{apn}, null);
        if (cursor != null) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                if (cursor.getString(cursor.getColumnIndex("numeric")).equals(code))
                    operatorFound = true;
                cursor.moveToNext();
            }
            cursor.close();
        }
        return operatorFound;
    }

    private static boolean getNetworkFromApnsFile(String code, String apn) {
        FileReader reader = null;
        boolean operatorFound = false;
        try {
            reader = new FileReader("/etc/apns-conf.xml");
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(reader);
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("apn")) {
                    HashMap<String, String> attributes = new HashMap<>();
                    for (int i = 0; i < xpp.getAttributeCount(); i++) {
                        attributes.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
                    }
                    if (attributes.containsKey("mcc") && attributes.containsKey("mnc") && code.equals(attributes.get("mcc")+attributes.get("mnc"))) {
                        if (!TextUtils.isEmpty(apn) && apn.equals(attributes.get("apn"))) {
                            operatorFound = true;
                            break;
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (XmlPullParserException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return operatorFound;
    }

    private static class Wrapper {
        public Context context;
        public int result;

        private Wrapper(Context ctx, int res) {
            this.context = ctx;
            this.result = res;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class SetMobileNetworkFromLollipop extends AsyncTask<Object, Void, Wrapper> {

        @Override
        protected Wrapper doInBackground(Object... params) {
            Context context = (Context) params[0];
            int sim = (int) params[1];
            boolean swtch = (boolean) params[2];
            boolean oldState = isMobileDataActive(context);
            String command = null;
            final String[] out = {new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()).toString() + "\n"};
            out[0] += "Current state: " + oldState + " Requested state: " + swtch + "\n";
            try {
                if (oldState != swtch) {
                    int state = swtch ? 1 : 0;
                    // Get the value of the "TRANSACTION_setDataEnabled" field.
                    String transactionCode = getTransactionCode(context);
                    out[0] += transactionCode + "\n";
                    // Android 5.1+ (API 22) and later.
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                        List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
                        if (sl != null) {
                            out[0] += sl.toString() + "\n";
                            for (SubscriptionInfo si : sl) {
                                if (transactionCode != null && transactionCode.length() > 0 && si.getSimSlotIndex() == sim) {
                                    command = "service call phone " + transactionCode + " i32 " + si.getSubscriptionId() + " i32 " + state;
                                    break;
                                }
                            }
                        }
                    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                        // Android 5.0 (API 21) only.
                        if (transactionCode != null && transactionCode.length() > 0)
                            command = "service call phone " + transactionCode + " i32 " + state;
                    }
                    if (CustomApplication.hasRoot() && command != null) {
                        out[0] += command + "\n";
                        Command cmd = new Command(0, command) {
                            @Override
                            public void commandOutput(int id, String line) {
                                out[0] += id + " " + line + "\n";
                                super.commandOutput(id, line);
                            }

                            @Override
                            public void commandTerminated(int id, String reason) {
                                out[0] += id + " " + reason + "\n";
                            }

                            @Override
                            public void commandCompleted(int id, int exitcode) {
                                out[0] += id + " " + exitcode + "\n";
                            }
                        };
                        RootShell.getShell(true).add(cmd);
                        for (int i = 1; i < 31; i++) {
                            sleep(1000);
                            if (oldState != isMobileDataActive(context)) {
                                out[0] += i + " seconds\n";
                                break;
                            }
                        }
                    } else
                        return new Wrapper(context, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                out[0] += e.toString() + "\n";
                ACRA.getErrorReporter().handleException(e);
                return new Wrapper(context, 2);
            }
            //Execution output
            try {
                File dir = new File(String.valueOf(context.getFilesDir()));
                // create the file in which we will write the contents
                String fileName = "setmobiledata.txt";
                File file = new File(dir, fileName);
                FileOutputStream os = new FileOutputStream(file, true);
                os.write(out[0].getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Wrapper(context, 0);
        }

        @Override
        protected void onPostExecute(Wrapper wrapper) {
            Context context = wrapper.context;
            int result = wrapper.result;
            switch (result) {
                case 0:
                    String out = "sim" + getActiveSIM(context) + " " + isMobileDataActive(context);
                    //Execution output
                    try {
                        File dir = new File(String.valueOf(context.getFilesDir()));
                        // create the file in which we will write the contents
                        String fileName = "setmobiledata.txt";
                        File file = new File(dir, fileName);
                        FileOutputStream os = new FileOutputStream(file, true);
                        out += "\n\n";
                        os.write(out.getBytes());
                        os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 1:
                    Toast.makeText(context, R.string.no_root_granted, Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(context, R.string.execution_failed, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private static String getTransactionCode(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephonyClass == null)
                try {
                    mTelephonyClass = Class.forName(tm.getClass().getName());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            final Method telephonyMethod = mTelephonyClass.getDeclaredMethod("getITelephony");
            telephonyMethod.setAccessible(true);
            final Object telephonyStub = telephonyMethod.invoke(tm);
            final Class<?> telephonyStubClass = Class.forName(telephonyStub.getClass().getName());
            final Class<?> c = telephonyStubClass.getDeclaringClass();
            final Field field = c.getDeclaredField("TRANSACTION_setDataEnabled");
            field.setAccessible(true);
            return String.valueOf(field.getInt(null));
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
            return null;
        }
    }

    private static void setMobileDataEnabled(Context context, boolean enabled, int sim) {
        try {
            final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field connectivityManagerField = conmanClass.getDeclaredField("mService");
            connectivityManagerField.setAccessible(true);
            final Object connectivityManager = connectivityManagerField.get(conman);
            final Class connectivityManagerClass = Class.forName(connectivityManager.getClass().getName());
            Method[] conmanMethods = connectivityManagerClass.getDeclaredMethods();
            for (Method m : conmanMethods) {
                if (m.getName().equals("setMobileDataEnabled")) {
                    m.setAccessible(true);
                    if (m.getParameterTypes().length == 1)
                        m.invoke(connectivityManager, enabled);
                    else if (m.getParameterTypes().length == 2) {
                        final Object[] params = {getSimIMEI(context).get(sim), enabled};
                        m.invoke(connectivityManager, params);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
    }

    public static boolean isMobileDataActive(Context context) {
        return hasActiveNetworkInfo(context) == 2 || isMobileDataEnabledFromSettings(context)
                || isMobileDataEnabledFromConnectivityManager(context) == 1;
    }

    private static boolean isMobileDataEnabledFromSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // The row has been moved to 'global' table in API level 17
            return Settings.Global.getInt(context.getContentResolver(), "mobile_data", 0) != 0;
        }
        try {
            // It was in 'secure' table before
            return Settings.Secure.getInt(context.getContentResolver(), "mobile_data") != 0;
        } catch (Settings.SettingNotFoundException e) {
            // It was in 'system' table originally, but I don't remember when that was the case.
            // So, probably, you won't need all these try/catches.
            // But, hey, it is better to be safe than sorry :)
            return Settings.System.getInt(context.getContentResolver(), "mobile_data", 0) != 0;
        }
    }

    private static int isMobileDataEnabledFromConnectivityManager(Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class<?> c = Class.forName(cm.getClass().getName());
            Method m;
            if (c != null) {
                m = getMethod(c, "getMobileDataEnabled", 0);
                if (m != null)
                    return (boolean) m.invoke(cm) ? 1 : 0;
                else
                    return 2;
            } else
                return 2;
        } catch (Exception e) {
            e.printStackTrace();
            return 2;
        }
    }

    public static int hasActiveNetworkInfo(Context context) {
        int state = 0; // Assume disabled
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mActiveNetworkInfo = cm.getActiveNetworkInfo();
        if (mActiveNetworkInfo != null) {
            String typeName = mActiveNetworkInfo.getTypeName().toLowerCase();
            boolean isConnected = mActiveNetworkInfo.isConnectedOrConnecting();
            int type = mActiveNetworkInfo.getType();
            if ((isNetworkTypeMobile(type)) && (typeName.contains("mobile")) && isConnected)
                state = 2;
            else if ((!isNetworkTypeMobile(type)) && (!typeName.contains("mobile")) && isConnected)
                state = 1;
        }
        return state;
    }

    private static boolean isNetworkTypeMobile(int networkType) {
        switch (networkType) {
            case 0:
            case 2:
            case 3:
            case 4:
            case 5:
            case 10:
            case 11:
            case 12:
            case 14:
            case 15:
                return true;
            default:
                return false;
        }
    }

    public static void toggleMobileDataConnection(boolean swtch, Context context, int sim) throws Exception {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mAlternative = prefs.getBoolean(Constants.PREF_OTHER[20], false);
        if (!swtch) {
            mLastActiveSIM = getActiveSIM(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                new SetMobileNetworkFromLollipop().execute(context, mLastActiveSIM, false);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && !CustomApplication.isOldMtkDevice()) {
                setMobileDataEnabled(context, false, mLastActiveSIM);
            } else {
                Intent localIntent = new Intent(Constants.DATA_DEFAULT_SIM);
                localIntent.putExtra("simid", Constants.DISABLED);
                context.sendBroadcast(localIntent);
            }
        }
        if (swtch && sim == Constants.DISABLED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isMobileDataActive(context))
                    new SetMobileNetworkFromLollipop().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context, mLastActiveSIM, false);
                new SetMobileNetworkFromLollipop().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context, mLastActiveSIM, true);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && !CustomApplication.isOldMtkDevice()) {
                setMobileDataEnabled(context, false, mLastActiveSIM);
            } else {
                Intent localIntent = new Intent(Constants.DATA_DEFAULT_SIM);
                if (mAlternative) {
                    int simAlt = Constants.DISABLED;
                    switch (mLastActiveSIM) {
                        case Constants.SIM1:
                            simAlt = prefs.getInt(Constants.PREF_OTHER[21], Constants.DISABLED);
                            break;
                        case Constants.SIM2:
                            simAlt = prefs.getInt(Constants.PREF_OTHER[22], Constants.DISABLED);
                            break;
                        case Constants.SIM3:
                            simAlt = prefs.getInt(Constants.PREF_OTHER[23], Constants.DISABLED);
                            break;
                    }
                    localIntent.putExtra("simid", (long) simAlt);
                } else
                    localIntent.putExtra("simid", (long) mLastActiveSIM);
                context.sendBroadcast(localIntent);
            }
            sleep(1000);
        } else if (sim != Constants.DISABLED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isMobileDataActive(context))
                    new SetMobileNetworkFromLollipop().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context, getActiveSIM(context), false);
                new SetMobileNetworkFromLollipop().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, context, sim, true);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && !CustomApplication.isOldMtkDevice()) {
                setMobileDataEnabled(context, false, sim);
            } else {
                Intent localIntent = new Intent(Constants.DATA_DEFAULT_SIM);
                if (mAlternative) {
                    int sim_ = Constants.DISABLED;
                    switch (sim) {
                        case Constants.SIM1:
                            sim_ = prefs.getInt(Constants.PREF_OTHER[21], Constants.DISABLED);
                            break;
                        case Constants.SIM2:
                            sim_ = prefs.getInt(Constants.PREF_OTHER[22], Constants.DISABLED);
                            break;
                        case Constants.SIM3:
                            sim_ = prefs.getInt(Constants.PREF_OTHER[23], Constants.DISABLED);
                            break;
                    }
                    localIntent.putExtra("simid", (long) sim_);
                } else
                    localIntent.putExtra("simid", (long) sim);
                context.sendBroadcast(localIntent);
            }
        }
        sleep(1000);
    }

    public static String getName(Context context, String key1, String key2, int sim) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(key1, true)) {
            ArrayList<String> names = getOperatorNames(context);
            return (names.size() > sim && names.get(sim) != null) ? names.get(sim) : context.getResources().getString(R.string.not_available);
        } else
            return prefs.getString(key2, "");
    }

    public static String getLogoFromCode(Context context, int sim) {
        if (sim >= 0) {
            ArrayList<String> opCodes = getOperatorCodes(context);
            if (opCodes.size() > sim && opCodes.get(sim) != null) {
                switch (opCodes.get(sim)) {
                    case "20416":
                    case "21901":
                    case "23001":
                    case "23102":
                    case "23203":
                    case "23430":
                    case "26201":
                    case "26206":
                    case "29401":
                    case "29702":
                    case "310160":
                    case "310200":
                    case "310210":
                    case "310220":
                    case "310230":
                    case "310240":
                    case "310250":
                    case "310260":
                    case "310270":
                    case "310490":
                    case "310580":
                    case "310660":
                    case "310800":
                        return "tmobile";
                    case "25044":
                    case "25099":
                    case "25502":
                    case "45207":
                    case "40101":
                    case "28204":
                    case "45609":
                    case "43605":
                    case "43404":
                        return "beeline";
                    case "25507":
                        return "trimob";
                    case "320370":
                    case "320720":
                    case "310170":
                    case "310150":
                    case "310680":
                    case "310070":
                    case "310560":
                    case "310410":
                    case "310380":
                    case "310980":
                    case "31038":
                        return "att";
                    case "20888":
                    case "20821":
                    case "20820":
                    case "34020":
                        return "bouygues_telecom";
                    case "46007":
                    case "46000":
                    case "46002":
                    case "45413":
                    case "45412":
                        return "china_mobile";
                    case "46003":
                    case "46005":
                    case "45502":
                        return "china_telecom";
                    case "46001":
                    case "46006":
                    case "45507":
                        return "china_unicom";
                    case "20201":
                    case "20202":
                    case "22603":
                        return "cosmote";
                    case "25503":
                        return "kyivstar";
                    case "25506":
                    case "28601":
                        return "turkcell";
                    case "25704":
                        return "life";
                    case "25002":
                        return "megafon";
                    case "25035":
                        return "motiv";
                    case "25050":
                    case "25001":
                    case "25702":
                    case "43801":
                    case "43407":
                        return "mts";
                    case "23002":
                    case "23010":
                    case "26207":
                    case "26208":
                    case "27202":
                    case "23106":
                    case "23402":
                    case "23410":
                    case "23411":
                        return "o2";
                    case "43709":
                        return "o";
                    case "28310":
                    case "23205":
                    case "23206":
                    case "65202":
                    case "26402":
                    case "62303":
                    case "61203":
                    case "63086":
                    case "37001":
                    case "20800":
                    case "20801":
                    case "20802":
                    case "74201":
                    case "61101":
                    case "63203":
                    case "42501":
                    case "41677":
                    case "29502":
                    case "27099":
                    case "64602":
                    case "61002":
                    case "34001":
                    case "340993":
                    case "61701":
                    case "25901":
                    case "61404":
                    case "26003":
                    case "64700":
                    case "647997":
                    case "22610":
                    case "60801":
                    case "23101":
                    case "23105":
                    case "21403":
                    case "21409":
                    case "21433":
                    case "22803":
                    case "60501":
                    case "64114":
                    case "23433":
                    case "23434":
                        return "orange";
                    case "26006":
                        return "play";
                    case "25007":
                    case "25015":
                        return "smarts";
                    case "25020":
                    case "24007":
                    case "22808":
                    case "21902":
                    case "24707":
                    case "24606":
                    case "20402":
                    case "24204":
                        return "tele2";
                    case "25701":
                        return "velcom";
                    case "31003":
                    case "31004":
                    case "31005":
                    case "31010":
                    case "31012":
                    case "310110":
                    case "310280":
                    case "310390":
                    case "310480":
                    case "310890":
                    case "310910":
                        return "verizon";
                    case "27602":
                    case "50503":
                    case "28001":
                    case "23003":
                    case "23099":
                    case "60202":
                    case "28802":
                    case "54201":
                    case "26202":
                    case "26209":
                    case "62002":
                    case "20205":
                    case "21670":
                    case "27402":
                    case "27403":
                    case "40401":
                    case "40405":
                    case "40411":
                    case "40413":
                    case "40415":
                    case "40420":
                    case "40427":
                    case "40430":
                    case "40443":
                    case "40446":
                    case "40460":
                    case "40566":
                    case "40567":
                    case "40484":
                    case "40486":
                    case "40488":
                    case "405750":
                    case "405751":
                    case "405752":
                    case "405753":
                    case "405754":
                    case "405755":
                    case "405756":
                    case "27201":
                    case "22210":
                    case "27801":
                    case "20404":
                    case "53001":
                    case "26801":
                    case "42702":
                    case "22601":
                    case "21401":
                    case "21406":
                    case "28602":
                    case "23403":
                    case "23415":
                    case "23491":
                    case "25501":
                        return "vodafone";
                    case "25011":
                        return "yota";
                    default:
                        return "none";
                }
            } else
                return "none";

        }
        return "none";
    }

    private static String getCountryZipCode(Context context){
        String CountryID;
        String CountryZipCode = "";

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //getNetworkCountryIso
        CountryID = tm.getSimCountryIso().toUpperCase();
        String[] rl = context.getResources().getStringArray(R.array.CountryCodes);
        for (String aRl : rl) {
            String[] g = aRl.split(",");
            if (g[1].trim().equals(CountryID.trim())) {
                CountryZipCode = g[0];
                break;
            }
        }
        return CountryZipCode;
    }

    public static String getFullNumber(Context context, String number) {
        String countryCode = getCountryZipCode(context);
        return number.replaceAll("[^0-9\\+]", "")        //remove all the non mNumbers (brackets dashes spaces etc.) except the + signs
                    .replaceAll("(^[1-9].+)", countryCode + "$1")         //if the number is starting with no zero and +, its a local number. prepend cc
                    .replaceAll("(.)(\\++)(.)", "$1$3")         //if there are left out +'s in the middle by mistake, remove them
                    .replaceAll("(^0{2}|^\\+)(.+)", "$2")       //make 00XXX... mNumbers and +XXXXX.. mNumbers into XXXX...
                    .replaceAll("^0([1-9])", countryCode + "$1");
    }

    public static void getTelephonyManagerMethods(Context context) {
        String out;
        try {
            File dir = new File(String.valueOf(context.getFilesDir()));
            // create the file in which we will write the contents
            String fileName = "telephony.txt";
            File file = new File(dir, fileName);
            FileOutputStream os = new FileOutputStream(file);
            Class<?> c = Class.forName(GENERIC);
            Method[] cm = c.getDeclaredMethods();
            for (Method m : cm) {
                out = m.toString() + "\n";
                os.write(out.getBytes());
            }
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void  sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}