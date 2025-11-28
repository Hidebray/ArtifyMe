package com.sevengroup.artifyme.repositories;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.sevengroup.artifyme.database.AppDatabase;
import com.sevengroup.artifyme.database.ProjectWithLatestVersion;
import com.sevengroup.artifyme.database.dao.ProjectDao;
import com.sevengroup.artifyme.database.entities.Project;
import com.sevengroup.artifyme.database.entities.Version;
import com.sevengroup.artifyme.utils.AppConstants;
import com.sevengroup.artifyme.utils.StorageUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectRepository {
    private static ProjectRepository INSTANCE;
    private final ProjectDao projectDao;
    private final Application application;
    private final ExecutorService executor;

    private ProjectRepository(Application application) {
        this.application = application;
        this.projectDao = AppDatabase.getInstance(application).projectDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized ProjectRepository getInstance(Application application) {
        if (INSTANCE == null) INSTANCE = new ProjectRepository(application);
        return INSTANCE;
    }

    public LiveData<List<ProjectWithLatestVersion>> getAllProjects() {
        return projectDao.getAllProjectsWithLatestVersion();
    }

    public LiveData<List<Version>> getProjectHistory(long projectId) {
        return projectDao.getAllVersionsForProject(projectId);
    }

    public void getLatestImagePath(long projectId, OnResultListener<String> listener) {
        executor.execute(() -> listener.onResult(projectDao.getLatestImagePath(projectId)));
    }

    public void createProject(Uri sourceUri, OnResultListener<Boolean> listener) {
        executor.execute(() -> {
            String newPath = StorageUtils.copyImageToAppStorage(application, sourceUri);
            if (newPath == null) {
                listener.onResult(false);
                return;
            }
            long createdTime = System.currentTimeMillis();
            Project newProject = new Project("ArtifyMe_" + createdTime, createdTime);
            long newProjectId = projectDao.insertProject(newProject);
            Version newVersion = new Version(newProjectId, newPath, createdTime, true);
            projectDao.insertVersion(newVersion);
            listener.onResult(true);
        });
    }

    public void deleteProject(long projectId, OnResultListener<Boolean> listener) {
        executor.execute(() -> {
            List<String> filePaths = projectDao.getAllFilePathsForProject(projectId);
            if (filePaths != null) {
                for (String path : filePaths) {
                    try {
                        File file = new File(path);
                        if (file.exists()) file.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            projectDao.deleteProject(projectId);
            listener.onResult(true);
        });
    }

    public void saveNewVersion(long projectId, String imagePath, OnResultListener<Boolean> listener) {
        executor.execute(() -> {
            if (imagePath == null) {
                listener.onResult(false);
                return;
            }
            Version version = new Version(projectId, imagePath, System.currentTimeMillis(), false);
            projectDao.insertVersion(version);
            listener.onResult(true);
        });
    }

    public interface OnResultListener<T> {
        void onResult(T result);
    }
}