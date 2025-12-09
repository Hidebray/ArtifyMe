package com.sevengroup.artifyme.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sevengroup.artifyme.R;
import com.sevengroup.artifyme.database.entities.Version;
import com.sevengroup.artifyme.repositories.ProjectRepository;
import com.sevengroup.artifyme.utils.AppExecutors;
import com.sevengroup.artifyme.utils.StorageUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectDetailViewModel extends AndroidViewModel {
    private final ProjectRepository repository;
    private final MutableLiveData<String> latestImagePath = new MutableLiveData<>();
    private final MutableLiveData<String> exportStatusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isExporting = new MutableLiveData<>();
    private final MutableLiveData<Boolean> projectDeleted = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToEditor = new MutableLiveData<>();

    public ProjectDetailViewModel(@NonNull Application application) {
        super(application);
        repository = ProjectRepository.getInstance(application);
    }

    public LiveData<List<Version>> getProjectHistory(long projectId) { return repository.getProjectHistory(projectId); }
    public LiveData<String> getLatestImagePath() { return latestImagePath; }
    public LiveData<String> getExportStatusMessage() { return exportStatusMessage; }
    public LiveData<Boolean> getIsExporting() { return isExporting; }
    public LiveData<Boolean> getProjectDeleted() { return projectDeleted; }
    public LiveData<String> getNavigateToEditor() { return navigateToEditor; }

    public void refreshLatestImagePath(long projectId) {
        repository.getLatestImagePath(projectId, path -> {
            if (path != null) latestImagePath.postValue(path);
        });
    }

    public void handleDeleteProject(long projectId) {
        repository.deleteProject(projectId, success -> {
            if (success) projectDeleted.postValue(true);
        });
    }

    public void exportImageToGallery(String imagePath) {
        isExporting.postValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            boolean success = StorageUtils.exportFileToPublicGallery(getApplication(), imagePath);
            String msg = success
                    ? getApplication().getString(R.string.msg_save_gallery_success)
                    : getApplication().getString(R.string.msg_save_gallery_error);
            exportStatusMessage.postValue(msg);
            isExporting.postValue(false);
        });
    }

    public void onEditVersionRequest(String imagePath) {
        navigateToEditor.setValue(imagePath);
    }

    public void onEditVersionNavigated() {
        navigateToEditor.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}