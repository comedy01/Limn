package dev.limn.outline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class OutlinePolicyTest {
    @Test
    void modeNormalizesToTheKnownStyles() {
        assertEquals(OutlinePolicy.MODE_RAINBOW, OutlinePolicy.normalizeMode("Rainbow"));
        assertEquals(OutlinePolicy.MODE_RAINBOW, OutlinePolicy.normalizeMode("  rainbow "));
        assertEquals(OutlinePolicy.MODE_SOLID, OutlinePolicy.normalizeMode("Solid Color"));
        assertEquals(OutlinePolicy.MODE_SOLID, OutlinePolicy.normalizeMode("nonsense"));
        assertEquals(OutlinePolicy.MODE_SOLID, OutlinePolicy.normalizeMode(null));
        assertEquals(OutlinePolicy.MODE_CHASE, OutlinePolicy.normalizeMode(" chase"));
        assertEquals(OutlinePolicy.MODE_MINING, OutlinePolicy.normalizeMode("mining progress"));
        assertTrue(OutlinePolicy.isRainbow("rainbow"));
        assertFalse(OutlinePolicy.isRainbow(null));
        assertFalse(OutlinePolicy.isRainbow("Chase"));
    }

    @Test
    void modeButtonCyclesThroughEveryStyleAndBack() {
        String mode = OutlinePolicy.MODE_SOLID;
        mode = OutlinePolicy.nextMode(mode);
        assertEquals(OutlinePolicy.MODE_RAINBOW, mode);
        mode = OutlinePolicy.nextMode(mode);
        assertEquals(OutlinePolicy.MODE_CHASE, mode);
        mode = OutlinePolicy.nextMode(mode);
        assertEquals(OutlinePolicy.MODE_MINING, mode);
        mode = OutlinePolicy.nextMode(mode);
        assertEquals(OutlinePolicy.MODE_SOLID, mode);
        assertEquals(OutlinePolicy.MODE_RAINBOW, OutlinePolicy.nextMode("garbage"));
    }

    @Test
    void everyStyleHasItsOwnTranslationKey() {
        assertEquals("solid", OutlinePolicy.modeKey(OutlinePolicy.MODE_SOLID));
        assertEquals("rainbow", OutlinePolicy.modeKey(OutlinePolicy.MODE_RAINBOW));
        assertEquals("chase", OutlinePolicy.modeKey(OutlinePolicy.MODE_CHASE));
        assertEquals("mining", OutlinePolicy.modeKey(OutlinePolicy.MODE_MINING));
        assertEquals("solid", OutlinePolicy.modeKey(null));
    }

    @Test
    void onlyRainbowAndChaseWithSpreadVaryAlongAnEdge() {
        assertTrue(OutlinePolicy.variesAlongEdges(OutlinePolicy.MODE_RAINBOW, 1.0D));
        assertTrue(OutlinePolicy.variesAlongEdges(OutlinePolicy.MODE_CHASE, 1.0D));
        assertFalse(OutlinePolicy.variesAlongEdges(OutlinePolicy.MODE_RAINBOW, 0.0D));
        assertFalse(OutlinePolicy.variesAlongEdges(OutlinePolicy.MODE_CHASE, 0.0D));
        assertFalse(OutlinePolicy.variesAlongEdges(OutlinePolicy.MODE_SOLID, 1.0D));
        assertFalse(OutlinePolicy.variesAlongEdges(OutlinePolicy.MODE_MINING, 1.0D));
    }

    @Test
    void colorAtPicksTheStyle() {
        int solid = 0xC033E0D0;
        assertEquals(solid, OutlinePolicy.colorAt(
                OutlinePolicy.MODE_SOLID, solid, 3.0D, 1.0D, 1.0D, 0.7D, 0.2D, 0.4D, 0.6D));
        assertEquals(
                OutlinePolicy.rainbowArgb(OutlinePolicy.hue(3.0D, 1.0D, 1.0D, 0.2D, 0.4D, 0.6D), 0xC0),
                OutlinePolicy.colorAt(
                        OutlinePolicy.MODE_RAINBOW, solid, 3.0D, 1.0D, 1.0D, 0.7D, 0.2D, 0.4D, 0.6D));
        assertEquals(
                OutlinePolicy.chaseArgb(solid, OutlinePolicy.hue(3.0D, 1.0D, 1.0D, 0.2D, 0.4D, 0.6D)),
                OutlinePolicy.colorAt(
                        OutlinePolicy.MODE_CHASE, solid, 3.0D, 1.0D, 1.0D, 0.7D, 0.2D, 0.4D, 0.6D));
        assertEquals(
                OutlinePolicy.miningArgb(solid, 0.7D),
                OutlinePolicy.colorAt(
                        OutlinePolicy.MODE_MINING, solid, 3.0D, 1.0D, 1.0D, 0.7D, 0.2D, 0.4D, 0.6D));
    }

    @Test
    void chaseIsBrightInTheBandAndDimOutsideIt() {
        int solid = 0xFF204060;
        int peak = OutlinePolicy.chaseArgb(solid, 0.0D);
        assertEquals(0xFF, peak >>> 24);
        assertEquals(0x90, (peak >> 16) & 0xFF);
        assertEquals(0xA0, (peak >> 8) & 0xFF);
        assertEquals(0xB0, peak & 0xFF);

        int rest = OutlinePolicy.chaseArgb(solid, 0.5D);
        assertEquals(0x40, rest >>> 24);
        assertEquals(solid & 0xFFFFFF, rest & 0xFFFFFF);
        assertEquals(rest, OutlinePolicy.chaseArgb(solid, OutlinePolicy.CHASE_BAND));
        assertEquals(rest, OutlinePolicy.chaseArgb(solid, 1.0D - OutlinePolicy.CHASE_BAND));
    }

    @Test
    void chaseBandIsSymmetricWrapsAndFadesSmoothly() {
        int solid = 0xFF204060;
        assertEquals(OutlinePolicy.chaseArgb(solid, 0.05D), OutlinePolicy.chaseArgb(solid, 0.95D));
        assertEquals(OutlinePolicy.chaseArgb(solid, 0.05D), OutlinePolicy.chaseArgb(solid, 1.05D));
        assertEquals(OutlinePolicy.chaseArgb(solid, 0.95D), OutlinePolicy.chaseArgb(solid, -0.05D));
        int previous = 0x100;
        for (double phase = 0.0D; phase <= 0.5D; phase += 0.01D) {
            int alpha = OutlinePolicy.chaseArgb(solid, phase) >>> 24;
            assertTrue(alpha <= previous, "alpha rose at phase " + phase);
            assertTrue(alpha >= 0x40, "alpha " + alpha + " at phase " + phase);
            previous = alpha;
        }
    }

    @Test
    void chaseScalesTheConfiguredOpacity() {
        assertEquals(0x80, OutlinePolicy.chaseArgb(0x80204060, 0.0D) >>> 24);
        assertEquals(0x20, OutlinePolicy.chaseArgb(0x80204060, 0.5D) >>> 24);
    }

    @Test
    void miningFadesFromTheConfiguredColorToRed() {
        int solid = 0x8033E0D0;
        assertEquals(solid, OutlinePolicy.miningArgb(solid, 0.0D));
        assertEquals(0x80000000 | OutlinePolicy.MINING_COLOR, OutlinePolicy.miningArgb(solid, 1.0D));
        int nearlyDone = OutlinePolicy.miningArgb(solid, 0.999D);
        assertTrue(((nearlyDone >> 16) & 0xFF) >= 0xFD, Integer.toHexString(nearlyDone));
        assertTrue(Math.abs(((nearlyDone >> 8) & 0xFF) - 0x20) <= 2, Integer.toHexString(nearlyDone));
        assertTrue(Math.abs((nearlyDone & 0xFF) - 0x20) <= 2, Integer.toHexString(nearlyDone));
    }

    @Test
    void miningTravelsAroundTheColorWheelInsteadOfThroughGray() {
        int solid = 0xFF33E0D0;
        for (int i = 0; i <= 20; i++) {
            int argb = OutlinePolicy.miningArgb(solid, i / 20.0D);
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            assertEquals(0xFF, argb >>> 24);
            assertTrue(max >= 0xE0, "step " + i + " is dim: " + Integer.toHexString(argb));
            assertTrue(max - min >= 0xA0, "step " + i + " is washed out: " + Integer.toHexString(argb));
        }
        int half = OutlinePolicy.miningArgb(solid, 0.5D);
        assertTrue(((half >> 8) & 0xFF) > (half & 0xFF), "teal should pass through green and yellow");
    }

    @Test
    void miningTakesTheShortWayRoundAndHandlesGray() {
        int blueHalf = OutlinePolicy.miningArgb(0xFF2020FF, 0.5D);
        assertTrue((blueHalf & 0xFF) > ((blueHalf >> 8) & 0xFF), "blue should pass through magenta");
        assertTrue(((blueHalf >> 16) & 0xFF) > ((blueHalf >> 8) & 0xFF), "blue should pass through magenta");

        int grayHalf = OutlinePolicy.miningArgb(0xFF808080, 0.5D);
        assertTrue(((grayHalf >> 16) & 0xFF) > ((grayHalf >> 8) & 0xFF), "gray should redden");
        assertEquals((grayHalf >> 8) & 0xFF, grayHalf & 0xFF);

        int blackHalf = OutlinePolicy.miningArgb(0xFF000000, 0.5D);
        assertEquals(0x80, (blackHalf >> 16) & 0xFF);
        assertEquals((blackHalf >> 8) & 0xFF, blackHalf & 0xFF);
    }

    @Test
    void miningClampsProgressAndIgnoresNonFiniteValues() {
        int solid = 0xFF33E0D0;
        assertEquals(solid, OutlinePolicy.miningArgb(solid, -2.0D));
        assertEquals(solid, OutlinePolicy.miningArgb(solid, Double.NaN));
        assertEquals(OutlinePolicy.miningArgb(solid, 1.0D), OutlinePolicy.miningArgb(solid, 7.0D));
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
