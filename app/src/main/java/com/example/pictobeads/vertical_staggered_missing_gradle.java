package com.example.pictobeads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A custom grid view that renders beads in a vertical brick pattern (staggered columns)
 * with a 3-row repeating missing bead pattern.
 */
public class vertical_staggered_missing_gradle extends b_gradle {
    private Paint paint;
    private Paint cellPaint;
    private Paint dotPaint;
    private Paint woodPaint;
    private Paint greyLinePaint;
    private Paint whiteLinePaint;

    /**
     * Initializes the vertical staggered grid view.
     * Input: context - context, columns - width, rows - height, resolution - scale, g_type - type.
     * Output: None.
     * Algorithm: Calls super constructor and initializes paint objects.
     */
    public vertical_staggered_missing_gradle(Context context, int columns, int rows, int resolution, int g_type) {
        super(context, columns, rows, resolution, g_type);
        init();
    }

    /**
     * Configures drawing paints.
     * Input: None.
     * Output: None.
     * Algorithm: Sets up colors and styles for beads and textures.
     */
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

    /**
     * Maps bitmap pixels to grid cells using vertical stagger and missing pattern.
     * Input: bitmap - source image.
     * Output: None.
     * Algorithm: Calculates bounds, iterates through cells, skips missing ones based on 3-row pattern, and samples image pixels. It then applies color variety limitation.
     */
    @Override
    public void setImageData(Bitmap bitmap) {
        if (bitmap == null) return;
        float gridWidth = columns * resolution;
        float gridHeight = rows * resolution + resolution / 2f;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (shouldBeMissing(r, c)) {
                    pixelColors[r][c] = 0;
                    continue;
                }
                float yOffset = (c % 2 == 1) ? resolution / 2f : 0;
                float centerX = c * resolution + resolution / 2f;
                float centerY = r * resolution + yOffset + resolution / 2f;
                
                float u = Math.min(1f, Math.max(0f, centerX / gridWidth));
                float v = Math.min(1f, Math.max(0f, centerY / gridHeight));
                
                int bX = (int) (u * (bitmap.getWidth() - 1));
                int bY = (int) (v * (bitmap.getHeight() - 1));
                pixelColors[r][c] = bitmap.getPixel(bX, bY);
            }
        }
        applyColorQuantization();
        invalidate();
    }

    /**
     * Determines if a specific bead should be missing based on a 3-row repeating pattern.
     * Input: r - row index, c - column index.
     * Output: boolean - true if missing.
     * Algorithm: Row 1 (idx 0) is full. Row 2 (idx 1) has even columns removed. Row 3 (idx 2) has odd columns removed.
     */
    private boolean shouldBeMissing(int r, int c) {
        int patternRow = r % 3;
        if (patternRow == 0) return false; // Row 1: Full
        if (patternRow == 1) return (c % 2 == 0); // Row 2: Even columns removed
        if (patternRow == 2) return (c % 2 == 1); // Row 3: Odd columns removed
        return false;
    }

    /**
     * Renders the grid with vertical stagger and 3-row missing pattern.
     * Input: canvas - destination.
     * Output: None.
     * Algorithm: Loops through cells, applies column-based Y offset, skips patterned missing beads, and draws.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (resolution <= 0) return;
        float padding = 2f;
        String shape = currentBead != null ? currentBead.getShape() : "round edged";

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (shouldBeMissing(r, c)) continue;
                
                float yOffset = (c % 2 == 1) ? resolution / 2f : 0;

                float l = c * resolution + padding;
                float t = r * resolution + yOffset + padding;
                float ri = l + resolution - 2 * padding;
                float bo = t + resolution - 2 * padding;

                if (currentBead != null) {
                    float hInset = currentBead.getHorizontalInset(resolution, false);
                    float vInset = currentBead.getVerticalInset(resolution, false);
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
                            canvas.drawLine(l + 4, t + (bo-t) * 0.35f, ri - 4, t + (bo-t) * 0.35f, greyLinePaint);
                            canvas.drawLine(l + 4, t + (bo-t) * 0.65f, ri - 4, t + (bo-t) * 0.65f, whiteLinePaint);
                        }
                    } else {
                        cellPaint.setColor(color);
                        drawBead(canvas, l, t, ri, bo, shape, cellPaint);
                    }
                }
                canvas.drawRect(c * resolution, r * resolution + yOffset, c * resolution + resolution, r * resolution + yOffset + resolution, paint);
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
        int desiredWidth = columns * resolution;
        int desiredHeight = (int) (rows * resolution + resolution / 2f);
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), 
                             resolveSize(desiredHeight, heightMeasureSpec));
    }
}
