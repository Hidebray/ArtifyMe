package com.sevengroup.artifyme.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class PermissionUtils {
    public static String getReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    public static boolean hasReadPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, getReadPermission())
                == PackageManager.PERMISSION_GRANTED;
    }
}