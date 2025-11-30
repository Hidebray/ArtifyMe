package com.sevengroup.artifyme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import java.util.List;

public class EditorToolsAdapter extends RecyclerView.Adapter<EditorToolsAdapter.ToolViewHolder> {
    private final List<String> toolList;
    private final OnToolClickListener toolClickListener;

    public interface OnToolClickListener {
        void onToolSelected(String toolName);
    }

    public EditorToolsAdapter (List<String> toolList, OnToolClickListener toolClickListener) {
        this.toolList = toolList;
        this.toolClickListener = toolClickListener;
    }

    @NonNull @Override
    public ToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editor_tool, parent, false);
        return new ToolViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ToolViewHolder holder, int position) {
        String toolName = toolList.get(position);
        int iconResId = R.drawable.edit_24px; // Icon mặc định

        int nameResId = 0;
        switch (toolName) {
            case "Crop":
                nameResId = R.string.tool_crop;
                iconResId = R.drawable.crop_24px; //
                break;
            case "Adjust":
                nameResId = R.string.tool_adjust;
                iconResId = R.drawable.adjust_24px; //
                break;
            case "Filter":
                nameResId = R.string.tool_filter;
                iconResId = R.drawable.filter_24px; //
                break;
            case "Text":
                nameResId = R.string.tool_text;
                iconResId = R.drawable.title_24px; //
                break;
        }

        if (nameResId != 0) {
            holder.txtToolName.setText(nameResId);
        } else {
            holder.txtToolName.setText(toolName);
        }

        holder.imgToolIcon.setImageResource(iconResId);
        holder.itemView.setOnClickListener(v -> toolClickListener.onToolSelected(toolName));
    }

    @Override public int getItemCount() { return toolList.size(); }

    public static class ToolViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgToolIcon;
        final TextView txtToolName;
        public ToolViewHolder(@NonNull View itemView) {
            super(itemView);
            imgToolIcon = itemView.findViewById(R.id.imgToolIcon);
            txtToolName = itemView.findViewById(R.id.txtToolName);
        }
    }
}