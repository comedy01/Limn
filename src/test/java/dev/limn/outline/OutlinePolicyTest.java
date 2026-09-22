package dev.limn.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class OutlinePolicyTest {
    @Test
    void modeNormalizesToTheTwoKnownStyles() {
        assertEquals(OutlinePolicy.MODE_RAINBOW, OutlinePolicy.normalizeMode("Rainbow"));
        assertEquals(OutlinePolicy.MODE_RAINBOW, OutlinePolicy.normalizeMode("  rainbow "));
        assertEquals(OutlinePolicy.MODE_SOLID, OutlinePolicy.normalizeMode("Solid Color"));
        assertEquals(OutlinePolicy.MODE_SOLID, OutlinePolicy.normalizeMode("nonsense"));
        assertEquals(OutlinePolicy.MODE_SOLID, OutlinePolicy.normalizeMode(null));
        assertTrue(OutlinePolicy.isRainbow("rainbow"));
        assertFalse(OutlinePolicy.isRainbow(null));
    }

    @Test
    void numbersClampAndRejectNonFiniteValues() {
        assertEquals(OutlinePolicy.MIN_WIDTH, OutlinePolicy.clampWidth(-3.0D));
        assertEquals(OutlinePolicy.MAX_WIDTH, OutlinePolicy.clampWidth(99.0D));
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, OutlinePolicy.clampWidth(Double.NaN));
        assertEquals(OutlinePolicy.MIN_SPEED, OutlinePolicy.clampSpeed(0.0D));
        assertEquals(OutlinePolicy.DEFAULT_SPEED,
                OutlinePolicy.clampSpeed(Double.POSITIVE_INFINITY));
        assertEquals(OutlinePolicy.MAX_SPREAD, OutlinePolicy.clampSpread(10.0D));
        assertEquals(0.0D, OutlinePolicy.clampSpread(-1.0D));
    }

    @Test
    void widthScalesTheVanillaLineWidth() {
        assertEquals(2.0F, OutlinePolicy.scaledWidth(2.0F, 1.0D));
        assertEquals(4.0F, OutlinePolicy.scaledWidth(2.0F, 2.0D));
        assertEquals(1.0F, OutlinePolicy.scaledWidth(2.0F, 0.0D));
        assertEquals(0.0F, OutlinePolicy.scaledWidth(-1.0F, 1.0D));
    }

    @Test
    void transparentSolidColorFallsBackToOpaque() {
        assertEquals(0xFF123456, OutlinePolicy.solidColor(0x00123456));
        assertEquals(0x80123456, OutlinePolicy.solidColor(0x80123456));
    }

    @Test
    void hueStaysInRangeAndMovesWithTime() {
        for (double seconds = -10.0D; seconds < 50.0D; seconds += 1.7D) {
            double hue = OutlinePolicy.hue(seconds, 1.0D, 1.0D, 0.3D, 0.6D, 0.9D);
            assertTrue(hue >= 0.0D && hue < 1.0D, "hue " + hue);
        }
        double early = OutlinePolicy.hue(0.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        double later = OutlinePolicy.hue(1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        assertEquals(0.25D, later - early, 1.0E-9D);
    }

    @Test
    void speedScalesTheCycleAndCompletesItInFourSecondsAtOne() {
        assertEquals(
                OutlinePolicy.hue(0.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D),
                OutlinePolicy.hue(4.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D),
                1.0E-9D);
        assertEquals(
                OutlinePolicy.hue(2.0D, 2.0D, 1.0D, 0.0D, 0.0D, 0.0D),
                OutlinePolicy.hue(4.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D),
                1.0E-9D);
    }

    @Test
    void spreadDecidesWhetherPositionChangesTheHue() {
        double a = OutlinePolicy.hue(3.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        double b = OutlinePolicy.hue(3.0D, 1.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        assertEquals(a, b, 1.0E-9D);
        double c = OutlinePolicy.hue(3.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        double d = OutlinePolicy.hue(3.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D);
        assertNotEquals(c, d);
        assertEquals(1.0D / 3.0D, OutlinePolicy.hue(0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D), 1.0E-9D);
    }

    @Test
    void rainbowHitsThePrimaryAndSecondaryHues() {
        assertEquals(0xFFFF0000, OutlinePolicy.rainbowArgb(0.0D, 0xFF));
        assertEquals(0xFFFFFF00, OutlinePolicy.rainbowArgb(1.0D / 6.0D, 0xFF));
        assertEquals(0xFF00FF00, OutlinePolicy.rainbowArgb(1.0D / 3.0D, 0xFF));
        assertEquals(0xFF00FFFF, OutlinePolicy.rainbowArgb(0.5D, 0xFF));
        assertEquals(0xFF0000FF, OutlinePolicy.rainbowArgb(2.0D / 3.0D, 0xFF));
        assertEquals(0xFFFF00FF, OutlinePolicy.rainbowArgb(5.0D / 6.0D, 0xFF));
    }

    @Test
    void rainbowKeepsTheRequestedAlphaAndWrapsOutOfRangeHues() {
        assertEquals(0x66FF0000, OutlinePolicy.rainbowArgb(0.0D, 0x66));
        assertEquals(
                OutlinePolicy.rainbowArgb(0.25D, 0xFF),
                OutlinePolicy.rainbowArgb(1.25D, 0xFF));
        assertEquals(
                OutlinePolicy.rainbowArgb(0.75D, 0xFF),
                OutlinePolicy.rainbowArgb(-0.25D, 0xFF));
    }

    @Test
    void everyRainbowColorIsFullyOpaqueBrightAndNeverBlack() {
        for (int i = 0; i < 360; i++) {
            int argb = OutlinePolicy.rainbowArgb(i / 360.0D, 0xFF);
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            assertEquals(255, Math.max(r, Math.max(g, b)), "hue " + i);
            assertEquals(0, Math.min(r, Math.min(g, b)), "hue " + i);
        }
    }

    @Test
    void edgeSegmentsFollowLengthWithinBounds() {
        assertEquals(1, OutlinePolicy.segmentCount(0.0D));
        assertEquals(1, OutlinePolicy.segmentCount(Double.NaN));
        assertEquals(8, OutlinePolicy.segmentCount(1.0D));
        assertEquals(4, OutlinePolicy.segmentCount(0.5D));
        assertEquals(OutlinePolicy.MAX_SEGMENTS, OutlinePolicy.segmentCount(500.0D));
    }
}
