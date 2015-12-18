package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Map;

public class TrafficDatabase extends SQLiteOpenHelper {


    private static final String DATABASE_TABLE = "data";
    private static SQLiteDatabase mSqLiteDatabase;

    private static final String DATABASE_CREATE_SCRIPT = "create table "
            + DATABASE_TABLE + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
            + " text not null, " + Constants.LAST_ACTIVE_SIM + " integer, "
            + Constants.LAST_RX + " long, " + Constants.LAST_TX + " long, "
            + Constants.SIM1RX + " long, " + Constants.SIM1TX + " long, "
            + Constants.TOTAL1 + " long, " + Constants.SIM2RX + " long, "
            + Constants.SIM2TX + " long, " + Constants.TOTAL2 + " long, "
            + Constants.SIM3RX + " long, " + Constants.SIM3TX + " long, "
            + Constants.TOTAL3 + " long, " + Constants.PERIOD1 + " integer,"
            + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer);";

    public TrafficDatabase(Context context) {
        super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
    }

    public TrafficDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory,
                           int version) {
        super(context, name, factory, version);
    }

    public TrafficDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory,
                           int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String ALTER_TBL = "";
        if (oldVersion < Constants.DATABASE_VERSION && oldVersion == 1) {
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.SIM3RX + " long;";
            db.execSQL(ALTER_TBL);
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.SIM3TX + " long;";
            db.execSQL(ALTER_TBL);
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.TOTAL3 + " long;";
            db.execSQL(ALTER_TBL);
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.PERIOD1 + " integer;";
            db.execSQL(ALTER_TBL);
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.PERIOD2 + " integer;";
            db.execSQL(ALTER_TBL);
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.PERIOD3 + " integer;";
            db.execSQL(ALTER_TBL);
        }

        if (oldVersion < Constants.DATABASE_VERSION && oldVersion == 2) {
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.PERIOD1 + " integer;";
            db.execSQL(ALTER_TBL);
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.PERIOD2 + " integer;";
            db.execSQL(ALTER_TBL);
            ALTER_TBL =
                    "ALTER TABLE " + DATABASE_TABLE +
                            " ADD COLUMN " + Constants.PERIOD3 + " integer;";
            db.execSQL(ALTER_TBL);
        }
    }

    public static Map<String, Object> read_writeTrafficData(int r_w, Map<String, Object> mMap, TrafficDatabase db) {

        if (r_w == Constants.WRITE) {
            ContentValues values = new ContentValues();
            values.put(Constants.SIM1RX, (Long) mMap.get(Constants.SIM1RX));
            values.put(Constants.SIM2RX, (Long) mMap.get(Constants.SIM2RX));
            values.put(Constants.SIM3RX, (Long) mMap.get(Constants.SIM3RX));
            values.put(Constants.SIM1TX, (Long) mMap.get(Constants.SIM1TX));
            values.put(Constants.SIM2TX, (Long) mMap.get(Constants.SIM2TX));
            values.put(Constants.SIM3TX, (Long) mMap.get(Constants.SIM3TX));
            values.put(Constants.TOTAL1, (Long) mMap.get(Constants.TOTAL1));
            values.put(Constants.TOTAL2, (Long) mMap.get(Constants.TOTAL2));
            values.put(Constants.TOTAL3, (Long) mMap.get(Constants.TOTAL3));
            values.put(Constants.LAST_ACTIVE_SIM, (int) mMap.get(Constants.LAST_ACTIVE_SIM));
            values.put(Constants.LAST_TX, (Long) mMap.get(Constants.LAST_TX));
            values.put(Constants.LAST_RX, (Long) mMap.get(Constants.LAST_RX));
            values.put(Constants.LAST_TIME, (String) mMap.get(Constants.LAST_TIME));
            values.put(Constants.LAST_DATE, (String) mMap.get(Constants.LAST_DATE));
            values.put(Constants.PERIOD1, (int) mMap.get(Constants.PERIOD1));
            values.put(Constants.PERIOD2, (int) mMap.get(Constants.PERIOD2));
            values.put(Constants.PERIOD3, (int) mMap.get(Constants.PERIOD3));

            mSqLiteDatabase = db.getWritableDatabase();
            mSqLiteDatabase.insert(DATABASE_TABLE, null, values);
        } else if (r_w == Constants.READ) {
            mSqLiteDatabase = db.getReadableDatabase();
            Cursor cursor = mSqLiteDatabase.query(DATABASE_TABLE, new String[] {Constants.LAST_DATE, Constants.LAST_TIME, Constants.LAST_ACTIVE_SIM,
                    Constants.LAST_RX, Constants.LAST_TX, Constants.SIM1RX, Constants.SIM1TX,Constants.TOTAL1,
                    Constants.SIM2RX, Constants.SIM2TX, Constants.TOTAL2, Constants.SIM3RX, Constants.SIM3TX,
                    Constants.TOTAL3, Constants.PERIOD1, Constants.PERIOD2, Constants.PERIOD3}, null, null, null, null, null);
            if (cursor.moveToLast()) {
                mMap.put(Constants.SIM1RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM1RX)));
                mMap.put(Constants.SIM2RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM2RX)));
                mMap.put(Constants.SIM3RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM3RX)));
                mMap.put(Constants.SIM1TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM1TX)));
                mMap.put(Constants.SIM2TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM2TX)));
                mMap.put(Constants.SIM3TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM3TX)));
                mMap.put(Constants.TOTAL1, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL1)));
                mMap.put(Constants.TOTAL2, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL2)));
                mMap.put(Constants.TOTAL3, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL3)));
                mMap.put(Constants.LAST_ACTIVE_SIM, cursor.getInt(cursor.getColumnIndex(Constants.LAST_ACTIVE_SIM)));
                mMap.put(Constants.LAST_RX, cursor.getLong(cursor.getColumnIndex(Constants.LAST_RX)));
                mMap.put(Constants.LAST_TX, cursor.getLong(cursor.getColumnIndex(Constants.LAST_TX)));
                mMap.put(Constants.LAST_TIME, cursor.getString(cursor.getColumnIndex(Constants.LAST_TIME)));
                mMap.put(Constants.LAST_DATE, cursor.getString(cursor.getColumnIndex(Constants.LAST_DATE)));
                mMap.put(Constants.PERIOD1, cursor.getLong(cursor.getColumnIndex(Constants.PERIOD1)));
                mMap.put(Constants.PERIOD2, cursor.getLong(cursor.getColumnIndex(Constants.PERIOD2)));
                mMap.put(Constants.PERIOD3, cursor.getLong(cursor.getColumnIndex(Constants.PERIOD3)));

            } else {
                mMap.put(Constants.SIM1RX, 0L);
                mMap.put(Constants.SIM2RX, 0L);
                mMap.put(Constants.SIM3RX, 0L);
                mMap.put(Constants.SIM1TX, 0L);
                mMap.put(Constants.SIM2TX, 0L);
                mMap.put(Constants.SIM3TX, 0L);
                mMap.put(Constants.TOTAL1, 0L);
                mMap.put(Constants.TOTAL2, 0L);
                mMap.put(Constants.TOTAL3, 0L);
                mMap.put(Constants.LAST_ACTIVE_SIM, 0);
                mMap.put(Constants.LAST_RX, 0L);
                mMap.put(Constants.LAST_TX, 0L);
                mMap.put(Constants.LAST_TIME, "");
                mMap.put(Constants.LAST_DATE, "");
                mMap.put(Constants.PERIOD1, 0);
                mMap.put(Constants.PERIOD2, 0);
                mMap.put(Constants.PERIOD3, 0);
            }
            cursor.close();
        } else if (r_w ==Constants.UPDATE) {
            ContentValues values = new ContentValues();
            values.put(Constants.SIM1RX, (Long) mMap.get(Constants.SIM1RX));
            values.put(Constants.SIM2RX, (Long) mMap.get(Constants.SIM2RX));
            values.put(Constants.SIM3RX, (Long) mMap.get(Constants.SIM3RX));
            values.put(Constants.SIM1TX, (Long) mMap.get(Constants.SIM1TX));
            values.put(Constants.SIM2TX, (Long) mMap.get(Constants.SIM2TX));
            values.put(Constants.SIM3TX, (Long) mMap.get(Constants.SIM3TX));
            values.put(Constants.TOTAL1, (Long) mMap.get(Constants.TOTAL1));
            values.put(Constants.TOTAL2, (Long) mMap.get(Constants.TOTAL2));
            values.put(Constants.TOTAL3, (Long) mMap.get(Constants.TOTAL3));
            values.put(Constants.LAST_ACTIVE_SIM, (int) mMap.get(Constants.LAST_ACTIVE_SIM));
            values.put(Constants.LAST_TX, (Long) mMap.get(Constants.LAST_TX));
            values.put(Constants.LAST_RX, (Long) mMap.get(Constants.LAST_RX));
            values.put(Constants.LAST_TIME, (String) mMap.get(Constants.LAST_TIME));
            values.put(Constants.LAST_DATE, (String) mMap.get(Constants.LAST_DATE));
            values.put(Constants.PERIOD1, (int) mMap.get(Constants.PERIOD1));
            values.put(Constants.PERIOD2, (int) mMap.get(Constants.PERIOD2));
            values.put(Constants.PERIOD3, (int) mMap.get(Constants.PERIOD3));
            mSqLiteDatabase = db.getWritableDatabase();
            String filter = Constants.LAST_DATE + "='" + mMap.get(Constants.LAST_DATE) + "'";
            mSqLiteDatabase.update(DATABASE_TABLE, values, filter, null);
        }
        return mMap;

    }

    public static boolean isEmpty(TrafficDatabase db) {
        boolean result;
        mSqLiteDatabase = db.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(DATABASE_TABLE, new String[] {Constants.LAST_DATE, Constants.LAST_TIME, Constants.LAST_ACTIVE_SIM,
                Constants.LAST_RX, Constants.LAST_TX, Constants.SIM1RX, Constants.SIM1TX, Constants.TOTAL1,
                Constants.SIM2RX, Constants.SIM2TX, Constants.TOTAL2, Constants.SIM3RX, Constants.SIM3TX,
                Constants.TOTAL3, Constants.PERIOD1, Constants.PERIOD2, Constants.PERIOD3}, null, null, null, null, null);
        result =  cursor != null && cursor.getCount() == 0;
        if (cursor != null) {
            cursor.close();
        }
        return result;
    }

    /*public static void cleanDB (TrafficDatabase db, String date, String time) {
        mSqLiteDatabase = db.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(DATABASE_TABLE, new String[] {Constants.LAST_DATE, Constants.LAST_TIME, Constants.LAST_ACTIVE_SIM,
                Constants.LAST_RX, Constants.LAST_TX, Constants.SIM1RX, Constants.SIM1TX, Constants.TOTAL1,
                Constants.SIM2RX, Constants.SIM2TX, Constants.TOTAL2, Constants.SIM3RX, Constants.SIM3TX, Constants.TOTAL3}, "date=? AND time=?", new String[] {date, time}, null, null, null);
        if (cursor != null && cursor.getCount() > 1) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount() - 1; i++) {
                cursor.re
                cursor.moveToNext();
            }
            cursor.close();
        }
    }*/

}
