package com.SevenGroup.ArtifyMe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class EditImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_screen);

        ImageView editImage = findViewById(R.id.editImage);
        int imageResId = getIntent().getIntExtra("imageResId", -1);
        if (imageResId != -1) {
            editImage.setImageResource(imageResId);
        }

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

        btnFilter.setOnClickListener(v -> {
            Intent intent = new Intent(EditImageActivity.this, FilterImageActivity.class);
            intent.putExtra("imageResId", imageResId);
            startActivity(intent);
        });

        btnCrop.setOnClickListener(v -> {
            Intent intent = new Intent(EditImageActivity.this, CropImageActivity.class);
            intent.putExtra("imageResId", imageResId);
            startActivity(intent);
        });
    }
}

