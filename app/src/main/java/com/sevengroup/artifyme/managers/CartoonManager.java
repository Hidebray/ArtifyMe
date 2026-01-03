package com.sevengroup.artifyme.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.sevengroup.artifyme.utils.AppExecutors;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CartoonManager {

    private final AppExecutors executors;
    // Thêm: Bộ nhận diện ảnh ML Kit
    private final ImageLabeler imageLabeler;

    public interface OnCartoonResultListener {
        void onSuccess(Bitmap result);
        void onError(String message);
        void onProgress(String status);
    }

    public CartoonManager(Context context) {
        this.executors = AppExecutors.getInstance();

        // Cấu hình ML Kit: Độ tin cậy > 50% mới lấy
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.5f)
                .build();
        imageLabeler = ImageLabeling.getClient(options);
    }

    public void generateCartoonFromImage(Bitmap inputImage, OnCartoonResultListener listener) {
        // 1. Phân tích ảnh để lấy từ khóa (Auto Prompt)
        executors.mainThread().execute(() -> listener.onProgress("AI đang phân tích ảnh..."));

        InputImage image = InputImage.fromBitmap(inputImage, 0);

        imageLabeler.process(image)
                .addOnSuccessListener(labels -> {
                    // Lấy danh sách các vật thể nhận diện được
                    List<String> keywords = new ArrayList<>();
                    for (ImageLabel label : labels) {
                        keywords.add(label.getText());
                    }

                    // Nếu không nhận diện được gì thì dùng từ khóa chung
                    if (keywords.isEmpty()) {
                        keywords.add("portrait");
                        keywords.add("person");
                    }

                    String detectedDescription = String.join(", ", keywords);
                    Log.d("CartoonManager", "Auto Prompt: " + detectedDescription);

                    // 2. Tạo Prompt hoàn chỉnh cho API
                    // Prompt này được tối ưu cho phong cách 3D Disney
                    String fullPrompt = "Cartoon character of " + detectedDescription
                            + ", 3d cartoon style, disney pixar style, cute, vibrant colors, high quality, masterpiece, 8k resolution, cinematic lighting";

                    // 3. Gọi API tạo ảnh (Chạy ở background)
                    executors.networkIO().execute(() ->
                            callPollinationsApi(fullPrompt, inputImage.getWidth(), inputImage.getHeight(), listener)
                    );
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi nhận diện, vẫn cố vẽ bằng prompt mặc định
                    String fallbackPrompt = "Cartoon character, 3d cartoon style, disney pixar style, cute, masterpiece";
                    executors.networkIO().execute(() ->
                            callPollinationsApi(fallbackPrompt, inputImage.getWidth(), inputImage.getHeight(), listener)
                    );
                });
    }

    // Hàm gọi API giữ nguyên logic cũ của bạn nhưng tách ra để tái sử dụng
    private void callPollinationsApi(String prompt, int w, int h, OnCartoonResultListener listener) {
        try {
            executors.mainThread().execute(() -> listener.onProgress("Đang vẽ tranh (Auto)..."));

            String encodedPrompt = URLEncoder.encode(prompt, "UTF-8");
            long seed = System.currentTimeMillis() % 10000;

            String url = String.format(
                    "https://image.pollinations.ai/prompt/%s?width=%d&height=%d&nologo=true&seed=%d&model=flux",
                    encodedPrompt, w, h, seed
            );

            Request request = new Request.Builder().url(url).get().build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Server error: " + response.code());
                if (response.body() == null) throw new IOException("Empty response");

                Bitmap result = BitmapFactory.decodeStream(response.body().byteStream());

                // Trả về kết quả
                executors.mainThread().execute(() -> listener.onSuccess(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            executors.mainThread().execute(() -> listener.onError("Lỗi: " + e.getMessage()));
        }
    }
}