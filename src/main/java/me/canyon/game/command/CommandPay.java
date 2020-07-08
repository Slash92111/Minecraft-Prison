package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import me.canyon.util.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class CommandPay extends BaseCommand {

    public CommandPay(String command, String usage, String description) { super(command, usage, description); }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", usage)));
            return false;
        }

        PlayerData playerData = plugin.getPlayerData(player);

        if (Bukkit.getPlayer(args[0]) == null) {
            player.sendMessage(plugin.getMessageFromConfig("other.playerIsntOnline", Map.of("%player_name%", args[0])));
            return false;
        }

        PlayerData otherData = plugin.getPlayerData(Bukkit.getPlayer(args[0]));

        long amount = Long.parseLong(args[1]);

        if (amount > playerData.getBalance()) {
            player.sendMessage(plugin.getMessageFromConfig("player.notEnoughMoneyPay"));
            return false;
        }

        playerData.setBalance(playerData.getBalance() - amount);
        otherData.setBalance(otherData.getBalance() + amount);

        player.sendMessage(plugin.getMessageFromConfig("player.sentMoney", Map.of("%amount%", new Utilities().formatEconomy(amount), "%player_name%", args[0])));
        otherData.getPlayer().sendMessage(plugin.getMessageFromConfig("player.receivedMoney", Map.of("%amount%", new Utilities().formatEconomy(amount), "%player_name%", player.getName())));

        return true;
    }
}
