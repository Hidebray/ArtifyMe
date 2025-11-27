package com.sevengroup.artifyme.managers;

import android.graphics.PointF;
import android.opengl.GLES20;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter;
import com.sevengroup.artifyme.fragments.editor.AdjustFragment;

public class AdjustEditorManager {

    private final GPUImageView mGpuImageView;
    private final GPUImageFilterGroup mAdjustGroup;

    private final GPUImageBrightnessFilter mBrightnessFilter;
    private final GPUImageContrastFilter mContrastFilter;
    private final GPUImageSaturationFilter mSaturationFilter;
    private final GPUImageWhiteBalanceFilter mWarmthFilter;
    private final GPUImageVignetteFilter mVignetteFilter;
    private final GPUImageRGBFilter mTintFilter;
    private final GrainFilter mGrainFilter;

    // Current values
    private float mBrightness = 0.0f;
    private float mContrast = 1.0f;
    private float mSaturation = 1.0f;
    private float mWarmth = 0.0f;
    private float mVignette = -1.0f;
    private float mTint = 0.0f;
    private float mGrain = -1.0f;

    // Saved state for cancel functionality
    private float mSavedBrightness = 0.0f;
    private float mSavedContrast = 1.0f;
    private float mSavedSaturation = 1.0f;
    private float mSavedWarmth = 0.0f;
    private float mSavedVignette = -1.0f;
    private float mSavedTint = 0.0f;
    private float mSavedGrain = -1.0f;

    public AdjustEditorManager(GPUImageView gpuImageView) {
        this.mGpuImageView = gpuImageView;

        mBrightnessFilter = new GPUImageBrightnessFilter(0.0f);
        mContrastFilter = new GPUImageContrastFilter(1.0f);
        mSaturationFilter = new GPUImageSaturationFilter(1.0f);
        mWarmthFilter = new GPUImageWhiteBalanceFilter(5000f, 0f);

        mVignetteFilter = new GPUImageVignetteFilter();
        mVignetteFilter.setVignetteCenter(new PointF(0.5f, 0.5f));
        mVignetteFilter.setVignetteStart(0.7f);
        mVignetteFilter.setVignetteEnd(1.3f);

        mTintFilter = new GPUImageRGBFilter(1.0f, 1.0f, 1.0f);

        mGrainFilter = new GrainFilter();
        mGrainFilter.setGrainIntensity(0.0f);

        mAdjustGroup = new GPUImageFilterGroup();
        mAdjustGroup.addFilter(mBrightnessFilter);
        mAdjustGroup.addFilter(mContrastFilter);
        mAdjustGroup.addFilter(mSaturationFilter);
        mAdjustGroup.addFilter(mWarmthFilter);
        mAdjustGroup.addFilter(mTintFilter);
        mAdjustGroup.addFilter(mVignetteFilter);
        mAdjustGroup.addFilter(mGrainFilter);

        // Save initial state
        saveCurrentState();
    }

    public GPUImageFilterGroup getAdjustFilterGroup() {
        return mAdjustGroup;
    }

    public void adjust(AdjustFragment.AdjustType type, float value) {
        switch (type) {
            case BRIGHTNESS:
                mBrightness = value;
                mBrightnessFilter.setBrightness(value);
                break;

            case CONTRAST:
                mContrast = 1.0f + value;
                mContrastFilter.setContrast(mContrast);
                break;

            case SATURATION:
                mSaturation = 1.0f + value;
                mSaturationFilter.setSaturation(mSaturation);
                break;

            case WARMTH:
                mWarmth = value;
                float temperature = 5000f + (mWarmth * 2000f);
                mWarmthFilter.setTemperature(temperature);
                break;

            case VIGNETTE:
                mVignette = value;
                float strength = (value + 1f) / 2f;
                float start = 0.7f - strength * 0.4f;
                float end = 1.3f - strength * 0.5f;
                mVignetteFilter.setVignetteCenter(new PointF(0.5f, 0.5f));
                mVignetteFilter.setVignetteStart(start);
                mVignetteFilter.setVignetteEnd(end);
                break;

            case TINT:
                mTint = value;
                // Map -1.0 (green) to 0.0 (neutral) to 1.0 (magenta/purple)
                float tintAmount = mTint * 0.3f;
                mTintFilter.setRed(1.0f + tintAmount);
                mTintFilter.setGreen(1.0f - tintAmount);
                mTintFilter.setBlue(1.0f + tintAmount);
                break;

            case GRAIN:
                mGrain = value;
                // Map -1.0 to 1.0 → grain intensity (0.0 to 0.3)
                float grainIntensity = Math.max(0f, (mGrain + 1.0f) / 2.0f) * 0.3f;
                mGrainFilter.setGrainIntensity(grainIntensity);
                break;
        }

        mGpuImageView.requestRender();
    }

