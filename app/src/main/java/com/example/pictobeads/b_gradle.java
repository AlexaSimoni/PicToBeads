package com.example.pictobeads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

/**
 * Base abstract class for custom grid views that render bead patterns.
 */
public abstract class b_gradle extends View {
    protected int columns;
    protected int rows;
    protected int resolution;
    protected int g_type; // 1=step,2=half step
    protected int[][] pixelColors;
    protected Bead currentBead;
    protected boolean removeBackground = false;

    /**
     * Constructs a base grid view.
     * Input: context - Android context, columns - grid width, rows - grid height, resolution - visual scale, g_type - pattern step type.
     * Output: A new b_gradle instance.
     * Algorithm: Initializes grid dimensions and pixel color array.
     */
    public b_gradle(Context context, int columns, int rows, int resolution, int g_type) {
        super(context);
        this.columns = columns;
        this.rows = rows;
        this.resolution = resolution;
        this.g_type = g_type;
        this.pixelColors = new int[rows][columns];
    }

    /**
     * Sets the current bead type and refreshes the view.
     * Input: bead - Bead object containing physical and visual properties.
     * Output: None.
     * Algorithm: Updates the currentBead field and calls invalidate() to trigger a redraw.
     */
    public void setBead(Bead bead) {
        this.currentBead = bead;
        invalidate();
    }

    /**
     * Toggles background removal and refreshes the view.
     * Input: remove - boolean flag.
     * Output: None.
     * Algorithm: Updates the removeBackground field and calls invalidate().
     */
    public void setRemoveBackground(boolean remove) {
        this.removeBackground = remove;
        invalidate();
    }

    /**
     * Abstract method to process image data into the grid.
     * Input: bitmap - Source image bitmap.
     * Output: None.
     * Algorithm: Subclasses should map bitmap pixels to the pixelColors array.
     */
    public abstract void setImageData(Bitmap bitmap);

    /**
     * Determines if a color should be treated as transparent background.
     * Input: color - pixel color integer.
     * Output: boolean true if background should be removed.
     * Algorithm: Checks if removeBackground is enabled and if RGB values exceed a high threshold (near white).
     */
    protected boolean isWhite(int color) {
        if (!removeBackground) return false;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        // Define white as very bright pixels
        return r > 240 && g > 240 && b > 240;
    }

    public int getColumns() { return columns; }
    public void setColumns(int columns) { this.columns = columns; }
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    public int getResolution() { return resolution; }
    public void setResolution(int resolution) { this.resolution = resolution; }
    public int getGr_type() { return g_type; }
    public void setGr_type(int g_type) { this.g_type = g_type; }
}
