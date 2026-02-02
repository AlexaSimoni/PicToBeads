package com.example.b;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

public abstract class b_gradle extends View {
    protected int columns;
    protected int rows;
    protected int resolution;
    protected int g_type; // 1=step,2=half step
    protected int[][] pixelColors;
    protected Bead currentBead;
    protected boolean removeBackground = false;

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

    public abstract void setImageData(Bitmap bitmap);

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
