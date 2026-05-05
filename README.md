# angelNCore

A Minecraft 1.21 plugin that adds a **supply and demand economy** to your server. Item prices shift dynamically based on player activity — buying drives prices up, selling drives them down, and prices slowly recover over time.

---

## Features

- Dynamic pricing driven by real player transactions
- Persistent economy data stored in SQLite
- Buy and sell items through a simple shop command
- Prices decay back toward base value over time (self-correcting market)
- Admin commands for managing player balances
- Fully configurable items, prices, and market behavior via `config.yml`

---

## Commands

| Command | Description |
|---|---|
| `/shop list` | View all items and their current buy/sell prices |
| `/shop buy <item> [amount]` | Buy items from the shop |
| `/shop sell <item> [amount]` | Sell items from your inventory |
| `/balance` | Check your own balance |
| `/balance <player>` | Check another player's balance |
| `/eco give <player> <amount>` | Give a player money (admin) |
| `/eco take <player> <amount>` | Take money from a player (admin) |
| `/eco set <player> <amount>` | Set a player's balance (admin) |

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `economy.use` | Everyone | Access to `/shop` and `/balance` |
| `economy.admin` | OP | Access to `/eco` admin commands |

---

## How the Economy Works

Every buy and sell transaction affects the market price:

- **Buying** increases the price by 5% per item purchased
- **Selling** decreases the price by 5% per item sold
- Prices are capped between **20%** and **500%** of the base price
- Every 60 seconds, all prices nudge back toward their base value

This means items that are heavily traded will fluctuate, and players can influence the market — or exploit it.

---

## Configuration

`config.yml` is generated on first run. Key settings:

```yaml
economy:
  starting-balance: 500.0
  currency-symbol: "$"

shop:
  price-change-rate: 0.05       # % price change per item transacted
  min-price-multiplier: 0.2     # floor: 20% of base price
  max-price-multiplier: 5.0     # ceiling: 500% of base price
  price-decay-rate: 0.01        # how fast prices return to base
  price-decay-interval: 60      # seconds between decay ticks
```

To add a new item, add an entry under `items:`:

```yaml
items:
  OBSIDIAN:
    base-price: 20.0
    display-name: "Obsidian"
```

The item key must match a valid Bukkit `Material` name.

---

## Installation

1. Drop the compiled `.jar` into your server's `plugins/` folder
2. Start or reload your server
3. Edit `plugins/angelNCore/config.yml` to customize items and settings
4. Reload with `/reload confirm` or restart the server

---

## Building

Requires Java 21 and Gradle.

```bash
./gradlew jar
```

Output jar will be in `build/libs/`.

---

## Dependencies

- [Spigot API 1.21](https://hub.spigotmc.org) — provided at runtime, not shaded
- SQLite JDBC — bundled with Spigot, no extra dependencies needed
