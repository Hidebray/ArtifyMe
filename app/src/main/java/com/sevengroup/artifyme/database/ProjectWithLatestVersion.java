package com.sevengroup.artifyme.database;

import androidx.room.Embedded;
import com.sevengroup.artifyme.database.entities.Project;

public class ProjectWithLatestVersion {
    @Embedded
    public Project project;
    public String latestVersionPath;
}