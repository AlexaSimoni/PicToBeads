package com.example.pictobeads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A grid view that renders beads in a standard square (checkerboard) pattern.
 */
public class math_gradle extends b_gradle {
    private Paint paint;
    private Paint cellPaint;
    private Paint dotPaint;
    private Paint woodPaint;
    private Paint greyLinePaint;
    private Paint whiteLinePaint;

    /**
     * Initializes the math grid view and its paint components.
     * Input: context - context, columns - grid width, rows - grid height, resolution - visual scale, g_type - pattern type.
     * Output: math_gradle instance.
     * Algorithm: Calls super constructor and executes init() to configure Paint objects for drawing.
     */
    public math_gradle(Context context, int columns, int rows, int resolution, int g_type) {
        super(context, columns, rows, resolution, g_type);
        init();
    }

    /**
     * Configures the visual appearance of the grid elements.
     * Input: None.
     * Output: None.
     * Algorithm: Instantiates multiple Paint objects for drawing grid lines, bead fills, wood textures, and highlights.
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
     * Samples image colors to populate the mosaic grid.
     * Input: bitmap - source image.
     * Output: None.
     * Algorithm: Iterates through each cell, calculates proportional coordinates (u,v), samples the nearest bitmap pixel, and stores it in pixelColors.
     */
    @Override
    public void setImageData(Bitmap bitmap) {
        if (bitmap == null) return;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                float u = (c + 0.5f) / columns;
                float v = (r + 0.5f) / rows;
                int bX = (int) (u * (bitmap.getWidth() - 1));
                int bY = (int) (v * (bitmap.getHeight() - 1));
                bX = Math.max(0, Math.min(bX, bitmap.getWidth() - 1));
                bY = Math.max(0, Math.min(bY, bitmap.getHeight() - 1));
                pixelColors[r][c] = bitmap.getPixel(bX, bY);
            }
        }
        invalidate();
    }

    /**
     * Renders the bead pattern on the screen.
     * Input: canvas - canvas to draw on.
     * Output: None.
     * Algorithm: Loops through cells, calculates physical bounds with insets, draws the bead shape, and applies texture overlays like dots or wood strokes.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (resolution <= 0) return;

        float padding = 2f;
        String shape = currentBead != null ? currentBead.getShape() : "round edged";

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                float l = c * resolution + padding;
                float t = r * resolution + padding;
                float ri = (c + 1) * resolution - padding;
                float bo = (r + 1) * resolution - padding;

                if (currentBead != null) {
                    float hInset = currentBead.getHorizontalInset(resolution, true);
                    float vInset = currentBead.getVerticalInset(resolution, true);
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
                            canvas.drawLine(l + (ri-l) * 0.35f, t + 4, l + (ri-l) * 0.35f, bo - 4, greyLinePaint);
                            canvas.drawLine(l + (ri-l) * 0.65f, t + 4, l + (ri-l) * 0.65f, bo - 4, whiteLinePaint);
                        }
                    } else {
                        cellPaint.setColor(color);
                        drawBead(canvas, l, t, ri, bo, shape, cellPaint);
                    }
                }
                canvas.drawRect(c * resolution, r * resolution, (c + 1) * resolution, (r + 1) * resolution, paint);
            }
        }
    }

    /**
     * Executes drawing logic for a specific geometric bead shape.
     * Input: canvas, bounds (l,t,r,b), shape - shape string, p - Paint object.
     * Output: None.
     * Algorithm: Switches on the shape string to draw a rectangle, oval, or rounded rectangle using the provided paint.
     */
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

    /**
     * Measures the view to fit the grid content.
     * Input: widthMeasureSpec, heightMeasureSpec.
     * Output: None.
     * Algorithm: Calculates pixel dimensions as (columns * resolution) and (rows * resolution), resolving them according to Android measure specifications.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(columns * resolution, widthMeasureSpec), 
                             resolveSize(rows * resolution, heightMeasureSpec));
    }
}
