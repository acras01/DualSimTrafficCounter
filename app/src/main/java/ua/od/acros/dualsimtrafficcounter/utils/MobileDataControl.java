package ua.od.acros.dualsimtrafficcounter.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.R;

public class MobileDataControl {

    private static int lastActiveSIM;
    private static boolean alt;
    private static NetworkInfo activeNetworkInfo;

    public static int isMultiSim(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            return sm.getActiveSubscriptionInfoList().size();
        } else {
            int ret = 1;
            for (int i = 0; i < 2; i++)
                try {
                    Class<?> c = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
                    Method getId = c.getMethod("getDeviceId", Integer.TYPE);
                    getId.setAccessible(true);
                    String id = (String) getId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i);
                    String idNext = (String) getId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i + 1);
                    if (!id.equals(idNext) && idNext != null)
                        ret++;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            return ret;
        }

    }

    public static ArrayList<String> getOperatorNames(Context context) {
        ArrayList<String> name = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            for (SubscriptionInfo si : sl) {
                name.add((String) si.getCarrierName());
            }
        } else {
            for (int i = 0; i < isMultiSim(context); i++) {
                try {
                    Class<?> c = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
                    Method getName = c.getMethod("getNetworkOperatorName", Integer.TYPE);
                    getName.setAccessible(true);
                    name.add(i, (String) getName.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i));
                } catch (ClassNotFoundException e0) {
                    name.add(i, "ClassNotFoundException");
                } catch (NoSuchMethodException e0) {
                    name.add(i, "NoSuchMethodException");
                } catch (InstantiationException e0) {
                    name.add(i, "InstantiationException");
                } catch (IllegalAccessException e0) {
                    name.add(i, "InstantiationException");
                } catch (InvocationTargetException e0) {
                    name.add(i, "InstantiationException");
                }
            }
        }
        return name;
    }

    /*private static boolean getNetworkFromDB(Context context, String code, String apn) {
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
    }*/

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

    private static long activeSIM(Context context, NetworkInfo networkInfo){

        String out = null;

        long sim = Constants.DISABLED;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                Class<?> c = Class.forName("android.telephony.SubscriptionManager");
                Method[] cm = c.getDeclaredMethods();
                for (Method m : cm) {
                    if (m.getName().equalsIgnoreCase("getDefaultDataSubscriptionInfo")) {
                        m.setAccessible(true);
                        SubscriptionInfo si = (SubscriptionInfo) m.invoke(c.getConstructor(android.content.Context.class).newInstance(context));
                        sim = si.getSimSlotIndex();
                        out = "getDefaultDataSubscriptionInfo " + sim + "\n";
                        break;
                    }
                }
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            if (sim == Constants.DISABLED) {
                SubscriptionManager sm = SubscriptionManager.from(context);
                List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
                for (int i = 0; i < sl.size(); i++) {
                    if (getNetworkFromApnsFile(String.valueOf(sl.get(i).getMcc()) + String.valueOf(sl.get(i).getMnc()), networkInfo.getExtraInfo())) {
                        sim = sl.get(i).getSimSlotIndex();
                        out = "getNetworkFromApnsFile " + sim + "\n";
                        break;
                    }
                }
            }
            if (sim == Constants.DISABLED) {
                try {
                    Class<?> c = Class.forName("android.telephony.SubscriptionManager");
                    Method[] cm = c.getDeclaredMethods();
                    for (Method m : cm) {
                        if (m.getName().equalsIgnoreCase("getDefaultDataSubId")) {
                            m.setAccessible(true);
                            sim = (int) m.invoke(c.getConstructor(android.content.Context.class).newInstance(context)) - 1;
                            out = "getDefaultDataSubId " + sim + "\n";
                            break;
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
            try {
                // to this path add a new directory path
                File dir = new File(String.valueOf(context.getFilesDir()));
                // create this directory if not already created
                dir.mkdir();
                // create the file in which we will write the contents
                String fileName ="log.txt";
                File file = new File(dir, fileName);
                FileOutputStream os = new FileOutputStream(file);
                os.write(out.getBytes());
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (sim == Constants.DISABLED) {
            try {
                Class<?> c = Class.forName(networkInfo.getClass().getName());
                Method[] cm = c.getDeclaredMethods();
                for (Method m : cm) {
                    if (m.getName().equalsIgnoreCase("getSimId")) {
                        m.setAccessible(true);
                        sim = (int) m.invoke(networkInfo);
                        break;
                    }
                }
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }  catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }
        if (sim == Constants.DISABLED) {
            for (int i = 0; i < isMultiSim(context); i++) {
                int state = Constants.DISABLED;
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                try {

                    state = (int) Class.forName(tm.getClass().getName()).getMethod("getDataState", Integer.TYPE).invoke(tm, i);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    try {
                        Class<?> c = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
                        Method[] cm = c.getDeclaredMethods();
                        for (Method m : cm) {
                            if (m.getName().equalsIgnoreCase("getDataState")) {
                                m.setAccessible(true);
                                state = (int) m.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i);
                                break;
                            }
                        }
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (NoSuchMethodException e1) {
                        e1.printStackTrace();
                    } catch (InstantiationException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (state == TelephonyManager.DATA_CONNECTED
                        || state == TelephonyManager.DATA_CONNECTING
                        || state == TelephonyManager.DATA_SUSPENDED) {
                    sim = i;
                    break;
                }
            }
        }
        if (sim == Constants.DISABLED) {
            for (long i = 0; i < isMultiSim(context); i++) {
                int state = Constants.DISABLED;
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                try {

                    state = (int) Class.forName(tm.getClass().getName()).getMethod("getDataState", Long.TYPE).invoke(tm, i);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    try {
                        Class<?> c = Class.forName("android.telephony.TelephonyManager");
                        Method[] cm = c.getDeclaredMethods();
                        for (Method m : cm) {
                            if (m.getName().equalsIgnoreCase("getDataState")) {
                                m.setAccessible(true);
                                state = (int) m.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i);
                                break;
                            }
                        }
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (NoSuchMethodException e1) {
                        e1.printStackTrace();
                    } catch (InstantiationException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (state == TelephonyManager.DATA_CONNECTED
                        || state == TelephonyManager.DATA_CONNECTING
                        || state == TelephonyManager.DATA_SUSPENDED) {
                    sim = i;
                    break;
                }
            }
        }
        return sim;
    }


    /*private static String getActiveSubscriberId(Context context) {
        final TelephonyManager tele = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);;
        return tele.getSubscriberId();
    }*/

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private static void setMobileNetworkFromLollipop(final Context context, int sim) throws Exception {
        String cmd;
        int state;
        try {
            // Get the current state of the mobile network.
            state = getMobileDataInfo(context)[0] == 2 ? 0 : 1;
            // Get the value of the "TRANSACTION_setDataEnabled" field.
            String transactionCode = getTransactionCode(context);
            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            for (SubscriptionInfo si : sl) {
                if (transactionCode != null && transactionCode.length() > 0 && si.getSimSlotIndex() == sim) {
                    cmd = "service call phone " + transactionCode + " i32 " + si.getSubscriptionId() + " i32 " + state;
                    if (RootTools.isAccessGiven()) {
                        final ArrayList<String> out = new ArrayList<>();
                        File dir = new File(String.valueOf(context.getFilesDir()));
                        // create this directory if not already created
                        dir.mkdir();
                        // create the file in which we will write the contents
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
                        Date now = new Date();
                        String fileName = formatter.format(now) + "_log.txt";
                        File file = new File(dir, fileName);
                        final FileOutputStream os = new FileOutputStream(file);
                        Command command = new Command(0, cmd) {
                            @Override
                            public void commandOutput(int id, String line) {
                                super.commandOutput(id, line);
                                out.add(String.valueOf(id) + ": " + line + "\n");
                            }
                            @Override
                            public void commandTerminated(int id, String reason) {
                                super.commandTerminated(id, reason);
                                try {
                                    String s = String.valueOf(id) + ": " + reason;
                                    os.write(s.getBytes());
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void commandCompleted(int id, int exitcode) {
                                super.commandCompleted(id, exitcode);
                                try {
                                    for (String s : out) {
                                        os.write(s.getBytes());
                                    }
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        RootTools.getShell(true).add(command);
                        //commandWait(command);
                    } else
                        Toast.makeText(context, R.string.no_root_granted, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        } catch(Exception e) {
                // Oops! Something went wrong, so we throw the exception here.
            throw e;
        }
    }

    /*@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static boolean isMobileDataEnabledFromLollipop(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "mobile_data", 0) == 1;
    }*/

    private static String getTransactionCode(Context context) throws Exception {
        try {
            final TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final Class<?> mTelephonyClass = Class.forName(mTelephonyManager.getClass().getName());
            final Method mTelephonyMethod = mTelephonyClass.getDeclaredMethod("getITelephony");
            mTelephonyMethod.setAccessible(true);
            final Object mTelephonyStub = mTelephonyMethod.invoke(mTelephonyManager);
            final Class<?> mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
            final Class<?> mClass = mTelephonyStubClass.getDeclaringClass();
            final Field field = mClass.getDeclaredField("TRANSACTION_setDataEnabled");
            field.setAccessible(true);
            return String.valueOf(field.getInt(null));
        } catch (Exception e) {
            // The "TRANSACTION_setDataEnabled" field is not available,
            // or named differently in the current API level, so we throw
            // an exception and inform users that the method is not available.
            throw e;
        }
    }

    /*private static void executeCommandViaSu(String option, String command) {
        boolean success = false;
        String su = "su";
        for (int i=0; i < 3; i++) {
            // Default "su" command executed successfully, then quit.
            if (success) {
                break;
            }
            // Else, execute other "su" commands.
            if (i == 1) {
                su = "/system/xbin/su";
            } else if (i == 2) {
                su = "/system/bin/su";
            }
            try {
                // Execute command as "su".
                Process p = Runtime.getRuntime().exec(su);
                DataOutputStream os = new DataOutputStream(p.getOutputStream());
                os.writeBytes(command + "\n");
                //Runtime.getRuntime().exec(new String[]{su, option , command});
            } catch (IOException e) {
                success = false;
                // Oops! Cannot execute `su` for some reason.
                // Log error here.
            } finally {
                success = true;
            }
        }
    }*/

    public static int[] getMobileDataInfo(Context context) {
        int[] mobileDataEnabled = {0, -1}; // Assume disabled
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = cm.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            mobileDataEnabled[1] = (int) activeSIM(context, activeNetworkInfo);
            String typeName = activeNetworkInfo.getTypeName().toLowerCase();
            boolean isConnected = activeNetworkInfo.isConnectedOrConnecting();
            int type = activeNetworkInfo.getType();
            if ((isNetworkTypeMobile(type)) && (typeName.contains("mobile")) && isConnected)
                mobileDataEnabled[0] = 2;
            else if ((!isNetworkTypeMobile(type)) && (!typeName.contains("mobile")) && isConnected)
                mobileDataEnabled[0] = 1;
        }
        return mobileDataEnabled;
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

    public static void toggleMobileDataConnection(boolean ON, Context context, int sim) throws Exception {
        if (!ON) {
            alt = false;
            if (MTKUtils.isMtkDevice() && MTKUtils.hasGeminiSupport()) {
                try {
                    lastActiveSIM = (int) Settings.System.getLong(context.getContentResolver(), "gprs_connection_sim_setting");
                    alt = true;
                } catch (Settings.SettingNotFoundException e0) {
                    e0.printStackTrace();
                    try {
                        lastActiveSIM = (int) Settings.System.getLong(context.getContentResolver(), "gprs_connection_setting");
                        alt = true;
                    } catch (Settings.SettingNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            if (!alt)
                lastActiveSIM = (int) activeSIM(context, activeNetworkInfo);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                setMobileNetworkFromLollipop(context, lastActiveSIM);
            } else {
                Intent localIntent = new Intent(Constants.DATA_DEFAULT_SIM);
                localIntent.putExtra("simid", Constants.DISABLED);
                context.sendBroadcast(localIntent);
            }
        }
        if (ON && sim == Constants.DISABLED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                setMobileNetworkFromLollipop(context, lastActiveSIM);
            } else {
                Intent localIntent = new Intent(Constants.DATA_DEFAULT_SIM);
                if (alt)
                    localIntent.putExtra("simid", (long) lastActiveSIM);
                else
                    localIntent.putExtra("simid", (long) lastActiveSIM + 1);
                context.sendBroadcast(localIntent);
            }
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                //Handle exception
            }
        } else if (sim != Constants.DISABLED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                setMobileNetworkFromLollipop(context, sim);
            } else {
                Intent localIntent = new Intent(Constants.DATA_DEFAULT_SIM);
                if (alt && lastActiveSIM > 2)
                    localIntent.putExtra("simid", (long) sim + 3);
                else
                    localIntent.putExtra("simid", (long) sim + 1);
                context.sendBroadcast(localIntent);
            }
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            //Handle exception
        }
    }

    private static void commandWait(Command cmd) throws Exception {
        int waitTill = 50;
        int waitTillMultiplier = 2;
        int waitTillLimit = 3200; //7 tries, 6350 msec

        while (!cmd.isFinished() && waitTill <= waitTillLimit) {
            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(waitTill);
                        waitTill *= waitTillMultiplier;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!cmd.isFinished()){
            Log.e("DSTC", "Could not finish root command in " + (waitTill / waitTillMultiplier));
        }
    }
}


        /*String id = getActiveSubscriberId(mContext);
        try {
            Object tmpl = null;
            long stats = 0;
            Class<?> a = Class.forName("android.net.NetworkTemplate");
            Class<?> b = Class.forName("android.net.INetworkStatsService");
            Method getState = b.getMethod("getNetworkTotalBytes", a, long.class, long.class);
            Method[] am = a.getDeclaredMethods();
            for (Method m : am) {
                if (m.getName().equalsIgnoreCase("buildTemplateMobileAll")) {
                    m.setAccessible(true);
                    tmpl = m.invoke(a.getClass(), id)
                    break;
                }
            }
            Object object = Proxy.newProxyInstance(b.getClass().getClassLoader(), b.getInterfaces(), new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("getNetworkTotalBytes")) {
                        return method.invoke(args[0], args[1], args[2], args[3]);
                    }
                    throw new RuntimeException("no method found");
                }
            });
            Object[] args = {b.getClass(), tmpl, Long.MIN_VALUE, Long.MAX_VALUE};
            stats = (long) ((b) object).getState(args);
        } catch (ClassNotFoundException e0) {
        } catch (NoSuchMethodException e0) {
        } catch (IllegalAccessException e0) {
        } catch (InvocationTargetException e0) {
        } catch (NoSuchFieldException e0) {
        }*/
