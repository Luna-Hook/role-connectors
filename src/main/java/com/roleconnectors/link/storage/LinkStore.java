package com.roleconnectors.link.storage;

import java.util.Optional;
import java.util.UUID;

public interface LinkStore {

    void init();

    void link(String discordId, UUID minecraftUuid);

    void unlink(String discordId);

    void unlinkByMinecraft(UUID minecraftUuid);

    Optional<UUID> getMinecraftUuid(String discordId);

    Optional<String> getDiscordId(UUID minecraftUuid);

    boolean isLinked(String discordId);

    boolean isLinkedByMinecraft(UUID minecraftUuid);

    void close();
}
