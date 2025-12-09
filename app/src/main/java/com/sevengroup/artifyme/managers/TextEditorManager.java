package com.sevengroup.artifyme.managers;

import android.content.Context;
import androidx.annotation.NonNull;
import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class TextEditorManager {
    private PhotoEditor mPhotoEditor;

    public TextEditorManager(Context context, PhotoEditorView view) {
        this.mPhotoEditor = new PhotoEditor.Builder(context, view)
                .setPinchTextScalable(true)
                .setClipSourceImage(true)
                .build();
    }

    public void addText(String text, int colorCode) {
        if (mPhotoEditor != null) mPhotoEditor.addText(text, colorCode);
    }

    public void saveImage(@NonNull OnSaveBitmap onSaveBitmap) {
        if (mPhotoEditor != null) mPhotoEditor.saveAsBitmap(onSaveBitmap);
    }

    public PhotoEditor getPhotoEditor() {
        return mPhotoEditor;
    }

    public void clearAllViews() {
        if (mPhotoEditor != null) {
            mPhotoEditor.clearAllViews();
        }
    }

    public void release() {
        mPhotoEditor = null;
    }
}