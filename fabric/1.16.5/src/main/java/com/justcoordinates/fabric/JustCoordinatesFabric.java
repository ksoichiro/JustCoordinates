package com.justcoordinates.fabric;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.CoordinatesShare;
import com.justcoordinates.HudConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;

public class JustCoordinatesFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudConfig.load(FabricLoader.getInstance().getConfigDir());
        KeyBindingHelper.registerKeyBinding(CoordinatesHudRenderer.getToggleKey());
        KeyBindingHelper.registerKeyBinding(CoordinatesHudRenderer.getOpenConfigKey());
        KeyBindingHelper.registerKeyBinding(CoordinatesHudRenderer.getShareKey());
        ClientTickEvents.END_CLIENT_TICK.register(client -> CoordinatesHudRenderer.handleTick());
        HudRenderCallback.EVENT.register((poseStack, tickDelta) ->
                CoordinatesHudRenderer.render(poseStack));
        ClientCommandManager.DISPATCHER.register(
                ClientCommandManager.literal("justcoordinates")
                        .then(ClientCommandManager.literal("share")
                                .executes(context -> {
                                    CoordinatesShare.share();
                                    return 1;
                                })));
    }
}
