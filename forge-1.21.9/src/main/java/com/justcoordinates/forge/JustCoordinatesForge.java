package com.justcoordinates.forge;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.JustCoordinates;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesForge {

    @Mod.EventBusSubscriber(modid = JustCoordinates.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRenderOverlay(CustomizeGuiOverlayEvent.Chat event) {
            CoordinatesHudRenderer.render(event.getGuiGraphics());
        }
    }
}
