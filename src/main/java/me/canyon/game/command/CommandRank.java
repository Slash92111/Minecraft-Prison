package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.game.player.PlayerData;
import me.canyon.game.player.rank.Rank;
import me.canyon.game.player.rank.Staff;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;

public class CommandRank extends BaseCommand {

    private int staffLevel;

    public CommandRank(String command, String usage, String description, int staffLevel) {
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

        if (args.length < 4) {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/rank <set> <player> <type> <rank>")));
            return false;
        }

        Player target = Bukkit.getPlayer(args[1]);

        MongoDB mongoDB = plugin.getMongoDBInstance();

        if (args[0].toLowerCase().matches("set")) {
            Document document = mongoDB.getDocument("IGN", args[1]);

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

                plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=" + cmd.getName().toLowerCase() + " " + String.join(" ", Arrays.asList(args)) + "| Target=" + uuid.toString());


                PlayerData targetData = plugin.getPlayerData(uuid);

                String type = args[2].toLowerCase();

                if (type.matches("staff")) {
                    Staff staff = Staff.valueOf(args[3].toUpperCase());
                    targetData.setStaff(staff);
                } else if (type.matches("donator")) {
                    Rank rank = Rank.valueOf(args[3].toUpperCase());
                    targetData.setRank(rank);
                }

                player.sendMessage(plugin.getMessageFromConfig("rank.set", Map.of("%player_name%", args[1], "%type%", type, "%rank%", args[3])));
            }
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

        if (cmd.getName().toLowerCase().matches("rank")) {
            if (args.length <= 1)
                return Collections.singletonList("set");
            else if (args.length == 2) {
                List<String> players = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
                return players;
            } else if (args.length == 3) {
                List<String> types = new ArrayList<>();
                types.add("donator");
                types.add("staff");
                return types;
            } else if (args.length == 4 && args[2].toLowerCase().matches("donator")) {
                    List<String> ranks = new ArrayList<>();
                    for (Rank rank : Rank.values())
                        ranks.add(rank.toString());
                    return ranks;
            } else if (args.length == 4 && args[2].toLowerCase().matches("staff")) {
                List<String> staffs = new ArrayList<>();
                for (Staff staff : Staff.values())
                    staffs.add(staff.toString());
                return staffs;
            }
        }

        return null;
    }
}
