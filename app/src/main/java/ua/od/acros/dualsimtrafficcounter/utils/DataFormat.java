package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;

import ua.od.acros.dualsimtrafficcounter.R;

public class DataFormat {

    static double kb = 1024.0;
    static double mb = kb * kb;
    static double gb = kb * mb;

    public static String formatData (Context mContext, long data) {
        if (data < mb)
            return (String.format("%.2f", data / kb)) + mContext.getString(R.string.kb);
        else if (data > gb)
            return (String.format("%.2f", data / gb)) + mContext.getString(R.string.gb);
        else
            return (String.format("%.2f", data / mb)) + mContext.getString(R.string.mb);
    }

    public static long getFormatLong (String data, int value) {
        long res = 0;
        double dData = Double.valueOf(data);
        switch (value) {
            case 0:
                res = (long) (dData * kb);
                break;
            case 1:
                res = (long) (dData * mb);
                break;
            case 2:
                res = (long) (dData * gb);
                break;
        }
        return res;
    }

    public static double getFormatDouble (String data, int value) {
        double res = 0;
        double dData = Double.valueOf(data);
        switch (value) {
            case 0:
                res = dData / kb;
                break;
            case 1:
                res = dData / mb;
                break;
            case 2:
                res = dData / gb;
                break;
        }
        return res;
    }
}
