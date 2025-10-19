package com.example.album;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import java.util.List;

public class FullScreenImageActivity extends AppCompatActivity {

    ThumbnailAdapter adapter;
    private int currentImageResId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView fullImage = findViewById(R.id.fullImage);
        RecyclerView thumbnailRecycler = findViewById(R.id.thumbnailRecycler);

        List<Photo> photoList = PhotoRepository.getAllPhotos(this);

        int imageResId = getIntent().getIntExtra("imageResId", -1);
        currentImageResId = imageResId;

        // LayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        thumbnailRecycler.setLayoutManager(layoutManager);

        // SnapHelper for smooth centering
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(thumbnailRecycler);

        // Adapter
        adapter = new ThumbnailAdapter(photoList, resId -> {
            fullImage.setImageResource(resId);
            adapter.setSelectedImage(resId);
            currentImageResId = resId; // update when thumbnail changes
        }, thumbnailRecycler);

        thumbnailRecycler.setAdapter(adapter);

        // If image passed from main
        if (imageResId != -1) {
            fullImage.setImageResource(imageResId);
            adapter.setSelectedImage(imageResId);

            // Find and scroll to selected position
            int selectedPos = adapter.getSelectedPosition();
            if (selectedPos != RecyclerView.NO_POSITION) {
                thumbnailRecycler.scrollToPosition(selectedPos);
            }
        } else {
            // Default first photo
            if (!photoList.isEmpty()) {
                currentImageResId = photoList.get(0).getImageResId();
                fullImage.setImageResource(photoList.get(0).getImageResId());
                adapter.setSelectedImage(photoList.get(0).getImageResId());
            }
        }

        // --- Buttons ---
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnInfo).setOnClickListener(v -> {
            Intent intent = new Intent(this, PhotoInfoActivity.class);
            intent.putExtra("imageResId", currentImageResId);
            startActivity(intent);
        });

        findViewById(R.id.btnAI).setOnClickListener(v ->
                Toast.makeText(this, "AI feature", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnDelete).setOnClickListener(v ->
                Toast.makeText(this, "Delete feature", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditImageActivity.class);
            intent.putExtra("imageResId", currentImageResId);
            startActivity(intent);
        });
    }
}
