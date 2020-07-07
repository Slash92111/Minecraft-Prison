package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandHome extends BaseCommand{

    public CommandHome(String command, String usage, String description) {
        super(command, usage, description);
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        PlayerData playerData = plugin.getPlayerData(player);

        if (args.length > 1) {
            if (args[0].toLowerCase().matches("add")) {
                if (args[1].toLowerCase().matches("add|remove")) {
                    player.sendMessage(plugin.getMessageFromConfig("home.invalidName", Map.of("%home_name%", args[1])));
                    return false;
                }

                if (playerData.getTotalHomes() == playerData.getMaxHomes()) {
                    player.sendMessage(plugin.getMessageFromConfig("home.maxHomes", Map.of("%max_homes%", playerData.getMaxHomes() + "")));
                    return false;
                }

                playerData.addHome(args[1], player.getLocation());
                player.sendMessage(plugin.getMessageFromConfig("home.addedHome", Map.of("%home_name%", args[1], "%max_homes%", playerData.getMaxHomes() + "", "%total_homes%", playerData.getTotalHomes() + "")));

            } else if (args[0].toLowerCase().matches("remove")) {
                if (playerData.getHomes().containsKey(args[1])) {
                    playerData.removeHome(args[1]);
                    player.sendMessage(plugin.getMessageFromConfig("home.removedHome", Map.of("%home_name%", args[1], "%max_homes%", playerData.getMaxHomes() + "", "%total_homes%", playerData.getTotalHomes() + "")));

                } else {
                    player.sendMessage(plugin.getMessageFromConfig("home.doesntExist"));
                    return false;
                }
            } else {
                player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/home <add/remove> <name>")));
                return false;
            }
        } else if (args.length == 1) {
            if (playerData.getHome(args[0]) != null)
                player.teleport(playerData.getHome(args[0]));
            else {
                player.sendMessage(plugin.getMessageFromConfig("home.doesntExist"));
                return false;
            }
        } else {
            StringBuilder homes = new StringBuilder();

            for (String home : plugin.getPlayerData(player).getHomes().keySet()) {
                homes.append(home).append(" ");
            }

            player.sendMessage(plugin.getMessageFromConfig("home.list", Map.of("%total_homes%", playerData.getTotalHomes() + "", "%max_homes%", playerData.getMaxHomes() + "", "%homes%", homes.toString())));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return null;

        Player player = (Player) sender;

        PlayerData playerData = plugin.getPlayerData(player);

        if (cmd.getName().toLowerCase().matches("home")) {
            if (args.length <= 1) {
                List<String> subCommands = new ArrayList<>();

                if (playerData.getHomes().size() != 0)
                    subCommands.addAll(playerData.getHomes().keySet());

                subCommands.add("add");
                subCommands.add("remove");

                return subCommands;
            } else if (args.length == 2) {
                if (args[0].toLowerCase().matches("remove")) {
                    if (playerData.getHomes().size() != 0)
                        return new ArrayList<>(playerData.getHomes().keySet());
                }
            }
        }

        return null;
    }
}
