package dev.limn.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class GameScreens {
    private GameScreens() {
    }

    static Screen current(Minecraft client) {
        return client.gui == null ? null : client.gui.screen();
    }
}
