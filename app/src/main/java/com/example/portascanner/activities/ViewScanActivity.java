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
import androidx.core.content.FileProvider;

import com.example.portascanner.R;
import com.example.portascanner.scans.Scan;
import com.example.portascanner.scans.ScanRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ViewScanActivity extends Activity {
    // TODO, heatmap, zoom

    private static final ScanRepository SCAN_REPOSITORY = ScanRepository.INSTANCE;
    private Bitmap imgPreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.scan_details);

        String scanName = this.getIntent().getStringExtra("com.example.portascanner.scan_name");

        Scan scan = SCAN_REPOSITORY.getScans().get(scanName);

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
            SCAN_REPOSITORY.delete(scanName);
            this.finish();
        });
        this.requireViewById(R.id.export_btn).setOnClickListener(v -> {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, saveImage(this.imgPreview));
            shareIntent.setType("image/jpg");
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, null));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.imgPreview.recycle();
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
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.example.portascanner.fileprovider", file);

        } catch (IOException e) {
            Log.d("Scan IO", "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }
}
