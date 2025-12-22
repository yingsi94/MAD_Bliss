package com.example.mooddistribution;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bliss.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;

public class Mood_Distribution extends AppCompatActivity {
    private DonutPieChart donutChart;
    private TextView tvDateRange;
    private FirebaseFirestore db;
    private Calendar displayCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_distribution);

        db = FirebaseFirestore.getInstance();
        displayCalendar = Calendar.getInstance();
        displayCalendar.setFirstDayOfWeek(Calendar.MONDAY);

        donutChart = findViewById(R.id.donutChart);
        tvDateRange = findViewById(R.id.tvDateRange);

        findViewById(R.id.btnPrevWeek).setOnClickListener(v -> shiftWeek(-1));
        findViewById(R.id.btnNextWeek).setOnClickListener(v -> shiftWeek(1));
        findViewById(R.id.tabWeeklySummary).setOnClickListener(v -> finish());

        loadDistributionData();
    }

    private void shiftWeek(int delta) {
        displayCalendar.add(Calendar.WEEK_OF_YEAR, delta);
        loadDistributionData();
    }

    private void loadDistributionData() {
        Calendar cal = (Calendar) displayCalendar.clone();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        long start = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_WEEK, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        tvDateRange.setText(sdf.format(new Date(start)) + " - " + sdf.format(new Date(end)));

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("mood_history")
                .whereGreaterThanOrEqualTo("time_millis", start)
                .whereLessThanOrEqualTo("time_millis", end)
                .get()
                .addOnSuccessListener(snapshots -> {
                    Map<String, Integer> counts = new HashMap<>();
                    int total = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String mood = doc.getString("mood");
                        if (mood != null) {
                            counts.put(mood.toLowerCase(), counts.getOrDefault(mood.toLowerCase(), 0) + 1);
                            total++;
                        }
                    }
                    updateChart(counts, total);
                });
    }

    private void updateChart(Map<String, Integer> counts, int total) {
        List<DonutPieChart.Segment> segments = new ArrayList<>();
        if (total > 0) {
            addIf(segments, counts, "happy", "#8979FF", 1.0f, "Happy");
            addIf(segments, counts, "calm", "#FF928A", 0.9f, "Calm");
            addIf(segments, counts, "sad", "#3CC3DF", 0.85f, "Sad");
            addIf(segments, counts, "stressed", "#FFAE4C", 0.8f, "Stressed");
        }
        donutChart.setSegments(segments);
    }

    private void addIf(List<DonutPieChart.Segment> list, Map<String, Integer> counts, String key, String color, float scale, String label) {
        if (counts.getOrDefault(key, 0) > 0) {
            list.add(new DonutPieChart.Segment(counts.get(key), color, scale, label));
        }
    }
}