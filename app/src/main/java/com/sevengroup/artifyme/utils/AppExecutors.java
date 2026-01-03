package com.sevengroup.artifyme.utils;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final Object LOCK = new Object();
    private static volatile AppExecutors sInstance;
    private final Executor diskIO;
    private final Executor mainThread;
    private final Executor networkIO;
    private final Executor computationIO;

    private AppExecutors(Executor diskIO, Executor networkIO, Executor mainThread, Executor computationIO) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
        this.computationIO = computationIO;
    }

    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    int threadCount = Runtime.getRuntime().availableProcessors();

                    sInstance = new AppExecutors(
                            Executors.newSingleThreadExecutor(),
                            Executors.newFixedThreadPool(3),
                            new MainThreadExecutor(),
                            Executors.newFixedThreadPool(Math.max(2, threadCount))
                    );
                }
            }
        }
        return sInstance;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    public Executor networkIO() {
        return networkIO;
    }

    public Executor computation() {
        return computationIO;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}