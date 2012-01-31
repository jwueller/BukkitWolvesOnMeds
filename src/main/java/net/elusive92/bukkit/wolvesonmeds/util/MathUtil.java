package net.elusive92.bukkit.wolvesonmeds.util;

public class MathUtil {

    /**
     * Ensures that a values does not leave a range.
     *
     * @param value
     * @param min
     * @param max
     * @return clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
}
