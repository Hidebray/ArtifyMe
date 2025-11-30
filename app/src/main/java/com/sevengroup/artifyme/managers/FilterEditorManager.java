package com.sevengroup.artifyme.managers;

import android.content.Context;
import android.graphics.Bitmap;
import com.sevengroup.artifyme.adapters.FilterAdapter;
import com.sevengroup.artifyme.utils.FilterGenerator;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;

public class FilterEditorManager {
    private final GPUImageView mGpuImageView;
    private final AdjustEditorManager mAdjustManager;

    private GPUImageFilter mColorFilter;
    private int mCurrentFilterIndex = 0;
    private int mSavedFilterIndex = 0;
    private GPUImageFilter mSavedColorFilter;

    public FilterEditorManager(GPUImageView gpuImageView) {
        this.mGpuImageView = gpuImageView;
        mAdjustManager = new AdjustEditorManager(gpuImageView);
        mColorFilter = new GPUImageFilter();

        updateFilterGroup();
    }

    private void updateFilterGroup() {
        // Tạo Group mới để tránh lỗi Threading
        GPUImageFilterGroup newFilterGroup = new GPUImageFilterGroup();
        newFilterGroup.addFilter(mColorFilter);
        newFilterGroup.addFilter(mAdjustManager.getAdjustFilterGroup());
        mGpuImageView.setFilter(newFilterGroup);
    }

    public void setImage(Bitmap bitmap) { mGpuImageView.setImage(bitmap); }
    public void setFilterIndex(int index) { this.mCurrentFilterIndex = index; }
    public int getFilterIndex() { return mCurrentFilterIndex; }

    public void saveCurrentState() {
        mSavedColorFilter = mColorFilter;
        mSavedFilterIndex = mCurrentFilterIndex;
        mAdjustManager.saveCurrentState();
    }

    public void restoreState() {
        mColorFilter = mSavedColorFilter;
        mCurrentFilterIndex = mSavedFilterIndex;
        mAdjustManager.restoreState();
        updateFilterGroup();
    }

    public void applyFilter(GPUImageFilter filter) {
        this.mColorFilter = filter;
        updateFilterGroup();
        mGpuImageView.requestRender();
    }

    public Bitmap capture() throws InterruptedException { return mGpuImageView.capture(); }

    public void resetAll() {
        mCurrentFilterIndex = 0;
        mColorFilter = new GPUImageFilter();
        mAdjustManager.reset();
        updateFilterGroup();
        mGpuImageView.requestRender();
    }

    public void resetStateAfterDestructiveEdit() {
        mCurrentFilterIndex = 0;
        mSavedFilterIndex = 0;
        mColorFilter = new GPUImageFilter();
        mAdjustManager.reset();
        updateFilterGroup();
    }

    public AdjustEditorManager getAdjustManager() {
        return mAdjustManager;
    }

    public Bitmap getBitmapWithFiltersApplied(Context context, Bitmap sourceBitmap) {
        if (sourceBitmap == null) return null;
        try {
            // 1. Tạo AdjustManager ảo (null view)
            AdjustEditorManager tempAdjustManager = new AdjustEditorManager(null);
            tempAdjustManager.applySettings(mAdjustManager.getAllSettings());

            // 2. Lấy Filter màu hiện tại
            GPUImageFilter tempColorFilter = new GPUImageFilter();
            List<FilterAdapter.FilterModel> filters = FilterGenerator.getFilters();
            if (mCurrentFilterIndex >= 0 && mCurrentFilterIndex < filters.size()) {
                tempColorFilter = filters.get(mCurrentFilterIndex).filter;
            }

            // 3. Gom nhóm
            GPUImageFilterGroup exportGroup = new GPUImageFilterGroup();
            exportGroup.addFilter(tempColorFilter);
            exportGroup.addFilter(tempAdjustManager.getAdjustFilterGroup());

            // 4. Render ngầm
            GPUImage renderer = new GPUImage(context);
            renderer.setImage(sourceBitmap);
            renderer.setFilter(exportGroup);

            return renderer.getBitmapWithFilterApplied();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            System.gc();
            return sourceBitmap; // Fallback nếu tràn bộ nhớ
        }
    }
}