package com.sevengroup.artifyme.fragments.editor;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.AdjustOptionsAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjustFragment extends Fragment {
    private AdjustListener listener;
    private AdjustType currentType = AdjustType.BRIGHTNESS;
    private HashMap<AdjustType, Float> currentValues = new HashMap<>();
    private SeekBar seekBar;

    public enum AdjustType {
        BRIGHTNESS, CONTRAST, SATURATION, WARMTH, VIGNETTE, TINT,
        GRAIN, SHARPNESS, EXPOSURE, HIGHLIGHTS, SHADOWS, GAMMA
    }

    public interface AdjustListener {
        void onAdjustmentChanged(AdjustType type, float value);
        void onAdjustApplied();
        void onAdjustCancelled();
    }

    public static AdjustFragment newInstance(Map<AdjustType, Float> values) {
        AdjustFragment f = new AdjustFragment();
        Bundle args = new Bundle();
        args.putSerializable("VALUES", new HashMap<>(values));
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (AdjustType t : AdjustType.values()) currentValues.put(t, 0f);
        if (getArguments() != null) {
            @SuppressWarnings("unchecked")
            HashMap<AdjustType, Float> passedValues = (HashMap<AdjustType, Float>) getArguments().getSerializable("VALUES");
            if (passedValues != null) {
                currentValues.putAll(passedValues);
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AdjustListener) listener = (AdjustListener) context;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adjust, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        seekBar = view.findViewById(R.id.seekbarAdjust);
        RecyclerView rcv = view.findViewById(R.id.rcvAdjustOptions);

        view.findViewById(R.id.btnApplyAdjust).setOnClickListener(v -> listener.onAdjustApplied());
        view.findViewById(R.id.btnCancelAdjust).setOnClickListener(v -> listener.onAdjustCancelled());

        List<AdjustOptionModel> options = getAdjustOptions();
        AdjustOptionsAdapter adapter = new AdjustOptionsAdapter(getContext(), options, type -> {
            currentType = type;
            float val = currentValues.getOrDefault(type, 0f);
            updateSeekBar(val);
        });

        rcv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rcv.setAdapter(adapter);

        float startVal = currentValues.getOrDefault(AdjustType.BRIGHTNESS, 0f);
        updateSeekBar(startVal);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    float val = (p - 50) / 50.0f;
                    currentValues.put(currentType, val);
                    if (listener != null) listener.onAdjustmentChanged(currentType, val);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void updateSeekBar(float value) {
        if (seekBar != null) {
            int progress = (int) ((value * 50) + 50);
            seekBar.setProgress(progress);
        }
    }

    // UPDATE: Hàm này để Activity gọi khi Undo
    public void refreshValues(Map<AdjustType, Float> newValues) {
        if (newValues == null) return;
        currentValues.putAll(newValues);
        float val = currentValues.getOrDefault(currentType, 0f);
        updateSeekBar(val);
    }

    private List<AdjustOptionModel> getAdjustOptions() {
        List<AdjustOptionModel> options = new ArrayList<>();
        options.add(new AdjustOptionModel(getString(R.string.adj_brightness), AdjustType.BRIGHTNESS, R.drawable.ic_brightness));
        options.add(new AdjustOptionModel(getString(R.string.adj_contrast), AdjustType.CONTRAST, R.drawable.ic_contrast));
        options.add(new AdjustOptionModel(getString(R.string.adj_saturation), AdjustType.SATURATION, R.drawable.ic_launcher_foreground));
        options.add(new AdjustOptionModel(getString(R.string.adj_warmth), AdjustType.WARMTH, R.drawable.ic_warmth));
        options.add(new AdjustOptionModel(getString(R.string.adj_vignette), AdjustType.VIGNETTE, R.drawable.ic_vignette));
        options.add(new AdjustOptionModel(getString(R.string.adj_tint), AdjustType.TINT, R.drawable.ic_launcher_foreground));
        options.add(new AdjustOptionModel(getString(R.string.adj_grain), AdjustType.GRAIN, R.drawable.ic_grain));
        options.add(new AdjustOptionModel(getString(R.string.adj_sharpness), AdjustType.SHARPNESS, R.drawable.ic_sharpness));
        options.add(new AdjustOptionModel(getString(R.string.adj_exposure), AdjustType.EXPOSURE, R.drawable.ic_exposure));
        options.add(new AdjustOptionModel(getString(R.string.adj_highlights), AdjustType.HIGHLIGHTS, R.drawable.ic_highlight));
        options.add(new AdjustOptionModel(getString(R.string.adj_shadows), AdjustType.SHADOWS, R.drawable.ic_shadow));
        options.add(new AdjustOptionModel(getString(R.string.adj_gamma), AdjustType.GAMMA, R.drawable.ic_launcher_foreground));
        return options;
    }

    public static class AdjustOptionModel {
        public String name;
        public AdjustType type;
        public int iconResId;
        public AdjustOptionModel(String n, AdjustType t, int i) { name = n; type = t; iconResId = i; }
    }
}