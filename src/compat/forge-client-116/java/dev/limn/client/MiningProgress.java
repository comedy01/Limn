package dev.limn.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

public final class MiningProgress {
    private static final Field DESTROY_PROGRESS = find();

    private MiningProgress() {
    }

    public static boolean available() {
        return DESTROY_PROGRESS != null;
    }

    public static double current() {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (DESTROY_PROGRESS == null || gameMode == null || !gameMode.isDestroying()) {
            return 0.0D;
        }
        try {
            return DESTROY_PROGRESS.getFloat(gameMode);
        } catch (IllegalAccessException e) {
            return 0.0D;
        }
    }

    private static Field find() {
        try {
            return ObfuscationReflectionHelper.findField(MultiPlayerGameMode.class, "field_78770_f");
        } catch (RuntimeException e) {
            return null;
        }
    }
}
