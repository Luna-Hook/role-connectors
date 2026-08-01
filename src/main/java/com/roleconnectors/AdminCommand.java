package com.roleconnectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AdminCommand implements CommandExecutor {

    private final RoleConnectorsPlugin plugin;

    public AdminCommand(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            case "link" -> handleLink(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== RoleConnectors Admin Commands ===").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/rc reload").color(NamedTextColor.GRAY).append(Component.text(" - Reload config").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/rc status").color(NamedTextColor.GRAY).append(Component.text(" - Show plugin status").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/rc link <discord-id> <player>").color(NamedTextColor.GRAY).append(Component.text(" - Force-link accounts").color(NamedTextColor.WHITE)));
    }

    private boolean handleReload(CommandSender sender) {
        plugin.getConfigManager().load();
        sender.sendMessage(Component.text("Configuration reloaded.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage(Component.text("=== RoleConnectors Status ===").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Bot connected: ").color(NamedTextColor.GRAY)
            .append(Component.text(plugin.isBotConnected() ? "Yes" : "No").color(plugin.isBotConnected() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("Guilds configured: ").color(NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(plugin.getConfigManager().getAllGuildIds().size())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Linked accounts: ").color(NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(plugin.getLinkManager() != null ? "N/A" : "N/A")).color(NamedTextColor.WHITE)));
        return true;
    }

    private boolean handleLink(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /rc link <discord-id> <player>").color(NamedTextColor.RED));
            return true;
        }

        String discordId = args[1];
        String playerName = args[2];
        var player = plugin.getServer().getOfflinePlayer(playerName);

        if (!player.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: " + playerName).color(NamedTextColor.RED));
            return true;
        }

        plugin.getLinkManager().getStore().link(discordId, player.getUniqueId());
        sender.sendMessage(Component.text("Linked Discord ID " + discordId + " to player " + playerName).color(NamedTextColor.GREEN));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getSyncManager().syncDiscordToMinecraft(discordId);
            plugin.getSyncManager().syncMinecraftToDiscord(player.getUniqueId());
        });

        return true;
    }
}
