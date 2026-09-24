package dev.limn.neoforge;

import dev.limn.client.LimnClient;
import dev.limn.client.gui.LimnSettingsScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = LimnClient.MOD_ID, dist = Dist.CLIENT)
public final class LimnNeoForge {
    public LimnNeoForge(IEventBus modBus, ModContainer container) {
        LimnClient.init(FMLPaths.CONFIGDIR.get());

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new LimnSettingsScreen(parent, Minecraft.getInstance().options));
    }
}
