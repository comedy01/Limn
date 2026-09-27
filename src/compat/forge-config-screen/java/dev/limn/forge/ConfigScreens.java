package dev.limn.forge;

import dev.limn.client.gui.LimnSettingsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

final class ConfigScreens {
    private ConfigScreens() {
    }

    static void register(ModLoadingContext context) {
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new LimnSettingsScreen(parent, client.options)));
    }
}
