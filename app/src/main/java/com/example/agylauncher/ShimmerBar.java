package com.example.agylauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/** Smooth progress bar with a moving shimmer highlight. */
public class ShimmerBar extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();
    private float target = 0f;
    private float shown = 0f;

    public ShimmerBar(Context c) {
        super(c);
    }

    public void setColors(int track, int fill) {
        trackPaint.setColor(track);
        fillPaint.setColor(fill);
        invalidate();
    }

    /** 0..1 */
    public void setProgress(float p) {
        target = Math.max(0f, Math.min(1f, p));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = h / 2f;

        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, r, r, trackPaint);

        shown += (target - shown) * 0.08f;
        float fw = Math.max(h, w * shown);
        rect.set(0, 0, fw, h);
        canvas.drawRoundRect(rect, r, r, fillPaint);

        float phase = (SystemClock.uptimeMillis() % 1400L) / 1400f;
        float gw = Math.max(h * 4f, w * 0.3f);
        float gx = -gw + phase * (fw + gw);
        glowPaint.setShader(new LinearGradient(gx, 0, gx + gw, 0,
                new int[]{0x00FFFFFF, 0x80FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        clip.reset();
        clip.addRoundRect(rect, r, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(0, 0, fw, h, glowPaint);
        canvas.restore();

        postInvalidateOnAnimation();
    }
}
