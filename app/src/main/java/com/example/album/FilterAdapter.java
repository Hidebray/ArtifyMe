package com.example.album;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {

    public interface OnFilterClickListener {
        void onFilterClick(String filterName, int position);
    }

    private final String[] filters;
    private final int[] filterIcons;
    private final OnFilterClickListener listener;

    // default select first filter
    private int selectedPosition = 0;

    public FilterAdapter(String[] filters, int[] filterIcons, OnFilterClickListener listener) {
        this.filters = filters;
        this.filterIcons = filterIcons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String filterName = filters[position];
        holder.filterLabel.setText(filterName);

        // safe icon set (fallback)
        if (position < filterIcons.length) {
            holder.filterIcon.setImageResource(filterIcons[position]);
        } else {
            holder.filterIcon.setImageResource(R.drawable.ic_filter);
        }

        // Use drawable backgrounds to show selection clearly
        if (position == selectedPosition) {
            holder.filterIcon.setBackgroundResource(R.drawable.round_button_selected);
        } else {
            holder.filterIcon.setBackgroundResource(R.drawable.round_button);
        }

        // safe click handling using binding adapter position
        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            int previous = selectedPosition;
            selectedPosition = adapterPos;

            if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);

            if (listener != null) listener.onFilterClick(filters[adapterPos], adapterPos);
        });
    }

    @Override
    public int getItemCount() {
        return filters.length;
    }

    /** Optional helper to get currently selected index from activity */
    public int getSelectedPosition() {
        return selectedPosition;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView filterIcon;
        TextView filterLabel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            filterIcon = itemView.findViewById(R.id.filterIcon);
            filterLabel = itemView.findViewById(R.id.filterLabel);
        }
    }
}
