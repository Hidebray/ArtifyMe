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
import com.sevengroup.artifyme.utils.FilterGenerator;

import java.util.List;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class FilterFragment extends Fragment implements FilterAdapter.OnFilterClickListener {
    private FilterListener listener;
    private Bitmap previewBitmap;
    private FilterAdapter adapter;
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
        if (adapter != null) {
            adapter.setThumbnailBitmap(bitmap);
        }
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
        List<FilterAdapter.FilterModel> filters = FilterGenerator.getFilters();

        adapter = new FilterAdapter(getContext(), filters, previewBitmap, currentFilterIndex, this);
        rcvFilters.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rcvFilters.setAdapter(adapter);

        if (currentFilterIndex >= 0 && currentFilterIndex < filters.size()) {
            rcvFilters.scrollToPosition(currentFilterIndex);
        }
    }

    @Override
    public void onDestroyView() {
        if (adapter != null) {
            adapter.release();
        }
        super.onDestroyView();
    }

    @Override
    public void onFilterSelected(GPUImageFilter filter, int index) {
        if (listener != null) listener.onFilterSelected(filter, index);
    }
}