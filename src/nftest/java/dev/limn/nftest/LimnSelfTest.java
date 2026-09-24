package dev.limn.nftest;

import dev.limn.client.LimnClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Method;
import java.nio.file.Files;

@Mod(value = "limn_test", dist = Dist.CLIENT)
public final class LimnSelfTest {
    private int ticks;
    private boolean done;

    public LimnSelfTest() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onTick);
    }

    private void onTick(ClientTickEvent.Post event) {
        if (done || ++ticks < 120) {
            return;
        }
        done = true;
        Minecraft client = Minecraft.getInstance();

        boolean mixinApplied = false;
        for (Method method : LevelRenderer.class.getDeclaredMethods()) {
            if (method.getName().contains("limn$")) {
                mixinApplied = true;
                break;
            }
        }

        boolean config = LimnClient.configPath() != null && Files.exists(LimnClient.configPath());
        boolean pass = mixinApplied && config;
        System.out.println("[Limn selftest] mixinApplied=" + mixinApplied
                + " config=" + config + " RESULT=" + (pass ? "PASS" : "FAIL"));
        client.stop();
    }
}
