package dev.limn.client.gui;

import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import dev.limn.config.OutlineConfig;
import dev.limn.outline.OutlinePolicy;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;

public final class LimnSettingsScreen extends LimnOptionsScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public LimnSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Texts.translatable("limn.options.title"));
    }

    @Override
    protected void addOptions() {
        OutlineConfig config = LimnClient.config();

        addRow(enabledButton(config), modeButton(config));

        addRow(
                intSlider("limn.options.alpha", "limn.options.color.tooltip",
                        (config.color() >>> 24) & 0xFF, value -> setChannel(config, 24, value)),
                intSlider("limn.options.red", "limn.options.color.tooltip",
                        (config.color() >>> 16) & 0xFF, value -> setChannel(config, 16, value)));

        addRow(
                intSlider("limn.options.green", "limn.options.color.tooltip",
                        (config.color() >>> 8) & 0xFF, value -> setChannel(config, 8, value)),
                intSlider("limn.options.blue", "limn.options.color.tooltip",
                        config.color() & 0xFF, value -> setChannel(config, 0, value)));

        AbstractWidget widthSlider = slider(new StepSlider(
                "limn.options.width",
                "limn.options.width.tooltip",
                OutlinePolicy.MIN_WIDTH,
                OutlinePolicy.MAX_WIDTH,
                0.1,
                config.width(),
                value -> String.format(Locale.ROOT, "%.2fx", value),
                config::setWidth));
        if (!LimnRuntime.SUPPORTS_LINE_WIDTH) {
            widthSlider.active = false;
            tooltip(widthSlider, Texts.translatable("limn.options.width.unsupported"));
        }
        AbstractWidget speedSlider = slider(new StepSlider(
                "limn.options.rainbow_speed",
                "limn.options.rainbow_speed.tooltip",
                OutlinePolicy.MIN_SPEED,
                OutlinePolicy.MAX_SPEED,
                0.1,
                config.rainbowSpeed(),
                value -> String.format(Locale.ROOT, "%.1f", value),
                config::setRainbowSpeed));
        addRow(widthSlider, speedSlider);

        AbstractWidget spreadSlider = slider(new StepSlider(
                "limn.options.rainbow_spread",
                "limn.options.rainbow_spread.tooltip",
                OutlinePolicy.MIN_SPREAD,
                OutlinePolicy.MAX_SPREAD,
                0.1,
                config.rainbowSpread(),
                value -> String.format(Locale.ROOT, "%.2f", value),
                config::setRainbowSpread));
        addRow(spreadSlider, resetButton(config));
    }

    @Override
    public void removed() {
        super.removed();
        LimnClient.saveConfig();
    }

    private AbstractWidget enabledButton(OutlineConfig config) {
        return tooltip(
                new Button(0, 0, WIDTH, HEIGHT, enabledLabel(config), button -> {
                    config.setEnabled(!config.enabled());
                    button.setMessage(enabledLabel(config));
                }),
                Texts.translatable("limn.options.enabled.tooltip"));
    }

    private static Component enabledLabel(OutlineConfig config) {
        Component state = Texts.translatable(config.enabled() ? "options.on" : "options.off");
        return Texts.translatable("options.generic_value", Texts.translatable("limn.options.enabled"), state);
    }

    private AbstractWidget modeButton(OutlineConfig config) {
        return new Button(0, 0, WIDTH, HEIGHT, modeLabel(config.mode()), button -> {
            config.setMode(OutlinePolicy.isRainbow(config.mode())
                    ? OutlinePolicy.MODE_SOLID
                    : OutlinePolicy.MODE_RAINBOW);
            button.setMessage(modeLabel(config.mode()));
        });
    }

    private static Component modeLabel(String mode) {
        String key = OutlinePolicy.isRainbow(mode) ? "limn.options.mode.rainbow" : "limn.options.mode.solid";
        return Texts.translatable("limn.options.mode", Texts.translatable(key));
    }

    private AbstractWidget resetButton(OutlineConfig config) {
        return new Button(0, 0, WIDTH, HEIGHT, Texts.translatable("limn.options.reset"), button -> {
            config.resetToDefaults();
            ScreenOpener.open(minecraft, new LimnSettingsScreen(lastScreen, options));
        });
    }

    private static void setChannel(OutlineConfig config, int shift, int value) {
        int mask = ~(0xFF << shift);
        config.setColor((config.color() & mask) | ((value & 0xFF) << shift));
    }

    private AbstractWidget intSlider(String captionKey, String tooltipKey, int initial, IntConsumer onChange) {
        return slider(new StepSlider(
                captionKey,
                tooltipKey,
                0,
                255,
                1,
                initial,
                value -> Integer.toString((int) value),
                value -> onChange.accept((int) value)));
    }

    private AbstractWidget slider(StepSlider slider) {
        return tooltip(slider, Texts.translatable(slider.tooltipKey));
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final String captionKey;
        private final String tooltipKey;
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

            super(0, 0, WIDTH, HEIGHT, Texts.empty(), 0.0);
            this.captionKey = captionKey;
            this.tooltipKey = tooltipKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            this.value = (snap(initial) - min) / (max - min);
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
            Component shown = Texts.literal(format.apply(current()));
            setMessage(Texts.translatable("options.generic_value", Texts.translatable(captionKey), shown));
        }

        @Override
        protected void applyValue() {
            double snapped = current();
            value = (snapped - min) / (max - min);
            onChange.accept(snapped);
        }
    }
}
