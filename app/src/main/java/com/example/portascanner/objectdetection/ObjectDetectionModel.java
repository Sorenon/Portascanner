package com.example.portascanner.objectdetection;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.example.portascanner.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class ObjectDetectionModel {
    public final static int INPUT_WIDTH = 640;
    public final static int INPUT_HEIGHT = 640;
    private final OrtSession session;
    private final FloatBuffer inputTensorBuffer;
    private final OnnxTensor inputTensor;

    public ObjectDetectionModel(Activity activity) throws OrtException, IOException {
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        session = env.createSession(readModel(activity));
        inputTensorBuffer = ByteBuffer.allocateDirect(3 * INPUT_WIDTH * INPUT_HEIGHT * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        inputTensor = OnnxTensor.createTensor(env, inputTensorBuffer, new long[]{1L, 3L, (long) INPUT_HEIGHT, (long) INPUT_WIDTH});
    }

    public List<Result> run(Bitmap bitmap) throws OrtException {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, true);
        bitmapToCHW(resizedBitmap, inputTensorBuffer);

        try (OrtSession.Result onnxResult = session.run(Collections.singletonMap("images", inputTensor))) {
            FloatBuffer outputs = ((OnnxTensor) onnxResult.get(0)).getFloatBuffer();
            List<Result> results = new ArrayList<>();

            float imgScaleX = (float) bitmap.getWidth() / INPUT_WIDTH;
            float imgScaleY = (float) bitmap.getHeight() / INPUT_HEIGHT;

            for (int i = 0; i < outputs.limit(); i += 6) {
                float x = outputs.get(i + 0);
                float y = outputs.get(i + 1);
                float w = outputs.get(i + 2);
                float h = outputs.get(i + 3);
                float s = outputs.get(i + 4);

                float left = imgScaleX * (x - w / 2);
                float top = imgScaleY * (y - h / 2);
                float right = imgScaleX * (x + w / 2);
                float bottom = imgScaleY * (y + h / 2);

                Rect rect = new Rect((int) left, (int) top, (int) right, (int) bottom);
                Result result = new Result(s, rect);
                results.add(result);
            }

            return results;
        }
    }

    private static byte[] readModel(Activity activity) throws IOException {
        try (InputStream inputStream = activity.getResources().openRawResource(R.raw.model)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer, 0, buffer.length)) > 0) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private static void bitmapToCHW(Bitmap bitmap, FloatBuffer outBuffer) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int pixelsCount = height * width;
        int[] pixels = new int[pixelsCount];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int offset_g;
        int offset_b;
        float r;
        offset_g = pixelsCount;
        offset_b = 2 * pixelsCount;

        for (int i = 0; i < pixelsCount; ++i) {
            int c = pixels[i];
            r = (float) (c >> 16 & 255) / 255.0F;
            float g = (float) (c >> 8 & 255) / 255.0F;
            float b = (float) (c & 255) / 255.0F;
            outBuffer.put(i, r);
            outBuffer.put(offset_g + i, g);
            outBuffer.put(offset_b + i, b);
        }
    }

    public void destroy() throws OrtException {
        this.session.close();
    }
}
