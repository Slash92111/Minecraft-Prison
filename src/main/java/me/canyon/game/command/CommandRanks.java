package me.canyon.game.command;

import me.canyon.Main;
import me.canyon.game.player.rank.Rank;
import me.canyon.game.player.rank.Staff;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandRanks extends BaseCommand {

    private int staffLevel;

    public CommandRanks(String command, String usage, String description, int staffLevel) {
        super(command, usage, description);
        this.staffLevel = staffLevel;
    }

    private Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;

        if (args.length == 0) {
            //TODO rank gui, also check if they have perms
            return true;
        }

        if (args.length < 3 && plugin.hasPlayerData(player) && plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel) {
            player.sendMessage(plugin.getMessageFromConfig("command.correctUsage", Map.of("%correct_usage%", "/<rank> <set> <prefix,color> <value>")));
            return false;
        }

        if (!(plugin.getPlayerData(player).getStaff().getOrder() >= staffLevel))
            return false;

        Rank rank = null;
        Staff staff = null;

        try {
            rank = Rank.valueOf(cmd.getName().toUpperCase());
        } catch (IllegalArgumentException e) {
            try {
                staff = Staff.valueOf(cmd.getName().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }



        if (args[0].toLowerCase().matches("set")) {

            if (args[1].toLowerCase().matches("prefix")) {
                if (rank != null) rank.setPrefix(args[2]);
                if (staff != null) staff.setPrefix(args[2]);

                plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=" + cmd.getName().toLowerCase() + " " + String.join(" ", Arrays.asList(args)));

                player.sendMessage(plugin.getMessageFromConfig("rank.update", Map.of("%rank%", cmd.getName().toUpperCase(), "%key%", args[1], "%value%", args[2])));
            } else if (args[1].toLowerCase().matches("color")) {
                for (ChatColor color : ChatColor.values()) {
                    if (color.name().equalsIgnoreCase(args[2])) {
                        if (rank != null) rank.setColor(color);
                        if (staff != null) staff.setColor(color);

                        plugin.getSplunkInstance().log("staff", "Command", "Sender=" + player.getUniqueId().toString() + " | Command=" + cmd.getName().toLowerCase() + " " + String.join(" ", Arrays.asList(args)));

                        player.sendMessage(plugin.getMessageFromConfig("rank.update", Map.of("%rank%", cmd.getName().toUpperCase(), "%key%", args[1], "%value%", args[2])));
                        return true;
                    }
                }
                player.sendMessage(plugin.getMessageFromConfig("rank.invalidColor"));
                if (rank != null) rank.setColor(ChatColor.WHITE);
                if (staff != null) staff.setColor(ChatColor.WHITE);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        String command = cmd.getName().toUpperCase();

        List<String> ranks = new ArrayList<>();
        for (Rank rank : Rank.values())
            ranks.add(rank.toString());
        for (Staff staff : Staff.values())
            ranks.add(staff.toString());

        if (ranks.contains(command)) {
            Rank rank = null;
            Staff staff = null;

            try {
                rank = Rank.valueOf(command);
            } catch (IllegalArgumentException e) {
                try {
                    staff = Staff.valueOf(command);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }

            if (! (sender instanceof Player))
                return null;

            Player cmdSender = (Player) sender;

            if (!(plugin.getPlayerData(cmdSender).getStaff().getOrder() >= staffLevel))
                return null;

            if (args.length <= 1)
                return Collections.singletonList("set");
            else if (args.length == 2) {
                List<String> subCommand = new ArrayList<>();
                subCommand.add("prefix");
                subCommand.add("color");
                return subCommand;
            } else if (args.length == 3) {
                if (args[1].toLowerCase().matches("prefix")) {
                    if (rank != null) return Collections.singletonList(rank.getPrefix());
                    if (staff != null) return Collections.singletonList(staff.getPrefix());
                    return null;
                } else if (args[1].toLowerCase().matches("color")) {
                    List<String> colors = new ArrayList<>();
                    for (ChatColor color : ChatColor.values())
                        colors.add(color.name());
                    return colors;
                }
            }
        }

        return null;
    }

}
