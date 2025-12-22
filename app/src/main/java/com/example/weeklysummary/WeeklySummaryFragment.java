package com.example.weeklysummary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bliss.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;

public class WeeklySummaryFragment extends Fragment {

    public WeeklySummaryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载你提供的 XML 布局
        return inflater.inflate(R.layout.fragment_weekly_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Find the Chart and Views
        BarChart barChart = view.findViewById(R.id.barChart);
        ImageView btnBack = view.findViewById(R.id.btnBack);
        TextView tvBack = view.findViewById(R.id.tvBack);

        // 处理返回逻辑
        View.OnClickListener backListener = v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        };
        btnBack.setOnClickListener(backListener);
        tvBack.setOnClickListener(backListener);

        // 2. Configure Chart Appearance
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        // Configure X-Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);

        // Configure Y-Axes
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);

        // 3. Create the Data
        ArrayList<BarEntry> bars = new ArrayList<>();
        bars.add(new BarEntry(1, 420));
        bars.add(new BarEntry(2, 475));
        bars.add(new BarEntry(3, 508));
        bars.add(new BarEntry(4, 660));

        // 4. Create DataSet and set properties
        BarDataSet barDataSet = new BarDataSet(bars, "Total Points");
        // 使用 requireContext() 替代 this
        barDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.purple_bar_chart));
        barDataSet.setDrawValues(false);

        // 5. Create BarData and set it to the chart
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.setFitBars(true);

        // 6. Animate and Refresh
        barChart.animateY(2000);
        barChart.invalidate();
    }
}