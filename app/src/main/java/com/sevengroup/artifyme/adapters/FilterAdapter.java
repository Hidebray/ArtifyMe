package com.sevengroup.artifyme.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.utils.AppExecutors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewHolder> {
    private final List<FilterModel> filterList;
    private final OnFilterClickListener listener;
    private Bitmap thumbnailBitmap;
    private final Map<Integer, Bitmap> previewCache = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int selectedPosition;
    private final GPUImage gpuImageHelper;
    private final AppExecutors appExecutors;

    public interface OnFilterClickListener {
        void onFilterSelected(GPUImageFilter filter, int position);
    }

    public static class FilterModel {
        public String name;
        public GPUImageFilter filter;
        public FilterModel(String name, GPUImageFilter filter) {
            this.name = name;
            this.filter = filter;
        }
    }

    public FilterAdapter(Context context, List<FilterModel> filterList, Bitmap thumbnailBitmap, int initialPosition, OnFilterClickListener listener) {
        this.filterList = filterList;
        this.thumbnailBitmap = thumbnailBitmap;
        this.listener = listener;
        this.selectedPosition = initialPosition;
        this.gpuImageHelper = new GPUImage(context);
        this.appExecutors = AppExecutors.getInstance();
    }

    public void release() {
        mainHandler.removeCallbacksAndMessages(null);
        previewCache.clear();
    }

    public void setThumbnailBitmap(Bitmap bitmap) {
        this.thumbnailBitmap = bitmap;
        this.previewCache.clear();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editor_filter, parent, false);
        return new FilterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        FilterModel item = filterList.get(position);
        holder.txtFilterName.setText(item.name);

        boolean isSelected = (position == selectedPosition);
        holder.itemView.setSelected(isSelected);

        if (previewCache.containsKey(position)) {
            holder.imgFilterPreview.setImageBitmap(previewCache.get(position));
        } else {
            holder.imgFilterPreview.setImageBitmap(thumbnailBitmap);

            appExecutors.diskIO().execute(() -> {
                if (previewCache.containsKey(position)) {
                    mainHandler.post(() -> notifyItemChanged(position));
                    return;
                }

                try {
                    Bitmap processedBitmap;
                    synchronized (gpuImageHelper) {
                        gpuImageHelper.setImage(thumbnailBitmap);
                        gpuImageHelper.setFilter(item.filter);
                        processedBitmap = gpuImageHelper.getBitmapWithFilterApplied();
                    }

                    if (processedBitmap != null) {
                        previewCache.put(position, processedBitmap);
                        mainHandler.post(() -> notifyItemChanged(position));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            listener.onFilterSelected(item.filter, selectedPosition);
        });
    }

    @Override public int getItemCount() { return filterList.size(); }

    public static class FilterViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFilterPreview;
        TextView txtFilterName;
        public FilterViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFilterPreview = itemView.findViewById(R.id.imgFilterPreview);
            txtFilterName = itemView.findViewById(R.id.txtFilterName);
        }
    }
}