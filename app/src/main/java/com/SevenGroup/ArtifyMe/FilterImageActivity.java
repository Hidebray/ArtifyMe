package com.SevenGroup.ArtifyMe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.SnapHelper;

public class FilterImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_screen);

        ImageView imagePreview = findViewById(R.id.ImageView);

        ImageButton btnCancel = findViewById(R.id.btnCancel);
        ImageButton btnDone = findViewById(R.id.btnDone);

        RecyclerView filterRecyclerView = findViewById(R.id.thumbnailRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        filterRecyclerView.setLayoutManager(layoutManager);

        // SnapHelper for center alignment
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(filterRecyclerView);

        Intent intent = getIntent();
        int imageResId = intent.getIntExtra("imageResId", -1);
        if (imageResId != -1) {
            imagePreview.setImageResource(imageResId);
        }

        // Buttons
        btnCancel.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v ->
                Toast.makeText(this, "Filter applied!", Toast.LENGTH_SHORT).show()
        );

        // filters & icons
        String[] filters = {"Warm", "Cool", "Vintage", "Mono", "Bright", "Dark", "Soft", "Sharp"};
        int[] filterIcons = {
                R.drawable.ic_filter, R.drawable.ic_filter, R.drawable.ic_filter,
                R.drawable.ic_filter, R.drawable.ic_filter, R.drawable.ic_filter,
                R.drawable.ic_filter, R.drawable.ic_filter
        };

        // RecyclerView
        filterRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        FilterAdapter adapter = new FilterAdapter(filters, filterIcons, (filterName, position) -> {
            centerItemInRecyclerView(filterRecyclerView, position);
        });

        filterRecyclerView.setAdapter(adapter);

        filterRecyclerView.post(() -> centerItemInRecyclerView(filterRecyclerView, adapter.getSelectedPosition()));

        filterRecyclerView.post(() -> {
            // Measure one child width after layout
            View firstChild = layoutManager.findViewByPosition(0);
            int halfItemWidth;
            if (firstChild != null) {
                halfItemWidth = firstChild.getWidth() / 2;
            } else {
                // fallback if not measured yet
                halfItemWidth = getResources().getDimensionPixelSize(R.dimen.filter_item_size) / 2;
            }

            // Scroll so first item is centered
            layoutManager.scrollToPositionWithOffset(0, (filterRecyclerView.getWidth() / 2) - halfItemWidth);
        });
    }

    private void centerItemInRecyclerView(RecyclerView recyclerView, int position) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) return;

        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;

        View itemView = linearLayoutManager.findViewByPosition(position);
        if (itemView == null) {
            recyclerView.scrollToPosition(position);
            itemView = linearLayoutManager.findViewByPosition(position);
        }

        final View targetView = itemView;
        recyclerView.post(() -> {
            if (targetView != null) {
                int itemCenter = targetView.getLeft() + (targetView.getWidth() / 2);
                int recyclerCenter = recyclerView.getWidth() / 2;
                int scrollOffset = itemCenter - recyclerCenter;
                recyclerView.smoothScrollBy(scrollOffset, 0);
            }
        });
    }
}
