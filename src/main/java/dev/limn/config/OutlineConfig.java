package dev.limn.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import dev.limn.outline.OutlinePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class OutlineConfig {
    public static final String FILE_NAME = "limn.json";
    public static final boolean DEFAULT_ENABLED = true;

    private static final Logger LOGGER = LoggerFactory.getLogger("limn");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("enabled")
    private boolean enabled = DEFAULT_ENABLED;

    @SerializedName("mode")
    private String mode = OutlinePolicy.MODE_SOLID;

    @SerializedName("color")
    private int color = OutlinePolicy.DEFAULT_COLOR;

    @SerializedName("width")
    private double width = OutlinePolicy.DEFAULT_WIDTH;

    @SerializedName("rainbowSpeed")
    private double rainbowSpeed = OutlinePolicy.DEFAULT_SPEED;

    @SerializedName("rainbowSpread")
    private double rainbowSpread = OutlinePolicy.DEFAULT_SPREAD;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public String mode() {
        return mode;
    }

    public void setMode(String value) {
        mode = OutlinePolicy.normalizeMode(value);
    }

    public int color() {
        return color;
    }

    public void setColor(int value) {
        color = value;
    }

    public double width() {
        return width;
    }

    public void setWidth(double value) {
        width = OutlinePolicy.clampWidth(value);
    }

    public double rainbowSpeed() {
        return rainbowSpeed;
    }

    public void setRainbowSpeed(double value) {
        rainbowSpeed = OutlinePolicy.clampSpeed(value);
    }

    public double rainbowSpread() {
        return rainbowSpread;
    }

    public void setRainbowSpread(double value) {
        rainbowSpread = OutlinePolicy.clampSpread(value);
    }

    public void resetToDefaults() {
        enabled = DEFAULT_ENABLED;
        mode = OutlinePolicy.MODE_SOLID;
        color = OutlinePolicy.DEFAULT_COLOR;
        width = OutlinePolicy.DEFAULT_WIDTH;
        rainbowSpeed = OutlinePolicy.DEFAULT_SPEED;
        rainbowSpread = OutlinePolicy.DEFAULT_SPREAD;
    }

    private void sanitize() {
        setMode(mode);
        setWidth(width);
        setRainbowSpeed(rainbowSpeed);
        setRainbowSpread(rainbowSpread);
    }

    public static OutlineConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            OutlineConfig fresh = new OutlineConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            OutlineConfig loaded = GSON.fromJson(reader, OutlineConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            loaded.sanitize();
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            OutlineConfig fresh = new OutlineConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(
                    file,
                    file.resolveSibling(file.getFileName() + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
