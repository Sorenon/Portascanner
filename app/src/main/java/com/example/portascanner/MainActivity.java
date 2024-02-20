package com.example.portascanner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.widget.ViewAnimator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.portascanner.models.ObjectDetectionModel;
import com.example.portascanner.objectdetection.Result;
import com.example.portascanner.objectdetection.ResultView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {

            });
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    public Preview preview;
    public ImageAnalysis imageAnalysis;
    public static Bitmap firstImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});

        this.cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        this.cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = this.cameraProviderFuture.get();
                bindCameraAndStart(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        MainActivity.scansDir(this).mkdir();
    }

    public static File scansDir(Activity activity) {
        return new File(activity.getFilesDir(), "scans");
    }

    void bindCameraAndStart(@NonNull ProcessCameraProvider cameraProvider) {
        this.preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        this.imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, this.imageAnalysis, this.preview);

        // start
        this.setContentView(R.layout.main);

        // Init Preview
        if (this.preview != null) {
            PreviewView previewView = this.requireViewById(R.id.camera_preview);
            previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
            previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
            this.preview.setSurfaceProvider(previewView.getSurfaceProvider());
        }

        // Initialise buttons
        this.requireViewById(R.id.new_scan_btn).setOnClickListener(v -> this.startScan());
        this.requireViewById(R.id.view_scans_btn).setOnClickListener(v -> {
            close();
            this.startActivity(new Intent(this, ViewScansActivity.class));
        });
        this.requireViewById(R.id.stop_scan_btn).setOnClickListener(v -> this.stopScan());
    }

    void startScan() {
        if (this.imageAnalysis == null) {
            return;
        }
        this.<ViewAnimator>requireViewById(R.id.menu_animator).setDisplayedChild(1);

        this.imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this::handleImage);
    }

    void stopScan() {
        this.imageAnalysis.clearAnalyzer();
        if (firstImage == null) {
            return;
        }
        this.<ViewAnimator>requireViewById(R.id.menu_animator).setDisplayedChild(0);

        ResultView resultView = this.requireViewById(R.id.results_view);
        resultView.setResults(new ArrayList<>(), new ArrayList<>());
        resultView.invalidate();

        Paint mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.RED);
        mPaintRectangle.setStrokeWidth(5);
        mPaintRectangle.setStyle(Paint.Style.STROKE);

        Canvas canvas = new Canvas(firstImage);
        for (Point point : points) {
            canvas.drawCircle(point.x, point.y, 10, mPaintRectangle);
        }

        this.points.clear();

        close();

        this.startActivity(new Intent(this, SaveScanActivity.class));
    }

    void close() {
        this.preview.setSurfaceProvider(null);
    }

    private ObjectDetectionModel objectDetectionModel;

    private final ArrayList<Point> points = new ArrayList<>();
    public static float scaleX;
    public static float scaleY;

    void handleImage(@NonNull ImageProxy image) {
        if (this.objectDetectionModel == null) {
            this.objectDetectionModel = new ObjectDetectionModel(this);
        }

        Bitmap bitmap = image.toBitmap();
        image.close();

        Log.d("Scan", "Start analysis");
        ArrayList<Result> results = this.objectDetectionModel.analyzeImage(bitmap);
        Log.d("Scan", "End analysis with " + results.size() + " results");

        this.runOnUiThread(() -> {
            for (Result result : results) {
                points.add(new Point(result.rect.centerX(), result.rect.centerY()));
            }

            ResultView resultView = this.findViewById(R.id.results_view);

            if (firstImage == null) {
                firstImage = bitmap;

                scaleX = (float) resultView.getHeight() / firstImage.getHeight();
                scaleY = (float) resultView.getWidth() / firstImage.getWidth();
            } else {
                bitmap.recycle();
            }

            if (resultView != null) {
                resultView.setResults(results, points);
                resultView.invalidate();
            }
        });
    }
}