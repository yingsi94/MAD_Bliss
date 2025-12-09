package com.example.mooddistribution;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Pie;

import java.util.ArrayList;
import java.util.List;

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
        DonutPieChart chart = findViewById(R.id.donutChart);

        List<DonutPieChart.Segment> data = new ArrayList<>();

        // Scale 1.0 = Purple (Largest)
        // Scale 0.9 = Red (Medium)
        // Scale 0.8 = Orange/Teal (Smallest)

        // Purple Slice (~45%)
        data.add(new DonutPieChart.Segment(45, "#7B61FF", 1.0f));

        // Orange Slice (~10%)
        data.add(new DonutPieChart.Segment(10, "#FFB74D", 0.8f));

        // Teal Slice (~10%)
        data.add(new DonutPieChart.Segment(10, "#4DD0E1", 0.8f));

        // Red Slice (~35%)
        data.add(new DonutPieChart.Segment(35, "#FF6B6B", 0.9f));

        chart.setSegments(data);
    }
}