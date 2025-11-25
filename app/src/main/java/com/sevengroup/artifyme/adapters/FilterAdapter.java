package com.sevengroup.artifyme.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewHolder> {
    private final List<FilterModel> filterList;
    private final OnFilterClickListener listener;
    private final Context context;
    private final Bitmap thumbnailBitmap;
    private final Map<Integer, Bitmap> previewCache = new HashMap<>();
    private final ExecutorService renderExecutor = Executors.newFixedThreadPool(1);
    private int selectedPosition;

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
        this.context = context;
        this.filterList = filterList;
        this.thumbnailBitmap = thumbnailBitmap;
        this.listener = listener;
        this.selectedPosition = initialPosition;
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

        if (previewCache.containsKey(position)) {
            holder.imgFilterPreview.setImageBitmap(previewCache.get(position));
        } else {

            if (thumbnailBitmap != null) {
                holder.imgFilterPreview.setImageBitmap(thumbnailBitmap);

                renderExecutor.execute(() -> {
                    try {
                        GPUImage gpuImage = new GPUImage(context);
                        gpuImage.setImage(thumbnailBitmap);
                        gpuImage.setFilter(item.filter);
                        Bitmap processedBitmap = gpuImage.getBitmapWithFilterApplied();

                        previewCache.put(position, processedBitmap);

                        new Handler(Looper.getMainLooper()).post(() -> {
                            notifyItemChanged(position);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        if (position == selectedPosition) {
            holder.txtFilterName.setTextColor(Color.YELLOW);
            holder.txtFilterName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.itemView.setBackgroundColor(Color.parseColor("#444444"));
        } else {
            holder.txtFilterName.setTextColor(Color.WHITE);
            holder.txtFilterName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
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