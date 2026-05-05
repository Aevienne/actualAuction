package me.angelique.actualAuction.command;

import me.angelique.actualAuction.gui.AuctionGuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AuctionCommand implements CommandExecutor {
    private final AuctionGuiManager guiManager;

    public AuctionCommand(AuctionGuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        guiManager.openMain(player);
        return true;
    }
}
