package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;

import ua.od.acros.dualsimtrafficcounter.R;

public class DataFormat {

    private final static double KB = 1024.0;
    private final static double MB = KB * KB;
    private final static double GB = KB * MB;

    public static String formatData(Context mContext, long data) {
        if (data < MB)
            return (String.format("%.2f", data / KB)) + mContext.getString(R.string.kb);
        else if (data > GB)
            return (String.format("%.2f", data / GB)) + mContext.getString(R.string.gb);
        else
            return (String.format("%.2f", data / MB)) + mContext.getString(R.string.mb);
    }

    public static long getFormatLong(String data, int value) {
        long res = 0;
        double dData = Double.valueOf(data);
        switch (value) {
            case 0:
                res = (long) (dData * KB);
                break;
            case 1:
                res = (long) (dData * MB);
                break;
            case 2:
                res = (long) (dData * GB);
                break;
    }
    return res;
}

    public static long getRoundLong(long data, String round, String value) {
        double div = Double.valueOf(round);
        switch (value) {
            case "0":
                div = div * KB;
                break;
            case "1":
                div = div * MB;
                break;
            case "2":
                div = div * KB;
                break;
        }
        return (long) (Math.ceil(data / div) * div);
    }
}
