package dev.limn.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.IdentityHashMap;
import java.util.Map;

public abstract class LimnOptionsScreen extends OptionsSubScreen {
    private final Map<AbstractWidget, Component> tooltips = new IdentityHashMap<>();
    private int rows;

    protected LimnOptionsScreen(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    protected abstract void addOptions();

    @Override
    protected void init() {
        rows = 0;
        tooltips.clear();
        addOptions();
        addRenderableWidget(new Button(width / 2 - 100, height - 27, 200, 20, CommonComponents.GUI_DONE, button -> onClose()));
    }

    protected void addRow(AbstractWidget left, AbstractWidget right) {
        int y = height / 6 - 12 + rows * 24;
        left.x = width / 2 - 155;
        left.y = y;
        right.x = width / 2 + 5;
        right.y = y;
        addRenderableWidget(left);
        addRenderableWidget(right);
        rows++;
    }

    // Widgets have no tooltip of their own before 1.19.3, so the screen draws them.
    protected <T extends AbstractWidget> T tooltip(T widget, Component tooltip) {
        tooltips.put(widget, tooltip);
        return widget;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, font, title, width / 2, 15, 0xFFFFFF);
        for (Map.Entry<AbstractWidget, Component> entry : tooltips.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget.visible
                    && mouseX >= widget.x && mouseX < widget.x + widget.getWidth()
                    && mouseY >= widget.y && mouseY < widget.y + widget.getHeight()) {
                renderTooltip(poseStack, font.split(entry.getValue(), 200), mouseX, mouseY);
                break;
            }
        }
    }
}
