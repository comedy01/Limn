package dev.limn.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.limn.client.gui.LimnSettingsScreen;
import net.minecraft.client.Minecraft;

public final class LimnModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new LimnSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
