package com.example.portascanner.activities;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
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
        this.setContentView(R.layout.scan_gallery);
        this.requireViewById(R.id.back_btn).setOnClickListener(v -> this.finish());
        ConstraintLayout grid = this.requireViewById(R.id.grid);
        Flow flow = this.requireViewById(R.id.flow);

        for (Map.Entry<String, Scan> entry : SCAN_REPOSITORY.getScans().entrySet()) {
            CardView cardView = makeCard(entry.getKey(), entry.getValue());
            cardView.setId(View.generateViewId());
            grid.addView(cardView);
            flow.addView(cardView);
        }
    }

    private CardView makeCard(String scanID, Scan scan) {
        float width = 200;

        CardView cardView = new CardView(this);
        ViewGroup.MarginLayoutParams cardLayoutParams = new LinearLayout.LayoutParams(
                (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, width, this.getResources().getDisplayMetrics()),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLayoutParams.setMargins(5, 5, 5, 5);
        cardView.setLayoutParams(cardLayoutParams);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, width, this.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, width * 3 / 4, this.getResources().getDisplayMetrics())
        ));
        imageView.setImageBitmap(scan.scanData.image);

        TextView titleView = new TextView(this);
        titleView.setText(scan.title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        TextView timestampView = new TextView(this);
        timestampView.setText(Scan.getPrettyFormattedTimestamp(scan.unixTimestamp));
        timestampView.setTextColor(ContextCompat.getColor(this.getBaseContext(), R.color.grey));
        timestampView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        cardView.addView(layout);
        layout.addView(imageView);
        layout.addView(titleView);
        layout.addView(timestampView);

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScanDetailsActivity.class);
            intent.putExtra("com.example.portascanner.scan_name", scanID);
            this.startActivity(intent);
        });
        return cardView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SCAN_REPOSITORY.removeListener(this.listener);
    }
}
