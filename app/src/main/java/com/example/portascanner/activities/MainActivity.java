package com.example.portascanner.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaPlayer;
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

import com.example.portascanner.R;
import com.example.portascanner.ScanData;
import com.example.portascanner.models.ObjectDetectionModel;
import com.example.portascanner.models.TestModel;
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
    public static ScanData scanData;

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
        PreviewView previewView = this.requireViewById(R.id.camera_preview);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        this.preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Initialise buttons
        this.requireViewById(R.id.new_scan_btn).setOnClickListener(v -> this.startScan());
        this.requireViewById(R.id.view_scans_btn).setOnClickListener(v -> this.startActivity(new Intent(this, ViewScansActivity.class)));
        this.requireViewById(R.id.stop_scan_btn).setOnClickListener(v -> this.stopScan());
    }

    void startScan() {
        if (this.imageAnalysis == null) {
            return;
        }
        this.<ViewAnimator>requireViewById(R.id.menu_animator).setDisplayedChild(1);

        this.imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this::handleImage);
        this.mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
        this.mediaPlayer.setLooping(true);
        this.lastSeenTime = System.currentTimeMillis();
    }

    void stopScan() {
        this.imageAnalysis.clearAnalyzer();
        this.mediaPlayer.release();
        this.mediaPlayer = null;
        if (scanData == null) {
            return;
        }
        this.<ViewAnimator>requireViewById(R.id.menu_animator).setDisplayedChild(0);

        ResultView resultView = this.requireViewById(R.id.results_view);
        resultView.setResults(new ArrayList<>(), new ArrayList<>());
        resultView.invalidate();

        this.startActivity(new Intent(this, SaveScanActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ResultView resultView = this.findViewById(R.id.results_view);
        if (resultView != null) {
            resultView.setResults(new ArrayList<>(), new ArrayList<>());
            resultView.invalidate();
        }
    }

    private ObjectDetectionModel objectDetectionModel;

    private long lastSeenTime = 0;
    private MediaPlayer mediaPlayer;

    void handleImage(@NonNull ImageProxy image) {
        if (this.objectDetectionModel == null) {
            this.objectDetectionModel = new TestModel(this);
        }

        Bitmap bitmap = image.toBitmap();
        image.close();

        Log.d("Scan", "Start analysis");
        ArrayList<Result> results = this.objectDetectionModel.analyzeImage(bitmap);
        Log.d("Scan", "End analysis with " + results.size() + " results");

        this.runOnUiThread(() -> {
            if (this.mediaPlayer == null) {
                return;
            }
            ResultView resultView = this.findViewById(R.id.results_view);

            if (scanData == null) {
                scanData = new ScanData();
                scanData.points = new ArrayList<>();
                scanData.image = bitmap;

                ResultView.scaleX = (float) resultView.getHeight() / bitmap.getHeight();
                ResultView.scaleY = (float) resultView.getWidth() / bitmap.getWidth();
            } else {
                bitmap.recycle();
            }

            long currentTime = System.currentTimeMillis();;
            if (results.isEmpty()) {
                if (currentTime - lastSeenTime > 3000 && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            } else {
                mediaPlayer.pause();
                mediaPlayer.seekTo(0);
                lastSeenTime = currentTime;
                Result result = results.get(0);
                scanData.points.add(new Point(result.rect.centerX(), result.rect.centerY()));
            }

            if (resultView != null) {
                resultView.setResults(results, scanData.points);
                resultView.invalidate();
            }
        });
    }
}