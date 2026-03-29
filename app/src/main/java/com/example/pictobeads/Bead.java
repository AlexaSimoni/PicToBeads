package com.example.pictobeads;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model class representing a bead with its physical properties and visual modifications.
 */
public class Bead {
    private final float size; 
    private final String shape; 
    private final String texture; 

    public Bead(float size, String shape, String texture) {
        this.size = size;
        this.shape = shape;
        this.texture = texture;
    }

    public float getSize() { return size; }
    public String getShape() { return shape; }
    public String getTexture() { return texture; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bead bead = (Bead) o;
        return Float.compare(bead.size, size) == 0 &&
                Objects.equals(shape, bead.shape) &&
                Objects.equals(texture, bead.texture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, shape, texture);
    }

    /**
     * Applies texture-specific color transformations based on the bead's texture property.
     */
    public int getModifiedColor(int originalColor) {
        int r = Color.red(originalColor);
        int g = Color.green(originalColor);
        int b = Color.blue(originalColor);

        switch (texture) {
            case "shiny glass":
                r = Math.min(255, (int)(r * 1.2f));
                g = Math.min(255, (int)(g * 1.2f));
                b = Math.min(255, (int)(b * 1.2f));
                break;
            case "transparent glass":
                r = (int)(r * 0.9f);
                g = (int)(g * 0.9f);
                b = (int)(b * 0.9f);
                break;
            case "solid color":
                float factor = 1.3f;
                r = Math.max(0, Math.min(255, (int)((r - 128) * factor + 128)));
                g = Math.max(0, Math.min(255, (int)((g - 128) * factor + 128)));
                b = Math.max(0, Math.min(255, (int)((b - 128) * factor + 128)));
                break;
            case "metallic":
                int grey = (int)(r * 0.299f + g * 0.587f + b * 0.114f);
                r = (int)(r * 0.8f + grey * 0.2f);
                g = (int)(g * 0.8f + grey * 0.2f);
                b = (int)(b * 0.8f + grey * 0.2f);
                break;
            case "wood matt":
                int greyWood = (int)(r * 0.299f + g * 0.587f + b * 0.114f);
                r = (int)(r * 0.95f + greyWood * 0.05f);
                g = (int)(g * 0.95f + greyWood * 0.05f);
                b = (int)(b * 0.95f + greyWood * 0.05f);
                break;
        }
        return Color.rgb(r, g, b);
    }

    public float getHorizontalInset(float resolution, boolean horizontalOrientation) {
        if (horizontalOrientation) {
            return "square".equals(shape) ? resolution * 0.15f : 0;
        } else {
            return size == 9 ? resolution * 0.15f : 0;
        }
    }

    public float getVerticalInset(float resolution, boolean horizontalOrientation) {
        if (horizontalOrientation) {
            return size == 9 ? resolution * 0.15f : 0;
        } else {
            return "square".equals(shape) ? resolution * 0.15f : 0;
        }
    }

    public int getWoodStrokeCount() {
        return "wood matt".equals(texture) ? 5 : 0;
    }

    public boolean shouldDrawWhiteDot() {
        return "shiny glass".equals(texture) || "transparent glass".equals(texture) || "metallic".equals(texture);
    }

    public boolean hasSquareStrokes() {
        return "square".equals(shape);
    }

    public static List<Bead> getStandardTypes() {
        List<Bead> beads = new ArrayList<>();
        beads.add(new Bead(2, "square", "shiny glass"));
        beads.add(new Bead(2, "round edged", "transparent glass"));
        beads.add(new Bead(3, "round edged", "solid color"));
        beads.add(new Bead(5, "round", "metallic"));
        beads.add(new Bead(8, "round", "wood matt"));
        beads.add(new Bead(8, "round", "shiny glass"));
        beads.add(new Bead(9, "round edged", "solid color"));
        return beads;
    }

    @Override
    public String toString() {
        return size + "mm " + shape + " (" + texture + ")";
    }
}
