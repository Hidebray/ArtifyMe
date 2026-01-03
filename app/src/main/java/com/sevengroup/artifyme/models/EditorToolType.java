package com.sevengroup.artifyme.models;

import com.sevengroup.artifyme.R;

public enum EditorToolType {
    CROP(R.string.tool_crop, R.drawable.crop_24px),
    ADJUST(R.string.tool_adjust, R.drawable.adjust_24px),
    FILTER(R.string.tool_filter, R.drawable.filter_24px),
    TEXT(R.string.tool_text, R.drawable.title_24px);

    private final int nameResId;
    private final int iconResId;

    EditorToolType(int nameResId, int iconResId) {
        this.nameResId = nameResId;
        this.iconResId = iconResId;
    }

    public int getNameResId() {
        return nameResId;
    }

    public int getIconResId() {
        return iconResId;
    }
}