package com.example.pictobeads;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight Color Quantizer using a weighted Median Cut algorithm.
 * Optimized for performance by processing unique colors and using a mapping cache.
 * Unions similar shades into shells while strictly respecting the user's requested color count.
 */
public class ColorQuantizer {

    /**
     * Quantizes the grid colors.
     * Uses a frequency map to reduce the input size for Median Cut, ensuring fast performance.
     */
    public static List<Integer> quantize(int[][] grid, int maxColors, boolean removeBg) {
        if (maxColors <= 0) return new ArrayList<>();

        int rows = grid.length;
        int cols = grid[0].length;

        // 1. Build Frequency Map (O(N) - avoids processing redundant pixels)
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                int color = grid[r][c];
                if (color == 0) continue;
                if (removeBg && isNearWhite(color)) {
                    grid[r][c] = Color.WHITE;
                    continue;
                }
                freqMap.put(color, freqMap.getOrDefault(color, 0) + 1);
            }
        }

        if (freqMap.isEmpty()) return new ArrayList<>();

        // 2. Perform Median Cut on unique colors (weighted)
        List<Integer> palette;
        if (freqMap.size() <= maxColors) {
            palette = new ArrayList<>(freqMap.keySet());
        } else {
            palette = performWeightedMedianCut(freqMap, maxColors);
        }

        // 3. Map pixels back to the vibrant palette using a cache
        Map<Integer, Integer> cache = new HashMap<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                int color = grid[r][c];
                if (color == 0 || color == Color.WHITE) continue;

                Integer mapped = cache.get(color);
                if (mapped == null) {
                    mapped = findNearestColor(color, palette);
                    cache.put(color, mapped);
                }
                grid[r][c] = mapped;
            }
        }
        return palette;
    }

    private static List<Integer> performWeightedMedianCut(Map<Integer, Integer> freqMap, int maxColors) {
        List<ColorCount> colorCounts = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            colorCounts.add(new ColorCount(entry.getKey(), entry.getValue()));
        }

        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(colorCounts));

        while (boxes.size() < maxColors) {
            Box boxToSplit = null;
            int maxRange = -1;
            for (Box box : boxes) {
                if (box.colors.size() > 1) {
                    int range = box.getLargestRange();
                    if (range > maxRange) {
                        maxRange = range;
                        boxToSplit = box;
                    }
                }
            }
            if (boxToSplit == null) break;
            boxes.remove(boxToSplit);
            boxes.addAll(boxToSplit.split());
        }

        List<Integer> palette = new ArrayList<>();
        for (Box box : boxes) {
            palette.add(box.getVibrantRepresentative());
        }
        return palette;
    }

    private static class ColorCount {
        int color;
        int count;
        ColorCount(int color, int count) {
            this.color = color;
            this.count = count;
        }
    }

    private static class Box {
        List<ColorCount> colors;
        int minR, maxR, minG, maxG, minB, maxB;

        Box(List<ColorCount> colors) {
            this.colors = colors;
            calculateBounds();
        }

        private void calculateBounds() {
            minR = minG = minB = 255;
            maxR = maxG = maxB = 0;
            for (ColorCount cc : colors) {
                int c = cc.color;
                int r = Color.red(c), g = Color.green(c), b = Color.blue(c);
                if (r < minR) minR = r; if (r > maxR) maxR = r;
                if (g < minG) minG = g; if (g > maxG) maxG = g;
                if (b < minB) minB = b; if (b > maxB) maxB = b;
            }
        }

        int getLargestRange() {
            return Math.max(maxR - minR, Math.max(maxG - minG, maxB - minB));
        }

        List<Box> split() {
            int rRange = maxR - minR, gRange = maxG - minG, bRange = maxB - minB;
            if (rRange >= gRange && rRange >= bRange) {
                Collections.sort(colors, (a, b) -> Integer.compare(Color.red(a.color), Color.red(b.color)));
            } else if (gRange >= rRange && gRange >= bRange) {
                Collections.sort(colors, (a, b) -> Integer.compare(Color.green(a.color), Color.green(b.color)));
            } else {
                Collections.sort(colors, (a, b) -> Integer.compare(Color.blue(a.color), Color.blue(b.color)));
            }

            long total = 0;
            for (ColorCount cc : colors) total += cc.count;
            
            long current = 0;
            int splitIndex = 1;
            for (int i = 0; i < colors.size() - 1; i++) {
                current += colors.get(i).count;
                if (current >= total / 2) {
                    splitIndex = i + 1;
                    break;
                }
            }

            List<Box> result = new ArrayList<>();
            result.add(new Box(new ArrayList<>(colors.subList(0, splitIndex))));
            result.add(new Box(new ArrayList<>(colors.subList(splitIndex, colors.size()))));
            return result;
        }

        int getVibrantRepresentative() {
            if (colors.isEmpty()) return 0;
            long r = 0, g = 0, b = 0, total = 0;
            for (ColorCount cc : colors) {
                r += (long) Color.red(cc.color) * cc.count;
                g += (long) Color.green(cc.color) * cc.count;
                b += (long) Color.blue(cc.color) * cc.count;
                total += cc.count;
            }
            int avgR = (int)(r / total), avgG = (int)(g / total), avgB = (int)(b / total);

            // Boost contrast slightly away from middle grey to keep colors vibrant
            float factor = 1.4f;
            avgR = Math.max(0, Math.min(255, (int)((avgR - 128) * factor + 128)));
            avgG = Math.max(0, Math.min(255, (int)((avgG - 128) * factor + 128)));
            avgB = Math.max(0, Math.min(255, (int)((avgB - 128) * factor + 128)));

            return Color.rgb(avgR, avgG, avgB);
        }
    }

    private static int findNearestColor(int target, List<Integer> palette) {
        int nearest = palette.get(0);
        double minDistance = Double.MAX_VALUE;
        int tr = Color.red(target), tg = Color.green(target), tb = Color.blue(target);
        for (int pColor : palette) {
            double dist = Math.pow(tr - Color.red(pColor), 2) + 
                          Math.pow(tg - Color.green(pColor), 2) + 
                          Math.pow(tb - Color.blue(pColor), 2);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = pColor;
            }
        }
        return nearest;
    }

    private static boolean isNearWhite(int color) {
        return Color.red(color) > 240 && Color.green(color) > 240 && Color.blue(color) > 240;
    }
}
