package me.angelique.actualAuction.config;

import org.bukkit.plugin.java.JavaPlugin;

public final class AuctionSettings {
    private final double minStartingPrice;
    private final double minBidIncrement;
    private final long defaultDurationSeconds;
    private final int browseSize;

    public AuctionSettings(JavaPlugin plugin) {
        this.minStartingPrice = plugin.getConfig().getDouble("auction.min-starting-price", 1.0D);
        this.minBidIncrement = plugin.getConfig().getDouble("auction.min-bid-increment", 1.0D);
        this.defaultDurationSeconds = plugin.getConfig().getLong("auction.default-duration-seconds", 300L);
        this.browseSize = plugin.getConfig().getInt("auction.browse-size", 54);
    }

    public double getMinStartingPrice() { return minStartingPrice; }
    public double getMinBidIncrement() { return minBidIncrement; }
    public long getDefaultDurationSeconds() { return defaultDurationSeconds; }
    public int getBrowseSize() { return browseSize; }
}
