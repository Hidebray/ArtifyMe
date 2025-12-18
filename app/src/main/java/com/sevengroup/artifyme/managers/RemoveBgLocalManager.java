package com.sevengroup.artifyme.managers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class RemoveBgLocalManager {

    Segmenter segmenter ;

    public RemoveBgLocalManager() {
        SelfieSegmenterOptions options =
                new SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build();
        segmenter = Segmentation.getClient(options);
    }

    public void removeBackground(Bitmap source, Consumer<Bitmap> callback) {
        InputImage image = InputImage.fromBitmap(source, 0);

        segmenter.process(image)
                .addOnSuccessListener(mask -> {
                    Bitmap result = applyMask(source, mask);
                    callback.accept(result);
                });
    }

    private Bitmap applyMask(Bitmap original, SegmentationMask mask) {
        Bitmap maskBitmap = Bitmap.createBitmap(
                mask.getWidth(),
                mask.getHeight(),
                Bitmap.Config.ALPHA_8
        );

        ByteBuffer buffer = mask.getBuffer();
        buffer.rewind();

        for (int y = 0; y < mask.getHeight(); y++) {
            for (int x = 0; x < mask.getWidth(); x++) {
                float confidence = buffer.getFloat();
                int alpha = (int) (confidence * 255);
                maskBitmap.setPixel(x, y, Color.argb(alpha, 255, 255, 255));
            }
        }

        Bitmap scaledMask = Bitmap.createScaledBitmap(
                maskBitmap,
                original.getWidth(),
                original.getHeight(),
                true
        );

        Bitmap result = Bitmap.createBitmap(
                original.getWidth(),
                original.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        canvas.drawBitmap(original, 0, 0, null);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(scaledMask, 0, 0, paint);

        return result;
    }
}