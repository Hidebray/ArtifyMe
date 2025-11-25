package com.sevengroup.artifyme.fragments.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.FilterAdapter;
import java.util.ArrayList;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.filter.*;

public class FilterFragment extends Fragment implements FilterAdapter.OnFilterClickListener {
    private FilterListener listener;
    private Bitmap previewBitmap;
    private int currentFilterIndex = 0;
    public interface FilterListener {
        void onFilterSelected(GPUImageFilter filter, int index);
        void onFilterApplied();
        void onFilterCancelled();
    }

    public static FilterFragment newInstance(int currentIndex) {
        FilterFragment fragment = new FilterFragment();
        Bundle args = new Bundle();
        args.putInt("INDEX", currentIndex);
        fragment.setArguments(args);
        return fragment;
    }
    public void setPreviewBitmap(Bitmap bitmap) {
        this.previewBitmap = bitmap;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentFilterIndex = getArguments().getInt("INDEX", 0);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FilterListener) {
            listener = (FilterListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement FilterListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> listener.onFilterApplied());
        view.findViewById(R.id.btnCancelFilter).setOnClickListener(v -> listener.onFilterCancelled());

        RecyclerView rcvFilters = view.findViewById(R.id.rcvFilters);

        List<FilterAdapter.FilterModel> filters = new ArrayList<>();
        filters.add(new FilterAdapter.FilterModel("Normal", new GPUImageFilter()));
        filters.add(new FilterAdapter.FilterModel("Sepia", new GPUImageSepiaToneFilter()));
        filters.add(new FilterAdapter.FilterModel("Grayscale", new GPUImageGrayscaleFilter()));
        filters.add(new FilterAdapter.FilterModel("Invert", new GPUImageColorInvertFilter()));
        filters.add(new FilterAdapter.FilterModel("Contrast", new GPUImageContrastFilter(2.0f)));
        filters.add(new FilterAdapter.FilterModel("Sketch", new GPUImageSketchFilter()));
        filters.add(new FilterAdapter.FilterModel("Vignette", new GPUImageVignetteFilter()));

        FilterAdapter adapter = new FilterAdapter(getContext(), filters, previewBitmap, currentFilterIndex, this);
        rcvFilters.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rcvFilters.setAdapter(adapter);
        rcvFilters.scrollToPosition(currentFilterIndex);
    }

    @Override
    public void onFilterSelected(GPUImageFilter filter, int index) {
        if (listener != null) listener.onFilterSelected(filter, index);
    }

}