package com.justcoordinates.fabric;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.CoordinatesShare;
import com.justcoordinates.HudConfig;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) ->
                CoordinatesHudRenderer.render(guiGraphics));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralCommandNode<FabricClientCommandSource> shareRoot = dispatcher.register(
                    ClientCommandManager.literal("justcoordinates")
                            .then(ClientCommandManager.literal("share")
                                    .executes(context -> {
                                        CoordinatesShare.share();
                                        return 1;
                                    })));
            // Best-effort short alias: if another client-side mod already owns "/jc", only the
            // alias is lost, never "/justcoordinates share".
            dispatcher.register(ClientCommandManager.literal("jc").redirect(shareRoot));
        });
    }
}
