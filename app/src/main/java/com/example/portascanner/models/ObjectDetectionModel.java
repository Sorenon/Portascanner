package com.example.portascanner.models;

import android.graphics.Bitmap;

import com.example.portascanner.activities.MainActivity;
import com.example.portascanner.objectdetection.PrePostProcessor;
import com.example.portascanner.objectdetection.Result;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.ArrayList;

public class ObjectDetectionModel {

    protected Module module;

    public ObjectDetectionModel(MainActivity activity) {
        this.module = LiteModuleLoader.loadModuleFromAsset(activity.getAssets(), assetName());
    }

    protected String assetName() {
        return "scanner_model.torchscript.ptl";
    }

    public ArrayList<Result> analyzeImage(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, PrePostProcessor.INPUT_WIDTH, PrePostProcessor.INPUT_HEIGHT, true);

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = module.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();

        float imgScaleX = (float) bitmap.getWidth() / PrePostProcessor.INPUT_WIDTH;
        float imgScaleY = (float) bitmap.getHeight() / PrePostProcessor.INPUT_HEIGHT;

        return PrePostProcessor.outputsToNMSPredictions(outputs, imgScaleX, imgScaleY, 1);
    }
}
