package com.roleconnectors.link;

import com.roleconnectors.RoleConnectorsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LinkCommand implements CommandExecutor {

    private final RoleConnectorsPlugin plugin;

    public LinkCommand(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (command.getName().equalsIgnoreCase("unlink")) {
            return handleUnlink(player);
        }

        return handleLink(player);
    }

    private boolean handleLink(Player player) {
        LinkManager linkManager = plugin.getLinkManager();
        if (linkManager.isLinkedByMinecraft(player.getUniqueId())) {
            player.sendMessage(Component.text("Your account is already linked! Use /unlink to unlink first.").color(NamedTextColor.RED));
            return true;
        }

        String code = linkManager.generateCode(player.getUniqueId());
        int timeout = plugin.getConfigManager().verifyCodeTimeout();

        player.sendMessage(Component.text("=== Account Linking ===").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("DM the Discord bot the following command:").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("/verify " + code).color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Code expires in " + timeout + " seconds.").color(NamedTextColor.YELLOW));

        return true;
    }

    private boolean handleUnlink(Player player) {
        LinkManager linkManager = plugin.getLinkManager();
        if (!linkManager.isLinkedByMinecraft(player.getUniqueId())) {
            player.sendMessage(Component.text("Your account is not linked.").color(NamedTextColor.RED));
            return true;
        }

        linkManager.unlinkByMinecraft(player.getUniqueId());
        player.sendMessage(Component.text("Your account has been unlinked.").color(NamedTextColor.GREEN));

        return true;
    }
}
