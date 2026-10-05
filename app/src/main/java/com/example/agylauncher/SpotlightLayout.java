package com.example.agylauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** Dims everything except one rounded "hole", draws a pulsing ring around it, and holds a hint. */
public class SpotlightLayout extends FrameLayout {

    private final RectF hole = new RectF();
    private final RectF ringRect = new RectF();
    private final Path scrimPath = new Path();
    private final Paint scrim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float radius;
    private int ringColor = 0xFFFFFFFF;
    private Runnable onHoleTap;
    private Runnable onOutsideTap;

    public SpotlightLayout(Context c) {
        super(c);
        setWillNotDraw(false);
        setClickable(true);
        scrim.setColor(0xB3000000);
        ring.setStyle(Paint.Style.STROKE);
    }

    public void setHole(float l, float t, float r, float b, float radius) {
        hole.set(l, t, r, b);
        this.radius = radius;
        invalidate();
    }

    public void setRingColor(int color) {
        ringColor = color;
    }

    public void setListeners(Runnable inHole, Runnable outside) {
        onHoleTap = inHole;
        onOutsideTap = outside;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        scrimPath.reset();
        scrimPath.setFillType(Path.FillType.EVEN_ODD);
        scrimPath.addRect(0f, 0f, w, h, Path.Direction.CW);
        scrimPath.addRoundRect(hole, radius, radius, Path.Direction.CW);
        canvas.drawPath(scrimPath, scrim);

        float density = getResources().getDisplayMetrics().density;
        float t = (SystemClock.uptimeMillis() % 1300L) / 1300f;
        float grow = t * 16f * density;
        ring.setColor(ringColor);
        ring.setAlpha(Math.round(255f * (1f - t) * 0.9f));
        ring.setStrokeWidth(2.5f * density);
        ringRect.set(hole.left - grow, hole.top - grow, hole.right + grow, hole.bottom + grow);
        canvas.drawRoundRect(ringRect, radius + grow, radius + grow, ring);

        ring.setAlpha(230);
        ring.setStrokeWidth(2f * density);
        canvas.drawRoundRect(hole, radius, radius, ring);

        super.dispatchDraw(canvas);
        postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            if (hole.contains(e.getX(), e.getY())) {
                if (onHoleTap != null) onHoleTap.run();
            } else if (onOutsideTap != null) {
                onOutsideTap.run();
            }
        }
        return true;
    }
}
