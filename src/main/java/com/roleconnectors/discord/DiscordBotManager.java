package com.roleconnectors.discord;

import com.roleconnectors.RoleConnectorsPlugin;
import com.roleconnectors.link.DiscordLinkListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordBotManager {

    private final RoleConnectorsPlugin plugin;
    private JDA jda;

    public DiscordBotManager(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        String token = plugin.getConfigManager().getBotToken();
        if (token == null || token.isBlank() || "YOUR_BOT_TOKEN_HERE".equals(token)) {
            plugin.getLogger().warning("Discord bot token not configured. Discord integration disabled.");
            return;
        }

        plugin.getLogger().info("Starting Discord bot...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                jda = JDABuilder.createDefault(token)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MODERATION,
                        GatewayIntent.MESSAGE_CONTENT
                    )
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                    .setActivity(Activity.playing("mc.yourserver.com"))
                    .addEventListeners(
                        new DiscordRoleListener(plugin),
                        new DiscordLinkListener(plugin)
                    )
                    .build();
                jda.awaitReady();

                plugin.getLogger().info("Discord bot connected as: " + jda.getSelfUser().getName());
                plugin.getLogger().info("Bot is in " + jda.getGuilds().size() + " guild(s).");

                DiscordLinkListener.registerSlashCommand(jda);

                if (plugin.getConfigManager().syncOnBotStart()) {
                    plugin.getLogger().info("Performing initial sync on bot start...");
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        for (String guildId : plugin.getConfigManager().getAllGuildIds()) {
                            var guild = jda.getGuildById(guildId);
                            if (guild == null) continue;

                            guild.loadMembers().get();
                            guild.getMembers().forEach(member -> {
                                String discordId = member.getId();
                                if (plugin.getLinkManager().isLinked(discordId)) {
                                    plugin.getSyncManager().syncDiscordToMinecraft(discordId);
                                }
                            });
                        }
                        plugin.getLogger().info("Initial sync complete.");
                    });
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to initialize Discord bot: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        if (jda != null) {
            plugin.getLogger().info("Shutting down Discord bot...");
            jda.shutdown();
            jda = null;
        }
    }

    public JDA getJda() {
        return jda;
    }

    public boolean isConnected() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }
}
