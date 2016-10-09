package com.afollestad.materialcamera.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.materialcamera.internal.BaseCaptureActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author Aidan Follestad (afollestad)
 */
public class CameraUtil {

    private CameraUtil() {
    }

    public static boolean isChromium() {
        return Build.BRAND.equalsIgnoreCase("chromium") &&
                Build.MANUFACTURER.equalsIgnoreCase("chromium");
    }

    public static String getDurationString(long durationMs) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(durationMs),
                TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMs))
        );
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public static File makeTempFile(@NonNull Context context, @Nullable String saveDir, String prefix, String extension) {
        if (saveDir == null)
            saveDir = context.getExternalCacheDir().getAbsolutePath();
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final File dir = new File(saveDir);
        dir.mkdirs();
        return new File(dir, prefix + timeStamp + extension);
    }

    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    public static List<Integer> getSupportedFlashModes(Context context, Camera.Parameters parameters) {
        //check has system feature for flash
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            List<String> modes = parameters.getSupportedFlashModes();
            if (modes == null || (modes.size() == 1 && modes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF))) {
                return null; //not supported
            } else {
                ArrayList<Integer> flashModes = new ArrayList<>();
                for (String mode : modes) {
                    switch (mode) {
                        case Camera.Parameters.FLASH_MODE_AUTO:
                            if (!flashModes.contains(BaseCaptureActivity.FLASH_MODE_AUTO))
                                flashModes.add(BaseCaptureActivity.FLASH_MODE_AUTO);
                            break;
                        case Camera.Parameters.FLASH_MODE_ON:
                            if (!flashModes.contains(BaseCaptureActivity.FLASH_MODE_ALWAYS_ON))
                                flashModes.add(BaseCaptureActivity.FLASH_MODE_ALWAYS_ON);
                            break;
                        case Camera.Parameters.FLASH_MODE_OFF:
                            if (!flashModes.contains(BaseCaptureActivity.FLASH_MODE_OFF))
                                flashModes.add(BaseCaptureActivity.FLASH_MODE_OFF);
                            break;
                        default:
                            break;
                    }
                }
                return flashModes;
            }
        } else {
            return null; //not supported
        }
    }

    // TODO: Take a hard look at how this works
    // Camera2
    public static List<Integer> getSupportedFlashModes(Context context, CameraCharacteristics characteristics) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null; //doesn't support camera2
        } else if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (flashAvailable == null || !flashAvailable)
                return null;

            int[] modes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            if (modes == null || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AE_MODE_OFF)) {
                return null; //not supported
            } else {
                ArrayList<Integer> flashModes = new ArrayList<>(3);
                for (int mode : modes) {
                    switch (mode) {
                        case CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH:
                            if (!flashModes.contains(BaseCaptureActivity.FLASH_MODE_AUTO))
                                flashModes.add(BaseCaptureActivity.FLASH_MODE_AUTO);
                            break;
                        case CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH:
                            if (!flashModes.contains(BaseCaptureActivity.FLASH_MODE_ALWAYS_ON))
                                flashModes.add(BaseCaptureActivity.FLASH_MODE_ALWAYS_ON);
                            break;
                        case CameraCharacteristics.CONTROL_AE_MODE_ON:
                            if (!flashModes.contains(BaseCaptureActivity.FLASH_MODE_OFF))
                                flashModes.add(BaseCaptureActivity.FLASH_MODE_OFF);
                        default:
                            break;
                    }
                }
                return flashModes;
            }
        }
        return null; //not supported
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean hasCamera2(Context context, boolean stillShot) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
        if (stillShot && ManufacturerUtil.isSamsungDevice()) return false;
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] idList = manager.getCameraIdList();
            boolean notNull = true;
            if (idList.length == 0) {
                notNull = false;
            } else {
                for (final String str : idList) {
                    if (str == null || str.trim().isEmpty()) {
                        notNull = false;
                        break;
                    }
                    final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);
                    //noinspection ConstantConditions
                    final int supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false;
                        break;
                    }
                }
            }
            return notNull;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @ColorInt
    public static int darkenColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        color = Color.HSVToColor(hsv);
        return color;
    }

    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    public static int adjustAlpha(int color, @SuppressWarnings("SameParameterValue") float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}