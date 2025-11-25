package com.sevengroup.artifyme.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.sevengroup.artifyme.database.dao.ProjectDao;
import com.sevengroup.artifyme.database.entities.Project;
import com.sevengroup.artifyme.database.entities.Version;

@Database(entities = {Project.class, Version.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProjectDao projectDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "artifyme_db").build();
                }
            }
        }
        return INSTANCE;
    }
}