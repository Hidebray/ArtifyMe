package com.sevengroup.artifyme.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.EditorToolsAdapter;
import com.sevengroup.artifyme.fragments.editor.AdjustFragment;
import com.sevengroup.artifyme.fragments.editor.FilterFragment;
import com.sevengroup.artifyme.fragments.editor.TextFragment;
import com.sevengroup.artifyme.managers.AdjustEditorManager;
import com.sevengroup.artifyme.managers.FilterEditorManager;
import com.sevengroup.artifyme.managers.TextEditorManager;
import com.sevengroup.artifyme.utils.AppConstants;
import com.sevengroup.artifyme.viewmodels.BasicEditorViewModel;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class BasicEditorActivity extends BaseActivity implements
        AdjustFragment.AdjustListener,
        TextFragment.TextFragmentListener,
        EditorToolsAdapter.OnToolClickListener,
        FilterFragment.FilterListener {

    private ProgressBar prbEditor;
    private GPUImageView gpuImgView;
    private PhotoEditorView photoEdtView;
    private RecyclerView rcvMainTools;
    private FrameLayout frlPanelContainer;

    // Header
    private TextView txtHeaderTitle;
    private View btnHeaderBack;
    private ImageView btnHeaderSave;

    private BasicEditorViewModel viewModel;
    private EditorToolsAdapter toolsAdapter;
    private TextEditorManager mTextManager;
    private FilterEditorManager mFilterManager;
    private AdjustEditorManager mAdjustManager;

    private long currentProjectId;
    private String latestImagePath;
    private Bitmap mainBitmap;
    private final List<String> toolList = Arrays.asList("Adjust", "Text", "Filter", "Crop");

    private final ActivityResultLauncher<Intent> cropResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    final Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        this.latestImagePath = resultUri.getPath();
                        // Reset filters before loading cropped image
                        mFilterManager.resetAll();
                        // Load the cropped image
                        viewModel.loadEditableBitmap(latestImagePath);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_editor);

        initViews();

        setupCustomHeader(txtHeaderTitle, "Editor", btnHeaderBack);
        if (btnHeaderSave != null) {
            btnHeaderSave.setVisibility(View.VISIBLE);
            btnHeaderSave.setOnClickListener(v -> saveChanges());
        }

        if (!loadIntentData()) return;

        mTextManager = new TextEditorManager(this, photoEdtView);
        mFilterManager = new FilterEditorManager(gpuImgView);
        mAdjustManager = mFilterManager.getAdjustManager();

        setupViewModel();
        setupToolsRecyclerView();
        viewModel.loadEditableBitmap(latestImagePath);
    }

    private void initViews() {
        prbEditor = findViewById(R.id.prbEditor);
        gpuImgView = findViewById(R.id.gpuImgView);
        photoEdtView = findViewById(R.id.photoEdtView);
        rcvMainTools = findViewById(R.id.rcvMainTools);
        frlPanelContainer = findViewById(R.id.frlPanelContainer);

        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        btnHeaderBack = findViewById(R.id.btnHeaderBack);
        btnHeaderSave = findViewById(R.id.btnHeaderSave);
    }

    private boolean loadIntentData() {
        if (getIntent() != null) {
            currentProjectId = getIntent().getLongExtra(AppConstants.KEY_PROJECT_ID, -1L);
            latestImagePath = getIntent().getStringExtra(AppConstants.KEY_IMAGE_PATH);
        }
        if (currentProjectId == -1 || latestImagePath == null) {
            showToast("Lỗi: Không tải được ảnh.");
            finish();
            return false;
        }
        return true;
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BasicEditorViewModel.class);
        viewModel.getIsLoading().observe(this, isLoading ->
                showLoading(prbEditor, isLoading != null && isLoading));
        viewModel.getErrorMessage().observe(this, error -> {
            showToast(error);
            if (error != null && error.contains("Cannot load")) finish();
        });
        viewModel.getLoadedBitmap().observe(this, bitmap -> {
            if (bitmap != null) {
                this.mainBitmap = bitmap;
                // Reset adjustments before loading new image to prevent stacking
                mFilterManager.resetAll();
                // Now load the image into the filter manager
                mFilterManager.setImage(mainBitmap);
                photoEdtView.getSource().setImageBitmap(mainBitmap);
                photoEdtView.getSource().setAlpha(0f);
                rcvMainTools.setVisibility(View.VISIBLE);
            }
        });
        viewModel.getSaveComplete().observe(this, isComplete -> {
            if (isComplete != null && isComplete) {
                showToast("Đã lưu thành công!");
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void setupToolsRecyclerView() {
        toolsAdapter = new EditorToolsAdapter(toolList, this);
        rcvMainTools.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rcvMainTools.setAdapter(toolsAdapter);
    }

    private void saveChanges() {
        showLoading(prbEditor, true);
        try {
            Bitmap processedBitmap = mFilterManager.capture();
            photoEdtView.getSource().setImageBitmap(processedBitmap);
            photoEdtView.getSource().setAlpha(1f);
            mTextManager.saveImage(savedBitmap -> viewModel.saveEditedImage(currentProjectId, savedBitmap));
        } catch (InterruptedException e) {
            e.printStackTrace();
            showLoading(prbEditor, false);
        }
    }

    @Override
    public void onToolSelected(String toolName) {
        gpuImgView.setVisibility(View.VISIBLE);
        photoEdtView.setVisibility(View.VISIBLE);
        switch (toolName) {
            case "Adjust":
                float b = mAdjustManager.getCurrentValue(AdjustFragment.AdjustType.BRIGHTNESS);
                float c = mAdjustManager.getCurrentValue(AdjustFragment.AdjustType.CONTRAST);
                float s = mAdjustManager.getCurrentValue(AdjustFragment.AdjustType.SATURATION);
                float w = mAdjustManager.getCurrentValue(AdjustFragment.AdjustType.WARMTH);
                float v = mAdjustManager.getCurrentValue(AdjustFragment.AdjustType.VIGNETTE);
                float t = mAdjustManager.getCurrentValue(AdjustFragment.AdjustType.TINT);
                float g = mAdjustManager.getCurrentValue(AdjustFragment.AdjustType.GRAIN);
                openFragment(AdjustFragment.newInstance(b, c, s, w, v, t, g));
                break;
            case "Text":
                openFragment(TextFragment.newInstance());
                break;
            case "Filter":
                int currentIndex = mFilterManager.getFilterIndex();
                FilterFragment filterFragment = FilterFragment.newInstance(currentIndex);
                Bitmap thumbnail = Bitmap.createScaledBitmap(mainBitmap, 150, 150, false);
                filterFragment.setPreviewBitmap(thumbnail);
                openFragment(filterFragment);
                break;
            case "Crop":
                startCrop();
                break;
        }
    }

    // --- Fragment Callbacks ---
    @Override
    public void onAdjustmentChanged(AdjustFragment.AdjustType type, float value) {
        mAdjustManager.adjust(type, value);
    }

    @Override
    public void onAdjustApplied() {
        mAdjustManager.saveCurrentState();
        closeFragment();
    }

    @Override
    public void onAdjustCancelled() {
        mAdjustManager.restoreState();
        closeFragment();
    }

    @Override
    public void onTextApplied(String text, int colorCode) {
        mTextManager.addText(text, colorCode);
        closeFragment();
    }

    @Override
    public void onTextCancelled() {
        closeFragment();
    }

    @Override
    public void onFilterSelected(GPUImageFilter filter, int index) {
        mFilterManager.setFilterIndex(index);
        mFilterManager.applyFilter(filter);
    }

    @Override
    public void onFilterApplied() {
        mFilterManager.saveCurrentState();
        closeFragment();
    }

    @Override
    public void onFilterCancelled() {
        mFilterManager.restoreState();
        closeFragment();
    }

    private void startCrop() {
        showLoading(prbEditor, true);

        new Thread(() -> {
            try {
                Bitmap currentBitmap = mFilterManager.capture();

                File tempFile = new File(getCacheDir(), "temp_crop_source_" + System.currentTimeMillis() + ".jpg");
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                    currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                }

                runOnUiThread(() -> {
                    showLoading(prbEditor, false);

                    Uri sourceUri = Uri.fromFile(tempFile);
                    File destFile = new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg");

                    UCrop.of(sourceUri, Uri.fromFile(destFile))
                            .withOptions(new UCrop.Options())
                            .start(BasicEditorActivity.this, cropResultLauncher);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showLoading(prbEditor, false);
                    showToast("Lỗi: Không thể chuẩn bị ảnh để cắt.");
                });
            }
        }).start();
    }

    private void openFragment(Fragment fragment) {
        frlPanelContainer.setVisibility(View.VISIBLE);
        rcvMainTools.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().replace(R.id.frlPanelContainer, fragment).commit();
    }

    private void closeFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frlPanelContainer);
        if (fragment != null) getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        frlPanelContainer.setVisibility(View.GONE);
        rcvMainTools.setVisibility(View.VISIBLE);
    }
}