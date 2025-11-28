package com.sevengroup.artifyme.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.sevengroup.artifyme.database.ProjectWithLatestVersion;
import com.sevengroup.artifyme.repositories.ProjectRepository;

import java.util.List;

public class GalleryViewModel extends AndroidViewModel {
    private final ProjectRepository repository;
    private final LiveData<List<ProjectWithLatestVersion>> allProjects;

    public GalleryViewModel(@NonNull Application application) {
        super(application);
        repository = ProjectRepository.getInstance(application);
        allProjects = repository.getAllProjects();
    }

    public LiveData<List<ProjectWithLatestVersion>> getAllProjects() {
        return allProjects;
    }

    public void handleNewImageImport(Uri imageUri) {
        repository.createProject(imageUri, isSuccess -> {
        });
    }
}