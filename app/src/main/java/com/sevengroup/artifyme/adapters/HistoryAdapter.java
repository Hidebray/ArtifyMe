package com.sevengroup.artifyme.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.database.entities.Version;
import com.sevengroup.artifyme.utils.DateUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private List<Version> versions = new ArrayList<>();
    private final Context context;

    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(Version version);
    }

    public HistoryAdapter(Context context, OnHistoryItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_version, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Version currentVersion = versions.get(position);
        holder.txtVersionName.setText(currentVersion.isOriginal ? "Gốc" : "Đã chỉnh sửa");
        holder.txtVersionCreatedTime.setText(DateUtils.formatDateTime(currentVersion.createdTime));
        File imageFile = new File(currentVersion.imagePath);
        if (imageFile.exists()) {
            Glide.with(context).load(imageFile).centerCrop().into(holder.imgThumbnail);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryItemClick(currentVersion);
            }
        });
    }

    @Override public int getItemCount() { return versions.size(); }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
        notifyDataSetChanged();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgThumbnail;
        final TextView txtVersionName;
        final TextView txtVersionCreatedTime;
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgHistoryThumbnail);
            txtVersionName = itemView.findViewById(R.id.txtVersionName);
            txtVersionCreatedTime = itemView.findViewById(R.id.txtVersionCreatedTime);
        }
    }
}