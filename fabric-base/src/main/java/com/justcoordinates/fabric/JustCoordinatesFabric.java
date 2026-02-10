package com.justcoordinates.fabric;

import com.justcoordinates.CoordinatesHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class JustCoordinatesFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) ->
                CoordinatesHudRenderer.render(guiGraphics));
    }
}
