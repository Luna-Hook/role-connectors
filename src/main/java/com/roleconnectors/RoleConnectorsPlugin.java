package com.roleconnectors;

import com.roleconnectors.config.ConfigManager;
import com.roleconnectors.discord.DiscordBotManager;
import com.roleconnectors.link.LinkCommand;
import com.roleconnectors.link.LinkManager;
import com.roleconnectors.minecraft.MinecraftGroupListener;
import com.roleconnectors.minecraft.PlayerJoinListener;
import com.roleconnectors.sync.SyncManager;
import net.dv8tion.jda.api.JDA;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RoleConnectorsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private LinkManager linkManager;
    private SyncManager syncManager;
    private DiscordBotManager discordBotManager;
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            getLogger().severe("LuckPerms not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        configManager.load();

        this.linkManager = new LinkManager(this);
        linkManager.init();

        this.syncManager = new SyncManager(this);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        MinecraftGroupListener mcListener = new MinecraftGroupListener(this);
        mcListener.register(luckPerms);

        getCommand("link").setExecutor(new LinkCommand(this));
        getCommand("unlink").setExecutor(new LinkCommand(this));
        getCommand("roleconnectors").setExecutor(new AdminCommand(this));

        this.discordBotManager = new DiscordBotManager(this);
        discordBotManager.start();

        getLogger().info("RoleConnectors v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (discordBotManager != null) {
            discordBotManager.shutdown();
        }
        if (linkManager != null) {
            linkManager.shutdown();
        }
        getLogger().info("RoleConnectors disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public JDA getJda() {
        return discordBotManager != null ? discordBotManager.getJda() : null;
    }

    public boolean isBotConnected() {
        return discordBotManager != null && discordBotManager.isConnected();
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
