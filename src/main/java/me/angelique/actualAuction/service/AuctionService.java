package me.angelique.actualAuction.service;

import me.angelique.actualAuction.ActualAuction;
import me.angelique.actualAuction.config.AuctionSettings;
import me.angelique.actualAuction.db.DatabaseManager;
import me.angelique.actualAuction.economy.VaultEconomyHook;
import me.angelique.actualAuction.model.AuctionEntry;
import me.angelique.angelNCore.events.AuctionSaleEvent;
import me.angelique.angelNCore.events.EventBus;
import me.angelique.angelNCore.services.MarketService;
import me.angelique.angelNCore.services.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AuctionService {
    private static final DecimalFormat MONEY = new DecimalFormat("0.00");
    private final ActualAuction plugin;
    private final AuctionSettings settings;
    private final DatabaseManager databaseManager;
    private final VaultEconomyHook economyHook;
    private final Map<UUID, AuctionEntry> auctions = new ConcurrentHashMap<>();
    private final Map<UUID, Double> heldFunds = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;

    public AuctionService(ActualAuction plugin, AuctionSettings settings, DatabaseManager databaseManager, VaultEconomyHook economyHook) {
        this.plugin = plugin;
        this.settings = settings;
        this.databaseManager = databaseManager;
        this.economyHook = economyHook;
    }

    public void loadActiveAuctions() {
        for (AuctionEntry entry : databaseManager.loadAuctions()) {
            auctions.put(entry.getId(), entry);
        }
    }

    public AuctionEntry createAuction(Player seller, ItemStack itemStack, double startingPrice, long durationSeconds) {
        long now = System.currentTimeMillis();
        AuctionEntry entry = new AuctionEntry(
                UUID.randomUUID(),
                seller.getUniqueId(),
                seller.getName(),
                itemStack.clone(),
                startingPrice,
                now,
                now + (durationSeconds * 1000L),
                null,
                null,
                startingPrice,
                false
        );
        auctions.put(entry.getId(), entry);
        databaseManager.saveAuction(entry);
        return entry;
    }

    public List<AuctionEntry> getActiveAuctions() {
        return auctions.values().stream()
                .filter(entry -> !entry.isClosed())
                .sorted(Comparator.comparingLong(AuctionEntry::getEndTimeMillis))
                .toList();
    }

    public AuctionEntry getAuction(UUID id) {
        return auctions.get(id);
    }

    public BidResult placeBid(Player bidder, UUID auctionId, double amount) {
        AuctionEntry entry = auctions.get(auctionId);
        if (entry == null || entry.isClosed()) {
            return BidResult.error("That auction is no longer available.");
        }
        if (entry.getSellerId().equals(bidder.getUniqueId())) {
            return BidResult.error("You cannot bid on your own auction.");
        }
        double minimumAllowed = entry.getCurrentBid() + settings.getMinBidIncrement();
        if (amount < minimumAllowed) {
            return BidResult.error("Minimum next bid is $" + formatMoney(minimumAllowed));
        }
        if (!economyHook.has(bidder, amount)) {
            return BidResult.error("You do not have enough money.");
        }
        if (!economyHook.withdraw(bidder, amount)) {
            return BidResult.error("Failed to hold your money. Try again.");
        }
        UUID previousBidder = entry.getHighestBidder();
        Double previousHeld = previousBidder == null ? null : heldFunds.get(previousBidder);
        if (!entry.placeBid(bidder.getUniqueId(), bidder.getName(), amount)) {
            economyHook.deposit(bidder, amount);
            return BidResult.error("Bid failed because the auction just changed.");
        }
        heldFunds.put(bidder.getUniqueId(), amount);
        if (previousBidder != null && previousHeld != null) {
            economyHook.deposit(Bukkit.getOfflinePlayer(previousBidder), previousHeld);
            heldFunds.remove(previousBidder);
            Player previousOnline = Bukkit.getPlayer(previousBidder);
            if (previousOnline != null) {
                previousOnline.sendMessage("§eYou were outbid on an auction. Your held funds were returned.");
            }
        }
        if (entry.getRemainingMillis() <= 30_000L) {
            entry.extendBySeconds(30L);
        }
        databaseManager.saveAuction(entry);
        Player seller = Bukkit.getPlayer(entry.getSellerId());
        if (seller != null) {
            seller.sendMessage("§e" + bidder.getName() + " bid $" + formatMoney(amount) + " on your item.");
        }
        return BidResult.success("Bid placed: $" + formatMoney(amount));
    }

    public void startTicker() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAuctions, 20L, 20L);
    }

    private void tickAuctions() {
        List<AuctionEntry> toClose = new ArrayList<>();
        for (AuctionEntry entry : auctions.values()) {
            if (!entry.isClosed() && entry.getRemainingMillis() <= 0L) {
                toClose.add(entry);
            }
        }
        for (AuctionEntry entry : toClose) {
            finishAuction(entry);
        }
    }

    private void finishAuction(AuctionEntry entry) {
        entry.close();
        databaseManager.saveAuction(entry);
        OfflinePlayer seller = Bukkit.getOfflinePlayer(entry.getSellerId());
        if (entry.getHighestBidder() == null) {
            Player sellerOnline = seller.getPlayer();
            if (sellerOnline != null) {
                sellerOnline.getInventory().addItem(entry.getItemStack());
                sellerOnline.sendMessage("§eYour auction ended with no bids. Item returned.");
            }
            return;
        }
        double amount = heldFunds.getOrDefault(entry.getHighestBidder(), entry.getCurrentBid());
        economyHook.deposit(seller, amount);
        heldFunds.remove(entry.getHighestBidder());
        Player winner = Bukkit.getPlayer(entry.getHighestBidder());
        if (winner != null) {
            winner.getInventory().addItem(entry.getItemStack());
            winner.sendMessage("§aYou won an auction for $" + formatMoney(amount) + ".");
        }
        Player sellerOnline = seller.getPlayer();
        if (sellerOnline != null) {
            sellerOnline.sendMessage("§aYour item sold to " + entry.getHighestBidderName() + " for $" + formatMoney(amount) + ".");
        }

        String itemType = entry.getItemStack().getType().name();
        int qty = entry.getItemStack().getAmount();
        EventBus.publish(new AuctionSaleEvent(
                entry.getId().toString(),
                entry.getSellerName(),
                entry.getHighestBidderName(),
                itemType,
                qty,
                amount
        ));
        MarketService market = ServiceRegistry.getMarketService();
        if (market != null) {
            market.recordTransaction(itemType, qty, amount / Math.max(1, qty));
        }
    }

    public String formatMoney(double value) {
        return MONEY.format(value);
    }

    public String formatRemaining(AuctionEntry entry) {
        long totalSeconds = entry.getRemainingMillis() / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + "m " + seconds + "s";
    }

    public AuctionSettings getSettings() {
        return settings;
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        for (AuctionEntry entry : auctions.values()) {
            databaseManager.saveAuction(entry);
        }
    }
}
