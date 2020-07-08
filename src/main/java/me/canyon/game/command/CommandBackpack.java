package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.game.player.PlayerData;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandBackpack extends BaseCommand {

    private int staffLevel;

    public CommandBackpack(String command, String usage, String description, int staffLevel) {
        super(command, usage, description);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length > 0) {
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

                targetData.getBackpack().open(1, player);

                plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=backpack | Target=" + uuid.toString());

            } else {
                player.sendMessage(plugin.getMessageFromConfig("other.playerDoesntExist", Map.of("%player_name%", args[0])));
                return false;
            }
        } else
            plugin.getPlayerData(player).getBackpack().open(1, player);


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (! (sender instanceof Player))
            return null;

        Player cmdSender = (Player) sender;

        if (!(plugin.getPlayerData(cmdSender).getStaff().getOrder() >= staffLevel))
            return null;

        if (cmd.getName().toLowerCase().matches("backpack")) {
            if (args.length <= 1) {
                List<String> players = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
                return players;
            }
        }

        return null;
    }
}
