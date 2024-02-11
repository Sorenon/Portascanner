package com.example.portascanner;

import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.util.Size;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {

            });
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    public Preview preview;
    public ImageAnalysis imageAnalysis;

    public Object screen;


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

        scansDir().mkdir();
    }

    public File scansDir() {
        return new File(this.getFilesDir(), "scans");
    }

    void bindCameraAndStart(@NonNull ProcessCameraProvider cameraProvider) {
        this.preview = new Preview.Builder()
                .setTargetResolution(new Size(512, 512))
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        this.imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(new Size(512, 512))
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, this.imageAnalysis, this.preview);

        this.screen = new MainScreen(this);
    }
}