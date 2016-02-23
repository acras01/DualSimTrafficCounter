package ua.od.acros.dualsimtrafficcounter.utils;

public class Constants {
    public static final int DATABASE_VERSION = 10;
    public static final String SIM1RX = "sim1rx";
    public static final String SIM2RX = "sim2rx";
    public static final String SIM3RX = "sim3rx";
    public static final String SIM1TX = "sim1tx";
    public static final String SIM2TX = "sim2tx";
    public static final String SIM3TX = "sim3tx";
    public static final String TOTAL1 = "total1";
    public static final String TOTAL2 = "total2";
    public static final String TOTAL3 = "total3";
    public static final String SIM1RX_N = "sim1rx_n";
    public static final String SIM2RX_N = "sim2rx_n";
    public static final String SIM3RX_N = "sim3rx_n";
    public static final String SIM1TX_N = "sim1tx_n";
    public static final String SIM2TX_N = "sim2tx_n";
    public static final String SIM3TX_N = "sim3tx_n";
    public static final String TOTAL1_N = "total1_n";
    public static final String TOTAL2_N = "total2_n";
    public static final String TOTAL3_N = "total3_n";
    public static final String SIM_ACTIVE = "active_sim";
    public static final String OPERATOR1 = "operator1";
    public static final String OPERATOR2 = "operator2";
    public static final String OPERATOR3 = "operator3";
    public static final String TRAFFIC_BROADCAST_ACTION = "ua.od.acros.dualsimtrafficcounter.DATABROADCAST";
    public static final String CALLS_BROADCAST_ACTION = "ua.od.acros.dualsimtrafficcounter.CALLS";
    public static final String TIP = "tip";
    public static final String APP_PREFERENCES = "ua.od.acros.dualsimtrafficcounter_preferences";
    public static final String WIDGET_PREFERENCES = "_widget_preferences";
    public static final String DATA_DEFAULT_SIM = "android.intent.action.DATA_DEFAULT_SIM";
    public static final String LAST_ACTIVE_SIM = "last_sim";
    public static final String LAST_TX = "lasttx";
    public static final String LAST_RX = "lastrx";
    public static final String LAST_TIME = "time";
    public static final String LAST_DATE = "date";
    public static final String CLEAR = "ua.od.acros.dualsimtrafficcounter.clear";
    public static final String SET_USAGE = "ua.od.acros.dualsimtrafficcounter.usage";
    public static final String ON_OFF = "ua.od.acros.dualsimtrafficcounter.onoff";
    public static final String CHANGE_ACTION = "change";
    public static final String SETTINGS_ACTION = "mobile_data";
    public static final String LIMIT_ACTION = "limit";
    public static final String OFF_ACTION = "off";
    public static final String ACTION = "ua.od.acros.dualsimtrafficcounter.action";
    public static final String ALARM_ACTION = "ua.od.acros.dualsimtrafficcounter.ALARM";
    public static final String CONTINUE_ACTION = "continue";
    public static final long NOTIFY_INTERVAL = 1000; // 1 second
    public static final String DATABASE_NAME = "mydatabase.db";
    public static final String[] PREF_SIM1 = {"stub", "limit1", "value1", "period1", "round1", "auto1", //5
            "name1", "autooff1", "prefer1", "time1", "day1", "everydayonoff1", "timeoff1", "timeon1", //13
            "op_round1", "op_limit1", "op_value1", "usenight1", "limitnight1", //18
            "valuenight1", "nighton1", "nightoff1", "nightround1", "operator_logo1", "reset1", "needsreset1", "nextreset1"}; //26
    public static final String[] PREF_SIM2 = {"stub", "limit2", "value2", "period2", "round2", "auto2", //5
            "name2", "autooff2", "prefer2", "time2", "day2", "everydayonoff2", "timeoff2", "timeon2", //13
            "op_round2", "op_limit2", "op_value2", "usenight2", "limitnight2", //18
            "valuenight2", "nighton2", "nightoff2", "nightround2", "operator_logo2", "reset2", "needsreset2", "nextreset2"}; //26
    public static final String[] PREF_SIM3 = {"stub", "limit3", "value3", "period3", "round3", "auto3", //5
            "name3", "autooff3", "prefer3", "time3", "day3", "everydayonoff3", "timeoff3", "timeon3", //13
            "op_round3", "op_limit31", "op_value3", "usenight3", "limitnight3", //18
            "valuenight3", "nighton3", "nightoff3", "nightround3", "operator_logo3", "reset3", "needsreset3", "nextreset3"}; //26
    public static final String[] PREF_OTHER = {"stub", "ringtone", "vibrate", "notification", //3
            "watchdog", "count_stopped", "watchdog_stopped", //6
            "fullinfo", "watchdog_timer", "first_run", "changeSIM", "acra.enable", //11
            "status_icon", "auto_sim", "user_sim", "operator_logo", "info_status", //16
            "continue_overlimit", "action_chosen", "data_remain", "alt", "sim1", "sim2", //22
            "sim3", "calls_stopped", "calllogger", "notification_tap", "calls_remain"}; //27
    public static final String[] PREF_WIDGET_TRAFFIC = {"stub", "names", "info", "speed", "icons", //4
            "logo1", "logo2", "logo3", //7
            "user_pick1", "user_pick2", "user_pick3", //10
            "icon_size", "size", "text_color", "useback", //14
            "background_color", "speedtext", "speedicons", //17
            "showsim1","showsim2", "showsim3", "showdiv", "active", "day_night", "data_remain"}; //24
    public static final String[] PREF_WIDGET_CALLS = {"stub", "names", "icons", //2
            "logo1", "logo2", "logo3", //5
            "user_pick1", "user_pick2", "user_pick3", //8
            "icon_size", "size", "text_color", "useback", //12
            "background_color", "showdiv", "showsim1","showsim2", "showsim3", "calls_remain"}; //18
    public static final String[] PREF_SIM1_CALLS = {"calls_stub", "calls_limit1", "calls_period1", "calls_round1", //3
            "calls_time1", "calls_day1", "calls_op_value1", "calls_period1", "calls_reset1", "calls_needs_reset1"}; //9
    public static final String[] PREF_SIM2_CALLS = {"calls_stub", "calls_limit2", "calls_period2", "calls_round2", //3
            "calls_time2", "calls_day2", "calls_op_value2", "calls_period2", "calls_reset2", "calls_needs_reset2"}; //9
    public static final String[] PREF_SIM3_CALLS = {"calls_stub", "calls_limit3", "calls_period3", "calls_round3", //3
            "calls_time3", "calls_day3", "calls_op_value3", "calls_period3", "calls_reset3", "calls_needs_reset3"}; //9
    public static final int STARTED_ID = 101;
    public static final int SIM1 = 0;
    public static final int SIM2 = 1;
    public static final int SIM3 = 2;
    public static final int DISABLED = -1;
    public static final int NULL = -2;
    public static final int COUNT = 1001;
    public static final int CHECK = 1002;
    public static final int MINUTE = 60 * 1000;
    public static final int SECOND = 1000;
    public static final String TEXT_SIZE = "15";
    public static final String ICON_SIZE = "30";
    public static final String NUMBER = "number";
    public static final String SPEEDRX = "rx_speed";
    public static final String SPEEDTX = "tx_speed";
    public static final String WIDGET_IDS = "widget_ids";
    public static final String PERIOD3 = "period3";
    public static final String PERIOD2 = "period2";
    public static final String PERIOD1 = "period1";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String CALL_DURATION = "duration";
    public static final String CALLS1 = "calls1";
    public static final String CALLS2 = "calls2";
    public static final String CALLS3 = "calls3";
    public static final String CALLS1_EX = "calls1_ex";
    public static final String CALLS2_EX = "calls2_ex";
    public static final String CALLS3_EX = "calls3_ex";
    public static final String OUTGOING_CALL_COUNT = "ua.od.acros.dualsimtrafficcounter.COUNT";
    public static final String CLEAR_CALLS = "ua.od.acros.dualsimtrafficcounter.CLEAR_CALLS";
    public static final String SET_DURATION = "ua.od.acros.dualsimtrafficcounter.DURATION";
    public static final String TRAFFIC_TAG = "_traffic";
    public static final String CALLS_TAG = "_calls";
    public static final String NEW_OUTGOING_CALL = "ua.od.acros.dualsimtrafficcounter.NEW_OUTGOING_CALL";
    public static final String PROCESS_OUTGOING_CALL = "ua.od.acros.dualsimtrafficcounter.PROCESS_OUTGOING_CALL";
}
