package com.sevengroup.artifyme.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import com.sevengroup.artifyme.utils.AppExecutors;
import com.sevengroup.artifyme.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackgroundEditorManager {

    private final Context context;
    private final AppExecutors executors;
    private static final String REMOVE_BG_API_KEY = BuildConfig.REMOVE_BG_API_KEY;

    private Translator translator;

    public interface OnAiResultListener {
        void onSuccess(Bitmap result);
        void onError(String message);
        void onProgress(String status);
    }

    public BackgroundEditorManager(Context context) {
        this.context = context;
        this.executors = AppExecutors.getInstance();

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.VIETNAMESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build();
        translator = Translation.getClient(options);

        // Download model
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        translator.downloadModelIfNeeded(conditions);
    }

    public void changeBackground(
            Bitmap inputBitmap,
            String prompt,
            OnAiResultListener listener
    ) {
        executors.networkIO().execute(() -> {
            try {
                String englishPrompt = translateToEnglish(prompt);

                Bitmap foreground;
                Bitmap background;

                // Step 1: Remove background
                executors.mainThread().execute(() ->
                        listener.onProgress("Đang xóa nền...")
                );
                foreground = removeBackground(inputBitmap);

                // Step 2: Generate new background
                executors.mainThread().execute(() ->
                        listener.onProgress("Đang tạo nền mới...")
                );
                background = generateBackgroundPollinations(
                        englishPrompt,
                        foreground.getWidth(),
                        foreground.getHeight()
                );

                // Step 3: Merge
                executors.mainThread().execute(() ->
                        listener.onProgress("Đang kết hợp...")
                );

                Bitmap merged = mergeWithGpuImage(foreground, background);

                executors.mainThread().execute(() ->
                        listener.onSuccess(merged)
                );
            } catch (Exception e) {
                e.printStackTrace();
                executors.mainThread().execute(() ->
                        listener.onError(parseErrorMessage(e))
                );
            }
        });
    }

    private String translateToEnglish(String prompt) {
        String trimmed = prompt.trim();

        // Check if already in English
        if (trimmed.matches(".*[a-zA-Z].*") &&
                !trimmed.matches(".*[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ].*")) {
            return prompt;
        }

        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> resultRef = new AtomicReference<>(prompt);

            translator.translate(trimmed)
                    .addOnSuccessListener(translatedText -> {
                        resultRef.set(translatedText);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        // Fallback to dictionary if ML Kit fails
                        resultRef.set(translateWithDictionary(trimmed));
                        latch.countDown();
                    });

            // Wait max 5 seconds for translation
            latch.await(5, TimeUnit.SECONDS);
            return resultRef.get();

        } catch (Exception e) {
            // Fallback to dictionary
            return translateWithDictionary(trimmed);
        }
    }

    /**
     * Fallback: Dictionary-based translation
     */
    private String translateWithDictionary(String prompt) {
        String lowerPrompt = prompt.toLowerCase().trim();

        Map<String, String> translations = new HashMap<>();

        // Landscapes
        translations.put("bãi biển", "beach");
        translations.put("biển", "ocean");
        translations.put("núi", "mountain");
        translations.put("rừng", "forest");
        translations.put("thành phố", "city");
        translations.put("sa mạc", "desert");
        translations.put("đồng cỏ", "grassland");
        translations.put("hồ", "lake");
        translations.put("sông", "river");

        // Time of day
        translations.put("hoàng hôn", "sunset");
        translations.put("bình minh", "sunrise");
        translations.put("ban đêm", "night");
        translations.put("ban ngày", "day");
        translations.put("buổi sáng", "morning");
        translations.put("buổi chiều", "afternoon");

        // Weather
        translations.put("tuyết", "snowy");
        translations.put("mưa", "rainy");
        translations.put("nắng", "sunny");
        translations.put("mây", "cloudy");
        translations.put("sương mù", "foggy");

        // Colors
        translations.put("xanh", "blue");
        translations.put("đỏ", "red");
        translations.put("vàng", "yellow");
        translations.put("hồng", "pink");
        translations.put("tím", "purple");
        translations.put("cam", "orange");

        // Objects
        translations.put("cây dừa", "palm trees");
        translations.put("cây", "trees");
        translations.put("hoa", "flowers");
        translations.put("đám mây", "clouds");
        translations.put("ngôi sao", "stars");
        translations.put("trăng", "moon");
        translations.put("mặt trời", "sun");

        // Styles
        translations.put("nghệ thuật", "artistic");
        translations.put("trừu tượng", "abstract");
        translations.put("hiện đại", "modern");
        translations.put("cổ điển", "classic");
        translations.put("anime", "anime");
        translations.put("phong cảnh", "landscape");

        String result = lowerPrompt;
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private Bitmap removeBackground(Bitmap bitmap) throws Exception {
        // Validate input bitmap
        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            throw new IOException("Ảnh không hợp lệ");
        }

        // Check if image is too distorted (basic check)
        if (bitmap.getWidth() > 10000 || bitmap.getHeight() > 10000) {
            throw new IOException("Ảnh quá lớn. Vui lòng sử dụng ảnh nhỏ hơn.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos);
        byte[] jpegBytes = baos.toByteArray();
        baos.reset();

        Bitmap normalizedBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        normalizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image_file", "image.png",
                        RequestBody.create(baos.toByteArray(),
                                MediaType.parse("image/png")))
                .addFormDataPart("size", "auto")
                .addFormDataPart("type", "auto")
                .build();

        Request request = new Request.Builder()
                .url("https://api.remove.bg/v1.0/removebg")
                .addHeader("X-Api-Key", REMOVE_BG_API_KEY)
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = "";
            try {
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
            } catch (Exception ignored) {}

            // Handle specific error codes
            switch (response.code()) {
                case 400:
                    // Bad request
                    throw new IOException("Ảnh bị lỗi hoặc quá méo mó. Hãy thử với ảnh khác hoặc bỏ bớt hiệu ứng.");
                case 402:
                    throw new IOException("Hết credit API. Vui lòng nạp thêm tại remove.bg");
                case 403:
                    throw new IOException("API key không hợp lệ");
                case 429:
                    throw new IOException("Quá nhiều yêu cầu. Vui lòng đợi 1 phút.");
                default:
                    throw new IOException("Lỗi xóa nền (code " + response.code() + "): " + errorBody);
            }
        }

        byte[] bytes = response.body().bytes();
        Bitmap result = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // Clean up
        if (normalizedBitmap != bitmap && !normalizedBitmap.isRecycled()) {
            normalizedBitmap.recycle();
        }

        if (result == null) {
            throw new IOException("Không thể xử lý ảnh. Hãy thử với ảnh khác.");
        }

        return result;
    }

    private Bitmap generateBackgroundPollinations(String prompt, int w, int h) throws Exception {
        String encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8");
        String url = String.format(
                "https://image.pollinations.ai/prompt/%s?width=%d&height=%d&nologo=true",
                encodedPrompt, w, h
        );

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Image generation failed: " + response.code());
        }

        byte[] bytes = response.body().bytes();
        Bitmap bg = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        if (bg == null) {
            throw new IOException("Failed to decode generated image");
        }

        return Bitmap.createScaledBitmap(bg, w, h, true);
    }

    /**
     * Merge foreground and background
     */
    private Bitmap mergeWithGpuImage(Bitmap foreground, Bitmap background) {
        // Skip GPUImage entirely - it causes transparency issues
        return mergeWithCanvas(foreground, background);
    }

    /**
     * Canvas-based merge with proper alpha compositing
     */
    private Bitmap mergeWithCanvas(Bitmap foreground, Bitmap background) {
        // Ensure both bitmaps are same size
        Bitmap scaledForeground = foreground;
        if (foreground.getWidth() != background.getWidth() ||
                foreground.getHeight() != background.getHeight()) {
            scaledForeground = Bitmap.createScaledBitmap(
                    foreground,
                    background.getWidth(),
                    background.getHeight(),
                    true
            );
        }

        Bitmap result = Bitmap.createBitmap(
                background.getWidth(),
                background.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(result);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setDither(true);

        paint.setXfermode(null);

        canvas.drawBitmap(background, 0, 0, paint);

        canvas.drawBitmap(scaledForeground, 0, 0, paint);

        if (scaledForeground != foreground && !scaledForeground.isRecycled()) {
            scaledForeground.recycle();
        }

        return result;
    }

    private String parseErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "Lỗi không xác định";

        if (msg.contains("Failed to connect") || msg.contains("Unable to resolve host")) {
            return "Lỗi kết nối mạng. Kiểm tra internet.";
        } else if (msg.contains("timeout")) {
            return "Quá thời gian chờ. Thử lại.";
        } else {
            return msg;
        }
    }

    public void release() {
        // Cleanup if needed
    }
}