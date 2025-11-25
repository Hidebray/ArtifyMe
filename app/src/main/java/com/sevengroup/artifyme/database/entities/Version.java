package com.sevengroup.artifyme.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "versions",
        foreignKeys = @ForeignKey(entity = Project.class,
                parentColumns = "projectId",
                childColumns = "projectId",
                onDelete = CASCADE))
public class Version {
    @PrimaryKey(autoGenerate = true)
    public long versionId;
    public long projectId;
    public String imagePath;
    public long createdTime;
    public boolean isOriginal;

    public Version(long projectId, String imagePath, long createdTime, boolean isOriginal) {
        this.projectId = projectId;
        this.imagePath = imagePath;
        this.createdTime = createdTime;
        this.isOriginal = isOriginal;
    }
}