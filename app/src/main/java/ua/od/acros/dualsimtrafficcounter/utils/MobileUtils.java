package ua.od.acros.dualsimtrafficcounter.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.R;

public class MobileUtils {

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
                    if (idNext != null && !id.equals(idNext))
                        ret++;
                } catch (Exception e) {
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
            SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
            int simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? isMultiSim(context)
                    : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
            for (int i = 0; i < simNumber; i++) {
                try {
                    Class<?> c = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
                    Method getName = c.getMethod("getNetworkOperatorName", Integer.TYPE);
                    getName.setAccessible(true);
                    name.add(i, (String) getName.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i));
                } catch (Exception e0) {
                    e0.printStackTrace();
                }
            }
            if (name.size() == 0) {
                for (int i = 0; i < simNumber; i++) {
                    try {
                        Class<?> c = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
                        Method getName = c.getMethod("getNetworkOperatorName", Long.TYPE);
                        getName.setAccessible(true);
                        name.add(i, (String) getName.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i));
                    } catch (Exception e0) {
                        e0.printStackTrace();
                    }
                }
            }
            if (name.size() == 0) {
                for (int i = 0; i < simNumber; i++) {
                    try {
                        Class<?> c = Class.forName("android.telephony.TelephonyManager");
                        Method getName = c.getMethod("getNetworkOperatorName", Long.TYPE);
                        getName.setAccessible(true);
                        name.add(i, (String) getName.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i));
                    } catch (Exception e0) {
                        e0.printStackTrace();
                    }
                }
            }
        }
        return name;
    }

    public static ArrayList<String> getOperatorCodes(Context context) {
        ArrayList<String> name = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            for (SubscriptionInfo si : sl) {
                name.add(String.valueOf(si.getMcc()) + String.valueOf(si.getMnc()));
            }
        } else {
            SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
            int simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? isMultiSim(context)
                    : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
            for (int i = 0; i < simNumber; i++) {
                try {
                    Class<?> c = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
                    Method getCode = c.getMethod("getSimOperator", Integer.TYPE);
                    getCode.setAccessible(true);
                    name.add(i, (String) getCode.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i));
                } catch (Exception e0) {
                    e0.printStackTrace();
                }
            }
            if (name.size() == 0) {
                for (int i = 0; i < simNumber; i++) {
                    try {
                        Class<?> c = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
                        Method getCode = c.getMethod("getSimOperator", Long.TYPE);
                        getCode.setAccessible(true);
                        name.add(i, (String) getCode.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i));
                    } catch (Exception e0) {
                        e0.printStackTrace();
                    }
                }
            }
            if (name.size() == 0) {
                for (int i = 0; i < simNumber; i++) {
                    try {
                        Class<?> c = Class.forName("android.telephony.TelephonyManager");
                        Method getCode = c.getMethod("getSimOperator", Long.TYPE);
                        getCode.setAccessible(true);
                        name.add(i, (String) getCode.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i));
                    } catch (Exception e0) {
                        e0.printStackTrace();
                    }
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

        //String out = null;

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
                        //out = "getDefaultDataSubscriptionInfo " + sim + "\n";
                        break;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            if (sim == Constants.DISABLED) {
                try {
                    sim = Settings.Global.getInt(context.getContentResolver(), "multi_sim_data_call") - 1;
                    //out = "getFromSettingsGlobal " + sim + "\n";
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (sim == Constants.DISABLED) {
                SubscriptionManager sm = SubscriptionManager.from(context);
                List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
                for (int i = 0; i < sl.size(); i++) {
                    if (getNetworkFromApnsFile(String.valueOf(sl.get(i).getMcc()) + String.valueOf(sl.get(i).getMnc()), networkInfo.getExtraInfo())) {
                        sim = sl.get(i).getSimSlotIndex();
                        //out = "getNetworkFromApnsFile " + sim + "\n";
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
                            //out = "getDefaultDataSubId " + sim + "\n";
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            /*try {
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
            }*/
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        int simNumber = prefs.getBoolean(Constants.PREF_OTHER[13], true) ? isMultiSim(context)
                : Integer.valueOf(prefs.getString(Constants.PREF_OTHER[14], "1"));
        if (sim == Constants.DISABLED) {
            for (int i = 0; i < simNumber; i++) {
                int state = Constants.DISABLED;
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
                } catch (Exception e) {
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
            for (int i = 0; i < simNumber; i++) {
                int state = Constants.DISABLED;
                try {
                    Class<?> c = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
                    Method[] cm = c.getDeclaredMethods();
                    for (Method m : cm) {
                        if (m.getName().equalsIgnoreCase("getDataState")) {
                            m.setAccessible(true);
                            m.getParameterTypes();
                            if (m.getParameterTypes().length > 1) {
                                state = (int) m.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
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
            for (int i = 0; i < simNumber; i++) {
                int state = Constants.DISABLED;
                try {
                    Class<?> c = Class.forName("android.telephony.TelephonyManager");
                    Method[] cm = c.getDeclaredMethods();
                    for (Method m : cm) {
                        if (m.getName().equalsIgnoreCase("getDataState")) {
                            m.setAccessible(true);
                            m.getParameterTypes();
                            if (m.getParameterTypes().length > 1) {
                                state = (int) m.invoke(c.getConstructor(android.content.Context.class).newInstance(context), (long) i);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
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
        if (sim == Constants.DISABLED && android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            try {
                sim = Settings.Global.getInt(context.getContentResolver(), "multi_sim_data_call") - 1;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }
        return (int) sim;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private static void setMobileNetworkFromLollipop(final Context context, int sim) throws Exception {
        String cmd;
        int state;
        try {
            // Get the current state of the mobile network.
            state = getMobileDataInfo(context, false)[0] == 2 ? 0 : 1;
            // Get the value of the "TRANSACTION_setDataEnabled" field.
            String transactionCode = getTransactionCode(context);
            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            for (SubscriptionInfo si : sl) {
                if (transactionCode != null && transactionCode.length() > 0 && si.getSimSlotIndex() == sim) {
                    cmd = "service call phone " + transactionCode + " i32 " + si.getSubscriptionId() + " i32 " + state;
                    if (RootTools.isAccessGiven()) {
                        /*final ArrayList<String> out = new ArrayList<>();
                        File dir = new File(String.valueOf(context.getFilesDir()));
                        // create this directory if not already created
                        dir.mkdir();
                        // create the file in which we will write the contents
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", context.getResources().getConfiguration().locale);
                        Date now = new Date();
                        String fileName = formatter.format(now) + "_log.txt";
                        File file = new File(dir, fileName);
                        final FileOutputStream os = new FileOutputStream(file);*/
                        Command command = new Command(0, cmd) {
                            @Override
                            public void commandOutput(int id, String line) {
                                super.commandOutput(id, line);
                                //out.add(String.valueOf(id) + ": " + line + "\n");
                            }
                            @Override
                            public void commandTerminated(int id, String reason) {
                                super.commandTerminated(id, reason);
                                /*try {
                                    String s = String.valueOf(id) + ": " + reason;
                                    os.write(s.getBytes());
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }*/
                            }
                            @Override
                            public void commandCompleted(int id, int exitcode) {
                                super.commandCompleted(id, exitcode);
                                /*try {
                                    for (String s : out) {
                                        os.write(s.getBytes());
                                    }
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }*/
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
            e.printStackTrace();
        }
    }

    private static String getTransactionCode(Context context) throws Exception {
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
    }

    public static int[] getMobileDataInfo(Context context, boolean sim) {
        int[] mobileDataEnabled = {0, -1}; // Assume disabled
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetworkInfo = cm.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            String typeName = activeNetworkInfo.getTypeName().toLowerCase();
            boolean isConnected = activeNetworkInfo.isConnectedOrConnecting();
            int type = activeNetworkInfo.getType();
            if ((isNetworkTypeMobile(type)) && (typeName.contains("mobile")) && isConnected) {
                mobileDataEnabled[0] = 2;
                if (sim)
                    mobileDataEnabled[1] = (int) activeSIM(context, activeNetworkInfo);
                else
                    mobileDataEnabled[1] = Constants.DISABLED;
            }
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
        if (!ON) {
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
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public static String getName(Context context, String key1, String key2, int sim) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (prefs.getBoolean(key1, true)) {
            ArrayList<String> opNames = getOperatorNames(context);
            return (opNames.size() > sim && opNames.get(sim) != null) ? opNames.get(sim) : context.getResources().getString(R.string.single_sim);
        } else
            return prefs.getString(key2, "");
    }

    public static String getLogoFromCode(Context context, int sim) {
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
                case "25501":
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
                    return "vodafone";
                case "25011":
                    return "yota";
                default:
                    return "none";
            }
        } else
            return context.getResources().getString(R.string.single_sim);

    }

}