package com.roleconnectors.link;

import com.roleconnectors.RoleConnectorsPlugin;
import com.roleconnectors.link.storage.JsonLinkStore;
import com.roleconnectors.link.storage.LinkStore;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LinkManager {

    private final RoleConnectorsPlugin plugin;
    private final LinkStore store;
    private final Map<String, PendingCode> pendingCodes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public LinkManager(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
        String storeType = plugin.getConfigManager().linkStoreType();
        if ("json".equalsIgnoreCase(storeType)) {
            this.store = new JsonLinkStore(plugin.getDataFolder(), plugin.getLogger());
        } else {
            plugin.getLogger().warning("Unknown link store type '" + storeType + "', defaulting to JSON.");
            this.store = new JsonLinkStore(plugin.getDataFolder(), plugin.getLogger());
        }
    }

    public void init() {
        store.init();
    }

    public void shutdown() {
        store.close();
    }

    public LinkStore getStore() {
        return store;
    }

    public String generateCode(UUID minecraftUuid) {
        String code = String.format("%06d", random.nextInt(1000000));
        long expiresAt = System.currentTimeMillis() + (plugin.getConfigManager().verifyCodeTimeout() * 1000L);
        pendingCodes.put(code, new PendingCode(code, minecraftUuid, expiresAt));
        return code;
    }

    public Optional<UUID> redeemCode(String code, String discordId) {
        PendingCode pending = pendingCodes.remove(code);
        if (pending == null) return Optional.empty();
        if (System.currentTimeMillis() > pending.expiresAt()) return Optional.empty();

        UUID uuid = pending.minecraftUuid();
        store.link(discordId, uuid);
        plugin.getLogger().info("Linked Discord user " + discordId + " to Minecraft UUID " + uuid);
        return Optional.of(uuid);
    }

    public void unlink(String discordId) {
        store.unlink(discordId);
    }

    public void unlinkByMinecraft(UUID minecraftUuid) {
        store.unlinkByMinecraft(minecraftUuid);
    }

    public Optional<UUID> getMinecraftUuid(String discordId) {
        return store.getMinecraftUuid(discordId);
    }

    public Optional<String> getDiscordId(UUID minecraftUuid) {
        return store.getDiscordId(minecraftUuid);
    }

    public boolean isLinked(String discordId) {
        return store.isLinked(discordId);
    }

    public boolean isLinkedByMinecraft(UUID minecraftUuid) {
        return store.isLinkedByMinecraft(minecraftUuid);
    }

    private record PendingCode(String code, UUID minecraftUuid, long expiresAt) {}
}
