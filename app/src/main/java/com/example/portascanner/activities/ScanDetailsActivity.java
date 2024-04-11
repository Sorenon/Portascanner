package com.example.portascanner.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.FileProvider;

import com.example.portascanner.HeatmapPainter;
import com.example.portascanner.R;
import com.example.portascanner.scans.Scan;
import com.example.portascanner.scans.ScanRepository;
import com.ortiz.touchview.TouchImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ScanDetailsActivity extends AppCompatActivity {
    private static final ScanRepository SCAN_REPOSITORY = ScanRepository.INSTANCE;
    private static final HeatmapPainter heatmapPainter = new HeatmapPainter();

    private Bitmap heatmap;
    private Bitmap currentPreview;
    private SwitchCompat marker_sw;
    private SwitchCompat heatmap_sw;
    private TouchImageView imageView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.scan_details);

        String scanName = this.getIntent().getStringExtra("com.example.portascanner.scan_name");

        Scan scan = SCAN_REPOSITORY.getScans().get(scanName);
        if (scan == null) {
            this.finish();
            return;
        }

        Bitmap scanImage = scan.scanData.image;
        this.heatmap = heatmapPainter.paint(scanImage.getWidth(), scanImage.getHeight(), scan.scanData.points);
        this.imageView = this.requireViewById(R.id.scan_img);

        this.marker_sw = this.requireViewById(R.id.marker_sw);
        this.heatmap_sw = this.requireViewById(R.id.heatmap_sw);
        this.marker_sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            this.refreshImage(scan);
        });
        this.heatmap_sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            this.refreshImage(scan);
        });

        this.refreshImage(scan);
        this.requireViewById(R.id.close_btn).setOnClickListener(v -> this.finish());
        this.requireViewById(R.id.delete_btn).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Scan")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    SCAN_REPOSITORY.delete(scanName);
                    this.finish();
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show());
        this.requireViewById(R.id.export_btn).setOnClickListener(v -> {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, saveImage(this.currentPreview));
            shareIntent.setType("image/jpg");
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, null));
        });
        this.<TextView>requireViewById(R.id.title_txt).setText(scan.title);
        this.<TextView>requireViewById(R.id.timestamp_txt).setText(Scan.getPrettyFormattedTimestamp(scan.unixTimestamp));
        TextView descView = this.requireViewById(R.id.desc_txt);
        if (scan.desc.isEmpty()) {
            descView.setVisibility(View.GONE);
        } else {
            descView.setText(scan.desc);
        }
    }

    private void refreshImage(Scan scan) {
        this.currentPreview = scan.scanData.image.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(this.currentPreview);

        if (heatmap_sw.isChecked()) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(this.heatmap, 0, 0, paint);
        }
        if (marker_sw.isChecked()) {
            Paint mPaintRectangle = new Paint();
            mPaintRectangle.setColor(Color.RED);
            mPaintRectangle.setStrokeWidth(3);
            mPaintRectangle.setStyle(Paint.Style.STROKE);

            for (Point point : scan.scanData.points) {
                canvas.drawLines(new float[]{
                        point.x, point.y - 10, point.x, point.y + 10,
                        point.x - 10, point.y, point.x + 10, point.y
                }, mPaintRectangle);
            }
        }

        imageView.setImageBitmap(this.currentPreview);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.currentPreview.recycle();
        this.heatmap.recycle();
    }

    /**
     * Saves the image as PNG to the app's cache directory.
     *
     * @param image Bitmap to save.
     * @return Uri of the saved file or null
     */
    public Uri saveImage(Bitmap image) {
        //TODO - Should be processed in another thread
        File imagesFolder = new File(this.getCacheDir(), "shared_images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.jpg");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.example.portascanner.fileprovider", file);

        } catch (IOException e) {
            Log.d("Scan IO", "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }
}
