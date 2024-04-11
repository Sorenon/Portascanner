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
    private Paint paint;
    private List<Result> results;
    private List<Point> points;
    private Result bestResult;

    public float scaleX;
    public float scaleY;

    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (results == null) return;

        for (Result result : results) {
            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);
            if (result != bestResult) {
                paint.setColor(Color.BLUE);
            }
            canvas.drawRect(result.rect, paint);
            if (result != bestResult) {
                paint.setColor(Color.RED);
            }
        }

        for (Point point : points) {
            canvas.drawCircle(point.x * this.scaleX, point.y * this.scaleY, 10, paint);
        }
    }

    public void setResults(List<Result> results, List<Point> points, Result bestResult) {
        this.results = results;
        for (Result result : results) {
            result.rect.top *= this.scaleY;
            result.rect.bottom *= this.scaleY;

            result.rect.left *= this.scaleX;
            result.rect.right *= this.scaleX;
        }
        this.points = points;
        this.bestResult = bestResult;
    }
}
