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

public class CommandUnban extends BaseCommand {

    private int staffLevel;

    public CommandUnban(String command, String usage, String description, int staffLevel) {
        super(command, usage, description);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (!(plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel))
            return false;

        if (args.length < 2) {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/unban <player> <reason>")));
            return false;
        }

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

            if (targetData.getTempBan() != null) {
                String reason = String.join(" ", Arrays.asList(args));
                reason = reason.substring(args[0].length() + 1);

                plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=unban | Target=" + uuid.toString() + " | Reason=" + reason);

                plugin.getPlayerData(uuid).setTempBan(null, "", "");

                player.sendMessage(plugin.getMessageFromConfig("ban.unban", Map.of("%player_name%", args[0])));
            } else if (targetData.getPermBan()) {
                String reason = String.join(" ", Arrays.asList(args));
                reason = reason.substring(args[0].length() + 1);

                plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=unban | Target=" + uuid.toString() + " | Reason=" + reason);

                plugin.getPlayerData(uuid).setPermBan(false, "", "");

                player.sendMessage(plugin.getMessageFromConfig("ban.unban", Map.of("%player_name%", args[0])));
            } else{
                player.sendMessage(plugin.getMessageFromConfig("ban.notCurrentlyBanned", Map.of("%player_name%", args[0])));
                return false;
            }
        } else {
            player.sendMessage(plugin.getMessageFromConfig("player.playerDoesntExist", Map.of("%player_name%", args[0])));
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return null;

        Player cmdSender = (Player) sender;

        if (!(plugin.getPlayerData(cmdSender).getStaff().getOrder() >= staffLevel))
            return null;

        if (cmd.getName().toLowerCase().matches("unban") && args.length <= 1) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
            return players;
        }

        return null;
    }
}
