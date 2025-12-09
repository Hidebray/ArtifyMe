package com.sevengroup.artifyme.viewmodels;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.repositories.ProjectRepository;
import com.sevengroup.artifyme.utils.AppExecutors;
import com.sevengroup.artifyme.utils.BitmapUtils;
import java.io.File;
import java.io.FileOutputStream;

public class BasicEditorViewModel extends AndroidViewModel {
    private final ProjectRepository repository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> loadedBitmap = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveComplete = new MutableLiveData<>();
    private final MutableLiveData<Pair<Uri, Uri>> cropStartEvent = new MutableLiveData<>();
    private final MutableLiveData<String> cropError = new MutableLiveData<>();

    public BasicEditorViewModel(@NonNull Application application) {
        super(application);
        repository = ProjectRepository.getInstance(application);
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Bitmap> getLoadedBitmap() { return loadedBitmap; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsSaving() { return isSaving; }
    public LiveData<Boolean> getSaveComplete() { return saveComplete; }
    public LiveData<Pair<Uri, Uri>> getCropStartEvent() { return cropStartEvent; }
    public LiveData<String> getCropError() { return cropError; }

    public void loadEditableBitmap(String imagePath) {
        isLoading.postValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            Bitmap bitmap = BitmapUtils.loadSafeBitmap(imagePath);
            if (bitmap != null) loadedBitmap.postValue(bitmap);
            else errorMessage.postValue(getApplication().getString(R.string.msg_error_load_image));
            isLoading.postValue(false);
        });
    }

    public void saveEditedImage(long projectId, Bitmap bitmapToSave) {
        isSaving.postValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            String newPath = BitmapUtils.saveBitmapToAppStorage(getApplication(), bitmapToSave);
            if (newPath == null) {
                errorMessage.postValue(getApplication().getString(R.string.msg_error_save_file));
                isSaving.postValue(false);
                return;
            }
            repository.saveNewVersion(projectId, newPath, success -> {
                isSaving.postValue(false);
                saveComplete.postValue(success);
            });
        });
    }

    public void prepareForCrop(Bitmap currentBitmap) {
        if (currentBitmap == null) {
            cropError.postValue(getApplication().getString(R.string.msg_crop_data_error));
            return;
        }

        isLoading.postValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                Context context = getApplication();
                File tempSource = new File(context.getCacheDir(), "temp_crop_input_" + System.currentTimeMillis() + ".png");
                try (FileOutputStream out = new FileOutputStream(tempSource)) {
                    currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                File tempDest = new File(context.getCacheDir(), "temp_crop_output_" + System.currentTimeMillis() + ".png");

                Uri sourceUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        tempSource);
                Uri destUri = Uri.fromFile(tempDest);

                cropStartEvent.postValue(new Pair<>(sourceUri, destUri));

            } catch (Exception e) {
                cropError.postValue(getApplication().getString(R.string.msg_crop_prepare_error, e.getMessage()));
                isLoading.postValue(false);
            }
        });
    }

    public void processCroppedImage(Uri resultUri) {
        isLoading.postValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                String path = resultUri.getPath();
                Bitmap croppedBitmap = BitmapUtils.loadSafeBitmap(path);

                if (croppedBitmap != null) {
                    loadedBitmap.postValue(croppedBitmap);
                } else {
                    errorMessage.postValue(getApplication().getString(R.string.msg_load_cropped_error));
                }
            } catch (Exception e) {
                errorMessage.postValue(getApplication().getString(R.string.msg_process_image_error, e.getMessage()));
            } finally {
                isLoading.postValue(false);
            }
        });
    }
}