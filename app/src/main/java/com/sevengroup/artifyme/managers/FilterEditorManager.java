package com.sevengroup.artifyme.managers;

import android.graphics.Bitmap;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;

public class FilterEditorManager {
    private final GPUImageView mGpuImageView;
    private final GPUImageFilterGroup mFilterGroup;
    private final AdjustEditorManager mAdjustManager;

    private GPUImageFilter mColorFilter;
    private int mCurrentFilterIndex = 0;
    private int mSavedFilterIndex = 0;
    private GPUImageFilter mSavedColorFilter;

    public FilterEditorManager(GPUImageView gpuImageView) {
        this.mGpuImageView = gpuImageView;

        // Create adjust manager
        mAdjustManager = new AdjustEditorManager(gpuImageView);

        mColorFilter = new GPUImageFilter();
        mFilterGroup = new GPUImageFilterGroup();
        updateFilterGroup();
        mGpuImageView.setFilter(mFilterGroup);
    }

    private void updateFilterGroup() {
        mFilterGroup.getFilters().clear();
        // Order: Color filter first, then all adjust filters
        mFilterGroup.addFilter(mColorFilter);
        // Add all adjust filters from AdjustEditorManager
        mFilterGroup.addFilter(mAdjustManager.getAdjustFilterGroup());
        mGpuImageView.setFilter(mFilterGroup);
    }

    public void setImage(Bitmap bitmap) {
        mGpuImageView.setImage(bitmap);
    }

    public void setFilterIndex(int index) {
        this.mCurrentFilterIndex = index;
    }

    public int getFilterIndex() {
        return mCurrentFilterIndex;
    }

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

    public Bitmap capture() throws InterruptedException {
        return mGpuImageView.capture();
    }

    public void resetAll() {
        mCurrentFilterIndex = 0;
        mColorFilter = new GPUImageFilter();
        mAdjustManager.reset();
        updateFilterGroup();
        mGpuImageView.requestRender();
    }

    // Expose adjust manager for BasicEditorActivity
    public AdjustEditorManager getAdjustManager() {
        return mAdjustManager;
    }
}