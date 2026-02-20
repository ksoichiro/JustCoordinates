package com.justcoordinates.neoforge;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.JustCoordinates;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesNeoForge {

    @EventBusSubscriber(modid = JustCoordinates.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(JustCoordinates.MOD_ID, "coordinates_hud"),
                    (guiGraphics, deltaTracker) -> CoordinatesHudRenderer.render(guiGraphics));
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(CoordinatesHudRenderer.getToggleKey());
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            CoordinatesHudRenderer.handleTick();
        }
    }
}
