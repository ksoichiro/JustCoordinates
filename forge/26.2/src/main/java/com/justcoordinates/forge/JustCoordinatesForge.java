package com.justcoordinates.forge;

import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.JustCoordinates;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesForge {
    public JustCoordinatesForge() {
        RegisterKeyMappingsEvent.BUS.addListener(event ->
                event.register(CoordinatesHudRenderer.getToggleKey()));
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(event ->
                CoordinatesHudRenderer.render(event.getGuiGraphics()));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event ->
                CoordinatesHudRenderer.handleTick());
    }
}
