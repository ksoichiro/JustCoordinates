package com.justcoordinates.forge;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.JustCoordinates;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesForge {

    @Mod.EventBusSubscriber(modid = JustCoordinates.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ClientRegistry.registerKeyBinding(CoordinatesHudRenderer.getToggleKey());
        }
    }

    @Mod.EventBusSubscriber(modid = JustCoordinates.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
            if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
            CoordinatesHudRenderer.render(event.getMatrixStack());
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                CoordinatesHudRenderer.handleTick();
            }
        }
    }
}
