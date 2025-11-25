package com.sevengroup.artifyme.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.fragments.detail.InfoBottomSheetFragment;
import com.sevengroup.artifyme.utils.AppConstants;
import com.sevengroup.artifyme.viewmodels.ProjectDetailViewModel;
import java.io.File;

public class ProjectDetailActivity extends BaseActivity {
    private ImageView imgProjectMainImage;
    private Button btnEditBasic, btnExport, btnDeleteProject;
    private ProgressBar prbExport;

    // Header
    private TextView txtHeaderTitle;
    private View btnHeaderBack;
    private ImageView btnHeaderInfo;

    private ProjectDetailViewModel viewModel;
    private long currentProjectId;
    private String latestImagePath;

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

        setupCustomHeader(txtHeaderTitle, "Chi tiết", btnHeaderBack);
        if (btnHeaderInfo != null) {
            btnHeaderInfo.setVisibility(View.VISIBLE);
            btnHeaderInfo.setOnClickListener(v -> showHistoryBottomSheet());
        }

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
            }
        }
        if (currentProjectId == -1 || latestImagePath == null) {
            showToast("Lỗi: Không thể tải dự án");
            finish();
            return false;
        }
        return true;
    }

    private void initViews() {
        imgProjectMainImage = findViewById(R.id.imgProjectMainImage);
        btnEditBasic = findViewById(R.id.btnEditBasic);
        btnExport = findViewById(R.id.btnExport);
        btnDeleteProject = findViewById(R.id.btnDeleteProject);
        prbExport = findViewById(R.id.prbExport);

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        btnHeaderBack = findViewById(R.id.btnHeaderBack);
        btnHeaderInfo = findViewById(R.id.btnHeaderInfo);

        btnEditBasic.setOnClickListener(v -> startBasicEditor());
        btnExport.setOnClickListener(v -> viewModel.exportImageToGallery(latestImagePath));
        btnDeleteProject.setOnClickListener(v -> confirmDelete());
    }

    private void observeViewModel() {
        viewModel.getLatestImagePath().observe(this, newPath -> {
            if (newPath != null && !newPath.isEmpty()) {
                this.latestImagePath = newPath;
                loadLatestImage();
            }
        });
        viewModel.getIsExporting().observe(this, isExporting ->
                showLoading(prbExport, isExporting != null && isExporting));
        viewModel.getExportStatusMessage().observe(this, this::showToast);
        viewModel.getProjectDeleted().observe(this, isDeleted -> {
            if (isDeleted != null && isDeleted) {
                showToast("Đã xóa dự án.");
                finish();
            }
        });
    }

    private void loadLatestImage() {
        File imageFile = new File(latestImagePath);
        if (imageFile.exists()) {
            Glide.with(this).load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .fitCenter().into(imgProjectMainImage);
        }
    }

    private void showHistoryBottomSheet() {
        InfoBottomSheetFragment.newInstance(currentProjectId)
                .show(getSupportFragmentManager(), "bottom_sheet");
    }

    private void startBasicEditor() {
        Intent intent = new Intent(this, BasicEditorActivity.class);
        Bundle data = new Bundle();
        data.putLong(AppConstants.KEY_PROJECT_ID, currentProjectId);
        data.putString(AppConstants.KEY_IMAGE_PATH, latestImagePath);
        intent.putExtras(data);
        editorResultLauncher.launch(intent);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Dự Án")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn dự án này?")
                .setPositiveButton("Xóa", (d, w) -> viewModel.handleDeleteProject(currentProjectId))
                .setNegativeButton("Hủy", null)
                .show();
    }
}