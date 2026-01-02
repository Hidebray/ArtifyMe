package com.sevengroup.artifyme.managers;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

public class RemoveBgLocalManager {

    private final Segmenter segmenter;

    public interface OnRemoveBgListener {
        void onResult(Bitmap bitmap);
    }

    public RemoveBgLocalManager() {
        // Dùng Selfie Segmenter (Ổn định, không crash)
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
                    Log.d("RemoveBgLocal", "Selfie Segmenter chạy xong, chuyển API cho đẹp.");
                    listener.onResult(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("RemoveBgLocal", "Lỗi: " + e.getMessage());
                    listener.onResult(null);
                });
    }

    public void close() {
        // Không cần đóng resource nặng
    }
}