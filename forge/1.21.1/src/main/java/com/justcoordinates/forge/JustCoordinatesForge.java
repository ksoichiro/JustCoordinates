package com.justcoordinates.forge;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.JustCoordinates;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesForge {

    @Mod.EventBusSubscriber(modid = JustCoordinates.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {
        @SubscribeEvent
        public static void addLayers(AddGuiOverlayLayersEvent event) {
            event.getLayeredDraw().add(
                    ResourceLocation.fromNamespaceAndPath(JustCoordinates.MOD_ID, "coordinates_hud"),
                    (guiGraphics, deltaTracker) -> CoordinatesHudRenderer.render(guiGraphics));
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(CoordinatesHudRenderer.getToggleKey());
        }
    }

    @Mod.EventBusSubscriber(modid = JustCoordinates.MOD_ID, value = Dist.CLIENT)
    public static class ClientTickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                CoordinatesHudRenderer.handleTick();
            }
        }
    }
}
