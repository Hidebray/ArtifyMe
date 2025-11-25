package com.sevengroup.artifyme.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class StorageUtils {
    public static String copyImageToAppStorage(Context context, Uri sourceUri) {
        try {
            InputStream in = context.getContentResolver().openInputStream(sourceUri);
            String fileName = AppConstants.FILE_PREFIX + System.currentTimeMillis() + ".jpg";
            File file = new File(context.getFilesDir(), fileName);
            try (OutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
            if (in != null) in.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean exportFileToPublicGallery(Context context, String sourceImagePath) {
        File sourceFile = new File(sourceImagePath);
        if (!sourceFile.exists()) return false;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "ArtifyMe");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        } else {
            File albumDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ArtifyMe");
            if (!albumDir.exists()) albumDir.mkdirs();
            values.put(MediaStore.Images.Media.DATA, new File(albumDir, sourceFile.getName()).getAbsolutePath());
        }

        try {
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return false;

            try (OutputStream out = context.getContentResolver().openOutputStream(uri);
                 InputStream in = new FileInputStream(sourceFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}