package com.justcoordinates;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CoordinatesHudRenderer {
    private static final int MARGIN = 2;
    private static final int PADDING = 2;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0x90505050;

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        if (mc.player == null) return;

        int x = Mth.floor(mc.player.getX());
        int y = Mth.floor(mc.player.getY());
        int z = Mth.floor(mc.player.getZ());

        Component text = Component.translatable(
                "justcoordinates.hud.position",
                String.valueOf(x), String.valueOf(y), String.valueOf(z));

        int textWidth = mc.font.width(text);
        int textHeight = mc.font.lineHeight;

        int bgX1 = MARGIN;
        int bgY1 = MARGIN;
        int bgX2 = MARGIN + PADDING + textWidth + PADDING;
        int bgY2 = MARGIN + PADDING + textHeight + PADDING;

        guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, BACKGROUND_COLOR);
        guiGraphics.drawString(mc.font, text,
                MARGIN + PADDING, MARGIN + PADDING, TEXT_COLOR);
    }
}
