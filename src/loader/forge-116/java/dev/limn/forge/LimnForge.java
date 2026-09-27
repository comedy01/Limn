package dev.limn.forge;

import dev.limn.client.LimnClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(LimnClient.MOD_ID)
public final class LimnForge {
    public LimnForge() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(
                ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        LimnClient.init(FMLPaths.CONFIGDIR.get());

        ConfigScreens.register(context);
        OutlineEvents.register();
    }
}
