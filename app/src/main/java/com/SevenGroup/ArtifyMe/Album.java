package com.SevenGroup.ArtifyMe;

public class Album {
    private String name;
    private int photoCount;
    private int coverResId;

    public Album(String name, int photoCount, int coverResId) {
        this.name = name;
        this.photoCount = photoCount;
        this.coverResId = coverResId;
    }

    public String getName() { return name; }
    public int getPhotoCount() { return photoCount; }
    public int getCoverResId() { return coverResId; }
}