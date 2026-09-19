package com.justcoordinates.neoforge;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.CoordinatesShare;
import com.justcoordinates.HudConfig;
import com.justcoordinates.JustCoordinates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesNeoForge {

    public JustCoordinatesNeoForge(ModContainer container) {
        HudConfig.load(FMLPaths.CONFIGDIR.get());
        // Client-only setup lives in a separate class: a Screen-typed lambda here
        // would be resolved during FML's reflective constructor lookup and crash a
        // dedicated server even behind this guard.
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            JustCoordinatesNeoForgeClient.registerConfigScreen(container);
        }
    }

    @EventBusSubscriber(modid = JustCoordinates.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(
                    Identifier.fromNamespaceAndPath(JustCoordinates.MOD_ID, "coordinates_hud"),
                    (guiGraphics, deltaTracker) -> CoordinatesHudRenderer.render(guiGraphics));
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(CoordinatesHudRenderer.getToggleKey());
            event.register(CoordinatesHudRenderer.getOpenConfigKey());
            event.register(CoordinatesHudRenderer.getShareKey());
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            CoordinatesHudRenderer.handleTick();
        }

        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(
                    Commands.literal("justcoordinates")
                            .then(Commands.literal("share")
                                    .executes(context -> {
                                        CoordinatesShare.share();
                                        return 1;
                                    })));
        }
    }
}
