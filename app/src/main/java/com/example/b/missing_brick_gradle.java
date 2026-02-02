package com.example.b;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class missing_brick_gradle extends b_gradle {
    private Paint paint;
    private Paint cellPaint;
    private Paint dotPaint;
    private Paint woodPaint;
    private Paint greyLinePaint;
    private Paint whiteLinePaint;

    public missing_brick_gradle(Context context, int columns, int rows, int resolution, int g_type) {
        super(context, columns, rows, resolution, g_type);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2f);
        paint.setStyle(Paint.Style.STROKE);

        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setStyle(Paint.Style.FILL);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);

        woodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        woodPaint.setColor(Color.BLACK);
        woodPaint.setAlpha(60);
        woodPaint.setStyle(Paint.Style.STROKE);
        woodPaint.setStrokeWidth(1.2f);

        greyLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greyLinePaint.setColor(Color.GRAY);
        greyLinePaint.setAlpha(120);
        greyLinePaint.setStyle(Paint.Style.STROKE);
        greyLinePaint.setStrokeWidth(1.5f);

        whiteLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteLinePaint.setColor(Color.WHITE);
        whiteLinePaint.setAlpha(150);
        whiteLinePaint.setStyle(Paint.Style.STROKE);
        whiteLinePaint.setStrokeWidth(1.5f);
    }

    @Override
    public void setImageData(Bitmap bitmap) {
        if (bitmap == null) return;
        float gridWidth = columns * resolution + resolution / 2f;
        float gridHeight = rows * resolution;
        for (int r = 0; r < rows; r++) {
            float xOffset = (r % 2 == 1) ? resolution / 2f : 0;
            for (int c = 0; c < columns; c++) {
                if (shouldBeMissing(r, c)) {
                    pixelColors[r][c] = 0;
                    continue;
                }
                float centerX = c * resolution + xOffset + resolution / 2f;
                float centerY = r * resolution + resolution / 2f;
                float u = Math.min(1f, Math.max(0f, centerX / gridWidth));
                float v = Math.min(1f, Math.max(0f, centerY / gridHeight));
                int bX = (int) (u * (bitmap.getWidth() - 1));
                int bY = (int) (v * (bitmap.getHeight() - 1));
                pixelColors[r][c] = bitmap.getPixel(bX, bY);
            }
        }
        invalidate();
    }

    private boolean shouldBeMissing(int r, int c) {
        int patternRow = r % 4;
        if (patternRow == 1) return c % 2 == 0;
        if (patternRow == 3) return c % 2 == 1;
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (resolution <= 0) return;
        float padding = 2f;
        String shape = currentBead != null ? currentBead.getShape() : "round edged";

        for (int r = 0; r < rows; r++) {
            float xOffset = (r % 2 == 1) ? resolution / 2f : 0;
            for (int c = 0; c < columns; c++) {
                if (shouldBeMissing(r, c)) continue;

                float l = c * resolution + xOffset + padding;
                float t = r * resolution + padding;
                float ri = l + resolution - 2 * padding;
                float bo = t + resolution - 2 * padding;

                // Odd rows (1, 3 -> index 0, 2) act as vertical grid
                // Even rows (2, 4 -> index 1, 3) act as horizontal grid
                boolean horizOrient = (r % 2 != 0); 

                if (currentBead != null) {
                    float hInset = currentBead.getHorizontalInset(resolution, horizOrient);
                    float vInset = currentBead.getVerticalInset(resolution, horizOrient);
                    l += hInset; ri -= hInset; t += vInset; bo -= vInset;
                }

                int color = pixelColors[r][c];
                if (color != 0) {
                    if (currentBead != null) {
                        cellPaint.setColor(currentBead.getModifiedColor(color));
                        drawBead(canvas, l, t, ri, bo, shape, cellPaint);

                        if (currentBead.shouldDrawWhiteDot()) {
                            canvas.drawCircle(l + (ri-l)*0.3f, t + (bo-t)*0.3f, (ri-l)*0.1f, dotPaint);
                        }

                        int woodStrokes = currentBead.getWoodStrokeCount();
                        if (woodStrokes > 0) {
                            for (int i = 1; i <= woodStrokes; i++) {
                                float strokeY = t + ((bo-t) * i / (woodStrokes + 1f));
                                canvas.drawLine(l + 4, strokeY, ri - 4, strokeY, woodPaint);
                            }
                        }

                        if (currentBead.hasSquareStrokes()) {
                            if (horizOrient) { // Lines are vertical
                                canvas.drawLine(l + (ri-l) * 0.35f, t + 4, l + (ri-l) * 0.35f, bo - 4, greyLinePaint);
                                canvas.drawLine(l + (ri-l) * 0.65f, t + 4, l + (ri-l) * 0.65f, bo - 4, whiteLinePaint);
                            } else { // Lines are horizontal
                                canvas.drawLine(l + 4, t + (bo-t) * 0.35f, ri - 4, t + (bo-t) * 0.35f, greyLinePaint);
                                canvas.drawLine(l + 4, t + (bo-t) * 0.65f, ri - 4, t + (bo-t) * 0.65f, whiteLinePaint);
                            }
                        }
                    } else {
                        cellPaint.setColor(color);
                        drawBead(canvas, l, t, ri, bo, shape, cellPaint);
                    }
                }
                canvas.drawRect(c * resolution + xOffset, r * resolution, c * resolution + xOffset + resolution, r * resolution + resolution, paint);
            }
        }
    }

    private void drawBead(Canvas canvas, float l, float t, float r, float b, String shape, Paint p) {
        if (shape.equals("square")) {
            canvas.drawRect(l, t, r, b, p);
        } else if (shape.equals("round")) {
            canvas.drawOval(l, t, r, b, p);
        } else {
            float rad = (r - l) * 0.2f;
            canvas.drawRoundRect(l, t, r, b, rad, rad, p);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) (columns * resolution + resolution / 2f);
        int desiredHeight = rows * resolution;
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), 
                             resolveSize(desiredHeight, heightMeasureSpec));
    }
}
