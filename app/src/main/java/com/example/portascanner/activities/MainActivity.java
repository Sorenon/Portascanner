package com.example.portascanner.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ViewAnimator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.portascanner.R;
import com.example.portascanner.scans.ScanCreator;
import com.example.portascanner.scans.ScanData;
import com.example.portascanner.scans.ScanRepository;
import com.example.portascanner.objectdetection.ResultView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {

            });
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    public Preview preview;
    public ScanCreator scanCreator;

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

        ScanRepository.INSTANCE = new ScanRepository(this);
    }

    void bindCameraAndStart(@NonNull ProcessCameraProvider cameraProvider) {
        this.preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, this.preview);

        // Start
        this.setContentView(R.layout.main);
        this.scanCreator = new ScanCreator(this, this.requireViewById(R.id.results_view), imageAnalysis);

        // Init Preview
        PreviewView previewView = this.requireViewById(R.id.camera_preview);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        this.preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Initialise buttons
        this.requireViewById(R.id.new_scan_btn).setOnClickListener(v -> this.startScan());
        this.requireViewById(R.id.view_scans_btn).setOnClickListener(v -> this.startActivity(new Intent(this, ViewScansActivity.class)));
        this.requireViewById(R.id.stop_scan_btn).setOnClickListener(v -> this.stopScan());
        this.requireViewById(R.id.about_btn).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Switch to test model")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> this.scanCreator.switchToTesting())
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show());
    }

    void startScan() {
        if (this.scanCreator == null) {
            return;
        }
        this.<ViewAnimator>requireViewById(R.id.menu_animator).setDisplayedChild(1);

        this.scanCreator.startScan();
    }

    void stopScan() {
        this.<ViewAnimator>requireViewById(R.id.menu_animator).setDisplayedChild(0);

        ScanData scan = this.scanCreator.stopScan();

        if (scan != null) {
            SaveScanActivity.SCAN_TO_SAVE = scan;
            this.startActivity(new Intent(this, SaveScanActivity.class));
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.scanCreator.destroy();
    }
}