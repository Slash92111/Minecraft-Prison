package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.database.MongoDB;
import me.canyon.game.player.rank.Staff;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class CommandMute extends BaseCommand {
    private int staffLevel;

    public CommandMute(String command, String usage, String description, int staffLevel) {
        super(command, usage, description);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (!(plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel))
            return false;

        if (args.length < 4) {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/mute <player> <time> <second,minute,hour,week,month,year> <reason>")));
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

            if (plugin.getPlayerData(uuid).getStaff() != Staff.NONE) {
                player.sendMessage(plugin.getMessageFromConfig("mute.canNotMuteStaff"));
                return false;
            }

            try {
                Date expires = new Date(System.currentTimeMillis() + CommandMute.MuteUnit.getTicks(args[2].toUpperCase(), Integer.parseInt(args[1])));

                String datePattern = "MM-dd-yyyy HH:mm:ss";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
                String date = simpleDateFormat.format(expires);

                String reason = String.join(" ", Arrays.asList(args));
                reason = reason.substring(args[0].length() + args[1].length() + args[2].length() + 3);

                plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=mute | Target=" + uuid.toString() + " | Expires=" + date + " | Reason=" + reason);

                plugin.getPlayerData(uuid).setMute(expires, reason);
                if (target != null && target.isOnline()) target.sendMessage(plugin.getMessageFromConfig("mute.setMuted", Map.of("%expires%", date, "%reason%", reason)));
                player.sendMessage(plugin.getMessageFromConfig("mute.mutedPlayer", Map.of("%expires%", date, "%reason%", reason, "%player_name%", args[0])));

            } catch (IllegalArgumentException ex) { //Entered an invalid BanUnit value
                player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "Correct Usage: /mute <player> <time> <second,minute,hour,week,month,year> <reason>")));
                return false;
            }
        } else {
            plugin.getMessageFromConfig("player.playerDoesntExist", Map.of("%player_name%", args[0]));
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

        if (cmd.getName().toLowerCase().matches("mute")) {
            if (args.length <= 1) {
                List<String> players = new ArrayList<>();
                Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
                return players;
            } else if (args.length == 2)
                return Collections.singletonList("1");
            else if (args.length == 3)
                return Arrays.asList("second", "minute", "hour", "day", "week", "month", "year");
        }

        return null;
    }

    private enum MuteUnit {
        SECOND("sec", 1/60),
        MINUTE("min", 1),
        HOUR("hour", 60),
        DAY("day", 60*24),
        WEEK("week", 60*24*7),
        MONTH("month", 30*60*24),
        YEAR("year", 30*60*24*12);

        private int multiplier;

        MuteUnit(String unit, int multiplier) {
            this.multiplier = multiplier;
        }

        public static long getTicks(String unit, int time) {
            long sec;

            try {
                sec = time * 60;
            } catch (NumberFormatException ex) {
                return 0;
            }

            return (sec *= CommandMute.MuteUnit.valueOf(unit).multiplier * 1000);
        }
    }
}
