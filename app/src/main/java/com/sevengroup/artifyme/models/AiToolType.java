package com.sevengroup.artifyme.models;

import com.sevengroup.artifyme.R;

public enum AiToolType {
    AI_BACKGROUND(R.string.ai_tool_background, R.drawable.ic_ai_background),
    AI_CARTOONIZE(R.string.ai_tool_cartoonize, R.drawable.ic_ai_cartoonize);

    private final int nameResId;
    private final int iconResId;

    AiToolType(int nameResId, int iconResId) {
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