package com.justcoordinates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HudConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String FILE_NAME = "justcoordinates.json";
    private static final Logger LOGGER = LogManager.getLogger(JustCoordinates.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                LOGGER.warn("{} is empty; using defaults", FILE_NAME);
                return;
            }
            if (json.has("position")) {
                String name = json.get("position").getAsString();
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
        JsonObject json = new JsonObject();
        json.addProperty("schema_version", CURRENT_SCHEMA_VERSION);
        json.addProperty("position", position.getSerializedName());
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save {}", FILE_NAME, e);
        }
    }
}
