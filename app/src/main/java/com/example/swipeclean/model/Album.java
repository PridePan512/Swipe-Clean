package com.example.swipeclean.model;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.stream.Collectors;

public class Album {

    private final List<Photo> mPhotos;
    public final String formatData;

    public Album(List<Photo> photos, String formatData) {
        this.mPhotos = photos;
        this.formatData = formatData;
    }

    public String getCoverPath() {
        if (mPhotos == null || mPhotos.isEmpty()) {
            return null;
        }
        Photo photo = mPhotos.stream().filter(item -> !item.isOperated()).findFirst().orElse(null);
        if (photo == null) {
            return mPhotos.get(mPhotos.size() - 1).path;

        } else {
            return photo.path;
        }
    }

    public long getId() {
        return formatData.hashCode();
    }

    public List<Photo> getPhotos() {
        return mPhotos;
    }

    public long getDateTime() {
        return mPhotos == null || mPhotos.isEmpty() ? 0 : mPhotos.get(0).date;
    }

    public int getTotalCount() {
        return mPhotos == null || mPhotos.isEmpty() ? 0 : mPhotos.size();
    }

    public int getCompletedCount() {
        return (int) mPhotos.stream().filter(photo -> photo.isKeep).count();
    }

    public int getOperatedIndex() {
        return (int) mPhotos.stream().filter(Photo::isOperated).count();
    }

    public boolean isCompleted() {
        return getTotalCount() == getCompletedCount();
    }

    public boolean isOperated() {
        return getTotalCount() == getOperatedIndex();
    }

    @NonNull
    public Album clone(Album album) {
        List<Photo> photos = album.getPhotos().stream().map(photo -> photo.clone(photo)).collect(Collectors.toList());
        return new Album(photos, album.formatData);
    }
}
