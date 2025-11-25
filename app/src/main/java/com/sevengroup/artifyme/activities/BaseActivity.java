package com.sevengroup.artifyme.activities;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    protected void setupCustomHeader(TextView txtTitle, String title, View btnBack) {
        if (txtTitle != null) {
            txtTitle.setText(title);
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    protected void showToast(String message) {
        if (message != null && !message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    protected void showLoading(View loadingView, boolean isShow) {
        if (loadingView != null) {
            loadingView.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }
}