package com.sevengroup.artifyme.managers;

import android.content.Context;
import androidx.annotation.NonNull;
import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class TextEditorManager {
    private final PhotoEditor mPhotoEditor;

    public TextEditorManager(Context context, PhotoEditorView view) {
        this.mPhotoEditor = new PhotoEditor.Builder(context, view)
                .setPinchTextScalable(true)
                .build();
    }

    public void addText(String text, int colorCode) {
        mPhotoEditor.addText(text, colorCode);
    }

    public void saveImage(@NonNull OnSaveBitmap onSaveBitmap) {
        mPhotoEditor.saveAsBitmap(onSaveBitmap);
    }
}