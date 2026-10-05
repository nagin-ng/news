package com.example.agylauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

/** Animated pixel arch whose blocks float and drift upward: the "anti-gravity" loader. */
public class AntigravityView extends View {

    private static final String[] ROWS = {
            ".....XX.....",
            "....XXXX....",
            "...XXXXXX...",
            "..XXXXXXXX..",
            ".XXXX..XXXX.",
            ".XXX....XXX.",
            "XXX......XXX",
            "XX........XX"
    };
    private static final int COLS = 12;
    private static final int[] STOPS = {0xFFFFC53D, 0xFFF2803A, 0xFF3DBE6E, 0xFF3B82F6, 0xFF8B5CF6};
    private static final float[] POS = {0f, 0.3f, 0.55f, 0.8f, 1f};
    private static final int PARTICLES = 10;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public AntigravityView(Context c) {
        super(c);
    }

    private static int lerp(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static int gradient(float t) {
        if (t <= 0f) return STOPS[0];
        for (int i = 1; i < STOPS.length; i++) {
            if (t <= POS[i]) {
                float k = (t - POS[i - 1]) / (POS[i] - POS[i - 1]);
                return lerp(STOPS[i - 1], STOPS[i], k);
            }
        }
        return STOPS[STOPS.length - 1];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float t = SystemClock.uptimeMillis() / 1000f;
        int n = ROWS.length;
        float cell = Math.min(w / (COLS + 2f), h / (n + 3.2f));
        float gap = cell * 0.14f;
        float ox = (w - COLS * cell) / 2f;
        float oy = h - (n + 0.6f) * cell;
        float bob = (float) Math.sin(t * 1.4f) * cell * 0.35f;

        // blocks
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < COLS; c++) {
                if (ROWS[r].charAt(c) != 'X') continue;
                float wave = (float) Math.sin(t * 2.2f + c * 0.55f - r * 0.42f);
                float dy = wave * cell * 0.28f - Math.max(0f, wave) * cell * 0.12f;
                float tw = 0.82f + 0.18f * (float) Math.sin(t * 3f + r * 1.3f + c * 0.7f);
                float gt = Math.min(1f, (r / (float) (n - 1)) * 0.85f + 0.15f * Math.abs(c - 5.5f) / 5.5f);
                paint.setColor(gradient(gt));
                paint.setAlpha(Math.round(255f * tw));
                float left = ox + c * cell + gap / 2f;
                float top = oy + r * cell + gap / 2f + bob + dy;
                rect.set(left, top, left + cell - gap, top + cell - gap);
                canvas.drawRoundRect(rect, cell * 0.22f, cell * 0.22f, paint);
            }
        }

        // little blocks rising from the arch and fading out
        for (int i = 0; i < PARTICLES; i++) {
            float ph = (t * 0.35f + i * 0.1f) % 1f;
            float x = ox + ((i * 1.13f) % COLS) * cell + (float) Math.sin(t * 1.5f + i) * cell * 0.3f;
            float y = oy + n * cell - ph * (n + 2.4f) * cell + bob;
            float s = cell * 0.30f * (1f - ph * 0.5f);
            paint.setColor(gradient(((i * 0.173f) % 1f)));
            paint.setAlpha(Math.round(255f * (1f - ph) * 0.85f));
            rect.set(x, y, x + s, y + s);
            canvas.drawRoundRect(rect, s * 0.25f, s * 0.25f, paint);
        }

        postInvalidateOnAnimation();
    }
}
