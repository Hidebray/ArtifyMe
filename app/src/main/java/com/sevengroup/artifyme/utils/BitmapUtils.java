package com.sevengroup.artifyme.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class BitmapUtils {
    private static final int MAX_DIMENSION = 2048;

    public static Bitmap loadSafeBitmap(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            int height = options.outHeight;
            int width = options.outWidth;
            int inSampleSize = 1;

            while (height > MAX_DIMENSION || width > MAX_DIMENSION) {
                height /= 2;
                width /= 2;
                inSampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            return BitmapFactory.decodeFile(imagePath, options);
        } catch (Exception e) {
            return null;
        }
    }

    public static String saveBitmapToAppStorage(Context context, Bitmap bitmapToSave) {
        if (bitmapToSave == null) return null;
        try {
            String fileName = AppConstants.FILE_PREFIX + System.currentTimeMillis() + ".jpg";
            File file = new File(context.getFilesDir(), fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static Pair<Integer, Integer> getImageResolution(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imagePath, options);

        int width = options.outWidth;
        int height = options.outHeight;

        return new Pair<>(width, height);
    }

    public static long getImageSizeInByte(String imagePath) {
        File imageFile = new File(imagePath);
        return imageFile.exists() ? imageFile.length() : 0L;
    }
}