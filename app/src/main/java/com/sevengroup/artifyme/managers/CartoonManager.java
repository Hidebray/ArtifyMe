package com.sevengroup.artifyme.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.sevengroup.artifyme.utils.AppExecutors;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CartoonManager {

    private final Context context;
    private final AppExecutors executors;
    private final Translator translator;

    public interface OnCartoonResultListener {
        void onSuccess(Bitmap result);
        void onError(String message);
        void onProgress(String status);
    }

    public CartoonManager(Context context) {
        this.context = context;
        this.executors = AppExecutors.getInstance();

        // Cấu hình dịch thuật (Việt -> Anh)
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.VIETNAMESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build();
        translator = Translation.getClient(options);

        // Tải model dịch nếu chưa có
        DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
        translator.downloadModelIfNeeded(conditions);
    }

    public void generateCartoon(String prompt, int width, int height, OnCartoonResultListener listener) {
        executors.networkIO().execute(() -> {
            try {
                // 1. Dịch Prompt sang tiếng Anh
                executors.mainThread().execute(() -> listener.onProgress("Đang phân tích mô tả..."));
                String englishPrompt = translateToEnglish(prompt);

                // 2. Gọi API tạo ảnh
                executors.mainThread().execute(() -> listener.onProgress("Đang vẽ tranh hoạt hình (AI)..."));

                // Prompt Engineering: Thêm từ khóa để ra phong cách Disney/Pixar 3D
                String stylePrompt = englishPrompt + ", 3d cartoon style, disney pixar style, cute, vibrant colors, high quality, masterpiece, 8k resolution, cinematic lighting";

                Bitmap result = callPollinationsApi(stylePrompt, width, height);

                // 3. Trả về kết quả
                executors.mainThread().execute(() -> listener.onSuccess(result));

            } catch (Exception e) {
                e.printStackTrace();
                executors.mainThread().execute(() -> listener.onError("Lỗi: " + e.getMessage()));
            }
        });
    }

    private Bitmap callPollinationsApi(String prompt, int w, int h) throws Exception {
        String encodedPrompt = URLEncoder.encode(prompt, "UTF-8");
        // Random seed để mỗi lần vẽ lại khác nhau một chút
        long seed = System.currentTimeMillis() % 10000;

        // Sử dụng model 'flux' cho chất lượng tốt nhất hiện nay trên Pollinations
        String url = String.format(
                "https://image.pollinations.ai/prompt/%s?width=%d&height=%d&nologo=true&seed=%d&model=flux",
                encodedPrompt, w, h, seed
        );

        Request request = new Request.Builder().url(url).get().build();

        // Tăng timeout lên 60s vì tạo ảnh mất thời gian
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Server error: " + response.code());
            if (response.body() == null) throw new IOException("Empty response");
            return BitmapFactory.decodeStream(response.body().byteStream());
        }
    }

    private String translateToEnglish(String prompt) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> resultRef = new AtomicReference<>(prompt);

            translator.translate(prompt)
                    .addOnSuccessListener(s -> {
                        resultRef.set(s);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> latch.countDown());

            // Chờ tối đa 3 giây cho việc dịch
            latch.await(3, TimeUnit.SECONDS);
            return resultRef.get();
        } catch (Exception e) {
            return prompt; // Nếu lỗi dịch thì dùng luôn tiếng Việt (Fallback)
        }
    }
}