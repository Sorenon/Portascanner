package com.example.portascanner.scans;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.portascanner.R;
import com.example.portascanner.activities.MainActivity;
import com.example.portascanner.objectdetection.ObjectDetectionModel;
import com.example.portascanner.objectdetection.Result;
import com.example.portascanner.objectdetection.ResultView;
import com.example.portascanner.objectdetection.TestModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScanCreator {
    private ObjectDetectionModel objectDetectionModel;

    private final Executor executor;
    private final MainActivity mainActivity;
    private final ResultView resultView;
    private final ImageAnalysis imageAnalysis;


    private MediaPlayer mediaPlayer;
    private ScanData partialScan;
    private long lastSeenTime = 0;
    private Analyzer activeAnalyzer;


    public ScanCreator(MainActivity mainActivity, ResultView resultView, ImageAnalysis imageAnalysis) {
        this.mainActivity = mainActivity;
        this.resultView = resultView;
        this.imageAnalysis = imageAnalysis;
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.execute(() -> this.objectDetectionModel = new ObjectDetectionModel(this.mainActivity));
    }

    private void handleFirstFrame(Bitmap bitmap) {
        this.partialScan = new ScanData();
        this.partialScan.points = new ArrayList<>();
        this.partialScan.image = bitmap;
        this.resultView.scaleX = (float) this.resultView.getHeight() / bitmap.getHeight();
        this.resultView.scaleY = (float) this.resultView.getWidth() / bitmap.getWidth();
        this.lastSeenTime = System.currentTimeMillis();
    }

    private void handleResults(List<Result> results) {
        long currentTime = System.currentTimeMillis();
        boolean lostSensor = currentTime - lastSeenTime > 3000;

        if (results.isEmpty()) {
            if (lostSensor && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } else {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);

            Point selectedResult = new Point();
            if (!lostSensor && results.size() > 1 && !partialScan.points.isEmpty()) {
                Point lastPoint = partialScan.points.get(partialScan.points.size() - 1);

                int bestDistanceSqr = Integer.MAX_VALUE;
                for (Result result : results) {
                    int resultX = result.rect.centerX();
                    int resultY = result.rect.centerY();

                    int distanceX = resultX - lastPoint.x;
                    int distanceY = resultY - lastPoint.y;
                    int distanceSqr = distanceX * distanceX + distanceY * distanceY;

                    if (distanceSqr < bestDistanceSqr) {
                        bestDistanceSqr = distanceSqr;
                        selectedResult.x = resultX;
                        selectedResult.y = resultY;
                    }
                }
            } else {
                Result mostConfident = results.get(0);
                for (int i = 1; i < results.size(); i++) {
                    Result result = results.get(i);
                    if (result.score > mostConfident.score) {
                        mostConfident = result;
                    }
                }
                selectedResult.x = mostConfident.rect.centerX();
                selectedResult.y = mostConfident.rect.centerY();
            }

            lastSeenTime = currentTime;
            partialScan.points.add(selectedResult);
        }

        resultView.setResults(results, partialScan.points);
        resultView.invalidate();
    }

    public void startScan() {
        this.activeAnalyzer = new Analyzer(this);
        this.mediaPlayer = MediaPlayer.create(this.mainActivity, R.raw.alarm);
        this.mediaPlayer.setLooping(true);
        this.imageAnalysis.setAnalyzer(this.executor, this.activeAnalyzer);
    }

    public ScanData stopScan() {
        this.imageAnalysis.clearAnalyzer();
        this.activeAnalyzer = null;
        this.mediaPlayer.release();
        this.mediaPlayer = null;
        this.resultView.setResults(new ArrayList<>(), new ArrayList<>());
        this.resultView.invalidate();
        return this.partialScan;
    }

    public void destroy() {
        this.executor.execute(() -> {
            this.objectDetectionModel.destroy();
            this.objectDetectionModel = null;
        });
    }

    public void switchToTesting() {
        this.executor.execute(() -> {
            this.objectDetectionModel.destroy();
            this.objectDetectionModel = new TestModel(this.mainActivity);
        });
    }

    public static class Analyzer implements ImageAnalysis.Analyzer {
        private final ScanCreator scanCreator;
        private boolean started;

        public Analyzer(ScanCreator scanCreator) {
            this.scanCreator = scanCreator;
        }

        @Override
        public void analyze(@NonNull ImageProxy image) {
            Bitmap bitmap = image.toBitmap();
            image.close();

            boolean firstFrame = false;
            if (!started) {
                started = true;
                firstFrame = true;
                scanCreator.mainActivity.runOnUiThread(() -> {
                    if (scanCreator.activeAnalyzer == this) {
                        scanCreator.handleFirstFrame(bitmap);
                    }
                });
            }

            Log.d("Scan", "Start analysis");
            List<Result> results = scanCreator.objectDetectionModel.analyzeImage(bitmap);
            Log.d("Scan", "End analysis with " + results.size() + " results");

            if (!firstFrame) {
                bitmap.recycle();
            }

            scanCreator.mainActivity.runOnUiThread(() -> {
                if (scanCreator.activeAnalyzer == this) {
                    scanCreator.handleResults(results);
                }
            });
        }
    }
}