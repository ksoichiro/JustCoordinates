package com.justcoordinates;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HudConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String FILE_NAME = "justcoordinates.toml";
    private static final Logger LOGGER = LogManager.getLogger(JustCoordinates.MOD_ID);

    private static Path configFile;
    private static HudPosition position = HudPosition.DEFAULT;

    private HudConfig() {
    }

    public static HudPosition getPosition() {
        return position;
    }

    public static void setPosition(HudPosition value) {
        position = value == null ? HudPosition.DEFAULT : value;
    }

    public static void load(Path configDir) {
        configFile = configDir.resolve(FILE_NAME);
        if (!Files.exists(configFile)) {
            return; // keep defaults; the file is first created on save
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            CommentedConfig toml = new TomlParser().parse(reader);
            if (toml.isEmpty()) {
                LOGGER.warn("{} is empty; using defaults", FILE_NAME);
                return;
            }
            Object value = toml.get("position");
            if (value != null) {
                if (!(value instanceof String name)) {
                    LOGGER.warn("Invalid position '{}' in {}; using default", value, FILE_NAME);
                    position = HudPosition.DEFAULT;
                    return;
                }
                HudPosition parsed = HudPosition.fromSerializedName(name);
                if (parsed == null) {
                    LOGGER.warn("Unknown position '{}' in {}; using default", name, FILE_NAME);
                    parsed = HudPosition.DEFAULT;
                }
                position = parsed;
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed to read {}; using defaults", FILE_NAME, e);
            position = HudPosition.DEFAULT;
        }
    }

    public static void save() {
        if (configFile == null) {
            LOGGER.warn("Config was never loaded; skipping save");
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            try (CommentedFileConfig toml = CommentedFileConfig.builder(configFile, TomlFormat.instance()).build()) {
                toml.set("schema_version", CURRENT_SCHEMA_VERSION);
                toml.set("position", position.getSerializedName());
                toml.save();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save {}", FILE_NAME, e);
        }
    }
}
