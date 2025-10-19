package com.example.album;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PhotoInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_screen);

        ImageView infoImage = findViewById(R.id.infoImage);
        TextView infoDate = findViewById(R.id.infoDate);
        TextView infoSize = findViewById(R.id.infoSize);
        ImageButton btnBack = findViewById(R.id.btnBack);

        int imageResId = getIntent().getIntExtra("imageResId", -1);
        if (imageResId != -1) {
            infoImage.setImageResource(imageResId);
        }

        infoDate.setText(R.string.info_dummy);
        infoSize.setText(R.string.info_dummy);

        btnBack.setOnClickListener(v -> finish());
    }
}
