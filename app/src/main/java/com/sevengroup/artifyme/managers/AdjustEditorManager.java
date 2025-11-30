package com.sevengroup.artifyme.managers;

import android.graphics.PointF;
import com.sevengroup.artifyme.fragments.editor.AdjustFragment.AdjustType;
import java.util.EnumMap;
import java.util.Map;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.*;

public class AdjustEditorManager {

    private final GPUImageView mGpuImageView;
    private final GPUImageFilterGroup mAdjustGroup;

    // Map lưu trữ Filter tương ứng với từng loại AdjustType
    private final Map<AdjustType, GPUImageFilter> filterMap = new EnumMap<>(AdjustType.class);

    // Map lưu trữ giá trị hiện tại (Value từ UI)
    private final Map<AdjustType, Float> currentValues = new EnumMap<>(AdjustType.class);

    // Map lưu trữ giá trị đã lưu (để Restore)
    private final Map<AdjustType, Float> savedValues = new EnumMap<>(AdjustType.class);

    public AdjustEditorManager(GPUImageView gpuImageView) {
        this.mGpuImageView = gpuImageView;
        this.mAdjustGroup = new GPUImageFilterGroup();
        initFilters();
    }

    private void initFilters() {
        // 1. Khởi tạo từng Filter và giá trị mặc định
        // Lưu ý: Thứ tự add vào đây sẽ là thứ tự áp dụng Filter lên ảnh

        // Brightness
        setupFilter(AdjustType.BRIGHTNESS, new GPUImageBrightnessFilter(0.0f), 0.0f);

        // Contrast
        setupFilter(AdjustType.CONTRAST, new GPUImageContrastFilter(1.0f), 0.0f);

        // Saturation
        setupFilter(AdjustType.SATURATION, new GPUImageSaturationFilter(1.0f), 0.0f);

        // Exposure
        setupFilter(AdjustType.EXPOSURE, new GPUImageExposureFilter(0.0f), 0.0f);

        // Gamma
        setupFilter(AdjustType.GAMMA, new GPUImageGammaFilter(1.0f), 0.0f); // Default UI val: 0.0 -> converted to 1.0

        // Highlight & Shadow
        setupFilter(AdjustType.HIGHLIGHTS, new GPUImageHighlightShadowFilter(0.0f, 1.0f), 0.0f); // Tách logic highlight
        // Lưu ý: HighlightShadowFilter thường xử lý cả 2, ở đây ta tách logic ra hoặc dùng chung filter
        // Để đơn giản theo code cũ, ta dùng riêng filter hoặc chung map nhưng cần cẩn thận.
        // Ở code cũ bạn dùng chung mHighlightShadowFilter cho cả 2 type.
        // Để tối ưu đúng map: Ta sẽ ánh xạ 2 Type vào cùng 1 instance filter trong map.
        GPUImageHighlightShadowFilter hsFilter = new GPUImageHighlightShadowFilter(0.0f, 1.0f);
        filterMap.put(AdjustType.HIGHLIGHTS, hsFilter);
        filterMap.put(AdjustType.SHADOWS, hsFilter);
        currentValues.put(AdjustType.HIGHLIGHTS, 0.0f);
        currentValues.put(AdjustType.SHADOWS, 0.0f);
        mAdjustGroup.addFilter(hsFilter);

        // Warmth
        setupFilter(AdjustType.WARMTH, new GPUImageWhiteBalanceFilter(5000f, 0f), 0.0f);

        // Tint
        setupFilter(AdjustType.TINT, new GPUImageRGBFilter(1.0f, 1.0f, 1.0f), 0.0f);

        // Sharpness
        setupFilter(AdjustType.SHARPNESS, new GPUImageSharpenFilter(0.0f), 0.0f);

        // Vignette
        GPUImageVignetteFilter vignetteFilter = new GPUImageVignetteFilter();
        vignetteFilter.setVignetteCenter(new PointF(0.5f, 0.5f));
        vignetteFilter.setVignetteStart(0.7f);
        vignetteFilter.setVignetteEnd(1.3f);
        setupFilter(AdjustType.VIGNETTE, vignetteFilter, -1.0f);

        // Grain (Custom)
        GrainFilter grainFilter = new GrainFilter();
        grainFilter.setGrainIntensity(0.0f);
        setupFilter(AdjustType.GRAIN, grainFilter, -1.0f);

        // Lưu trạng thái ban đầu
        saveCurrentState();
    }

    private void setupFilter(AdjustType type, GPUImageFilter filter, float defaultValue) {
        filterMap.put(type, filter);
        currentValues.put(type, defaultValue);
        mAdjustGroup.addFilter(filter);
    }

    public GPUImageFilterGroup getAdjustFilterGroup() {
        return mAdjustGroup;
    }

    public void adjust(AdjustType type, float value) {
        // Cập nhật giá trị vào Map
        currentValues.put(type, value);

        // Lấy filter tương ứng
        GPUImageFilter filter = filterMap.get(type);
        if (filter != null) {
            updateFilterValue(type, filter, value);
        }

        // Render lại
        if (mGpuImageView != null) {
            mGpuImageView.requestRender();
        }
    }

