package com.justcoordinates;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(new TranslatableComponent("justcoordinates.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addButton(new Button(width / 2 - 100, height / 2 - 24, 200, 20, positionLabel(),
                button -> {
                    HudConfig.setPosition(HudConfig.getPosition().next());
                    button.setMessage(positionLabel());
                }));
        addButton(new Button(width / 2 - 100, height / 2 + 4, 200, 20, CommonComponents.GUI_DONE,
                button -> onClose()));
    }

    private Component positionLabel() {
        return new TranslatableComponent("justcoordinates.config.position")
                .append(": ")
                .append(new TranslatableComponent(HudConfig.getPosition().getTranslationKey()));
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
