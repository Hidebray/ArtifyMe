package com.sevengroup.artifyme.utils;

import java.util.ArrayList;
import java.util.List;
import jp.co.cyberagent.android.gpuimage.filter.*;
import com.sevengroup.artifyme.adapters.FilterAdapter;

public class FilterGenerator {
    private static final float[] YELLOW_GREEN_MATRIX = {
            0.7f, 0.2f, 0.1f, 0, 0,
            0.2f, 0.7f, 0.1f, 0, 0,
            0.1f, 0.2f, 0.7f, 0, 0,
            0, 0, 0, 1, 0
    };

    private static final float[] OVER_BRIGHT_MATRIX = {
            0.393f, 0.769f, 0.189f, 0, 0,
            0.349f, 0.686f, 0.168f, 0, 0,
            0.272f, 0.534f, 0.131f, 0, 0,
            0, 0, 0, 1, 0
    };

    private static final float[] OVER_RED_MATRIX = {
            1, 0, 0, 0, -30,
            0, 1, 0, 0, -30,
            0, 0, 1, 0, 50,
            0, 0, 0, 1, 0
    };

    public static List<FilterAdapter.FilterModel> getFilters() {
        List<FilterAdapter.FilterModel> filters = new ArrayList<>();

        filters.add(new FilterAdapter.FilterModel("Normal", new GPUImageFilter()));

        // Nhóm Artistic
        filters.add(new FilterAdapter.FilterModel("Contrast", new GPUImageContrastFilter(2.0f)));
        filters.add(new FilterAdapter.FilterModel("Pixelate", new GPUImagePixelationFilter()));
        filters.add(new FilterAdapter.FilterModel("Haze", new GPUImageHazeFilter()));
        filters.add(new FilterAdapter.FilterModel("Sepia", new GPUImageSepiaToneFilter()));
        filters.add(new FilterAdapter.FilterModel("Grayscale", new GPUImageGrayscaleFilter()));
        filters.add(new FilterAdapter.FilterModel("Invert", new GPUImageColorInvertFilter()));
        filters.add(new FilterAdapter.FilterModel("Monochrome", new GPUImageMonochromeFilter(1.0f, new float[]{0.6f, 0.45f, 0.3f, 1.0f})));
        filters.add(new FilterAdapter.FilterModel("Halftone", new GPUImageHalftoneFilter()));
        filters.add(new FilterAdapter.FilterModel("Sketch", new GPUImageSketchFilter()));
        filters.add(new FilterAdapter.FilterModel("Slight Invert", new GPUImageSaturationFilter(-1.0f)));
        filters.add(new FilterAdapter.FilterModel("Vignette", new GPUImageVignetteFilter()));
        filters.add(new FilterAdapter.FilterModel("Toon", new GPUImageToonFilter()));
        filters.add(new FilterAdapter.FilterModel("SmoothToon", new GPUImageSmoothToonFilter()));
        filters.add(new FilterAdapter.FilterModel("HighlightShadow", new GPUImageHighlightShadowFilter(0.0f, 1.0f)));
        filters.add(new FilterAdapter.FilterModel("Swirl", new GPUImageSwirlFilter()));

        filters.add(new FilterAdapter.FilterModel("Yellow Green", new GPUImageColorMatrixFilter(1.0f, YELLOW_GREEN_MATRIX)));
        filters.add(new FilterAdapter.FilterModel("Over Bright", new GPUImageColorMatrixFilter(1.0f, OVER_BRIGHT_MATRIX)));
        filters.add(new FilterAdapter.FilterModel("Over Red", new GPUImageColorMatrixFilter(1.0f, OVER_RED_MATRIX)));

        return filters;
    }
}