package me.angelique.actualAuction.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuctionEntry {
    private final UUID id;
    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack itemStack;
    private final double startingPrice;
    private final long createdAtMillis;
    private long endTimeMillis;
    private UUID highestBidder;
    private String highestBidderName;
    private double currentBid;
    private boolean closed;

    public AuctionEntry(UUID id, UUID sellerId, String sellerName, ItemStack itemStack, double startingPrice, long createdAtMillis, long endTimeMillis, UUID highestBidder, String highestBidderName, double currentBid, boolean closed) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.startingPrice = startingPrice;
        this.createdAtMillis = createdAtMillis;
        this.endTimeMillis = endTimeMillis;
        this.highestBidder = highestBidder;
        this.highestBidderName = highestBidderName;
        this.currentBid = currentBid;
        this.closed = closed;
    }

    public UUID getId() { return id; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItemStack() { return itemStack.clone(); }
    public double getStartingPrice() { return startingPrice; }
    public long getCreatedAtMillis() { return createdAtMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public UUID getHighestBidder() { return highestBidder; }
    public String getHighestBidderName() { return highestBidderName; }
    public double getCurrentBid() { return currentBid; }
    public boolean isClosed() { return closed; }

    public long getRemainingMillis() {
        return Math.max(0L, endTimeMillis - System.currentTimeMillis());
    }

    public void extendBySeconds(long seconds) {
        this.endTimeMillis += seconds * 1000L;
    }

    public synchronized boolean placeBid(UUID bidderId, String bidderName, double amount) {
        if (closed || System.currentTimeMillis() >= endTimeMillis) {
            return false;
        }
        if (amount <= currentBid) {
            return false;
        }
        this.highestBidder = bidderId;
        this.highestBidderName = bidderName;
        this.currentBid = amount;
        return true;
    }

    public synchronized void close() {
        this.closed = true;
    }
}
