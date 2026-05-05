package me.angelique.actualAuction.listener;

import me.angelique.actualAuction.ActualAuction;
import me.angelique.actualAuction.gui.AuctionGuiManager;
import me.angelique.actualAuction.input.ChatInputService;
import me.angelique.actualAuction.input.PendingPrompt;
import me.angelique.actualAuction.input.PromptType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatInputListener implements Listener {
    private final ActualAuction plugin;
    private final AuctionGuiManager guiManager;
    private final ChatInputService chatInputService;

    public ChatInputListener(ActualAuction plugin, AuctionGuiManager guiManager, ChatInputService chatInputService) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingPrompt prompt = chatInputService.getPrompt(player.getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (message.equalsIgnoreCase("cancel")) {
                guiManager.cancelPrompt(player);
                return;
            }
            if (prompt.type() == PromptType.CREATE_STARTING_PRICE) {
                guiManager.handleCreateStartingPrice(player, message);
                return;
            }
            if (prompt.type() == PromptType.CREATE_DURATION) {
                guiManager.handleCreateDuration(player, message);
                return;
            }
            if (prompt.type() == PromptType.CUSTOM_BID && prompt.auctionId() != null) {
                guiManager.handleCustomBid(player, prompt.auctionId(), message);
            }
        });
    }
}
