package com.sevengroup.artifyme.managers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.nio.ByteBuffer;

public class RemoveBgLocalManager {

    private final Segmenter segmenter;

    public interface OnRemoveBgListener {
        void onResult(Bitmap bitmap);
    }

    public RemoveBgLocalManager() {
        SelfieSegmenterOptions options = new SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build();
        segmenter = Segmentation.getClient(options);
    }

    public void removeBackground(Bitmap inputBitmap, OnRemoveBgListener listener) {
        if (inputBitmap == null) {
            listener.onResult(null);
            return;
        }

        InputImage image = InputImage.fromBitmap(inputBitmap, 0);

        segmenter.process(image)
                .addOnSuccessListener(segmentationMask -> {
                    ByteBuffer buffer = segmentationMask.getBuffer();
                    int width = segmentationMask.getWidth();
                    int height = segmentationMask.getHeight();

                    int totalPixels = width * height;
                    int foregroundPixels = 0;

                    int[] colors = new int[totalPixels];

                    for (int i = 0; i < totalPixels; i++) {
                        float confidence = buffer.getFloat();
                        if (confidence > 0.5f) {
                            colors[i] = Color.BLACK;
                            foregroundPixels++;
                        } else {
                            colors[i] = Color.TRANSPARENT;
                        }
                    }

                    float coverage = (float) foregroundPixels / totalPixels;
                    Log.d("RemoveBgLocal", "Tỉ lệ chủ thể/ảnh: " + (coverage * 100) + "%");

                    if (coverage < 0.05f || coverage > 0.90f) {
                        Log.w("RemoveBgLocal", "Chất lượng tách nền kém. Chuyển sang API.");
                        listener.onResult(null);
                        return;
                    }

                    Bitmap maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    maskBitmap.setPixels(colors, 0, width, 0, 0, width, height);

                    Bitmap scaledMask = Bitmap.createScaledBitmap(maskBitmap, inputBitmap.getWidth(), inputBitmap.getHeight(), true);

                    Bitmap result = applyMask(inputBitmap, scaledMask);
                    listener.onResult(result);

                    // Dọn dẹp
                    if (maskBitmap != scaledMask) maskBitmap.recycle();
                })
                .addOnFailureListener(e -> {
                    Log.e("RemoveBgLocal", "Lỗi ML Kit: " + e.getMessage());
                    listener.onResult(null);
                });
    }

    private Bitmap applyMask(Bitmap original, Bitmap mask) {
        Bitmap result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(original, 0, 0, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(mask, 0, 0, paint);

        paint.setXfermode(null);

        return result;
    }

    public void close() {
        if (segmenter != null) {
            segmenter.close();
        }
    }
}