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

import androidx.core.content.FileProvider; // Bắt buộc để tránh crash

import ja.burhanrashid52.photoeditor.OnSaveBitmap; // Để lưu ảnh gộp

import java.io.FileOutputStream;

public class BasicEditorActivity extends BaseActivity implements
        AdjustFragment.AdjustListener,
        TextFragment.TextFragmentListener,
        EditorToolsAdapter.OnToolClickListener,
        FilterFragment.FilterListener {

    private final List<String> toolList = Arrays.asList("Adjust", "Text", "Filter", "Crop");
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
    private final ActivityResultLauncher<Intent> cropResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    final Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        try {
                            Bitmap croppedBitmap = android.provider.MediaStore.Images.Media.getBitmap(
                                    getContentResolver(), resultUri
                            );

                            applyCropResultToPhotoEditor(croppedBitmap);

                        } catch (Exception e) {
                            e.printStackTrace();
                            showToast("Lỗi đọc ảnh crop");
                        }
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(result.getData());
                    showToast("Lỗi cắt ảnh: " + (cropError != null ? cropError.getMessage() : "Unknown"));
                }
                showLoading(prbEditor, false); // Tắt loading
            });
    private long currentProjectId;
    private String latestImagePath;
    private Bitmap mainBitmap;
    private boolean isInitialLoad = true;

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
        // Save ALL layers in PhotoEditorView (ảnh crop + filter + text)
        mTextManager.getPhotoEditor().saveAsBitmap(new OnSaveBitmap() {
            @Override
            public void onBitmapReady(Bitmap finalBitmap) {

                // Lưu xuống DB
                viewModel.saveEditedImage(currentProjectId, finalBitmap);
            }
        });
    }

    @Override
    public void onToolSelected(String toolName) {
        gpuImgView.setVisibility(View.VISIBLE);
        photoEdtView.setVisibility(View.VISIBLE);
        switch (toolName) {
            case "Adjust":
                float b = mFilterManager.getCurrentValue(AdjustFragment.AdjustType.BRIGHTNESS);
                float c = mFilterManager.getCurrentValue(AdjustFragment.AdjustType.CONTRAST);
                float s = mFilterManager.getCurrentValue(AdjustFragment.AdjustType.SATURATION);
                openFragment(AdjustFragment.newInstance(b, c, s));
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
        mFilterManager.adjustImage(type, value);
    }

    @Override
    public void onAdjustApplied() {
        mFilterManager.saveCurrentState();
        closeFragment();
    }

    @Override
    public void onAdjustCancelled() {
        mFilterManager.restoreState();
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

        // BƯỚC A: Lấy ảnh nền đã có Filter (Màu sắc/Độ sáng)
        // Hàm capture() này lấy từ FilterEditorManager
        Bitmap filterBitmap = null;
        try {
            filterBitmap = mFilterManager.capture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (filterBitmap == null) {
            showLoading(prbEditor, false);
            showToast("Lỗi: Không thể lấy ảnh filter");
            return;
        }

        // BƯỚC B: Đưa ảnh Filter vào PhotoEditorView để chuẩn bị gộp với Text
        photoEdtView.getSource().setImageBitmap(filterBitmap);
        photoEdtView.getSource().setAlpha(1f); // Đảm bảo ảnh hiện rõ

        // BƯỚC C: Dùng PhotoEditor để "chụp" lại toàn bộ (Nền + Filter + Text)
        // Hàm getPhotoEditor() này ta vừa thêm vào TextEditorManager
        mTextManager.getPhotoEditor().saveAsBitmap(new OnSaveBitmap() {
            @Override
            public void onBitmapReady(Bitmap saveBitmap) {
                // Đã có ảnh gộp (saveBitmap) gồm cả Chữ và Filter
                // Bây giờ chạy luồng nền để lưu ra file tạm
                new Thread(() -> {
                    try {
                        // 1. Tạo file nguồn tạm thời (Input for Crop)
                        File tempSource = new File(getCacheDir(), "temp_crop_input_" + System.currentTimeMillis() + ".jpg");
                        FileOutputStream out = new FileOutputStream(tempSource);
                        saveBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        out.flush();
                        out.close();

                        // 2. Tạo file đích (Output from Crop)
                        File tempDest = new File(getCacheDir(), "temp_crop_output_" + System.currentTimeMillis() + ".jpg");

                        // 3. Tạo URI an toàn bằng FileProvider
                        // Lưu ý: "com.sevengroup.artifyme.fileprovider" phải khớp với trong AndroidManifest.xml
                        Uri sourceUri = FileProvider.getUriForFile(
                                BasicEditorActivity.this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                tempSource
                        );
                        Uri destUri = Uri.fromFile(tempDest);

                        // 4. Mở UCrop trên UI Thread
                        runOnUiThread(() -> {
                            UCrop.Options options = new UCrop.Options();
                            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
                            options.setFreeStyleCropEnabled(true); // Cho phép cắt tự do

                            UCrop.of(sourceUri, destUri)
                                    .withOptions(options)
                                    .start(BasicEditorActivity.this, cropResultLauncher);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            showLoading(prbEditor, false);
                            showToast("Lỗi chuẩn bị ảnh: " + e.getMessage());
                        });
                    }
                }).start();
            }


        });
    }

    private void applyCropResultToPhotoEditor(Bitmap croppedBitmap) {

        if (croppedBitmap == null) {
            showToast("Lỗi: Bitmap crop null");
            return;
        }

        // Xóa toàn bộ Text cũ
        mTextManager.clearAllViews();

        // Đẩy ảnh crop vào PhotoEditorView
        photoEdtView.getSource().setImageBitmap(croppedBitmap);
        photoEdtView.setVisibility(View.VISIBLE);

        // Tắt GPUImageView vì Filter lúc này đã reset để chờ lọc lại
        gpuImgView.setVisibility(View.INVISIBLE);

        // Đặt ảnh mới vào FilterEditorManager để tiếp tục chỉnh sau này
        mFilterManager.setImage(croppedBitmap);

        // Reset trạng thái bộ lọc để không cộng dồn filter của ảnh cũ
        mFilterManager.resetStateAfterDestructiveEdit();

        showToast("Đã áp dụng ảnh crop!");
    }

    private void openFragment(Fragment fragment) {
        frlPanelContainer.setVisibility(View.VISIBLE);
        rcvMainTools.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().replace(R.id.frlPanelContainer, fragment).commit();
    }

    private void closeFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frlPanelContainer);
        if (fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        frlPanelContainer.setVisibility(View.GONE);
        rcvMainTools.setVisibility(View.VISIBLE);
    }
}