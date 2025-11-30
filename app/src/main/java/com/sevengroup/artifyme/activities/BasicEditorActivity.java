package com.sevengroup.artifyme.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.EditorToolsAdapter;
import com.sevengroup.artifyme.adapters.FilterAdapter;
import com.sevengroup.artifyme.fragments.editor.AdjustFragment;
import com.sevengroup.artifyme.fragments.editor.FilterFragment;
import com.sevengroup.artifyme.fragments.editor.TextFragment;
import com.sevengroup.artifyme.managers.AdjustEditorManager;
import com.sevengroup.artifyme.managers.FilterEditorManager;
import com.sevengroup.artifyme.managers.HistoryManager;
import com.sevengroup.artifyme.managers.TextEditorManager;
import com.sevengroup.artifyme.models.EditorState;
import com.sevengroup.artifyme.utils.AppConstants;
import com.sevengroup.artifyme.utils.AppExecutors;
import com.sevengroup.artifyme.utils.FilterGenerator;
import com.sevengroup.artifyme.viewmodels.BasicEditorViewModel;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ja.burhanrashid52.photoeditor.PhotoEditorView;
import jp.co.cyberagent.android.gpuimage.GPUImage;
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
    private RecyclerView rcvTools;
    private FrameLayout frameSubTool;

    private ImageButton btnClose, btnUndo, btnRedo;
    private TextView btnSave;

    private BasicEditorViewModel viewModel;
    private EditorToolsAdapter toolsAdapter;
    private TextEditorManager mTextManager;
    private FilterEditorManager mFilterManager;
    private AdjustEditorManager mAdjustManager;

    private final HistoryManager mHistoryManager = new HistoryManager();
    private EditorState mStateBeforeEdit;

    // Undo logic
    private Bitmap mBitmapBeforeDestructiveAction;
    private EditorState mStateBeforeDestructiveAction;

    // Cờ đánh dấu ảnh đã bị cắt/nướng
    private boolean isImageCroppedAndBaked = false;

    private long currentProjectId;
    private String latestImagePath;
    private Bitmap mainBitmap;
    private final List<String> toolList = Arrays.asList("Crop", "Adjust", "Filter", "Text");

    private File lastTempSourceFile = null;
    private File lastTempDestFile = null;

    private OnBackPressedCallback onBackPressedCallback;

    private final ActivityResultLauncher<Intent> cropResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                try {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        final Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) handleCropResult(resultUri);
                        else showLoading(prbEditor, false);
                    } else {
                        deleteTempFilesIfAny();
                        showLoading(prbEditor, false);
                        if (result.getResultCode() == UCrop.RESULT_ERROR) {
                            final Throwable cropError = UCrop.getError(result.getData());
                            showToast("Lỗi cắt ảnh: " + (cropError != null ? cropError.getMessage() : "Unknown"));
                        }
                    }
                } catch (Exception e) {
                    showLoading(prbEditor, false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_editor);

        if (!loadIntentData()) return;

        initViews();
        setupBackPressHandling();

        mTextManager = new TextEditorManager(this, photoEdtView);
        mFilterManager = new FilterEditorManager(gpuImgView);
        mAdjustManager = mFilterManager.getAdjustManager();

        setupViewModel();
        setupToolsRecyclerView();

        viewModel.loadEditableBitmap(latestImagePath);
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        btnSave = findViewById(R.id.btnSave);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        prbEditor = findViewById(R.id.prbEditor);
        gpuImgView = findViewById(R.id.gpuImgView);
        photoEdtView = findViewById(R.id.photoEdtView);
        rcvTools = findViewById(R.id.rcvTools);
        frameSubTool = findViewById(R.id.frameSubTool);

        btnClose.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveChanges());

        btnUndo.setOnClickListener(v -> {
            if (mHistoryManager.canUndo()) {
                EditorState current = captureCurrentState();
                EditorState prevState = mHistoryManager.undo(current);
                if (prevState != null) applyState(prevState);
                updateUndoRedoUI();
            } else if (mBitmapBeforeDestructiveAction != null) {
                restoreStateBeforeDestructiveAction();
            }
        });

        btnRedo.setOnClickListener(v -> {
            if (mHistoryManager.canRedo()) {
                EditorState current = captureCurrentState();
                EditorState nextState = mHistoryManager.redo(current);
                if (nextState != null) applyState(nextState);
                updateUndoRedoUI();
            }
        });

        updateUndoRedoUI();
    }

    private void setupBackPressHandling() {
        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                closeFragment();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private boolean loadIntentData() {
        if (getIntent() != null) {
            currentProjectId = getIntent().getLongExtra(AppConstants.KEY_PROJECT_ID, -1L);
            latestImagePath = getIntent().getStringExtra(AppConstants.KEY_IMAGE_PATH);
        }
        if (currentProjectId == -1 || latestImagePath == null) {
            showToast("Lỗi: Không thể tải ảnh");
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
                photoEdtView.getSource().setScaleType(ImageView.ScaleType.FIT_CENTER);
                gpuImgView.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);

                mFilterManager.resetAll();
                mFilterManager.setImage(mainBitmap);
                photoEdtView.getSource().setImageBitmap(mainBitmap);

                setGpuMode(true);
                rcvTools.setVisibility(View.VISIBLE);
                mHistoryManager.clear();
                updateUndoRedoUI();
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
        rcvTools.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rcvTools.setAdapter(toolsAdapter);
    }

    // UPDATE: Hàm Save đã dùng render ngầm để fix viền đen
    private void saveChanges() {
        showLoading(prbEditor, true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                Bitmap processedBitmap = mFilterManager.getBitmapWithFiltersApplied(this, mainBitmap);
                if (processedBitmap == null) processedBitmap = mainBitmap;

                Bitmap finalBmp = processedBitmap;
                runOnUiThread(() -> {
                    photoEdtView.getSource().setImageBitmap(finalBmp);
                    photoEdtView.getSource().setAlpha(1f);
                    mTextManager.saveImage(savedBitmap -> viewModel.saveEditedImage(currentProjectId, savedBitmap));
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showLoading(prbEditor, false));
            }
        });
    }

    // UPDATE: Logic chọn tool xử lý ảnh crop
    @Override
    public void onToolSelected(String toolName) {
        mStateBeforeEdit = captureCurrentState();

        // Nếu ảnh đã Crop và người dùng muốn chỉnh màu tiếp -> Nạp lại vào GPU
        if (isImageCroppedAndBaked && (toolName.equals("Adjust") || toolName.equals("Filter"))) {
            mFilterManager.setImage(mainBitmap);
            gpuImgView.setVisibility(View.VISIBLE);
            photoEdtView.getSource().setImageBitmap(null); // Làm rỗng lớp trên
            isImageCroppedAndBaked = false;
        }

        switch (toolName) {
            case "Crop": startCrop(); break;
            case "Adjust":
                setGpuMode(true);
                photoEdtView.setVisibility(View.VISIBLE);
                openFragment(AdjustFragment.newInstance(mAdjustManager.getAllSettings()));
                break;
            case "Filter":
                setGpuMode(true);
                photoEdtView.setVisibility(View.VISIBLE);
                int currentIndex = mFilterManager.getFilterIndex();
                FilterFragment filterFragment = FilterFragment.newInstance(currentIndex);
                // Dùng mainBitmap để tạo thumbnail
                new Thread(() -> {
                    Bitmap thumb = android.media.ThumbnailUtils.extractThumbnail(mainBitmap, 150, 150);
                    runOnUiThread(() -> filterFragment.setPreviewBitmap(thumb));
                }).start();
                openFragment(filterFragment);
                break;
            case "Text":
                setGpuMode(true);
                photoEdtView.setVisibility(View.VISIBLE);
                openFragment(TextFragment.newInstance());
                break;
        }
    }

    private void setGpuMode(boolean enable) {
        if (enable) {
            photoEdtView.getSource().setAlpha(0f);
            gpuImgView.setVisibility(View.VISIBLE);
        } else {
            photoEdtView.getSource().setAlpha(1f);
            gpuImgView.setVisibility(View.GONE);
        }
    }

    private void restoreStateBeforeDestructiveAction() {
        if (mBitmapBeforeDestructiveAction == null) return;
        safeRecycleBitmap(mainBitmap);
        this.mainBitmap = mBitmapBeforeDestructiveAction;
        photoEdtView.getSource().setImageBitmap(mainBitmap);
        mFilterManager.setImage(mainBitmap);

        if (mStateBeforeDestructiveAction != null) applyState(mStateBeforeDestructiveAction);

        mBitmapBeforeDestructiveAction = null;
        mStateBeforeDestructiveAction = null;
        mTextManager.clearAllViews();
        isImageCroppedAndBaked = false;

        setGpuMode(true);
        updateUndoRedoUI();
        showToast("Đã khôi phục ảnh gốc.");
    }

    private void saveSnapshotToHistory() {
        if (mStateBeforeEdit != null) {
            mHistoryManager.saveState(mStateBeforeEdit);
            updateUndoRedoUI();
        }
    }

    private void updateUndoRedoUI() {
        if (mHistoryManager == null) return;
        boolean canUndo = mHistoryManager.canUndo() || mBitmapBeforeDestructiveAction != null;
        btnUndo.setAlpha(canUndo ? 1.0f : 0.3f);
        btnUndo.setEnabled(canUndo);
        boolean canRedo = mHistoryManager.canRedo();
        btnRedo.setAlpha(canRedo ? 1.0f : 0.3f);
        btnRedo.setEnabled(canRedo);
    }

    private EditorState captureCurrentState() {
        int idx = mFilterManager.getFilterIndex();
        Map<AdjustFragment.AdjustType, Float> adj = mAdjustManager.getAllSettings();
        return new EditorState(idx, adj);
    }

    // UPDATE: Hàm applyState cập nhật cả UI
    private void applyState(EditorState state) {
        mAdjustManager.applySettings(state.adjustValues);
        List<FilterAdapter.FilterModel> filters = FilterGenerator.getFilters();
        if (state.filterIndex >= 0 && state.filterIndex < filters.size()) {
            mFilterManager.setFilterIndex(state.filterIndex);
            mFilterManager.applyFilter(filters.get(state.filterIndex).filter);
        }

        // Refresh UI
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frameSubTool);
        if (fragment instanceof AdjustFragment && fragment.isVisible()) {
            ((AdjustFragment) fragment).refreshValues(state.adjustValues);
        }

        // Chốt saved state
        mFilterManager.saveCurrentState();
    }

    @Override public void onAdjustmentChanged(AdjustFragment.AdjustType type, float value) {
        mAdjustManager.adjust(type, value);
    }
    @Override public void onAdjustApplied() {
        saveSnapshotToHistory();
        mAdjustManager.saveCurrentState();
        closeFragment();
    }
    @Override public void onAdjustCancelled() {
        mAdjustManager.restoreState();
        closeFragment();
    }
    @Override public void onTextApplied(String text, int colorCode) {
        mTextManager.addText(text, colorCode);
        closeFragment();
    }
    @Override public void onTextCancelled() { closeFragment(); }
    @Override public void onFilterSelected(GPUImageFilter filter, int index) {
        mFilterManager.setFilterIndex(index);
        mFilterManager.applyFilter(filter);
    }
    @Override public void onFilterApplied() {
        saveSnapshotToHistory();
        mFilterManager.saveCurrentState();
        closeFragment();
    }
    @Override public void onFilterCancelled() {
        mFilterManager.restoreState();
        closeFragment();
    }

    private void startCrop() {
        showLoading(prbEditor, true);
        new Thread(() -> {
            try {
                Bitmap sourceBitmap = mFilterManager.getBitmapWithFiltersApplied(getApplicationContext(), mainBitmap);
                if (sourceBitmap == null) sourceBitmap = mainBitmap;

                Bitmap finalBmp = sourceBitmap;
                runOnUiThread(() -> {
                    photoEdtView.getSource().setImageBitmap(finalBmp);
                    photoEdtView.getSource().setAlpha(1f);

                    mTextManager.getPhotoEditor().saveAsBitmap(saveBitmap -> {
                        // --- XÓA ĐOẠN CODE NÀY ---
                        // if (finalBmp != null && finalBmp != mainBitmap && !finalBmp.isRecycled()) {
                        //     finalBmp.recycle();
                        // }
                        // --------------------------
                        // Lý do: finalBmp đang được photoEdtView hiển thị.
                        // Không được recycle nó lúc này.
                        // Nó sẽ tự được dọn dẹp khi bạn set ảnh mới sau khi crop xong.

                        AppExecutors.getInstance().diskIO().execute(() -> {
                            try {
                                File tempSource = new File(getCacheDir(), "temp_crop_input_" + System.currentTimeMillis() + ".png");
                                FileOutputStream out = new FileOutputStream(tempSource);
                                saveBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                out.flush(); out.close();

                                File tempDest = new File(getCacheDir(), "temp_crop_output_" + System.currentTimeMillis() + ".png");
                                lastTempSourceFile = tempSource;
                                lastTempDestFile = tempDest;

                                Uri sourceUri = FileProvider.getUriForFile(BasicEditorActivity.this, getApplicationContext().getPackageName() + ".fileprovider", tempSource);
                                Uri destUri = Uri.fromFile(tempDest);

                                UCrop.Options options = new UCrop.Options();
                                options.setCompressionFormat(Bitmap.CompressFormat.PNG);
                                options.setFreeStyleCropEnabled(true);

                                runOnUiThread(() -> {
                                    Intent uIntent = UCrop.of(sourceUri, destUri)
                                            .withOptions(options)
                                            .getIntent(BasicEditorActivity.this);
                                    uIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    cropResultLauncher.launch(uIntent);
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(() -> showLoading(prbEditor, false));
                            }
                        });
                    });
                });
            } catch (Exception e) {
                runOnUiThread(() -> showLoading(prbEditor, false));
            }
        }).start();
    }

    private void handleCropResult(Uri resultUri) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                Bitmap croppedBitmap = loadBitmapFromUri(resultUri);
                runOnUiThread(() -> {
                    if (croppedBitmap != null) applyCropResultToPhotoEditor(croppedBitmap);
                    else showToast("Lỗi đọc ảnh crop");
                    deleteTempFilesIfAny();
                });
            } catch (Exception e) {
                runOnUiThread(() -> showToast("Lỗi xử lý ảnh crop"));
            } finally {
                runOnUiThread(() -> showLoading(prbEditor, false));
            }
        });
    }

    private void applyCropResultToPhotoEditor(Bitmap croppedBitmap) {
        if (croppedBitmap == null) return;
        mBitmapBeforeDestructiveAction = mainBitmap;
        mStateBeforeDestructiveAction = captureCurrentState();

        mTextManager.clearAllViews();

        this.mainBitmap = croppedBitmap;
        photoEdtView.getSource().setImageBitmap(mainBitmap);
        mFilterManager.setImage(mainBitmap);
        mFilterManager.resetStateAfterDestructiveEdit();

        isImageCroppedAndBaked = true;

        setGpuMode(true);
        mHistoryManager.clear();
        updateUndoRedoUI();
        showToast("Đã cắt ảnh!");
    }

    private void openFragment(Fragment fragment) {
        frameSubTool.setVisibility(View.VISIBLE);
        rcvTools.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().replace(R.id.frameSubTool, fragment).commit();
        if (onBackPressedCallback != null) onBackPressedCallback.setEnabled(true);
    }

    private void closeFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frameSubTool);
        if (fragment != null) getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        frameSubTool.setVisibility(View.GONE);
        rcvTools.setVisibility(View.VISIBLE);
        if (onBackPressedCallback != null) onBackPressedCallback.setEnabled(false);
    }

    private Bitmap loadBitmapFromUri(Uri uri) throws Exception {
        if (uri == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source src = ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(src, (decoder, info, source) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                decoder.setMutableRequired(true);
            });
        } else {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                return bmp.copy(Bitmap.Config.ARGB_8888, true);
            }
        }
    }

    private void safeRecycleBitmap(Bitmap bmp) {
        if (bmp != null && !bmp.isRecycled()) {
            try { bmp.recycle(); } catch (Exception ignored) {}
        }
    }

    private void deleteTempFilesIfAny() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                if (lastTempSourceFile != null && lastTempSourceFile.exists()) lastTempSourceFile.delete();
                if (lastTempDestFile != null && lastTempDestFile.exists()) lastTempDestFile.delete();
                File cacheDir = getCacheDir();
                File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("temp_crop_"));
                if (files != null) {
                    for (File f : files) f.delete();
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        safeRecycleBitmap(mainBitmap);
        safeRecycleBitmap(mBitmapBeforeDestructiveAction);
        deleteTempFilesIfAny();
    }
}