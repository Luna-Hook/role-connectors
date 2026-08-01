package com.roleconnectors.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final Map<String, List<ConnectorEntry>> guildConnectors = new ConcurrentHashMap<>();
    private final Map<String, ConnectorEntry> discordRoleIndex = new ConcurrentHashMap<>();
    private final Map<String, List<ConnectorEntry>> mcGroupIndex = new ConcurrentHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        guildConnectors.clear();
        discordRoleIndex.clear();
        mcGroupIndex.clear();

        ConfigurationSection guildsSection = config.getConfigurationSection("guilds");
        if (guildsSection == null) return;

        for (String guildId : guildsSection.getKeys(false)) {
            ConfigurationSection connectorsSection = guildsSection.getConfigurationSection(guildId + ".connectors");
            if (connectorsSection == null) continue;

            List<ConnectorEntry> entries = new ArrayList<>();

            for (String discordRole : connectorsSection.getKeys(false)) {
                ConfigurationSection entrySection = connectorsSection.getConfigurationSection(discordRole);
                if (entrySection == null) continue;

                String mcGroup = entrySection.getString("group");
                if (mcGroup == null || mcGroup.isBlank()) {
                    plugin.getLogger().warning("Skipping connector for role '" + discordRole + "' in guild " + guildId + ": no 'group' set.");
                    continue;
                }

                Direction direction = Direction.fromString(entrySection.getString("direction"));
                ConnectorEntry entry = new ConnectorEntry(guildId, discordRole, mcGroup, direction);

                entries.add(entry);
                discordRoleIndex.put(guildId + ":" + discordRole.toLowerCase(), entry);
                mcGroupIndex.computeIfAbsent(mcGroup.toLowerCase(), k -> new ArrayList<>()).add(entry);
            }

            guildConnectors.put(guildId, entries);
        }

        plugin.getLogger().info("Loaded " + discordRoleIndex.size() + " connector(s) across " + guildConnectors.size() + " guild(s).");
    }

    public String getBotToken() {
        return config.getString("discord.bot-token", "YOUR_BOT_TOKEN_HERE");
    }

    public List<ConnectorEntry> getConnectorsForGuild(String guildId) {
        return guildConnectors.getOrDefault(guildId, Collections.emptyList());
    }

    public ConnectorEntry findByDiscordRole(String guildId, String roleName) {
        return discordRoleIndex.get(guildId + ":" + roleName.toLowerCase());
    }

    public List<ConnectorEntry> findByMinecraftGroup(String groupName) {
        return mcGroupIndex.getOrDefault(groupName.toLowerCase(), Collections.emptyList());
    }

    public boolean syncOnJoin() {
        return config.getBoolean("settings.sync-on-join", true);
    }

    public boolean syncOnBotStart() {
        return config.getBoolean("settings.sync-on-bot-start", false);
    }

    public boolean removeGroupOnRoleRemove() {
        return config.getBoolean("settings.remove-group-on-role-remove", true);
    }

    public boolean removeRoleOnGroupRemove() {
        return config.getBoolean("settings.remove-role-on-group-remove", true);
    }

    public boolean logSyncActions() {
        return config.getBoolean("settings.log-sync-actions", true);
    }

    public int verifyCodeTimeout() {
        return config.getInt("linking.verify-code-timeout", 300);
    }

    public String linkStoreType() {
        return config.getString("linking.store", "json");
    }

    public List<String> getAllGuildIds() {
        return new ArrayList<>(guildConnectors.keySet());
    }

    public boolean hasConnectors() {
        return !discordRoleIndex.isEmpty();
    }
}
