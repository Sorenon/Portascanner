package com.example.portascanner;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

public class MainScreen {
    private final MainActivity activity;
    private Bitmap firstImage;

    public MainScreen(MainActivity activity) {
        this.activity = activity;

        this.activity.setContentView(R.layout.main);

        // Init Preview
        if (this.activity.preview != null) {
            PreviewView previewView = this.activity.requireViewById(R.id.camera_preview);
            previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
            previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
            this.activity.preview.setSurfaceProvider(previewView.getSurfaceProvider());
        }

        // Initialise buttons
        this.activity.requireViewById(R.id.new_scan_btn).setOnClickListener(v -> this.startScan());
        this.activity.requireViewById(R.id.view_scans_btn).setOnClickListener(v -> {
            close();
            this.activity.screen = new ViewScansScreen(this.activity);
        });
        this.activity.requireViewById(R.id.stop_scan_btn).setOnClickListener(v -> this.stopScan());
    }

    void startScan() {
        if (this.activity.imageAnalysis == null) {
            return;
        }
        this.activity.<ViewAnimator>requireViewById(R.id.menu_animator).setDisplayedChild(1);
        this.activity.imageAnalysis.setAnalyzer(this.activity.getMainExecutor(), this::handleImage);
    }

    void stopScan() {
        this.activity.imageAnalysis.clearAnalyzer();
        if (this.firstImage == null) {
            this.activity.<ViewAnimator>requireViewById(R.id.menu_animator).setDisplayedChild(0);
            return;
        }

        close();
        this.activity.screen = new SaveScanScreen(this.activity, this.firstImage);
    }

    void close() {
        this.activity.preview.setSurfaceProvider(null);
    }

    void handleImage(@NonNull ImageProxy image) {
        Bitmap bitmap = image.toBitmap();
        image.close();

        int halfWidth = bitmap.getWidth() / 2;
        int halfHeight = bitmap.getHeight() / 2;
        int halfSize = Math.min(halfWidth, halfHeight);

        Matrix matrix = new Matrix();
        matrix.postScale((512.f * 0.5f) / halfSize, (512.f * 0.5f) / halfSize);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, halfWidth - halfSize, halfHeight - halfSize, halfSize * 2, halfSize * 2, matrix, false);
        bitmap.recycle();

        if (this.firstImage == null) {
            this.firstImage = resizedBitmap;
        } else {
            resizedBitmap.recycle();
        }
    }
}
