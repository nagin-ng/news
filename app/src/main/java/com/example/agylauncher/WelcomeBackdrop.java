package com.example.agylauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/** Soft gradient background with drifting colour orbs and twinkling stars. */
public class WelcomeBackdrop extends View {

    private static final int STARS = 30;

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbA = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbB = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] sx = new float[STARS];
    private final float[] sy = new float[STARS];
    private final float[] sp = new float[STARS];
    private final float[] ss = new float[STARS];

    private int top = 0xFFFCF6EF;
    private int bottom = 0xFFF8DFCF;
    private int colA = 0xFFC6613F;
    private int colB = 0xFFFFFFFF;
    private int colStar = 0xFFC6613F;

    public WelcomeBackdrop(Context c) {
        super(c);
        Random r = new Random(7);
        for (int i = 0; i < STARS; i++) {
            sx[i] = r.nextFloat();
            sy[i] = r.nextFloat();
            sp[i] = r.nextFloat() * 6.28f;
            ss[i] = 0.6f + r.nextFloat() * 1.6f;
        }
    }

    public void setColors(int top, int bottom, int orbA, int orbB, int star) {
        this.top = top;
        this.bottom = bottom;
        this.colA = orbA;
        this.colB = orbB;
        this.colStar = star;
        rebuild();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rebuild();
    }

    private void rebuild() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        bg.setShader(new LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP));
        int a = colA & 0x00FFFFFF;
        int b = colB & 0x00FFFFFF;
        orbA.setShader(new RadialGradient(0f, 0f, w * 0.75f, a | 0x55000000, a, Shader.TileMode.CLAMP));
        orbB.setShader(new RadialGradient(0f, 0f, w * 0.85f, b | 0x40000000, b, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        float t = SystemClock.uptimeMillis() / 1000f;
        float density = getResources().getDisplayMetrics().density;

        canvas.drawRect(0f, 0f, w, h, bg);

        canvas.save();
        canvas.translate(w * 0.18f + (float) Math.sin(t * 0.35f) * w * 0.07f,
                h * 0.20f + (float) Math.cos(t * 0.27f) * h * 0.03f);
        canvas.drawCircle(0f, 0f, w * 0.75f, orbA);
        canvas.restore();

        canvas.save();
        canvas.translate(w * 0.88f + (float) Math.cos(t * 0.30f) * w * 0.06f,
                h * 0.82f + (float) Math.sin(t * 0.25f) * h * 0.03f);
        canvas.drawCircle(0f, 0f, w * 0.85f, orbB);
        canvas.restore();

        int rgb = colStar & 0x00FFFFFF;
        for (int i = 0; i < STARS; i++) {
            float a = 0.10f + 0.40f * (0.5f + 0.5f * (float) Math.sin(t * 1.1f + sp[i]));
            dot.setColor(rgb | (Math.round(a * 255f) << 24));
            canvas.drawCircle(sx[i] * w, sy[i] * h, ss[i] * density, dot);
        }

        postInvalidateOnAnimation();
    }
}
