package com.justcoordinates;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.PoseStack;
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
                        (HudPosition value) -> Component.translatable(value.getTranslationKey()))
                .withValues(Arrays.asList(HudPosition.values()))
                .withInitialValue(HudConfig.getPosition())
                .create(width / 2 - 100, height / 2 - 24, 200, 20,
                        Component.translatable("justcoordinates.config.position"),
                        (button, value) -> HudConfig.setPosition(value)));
        addRenderableWidget(new Button(width / 2 - 100, height / 2 + 4, 200, 20,
                CommonComponents.GUI_DONE, button -> onClose()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, title, width / 2, height / 2 - 60, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        HudConfig.save();
        minecraft.setScreen(parent);
    }
}
