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
import com.example.mooddistribution.MoodDistributionFragment; // 确保导入了目标 Fragment
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
        // 加载布局文件
        return inflater.inflate(R.layout.fragment_weekly_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 初始化视图控件
        BarChart barChart = view.findViewById(R.id.barChart);
        ImageView btnBack = view.findViewById(R.id.btnBack);
        TextView tvBack = view.findViewById(R.id.tvBack);

        // 找到指向 Mood Distribution 的入口卡片 (请确保 XML 中 ID 为 cardMood)
        View cardMood = view.findViewById(R.id.cardMood);

        // 2. 处理返回逻辑
        View.OnClickListener backListener = v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        };
        btnBack.setOnClickListener(backListener);
        tvBack.setOnClickListener(backListener);

        // 3. 处理跳转逻辑 (点击卡片去 Mood Distribution)
        if (cardMood != null) {
            cardMood.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new MoodDistributionFragment())
                        .addToBackStack(null) // 加入回退栈，按返回键能回到本页
                        .commit();
            });
        }

        // 4. 配置图表外观
        setupBarChart(barChart);

        // 5. 填充图表数据
        setChartData(barChart);
    }

    private void setupBarChart(BarChart barChart) {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        // 配置 X 轴
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);

        // 配置 Y 轴
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
    }

    private void setChartData(BarChart barChart) {
        ArrayList<BarEntry> bars = new ArrayList<>();
        bars.add(new BarEntry(1, 420));
        bars.add(new BarEntry(2, 475));
        bars.add(new BarEntry(3, 508));
        bars.add(new BarEntry(4, 660));

        BarDataSet barDataSet = new BarDataSet(bars, "Total Points");

        // 设置柱状图颜色
        barDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.purple_bar_chart));
        barDataSet.setDrawValues(false);

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.setFitBars(true);

        // 动画刷新
        barChart.animateY(2000);
        barChart.invalidate();
    }
}