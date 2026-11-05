package com.qins.uistudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Compact, API-19-compatible cellular signal indicator for the launcher header. */
public final class SignalStrengthIndicatorView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int level = -1;
    private boolean noService = false;
    private int activeColor = 0xFF356FE5;
    private int inactiveColor = 0xFF808080;

    public SignalStrengthIndicatorView(Context context) {
        super(context);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setSignal(int level, boolean noService, int activeColor, int inactiveColor) {
        this.level = Math.max(-1, Math.min(4, level));
        this.noService = noService;
        this.activeColor = activeColor;
        this.inactiveColor = inactiveColor;
        setContentDescription(noService ? "No cellular service" :
                (level < 0 ? "Cellular signal unavailable" : "Cellular signal " + level + " of 4"));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float barWidth = Math.max(1f, w / 8f);
        float gap = barWidth * 0.55f;
        float total = 4f * barWidth + 3f * gap;
        float left = (w - total) / 2f;
        float bottom = h * 0.88f;
        float maxHeight = h * 0.82f;
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 4; i++) {
            float barHeight = maxHeight * (i + 1) / 4f;
            paint.setColor(!noService && level >= 0 && i < level ? activeColor : inactiveColor);
            paint.setAlpha(!noService && level >= 0 && i < level ? 255 : 105);
            float x = left + i * (barWidth + gap);
            canvas.drawRect(x, bottom - barHeight, x + barWidth, bottom, paint);
        }
        if (noService) {
            paint.setColor(0xFFE34B4B);
            paint.setAlpha(255);
            paint.setStrokeWidth(Math.max(1.2f, barWidth * 0.65f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(w * 0.12f, h * 0.18f, w * 0.88f, h * 0.86f, paint);
        }
    }
}
