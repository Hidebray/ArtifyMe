package com.SevenGroup.ArtifyMe;

import android.content.Intent;
import android.net.Uri; // Thêm import
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide; // Thêm import

public class EditImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_screen);

        ImageView editImage = findViewById(R.id.editImage);

        // --- Cập nhật logic load ảnh ---
        String imageUriString = getIntent().getStringExtra("IMAGE_URI");
        int imageResId = getIntent().getIntExtra("imageResId", -1);

        Uri finalImageUri = null; // Biến để lưu Uri và truyền đi

        if (imageUriString != null) {
            // Luồng mới: Load ảnh từ Uri (do MainActivity truyền sang)
            finalImageUri = Uri.parse(imageUriString);
            Glide.with(this)
                    .load(finalImageUri)
                    .into(editImage);
        } else if (imageResId != -1) {
            // Luồng cũ: Load ảnh từ drawable resource (do FullScreenImageActivity truyền sang)
            editImage.setImageResource(imageResId);
            // Tạo một Uri giả lập cho drawable resource để truyền đi
            finalImageUri = Uri.parse("android.resource://" + getPackageName() + "/" + imageResId);
        }
        // ------------------------------

        // --- Top bar ---
        ImageButton btnCancel = findViewById(R.id.btnCancel);
        ImageButton btnDone = findViewById(R.id.btnDone);

        btnCancel.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v ->
                Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show());

        // --- Bottom buttons ---
        ImageButton btnAdjust = findViewById(R.id.btnAdjust);
        ImageButton btnFilter = findViewById(R.id.btnFilter);
        ImageButton btnCrop = findViewById(R.id.btnCrop);

        btnAdjust.setOnClickListener(v ->
                Toast.makeText(this, "Adjust feature", Toast.LENGTH_SHORT).show());

        // Cần một biến final để dùng trong lambda
        final Uri uriToPass = finalImageUri;

        // Cập nhật để truyền Uri
        btnFilter.setOnClickListener(v -> {
            if (uriToPass != null) {
                Intent intent = new Intent(EditImageActivity.this, FilterImageActivity.class);
                intent.putExtra("IMAGE_URI", uriToPass.toString());
                startActivity(intent);
            }
        });

        // Cập nhật để truyền Uri
        btnCrop.setOnClickListener(v -> {
            if (uriToPass != null) {
                Intent intent = new Intent(EditImageActivity.this, CropImageActivity.class);
                intent.putExtra("IMAGE_URI", uriToPass.toString());
                startActivity(intent);
            }
        });
    }
}