package com.roleconnectors.minecraft;

import com.roleconnectors.RoleConnectorsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final RoleConnectorsPlugin plugin;

    public PlayerJoinListener(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfigManager().syncOnJoin()) return;

        plugin.getLinkManager().getDiscordId(event.getPlayer().getUniqueId()).ifPresent(discordId -> {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getSyncManager().syncMinecraftToDiscord(event.getPlayer().getUniqueId());
            });
        });
    }
}
