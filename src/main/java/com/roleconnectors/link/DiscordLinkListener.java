package com.roleconnectors.link;

import com.roleconnectors.RoleConnectorsPlugin;
import com.roleconnectors.sync.SyncManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class DiscordLinkListener extends ListenerAdapter {

    private final RoleConnectorsPlugin plugin;

    public DiscordLinkListener(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("verify")) return;
        if (!event.isFromGuild() && event.getChannelType() != net.dv8tion.jda.api.entities.channel.ChannelType.PRIVATE) {
            event.reply("Please run this command in DMs with the bot.").setEphemeral(true).queue();
            return;
        }

        String code = event.getOption("code") != null ? event.getOption("code").getAsString() : "";
        if (code.isBlank()) {
            event.reply("Usage: /verify <code>").setEphemeral(true).queue();
            return;
        }

        String discordId = event.getUser().getId();
        LinkManager linkManager = plugin.getLinkManager();

        Optional<UUID> uuid = linkManager.redeemCode(code, discordId);
        if (uuid.isEmpty()) {
            event.reply("Invalid or expired code. Please run /link in-game to get a new code.").setEphemeral(true).queue();
            return;
        }

        event.reply("Account linked successfully! Syncing roles...").setEphemeral(true).queue();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            SyncManager syncManager = plugin.getSyncManager();
            syncManager.syncDiscordToMinecraft(discordId);
            syncManager.syncMinecraftToDiscord(uuid.get());
        });
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(net.dv8tion.jda.api.entities.channel.ChannelType.PRIVATE)) return;

        String content = event.getMessage().getContentRaw().trim();
        if (!content.startsWith("/verify ")) return;

        String code = content.substring(8).trim();
        String discordId = event.getAuthor().getId();
        LinkManager linkManager = plugin.getLinkManager();

        Optional<UUID> uuid = linkManager.redeemCode(code, discordId);
        if (uuid.isEmpty()) {
            event.getChannel().sendMessage("Invalid or expired code. Please run /link in-game to get a new code.").queue();
            return;
        }

        event.getChannel().sendMessage("Account linked successfully! Syncing roles...").queue();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            SyncManager syncManager = plugin.getSyncManager();
            syncManager.syncDiscordToMinecraft(discordId);
            syncManager.syncMinecraftToDiscord(uuid.get());
        });
    }

    public static void registerSlashCommand(net.dv8tion.jda.api.JDA jda) {
        jda.updateCommands().addCommands(
            Commands.slash("verify", "Verify your Minecraft account with a code from /link in-game")
                .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "code", "The 6-digit code from /link", true)
        ).queue();
    }
}
