package com.justcoordinates.forge;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.JustCoordinates;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesForge {

    @Mod.EventBusSubscriber(modid = JustCoordinates.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("coordinates_hud",
                    (gui, poseStack, partialTick, screenWidth, screenHeight) ->
                            CoordinatesHudRenderer.render(poseStack));
        }
    }
}
