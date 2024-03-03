package com.example.portascanner.activities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.portascanner.R;
import com.example.portascanner.Scan;
import com.example.portascanner.ScanData;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class SaveScanActivity extends AppCompatActivity {
    private ScanData scanData;
    private Bitmap imgPreview;
    private long unixTimestamp;
    private String timestamp;
    private EditText titleTxt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MainActivity.scanData == null) {
            this.finish();
            return;
        }

        this.scanData = MainActivity.scanData;
        MainActivity.scanData = null;
        this.imgPreview = this.scanData.image.copy(Bitmap.Config.RGB_565, true);

        Paint mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.RED);
        mPaintRectangle.setStrokeWidth(5);
        mPaintRectangle.setStyle(Paint.Style.STROKE);

        Canvas canvas = new Canvas(imgPreview);
        for (Point point : this.scanData.points) {
            canvas.drawCircle(point.x, point.y, 10, mPaintRectangle);
        }

        this.unixTimestamp = Instant.now().getEpochSecond();
        this.timestamp = LocalDateTime.ofEpochSecond(this.unixTimestamp, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        this.setContentView(R.layout.save_scan);
        ImageView imageView = this.requireViewById(R.id.scan_img);
        imageView.setImageBitmap(imgPreview);
        this.titleTxt = this.requireViewById(R.id.title_txt);
        this.titleTxt.setHint(this.timestamp);

        // Initialise buttons
        this.requireViewById(R.id.save_scan_btn).setOnClickListener(v -> this.save());
        this.requireViewById(R.id.delete_scan_btn).setOnClickListener(v -> this.finish());
    }

    void save() {
        String title = this.titleTxt.getText().toString();
        if (title.length() == 0) {
            title = this.timestamp;
        }
        String desc = this.<EditText>requireViewById(R.id.desc_txt).getText().toString();

        Scan scan = new Scan();
        scan.title = title;
        scan.desc = desc;
        scan.unixTimestamp = unixTimestamp;
        scan.scanData = this.scanData;

        scan.save(timestamp, MainActivity.scansDir(this));

        this.finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.scanData.image.recycle();
        this.imgPreview.recycle();
    }
}
