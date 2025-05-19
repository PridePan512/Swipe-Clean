package com.example.swipeclean.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.swipeclean.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;


public class DualProgressIndicator extends LinearProgressIndicator {
    private int mSecondaryProgress = 0;
    private final Paint mPaint;

    public DualProgressIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(context, R.color.colorPrimary));
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
    }

    public void setSecondaryProgress(int progress) {
        mSecondaryProgress = progress;
        invalidate();
    }

    @Override
    protected synchronized void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = (getWidth() * mSecondaryProgress) / getMax();
        int height = getHeight();

        canvas.drawRoundRect(0, 0, width, height, height / 2f, height / 2f, mPaint);
    }

}

