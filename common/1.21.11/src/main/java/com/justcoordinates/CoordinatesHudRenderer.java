package com.justcoordinates;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class CoordinatesHudRenderer {
    private static final int MARGIN = 2;
    private static final int PADDING = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0x90505050;

    private static boolean visible = true;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(JustCoordinates.MOD_ID, JustCoordinates.MOD_ID));

    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.justcoordinates.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.justcoordinates.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static KeyMapping getToggleKey() {
        return TOGGLE_KEY;
    }

    public static KeyMapping getOpenConfigKey() {
        return OPEN_CONFIG_KEY;
    }

    public static void handleTick() {
        while (TOGGLE_KEY.consumeClick()) {
            visible = !visible;
        }
        while (OPEN_CONFIG_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new ConfigScreen(null));
        }
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        if (mc.player == null) return;
        if (!visible) return;

        int x = Mth.floor(mc.player.getX());
        int y = Mth.floor(mc.player.getY());
        int z = Mth.floor(mc.player.getZ());

        Component text = Component.translatable(
                "justcoordinates.hud.position",
                String.valueOf(x), String.valueOf(y), String.valueOf(z));

        int textWidth = mc.font.width(text);
        int textHeight = mc.font.lineHeight;

        int hudWidth = PADDING + textWidth + PADDING;
        int hudHeight = PADDING + textHeight + PADDING;
        HudPosition position = HudConfig.getPosition();
        int bgX1 = position.resolveX(guiGraphics.guiWidth(), hudWidth, MARGIN);
        int bgY1 = position.resolveY(guiGraphics.guiHeight(), hudHeight, MARGIN);

        guiGraphics.fill(bgX1, bgY1, bgX1 + hudWidth, bgY1 + hudHeight, BACKGROUND_COLOR);
        guiGraphics.drawString(mc.font, text,
                bgX1 + PADDING, bgY1 + PADDING, TEXT_COLOR);
    }
}
