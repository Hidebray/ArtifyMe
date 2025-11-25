package com.sevengroup.artifyme.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class Project {
    @PrimaryKey(autoGenerate = true)
    public long projectId;
    public String projectName;
    public long createdTime;

    public Project(String projectName, long createdTime) {
        this.projectName = projectName;
        this.createdTime = createdTime;
    }
}