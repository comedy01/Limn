package dev.limn.forge;

import dev.limn.client.gui.LimnSettingsScreen;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;

final class ConfigScreens {
    private ConfigScreens() {
    }

    static void register(ModLoadingContext context) {
        context.registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory(
                        (client, parent) -> new LimnSettingsScreen(parent, client.options)));
    }
}
