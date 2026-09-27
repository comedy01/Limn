package dev.limn.fabrictest;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import dev.limn.client.gui.LimnSettingsScreen;
import dev.limn.outline.OutlinePolicy;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.shapes.Shapes;

import java.lang.reflect.Method;
import java.nio.file.Files;

public final class LimnSelfTest implements ClientModInitializer {
    private int ticks;
    private boolean done;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (done) {
            return;
        }
        ticks++;
        // The enabled toggle is the first widget: left column, first row.
        double buttonX = client.getWindow().getGuiScaledWidth() / 2.0 - 80;
        double buttonY = client.getWindow().getGuiScaledHeight() / 6 - 2;
        if (ticks == 120) {
            client.setScreen(new LimnSettingsScreen(client.screen, client.options));
            return;
        }
        if (ticks > 120 && ticks < 180) {
            hover(client, buttonX, buttonY);
        }
        if (ticks == 179) {
            Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(), message -> { });
        }
        if (ticks < 180) {
            return;
        }
        done = true;

        boolean mixinApplied = false;
        for (Method method : LevelRenderer.class.getDeclaredMethods()) {
            if (method.getName().contains("limn$")) {
                mixinApplied = true;
                break;
            }
        }

        boolean config = LimnClient.configPath() != null && Files.exists(LimnClient.configPath());

        boolean screen = client.screen instanceof LimnSettingsScreen;
        boolean before = LimnClient.config().enabled();
        if (screen) {
            client.screen.mouseClicked(buttonX, buttonY, 0);
        }
        boolean toggled = screen && LimnClient.config().enabled() != before;
        LimnClient.config().setEnabled(before);

        boolean drawn = true;
        for (String mode : new String[] {OutlinePolicy.MODE_SOLID, OutlinePolicy.MODE_RAINBOW}) {
            try {
                LimnClient.config().setMode(mode);
                BufferBuilder buffer = new BufferBuilder(4096);
                buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                LimnRuntime.draw(new PoseStack().last(), buffer, Shapes.block(), 1.0F);
                buffer.end();
            } catch (Throwable t) {
                t.printStackTrace();
                drawn = false;
            }
        }
        LimnClient.config().setMode(OutlinePolicy.MODE_SOLID);

        boolean pass = mixinApplied && config && screen && toggled && drawn;
        System.out.println("[Limn selftest] mixinApplied=" + mixinApplied
                + " config=" + config + " screen=" + screen + " toggled=" + toggled + " drawn=" + drawn
                + " RESULT=" + (pass ? "PASS" : "FAIL"));
        client.setScreen(null);
        client.stop();
    }

    // Real cursor moves don't reach an unfocused window, so feed the mouse handler directly.
    private static void hover(Minecraft client, double x, double y) {
        try {
            Method onMove = MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            onMove.setAccessible(true);
            double scale = client.getWindow().getGuiScale();
            onMove.invoke(client.mouseHandler, client.getWindow().getWindow(), x * scale, y * scale);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
