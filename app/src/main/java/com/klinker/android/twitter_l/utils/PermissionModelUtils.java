package com.klinker.android.twitter_l.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;

import com.klinker.android.twitter_l.R;

public class PermissionModelUtils {

    public static final String[] NECESSARY_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String PERMISSION_CHECK_PREF = "marshmallow_permission_check";

    private Context context;
    private SharedPreferences sharedPrefs;

    public PermissionModelUtils(Context context) {
        this.context = context;
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean needPermissionCheck() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                sharedPrefs.getBoolean(PERMISSION_CHECK_PREF, true);
    }

    public void showPermissionExplanationThenAuthorization() {
        new AlertDialog.Builder(context)
                .setTitle(R.string.permission_check_title)
                .setMessage(R.string.permission_check_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions();
                        sharedPrefs.edit().putBoolean(PERMISSION_CHECK_PREF, false).apply();
                    }
                })
                .setCancelable(false)
                .create().show();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            ((Activity)context).requestPermissions(NECESSARY_PERMISSIONS, 1);
    }

    public void showStorageIssue(Throwable e) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new AlertDialog.Builder(context)
                    .setTitle("Storage Permission")
                    .setMessage("Talon needs the storage permission to complete this. Please grant this permission, then retry.")
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((Activity) context).requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    })
                    .create().show();
        } else {
            new AlertDialog.Builder(context)
                    .setTitle("Something went wrong")
                    .setMessage("Here is the description: " + e.getMessage())
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create().show();
        }
    }

    public void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((Activity) context).requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    public void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((Activity) context).requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
}
