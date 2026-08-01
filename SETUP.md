# RoleConnectors Setup Guide

Bidirectional Discord role ↔ Minecraft LuckPerms group sync for Paper 1.21.1.

## Requirements

- **Minecraft Server**: Paper 1.21.1
- **Plugin Dependency**: [LuckPerms](https://luckperms.net/) (v5.4+)
- **Java**: 21
- **Discord**: A Discord application with a bot token (privileged intents enabled)

## Quick Start

1. Drop `RoleConnectors-1.1.0.jar` and `LuckPerms.jar` into your server's `plugins/` folder
2. Start the server once to generate default configs
3. Stop the server and edit `plugins/RoleConnectors/config.yml`
4. Restart the server

---

## Step 1: Create a Discord Bot

1. Go to https://discord.com/developers/applications
2. Click **New Application** → give it a name
3. Go to **Bot** tab → click **Reset Token** → copy the token
4. Under **Privileged Gateway Intents**, enable:
   - ✅ **Server Members Intent**
   - ✅ **Message Content Intent**
5. Go to **OAuth2 → URL Generator**:
   - Scopes: `bot`, `applications.commands`
   - Bot Permissions: `Manage Roles`, `Send Messages`, `Read Messages`
6. Use the generated URL to invite the bot to your server
7. Make sure the bot's role is **above** the roles it needs to assign in the server's role list (drag it higher)

---

## Step 2: Configure LuckPerms

Create the groups you want to sync in LuckPerms (in-game or via LP web editor):

```
/lp creategroup vip
/lp creategroup moderator
/lp creategroup admin
```

Assign permissions to each group as needed.

---

## Step 3: Configure the Plugin

Edit `plugins/RoleConnectors/config.yml`:

```yaml
discord:
  bot-token: "YOUR_BOT_TOKEN_HERE"

guilds:
  "123456789012345678":   # Replace with your Discord server ID
    connectors:
      "VIP":
        group: "vip"
        direction: "BOTH"
      "Moderator":
        group: "moderator"
        direction: "DISCORD_TO_MINECRAFT"
      "Builder":
        group: "builder"
        direction: "MINECRAFT_TO_DISCORD"
      "Admin":
        group: "admin"
        direction: "BOTH"

settings:
  sync-on-join: true
  sync-on-bot-start: false
  remove-group-on-role-remove: true
  remove-role-on-group-remove: true
  log-sync-actions: true
```

### Finding Your Guild ID

In Discord: **Server Settings → Widget → Server ID** (or right-click your server icon → Copy ID if Developer Mode is enabled).

### Direction Options

| Direction | Description |
|-----------|-------------|
| `BOTH` | Sync both ways: Discord role ↔ Minecraft group |
| `DISCORD_TO_MINECRAFT` | Only Discord → Minecraft (role add/remove changes LP group) |
| `MINECRAFT_TO_DISCORD` | Only Minecraft → Discord (LP group change changes Discord role) |

### Multiple Guilds

You can configure multiple Discord servers:

```yaml
guilds:
  "123456789012345678":
    connectors:
      "VIP":
        group: "vip"
        direction: "BOTH"
  "987654321098765432":
    connectors:
      "Member":
        group: "member"
        direction: "BOTH"
```

---

## Step 4: Link Your Accounts

Players must link their Discord account to their Minecraft account for sync to work.

### Method 1: In-Game Command (Recommended)

1. Join the Minecraft server
2. Run `/link`
3. You'll see a 6-digit code
4. DM the Discord bot: `/verify 123456` (or type `!verify 123456`)
5. Your accounts are now linked and roles will sync automatically

### Method 2: Admin Force-Link

Server operators can link accounts manually:

```
/rc link <discord-user-id> <minecraft-player-name>
```

Example: `/rc link 123456789012345678 Notch`

### Unlinking

Players can run `/unlink` in-game to disconnect their accounts.

---

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/link` | `roleconnectors.link` | Generate a verification code |
| `/unlink` | `roleconnectors.link` | Unlink your accounts |
| `/rc reload` | `roleconnectors.admin` | Reload the configuration |
| `/rc status` | `roleconnectors.admin` | Show bot connection status |
| `/rc link <id> <player>` | `roleconnectors.admin` | Force-link accounts |

---

## How It Works

```
Discord Role Added ──→ GuildMemberRoleAddEvent
                        └─→ Linked? → Add LP group via LuckPerms API

Minecraft LP Group Added ──→ LuckPerms NodeAddEvent
                            └─→ Linked? → Add Discord role via JDA API

Player Joins Server ──→ PlayerJoinEvent
                        └─→ Linked? → Sync all LP groups → Discord roles

Bot Starts ──→ (optional) Sync all Discord roles → LP groups
```

### Sync Logic

- **Add side is idempotent**: Adding a group/role the player already has is a no-op
- **Remove side respects config**: Disable via `remove-group-on-role-remove` or `remove-role-on-group-remove`
- **Async**: All sync operations happen asynchronously to avoid blocking the server
- **Logging**: Set `log-sync-actions: true` to see sync activity in console

---

## Building from Source

```bash
# Clone or navigate to the project
cd RoleConnectors

# Build (creates build/libs/RoleConnectors-1.1.0.jar)
./gradlew build

# The output jar is in:
# build/libs/RoleConnectors-1.1.0.jar
```

### Build Dependencies (auto-resolved)

| Dependency | Purpose |
|-----------|---------|
| Paper API 1.21.1 | Minecraft server API |
| LuckPerms API 5.4 | Permission group management (compile only) |
| JDA 5.5.0 | Discord bot library (shaded into jar) |
| log4j-slf4j-impl | Logging bridge for JDA (shaded into jar) |

---

## Configuration Reference

### Full `config.yml`

```yaml
discord:
  bot-token: "YOUR_BOT_TOKEN_HERE"

guilds:
  "GUILD_ID_HERE":
    connectors:
      "Discord Role Name":
        group: "luckperms-group-name"
        direction: "BOTH"

linking:
  verify-code-timeout: 300
  store: "json"

settings:
  sync-on-join: true
  sync-on-bot-start: false
  remove-group-on-role-remove: true
  remove-role-on-group-remove: true
  log-sync-actions: true
```

### Settings Reference

| Setting | Default | Description |
|---------|---------|-------------|
| `discord.bot-token` | `YOUR_BOT_TOKEN_HERE` | Discord bot token |
| `guilds` | `{}` | Map of guild IDs to connector configs |
| `connectors.<role>.group` | — | LuckPerms group name |
| `connectors.<role>.direction` | `BOTH` | Sync direction |
| `linking.verify-code-timeout` | `300` | Seconds before verification code expires |
| `linking.store` | `json` | Storage method: `json` or `sqlite` |
| `settings.sync-on-join` | `true` | Sync MC groups → Discord when player joins |
| `settings.sync-on-bot-start` | `false` | Sync all Discord roles → MC groups on bot start |
| `settings.remove-group-on-role-remove` | `true` | Remove LP group when Discord role is removed |
| `settings.remove-role-on-group-remove` | `true` | Remove Discord role when LP group is removed |
| `settings.log-sync-actions` | `true` | Log all sync operations to console |

---

## Troubleshooting

### Bot doesn't connect

- Check the bot token is correct
- Ensure the bot has `Server Members Intent` enabled in Discord Developer Portal
- Check the bot is invited to the server with correct permissions
- Look for errors in `logs/latest.log`

### Roles not syncing

- Make sure accounts are linked (`/rc status` shows linked count)
- Verify connector entries match Discord role names **exactly** (case-sensitive)
- Check the bot's role is higher than the roles it needs to assign
- Check `log-sync-actions: true` and look for `[Sync]` messages in the log
- Run `/rc reload` after config changes

### Infinite loop protection

The plugin uses **additive-only** sync from events: it adds missing roles/groups but doesn't remove them during event-driven sync (removal only happens on explicit role/group removal). This prevents feedback loops where:
```
Discord role added → LP group added → Discord role added → ...
```

### Player not found in Discord

- Ensure the player has linked their account (`/link` in-game)
- For initial setup, use `/rc link <discord-id> <player>` to force-link
- The bot must have the `Server Members Intent` to see members

---

## File Structure

```
plugins/RoleConnectors/
├── config.yml                    # Main configuration
└── linked-accounts.json          # Discord-to-Minecraft account links
```

---
