package com.roleconnectors.discord;

import com.roleconnectors.RoleConnectorsPlugin;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordRoleListener extends ListenerAdapter {

    private final RoleConnectorsPlugin plugin;

    public DiscordRoleListener(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        String guildId = event.getGuild().getId();
        String discordId = event.getMember().getId();

        if (!plugin.getLinkManager().isLinked(discordId)) return;

        event.getRoles().forEach(role -> {
            if (plugin.getConfigManager().findByDiscordRole(guildId, role.getName()) != null) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getSyncManager().addDiscordRole(guildId, discordId, role.getName());
                });
            }
        });
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        String guildId = event.getGuild().getId();
        String discordId = event.getMember().getId();

        if (!plugin.getLinkManager().isLinked(discordId)) return;

        event.getRoles().forEach(role -> {
            if (plugin.getConfigManager().findByDiscordRole(guildId, role.getName()) != null) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getSyncManager().removeDiscordRole(guildId, discordId, role.getName());
                });
            }
        });
    }
}
