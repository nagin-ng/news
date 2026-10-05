package com.example.agylauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

/** Round profile picture with an animated gradient ring. Falls back to initials. */
public class AvatarView extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private Bitmap bitmap;
    private BitmapShader shader;
    private String initials = "NH";
    private int accent = 0xFFC6613F;
    private int gapColor = Color.WHITE;

    public AvatarView(Context c) {
        super(c);
        ringPaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
    }

    public void setBitmap(Bitmap b) {
        bitmap = b;
        shader = b == null ? null : new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        invalidate();
    }

    public void setInitials(String s) {
        initials = s;
        invalidate();
    }

    public void setColors(int accent, int gap) {
        this.accent = accent;
        this.gapColor = gap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float s = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        float ringW = s * 0.045f;
        float gap = s * 0.035f;
        float t = SystemClock.uptimeMillis() / 1000f;

        // turning ring
        ringPaint.setStrokeWidth(ringW);
        ringPaint.setShader(new SweepGradient(cx, cy,
                new int[]{accent, 0xFF8B5CF6, 0xFF3B82F6, 0xFF3DBE6E, 0xFFFFC53D, accent}, null));
        canvas.save();
        canvas.rotate((t * 40f) % 360f, cx, cy);
        canvas.drawCircle(cx, cy, s / 2f - ringW / 2f, ringPaint);
        canvas.restore();

        // gap
        fillPaint.setColor(gapColor);
        canvas.drawCircle(cx, cy, s / 2f - ringW - gap / 2f, fillPaint);

        float r = s / 2f - ringW - gap;
        if (shader != null && bitmap != null) {
            float d = r * 2f;
            float scale = Math.max(d / bitmap.getWidth(), d / bitmap.getHeight());
            matrix.reset();
            matrix.setScale(scale, scale);
            matrix.postTranslate(cx - bitmap.getWidth() * scale / 2f, cy - bitmap.getHeight() * scale / 2f);
            shader.setLocalMatrix(matrix);
            photoPaint.setShader(shader);
            canvas.drawCircle(cx, cy, r, photoPaint);
        } else {
            fillPaint.setColor(accent);
            canvas.drawCircle(cx, cy, r, fillPaint);
            textPaint.setTextSize(r * 0.9f);
            canvas.drawText(initials, cx, cy + r * 0.32f, textPaint);
        }
        postInvalidateOnAnimation();
    }
}
