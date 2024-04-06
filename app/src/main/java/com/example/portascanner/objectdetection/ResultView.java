// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.example.portascanner.objectdetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;


public class ResultView extends View {
    private Paint mPaintRectangle;
    private List<Result> results;
    private List<Point> points;

    public float scaleX;
    public float scaleY;

    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.RED);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (results == null) return;

        for (Result result : results) {
            mPaintRectangle.setStrokeWidth(5);
            mPaintRectangle.setStyle(Paint.Style.STROKE);
            canvas.drawRect(result.rect, mPaintRectangle);
        }

        for (Point point : points) {
            canvas.drawCircle(point.x * this.scaleX, point.y * this.scaleY, 10, mPaintRectangle);
        }
    }

    public void setResults(List<Result> results, List<Point> points) {
        this.results = results;
        for (Result result : results) {
            result.rect.top *= this.scaleY;
            result.rect.bottom *= this.scaleY;

            result.rect.left *= this.scaleX;
            result.rect.right *= this.scaleX;
        }
        this.points = points;
    }
}
