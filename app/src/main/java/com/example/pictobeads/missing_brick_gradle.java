package com.example.pictobeads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A custom grid view that renders beads in a staggered brick pattern with intentional gaps.
 * If a row ends with a missing bead, an extra bead is added to ensure the row ends visibly.
 */
public class missing_brick_gradle extends b_gradle {
    private Paint paint;
    private Paint cellPaint;
    private Paint dotPaint;
    private Paint woodPaint;
    private Paint greyLinePaint;
    private Paint whiteLinePaint;

    /**
     * Initializes the staggered grid view with missing bricks and potential extra end-beads.
     * Input: context - application context, columns - base number of beads wide, rows - number of beads high, resolution - size of one bead, g_type - pattern type.
     * Output: None.
     * Algorithm: Calls super constructor, re-allocates pixelColors to allow for one extra column (n+1), and initializes paint objects.
     */
    public missing_brick_gradle(Context context, int columns, int rows, int resolution, int g_type) {
        super(context, columns, rows, resolution, g_type);
        // Re-allocate to handle up to 'columns' index (total columns + 1)
        this.pixelColors = new int[rows][columns + 1];
        init();
    }

    /**
     * Configures the Paint objects used for rendering.
     * Input: None.
     * Output: None.
     * Algorithm: Instantiates and sets properties like anti-aliasing, color, and stroke style for various UI elements.
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
     * Processes an image to determine grid cell colors, including extra end-beads.
     * Input: bitmap - the source image to be converted.
     * Output: None.
     * Algorithm: Calculates grid dimensions, iterates through non-missing cells (up to columns index), maps their center to image coordinates, and samples pixel colors. It then applies color variety limitation.
     */
    @Override
    public void setImageData(Bitmap bitmap) {
        if (bitmap == null) return;
        float gridWidth = (columns + 1) * resolution + resolution / 2f;
        float gridHeight = rows * resolution;
        for (int r = 0; r < rows; r++) {
            float xOffset = (r % 2 == 0) ? resolution / 2f : 0;
            for (int c = 0; c <= columns; c++) {
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
        applyColorQuantization();
        invalidate();
    }

    /**
     * Determines if a specific grid cell should be empty, handling the extra end-bead logic.
     * Input: r - row index, c - column index.
     * Output: boolean - true if the cell is missing.
     * Algorithm: Checks row and column modulo values. For the extra column (c == columns), returns false only if the previous bead was missing.
     */
    private boolean shouldBeMissing(int r, int c) {
        int patternRow = r % 4;
        
        if (c == columns) {
            // Row 2: index 1, Row 4: index 3
            // Add extra bead only if the pattern would have ended the row with a missing bead.
            if (patternRow == 1) return (columns - 1) % 2 != 0; // If index 'columns-1' is odd, it stayed, so 'columns' is missing.
            if (patternRow == 3) return (columns - 1) % 2 == 0; // If index 'columns-1' is even, it stayed, so 'columns' is missing.
            return true; // No extra bead for Row 1 and 3.
        }

        if (patternRow == 1) return c % 2 == 0;
        if (patternRow == 3) return c % 2 == 1;
        return false;
    }

    /**
     * Renders the custom grid pattern on a Canvas, including extra end-beads.
     * Input: canvas - the destination for drawing operations.
     * Output: None.
     * Algorithm: Iterates through visible cells (up to columns index), calculates drawing coordinates with staggered offsets and bead insets, and draws bead shapes and textures.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (resolution <= 0) return;
        float padding = 2f;
        String shape = currentBead != null ? currentBead.getShape() : "round edged";

        for (int r = 0; r < rows; r++) {
            float xOffset = (r % 2 == 0) ? resolution / 2f : 0;
            for (int c = 0; c <= columns; c++) {
                if (shouldBeMissing(r, c)) continue;

                float l = c * resolution + xOffset + padding;
                float t = r * resolution + padding;
                float ri = l + resolution - 2 * padding;
                float bo = t + resolution - 2 * padding;

                boolean horizOrient = true; 

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
                            canvas.drawLine(l + (ri-l) * 0.35f, t + 4, l + (ri-l) * 0.35f, bo - 4, greyLinePaint);
                            canvas.drawLine(l + (ri-l) * 0.65f, t + 4, l + (ri-l) * 0.65f, bo - 4, whiteLinePaint);
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
        // Account for potential extra bead at index 'columns'
        int desiredWidth = (int) ((columns + 1) * resolution + resolution / 2f);
        int desiredHeight = rows * resolution;
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), 
                             resolveSize(desiredHeight, heightMeasureSpec));
    }
}
