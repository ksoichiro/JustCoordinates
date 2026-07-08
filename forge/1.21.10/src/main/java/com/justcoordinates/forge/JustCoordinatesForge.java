package com.justcoordinates.forge;

import com.justcoordinates.ConfigScreen;
import com.justcoordinates.CoordinatesHudRenderer;
import com.justcoordinates.HudConfig;
import com.justcoordinates.JustCoordinates;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(JustCoordinates.MOD_ID)
public class JustCoordinatesForge {
    public JustCoordinatesForge() {
        HudConfig.load(FMLPaths.CONFIGDIR.get());
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            event.register(CoordinatesHudRenderer.getToggleKey());
            event.register(CoordinatesHudRenderer.getOpenConfigKey());
        });
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(event ->
                CoordinatesHudRenderer.render(event.getGuiGraphics()));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event ->
                CoordinatesHudRenderer.handleTick());
    }
}
