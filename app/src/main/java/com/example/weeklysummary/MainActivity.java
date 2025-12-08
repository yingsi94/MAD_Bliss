package com.example.weeklysummary;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Find the Chart
        BarChart barChart = findViewById(R.id.barChart);

        // 2. Create the Data (The "Buckets")
        // BarEntry(x-position, y-value)
        ArrayList<BarEntry> bars = new ArrayList<>();
        bars.add(new BarEntry(1, 420));
        bars.add(new BarEntry(2, 475));
        bars.add(new BarEntry(3, 508));
        bars.add(new BarEntry(4, 660));

        BarDataSet barDataSet = new BarDataSet(bars, "Total Points");
        barDataSet.setColor(R.color.purple_bar_chart);
        // 1. Hide the Y-Axis Details (Left Side)
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawLabels(false);      // Hides the numbers (400, 450, 500, etc.)
        leftAxis.setDrawAxisLine(false);    // Hides the thick vertical line on the left
        leftAxis.setDrawGridLines(false);   // Hides the horizontal grid lines

// 2. Hide the Right Y-Axis Entirely (since it mirrors the left in your image)
        barChart.getAxisRight().setEnabled(false);

// 3. Hide the X-Axis Details (Bottom)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setDrawLabels(false);         // Hides any labels or tick marks under the bars
        xAxis.setDrawAxisLine(false);       // Hides the thick horizontal line at the bottom
        xAxis.setDrawGridLines(false);      // Hides the vertical grid lines
        barChart.invalidate();
        barChart.getDescription().setEnabled(false); // Hides the description label

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.animateY(2000); // Cool animation
        barChart.invalidate();
    }
}