package com.sevengroup.artifyme.fragments.bottomsheet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sevengroup.artifyme.R;

public class AiBackgroundSheet extends BottomSheetDialogFragment {

    private EditText edtPrompt;
    private OnGenerateListener listener;

    public interface OnGenerateListener {
        void onGenerate(String prompt);
    }

    public void setListener(OnGenerateListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_ai_background, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtPrompt = view.findViewById(R.id.edtPrompt);
        MaterialButton btnGenerate = view.findViewById(R.id.btnGenerateBg);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupSuggestions);

        // Logic 1: Khi bấm vào Chip gợi ý -> Điền text vào ô nhập
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setOnClickListener(v -> {
                edtPrompt.setText(chip.getText());
                edtPrompt.setSelection(edtPrompt.getText().length()); // Đưa con trỏ về cuối
            });
        }

        // Logic 2: Bấm nút Tạo
        btnGenerate.setOnClickListener(v -> {
            String prompt = edtPrompt.getText().toString().trim();
            if (TextUtils.isEmpty(prompt)) {
                edtPrompt.setError("Vui lòng nhập mô tả hoặc chọn gợi ý");
                return;
            }

            if (listener != null) {
                listener.onGenerate(prompt);
            }
            dismiss(); // Đóng sheet
        });
    }
}