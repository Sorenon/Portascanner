package com.example.portascanner.scans;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ScanRepository {
    public static ScanRepository INSTANCE;

    private final File scansDirectory;
    private final Set<Listener> listeners = new HashSet<>();
    private final Map<String, Scan> scans = new HashMap<>();

    public ScanRepository(Activity activity) {
        this.scansDirectory = new File(activity.getFilesDir(), "scans");
        this.scansDirectory.mkdir();
    }

    public void save(Scan scan) {
        String timestamp = LocalDateTime.ofEpochSecond(scan.unixTimestamp, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        File scanFile = getScanFile(timestamp + ".scan.zip");
        int i = 0;
        while (scanFile.exists()) {
            scanFile = getScanFile(timestamp + "_" + i++ + ".scan.zip");
        }

        try {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(scanFile.toPath()))) {
                zipOutputStream.putNextEntry(new ZipEntry("image.png"));
                scan.scanData.image.compress(Bitmap.CompressFormat.PNG, 100, zipOutputStream);

                zipOutputStream.putNextEntry(new ZipEntry("data.json"));
                try (JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(zipOutputStream))) {
                    jsonWriter.beginObject();
                    jsonWriter.name("title").value(scan.title);
                    jsonWriter.name("desc").value(scan.desc);
                    jsonWriter.name("time").value(scan.unixTimestamp);
                    jsonWriter.name("points").beginArray();
                    for (Point point : scan.scanData.points) {
                        jsonWriter.value(point.x).value(point.y);
                    }
                    jsonWriter.endArray();
                    jsonWriter.endObject();
                }
            }
        } catch (IOException e) {
            Log.d("Scan IO", "IOException while saving scan: " + e.getMessage());
        }

        if (!this.listeners.isEmpty()) {
            String scanName = scanFile.getName();
            this.scans.put(scanName, scan);
            for (Listener listener : this.listeners) {
                listener.onChange();
            }
        }
    }


    private static Scan loadScan(File file) throws IOException, JSONException {
        Scan scan = new Scan();
        scan.scanData = new ScanData();

        try (ZipFile zipFile = new ZipFile(file)) {
            scan.scanData.image = BitmapFactory.decodeStream(zipFile.getInputStream(new ZipEntry("image.png")));

            BufferedReader r = new BufferedReader(new InputStreamReader(zipFile.getInputStream(new ZipEntry("data.json"))));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }

            JSONObject jsonReader = new JSONObject(total.toString());

            scan.title = jsonReader.getString("title");
            scan.desc = jsonReader.getString("desc");
            scan.unixTimestamp = jsonReader.getLong("time");

            JSONArray arr = jsonReader.getJSONArray("points");
            int numPoints = arr.length() / 2;
            scan.scanData.points = new ArrayList<>(numPoints);
            for (int i = 0; i < numPoints; i++) {
                scan.scanData.points.add(new Point(arr.getInt(i * 2), arr.getInt(i * 2 + 1)));
            }
        }

        return scan;
    }

    public void delete(String name) {
        Scan scan = this.scans.remove(name);
        if (scan != null) {
            scan.scanData.image.recycle();
            for (Listener listener : this.listeners) {
                listener.onChange();
            }
        }
        getScanFile(name).delete();
    }

    public Listener addListener(Listener listener) {
        this.listeners.add(listener);
        if (this.listeners.size() > 1) {
            return listener;
        }

        File[] files = this.scansDirectory.listFiles();
        if (files == null) {
            return listener;
        }

        for (File scanFile : files) {
            try {
                Scan scan = loadScan(scanFile);
                this.scans.put(scanFile.getName(), scan);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return listener;
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
        if (!this.listeners.isEmpty()) {
            return;
        }

        for (Scan scan : this.scans.values()) {
            scan.scanData.image.recycle();
        }

        this.scans.clear();
    }

    public Map<String, Scan> getScans() {
        if (this.listeners.isEmpty()) {
            return null;
        }
        return this.scans;
    }

    private File getScanFile(String name) {
        return new File(this.scansDirectory, name);
    }

    public interface Listener {
        void onChange();
    }
}
