package com.sevengroup.artifyme.managers;

import android.content.Context;

import androidx.annotation.NonNull;

import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class TextEditorManager {
    private final PhotoEditor mPhotoEditor;

    public TextEditorManager(Context context, PhotoEditorView view) {
        // Cấu hình PhotoEditor
        this.mPhotoEditor = new PhotoEditor.Builder(context, view)
                .setPinchTextScalable(true) // Cho phép dùng 2 ngón tay phóng to/thu nhỏ chữ
                // .setClipSourceImage(true) // Có thể bật nếu muốn sticker bị cắt theo khung ảnh
                .build();
    }

    public void addText(String text, int colorCode) {
        mPhotoEditor.addText(text, colorCode);
    }

    // Hàm lưu ảnh (Wrapper)
    public void saveImage(@NonNull OnSaveBitmap onSaveBitmap) {
        mPhotoEditor.saveAsBitmap(onSaveBitmap);
    }


    public PhotoEditor getPhotoEditor() {
        return mPhotoEditor;
    }


    // Dùng để dọn dẹp màn hình sau khi cắt ảnh xong
    public void clearAllViews() {
        if (mPhotoEditor != null) {
            mPhotoEditor.clearAllViews();
        }
    }
}