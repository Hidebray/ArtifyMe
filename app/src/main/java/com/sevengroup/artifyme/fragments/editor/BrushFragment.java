package com.sevengroup.artifyme.fragments.editor;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.ColorPickerAdapter;
import java.util.ArrayList;
import java.util.List;

public class BrushFragment extends BottomSheetDialogFragment {
    private SeekBar sbSize, sbOpacity;
    private View btnEraserContainer;
    private ImageView imgEraser;
    private BrushListener listener;

    // State management
    private int mCurrentColor = Color.RED; // Màu mặc định
    private int mCurrentSize = 20;
    private int mCurrentOpacity = 100; // 0-100
    private boolean isEraserState = false;

    public interface BrushListener {
        void onBrushSizeChanged(float size);
        void onBrushColorChanged(int color);
        void onBrushEraser();
        void onBrushFinished();
    }

    public void setListener(BrushListener listener) { this.listener = listener; }
    public static BrushFragment newInstance() { return new BrushFragment(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_brush, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sbSize = view.findViewById(R.id.sbSize);
        sbOpacity = view.findViewById(R.id.sbOpacity);
        RecyclerView rcvColors = view.findViewById(R.id.rcvColors);
        btnEraserContainer = view.findViewById(R.id.btnEraser);
        imgEraser = view.findViewById(R.id.imgEraser);
        View btnDone = view.findViewById(R.id.btnDone);

        // 1. Tạo bảng màu phong phú hơn
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#000000")); // Đen
        colors.add(Color.parseColor("#FFFFFF")); // Trắng
        colors.add(Color.parseColor("#F44336")); // Đỏ
        colors.add(Color.parseColor("#E91E63")); // Hồng
        colors.add(Color.parseColor("#9C27B0")); // Tím
        colors.add(Color.parseColor("#2196F3")); // Xanh dương
        colors.add(Color.parseColor("#03A9F4")); // Xanh nhạt
        colors.add(Color.parseColor("#009688")); // Teal
        colors.add(Color.parseColor("#4CAF50")); // Xanh lá
        colors.add(Color.parseColor("#FFEB3B")); // Vàng
        colors.add(Color.parseColor("#FF9800")); // Cam
        colors.add(Color.parseColor("#795548")); // Nâu

        // Mặc định chọn màu đầu tiên nếu chưa có
        if (mCurrentColor == 0) mCurrentColor = colors.get(2);

        ColorPickerAdapter adapter = new ColorPickerAdapter(colors, color -> {
            // Khi người dùng chọn màu:
            isEraserState = false;
            mCurrentColor = color; // Lưu màu gốc (không alpha)
            updateBrushColor();    // Tính toán lại màu với opacity hiện tại
            updateEraserStateUI(); // Cập nhật giao diện nút tẩy (tắt active)
        });

        rcvColors.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rcvColors.setAdapter(adapter);

        // 2. Logic SeekBar Size
        sbSize.setMax(100);
        sbSize.setProgress(mCurrentSize);
        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentSize = progress;
                if(listener != null) listener.onBrushSizeChanged((float) progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 3. Logic SeekBar Opacity
        sbOpacity.setMax(100);
        sbOpacity.setProgress(mCurrentOpacity);
        sbOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentOpacity = progress;
                if (!isEraserState) {
                    updateBrushColor(); // Chỉ cập nhật màu nếu không phải đang tẩy
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 4. Logic Eraser Toggle
        btnEraserContainer.setOnClickListener(v -> {
            isEraserState = !isEraserState; // Đảo trạng thái
            if (isEraserState) {
                if(listener != null) listener.onBrushEraser();
            } else {
                updateBrushColor(); // Quay lại màu cũ
            }
            updateEraserStateUI();
        });

        btnDone.setOnClickListener(v -> {
            if(listener != null) listener.onBrushFinished();
            dismiss();
        });

        // Khởi tạo trạng thái ban đầu
        updateEraserStateUI();
    }

    private void updateBrushColor() {
        // Tính toán màu thực tế dựa trên màu gốc và thanh Opacity
        // Alpha trong Android Color đi từ 0-255. SeekBar 0-100.
        int alpha = (mCurrentOpacity * 255) / 100;
        int colorWithAlpha = androidx.core.graphics.ColorUtils.setAlphaComponent(mCurrentColor, alpha);

        if(listener != null) listener.onBrushColorChanged(colorWithAlpha);
    }

    private void updateEraserStateUI() {
        if (isEraserState) {
            // Active: Đổi màu nền hoặc icon để báo hiệu
            btnEraserContainer.setBackgroundResource(R.drawable.bg_pill_white_shadow); // Bạn có thể tạo drawable bg_active_selected
            imgEraser.setColorFilter(ContextCompat.getColor(requireContext(), R.color.brand_primary));
            btnEraserContainer.setAlpha(1.0f);
        } else {
            // Inactive
            btnEraserContainer.setBackgroundResource(R.drawable.bg_gradient_bottom);
            imgEraser.setColorFilter(Color.BLACK);
            btnEraserContainer.setAlpha(0.5f); // Làm mờ nhẹ để thấy không được chọn
        }
    }
}