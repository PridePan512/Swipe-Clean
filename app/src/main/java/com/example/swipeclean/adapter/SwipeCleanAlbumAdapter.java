package com.example.swipeclean.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.swipeclean.R;
import com.example.swipeclean.model.Album;
import com.example.swipeclean.view.DualProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SwipeCleanAlbumAdapter extends RecyclerView.Adapter<SwipeCleanAlbumAdapter.MyViewHolder> {

    public interface ItemClickListener {
        void onCompletedItemClick(long albumId, String albumFormatDate);

        void onUncompletedItemClick(long albumId);
    }

    private final List<Album> mAlbums = new ArrayList<>();
    private final ItemClickListener mListener;

    public SwipeCleanAlbumAdapter(ItemClickListener listener) {
        mListener = listener;
    }

    public void setData(List<Album> albums) {
        List<Album> newAlbums = albums.stream().map(album -> album.clone(album)).collect(Collectors.toList());
        DiffUtil.Callback callback = new MyDiffCallback(mAlbums, newAlbums);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(callback);
        mAlbums.clear();
        mAlbums.addAll(newAlbums);
        diffResult.dispatchUpdatesTo(this);
    }

    public List<Album> getData() {
        return mAlbums;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_swipe_clean_album, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        Album album = mAlbums.get(position);

        Glide
                .with(context)
                .load(album.getCoverPath())
                .error(R.drawable.ic_vector_doc_image)
                .into(holder.mCoverImageView);

        int totalCount = album.getTotalCount();
        int completedCunt = album.getCompletedCount();
        int operatedCount = album.getOperatedIndex();

        holder.mDateTextView.setText(album.formatData);
        holder.mProgressTextView.setText(String.format(Locale.getDefault(), "%d/%d %s", completedCunt,totalCount,"张照片" ));

        //Make sure it is visible even with little progress
        holder.mProgressIndicator.setMax(103);
        holder.mProgressIndicator.setProgress(operatedCount == 0 ? 0 : 3 + (int) (100f * operatedCount / totalCount));
        holder.mProgressIndicator.setSecondaryProgress(completedCunt == 0 ? 0 : 3 + (int) (100f * completedCunt / totalCount));

        if (album.isCompleted()) {
            holder.mCompletedImageView.setVisibility(View.VISIBLE);
            holder.mCompletedView.setVisibility(View.VISIBLE);
            holder.mDateTextView.setTextColor(ContextCompat.getColor(context, R.color.text_sub));
            holder.mDateTextView.setPaintFlags(holder.mDateTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onCompletedItemClick(album.getId(), album.formatData);
                }
            });

        } else {
            holder.mCompletedImageView.setVisibility(View.GONE);
            holder.mCompletedView.setVisibility(View.GONE);
            holder.mDateTextView.setTextColor(ContextCompat.getColor(context, R.color.text_main));
            holder.mDateTextView.setPaintFlags(0);
            holder.itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onUncompletedItemClick(album.getId());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mAlbums.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mCoverImageView;
        private final TextView mDateTextView;
        private final TextView mProgressTextView;
        private final DualProgressIndicator mProgressIndicator;
        private final ImageView mCompletedImageView;
        private final View mCompletedView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            mCoverImageView = itemView.findViewById(R.id.iv_cover);
            mDateTextView = itemView.findViewById(R.id.tv_date);
            mProgressTextView = itemView.findViewById(R.id.tv_progress);
            mProgressIndicator = itemView.findViewById(R.id.lp_progress);
            mCompletedImageView = itemView.findViewById(R.id.iv_completed);
            mCompletedView = itemView.findViewById(R.id.v_completed);
        }
    }

    public static class MyDiffCallback extends DiffUtil.Callback {

        private final List<Album> oldList;
        private final List<Album> newList;

        public MyDiffCallback(List<Album> oldList, List<Album> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList == null ? 0 : oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList == null ? 0 : newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Album newAlbum = newList.get(newItemPosition);
            Album oldAlbum = oldList.get(oldItemPosition);

            return newAlbum.getId() == oldAlbum.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Album newAlbum = newList.get(newItemPosition);
            Album oldAlbum = oldList.get(oldItemPosition);

            return newAlbum.getTotalCount() == oldAlbum.getTotalCount() &&
                    newAlbum.getCompletedCount() == oldAlbum.getCompletedCount() &&
                    newAlbum.getOperatedIndex() == oldAlbum.getOperatedIndex();
        }

    }
}
