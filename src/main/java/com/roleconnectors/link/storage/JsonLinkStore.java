package com.roleconnectors.link.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class JsonLinkStore implements LinkStore {

    private final File file;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, String> links = new ConcurrentHashMap<>();

    public JsonLinkStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "linked-accounts.json");
        this.logger = logger;
    }

    @Override
    public void init() {
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    links.putAll(loaded);
                }
                logger.info("Loaded " + links.size() + " linked account(s) from JSON.");
            } catch (IOException e) {
                logger.warning("Failed to load linked accounts: " + e.getMessage());
            }
        }
    }

    @Override
    public void link(String discordId, UUID minecraftUuid) {
        links.put(discordId, minecraftUuid.toString());
        save();
    }

    @Override
    public void unlink(String discordId) {
        links.remove(discordId);
        save();
    }

    @Override
    public void unlinkByMinecraft(UUID minecraftUuid) {
        links.values().removeIf(v -> v.equals(minecraftUuid.toString()));
        save();
    }

    @Override
    public Optional<UUID> getMinecraftUuid(String discordId) {
        String uuid = links.get(discordId);
        if (uuid == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getDiscordId(UUID minecraftUuid) {
        return links.entrySet().stream()
            .filter(e -> e.getValue().equals(minecraftUuid.toString()))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    @Override
    public boolean isLinked(String discordId) {
        return links.containsKey(discordId);
    }

    @Override
    public boolean isLinkedByMinecraft(UUID minecraftUuid) {
        return links.containsValue(minecraftUuid.toString());
    }

    @Override
    public void close() {
        save();
    }

    private synchronized void save() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(links, writer);
        } catch (IOException e) {
            logger.warning("Failed to save linked accounts: " + e.getMessage());
        }
    }
}
