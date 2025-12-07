package com.sevengroup.artifyme.models;

import android.graphics.drawable.Drawable;

public class ShareApp {
    public String name;
    public Drawable icon;
    public String packageName;
    public String activityName;

    public ShareApp(String name, Drawable icon, String packageName, String activityName) {
        this.name = name;
        this.icon = icon;
        this.packageName = packageName;
        this.activityName = activityName;
    }
}