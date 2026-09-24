package dev.limn.forge;

import dev.limn.client.LimnClient;
import dev.limn.client.gui.LimnSettingsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkConstants;

@Mod(LimnClient.MOD_ID)
public final class LimnForge {
    public LimnForge() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        LimnClient.init(FMLPaths.CONFIGDIR.get());

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new LimnSettingsScreen(parent, client.options)));
    }
}
