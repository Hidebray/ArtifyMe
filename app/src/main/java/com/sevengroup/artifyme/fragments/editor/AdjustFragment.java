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
import java.util.List;

public class AdjustFragment extends Fragment {
    private AdjustListener listener;
    private AdjustType currentType = AdjustType.BRIGHTNESS;
    private float mBrightness, mContrast, mSaturation, mWarmth, mVignette, mTint, mGrain;

    public enum AdjustType {
        BRIGHTNESS,
        CONTRAST,
        SATURATION,
        WARMTH,
        VIGNETTE,
        TINT,
        GRAIN
    }

    public interface AdjustListener {
        void onAdjustmentChanged(AdjustType type, float value);
        void onAdjustApplied();
        void onAdjustCancelled();
    }

    public static AdjustFragment newInstance(float b, float c, float s, float w, float v, float t, float g) {
        AdjustFragment f = new AdjustFragment();
        Bundle args = new Bundle();
        args.putFloat("B", b);
        args.putFloat("C", c);
        args.putFloat("S", s);
        args.putFloat("W", w);
        args.putFloat("V", v);
        args.putFloat("T", t);
        args.putFloat("G", g);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBrightness = getArguments().getFloat("B");
            mContrast = getArguments().getFloat("C");
            mSaturation = getArguments().getFloat("S");
            mWarmth = getArguments().getFloat("W");
            mVignette = getArguments().getFloat("V");
            mTint = getArguments().getFloat("T");
            mGrain = getArguments().getFloat("G");
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
        SeekBar seekBar = view.findViewById(R.id.seekbarAdjust);
        view.findViewById(R.id.btnApplyAdjust).setOnClickListener(v -> listener.onAdjustApplied());
        view.findViewById(R.id.btnCancelAdjust).setOnClickListener(v -> listener.onAdjustCancelled());
        RecyclerView rcv = view.findViewById(R.id.rcvAdjustOptions);

        List<AdjustOptionModel> options = new ArrayList<>();
        options.add(new AdjustOptionModel("Độ sáng", AdjustType.BRIGHTNESS, android.R.drawable.ic_menu_rotate));
        options.add(new AdjustOptionModel("Tương phản", AdjustType.CONTRAST, android.R.drawable.ic_menu_crop));
        options.add(new AdjustOptionModel("Bão hòa", AdjustType.SATURATION, android.R.drawable.ic_menu_view));
        options.add(new AdjustOptionModel("Ấm/Lạnh", AdjustType.WARMTH, android.R.drawable.ic_menu_compass));
        options.add(new AdjustOptionModel("Viền tối", AdjustType.VIGNETTE, android.R.drawable.ic_menu_gallery));
        options.add(new AdjustOptionModel("Màu xanh/tím", AdjustType.TINT, android.R.drawable.ic_menu_manage));
        options.add(new AdjustOptionModel("Hạt phim", AdjustType.GRAIN, android.R.drawable.ic_menu_zoom));

        AdjustOptionsAdapter adapter = new AdjustOptionsAdapter(getContext(), options, type -> {
            currentType = type;
            float val = 0;
            switch(type) {
                case BRIGHTNESS: val = mBrightness; break;
                case CONTRAST: val = mContrast; break;
                case SATURATION: val = mSaturation; break;
                case WARMTH: val = mWarmth; break;
                case VIGNETTE: val = mVignette; break;
                case TINT: val = mTint; break;
                case GRAIN: val = mGrain; break;
            }
            int progress = (int)((val * 50) + 50);
            seekBar.setProgress(progress);
        });
        rcv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rcv.setAdapter(adapter);

        seekBar.setProgress((int)((mBrightness * 50) + 50));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if(fromUser) {
                    float val = (p - 50) / 50.0f;
                    switch(currentType) {
                        case BRIGHTNESS: mBrightness = val; break;
                        case CONTRAST: mContrast = val; break;
                        case SATURATION: mSaturation = val; break;
                        case WARMTH: mWarmth = val; break;
                        case VIGNETTE: mVignette = val; break;
                        case TINT: mTint = val; break;
                        case GRAIN: mGrain = val; break;
                    }
                    listener.onAdjustmentChanged(currentType, val);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    public static class AdjustOptionModel {
        public String name;
        public AdjustType type;
        public int iconResId;
        public AdjustOptionModel(String n, AdjustType t, int i) {
            name = n;
            type = t;
            iconResId = i;
        }
    }
}