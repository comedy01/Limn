package dev.limn.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.limn.outline.OutlinePolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutlineConfigTest {
    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve("config").resolve(OutlineConfig.FILE_NAME);
    }

    private void write(String json) throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), json, StandardCharsets.UTF_8);
    }

    @Test
    void defaults() {
        OutlineConfig config = new OutlineConfig();
        assertTrue(config.enabled());
        assertEquals(OutlinePolicy.MODE_SOLID, config.mode());
        assertEquals(OutlinePolicy.DEFAULT_COLOR, config.color());
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, config.width());
        assertEquals(OutlinePolicy.DEFAULT_SPEED, config.rainbowSpeed());
        assertEquals(OutlinePolicy.DEFAULT_SPREAD, config.rainbowSpread());
    }

    @Test
    void settersClamp() {
        OutlineConfig config = new OutlineConfig();

        config.setEnabled(false);
        assertFalse(config.enabled());

        config.setMode("garbage");
        assertEquals(OutlinePolicy.MODE_SOLID, config.mode());
        config.setMode("rainbow");
        assertEquals(OutlinePolicy.MODE_RAINBOW, config.mode());

        config.setColor(0x80FF00AA);
        assertEquals(0x80FF00AA, config.color());

        config.setWidth(100.0);
        assertEquals(OutlinePolicy.MAX_WIDTH, config.width());
        config.setWidth(-3.0);
        assertEquals(OutlinePolicy.MIN_WIDTH, config.width());
        config.setWidth(Double.NaN);
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, config.width());

        config.setRainbowSpeed(-100.0);
        assertEquals(OutlinePolicy.MIN_SPEED, config.rainbowSpeed());
        config.setRainbowSpeed(500.0);
        assertEquals(OutlinePolicy.MAX_SPEED, config.rainbowSpeed());

        config.setRainbowSpread(10.0);
        assertEquals(OutlinePolicy.MAX_SPREAD, config.rainbowSpread());
        config.setRainbowSpread(-1.0);
        assertEquals(OutlinePolicy.MIN_SPREAD, config.rainbowSpread());
    }

    @Test
    void resetToDefaults() {
        OutlineConfig config = new OutlineConfig();
        config.setEnabled(false);
        config.setMode("Rainbow");
        config.setColor(0x11223344);
        config.setWidth(3.0);
        config.setRainbowSpeed(4.0);
        config.setRainbowSpread(2.0);
        config.resetToDefaults();
        assertTrue(config.enabled());
        assertEquals(OutlinePolicy.MODE_SOLID, config.mode());
        assertEquals(OutlinePolicy.DEFAULT_COLOR, config.color());
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, config.width());
        assertEquals(OutlinePolicy.DEFAULT_SPEED, config.rainbowSpeed());
        assertEquals(OutlinePolicy.DEFAULT_SPREAD, config.rainbowSpread());
    }

    @Test
    void saveAndLoad() throws IOException {
        OutlineConfig config = new OutlineConfig();
        config.setEnabled(false);
        config.setMode("Rainbow");
        config.setColor(0xCC123456);
        config.setWidth(2.5);
        config.setRainbowSpeed(3.0);
        config.setRainbowSpread(0.5);
        config.save(file());

        OutlineConfig reloaded = OutlineConfig.load(file());
        assertFalse(reloaded.enabled());
        assertEquals(OutlinePolicy.MODE_RAINBOW, reloaded.mode());
        assertEquals(0xCC123456, reloaded.color());
        assertEquals(2.5, reloaded.width());
        assertEquals(3.0, reloaded.rainbowSpeed());
        assertEquals(0.5, reloaded.rainbowSpread());
    }

    @Test
    void saveCreatesDirectories() throws IOException {
        new OutlineConfig().save(file());
        assertTrue(Files.isRegularFile(file()));
        assertFalse(Files.exists(file().resolveSibling(OutlineConfig.FILE_NAME + ".tmp")));
    }

    @Test
    void savedKeys() throws IOException {
        new OutlineConfig().save(file());
        String json = Files.readString(file(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"enabled\": true"), json);
        assertTrue(json.contains("\"mode\": \"Solid Color\""), json);
        assertTrue(json.contains("\"width\": 1.0"), json);
    }

    @Test
    void missingFile() {
        OutlineConfig config = OutlineConfig.load(file());
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, config.width());
        assertTrue(Files.isRegularFile(file()));
    }

    @Test
    void outOfRangeValues() throws IOException {
        write("{\"width\": 500, \"rainbowSpeed\": -4, \"mode\": \"garbage\"}");
        OutlineConfig config = OutlineConfig.load(file());
        assertEquals(OutlinePolicy.MAX_WIDTH, config.width());
        assertEquals(OutlinePolicy.MIN_SPEED, config.rainbowSpeed());
        assertEquals(OutlinePolicy.MODE_SOLID, config.mode());
    }

    @Test
    void missingAndUnknownKeys() throws IOException {
        write("{\"width\": 2.5, \"somethingFromAFutureVersion\": [1, 2, 3]}");
        OutlineConfig config = OutlineConfig.load(file());
        assertEquals(2.5, config.width());
        assertEquals(OutlinePolicy.DEFAULT_SPEED, config.rainbowSpeed());
    }

    @Test
    void nonFiniteNumbers() throws IOException {
        write("{\"width\": NaN, \"rainbowSpeed\": Infinity}");
        OutlineConfig config = OutlineConfig.load(file());
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, config.width());
        assertEquals(OutlinePolicy.DEFAULT_SPEED, config.rainbowSpeed());
    }

    @Test
    void brokenFileIsMovedAside() throws IOException {
        write("{ this is not json");
        OutlineConfig config = OutlineConfig.load(file());
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, config.width());

        Path backup = file().resolveSibling(OutlineConfig.FILE_NAME + ".broken");
        assertEquals("{ this is not json", Files.readString(backup, StandardCharsets.UTF_8));
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, OutlineConfig.load(file()).width());
    }

    @Test
    void wrongTypedValue() throws IOException {
        write("{\"width\": \"lots\"}");
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, OutlineConfig.load(file()).width());
        assertTrue(Files.exists(file().resolveSibling(OutlineConfig.FILE_NAME + ".broken")));
    }

    @Test
    void emptyFile() throws IOException {
        write("");
        OutlineConfig config = OutlineConfig.load(file());
        assertEquals(OutlinePolicy.DEFAULT_WIDTH, config.width());
        assertEquals(OutlinePolicy.DEFAULT_SPEED, config.rainbowSpeed());
    }
}
