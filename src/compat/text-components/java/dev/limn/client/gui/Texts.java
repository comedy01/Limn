package dev.limn.client.gui;

import net.minecraft.network.chat.Component;

final class Texts {
    private Texts() {
    }

    static Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    static Component literal(String text) {
        return Component.literal(text);
    }

    static Component empty() {
        return Component.empty();
    }
}
