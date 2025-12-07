package com.sevengroup.artifyme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.models.ShareApp;
import java.util.List;

public class ShareAppAdapter extends RecyclerView.Adapter<ShareAppAdapter.ViewHolder> {
    private final List<ShareApp> apps;
    private final OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(ShareApp app);
    }

    public ShareAppAdapter(List<ShareApp> apps, OnAppClickListener listener) {
        this.apps = apps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShareApp app = apps.get(position);
        holder.imgAppIcon.setImageDrawable(app.icon);
        holder.txtAppName.setText(app.name);
        holder.itemView.setOnClickListener(v -> listener.onAppClick(app));
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAppIcon;
        TextView txtAppName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAppIcon = itemView.findViewById(R.id.imgAppIcon);
            txtAppName = itemView.findViewById(R.id.txtAppName);
        }
    }
}