package com.example.portascanner.scans;

import android.app.Activity;
import android.content.Context;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ai.onnxruntime.OrtException;

public class ScanCreator {
    private ObjectDetectionModel objectDetectionModel;

    private final Executor executor;
    private final Activity activity;
    private final ResultView resultView;
    private final ImageAnalysis imageAnalysis;
    private final List<Long> queuedSensorReadings;
    private final List<Point> sensorPositions;

    private MediaPlayer mediaPlayer;
    private ScanData partialScan;
    private long lastResultTime;
    private Analyzer activeAnalyzer;

    public ScanCreator(Activity activity, ResultView resultView, ImageAnalysis imageAnalysis) {
        this.activity = activity;
        this.resultView = resultView;
        this.imageAnalysis = imageAnalysis;
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.execute(() -> {
            try {
                this.objectDetectionModel = new ObjectDetectionModel(this.activity);
            } catch (OrtException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.queuedSensorReadings = new ArrayList<>();
        this.sensorPositions = new ArrayList<>();

        // Init thing for fake points
        Random random = new Random();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(Math.max((long) (1000 + random.nextGaussian() * 500), 0));
                } catch (InterruptedException ignored) {
                }
                long readingTime = System.currentTimeMillis();
                this.activity.runOnUiThread(() -> {
                    if (this.activeAnalyzer != null) {
                        this.queuedSensorReadings.add(readingTime);
                    }
                });
            }
        }).start();
    }

    private void handleFirstFrame(Bitmap bitmap) {
        this.partialScan = new ScanData();
        this.partialScan.points = new ArrayList<>();
        this.partialScan.image = bitmap;
        this.resultView.scaleX = (float) this.resultView.getHeight() / bitmap.getHeight();
        this.resultView.scaleY = (float) this.resultView.getWidth() / bitmap.getWidth();
        this.lastResultTime = System.currentTimeMillis();
    }

    private void handleResults(List<Result> results) {
        long now = System.currentTimeMillis();

        if (results.isEmpty()) {
            // If its been >3s since the last result, play a sound to alert the user
            if (now - this.lastResultTime > 3000) {
                this.mediaPlayer.start();
            }

            // If its been >500ms since the last result, start dropping results or use the position of the last result
            if (now - lastResultTime > 500) {
                this.queuedSensorReadings.removeIf(readingTime -> {
                    // If the reading was <500ms after the last result, use the last result
                    if (!this.sensorPositions.isEmpty() && readingTime - lastResultTime < 500) {
                        Point lastPosition = this.sensorPositions.get(this.sensorPositions.size() - 1);
                        this.partialScan.points.add(lastPosition);
                        return true;
                    }
                    // Drop the reading if it's >500ms old, otherwise wait for a new result
                    return now - readingTime > 500;
                });
            }
        } else {
            if (this.mediaPlayer.isPlaying()) {
                this.mediaPlayer.pause();
                this.mediaPlayer.seekTo(0);
            }

            Result bestResult = this.findBestResult(now, results);
            Point selectedResult = new Point(bestResult.rect.centerX(), bestResult.rect.centerY());

            // If it's been <500ms since the last result, interpolate between the two positions
            if (!this.sensorPositions.isEmpty() && now - this.lastResultTime < 500) {
                Point lastPosition = this.sensorPositions.get(this.sensorPositions.size() - 1);

                for (long readingTime : this.queuedSensorReadings) {
                    if (readingTime < this.lastResultTime) {
                        this.partialScan.points.add(lastPosition);
                        continue;
                    }

                    // 1 = now
                    // 0 = lastResultTime
                    float iLerp = 1 - (float) (now - readingTime) / (float) (now - lastResultTime);

                    int x = (int) (lastPosition.x * (1 - iLerp) + selectedResult.x * iLerp);
                    int y = (int) (lastPosition.y * (1 - iLerp) + selectedResult.y * iLerp);
                    this.partialScan.points.add(new Point(x, y));
                }
            } else {
                for (long readingTime : this.queuedSensorReadings) {
                    // Only accept readings which are <500ms old
                    if (now - readingTime < 500) {
                        this.partialScan.points.add(selectedResult);
                    }
                }
            }

            this.lastResultTime = now;
            this.sensorPositions.add(selectedResult);

            this.queuedSensorReadings.clear();
        }

        this.resultView.setResults(results, partialScan.points, this.findBestResult(System.currentTimeMillis(), results));
        this.resultView.invalidate();
    }

    public Result findBestResult(long currentTimeMillis, List<Result> results) {
        if (results.isEmpty()) {
            return null;
        }

        Result bestResult = null;
        if (currentTimeMillis - this.lastResultTime < 1000 && results.size() > 1 && !this.sensorPositions.isEmpty()) {
            Point lastPoint = this.sensorPositions.get(this.sensorPositions.size() - 1);

            int bestDistanceSqr = Integer.MAX_VALUE;
            for (Result result : results) {
                int resultX = result.rect.centerX();
                int resultY = result.rect.centerY();

                int distanceX = resultX - lastPoint.x;
                int distanceY = resultY - lastPoint.y;
                int distanceSqr = distanceX * distanceX + distanceY * distanceY;

                if (distanceSqr < bestDistanceSqr) {
                    bestDistanceSqr = distanceSqr;
                    bestResult = result;
                }
            }
        } else {
            bestResult = results.get(0);
            for (int i = 1; i < results.size(); i++) {
                Result result = results.get(i);
                if (result.score > bestResult.score) {
                    bestResult = result;
                }
            }
        }
        return bestResult;
    }

    public void startScan() {
        this.activeAnalyzer = new Analyzer(this);
        this.mediaPlayer = MediaPlayer.create(this.activity, R.raw.alarm);
        this.mediaPlayer.setLooping(true);
        this.imageAnalysis.setAnalyzer(this.executor, this.activeAnalyzer);
    }

    public ScanData stopScan() {
        this.imageAnalysis.clearAnalyzer();
        this.activeAnalyzer = null;
        this.mediaPlayer.release();
        this.mediaPlayer = null;
        this.resultView.setResults(Collections.emptyList(), Collections.emptyList(), null);
        this.resultView.invalidate();
        this.sensorPositions.clear();
        this.queuedSensorReadings.clear();
        return this.partialScan;
    }

    public void destroy() {
        this.executor.execute(() -> {
            try {
                this.objectDetectionModel.destroy();
            } catch (OrtException e) {
                throw new RuntimeException(e);
            }
            this.objectDetectionModel = null;
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
                scanCreator.activity.runOnUiThread(() -> {
                    if (scanCreator.activeAnalyzer == this) {
                        scanCreator.handleFirstFrame(bitmap);
                    }
                });
            }
            long start = System.currentTimeMillis();

            Log.d("Scanning", "Start analysis");
            try {
                List<Result> results = scanCreator.objectDetectionModel.run(bitmap);

                Log.d("Scanning", "End analysis with " + results.size() + " results");

                long dur = System.currentTimeMillis() - start;
                Log.i("Analysis Time Taken", "Time Taken: " + dur + "ms");

                if (!firstFrame) {
                    bitmap.recycle();
                }

                scanCreator.activity.runOnUiThread(() -> {
                    if (scanCreator.activeAnalyzer == this) {
                        scanCreator.handleResults(results);
                    }
                });
            } catch (OrtException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
