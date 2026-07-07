package com.justcoordinates;

import java.util.Arrays;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("justcoordinates.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(CycleButton.builder(
                        (HudPosition value) -> Component.translatable(value.getTranslationKey()),
                        HudConfig.getPosition())
                .withValues(Arrays.asList(HudPosition.values()))
                .create(width / 2 - 100, height / 2 - 24, 200, 20,
                        Component.translatable("justcoordinates.config.position"),
                        (button, value) -> HudConfig.setPosition(value)));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height / 2 + 4, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        HudConfig.save();
        minecraft.setScreen(parent);
    }
}
