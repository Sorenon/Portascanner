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
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScanCreator {
    private ObjectDetectionModel objectDetectionModel;

    private final Executor executor;
    private final MainActivity mainActivity;
    private final ResultView resultView;
    private final ImageAnalysis imageAnalysis;
    private final List<Long> queuedSensorReadings;
    private final List<Point> sensorPositions;

    private MediaPlayer mediaPlayer;
    private ScanData partialScan;
    private long lastResultTime;
    private Analyzer activeAnalyzer;

    public ScanCreator(MainActivity mainActivity, ResultView resultView, ImageAnalysis imageAnalysis) {
        this.mainActivity = mainActivity;
        this.resultView = resultView;
        this.imageAnalysis = imageAnalysis;
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.execute(() -> this.objectDetectionModel = new ObjectDetectionModel(this.mainActivity));
        this.queuedSensorReadings = new ArrayList<>();
        this.sensorPositions = new ArrayList<>();

        Random random = new Random();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(Math.max((long) (1000 + random.nextGaussian() * 500), 0));
                } catch (InterruptedException ignored) {
                }
                long readingTime = System.currentTimeMillis();
                this.mainActivity.runOnUiThread(() -> {
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
            this.mediaPlayer.pause();
            this.mediaPlayer.seekTo(0);

            Point selectedResult = new Point();
            if (now - lastResultTime < 1000 && results.size() > 1 && !this.sensorPositions.isEmpty()) {
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

        this.resultView.setResults(results, partialScan.points);
        this.resultView.invalidate();
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
        this.sensorPositions.clear();
        this.queuedSensorReadings.clear();
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
