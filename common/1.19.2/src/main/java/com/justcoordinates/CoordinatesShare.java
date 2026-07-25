package com.justcoordinates;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class CoordinatesShare {
    private CoordinatesShare() {
    }

    public static void share() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        String message = CoordinatesMessage.format(
                Mth.floor(player.getX()),
                Mth.floor(player.getY()),
                Mth.floor(player.getZ()));
        // 1.19.2 only: LocalPlayer#sendChat is private; chatSigned is the public entry point.
        // Its second argument is the (unused) signed-chat preview component.
        player.chatSigned(message, null);
    }
}
