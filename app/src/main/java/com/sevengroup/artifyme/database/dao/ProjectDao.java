package com.sevengroup.artifyme.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.sevengroup.artifyme.database.ProjectWithLatestVersion;
import com.sevengroup.artifyme.database.entities.Project;
import com.sevengroup.artifyme.database.entities.Version;

import java.util.List;

@Dao
public interface ProjectDao {
    @Insert
    long insertProject(Project project);

    @Insert
    void insertVersion(Version version);

    @Transaction
    @Query("SELECT p.*, v.imagePath AS latestVersionPath FROM projects p " +
            "JOIN versions v ON v.versionId = (" +
            "SELECT versionId FROM versions WHERE projectId = p.projectId ORDER BY createdTime DESC LIMIT 1) " +
            "ORDER BY p.createdTime DESC")
    LiveData<List<ProjectWithLatestVersion>> getAllProjectsWithLatestVersion();

    @Query("SELECT * FROM versions WHERE projectId = :projectId ORDER BY createdTime ASC")
    LiveData<List<Version>> getAllVersionsForProject(long projectId);

    @Query("SELECT imagePath FROM versions WHERE projectId = :projectId ORDER BY createdTime DESC LIMIT 1")
    String getLatestImagePath(long projectId);

    @Query("SELECT imagePath FROM versions WHERE projectId = :projectId")
    List<String> getAllFilePathsForProject(long projectId);

    @Query("DELETE FROM projects WHERE projectId = :projectId")
    void deleteProject(long projectId);
}