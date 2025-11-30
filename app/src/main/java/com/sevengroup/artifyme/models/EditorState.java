package com.sevengroup.artifyme.models;

import com.sevengroup.artifyme.fragments.editor.AdjustFragment;
import java.util.HashMap;
import java.util.Map;

public class EditorState {
    public final int filterIndex;
    public final Map<AdjustFragment.AdjustType, Float> adjustValues;

    public EditorState(int filterIndex, Map<AdjustFragment.AdjustType, Float> adjustValues) {
        this.filterIndex = filterIndex;
        this.adjustValues = new HashMap<>(adjustValues);
    }
}