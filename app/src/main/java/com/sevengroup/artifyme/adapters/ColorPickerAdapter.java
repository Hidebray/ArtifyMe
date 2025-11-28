package com.sevengroup.artifyme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevengroup.artifyme.R;

import java.util.List;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {
    private final List<Integer> colorList;
    private final OnColorClickListener colorClickListener;
    private int selectedPosition = 0;

    public ColorPickerAdapter(List<Integer> colorList, OnColorClickListener colorClickListener) {
        this.colorList = colorList;
        this.colorClickListener = colorClickListener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemview = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_picker, parent, false);
        return new ColorViewHolder(itemview);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        int colorCode = colorList.get(position);
        holder.viewColorSwatch.setBackgroundColor(colorCode);
        if (position == selectedPosition) {
            holder.imgColorCheck.setVisibility(View.VISIBLE);

            if (colorCode == android.graphics.Color.BLACK) {
                holder.imgColorCheck.setColorFilter(android.graphics.Color.WHITE);
            } else {
                holder.imgColorCheck.setColorFilter(android.graphics.Color.BLACK);
            }
        } else {
            holder.imgColorCheck.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            colorClickListener.onColorSelected(colorCode);
        });
    }

    @Override
    public int getItemCount() {
        return colorList.size();
    }

    public interface OnColorClickListener {
        void onColorSelected(int colorCode);
    }

    public static class ColorViewHolder extends RecyclerView.ViewHolder {
        final View viewColorSwatch;
        final ImageView imgColorCheck;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorSwatch = itemView.findViewById(R.id.viewColorSwatch);
            imgColorCheck = itemView.findViewById(R.id.imgColorCheck);
        }
    }
}