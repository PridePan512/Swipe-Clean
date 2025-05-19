package com.example.swipeclean.model;

import androidx.annotation.NonNull;

public class Photo {

    public final long id;
    public final String path;
    public final long date;
    public final int width;
    public final int height;
    public final long size;
    public final String displayName;
    public boolean isKeep;
    public boolean isDelete;

    public Photo(long id, String displayName, String path, long date, int width, int height, long size, boolean isKeep, boolean isDelete) {
        this.id = id;
        this.displayName = displayName;
        this.path = path;
        this.date = date;
        this.width = width;
        this.height = height;
        this.size = size;
        this.isKeep = isKeep;
        this.isDelete = isDelete;
    }

    public boolean isOperated() {
        return isKeep || isDelete;
    }

    @NonNull
    public Photo clone(Photo photo) {
        return new Photo(photo.id, photo.displayName, photo.path, photo.date, photo.width, photo.height, photo.size, photo.isKeep, photo.isDelete);
    }

    public String getDisplayName() {
        return path;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getTakenTime() {
        return date;
    }
}
