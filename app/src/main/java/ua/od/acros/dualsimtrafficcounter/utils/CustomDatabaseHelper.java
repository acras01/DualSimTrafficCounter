package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

public class CustomDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATA_TABLE = "data";
    private static final String CALLS_TABLE = "calls";
    private static final String WHITE_LIST_1 = "list1";
    private static final String WHITE_LIST_2 = "list2";
    private static final String WHITE_LIST_3 = "list3";
    private static final String BLACK_LIST_1 = "list1_b";
    private static final String BLACK_LIST_2 = "list2_b";
    private static final String BLACK_LIST_3 = "list3_b";
    private static SQLiteDatabase mSqLiteDatabase;
    private static CustomDatabaseHelper mInstance;

    public static CustomDatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new CustomDatabaseHelper(context);
        }
        return mInstance;
    }

    private CustomDatabaseHelper(Context context) {
        super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
    }

    private CustomDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                                 int version) {
        super(context, name, factory, version);
    }

    private CustomDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                                 int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String DATABASE_CREATE_SCRIPT = "create table "
                + DATA_TABLE + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                + " text not null, " + Constants.LAST_ACTIVE_SIM + " integer, "
                + Constants.LAST_RX + " long, " + Constants.LAST_TX + " long, "
                + Constants.SIM1RX + " long, " + Constants.SIM1TX + " long, "
                + Constants.TOTAL1 + " long, " + Constants.SIM2RX + " long, "
                + Constants.SIM2TX + " long, " + Constants.TOTAL2 + " long, "
                + Constants.SIM3RX + " long, " + Constants.SIM3TX + " long, "
                + Constants.TOTAL3 + " long, " + Constants.PERIOD1 + " integer,"
                + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer, "
                + Constants.SIM1RX_N + " long, "
                + Constants.SIM1TX_N + " long, " + Constants.TOTAL1_N + " long, "
                + Constants.SIM2RX_N + " long, " + Constants.SIM2TX_N + " long, "
                + Constants.TOTAL2_N + " long, " + Constants.SIM3RX_N + " long,"
                + Constants.SIM3TX_N + " long, " + Constants.TOTAL3_N + " long);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + CALLS_TABLE + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                + " text not null, " +  Constants.CALLS1 + " long, "
                + Constants.CALLS1_EX + " long, " + Constants.CALLS2 + " long, "
                + Constants.CALLS2_EX + " long, " + Constants.CALLS3 + " long, "
                + Constants.CALLS3_EX + " long, " + Constants.PERIOD1 + " integer,"
                + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + WHITE_LIST_1 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + WHITE_LIST_2 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + WHITE_LIST_3 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + BLACK_LIST_1 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + BLACK_LIST_2 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + BLACK_LIST_3 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String ALTER_TBL;
        if (oldVersion < Constants.DATABASE_VERSION) {
            if (oldVersion <= 1) {
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM3RX + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM3TX + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL3 + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.PERIOD1 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.PERIOD2 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.PERIOD3 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM1RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM1TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL1_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM2RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM2TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL2_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM3RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM3TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL3_N + " long;";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 2) {
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.PERIOD1 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.PERIOD2 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.PERIOD3 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM1RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM1TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL1_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM2RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM2TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL2_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM3RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM3TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL3_N + " long;";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 3) {
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM1RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM1TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL1_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM2RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM2TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL2_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM3RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.SIM3TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + DATA_TABLE +
                                " ADD COLUMN " + Constants.TOTAL3_N + " long;";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 4) {
                ALTER_TBL = "create table "
                        + CALLS_TABLE + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                        + " text not null, " + Constants.CALLS1 + " long, "
                        + Constants.CALLS1_EX + " long, " + Constants.CALLS2 + " long, "
                        + Constants.CALLS2_EX + " long, " + Constants.CALLS3 + " long, "
                        + Constants.CALLS3_EX + " long, " + Constants.PERIOD1 + " integer,"
                        + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer);";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 6) {
                db.execSQL("DROP TABLE IF EXISTS " + CALLS_TABLE);
                ALTER_TBL = "create table "
                        + CALLS_TABLE + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                        + " text not null, " + Constants.CALLS1 + " long, "
                        + Constants.CALLS1_EX + " long, " + Constants.CALLS2 + " long, "
                        + Constants.CALLS2_EX + " long, " + Constants.CALLS3 + " long, "
                        + Constants.CALLS3_EX + " long, " + Constants.PERIOD1 + " integer,"
                        + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer);";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 7) {
                ALTER_TBL = "create table "
                        + WHITE_LIST_1 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
                ALTER_TBL = "create table "
                        + WHITE_LIST_2 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
                ALTER_TBL = "create table "
                        + WHITE_LIST_3 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 8) {
                ALTER_TBL = "create table "
                        + BLACK_LIST_1 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
                ALTER_TBL = "create table "
                        + BLACK_LIST_2 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
                ALTER_TBL = "create table "
                        + BLACK_LIST_3 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
            }
        }
    }

    public static ContentValues readTrafficData(CustomDatabaseHelper db) {
        ContentValues cv = new ContentValues();
        mSqLiteDatabase = db.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(DATA_TABLE, new String[]{Constants.LAST_DATE, Constants.LAST_TIME, Constants.LAST_ACTIVE_SIM,
                Constants.LAST_RX, Constants.LAST_TX, Constants.SIM1RX, Constants.SIM1TX, Constants.TOTAL1,
                Constants.SIM2RX, Constants.SIM2TX, Constants.TOTAL2, Constants.SIM3RX, Constants.SIM3TX,
                Constants.TOTAL3, Constants.PERIOD1, Constants.PERIOD2, Constants.PERIOD3, Constants.SIM1RX_N,
                Constants.SIM1TX_N, Constants.TOTAL1_N, Constants.SIM2RX_N, Constants.SIM2TX_N, Constants.TOTAL2_N,
                Constants.SIM3RX_N, Constants.SIM3TX_N, Constants.TOTAL3_N}, null, null, null, null, null);
        if (cursor.moveToLast()) {
            cv.put(Constants.SIM1RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM1RX)));
            cv.put(Constants.SIM2RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM2RX)));
            cv.put(Constants.SIM3RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM3RX)));
            cv.put(Constants.SIM1TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM1TX)));
            cv.put(Constants.SIM2TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM2TX)));
            cv.put(Constants.SIM3TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM3TX)));
            cv.put(Constants.TOTAL1, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL1)));
            cv.put(Constants.TOTAL2, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL2)));
            cv.put(Constants.TOTAL3, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL3)));
            cv.put(Constants.LAST_ACTIVE_SIM, cursor.getInt(cursor.getColumnIndex(Constants.LAST_ACTIVE_SIM)));
            cv.put(Constants.LAST_RX, cursor.getLong(cursor.getColumnIndex(Constants.LAST_RX)));
            cv.put(Constants.LAST_TX, cursor.getLong(cursor.getColumnIndex(Constants.LAST_TX)));
            cv.put(Constants.LAST_TIME, cursor.getString(cursor.getColumnIndex(Constants.LAST_TIME)));
            cv.put(Constants.LAST_DATE, cursor.getString(cursor.getColumnIndex(Constants.LAST_DATE)));
            cv.put(Constants.PERIOD1, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD1)));
            cv.put(Constants.PERIOD2, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD2)));
            cv.put(Constants.PERIOD3, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD3)));
            cv.put(Constants.SIM1RX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM1RX_N)));
            cv.put(Constants.SIM2RX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM2RX_N)));
            cv.put(Constants.SIM3RX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM3RX_N)));
            cv.put(Constants.SIM1TX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM1TX_N)));
            cv.put(Constants.SIM2TX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM2TX_N)));
            cv.put(Constants.SIM3TX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM3TX_N)));
            cv.put(Constants.TOTAL1_N, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL1_N)));
            cv.put(Constants.TOTAL2_N, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL2_N)));
            cv.put(Constants.TOTAL3_N, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL3_N)));
        } else {
            cv.put(Constants.SIM1RX, 0L);
            cv.put(Constants.SIM2RX, 0L);
            cv.put(Constants.SIM3RX, 0L);
            cv.put(Constants.SIM1TX, 0L);
            cv.put(Constants.SIM2TX, 0L);
            cv.put(Constants.SIM3TX, 0L);
            cv.put(Constants.TOTAL1, 0L);
            cv.put(Constants.TOTAL2, 0L);
            cv.put(Constants.TOTAL3, 0L);
            cv.put(Constants.LAST_ACTIVE_SIM, 0);
            cv.put(Constants.LAST_RX, 0L);
            cv.put(Constants.LAST_TX, 0L);
            cv.put(Constants.LAST_TIME, "");
            cv.put(Constants.LAST_DATE, "");
            cv.put(Constants.PERIOD1, 0);
            cv.put(Constants.PERIOD2, 0);
            cv.put(Constants.PERIOD3, 0);
            cv.put(Constants.SIM1RX_N, 0L);
            cv.put(Constants.SIM2RX_N, 0L);
            cv.put(Constants.SIM3RX_N, 0L);
            cv.put(Constants.SIM1TX_N, 0L);
            cv.put(Constants.SIM2TX_N, 0L);
            cv.put(Constants.SIM3TX_N, 0L);
            cv.put(Constants.TOTAL1_N, 0L);
            cv.put(Constants.TOTAL2_N, 0L);
            cv.put(Constants.TOTAL3_N, 0L);
        }
        cursor.close();
        return cv;
    }

    public static void writeTrafficData(ContentValues mMap, CustomDatabaseHelper db) {
        mSqLiteDatabase = db.getWritableDatabase();
        String filter = Constants.LAST_DATE + "='" + mMap.get(Constants.LAST_DATE) + "'";
        int id = mSqLiteDatabase.update(DATA_TABLE, mMap, filter, null);
        if (id == 0)
            mSqLiteDatabase.insert(DATA_TABLE, null, mMap);
    }

    public static boolean isTrafficTableEmpty(CustomDatabaseHelper db) {
        boolean result;
        mSqLiteDatabase = db.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(DATA_TABLE, new String[]{Constants.LAST_DATE, Constants.LAST_TIME, Constants.LAST_ACTIVE_SIM,
                Constants.LAST_RX, Constants.LAST_TX, Constants.SIM1RX, Constants.SIM1TX, Constants.TOTAL1,
                Constants.SIM2RX, Constants.SIM2TX, Constants.TOTAL2, Constants.SIM3RX, Constants.SIM3TX,
                Constants.TOTAL3, Constants.PERIOD1, Constants.PERIOD2, Constants.PERIOD3, Constants.SIM1RX_N,
                Constants.SIM1TX_N, Constants.TOTAL1_N, Constants.SIM2RX_N, Constants.SIM2TX_N, Constants.TOTAL2_N,
                Constants.SIM3RX_N, Constants.SIM3TX_N, Constants.TOTAL3_N}, null, null, null, null, null);
        result = cursor != null && cursor.getCount() == 0;
        if (cursor != null) {
            cursor.close();
        }
        return result;
    }

    public static boolean isCallsTableEmpty(CustomDatabaseHelper db) {
        boolean result;
        mSqLiteDatabase = db.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(CALLS_TABLE, new String[]{Constants.LAST_DATE, Constants.LAST_TIME, Constants.CALLS1,
                Constants.CALLS1_EX, Constants.CALLS2, Constants.CALLS2_EX, Constants.CALLS3, Constants.CALLS3_EX,
                Constants.PERIOD1, Constants.PERIOD2, Constants.PERIOD3}, null, null, null, null, null);
        result = cursor != null && cursor.getCount() == 0;
        if (cursor != null) {
            cursor.close();
        }
        return result;
    }

    public static Bundle getDataForDate(CustomDatabaseHelper db, String date, int sim, SharedPreferences prefs) {

        ContentValues cv1 = new ContentValues();
        ContentValues cv2 = new ContentValues();
        Bundle out = new Bundle();

        mSqLiteDatabase = db.getReadableDatabase();

        DateTimeFormatter fmtDate = DateTimeFormat.forPattern(Constants.DATE_FORMAT);
        DateTime queried = fmtDate.parseDateTime(date);

        if (queried.isAfterNow())
            return null;

        String dayBeforeDate = queried.minusDays(1).toString(fmtDate);

        Cursor cursorToDate = mSqLiteDatabase.query(DATA_TABLE, new String[]{
                Constants.SIM1RX, Constants.SIM1TX, Constants.TOTAL1, Constants.SIM2RX, Constants.SIM2TX,
                Constants.TOTAL2, Constants.SIM3RX, Constants.SIM3TX, Constants.TOTAL3, Constants.PERIOD1,
                Constants.PERIOD2, Constants.PERIOD3, Constants.SIM1RX_N, Constants.SIM1TX_N, Constants.TOTAL1_N,
                Constants.SIM2RX_N, Constants.SIM2TX_N, Constants.TOTAL2_N, Constants.SIM3RX_N, Constants.SIM3TX_N,
                Constants.TOTAL3_N}, Constants.LAST_DATE + " = ?", new String[]{queried.toString(fmtDate)}, null, null, null);
        if (cursorToDate.moveToLast()) {
            cv1.put(Constants.SIM1RX, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM1RX)));
            cv1.put(Constants.SIM2RX, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM2RX)));
            cv1.put(Constants.SIM3RX, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM3RX)));
            cv1.put(Constants.SIM1TX, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM1TX)));
            cv1.put(Constants.SIM2TX, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM2TX)));
            cv1.put(Constants.SIM3TX, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM3TX)));
            cv1.put(Constants.TOTAL1, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL1)));
            cv1.put(Constants.TOTAL2, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL2)));
            cv1.put(Constants.TOTAL3, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL3)));
            cv1.put(Constants.PERIOD1, cursorToDate.getInt(cursorToDate.getColumnIndex(Constants.PERIOD1)));
            cv1.put(Constants.PERIOD2, cursorToDate.getInt(cursorToDate.getColumnIndex(Constants.PERIOD2)));
            cv1.put(Constants.PERIOD3, cursorToDate.getInt(cursorToDate.getColumnIndex(Constants.PERIOD3)));
            cv1.put(Constants.SIM1RX_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM1RX_N)));
            cv1.put(Constants.SIM2RX_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM2RX_N)));
            cv1.put(Constants.SIM3RX_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM3RX_N)));
            cv1.put(Constants.SIM1TX_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM1TX_N)));
            cv1.put(Constants.SIM2TX_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM2TX_N)));
            cv1.put(Constants.SIM3TX_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM3TX_N)));
            cv1.put(Constants.TOTAL1_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL1_N)));
            cv1.put(Constants.TOTAL2_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL2_N)));
            cv1.put(Constants.TOTAL3_N, cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL3_N)));
        }
        Cursor cursorToDayBeforeDate = mSqLiteDatabase.query(DATA_TABLE, new String[]{
                Constants.SIM1RX, Constants.SIM1TX, Constants.TOTAL1, Constants.SIM2RX, Constants.SIM2TX,
                Constants.TOTAL2, Constants.SIM3RX, Constants.SIM3TX, Constants.TOTAL3, Constants.PERIOD1,
                Constants.PERIOD2, Constants.PERIOD3, Constants.SIM1RX_N, Constants.SIM1TX_N, Constants.TOTAL1_N,
                Constants.SIM2RX_N, Constants.SIM2TX_N, Constants.TOTAL2_N, Constants.SIM3RX_N, Constants.SIM3TX_N,
                Constants.TOTAL3_N}, Constants.LAST_DATE + " = ?", new String[]{dayBeforeDate}, null, null, null);
        if (cursorToDayBeforeDate.moveToLast()) {
            cv2.put(Constants.SIM1RX, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM1RX)));
            cv2.put(Constants.SIM2RX, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM2RX)));
            cv2.put(Constants.SIM3RX, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM3RX)));
            cv2.put(Constants.SIM1TX, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM1TX)));
            cv2.put(Constants.SIM2TX, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM2TX)));
            cv2.put(Constants.SIM3TX, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM3TX)));
            cv2.put(Constants.TOTAL1, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL1)));
            cv2.put(Constants.TOTAL2, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL2)));
            cv2.put(Constants.TOTAL3, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL3)));
            cv2.put(Constants.PERIOD1, cursorToDayBeforeDate.getInt(cursorToDayBeforeDate.getColumnIndex(Constants.PERIOD1)));
            cv2.put(Constants.PERIOD2, cursorToDayBeforeDate.getInt(cursorToDayBeforeDate.getColumnIndex(Constants.PERIOD2)));
            cv2.put(Constants.PERIOD3, cursorToDayBeforeDate.getInt(cursorToDayBeforeDate.getColumnIndex(Constants.PERIOD3)));
            cv2.put(Constants.SIM1RX_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM1RX_N)));
            cv2.put(Constants.SIM2RX_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM2RX_N)));
            cv2.put(Constants.SIM3RX_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM3RX_N)));
            cv2.put(Constants.SIM1TX_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM1TX_N)));
            cv2.put(Constants.SIM2TX_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM2TX_N)));
            cv2.put(Constants.SIM3TX_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM3TX_N)));
            cv2.put(Constants.TOTAL1_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL1_N)));
            cv2.put(Constants.TOTAL2_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL2_N)));
            cv2.put(Constants.TOTAL3_N, cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL3_N)));
        }
        switch (sim) {
            case Constants.SIM1:
                switch (prefs.getString(Constants.PREF_SIM1[3], "0")) {
                    case "0":
                        if (cv1.size() > 0) {
                            out.putLong("rx", (long) cv1.get(Constants.SIM1RX));
                            out.putLong("tx", (long) cv1.get(Constants.SIM1TX));
                            out.putLong("tot", (long) cv1.get(Constants.TOTAL1));
                            out.putLong("rx_n", (long) cv1.get(Constants.SIM1RX_N));
                            out.putLong("tx_n", (long) cv1.get(Constants.SIM1TX_N));
                            out.putLong("tot_n", (long) cv1.get(Constants.TOTAL1_N));
                        } else
                            return null;
                        break;
                    case "1":
                        if (queried.getDayOfMonth() != Integer.valueOf(prefs.getString(Constants.PREF_SIM1[10], "1")))
                            if (cv1.size() > 0 && cv2.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM1RX) - (long) cv2.get(Constants.SIM1RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM1TX) - (long) cv2.get(Constants.SIM1TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL1) - (long) cv2.get(Constants.TOTAL1));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM1RX_N) - (long) cv2.get(Constants.SIM1RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM1TX_N) - (long) cv2.get(Constants.SIM1TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL1_N) - (long) cv2.get(Constants.TOTAL1_N));
                            } else if (cv1.size() > 0 && cv2.size() == 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM1RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM1TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL1));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM1RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM1TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL1_N));
                            } else
                                return null;
                        else {
                            if (cv1.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM1RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM1TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL1));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM1RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM1TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL1_N));
                            } else
                                return null;
                        }
                        break;
                    case "2":
                        if ((int) cv1.get(Constants.PERIOD1) == 0) {
                            if (cv1.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM1RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM1TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL1));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM1RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM1TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL1_N));
                            } else
                                return null;
                        } else {
                            if (cv1.size() > 0 && cv2.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM1RX) - (long) cv2.get(Constants.SIM1RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM1TX) - (long) cv2.get(Constants.SIM1TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL1) - (long) cv2.get(Constants.TOTAL1));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM1RX_N) - (long) cv2.get(Constants.SIM1RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM1TX_N) - (long) cv2.get(Constants.SIM1TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL1_N) - (long) cv2.get(Constants.TOTAL1_N));
                            }  else if (cv1.size() > 0 && cv2.size() == 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM1RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM1TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL1));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM1RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM1TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL1_N));
                            } else
                                return null;
                        }
                        break;
                }
                break;
            case Constants.SIM2:
                switch (prefs.getString(Constants.PREF_SIM2[3], "0")) {
                    case "0":
                        if (cv1.size() > 0) {
                            out.putLong("rx", (long) cv1.get(Constants.SIM2RX));
                            out.putLong("tx", (long) cv1.get(Constants.SIM2TX));
                            out.putLong("tot", (long) cv1.get(Constants.TOTAL2));
                            out.putLong("rx_n", (long) cv1.get(Constants.SIM2RX_N));
                            out.putLong("tx_n", (long) cv1.get(Constants.SIM2TX_N));
                            out.putLong("tot_n", (long) cv1.get(Constants.TOTAL2_N));
                        } else
                            return null;
                        break;
                    case "1":
                        if (queried.getDayOfMonth() != Integer.valueOf(prefs.getString(Constants.PREF_SIM2[10], "1")))
                            if (cv1.size() > 0 && cv2.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM2RX) - (long) cv2.get(Constants.SIM2RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM2TX) - (long) cv2.get(Constants.SIM2TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL2) - (long) cv2.get(Constants.TOTAL2));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM2RX_N) - (long) cv2.get(Constants.SIM2RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM2TX_N) - (long) cv2.get(Constants.SIM2TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL2_N) - (long) cv2.get(Constants.TOTAL2_N));
                            } else if (cv1.size() > 0 && cv2.size() == 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM2RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM2TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL2));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM2RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM2TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL2_N));
                            } else
                                return null;
                        else {
                            if (cv1.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM2RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM2TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL2));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM2RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM2TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL2_N));
                            } else
                                return null;
                        }
                        break;
                    case "2":
                        if ((int) cv1.get(Constants.PERIOD2) == 0)
                            if (cv1.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM2RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM2TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL2));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM2RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM2TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL2_N));
                            } else
                                return null;
                        else {
                            if (cv1.size() > 0 && cv2.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM2RX) - (long) cv2.get(Constants.SIM2RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM2TX) - (long) cv2.get(Constants.SIM2TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL2) - (long) cv2.get(Constants.TOTAL2));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM2RX_N) - (long) cv2.get(Constants.SIM2RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM2TX_N) - (long) cv2.get(Constants.SIM2TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL2_N) - (long) cv2.get(Constants.TOTAL2_N));
                            } else if (cv1.size() > 0 && cv2.size() == 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM2RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM2TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL2));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM2RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM2TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL2_N));
                            } else
                                return null;
                        }
                        break;
                }
                break;
            case Constants.SIM3:
                switch (prefs.getString(Constants.PREF_SIM3[3], "0")) {
                    case "0":
                        if (cv1.size() > 0) {
                            out.putLong("rx", (long) cv1.get(Constants.SIM3RX));
                            out.putLong("tx", (long) cv1.get(Constants.SIM3TX));
                            out.putLong("tot", (long) cv1.get(Constants.TOTAL3));
                            out.putLong("rx_n", (long) cv1.get(Constants.SIM3RX_N));
                            out.putLong("tx_n", (long) cv1.get(Constants.SIM3TX_N));
                            out.putLong("tot_n", (long) cv1.get(Constants.TOTAL3_N));
                        } else
                            return null;
                        break;
                    case "1":
                        if (queried.getDayOfMonth() != Integer.valueOf(prefs.getString(Constants.PREF_SIM3[10], "1")))
                            if (cv1.size() > 0 && cv2.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM3RX) - (long) cv2.get(Constants.SIM3RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM3TX) - (long) cv2.get(Constants.SIM3TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL3) - (long) cv2.get(Constants.TOTAL3));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM3RX_N) - (long) cv2.get(Constants.SIM3RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM3TX_N) - (long) cv2.get(Constants.SIM3TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL3_N) - (long) cv2.get(Constants.TOTAL3_N));
                            } else if (cv1.size() > 0 && cv2.size() == 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM3RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM3TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL3));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM3RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM3TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL3_N));
                            } else
                                return null;
                        else {
                            if (cv1.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM3RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM3TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL3));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM3RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM3TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL3_N));
                            } else
                                return null;
                        }
                        break;
                    case "2":
                        if ((int) cv1.get(Constants.PERIOD3) == 0)
                            if (cv1.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM3RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM3TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL3));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM3RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM3TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL3_N));
                            } else
                                return null;
                        else {
                            if (cv1.size() > 0 && cv2.size() > 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM3RX) - (long) cv2.get(Constants.SIM3RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM3TX) - (long) cv2.get(Constants.SIM3TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL3) - (long) cv2.get(Constants.TOTAL3));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM3RX_N) - (long) cv2.get(Constants.SIM3RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM3TX_N) - (long) cv2.get(Constants.SIM3TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL3_N) - (long) cv2.get(Constants.TOTAL3_N));
                            } else if (cv1.size() > 0 && cv2.size() == 0) {
                                out.putLong("rx", (long) cv1.get(Constants.SIM3RX));
                                out.putLong("tx", (long) cv1.get(Constants.SIM3TX));
                                out.putLong("tot", (long) cv1.get(Constants.TOTAL3));
                                out.putLong("rx_n", (long) cv1.get(Constants.SIM3RX_N));
                                out.putLong("tx_n", (long) cv1.get(Constants.SIM3TX_N));
                                out.putLong("tot_n", (long) cv1.get(Constants.TOTAL3_N));
                            } else
                                return null;
                        }
                        break;
                }
                break;
        }
        cursorToDate.close();
        cursorToDayBeforeDate.close();
        return out;
    }

    public static ContentValues readCallsData(CustomDatabaseHelper dbHelper) {
        ContentValues cv = new ContentValues();
        mSqLiteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(CALLS_TABLE, new String[]{Constants.LAST_DATE, Constants.LAST_TIME, Constants.CALLS1,
                Constants.CALLS1_EX, Constants.CALLS2, Constants.CALLS2_EX, Constants.CALLS3, Constants.CALLS3_EX,
                Constants.PERIOD1, Constants.PERIOD2, Constants.PERIOD3}, null, null, null, null, null);
        if (cursor.moveToLast()) {
            cv.put(Constants.CALLS1, cursor.getLong(cursor.getColumnIndex(Constants.CALLS1)));
            cv.put(Constants.CALLS1_EX, cursor.getLong(cursor.getColumnIndex(Constants.CALLS1_EX)));
            cv.put(Constants.CALLS2, cursor.getLong(cursor.getColumnIndex(Constants.CALLS2)));
            cv.put(Constants.CALLS2_EX, cursor.getLong(cursor.getColumnIndex(Constants.CALLS2_EX)));
            cv.put(Constants.CALLS3, cursor.getLong(cursor.getColumnIndex(Constants.CALLS3)));
            cv.put(Constants.CALLS3_EX, cursor.getLong(cursor.getColumnIndex(Constants.CALLS3_EX)));
            cv.put(Constants.LAST_TIME, cursor.getString(cursor.getColumnIndex(Constants.LAST_TIME)));
            cv.put(Constants.LAST_DATE, cursor.getString(cursor.getColumnIndex(Constants.LAST_DATE)));
            cv.put(Constants.PERIOD1, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD1)));
            cv.put(Constants.PERIOD2, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD2)));
            cv.put(Constants.PERIOD3, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD3)));
        } else {
            cv.put(Constants.CALLS1, 0L);
            cv.put(Constants.CALLS1_EX, 0L);
            cv.put(Constants.CALLS2, 0L);
            cv.put(Constants.CALLS2_EX, 0L);
            cv.put(Constants.CALLS3, 0L);
            cv.put(Constants.CALLS3_EX, 0L);
            cv.put(Constants.LAST_TIME, "");
            cv.put(Constants.LAST_DATE, "");
            cv.put(Constants.PERIOD1, 0);
            cv.put(Constants.PERIOD2, 0);
            cv.put(Constants.PERIOD3, 0);
        }
        cursor.close();
        return cv;
    }

    public static void writeCallsData(ContentValues mCalls, CustomDatabaseHelper dbHelper) {
        mSqLiteDatabase = dbHelper.getWritableDatabase();
        String filter = Constants.LAST_DATE + "='" + mCalls.get(Constants.LAST_DATE) + "'";
        int id = mSqLiteDatabase.update(CALLS_TABLE, mCalls, filter, null);
        if (id == 0)
            mSqLiteDatabase.insert(CALLS_TABLE, null, mCalls);
    }

    public static void writeWhiteList(int sim, ArrayList<String> list, CustomDatabaseHelper dbHelper) {
        mSqLiteDatabase = dbHelper.getWritableDatabase();
        String table = "";
        switch (sim) {
            case Constants.SIM1:
                table = WHITE_LIST_1;
                break;
            case Constants.SIM2:
                table = WHITE_LIST_2;
                break;
            case Constants.SIM3:
                table = WHITE_LIST_3;
                break;
        }
        if (sim >= 0) {
            mSqLiteDatabase.execSQL("DELETE FROM " + table);
            for (String s : list) {
                ContentValues cv = new ContentValues();
                cv.put(Constants.NUMBER, s);
                mSqLiteDatabase.insert(table, null, cv);
            }
        }
    }

    public static ArrayList<String> readWhiteList(int sim, CustomDatabaseHelper dbHelper) {
        ArrayList<String> list = new ArrayList<>();
        mSqLiteDatabase = dbHelper.getReadableDatabase();
        String table = "";
        switch (sim) {
            case Constants.SIM1:
                table = WHITE_LIST_1;
                break;
            case Constants.SIM2:
                table = WHITE_LIST_2;
                break;
            case Constants.SIM3:
                table = WHITE_LIST_3;
                break;
        }
        if (sim >= 0) {
            try {
                Cursor cursor = mSqLiteDatabase.query(table, new String[]{Constants.NUMBER}, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        list.add(cursor.getString(cursor.getColumnIndex(Constants.NUMBER)));
                        cursor.moveToNext();
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static void writeBlackList(int sim, ArrayList<String> list, CustomDatabaseHelper dbHelper) {
        mSqLiteDatabase = dbHelper.getWritableDatabase();
        String table = "";
        switch (sim) {
            case Constants.SIM1:
                table = BLACK_LIST_1;
                break;
            case Constants.SIM2:
                table = BLACK_LIST_2;
                break;
            case Constants.SIM3:
                table = BLACK_LIST_3;
                break;
        }
        if (sim >= 0) {
            mSqLiteDatabase.execSQL("DELETE FROM " + table);
            for (String s : list) {
                ContentValues cv = new ContentValues();
                cv.put(Constants.NUMBER, s);
                mSqLiteDatabase.insert(table, null, cv);
            }
        }
    }

    public static ArrayList<String> readBlackList(int sim, CustomDatabaseHelper dbHelper) {
        ArrayList<String> list = new ArrayList<>();
        mSqLiteDatabase = dbHelper.getReadableDatabase();
        String table = "";
        switch (sim) {
            case Constants.SIM1:
                table = BLACK_LIST_1;
                break;
            case Constants.SIM2:
                table = BLACK_LIST_2;
                break;
            case Constants.SIM3:
                table = BLACK_LIST_3;
                break;
        }
        if (sim >= 0) {
            try {
                Cursor cursor = mSqLiteDatabase.query(table, new String[]{Constants.NUMBER}, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        list.add(cursor.getString(cursor.getColumnIndex(Constants.NUMBER)));
                        cursor.moveToNext();
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}