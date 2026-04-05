package com.example.pictobeads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

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
    protected int maxColors = 20; // Default limit
    protected List<Integer> currentPalette = new ArrayList<>();

    public b_gradle(Context context, int columns, int rows, int resolution, int g_type) {
        super(context);
        this.columns = columns;
        this.rows = rows;
        this.resolution = resolution;
        this.g_type = g_type;
        this.pixelColors = new int[rows][columns];
    }

    public void setBead(Bead bead) {
        this.currentBead = bead;
        invalidate();
    }

    public void setRemoveBackground(boolean remove) {
        this.removeBackground = remove;
        invalidate();
    }

    public void setMaxColors(int maxColors) {
        this.maxColors = maxColors;
    }

    public List<Integer> getCurrentPalette() {
        return currentPalette;
    }

    public abstract void setImageData(Bitmap bitmap);

    /**
     * Applies color quantization and updates the currentPalette.
     */
    protected void applyColorQuantization() {
        if (maxColors > 0) {
            currentPalette = ColorQuantizer.quantize(pixelColors, maxColors, removeBackground);
        } else {
            currentPalette.clear();
        }
    }

    protected boolean isWhite(int color) {
        if (!removeBackground) return false;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
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
