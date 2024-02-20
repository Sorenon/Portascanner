package com.example.portascanner.models;

import android.graphics.Bitmap;

import com.example.portascanner.MainActivity;
import com.example.portascanner.objectdetection.Result;

import java.util.ArrayList;

public class TestModel extends ObjectDetectionModel {

    private static final String[] classes = {"person",
            "bicycle",
            "car",
            "motorcycle",
            "airplane",
            "bus",
            "train",
            "truck",
            "boat",
            "traffic light",
            "fire hydrant",
            "stop sign",
            "parking meter",
            "bench",
            "bird",
            "cat",
            "dog",
            "horse",
            "sheep",
            "cow",
            "elephant",
            "bear",
            "zebra",
            "giraffe",
            "backpack",
            "umbrella",
            "handbag",
            "tie",
            "suitcase",
            "frisbee",
            "skis",
            "snowboard",
            "sports ball",
            "kite",
            "baseball bat",
            "baseball glove",
            "skateboard",
            "surfboard",
            "tennis racket",
            "bottle",
            "wine glass",
            "cup",
            "fork",
            "knife",
            "spoon",
            "bowl",
            "banana",
            "apple",
            "sandwich",
            "orange",
            "broccoli",
            "carrot",
            "hot dog",
            "pizza",
            "donut",
            "cake",
            "chair",
            "couch",
            "potted plant",
            "bed",
            "dining table",
            "toilet",
            "tv",
            "laptop",
            "mouse",
            "remote",
            "keyboard",
            "cell phone",
            "microwave",
            "oven",
            "toaster",
            "sink",
            "refrigerator",
            "book",
            "clock",
            "vase",
            "scissors",
            "teddy bear",
            "hair drier",
            "toothbrush"};

    public TestModel(MainActivity activity) {
        super(activity);
    }

    @Override
    protected String assetName() {
        return "test_model.torchscript.ptl";
    }

    @Override
    public ArrayList<Result> analyzeImage(Bitmap bitmap) {
        ArrayList<Result> results = super.analyzeImage(bitmap);
     
        results.removeIf(result -> !useResult(result));
        
        return results;
    }

    public static boolean useResult(Result result) {
        String clazz = classes[result.classIndex];
        return clazz.equals("cell phone") || clazz.equals("mouse") || clazz.equals("remote") || clazz.equals("parking meter");
    }
}
