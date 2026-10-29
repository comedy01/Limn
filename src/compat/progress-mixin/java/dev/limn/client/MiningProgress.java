package dev.limn.client;

import dev.limn.mixin.GameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

public final class MiningProgress {
    private MiningProgress() {
    }

    public static boolean available() {
        return GameModeAccessor.class.isAssignableFrom(MultiPlayerGameMode.class);
    }

    public static double current() {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null || !gameMode.isDestroying()) {
            return 0.0D;
        }
        return ((GameModeAccessor) gameMode).limn$destroyProgress();
    }
}
