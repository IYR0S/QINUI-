package com.qins.uistudio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Lightweight API-19-compatible dot-matrix text treatment.
 * It rasterizes TextView glyphs and redraws their coverage as a regular
 * field of round points, without a downloadable font or newer APIs.
 */
public final class DotMatrixTextView extends TextView {
    private Bitmap buffer;
    private Canvas bufferCanvas;
    private int[] pixels;
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int bufferWidth;
    private int bufferHeight;

    public DotMatrixTextView(Context context) { super(context); init(); }
    public DotMatrixTextView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public DotMatrixTextView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setIncludeFontPadding(false);
    }

    private void releaseBuffer() {
        if (buffer != null && !buffer.isRecycled()) buffer.recycle();
        buffer = null;
        bufferCanvas = null;
        pixels = null;
        bufferWidth = 0;
        bufferHeight = 0;
    }

    @Override protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0 || getText() == null || getText().length() == 0) {
            super.onDraw(canvas);
            return;
        }
        if (buffer == null || bufferWidth != width || bufferHeight != height) {
            releaseBuffer();
            buffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bufferCanvas = new Canvas(buffer);
            pixels = new int[width * height];
            bufferWidth = width;
            bufferHeight = height;
        }
        buffer.eraseColor(Color.TRANSPARENT);
        super.onDraw(bufferCanvas);
        buffer.getPixels(pixels, 0, width, 0, 0, width, height);
        dotPaint.setColor(getCurrentTextColor());
        float density = getResources().getDisplayMetrics().density;
        int step = Math.max(2, Math.round(2.7f * density));
        float radius = Math.max(0.65f, 0.72f * density);
        for (int y = step / 2; y < height; y += step) {
            int offset = y * width;
            for (int x = step / 2; x < width; x += step) {
                if (((pixels[offset + x] >>> 24) & 255) > 56) {
                    canvas.drawCircle(x, y, radius, dotPaint);
                }
            }
        }
    }

    @Override protected void onDetachedFromWindow() {
        releaseBuffer();
        super.onDetachedFromWindow();
    }
}