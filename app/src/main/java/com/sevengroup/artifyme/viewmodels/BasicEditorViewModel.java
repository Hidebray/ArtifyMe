package com.sevengroup.artifyme.viewmodels;

import android.app.Application;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sevengroup.artifyme.repositories.ProjectRepository;
import com.sevengroup.artifyme.utils.BitmapUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicEditorViewModel extends AndroidViewModel {
    private final ProjectRepository repository;
    private final ExecutorService executor;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> loadedBitmap = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveComplete = new MutableLiveData<>();

    public BasicEditorViewModel(@NonNull Application application) {
        super(application);
        repository = ProjectRepository.getInstance(application);
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Bitmap> getLoadedBitmap() { return loadedBitmap; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsSaving() { return isSaving; }
    public LiveData<Boolean> getSaveComplete() { return saveComplete; }

    public void loadEditableBitmap(String imagePath) {
        isLoading.postValue(true);
        executor.execute(() -> {
            Bitmap bitmap = BitmapUtils.loadSafeBitmap(imagePath);
            if (bitmap != null) loadedBitmap.postValue(bitmap);
            else errorMessage.postValue("Lỗi: Không thể tải ảnh.");
            isLoading.postValue(false);
        });
    }

    public void saveEditedImage(long projectId, Bitmap bitmapToSave) {
        isSaving.postValue(true);
        executor.execute(() -> {
            String newPath = BitmapUtils.saveBitmapToAppStorage(getApplication(), bitmapToSave);
            if (newPath == null) {
                errorMessage.postValue("Lỗi: Không thể lưu file.");
                isSaving.postValue(false);
                return;
            }
            repository.saveNewVersion(projectId, newPath, success -> {
                isSaving.postValue(false);
                saveComplete.postValue(success);
            });
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}