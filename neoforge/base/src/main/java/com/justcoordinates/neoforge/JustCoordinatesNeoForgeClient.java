package com.justcoordinates.neoforge;

import com.justcoordinates.ConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Kept separate from the mod entry class: a Screen-typed lambda in the entry
// class is resolved during FML's reflective constructor lookup and crashes a
// dedicated server even behind a dist guard. This class is only ever loaded
// from the guarded client branch.
final class JustCoordinatesNeoForgeClient {
    private JustCoordinatesNeoForgeClient() {
    }

    static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraftOrContainer, parent) -> new ConfigScreen(parent));
    }
}
