package com.example.portascanner;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.JsonWriter;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class SaveScanScreen {
    private final MainActivity activity;
    private final Bitmap img;
    private final long unixTimestamp;
    private final String timestamp;
    private final EditText titleTxt;

    public SaveScanScreen(MainActivity activity, Bitmap img) {
        this.activity = activity;
        this.img = img;
        this.unixTimestamp = Instant.now().getEpochSecond();
        this.timestamp = LocalDateTime.ofEpochSecond(this.unixTimestamp, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        this.activity.setContentView(R.layout.save_scan);
        ImageView imageView = this.activity.requireViewById(R.id.scan_img);
        imageView.setImageBitmap(img);
        this.titleTxt = this.activity.requireViewById(R.id.title_txt);
        this.titleTxt.setHint(this.timestamp);

        // Initialise buttons
        this.activity.requireViewById(R.id.save_scan_btn).setOnClickListener(v -> this.save());
        this.activity.requireViewById(R.id.delete_scan_btn).setOnClickListener(v -> this.close());
    }

    void save() {
        String title = this.titleTxt.getText().toString();
        if (title.length() == 0) {
            title = this.timestamp;
        }
        String desc = this.activity.<EditText>requireViewById(R.id.desc_txt).getText().toString();

        File jsonFile = new File(this.activity.scansDir(), timestamp + ".json");
        File jpegFile = new File(this.activity.scansDir(), timestamp + ".jpeg");

        try {
            saveBitmapAsJpeg(this.img, Files.newOutputStream(jpegFile.toPath()));

            FileWriter fileWriter = new FileWriter(jsonFile);
            JsonWriter jsonWriter = new JsonWriter(fileWriter);
            jsonWriter.beginObject();
            jsonWriter.name("title").value(title);
            jsonWriter.name("desc").value(desc);
            jsonWriter.name("time").value(this.unixTimestamp);
            jsonWriter.endObject();
            fileWriter.close();
        } catch (Exception e) {
            jpegFile.delete();
            jsonFile.delete();
            throw new RuntimeException(e);
        }

        this.close();
    }

    void close() {
        this.img.recycle();
        this.activity.screen = new MainScreen(this.activity);
    }

    private void exportBitmap(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/portascanner");
        values.put(MediaStore.Images.Media.IS_PENDING, true);

        Uri uri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try {
                saveBitmapAsJpeg(bitmap, activity.getContentResolver().openOutputStream(uri));
                values.put(MediaStore.Images.Media.IS_PENDING, false);
                activity.getContentResolver().update(uri, values, null, null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveBitmapAsJpeg(@NonNull Bitmap bitmap, OutputStream outputStream) throws IOException {
        if (outputStream != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            outputStream.close();
        }
    }
}
