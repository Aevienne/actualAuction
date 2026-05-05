package me.angelique.actualAuction;

import me.angelique.actualAuction.command.AuctionCommand;
import me.angelique.actualAuction.config.AuctionSettings;
import me.angelique.actualAuction.db.DatabaseManager;
import me.angelique.actualAuction.economy.VaultEconomyHook;
import me.angelique.actualAuction.gui.AuctionGuiManager;
import me.angelique.actualAuction.input.ChatInputService;
import me.angelique.actualAuction.listener.AuctionListener;
import me.angelique.actualAuction.listener.ChatInputListener;
import me.angelique.actualAuction.service.AuctionService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ActualAuction extends JavaPlugin {
    private AuctionSettings settings;
    private DatabaseManager databaseManager;
    private VaultEconomyHook economyHook;
    private AuctionService auctionService;
    private AuctionGuiManager guiManager;
    private ChatInputService chatInputService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new AuctionSettings(this);
        this.databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        this.economyHook = new VaultEconomyHook();
        if (!economyHook.setup(this)) {
            getLogger().severe("Vault economy provider not found. Disable plugin until Vault + economy plugin are installed.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.auctionService = new AuctionService(this, settings, databaseManager, economyHook);
        auctionService.loadActiveAuctions();
        this.chatInputService = new ChatInputService();
        this.guiManager = new AuctionGuiManager(this, auctionService, chatInputService, settings);

        Objects.requireNonNull(getCommand("auction"), "auction command not defined")
                .setExecutor(new AuctionCommand(guiManager));

        Bukkit.getPluginManager().registerEvents(new AuctionListener(guiManager, chatInputService), this);
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(this, guiManager, chatInputService), this);
        auctionService.startTicker();
        getLogger().info("actualAuction enabled.");
    }

    @Override
    public void onDisable() {
        if (auctionService != null) {
            auctionService.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
