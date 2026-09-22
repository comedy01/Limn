package dev.limn.client.gui;

import com.mojang.serialization.Codec;
import dev.limn.client.LimnClient;
import dev.limn.config.OutlineConfig;
import dev.limn.outline.OutlinePolicy;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

public final class LimnSettingsScreen extends OptionsSubScreen {
    private static final int WIDE_BUTTON = 310;

    public LimnSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("limn.options.title"));
    }

    @Override
    protected void addOptions() {
        OutlineConfig config = LimnClient.config();

        list.addBig(OptionInstance.createBoolean(
                "limn.options.enabled",
                OptionInstance.cachedConstantTooltip(Component.translatable("limn.options.enabled.tooltip")),
                config.enabled(),
                config::setEnabled));

        list.addBig(Button.builder(
                        modeLabel(config.mode()),
                        button -> {
                            config.setMode(OutlinePolicy.isRainbow(config.mode())
                                    ? OutlinePolicy.MODE_SOLID
                                    : OutlinePolicy.MODE_RAINBOW);
                            rebuildWidgets();
                        })
                .width(WIDE_BUTTON)
                .build());

        list.addBig(intSlider(
                "limn.options.alpha",
                "limn.options.color.tooltip",
                (config.color() >>> 24) & 0xFF,
                value -> setChannel(config, 24, value)));
        list.addBig(intSlider(
                "limn.options.red",
                "limn.options.color.tooltip",
                (config.color() >>> 16) & 0xFF,
                value -> setChannel(config, 16, value)));
        list.addBig(intSlider(
                "limn.options.green",
                "limn.options.color.tooltip",
                (config.color() >>> 8) & 0xFF,
                value -> setChannel(config, 8, value)));
        list.addBig(intSlider(
                "limn.options.blue",
                "limn.options.color.tooltip",
                config.color() & 0xFF,
                value -> setChannel(config, 0, value)));

        list.addBig(slider(
                "limn.options.width",
                "limn.options.width.tooltip",
                OutlinePolicy.MIN_WIDTH,
                OutlinePolicy.MAX_WIDTH,
                0.1,
                config.width(),
                value -> String.format(Locale.ROOT, "%.2fx", value),
                config::setWidth));

        list.addBig(slider(
                "limn.options.rainbow_speed",
                "limn.options.rainbow_speed.tooltip",
                OutlinePolicy.MIN_SPEED,
                OutlinePolicy.MAX_SPEED,
                0.1,
                config.rainbowSpeed(),
                value -> String.format(Locale.ROOT, "%.1f", value),
                config::setRainbowSpeed));

        list.addBig(slider(
                "limn.options.rainbow_spread",
                "limn.options.rainbow_spread.tooltip",
                OutlinePolicy.MIN_SPREAD,
                OutlinePolicy.MAX_SPREAD,
                0.1,
                config.rainbowSpread(),
                value -> String.format(Locale.ROOT, "%.2f", value),
                config::setRainbowSpread));

        list.addBig(Button.builder(
                        Component.translatable("limn.options.reset"),
                        button -> {
                            config.resetToDefaults();
                            rebuildWidgets();
                        })
                .width(WIDE_BUTTON)
                .build());
    }

    @Override
    public void removed() {
        super.removed();
        LimnClient.saveConfig();
    }

    private static void setChannel(OutlineConfig config, int shift, int value) {
        int mask = ~(0xFF << shift);
        config.setColor((config.color() & mask) | ((value & 0xFF) << shift));
    }

    private static Component modeLabel(String mode) {
        String key = OutlinePolicy.isRainbow(mode) ? "limn.options.mode.rainbow" : "limn.options.mode.solid";
        return Component.translatable("limn.options.mode", Component.translatable(key));
    }

    private static OptionInstance<Double> slider(
            String captionKey,
            String tooltipKey,
            double min,
            double max,
            double step,
            double initial,
            DoubleFunction<String> format,
            OptionInstance.ValueUpdateListener<Double> onChange) {

        SteppedRange range = new SteppedRange(min, max, step);
        return new OptionInstance<>(
                captionKey,
                OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey)),
                (caption, value) -> Component.translatable(
                        "options.generic_value", caption, Component.literal(format.apply(value))),
                range,
                range.snap(initial),
                onChange);
    }

    private static OptionInstance<Integer> intSlider(
            String captionKey,
            String tooltipKey,
            int initial,
            OptionInstance.ValueUpdateListener<Integer> onChange) {

        IntFunction<String> format = value -> Integer.toString(value);
        return new OptionInstance<>(
                captionKey,
                OptionInstance.cachedConstantTooltip(Component.translatable(tooltipKey)),
                (caption, value) -> Component.translatable(
                        "options.generic_value", caption, Component.literal(format.apply(value))),
                new ByteRange(),
                Math.max(0, Math.min(255, initial)),
                onChange);
    }

    private record SteppedRange(double min, double max, double step)
            implements OptionInstance.SliderableValueSet<Double> {

        double snap(double value) {
            double clamped = Math.max(min, Math.min(max, value));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        @Override
        public double toSliderValue(Double value) {
            return (snap(value) - min) / (max - min);
        }

        @Override
        public Double fromSliderValue(double slider) {
            return snap(min + Math.max(0.0, Math.min(1.0, slider)) * (max - min));
        }

        @Override
        public Optional<Double> validateValue(Double value) {
            return value != null && value >= min && value <= max ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Codec<Double> codec() {
            return Codec.doubleRange(min, max);
        }
    }

    private record ByteRange() implements OptionInstance.SliderableValueSet<Integer> {
        @Override
        public double toSliderValue(Integer value) {
            return Math.max(0, Math.min(255, value)) / 255.0;
        }

        @Override
        public Integer fromSliderValue(double slider) {
            return (int) Math.round(Math.max(0.0, Math.min(1.0, slider)) * 255.0);
        }

        @Override
        public Optional<Integer> validateValue(Integer value) {
            return value != null && value >= 0 && value <= 255 ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.intRange(0, 255);
        }
    }
}
