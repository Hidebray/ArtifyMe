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

    public EditorToolsAdapter(List<String> toolList, OnToolClickListener toolClickListener) {
        this.toolList = toolList;
        this.toolClickListener = toolClickListener;
    }

    @NonNull
    @Override
    public ToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editor_tool, parent, false);
        return new ToolViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ToolViewHolder holder, int position) {
        String toolName = toolList.get(position);
        holder.txtToolName.setText(toolName);
        holder.imgToolIcon.setImageResource(R.drawable.edit_24px);
        holder.itemView.setOnClickListener(v -> toolClickListener.onToolSelected(toolName));
    }

    @Override
    public int getItemCount() {
        return toolList.size();
    }

    public interface OnToolClickListener {
        void onToolSelected(String toolName);
    }

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