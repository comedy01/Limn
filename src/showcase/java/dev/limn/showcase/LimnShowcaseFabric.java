package dev.limn.showcase;

import net.fabricmc.api.ClientModInitializer;

public final class LimnShowcaseFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Showcase.start();
    }
}
