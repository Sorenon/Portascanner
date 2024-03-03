// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.example.portascanner.objectdetection;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class PrePostProcessor {
    // for yolov5 model, no need to apply MEAN and STD
    public final static float[] NO_MEAN_RGB = new float[]{0.0f, 0.0f, 0.0f};
    public final static float[] NO_STD_RGB = new float[]{1.0f, 1.0f, 1.0f};

    // model input image size
    public final static int INPUT_WIDTH = 640;
    public final static int INPUT_HEIGHT = 640;

    // model output is of size 25200*(num_of_class+5)
    private static int mOutputRow = 25200; // as decided by the YOLOv5 model for input image of size 640*640
    private static float mThreshold = 0.30f; // score above which a detection is generated
    private static int mNmsLimit = 15;


    // The two methods nonMaxSuppression and IOU below are ported from https://github.com/hollance/YOLO-CoreML-MPSNNGraph/blob/master/Common/Helpers.swift

    /**
     * Removes bounding boxes that overlap too much with other boxes that have
     * a higher score.
     * - Parameters:
     * - boxes: an array of bounding boxes and their scores
     * - limit: the maximum number of boxes that will be selected
     * - threshold: used to decide whether boxes overlap too much
     */
    static ArrayList<Result> nonMaxSuppression(ArrayList<Result> boxes, int limit, float threshold) {

        // Do an argsort on the confidence scores, from high to low.
        boxes.sort(Comparator.comparing(o -> o.score));

        ArrayList<Result> selected = new ArrayList<>();
        boolean[] active = new boolean[boxes.size()];
        Arrays.fill(active, true);
        int numActive = active.length;

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        boolean done = false;
        for (int i = 0; i < boxes.size() && !done; i++) {
            if (active[i]) {
                Result boxA = boxes.get(i);
                selected.add(boxA);
                if (selected.size() >= limit) break;

                for (int j = i + 1; j < boxes.size(); j++) {
                    if (active[j]) {
                        Result boxB = boxes.get(j);
                        if (IOU(boxA.rect, boxB.rect) > threshold) {
                            active[j] = false;
                            numActive -= 1;
                            if (numActive <= 0) {
                                done = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return selected;
    }

    /**
     * Computes intersection-over-union overlap between two bounding boxes.
     */
    static float IOU(Rect a, Rect b) {
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        if (areaA <= 0.0) return 0.0f;

        float areaB = (b.right - b.left) * (b.bottom - b.top);
        if (areaB <= 0.0) return 0.0f;

        float intersectionMinX = Math.max(a.left, b.left);
        float intersectionMinY = Math.max(a.top, b.top);
        float intersectionMaxX = Math.min(a.right, b.right);
        float intersectionMaxY = Math.min(a.bottom, b.bottom);
        float intersectionArea = Math.max(intersectionMaxY - intersectionMinY, 0) *
                Math.max(intersectionMaxX - intersectionMinX, 0);
        return intersectionArea / (areaA + areaB - intersectionArea);
    }

    public static ArrayList<Result> outputsToNMSPredictions(float[] outputs, float imgScaleX, float imgScaleY, int classes) {
        ArrayList<Result> results = new ArrayList<>();

        // left, top, right, bottom, score and num_of_class * probability
        int outputColumn = 5 + classes;

        for (int i = 0; i < mOutputRow; i++) {
            if (outputs[i * outputColumn + 4] > mThreshold) {
                float x = outputs[i * outputColumn];
                float y = outputs[i * outputColumn + 1];
                float w = outputs[i * outputColumn + 2];
                float h = outputs[i * outputColumn + 3];

                float left = imgScaleX * (x - w / 2);
                float top = imgScaleY * (y - h / 2);
                float right = imgScaleX * (x + w / 2);
                float bottom = imgScaleY * (y + h / 2);

                float max = outputs[i * outputColumn + 5];
                int cls = 0;
                for (int j = 0; j < outputColumn - 5; j++) {
                    if (outputs[i * outputColumn + 5 + j] > max) {
                        max = outputs[i * outputColumn + 5 + j];
                        cls = j;
                    }
                }

                Rect rect = new Rect((int) left, (int) top, (int) right, (int) bottom);
                Result result = new Result(cls, outputs[i * outputColumn + 4], rect);
                results.add(result);
            }
        }
        return nonMaxSuppression(results, mNmsLimit, mThreshold);
    }
}
