package com.example.portascanner.activities;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

import android.content.Intent;
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
import com.example.portascanner.scans.Scan;
import com.example.portascanner.scans.ScanRepository;

import java.util.Map;

public class ScanGalleryActivity extends AppCompatActivity {

    private static final ScanRepository SCAN_REPOSITORY = ScanRepository.INSTANCE;
    private ScanRepository.Listener listener;
    private boolean needsReload;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.listener = SCAN_REPOSITORY.addListener(() -> this.needsReload = true);
        this.reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.needsReload) {
            this.needsReload = false;
            this.reload();
        }
    }

    private void reload() {
        this.setContentView(R.layout.view_scans);

        this.requireViewById(R.id.back_btn).setOnClickListener(v -> this.finish());

        LinearLayout grid = this.requireViewById(R.id.grid);
        for (Map.Entry<String, Scan> entry : SCAN_REPOSITORY.getScans().entrySet()) {
            String name = entry.getKey();
            Scan scan = entry.getValue();

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
            timestampView.setText(Scan.getFormattedTimestamp(scan.unixTimestamp));
            timestampView.setTextColor(ContextCompat.getColor(this.getBaseContext(), R.color.grey));

            cardView.addView(layout);
            layout.addView(imageView);
            layout.addView(titleView);
            layout.addView(timestampView);

            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(this, ScanDetailsActivity.class);
                intent.putExtra("com.example.portascanner.scan_name", name);
                this.startActivity(intent);
            });

            grid.addView(cardView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SCAN_REPOSITORY.removeListener(this.listener);
    }
}
