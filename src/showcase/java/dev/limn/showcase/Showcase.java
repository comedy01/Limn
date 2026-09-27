package dev.limn.showcase;

import com.mojang.datafixers.util.Pair;
import dev.limn.client.LimnClient;
import dev.limn.client.gui.LimnSettingsScreen;
import dev.limn.config.OutlineConfig;
import dev.limn.outline.OutlinePolicy;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

public final class Showcase {
    private static final String WORLD = "limn-showcase";
    private static final long SETTLE_NANOS = 3_000_000_000L;
    private static final double SHOW_WIDTH = 3.0;
    private static final int[] PALETTE = {
            0xFF33E0D0, 0xFFFF3B3B, 0xFFFF9A1F, 0xFFFFE030, 0xFF7CFF3A, 0xFF3AA8FF, 0xFF9B5CFF, 0xFFFF5CC8,
            0xFFFFFFFF};
    private static Showcase instance;

    private final Path out;
    private final Recorder recorder;
    private final long seed;
    private final List<Action> actions = new ArrayList<>();
    private final AtomicReference<BlockPos> site = new AtomicReference<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int frame;
    private long actionStart;
    private Path pendingStill;
    private boolean showHand;

    private interface Action {
        boolean run(int frame);
    }

    private Showcase(Path out, String ffmpeg, long seed) {
        this.out = out;
        this.recorder = new Recorder(ffmpeg);
        this.seed = seed;
    }

    public static void start() {
        String ffmpeg = System.getProperty("limn.ffmpeg", "ffmpeg");
        long seed = Long.parseLong(System.getProperty("limn.seed", "20260925"));
        instance = new Showcase(Path.of(System.getProperty("limn.showcase")), ffmpeg, seed);
    }

    public static boolean hideHand() {
        return instance != null && instance.started && !instance.showHand;
    }

    public static long nanos() {
        return Clock.nanos();
    }

    public static void frame() {
        if (instance != null) {
            instance.onFrame();
        }
    }

