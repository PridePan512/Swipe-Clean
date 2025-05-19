package com.example.swipeclean.utils;

import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;

public class ReClickPreventViewClickListener implements View.OnClickListener {

    private final View.OnClickListener mListener;
    private long mLastClickTime = 0;

    public static ReClickPreventViewClickListener defendFor(@NonNull View.OnClickListener listener) {
        return new ReClickPreventViewClickListener(listener);
    }

    private ReClickPreventViewClickListener(View.OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View v) {
        long time = SystemClock.elapsedRealtime();
        if (time - mLastClickTime >= 500) {
            mLastClickTime = time;
            mListener.onClick(v);
        }
    }
}
