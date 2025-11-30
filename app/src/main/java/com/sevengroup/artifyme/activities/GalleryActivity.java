package com.sevengroup.artifyme.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.ProjectAdapter;
import com.sevengroup.artifyme.database.ProjectWithLatestVersion;
import com.sevengroup.artifyme.utils.AppConstants;
import com.sevengroup.artifyme.utils.PermissionUtils;
import com.sevengroup.artifyme.viewmodels.GalleryViewModel;

public class GalleryActivity extends BaseActivity implements ProjectAdapter.OnProjectClickListener {
    private RecyclerView rcvProjects;
    private LinearLayout layoutHeader;
    private LinearLayout layoutEmptyState;
    private ExtendedFloatingActionButton btnAdd;
    private GalleryViewModel galleryViewModel;
    private ProjectAdapter projectAdapter;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    showToast("Cần cấp quyền truy cập ảnh để sử dụng.");
                    showEmptyStateViews(true);
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    galleryViewModel.handleNewImageImport(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        initViews();
        setupRecyclerView();
        setupViewModel();
        checkAndRequestPermissions();
    }

    private void initViews() {
        rcvProjects = findViewById(R.id.rcvProjects);
        layoutHeader = findViewById(R.id.layoutHeader);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        layoutHeader.setElevation(0f);
    }

    private void setupRecyclerView() {
        projectAdapter = new ProjectAdapter(this, this);
        rcvProjects.setLayoutManager(new GridLayoutManager(this, 2));
        rcvProjects.setAdapter(projectAdapter);
        rcvProjects.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && btnAdd.isExtended()) {
                    btnAdd.shrink();
                } else if (dy < 0 && !btnAdd.isExtended()) {
                    btnAdd.extend();
                }
            }
        });
    }

    private void setupViewModel() {
        galleryViewModel = new ViewModelProvider(this).get(GalleryViewModel.class);
        galleryViewModel.getAllProjects().observe(this, projects -> {
            if (projects == null || projects.isEmpty()) {
                showEmptyStateViews(true);
            } else {
                showEmptyStateViews(false);
                projectAdapter.setProjects(projects);
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (!PermissionUtils.hasReadPermission(this)) {
            requestPermissionLauncher.launch(PermissionUtils.getReadPermission());
        }
    }

    private void showEmptyStateViews(boolean show) {
        if (show) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rcvProjects.setVisibility(View.GONE);
            if (!btnAdd.isExtended()) btnAdd.extend();
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rcvProjects.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onProjectClick(ProjectWithLatestVersion project) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        Bundle data = new Bundle();
        data.putLong(AppConstants.KEY_PROJECT_ID, project.project.projectId);
        data.putString(AppConstants.KEY_PROJECT_NAME, project.project.projectName);
        data.putString(AppConstants.KEY_IMAGE_PATH, project.latestVersionPath);
        intent.putExtras(data);
        startActivity(intent);
    }
}