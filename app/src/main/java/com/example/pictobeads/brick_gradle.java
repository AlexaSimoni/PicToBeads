package com.example.pictobeads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A grid view that renders beads in a brick-like (staggered) pattern.
 */
public class brick_gradle extends b_gradle {
    private Paint paint;
    private Paint cellPaint;
    private Paint dotPaint;
    private Paint woodPaint;
    private Paint greyLinePaint;
    private Paint whiteLinePaint;
    private boolean isVertical;

    /**
     * Constructs a brick grid view.
     * Input: context, columns, rows, resolution, g_type, isVertical.
     * Output: A new brick_gradle instance.
     * Algorithm: Calls super constructor and initializes Paint objects.
     */
    public brick_gradle(Context context, int columns, int rows, int resolution, int g_type, boolean isVertical) {
        super(context, columns, rows, resolution, g_type);
        this.isVertical = isVertical;
        init();
    }

    /**
     * Initializes Paint objects for drawing.
     * Input: None.
     * Output: None.
     * Algorithm: Configures Paint objects for borders, fills, and bead effects.
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
     * Maps image pixels to the staggered grid cells.
     * Input: bitmap - The source bitmap.
     * Output: None.
     * Algorithm: Iterates through each cell, calculates a staggered (x,y) offset based on row/column index, maps to a (u,v) coordinate, and samples the bitmap.
     */
    @Override
    public void setImageData(Bitmap bitmap) {
        if (bitmap == null) return;

        float gridWidth = isVertical ? columns * resolution : columns * resolution + resolution / 2f;
        float gridHeight = isVertical ? rows * resolution + resolution / 2f : rows * resolution;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                float xOffset = (!isVertical && r % 2 == 1) ? resolution / 2f : 0;
                float yOffset = (isVertical && c % 2 == 1) ? resolution / 2f : 0;

                float centerX = c * resolution + xOffset + resolution / 2f;
                float centerY = r * resolution + yOffset + resolution / 2f;

                float u = Math.min(1f, Math.max(0f, centerX / gridWidth));
                float v = Math.min(1f, Math.max(0f, centerY / gridHeight));

                int bX = (int) (u * (bitmap.getWidth() - 1));
                int bY = (int) (v * (bitmap.getHeight() - 1));
                
                pixelColors[r][c] = bitmap.getPixel(bX, bY);
            }
        }
        invalidate();
    }

    /**
     * Renders the staggered bead grid onto the canvas.
     * Input: canvas - The canvas to draw on.
     * Output: None.
     * Algorithm: Iterates through each cell, applies a staggered offset, calculates drawing bounds, and draws the bead with visual effects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (resolution <= 0) return;

        float padding = 2f;
        String shape = currentBead != null ? currentBead.getShape() : "round edged";

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                float xOffset = (!isVertical && r % 2 == 1) ? resolution / 2f : 0;
                float yOffset = (isVertical && c % 2 == 1) ? resolution / 2f : 0;

                float l = c * resolution + xOffset + padding;
                float t = r * resolution + yOffset + padding;
                float ri = l + resolution - 2 * padding;
                float bo = t + resolution - 2 * padding;

                if (currentBead != null) {
                    boolean horizOrient = !isVertical;
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
                            if (!isVertical) { // Horizontal: vertical lines
                                canvas.drawLine(l + (ri-l) * 0.35f, t + 4, l + (ri-l) * 0.35f, bo - 4, greyLinePaint);
                                canvas.drawLine(l + (ri-l) * 0.65f, t + 4, l + (ri-l) * 0.65f, bo - 4, whiteLinePaint);
                            } else { // Vertical: horizontal lines
                                canvas.drawLine(l + 4, t + (bo-t) * 0.35f, ri - 4, t + (bo-t) * 0.35f, greyLinePaint);
                                canvas.drawLine(l + 4, t + (bo-t) * 0.65f, ri - 4, t + (bo-t) * 0.65f, whiteLinePaint);
                            }
                        }
                    } else {
                        cellPaint.setColor(color);
                        drawBead(canvas, l, t, ri, bo, shape, cellPaint);
                    }
                }
                canvas.drawRect(c * resolution + xOffset, r * resolution + yOffset, c * resolution + xOffset + resolution, r * resolution + yOffset + resolution, paint);
            }
        }
    }

    /**
     * Draws a single bead shape.
     * Input: canvas, bounds (l,t,r,b), shape string, paint object.
     * Output: None.
     * Algorithm: Draws a rect, oval, or roundrect based on the shape string.
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
     * Determines the view's dimensions.
     * Input: widthMeasureSpec, heightMeasureSpec.
     * Output: None.
     * Algorithm: Sets the measured dimension based on column/row count, resolution, and staggered offsets.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) (columns * resolution + (!isVertical ? resolution / 2f : 0));
        int desiredHeight = (int) (rows * resolution + (isVertical ? resolution / 2f : 0));
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), 
                             resolveSize(desiredHeight, heightMeasureSpec));
    }
}
