package com.example.portascanner;

import static android.graphics.Color.argb;
import static android.graphics.Color.rgb;

import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.List;

// Inspired by https://github.com/pa7/heatmap.js
public class HeatmapPainter {

    private final int[] colorPalette;

    public HeatmapPainter() {
        this.colorPalette = this.createColorPalette();
    }

    private int[] createColorPalette() {
        Bitmap bitmap = Bitmap.createBitmap(256, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        LinearGradient gradient = new LinearGradient(0, 0, 256, 1,
                new int[]{rgb(0, 0, 255), rgb(0, 255, 0), rgb(255, 255, 0), rgb(255, 0, 0)},
                new float[]{0.25f, 0.55f, 0.85f, 1.0f},
                Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(gradient);
        canvas.drawLine(0, 0, 256, 1, paint);
        int[] palette = new int[256];
        bitmap.getPixels(palette, 0, 256, 0, 0, 256, 1);
        bitmap.recycle();
        return palette;
    }

    private void paintAlpha(Canvas canvas, List<Point> points) {
        int radius = 40;
        int alpha = 100;
        float blur = 0.5f;

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        for (Point point : points) {
            RadialGradient gradient = new RadialGradient(point.x, point.y, radius * blur,
                    new int[]{argb(alpha, 0, 0, 0), argb(0, 0, 0, 0)},
                    null, Shader.TileMode.CLAMP);
            paint.setShader(gradient);
            canvas.drawCircle(point.x, point.y, (float) (2 * radius), paint);
        }
    }

    public Bitmap paint(int width, int height, List<Point> points) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(argb(0, 0,0,0), BlendMode.CLEAR);

        this.paintAlpha(canvas, points);

        int[] image = new int[width * height];
        bitmap.getPixels(image, 0, width, 0, 0, width, height);

        for (int i = 0; i < image.length; i++) {
            int alpha = Color.alpha(image[i]);
            if (alpha != 0) {
                image[i] = this.colorPalette[alpha];
            }
        }

        bitmap.setPixels(image, 0, width, 0, 0, width, height);

        return bitmap;
    }
}
