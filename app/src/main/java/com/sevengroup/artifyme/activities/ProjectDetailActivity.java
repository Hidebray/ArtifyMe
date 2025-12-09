package com.sevengroup.artifyme.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.fragments.detail.InfoBottomSheetFragment;
import com.sevengroup.artifyme.fragments.share.ShareBottomSheetFragment;
import com.sevengroup.artifyme.utils.AppConstants;
import com.sevengroup.artifyme.viewmodels.ProjectDetailViewModel;
import java.io.File;

public class ProjectDetailActivity extends BaseActivity {
    private ImageView imgProject;
    private ImageButton btnBack, btnInfo;
    private TextView txtProjectName;
    private ProgressBar prbShare;
    private LinearLayout btnShare, btnEdit, btnDelete;

    private ProjectDetailViewModel viewModel;
    private long currentProjectId;
    private String currentProjectName;
    private String latestImagePath;

    private final ActivityResultLauncher<String> requestWritePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    viewModel.exportImageToGallery(latestImagePath);
                } else {
                    showToast("Cần quyền truy cập bộ nhớ để lưu ảnh (Android 9 trở xuống).");
                }
            });

    private final ActivityResultLauncher<Intent> editorResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    viewModel.refreshLatestImagePath(currentProjectId);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        if (!loadIntentData()) return;

        initViews();

        viewModel = new ViewModelProvider(this).get(ProjectDetailViewModel.class);
        observeViewModel();
        loadLatestImage();
    }

    private boolean loadIntentData() {
        if (getIntent() != null) {
            Bundle data = getIntent().getExtras();
            if (data != null) {
                currentProjectId = data.getLong(AppConstants.KEY_PROJECT_ID, -1L);
                latestImagePath = data.getString(AppConstants.KEY_IMAGE_PATH);
                currentProjectName = data.getString(AppConstants.KEY_PROJECT_NAME, getString(R.string.default_project_name));            }
        }
        if (currentProjectId == -1 || latestImagePath == null) {
            showToast(getString(R.string.msg_error_load_project));
            finish();
            return false;
        }
        return true;
    }

    private void initViews() {
        imgProject = findViewById(R.id.imgProject);
        btnBack = findViewById(R.id.btnBack);
        btnInfo = findViewById(R.id.btnInfo);
        txtProjectName = findViewById(R.id.txtProjectName);
        btnShare = findViewById(R.id.btnShare);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        prbShare = findViewById(R.id.prbShare);

        if (currentProjectName != null) {
            txtProjectName.setText(currentProjectName);
        }

        btnBack.setOnClickListener(v -> finish());
        btnInfo.setOnClickListener(v -> showHistoryBottomSheet());
        btnEdit.setOnClickListener(v -> startBasicEditor());
        btnShare.setOnClickListener(v -> showShareBottomSheet());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void observeViewModel() {
        viewModel.getLatestImagePath().observe(this, newPath -> {
            if (newPath != null && !newPath.isEmpty()) {
                this.latestImagePath = newPath;
                loadLatestImage();
            }
        });
        viewModel.getIsExporting().observe(this, isExporting ->
                showLoading(prbShare, isExporting != null && isExporting));
        viewModel.getExportStatusMessage().observe(this, this::showToast);
        viewModel.getProjectDeleted().observe(this, isDeleted -> {
            if (isDeleted != null && isDeleted) {
                showToast("Đã xóa dự án.");
                finish();
            }
        });
        viewModel.getNavigateToEditor().observe(this, path -> {
            if (path != null) {
                startBasicEditor(path);
                viewModel.onEditVersionNavigated();
            }
        });
    }

    private void loadLatestImage() {
        File imageFile = new File(latestImagePath);
        if (imageFile.exists()) {
            Glide.with(this).load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .fitCenter()
                    .into(imgProject);
        }
    }

    private void showHistoryBottomSheet() {
        InfoBottomSheetFragment.newInstance(currentProjectId)
                .show(getSupportFragmentManager(), "bottom_sheet");
    }

    private void showShareBottomSheet() {
        ShareBottomSheetFragment shareSheet = ShareBottomSheetFragment.newInstance(latestImagePath);
        shareSheet.setOnExportClickListener(this::handleExport);
        shareSheet.show(getSupportFragmentManager(), "share_bottom_sheet");
    }

    private void handleExport() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                viewModel.exportImageToGallery(latestImagePath);
            } else {
                requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else {
            viewModel.exportImageToGallery(latestImagePath);
        }
    }

    private void startBasicEditor() {
        startBasicEditor(latestImagePath);
    }
    private void startBasicEditor(String imagePath) {
        Intent intent = new Intent(this, BasicEditorActivity.class);
        Bundle data = new Bundle();
        data.putLong(AppConstants.KEY_PROJECT_ID, currentProjectId);
        data.putString(AppConstants.KEY_IMAGE_PATH, imagePath);
        intent.putExtras(data);
        editorResultLauncher.launch(intent);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_msg)
                .setPositiveButton(R.string.btn_delete, (d, w) -> viewModel.handleDeleteProject(currentProjectId))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}