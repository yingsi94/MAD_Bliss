package com.example.mooddistribution;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class DonutPieChart extends View {
    private Paint slicePaint, centerPaint, textPaint, subTextPaint;
    private List<Segment> segments = new ArrayList<>();
    private float totalValue = 0;
    private int selectedIndex = -1;

    public static class Segment {
        public float value;
        public int color;
        public float scale;
        public String label;

        public Segment(float value, String colorHex, float scale, String label) {
            this.value = value;
            this.color = Color.parseColor(colorHex);
            this.scale = scale;
            this.label = label;
        }
    }

    public DonutPieChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slicePaint.setStyle(Paint.Style.FILL);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setColor(Color.WHITE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#1F2937"));
        textPaint.setTextSize(90f);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(Color.GRAY);
        subTextPaint.setTextSize(35f);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setSegments(List<Segment> data) {
        this.segments = data;
        this.totalValue = 0;
        this.selectedIndex = -1;
        for (Segment s : data) totalValue += s.value;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && totalValue > 0) {
            float x = event.getX() - getWidth() / 2f;
            float y = event.getY() - getHeight() / 2f;
            double angle = Math.toDegrees(Math.atan2(y, x));
            if (angle < 0) angle += 360;
            float adjustedAngle = (float) ((angle + 90) % 360);

            float tempAngle = 0;
            for (int i = 0; i < segments.size(); i++) {
                float sweep = (segments.get(i).value / totalValue) * 360f;
                if (adjustedAngle >= tempAngle && adjustedAngle <= tempAngle + sweep) {
                    selectedIndex = (selectedIndex == i) ? -1 : i;
                    invalidate();
                    return true;
                }
                tempAngle += sweep;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = Math.min(centerX, centerY) * 0.85f;

        if (segments.isEmpty() || totalValue == 0) return;

        float currentAngle = 270f;
        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            float sweep = (s.value / totalValue) * 360f;
            float radius = baseRadius * s.scale;
            // 如果被选中，稍微增加半径实现“弹出”效果
            if (i == selectedIndex) radius += 15f;

            RectF rect = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            slicePaint.setColor(s.color);
            canvas.drawArc(rect, currentAngle, sweep, true, slicePaint);
            currentAngle += sweep;
        }

        if (selectedIndex != -1) {
            Segment s = segments.get(selectedIndex);
            int percent = Math.round((s.value / totalValue) * 100);
            drawCenter(canvas, centerX, centerY, baseRadius, percent + "%", s.label);
        } else {
            drawCenter(canvas, centerX, centerY, baseRadius, String.valueOf((int)totalValue), "Total");
        }
    }

    private void drawCenter(Canvas canvas, float cx, float cy, float rad, String main, String sub) {
        canvas.drawCircle(cx, cy, rad * 0.6f, centerPaint);
        float offset = (textPaint.descent() + textPaint.ascent()) / 2;
        canvas.drawText(main, cx, cy - offset - 15, textPaint);
        canvas.drawText(sub, cx, cy - offset + 45, subTextPaint);
    }
}