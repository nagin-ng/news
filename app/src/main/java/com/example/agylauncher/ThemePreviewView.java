package com.example.agylauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Tiny chat mock-up used as a theme tile. mode: 0 = system (split), 1 = light, 2 = dark. */
public class ThemePreviewView extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path clip = new Path();
    private int mode;
    private int accent = 0xFFC6613F;

    public ThemePreviewView(Context c, int mode) {
        super(c);
        this.mode = mode;
    }

    public void setAccent(int a) {
        accent = a;
        invalidate();
    }

    private void half(Canvas c, float l, float rr, float h, boolean dark) {
        float d = getResources().getDisplayMetrics().density;
        p.setColor(dark ? 0xFF262624 : 0xFFFAF9F5);
        c.drawRect(l, 0f, rr, h, p);
        p.setColor(dark ? 0xFF3A3935 : 0xFFEAE7DC);
        r.set(l + (rr - l) * 0.38f, h * 0.2f, l + (rr - l) * 0.9f, h * 0.38f);
        c.drawRoundRect(r, 4 * d, 4 * d, p);
        p.setColor(dark ? 0xFF8D8B82 : 0xFFB9B6AC);
        r.set(l + (rr - l) * 0.1f, h * 0.5f, l + (rr - l) * 0.78f, h * 0.58f);
        c.drawRoundRect(r, 3 * d, 3 * d, p);
        r.set(l + (rr - l) * 0.1f, h * 0.64f, l + (rr - l) * 0.55f, h * 0.72f);
        c.drawRoundRect(r, 3 * d, 3 * d, p);
        p.setColor(accent);
        r.set(l + (rr - l) * 0.1f, h * 0.82f, l + (rr - l) * 0.34f, h * 0.92f);
        c.drawRoundRect(r, 5 * d, 5 * d, p);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float d = getResources().getDisplayMetrics().density;
        clip.reset();
        r.set(0f, 0f, w, h);
        clip.addRoundRect(r, 12 * d, 12 * d, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        if (mode == 0) {
            half(canvas, 0f, w / 2f, h, false);
            half(canvas, w / 2f, w, h, true);
        } else {
            half(canvas, 0f, w, h, mode == 2);
        }
        canvas.restore();
    }
}
