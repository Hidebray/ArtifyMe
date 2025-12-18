package com.sevengroup.artifyme.managers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.sevengroup.artifyme.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RemoveBgApiManager {

    private static final String API_URL = "https://api.remove.bg/v1.0/removebg";
    private final OkHttpClient client = new OkHttpClient();

    public void removeBackground(Bitmap bitmap, Consumer<Bitmap> callback, Consumer<String> onError) {
        new Thread(() -> {
            try {
                // 1. Bitmap -> PNG byte[]
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] imageBytes = bos.toByteArray();

                RequestBody imageBody =
                        RequestBody.create(imageBytes, MediaType.parse("image/png"));

                MultipartBody requestBody =
                        new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("image_file", "image.png", imageBody)
                                .addFormDataPart("size", "auto")
                                .build();

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("X-Api-Key", BuildConfig.REMOVE_BG_API_KEY)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new Exception("API error: " + response.code());
                }

                InputStream is = response.body().byteStream();
                Bitmap result = BitmapFactory.decodeStream(is);

                callback.accept(result);

            } catch (Exception e) {
                onError.accept(e.getMessage());
            }
        }).start();
    }
}
