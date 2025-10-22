package com.SevenGroup.ArtifyMe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ThumbViewHolder> {

    private final List<Photo> photos;
    private final OnThumbnailClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private final RecyclerView recyclerView;

    public interface OnThumbnailClickListener {
        void onThumbnailClick(int imageResId);
    }

    public ThumbnailAdapter(List<Photo> photos, OnThumbnailClickListener listener, RecyclerView recyclerView) {
        this.photos = photos;
        this.listener = listener;
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ThumbViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_thumbnail, parent, false);
        return new ThumbViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.imageView.setImageResource(photo.getImageResId());

        // update border depending on selection
        holder.itemView.setBackgroundResource(position == selectedPosition
                ? R.drawable.thumb_border_selected
                : R.drawable.thumb_border);

        // Use getBindingAdapterPosition() to get the up-to-date adapter position
        holder.imageView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            if (listener != null) listener.onThumbnailClick(photo.getImageResId());

            // update selected state and refresh changed items
            int oldPosition = selectedPosition;
            selectedPosition = adapterPosition;

            if (oldPosition != RecyclerView.NO_POSITION) notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);

            // scroll to center the selected item
            scrollToCenter(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public static class ThumbViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageView;

        public ThumbViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbImage);
        }
    }

    public void setSelectedImage(int imageResId) {
        int oldPosition = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;

        for (int i = 0; i < photos.size(); i++) {
            if (photos.get(i).getImageResId() == imageResId) {
                selectedPosition = i;
                break;
            }
        }

        if (oldPosition != RecyclerView.NO_POSITION) notifyItemChanged(oldPosition);
        if (selectedPosition != RecyclerView.NO_POSITION) notifyItemChanged(selectedPosition);

        // center the selected if possible
        if (selectedPosition != RecyclerView.NO_POSITION) scrollToCenter(selectedPosition);
    }

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= photos.size()) return;

        int oldPosition = selectedPosition;
        selectedPosition = position;

        if (oldPosition != RecyclerView.NO_POSITION) notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);

        scrollToCenter(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    private void scrollToCenter(int position) {
        if (recyclerView == null || recyclerView.getLayoutManager() == null) return;

        recyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            View view = layoutManager.findViewByPosition(position);
            if (view == null) {
                // child not laid out yet — scroll then retry
                recyclerView.scrollToPosition(position);
                recyclerView.post(() -> scrollToCenter(position));
                return;
            }

            int itemWidth = view.getWidth();
            int recyclerWidth = recyclerView.getWidth();
            int scrollOffset = view.getLeft() - (recyclerWidth / 2 - itemWidth / 2);
            recyclerView.smoothScrollBy(scrollOffset, 0);
        });
    }
}
