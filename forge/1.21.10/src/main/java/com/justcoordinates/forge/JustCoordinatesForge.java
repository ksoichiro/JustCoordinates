package com.justcoordinates.forge;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.CoordinatesShare;
import com.justcoordinates.HudConfig;
import com.justcoordinates.JustCoordinates;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesForge {
    public JustCoordinatesForge() {
        HudConfig.load(FMLPaths.CONFIGDIR.get());
        // Client-only setup lives in a separate class: a Screen-typed lambda here
        // would be resolved during mod class loading and crash a dedicated server
        // even behind this guard.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            JustCoordinatesForgeClient.registerConfigScreen();
        }
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            event.register(CoordinatesHudRenderer.getToggleKey());
            event.register(CoordinatesHudRenderer.getOpenConfigKey());
            event.register(CoordinatesHudRenderer.getShareKey());
        });
        RegisterClientCommandsEvent.BUS.addListener(event -> {
            LiteralCommandNode<CommandSourceStack> shareRoot = event.getDispatcher().register(
                    Commands.literal("justcoordinates")
                            .then(Commands.literal("share")
                                    .executes(context -> {
                                        CoordinatesShare.share();
                                        return 1;
                                    })));
            // Best-effort short alias: if another client-side mod already owns "/jc", only the
            // alias is lost, never "/justcoordinates share".
            event.getDispatcher().register(Commands.literal("jc").redirect(shareRoot));
        });
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(event ->
                CoordinatesHudRenderer.render(event.getGuiGraphics()));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event ->
                CoordinatesHudRenderer.handleTick());
    }
}
