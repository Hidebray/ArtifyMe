package com.sevengroup.artifyme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.models.AiToolType;

import java.util.List;

public class AiToolsAdapter extends RecyclerView.Adapter<AiToolsAdapter.AiToolViewHolder> {
    private final List<AiToolType> toolList;
    private final OnAiToolClickListener clickListener;

    public interface OnAiToolClickListener {
        void onAiToolSelected(AiToolType toolType);
    }

    public AiToolsAdapter(List<AiToolType> toolList, OnAiToolClickListener clickListener) {
        this.toolList = toolList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public AiToolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_tool, parent, false);
        return new AiToolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AiToolViewHolder holder, int position) {
        AiToolType tool = toolList.get(position);
        holder.txtToolName.setText(tool.getNameResId());
        holder.imgToolIcon.setImageResource(tool.getIconResId());
        holder.itemView.setOnClickListener(v -> clickListener.onAiToolSelected(tool));
    }

    @Override
    public int getItemCount() {
        return toolList.size();
    }

    static class AiToolViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgToolIcon;
        final TextView txtToolName;

        AiToolViewHolder(@NonNull View itemView) {
            super(itemView);
            imgToolIcon = itemView.findViewById(R.id.imgToolIcon);
            txtToolName = itemView.findViewById(R.id.txtToolName);
        }
    }
}