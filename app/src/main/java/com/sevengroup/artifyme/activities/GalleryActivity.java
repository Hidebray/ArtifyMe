package com.sevengroup.artifyme.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.ProjectAdapter;
import com.sevengroup.artifyme.database.ProjectWithLatestVersion;
import com.sevengroup.artifyme.utils.AppConstants;
import com.sevengroup.artifyme.utils.PermissionUtils;
import com.sevengroup.artifyme.viewmodels.GalleryViewModel;

public class GalleryActivity extends BaseActivity implements ProjectAdapter.OnProjectClickListener {
    private RecyclerView rcvProjects;
    private TextView txtEmptyState;
    private FloatingActionButton fabAddProject;
    private GalleryViewModel galleryViewModel;
    private ProjectAdapter projectAdapter;

    // Header Views
    private TextView txtHeaderTitle;
    private View btnHeaderBack;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    showToast("Cần cấp quyền truy cập ảnh để sử dụng.");
                    showEmptyStateViews(true);
                } else {
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

        setupCustomHeader(txtHeaderTitle, "ArtifyMe", null);
        if (btnHeaderBack != null) btnHeaderBack.setVisibility(View.GONE);

        setupRecyclerView();
        setupViewModel();
        checkAndRequestPermissions();
    }

    private void initViews() {
        rcvProjects = findViewById(R.id.rcv_projects);
        txtEmptyState = findViewById(R.id.txtEmptyState);
        fabAddProject = findViewById(R.id.fabAddProject);

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        btnHeaderBack = findViewById(R.id.btnHeaderBack);

        fabAddProject.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void setupRecyclerView() {
        projectAdapter = new ProjectAdapter(this, this);
        rcvProjects.setLayoutManager(new GridLayoutManager(this, 2));
        rcvProjects.setAdapter(projectAdapter);
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
        txtEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rcvProjects.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onProjectClick(ProjectWithLatestVersion project) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        Bundle data = new Bundle();
        data.putLong(AppConstants.KEY_PROJECT_ID, project.project.projectId);
        data.putString(AppConstants.KEY_IMAGE_PATH, project.latestVersionPath);
        intent.putExtras(data);
        startActivity(intent);
    }
}