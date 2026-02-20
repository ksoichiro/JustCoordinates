package com.justcoordinates.fabric;

import com.justcoordinates.CoordinatesHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class JustCoordinatesFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(CoordinatesHudRenderer.getToggleKey());
        ClientTickEvents.END_CLIENT_TICK.register(client -> CoordinatesHudRenderer.handleTick());
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) ->
                CoordinatesHudRenderer.render(guiGraphics));
    }
}
