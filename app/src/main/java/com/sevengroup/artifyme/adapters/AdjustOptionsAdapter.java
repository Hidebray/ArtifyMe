package com.sevengroup.artifyme.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.fragments.editor.AdjustFragment;
import java.util.List;

public class AdjustOptionsAdapter extends RecyclerView.Adapter<AdjustOptionsAdapter.ViewHolder> {
    private final List<AdjustFragment.AdjustOptionModel> options;
    private final OnAdjustSelectListener listener;
    private int selectedPosition = 0;

    public interface OnAdjustSelectListener {
        void onOptionSelected(AdjustFragment.AdjustType type);
    }

    public AdjustOptionsAdapter(Context context, List<AdjustFragment.AdjustOptionModel> options, OnAdjustSelectListener listener) {
        this.options = options;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adjust_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdjustFragment.AdjustOptionModel option = options.get(position);
        holder.txtName.setText(option.name);
        holder.imgIcon.setImageResource(option.iconResId);
        boolean isSelected = (position == selectedPosition);
        holder.itemView.setSelected(isSelected);
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onOptionSelected(option.type);
        });
    }

    @Override public int getItemCount() { return options.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgOptionIcon);
            txtName = itemView.findViewById(R.id.txtOptionName);
        }
    }
}