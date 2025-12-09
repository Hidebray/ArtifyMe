package com.sevengroup.artifyme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.models.EditorToolType;

import java.util.List;

public class EditorToolsAdapter extends RecyclerView.Adapter<EditorToolsAdapter.ToolViewHolder> {
    private final List<EditorToolType> toolList;
    private final OnToolClickListener toolClickListener;

    public interface OnToolClickListener {
        void onToolSelected(EditorToolType toolType);
    }

    public EditorToolsAdapter(List<EditorToolType> toolList, OnToolClickListener toolClickListener) {
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
        EditorToolType tool = toolList.get(position);

        holder.txtToolName.setText(tool.getNameResId());
        holder.imgToolIcon.setImageResource(tool.getIconResId());

        holder.itemView.setOnClickListener(v -> toolClickListener.onToolSelected(tool));
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