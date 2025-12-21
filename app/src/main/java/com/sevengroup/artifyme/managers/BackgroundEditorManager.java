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

    private final Translator translator;

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

        boolean hasVietnameseChars = trimmed.matches(".*[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ].*");

        if (!hasVietnameseChars) {
            return trimmed;
        }

        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> resultRef = new AtomicReference<>(null);

            translator.translate(trimmed)
                    .addOnSuccessListener(translatedText -> {
                        resultRef.set(translatedText);
                        latch.countDown();
                    })
                    .addOnFailureListener(e -> latch.countDown());

            boolean completed = latch.await(5, TimeUnit.SECONDS);

            String mlResult = resultRef.get();

            if (completed && mlResult != null && !mlResult.trim().isEmpty()) {
                return mlResult;
            }

            return translateWithDictionary(trimmed);

        } catch (Exception e) {
            return translateWithDictionary(trimmed);
        }
    }

    private String translateWithDictionary(String prompt) {
        String lowerPrompt = prompt.toLowerCase().trim();

        Map<String, String> translations = new HashMap<>();

        translations.put("bãi biển", "beach");
        translations.put("biển", "ocean");
        translations.put("núi", "mountain");
        translations.put("rừng", "forest");
        translations.put("thành phố", "city");
        translations.put("sa mạc", "desert");
        translations.put("đồng cỏ", "grassland");
        translations.put("hồ", "lake");
        translations.put("sông", "river");

        translations.put("hoàng hôn", "sunset");
        translations.put("bình minh", "sunrise");
        translations.put("ban đêm", "night");
        translations.put("ban ngày", "day");
        translations.put("buổi sáng", "morning");
        translations.put("buổi chiều", "afternoon");

        translations.put("tuyết", "snowy");
        translations.put("mưa", "rainy");
        translations.put("nắng", "sunny");
        translations.put("mây", "cloudy");
        translations.put("sương mù", "foggy");

        translations.put("xanh", "blue");
        translations.put("đỏ", "red");
        translations.put("vàng", "yellow");
        translations.put("hồng", "pink");
        translations.put("tím", "purple");
        translations.put("cam", "orange");

        translations.put("cây dừa", "palm trees");
        translations.put("cây", "trees");
        translations.put("hoa", "flowers");
        translations.put("đám mây", "clouds");
        translations.put("ngôi sao", "stars");
        translations.put("trăng", "moon");
        translations.put("mặt trời", "sun");

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
        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            throw new IOException("Ảnh không hợp lệ");
        }

        if (bitmap.getWidth() > 8000 || bitmap.getHeight() > 8000) {
            throw new IOException("Ảnh quá lớn (tối đa 8000x8000 pixels). Vui lòng resize ảnh trước.");
        }

        float aspectRatio = (float) bitmap.getWidth() / bitmap.getHeight();
        if (aspectRatio > 10 || aspectRatio < 0.1) {
            throw new IOException("Tỉ lệ ảnh không hợp lệ. Vui lòng sử dụng ảnh có tỉ lệ hợp lý.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos);
        byte[] jpegBytes = baos.toByteArray();
        baos.reset();

        Bitmap normalizedBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        if (normalizedBitmap == null) {
            throw new IOException("Không thể xử lý ảnh. File có thể bị lỗi.");
        }

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
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                String errorBody = "";
                try {
                    if (response.body() != null) {
                        errorBody = response.body().string();
                    }
                } catch (Exception ignored) {
                }

                // error code
                switch (response.code()) {
                    case 400:
                        if (errorBody.contains("image_file")) {
                            throw new IOException("File ảnh bị lỗi. Hãy chọn ảnh khác.");
                        }
                        throw new IOException("Ảnh không hợp lệ. Thử giảm hiệu ứng hoặc dùng ảnh khác.");
                    case 402:
                        throw new IOException("Hết credit API. Liên hệ admin để nạp thêm.");
                    case 403:
                        throw new IOException("API key không hợp lệ. Vui lòng cập nhật app.");
                    case 429:
                        throw new IOException("Quá nhiều yêu cầu. Vui lòng đợi 1 phút rồi thử lại.");
                    case 500:
                    case 502:
                    case 503:
                        throw new IOException("Server remove.bg đang bận. Vui lòng thử lại sau.");
                    default:
                        throw new IOException("Lỗi xóa nền (code " + response.code() + ")");
                }
            }

            byte[] bytes = response.body().bytes();
            Bitmap result = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            if (result == null) {
                throw new IOException("Không thể xử lý ảnh trả về. Thử lại sau.");
            }

            return result;

        } finally {
            if (normalizedBitmap != bitmap && normalizedBitmap != null && !normalizedBitmap.isRecycled()) {
                normalizedBitmap.recycle();
            }
        }
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

    private Bitmap mergeWithGpuImage(Bitmap foreground, Bitmap background) {
        return mergeWithCanvas(foreground, background);
    }

    private Bitmap mergeWithCanvas(Bitmap foreground, Bitmap background) {
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