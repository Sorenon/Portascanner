package com.example.portascanner.activities;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.portascanner.R;
import com.example.portascanner.Scan;

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

public class ViewScansActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        List<String> scanFileNames = new ArrayList<>();

        this.setContentView(R.layout.view_scans);

        for (File file : Objects.requireNonNull(MainActivity.scansDir(this).listFiles())) {
            String name = file.getName();
            if (!name.endsWith(".json")) {
                continue;
            }
            scanFileNames.add(name.substring(0, name.length() - 5));
        }

        this.requireViewById(R.id.back_btn).setOnClickListener(v -> this.finish());

        LinearLayout grid = this.requireViewById(R.id.grid);
        for (String name : scanFileNames) {
            try {
                Scan scan = Scan.load(name, MainActivity.scansDir(this));

                CardView cardView = new CardView(this);
                ViewGroup.MarginLayoutParams params1 = new LinearLayout.LayoutParams(
                        (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 150, this.getResources().getDisplayMetrics()),
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params1.setMargins(5, 5, 5, 5);

                cardView.setLayoutParams(params1);

                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);

                ImageView imageView = new ImageView(this);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 150, this.getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 150, this.getResources().getDisplayMetrics())
                ));
                imageView.setImageBitmap(scan.scanData.image);

                TextView titleView = new TextView(this);
                titleView.setText(scan.title);

                TextView timestampView = new TextView(this);
                timestampView.setText(LocalDateTime.ofEpochSecond(scan.unixTimestamp, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                timestampView.setTextColor(ContextCompat.getColor(this.getBaseContext(), R.color.grey));

                cardView.addView(layout);
                layout.addView(imageView);
                layout.addView(titleView);
                layout.addView(timestampView);

                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ViewScanActivity.class);
                    intent.putExtra("com.example.portascanner.scan_name", name);
                    this.startActivity(intent);
                });

                grid.addView(cardView);

            } catch (IOException | JSONException e) {
                new File(MainActivity.scansDir(this), name + ".json").delete();
                new File(MainActivity.scansDir(this), name + ".jpeg").delete();

                e.printStackTrace();
            }
        }

    }
}
