package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CallLogger implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final String ENUM_PHONE_STATE = Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN ?
            "com.android.internal.telephony.PhoneConstants$State" :
            "com.android.internal.telephony.Phone$State";
    private static final String CLASS_CALLS_MANAGER = "com.android.server.telecom.CallsManager";
    private static final String CLASS_CALL = "com.android.server.telecom.Call";
    private static final String CLASS_PHONE_UTILS = " com.android.phone.PhoneUtils";
    private static final int CALL_STATE_ACTIVE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? 3 : 2;
    private static final String ENUM_CALL_STATE = "com.android.internal.telephony.Call$State";
    private static final String CLASS_ASYNC_RESULT = "android.os.AsyncResult";
    private static final String CLASS_CALL_NOTIFIER = "com.android.phone.CallNotifier";
    private static XSharedPreferences mXPrefs;
    private static final List<String> PACKAGE_NAMES = new ArrayList<>(Arrays.asList(
            "com.google.android.dialer", "com.android.dialer", "com.android.phone"));
    private static final String CLASS_IN_CALL_PRESENTER = "com.android.incallui.InCallPresenter";
    private static final String ENUM_IN_CALL_STATE = "com.android.incallui.InCallPresenter$InCallState";
    private static final String CLASS_CALL_LIST = "com.android.incallui.CallList";
    private static Object mPreviousCallState;
    private static Object mPrePreviousCallState;
    private Object mOutgoingCall;
    private HashMap<String, Object> mActiveCallList = new HashMap<>();
    private Bundle mActiveCallStartList = new Bundle();
    private Bundle mActiveCallSimList = new Bundle();

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        mXPrefs = new XSharedPreferences(Constants.APP_PREFERENCES);
        mXPrefs.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (PACKAGE_NAMES.contains(loadPackageParam.packageName)) {
            XposedBridge.log("Loaded app: " + loadPackageParam.packageName);
            final Class<?> classPhoneUtils = XposedHelpers.findClass(CLASS_PHONE_UTILS, loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(classPhoneUtils, "getInitialNumber", Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String number = ((Intent) param.args[0]).getStringExtra("android.phone.extra.ACTUAL_NUMBER_TO_DIAL");
                        Context context = AndroidAppHelper.currentApplication();
                        Intent intent = new Intent(Constants.NEW_OUTGOING_CALL);
                        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
                        context.sendBroadcast(intent);
                        XposedBridge.log(number);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                final Class<?> classCallNotifier = XposedHelpers.findClass(CLASS_CALL_NOTIFIER, loadPackageParam.classLoader);
                final Class<? extends Enum> enumPhoneState = (Class<? extends Enum>) Class.forName(ENUM_PHONE_STATE);
                final Class<? extends Enum> enumCallState = (Class<? extends Enum>) Class.forName(ENUM_CALL_STATE);
                if (CustomApplication.isOldMtkDevice()) {
                    XposedHelpers.findAndHookMethod(classCallNotifier, "onDisconnect",
                            CLASS_ASYNC_RESULT, int.class, onDisconnectHook);
                } else {
                    XposedHelpers.findAndHookMethod(classCallNotifier, "onDisconnect",
                            CLASS_ASYNC_RESULT, onDisconnectHook);
                }
                XposedHelpers.findAndHookMethod(classCallNotifier, "onPhoneStateChanged", CLASS_ASYNC_RESULT, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = AndroidAppHelper.currentApplication();
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
                                    ArrayList<String> id = MobileUtils.getDeviceIds(context);
                                    int sim = Constants.DISABLED;
                                    for (int i = 0; i < id.size(); i++) {
                                        if (imei.equals(id.get(i)))
                                            sim = i;
                                    }
                                    XposedBridge.log("Outgoing call answered: " + sim);
                                    Intent i = new Intent(Constants.OUTGOING_CALL_ANSWERED);
                                    i.putExtra(Constants.SIM_ACTIVE, sim);
                                    context.sendBroadcast(i);
                                }
                            }
                        }
                    }
                });
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                try {
                    Class<?> mClassInCallPresenter = XposedHelpers.findClass(CLASS_IN_CALL_PRESENTER, loadPackageParam.classLoader);
                    final Class<? extends Enum> enumInCallState = (Class<? extends Enum>) XposedHelpers.findClass(ENUM_IN_CALL_STATE,
                            loadPackageParam.classLoader);
                    final long[] start = {0};
                    final int[] sim = {0};
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
                                Context context = AndroidAppHelper.currentApplication();
                                long durationMillis = System.currentTimeMillis() - start[0];
                                XposedBridge.log(sim[0] + " - Outgoing call ended: " + durationMillis / 1000 + "s");
                                Intent i = new Intent(Constants.OUTGOING_CALL_ENDED);
                                i.putExtra(Constants.SIM_ACTIVE, sim[0]);
                                i.putExtra(Constants.CALL_DURATION, durationMillis);
                                context.sendBroadcast(i);
                            }
                        }
                    });
                    XposedHelpers.findAndHookMethod(mClassInCallPresenter, "getPotentialStateFromCallList",
                            CLASS_CALL_LIST, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    Context context = AndroidAppHelper.currentApplication();
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
                                            final int callState = (Integer) XposedHelpers.callMethod(activeCall, "getState");
                                            final boolean activeOutgoing = (callState == CALL_STATE_ACTIVE &&
                                                    mPreviousCallState == Enum.valueOf(enumInCallState, "OUTGOING"));
                                            if (activeOutgoing) {
                                                sim[0] = MobileUtils.getActiveSimForCall(context);
                                                start[0] = System.currentTimeMillis();
                                                XposedBridge.log("Outgoing call answered: " + sim[0]);
                                                Intent i = new Intent(Constants.OUTGOING_CALL_ANSWERED);
                                                i.putExtra(Constants.SIM_ACTIVE, sim[0]);
                                                context.sendBroadcast(i);
                                            }
                                        }
                                    }
                                    mPreviousCallState = state;
                                }
                            });
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            } else {
                Class<?> clsCallsManager = XposedHelpers.findClass(CLASS_CALLS_MANAGER, loadPackageParam.classLoader);
                XposedHelpers.findAndHookMethod(clsCallsManager, "setCallState", CLASS_CALL, int.class, String.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                onCallStateChanged(param.args[0], (int)param.args[1]);
                            }
                        });
            }
        }
    }

    private void onCallStateChanged(Object call, int state) {
        Context context = AndroidAppHelper.currentApplication();
        String key = call.toString();
        long start;
        int sim;
        // keep track of active calls
        if (state == CallState.ACTIVE && !mActiveCallList.containsValue(call)) {
            mActiveCallList.put(key, call);
        }
        // register outgoing call
        if (state == CallState.DIALING && mOutgoingCall == null) {
            mOutgoingCall = call;
        }
        // vibrate on outgoing connected and periodic
        if (state == CallState.ACTIVE && call == mOutgoingCall) {
            sim = MobileUtils.getActiveSimForCall(context);
            start = System.currentTimeMillis();
            mActiveCallSimList.putInt(key, sim);
            mActiveCallStartList.putLong(key, start);
            XposedBridge.log("Outgoing call answered: " + sim);
            Intent i = new Intent(Constants.OUTGOING_CALL_ANSWERED);
            i.putExtra(Constants.SIM_ACTIVE, sim);
            context.sendBroadcast(i);
        }
        // handle call disconnected
        if (state == CallState.DISCONNECTED && call == mOutgoingCall) {
            sim = mActiveCallSimList.getInt(key);
            start = mActiveCallStartList.getLong(key);
            long durationMillis = System.currentTimeMillis() - start;
            XposedBridge.log(sim + " - Outgoing call ended: " + durationMillis / 1000 + "s");
            Intent i = new Intent(Constants.OUTGOING_CALL_ENDED);
            i.putExtra(Constants.SIM_ACTIVE, sim);
            i.putExtra(Constants.CALL_DURATION, durationMillis);
            context.sendBroadcast(i);
        }
    }

    private static XC_MethodHook onDisconnectHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                refreshPrefs();
                Context context = AndroidAppHelper.currentApplication();
                Object conn = XposedHelpers.getObjectField(param.args[0], "result");
                if (conn != null && !(Boolean) XposedHelpers.callMethod(conn, "isIncoming")) {
                    Object call = XposedHelpers.callMethod(conn, "getCall");
                    Object phone = XposedHelpers.callMethod(call, "getPhone");
                    String imei = (String) XposedHelpers.callMethod(phone, "getDeviceId");
                    ArrayList<String> id = MobileUtils.getDeviceIds(context);
                    int sim = Constants.DISABLED;
                    for (int i = 0; i < id.size(); i++) {
                        if (imei.equals(id.get(i)))
                            sim = i;
                    }
                    long durationMillis = (long) XposedHelpers.callMethod(conn, "getDurationMillis");
                    XposedBridge.log(imei + " - Outgoing call ended: " + durationMillis / 1000 + "s");
                    Intent i = new Intent(Constants.OUTGOING_CALL_ENDED);
                    i.putExtra(Constants.SIM_ACTIVE, sim);
                    i.putExtra(Constants.CALL_DURATION, durationMillis);
                    context.sendBroadcast(i);
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
            if ((Integer) XposedHelpers.callMethod(phone, "getPhoneType") ==
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

    private class CallState {
        private CallState() {}

        /**
         * Indicates that a call is new and not connected. This is used as the default state internally
         * within Telecom and should not be used between Telecom and call services. Call services are
         * not expected to ever interact with NEW calls, but {@link android.telecom.InCallService}s will
         * see calls in this state.
         */
        public static final int NEW = 0;

        /**
         * The initial state of an outgoing {@code Call}.
         * Common transitions are to {@link #DIALING} state for a successful call or
         * {@link #DISCONNECTED} if it failed.
         */
        public static final int CONNECTING = 1;

        /**
         * The state of an outgoing {@code Call} when waiting on user to select a
         * {@link android.telecom.PhoneAccount} through which to place the call.
         */
        public static final int SELECT_PHONE_ACCOUNT = 2;

        /**
         * Indicates that a call is outgoing and in the dialing state. A call transitions to this state
         * once an outgoing call has begun (e.g., user presses the dial button in Dialer). Calls in this
         * state usually transition to {@link #ACTIVE} if the call was answered or {@link #DISCONNECTED}
         * if the call was disconnected somehow (e.g., failure or cancellation of the call by the user).
         */
        public static final int DIALING = 3;

        /**
         * Indicates that a call is incoming and the user still has the option of answering, rejecting,
         * or doing nothing with the call. This state is usually associated with some type of audible
         * ringtone. Normal transitions are to {@link #ACTIVE} if answered or {@link #DISCONNECTED}
         * otherwise.
         */
        public static final int RINGING = 4;

        /**
         * Indicates that a call is currently connected to another party and a communication channel is
         * open between them. The normal transition to this state is by the user answering a
         * {@link #DIALING} call or a {@link #RINGING} call being answered by the other party.
         */
        public static final int ACTIVE = 5;

        /**
         * Indicates that the call is currently on hold. In this state, the call is not terminated
         * but no communication is allowed until the call is no longer on hold. The typical transition
         * to this state is by the user putting an {@link #ACTIVE} call on hold by explicitly performing
         * an action, such as clicking the hold button.
         */
        public static final int ON_HOLD = 6;

        /**
         * Indicates that a call is currently disconnected. All states can transition to this state
         * by the call service giving notice that the connection has been severed. When the user
         * explicitly ends a call, it will not transition to this state until the call service confirms
         * the disconnection or communication was lost to the call service currently responsible for
         * this call (e.g., call service crashes).
         */
        public static final int DISCONNECTED = 7;

        /**
         * Indicates that the call was attempted (mostly in the context of outgoing, at least at the
         * time of writing) but cancelled before it was successfully connected.
         */
        public static final int ABORTED = 8;

        /**
         * Indicates that the call is in the process of being disconnected and will transition next
         * to a {@link #DISCONNECTED} state.
         * <p>
         * This state is not expected to be communicated from the Telephony layer, but will be reported
         * to the InCall UI for calls where disconnection has been initiated by the user but the
         * ConnectionService has confirmed the call as disconnected.
         */
        public static final int DISCONNECTING = 9;

        public String toString(int callState) {
            switch (callState) {
                case NEW:
                    return "NEW";
                case CONNECTING:
                    return "CONNECTING";
                case SELECT_PHONE_ACCOUNT:
                    return "SELECT_PHONE_ACCOUNT";
                case DIALING:
                    return "DIALING";
                case RINGING:
                    return "RINGING";
                case ACTIVE:
                    return "ACTIVE";
                case ON_HOLD:
                    return "ON_HOLD";
                case DISCONNECTED:
                    return "DISCONNECTED";
                case ABORTED:
                    return "ABORTED";
                case DISCONNECTING:
                    return "DISCONNECTING";
                default:
                    return "UNKNOWN";
            }
        }
    }
}
