package com.justcoordinates.forge;

import com.justcoordinates.ConfigScreen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

// Kept separate from the mod entry class: a Screen-typed lambda in the entry
// class is resolved during mod class loading and crashes a dedicated server
// even behind a dist guard. This class is only ever loaded from the guarded
// client branch.
final class JustCoordinatesForgeClient {
    private JustCoordinatesForgeClient() {
    }

    static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, parent) -> new ConfigScreen(parent));
    }
}
