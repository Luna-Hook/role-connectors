package com.roleconnectors.config;

public record ConnectorEntry(
    String guildId,
    String discordRoleName,
    String minecraftGroup,
    Direction direction
) {
    public boolean syncsDiscordToMinecraft() {
        return direction == Direction.BOTH || direction == Direction.DISCORD_TO_MINECRAFT;
    }

    public boolean syncsMinecraftToDiscord() {
        return direction == Direction.BOTH || direction == Direction.MINECRAFT_TO_DISCORD;
    }
}
