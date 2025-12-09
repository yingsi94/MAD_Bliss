package com.example.mooddistribution;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DonutPieChart extends View {

    private Paint slicePaint;
    private Paint centerPaint;
    private Paint textPaint;
    private List<Segment> segments = new ArrayList<>();
    private float totalValue = 0;

    // Simple data holder
    public static class Segment {
        float value;
        int color;
        String label;
        // Scale factor: 1.0 = big (purple), 0.9 = medium (red), 0.8 = small
        float scale;

        public Segment(float value, String colorHex, float scale) {
            this.value = value;
            this.color = Color.parseColor(colorHex);
            this.scale = scale;
        }
    }

    public DonutPieChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 1. Paint for the slices
        slicePaint = new Paint();
        slicePaint.setAntiAlias(true);
        slicePaint.setStyle(Paint.Style.FILL);

        // 2. Paint for the white hole
        centerPaint = new Paint();
        centerPaint.setColor(Color.WHITE);
        centerPaint.setAntiAlias(true);

        // 3. Paint for the text
        textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(60f);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
    }

    public void setSegments(List<Segment> data) {
        this.segments = data;
        this.totalValue = 0;
        for (Segment s : data) totalValue += s.value;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (segments.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();
        int minDim = Math.min(width, height);

        // Center coordinates
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Base radius (leave padding)
        float maxRadius = (minDim / 2f) * 0.9f;

        float currentAngle = 270; // Start at top (12 o'clock)

        // DRAW SLICES
        for (Segment s : segments) {
            float sweepAngle = (s.value / totalValue) * 360f;

            // Adjust radius based on the 'scale' property to create the stepped look
            float currentRadius = maxRadius * s.scale;

            RectF bounds = new RectF(
                    centerX - currentRadius, centerY - currentRadius,
                    centerX + currentRadius, centerY + currentRadius
            );

            slicePaint.setColor(s.color);
            // useCenter=true makes it a pie slice (wedges), not just an arc line
            canvas.drawArc(bounds, currentAngle, sweepAngle, true, slicePaint);

            currentAngle += sweepAngle;
        }

        // DRAW CENTER HOLE (To make it a donut)
        float holeRadius = maxRadius * 0.45f; // Hole is 45% of size
        canvas.drawCircle(centerX, centerY, holeRadius, centerPaint);

        // DRAW CENTER TEXT
        // Calculate vertical centering
        float textHeightOffset = (textPaint.descent() + textPaint.ascent()) / 2;
        canvas.drawText("100", centerX, centerY - textHeightOffset, textPaint);
    }
}