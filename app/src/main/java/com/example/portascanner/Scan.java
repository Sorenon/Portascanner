package com.example.portascanner;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.JsonWriter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

public class Scan {
    public String title;
    public String desc;
    public long unixTimestamp;
    public ScanData scanData;

    public void save(String name, File dir) {
        File jsonFile = new File(dir, name + ".json");
        File jpegFile = new File(dir, name + ".jpeg");

        try {
            saveBitmapAsJpeg(scanData.image, Files.newOutputStream(jpegFile.toPath()));
            FileWriter fileWriter = new FileWriter(jsonFile);
            JsonWriter jsonWriter = new JsonWriter(fileWriter);
            jsonWriter.beginObject();
            jsonWriter.name("title").value(title);
            jsonWriter.name("desc").value(desc);
            jsonWriter.name("time").value(unixTimestamp);
            jsonWriter.name("points").beginArray();
            for (Point point : scanData.points) {
                jsonWriter.value(point.x).value(point.y);
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
            fileWriter.close();
        } catch (Exception e) {
            jpegFile.delete();
            jsonFile.delete();
            throw new RuntimeException(e);
        }
    }

    public static Scan load(String name, File dir) throws IOException, JSONException {
        Scan scan = new Scan();
        scan.scanData = new ScanData();

        JSONObject jsonReader = new JSONObject(new String(Files.readAllBytes(new File(dir, name + ".json").toPath()), StandardCharsets.UTF_8));
        scan.title = jsonReader.getString("title");
        scan.desc = jsonReader.getString("desc");
        scan.unixTimestamp = jsonReader.getLong("time");

        scan.scanData.image = BitmapFactory.decodeFile(new File(dir, name + ".jpeg").getPath());

        JSONArray arr = jsonReader.getJSONArray("points");
        int numPoints = arr.length() / 2;
        scan.scanData.points = new ArrayList<>(numPoints);
        for (int i = 0; i < numPoints; i++) {
            scan.scanData.points.add(new Point(arr.getInt(i * 2), arr.getInt(i * 2 + 1)));
        }

        return scan;
    }

    /**
     * Saves the image as PNG to the app's cache directory.
     * @param image Bitmap to save.
     * @return Uri of the saved file or null
     */
    public static Uri saveImage(Context context, Bitmap image) {
        //TODO - Should be processed in another thread
        File imagesFolder = new File(context.getCacheDir(), "shared_images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.jpg");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, "com.example.portascanner.fileprovider", file);

        } catch (IOException e) {
            Log.d("TESTTESTTEST", "IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }

    private void exportBitmap(ContentResolver contentResolver, Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/portascanner");
        values.put(MediaStore.Images.Media.IS_PENDING, true);

        Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try {
                saveBitmapAsJpeg(bitmap, contentResolver.openOutputStream(uri));
                values.put(MediaStore.Images.Media.IS_PENDING, false);
                contentResolver.update(uri, values, null, null);
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
