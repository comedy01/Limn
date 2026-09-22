package dev.limn.client.gui;

import dev.limn.client.LimnClient;
import dev.limn.config.OutlineConfig;
import dev.limn.outline.OutlinePolicy;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;

public final class LimnSettingsScreen extends OptionsSubScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public LimnSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("limn.options.title"));
    }

    @Override
    protected void addOptions() {
        OutlineConfig config = LimnClient.config();

        list.addSmall(List.of(enabledButton(config), modeButton(config)));

        list.addSmall(List.of(
                intSlider("limn.options.alpha", "limn.options.color.tooltip",
                        (config.color() >>> 24) & 0xFF, value -> setChannel(config, 24, value)),
                intSlider("limn.options.red", "limn.options.color.tooltip",
                        (config.color() >>> 16) & 0xFF, value -> setChannel(config, 16, value))));

        list.addSmall(List.of(
                intSlider("limn.options.green", "limn.options.color.tooltip",
                        (config.color() >>> 8) & 0xFF, value -> setChannel(config, 8, value)),
                intSlider("limn.options.blue", "limn.options.color.tooltip",
                        config.color() & 0xFF, value -> setChannel(config, 0, value))));

        AbstractWidget widthSlider = new StepSlider(
                "limn.options.width",
                "limn.options.width.tooltip",
                OutlinePolicy.MIN_WIDTH,
                OutlinePolicy.MAX_WIDTH,
                0.1,
                config.width(),
                value -> String.format(Locale.ROOT, "%.2fx", value),
                config::setWidth);
        AbstractWidget speedSlider = new StepSlider(
                "limn.options.rainbow_speed",
                "limn.options.rainbow_speed.tooltip",
                OutlinePolicy.MIN_SPEED,
                OutlinePolicy.MAX_SPEED,
                0.1,
                config.rainbowSpeed(),
                value -> String.format(Locale.ROOT, "%.1f", value),
                config::setRainbowSpeed);
        list.addSmall(List.of(widthSlider, speedSlider));

        AbstractWidget spreadSlider = new StepSlider(
                "limn.options.rainbow_spread",
                "limn.options.rainbow_spread.tooltip",
                OutlinePolicy.MIN_SPREAD,
                OutlinePolicy.MAX_SPREAD,
                0.1,
                config.rainbowSpread(),
                value -> String.format(Locale.ROOT, "%.2f", value),
                config::setRainbowSpread);
        list.addSmall(List.of(spreadSlider, resetButton(config)));
    }

    @Override
    public void removed() {
        super.removed();
        LimnClient.saveConfig();
    }

    private AbstractWidget enabledButton(OutlineConfig config) {
        return Button.builder(enabledLabel(config), button -> {
                    config.setEnabled(!config.enabled());
                    button.setMessage(enabledLabel(config));
                })
                .width(WIDTH)
                .tooltip(Tooltip.create(Component.translatable("limn.options.enabled.tooltip")))
                .build();
    }

    private static Component enabledLabel(OutlineConfig config) {
        Component state = Component.translatable(config.enabled() ? "options.on" : "options.off");
        return Component.translatable("options.generic_value", Component.translatable("limn.options.enabled"), state);
    }

    private AbstractWidget modeButton(OutlineConfig config) {
        return Button.builder(modeLabel(config.mode()), button -> {
                    config.setMode(OutlinePolicy.isRainbow(config.mode())
                            ? OutlinePolicy.MODE_SOLID
                            : OutlinePolicy.MODE_RAINBOW);
                    button.setMessage(modeLabel(config.mode()));
                })
                .width(WIDTH)
                .build();
    }

    private static Component modeLabel(String mode) {
        String key = OutlinePolicy.isRainbow(mode) ? "limn.options.mode.rainbow" : "limn.options.mode.solid";
        return Component.translatable("limn.options.mode", Component.translatable(key));
    }

    private AbstractWidget resetButton(OutlineConfig config) {
        return Button.builder(Component.translatable("limn.options.reset"), button -> {
                    config.resetToDefaults();
                    rebuildWidgets();
                })
                .width(WIDTH)
                .build();
    }

    private static void setChannel(OutlineConfig config, int shift, int value) {
        int mask = ~(0xFF << shift);
        config.setColor((config.color() & mask) | ((value & 0xFF) << shift));
    }

    private static AbstractWidget intSlider(String captionKey, String tooltipKey, int initial, IntConsumer onChange) {
        return new StepSlider(
                captionKey,
                tooltipKey,
                0,
                255,
                1,
                initial,
                value -> Integer.toString((int) value),
                value -> onChange.accept((int) value));
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final String captionKey;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> format;
        private final DoubleConsumer onChange;

        StepSlider(
                String captionKey,
                String tooltipKey,
                double min,
                double max,
                double step,
                double initial,
                DoubleFunction<String> format,
                DoubleConsumer onChange) {

            super(0, 0, WIDTH, HEIGHT, Component.empty(), 0.0);
            this.captionKey = captionKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            this.value = (snap(initial) - min) / (max - min);
            setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
            updateMessage();
        }

        private double snap(double raw) {
            double clamped = Math.max(min, Math.min(max, raw));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        private double current() {
            return snap(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            Component shown = Component.literal(format.apply(current()));
            setMessage(Component.translatable("options.generic_value", Component.translatable(captionKey), shown));
        }

        @Override
        protected void applyValue() {
            double snapped = current();
            value = (snapped - min) / (max - min);
            onChange.accept(snapped);
        }
    }
}