    // Logic chuyển đổi giá trị UI (-1.0 -> 1.0 hoặc 0 -> 100) sang tham số Filter
    private void updateFilterValue(AdjustType type, GPUImageFilter filter, float value) {
        switch (type) {
            case BRIGHTNESS:
                ((GPUImageBrightnessFilter) filter).setBrightness(value);
                break;
            case CONTRAST:
                ((GPUImageContrastFilter) filter).setContrast(1.0f + value);
                break;
            case SATURATION:
                ((GPUImageSaturationFilter) filter).setSaturation(1.0f + value);
                break;
            case WARMTH:
                // 5000f là mốc trung bình
                ((GPUImageWhiteBalanceFilter) filter).setTemperature(5000f + (value * 2000f));
                break;
            case VIGNETTE:
                GPUImageVignetteFilter vFilter = (GPUImageVignetteFilter) filter;
                float strength = (value + 1f) / 2f;
                vFilter.setVignetteStart(0.7f - strength * 0.4f);
                vFilter.setVignetteEnd(1.3f - strength * 0.5f);
                break;
            case TINT:
                GPUImageRGBFilter tFilter = (GPUImageRGBFilter) filter;
                float tintAmount = value * 0.3f;
                tFilter.setRed(1.0f + tintAmount);
                tFilter.setGreen(1.0f - tintAmount);
                tFilter.setBlue(1.0f + tintAmount);
                break;
            case GRAIN:
                // Normalize từ -1..1 về 0..1 rồi scale intensity
                float grainVal = Math.max(0f, (value + 1.0f) / 2.0f) * 0.3f;
                ((GrainFilter) filter).setGrainIntensity(grainVal);
                break;
            case SHARPNESS:
                ((GPUImageSharpenFilter) filter).setSharpness(value * 4.0f);
                break;
            case EXPOSURE:
                ((GPUImageExposureFilter) filter).setExposure(value * 2.0f);
                break;
            case HIGHLIGHTS:
                // Highlight và Shadow dùng chung 1 instance filter trong Map
                // Nên khi set, ta chỉ gọi hàm set tương ứng
                ((GPUImageHighlightShadowFilter) filter).setHighlights((value + 1.0f) / 2.0f);
                break;
            case SHADOWS:
                ((GPUImageHighlightShadowFilter) filter).setShadows((value + 1.0f) / 2.0f);
                break;
            case GAMMA:
                ((GPUImageGammaFilter) filter).setGamma(Math.max(0.1f, 1.0f + value));
                break;
        }
    }

    public float getCurrentValue(AdjustType type) {
        return currentValues.getOrDefault(type, 0.0f);
    }

    public void saveCurrentState() {
        savedValues.clear();
        savedValues.putAll(currentValues);
    }

    public void restoreState() {
        // Khôi phục giá trị từ savedValues vào currentValues
        currentValues.clear();
        currentValues.putAll(savedValues);

        // Apply lại toàn bộ filter theo giá trị vừa khôi phục
        reApplyAllFilters();
    }

    public void reset() {
        // Reset về giá trị mặc định
        for (AdjustType type : AdjustType.values()) {
            float defVal = 0.0f;
            if (type == AdjustType.VIGNETTE || type == AdjustType.GRAIN) defVal = -1.0f;
            currentValues.put(type, defVal);
        }

        saveCurrentState(); // Lưu trạng thái reset làm gốc
        reApplyAllFilters();
    }

    private void reApplyAllFilters() {
        for (Map.Entry<AdjustType, Float> entry : currentValues.entrySet()) {
            AdjustType type = entry.getKey();
            Float value = entry.getValue();
            GPUImageFilter filter = filterMap.get(type);
            if (filter != null) {
                updateFilterValue(type, filter, value);
            }
        }
        if (mGpuImageView != null) {
            mGpuImageView.requestRender();
        }
    }

    public Map<AdjustType, Float> getAllSettings() {
        return new EnumMap<>(currentValues);
    }

    public void applySettings(Map<AdjustType, Float> settings) {
        if (settings == null) return;
        for (Map.Entry<AdjustType, Float> entry : settings.entrySet()) {
            adjust(entry.getKey(), entry.getValue());
        }
    }

    // --- Custom Grain Filter (Giữ nguyên) ---
    static class GrainFilter extends GPUImageFilter {
        private float mGrainIntensity = 0.0f;
        private int mIntensityLocation = -1;
        private static final String GRAIN_FRAGMENT_SHADER =
                "precision highp float;\n" +
                        "varying vec2 textureCoordinate;\n" +
                        "uniform sampler2D inputImageTexture;\n" +
                        "uniform float intensity;\n" +
                        "float rand(vec2 co) {\n" +
                        "    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);\n" +
                        "}\n" +
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
            mIntensityLocation = android.opengl.GLES20.glGetUniformLocation(getProgram(), "intensity");
        }

        @Override
        public void onInitialized() {
            super.onInitialized();
            setGrainIntensity(mGrainIntensity);
        }

        public void setGrainIntensity(float intensity) {
            mGrainIntensity = intensity;
            if (mIntensityLocation >= 0) setFloat(mIntensityLocation, mGrainIntensity);
        }
    }
}