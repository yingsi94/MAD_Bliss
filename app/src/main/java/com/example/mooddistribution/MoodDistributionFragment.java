package com.example.mooddistribution;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.bliss.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodDistributionFragment extends Fragment {

    private DonutPieChart donutChart;
    private TextView tvDateRange, tvEmptyState;
    private FirebaseFirestore db;
    private Calendar displayCalendar;

    public MoodDistributionFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mood_distribution, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        displayCalendar = Calendar.getInstance();
        displayCalendar.setFirstDayOfWeek(Calendar.MONDAY);

        donutChart = view.findViewById(R.id.donutChart);
        tvDateRange = view.findViewById(R.id.tvDateRange);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        // --- 返回 Home 逻辑 ---
        View.OnClickListener backHomeListener = v -> {
            if (getActivity() != null) {
                // POP_BACK_STACK_INCLUSIVE 会弹出所有记录，直到回到 MainActivity 默认显示的 Home
                getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        };
        view.findViewById(R.id.btnBack).setOnClickListener(backHomeListener);
        view.findViewById(R.id.tvBack).setOnClickListener(backHomeListener);

        // --- 返回 Weekly Summary (上一级) ---
        view.findViewById(R.id.tabWeeklySummary).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        // 周切换
        view.findViewById(R.id.btnPrevWeek).setOnClickListener(v -> shiftWeek(-1));
        view.findViewById(R.id.btnNextWeek).setOnClickListener(v -> shiftWeek(1));

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
                    if (!isAdded()) return;
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
        if (total == 0) {
            donutChart.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            donutChart.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            List<DonutPieChart.Segment> segments = new ArrayList<>();
            addIf(segments, counts, "happy", "#8979FF", 1.0f, "Happy");
            addIf(segments, counts, "calm", "#FF928A", 0.9f, "Calm");
            addIf(segments, counts, "sad", "#3CC3DF", 0.85f, "Sad");
            addIf(segments, counts, "stressed", "#FFAE4C", 0.8f, "Stressed");
            donutChart.setSegments(segments);
        }
    }

    private void addIf(List<DonutPieChart.Segment> list, Map<String, Integer> counts, String key, String color, float scale, String label) {
        if (counts.containsKey(key) && counts.get(key) > 0) {
            list.add(new DonutPieChart.Segment(counts.get(key), color, scale, label));
        }
    }
}