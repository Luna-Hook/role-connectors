package com.roleconnectors.minecraft;

import com.roleconnectors.RoleConnectorsPlugin;
import com.roleconnectors.config.ConnectorEntry;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MinecraftGroupListener {

    private final RoleConnectorsPlugin plugin;

    public MinecraftGroupListener(RoleConnectorsPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(LuckPerms luckPerms) {
        EventBus eventBus = luckPerms.getEventBus();

        eventBus.subscribe(NodeAddEvent.class, event -> {
            if (!(event.getTarget() instanceof User user)) return;
            if (!event.getNode().getType().equals(NodeType.INHERITANCE)) return;

            String groupName = event.getNode().getKey();
            if (groupName.startsWith("group.")) {
                groupName = groupName.substring(6);
            }

            handleGroupChange(user.getUniqueId(), groupName, true);
        });

        eventBus.subscribe(NodeRemoveEvent.class, event -> {
            if (!(event.getTarget() instanceof User user)) return;
            if (!event.getNode().getType().equals(NodeType.INHERITANCE)) return;

            String groupName = event.getNode().getKey();
            if (groupName.startsWith("group.")) {
                groupName = groupName.substring(6);
            }

            handleGroupChange(user.getUniqueId(), groupName, false);
        });
    }

    private void handleGroupChange(UUID minecraftUuid, String groupName, boolean added) {
        List<ConnectorEntry> entries = plugin.getConfigManager().findByMinecraftGroup(groupName);
        if (entries.isEmpty()) return;

        Optional<String> optDiscordId = plugin.getLinkManager().getDiscordId(minecraftUuid);
        if (optDiscordId.isEmpty()) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (added) {
                plugin.getSyncManager().addMinecraftGroup(minecraftUuid, groupName);
            } else if (plugin.getConfigManager().removeRoleOnGroupRemove()) {
                plugin.getSyncManager().removeMinecraftGroup(minecraftUuid, groupName);
            }
        });
    }
}
