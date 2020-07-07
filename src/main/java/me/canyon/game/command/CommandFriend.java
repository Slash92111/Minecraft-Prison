package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.game.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandFriend extends BaseCommand {

    private Main plugin = Main.getInstance();

    public CommandFriend(String command, String usage, String description) {
        super(command, usage, description);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        if (args.length < 2)
            return false;

        //friend add/remove <name>

        Player player = (Player) sender;
        PlayerData playerData = plugin.getPlayerData(player);

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("add")) {
            Player friend = Bukkit.getPlayer(args[1]);

            try {
                if (friend.isOnline()) {
                    if (playerData.getPendingFriends().contains(friend)) {
                        playerData.removePendingFriend(friend);
                        playerData.addFriend(friend);
                        plugin.getPlayerData(friend).addFriend(player);
                        friend.sendMessage(plugin.getMessageFromConfig("friend.accepted", Map.of("%player_name%", player.getName())));
                        player.sendMessage(plugin.getMessageFromConfig("friend.accepted", Map.of("%player_name%", friend.getName())));
                    } else {
                        plugin.getPlayerData(friend).addPendingFriend(player);
                        friend.sendMessage(plugin.getMessageFromConfig("friend.pending", Map.of("%player_name%", player.getName())));
                        player.sendMessage(plugin.getMessageFromConfig("friend.sent", Map.of("%player_name%", friend.getName())));
                    }
                }
            } catch (NullPointerException ex) {
                player.sendMessage(plugin.getMessageFromConfig("player.playerIsntOnline", Map.of("%player_name%", args[1])));
            }
        } else if (subCommand.equals("remove")) {
            String friend = args[1];

            if (playerData.getFriends().containsValue(friend)) {
                HashMap<String, String> friends = playerData.getFriends();

                for (String key : friends.keySet()) {
                    if (friends.get(key).equals(friend)) {
                        friends.remove(key);
                        player.sendMessage(plugin.getMessageFromConfig("friend.removed", Map.of("%player_name%", friend)));

                        plugin.getPlayerData(UUID.fromString(key)).removeFriend(player);
                    }
                }
            }
        } else if (subCommand.equals("decline")) {
            String friend = args[1];


        } else {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/friend <add/remove> <name>")));
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return null;

        Player cmdSender = (Player) sender;

        if (cmd.getName().toLowerCase().matches("friend")) {
            if (args.length <= 1) {
                return Arrays.asList("add", "remove");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("add")) {
                    List<String> players = new ArrayList<>();
                    Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
                    return players;
                } else if (args[0].equalsIgnoreCase("remove")) {
                    return new ArrayList<>(plugin.getPlayerData(cmdSender).getFriends().values());
                }
            }
        }

        return null;
    }
}