    public float getCurrentValue(AdjustFragment.AdjustType type) {
        switch (type) {
            case BRIGHTNESS:
                return mBrightness;
            case CONTRAST:
                return mContrast - 1.0f;
            case SATURATION:
                return mSaturation - 1.0f;
            case WARMTH:
                return mWarmth;
            case VIGNETTE:
                return mVignette;
            case TINT:
                return mTint;
            case GRAIN:
                return mGrain;
            default:
                return 0.0f;
        }
    }

    public void saveCurrentState() {
        mSavedBrightness = mBrightness;
        mSavedContrast = mContrast;
        mSavedSaturation = mSaturation;
        mSavedWarmth = mWarmth;
        mSavedVignette = mVignette;
        mSavedTint = mTint;
        mSavedGrain = mGrain;
    }

    public void restoreState() {
        mBrightness = mSavedBrightness;
        mContrast = mSavedContrast;
        mSaturation = mSavedSaturation;
        mWarmth = mSavedWarmth;
        mVignette = mSavedVignette;
        mTint = mSavedTint;
        mGrain = mSavedGrain;

        mBrightnessFilter.setBrightness(mBrightness);
        mContrastFilter.setContrast(mContrast);
        mSaturationFilter.setSaturation(mSaturation);

        float temperature = 5000f + (mWarmth * 2000f);
        mWarmthFilter.setTemperature(temperature);

        float strength = (mVignette + 1f) / 2f;
        float start = 0.7f - strength * 0.4f;
        float end = 1.3f - strength * 0.5f;
        mVignetteFilter.setVignetteCenter(new PointF(0.5f, 0.5f));
        mVignetteFilter.setVignetteStart(start);
        mVignetteFilter.setVignetteEnd(end);

        float tintAmount = mTint * 0.3f;
        mTintFilter.setRed(1.0f + tintAmount);
        mTintFilter.setGreen(1.0f - tintAmount);
        mTintFilter.setBlue(1.0f + tintAmount);

        float grainIntensity = Math.max(0f, (mGrain + 1.0f) / 2.0f) * 0.3f;
        mGrainFilter.setGrainIntensity(grainIntensity);

        mGpuImageView.requestRender();
    }

    public void reset() {
        mBrightness = 0.0f;
        mContrast = 1.0f;
        mSaturation = 1.0f;
        mWarmth = 0.0f;
        mVignette = -1.0f;
        mTint = 0.0f;
        mGrain = -1.0f;

        mBrightnessFilter.setBrightness(0.0f);
        mContrastFilter.setContrast(1.0f);
        mSaturationFilter.setSaturation(1.0f);
        mWarmthFilter.setTemperature(5000f);
        mVignetteFilter.setVignetteStart(0.7f);
        mVignetteFilter.setVignetteEnd(1.3f);
        mVignetteFilter.setVignetteCenter(new PointF(0.5f, 0.5f));
        mTintFilter.setRed(1.0f);
        mTintFilter.setGreen(1.0f);
        mTintFilter.setBlue(1.0f);
        mGrainFilter.setGrainIntensity(0.0f);

        saveCurrentState();
        mGpuImageView.requestRender();
    }

    // Custom Grain Filter
    static class GrainFilter extends GPUImageFilter {
        private float mGrainIntensity = 0.0f;
        private int mIntensityLocation = -1;

        private static final String GRAIN_FRAGMENT_SHADER =
                "precision highp float;\n" +
                        "varying vec2 textureCoordinate;\n" +
                        "uniform sampler2D inputImageTexture;\n" +
                        "uniform float intensity;\n" +
                        "\n" +
                        "float rand(vec2 co) {\n" +
                        "    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);\n" +
                        "}\n" +
                        "\n" +
                        "void main() {\n" +
                        "    vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                        "    float noise = rand(textureCoordinate * vec2(800.0, 800.0));\n" +
                        "    noise = (noise - 0.5) * intensity;\n" +
                        "    textureColor.rgb += vec3(noise);\n" +
                        "    gl_FragColor = textureColor;\n" +
                        "}\n";

        public GrainFilter() {
            super(NO_FILTER_VERTEX_SHADER, GRAIN_FRAGMENT_SHADER);
        }

        @Override
        public void onInit() {
            super.onInit();
            mIntensityLocation = GLES20.glGetUniformLocation(getProgram(), "intensity");
        }

        @Override
        public void onInitialized() {
            super.onInitialized();
            setGrainIntensity(mGrainIntensity);
        }

        public void setGrainIntensity(float intensity) {
            mGrainIntensity = intensity;
            if (mIntensityLocation >= 0) {
                setFloat(mIntensityLocation, mGrainIntensity);
            }
        }
    }
}