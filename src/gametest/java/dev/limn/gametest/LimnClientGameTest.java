package dev.limn.gametest;

import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import dev.limn.client.gui.LimnSettingsScreen;
import dev.limn.config.OutlineConfig;
import dev.limn.outline.OutlinePolicy;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.IOException;
import java.nio.file.Files;

public class LimnClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitTicks(20);
            runInWorld(context);
        }
        log("ALL CHECKS PASSED");
    }

    private void runInWorld(ClientGameTestContext context) {
        OutlineConfig config = LimnClient.config();
        config.resetToDefaults();

        context.runOnClient(client -> {
            client.player.setYRot(0.0F);
            client.player.setXRot(90.0F);
        });
        context.waitTicks(5);

        check(config.enabled(), "the outline is not enabled by default");
        check(OutlinePolicy.MODE_SOLID.equals(config.mode()), "the outline mode is not Solid Color by default");
        context.waitTicks(5);
        log("screenshot: " + context.takeScreenshot("limn-solid"));

        config.setWidth(OutlinePolicy.MAX_WIDTH);
        context.waitTicks(5);
        log("screenshot: " + context.takeScreenshot("limn-thick"));
        config.setWidth(OutlinePolicy.DEFAULT_WIDTH);
        context.waitTicks(5);

        config.setMode(OutlinePolicy.MODE_RAINBOW);
        context.waitTicks(10);
        log("screenshot: " + context.takeScreenshot("limn-rainbow"));

        config.setEnabled(false);
        context.waitTicks(5);
        log("screenshot: " + context.takeScreenshot("limn-disabled"));

        config.setEnabled(true);
        config.setMode(OutlinePolicy.MODE_SOLID);
        context.waitTicks(5);

        config.setWidth(3.0);
        config.setRainbowSpeed(2.0);
        config.setEnabled(false);
        context.setScreen(() -> new LimnSettingsScreen(null, Minecraft.getInstance().options));
        context.waitForScreen(LimnSettingsScreen.class);
        context.waitTicks(5);
        log("screenshot: " + context.takeScreenshot("limn-settings"));

        clickButton(context, "limn.options.mode");
        context.waitTicks(5);
        check(OutlinePolicy.isRainbow(config.mode()), "clicking the mode button did not switch to Rainbow");
        log("screenshot: " + context.takeScreenshot("limn-settings-mode-toggled"));
        clickButton(context, "limn.options.mode");
        context.waitTicks(5);
        check(!OutlinePolicy.isRainbow(config.mode()), "clicking the mode button again did not switch back");

        double speedBefore = config.rainbowSpeed();
        dragSliderToMax(context, "Rainbow Speed");
        context.waitTicks(5);
        check(config.rainbowSpeed() != speedBefore, "dragging the rainbow speed slider did not change the config");
        log("screenshot: " + context.takeScreenshot("limn-settings-slider-dragged"));

        double widthBefore = config.width();
        dragSliderToMax(context, "Line Width");
        context.waitTicks(5);
        if (LimnRuntime.SUPPORTS_LINE_WIDTH) {
            check(config.width() != widthBefore, "dragging the width slider did not change the config");
        } else {
            check(config.width() == widthBefore, "the disabled width slider still responded to a drag");
        }

        clickButton(context, "limn.options.reset");
        context.waitTicks(5);
        check(config.enabled(), "reset did not restore enabled");
        check(config.width() == OutlinePolicy.DEFAULT_WIDTH, "reset did not restore the width");
        check(config.rainbowSpeed() == OutlinePolicy.DEFAULT_SPEED, "reset did not restore rainbow speed");
        log("screenshot: " + context.takeScreenshot("limn-settings-after-reset"));

        int speedSliders = context.computeOnClient(client -> countSliders(GameScreens.current(client), "Rainbow Speed"));
        check(speedSliders == 1, "reset left " + speedSliders + " rainbow speed sliders on the screen");
        dragSliderToMax(context, "Rainbow Speed");
        context.waitTicks(5);
        check(config.rainbowSpeed() == OutlinePolicy.MAX_SPEED, "dragging the rainbow speed slider after reset did not change the config");
        String speedShown = context.computeOnClient(client ->
                findSlider(GameScreens.current(client), "Rainbow Speed").getMessage().getString());
        check(speedShown.contains("5.0"), "the rainbow speed slider did not move after reset: " + speedShown);

        config.setWidth(2.5);
        context.setScreen(() -> null);
        context.waitTicks(5);
        String saved = readConfigFile();
        check(saved.contains("\"width\": 2.5"), "settings were not saved on close: " + saved);
        log("saved on close: " + saved.replace('\n', ' '));

        config.resetToDefaults();
        LimnClient.saveConfig();
    }

    private static void clickButton(ClientGameTestContext context, String translationKey) {
        context.getInput().setCursorPos(200.0, 200.0);
        context.getInput().scroll(-20.0);
        context.waitTicks(2);
        double[] center = context.computeOnClient(client -> {
            Button button = findButton(GameScreens.current(client), translationKey);
            if (button == null) {
                throw new AssertionError("no button '" + translationKey + "' on the current screen");
            }
            double scale = client.getWindow().getGuiScale();
            return new double[] {
                    (button.getX() + button.getWidth() / 2.0) * scale,
                    (button.getY() + button.getHeight() / 2.0) * scale};
        });
        context.getInput().setCursorPos(center[0], center[1]);
        context.waitTick();
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static void dragSliderToMax(ClientGameTestContext context, String captionText) {
        context.getInput().setCursorPos(200.0, 200.0);
        context.getInput().scroll(-20.0);
        context.waitTicks(2);
        double[] bounds = context.computeOnClient(client -> {
            AbstractSliderButton slider = findSlider(GameScreens.current(client), captionText);
            if (slider == null) {
                throw new AssertionError("no slider '" + captionText + "' on the current screen");
            }
            double scale = client.getWindow().getGuiScale();
            return new double[] {
                    (slider.getX() + slider.getWidth() - 2.0) * scale,
                    (slider.getY() + slider.getHeight() / 2.0) * scale};
        });
        context.getInput().setCursorPos(bounds[0], bounds[1]);
        context.waitTick();
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static AbstractSliderButton findSlider(GuiEventListener node, String captionText) {
        if (node instanceof AbstractSliderButton slider && slider.getMessage().getString().contains(captionText)) {
            return slider;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractSliderButton found = findSlider(child, captionText);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int countSliders(GuiEventListener node, String captionText) {
        int count = node instanceof AbstractSliderButton slider && slider.getMessage().getString().contains(captionText) ? 1 : 0;
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                count += countSliders(child, captionText);
            }
        }
        return count;
    }

    private static Button findButton(GuiEventListener node, String translationKey) {
        if (node instanceof Button button
                && button.getMessage().getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(translationKey)) {
            return button;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                Button found = findButton(child, translationKey);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String readConfigFile() {
        try {
            return Files.readString(LimnClient.configPath());
        } catch (IOException e) {
            throw new AssertionError("could not read " + LimnClient.configPath(), e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void log(String message) {
        System.out.println("[Limn gametest] " + message);
    }
}
