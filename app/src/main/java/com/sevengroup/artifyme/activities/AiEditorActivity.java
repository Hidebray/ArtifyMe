package com.sevengroup.artifyme.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.adapters.AiToolsAdapter;
import com.sevengroup.artifyme.managers.BackgroundEditorManager;
import com.sevengroup.artifyme.managers.CartoonManager;
import com.sevengroup.artifyme.models.AiToolType;
import com.sevengroup.artifyme.repositories.ProjectRepository;
import com.sevengroup.artifyme.utils.AppConstants;
import com.sevengroup.artifyme.utils.AppExecutors;
import com.sevengroup.artifyme.utils.BitmapUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
public class AiEditorActivity extends BaseActivity implements
        AiToolsAdapter.OnAiToolClickListener {
    private ImageButton btnClose;
    private ImageView imgOriginal;
    private ImageView imgAiResult;
    private FrameLayout layoutLoadingOverlay;
    private TextView txtProcessingStatus;
    private RecyclerView rcvAiTools;
    private ConstraintLayout layoutPreview;
    private MaterialButton btnCancelPreview, btnSaveResult;

    // Data
    private long currentProjectId;
    private String latestImagePath;
    private Bitmap originalBitmap;
    private Bitmap aiResultBitmap;
    private BackgroundEditorManager mBackgroundManager;
    private CartoonManager mCartoonManager;
    private final List<AiToolType> aiToolList = Arrays.asList(AiToolType.values());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_editor);

        if (!loadIntentData()) return;

        initViews();
        setupRecyclerView();
        loadImage();

        mBackgroundManager = new BackgroundEditorManager(this);
        mCartoonManager = new CartoonManager(this);
    }

    private boolean loadIntentData() {
        if (getIntent() != null) {
            currentProjectId = getIntent().getLongExtra(AppConstants.KEY_PROJECT_ID, -1L);
            latestImagePath = getIntent().getStringExtra(AppConstants.KEY_IMAGE_PATH);
        }
        if (currentProjectId == -1 || latestImagePath == null) {
            showToast(getString(R.string.msg_error_load_project));
            finish();
            return false;
        }
        return true;
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        imgOriginal = findViewById(R.id.imgOriginal);
        imgAiResult = findViewById(R.id.imgAiResult);
        txtProcessingStatus = findViewById(R.id.txtProcessingStatus);
        layoutLoadingOverlay = findViewById(R.id.layoutLoadingOverlay);
        rcvAiTools = findViewById(R.id.rcvAiTools);
        layoutPreview = findViewById(R.id.layoutPreview);
        btnCancelPreview = findViewById(R.id.btnCancelPreview);
        btnSaveResult = findViewById(R.id.btnSaveResult);

        btnClose.setOnClickListener(v -> finish());
        btnCancelPreview.setOnClickListener(v -> hidePreview());
        btnSaveResult.setOnClickListener(v -> saveAiResult());
    }

    private void setupRecyclerView() {
        AiToolsAdapter adapter = new AiToolsAdapter(aiToolList, this);
        rcvAiTools.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        rcvAiTools.setAdapter(adapter);
    }

    private void loadImage() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            originalBitmap = BitmapUtils.loadSafeBitmap(latestImagePath);
            runOnUiThread(() -> {
                if (originalBitmap != null) {
                    Glide.with(this)
                            .load(new File(latestImagePath))
                            .into(imgOriginal);
                } else {
                    showToast(getString(R.string.msg_ai_no_image));
                    finish();
                }
            });
        });
    }

    @Override
    public void onAiToolSelected(AiToolType toolType) {
        switch (toolType) {
            case AI_BACKGROUND:
                showAiBackgroundDialog();
                break;
            case AI_CARTOONIZE:
                showAiCartoonDialog();
                break;
            default:
                break;
        }
    }

    private void showAiBackgroundDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_ai_prompt, null);
        EditText edtPrompt = dialogView.findViewById(R.id.edtPrompt);

        new AlertDialog.Builder(this)
                .setTitle(R.string.ai_bg_dialog_title)
                .setView(dialogView)
                .setMessage(" Lưu ý: Nếu ảnh có quá nhiều hiệu ứng (swirl, sketch, v.v.), " +
                        "AI có thể không xóa nền được. Hãy thử với ảnh gốc để có kết quả tốt nhất.")
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String prompt = edtPrompt.getText().toString().trim();
                    if (!TextUtils.isEmpty(prompt)) {
                        processAiBackground(prompt);
                    } else {
                        showToast(getString(R.string.msg_enter_prompt));
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void processAiBackground(String prompt) {
        if (originalBitmap == null) {
            showToast(getString(R.string.msg_ai_no_image));
            return;
        }

        // Show loading
        showLoading(true, getString(R.string.ai_bg_processing));

        mBackgroundManager.changeBackground(
                originalBitmap,
                prompt,
                new BackgroundEditorManager.OnAiResultListener() {
                    @Override
                    public void onSuccess(Bitmap result) {
                        if (isFinishing() || isDestroyed()) return;
                        aiResultBitmap = result;
                        showLoading(false, null);
                        showPreview(result);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        showLoading(false, null);
                        showToast(String.format(getString(R.string.msg_ai_processing_error), message));
                    }

                    @Override
                    public void onProgress(String status) {
                        if (isFinishing() || isDestroyed()) return;
                        runOnUiThread(() -> {
                            if (txtProcessingStatus != null) {
                                txtProcessingStatus.setText(status);
                            }
                        });
                    }
                }
        );
    }

    // --- CODE MỚI CHO CARTOON ---

    private void showAiCartoonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_prompt, null);
        EditText edtPrompt = dialogView.findViewById(R.id.edtPrompt);

        // Đổi hint để người dùng biết nên nhập gì
        edtPrompt.setHint("VD: Chàng trai đeo kính, áo vest xanh...");

        new AlertDialog.Builder(this)
                .setTitle("Tạo nhân vật Hoạt hình")
                .setView(dialogView)
                .setMessage("Nhập mô tả chi tiết về nhân vật bạn muốn vẽ. AI sẽ tạo ra một bức tranh 3D Disney/Pixar cực đẹp dựa trên mô tả đó.")
                .setPositiveButton("Vẽ ngay", (dialog, which) -> {
                    String prompt = edtPrompt.getText().toString().trim();
                    if (!TextUtils.isEmpty(prompt)) {
                        processAiCartoon(prompt);
                    } else {
                        showToast(getString(R.string.msg_enter_prompt));
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void processAiCartoon(String prompt) {
        // Show loading
        showLoading(true, "Đang vẽ tranh hoạt hình...");

        // Lấy kích thước ảnh gốc để tạo ảnh mới cùng tỉ lệ
        int w = 1024; // Mặc định hoặc lấy imgOriginal.getWidth();
        int h = 1024;
        if (originalBitmap != null) {
            w = originalBitmap.getWidth();
            h = originalBitmap.getHeight();
        }

        mCartoonManager.generateCartoon(prompt, w, h, new CartoonManager.OnCartoonResultListener() {
            @Override
            public void onSuccess(Bitmap result) {
                if (isFinishing() || isDestroyed()) return;
                aiResultBitmap = result; // Lưu kết quả vào biến chung để nút Save hoạt động
                showLoading(false, null);
                showPreview(result); // Hiển thị lên màn hình
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                showLoading(false, null);
                showToast("Lỗi: " + message);
            }

            @Override
            public void onProgress(String status) {
                runOnUiThread(() -> {
                    if (txtProcessingStatus != null) {
                        txtProcessingStatus.setText(status);
                    }
                });
            }
        });
    }

    private void showPreview(Bitmap result) {
        imgAiResult.setImageBitmap(result);
        layoutPreview.setVisibility(View.VISIBLE);
        rcvAiTools.setVisibility(View.GONE);
    }

    private void hidePreview() {
        layoutPreview.setVisibility(View.GONE);
        rcvAiTools.setVisibility(View.VISIBLE);

        // Clean up bitmap
        if (aiResultBitmap != null && !aiResultBitmap.isRecycled()) {
            aiResultBitmap.recycle();
            aiResultBitmap = null;
        }
    }

    private void saveAiResult() {
        if (aiResultBitmap == null) {
            showToast(getString(R.string.msg_ai_save_failed));
            return;
        }

        showLoading(true, getString(R.string.msg_ai_applying));

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // Save bitmap to storage
                String savedPath = BitmapUtils.saveBitmapToAppStorage(this, aiResultBitmap);

                if (savedPath != null) {
                    // Add to database as new version
                    ProjectRepository repo = ProjectRepository.getInstance(getApplication());
                    repo.saveNewVersion(
                            currentProjectId,
                            savedPath,
                            success -> runOnUiThread(() -> {
                                showLoading(false, null);
                                if (success != null && success) {
                                    showToast(getString(R.string.msg_ai_success));
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    showToast(getString(R.string.msg_ai_save_failed));
                                }
                            })
                    );
                } else {
                    runOnUiThread(() -> {
                        showLoading(false, null);
                        showToast(getString(R.string.msg_ai_save_failed));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showLoading(false, null);
                    showToast("Error: " + e.getMessage());
                });
            }
        });
    }

    private void showLoading(boolean show, String message) {
        if (layoutLoadingOverlay != null) {
            layoutLoadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (txtProcessingStatus != null && message != null) {
            txtProcessingStatus.setText(message);
        }

        // Disable interactions during processing
        if (rcvAiTools != null) {
            rcvAiTools.setEnabled(!show);
        }
        if (btnClose != null) {
            btnClose.setEnabled(!show);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundManager != null) {
            mBackgroundManager.release();
        }
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (aiResultBitmap != null && !aiResultBitmap.isRecycled()) {
            aiResultBitmap.recycle();
        }
    }
}
