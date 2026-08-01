package com.roleconnectors.sync;

import com.roleconnectors.RoleConnectorsPlugin;
import com.roleconnectors.config.ConnectorEntry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SyncManager {

    private final RoleConnectorsPlugin plugin;

    public SyncManager(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
    }

    public void syncDiscordToMinecraft(String discordId) {
        LuckPerms luckPerms = plugin.getLuckPerms();
        JDA jda = plugin.getJda();
        if (jda == null) return;

        Optional<UUID> optUuid = plugin.getLinkManager().getMinecraftUuid(discordId);
        if (optUuid.isEmpty()) return;

        UUID minecraftUuid = optUuid.get();

        for (String guildId : plugin.getConfigManager().getAllGuildIds()) {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) continue;

            Member member = guild.getMemberById(discordId);
            if (member == null) continue;

            Set<String> memberRoleNames = member.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

            List<ConnectorEntry> connectors = plugin.getConfigManager().getConnectorsForGuild(guildId);

            for (ConnectorEntry entry : connectors) {
                if (!entry.syncsDiscordToMinecraft()) continue;

                boolean hasRole = memberRoleNames.contains(entry.discordRoleName());

                if (hasRole) {
                    addGroupIfAbsent(luckPerms, minecraftUuid, entry);
                } else if (plugin.getConfigManager().removeGroupOnRoleRemove()) {
                    removeGroupIfPresent(luckPerms, minecraftUuid, entry);
                }
            }
        }
    }

    public void syncMinecraftToDiscord(UUID minecraftUuid) {
        LuckPerms luckPerms = plugin.getLuckPerms();
        JDA jda = plugin.getJda();
        if (jda == null) return;

        Optional<String> optDiscordId = plugin.getLinkManager().getDiscordId(minecraftUuid);
        if (optDiscordId.isEmpty()) return;

        String discordId = optDiscordId.get();
        Player player = Bukkit.getPlayer(minecraftUuid);

        User user;
        if (player != null) {
            user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        } else {
            user = luckPerms.getUserManager().loadUser(minecraftUuid).join();
        }

        Set<String> playerGroups = user.getInheritedGroups(user.getQueryOptions()).stream()
            .map(g -> g.getName().toLowerCase())
            .collect(Collectors.toSet());

        Set<String> processedRoles = new HashSet<>();

        for (String guildId : plugin.getConfigManager().getAllGuildIds()) {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) continue;

            Member member = guild.getMemberById(discordId);
            if (member == null) continue;

            Set<String> memberRoleNames = member.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

            List<ConnectorEntry> connectors = plugin.getConfigManager().getConnectorsForGuild(guildId);

            for (ConnectorEntry entry : connectors) {
                if (!entry.syncsMinecraftToDiscord()) continue;
                if (processedRoles.contains(entry.discordRoleName())) continue;
                processedRoles.add(entry.discordRoleName());

                boolean hasGroup = playerGroups.contains(entry.minecraftGroup().toLowerCase());
                boolean hasRole = memberRoleNames.contains(entry.discordRoleName());

                List<Role> discordRoles = guild.getRolesByName(entry.discordRoleName(), true);
                if (discordRoles.isEmpty()) {
                    log("Discord role '" + entry.discordRoleName() + "' not found in guild " + guildId + ", skipping.");
                    continue;
                }
                Role discordRole = discordRoles.get(0);

                if (hasGroup && !hasRole) {
                    guild.addRoleToMember(member, discordRole).queue(
                        v -> log("Added Discord role '" + entry.discordRoleName() + "' to " + discordId + " (MC group '" + entry.minecraftGroup() + "')"),
                        e -> plugin.getLogger().warning("Failed to add Discord role: " + e.getMessage())
                    );
                } else if (!hasGroup && hasRole && plugin.getConfigManager().removeRoleOnGroupRemove()) {
                    guild.removeRoleFromMember(member, discordRole).queue(
                        v -> log("Removed Discord role '" + entry.discordRoleName() + "' from " + discordId + " (MC group '" + entry.minecraftGroup() + "' removed)"),
                        e -> plugin.getLogger().warning("Failed to remove Discord role: " + e.getMessage())
                    );
                }
            }
        }

        if (player == null) {
            luckPerms.getUserManager().cleanupUser(user);
        }
    }

    public void addMinecraftGroup(UUID minecraftUuid, String groupName) {
        syncMinecraftToDiscord(minecraftUuid);
    }

    public void removeMinecraftGroup(UUID minecraftUuid, String groupName) {
        syncMinecraftToDiscord(minecraftUuid);
    }

    public void addDiscordRole(String guildId, String discordId, String discordRoleName) {
        ConnectorEntry entry = plugin.getConfigManager().findByDiscordRole(guildId, discordRoleName);
        if (entry == null || !entry.syncsDiscordToMinecraft()) return;

        Optional<UUID> optUuid = plugin.getLinkManager().getMinecraftUuid(discordId);
        if (optUuid.isEmpty()) return;

        addGroupIfAbsent(plugin.getLuckPerms(), optUuid.get(), entry);
    }

    public void removeDiscordRole(String guildId, String discordId, String discordRoleName) {
        if (!plugin.getConfigManager().removeGroupOnRoleRemove()) return;

        ConnectorEntry entry = plugin.getConfigManager().findByDiscordRole(guildId, discordRoleName);
        if (entry == null || !entry.syncsDiscordToMinecraft()) return;

        Optional<UUID> optUuid = plugin.getLinkManager().getMinecraftUuid(discordId);
        if (optUuid.isEmpty()) return;

        removeGroupIfPresent(plugin.getLuckPerms(), optUuid.get(), entry);
    }

    private void addGroupIfAbsent(LuckPerms luckPerms, UUID minecraftUuid, ConnectorEntry entry) {
        luckPerms.getUserManager().modifyUser(minecraftUuid, user -> {
            user.data().add(Node.builder("group." + entry.minecraftGroup()).build());
        }).thenRun(() -> luckPerms.runUpdateTask());

        log("Added LP group '" + entry.minecraftGroup() + "' to " + minecraftUuid + " via connector '" + entry.discordRoleName() + "'");
    }

    private void removeGroupIfPresent(LuckPerms luckPerms, UUID minecraftUuid, ConnectorEntry entry) {
        luckPerms.getUserManager().modifyUser(minecraftUuid, user -> {
            user.data().remove(Node.builder("group." + entry.minecraftGroup()).build());
        }).thenRun(() -> luckPerms.runUpdateTask());

        log("Removed LP group '" + entry.minecraftGroup() + "' from " + minecraftUuid + " via connector '" + entry.discordRoleName() + "'");
    }

    private void log(String message) {
        if (plugin.getConfigManager().logSyncActions()) {
            plugin.getLogger().info("[Sync] " + message);
        }
    }
}
