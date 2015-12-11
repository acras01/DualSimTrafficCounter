package ua.od.acros.dualsimtrafficcounter.utils;

public class Constants {
    public static final int DATABASE_VERSION = 2;
    public static final String SIM1RX = "sim1rx";
    public static final String SIM2RX = "sim2rx";
    public static final String SIM3RX = "sim3rx";
    public static final String SIM1TX = "sim1tx";
    public static final String SIM2TX = "sim2tx";
    public static final String SIM3TX = "sim3tx";
    public static final String TOTAL1 = "total1";
    public static final String TOTAL2 = "total2";
    public static final String TOTAL3 = "total3";
    public static final String SIM_ACTIVE = "active_sim";
    public static final String OPERATOR1 = "operator1";
    public static final String OPERATOR2 = "operator2";
    public static final String OPERATOR3 = "operator3";
    public static final String BROADCAST_ACTION = "ua.od.acros.dualsimtrafficcounter.DATABROADCAST";
    public static final String TIP = "tip";
    public static final String APP_PREFERENCES = "ua.od.acros.dualsimtrafficcounter_preferences";
    public static final String WIDGET_PREFERENCES = "widget_preferences";
    public static final String DATA_DEFAULT_SIM = "android.intent.action.DATA_DEFAULT_SIM";
    public static final int READ = 0;
    public static final int WRITE = 1;
    public static final int UPDATE = 2;
    public static final String LAST_ACTIVE_SIM = "last_sim";
    public static final String LAST_TX = "lasttx";
    public static final String LAST_RX = "lastrx";
    public static final String LAST_TIME = "time";
    public static final String LAST_DATE = "date";
    public static final String CLEAR1 = "clear1";
    public static final String CLEAR2 = "clear2";
    public static final String CLEAR3 = "clear3";
    public static final String SET_USAGE = "usage";
    public static final String ON_OFF = "onoff";
    public static final String CHOOSE_ACTION = "choose";
    public static final String LIMIT_ACTION = "limit";
    public static final String OFF_ACTION = "off";
    public static final String ACTION = "action";
    public static final String ALARM_ACTION = "alarm";
    public static final String CONTINUE_ACTION = "continue";
    public static final long NOTIFY_INTERVAL = 1000; // 0.5 seconds
    public static final String DATABASE_NAME = "mydatabase.db";
    public static final String[] PREF_SIM1 = {"stub", "limit1", "value1", "period1", "round1", "auto1", //5
            "name1", "autooff1", "prefer1", "time1", "day1", "everydayonoff1", "timeoff1", "timeon1"}; //13
    public static final String[] PREF_SIM2 = {"stub", "limit2", "value2", "period2", "round2", "auto2", //5
            "name2", "autooff2", "prefer2", "time2", "day2", "everydayonoff2", "timeoff2", "timeon2"}; //13
    public static final String[] PREF_SIM3 = {"stub", "limit3", "value3", "period3", "round3", "auto3", //5
            "name3", "autooff3", "prefer3", "time3", "day3", "everydayonoff3", "timeoff3", "timeon3"}; //13
    public static final String[] PREF_OTHER = {"stub", "ringtone", "vibrate", "notification", //3
            "watchdog", "count_stopped", "watchdog_stopped", //6
            "fullinfo", "watchdog_timer","first_run", "changeSIM"}; //10
    public static final String[] PREF_WIDGET = {"stub", "names", "info", "speed", "icons", //4
            "logo1", "logo2", "logo3", //7
            "user_pick1", "user_pick2", "user_pick3", //10
            "icon_size", "size", "text_color", "useback", //14
            "background_color", "speedtext", "speedicons", //17
            "showsim1","showsim2", "showsim3"}; //20
    public static final int STARTED_ID = 101;
    public static final int SIM1 = 0;
    public static final int SIM2 = 1;
    public static final int SIM3 = 2;
    public static final int DISABLED = -1;
    public static final int NULL = -2;
    public static final int COUNT = 1001;
    public static final int CHECK = 1002;
    public static final String TEXT_SIZE = "15";
    public static final String ICON_SIZE = "30";
    public static final String SPEEDRX = "rx_speed";
    public static final String SPEEDTX = "tx_speed";
}
