package com.example.swipeclean.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.swipeclean.R;
import com.example.swipeclean.model.Photo;

import java.util.Collections;
import java.util.List;

public class SwipeCleanRecycleBinAdapter extends RecyclerView.Adapter<SwipeCleanRecycleBinAdapter.MyViewHolder> {

    public interface OperationListener {
        void onItemKeepClick(int itemPosition, Photo photo);

        void onItemClick(int itemPosition, Photo photo);
    }

    private final List<Photo> mPhotos;
    private final OperationListener mListener;

    public SwipeCleanRecycleBinAdapter(List<Photo> photos, OperationListener listener) {
        Collections.reverse(photos);
        mPhotos = photos;
        mListener = listener;
    }

    public void removePhoto(Photo photo) {
        if (mPhotos == null || mPhotos.isEmpty()) {
            return;
        }

        mPhotos.remove(photo);
    }

    public long getTotalSize() {
        if (mPhotos == null || mPhotos.isEmpty()) {
            return 0;
        }

        return mPhotos.stream().mapToLong(Photo::getSize).sum();
    }

    public List<Photo> getData() {
        return mPhotos;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item_swipe_clean_recycle_bin, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Photo photo = mPhotos.get(position);
        Context context = holder.itemView.getContext();

        Glide
                .with(context)
                .load(photo.getPath())
                .error(R.drawable.ic_vector_doc_image)
                .into(holder.mPhotoImageView);

        holder.mKeepView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemKeepClick(holder.getBindingAdapterPosition(), photo);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(holder.getBindingAdapterPosition(), photo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPhotos == null ? 0 : mPhotos.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mPhotoImageView;
        private final ImageView mKeepView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            mPhotoImageView = itemView.findViewById(R.id.iv_photo);
            mKeepView = itemView.findViewById(R.id.iv_keep);
        }
    }
}
