package com.example.portascanner;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

import android.net.Uri;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ViewScansScreen {
    private final MainActivity activity;

    public ViewScansScreen(MainActivity activity) {
        this.activity = activity;
        List<String> scanFileNames = new ArrayList<>();

        this.activity.setContentView(R.layout.view_scans);

        for (File file : Objects.requireNonNull(this.activity.scansDir().listFiles())) {
            String name = file.getName();
            if (!name.endsWith(".json")) {
                continue;
            }
            scanFileNames.add(name.substring(0, name.length() - 5));
        }

        this.activity.requireViewById(R.id.back_btn).setOnClickListener(v -> this.activity.screen = new MainScreen(this.activity));

        LinearLayout grid = this.activity.requireViewById(R.id.grid);
        for (String name : scanFileNames) {
            try {
                JSONObject jsonReader = new JSONObject(new String(Files.readAllBytes(new File(this.activity.scansDir(), name + ".json").toPath()), StandardCharsets.UTF_8));
                String title = jsonReader.getString("title");
                long timestamp = jsonReader.getLong("time");

                CardView cardView = new CardView(this.activity);
                ViewGroup.MarginLayoutParams params1 = new LinearLayout.LayoutParams(
                        (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 150, this.activity.getResources().getDisplayMetrics()),
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params1.setMargins(5, 5, 5, 5);

                cardView.setLayoutParams(params1);

                LinearLayout layout = new LinearLayout(this.activity);
                layout.setOrientation(LinearLayout.VERTICAL);

                ImageView imageView = new ImageView(this.activity);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 150, this.activity.getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 150, this.activity.getResources().getDisplayMetrics())
                        ));
                imageView.setImageURI(Uri.fromFile(new File(this.activity.scansDir(), name + ".jpeg")));

                TextView titleView = new TextView(this.activity);
                titleView.setText(title);

                TextView timestampView = new TextView(this.activity);
                timestampView.setText(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                timestampView.setTextColor(this.activity.getResources().getColor(R.color.grey));

                cardView.addView(layout);
                layout.addView(imageView);
                layout.addView(titleView);
                layout.addView(timestampView);


                grid.addView(cardView);

            } catch (IOException | JSONException e) {
                new File(this.activity.scansDir(), name + ".json").delete();
                new File(this.activity.scansDir(), name + ".jpeg").delete();

                e.printStackTrace();
            }
        }
    }
}