    private void onFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 120) {
                started = true;
                setUpOptions(mc);
                plan(mc);
                log("creating world with seed " + seed);
                deleteWorld(mc);
                Worlds.create(mc, WORLD, false, seed);
            }
            return;
        }
        Clock.step();
        mc.gui.toastManager().clear();
        try {
            while (index < actions.size()) {
                if (frame == 0) {
                    actionStart = System.nanoTime();
                }
                if (!actions.get(index).run(frame++)) {
                    return;
                }
                index++;
                frame = 0;
            }
        } catch (Throwable e) {
            log("FAILED: " + e);
            e.printStackTrace();
        }
        finish(mc);
    }

    private void finish(Minecraft mc) {
        finished = true;
        Clock.release();
        release(mc);
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(90_000L);
            } catch (InterruptedException e) {
                return;
            }
            log("the game did not close, forcing it");
            Runtime.getRuntime().halt(1);
        }, "limn-showcase-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        LimnClient.config().resetToDefaults();
        LimnClient.saveConfig();
        log("showcase done");
        mc.stop();
    }

    private static void setUpOptions(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.renderDistance().set(12);
        mc.options.bobView().set(false);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private void once(Runnable action) {
        actions.add(f -> {
            action.run();
            return true;
        });
    }

    private void waitFrames(int frames) {
        actions.add(f -> f >= frames);
    }

    private void until(BooleanSupplier ready) {
        actions.add(f -> ready.getAsBoolean());
    }

    private void settle(Minecraft mc, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= 90 && System.nanoTime() - actionStart > SETTLE_NANOS
                    && (mc.levelRenderer.hasRenderedAllSections() || System.nanoTime() - actionStart > 20 * SETTLE_NANOS);
        });
    }

    private void record(String name, int frames, IntConsumer script) {
        actions.add(f -> {
            if (f == 0) {
                log("recording " + name);
                recorder.start(out.resolve("clips").resolve(name + ".mp4"));
            } else {
                Path still = pendingStill;
                pendingStill = null;
                recorder.capture(still);
            }
            if (f < frames) {
                script.accept(f);
                return false;
            }
            return true;
        });
        until(recorder::drained);
        once(recorder::stop);
    }

    private void shot(String name) {
        pendingStill = out.resolve("stills").resolve(name + ".png");
    }

    static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }

    private static void release(Minecraft mc) {
        mc.options.keyUp.setDown(false);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void cmd(Minecraft mc, String format, Object... args) {
        run(mc, String.format(Locale.ROOT, format, args));
    }

    private static void face(Minecraft mc, double yaw, double pitch) {
        LocalPlayer player = mc.player;
        player.setYRot((float) yaw);
        player.setXRot((float) pitch);
        player.yRotO = (float) yaw;
        player.xRotO = (float) pitch;
        player.setYHeadRot((float) yaw);
        player.yHeadRotO = (float) yaw;
    }

    private static void lookAt(Minecraft mc, Vec3 target) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        face(mc, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private static boolean wanted(String scene) {
        String only = System.getProperty("limn.scenes");
        return only == null || List.of(only.split(",")).contains(scene);
    }

    private Vec3 at(double dx, double dy, double dz) {
        BlockPos c = site.get();
        return new Vec3(c.getX() + dx + 0.5, c.getY() + dy, c.getZ() + dz + 0.5);
    }

    private void locate(Minecraft mc) {
        once(() -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerLevel level = server.overworld();
                Pair<BlockPos, Holder<Biome>> pair = level.findClosestBiome3d(h -> h.is(Biomes.CHERRY_GROVE),
                        BlockPos.ZERO, 6400, 32, 64);
                BlockPos found = pair == null ? BlockPos.ZERO : pair.getFirst();
                int y = level.getChunk(found.getX() >> 4, found.getZ() >> 4)
                        .getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getX() & 15, found.getZ() & 15);
                site.set(new BlockPos(found.getX(), y + 1, found.getZ()));
                log("cherry grove at " + site.get().toShortString());
            });
        });
        until(() -> site.get() != null);
    }

    private static void fill(Minecraft mc, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        int layers = Math.max(1, 32768 / ((Math.abs(x2 - x1) + 1) * (Math.abs(z2 - z1) + 1)));
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y += layers) {
            cmd(mc, "fill %d %d %d %d %d %d %s", x1, y, z1, x2, Math.min(y + layers - 1, Math.max(y1, y2)), z2, block);
        }
    }

    private void set(Minecraft mc, int dx, int dy, int dz, String block) {
        BlockPos c = site.get();
        cmd(mc, "setblock %d %d %d minecraft:%s", c.getX() + dx, c.getY() + dy, c.getZ() + dz, block);
    }

    private static final String[] ROW = {
            "grindstone[face=floor,facing=north]", "decorated_pot[facing=north]", "brewing_stand",
            "chest[facing=north]", "enchanting_table", "bell[attachment=floor,facing=north]", "barrel[facing=up]",
            "lantern[hanging=false]", "anvil[facing=east]"};
    private static final String[][] WALL = {
            {"hay_block", "pumpkin", "melon", "cherry_planks", "oak_log"},
            {"bookshelf", "cherry_log", "hay_block", "melon", "pumpkin"}};

    private void buildSet(Minecraft mc) {
        BlockPos c = site.get();
        int x = c.getX();
        int z = c.getZ();
        int ground = c.getY() - 1;
        fill(mc, x - 22, ground + 1, z - 22, x + 22, ground + 40, z + 22, "minecraft:air");
        fill(mc, x - 22, ground - 6, z - 22, x + 22, ground - 1, z + 22, "minecraft:dirt");
        fill(mc, x - 22, ground, z - 22, x + 22, ground, z + 22, "minecraft:grass_block");
        int[][] trees = {{-2, 14}, {9, 12}, {-12, 13}, {18, 4}, {-18, -2}, {16, -16}, {-15, -17}, {2, -18}, {20, 18}};
        for (int[] tree : trees) {
            cmd(mc, "place feature minecraft:cherry %d %d %d", x + tree[0], ground + 1, z + tree[1]);
        }
        Random random = new Random(seed);
        String[] flowers = {"pink_tulip", "allium", "oxeye_daisy", "lily_of_the_valley", "cornflower"};
        String[] facings = {"north", "south", "east", "west"};
        for (int dx = -21; dx <= 21; dx++) {
            for (int dz = -21; dz <= 21; dz++) {
                double roll = random.nextDouble();
                String block;
                if (roll < 0.14) {
                    block = "pink_petals[flower_amount=" + (1 + random.nextInt(4)) + ",facing="
                            + facings[random.nextInt(4)] + "]";
                } else if (roll < 0.20) {
                    block = "short_grass";
                } else if (roll < 0.215) {
                    block = flowers[random.nextInt(flowers.length)];
                } else {
                    continue;
                }
                cmd(mc, "setblock %d %d %d minecraft:%s", x + dx, ground + 1, z + dz, block);
            }
        }
        fill(mc, x - 5, ground + 1, z - 1, x + 5, ground + 1, z + 4, "minecraft:air");
        fill(mc, x - 11, ground + 1, z - 7, x - 5, ground + 1, z + 7, "minecraft:air");
        fill(mc, x + 2, ground + 1, z - 7, x + 10, ground + 1, z - 1, "minecraft:air");
        for (int i = 0; i < ROW.length; i++) {
            set(mc, i - 4, 0, 3, ROW[i]);
        }
        set(mc, -9, 0, 5, "crafting_table");
        set(mc, -9, 0, -5, "bookshelf");
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < 5; i++) {
                set(mc, 4 + i, row, -6, WALL[row][i]);
            }
        }
        run(mc, "kill @e[type=minecraft:item]");
        run(mc, "clear @p");
        run(mc, "item replace entity @p hotbar.0 with minecraft:diamond_pickaxe");
        run(mc, "item replace entity @p hotbar.1 with minecraft:cherry_planks 64");
    }

    private void scene(Minecraft mc, OutlineConfig config, double dx, double dz, Runnable setup, Runnable hold) {
        once(() -> {
            release(mc);
            Screens.open(mc, null);
            config.resetToDefaults();
            config.setWidth(SHOW_WIDTH);
            Vec3 p = at(dx, 0, dz);
            cmd(mc, "tp @p %.2f %.2f %.2f", p.x, p.y, p.z);
            run(mc, "kill @e[type=minecraft:item]");
            select(mc, 8);
            showHand = false;
        });
        waitFrames(15);
        once(setup);
        settle(mc, hold);
    }

    private void plan(Minecraft mc) {
        OutlineConfig config = LimnClient.config();

        until(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null);
        once(() -> {
            log("world loaded");
            Clock.fix();
            config.resetToDefaults();
            run(mc, "time set 2500");
            run(mc, "weather clear 1000000");
            run(mc, "gamerule advance_time false");
            run(mc, "gamerule spawn_mobs false");
        });
        locate(mc);
        once(() -> {
            BlockPos c = site.get();
            cmd(mc, "forceload add %d %d %d %d", c.getX() - 30, c.getZ() - 30, c.getX() + 30, c.getZ() + 30);
            cmd(mc, "tp @p %d %d %d", c.getX(), c.getY() + 45, c.getZ());
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        });
        settle(mc, () -> {
        });
        once(() -> buildSet(mc));
        waitFrames(60);
        once(() -> {
            mc.player.getAbilities().flying = false;
            mc.player.onUpdateAbilities();
        });
        film(mc, config);
    }

    private void film(Minecraft mc, OutlineConfig config) {
        if (wanted("intro")) {
            scene(mc, config, 0, -0.5, () -> {
                config.setMode(OutlinePolicy.MODE_RAINBOW);
                config.setRainbowSpeed(1.5);
            }, () -> lookAt(mc, at(2.8, 0.45, 3)));
            record("01-intro", 330, f -> {
                lookAt(mc, lerp(at(2.8, 0.45, 3), at(-2.8, 0.45, 3), smooth(f / 330.0)));
                if (f == 95) {
                    shot("rainbow-row-1");
                } else if (f == 205) {
                    shot("rainbow-row-2");
                }
            });
        }

        if (wanted("colors")) {
            scene(mc, config, -7.4, 3.4, () -> {
            }, () -> lookAt(mc, at(-9, 0.5, 5)));
            record("02-colors", 270, f -> {
                config.setColor(PALETTE[Math.min(PALETTE.length - 1, f / 30)]);
                lookAt(mc, at(-9, 0.5, 5).add(lerp(-0.12, 0.12, smooth(f / 270.0)), 0, 0));
                if (f == 45) {
                    shot("color-red");
                } else if (f == 105) {
                    shot("color-yellow");
                } else if (f == 165) {
                    shot("color-blue");
                } else if (f == 225) {
                    shot("color-pink");
                }
            });
        }

        if (wanted("rainbow")) {
            scene(mc, config, -7.4, -3.4, () -> {
                config.setMode(OutlinePolicy.MODE_RAINBOW);
                config.setWidth(3.5);
            }, () -> lookAt(mc, at(-9, 0.5, -5)));
            record("03-rainbow", 240, f -> {
                lookAt(mc, at(-9, 0.5, -5).add(0, 0, lerp(0.1, -0.1, smooth(f / 240.0))));
                if (f == 120) {
                    shot("rainbow");
                }
            });
        }

        if (wanted("width")) {
            scene(mc, config, -7.4, 3.4, () -> {
                config.setColor(0xFFFFFFFF);
                config.setWidth(OutlinePolicy.MIN_WIDTH);
            }, () -> lookAt(mc, at(-9, 0.5, 5)));
            record("04-width", 240, f -> {
                double t = smooth((f - 20) / 190.0);
                config.setWidth(lerp(OutlinePolicy.MIN_WIDTH, OutlinePolicy.MAX_WIDTH, t));
                lookAt(mc, at(-9, 0.5, 5));
                if (f == 20) {
                    shot("width-thin");
                } else if (f == 225) {
                    shot("width-thick");
                }
            });
        }

        if (wanted("spread")) {
            scene(mc, config, -7.4, -3.4, () -> {
                config.setMode(OutlinePolicy.MODE_RAINBOW);
                config.setWidth(3.5);
                config.setRainbowSpread(OutlinePolicy.MIN_SPREAD);
                config.setRainbowSpeed(2.0);
            }, () -> lookAt(mc, at(-9, 0.5, -5)));
            record("05-spread", 270, f -> {
                if (f == 95) {
                    config.setRainbowSpread(1.0);
                } else if (f == 180) {
                    config.setRainbowSpread(OutlinePolicy.MAX_SPREAD);
                    config.setRainbowSpeed(1.0);
                }
                lookAt(mc, at(-9, 0.5, -5));
                if (f == 60) {
                    shot("spread-zero");
                } else if (f == 250) {
                    shot("spread-high");
                }
            });
        }

        if (wanted("mine")) {
            scene(mc, config, 6, -3, () -> {
                config.setMode(OutlinePolicy.MODE_RAINBOW);
                config.setRainbowSpeed(1.5);
                select(mc, 0);
                showHand = true;
            }, () -> lookAt(mc, at(4, 1.5, -6)));
            record("06-mine-build", 300, f -> {
                if (f < 150) {
                    int i = Math.min(4, f / 30);
                    int local = f - i * 30;
                    lookAt(mc, lerp(at(4 + Math.max(0, i - 1), 1.5, -6), at(4 + i, 1.5, -6),
                            i == 0 ? 1.0 : smooth(local / 12.0)));
                    if (local == 16) {
                        KeyMapping.click(mc.options.keyAttack.getDefaultKey());
                    }
                    if (f == 80) {
                        shot("mine");
                    }
                } else {
                    if (f == 150) {
                        select(mc, 1);
                    }
                    int i = Math.min(4, (f - 150) / 30);
                    int local = (f - 150) - i * 30;
                    Vec3 from = i == 0 ? at(8, 1.5, -6) : at(9 - i, 1.02, -6);
                    lookAt(mc, lerp(from, at(8 - i, 1.02, -6), smooth(local / 12.0)));
                    if (local == 16) {
                        KeyMapping.click(mc.options.keyUse.getDefaultKey());
                    }
                    if (f == 262) {
                        shot("build");
                    }
                }
            });
            once(() -> release(mc));
        }

        if (wanted("settings")) {
            scene(mc, config, 0, -0.5, () -> {
            }, () -> lookAt(mc, at(0, 0.45, 3)));
            once(() -> Screens.open(mc, new LimnSettingsScreen(null, mc.options)));
            waitFrames(20);
            record("07-settings", 240, f -> {
                if (f == 30) {
                    click(mc, "Outline Style", 0.5);
                } else if (f == 75) {
                    click(mc, "Red", 0.9);
                } else if (f == 105) {
                    click(mc, "Green", 0.25);
                } else if (f == 135) {
                    click(mc, "Line Width", 0.6);
                } else if (f == 165) {
                    click(mc, "Rainbow Speed", 0.45);
                } else if (f == 195) {
                    click(mc, "Rainbow Spread", 0.7);
                } else if (f == 215) {
                    shot("settings");
                }
            });
            once(() -> Screens.open(mc, null));
        }

        if (wanted("outro")) {
            once(() -> run(mc, "time set 12300"));
            scene(mc, config, 0, -0.5, () -> {
                config.setMode(OutlinePolicy.MODE_RAINBOW);
                config.setWidth(3.5);
            }, () -> lookAt(mc, at(0, 0.45, 3)));
            record("08-outro", 300, f -> {
                lookAt(mc, lerp(at(0, 0.45, 3), at(-2, 0.45, 3), smooth(f / 300.0)));
                if (f == 120) {
                    shot("sunset");
                }
            });
            once(() -> run(mc, "time set 2500"));
        }
    }

    private static void click(Minecraft mc, String label, double along) {
        AbstractWidget widget = find(Screens.current(mc), label);
        if (widget == null) {
            throw new IllegalStateException("no widget " + label);
        }
        Ui.click(Screens.current(mc), widget.getX() + widget.getWidth() * along,
                widget.getY() + widget.getHeight() / 2.0);
    }

    private static AbstractWidget find(GuiEventListener node, String label) {
        if (node instanceof AbstractWidget widget && widget.getMessage().getString().startsWith(label)) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = find(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void deleteWorld(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static void log(String message) {
        System.out.println("[limn-showcase] " + message);
    }
}
