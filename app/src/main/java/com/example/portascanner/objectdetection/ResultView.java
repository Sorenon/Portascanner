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

import com.example.portascanner.MainActivity;

import java.util.ArrayList;


public class ResultView extends View {

    private Paint mPaintRectangle;
    private ArrayList<Result> mResults;
    private ArrayList<Point> mpoints;

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

        if (mResults == null) return;

        for (Result result : mResults) {
            mPaintRectangle.setStrokeWidth(5);
            mPaintRectangle.setStyle(Paint.Style.STROKE);
            canvas.drawRect(result.rect, mPaintRectangle);
        }

        for (Point point : mpoints) {
            canvas.drawCircle(point.x * MainActivity.scaleX, point.y * MainActivity.scaleY, 10, mPaintRectangle);
        }
    }

    public void setResults(ArrayList<Result> results, ArrayList<Point> points) {
        mResults = results;
        for (Result result : results) {
            result.rect.top *= MainActivity.scaleY;
            result.rect.bottom *= MainActivity.scaleY;

            result.rect.left *= MainActivity.scaleX;
            result.rect.right *= MainActivity.scaleX;
        }

        mpoints = points;
    }
}
