package com.SevenGroup.ArtifyMe;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CropImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_screen);

        ImageView editImageView = findViewById(R.id.editImageView);
        ImageButton btnCancel = findViewById(R.id.btnCancel);
        ImageButton btnDone = findViewById(R.id.btnDone);
        ImageButton btnCrop = findViewById(R.id.btnCrop);
        ImageButton btnRotate = findViewById(R.id.btnRotate);

        // Get selected image
        int imageResId = getIntent().getIntExtra("imageResId", -1);
        if (imageResId != -1) {
            editImageView.setImageResource(imageResId);
        }

        // --- Top bar ---
        btnCancel.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v ->
                Toast.makeText(this, "Done clicked", Toast.LENGTH_SHORT).show());

        // --- Bottom buttons ---
        btnCrop.setOnClickListener(v ->
                Toast.makeText(this, "Crop feature", Toast.LENGTH_SHORT).show());
        btnRotate.setOnClickListener(v ->
                Toast.makeText(this, "Rotate feature", Toast.LENGTH_SHORT).show());
    }
}

