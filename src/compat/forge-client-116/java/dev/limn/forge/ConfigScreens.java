package dev.limn.forge;

import dev.limn.client.gui.LimnSettingsScreen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

final class ConfigScreens {
    private ConfigScreens() {
    }

    static void register(ModLoadingContext context) {
        context.registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY,
                () -> (client, parent) -> new LimnSettingsScreen(parent, client.options));
    }
}
