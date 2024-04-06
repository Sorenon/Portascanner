package com.example.portascanner.objectdetection;

import android.graphics.Rect;

public class Result {
    public Float score;
    public Rect rect;

    public Result(Float score, Rect rect) {
        this.score = score;
        this.rect = rect;
    }
}
