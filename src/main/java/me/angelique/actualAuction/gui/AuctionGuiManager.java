package me.angelique.actualAuction.gui;

import me.angelique.actualAuction.ActualAuction;
import me.angelique.actualAuction.config.AuctionSettings;
import me.angelique.actualAuction.input.ChatInputService;
import me.angelique.actualAuction.input.PendingPrompt;
import me.angelique.actualAuction.input.PromptType;
import me.angelique.actualAuction.model.AuctionEntry;
import me.angelique.actualAuction.service.AuctionService;
import me.angelique.actualAuction.util.GuiKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AuctionGuiManager {
    private final ActualAuction plugin;
    private final AuctionService auctionService;
    private final ChatInputService chatInputService;
    private final AuctionSettings settings;
    private final NamespacedKey auctionIdKey;
    private final NamespacedKey actionKey;
    private final Map<UUID, ItemStack> listingItemBuffer = new ConcurrentHashMap<>();
    private final Map<UUID, Double> listingPriceBuffer = new ConcurrentHashMap<>();

    public AuctionGuiManager(ActualAuction plugin, AuctionService auctionService, ChatInputService chatInputService, AuctionSettings settings) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.chatInputService = chatInputService;
        this.settings = settings;
        this.auctionIdKey = new NamespacedKey(plugin, "auction_id");
        this.actionKey = new NamespacedKey(plugin, "gui_action");
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(player, settings.getBrowseSize(), GuiKeys.MAIN_TITLE);
        List<AuctionEntry> auctions = auctionService.getActiveAuctions();
        int slot = 0;
        for (AuctionEntry entry : auctions) {
            if (slot >= 45) break;
            ItemStack display = entry.getItemStack();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(
                        "§7Seller: §f" + entry.getSellerName(),
                        "§7Start: §f$" + auctionService.formatMoney(entry.getStartingPrice()),
                        "§7Current: §a$" + auctionService.formatMoney(entry.getCurrentBid()),
                        "§7Highest: §f" + (entry.getHighestBidderName() == null ? "None" : entry.getHighestBidderName()),
                        "§7Time left: §e" + auctionService.formatRemaining(entry),
                        "§8Click to open bidding view"
                ));
                meta.getPersistentDataContainer().set(auctionIdKey, PersistentDataType.STRING, entry.getId().toString());
                display.setItemMeta(meta);
            }
            inventory.setItem(slot++, display);
        }
        inventory.setItem(49, actionItem(Material.EMERALD, "§aCreate Auction", "create"));
        player.openInventory(inventory);
    }

    public void openAuctionDetail(Player player, AuctionEntry entry) {
        Inventory inventory = Bukkit.createInventory(player, 27, GuiKeys.DETAIL_TITLE);
        ItemStack item = entry.getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(
                    "§7Seller: §f" + entry.getSellerName(),
                    "§7Start price: §f$" + auctionService.formatMoney(entry.getStartingPrice()),
                    "§7Current bid: §a$" + auctionService.formatMoney(entry.getCurrentBid()),
                    "§7Highest bidder: §f" + (entry.getHighestBidderName() == null ? "None" : entry.getHighestBidderName()),
                    "§7Time left: §e" + auctionService.formatRemaining(entry)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        double increment = auctionService.getSettings().getMinBidIncrement();
        inventory.setItem(13, item);
        inventory.setItem(10, bidItem(entry.getId(), entry.getCurrentBid() + increment, "§aBid +min"));
        inventory.setItem(12, bidItem(entry.getId(), entry.getCurrentBid() + (increment * 5), "§aBid +x5"));
        inventory.setItem(14, bidItem(entry.getId(), entry.getCurrentBid() + (increment * 10), "§aBid +x10"));
        inventory.setItem(16, customBidItem(entry.getId()));
        inventory.setItem(22, actionItem(Material.BARRIER, "§cBack", "back"));
        player.openInventory(inventory);
    }

    public void beginCreateAuction(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage("§cHold the item you want to auction in your main hand.");
            return;
        }
        listingItemBuffer.put(player.getUniqueId(), hand.clone());
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        chatInputService.setPrompt(player.getUniqueId(), new PendingPrompt(PromptType.CREATE_STARTING_PRICE, null, 0.0D));
        player.closeInventory();
        player.sendMessage("§eType the starting price in chat. Type cancel to abort.");
    }

    public void handleCreateStartingPrice(Player player, String message) {
        double price;
        try {
            price = Double.parseDouble(message);
        } catch (NumberFormatException exception) {
            player.sendMessage("§cInvalid number. Enter a valid starting price.");
            return;
        }
        if (price < settings.getMinStartingPrice()) {
            player.sendMessage("§cMinimum starting price is $" + auctionService.formatMoney(settings.getMinStartingPrice()));
            return;
        }
        listingPriceBuffer.put(player.getUniqueId(), price);
        chatInputService.setPrompt(player.getUniqueId(), new PendingPrompt(PromptType.CREATE_DURATION, null, price));
        player.sendMessage("§eType duration in seconds. Default is " + settings.getDefaultDurationSeconds() + ". Type cancel to abort.");
    }

    public void handleCreateDuration(Player player, String message) {
        long duration;
        try {
            duration = Long.parseLong(message);
        } catch (NumberFormatException exception) {
            player.sendMessage("§cInvalid number. Enter duration in seconds.");
            return;
        }
        if (duration <= 0L) {
            player.sendMessage("§cDuration must be above 0.");
            return;
        }
        ItemStack item = listingItemBuffer.remove(player.getUniqueId());
        Double price = listingPriceBuffer.remove(player.getUniqueId());
        chatInputService.removePrompt(player.getUniqueId());
        if (item == null || price == null) {
            player.sendMessage("§cAuction creation state was lost. Try again.");
            return;
        }
        AuctionEntry entry = auctionService.createAuction(player, item, price, duration);
        player.sendMessage("§aAuction created for $" + auctionService.formatMoney(price) + " lasting " + duration + " seconds.");
        openAuctionDetail(player, entry);
    }

    public void beginCustomBid(Player player, UUID auctionId) {
        chatInputService.setPrompt(player.getUniqueId(), new PendingPrompt(PromptType.CUSTOM_BID, auctionId, 0.0D));
        player.closeInventory();
        player.sendMessage("§eType your bid amount in chat. Type cancel to abort.");
    }

    public void handleCustomBid(Player player, UUID auctionId, String message) {
        double amount;
        try {
            amount = Double.parseDouble(message);
        } catch (NumberFormatException exception) {
            player.sendMessage("§cInvalid number. Enter a valid bid amount.");
            return;
        }
        chatInputService.removePrompt(player.getUniqueId());
        var result = auctionService.placeBid(player, auctionId, amount);
        player.sendMessage((result.success() ? "§a" : "§c") + result.message());
        AuctionEntry entry = auctionService.getAuction(auctionId);
        if (entry != null && !entry.isClosed()) {
            openAuctionDetail(player, entry);
        }
    }

    private ItemStack bidItem(UUID auctionId, double amount, String name) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + " §7($" + auctionService.formatMoney(amount) + ")");
            meta.getPersistentDataContainer().set(auctionIdKey, PersistentDataType.STRING, auctionId.toString());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "bid:" + amount);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack customBidItem(UUID auctionId) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bCustom Bid");
            meta.setLore(List.of("§7Enter your own bid amount in chat."));
            meta.getPersistentDataContainer().set(auctionIdKey, PersistentDataType.STRING, auctionId.toString());
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "custom_bid");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack actionItem(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void cancelPrompt(Player player) {
        chatInputService.removePrompt(player.getUniqueId());
        ItemStack bufferedItem = listingItemBuffer.remove(player.getUniqueId());
        listingPriceBuffer.remove(player.getUniqueId());
        if (bufferedItem != null) {
            player.getInventory().addItem(bufferedItem);
        }
        player.sendMessage("§eAuction input cancelled.");
        openMain(player);
    }

    public NamespacedKey getAuctionIdKey() { return auctionIdKey; }
    public NamespacedKey getActionKey() { return actionKey; }
    public AuctionService getAuctionService() { return auctionService; }
}
