package com.example.portascanner.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.portascanner.R;
import com.example.portascanner.Scan;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class ViewScanActivity extends Activity {
    // TODO, init, heatmap, export, delete, zoom

    private Scan scan;
    private Bitmap imgPreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.scan_details);

        File dir = MainActivity.scansDir(this);
        String scanName = this.getIntent().getStringExtra("com.example.portascanner.scan_name");

        try {
            this.scan = Scan.load(scanName, dir);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

        this.imgPreview = scan.scanData.image.copy(Bitmap.Config.RGB_565, true);

        Paint mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.RED);
        mPaintRectangle.setStrokeWidth(5);
        mPaintRectangle.setStyle(Paint.Style.STROKE);

        Canvas canvas = new Canvas(imgPreview);
        for (Point point : scan.scanData.points) {
            canvas.drawCircle(point.x, point.y, 10, mPaintRectangle);
        }

        ImageView imageView = this.requireViewById(R.id.scan_img1);
        ImageView imageView2 = this.requireViewById(R.id.scan_img2);

        imageView.setImageBitmap(scan.scanData.image);
        imageView2.setImageBitmap(imgPreview);

        this.requireViewById(R.id.close_btn).setOnClickListener(v -> this.finish());
        this.requireViewById(R.id.delete_btn).setOnClickListener(v -> {
            this.finish();

            new File(dir, scanName + ".json").delete();
            new File(dir, scanName + ".jpeg").delete();
        });
        this.requireViewById(R.id.export_btn).setOnClickListener(v -> {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Scan.saveImage(this, this.imgPreview));
            shareIntent.setType("image/jpg");
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, null));
        });
    }
}
