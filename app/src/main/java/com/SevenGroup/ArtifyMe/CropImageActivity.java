package com.SevenGroup.ArtifyMe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Thêm import này

import com.bumptech.glide.Glide;
import com.yalantis.ucrop.UCrop; // Thêm import UCrop

import java.io.File;

public class CropImageActivity extends AppCompatActivity {

    private ImageView editImageView;
    private Uri sourceUri; // Lưu Uri của ảnh (có thể đã được crop)

    // 1. Khai báo ActivityResultLauncher cho UCrop
    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Lấy Uri kết quả từ UCrop
                    final Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        // Cập nhật sourceUri (để nếu 'Crop' lần nữa, nó sẽ crop ảnh đã crop)
                        sourceUri = resultUri;

                        // Hiển thị ảnh đã cắt lên ImageView
                        Glide.with(this).load(sourceUri).into(editImageView);

                        Toast.makeText(this, "Cắt ảnh thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không lấy được ảnh đã cắt", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    // Xử lý lỗi từ UCrop
                    final Throwable cropError = UCrop.getError(result.getData());
                    Toast.makeText(this, "Lỗi cắt ảnh: " + (cropError != null ? cropError.getMessage() : "Lỗi không xác định"), Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_screen);

        editImageView = findViewById(R.id.editImageView);
        ImageButton btnCancel = findViewById(R.id.btnCancel);
        ImageButton btnDone = findViewById(R.id.btnDone);
        ImageButton btnCrop = findViewById(R.id.btnCrop);
        ImageButton btnRotate = findViewById(R.id.btnRotate);

        // --- Lấy và hiển thị ảnh nguồn ---
        String imageUriString = getIntent().getStringExtra("IMAGE_URI");
        if (imageUriString != null) {
            sourceUri = Uri.parse(imageUriString);
            Glide.with(this)
                    .load(sourceUri)
                    .into(editImageView);
        } else {
            // Xử lý lỗi nếu không có Uri
            Toast.makeText(this, "Lỗi: Không tìm thấy ảnh", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Top bar ---
        btnCancel.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v ->
                // Logic nút Done giữ nguyên theo file cũ của bạn
                Toast.makeText(this, "Done clicked", Toast.LENGTH_SHORT).show());

        // --- Bottom buttons ---

        // 2. Cập nhật btnCrop để gọi startCrop
        btnCrop.setOnClickListener(v -> {
            if (sourceUri != null) {
                startCrop(sourceUri);
            } else {
                Toast.makeText(this, "Lỗi: Không có ảnh để cắt", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Giữ nguyên btnRotate (theo yêu cầu)
        btnRotate.setOnClickListener(v ->
                Toast.makeText(this, "Rotate feature", Toast.LENGTH_SHORT).show());
    }

    // 4. Hàm startCrop (logic của bạn)
    private void startCrop(Uri sourceUri) {
        try {
            String destinationFileName = "CROPPED_" + System.currentTimeMillis() + ".jpg";
            // Sử dụng file trong Cache
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));

            UCrop uCrop = UCrop.of(sourceUri, destinationUri);

            // Cấu hình (Tùy chọn)
            // Ví dụ: Thêm cấu hình cơ bản cho UCrop
            UCrop.Options options = new UCrop.Options();
            // options.setToolbarColor(ContextCompat.getColor(this, R.color.your_toolbar_color));
            // options.setStatusBarColor(ContextCompat.getColor(this, R.color.your_statusbar_color));
            options.setToolbarTitle("Cắt Ảnh");
            options.setCompressionQuality(90); // Đặt chất lượng nén
            uCrop = uCrop.withOptions(options);

            // Lấy Intent từ UCrop và khởi chạy
            Intent intent = uCrop.getIntent(this);
            cropLauncher.launch(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi chuẩn bị cắt ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}