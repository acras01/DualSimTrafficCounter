package ua.od.acros.dualsimtrafficcounter.utils;

import android.os.Build;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MTKUtils {

    private static Boolean mIsMtkDevice = null;
    private static Boolean mHasGeminiSupport = null;
    // Supported MTK devices
    private static final Set<String> MTK_DEVICES = new HashSet<String>(Arrays.asList(
            new String[]{
                    // Single-core SoC
                    "mt6575",
                    // Dual-core SoC
                    "mt6572",
                    "mt6577",
                    "mt8377",
                    // Quad-core SoC
                    "mt6582",
                    "mt6589",
                    "mt8389",
                    // Octa-core SoC
                    "mt6592"
            }
    ));

    public static boolean isMtkDevice() {
        if (mIsMtkDevice != null) return mIsMtkDevice;

        mIsMtkDevice = MTK_DEVICES.contains(Build.HARDWARE.toLowerCase());
        return mIsMtkDevice;
    }

    public static boolean hasGeminiSupport() {
        if (mHasGeminiSupport != null) return mHasGeminiSupport;
        mHasGeminiSupport = com.mediatek.compatibility.gemini.GeminiSupport.isGeminiFeatureEnabled();
        return mHasGeminiSupport;
    }
}
