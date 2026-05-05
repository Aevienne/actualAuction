package me.angelique.actualAuction.listener;

import me.angelique.actualAuction.gui.AuctionGuiManager;
import me.angelique.actualAuction.input.ChatInputService;
import me.angelique.actualAuction.model.AuctionEntry;
import me.angelique.actualAuction.util.GuiKeys;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class AuctionListener implements Listener {
    private final AuctionGuiManager guiManager;
    private final ChatInputService chatInputService;

    public AuctionListener(AuctionGuiManager guiManager, ChatInputService chatInputService) {
        this.guiManager = guiManager;
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!GuiKeys.MAIN_TITLE.equals(title) && !GuiKeys.DETAIL_TITLE.equals(title)) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        String action = meta.getPersistentDataContainer().get(guiManager.getActionKey(), PersistentDataType.STRING);
        String auctionIdRaw = meta.getPersistentDataContainer().get(guiManager.getAuctionIdKey(), PersistentDataType.STRING);

        if ("create".equals(action)) {
            guiManager.beginCreateAuction(player);
            return;
        }
        if ("back".equals(action)) {
            guiManager.openMain(player);
            return;
        }
        if ("custom_bid".equals(action) && auctionIdRaw != null) {
            guiManager.beginCustomBid(player, UUID.fromString(auctionIdRaw));
            return;
        }
        if (action != null && action.startsWith("bid:") && auctionIdRaw != null) {
            double amount = Double.parseDouble(action.substring(4));
            var result = guiManager.getAuctionService().placeBid(player, UUID.fromString(auctionIdRaw), amount);
            player.sendMessage((result.success() ? "§a" : "§c") + result.message());
            AuctionEntry entry = guiManager.getAuctionService().getAuction(UUID.fromString(auctionIdRaw));
            if (entry != null && !entry.isClosed()) {
                guiManager.openAuctionDetail(player, entry);
            } else {
                guiManager.openMain(player);
            }
            return;
        }
        if (auctionIdRaw != null) {
            AuctionEntry entry = guiManager.getAuctionService().getAuction(UUID.fromString(auctionIdRaw));
            if (entry != null && !entry.isClosed()) {
                guiManager.openAuctionDetail(player, entry);
            }
        }
    }
}
