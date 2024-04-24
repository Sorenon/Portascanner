package com.example.portascanner.scans;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.portascanner.R;
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
    /**
     *  To avoid synchronisation issues, uses of {@code objectDetectionModel} must take place on the thread owned by {@code objectDetectionModelExecutor}
     */
    private ObjectDetectionModel objectDetectionModel;

    private final Executor objectDetectionThread;
    private final Activity activity;
    private final ResultView resultView;
    private final ImageAnalysis imageAnalysis;
    private final List<Long> queuedSensorReadings;
    private final List<Point> sensorPositions;

    private MediaPlayer mediaPlayer;
    private ScanResults partialScan;
    private long lastScannerPositionTime;
    private Analyzer activeAnalyzer;

    public ScanCreator(Activity activity, ResultView resultView, ImageAnalysis imageAnalysis) {
        this.activity = activity;
        this.resultView = resultView;
        this.imageAnalysis = imageAnalysis;
        this.objectDetectionThread = Executors.newSingleThreadExecutor();
        this.objectDetectionThread.execute(() -> {
            try {
                this.objectDetectionModel = new ObjectDetectionModel(this.activity);
            } catch (OrtException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.queuedSensorReadings = new ArrayList<>();
        this.sensorPositions = new ArrayList<>();

        // Initialise a thread to schedule fake sensor readings at random intervals
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
        this.partialScan = new ScanResults();
        this.partialScan.points = new ArrayList<>();
        this.partialScan.image = bitmap;
        this.resultView.scaleX = (float) this.resultView.getHeight() / bitmap.getHeight();
        this.resultView.scaleY = (float) this.resultView.getWidth() / bitmap.getWidth();
        this.lastScannerPositionTime = System.currentTimeMillis();
    }

    private void handleResults(List<Result> scannerPositions) {
        long now = System.currentTimeMillis();
        Result bestResult = this.findBestResult(now, scannerPositions);

        if (bestResult == null) {
            // If its been >3s since the latest scanner position, play a sound to alert the user
            if (now - this.lastScannerPositionTime > 3000) {
                this.mediaPlayer.start();
            }

            // If its been >500ms since the latest scanner position, start dropping results or use the latest scanner position
            if (now - lastScannerPositionTime > 500) {
                this.queuedSensorReadings.removeIf(readingTime -> {
                    // If the reading was <500ms after the latest scanner position, use the latest scanner position
                    if (!this.sensorPositions.isEmpty() && readingTime - lastScannerPositionTime < 500) {
                        Point lastPosition = this.sensorPositions.get(this.sensorPositions.size() - 1);
                        this.partialScan.points.add(lastPosition);
                        return true;
                    }
                    // Drop the reading if it's >500ms old
                    return now - readingTime > 500;
                    // Otherwise wait for new scanner positions
                });
            }
        } else {
            if (this.mediaPlayer.isPlaying()) {
                this.mediaPlayer.pause();
                this.mediaPlayer.seekTo(0);
            }

            Point bestPosition = new Point(bestResult.rect.centerX(), bestResult.rect.centerY());

            // If it's been <500ms since the previous scanner position, interpolate between the old and new positions
            if (!this.sensorPositions.isEmpty() && now - this.lastScannerPositionTime < 500) {
                Point lastPosition = this.sensorPositions.get(this.sensorPositions.size() - 1);

                for (long readingTime : this.queuedSensorReadings) {
                    if (readingTime < this.lastScannerPositionTime) {
                        this.partialScan.points.add(lastPosition);
                        continue;
                    }

                    // 1 = now
                    // 0 = lastResultTime
                    float invLerp = 1 - (float) (now - readingTime) / (float) (now - this.lastScannerPositionTime);

                    int x = (int) (lastPosition.x * (1 - invLerp) + bestPosition.x * invLerp);
                    int y = (int) (lastPosition.y * (1 - invLerp) + bestPosition.y * invLerp);
                    this.partialScan.points.add(new Point(x, y));
                }
            } else {
                for (long readingTime : this.queuedSensorReadings) {
                    // Only accept readings which are <500ms old
                    if (now - readingTime < 500) {
                        this.partialScan.points.add(bestPosition);
                    }
                }
            }

            this.lastScannerPositionTime = now;
            this.sensorPositions.add(bestPosition);

            this.queuedSensorReadings.clear();
        }

        this.resultView.setResults(scannerPositions, partialScan.points, this.findBestResult(System.currentTimeMillis(), scannerPositions));
        this.resultView.invalidate();
    }

    /**
     * As the model will sometimes return more than one position this method can be used to try select
     * the result most likely to be the sensor head.
     * @param timeMillis the time in milliseconds as provided by {@code System.currentTimeMillis()}
     * @param results the list of results to search
     * @return the best result
     */
    public Result findBestResult(long timeMillis, List<Result> results) {
        if (results.isEmpty()) {
            return null;
        }

        Result bestResult = null;
        if (timeMillis - this.lastScannerPositionTime < 1000 && results.size() > 1 && !this.sensorPositions.isEmpty()) {
            // If the last result was less than a second ago, select the closest result to the previous sensor position
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
            // Otherwise select the result with the highest confidence
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
        this.imageAnalysis.setAnalyzer(this.objectDetectionThread, this.activeAnalyzer);
    }

    public ScanResults stopScan() {
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
        this.objectDetectionThread.execute(() -> {
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
