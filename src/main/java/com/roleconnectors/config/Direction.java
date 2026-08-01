package com.roleconnectors.config;

public enum Direction {
    BOTH,
    DISCORD_TO_MINECRAFT,
    MINECRAFT_TO_DISCORD;

    public static Direction fromString(String s) {
        if (s == null) return BOTH;
        return switch (s.toUpperCase()) {
            case "DISCORD_TO_MINECRAFT" -> DISCORD_TO_MINECRAFT;
            case "MINECRAFT_TO_DISCORD" -> MINECRAFT_TO_DISCORD;
            default -> BOTH;
        };
    }
}
