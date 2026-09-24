package dev.limn.fabric;

import dev.limn.client.LimnClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class LimnFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LimnClient.init(FabricLoader.getInstance().getConfigDir());
    }
}
