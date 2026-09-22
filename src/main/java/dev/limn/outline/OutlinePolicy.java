package dev.limn.outline;

public final class OutlinePolicy {
    public static final String MODE_SOLID = "Solid Color";
    public static final String MODE_RAINBOW = "Rainbow";

    public static final int DEFAULT_COLOR = 0xFF33E0D0;

    public static final double MIN_WIDTH = 0.5D;
    public static final double MAX_WIDTH = 4.0D;
    public static final double DEFAULT_WIDTH = 1.0D;

    public static final double MIN_SPEED = 0.1D;
    public static final double MAX_SPEED = 5.0D;
    public static final double DEFAULT_SPEED = 1.0D;

    public static final double MIN_SPREAD = 0.0D;
    public static final double MAX_SPREAD = 3.0D;
    public static final double DEFAULT_SPREAD = 1.0D;

    static final double CYCLE_SECONDS = 4.0D;
    static final int SEGMENTS_PER_BLOCK = 8;
    static final int MAX_SEGMENTS = 32;

    private OutlinePolicy() {
    }

    public static String normalizeMode(String mode) {
        return MODE_RAINBOW.equalsIgnoreCase(mode == null ? "" : mode.trim())
                ? MODE_RAINBOW
                : MODE_SOLID;
    }

    public static boolean isRainbow(String mode) {
        return MODE_RAINBOW.equals(normalizeMode(mode));
    }

    public static double clampWidth(double width) {
        return clamp(width, MIN_WIDTH, MAX_WIDTH, DEFAULT_WIDTH);
    }

    public static double clampSpeed(double speed) {
        return clamp(speed, MIN_SPEED, MAX_SPEED, DEFAULT_SPEED);
    }

    public static double clampSpread(double spread) {
        return clamp(spread, MIN_SPREAD, MAX_SPREAD, DEFAULT_SPREAD);
    }

    public static float scaledWidth(float vanillaWidth, double multiplier) {
        return (float) (Math.max(0.0F, vanillaWidth) * clampWidth(multiplier));
    }

    public static int solidColor(int configured) {
        return (configured >>> 24) == 0 ? configured | 0xFF000000 : configured;
    }

    public static double hue(
            double seconds, double speed, double spread, double x, double y, double z) {
        double phase = seconds * clampSpeed(speed) / CYCLE_SECONDS;
        double offset = clampSpread(spread) * (x + y + z) / 3.0D;
        double hue = (phase + offset) % 1.0D;
        return hue < 0.0D ? hue + 1.0D : hue;
    }

    public static int rainbowArgb(double hue, int alpha) {
        double h = ((hue % 1.0D) + 1.0D) % 1.0D * 6.0D;
        int sector = (int) h;
        double f = h - sector;
        int rising = channel(f);
        int falling = channel(1.0D - f);
        int rgb = switch (sector) {
            case 0 -> pack(255, rising, 0);
            case 1 -> pack(falling, 255, 0);
            case 2 -> pack(0, 255, rising);
            case 3 -> pack(0, falling, 255);
            case 4 -> pack(rising, 0, 255);
            default -> pack(255, 0, falling);
        };
        return ((alpha & 0xFF) << 24) | rgb;
    }

    public static int segmentCount(double edgeLength) {
        if (!Double.isFinite(edgeLength) || edgeLength <= 0.0D) {
            return 1;
        }
        int count = (int) Math.ceil(edgeLength * SEGMENTS_PER_BLOCK);
        return Math.max(1, Math.min(MAX_SEGMENTS, count));
    }

    private static int channel(double fraction) {
        return (int) Math.round(Math.max(0.0D, Math.min(1.0D, fraction)) * 255.0D);
    }

    private static int pack(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    private static double clamp(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
