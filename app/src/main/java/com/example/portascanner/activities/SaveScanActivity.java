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
import com.example.portascanner.scans.Scan;
import com.example.portascanner.scans.ScanData;
import com.example.portascanner.scans.ScanRepository;

import java.time.Instant;

public class SaveScanActivity extends AppCompatActivity {
    public static ScanData SCAN_TO_SAVE;

    private ScanData scanData;
    private Bitmap imgPreview;
    private long unixTimestamp;
    private EditText titleTxt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SCAN_TO_SAVE == null) {
            this.finish();
            return;
        }

        this.scanData = SCAN_TO_SAVE;
        SCAN_TO_SAVE = null;
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

        this.setContentView(R.layout.save_scan);
        ImageView imageView = this.requireViewById(R.id.scan_img);
        imageView.setImageBitmap(imgPreview);
        this.titleTxt = this.requireViewById(R.id.title_txt);
        this.titleTxt.setHint(Scan.getFormattedTimestamp(this.unixTimestamp));

        // Initialise buttons
        this.requireViewById(R.id.save_scan_btn).setOnClickListener(v -> this.save());
        this.requireViewById(R.id.delete_scan_btn).setOnClickListener(v -> this.finish());
    }

    void save() {
        String title = this.titleTxt.getText().toString();
        if (title.isEmpty()) {
            title = this.titleTxt.getHint().toString();
        }
        String desc = this.<EditText>requireViewById(R.id.desc_txt).getText().toString();

        Scan scan = new Scan();
        scan.title = title;
        scan.desc = desc;
        scan.unixTimestamp = unixTimestamp;
        scan.scanData = this.scanData;

        ScanRepository.INSTANCE.save(scan);

        this.finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.scanData.image.recycle();
        this.imgPreview.recycle();
    }
}
