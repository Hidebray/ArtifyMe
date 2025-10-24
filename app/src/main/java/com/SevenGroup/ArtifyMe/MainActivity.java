package com.SevenGroup.ArtifyMe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    // 1. Khai báo ActivityResultLauncher cho việc chọn ảnh
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Lấy URI của ảnh được chọn
                    Uri selectedImageUri = result.getData().getData();

                    if (selectedImageUri != null) {
                        // Chuyển sang EditImageActivity, truyền URI của ảnh qua Intent
                        Intent editIntent = new Intent(MainActivity.this, EditImageActivity.class);
                        // Chúng ta truyền URI dưới dạng String
                        editIntent.putExtra("IMAGE_URI", selectedImageUri.toString());
                        startActivity(editIntent);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Chưa chọn ảnh nào.", Toast.LENGTH_SHORT).show();
                }
            });

    // 2. Khai báo ActivityResultLauncher cho việc xin quyền truy cập bộ nhớ
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Nếu quyền được cấp, mở Gallery
                    openGallery();
                } else {
                    Toast.makeText(MainActivity.this, "Cần quyền truy cập bộ nhớ để chọn ảnh.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        FloatingActionButton fab = findViewById(R.id.fab_add_album);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("All Images");
            else tab.setText("Albums");
        }).attach();

        // 3. Cập nhật OnClickListener của FAB
        fab.setOnClickListener(v ->
                // Gọi hàm kiểm tra quyền và mở thư viện
                checkPermissionAndOpenGallery()
        );

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Show only on All Images
                if (position == 0) {
                    fab.show();
                } else {
                    fab.hide();
                }
            }
        });
    }

    // 4. Hàm kiểm tra quyền và mở thư viện ảnh
    private void checkPermissionAndOpenGallery() {
        String permission;

        // Xác định quyền cần thiết dựa trên phiên bản SDK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) trở lên
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            // Android 12 (API 32) trở xuống
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        // Xin/Kiểm tra quyền đã xác định
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    // 5. Hàm mở thư viện ảnh
    private void openGallery() {
        // Intent chuẩn để chọn ảnh
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }
}