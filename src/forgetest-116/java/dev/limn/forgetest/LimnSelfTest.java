package dev.limn.forgetest;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import dev.limn.client.gui.LimnSettingsScreen;
import dev.limn.outline.OutlinePolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Properties;

@Mod("limn_test")
public final class LimnSelfTest {
    private int ticks;
    private int inWorld;
    private boolean created;
    private boolean done;
    private int replaced;
    private int highlights;

    private boolean replacedWhenEnabled;
    private boolean keptVanillaWhenDisabled;
    private boolean screenOpened;
    private boolean toggled;

    public LimnSelfTest() {
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, DrawHighlightEvent.HighlightBlock.class, event -> {
            highlights++;
            if (event.isCanceled()) {
                replaced++;
            }
        });
    }

    private void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || done) {
            return;
        }
        ticks++;
        Minecraft client = Minecraft.getInstance();
        if (!created) {
            if (ticks > 40 && client.screen instanceof TitleScreen) {
                created = true;
                createFlatWorld(client);
            }
            return;
        }
        if (client.player == null || client.level == null) {
            return;
        }
        inWorld++;
        client.player.xRot = 90.0F;

        if (inWorld == 60) {
            LimnClient.config().setEnabled(true);
            LimnClient.config().setMode(OutlinePolicy.MODE_SOLID);
            LimnClient.config().setColor(0xFFFF2020);
            replaced = 0;
            highlights = 0;
        } else if (inWorld == 100) {
            replacedWhenEnabled = client.hitResult != null
                    && client.hitResult.getType() == HitResult.Type.BLOCK
                    && highlights > 0 && replaced == highlights;
            System.out.println("[Limn selftest] enabled: highlights=" + highlights + " replaced=" + replaced);
            screenshot(client, "limn-116-solid.png");
            LimnClient.config().setMode(OutlinePolicy.MODE_RAINBOW);
        } else if (inWorld == 140) {
            screenshot(client, "limn-116-rainbow.png");
            LimnClient.config().setEnabled(false);
            replaced = 0;
            highlights = 0;
        } else if (inWorld == 180) {
            keptVanillaWhenDisabled = highlights > 0 && replaced == 0;
            System.out.println("[Limn selftest] disabled: highlights=" + highlights + " replaced=" + replaced);
            screenshot(client, "limn-116-vanilla.png");
            LimnClient.config().setEnabled(true);
            LimnClient.config().setMode(OutlinePolicy.MODE_SOLID);
            client.setScreen(new LimnSettingsScreen(null, client.options));
        } else if (inWorld > 180 && inWorld < 220) {
            hover(client, toggleX(client), toggleY(client));
        } else if (inWorld == 220) {
            screenshot(client, "limn-116-settings.png");
            screenOpened = client.screen instanceof LimnSettingsScreen;
            boolean before = LimnClient.config().enabled();
            if (screenOpened) {
                client.screen.mouseClicked(toggleX(client), toggleY(client), 0);
            }
            toggled = screenOpened && LimnClient.config().enabled() != before;
            LimnClient.config().setEnabled(before);
            finish(client);
        }
    }

    private void finish(Minecraft client) {
        done = true;
        boolean config = LimnClient.configPath() != null && Files.exists(LimnClient.configPath());
        boolean modsMenu = ModList.get().getModContainerById("limn")
                .flatMap(mod -> mod.getCustomExtension(ExtensionPoint.CONFIGGUIFACTORY))
                .map(factory -> factory.apply(client, null) instanceof LimnSettingsScreen)
                .orElse(false);

        boolean drawn = true;
        for (String mode : new String[] {OutlinePolicy.MODE_SOLID, OutlinePolicy.MODE_RAINBOW}) {
            try {
                LimnClient.config().setMode(mode);
                BufferBuilder buffer = new BufferBuilder(4096);
                buffer.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
                LimnRuntime.draw(new PoseStack().last(), buffer, Shapes.block(), 1.0F);
                buffer.end();
            } catch (Throwable t) {
                t.printStackTrace();
                drawn = false;
            }
        }
        LimnClient.config().resetToDefaults();
        LimnClient.saveConfig();

        boolean pass = replacedWhenEnabled && keptVanillaWhenDisabled && config && modsMenu && screenOpened && toggled && drawn;
        System.out.println("[Limn selftest] replacedWhenEnabled=" + replacedWhenEnabled
                + " keptVanillaWhenDisabled=" + keptVanillaWhenDisabled + " config=" + config + " modsMenu=" + modsMenu
                + " screen=" + screenOpened + " toggled=" + toggled + " drawn=" + drawn
                + " RESULT=" + (pass ? "PASS" : "FAIL"));
        client.setScreen(null);
        client.stop();
    }

    private static void createFlatWorld(Minecraft client) {
        String name = "limn-selftest-" + System.currentTimeMillis();
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess.RegistryHolder registries = RegistryAccess.builtin();
        Properties properties = new Properties();
        properties.setProperty("level-type", "flat");
        client.createLevel(name, settings, registries, WorldGenSettings.create(registries, properties));
    }

    private static double toggleX(Minecraft client) {
        return client.getWindow().getGuiScaledWidth() / 2.0 - 80;
    }

    private static double toggleY(Minecraft client) {
        return client.getWindow().getGuiScaledHeight() / 6 - 2;
    }

    private static void screenshot(Minecraft client, String name) {
        Screenshot.grab(client.gameDirectory, name, client.getWindow().getWidth(), client.getWindow().getHeight(),
                client.getMainRenderTarget(), message -> { });
    }

    private static void hover(Minecraft client, double x, double y) {
        try {
            Method onMove = MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            onMove.setAccessible(true);
            double scale = client.getWindow().getGuiScale();
            onMove.invoke(client.mouseHandler, client.getWindow().getWindow(), x * scale, y * scale);
        } catch (ReflectiveOperationException e) {
        }
    }
}
