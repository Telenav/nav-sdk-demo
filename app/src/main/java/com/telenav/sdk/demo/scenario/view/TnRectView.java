/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.demo.scenario.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * View the actual range of a region
 */
public class TnRectView extends View {

    private Rect newRect;
    private  Paint paint;

    public TnRectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (newRect != null) {
            Rect rect = newRect;
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            canvas.drawRect(rect, paint);
        }
    }

    public void setRect(Rect rect) {
        if (rect != null) {
            newRect = rect;
            invalidate();
        }
    }

}
