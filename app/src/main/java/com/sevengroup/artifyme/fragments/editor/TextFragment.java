package com.sevengroup.artifyme.fragments.editor;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.ColorPickerAdapter;
import java.util.ArrayList;
import java.util.List;

public class TextFragment extends Fragment implements ColorPickerAdapter.OnColorClickListener {
    private EditText edtAddText;
    private int selectedColorCode = Color.WHITE;
    private TextFragmentListener listener;

    public interface TextFragmentListener {
        void onTextApplied(String text, int colorCode);
        void onTextCancelled();
    }

    public static TextFragment newInstance() { return new TextFragment(); }

    @Override public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TextFragmentListener) listener = (TextFragmentListener) context;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_text, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        edtAddText = view.findViewById(R.id.edtAddText);
        view.findViewById(R.id.btnCancelText).setOnClickListener(v -> listener.onTextCancelled());
        view.findViewById(R.id.btnApplyText).setOnClickListener(v -> {
            String text = edtAddText.getText().toString();
            if (!text.isEmpty()) listener.onTextApplied(text, selectedColorCode);
        });

        RecyclerView rcv = view.findViewById(R.id.rcvColorPicker);
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.WHITE); colors.add(Color.BLACK); colors.add(Color.RED);
        colors.add(Color.GREEN); colors.add(Color.BLUE); colors.add(Color.YELLOW);
        colors.add(Color.CYAN); colors.add(Color.MAGENTA);

        ColorPickerAdapter adapter = new ColorPickerAdapter(colors, this);
        rcv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rcv.setAdapter(adapter);
    }

    @Override public void onColorSelected(int colorCode) { selectedColorCode = colorCode; }
}