# actualAuction

A Minecraft 1.21 Paper plugin that adds a **true auction house** to your server. Players can list items with a starting price and auction duration, other players can place competing bids, and the highest bidder wins when the timer ends.

---

## Features

- Chest GUI auction house for browsing active listings
- Real bidding system with starting price and highest-bid tracking
- Timed auctions with automatic expiration handling
- Custom bid entry and quick bid buttons
- Vault economy integration for real balance handling
- SQLite persistence for saved auction data across restarts
- Seller payout and bidder refund handling when auctions update or end

---

## Commands

| Command | Description |
|---|---|
| `/auction` | Open the auction house GUI |
| `/ah` | Alias for `/auction` |

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `actualauction.use` | Everyone | Access to the auction house |
| `actualauction.admin` | OP | Reserved for admin features |

---

## How the Auction Works

Every auction follows a normal bidding flow:

- A player lists the item they are holding
- The player enters a **starting price**
- The player enters an **auction duration**
- Other players bid against each other through the GUI
- The highest valid bidder wins when the timer expires
- The seller receives the final sale amount

If a player is outbid, their previously held bid amount is returned. If no one bids, the listed item is returned to the seller.

---

## Configuration

`config.yml` is generated on first run. Key settings:

```yaml
auction:
  default-duration-seconds: 300
  min-starting-price: 1.0
  min-bid-increment: 1.0
  browse-size: 54
```

### Setting meanings

- `default-duration-seconds` - Default auction duration
- `min-starting-price` - Lowest allowed starting price
- `min-bid-increment` - Minimum amount required above the current bid
- `browse-size` - Inventory size used for the auction browser GUI

---

## Installation

1. Drop the compiled `.jar` into your server's `plugins/` folder
2. Install **Vault**
3. Install a Vault-compatible economy plugin such as **EssentialsX**
4. Start or restart your server
5. Edit `plugins/actualAuction/config.yml` if needed
6. Restart the server after changes

---

## Building

Requires Java 21 and Gradle.

```bash
./gradlew jar
```

Output jar will be in `build/libs/`.

---

## Dependencies

- [Paper API 1.21](https://docs.papermc.io/) — used for plugin development and provided by the server at runtime
- [Vault](https://dev.bukkit.org/projects/vault) — required for economy integration
- A Vault-compatible economy plugin such as [EssentialsX](https://essentialsx.net/downloads) — required for player balances
- SQLite JDBC — bundled in the plugin jar for local auction data storage