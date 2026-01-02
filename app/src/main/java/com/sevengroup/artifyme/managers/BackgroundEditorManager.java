package com.sevengroup.artifyme.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import com.sevengroup.artifyme.utils.AppExecutors;
import com.sevengroup.artifyme.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
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

    // CẢI TIẾN 3: Thêm Local Manager để xử lý offline/tiết kiệm API
    private final RemoveBgLocalManager localRemoveBgManager;

    public interface OnAiResultListener {
        void onSuccess(Bitmap result);
        void onError(String message);
        void onProgress(String status);
    }

    public BackgroundEditorManager(Context context) {
        this.context = context;
        this.executors = AppExecutors.getInstance();
        this.localRemoveBgManager = new RemoveBgLocalManager(); // Khởi tạo ML Kit Manager

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

                Bitmap foreground = null;
                Bitmap background;

                // --- BƯỚC 1: Xóa nền (HYBRID APPROACH: Local trước, API sau) ---
                executors.mainThread().execute(() ->
                        listener.onProgress("Đang tách nền (AI)...")
                );

                // Thử dùng Local ML Kit trước
                try {
                    CountDownLatch latch = new CountDownLatch(1);
                    AtomicReference<Bitmap> localResult = new AtomicReference<>();

                    localRemoveBgManager.removeBackground(inputBitmap, bitmap -> {
                        localResult.set(bitmap);
                        latch.countDown();
                    });

                    boolean finished = latch.await(5, TimeUnit.SECONDS);
                    Bitmap result = localResult.get();

                    if (finished && result != null) {
                        foreground = result;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Nếu Local thất bại hoặc trả về null, dùng API remove.bg (Fallback)
                if (foreground == null) {
                    executors.mainThread().execute(() ->
                            listener.onProgress("Local AI chưa tối ưu, đang dùng Cloud AI...")
                    );
                    foreground = removeBackgroundApi(inputBitmap);
                }

                if (foreground == null) {
                    throw new IOException("Không thể tách nền ảnh này.");
                }

                // --- BƯỚC 2: Tạo nền mới (PROMPT ENGINEERING) ---
                executors.mainThread().execute(() ->
                        listener.onProgress("Đang vẽ nền mới...")
                );

                background = generateBackgroundPollinations(
                        englishPrompt,
                        inputBitmap.getWidth(), // Dùng kích thước gốc
                        inputBitmap.getHeight()
                );

                // --- BƯỚC 3: Ghép ảnh (DROP SHADOW) ---
                executors.mainThread().execute(() ->
                        listener.onProgress("Đang hoàn thiện...")
                );

                Bitmap merged = mergeWithCanvas(foreground, background);

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

        if (!hasVietnameseChars) return trimmed;

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

        // (Giữ nguyên danh sách từ điển của bạn)
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
        translations.put("tuyết", "snowy");
        translations.put("mưa", "rainy");
        translations.put("nắng", "sunny");
        translations.put("cây", "trees");
        translations.put("hoa", "flowers");
        translations.put("mây", "clouds");

        String result = lowerPrompt;
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    // Đổi tên hàm cũ thành removeBackgroundApi để phân biệt
    private Bitmap removeBackgroundApi(Bitmap bitmap) throws Exception {
        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            throw new IOException("Ảnh không hợp lệ");
        }

        // Logic resize ảnh nếu quá lớn để tiết kiệm băng thông
        if (bitmap.getWidth() > 3000 || bitmap.getHeight() > 3000) {
            // Resize logic here if needed, or keep original check
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] jpegBytes = baos.toByteArray();
        baos.reset();

        Bitmap normalizedBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        if (normalizedBitmap == null) throw new IOException("Lỗi xử lý ảnh.");

        // Compress sang PNG để giữ chất lượng cho API
        normalizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image_file", "image.png",
                        RequestBody.create(baos.toByteArray(), MediaType.parse("image/png")))
                .addFormDataPart("size", "auto")
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

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                if (response.code() == 402) throw new IOException("Hết credit API remove.bg.");
                if (response.code() == 403) throw new IOException("API Key không đúng.");
                throw new IOException("Lỗi xóa nền: " + response.code() + " " + errorBody);
            }

            byte[] bytes = response.body().bytes();
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } finally {
            if (normalizedBitmap != bitmap && !normalizedBitmap.isRecycled()) {
                normalizedBitmap.recycle();
            }
        }
    }

    private Bitmap generateBackgroundPollinations(String prompt, int w, int h) throws Exception {
        // CẢI TIẾN 2: Prompt Engineering - Thêm từ khóa chất lượng
        String enhancedPrompt = prompt + ", highly detailed, 8k resolution, cinematic lighting, photorealistic, professional photography, hdr, masterpiece";

        String encodedPrompt = URLEncoder.encode(enhancedPrompt, "UTF-8");

        // Thêm seed random để mỗi lần tạo ra một ảnh khác nhau dù cùng prompt
        long seed = System.currentTimeMillis() % 10000;

        String url = String.format(
                "https://image.pollinations.ai/prompt/%s?width=%d&height=%d&nologo=true&seed=%d&model=flux",
                encodedPrompt, w, h, seed
        );

        Request request = new Request.Builder().url(url).get().build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // Tăng timeout vì tạo ảnh lâu
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Response response = client. newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Lỗi tạo ảnh AI: " + response.code());
        }

        byte[] bytes = response.body().bytes();
        Bitmap bg = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bg == null) throw new IOException("Không thể giải mã ảnh từ AI.");

        return Bitmap.createScaledBitmap(bg, w, h, true);
    }

    private Bitmap mergeWithCanvas(Bitmap foreground, Bitmap background) {
        // CẢI TIẾN 1: Thêm bóng đổ (Drop Shadow)

        Bitmap scaledForeground = foreground;
        // Resize foreground cho khớp tỉ lệ background nếu cần
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

        // 1. Vẽ Background
        canvas.drawBitmap(background, 0, 0, paint);

        // 2. Vẽ Bóng đổ (Shadow)
        // Tạo paint vẽ bóng (màu đen, alpha thấp)
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN));
        shadowPaint.setAlpha(80); // Độ trong suốt của bóng (0-255)

        float shadowOffsetX = background.getWidth() * 0.02f; // Dịch phải 2%
        float shadowOffsetY = background.getHeight() * 0.02f; // Dịch xuống 2%

        canvas.save();
        canvas.translate(shadowOffsetX, shadowOffsetY);
        // Vẽ chính hình dáng của foreground nhưng tô đen để làm bóng
        canvas.drawBitmap(scaledForeground, 0, 0, shadowPaint);
        canvas.restore();

        // 3. Vẽ Foreground đè lên trên
        canvas.drawBitmap(scaledForeground, 0, 0, paint);

        // Cleanup
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
        }
        return msg;
    }

    public void release() {
        // Cleanup resources
    }
}