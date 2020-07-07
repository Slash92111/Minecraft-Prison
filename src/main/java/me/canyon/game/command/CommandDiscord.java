package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class CommandDiscord extends BaseCommand {

    public CommandDiscord(String command, String usage, String description) {
        super(command, usage, description);
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", usage)));
            return false;
        }

        PlayerData playerData = plugin.getPlayerData(player);

        if (!playerData.getDiscordID().equals("")) {
            player.sendMessage(plugin.getMessageFromConfig("discord.alreadyLinked"));
            return false;
        }

        if (playerData.checkDiscordCode(Integer.parseInt(args[0])))
            player.sendMessage(plugin.getMessageFromConfig("discord.successfullyLinked"));
        else
            player.sendMessage(plugin.getMessageFromConfig("discord.wrongCode"));
        return true;
    }

}
