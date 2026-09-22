package dev.limn.client;

import dev.limn.config.OutlineConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class LimnClient implements ClientModInitializer {
    public static final String MOD_ID = "limn";

    private static OutlineConfig config = new OutlineConfig();

    @Override
    public void onInitializeClient() {
        config = OutlineConfig.load(configPath());
    }

    public static OutlineConfig config() {
        return config;
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(OutlineConfig.FILE_NAME);
    }

    public static void saveConfig() {
        config.saveQuietly(configPath());
    }
}
