package dev.limn.client;

import dev.limn.config.OutlineConfig;

import java.nio.file.Path;

public final class LimnClient {
    public static final String MOD_ID = "limn";

    private static OutlineConfig config = new OutlineConfig();
    private static Path configPath;

    private LimnClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(OutlineConfig.FILE_NAME);
        config = OutlineConfig.load(configPath);
    }

    public static OutlineConfig config() {
        return config;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }
}
