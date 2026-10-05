package com.example.agylauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Vector-style icons drawn with Canvas (no image resources needed). */
public class IconView extends View {

    static final int MENU = 0, DOC = 1, NEWCHAT = 2, DOTS = 3, PLUS = 4, MIC = 5, UP = 6,
            WAVE = 7, COPY = 8, SHARE = 9, PLAY = 10, THUMB_UP = 11, THUMB_DOWN = 12,
            RETRY = 13, CLOSE = 14, CHEVRON = 15, BACK = 16, SETTINGS = 17;

    private int type;
    private int color = Color.BLACK;
    private int bgColor = Color.WHITE;
    private boolean filled = false;
    private float iconDp = 24f;

    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public IconView(Context c, int type) {
        super(c);
        this.type = type;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
    }

    public void setType(int t) {
        type = t;
        invalidate();
    }

    public void setColors(int color, int bg) {
        this.color = color;
        this.bgColor = bg;
        invalidate();
    }

    public void setFilled(boolean f) {
        filled = f;
        invalidate();
    }

    public void setIconDp(float dp) {
        iconDp = dp;
        invalidate();
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2) {
        c.drawLine(x1, y1, x2, y2, stroke);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        float size = iconDp * density;
        float u = size / 24f;
        canvas.save();
        canvas.translate(getWidth() / 2f - size / 2f, getHeight() / 2f - size / 2f);
        canvas.scale(u, u);

        stroke.setColor(color);
        stroke.setStrokeWidth(1.9f);
        fill.setColor(color);

        switch (type) {
            case MENU:
                line(canvas, 3, 7, 21, 7);
                line(canvas, 3, 12, 15, 12);
                line(canvas, 3, 17, 9, 17);
                break;

            case DOC:
                path.reset();
                path.moveTo(6, 3);
                path.lineTo(14, 3);
                path.lineTo(19, 8);
                path.lineTo(19, 21);
                path.lineTo(6, 21);
                path.close();
                canvas.drawPath(path, stroke);
                path.reset();
                path.moveTo(14, 3);
                path.lineTo(14, 8);
                path.lineTo(19, 8);
                canvas.drawPath(path, stroke);
                path.reset();
                path.moveTo(9, 13);
                path.rQuadTo(1.2f, -1.6f, 2.4f, 0);
                path.rQuadTo(1.2f, 1.6f, 2.4f, 0);
                canvas.drawPath(path, stroke);
                path.reset();
                path.moveTo(9, 17);
                path.rQuadTo(1.2f, -1.6f, 2.4f, 0);
                path.rQuadTo(1.2f, 1.6f, 2.4f, 0);
                canvas.drawPath(path, stroke);
                break;

            case NEWCHAT:
                canvas.drawCircle(12, 11, 9.5f, fill);
                path.reset();
                path.moveTo(4.5f, 16.5f);
                path.lineTo(2.5f, 22f);
                path.lineTo(9.5f, 19.5f);
                path.close();
                canvas.drawPath(path, fill);
                stroke.setColor(bgColor);
                stroke.setStrokeWidth(2.2f);
                line(canvas, 12, 7, 12, 15);
                line(canvas, 8, 11, 16, 11);
                break;

            case DOTS:
                canvas.drawCircle(12, 5, 1.9f, fill);
                canvas.drawCircle(12, 12, 1.9f, fill);
                canvas.drawCircle(12, 19, 1.9f, fill);
                break;

            case PLUS:
                line(canvas, 12, 5, 12, 19);
                line(canvas, 5, 12, 19, 12);
                break;

            case MIC:
                rect.set(9, 2.5f, 15, 13.5f);
                canvas.drawRoundRect(rect, 3, 3, stroke);
                rect.set(5.5f, 6, 18.5f, 18);
                canvas.drawArc(rect, 0, 180, false, stroke);
                line(canvas, 12, 18, 12, 21.5f);
                line(canvas, 8.5f, 21.5f, 15.5f, 21.5f);
                break;

            case UP:
                line(canvas, 12, 19, 12, 5);
                path.reset();
                path.moveTo(6, 11);
                path.lineTo(12, 5);
                path.lineTo(18, 11);
                canvas.drawPath(path, stroke);
                break;

            case WAVE: {
                stroke.setStrokeWidth(2.3f);
                float[] xs = {5f, 8.5f, 12f, 15.5f, 19f};
                float[] hs = {3f, 6f, 9f, 5.5f, 2.5f};
                for (int i = 0; i < xs.length; i++) {
                    line(canvas, xs[i], 12 - hs[i], xs[i], 12 + hs[i]);
                }
                break;
            }

            case COPY:
                rect.set(3.5f, 9.5f, 14.5f, 20.5f);
                canvas.drawRoundRect(rect, 2.2f, 2.2f, stroke);
                path.reset();
                path.moveTo(8.5f, 9.5f);
                path.lineTo(8.5f, 5.5f);
                path.quadTo(8.5f, 4f, 10f, 4f);
                path.lineTo(18.5f, 4f);
                path.quadTo(20f, 4f, 20f, 5.5f);
                path.lineTo(20f, 14f);
                path.quadTo(20f, 15.5f, 18.5f, 15.5f);
                path.lineTo(14.5f, 15.5f);
                canvas.drawPath(path, stroke);
                break;

            case SHARE:
                path.reset();
                path.moveTo(4, 19);
                path.lineTo(4, 17);
                path.quadTo(4, 11, 10, 11);
                path.lineTo(20, 11);
                canvas.drawPath(path, stroke);
                path.reset();
                path.moveTo(16, 7);
                path.lineTo(20, 11);
                path.lineTo(16, 15);
                canvas.drawPath(path, stroke);
                break;

            case PLAY:
                path.reset();
                path.moveTo(7, 4.5f);
                path.lineTo(19, 12);
                path.lineTo(7, 19.5f);
                path.close();
                canvas.drawPath(path, stroke);
                break;

            case THUMB_UP:
            case THUMB_DOWN:
                if (type == THUMB_DOWN) canvas.rotate(180, 12, 12);
                path.reset();
                path.moveTo(7, 10.5f);
                path.lineTo(11.5f, 2.5f);
                path.quadTo(15.5f, 2.5f, 14.8f, 6.2f);
                path.lineTo(14, 10);
                path.lineTo(19.5f, 10);
                path.quadTo(22, 10.2f, 21.6f, 12.8f);
                path.lineTo(20, 19.5f);
                path.quadTo(19.6f, 21.5f, 17.6f, 21.5f);
                path.lineTo(7, 21.5f);
                path.close();
                if (filled) canvas.drawPath(path, fill);
                canvas.drawPath(path, stroke);
                rect.set(2.5f, 10.5f, 7f, 21.5f);
                canvas.drawRoundRect(rect, 1.6f, 1.6f, stroke);
                break;

            case RETRY: {
                rect.set(4.5f, 4.5f, 19.5f, 19.5f);
                canvas.drawArc(rect, 25, 290, false, stroke);
                double th = Math.toRadians(315);
                float px = (float) (12 + 7.5 * Math.cos(th));
                float py = (float) (12 + 7.5 * Math.sin(th));
                double tx = -Math.sin(th);
                double ty = Math.cos(th);
                for (int s = -1; s <= 1; s += 2) {
                    double a = Math.toRadians(145 * s);
                    double rx = tx * Math.cos(a) - ty * Math.sin(a);
                    double ry = tx * Math.sin(a) + ty * Math.cos(a);
                    line(canvas, px, py, (float) (px + rx * 4.2), (float) (py + ry * 4.2));
                }
                break;
            }

            case CLOSE:
                line(canvas, 6, 6, 18, 18);
                line(canvas, 18, 6, 6, 18);
                break;

            case SETTINGS: {
                canvas.drawCircle(12, 12, 3.1f, stroke);
                canvas.drawCircle(12, 12, 6.4f, stroke);
                stroke.setStrokeWidth(2.7f);
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45);
                    float cs = (float) Math.cos(a);
                    float sn = (float) Math.sin(a);
                    line(canvas, 12 + cs * 7.4f, 12 + sn * 7.4f, 12 + cs * 9.8f, 12 + sn * 9.8f);
                }
                break;
            }

            case BACK:
                line(canvas, 20, 12, 5, 12);
                path.reset();
                path.moveTo(11, 6);
                path.lineTo(5, 12);
                path.lineTo(11, 18);
                canvas.drawPath(path, stroke);
                break;

            case CHEVRON:
                path.reset();
                path.moveTo(8, 10);
                path.lineTo(12, 14);
                path.lineTo(16, 10);
                canvas.drawPath(path, stroke);
                break;

            default:
                break;
        }
        canvas.restore();
    }
}
