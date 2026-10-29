package dev.limn.outline;

public final class OutlinePolicy {
    public static final String MODE_SOLID = "Solid Color";
    public static final String MODE_RAINBOW = "Rainbow";
    public static final String MODE_CHASE = "Chase";
    public static final String MODE_MINING = "Mining Progress";

    private static final String[] MODES = {MODE_SOLID, MODE_RAINBOW, MODE_CHASE, MODE_MINING};
    private static final String[] MODE_KEYS = {"solid", "rainbow", "chase", "mining"};

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

    static final double CHASE_BAND = 0.2D;
    static final double CHASE_BASE_ALPHA = 0.25D;
    static final double CHASE_HIGHLIGHT = 0.5D;
    static final int MINING_COLOR = 0xFF2020;

    private OutlinePolicy() {
    }

    public static String normalizeMode(String mode) {
        return MODES[modeIndex(mode)];
    }

    public static String nextMode(String mode) {
        return MODES[(modeIndex(mode) + 1) % MODES.length];
    }

    public static String modeKey(String mode) {
        return MODE_KEYS[modeIndex(mode)];
    }

    private static int modeIndex(String mode) {
        String trimmed = mode == null ? "" : mode.trim();
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equalsIgnoreCase(trimmed)) {
                return i;
            }
        }
        return 0;
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

    public static boolean variesAlongEdges(String mode, double spread) {
        String normalized = normalizeMode(mode);
        return (MODE_RAINBOW.equals(normalized) || MODE_CHASE.equals(normalized))
                && clampSpread(spread) > 0.0D;
    }

    public static int colorAt(
            String mode,
            int solid,
            double seconds,
            double speed,
            double spread,
            double progress,
            double x,
            double y,
            double z) {
        String normalized = normalizeMode(mode);
        if (MODE_RAINBOW.equals(normalized)) {
            return rainbowArgb(hue(seconds, speed, spread, x, y, z), solid >>> 24);
        }
        if (MODE_CHASE.equals(normalized)) {
            return chaseArgb(solid, hue(seconds, speed, spread, x, y, z));
        }
        if (MODE_MINING.equals(normalized)) {
            return miningArgb(solid, progress);
        }
        return solid;
    }

    public static int chaseArgb(int solid, double phase) {
        double wrapped = ((phase % 1.0D) + 1.0D) % 1.0D;
        double distance = Math.min(wrapped, 1.0D - wrapped);
        double linear = Math.max(0.0D, 1.0D - distance / CHASE_BAND);
        double glow = linear * linear * (3.0D - 2.0D * linear);
        int alpha = (int) Math.round((solid >>> 24) * (CHASE_BASE_ALPHA + (1.0D - CHASE_BASE_ALPHA) * glow));
        return (alpha << 24) | mix(solid, 0xFFFFFF, CHASE_HIGHLIGHT * glow);
    }

    public static int miningArgb(int solid, double progress) {
        double amount = Double.isFinite(progress) ? Math.max(0.0D, Math.min(1.0D, progress)) : 0.0D;
        int alpha = solid & 0xFF000000;
        if (amount <= 0.0D) {
            return solid;
        }
        if (amount >= 1.0D) {
            return alpha | MINING_COLOR;
        }
        double[] from = hsv(solid);
        double[] to = hsv(MINING_COLOR);
        double turn = to[0] - from[0];
        if (turn > 0.5D) {
            turn -= 1.0D;
        } else if (turn < -0.5D) {
            turn += 1.0D;
        }
        double hue = from[1] == 0.0D ? to[0] : from[0] + turn * amount;
        double saturation = from[1] + (to[1] - from[1]) * amount;
        double value = from[2] + (to[2] - from[2]) * amount;
        return alpha | scale(rainbowArgb(hue, 0), saturation, value);
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

    private static double[] hsv(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0D;
        double g = ((rgb >> 8) & 0xFF) / 255.0D;
        double b = (rgb & 0xFF) / 255.0D;
        double max = Math.max(r, Math.max(g, b));
        double range = max - Math.min(r, Math.min(g, b));
        double hue = 0.0D;
        if (range > 0.0D) {
            if (max == r) {
                hue = ((g - b) / range + 6.0D) % 6.0D;
            } else if (max == g) {
                hue = (b - r) / range + 2.0D;
            } else {
                hue = (r - g) / range + 4.0D;
            }
        }
        return new double[] {hue / 6.0D, max == 0.0D ? 0.0D : range / max, max};
    }

    private static int scale(int pureHue, double saturation, double value) {
        int r = channel(value * (1.0D - saturation * (1.0D - ((pureHue >> 16) & 0xFF) / 255.0D)));
        int g = channel(value * (1.0D - saturation * (1.0D - ((pureHue >> 8) & 0xFF) / 255.0D)));
        int b = channel(value * (1.0D - saturation * (1.0D - (pureHue & 0xFF) / 255.0D)));
        return pack(r, g, b);
    }

    private static int mix(int from, int to, double amount) {
        int r = mixChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, amount);
        int g = mixChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, amount);
        int b = mixChannel(from & 0xFF, to & 0xFF, amount);
        return pack(r, g, b);
    }

    private static int mixChannel(int from, int to, double amount) {
        return (int) Math.round(from + (to - from) * amount);
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
