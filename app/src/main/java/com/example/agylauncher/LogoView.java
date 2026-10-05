package com.example.agylauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/** Animated app logo for the welcome screen: glowing tile, sparkle and orbiting stars. */
public class LogoView extends View {

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tile = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hi = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path star = new Path();
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private int accent = 0xFFC6613F;

    public LogoView(Context c) {
        super(c);
        white.setColor(0xFFFFFFFF);
        buildStar();
    }

    public void setAccent(int a) {
        accent = a;
        rebuildShaders();
        invalidate();
    }

    private void buildStar() {
        star.moveTo(0f, -1f);
        star.cubicTo(0.07f, -0.36f, 0.36f, -0.07f, 1f, 0f);
        star.cubicTo(0.36f, 0.07f, 0.07f, 0.36f, 0f, 1f);
        star.cubicTo(-0.07f, 0.36f, -0.36f, 0.07f, -1f, 0f);
        star.cubicTo(-0.36f, -0.07f, -0.07f, -0.36f, 0f, -1f);
        star.close();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rebuildShaders();
    }

    private void rebuildShaders() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        float s = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        float tw = s * 0.46f;
        int rgb = accent & 0x00FFFFFF;
        glow.setShader(new RadialGradient(0f, 0f, s * 0.5f,
                rgb | 0x66000000, rgb, Shader.TileMode.CLAMP));
        tile.setShader(new LinearGradient(cx - tw / 2f, cy - tw / 2f, cx + tw / 2f, cy + tw / 2f,
                0xFFF29C78, 0xFFB34B2B, Shader.TileMode.CLAMP));
        hi.setShader(new RadialGradient(cx - tw * 0.22f, cy - tw * 0.3f, tw * 0.75f,
                0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    private void drawSpark(Canvas c, float x, float y, float r, float t, float phase, int color) {
        float a = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 2.2f + phase));
        spark.setColor(color);
        spark.setAlpha(Math.round(a * Color.alpha(color)));
        c.save();
        c.translate(x, y);
        c.scale(r, r);
        c.drawPath(star, spark);
        c.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        float s = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        float t = SystemClock.uptimeMillis() / 1000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 1.6f);

        // breathing glow
        canvas.save();
        canvas.translate(cx, cy);
        float k = 0.82f + 0.18f * pulse;
        canvas.scale(k, k);
        canvas.drawCircle(0f, 0f, s * 0.5f, glow);
        canvas.restore();

        // floating tile
        float bob = (float) Math.sin(t * 1.1f) * s * 0.012f;
        canvas.save();
        canvas.translate(0f, bob);
        float tw = s * 0.46f;
        float rad = tw * 0.28f;
        rect.set(cx - tw / 2f, cy - tw / 2f, cx + tw / 2f, cy + tw / 2f);

        shade.setColor(0x26000000);
        canvas.drawRoundRect(rect.left, rect.top + tw * 0.05f, rect.right, rect.bottom + tw * 0.05f,
                rad, rad, shade);
        canvas.drawRoundRect(rect, rad, rad, tile);

        clip.reset();
        clip.addRoundRect(rect, rad, rad, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(rect, hi);
        canvas.restore();

        // main sparkle
        float r = tw * 0.31f * (1f + 0.05f * (float) Math.sin(t * 2.0f));
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate((float) Math.sin(t * 0.9f) * 5f);
        canvas.save();
        canvas.translate(0f, r * 0.06f);
        canvas.scale(r, r);
        shade.setColor(0x33000000);
        canvas.drawPath(star, shade);
        canvas.restore();
        canvas.scale(r, r);
        canvas.drawPath(star, white);
        canvas.restore();

        drawSpark(canvas, cx + tw * 0.27f, cy - tw * 0.27f, tw * 0.075f, t, 0f, 0xCCFFFFFF);
        drawSpark(canvas, cx - tw * 0.28f, cy + tw * 0.28f, tw * 0.05f, t, 1.7f, 0x99FFFFFF);
        canvas.restore();

        // orbiting sparkles
        float ang = t * 0.7f;
        float orbit = s * 0.40f;
        int ac = accent | 0xFF000000;
        drawSpark(canvas, cx + (float) Math.cos(ang) * orbit, cy + (float) Math.sin(ang) * orbit * 0.8f,
                s * 0.035f, t, 0.5f, ac);
        float ang2 = (float) (ang + Math.PI);
        drawSpark(canvas, cx + (float) Math.cos(ang2) * orbit, cy + (float) Math.sin(ang2) * orbit * 0.8f,
                s * 0.025f, t, 2.4f, ac);

        postInvalidateOnAnimation();
    }
}
