package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.game.player.PlayerData;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandUpload extends BaseCommand {
    private int staffLevel;

    public CommandUpload(String command, String usage, String description, int staffLevel) {
        super(command, usage, description);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length == 1) { //Uploading another players data

            if (!(plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel))
                return false;

            Player target = Bukkit.getPlayer(args[0]);

            MongoDB mongoDB = plugin.getMongoDBInstance();
            Document document = mongoDB.getDocument("IGN", args[0]);

            if (document != null) {
                UUID uuid;

                if (target != null) {
                    if (!plugin.hasPlayerData(target))
                        plugin.setPlayerData(target, true);

                    uuid = target.getUniqueId();
                } else {
                    uuid = UUID.fromString(document.getString("UUID"));
                    if (!plugin.hasPlayerData(uuid))
                        plugin.setPlayerData(uuid, true);
                }

                PlayerData targetData = plugin.getPlayerData(uuid);
                targetData.upload();

                plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=" + cmd.getName().toLowerCase() + "| Target=" + uuid.toString());


                player.sendMessage(plugin.getMessageFromConfig("player.uploadedAnotherPlayersData", Map.of("%player_name%", args[0])));
            } else {
                player.sendMessage(plugin.getMessageFromConfig("player.playerDoesntExist", Map.of("%player_name%", args[0])));
                return false;
            }
        } else if (args.length == 0) { //Upload their own data
            if (!plugin.hasPlayerData(player))
                plugin.setPlayerData(player);

            PlayerData playerData = plugin.getPlayerData(player);

            playerData.upload();

            player.sendMessage(plugin.getMessageFromConfig("player.uploadData"));
        } else {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/upload <player>")));
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().toLowerCase().matches("upload")) {
            if (! (sender instanceof Player))
                return null;

            Player cmdSender = (Player) sender;

            if (!(plugin.getPlayerData(cmdSender).getStaff().getOrder() >= staffLevel))
                return null;

            if (args.length <= 1) {
                List<String> players = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
                return players;
            }
        }

        return null;
    }
}
