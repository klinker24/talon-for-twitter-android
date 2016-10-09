package com.afollestad.materialcamera.util;

import android.os.Build;

/**
 * This class exists to provide a place to define device specific information as some
 * manufacturers/devices require specific camera setup/requirements.
 */
public class ManufacturerUtil {

    public ManufacturerUtil() {
    }

    // Samsung device info
    private static final String SAMSUNG_MANUFACTURER = "samsung";

    // Samsung Galaxy S3 info
    private static final String SAMSUNG_S3_DEVICE_COMMON_PREFIX = "d2";
    public static final Integer SAMSUNG_S3_PREVIEW_WIDTH = 640;
    public static final Integer SAMSUNG_S3_PREVIEW_HEIGHT = 480;

    // Samsung Galaxy helper functions
    static boolean isSamsungDevice() {
        return SAMSUNG_MANUFACTURER.equals(Build.MANUFACTURER.toLowerCase());
    }

    public static boolean isSamsungGalaxyS3() {
        return Build.DEVICE.startsWith(SAMSUNG_S3_DEVICE_COMMON_PREFIX);
    }
}
