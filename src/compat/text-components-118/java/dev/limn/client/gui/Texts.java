package dev.limn.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

final class Texts {
    private Texts() {
    }

    static Component translatable(String key, Object... args) {
        return new TranslatableComponent(key, args);
    }

    static Component literal(String text) {
        return new TextComponent(text);
    }

    static Component empty() {
        return TextComponent.EMPTY;
    }
}
