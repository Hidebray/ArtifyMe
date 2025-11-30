package com.sevengroup.artifyme.adapters;

import android.annotation.SuppressLint;
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
import com.sevengroup.artifyme.database.ProjectWithLatestVersion;
import com.sevengroup.artifyme.utils.DateUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {
    private List<ProjectWithLatestVersion> projects = new ArrayList<>();
    private final OnProjectClickListener clickListener;
    private final Context context;

    public interface OnProjectClickListener {
        void onProjectClick(ProjectWithLatestVersion project);
    }

    public ProjectAdapter(Context context, OnProjectClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
    }

    @NonNull @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        ProjectWithLatestVersion currentProject = projects.get(position);
        holder.txtProjectName.setText(currentProject.project.projectName);
        holder.txtProjectDate.setText(DateUtils.formatDateTime(currentProject.project.createdTime));
        File imageFile = new File(currentProject.latestVersionPath);
        if (imageFile.exists()) {
            Glide.with(context).load(imageFile).centerCrop().into(holder.imgThumbnail);
        }
        holder.itemView.setOnClickListener(v -> clickListener.onProjectClick(currentProject));
    }

    @Override public int getItemCount() { return projects.size(); }

    @SuppressLint("NotifyDataSetChanged")
    public void setProjects(List<ProjectWithLatestVersion> projects) {
        this.projects = projects;
        notifyDataSetChanged();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgThumbnail;
        final TextView txtProjectName;
        final TextView txtProjectDate;
        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            txtProjectName = itemView.findViewById(R.id.txtProjectName);
            txtProjectDate = itemView.findViewById(R.id.txtProjectDate);
        }
    }
}