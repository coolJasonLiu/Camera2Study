package com.example.camera2study;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class textureView extends TextureView {
    public textureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width/9*8, width/3*5);
    }
}
