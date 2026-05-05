package me.angelique.actualAuction.db;

import me.angelique.actualAuction.model.AuctionEntry;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            Path dbPath = plugin.getDataFolder().toPath().resolve("auctions.db");
            plugin.getDataFolder().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS auctions (
                            id TEXT PRIMARY KEY,
                            seller_id TEXT NOT NULL,
                            seller_name TEXT NOT NULL,
                            item_data TEXT NOT NULL,
                            starting_price REAL NOT NULL,
                            created_at INTEGER NOT NULL,
                            end_time INTEGER NOT NULL,
                            highest_bidder TEXT,
                            highest_bidder_name TEXT,
                            current_bid REAL NOT NULL,
                            closed INTEGER NOT NULL
                        )
                        """);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize database", exception);
        }
    }

    public List<AuctionEntry> loadAuctions() {
        List<AuctionEntry> auctions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM auctions")) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                UUID highestBidder = rs.getString("highest_bidder") == null ? null : UUID.fromString(rs.getString("highest_bidder"));
                auctions.add(new AuctionEntry(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("seller_id")),
                        rs.getString("seller_name"),
                        deserializeItem(rs.getString("item_data")),
                        rs.getDouble("starting_price"),
                        rs.getLong("created_at"),
                        rs.getLong("end_time"),
                        highestBidder,
                        rs.getString("highest_bidder_name"),
                        rs.getDouble("current_bid"),
                        rs.getInt("closed") == 1
                ));
            }
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to load auctions: " + exception.getMessage());
        }
        return auctions;
    }

    public void saveAuction(AuctionEntry entry) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO auctions (id, seller_id, seller_name, item_data, starting_price, created_at, end_time, highest_bidder, highest_bidder_name, current_bid, closed)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    seller_id=excluded.seller_id,
                    seller_name=excluded.seller_name,
                    item_data=excluded.item_data,
                    starting_price=excluded.starting_price,
                    created_at=excluded.created_at,
                    end_time=excluded.end_time,
                    highest_bidder=excluded.highest_bidder,
                    highest_bidder_name=excluded.highest_bidder_name,
                    current_bid=excluded.current_bid,
                    closed=excluded.closed
                """)) {
            statement.setString(1, entry.getId().toString());
            statement.setString(2, entry.getSellerId().toString());
            statement.setString(3, entry.getSellerName());
            statement.setString(4, serializeItem(entry.getItemStack()));
            statement.setDouble(5, entry.getStartingPrice());
            statement.setLong(6, entry.getCreatedAtMillis());
            statement.setLong(7, entry.getEndTimeMillis());
            statement.setString(8, entry.getHighestBidder() == null ? null : entry.getHighestBidder().toString());
            statement.setString(9, entry.getHighestBidderName());
            statement.setDouble(10, entry.getCurrentBid());
            statement.setInt(11, entry.isClosed() ? 1 : 0);
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to save auction: " + exception.getMessage());
        }
    }

    private String serializeItem(ItemStack itemStack) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(itemStack);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    private ItemStack deserializeItem(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            Object object = dataInput.readObject();
            return object instanceof ItemStack itemStack ? itemStack : new ItemStack(org.bukkit.Material.STONE);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
