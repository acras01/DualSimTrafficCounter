package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class XposedUtils {

    public static boolean isPackageExisted(Context context, String targetPackage){
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

}
