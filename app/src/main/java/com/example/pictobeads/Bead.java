package com.example.pictobeads;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a bead with its physical properties and visual modifications.
 */
public class Bead {
    private final float size; 
    private final String shape; 
    private final String texture; 

    /**
     * Constructs a new Bead.
     * Input: size - physical size in mm, shape - bead shape name, texture - visual texture name.
     * Output: A new Bead instance.
     * Algorithm: Assigns input parameters to final member variables.
     */
    public Bead(float size, String shape, String texture) {
        this.size = size;
        this.shape = shape;
        this.texture = texture;
    }

    /**
     * Returns the bead size.
     * Input: None.
     * Output: size - float representing size in mm.
     * Algorithm: Returns the private size field.
     */
    public float getSize() { return size; }

    /**
     * Returns the bead shape.
     * Input: None.
     * Output: shape - String representing the shape.
     * Algorithm: Returns the private shape field.
     */
    public String getShape() { return shape; }

    /**
     * Returns the bead texture.
     * Input: None.
     * Output: texture - String representing the texture.
     * Algorithm: Returns the private texture field.
     */
    public String getTexture() { return texture; }

    /**
     * Applies texture-specific color transformations based on the bead's texture property.
     * Input: originalColor - The base color to modify.
     * Output: The resulting modified color as an integer.
     * Algorithm: Extracts RGB components and applies mathematical scaling or grayscale conversion based on the texture string.
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

    /**
     * Calculates the horizontal inset for drawing based on orientation and shape.
     * Input: resolution - drawing resolution, horizontalOrientation - layout flag.
     * Output: The inset value as a float.
     * Algorithm: Uses shape and size to determine a proportional inset for specific bead types.
     */
    public float getHorizontalInset(float resolution, boolean horizontalOrientation) {
        if (horizontalOrientation) {
            // Standard: square beads are thiner
            return "square".equals(shape) ? resolution * 0.15f : 0;
        } else {
            // Vertical: 9mm beads are thiner
            return size == 9 ? resolution * 0.15f : 0;
        }
    }

    /**
     * Calculates the vertical inset for drawing based on orientation and shape.
     * Input: resolution - drawing resolution, horizontalOrientation - layout flag.
     * Output: The inset value as a float.
     * Algorithm: Uses shape and size to determine a proportional vertical inset.
     */
    public float getVerticalInset(float resolution, boolean horizontalOrientation) {
        if (horizontalOrientation) {
            // Standard: 9mm beads are shorter
            return size == 9 ? resolution * 0.15f : 0;
        } else {
            // Vertical: square beads are shorter
            return "square".equals(shape) ? resolution * 0.15f : 0;
        }
    }

    /**
     * Returns the wood texture stroke count.
     * Input: None.
     * Output: int count of strokes.
     * Algorithm: Returns 5 if texture is "wood matt", otherwise 0.
     */
    public int getWoodStrokeCount() {
        return "wood matt".equals(texture) ? 5 : 0;
    }

    /**
     * Determines if a highlight dot should be drawn.
     * Input: None.
     * Output: boolean flag.
     * Algorithm: Returns true for specific shiny or glass textures.
     */
    public boolean shouldDrawWhiteDot() {
        return "shiny glass".equals(texture) || "transparent glass".equals(texture) || "metallic".equals(texture);
    }

    /**
     * Checks if the bead has square visual traits.
     * Input: None.
     * Output: boolean flag.
     * Algorithm: Returns true if shape is "square".
     */
    public boolean hasSquareStrokes() {
        return "square".equals(shape);
    }

    /**
     * Provides default bead configurations.
     * Input: None.
     * Output: List of Bead objects.
     * Algorithm: Creates an ArrayList and adds hardcoded standard bead configurations.
     */
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

    /**
     * Returns string representation.
     * Input: None.
     * Output: Formatted string.
     * Algorithm: Concatenates size, shape, and texture.
     */
    @Override
    public String toString() {
        return size + "mm " + shape + " (" + texture + ")";
    }
}
