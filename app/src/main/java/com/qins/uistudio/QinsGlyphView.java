package com.qins.uistudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small original vector glyphs for QINS controls; drawn at runtime for API 19. */
public final class QinsGlyphView extends View {
    private final String glyph;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int tint;

    public QinsGlyphView(Context context, String glyph, int tint) {
        super(context);
        this.glyph = glyph == null ? "" : glyph;
        this.tint = tint;
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setTint(int color) {
        tint = color;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float unit = Math.min(getWidth(), getHeight()) / 24f;
        if (unit <= 0) return;
        canvas.save();
        canvas.translate(getWidth() / 2f, getHeight() / 2f);
        canvas.scale(unit, unit);

        paint.setColor(tint);
        paint.setStrokeWidth(1.9f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);

        if ("apps".equals(glyph)) drawApps(canvas);
        else if ("wifi".equals(glyph)) drawWifi(canvas);
        else if ("bluetooth".equals(glyph)) drawBluetooth(canvas);
        else if ("sound".equals(glyph) || "volume".equals(glyph)) drawSound(canvas);
        else if ("brightness".equals(glyph)) drawBrightness(canvas);
        else if ("home".equals(glyph)) drawHome(canvas);
        else if ("add".equals(glyph)) drawAdd(canvas);
        else if ("remove".equals(glyph)) drawRemove(canvas);
        else if ("done".equals(glyph)) drawDone(canvas);
        else if ("left".equals(glyph)) drawArrow(canvas, false);
        else if ("right".equals(glyph)) drawArrow(canvas, true);

        canvas.restore();
    }

    private void drawApps(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        float r = 3.2f;
        c.drawRoundRect(-9, -9, -2, -2, 1.7f, 1.7f, paint);
        c.drawRoundRect(2, -9, 9, -2, 1.7f, 1.7f, paint);
        c.drawRoundRect(-9, 2, -2, 9, 1.7f, 1.7f, paint);
        c.drawRoundRect(2, 2, 9, 9, 1.7f, 1.7f, paint);
    }

    private void drawWifi(Canvas c) {
        c.drawArc(-10, -10, 10, 10, 225, 90, false, paint);
        c.drawArc(-6.5f, -6.5f, 6.5f, 6.5f, 225, 90, false, paint);
        c.drawArc(-3, -3, 3, 3, 225, 90, false, paint);
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(0, 8.5f, 1.7f, paint);
    }

    private void drawBluetooth(Canvas c) {
        Path p = new Path();
        p.moveTo(0, -11);
        p.lineTo(7, -5);
        p.lineTo(-6, 6);
        p.moveTo(0, -11);
        p.lineTo(0, 11);
        p.lineTo(7, 5);
        p.lineTo(-6, -6);
        c.drawPath(p, paint);
    }

    private void drawSound(Canvas c) {
        Path speaker = new Path();
        speaker.moveTo(-10, -4);
        speaker.lineTo(-5, -4);
        speaker.lineTo(1, -9);
        speaker.lineTo(1, 9);
        speaker.lineTo(-5, 4);
        speaker.lineTo(-10, 4);
        speaker.close();
        c.drawPath(speaker, paint);
        c.drawArc(-2, -8, 11, 8, -55, 110, false, paint);
        c.drawArc(-1, -12, 16, 12, -55, 110, false, paint);
    }

    private void drawBrightness(Canvas c) {
        c.drawCircle(0, 0, 4.2f, paint);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4.0;
            float x1 = (float) Math.cos(angle) * 7.0f;
            float y1 = (float) Math.sin(angle) * 7.0f;
            float x2 = (float) Math.cos(angle) * 10.5f;
            float y2 = (float) Math.sin(angle) * 10.5f;
            c.drawLine(x1, y1, x2, y2, paint);
        }
    }

    private void drawHome(Canvas c) {
        Path p = new Path();
        p.moveTo(-10, -1);
        p.lineTo(0, -10);
        p.lineTo(10, -1);
        p.moveTo(-7, -3);
        p.lineTo(-7, 9);
        p.lineTo(7, 9);
        p.lineTo(7, -3);
        c.drawPath(p, paint);
        c.drawRect(-2, 2, 2, 9, paint);
    }

    private void drawAdd(Canvas c) {
        c.drawLine(0, -9, 0, 9, paint);
        c.drawLine(-9, 0, 9, 0, paint);
    }

    private void drawRemove(Canvas c) {
        c.drawLine(-8, -8, 8, 8, paint);
        c.drawLine(8, -8, -8, 8, paint);
    }

    private void drawArrow(Canvas c, boolean right) {
        Path p = new Path();
        if (right) { p.moveTo(-4, -8); p.lineTo(4, 0); p.lineTo(-4, 8); }
        else { p.moveTo(4, -8); p.lineTo(-4, 0); p.lineTo(4, 8); }
        c.drawPath(p, paint);
    }

    private void drawDone(Canvas c) {
        Path p = new Path();
        p.moveTo(-9, 0);
        p.lineTo(-3, 6);
        p.lineTo(9, -7);
        c.drawPath(p, paint);
    }
}
