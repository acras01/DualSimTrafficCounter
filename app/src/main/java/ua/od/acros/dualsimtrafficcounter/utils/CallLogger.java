package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CallLogger implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final String ENUM_PHONE_STATE = Build.VERSION.SDK_INT > 16 ?
            "com.android.internal.telephony.PhoneConstants$State" :
            "com.android.internal.telephony.Phone$State";
    private static final int CALL_STATE_ACTIVE = Build.VERSION.SDK_INT >= 22 ? 3 : 2;
    private static final String ENUM_CALL_STATE = "com.android.internal.telephony.Call$State";
    private static final String CLASS_ASYNC_RESULT = "android.os.AsyncResult";
    private static final String CLASS_CALL_NOTIFIER = "com.android.phone.CallNotifier";
    private static XSharedPreferences mXPrefs;
    public static final List<String> PACKAGE_NAMES = new ArrayList<>(Arrays.asList(
            "com.google.android.dialer", "com.android.dialer", "com.android.phone"));
    private static final String CLASS_IN_CALL_PRESENTER = "com.android.incallui.InCallPresenter";
    private static final String ENUM_IN_CALL_STATE = "com.android.incallui.InCallPresenter$InCallState";
    private static final String CLASS_CALL_LIST = "com.android.incallui.CallList";
    private static Object mPreviousCallState;
    private static Object mPrePreviousCallState;
    private static Context mContext;


    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        mXPrefs = new XSharedPreferences(Constants.APP_PREFERENCES);
        mXPrefs.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!PACKAGE_NAMES.contains(loadPackageParam.packageName))
            return;
        XposedBridge.log("Loaded app: " + loadPackageParam.packageName);
        mContext = AndroidAppHelper.currentApplication();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            final Class<?> classCallNotifier = XposedHelpers.findClass(CLASS_CALL_NOTIFIER, loadPackageParam.classLoader);
            final Class<? extends Enum> enumPhoneState = (Class<? extends Enum>) Class.forName(ENUM_PHONE_STATE);
            final Class<? extends Enum> enumCallState = (Class<? extends Enum>) Class.forName(ENUM_CALL_STATE);
            if (MTKUtils.isMtkDevice()) {
                XposedHelpers.findAndHookMethod(classCallNotifier, "onDisconnect",
                        CLASS_ASYNC_RESULT, int.class, onDisconnectHook);
            } else {
                XposedHelpers.findAndHookMethod(classCallNotifier, "onDisconnect",
                        CLASS_ASYNC_RESULT, onDisconnectHook);
            }
            XposedHelpers.findAndHookMethod(classCallNotifier, "onPhoneStateChanged", CLASS_ASYNC_RESULT, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Object cm = XposedHelpers.getObjectField(param.thisObject, "mCM");
                    final Object state = XposedHelpers.callMethod(cm, "getState");
                    if (state == Enum.valueOf(enumPhoneState, "OFFHOOK")) {
                        final Object fgPhone = XposedHelpers.callMethod(cm, "getFgPhone");
                        final Object activeCall = getCurrentCall(fgPhone);
                        final Object conn = getConnection(fgPhone, activeCall);
                        if (activeCall != null) {
                            if (XposedHelpers.callMethod(activeCall, "getState") == Enum.valueOf(enumCallState, "ACTIVE") &&
                                    !(Boolean) XposedHelpers.callMethod(conn, "isIncoming")) {
                                String imei = (String) XposedHelpers.callMethod(fgPhone, "getDeviceId");
                                XposedBridge.log("Outgoing call answered: " + imei);
                            }
                        }
                    }
                }
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                Class<?> mClassInCallPresenter = XposedHelpers.findClass(CLASS_IN_CALL_PRESENTER, loadPackageParam.classLoader);
                final Class<? extends Enum> enumInCallState = (Class<? extends Enum>) XposedHelpers.findClass(ENUM_IN_CALL_STATE,
                        loadPackageParam.classLoader);
                final long[] start = {0};
                final String[] imei = {" "};

                XposedBridge.hookAllMethods(mClassInCallPresenter, "setUp", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mPrePreviousCallState = null;
                        mPreviousCallState = null;

                    }
                });

                XposedBridge.hookAllMethods(mClassInCallPresenter, "onDisconnect", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (mPreviousCallState == Enum.valueOf(enumInCallState, "INCALL") &&
                                mPrePreviousCallState == Enum.valueOf(enumInCallState, "OUTGOING")) {
                            long durationMillis = System.currentTimeMillis() - start[0];
                            XposedBridge.log(imei[0] + " - Outgoing call ended: " + durationMillis / 1000 + "s");
                            Intent intent = new Intent(Constants.OUTGOING_CALL);
                            intent.putExtra(Constants.SIM_ACTIVE, imei);
                            intent.putExtra(Constants.CALL_DURATION, durationMillis);
                            mContext.sendBroadcast(intent);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(mClassInCallPresenter, "getPotentialStateFromCallList",
                        CLASS_CALL_LIST, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                Object state = param.getResult();
                                if (mPreviousCallState == null ||
                                        mPreviousCallState == Enum.valueOf(enumInCallState, "NO_CALLS")) {
                                    refreshPrefs();
                                }
                                if (state == Enum.valueOf(enumInCallState, "OUTGOING")) {
                                    mPrePreviousCallState = state;
                                }
                                if (state == Enum.valueOf(enumInCallState, "INCALL")) {
                                    Object activeCall = XposedHelpers.callMethod(param.args[0], "getActiveCall");
                                    if (activeCall != null) {
                                        final Object phone = XposedHelpers.callMethod(activeCall, "getPhone");
                                        final int callState = (Integer) XposedHelpers.callMethod(activeCall, "getState");
                                        final boolean activeOutgoing = (callState == CALL_STATE_ACTIVE &&
                                                mPreviousCallState == Enum.valueOf(enumInCallState, "OUTGOING"));
                                        if (activeOutgoing) {
                                            imei[0] = (String) XposedHelpers.callMethod(phone, "getDeviceId");
                                            XposedBridge.log("Outgoing call answered: " + imei[0]);
                                        }
                                    }
                                }
                                mPreviousCallState = state;
                            }
                        });
            } catch(Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    private static XC_MethodHook onDisconnectHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                refreshPrefs();
                Object conn = XposedHelpers.getObjectField(param.args[0], "result");
                if (conn != null && !(Boolean) XposedHelpers.callMethod(conn, "isIncoming")) {
                    Object call = XposedHelpers.callMethod(conn, "getCall");
                    Object phone = XposedHelpers.callMethod(call, "getPhone");
                    String imei = (String) XposedHelpers.callMethod(phone, "getDeviceId");
                    ArrayList<String> id = MobileUtils.getSimIMEI(mContext);
                    int sim = Constants.DISABLED;
                    for (int i = 0; i < id.size(); i++) {
                        if (imei.equals(id.get(i)))
                            sim = i;
                    }
                    long durationMillis = (long) XposedHelpers.callMethod(conn, "getDurationMillis");
                    XposedBridge.log(imei + " - Outgoing call ended: " + durationMillis / 1000 + "s");
                    Intent intent = new Intent(Constants.OUTGOING_CALL);
                    intent.putExtra(Constants.SIM_ACTIVE, sim);
                    intent.putExtra(Constants.CALL_DURATION, durationMillis);
                    mContext.sendBroadcast(intent);
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static Object getCurrentCall(Object phone) {
        try {
            Object ringing = XposedHelpers.callMethod(phone, "getRingingCall");
            Object fg = XposedHelpers.callMethod(phone, "getForegroundCall");
            Object bg = XposedHelpers.callMethod(phone, "getBackgroundCall");
            if (!(Boolean) XposedHelpers.callMethod(ringing, "isIdle")) {
                return ringing;
            }
            if (!(Boolean) XposedHelpers.callMethod(fg, "isIdle")) {
                return fg;
            }
            if (!(Boolean) XposedHelpers.callMethod(bg, "isIdle")) {
                return bg;
            }
            return fg;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    private static Object getConnection(Object phone, Object call) {
        if (call == null)
            return null;
        try {
            if ((Integer)XposedHelpers.callMethod(phone, "getPhoneType") ==
                    TelephonyManager.PHONE_TYPE_CDMA) {
                return XposedHelpers.callMethod(call, "getLatestConnection");
            }
            return XposedHelpers.callMethod(call, "getEarliestConnection");
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    private static void refreshPrefs() {
        if (mXPrefs != null) {
            mXPrefs.reload();
        }
    }
}
