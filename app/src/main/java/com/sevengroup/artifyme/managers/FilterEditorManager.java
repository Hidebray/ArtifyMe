package com.sevengroup.artifyme.managers;

import android.graphics.Bitmap;
import com.sevengroup.artifyme.fragments.editor.AdjustFragment;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter;

public class FilterEditorManager {
    private final GPUImageView mGpuImageView;
    private final GPUImageFilterGroup mFilterGroup;
    private final GPUImageBrightnessFilter mBrightnessFilter;
    private final GPUImageContrastFilter mContrastFilter;
    private final GPUImageSaturationFilter mSaturationFilter;
    private GPUImageFilter mColorFilter;

    private float mBrightness = 0.0f;
    private float mContrast = 1.0f;
    private float mSaturation = 1.0f;
    private int mCurrentFilterIndex = 0;
    private int mSavedFilterIndex = 0;
    private float mSavedBrightness, mSavedContrast, mSavedSaturation;
    private GPUImageFilter mSavedColorFilter;

    public FilterEditorManager(GPUImageView gpuImageView) {
        this.mGpuImageView = gpuImageView;
        mBrightnessFilter = new GPUImageBrightnessFilter(0.0f);
        mContrastFilter = new GPUImageContrastFilter(1.0f);
        mSaturationFilter = new GPUImageSaturationFilter(1.0f);
        mColorFilter = new GPUImageFilter();
        mFilterGroup = new GPUImageFilterGroup();
        updateFilterGroup();
        mGpuImageView.setFilter(mFilterGroup);
    }

    private void updateFilterGroup() {
        mFilterGroup.getFilters().clear();
        mFilterGroup.addFilter(mColorFilter);
        mFilterGroup.addFilter(mBrightnessFilter);
        mFilterGroup.addFilter(mContrastFilter);
        mFilterGroup.addFilter(mSaturationFilter);
        mGpuImageView.setFilter(mFilterGroup);
    }

    public void setImage(Bitmap bitmap) { mGpuImageView.setImage(bitmap); }

    public void setFilterIndex(int index) {
        this.mCurrentFilterIndex = index;
    }

    public int getFilterIndex() {
        return mCurrentFilterIndex;
    }

    public void saveCurrentState() {
        mSavedBrightness = mBrightness;
        mSavedContrast = mContrast;
        mSavedSaturation = mSaturation;
        mSavedColorFilter = mColorFilter;
        mSavedFilterIndex = mCurrentFilterIndex;
    }

    public void restoreState() {
        mBrightness = mSavedBrightness;
        mContrast = mSavedContrast;
        mSaturation = mSavedSaturation;
        mColorFilter = mSavedColorFilter;
        mCurrentFilterIndex = mSavedFilterIndex;
        mBrightnessFilter.setBrightness(mBrightness);
        mContrastFilter.setContrast(mContrast);
        mSaturationFilter.setSaturation(mSaturation);
        updateFilterGroup();
        mGpuImageView.requestRender();
    }

    public void adjustImage(AdjustFragment.AdjustType type, float value) {
        switch (type) {
            case BRIGHTNESS:
                mBrightness = value;
                mBrightnessFilter.setBrightness(mBrightness);
                break;
            case CONTRAST:
                mContrast = value + 1.0f;
                mContrastFilter.setContrast(mContrast);
                break;
            case SATURATION:
                mSaturation = value + 1.0f;
                mSaturationFilter.setSaturation(mSaturation);
                break;
        }
        mGpuImageView.requestRender();
    }

    public void applyFilter(GPUImageFilter filter) {
        this.mColorFilter = filter;
        updateFilterGroup();
        mGpuImageView.requestRender();
    }

    public float getCurrentValue(AdjustFragment.AdjustType type) {
        switch (type) {
            case BRIGHTNESS: return mBrightness;
            case CONTRAST: return mContrast - 1.0f;
            case SATURATION: return mSaturation - 1.0f;
            default: return 0.0f;
        }
    }

    public Bitmap capture() throws InterruptedException { return mGpuImageView.capture(); }

    public void resetAll() {
        mBrightness = 0.0f;
        mContrast = 1.0f;
        mSaturation = 1.0f;
        mCurrentFilterIndex = 0;
        mColorFilter = new jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter(); // Filter rỗng

        mBrightnessFilter.setBrightness(mBrightness);
        mContrastFilter.setContrast(mContrast);
        mSaturationFilter.setSaturation(mSaturation);

        updateFilterGroup();
        mGpuImageView.requestRender();
    }
}